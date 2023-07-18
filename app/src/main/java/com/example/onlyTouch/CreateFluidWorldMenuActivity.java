package com.example.onlyTouch;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;

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

        app = (MyApplication) getApplication();
        Bitmap bitmap = app.getObj();
        MenuActivity.PictureButton select = app.getSelect();

        ArrayList<Vec2> touchList = null;
        if (select == MenuActivity.PictureButton.CreateDraw) {
            touchList = app.getTouchList();
        }

        //-------------------------------
        // パーティクルレンダリングビュー
        //-------------------------------
        // 生成
        glView = new FluidGLSurfaceView(this, bitmap, select, touchList);
        // レイアウトに追加
        setContentView(R.layout.activity_fluid_design);
        ViewGroup root = findViewById(R.id.gl_view_root);
        root.addView(glView);

        //---------------
        // メニューの設定
        //---------------
        setMenu();
    }


    /*
     * メニューの設定
     */
    private void setMenu() {
        // メニュー展開リスナーの設定
        findViewById(R.id.ib_menu_expand).setOnClickListener(new CollapsedMenuListerner());

        //-------------------
        // 各種メニューリスナー
        //-------------------
        // 重力変更
        findViewById(R.id.ib_gravity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FluidWorldRenderer render = glView.getRenderer();
                render.switchGravity(true);
            }
        });

        // パーティクルを中心に再生成
        findViewById(R.id.ib_center).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FluidWorldRenderer render = glView.getRenderer();
                render.regenerationAtCenter();
            }
        });

        // 柔らかさの変更
        findViewById(R.id.ib_soft).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 現在設定中の柔らかを取得
                FluidWorldRenderer render = glView.getRenderer();
                int currentValue = render.getSoftness();

                // ダイアログを開く
                showChangeSoftDialog( currentValue );
            }
        });

        // 銃弾OnOff
        findViewById(R.id.ib_bullet).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FluidWorldRenderer render = glView.getRenderer();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    render.switchBullet();
                }
            }
        });

        // homeに戻る
        findViewById(R.id.ib_home).setOnClickListener(new View.OnClickListener() {
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
     * 柔らかさ変更ダイアログを開く
     */
    private void showChangeSoftDialog( int currentSoftness ) {

        ChangeSoftDialog dialog = ChangeSoftDialog.newInstance();
        dialog.selectedSoftness( currentSoftness );
        dialog.setOnPositiveClickListener(new ChangeSoftDialog.PositiveClickListener() {
                @Override
                public void onPositiveClick(int softness) {
                    // ユーザーの選択した柔らかさをパーティクルに反映
                    FluidWorldRenderer render = glView.getRenderer();
                    render.setSoftness(softness);
                }
            }
        );
        dialog.show( getFragmentManager(), "softness" );
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
            ViewGroup menu = findViewById(R.id.cl_menu_expanded);
            ViewGroup explanation = findViewById(R.id.root_explanation);

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
        // 展開後メニュー
        ViewGroup menu_top = findViewById(R.id.cl_menu_expanded);
        menu_top.getGlobalVisibleRect(menu_corners);

        findViewById(R.id.container).getGlobalVisibleRect(corners, globalOffset);
        menu_corners.offset(-globalOffset.x, -globalOffset.y);

        FluidWorldRenderer fluidRenderer = glView.getRenderer();
        fluidRenderer.setExpandedMenuRect(menu_corners.top, menu_corners.left, menu_corners.right, menu_corners.bottom);

        // 展開前メニュー
        ViewGroup menu_collapsed = findViewById(R.id.cl_menu_collapsed);
        menu_collapsed.getGlobalVisibleRect(menu_corners);
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
