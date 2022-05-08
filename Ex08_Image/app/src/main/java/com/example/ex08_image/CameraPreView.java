package com.example.ex08_image;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.google.ar.core.Frame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class CameraPreView {


    // 점
    // GPU를 이용하여 고속 계산 하여 화면 처리 하기 위한 코드
    String vertexShaderCode =
            "attribute vec4 vPosition;"+
            "attribute vec2 aTexCoord;"+
            "varying vec2 vTexCoord;"+

            "void main () {" +
                 "vTexCoord = aTexCoord;"+
                 "gl_Position = vPosition;"+
            "}";

    String fragmentShaderCode =
            "#extension GL_OES_EGL_image_external : require \n"+
            "precision mediump float;"+
                    "uniform samplerExternalOES sTexture;"+
                    "varying vec2 vTexCoord;" +
                    "void main() {"+
                    "   gl_FragColor = texture2D(sTexture, vTexCoord);"+
                    "}";

    static float[] QUARD_COORDS = {
            // x,   y,    z
            -1.0f, -1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            1.0f, 1.0f, 0.0f,
    };

    static float[] QUARD_TEXCOORDS = {
           0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
    };

    int[] mTextures;
    FloatBuffer mVertices;  // 점 정보
    FloatBuffer mTexCoords;  // texture 좌표
    FloatBuffer mTextCoordsTransformed;

    int mProgram;

    CameraPreView(){
        mVertices = ByteBuffer.allocateDirect(QUARD_COORDS.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(QUARD_COORDS);
        mVertices.position(0);

        mTexCoords = ByteBuffer.allocateDirect(QUARD_TEXCOORDS.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTexCoords.put(QUARD_TEXCOORDS);
        mTexCoords.position(0);

        mTextCoordsTransformed = ByteBuffer.allocateDirect(QUARD_TEXCOORDS.length*4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    // 카메라 초기화
    void init(){
        // 텍스쳐 생성
        mTextures = new int[1];
        GLES20.glGenTextures(1,mTextures,0);

        // 텍스쳐 바인딩 -> 외부에서의 텍스쳐를 지정위치에 binding
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextures[0]);

        // glTexParameteri : 축소, 확대 필터를 설정, 점의 경계를 부드럽게 보느냐 반복시킬것인가 등등 설정
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);


        // 쉐이더 생성
        int vShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vShader, vertexShaderCode);

        // 컴파일
        GLES20.glCompileShader(vShader);

        //텍스쳐
        int fShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fShader, fragmentShaderCode);

        // 컴파일
        GLES20.glCompileShader(fShader);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vShader);
        GLES20.glAttachShader(mProgram, fShader);
        GLES20.glLinkProgram(mProgram);
    }

    // 카메라로 그리기
    void draw(){
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextures[0]);

        GLES20.glUseProgram(mProgram);

        int position = GLES20.glGetAttribLocation(mProgram, "vPosition");

        int texCoord = GLES20.glGetAttribLocation(mProgram, "aTexCoord");

        // 점, 색 좌표 계산
        GLES20.glVertexAttribPointer(position,3,GLES20.GL_FLOAT, false, 0, mVertices);
        GLES20.glVertexAttribPointer(texCoord,2,GLES20.GL_FLOAT, false, 0, mTextCoordsTransformed);

        // GPU 활성화
        GLES20.glEnableVertexAttribArray(position);
        GLES20.glEnableVertexAttribArray(texCoord);

        // 그린다
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0,4);

        // 비활성화
        GLES20.glDisableVertexAttribArray(position);
        GLES20.glDisableVertexAttribArray(texCoord);

    }

    // x, y, z  ==> 객체의 좌표 (이동, 회전, 크기)
    // u, v, ww ==> 맵핑(이미지)의 좌표 (이동, 회전, 크기)
    void transformDisplayGeometry(Frame frame){
        frame.transformDisplayUvCoords(mTexCoords,mTextCoordsTransformed);
    }
}
