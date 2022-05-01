package com.example.ex_05_motiontracking;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Line {
    // GPU를 이용하여 고속 계산 하여 화면 처리하기 위한 코드
    String vertexShaderCode =
            "attribute vec3 aPosition;" +
                    "attribute vec4 aColor;" +
                    "uniform mat4 uMVPMatrix;" +    // 4 x 4 형태의 상수로 지정
                    "varying vec4 vColor;" +
                    "void main (){" +
                    "vColor = aColor;" +
                    "gl_Position = uMVPMatrix * vec4(aPosition.x, aPosition.y, aPosition.z, 1.0);" +    // gl_Position : OpenGL에 있는 변수 ::> 계산식 uMVPMatrix * vPosition
                    "}";
    String fragmentShaderCode =
            "precision mediump float;" + //precision = 정밀도 mediump = 중간값
                    "varying vec4 vColor;" +
                    "void main() {" +
                    "gl_FragColor = vColor;" +
                    "}";

    float[] mModelMatrix = new float[16];
    float[] mViewMatrix = new float[16];
    float[] mProjMatrix = new float[16];

    FloatBuffer mVertices;
    FloatBuffer mColors;
    ShortBuffer mIndices;
    int mProgram;

    boolean isInited = false;

    public Line(float [] end, float x, float y, float z,  int color) {

        float[] vertices = {x, y, z, end[0], end[1], end[2]};

        float[] mColor = new float[] {
                Color.red(color) / 255.f,
                Color.green(color) / 255.f,
                Color.blue(color) / 255.f,
                1.0f,
                Color.red(color) / 255.f,
                Color.green(color) / 255.f,
                Color.blue(color) / 255.f,
                1.0f
        };

        short [] indices = {0, 1};

        // 점
        mVertices = ByteBuffer.allocateDirect(vertices.length * Float.SIZE / 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(vertices);
        mVertices.position(0);

        // 색
        mColors = ByteBuffer.allocateDirect(mColor.length * Float.SIZE / 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mColors.put(mColor);
        mColors.position(0);

        // 순서
        mIndices = ByteBuffer.allocateDirect(indices.length * Float.SIZE / 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        mIndices.put(indices);
        mIndices.position(0);

        Log.d("선이야 : ", Color.red(color) + ", " + Color.green(color) + ", " + Color.blue(color));

    }

    // 초기화
    void init(){

        // 점위치 계산식
        int vShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vShader, vertexShaderCode);
        GLES20.glCompileShader(vShader);

        // 텍스처
        int fShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fShader, fragmentShaderCode);
        GLES20.glCompileShader(fShader);

        mProgram = GLES20.glCreateProgram();
        //점위치 계산식 합치기
        GLES20.glAttachShader(mProgram, vShader);
        //색상 계산식 합치기
        GLES20.glAttachShader(mProgram, fShader);
        GLES20.glLinkProgram(mProgram);

        isInited = true;
    }


    // 도형 그리기 --> MainRenderer.onDrawFrame() 에서 호출하여 그리기
    void draw(float lineWeight) {
        GLES20.glUseProgram(mProgram);

        // 점, 색 계산방식
        int position = GLES20.glGetAttribLocation(mProgram, "aPosition");
        int color = GLES20.glGetAttribLocation(mProgram, "aColor");
        int mvp = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

        float[] mvpMatrix = new float[16];  // view
        float[] mvMatrix = new float[16];   // projection



        Matrix.multiplyMM(mvMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, mProjMatrix, 0, mvMatrix, 0);

        // mvp 번호에 해당하는 변수에 mvpMatrix 대입
        GLES20.glUniformMatrix4fv(mvp, 1, false, mvpMatrix, 0);

        // GPU 활성화
        GLES20.glEnableVertexAttribArray(position);
        GLES20.glEnableVertexAttribArray(color);
        GLES20.glLineWidth(lineWeight); // 라인 두께

        // 점, 색 번호에 해당하는 변수에 각각 대입
        // 점 float * 점 3(삼각형)
        GLES20.glVertexAttribPointer(position, 3, GLES20.GL_FLOAT, false, 4 * 3, mVertices);
        // 점 float * rgba
        GLES20.glVertexAttribPointer(color, 3, GLES20.GL_FLOAT, false, 4 * 4, mColors);


        // 그린다.
        //                        선으로 그린다.,       순서의 보유량,          순서 자료형,       순서 내용
        GLES20.glDrawElements(GLES20.GL_LINES, mIndices.capacity(), GLES20.GL_UNSIGNED_SHORT, mIndices);


        // GPU비활성화
        GLES20.glDisableVertexAttribArray(position);
        GLES20.glDisableVertexAttribArray(color);

    }

    void setmModelMatrix(float[] matrix){
        System.arraycopy(matrix, 0, mModelMatrix, 0, 16);
    }

    void updateViewMatrix(float[] mViewMatrix) {
        // 배열 복제
        //               원본        시작위치   복사될 배열    복사배열 시작위치      개수
        System.arraycopy(mViewMatrix, 0, this.mViewMatrix, 0, 16);
    }

    void updateProjMatrix(float[] mProjMatrix) {
        // 배열 복제
        //               원본        시작위치   복사될 배열    복사배열 시작위치      개수
        System.arraycopy(mProjMatrix, 0, this.mProjMatrix, 0, 16);
    }
}
