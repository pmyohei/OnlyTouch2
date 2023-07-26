package com.example.onlyTouch;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
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

        final View cl_message = findViewById(R.id.cl_message);
        cl_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), ParticleWorldActivity.class);
                startActivity(intent);
            }
        });
    }
}

