package com.example.onlyTouch;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;

import com.google.fpl.liquidfun.Vec2;

import java.util.ArrayList;

/*
 * 流体画面
 *   レイヤー２：操作メニュー用画面
 */
public class CreateFluidWorldMenuActivity extends AppCompatActivity {

    private FluidGLSurfaceView glView;
    private MyApplication app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = (MyApplication)getApplication();
        Bitmap bitmap                     = app.getObj();
        MenuActivity.PictureButton select = app.getSelect();

        ArrayList<Vec2> touchList = null;
        if(select == MenuActivity.PictureButton.CreateDraw){
            touchList = app.getTouchList();
        }

        // 流体レンダリングビューを生成
        glView = new FluidGLSurfaceView(this, bitmap, select,  touchList );
        // レンダリングビューをレイアウトに追加
        setContentView(R.layout.activity_fluid_design);
        LinearLayout root = findViewById(R.id.gl_view_root);
        root.addView(glView);

        // 画面下部のメニュー
        findViewById(R.id.bottom_menu_init).setOnClickListener( new CollapsedMenuListerner() );

        // 別画像の選択
        findViewById(R.id.other_picture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("Return", "Gallery");
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        // 流体再生成
        findViewById(R.id.regeneration).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FluidWorldRenderer render = glView.getRenderer();
                render.reqRegeneration();
            }
        });

        // 銃弾
        findViewById(R.id.pin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FluidWorldRenderer render = glView.getRenderer();
                render.reqCannonCtrl(true);
            }
        });

        // 重力OnOff切り替え
        findViewById(R.id.gravity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FluidWorldRenderer render = glView.getRenderer();
                render.switchGravity(true);
            }
        });

        // メニュー画面に戻る
        findViewById(R.id.return_menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("Return", "Menu");
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    /*
     * メニュー(折りたたみ時)のリスナー
     */
    private class CollapsedMenuListerner implements View.OnClickListener {

        // リスナー受付(初期値は受付OK)
        public boolean mIsEnable = true;

        @Override
        public void onClick(View view) {

            //--------------
            // リスナー受付制御
            //--------------
            // リスナー受付中でなければ、なにもしない
            if (!mIsEnable) {
                return;
            }

            // リスナーの処理を終えるまでは、受付不可にする
            mIsEnable = false;

            //--------------
            // menu制御
            //--------------
            LinearLayout menu = findViewById(R.id.bottom_menu_contents);
            LinearLayout explanation = findViewById(R.id.root_explanation);

            if (menu.getVisibility() != View.VISIBLE) {
                // 開く
                expandMenu( menu, explanation );
            } else {
                // 折りたたむ
                collapseMenu( menu, explanation );
            }
        }

        /*
         * メニューを開く
         */
        private void expandMenu( final View menu, final View explanation ){

            //---------------------
            // メニュー本体制御
            //---------------------
            // Viewをアニメーション
            slideUp(menu);
            // menu背景物体の移動
            glView.getRenderer().moveMenuBody(FluidWorldRenderer.MENU_MOVE_STATE_UP);

            //---------------------
            // 各種メニュー説明文
            //---------------------
            // 説明文のアニメーション表示
            explanation.setVisibility(View.VISIBLE);
            Animation animation_in = AnimationUtils.loadAnimation(explanation.getContext(), R.anim.slide_right);
            explanation.startAnimation(animation_in);
        }

        /*
         * メニューを折りたたむ
         */
        private void collapseMenu( final View menu, final View explanation ){

            //---------------------
            // メニュー本体制御
            //---------------------
            // Viewをアニメーション
            slideDown(menu);
            // menu背景物体の移動
            glView.getRenderer().moveMenuBody(FluidWorldRenderer.MENU_MOVE_STATE_DOWN);

            //---------------------
            // 各種メニュー説明文
            //---------------------
            // 説明文のアニメーション非表示
            explanation.setVisibility(View.INVISIBLE);
            Animation animation_in = AnimationUtils.loadAnimation(explanation.getContext(), R.anim.slide_left);
            explanation.startAnimation(animation_in);
        }

        /*
         * アニメーション：スライドアップ
         */
        private void slideUp(final View v ){
            v.setVisibility(View.VISIBLE);

            //------------------
            // animate生成
            //------------------
            int duration = getResources().getInteger(R.integer.menu_up_anim_duration);

            // animate生成
            TranslateAnimation animate = new TranslateAnimation(
                    0,
                    0,
                    v.getHeight(),
                    0);
            animate.setDuration(duration);
            animate.setFillAfter(true);
            animate.setInterpolator(new LinearInterpolator());
            animate.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    v.setAlpha(1);
                }
                @Override
                public void onAnimationEnd(final Animation animation) {
                    // menuアニメーション停止通知
                    glView.getRenderer().moveMenuBody(FluidWorldRenderer.MENU_MOVE_STATE_STOP);

                    // アニメーションが終了すれば、受付可能にする
                    mIsEnable = true;
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            v.startAnimation(animate);
        }

        /*
         * アニメーション：スライドダウン
         */
        private void slideDown(final View v ){
            v.setVisibility(View.INVISIBLE);

            //------------------
            // animate生成
            //------------------
            int duration = getResources().getInteger(R.integer.menu_down_anim_duration);

            // animate生成
            TranslateAnimation animate = new TranslateAnimation(
                    0,
                    0,
                    0,
                    v.getHeight());
            animate.setDuration(duration);
            animate.setFillAfter(true);
            animate.setInterpolator(new LinearInterpolator());
            animate.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    /* do nothing */
                }
                @Override
                public void onAnimationEnd(final Animation animation) {
                    // 初期menuと本体が重なった位置にくる。後ろに色があるのが見えてしまうため、透明にしとく
                    v.setAlpha(0);

                    // menuアニメーション停止通知
                    glView.getRenderer().moveMenuBody(FluidWorldRenderer.MENU_MOVE_STATE_STOP);

                    // アニメーションが終了すれば、受付可能にする
                    mIsEnable = true;
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                    /* do nothing */
                }
            });
            v.startAnimation(animate);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        Rect menu_corners = new Rect();
        Rect corners = new Rect();
        Point globalOffset = new Point();

        /* メニューの位置・サイズを渡す( @@四隅の位置取得の方法はその内調査(主にoffset)@@ ) */
        // メニュー上部
        LinearLayout menu_top = findViewById(R.id.bottom_menu_contents);
        menu_top.getGlobalVisibleRect(menu_corners);

        findViewById(R.id.container).getGlobalVisibleRect(corners, globalOffset);
        menu_corners.offset(-globalOffset.x, -globalOffset.y);

        FluidWorldRenderer fluidRenderer = glView.getRenderer();
        fluidRenderer.setExpandedMenuRect(menu_corners.top, menu_corners.left, menu_corners.right, menu_corners.bottom);

        // メニュー下部
        LinearLayout menu_bottom = findViewById(R.id.bottom_menu_init);
        menu_bottom.getGlobalVisibleRect(menu_corners);
        menu_corners.offset(-globalOffset.x, -globalOffset.y);

        fluidRenderer.setCollapsedMenuRect(menu_corners.top, menu_corners.left, menu_corners.right, menu_corners.bottom);

        // メニューのRect情報設定完了
        fluidRenderer.finishSetMenuRect();

        // 横幅を取得後、メニュー本体は隠す
        menu_top.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onResume(){
        super.onResume();
        glView.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
        glView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        app.clearObj();
    }

}
