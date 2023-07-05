package com.example.onlyTouch;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

/*
 * スプラッシュイメージの表示画面
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //スプラッシュのイメージにアニメーションを設定する
        final ImageView imageView = (ImageView)findViewById(R.id.splash_image);

        //アニメーションを順番で設定
        AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.splash);
        set.setTarget(imageView);
        set.start();

        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                //アニメーション終了時に画面遷移
                startActivity(new Intent(imageView.getContext(), MenuActivity.class));
            }
        });
    }
}
