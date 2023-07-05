package com.example.onlyTouch;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

/*
 * （現在仕様としてなし）
 */
public class CreateSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_selection);

        Animation up = AnimationUtils.loadAnimation(this, R.anim.create_frame_up);
        LinearLayout frame = findViewById(R.id.contentsFrame);
        frame.setVisibility(View.VISIBLE);
        frame.startAnimation(up);

        

    }
}
