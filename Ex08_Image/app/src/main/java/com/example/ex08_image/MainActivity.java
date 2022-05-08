package com.example.ex08_image;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.display.DisplayManager;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    GLSurfaceView mSurfaceView;
    MainRenderer mRenderer;

    Session mSession;
    Config mConfig;

    boolean mUserRequestedInstall = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideStatusBarAndTitleBar();
        setContentView(R.layout.activity_main);

        mSurfaceView = findViewById(R.id.glsurfaceview);
        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if(displayManager != null){
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {

                }

                @Override
                public void onDisplayRemoved(int displayId) {

                }

                @Override
                public void onDisplayChanged(int displayId) {
                    synchronized(this){
                        mRenderer.mViewportChanged = true;
                    }
                }
            },null);
        }

        mRenderer = new MainRenderer(this, new MainRenderer.RenderCallback() {
            @Override
            public void preRender() {
                if(mRenderer.mViewportChanged){
                    Display display = getWindowManager().getDefaultDisplay();
                    int displayRotation = display.getRotation();
                    mRenderer.updateSession(mSession, displayRotation);
                }

                mSession.setCameraTextureName(mRenderer.getTextureId());

                Frame frame = null;

                try{
                    frame = mSession.update();
                } catch (CameraNotAvailableException e){
                    e.printStackTrace();
                }

                if(frame.hasDisplayGeometryChanged()){
                    mRenderer.mCamera.transformDisplayGeometry(frame);
                }

//                mRenderer.mObj.setModelMatrix(modelMatrix);

                Camera camera = frame.getCamera();
                float [] projMatrix = new float[16];
                camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100f);
                float[] viewMatrix = new float[16];
                camera.getViewMatrix(viewMatrix, 0);

                // 이미지추적결과에 따른 그리기 설정
                drawImage(frame);

                mRenderer.setProjectionMatrix(projMatrix);
                mRenderer.updateViewMatrix(viewMatrix);

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
        requestCameraPermission();
        try {
            if(mSession == null){
                switch(ArCoreApk.getInstance().requestInstall(this, true)){
                    case INSTALLED:
                        mSession = new Session(this);
                        Log.d("메인", "ARCore session 생성");
                        break;
                    case INSTALL_REQUESTED:
                        Log.d("메인","ARCore 설치 필요");
                        mUserRequestedInstall = false;
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mConfig = new Config(mSession);

        mConfig.setFocusMode(Config.FocusMode.AUTO);
        // 이미지데이터베이스 설정
        setUpImgDB(mConfig);


        mSession.configure(mConfig);

        try {
            mSession.resume();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }
        mSurfaceView.onResume();
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    // 이미지데이터베이스 설정
    void setUpImgDB(Config config){
        // 이미지 데이터베이스 생성
        AugmentedImageDatabase imageDatabase = new AugmentedImageDatabase(mSession);

        try {
            // 파일스트림로드
            InputStream is = getAssets().open("botimg.png");
            // 파일스트림에서 Bitmap 생성
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            // 이미지데이터베이스에 bitmap 추가
            imageDatabase.addImage("로봇",bitmap);

            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // session config 생성한 이미지데이터베이스로 설정
        // 이미지추적 활성화
        config.setAugmentedImageDatabase(imageDatabase);

    }

    // 이미지추적결과에 따른 그리기 설정
    void drawImage(Frame frame){

        mRenderer.isImgFind = false;
        // frame(카메라) 에서 찾은 이미지들을 Collection으로 받아온다.
        Collection<AugmentedImage> updatedAugmentedImages =
                frame.getUpdatedTrackables(AugmentedImage.class);

        // 찾은 이미지들을 돌린다.
        for (AugmentedImage img : updatedAugmentedImages) {
            if (img.getTrackingState() == TrackingState.TRACKING) {
                mRenderer.isImgFind = true;
                Pose imgPose = img.getCenterPose();
                Log.d("이미지 찾음", img.getIndex() +", "+img.getName());
                if(!drawTag) {
                    float[] matrix = new float[16];
                    imgPose.toMatrix(matrix, 0);

                    Matrix.scaleM(matrix, 0, 0.02f, 0.02f, 0.02f);

                    moveObj(matrix);
                }

                switch (img.getTrackingMethod()) {
                    case LAST_KNOWN_POSE:
                        // The planar target is currently being tracked based on its last
                        // known pose.
                        break;
                    case FULL_TRACKING:
                        // The planar target is being tracked using the current camera image.
                        break;
                    case NOT_TRACKING:
                        // The planar target isn't been tracked.
                        break;
                }


            }
        }
    }

    boolean stop = false;
    boolean drawTag =false;

    void moveObj(float[] matrix) {
        if (!drawTag) {
            float[] newMatrix = new float[16];
            Matrix.translateM(matrix, 0, 0f, -10f, 0f);
            new Thread() {
                @Override
                public void run() {
                    int i = 0;
                    while (!stop) {
                        drawTag = true;
                        mRenderer.mObj.setModelMatrix(matrix);
                        Matrix.translateM(matrix, 0, 0f, 0.1f, 0f);
                        SystemClock.sleep(100);
                        i++;

                        if (i > 200) {
                            stop = true;
                            drawTag = false;
                        }
                    }
                }
            }.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSurfaceView.onPause();
        mSession.pause();

    }

    void hideStatusBarAndTitleBar(){
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    void requestCameraPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    0
            );
        }
    }

}