package com.example.ex_03_camera_share;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.google.ar.core.Frame;
import com.google.ar.core.Session;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/*
192.   168.    10.   ?
0~255  0~255 0~255 0~255

0 : 네트워크 ID
255 : BroadCast

1 번째는 변하지않음

255.       255.        255.        0
1111 1111  1111  1111  11111 1111 0000 0000
 */

public class MainRenderer implements GLSurfaceView.Renderer {
    final static String TAG = "MainRenderer :";

    Rendercallback myCallBack;
    CameraPreView mCamera;

    //화면이 변화되었다면 true
    boolean viewportChanged;

    int width, height;

    interface Rendercallback{
        void preRender(); //MainActivty에서 재정의하여 호출하게 함
    }

    //생성시 RenderCallBack을 매개변수로 대입받아 자신의 멤버
    // MainActivity에서 생성하므로 MainAcitivty의 것을 받아서 처리가능도록 한다.
    MainRenderer(Rendercallback myCallBack){
        mCamera = new CameraPreView();
        this.myCallBack = myCallBack;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        Log.d(TAG,  "onSurfaceCreated");

        //                    R        G          B         A      --> 노랑색
        GLES20.glClearColor(1.0f, 1.0f,0.0f, 1.0f); //노란색
        mCamera.init();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {

        Log.d(TAG,  "onSurfaceChanged");
        GLES20.glViewport(0,0,width, height);
        viewportChanged = true; // 화면이 돌아갔는지
        this.width = width;
        this.height = height;
    }

    @Override
    public void onDrawFrame(GL10 gl10) {

        //Log.d(TAG,  "onDrawFrame");
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT| GLES20.GL_DEPTH_BUFFER_BIT);

        //카메라로부터 새로 받은 영상으로 화면을 업데이트 할 것임
        myCallBack.preRender();

        GLES20.glDepthMask(false);
        mCamera.draw();
        GLES20.glDepthMask(true);

    }

    //화면 변환이 되었다는 것을 지시할 메소드 ==> MainActivity 에서 실행할 것이다.
    void onDisplayChanged(){
        viewportChanged = true;
    }

    //session 업데이트시 화면 변환 상태를 보고 session 의 화면을 변경한다.
    //보통 화면 회전에 대한 처리이다.
    void updateSession(Session session, int rotation){
        if(viewportChanged){

            //디스플레이 화면 방향 설정
            session.setDisplayGeometry(rotation,width,height);
            viewportChanged=false;
            Log.d(TAG,"UpdateSession 실행");
        }
    }

    int getTextureId(){
        return mCamera==null ? -1 : mCamera.mTextures[0]; // 카메라를 넘겨줘야한다. 만약에 카메라가 없으면 에러난다.
    }

    void transformDisplayGeometry(Frame frame){
        mCamera.transformDisplayGeometry(frame);
    }

}
