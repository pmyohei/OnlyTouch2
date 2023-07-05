package com.example.onlyTouch;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.fpl.liquidfun.Vec2;

import java.util.ArrayList;

/*
 * 流体描画画面
 */
public class CreateDrawFluidActivity extends AppCompatActivity {

    PaintView mPaintView;
    private MyApplication app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_draw);

        app = (MyApplication)getApplication();

        //ペイント画面
        mPaintView = new PaintView(this);
        LinearLayout paint = findViewById(R.id.paint);
        paint.addView( mPaintView );

        //確定ボタン
        Button btn = findViewById(R.id.end);
        btn.setOnClickListener( new View.OnClickListener(){
            @Override
            public void onClick(View view) {

                //ペイント情報の成形を行わせる
                mPaintView.reqMoldingPaintInfo();

                //描画したBitmaoを取得
                Bitmap bmp = mPaintView.getBitmap();
                if(bmp == null) {
                    return;
                }
                //描画した際のタッチ位置を取得
                ArrayList<Vec2> touchList = mPaintView.getTouchList();
                if(touchList == null) {
                    return;
                }

                //画像に割り当て
                //ImageView image = findViewById(R.id.image);
                //image.setImageBitmap(bmp);

                //共通クラスにBitmapを格納
                //(アクティビティ間の画像はサイズが大きいとエラーで落ちるため、共通クラスを介する)
                app.setObj(bmp);
                app.setSelect(MenuActivity.PictureButton.CreateDraw);
                app.setTouchList(touchList);

                Intent intent = new Intent(view.getContext(), CreateFluidWorldMenuActivity.class);
                startActivity(intent);
            }
        });

    }
}