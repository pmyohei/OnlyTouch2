package com.example.onlyTouch;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

/*
 * スプラッシュ画面
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // スプラッシュアニメーション開始
        startSplashAnimation();
    }

    /*
     * スプラッシュアニメーション開始
     */
    private void startSplashAnimation(){

        // スプラッシュのイメージにアニメーションを設定する
        final TextView tx_splash = (TextView)findViewById(R.id.tx_splash);

        // アニメーションを順番で設定
        AnimatorSet animator = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.splash);
        animator.setTarget(tx_splash);

        // アニメーション終了時処理
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                //アニメーション終了時に画面遷移
                startActivity(new Intent(tx_splash.getContext(), HomeActivity.class));
                overridePendingTransition( R.anim.page_slide_up, R.anim.page_slide_down);
            }
        });

        // start
        animator.start();
    }
}
