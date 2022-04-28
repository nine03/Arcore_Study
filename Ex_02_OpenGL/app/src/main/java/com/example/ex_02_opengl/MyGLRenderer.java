package com.example.ex_02_opengl;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLRenderer implements GLSurfaceView.Renderer {

//    Square  myBox;
//    Square1 myBox1;
//    Square2 myBox2;
//    Square3 myBox3;
//    Square4 myBox4;
//    Square5 myBox5;

    // 데이블을 붙이다
    ObjRenderer myTable;

    float [] mMVPMatrix = new float[16];
    float [] mProjectionMatrix = new float[16];
    float [] mViewMatrix = new float[16];

    //GLSurfaceView가 생성되었을때 한번 호출되는 메소드입니다.
    //OpenGL 환경 설정, OpenGL 그래픽 객체 초기화 등과 같은 처리를 할때 사용됩니다.

    // MyGLRenderer 생성자 생성후 context , objName 넣어주기
    // obj파일, jpg 파일을 가져와서 사용할수있다.
    MyGLRenderer(Context context) {
        myTable = new ObjRenderer(context,"table.obj","table.jpg");
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glClearColor(0.0f, 1.0f, 1.0f, 1.0f);

//        myBox = new Square();
//        myBox1 = new Square1();
//        myBox2 = new Square2();
//        myBox3 = new Square3();
//        myBox4 = new Square4();
//        myBox5 = new Square5();

        myTable.init();

    }

    //GLSurfaceView의 크기 변경 또는 디바이스 화면의 방향 전환 등으로 인해
    //GLSurfaceView의 geometry가 바뀔때 호출되는 메소드입니다.

    //화면갱신 되면서 화면 에서 배치
    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        //viewport를 설정합니다.
        //specifies the affine transformation of x and y from
        //normalized device coordinates to window coordinates
        //viewport rectangle의 왼쪽 아래를 (0,0)으로 지정하고
        //viewport의 width와 height를 지정합니다.
        GLES20.glViewport(0,0,width, height);

        float ratio = (float) width  * 30 / height;

        Matrix.frustumM(mProjectionMatrix, 0,-ratio,ratio,-10,10,20,300);
    }

    //GLSurfaceView가 다시 그려질때 마다 호출되는 메소드입니다.
    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        Matrix.setLookAtM(mViewMatrix, 0,
                //x, y, z
//                  -3,3,3,  //카메라 위치
//                0,0,0, //카메라 시선
//                0,1,0//카메라 윗방향

                80,80,150, //카메라 위치
                0,0,-30, //카메라 시선
                0,1,0 //카메라 윗방향

        );


        // mProjectionMatrix, mViewMatrix 합치다
        // Matrix.setIdentityM(mMVPMatrix,0);
      //  Matrix.multiplyMM(mMVPMatrix, 0,mProjectionMatrix,0, mViewMatrix,0);

        //정사각형 그리기
//        myBox5.draw(mMVPMatrix);
//        myBox3.draw(mMVPMatrix);
//        myBox2.draw(mMVPMatrix);
//        myBox1.draw(mMVPMatrix);
//        myBox4.draw(mMVPMatrix);
       // myBox.draw(mMVPMatrix);

        Matrix.setIdentityM(mMVPMatrix,0);
        myTable.setProjectionMatrix(mProjectionMatrix);
        myTable.setViewMatrix(mViewMatrix);
        myTable.setModelMatrix(mMVPMatrix);
        myTable.draw();
    }


    //GPU를 이용하여 그리기를 연산한다.
    static int loadShader(int type, String shaderCode){

        int res = GLES20.glCreateShader(type);

        GLES20.glShaderSource(res, shaderCode);
        GLES20.glCompileShader(res);
        return res;
    }
}
