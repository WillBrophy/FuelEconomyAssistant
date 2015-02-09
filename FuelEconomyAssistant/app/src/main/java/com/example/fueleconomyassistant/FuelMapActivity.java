package com.example.fueleconomyassistant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.maps.MapActivity;
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
import com.google.gson.Gson;
//import com.google.android.maps.MapView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
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

import org.json.JSONArray;
import org.json.JSONObject;

//import org.apache.http.HttpRequest;
//import org.apache.http.HttpRequestFactory;
//import org.apache.http.client.HttpResponseException;

public class FuelMapActivity extends Activity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener{

    private static final String PLACES_SEARCH_URL =  "https://maps.googleapis.com/maps/api/place/search/json?";
    private static final String API_KEY = "AIzaSyDCpu4StAm560O5yzLDfV5Iin2_eyZCrHQ";
    private static final boolean PRINT_AS_STRING = false;
    private static final String GAS_STATIONS_TYPE = "gas_station";

    private Button mBackButton;
	private ListView mListView;
	private ArrayAdapter<Place> mAdapter;
    private ArrayList<Place> mPlaces;
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
		mPlaces = new ArrayList<Place>();
		mAdapter = new StationsAdapter(this, R.layout.station_item, mPlaces);
		mListView = (ListView) findViewById(R.id.stations_list);
		mListView.setAdapter(mAdapter);
		
		mBackButton = (Button) findViewById(R.id.back_button);
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
        mLocationRequest.setInterval(mLocUpdInterval);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Place p = mPlaces.get(i);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" +
                p.geometry.location.lat + "," + p.geometry.location.lng));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if(intent.resolveActivity(getPackageManager()) !=  null){
                    startActivity(intent);
                }

//                Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
//                        Uri.parse("http://maps.google.com/maps?daddr="+  p.geometry.location.lat + "," + p.geometry.location.lng));
//                startActivity(intent);
            }
        });
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
        Log.d("WILL" ,"In on connected");
        mLastLocation = (LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient));
        if (mLastLocation != null) {
//            LatLng latlng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
//            MarkerOptions marker = new MarkerOptions();
//            marker.position(latlng);
//            marker.title("You are here!");
//            mMap.addMarker(marker);
//            Log.d("WILL", "added marker");
            (new RunTask()).execute();
//            mMap.clear();
        }else{
            Log.d("WILL", "mLastLocatoin was null");
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
        mLastLocation = location;
        Log.d("WILL", "in on location changed");
//        if (mLastLocation != null) {
//            LatLng latlng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
////            mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
////            mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
//            MarkerOptions marker = new MarkerOptions();
//            marker.position(latlng);
//            marker.title("You are here!");
//            mMap.addMarker(marker);
//            Log.d("WILL", "added marker");
//            (new RunTask()).execute();
////            mMap.clear();
//        }else{
//            Log.d("WILL", "mLastLocatoin was null");
//        }
    }

    private class StationsAdapter extends ArrayAdapter<Place>{

		public StationsAdapter(Context context, int resource, List<Place> objects) {
			super(context, resource, objects);
			// TODO Auto-generated constructor stub
			
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
        Log.d("WILL", "in on map ready");
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    public PlacesList performSearch() throws Exception {
        try {
            System.out.println("Perform Search ....");
            System.out.println("-------------------");
            HttpRequestFactory httpRequestFactory = createRequestFactory(transport);
            HttpRequest request = httpRequestFactory.buildGetRequest(new GenericUrl(PLACES_SEARCH_URL));
            request.getUrl().put("key", API_KEY);
            request.getUrl().put("location", mLastLocation.getLatitude() + "," + mLastLocation.getLongitude());
            request.getUrl().put("radius", 50000);
            request.getUrl().put("sensor", "true");
            request.getUrl().put("type", GAS_STATIONS_TYPE);
            Log.d("WILL", request.getUrl().toString());

            if (PRINT_AS_STRING) {
                System.out.println(request.execute().parseAsString());
            } else {
                PlacesList places = request.execute().parseAs(PlacesList.class);
                System.out.println("STATUS = " + places.status);
                return places;

            }

        } catch (HttpResponseException e) {
            System.err.println(e.getContent());
            throw e;
        }
        return null;
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
            }catch(Exception e){
                Log.d("WILL", "EXCEPTION");
                e.printStackTrace();
            }
            return places;
        }

        @Override
        protected void onPostExecute(PlacesList places){
            if(places == null || !places.status.equals("OK")){
                Log.e("WILL", "Failed to get places");
                return;
            }
            mPlaces.clear();

            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            int i = 0;
            for(Place p : places.results){
                if(p.geometry != null && p.geometry.location != null){
                    i++;
                    LatLng latlng = new LatLng(p.geometry.location.lat, p.geometry.location.lng);
                    MarkerOptions marker = new MarkerOptions();
                    marker.position(latlng);
                    marker.title(p.name);
//                    marker.icon(getMarkerIcon(i));
                    mMap.addMarker(marker);
                    builder.include(latlng);
                    mPlaces.add(p);
                }else{
                    if(p.geometry == null){
                        Log.d("WILL", "geometry was null for " + p.name);
                    }else{
                        Log.d("WILL", "location was null for " + p.name);
                    }
                }
            }
            Collections.sort(mPlaces, new Comparator<Place>() {
                @Override
                public int compare(Place place, Place place2) {
                    float[] dist1 = new float[1];
                    float[] dist2 = new float[1];
                    Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(), place.geometry.location.lat, place.geometry.location.lng, dist1);
                    Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(), place2.geometry.location.lat, place2.geometry.location.lng, dist2);
                    return Float.compare(dist1[0], dist2[0]);
                }
            });
            mAdapter.notifyDataSetChanged();
            LatLngBounds bounds = builder.build();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 20));
        }
        private BitmapDescriptor getMarkerIcon(int i){
            Bitmap.Config conf = Bitmap.Config.ARGB_8888;
            Bitmap bmp = Bitmap.createBitmap(200, 50, conf);
            Canvas canvas = new Canvas(bmp);
            Paint paint = new Paint();
            canvas.drawText(Integer.toString(i), 0, 50, paint); // paint defines the text color, stroke width, size
//            mMap.addMarker(new MarkerOptions()
//                            .position(clickedPosition)
//                                    //.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker2))
//                            .icon(BitmapDescriptorFactory.fromBitmap(bmp))
//                            .anchor(0.5f, 1)
//            );
            return BitmapDescriptorFactory.fromBitmap(bmp);
        }


    }
}
