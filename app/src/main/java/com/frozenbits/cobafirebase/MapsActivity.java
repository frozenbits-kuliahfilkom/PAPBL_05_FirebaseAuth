package com.frozenbits.cobafirebase;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private FirebaseAuth mAuth;
    private static final int RC_SIGN_IN = 999;
    private GoogleMap mMap;
    private Double lat, longi;
    Button btn_logout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        btn_logout = findViewById(R.id.btn_logout);
        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                finish();
                startActivity(getIntent()); //back to login page
            }
        });
        btn_logout.setVisibility(View.INVISIBLE);

        if (currentUser == null){ //berarti user belum login
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build(),
                    new AuthUI.IdpConfig.GoogleBuilder().build()
            );

            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    RC_SIGN_IN);
        } else {
            startMap();
            btn_logout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    protected void createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        lat = locationResult.getLastLocation().getLatitude();
                        longi = locationResult.getLastLocation().getLongitude();
                        LatLng currentLocation = new LatLng(lat, longi);

                        Context context = getApplicationContext();
                        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                        List<Address> alamat;
                        String namaJalan = "", namaKelurahan = "", kodePos = "";
                        try {
                            alamat = geocoder.getFromLocation(lat, longi, 1);
                            if (alamat != null && alamat.size() > 0) {
                                namaJalan = alamat.get(0).getAddressLine(0);
                                namaKelurahan = alamat.get(0).getSubLocality();
                                kodePos = alamat.get(0).getPostalCode();
                                mMap.addMarker(new MarkerOptions().position(currentLocation).title(namaKelurahan+", "+kodePos));
                            } else {
                                Toast gagal = Toast.makeText(context, "Maaf, tidak dapat menemukan alamat Anda di Maps.", Toast.LENGTH_LONG);
                                gagal.setGravity(Gravity.TOP|Gravity.CENTER, 0, 20);
                                gagal.show();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(currentLocation, 15);
                        mMap.animateCamera(yourLocation);
                    }
                },
                Looper.myLooper());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN){
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if(resultCode == RESULT_OK){ //login sukses
                startMap();
                btn_logout.setVisibility(View.VISIBLE);
            } else { //login gagal
                Toast err = Toast.makeText(this, response.getError().getErrorCode(), Toast.LENGTH_LONG);
                err.show();
            }
        }
    }

    private void startMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        getLocationPermission();
        createLocationRequest();
    }
}
