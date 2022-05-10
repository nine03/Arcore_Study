package com.example.ex_09_map;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.Arrays;

public class MyPlace {
    String title;
    LatLng latLng;
    double latitude, longitute;
    int color;

    double [] arPos;

    MyPlace(String title, double latitude, double longitute, int color){
        this.title = title;
        this.latLng = new LatLng(latitude, longitute);
        this.latitude = latitude;
        this.longitute = longitute;
        this.color = color;
    }

    double rate = 10;
    void setArPosition(Location currentLocation, float [] mePos){
        arPos = new double[]{
                mePos[0]+(currentLocation.getLongitude()-longitute)*rate,
                mePos[1]+(currentLocation.getLatitude()-latitude)*rate,
                mePos[2]*3
        };
        Log.d("내 좌표", Arrays.toString(mePos));
        Log.d("큐브 좌표", Arrays.toString(arPos));
    }
}
