package com.example.ex_02_opengl;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class MyGLSurfaceView extends GLSurfaceView {


    public MyGLSurfaceView(Context context) {
        super(context);

        // OpenGL ES 2.0 context를 생성합니다.
        // 버전

        setEGLContextClientVersion(2);

        // GLSurfaceView에 그래픽 객체를 그리는 처리를 하는 renderer를 설정합니다.
        setRenderer(new MyGLRenderer(context));

        //Surface가 생성될때와 GLSurfaceView클래스의 requestRender 메소드가 호출될때에만
        //화면을 다시 그리게 됩니다.
        // 화면이 바뀔때
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
}
