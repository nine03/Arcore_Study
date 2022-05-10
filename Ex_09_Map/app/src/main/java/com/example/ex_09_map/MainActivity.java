package com.example.ex_09_map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.LightEstimate;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    SupportMapFragment mapFragment;
    GLSurfaceView mSurfaceView;
    MainRenderer mRenderer;

    Session mSession;
    Config mConfig;

    boolean mUserRequestedInstall = true;

    float mCurrentX, mCurrentY;

    ArrayList<MyPlace> mPlaces = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestCameraPermission();

        hideStatusBarANdTitleBar();
        setContentView(R.layout.activity_main);


        mPlaces.add(new MyPlace("신논현역",37.50446350000012, 127.0161415999997 , Color.RED));
        mPlaces.add(new MyPlace("우리집",37.46469200000025, 127.0237048, Color.BLUE));
        mPlaces.add(new MyPlace("강남역",37.497951699999575,  127.02343460000012,  Color.YELLOW));
        mPlaces.add(new MyPlace("수원시청", 37.263475999999585,  127.02446179999973, Color.GREEN ));
        mSurfaceView = (GLSurfaceView)findViewById(R.id.gl_surface_view);
        mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map_fragment);


        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if(displayManager != null){
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int i) {}

                @Override
                public void onDisplayRemoved(int i) {}

                @Override
                public void onDisplayChanged(int i) {
                    synchronized (this){
                        mRenderer.mViewportChanged = true;
                    }
                }
            }, null);
        }

        mapFragment.getMapAsync(this);

        mRenderer = new MainRenderer(this,new MainRenderer.RenderCallback() {
            @Override
            public void preRender() {
                if(mRenderer.mViewportChanged){
                    Display display = getWindowManager().getDefaultDisplay();
                    int displayRotation = display.getRotation();
                    mRenderer.updateSession(mSession, displayRotation);
                }

                mSession.setCameraTextureName(mRenderer.getTextureId());

                Frame frame = null;

                try {
                    frame = mSession.update();
                } catch (CameraNotAvailableException e) {
                    e.printStackTrace();
                }

                if(frame.hasDisplayGeometryChanged()){
                    mRenderer.mCamera.transformDisplayGeometry(frame);
                }

                /*float [] modelMatrix = new float[16];
                float [] cubeMatrix = new float[16];
                Matrix.translateM(modelMatrix, 0, 0f, 0.0f, -2f);
                pose.toMatrix(modelMatrix,0); // 좌표를 가지고 matrix 화 함
                pose.toMatrix(cubeMatrix,0); // 좌표를 가지고 matrix 화 함
                mRenderer.mObj.setModelMatrix(modelMatrix);

                //큐브의 modelMatrix를 터치한 증강현실 modelMatrix로 설정
                mRenderer.mCube.setModelMatrix(cubeMatrix);*/


                Camera camera = frame.getCamera();
                float [] projMatrix = new float[16];
                camera.getProjectionMatrix(projMatrix,0,0.1f, 100f);
                float [] viewMatrix = new float[16];
                camera.getViewMatrix(viewMatrix,0);

                mRenderer.setProjectionMatrix(projMatrix);
                mRenderer.updateViewMatrix(viewMatrix);


                ///가운데 내 위치
                mRenderer.mObj.setViewMatrix(viewMatrix);

                //포지션 x,y,z 값을 가져온다.
                mePos = calculateInitialMePoint(
                        mRenderer.mViewportWidth,
                        mRenderer.mViewportHeight,
                        projMatrix,
                        viewMatrix
                );

                float [] modelMatrix = new float[16];
                //좌표 초기화
                Matrix.setIdentityM(modelMatrix,0);
                Matrix.translateM(modelMatrix,0,mePos[0],mePos[1],mePos[2]);
                Matrix.scaleM(modelMatrix,0,0.1f,0.1f,0.1f);
                mRenderer.mObj.setModelMatrix(modelMatrix);


                //큐브그리기
                if(makeCube && currentLocation!=null){ //지도에서 내 위치 찾아온 다음 그리기
                    //만들기 실행 표시
                    makeCube = false;
                    for (MyPlace place : mPlaces) {
                        //MyPlace place = mPlaces.get(0);
                        place.setArPosition(currentLocation, mePos);
                        mRenderer.addCube(place);
                    }

                }
            }
        });


        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8,8,8,8,16,0);
        mSurfaceView.setRenderer(mRenderer);

    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            if(mSession==null){
                switch(ArCoreApk.getInstance().requestInstall(this, mUserRequestedInstall)){
                    case INSTALLED:
                        mSession = new Session(this);
                        Log.d("메인"," ARCore session 생성");
                        break;
                    case INSTALL_REQUESTED:
                        Log.d("메인"," ARCore 설치가 필요함");
                        mUserRequestedInstall = false;
                        break;

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        mConfig = new Config(mSession);

        mSession.configure(mConfig);

        try {
            mSession.resume();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }

        mSurfaceView.onResume();
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mSurfaceView.onPause();
        mSession.pause();
    }

    void hideStatusBarANdTitleBar(){
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
    }

    void requestCameraPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                    },
                    0
            );
        }
    }

    // 박스 만들기 실시 여부
    boolean makeCube = true;

    //맵 정보 받을 객체
    GoogleMap mMap;
    // 현 장비의 위치 받을 객체
    FusedLocationProviderClient fusedLocClient;

    //현재 좌표
    Location currentLocation;

    //ARCore의 현재 위치 -- 실상 화면에 보이지 않아도 무방하다
    float [] mePos = new float[3];

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        googleMap.setMyLocationEnabled(true);
        fusedLocClient = LocationServices.getFusedLocationProviderClient(this);

        //위치정보 설정 요청 객체
        LocationRequest locRequest = LocationRequest.create();
        locRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); //고성능으로 처리
        locRequest.setInterval(1000); //1초마다 요청

        LocationCallback locCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                //super.onLocationResult(locationResult);
                //요청결과가 있다면
                if(locationResult != null){
                    for (Location loc:locationResult.getLocations() ) {
                        //현재 위치 받기
                        currentLocation = loc;
                        setLastLocation(loc);
                    }
                }
            }
        };

        //요청객체로 갱신정보 설정
        fusedLocClient.requestLocationUpdates(locRequest,locCallback, Looper.myLooper());
    }

    void places(){
        for (MyPlace place : mPlaces) {
            mMap.addMarker(
                    new MarkerOptions()
                    .position(place.latLng)
                    .title(place.title)
            );
        }
   }

   float bearing = 0f;

   boolean firstChk = true;
    void setLastLocation(Location loc){
        if(firstChk) {
           // firstChk = false;
            bearing +=15;
                                          //위도,              경도
            LatLng latlng = new LatLng(loc.getLatitude(), loc.getLongitude());

            CameraPosition camPos = new CameraPosition.Builder()
                    .target(latlng)
                    .tilt(45f)
                    .bearing(bearing)
                    .zoom(15f)
                    .build();

            places();

            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(camPos));
        }
    }


    //화면 가운데 내위치 보여주기
    float[] calculateInitialMePoint(int width, int height,
                                    float[] projMat, float[] viewMat) {
        return getScreenPoint(width / 2, height - 50.0f, width, height, projMat, viewMat);
    }
    //평면화
    public float[] getScreenPoint(float x, float y, float w, float h,
                                  float[] projMat, float[] viewMat) {
        float[] position = new float[3];
        float[] direction = new float[3];

        x = x * 2 / w - 1.0f;
        y = (h - y) * 2 / h - 1.0f;

        float[] viewProjMat = new float[16];
        Matrix.multiplyMM(viewProjMat, 0, projMat, 0, viewMat, 0);

        float[] invertedMat = new float[16];
        Matrix.setIdentityM(invertedMat, 0);
        Matrix.invertM(invertedMat, 0, viewProjMat, 0);

        float[] farScreenPoint = new float[]{x, y, 1.0F, 1.0F};
        float[] nearScreenPoint = new float[]{x, y, -1.0F, 1.0F};
        float[] nearPlanePoint = new float[4];
        float[] farPlanePoint = new float[4];

        Matrix.multiplyMV(nearPlanePoint, 0, invertedMat, 0, nearScreenPoint, 0);
        Matrix.multiplyMV(farPlanePoint, 0, invertedMat, 0, farScreenPoint, 0);

        position[0] = nearPlanePoint[0] / nearPlanePoint[3];
        position[1] = nearPlanePoint[1] / nearPlanePoint[3];
        position[2] = nearPlanePoint[2] / nearPlanePoint[3];

        direction[0] = farPlanePoint[0] / farPlanePoint[3] - position[0];
        direction[1] = farPlanePoint[1] / farPlanePoint[3] - position[1];
        direction[2] = farPlanePoint[2] / farPlanePoint[3] - position[2];

        normalize(direction);

        position[0] += (direction[0] * 0.1f);
        position[1] += (direction[1] * 0.1f);
        position[2] += (direction[2] * 0.1f);

        return position;
    }

    // 정규화
    private void normalize(float[] v) {
        double norm = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        v[0] /= norm;
        v[1] /= norm;
        v[2] /= norm;
    }
}