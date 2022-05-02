package com.example.ex_06_painting;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Line {



    //GPU 를 이용하여 고속 계산 하여 화면 처리 하기 위한 코드
    String vertexShaderString =
            "attribute vec3 aPosition ; " +
                    "uniform vec4 aColor; " +
                    "uniform mat4 uMVPMatrix; " +  //4 x 4  형태의 상수로 지정
                    "varying vec4 vColor; " +
                    "void main () {" +
                    "    vColor = aColor; " +
                    "    gl_Position = uMVPMatrix * vec4(aPosition.x, aPosition.y, aPosition.z, 1.0) ;"+
                    "}";

    String fragmentShaderString =
            "precision mediump float; "+
                    "varying vec4 vColor; " +
                    "void main() { "+
                    "   gl_FragColor = vColor; "+
                    "}";


    float [] mModelMatrix = new float[16];
    float [] mViewMatrix = new float[16];
    float [] mProjMatrix = new float[16];

    float [] mColor  = new float[]{1.0f, 1.0f,1.0f, 1.0f};

    //현재 점 번호
    int mNumPoints = 0;

    //최대 점갯수 (점의 배열 좌표 갯수와 동일)
    int maxPoints = 1000;

    // 1000개의 점 * xyz
    float [] mPoint  = new float[maxPoints * 3];

    FloatBuffer mVertices;
    int mProgram;

    boolean isInited = false;

    int [] mVbo;

    //새로운 라인 만들기
    Line(){

        Log.d("라인", "라인생성");
    }

    //그리기 직전에 좌표 수정
    void update(){

        mVertices = ByteBuffer.allocateDirect(mPoint.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(mPoint);
        mVertices.position(0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,mVbo[0]);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0,mNumPoints *3 * Float.BYTES, mVertices);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        Log.d("라인", "그리기 전 갱신");

//        //색
//        mColors = ByteBuffer.allocateDirect(mColor.length * 4)
//                .order(ByteOrder.nativeOrder()).asFloatBuffer();
//        mColors.put(mColor);
//        mColors.position(0);
//
//        //순서
//        mIndices = ByteBuffer.allocateDirect(indices.length * 2)
//                .order(ByteOrder.nativeOrder()).asShortBuffer();
//        mIndices.put(indices);
//        mIndices.position(0);


    }

    // 점 추가? 점 갱신하기
    void updatePoint(float x, float y, float z){
        // 그린 점의 갯수가 최대치이면 점 정보를 갱신하지 않는다.
        if(mNumPoints >= maxPoints - 1){
            return;
        }

        //현재 점번호에 좌표 받는다.
        mPoint[mNumPoints*3 +0] = x; // xyz니까 *3
        mPoint[mNumPoints*3 +1] = y;
        mPoint[mNumPoints*3 +2] = z;
        mNumPoints++; //현재 점 번호 증가

        Log.d("라인", "점 추가");
    }

    //초기화
    void init(){

        mVbo = new int[1];
        GLES20.glGenBuffers(1,mVbo,0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,mVbo[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, maxPoints *3 * Float.BYTES, null, GLES20.GL_DYNAMIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);


        //점위치 계산식
        int vShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vShader, vertexShaderString);
        GLES20.glCompileShader(vShader);

        //텍스처
        int fShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fShader, fragmentShaderString);
        GLES20.glCompileShader(fShader);

        mProgram = GLES20.glCreateProgram();
        //점위치 계산식 합치기
        GLES20.glAttachShader(mProgram,vShader);
        //색상 계산식 합치기
        GLES20.glAttachShader(mProgram,fShader);
        GLES20.glLinkProgram(mProgram);

        isInited = true;

        Log.d("라인", " 초기화");
    }


    //도형그리기 --> MyGLRenderer.onDrawFrame() 에서 호출하여 그리기
    void draw(){

        GLES20.glUseProgram(mProgram);

        //점,색 계산방식
        int position = GLES20.glGetAttribLocation(mProgram, "aPosition");
        int color = GLES20.glGetUniformLocation(mProgram, "aColor");
        int mvp = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

        float [] mvpMatrix = new float[16];
        float [] mvMatrix = new float[16];

        Matrix.multiplyMM(mvMatrix, 0,mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0,mProjMatrix, 0,mvMatrix , 0);




        //GPU 활성화
        GLES20.glEnableVertexAttribArray(position);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,mVbo[0]);
        GLES20.glVertexAttribPointer(position, 3, GLES20.GL_FLOAT, false,4*3, 0);
        GLES20.glUniform4f(color, mColor[0],mColor[1],mColor[2],mColor[3]);
        //mvp 번호에 해당하는 변수에 mvpMatrix 대입
        GLES20.glUniformMatrix4fv(mvp,1, false, mvpMatrix,0);


        // 색 float * rgba
        // GLES20.glVertexAttribPointer(color, 3, GLES20.GL_FLOAT, false,4*4, mColors);

        //라인 두께
        GLES20.glLineWidth(50f);
        //그린다
        //
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, mNumPoints);

        //GPU 비활성화
        GLES20.glDisableVertexAttribArray(position);
        //  GLES20.glDisableVertexAttribArray(color);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,0);

    }

    void setmModelMatrix(float [] matrix){
        System.arraycopy(matrix, 0, mModelMatrix,0,16);
    }
    void updateProjMatrix(float [] projMatrix){
        System.arraycopy(projMatrix,0 , this.mProjMatrix, 0,        16);
    }

    void updateViewMatrix(float [] viewMatrix){
        System.arraycopy(viewMatrix,0 , this.mViewMatrix, 0,        16);
    }

}