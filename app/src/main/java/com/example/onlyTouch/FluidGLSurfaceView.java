package com.example.onlyTouch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;

import com.google.fpl.liquidfun.Vec2;

import java.util.ArrayList;

/*
 * 流体世界用のGLSurfaceView
 */
@SuppressLint("ViewConstructor")
public class FluidGLSurfaceView extends GLSurfaceView {

    FluidWorldRenderer mRenderer;

    public FluidGLSurfaceView(Context context, Bitmap bmp, MenuActivity.PictureButton select, ArrayList<Vec2> touchList) {
        super(context);

        // Rendererオブジェクトに描画を委譲
        mRenderer = new FluidWorldRenderer(this, bmp, select, touchList);
        setRenderer(mRenderer);

        setOnTouchListener(mRenderer);
    }

    public FluidWorldRenderer getRenderer() {
        return mRenderer;
    }
}
