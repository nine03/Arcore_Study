package com.example.ex_05_motiontracking;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.google.ar.core.Session;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainRenderer implements GLSurfaceView.Renderer { // GLSurfaceView.Renderer = 이너인터페이스

    RenderCallBack myCallBack;

    CameraPreView mCamera;
    PointCloudRenderer mPointCloud;
    Sphere sphere;

    // 화면이 변환되엇다면 true,
    boolean viewprotChanged;

    int width, height;


    interface RenderCallBack{
        void preRender(); // MainActivity 에서 재정의하여 호출하도록 함
    }
    
    // 생성시 RenderCallBack 을 매개변수로 대입받아 자신의 멤버로 넣는다
    // MainActivity 에서 생성하므로 MainActivity 의 것을 받아서 처리기능 토록 한다
    MainRenderer(RenderCallBack myCallBack){
        mCamera = new CameraPreView();
        mPointCloud = new PointCloudRenderer();
        sphere = new Sphere();

        this.myCallBack = myCallBack;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        Log.d("MainRenderer : ", "onSurfaceCreated() 실행");

//                              R          G         B          A   --> 노란색
        GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);

        mCamera.init(); // 초기화 시켜주기
        mPointCloud.init(); // 초기화 시켜주기
        sphere.init();
        if(mLineX!=null){
            mLineX.init();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        Log.d("MainRenderer : ", "onSurfaceChanged() 실행");

        GLES20.glViewport(0, 0, width, height);

        viewprotChanged = true;
        this.width = width;
        this.height = height;
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        Log.d("MainRenderer : ", "onDrawFrame() 실행");

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // 카메라로부터 새로 받은 영상으로 화면을 업데이트 할 것임
        myCallBack.preRender();

        // 카메라로 받은 화면 그리기
        GLES20.glDepthMask(false);
        mCamera.draw();
        GLES20.glDepthMask(true);

        // 포인트클라우드 그리기
        // 도형(직사각형)과 비슷
        mPointCloud.draw();

        // 점 그리기
        sphere.draw();
        
        // 선그리기
        if(mLineX!=null) {
            if(!mLineX.isInited) {
                mLineX.init();
            }
            mLineX.draw();
        }
        if(mLineY!=null) {
            if(!mLineY.isInited) {
                mLineY.init();
            }
            mLineY.draw();
        }
        if(mLineZ!=null) {
            if(!mLineZ.isInited) {
                mLineZ.init();
            }
            mLineZ.draw();
        }
    }
    
    // 화면 변환이 되었다는 것을 지시할 메소드 ==> MainActivity 에서 실행할 것이다
    void onDisplayChanged(){
        viewprotChanged = true;
        Log.d("MainRenderer => onDisplayChanged : ",   viewprotChanged+" 실행");
    }

    // session 업데이트시 화면 변환 상태를 보고 session 의 화면을 변경한다
    // 보통 화면 회전에 대한 처리이다
    void updateSession(Session session, int rotation) {
        if (viewprotChanged){
            
            // 디스플레이 화면 방향 설정
            session.setDisplayGeometry(rotation, width, height);
            viewprotChanged = false;
//            Log.d("MainRenderer : ", "updateSession 실행");
        }
    }

    int getTextureId(){
        return mCamera == null ? -1 : mCamera.mTextures[0];
    }

//    void transformDisplayGeometry(Frame frame) {
//        mCamera.transformDisplayGeometry(frame);
//    }

    void addPoint(float x, float y, float z){
        float [] matrix = new float[16];
        Matrix.setIdentityM(matrix, 0);
        Matrix.translateM(matrix, 0, x, y, z);

        sphere.addCount();
        sphere.setmModelMatrix(matrix);
//        System.arraycopy(matrix, 0, sphere.mModelMatrix, 0, 16);
    }
    Line mLineX, mLineY, mLineZ;
    //float [] projMatrix = new float[16];
    
    public void addLineX(float [] pps , float x, float y, float z) {
        mLineX = new Line(pps,x,y,z,Color.RED);
        // 생성하고 나서 객체가 있어야 한다.
        float [] matrix = new float[16];
        Matrix.setIdentityM(matrix, 0);
        Matrix.translateM(matrix, 0, x, y, z);
        mLineX.setmModelMatrix(matrix);
    }
    public void addLineY(float [] pps , float x, float y, float z) {
        mLineY = new Line(pps,x,y,z,Color.GREEN);

        float [] matrix = new float[16];
        Matrix.setIdentityM(matrix, 0);
        Matrix.translateM(matrix, 0, x, y, z);
        mLineY.setmModelMatrix(matrix);
    }
    public void addLineZ(float [] pps , float x, float y, float z) {
        mLineZ = new Line(pps,x,y,z,Color.BLUE);
        float [] matrix = new float[16];
        Matrix.setIdentityM(matrix, 0);
        Matrix.translateM(matrix, 0, x, y, z);
        mLineZ.setmModelMatrix(matrix);
    }

    void updateProjMatrix(float [] projMatrix){
        //System.arraycopy(projMatrix, 0, this.projMatrix, 0 , 16);
        mPointCloud.updateProjMatrix(projMatrix);
        sphere.updateProjMatrix(projMatrix);
        if(mLineX!=null) {
            mLineX.updateProjMatrix(projMatrix);
        }
        if(mLineY!=null) {
            mLineY.updateProjMatrix(projMatrix);
        }
        if(mLineZ!=null) {
            mLineZ.updateProjMatrix(projMatrix);
        }
    }

    void updateViewMatrix(float [] viewMatrix){
        mPointCloud.updateViewMatrix(viewMatrix);
        sphere.updateViewMatrix(viewMatrix);

        if(mLineX!=null) {
            mLineX.updateViewMatrix(viewMatrix);
        }
        if(mLineY!=null) {
            mLineY.updateViewMatrix(viewMatrix);
        }
        if(mLineZ!=null) {
            mLineZ.updateViewMatrix(viewMatrix);
        }
    }
}
