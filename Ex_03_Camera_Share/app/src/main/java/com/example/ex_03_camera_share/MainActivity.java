package com.example.ex_03_camera_share;

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

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;

public class MainActivity extends AppCompatActivity {

    Session mSession; // com.google.ar.core를 가지고옴

    GLSurfaceView mySurView;

    MainRenderer mRenderer;

    Config mConfig; //ARCore session 설정정보를 받을 변수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mySurView = (GLSurfaceView)findViewById(R.id.glsurfaceview);

        //MainActivity의 화면관리 매니저 --> 화면변화를 감지 :: 현재 시스템에서 서비스 지원
        DisplayManager displayManager = (DisplayManager)getSystemService(DISPLAY_SERVICE);

        //화면 변화가 발생되면 MainRenderer의 화면변환을 실행시킨다.
        if(displayManager != null){

            //화면에 대한 리스너 실행
            displayManager.registerDisplayListener(

                    //익명클래스로 정의
                    // onDisplayAdded, onDisplayRemoved 설정안해도된다.
                    new DisplayManager.DisplayListener() {
                        @Override
                        public void onDisplayAdded(int i) {

                        }

                        @Override
                        public void onDisplayRemoved(int i) {

                        }

                        //화면이 변경되었다면
                        @Override
                        public void onDisplayChanged(int i) {
                            synchronized (this) {
                                //화면 갱신 인지 메소드 실행
                                mRenderer.onDisplayChanged();
                            }
                        }
                    } , null
            );
        }

        MainRenderer.Rendercallback mr = new MainRenderer.Rendercallback() {

            //렌더링 작업
            @Override
            public void preRender() {

                //화면이 회전되었다면
                if(mRenderer.viewportChanged){
                    //현재 화면 가져오기
                    Display display = getWindowManager().getDefaultDisplay();
                    mRenderer.updateSession(mSession,display.getRotation());
                }
                //sesson 객체와 연결해서 화면 그리게 하기
                mSession.setCameraTextureName(mRenderer.getTextureId());

                //화면 그리기에서 사용할 frame --> session 이 업데이트 되면 새로운 프레임을 받는다.
                Frame frame = null;

                // 카메라가 제대로 작동안하면 에러난다.
                try {
                    frame = mSession.update();
                } catch (CameraNotAvailableException e) {
                    e.printStackTrace();
                }

                //화면을 바꾸기 위한 작업
                mRenderer.mCamera.transformDisplayGeometry(frame);

            }
        };
        mRenderer = new MainRenderer(mr);


        //pause시 관련데이터가 사라지는것을 막는다.
        mySurView.setPreserveEGLContextOnPause(true);
        //버전을 2.0사용
        mySurView.setEGLContextClientVersion(2); // 버전을 2.0 사용
        //화면을 그리는 Renderer를 지정한다.
        //새로 정의한 MainRenderer을 사용한다.
        mySurView.setRenderer(mRenderer);

        //렌더링 계속 호출
        mySurView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mySurView.onPause();
        mSession.pause();
    }

    // 화면이 올라올때마다 실행됨
    @Override
    protected void onResume() {
        super.onResume();
        cameraPerm();

        //ARCore가 정상적으로 설치 되어 있는가?
        // Log.d("session requestInstall ?",
        //       ArCoreApk.getInstance().requestInstall(this,true)+"");
        try {
            if(mSession == null){
                switch (ArCoreApk.getInstance().requestInstall(this, true)){
                    case INSTALLED: //정상적으로 설치되어있으면 Session을 생성한다
                        mSession = new Session(this);
                        Log.d("Session?", "Session!");
                        break;
                    case INSTALL_REQUESTED: // ARCore 설치 필요
                        Log.d("Session?", "Session!");
                        break;
                }
                // mSession = new Session(this);
                // Log.d("session 안감","session 생성이여!!!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //화면 갱신시 세션설정 정보를 받아서 내세션의 설정으로 올린다.
        mConfig = new Config(mSession);
        mSession.configure(mConfig);
        try {
            mSession.resume();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }
        mySurView.onResume();
    }

    //카메라 퍼미션 요청
    void cameraPerm(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.CAMERA}, 0);
        }
    }
}