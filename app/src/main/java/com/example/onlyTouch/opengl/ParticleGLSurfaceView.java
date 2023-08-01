package com.example.onlyTouch.opengl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;

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

        // Renderer生成
        mRenderer = createRenderer();
        // Rendererオブジェクトに描画を委譲
        setRenderer(mRenderer);
    }

    /*
     * Renderer生成
     */
    private ParticleWorldRenderer createRenderer() {

        // Renderer生成
        ParticleWorldRenderer renderer = new ParticleWorldRenderer(this);
        // タッチリスナーを付与
        setOnTouchListener(renderer);

        return renderer;
    }

    /*
     * Renderer取得
     */
    public ParticleWorldRenderer getRenderer() {
        return mRenderer;
    }

    @Override
    public void onResume() {
        // do nothing（スリープからの復帰後、そのまま処理を再開させるため）
    }
    @Override
    public void onPause() {
        // do nothing（スリープからの復帰後、そのまま処理を再開させるため）
    }
}
