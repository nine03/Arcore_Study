package com.example.myapplication;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;

public class MainActivity extends AppCompatActivity {

    Session mSession;  // com.google.ar.core를 가지고옴
    GLSurfaceView mySurView;

    MainRenderer mRenderer;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mySurView = (GLSurfaceView) findViewById(R.id.glsurfaceview);


        MainRenderer.RenderCallBack mr = new MainRenderer.RenderCallBack() {
            @Override
            public void perRender() {

                // session 객체와 연결해서 화면 그리기 하기
                mSession.setCameraTextureName(mRenderer.getTextureId());
            }
        };


        mRenderer = new MainRenderer(mr);

        // pause 시 관련 데이터가 사라지는 것을 막는다.
        mySurView.setPreserveEGLContextOnPause(true);
        mySurView.setEGLContextClientVersion(2); // 버전을 2.0 사용

        // 화면을 그리는 Renderer를 지정한다.
        // 새로 정의한 MainRenderer를 사용한다.
        mySurView.setRenderer(mRenderer);
    }

    // 화면이 올라올떄마다 실행됨
    @Override
    protected void onResume() {
        super.onResume();
        cameraPerm();

        try {
            if(mSession == null) {

                // ARCore가 정상적으로 설치 되어 있는가?

               // Log.d("session requestInstall ?",
                 //       ArCoreApk.getInstance().requestInstall(this,true)+"");

                switch (ArCoreApk.getInstance().requestInstall(this,true)) {

                    case INSTALLED: // ARCore 정상설치 되었음
                        // ARCore가 정상설치 되어서 session을 생성가능한 형태임
                        mSession = new Session(this);
                        Log.d("session 안감","session 생성이여!!!");
                        break;

                    case INSTALL_REQUESTED: // ARCore 설치 필요
                        mSession = new Session(this);
                        Log.d("session 안감","session 생성이여!!!");
                        break;
                }

               // mSession = new Session(this);
               // Log.d("session 안감","session 생성이여!!!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // 카메라 퍼미션 요청
    void cameraPerm() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.CAMERA},
                    0
                    );
        }
    }
}