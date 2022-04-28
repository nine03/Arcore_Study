package com.example.test;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "MyGLRenderer";
    private Triangle mTriangle;

    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mTranslateMatrix = new float[16];
    private int screenHeight;
    private int screenWidth;


    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig eglConfig) {
        GLES20.glClearColor(0.0f,0.0f,0.0f,1.0f);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        mTriangle = new Triangle();
        Matrix.setIdentityM(mTranslateMatrix,0);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;

        GLES20.glViewport(0,0,width,height);
        float ratio = (float) width / height;
        Matrix.frustumM(mProjectionMatrix,0,-ratio,ratio,-1,1,3,10);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        float[] scratch = new float[16];

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        Matrix.setLookAtM(mViewMatrix,0,0,0,-5,0f,0f,0f,0f,1.0f,0f);
        Matrix.multiplyMM(mMVPMatrix,0,mProjectionMatrix,0,mViewMatrix,0);
        Matrix.multiplyMM(scratch,0,mMVPMatrix,0,mTranslateMatrix,0);
        mTriangle.draw(scratch);
    }

    public static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader,shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR){
            Log.e(TAG, glOperation + ": glError" + error);
            throw new RuntimeException(glOperation + ": glError" + error);
        }
    }

    public void translate(float dx, float dy, float dz) {
        Matrix.translateM(mTranslateMatrix,0,dx * 2f / screenHeight,dy * 2f / screenHeight, dz * 2f /screenHeight);
    }

    public void rotate(float dx, float dy) {
        Matrix.rotateM(mTranslateMatrix,0, dx * screenWidth / screenHeight,0,1f,0f);
        Matrix.rotateM(mTranslateMatrix,0,dy * screenWidth / screenHeight, 1f,0f,0f);
    }
}
