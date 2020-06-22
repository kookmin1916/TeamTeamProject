package com.example.teamteamproject;

import com.example.teamteamproject.R;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    GoogleMap google_map;

    ToggleButton toggle_button;

    InputStream input_stream;
    BufferedReader buffer_reader;

    final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    HashMap<String, JSONObject> data_map = new HashMap<>();

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);
        mapFragment.getMapAsync(this);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }
        } else {
        }

        toggle_button = (ToggleButton)findViewById(R.id.toggle1);
        final LocationManager location_manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        toggle_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    if(toggle_button.isChecked()) {
                        assert location_manager != null;
                        location_manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                100,
                                1,
                                mLocationListener);
                        location_manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                                100,
                                1,
                                mLocationListener);
                    } else{
                        assert location_manager != null;
                        location_manager.removeUpdates(mLocationListener);
                    }
                } catch(SecurityException ex){
                }
            }
        });

        try {
            input_stream = this.getResources().openRawResource(R.raw.seoul_road);
            buffer_reader = new BufferedReader(new InputStreamReader(input_stream, "EUC-KR"));

            InputStream json_input_stream = this.getResources().openRawResource(R.raw.road_data);
            BufferedReader json_buffer_reader = new BufferedReader(new InputStreamReader(json_input_stream, StandardCharsets.UTF_8));
            String json_str = json_buffer_reader.readLine();

            JSONObject json_object = new JSONObject(json_str);
            JSONArray json_array = json_object.getJSONArray("records");

            for(int i = 0; i < json_array.length(); i++) {
                JSONObject current_object = json_array.getJSONObject(i);
//                if (current_object.getString("관리기관명").equals("서울특별시 양천구청"))
                data_map.put(current_object.getString("길명"), current_object);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        google_map = googleMap;

        try {
            String str = buffer_reader.readLine();
            while((str = buffer_reader.readLine()) != null) {
                String name = str.split(",")[0];
                name = name.substring(1, name.length() - 1);
                JSONObject current = data_map.get(name);

                String explanation = current.getString("길소개");
                double longitude = Double.parseDouble(current.getString("경도"));
                double latitude = Double.parseDouble(current.getString("위도"));

                LatLng lat_lng = new LatLng(latitude, longitude);

                MarkerOptions marker_options = new MarkerOptions();
                marker_options.position(lat_lng);
                marker_options.title(name);
                marker_options.snippet(explanation);
                google_map.addMarker(marker_options);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        LatLng seoul = new LatLng(37.56, 126.97);

        google_map.moveCamera(CameraUpdateFactory.newLatLng(seoul));
        google_map.animateCamera(CameraUpdateFactory.zoomTo(11));
    }

    private final LocationListener mLocationListener = new LocationListener() {
        @SuppressLint("SetTextI18n")
        public void onLocationChanged(Location location) {
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();
            google_map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));
        }
        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };
}