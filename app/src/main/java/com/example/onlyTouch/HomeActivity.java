package com.example.onlyTouch;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

/*
 * Home画面
 */
public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // タッチ画面遷移リスナー
        setTouch();
    }

    /*
     * タッチ画面遷移リスナー
     */
    private void setTouch(){

        final TextView tx_splash = (TextView)findViewById(R.id.tx_touch);
        tx_splash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), CreateDrawFluidActivity.class);
                startActivity(intent);
            }
        });
    }
}

