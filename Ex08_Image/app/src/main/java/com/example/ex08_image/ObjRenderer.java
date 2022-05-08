package com.example.ex08_image;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;

public class ObjRenderer {

    private static final String TAG = ObjRenderer.class.getSimpleName();

    private final String vertexShaderString =
            "uniform mat4 uMvMatrix;\n" +
                    "uniform mat4 uMvpMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec3 aNormal;\n" +
                    "attribute vec2 aTexCoord;\n" +
                    "varying vec3 vPosition;\n" +
                    "varying vec3 vNormal;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main() {\n" +
                    "   vPosition = (uMvMatrix * aPosition).xyz;\n" +
                    "   vNormal = normalize((uMvMatrix * vec4(aNormal, 0.0)).xyz);\n" +
                    "   vTexCoord = aTexCoord;\n" +
                    "   gl_Position = uMvpMatrix * vec4(aPosition.xyz, 1.0);\n" +
                    "}";

    private final String fragmentShaderString =
            "precision mediump float;\n" +
                    "uniform sampler2D uTexture;\n" +
                    "uniform vec4 uLighting;\n" +   // 빛 처리
                    "uniform vec4 uMaterial;\n" +
                    "varying vec3 vPosition;\n" +
                    "varying vec3 vNormal;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main() {\n" +
                    "    const float kGamma = 0.4545454;\n" +
                    "    const float kInverseGamma = 2.2;\n" +
                    "    vec3 viewLightDirection = uLighting.xyz;\n" +
                    "    float lightIntensity = uLighting.w;\n" +       // 빛처리
                    "    float materialAmbient = uMaterial.x;\n" +
                    "    float materialDiffuse = uMaterial.y;\n" +
                    "    float materialSpecular = uMaterial.z;\n" +
                    "    float materialSpecularPower = uMaterial.w;\n" +
                    "    vec3 viewFragmentDirection = normalize(vPosition);\n" +
                    "    vec3 viewNormal = normalize(vNormal);\n" +
                    "    vec4 objectColor = texture2D(uTexture, vec2(vTexCoord.x, 1.0 - vTexCoord.y));\n" +
                    "    objectColor.rgb = pow(objectColor.rgb, vec3(kInverseGamma));\n" +
                    "    float ambient = materialAmbient;\n" +
                    "    float diffuse = lightIntensity * materialDiffuse * 0.5 * (dot(viewNormal, viewLightDirection) + 1.0);\n" +
                    "    vec3 reflectedLightDirection = reflect(viewLightDirection, viewNormal);\n" +
                    "    float specularStrength = max(0.0, dot(viewFragmentDirection, reflectedLightDirection));\n" +
                    "    float specular = lightIntensity * materialSpecular * pow(specularStrength, materialSpecularPower);\n" +
                    "    gl_FragColor.a = objectColor.a;\n" +
                    "    gl_FragColor.rgb = pow(objectColor.rgb * (ambient + diffuse) + specular, vec3(kGamma));\n" +
                    "}";

    private Context mContext;
    private String mObjName;
    private String mTextureName;

    private Obj mObj;

    private int mProgram;
    private int[] mTextures;
    private int[] mVbos;
    private int mVerticesBaseAddress;
    private int mTexCoordsBaseAddress;
    private int mNormalsBaseAddress;
    private int mIndicesCount;

    private float[] mModelMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mProjMatrix = new float[16];

    // 빛의 세기 변수
    private float mLightIntensity;

    float[] mColorCorrection ;

    public ObjRenderer(Context context, String objName, String textureName) {
        mContext = context;
        mObjName = objName;
        mTextureName = textureName;
    }

    public void init() {
        try {
            InputStream is = mContext.getAssets().open(mObjName);
            Bitmap bmp = BitmapFactory.decodeStream(mContext.getAssets().open(mTextureName));
            mObj = ObjReader.read(is);
            mObj = ObjUtils.convertToRenderable(mObj);

            mTextures = new int[1];
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glGenTextures(1, mTextures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

            bmp.recycle();
        }
        catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        if (mObj == null || mTextures[0] == -1) {
            Log.e(TAG, "Failed to init obj - " + mObjName + ", " + mTextureName);
        }

        ShortBuffer indices = ObjData.convertToShortBuffer(ObjData.getFaceVertexIndices(mObj, 3));
        FloatBuffer vertices = ObjData.getVertices(mObj);
        FloatBuffer texCoords = ObjData.getTexCoords(mObj, 2);
        FloatBuffer normals = ObjData.getNormals(mObj);

        mVbos = new int[2];
        GLES20.glGenBuffers(2, mVbos, 0);

        mVerticesBaseAddress = 0;
        mTexCoordsBaseAddress = mVerticesBaseAddress + 4 * vertices.limit();
        mNormalsBaseAddress = mTexCoordsBaseAddress + 4 * texCoords.limit();
        final int totalBytes = mNormalsBaseAddress + 4 * normals.limit();

        mIndicesCount = indices.limit();

        // vertexBufferId
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbos[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, totalBytes, null, GLES20.GL_STATIC_DRAW);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, mVerticesBaseAddress, 4 * vertices.limit(), vertices);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, mTexCoordsBaseAddress, 4 * texCoords.limit(), texCoords);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, mNormalsBaseAddress, 4 * normals.limit(), normals);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        // indexBufferId
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mVbos[1]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, 2 * mIndicesCount, indices, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        int vShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vShader, vertexShaderString);
        GLES20.glCompileShader(vShader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(vShader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile vertex shader.");
            GLES20.glDeleteShader(vShader);
        }

        int fShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fShader, fragmentShaderString);
        GLES20.glCompileShader(fShader);
        GLES20.glGetShaderiv(fShader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile fragment shader.");
            GLES20.glDeleteShader(fShader);
        }

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vShader);
        GLES20.glAttachShader(mProgram, fShader);
        GLES20.glLinkProgram(mProgram);
        int[] linked = new int[1];
        GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e(TAG, "Could not link program.");
        }
    }

    public void draw() {
        float[] mvMatrix = new float[16];
        float[] mvpMatrix = new float[16];
        Matrix.multiplyMM(mvMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, mProjMatrix, 0, mvMatrix, 0);

        GLES20.glUseProgram(mProgram);

        int mv = GLES20.glGetUniformLocation(mProgram, "uMvMatrix");
        int mvp = GLES20.glGetUniformLocation(mProgram, "uMvpMatrix");

        int position = GLES20.glGetAttribLocation(mProgram, "aPosition");
        int normal = GLES20.glGetAttribLocation(mProgram, "aNormal");
        int texCoord = GLES20.glGetAttribLocation(mProgram, "aTexCoord");

        int texture = GLES20.glGetUniformLocation(mProgram, "uTexture");

        int lighting = GLES20.glGetUniformLocation(mProgram, "uLighting");
        int material = GLES20.glGetUniformLocation(mProgram, "uMaterial");

        int colorCorrection = GLES20.glGetUniformLocation(mProgram, "uColorCorrection");


        float[] viewLightDirection = new float[4];
        float[] lightDirection = new float[] {0.250f, 0.866f, 0.433f, 0.0f};
        Matrix.multiplyMV(viewLightDirection, 0, mvMatrix, 0, lightDirection, 0);
        normalize(viewLightDirection);
        // 빛의 세기를 받아 재질을 표현한다.
        GLES20.glUniform4f(lighting, viewLightDirection[0], viewLightDirection[1], viewLightDirection[2], mLightIntensity);
//        if(mColorCorrection != null) {
//            GLES20.glUniform4f(colorCorrection, mColorCorrection[0], mColorCorrection[1], mColorCorrection[2], mColorCorrection[3]);
//        }

        float ambient = 0.3f;
        float diffuse = 1.0f;
        float specular = 1.0f;
        float specularPower = 6.0f;
        GLES20.glUniform4f(material, ambient, diffuse, specular, specularPower);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLES20.glUniform1i(texture, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbos[0]);
        GLES20.glVertexAttribPointer(position, 3, GLES20.GL_FLOAT, false, 0, mVerticesBaseAddress);
        GLES20.glVertexAttribPointer(normal, 3, GLES20.GL_FLOAT, false, 0, mNormalsBaseAddress);
        GLES20.glVertexAttribPointer(texCoord, 2, GLES20.GL_FLOAT, false, 0, mTexCoordsBaseAddress);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLES20.glUniformMatrix4fv(mv, 1, false, mvMatrix, 0);
        GLES20.glUniformMatrix4fv(mvp, 1, false, mvpMatrix, 0);

        GLES20.glEnableVertexAttribArray(position);
        GLES20.glEnableVertexAttribArray(normal);
        GLES20.glEnableVertexAttribArray(texCoord);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mVbos[1]);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mIndicesCount, GLES20.GL_UNSIGNED_SHORT, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        GLES20.glDisableVertexAttribArray(position);
        GLES20.glDisableVertexAttribArray(normal);
        GLES20.glDisableVertexAttribArray(texCoord);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public void setModelMatrix(float[] modelMatrix) {
        System.arraycopy(modelMatrix, 0, mModelMatrix, 0, 16);
    }

    public void setProjectionMatrix(float[] projMatrix) {
        System.arraycopy(projMatrix, 0, mProjMatrix, 0, 16);
    }

    public void setViewMatrix(float[] viewMatrix) {
        System.arraycopy(viewMatrix, 0, mViewMatrix, 0, 16);
    }

    // 빛 감도를 처리하는 메서드
    public void setLightIntensity(float lightIntensity) {
        mLightIntensity = lightIntensity;
    }

    private void normalize(float[] v) {
        double norm = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        v[0] /= norm;
        v[1] /= norm;
        v[2] /= norm;
    }
    void setColorCorrection(float[] colorCorrection){
        mColorCorrection = new float[]{colorCorrection[0], colorCorrection[1], colorCorrection[2], colorCorrection[3]};

//        mColorCorrection[0] = colorCorrection[0];
//        mColorCorrection[1] = colorCorrection[1];
//        mColorCorrection[2] = colorCorrection[2];
//        mColorCorrection[3] = colorCorrection[3];

    }
}
