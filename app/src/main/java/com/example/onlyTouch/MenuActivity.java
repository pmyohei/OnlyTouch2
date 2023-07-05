package com.example.onlyTouch;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

/*
 * メニュー画面
 */
public class MenuActivity extends AppCompatActivity {

    private static final int RESULT_PICK_IMAGEFILE = 1000;
    private Uri uri;

    private LinearLayout create;
    private HorizontalScrollView create_select;
    private int create_select_height;

    private LinearLayout picture;
    private LinearLayout picture_select;
    private int picture_select_height;

    private boolean ac_create = false;

    enum PictureButton{
        Soft,
        Hard,
        VeryHard,
        Break,
        CreateDraw
    }

    private PictureButton pictureButton = PictureButton.Soft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Createメニュー
        create = findViewById(R.id.create);
        create_select = findViewById(R.id.create_select);

        //紹介対応
        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), CreateDrawFluidActivity.class);
                startActivity(intent);
            }
        });
        //-----------------

        LinearLayout sub_create = findViewById(R.id.mochi);
        sub_create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), CreateSelectionActivity.class);
                startActivity(intent);
            }
        });

        sub_create = findViewById(R.id.create_touch);
        sub_create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), CreateFluidTouchWorldActivity.class);
                startActivity(intent);
            }
        });

        sub_create = findViewById(R.id.create_draw);
        sub_create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), CreateDrawFluidActivity.class);
                startActivity(intent);
            }
        });

        //Pictureメニュー
        picture = findViewById(R.id.picture);
        picture_select = findViewById(R.id.picture_select);

        findViewById(R.id.soft).setOnClickListener(new pictureButtonListener());
        findViewById(R.id.hard).setOnClickListener(new pictureButtonListener());
        findViewById(R.id.veryhard).setOnClickListener(new pictureButtonListener());
        findViewById(R.id.likebreak).setOnClickListener(new pictureButtonListener());

        //Information
        LinearLayout information = findViewById(R.id.Information);
        information.setOnClickListener( new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                //Information画面へ遷移
                Intent intent = new Intent(view.getContext(), AppInformationActivity.class);
                startActivity(intent);
            }
        });
    }

    //リスナー：Picture付属ボタン
    private class pictureButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {

            //押下ボタンを保持
            switch(view.getId()){
                case R.id.soft:
                    pictureButton = PictureButton.Soft;
                    break;
                case R.id.hard:
                    pictureButton = PictureButton.Hard;
                    break;
                case R.id.veryhard:
                    pictureButton = PictureButton.VeryHard;
                    break;
                case R.id.likebreak:
                    pictureButton = PictureButton.Break;
                    break;
            }

            //Pictureアクティビティへ遷移
            Intent intent = new Intent(view.getContext(), SelectPictureActivity.class);
            intent.putExtra("PictureButton", pictureButton);
            startActivity(intent);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        //本Activityの初回起動時のみ
        if(!ac_create){
            //フラグ更新(非初回)
            ac_create = true;

            //アニメーションの設定
            //setAnimationCreate();
            //setAnimationPicture();

            //アニメーションの設定
            //setAnimation((View)findViewById(R.id.create), (View)findViewById(R.id.create_select));
            setAnimation((View)findViewById(R.id.picture), (View)findViewById(R.id.picture_select));
        }
    }

    ////////////////////////////////////
    //  アニメーションの設定
    //  para1：アニメーション開始のリスナーとなるView
    //  para2：アニメーション対象のView
    //
    private void setAnimation(View listernerView, final View animationView){
        // ExpandするViewの元のサイズを保持する
        int height = animationView.getHeight();

        // ビューを開くアニメーションを生成
        final VerticalAnimation expandAnim = new VerticalAnimation(animationView, height, 0, 300);
        // ビューを閉じるアニメーションを生成
        final VerticalAnimation collapseAnim = new VerticalAnimation(animationView, -animationView.getHeight(), animationView.getHeight(), 300);

        //初期表示として、閉じた状態にする(durationを短くして、見えないようにする)
        VerticalAnimation collapseAnimIni = new VerticalAnimation(animationView, -animationView.getHeight(), animationView.getHeight(), 1);
        animationView.startAnimation(collapseAnimIni);

        listernerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animationView.clearAnimation();
                if(animationView.getHeight() > 0) {
                    animationView.startAnimation(collapseAnim);
                } else{
                    animationView.startAnimation(expandAnim);
                }
            }
        });
    }


}

