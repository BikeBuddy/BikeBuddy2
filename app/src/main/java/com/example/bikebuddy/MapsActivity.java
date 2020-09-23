package com.example.bikebuddy;

import androidx.annotation.NonNull;


import android.location.Address;
import android.location.Geocoder;


import com.google.android.gms.common.api.Status;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerDragListener  {

    private static final String TAG = MapsActivity.class.getSimpleName();

    private GoogleMap mMap;

    public WeatherFunctions weatherFunctions;
    public FetchWeather fetchWeather;
    HashMap<String, String> weatherIcons;

    private RouteManager routeManager;
    private Geocoder geoCoder;
    private float zoomLevel = 10.0f;
    private LatLng currentLocation;
    private List<Address> locationsList;
    private Bitmap smallMarker;

    private CameraPosition cameraPosition;
    private FusedLocationProviderClient fusedLocationProviderClient;
    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    protected Location lastKnownLocation;
    // init data for autocomplete to store
    protected LatLng autoCompleteLatLng;

    // A default location (Auckland, New Zealand) and default zoom to use when location permission is
    // A default location (Auckland, New Zealand) and default zoom to use when location permission is
    // not granted.
    private final LatLng defaultLocation = new LatLng(-36.8483, 174.7625);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        setContentView(R.layout.activity_maps);
        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        geoCoder = new Geocoder(this);
    }


    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
        }
        super.onSaveInstanceState(outState);
    }


    // initialise places API
    private void initPlaces() {
        // Initialize Places.
        Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));

        // Create a new Places client instance.
        PlacesClient placesClient = Places.createClient(this);
    }

    // initialise autocomplete search bar
    private void initAutoComplete() {
        final AutocompleteSupportFragment autocompleteSupportFragment =
                (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // restrict place field results to ID, Address, LatLng, and Name (basic data, no extra fees)
        autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME));

        // restrict results to nz --- could be changed to grab the user's geolocated country.
        autocompleteSupportFragment.setCountry("nz");

        autocompleteSupportFragment.setOnPlaceSelectedListener((new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                // grab found location data from 'place'
                // currently just grabbing LatLng for marker making
                autoCompleteLatLng = place.getLatLng();
                routeManager.recieveLatLong(autoCompleteLatLng);
                // display found lat long (for debugging)
                //Toast.makeText(MapsActivity.this, "LAT"+autoCompleteLatLng.latitude+"\nLONG"+autoCompleteLatLng.longitude, Toast.LENGTH_LONG).show();

                // go to found location
                mMap.animateCamera(CameraUpdateFactory.newLatLng(autoCompleteLatLng));
                // make marker
                // MarkerOptions searchedLocationMarker = new MarkerOptions().position(autoCompleteLatLng).title(place.getAddress());
                //mMap.addMarker(searchedLocationMarker);
            }
            @Override
            public void onError(@NonNull Status status) {

            }
        }));
    }


    //instead of making the button invisible should we change the text to instructions, eg "please select destination"
    public void toggleRouteButton() {
        // make route button visible
        View routeButt = findViewById(R.id.route_button);
        if(routeButt.getVisibility() == View.INVISIBLE)
        {
            routeButt.setVisibility(View.VISIBLE);
        }
        else
        {
            routeButt.setVisibility(View.INVISIBLE);
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mMap = googleMap;

        initFetchWeather();
        initWeatherFunctions();

        HashMap<String, Drawable> weatherIcons = new HashMap<String, Drawable>();

        // stock google maps UI buttons
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // call initialisations
        initPlaces();
        initAutoComplete();

        this.routeManager = new RouteManager(this, mMap, geoCoder);
        routeManager.routeFetcher = new RouteFetcher(getResources().getString(R.string.google_maps_key), mMap); //jsonRoutes needs reference to mMap

        // start the camera above nz
       // mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(-42, 172)));
       // mMap.moveCamera(CameraUpdateFactory.zoomTo(5));
        this.mMap.setOnCameraIdleListener(onCameraIdleListener);

        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
        this.mMap.setInfoWindowAdapter(customInfoWindowAdapter);
        
        toggleRouteButton();
    
        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();

        //sets origin to gps location
        routeManager.setUpOriginFromLocation(lastKnownLocation);

        //action listener for draggable markers
        mMap.setOnMarkerDragListener(this);

        //ActionListener for long press --PK
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            public void onMapLongClick(LatLng latLng) {
                routeManager.recieveLatLong(latLng);
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            }
        });
    }





    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            } else {
                //location services denied, move camera to default location
                mMap.moveCamera(CameraUpdateFactory
                        .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                mMap.getUiSettings().setMyLocationButtonEnabled(false);

            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                getDeviceLocation();
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                //getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }


    // Use a custom info window adapter to handle multiple lines of text in the
    // info window contents.
    private GoogleMap.InfoWindowAdapter customInfoWindowAdapter =
            new GoogleMap.InfoWindowAdapter() {
                @Override
                // Return null here, so that getInfoContents() is called next.
                public View getInfoWindow(Marker arg0) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    // Inflate the layouts for the info window, title and snippet.
                    View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents,
                            (FrameLayout) findViewById(R.id.map), false);

                    TextView title = infoWindow.findViewById(R.id.title);
                    title.setText(marker.getTitle());

                    TextView snippet = infoWindow.findViewById(R.id.snippet);
                    snippet.setText(marker.getSnippet());

                    return infoWindow;
                }
            };

    private GoogleMap.OnCameraIdleListener onCameraIdleListener =
        new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                zoomLevel = mMap.getCameraPosition().zoom;
                currentLocation = mMap.getCameraPosition().target;
                // Grid locations
                //generateLocations(currentLocation);
                //displayLocations(smallMarker);
                // Geocoder locations
                //creates new list of locations based on camera centre position.
                locationsList = getAddressListFromLatLong(currentLocation.latitude, currentLocation.longitude);
                getLocationsWeather();
            }
        };

    public  List<Address> getAddressListFromLatLong(double lat, double lng) {

        Geocoder geocoder = new Geocoder(this);

        List<Address> addressList = null;
        try {
            addressList = geocoder.getFromLocation(lat, lng, 20);
            // 20 is no of address you want to fetch near by the given lat-long
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return addressList;
    }

    public void getLocationsWeather() {

        mMap.clear();
/*        for (Address a : locationsList) {
            fw.fetch(a.getLatitude(), a.getLongitude());
            //delete a from list after request?
        }*/
        if (locationsList != null ){
            //had to change to iterator in order to delete
            Iterator<Address> it = locationsList.iterator();
            while (it.hasNext()) {
                Address a = it.next();
                fetchWeather.fetch(a.getLatitude(), a.getLongitude());
                it.remove();
            }
        }
        routeManager.updateMap();
    }

    public void initWeatherFunctions() {
       this.weatherFunctions = new WeatherFunctions(this, this.mMap);
    }

    public void initFetchWeather() {
        this.fetchWeather = new FetchWeather(this);
    }


    //updates the snippet, Address etc when start and destination markers are dragged
    public void onMarkerDragEnd(Marker marker) {
        mMap.clear();//clears the old poly line if there was one
        routeManager.startingOrigin.update();
        routeManager.theDestination.update();
    }
    public void onMarkerDragStart(Marker marker) {    }
    public void onMarkerDrag(Marker marker) {    }


}

