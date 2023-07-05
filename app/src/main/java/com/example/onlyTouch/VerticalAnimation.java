package com.example.onlyTouch;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;

public class VerticalAnimation extends Animation {

    View view;
    final int addHeight;
    int startHeight;

    // 対象ビュー
    // アニメーションの範囲　　　( = 表示非表示の切り替えアニメーションなら、対象のビューの高さ)
    // アニメーションを開始するときのビューの高さ =
    public VerticalAnimation(View view, int addHeight, int startHeight, long duration) {
        this.view = view;
        this.addHeight = addHeight;
        this.startHeight = startHeight;

        setDuration(duration);
        setInterpolator(new DecelerateInterpolator());
    }

    // 描画のタイミングで呼ばれる。
    // interpolatedTime：アニメーションの進行値(0.0～1.0fまでの値で経過時間が通知される)
    //
    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        int newHeight = (int) (startHeight + addHeight * interpolatedTime);
        view.getLayoutParams().height = newHeight;
        view.requestLayout();
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
    }

    @Override
    public boolean willChangeBounds() {
        return true;
    }
}
