package com.example.ex_02_opengl;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Square2 {

    // GPU를 이용하여 고속 계산하여 화면 처리하기 위한 코드
    String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" + // 4 x 4 형태의 상수로 지정
                    "attribute vec4 vPosition;" +  // vec4 -> 3차원 좌표
                    "void main() {" +
                    "gl_Position = uMVPMatrix * vPosition;" +
                    // gl_Position : OpenGL에 있는 변수 ::> 계산식 uMVPMatrix * vPosition
                    "}";

    String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "gl_FragColor = vColor;" +
                    "}";


    // 직사각형 점의 좌표
    static float [] squareCoords = {
            // x, y, z
            -0.5f, 0.5f, 1.0f,  // 왼쪽 위
            -0.5f, -0.5f, 1.0f, // 왼쪽 아래
            0.5f, -0.5f, 1.0f,   // 오른쪽 아래
            0.5f, 0.5f, 1.0f   // 오른쪽 위
    };

    float [] color = {0.0f, 1.0f, 0.0f, 1.5f};

    FloatBuffer vertexBuffer;
    ShortBuffer drawBuffer;
    int mProgram;


    short [] drawOrder = {
            0,1,2,
            0,2,3
    };

    public Square2(){
        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4); //float가 4바이트라 그 크기만큼 만들어줌
        bb.order(ByteOrder.nativeOrder());

        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        bb = ByteBuffer.allocateDirect(drawOrder.length * 2);

        bb.order(ByteOrder.nativeOrder());

        drawBuffer = bb.asShortBuffer();
        drawBuffer.put(drawOrder);
        drawBuffer.position(0);

        // 점위치 계산식
        // vertexShderCode -> vertexShader
        int vertexShader = MyGLRenderer.loadShader(
                GLES20.GL_VERTEX_SHADER,
                vertexShaderCode
        );

        // 점색상 계산식
        // fragmentShaderCode -> fragmentShader
        int fragmentShader = MyGLRenderer.loadShader(
                GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode
        );

        // mProgram = vertexShader + fragmentShader
        mProgram = GLES20.glCreateProgram();
        //점위치 계산식 합치기
        GLES20.glAttachShader(mProgram, vertexShader);
        //색상 계산식 합치기
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram); //도형 렌더링 계산식 정보 넣는다.
    }



    int mPositionHandle, mColorHandle, mMVPMatrixHandle;

    // 도형그리기 --> MyGLRenderer.onDrawFrame() 에서 호출하여 그리기
    void draw(float [] mMVPMatrix) {

        //렌더링 계산식 정보 사용한다.
        GLES20.glUseProgram(mProgram);

        // vPosition
        //mProgram ==> vertexShader
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        GLES20.glEnableVertexAttribArray(mPositionHandle);

        GLES20.glVertexAttribPointer(
                mPositionHandle,    // 정점 속성의 인덱스 지정
                3,           // 점속상 - 좌표계
                GLES20.GL_FLOAT,    // 점의 자료형 float
                false, // 정규화 true, 직접변환 false
                3 * 4,     // 점 속성의 stride(간격)
                vertexBuffer        // 점 정보
        );

        // vColor
        //mProgram ==> fragmentShader
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        mMVPMatrixHandle =  GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

        // 그려지는 곳에 위치, 보이는 정보를 적용한다.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // 직사각형 그린다.
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,
                drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT,
                drawBuffer
        );

        // 닫는다
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}