package com.example.fueleconomyassistant;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.maps.MapActivity;
//import com.google.android.maps.MapView;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class FuelMapActivity extends Activity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener{
	private Button mBackButton;
	private ListView mListView;
	private ArrayAdapter<String> mAdapter;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private boolean mRequestingLocationUpdates = true;
    private LocationRequest mLocationRequest;
    private long mLocUpdInterval = 1000;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);
		ArrayList<String> test = new ArrayList<String>();
		test.add("mobile");
		test.add("shell");
		test.add("exxon");
		test.add("BP");
		
		mAdapter = new StationsAdapter(this, R.layout.station_item, test);
		mListView = (ListView) findViewById(R.id.stations_list);
		mListView.setAdapter(mAdapter);
		
		mBackButton = (Button) findViewById(R.id.back_button);
		mBackButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});
        MapFragment mf = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        if(mf != null){
            mf.getMapAsync(this);
            Log.d("WILL", "map fragment was not null");
        }
        buildGoogleApiClient();
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(mLocUpdInterval);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);


//		MapView mv = (MapView) findViewById(R.id.mapView);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.map, menu);
		return true;
	}

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        Log.d("WILL" ,"In on connected");
        if (mLastLocation != null) {
            LatLng latlng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
//            mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
//            mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
            MarkerOptions marker = new MarkerOptions();
            marker.position(latlng);
            marker.title("You are here!");
            mMap.addMarker(marker);
            Log.d("WILL", "added marker");
//            mMap.clear();
        }

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        //TODO: notify the user that we have lost connection
        Log.d("WILL", "in on connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //TODO: notify the user that we were unable to connect
        Log.d("WILL", "in on connection failed");
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    private class StationsAdapter extends ArrayAdapter<String>{

		public StationsAdapter(Context context, int resource, List<String> objects) {
			super(context, resource, objects);
			// TODO Auto-generated constructor stub
			
		}
		
		public View getView (int position, View convertView, ViewGroup parent){
			View v = getLayoutInflater().inflate(R.layout.station_item, parent, false);
			((TextView) v.findViewById(R.id.text1)).setText(this.getItem(position));
			((TextView) v.findViewById(R.id.text2)).setText("12.8 miles");
			
			return v;
		}
	}
	


    @Override
    public void onMapReady(GoogleMap googleMap){
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        Log.d("WILL", "in on map ready");
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
}
