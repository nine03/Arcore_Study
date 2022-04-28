package com.example.test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.opengl.GLES20;
import android.opengl.Matrix;


public class Triangle {

    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "attribute vec4 aColor;" +  // 각 vertex에 있을 색상(rgba)값 버퍼
                    "varying vec4 ourColor;" +  // fragement shader로 넘어갈 보간 값
                    "void main() {" +
                    "    gl_Position = uMVPMatrix * vPosition;" +
                    "    ourColor = aColor;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "varying vec4 ourColor;" +  // 넘겨받은 보간 값
                    "void main() {" +
                    "    gl_FragColor = ourColor;" +
                    "}";

    private final FloatBuffer vertexBuffer;
    private final ShortBuffer drawListBuffer;
    private final FloatBuffer colorBuffer;

    private final int mProgram;

    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;

    // 삼각형의 좌표
    static float coords[] = {
            0f, 0f, 3.464101f,           // A(0, 0, 2*sqrt(3))
            0f, 3.464101f, -1.732050f,   // B(0, 2*sqrt(3), -sqrt(3))
            -3f, -1.732050f, -1.732050f, // C(-3, -sqrt(3), -sqrt(3))
            3f, -1.732050f, -1.732050f   // D(3, -sqrt(3), -sqrt(3))
    };


    // 삼각형 그리는 순서
    final short[] drawOrder = { // order to draw vertices
            0, 1, 2,  // ABC
            0, 2, 3,  // ACD
            0, 3, 1,  // ADB
            1, 3, 2   // BDC
    };

    private final int vertexCount = coords.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    // 색입히기
    float colors[] = {
            1f, 1f, 1f, 1f,
            1f, 0f, 0f, 1f,
            0f, 1f, 0f, 1f,
            0f, 0f, 1f, 1f
    };

    static final int COLOR_PER_VERTEX = 4; // rgba
    private final int colorCount = coords.length / COORDS_PER_VERTEX;
    private final int colorStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    public Triangle() {
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                coords.length * 4);
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());
        // create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        vertexBuffer.put(coords);
        // set the buffer to read the first coordinate
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
                drawOrder.length * 2); // short은 2바이트라서 2를 곱한다.
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // initialize byte buffer for the draw list
        colorBuffer = ByteBuffer.allocateDirect(colors.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        colorBuffer.put(colors);
        colorBuffer.position(0);

        // prepare shaders and OpenGL program
        int vertexShader = MyGLRenderer.loadShader(
                GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = MyGLRenderer.loadShader(
                GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables
    }

    public void draw(float[] mvpMatrix) {
        Matrix.scaleM(mvpMatrix, 0, 0.15f, 0.15f, 0.15f);

        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);
        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);
        // Enable a handle to the triangle color for vertices
        GLES20.glEnableVertexAttribArray(mColorHandle);
        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetAttribLocation(mProgram, "aColor");
        GLES20.glVertexAttribPointer(
                mColorHandle, COLOR_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                4 * COLOR_PER_VERTEX, colorBuffer);
        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        MyGLRenderer.checkGlError("glGetUniformLocation");
        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        MyGLRenderer.checkGlError("glUniformMatrix4fv");
        // Draw the triangle
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mColorHandle);
    }
}