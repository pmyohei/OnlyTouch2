package com.example.onlyTouch;

import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;

import com.example.onlyTouch.opengl.ParticleGLSurfaceView;
import com.example.onlyTouch.opengl.ParticleWorldRenderer;

/*
 * 流体画面
 *   レイヤー２：操作メニュー用画面
 */
public class ParticleWorldActivity extends AppCompatActivity {

    private ParticleGLSurfaceView mGLSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //-------------------------------
        // パーティクルレンダリングビュー
        //-------------------------------
        setOpenGL();

        //---------------
        // メニューの設定
        //---------------
        setMenu();
    }

    /*
     * OpenGL ESの設定
     */
    private void setOpenGL() {
        // ビューコンテナ生成
        mGLSurfaceView = new ParticleGLSurfaceView(this);

        // レイアウトに追加
        setContentView(R.layout.activity_particle_world);
        ViewGroup root = findViewById(R.id.gl_view_root);
        root.addView(mGLSurfaceView);
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
                // 現在設定中の重力を取得
                ParticleWorldRenderer render = mGLSurfaceView.getRenderer();
                int currentValue = render.getGravity();

                // ダイアログを開く
                showChangeGravityDialog( currentValue );
            }
        });

        // パーティクルを中心に再生成
        findViewById(R.id.ib_center).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParticleWorldRenderer render = mGLSurfaceView.getRenderer();
                render.regenerationAtCenter();
            }
        });

        // 柔らかさの変更
        findViewById(R.id.ib_soft).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 現在設定中の柔らかを取得
                ParticleWorldRenderer render = mGLSurfaceView.getRenderer();
                int currentValue = render.getSoftness();

                // ダイアログを開く
                showChangeSoftDialog( currentValue );
            }
        });

        // 銃弾OnOff
        findViewById(R.id.ib_bullet).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParticleWorldRenderer render = mGLSurfaceView.getRenderer();
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
     * 重力変更ダイアログを開く
     */
    private void showChangeGravityDialog( int gravity ) {

        ChangeGravityDialog dialog = ChangeGravityDialog.newInstance();
        dialog.selectedGravity( gravity );
        dialog.setOnPositiveClickListener(new ChangeGravityDialog.PositiveClickListener() {
                @Override
                public void onPositiveClick(int gravity) {
                    // ユーザーの選択した重力を反映
                    ParticleWorldRenderer render = mGLSurfaceView.getRenderer();
                    render.setGravity(gravity);
                }
            }
        );
        dialog.show( getFragmentManager(), "gravity" );
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
                    ParticleWorldRenderer render = mGLSurfaceView.getRenderer();
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
        // （アニメーション中は受付不可にする）
        private boolean mIsControlReception = true;

        // menu折りたたみ
        private boolean mIsCollapsed = true;

        @Override
        public void onClick(View view) {
            //--------------
            // リスナー受付制御
            //--------------
            // リスナー受付中でなければ、なにもしない
            if (!mIsControlReception) {
                return;
            }
            // アニメーション終了するまでは、受付不可
            mIsControlReception = false;

            //--------------
            // menu制御
            //--------------
            ViewGroup menuExpanded = findViewById(R.id.cl_menu_expanded);
            ViewGroup explanation = findViewById(R.id.root_explanation);

            if ( mIsCollapsed ) {
                // 開く
                expandMenu( menuExpanded, explanation );
                mIsCollapsed = false;
            } else {
                // 折りたたむ
                collapseMenu( menuExpanded, explanation );
                mIsCollapsed = true;
            }
        }

        /*
         * メニューを開く
         */
        private void expandMenu( final View menu, final View explanation ){

            //---------------------
            // メニュー本体制御
            //---------------------
            // メニューをスライドアップ
            slideUp(menu);

            //---------------------
            // 各種メニュー説明文
            //---------------------
            // 説明文のアニメーション表示
            explanation.setVisibility(View.VISIBLE);
            Animation animation_in = AnimationUtils.loadAnimation(explanation.getContext(), R.anim.menu_exp_show);
            explanation.startAnimation(animation_in);
        }

        /*
         * メニューを折りたたむ
         */
        private void collapseMenu( final View menu, final View explanation ){

            //---------------------
            // メニュー本体制御
            //---------------------
            // メニューをスライドダウン
            slideDown(menu);

            //---------------------
            // 各種メニュー説明文
            //---------------------
            // 説明文のアニメーション非表示
            explanation.setVisibility(View.INVISIBLE);
            Animation animation_in = AnimationUtils.loadAnimation(explanation.getContext(), R.anim.menu_exp_close);
            explanation.startAnimation(animation_in);
        }

        /*
         * アニメーション：スライドアップ
         */
        private void slideUp(final View menu ){

            //------------------
            // animate生成
            //------------------
            int duration = getResources().getInteger(R.integer.menu_up_anim_duration);

            // animate生成
            TranslateAnimation animate = new TranslateAnimation(
                    0,
                    0,
                    menu.getHeight(),
                    0);
            animate.setDuration(duration);
            animate.setFillAfter(true);
            animate.setInterpolator(new DecelerateInterpolator());
            animate.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    // ビューを可視化
                    menu.setVisibility(View.VISIBLE);
                }
                @Override
                public void onAnimationEnd(final Animation animation) {
                    // アニメーションが終了すれば、受付可能にする
                    mIsControlReception = true;
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            menu.startAnimation(animate);
        }

        /*
         * アニメーション：スライドダウン
         */
        private void slideDown(final View menu ){

            //------------------
            // animate生成
            //------------------
            int duration = getResources().getInteger(R.integer.menu_down_anim_duration);

            // animate生成
            TranslateAnimation animate = new TranslateAnimation(
                    0,
                    0,
                    0,
                    menu.getHeight());
            animate.setDuration(duration);
            animate.setFillAfter(true);
            animate.setInterpolator(new DecelerateInterpolator());
            animate.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    // do nothing
                }
                @Override
                public void onAnimationEnd(final Animation animation) {
                    // ビューを不可視化
                    menu.setVisibility(View.GONE);

                    // アニメーションが終了すれば、受付可能にする
                    mIsControlReception = true;
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                    // do nothing
                }
            });
            menu.startAnimation(animate);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        Rect menuCorners = new Rect();
        Point globalOffset = new Point();

        //------------------------
        // メニューの位置・サイズ
        //------------------------
        // 折りたたみメニューの情報
        ViewGroup menu_collapsed = findViewById(R.id.cl_menu_collapsed);
        menu_collapsed.getGlobalVisibleRect(menuCorners);
        menuCorners.offset(-globalOffset.x, -globalOffset.y);

        // メニューのRect情報をWorldに渡す
        ParticleWorldRenderer fluidRenderer = mGLSurfaceView.getRenderer();
        fluidRenderer.setCollapsedMenuRect(menuCorners.top, menuCorners.left, menuCorners.right, menuCorners.bottom);
        fluidRenderer.finishSetMenuRect();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
        mGLSurfaceView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
