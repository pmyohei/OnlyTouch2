package com.example.onlyTouch.opengl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;

/*
 * ビューコンテナ：パーティクルレンダリング用
 */
@SuppressLint("ViewConstructor")
public class ParticleGLSurfaceView extends GLSurfaceView {

    // パーティクル物理世界のレンダリング
    ParticleWorldRenderer mRenderer;

    /*
     * コンストラクタ
     */
    public ParticleGLSurfaceView(Context context) {
        super(context);

        // Rendererオブジェクトに描画を委譲
        mRenderer = new ParticleWorldRenderer(this);
        setRenderer(mRenderer);

        // リスナー
        setOnTouchListener(mRenderer);
    }

    /*
     * Renderer取得
     */
    public ParticleWorldRenderer getRenderer() {
        return mRenderer;
    }
}
