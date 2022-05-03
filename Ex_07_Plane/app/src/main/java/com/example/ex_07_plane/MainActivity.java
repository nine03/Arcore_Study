package com.example.ex_07_plane;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;

import java.util.Collection;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView myTextView;
    GLSurfaceView mSurfaceView;
    MainRenderer mRenderer;

    Session mSession;
    Config mConfig;

    boolean mUserRequestedInstall = true, mTouched = false;

    float mCurrentX, mCurrentY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideStatusBarANdTitleBar();
        setContentView(R.layout.activity_main);

        mSurfaceView = (GLSurfaceView)findViewById(R.id.gl_surface_view);
        myTextView = (TextView)findViewById(R.id.myTextView);

        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if(displayManager != null) {
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int i) {}

                @Override
                public void onDisplayRemoved(int i) {}

                @Override
                public void onDisplayChanged(int i) {
                    synchronized (this) {
                        mRenderer.mViewportChanged = true;
                    }
                }
            },null);
        }

        mRenderer = new MainRenderer(this,new MainRenderer.RenderCallback() {
            @Override
            public void preRender() {
                if(mRenderer.mViewportChanged){
                    Display display = getWindowManager().getDefaultDisplay();
                    int displayRotation = display.getRotation();
                    mRenderer.updateSession(mSession,displayRotation);
                }

                mSession.setCameraTextureName(mRenderer.getTextureId());

                Frame frame = null;

                try {
                    frame = mSession.update();
                } catch (CameraNotAvailableException e) {
                    e.printStackTrace();
                }

                if(frame.hasDisplayGeometryChanged()) {
                    mRenderer.mCamera.transformDisplayGeometry(frame);
                }

                PointCloud pointCloud = frame.acquirePointCloud();
                mRenderer.mPointCloud.update(pointCloud);
                pointCloud.release(); // 자원해제

                // 터치하였다면
                if(mTouched) {
                    List<HitResult> results = frame.hitTest(mCurrentX,mCurrentY); // 증강현실에서 x,y 좌표
                    for(HitResult result :results) {
                        Pose pose = result.getHitPose(); // 증강공간에서의 좌표
                        float [] modelMatrix = new float[16];
                        pose.toMatrix(modelMatrix,0); // 좌표를 가지고 matrix화 함

                        // 증강공간의 좌표에 객체 있는지 받아온다. (Plane이 걸려있는가?)
                        Trackable trackable = result.getTrackable();

                        //  좌표에 걸린 객체가 Plane 인가?(먼저) 그리고
                        if(trackable instanceof Plane &&
                                // Plane Polygon 플리곤(면)안에 좌표가 있는가?
                                ((Plane)trackable).isPoseInPolygon(pose)
                        ) {

                            // 큐브의 modelMatrix를 터치한 증강현실 modelMatrix로 설정
                            // mRenderer.mCube.setModelMatrix(modelMatrix);
                            mRenderer.mObj.setModelMatrix(modelMatrix);
                        }

                    }

                    mTouched = false;
                }

                // Session으로부터 증강현실 속에서의 평면이나 점 객체를 얻을 수 있다.
                //                           Plane   Point
                Collection<Plane> planes = mSession.getAllTrackables(Plane.class);
                // ARCore 상의 Plane들을 얻는다.

                boolean isPlaneDetected = false;

                for (Plane plane : planes) {
                    // plane이 정상이라면 getTrackingState 이 카메라의 현재 모션 추적 상태를 반환한다
                    if(plane.getTrackingState() == TrackingState.TRACKING && // 추적 가능 항목이 현재 추적되고 있으며 해당 포즈가 현재 상태
                    plane.getSubsumedBy() == null) {// plane.getSubsumedBy() 다른 평면이 존재하는지?

                        isPlaneDetected = true;
                        // 랜더링에서 plane 정보를 갱신하여 출력
                        mRenderer.mPlane.update(plane);
                    }
                }

                if(isPlaneDetected) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            myTextView.setText("평면 찾았어요");
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            myTextView.setText("평면 어디로 갔나?");
                        }
                    });
                }

                Camera camera = frame.getCamera(); // 카메라 달아줘야함
                float [] projMatrix = new float[16];
                camera.getProjectionMatrix(projMatrix,0,0.1f,100f);
                float [] viewMatrix = new float[16];
                camera.getViewMatrix(viewMatrix,0);

                mRenderer.setProjectionMatrix(projMatrix);
                mRenderer.updataViewMatrix(viewMatrix);
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
        requestCamerPermission();
        try{
        if(mSession == null) {
            switch(ArCoreApk.getInstance().requestInstall(this,mUserRequestedInstall)) {
                case INSTALLED:
                        mSession = new Session(this);
                        Log.d("메인","ARCore session 생성");
                        break;
                case INSTALL_REQUESTED:
                    Log.d("메인","ARCore 설치 필요함");
                    mUserRequestedInstall = false;
                    break;

            }
        }

            } catch (Exception e){
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

    void hideStatusBarANdTitleBar() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                );
    }

    void requestCamerPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {Manifest.permission.CAMERA},
                    0
                    );
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            mTouched = true;
            mCurrentX = event.getX();
            mCurrentY = event.getY();

        }
        return true;
    }


}