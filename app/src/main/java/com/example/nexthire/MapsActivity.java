package com.example.nexthire;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private double lat;
    private double lng;
    private String companyName;

    // Kod request untuk location permission
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // ✅ TAMBAH: GPS client
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng currentLatLng;

    // ✅ TAMBAH: API Key
    private static final String API_KEY = BuildConfig.MAPS_API_KEY;
    // ✅ TAMBAH: List untuk markers nearby
    private List<Marker> nearbyMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        lat = getIntent().getDoubleExtra("LATITUDE", 3.1473); // default Maybank
        lng = getIntent().getDoubleExtra("LONGITUDE", 101.6994);
        companyName = getIntent().getStringExtra("COMPANY_NAME");
        if (companyName == null) companyName = "Company Location";

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // ✅ TAMBAH: Initialize GPS client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // ✅ TAMBAH: Bind buttons
        Button btnLRT = findViewById(R.id.btnLRT);
        Button btnBus = findViewById(R.id.btnBus);
        Button btnParking = findViewById(R.id.btnParking);

        btnLRT.setOnClickListener(v -> searchNearby("LRT station|MRT station|KTM station"));
        btnBus.setOnClickListener(v -> searchNearby("bus stop"));
        btnParking.setOnClickListener(v -> searchNearby("parking lot"));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Marker untuk lokasi syarikat (SEDIA ADA - TAK DIUBAH)
        LatLng companyLatLng = new LatLng(lat, lng);
        mMap.addMarker(new MarkerOptions()
                .position(companyLatLng)
                .title(companyName)
                .snippet("NextHire Corporate Partner"));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(companyLatLng, 15.0f));
        mMap.getUiSettings().setZoomControlsEnabled(true);

        enableMyLocation();
        getCurrentLocation(); // ✅ TAMBAH: Dapatkan lokasi user
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    // ✅ TAMBAH: Dapatkan lokasi semasa pengguna
    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Task<Location> task = fusedLocationClient.getLastLocation();
        task.addOnSuccessListener(location -> {
            if (location != null) {
                currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                // Tunjuk marker "You are here" (biru)
                mMap.addMarker(new MarkerOptions()
                        .position(currentLatLng)
                        .title("📍 You are here")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

            } else {
                Toast.makeText(this, "Please turn on GPS!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ TAMBAH: Search Nearby Places guna Google Places API
    private void searchNearby(String keyword) {
        // Guna location company, bukan user
        LatLng companyLocation = new LatLng(lat, lng);

        // Clear previous markers
        for (Marker marker : nearbyMarkers) {
            marker.remove();
        }
        nearbyMarkers.clear();

        // Search based on COMPANY location
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + companyLocation.latitude + "," + companyLocation.longitude +
                "&radius=2000" +
                "&keyword=" + keyword +
                "&key=" + API_KEY;

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String status = response.getString("status");
                        if (!status.equals("OK")) {
                            Toast.makeText(this, "API Status: " + status, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JSONArray results = response.getJSONArray("results");
                        if (results.length() == 0) {
                            Toast.makeText(this, "No " + keyword + " found near office", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject place = results.getJSONObject(i);
                            JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                            double placeLat = location.getDouble("lat");
                            double placeLng = location.getDouble("lng");
                            String name = place.getString("name");
                            String vicinity = place.getString("vicinity");

                            LatLng placeLatLng = new LatLng(placeLat, placeLng);
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(placeLatLng)
                                    .title(name)
                                    .snippet(vicinity)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                            nearbyMarkers.add(marker);
                        }

                        Toast.makeText(this, "Found " + results.length() + " " + keyword + " near office ✅", Toast.LENGTH_SHORT).show();
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(companyLocation, 14));

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });

        queue.add(request);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null) {
                    try {
                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);
                        getCurrentLocation(); // ✅ TAMBAH: Ambil location lepas dapat permission
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}