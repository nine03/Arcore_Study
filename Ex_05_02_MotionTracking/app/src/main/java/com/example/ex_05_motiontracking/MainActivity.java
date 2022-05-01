package com.example.ex_05_motiontracking;

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
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    Session mSession;
    GLSurfaceView mySurView;
    TextView my_textView;
    MainRenderer mRenderer;
    Config mConfig;    // ARCore Session 설정정보를 받을 변수
    Float displayX, displayY;
    Boolean mTouched = false;

    String ttt;

    Button blackBtn;
    Button whiteBtn;
    Button redBtn;
    Button greenBtn;
    Button blueBtn;
    SeekBar seekBar;

    Pose pose;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //타이틀 바 없애기
        hideStatusBar();

        setContentView(R.layout.activity_main);
        mySurView = (GLSurfaceView) findViewById(R.id.glsurfaceview);
        my_textView = (TextView) findViewById(R.id.my_textView);
        blackBtn = (Button) findViewById(R.id.blackBtn); // 검은색 버튼
        whiteBtn = (Button) findViewById(R.id.whiteBtn); // 하얀색 버튼
        redBtn = (Button) findViewById(R.id.redBtn); // 빨간색 버튼
        greenBtn = (Button) findViewById(R.id.greenBtn); // 초록색 버튼
        blueBtn = (Button) findViewById(R.id.blueBtn); // 파란색 버튼
        seekBar = (SeekBar) findViewById(R.id.seekBar); 

        // 선이 생기면 SeekBar도 생기도록
        seekBar.setVisibility(View.GONE);

        // 검정색을 눌렀을때 동작 
        blackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRenderer.sphereColor(0.0f, 0.0f, 0.0f, 0.0f);
                mRenderer.addPoint(pose.tx(), pose.ty(), pose.tz());

            }
        });
        
        // 하얀색을 눌렀을때 동작
        whiteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRenderer.sphereColor(1.0f, 1.0f, 1.0f, 1.0f);
                mRenderer.addPoint(pose.tx(), pose.ty(), pose.tz());
            }
        });

        // 검정색을 눌렀을때 동작 
        redBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 버튼의 색상을 가져오는것에 실패하여 임의로 부여하여 전달했습니다..

//                ColorDrawable colorD = (ColorDrawable) redBtn.getBackground();
//                int color = colorD.getColor();
//                Log.d("redBtn Color ==> " , color+""); // -65536이 뜸..?

                mRenderer.sphereColor(1.0f, 0.0f, 0.0f, 1.0f);
                mRenderer.addPoint(pose.tx(), pose.ty(), pose.tz());
            }
        });

        // 초록색을 눌렀을때 동작 
        greenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRenderer.sphereColor(0.0f, 1.0f, 0.0f, 1.0f);
                mRenderer.addPoint(pose.tx(), pose.ty(), pose.tz());

            }
        });

        // 파란색을 눌렀을때 동작 
        blueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRenderer.sphereColor(0.0f, 0.0f, 1.0f, 1.0f);
                mRenderer.addPoint(pose.tx(), pose.ty(), pose.tz());

            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Seekbar 가 변경 되면서 받아온 int 값을 float형으로 전달하여 라인을 다시 그리게 한다.
                float lineWeight = progress;
                mRenderer.lineWeight = lineWeight;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //MainAtivity의 화면 관리 매니저 --> 화면변화를 감지 :: 현재 시스템에서 서비스 지원
        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);

        //화면 변화가 발생되면 MainRenderer의 화면 변환을 실행시킨다.
        if(displayManager != null){
            //화면에 대한 리스너 실행
            displayManager.registerDisplayListener(
                    // 익명클래스로 정의
                    new DisplayManager.DisplayListener() {
                        @Override
                        public void onDisplayAdded(int displayId) {

                        }

                        @Override
                        public void onDisplayRemoved(int displayId) {

                        }

                        // 화면이 변경되었다면
                        @Override
                        public void onDisplayChanged(int displayId) {
                            synchronized (this) {
                                //화면 갱신 인지 메소드 실행
                                mRenderer.onDisplayChanged();
                            }
                        }
                    },
                    null
            );
        }

        MainRenderer.RendererCallBack mr = new MainRenderer.RendererCallBack() {
            // 랜더러 작업
            @Override
            public void preRender() {
                //화면이 회전되었다면,
                if (mRenderer.viewprotChanged){
                    //현재 화면 가져오기
                    Display display = getWindowManager().getDefaultDisplay();

                    mRenderer.updateSession(mSession, display.getRotation());
                }

                // session 객체와 연결해서 화면 그리기 하기
                mSession.setCameraTextureName(mRenderer.getTextureId());

                // 화면 그리기에서 사용할 frame --> session 이 업데이트 되면 새로운 프레임을 받는다.
                Frame frame = null;

                try {
                    frame = mSession.update();
                } catch (CameraNotAvailableException e) {
                    e.printStackTrace();
                }
                // 화면(카메라 정보)을 바꾸기 위한 작업
                mRenderer.transformDisplayGeometry(frame);


                //// ↓↓↓↓↓↓↓ PointCloud 설정 구간

                // ARCore 에 정의된 클래스로
                // 현재 프레임에서 특정있는 점들에 대한 포인트값(3차원 좌표값)
                PointCloud pointCloud = frame.acquirePointCloud();

                // 포인트 값을 적용시키기위해 MainRenderer -> PointCloud.update() 실행
                mRenderer.mPointCloud.update(pointCloud);

                // 사용이 끝난 포인트 자원해제
                pointCloud.release();

                int i = 0;

                ttt = "";

                /* 화면 터치시 작업 시작*/
                if (mTouched){
                    List<HitResult> arr = frame.hitTest(displayX, displayY);
                    Log.d("preRender : ", "건드렸다." + arr);
//                    int i = 0;
//                    ttt = "";

                    for(HitResult hr : arr){
                        pose = hr.getHitPose();

                        // getXAxis = float[] 로 반환됨
                        // 축의 방향성을 알려줄 때 점이 세개가 필요하여 배열로 제공됨
                        float[] xx = pose.getXAxis();
                        float[] yy = pose.getYAxis();
                        float[] zz = pose.getZAxis();

                        // pose.qx, qy, qz는 회전값에 대한 정보
                        // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓ 는 좌표값에 대한 정보
                        mRenderer.addPoint(pose.tx(), pose.ty(), pose.tz());
                        // x축
                        mRenderer.addLineX(xx, pose.tx(), pose.ty(), pose.tz());
                        // y축
                        mRenderer.addLineY(yy, pose.tx(), pose.ty(), pose.tz());
                        // z축
                        mRenderer.addLineZ(zz, pose.tx(), pose.ty(), pose.tz());

                        // Log.d("arr " + i + ":", hr.toString());
                        Log.d("arr" + i + ":", arr.toString());
                        ttt += pose.toString()+"/n";
                        i++;
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            my_textView.setText(ttt);
                            seekBar.setVisibility(View.VISIBLE);
                        }
                    });

                    mTouched = false;

                }
                /* 화면 터치시 작업 끝*/

                // 카메라 frame 에서 받는다
                // --> mPointCloud 에서 렌더링 할 때 카메라의 좌표계산을  받아서 처리
                Camera camera = frame.getCamera();

                float[] projMatrix = new float[16];
                float[] viewMatrix = new float[16];

                camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100.0f);

                camera.getViewMatrix(viewMatrix, 0);

//                mRenderer.mPointCloud.updateMatrix(viewMatrix, projMatrix);
                mRenderer.updateViewMatrix(viewMatrix);
                mRenderer.updateProjMatrix(projMatrix);
            }
        };
        mRenderer = new MainRenderer(mr);

        // pause 시 관련 데이터가 사라지는 것을 막는다.
        mySurView.setPreserveEGLContextOnPause(true);
        // // 버전을 2.0 사용
        mySurView.setEGLContextClientVersion(2);

        // 화면을 그리는 Renderer를 지정한다.
        // 새로 정의한 MainRenderer를 사용한다.
        mySurView.setRenderer(mRenderer);

        // 랜더링 계속 호출
        mySurView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mySurView.onPause();
        mSession.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraPerm();

        try {
            if (mSession == null){
                // ARCore가 정상적으로 설치되어있는가?
                // Log.d("session requestInstall ? ",
                // ArCoreApk.getInstance().requestInstall(this, true) + "");
                switch(ArCoreApk.getInstance().requestInstall(this,true)){
                    case INSTALLED : // ARCore 정상설치 됨
                        // ARCore 가 정상설치 되어서 session 을 생성가능한 형태임
                        mSession = new Session(this);
                        Log.d("session 인감", "session 생성이여!!!");
                        break;

                    case INSTALL_REQUESTED : // ARCore 설치필요
                        Log.d("session 인감", "ARCore INSTALL_REQUSTED");
                        break;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //화면 갱신 시 Session정보 를 받아서 내Session의 설정으로 올린다.
        mConfig = new Config(mSession);

        mSession.configure(mConfig);

        try {
            mSession.resume();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }
        mySurView.onResume();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mTouched = true;
        displayX = event.getX();
        displayY = event.getY();

        // event.getX(), event.getY() : 화면에서의 좌표
        // Log.d("MainActivity : ", "건드렸다." + event.getX() + "," + event.getY());
        return true;
    }

    // 카메라 퍼미션 요청
    void cameraPerm(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},0);

        }
    }

    void hideStatusBar(){

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
    }

}
