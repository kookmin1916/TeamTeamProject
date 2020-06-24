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
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.BufferedReader;
import java.io.Console;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    GoogleMap google_map;
    Geocoder geocoder;

    ToggleButton toggle_button, start_button;
    TextView recommend_text_view, chosen_text_view;

    InputStream input_stream;
    BufferedReader buffer_reader;

    HashMap<String, JSONObject> data_map = new HashMap<>();
    List<JSONObject> road_list = new ArrayList<>();
    List<Marker> marker_list = new ArrayList<>();
    String chosen_road = "";
    Polyline last_polyline = null;

    final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    final int INF = 987654321;

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

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.INTERNET)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.INTERNET},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }
        } else {
        }

        recommend_text_view = (TextView) findViewById(R.id.recommend_text_view);
        chosen_text_view = (TextView) findViewById(R.id.chosen_text_view);
        toggle_button = (ToggleButton) findViewById(R.id.toggle1);
        start_button = (ToggleButton) findViewById(R.id.toggle2);
        geocoder = new Geocoder(this);

        final LocationManager location_manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        toggle_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (toggle_button.isChecked()) {
                        assert location_manager != null;
                        location_manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                100,
                                1,
                                mLocationListener);
                        location_manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                                100,
                                1,
                                mLocationListener);
                    } else {
                        recommend_text_view.setText("");
                        assert location_manager != null;
                        location_manager.removeUpdates(mLocationListener);
                    }
                } catch (SecurityException ex) {
                }
            }
        });

        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(start_button.isChecked()) {
                    if(!chosen_road.equals("")) {
                        try {
                            for(int i = 0; i < marker_list.size(); i++)
                                marker_list.get(i).setAlpha(0);

                            LatLng last = new LatLng(37.56, 126.97);
                            String[] route = data_map.get(chosen_road).getString("경로정보").split(" - |→");
                            List<LatLng> positions = new ArrayList<>();
                            for(int i = 0; i < route.length; i++) {
                                LatLng lat_lng = searchPosition(last, route[i]);
                                if(lat_lng == null) {
                                    showToastMessage("네트워크 오류");
                                    continue;
                                }
                                last = lat_lng;
                                positions.add(lat_lng);
                            }
                            PolylineOptions polyline_options = new PolylineOptions();
                            polyline_options.color(Color.RED);
                            polyline_options.width(5);
                            polyline_options.addAll(positions);
                            polyline_options.jointType(JointType.ROUND);
                            last_polyline = google_map.addPolyline(polyline_options);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    for(int i = 0; i < marker_list.size(); i++)
                        marker_list.get(i).setAlpha(1);

                    if(last_polyline != null)
                        last_polyline.remove();
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

            for (int i = 0; i < json_array.length(); i++) {
                JSONObject current_object = json_array.getJSONObject(i);
//                if (current_object.getString("관리기관명").equals("서울특별시 양천구청"))
                data_map.put(current_object.getString("길명"), current_object);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            String str = null;
            str = buffer_reader.readLine();
            while ((str = buffer_reader.readLine()) != null) {
                String name = str.split(",")[0];
                name = name.substring(1, name.length() - 1);
                JSONObject current = data_map.get(name);
                road_list.add(current);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        google_map = googleMap;

        initMarker();

        double latitude = INF;
        double longitude = INF;

        try {
            FileInputStream file_input_stream = null;
            file_input_stream = openFileInput("last_position.txt");
            BufferedReader file_buffer_reader = new BufferedReader(
                    new InputStreamReader(file_input_stream));

            String data = file_buffer_reader.readLine();
            if(data != null) {
                String[] position = data.split(" ");
                latitude = Double.parseDouble(position[0]);
                longitude = Double.parseDouble(position[1]);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        LatLng lat_lng = new LatLng(37.56, 126.97);
        if(latitude != INF)
            lat_lng = new LatLng(latitude, longitude);

        google_map.setOnMarkerClickListener(this);

        google_map.moveCamera(CameraUpdateFactory.newLatLng(lat_lng));
        google_map.animateCamera(CameraUpdateFactory.zoomTo(11));
    }

    private final LocationListener mLocationListener = new LocationListener() {
        @SuppressLint("SetTextI18n")
        public void onLocationChanged(Location location) {
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();
            google_map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));

            try {
                FileOutputStream file_output_stream = null;
                file_output_stream = openFileOutput("last_position.txt", Context.MODE_PRIVATE);
                file_output_stream.write((String.valueOf(latitude) + " "
                        + String.valueOf(longitude)).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

            double min_dist = INF;
            int idx = -1;
            try {
                for(int i = 0; i < road_list.size(); i++) {
                    double dist = distance(latitude, longitude,
                            Double.parseDouble(road_list.get(i).getString("위도")),
                                    Double.parseDouble(road_list.get(i).getString("경도")));
                    if(dist < min_dist) {
                        min_dist = dist;
                        idx = i;
                    }
                }

                if(idx != -1)
                    recommend_text_view.setText(road_list.get(idx).getString("길명")
                            + "(" + String.valueOf((int)(min_dist * 1000)) + "m)");
            }catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private void file_output_stream(String s, int modePrivate) {
        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    @Override
    public boolean onMarkerClick(Marker marker) {
        if(marker.getAlpha() == 1) {
            marker.showInfoWindow();
            chosen_road = marker.getTitle();
            chosen_text_view.setText(chosen_road);
        }
        return true;
    }

    public void initMarker() {
        try {
            for(int i = 0; i < road_list.size(); i++) {
                JSONObject current = road_list.get(i);

                assert current != null;
                String name = current.getString("길명");
                String explanation = current.getString("길소개");
                double latitude = Double.parseDouble(current.getString("위도"));
                double longitude = Double.parseDouble(current.getString("경도"));

                LatLng lat_lng = new LatLng(latitude, longitude);

                MarkerOptions marker_options = new MarkerOptions();
                marker_options.position(lat_lng);
                marker_options.title(name);
                marker_options.snippet(explanation);
                marker_list.add(google_map.addMarker(marker_options));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public double distance(double lat1, double lon1, double lat2, double lon2) {
        lon1 = Math.toRadians(lon1);
        lat1 = Math.toRadians(lat1);
        lon2 = Math.toRadians(lon2);
        lat2 = Math.toRadians(lat2);

        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dlon / 2), 2);

        double c = 2 * Math.asin(Math.sqrt(a));
        double r = 6371;
        return (c * r);
    }

    public LatLng searchPosition(LatLng last, String name) {
        List<Address> addressList = null;

        try{
            addressList = geocoder.getFromLocationName(name,10);
            if(addressList.size() == 0)
                return null;

            double min_dist = INF, min_lat = INF, min_lon = INF;
            for(int j = 0; j < addressList.size(); j++) {
                String[] split_str = addressList.get(j).toString().split(",");
                int lat_idx = -1, lon_idx = -1;
                for (int i = 0; i < split_str.length; i++) {
                    if (split_str[i].length() < 3)
                        continue;
                    if (split_str[i].substring(0, 3).equals("lat"))
                        lat_idx = i;
                    if (split_str[i].substring(0, 3).equals("lon"))
                        lon_idx = i;
                }
                if (lat_idx == -1 || lon_idx == -1)
                    continue;

                double lat = Double.parseDouble(split_str[lat_idx].substring(9));
                double lon = Double.parseDouble(split_str[lon_idx].substring(10));
                double dist = distance(last.latitude, last.longitude, lat, lon);
                if(dist < min_dist) {
                    min_dist = dist;
                    min_lat = lat;
                    min_lon = lon;
                }
            }

            if(min_dist == INF)
                return null;

            return new LatLng(min_lat, min_lon);
        } catch(IOException | ArrayIndexOutOfBoundsException e){
            e.printStackTrace();
            return null;
        }

    }

    public void showToastMessage(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT);
    }
}