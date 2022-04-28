package com.example.test;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public class MyGLSurfaceView extends GLSurfaceView {

    private final MyGLRenderer mRenderer;
    public MyGLSurfaceView(Context context) {
        super(context);

        setEGLContextClientVersion(2);

        mRenderer = new MyGLRenderer();
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
    private float mPreviousX;
    private float mPreviousY;

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float dx = mPreviousX - x;
                float dy = mPreviousY - y;

                mRenderer.rotate(dx,dy);
                requestRender();
        }
        mPreviousX = x;
        mPreviousY = y;
        return true;
    }
}
