package com.example.fueleconomyassistant;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

//import com.google.android.maps.MapView;

//import org.apache.http.HttpRequest;
//import org.apache.http.HttpRequestFactory;
//import org.apache.http.client.HttpResponseException;

public class FuelMapActivity extends BaseActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener{

    private static final String PLACES_SEARCH_URL =  "https://maps.googleapis.com/maps/api/place/search/json?";
    private static final String API_KEY = "AIzaSyDCpu4StAm560O5yzLDfV5Iin2_eyZCrHQ";
    private static final String GAS_STATIONS_TYPE = "gas_station";
    private String preferedFueld = "";

    private TextView mFuelLevelView;
    private ArrayAdapter<Place> mAdapter;
    private ArrayList<Place> mPlaces;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private boolean mRequestingLocationUpdates = true;
    private LocationRequest mLocationRequest;

    private Timer mUpdateTimer;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        preferedFueld = prefs.getString("prefered_fuel_pref", "");
		setContentView(R.layout.activity_map);
		mPlaces = new ArrayList<Place>();
		mAdapter = new StationsAdapter(this, R.layout.station_item, mPlaces);
        ListView mListView = (ListView) findViewById(R.id.stations_list);
		mListView.setAdapter(mAdapter);
        mFuelLevelView = (TextView) findViewById(R.id.economy_value);

        Button mBackButton = (Button) findViewById(R.id.back_button);
		mBackButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        MapFragment mf = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        if(mf != null){
            mf.getMapAsync(this);
            Log.d("WILL", "map fragment was not null");
        }
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        mLocationRequest = new LocationRequest();
        long mLocUpdInterval = 1000;
        mLocationRequest.setInterval(mLocUpdInterval);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Place p = mPlaces.get(i);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" +
                        p.geometry.location.lat + "," + p.geometry.location.lng));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
//                Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
//                        Uri.parse("http://maps.google.com/maps?daddr="+  p.geometry.location.lat + "," + p.geometry.location.lng));
//                startActivity(intent);
            }
        });
//        startTestService();
//		MapView mv = (MapView) findViewById(R.id.mapView);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.map, menu);
		return true;
	}

    public void updateValues(){
        mUpdateTimer = new Timer();
        TimerTask t = new TimerTask() {
            @Override
            public void run() {
                Log.d("WILL", "looping");
                FuelMapActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ObdDataPoint currentLevel = MainActivity.mService.getFuelLevel();
                        if (currentLevel != null) {
                            String s = String.format("%.2f", currentLevel.getValue()) + "%";
                            mFuelLevelView.setText(s);
                        } else {
                            Log.d("WILL", "current level was null");
                        }
                    }
                });
            }
        };
        mUpdateTimer.schedule(t, 0, 10000);
    }

    private void stopUITimer(){
        if (mUpdateTimer != null) {
            mUpdateTimer.cancel();
            mUpdateTimer.purge();
            mUpdateTimer = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopUITimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if((mGoogleApiClient.isConnected() && mRequestingLocationUpdates)){
            stopLocationUpdates();
        }
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
        if(mUpdateTimer != null){
            stopUITimer();
        }
        updateValues();
    }
    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = (LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient));
        if (mLastLocation != null) {
            (new RunTask()).execute();
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
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //TODO: notify the user that we were unable to connect
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
    }

    private class StationsAdapter extends ArrayAdapter<Place>{

		public StationsAdapter(Context context, int resource, List<Place> objects) {
			super(context, resource, objects);
		}
		
		public View getView (int position, View convertView, ViewGroup parent){
            Place p = getItem(position);
			View v = getLayoutInflater().inflate(R.layout.station_item, parent, false);
			((TextView) v.findViewById(R.id.text1)).setText(p.name);
            float[] distMeters = new float[1];
			Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(), p.geometry.location.lat, p.geometry.location.lng, distMeters);
            double miles = distMeters[0]/1609.34;
            String milesString = String.format("%.1f mi", miles);
            ((TextView) v.findViewById(R.id.text2)).setText(milesString);
			
			return v;
		}
	}

    @Override
    public void onMapReady(GoogleMap googleMap){
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    public PlacesList performSearch() throws IOException {
        try {
            //all denoted by googles info, do not change strings
            System.out.println("Perform Search ....");
            System.out.println("-------------------");
            HttpRequestFactory httpRequestFactory = createRequestFactory(transport);
            HttpRequest request = httpRequestFactory.buildGetRequest(new GenericUrl(PLACES_SEARCH_URL));
            request.getUrl().put("key", API_KEY);
            request.getUrl().put("location", mLastLocation.getLatitude() + "," + mLastLocation.getLongitude());
            request.getUrl().put("radius", 50000);
            request.getUrl().put("sensor", "true");
            request.getUrl().put("type", GAS_STATIONS_TYPE);

                PlacesList places = request.execute().parseAs(PlacesList.class);
                System.out.println("STATUS = " + places.status);
                return places;

        } catch (HttpResponseException e) {
            System.err.println(e.getContent());
            throw e;
        }
    }

    private static final HttpTransport transport = new ApacheHttpTransport();

    public static HttpRequestFactory createRequestFactory(final HttpTransport transport) {

        return transport.createRequestFactory(new HttpRequestInitializer() {
            public void initialize(com.google.api.client.http.HttpRequest request) {
//                GoogleHeaders headers = new GoogleHeaders();
                HttpHeaders headers = new HttpHeaders();
                headers.setUserAgent("FuelAssistant-Places-Test");
                request.setHeaders(headers);
                JsonObjectParser parser = new JsonObjectParser(new JacksonFactory());
                request.setParser(parser);
            }
        });
    }

    private class RunTask extends AsyncTask<Void, Void, PlacesList>{
        @Override
        protected PlacesList doInBackground(Void... voids) {
            PlacesList places = null;
            try{
                places = performSearch();
            }catch(IOException e){
                Log.d("WILL", "EXCEPTION");
                e.printStackTrace();
            }
            return places;
        }

        @Override
        protected void onPostExecute(PlacesList places){
            if(places == null || !places.status.equals("OK")){
                //failed to get places
                return;
            }
            mPlaces.clear();

            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for(Place p : places.results){
                if(p.geometry != null && p.geometry.location != null){
                    LatLng latlng = new LatLng(p.geometry.location.lat, p.geometry.location.lng);
                    MarkerOptions marker = new MarkerOptions();
                    marker.position(latlng);
                    marker.title(p.name);
                    mMap.addMarker(marker);
                    builder.include(latlng);
                    mPlaces.add(p);
                }
            }
            Collections.sort(mPlaces, new Comparator<Place>() {
                @Override
                public int compare(Place place, Place place2) {
                    if(place.name.contains(preferedFueld) && !place2.name.contains(preferedFueld)){
                        return -1;
                    }else if(!place.name.contains(preferedFueld) && place2.name.contains(preferedFueld)){
                        return 1;
                    }
                    float[] dist1 = new float[1];
                    float[] dist2 = new float[1];
                    Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(), place.geometry.location.lat, place.geometry.location.lng, dist1);
                    Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(), place2.geometry.location.lat, place2.geometry.location.lng, dist2);
                    return Float.compare(dist1[0], dist2[0]);
                }
            });
            mAdapter.notifyDataSetChanged();
            final LatLngBounds bounds = builder.build();
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    FuelMapActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 20));
                        }
                    });
                }
            }, 1500);
        }
    }
}
