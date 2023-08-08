package com.example.onlyTouch.object;

import android.os.Build;
import android.view.MotionEvent;

import androidx.annotation.RequiresApi;

import com.example.onlyTouch.convert.Conversion;
import com.example.onlyTouch.opengl.ParticleGLSurfaceView;
import com.example.onlyTouch.particle.ParticleManager;
import com.google.fpl.liquidfun.Body;
import com.google.fpl.liquidfun.World;

import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

/*
 * 銃弾管理
 */
public class BulletManager {

    //-----------------
    // 定数
    //-----------------
    // 弾サイズ
    private static final float BULLET_SIZE = 0.4f;
    // テクスチャ未生成
    private final int NO_TEXTURE = -1;

    //-----------------
    // 変数
    //-----------------
    private final World mWorld;
    private final ParticleGLSurfaceView mGLSurfaceView;
    private int mTextureID;

    private boolean mOnBullet;
    private final ArrayList<Bullet> mShotBullets;
    private final ArrayList<Bullet> mRemoveBullets;
    private int mBulletShotCycle;
    private float mBulletShotPosX;
    private float mBulletShotWorldPosY;

    /*
     * コンストラクタ
     */
    public BulletManager(World world, ParticleGLSurfaceView glSurfaceView) {

        mWorld = world;
        mGLSurfaceView = glSurfaceView;

        //----------
        // 初期設定
        //----------
        mOnBullet = false;
        mRemoveBullets = new ArrayList<>();
        mShotBullets = new ArrayList<>();
        mTextureID = NO_TEXTURE;
    }

    /*
     *  弾の管理(生成・削除)
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void bulletManage(GL10 gl, ParticleManager particleManager) {

        // 大砲offなら何もしない
        if ( !mOnBullet ) {
            return;
        }

        // 位置が急上昇した境界パーティクルを取得
        int tooRiseIndex = particleManager.getTooRiseBorderParticle();
        // 保持している境界パーティクルの位置情報を更新
        particleManager.updateBorderParticlePosY();

        //-------------
        // 弾の描画
        //-------------
        // 発射済みの弾の描画
        for (Bullet bullet : mShotBullets) {

            //---------------
            // 弾の減速対応
            // (境界パーティクルに掠った際、パーティクルが急上昇するのを防ぐための対応)
            //---------------
            if (tooRiseIndex != particleManager.NOTHING_TOO_RISE) {
                float borderY = particleManager.getCurrentParticleSystem().getParticlePositionY(tooRiseIndex);
                float bulletY = bullet.getBody().getPositionY();

                // 急上昇した境界パーティクルよりも上に位置する弾は減速させる
                if (bulletY >= borderY) {
                    // 減速
                    bullet.loseSpeed();
                }
            }

            //-----------
            // 減速判定
            //-----------
            boolean isDeceleration = bullet.isDeceleration();
            if (isDeceleration) {
                // 減速した弾は、削除リストに追加し描画もスキップ
                mRemoveBullets.add(bullet);
                continue;
            }

            // 描画
            bullet.draw(gl);
        }

        //-----------------
        // 削除対象の弾を削除
        //-----------------
        removeBullets();

        //-----------------
        // 弾の生成
        //-----------------
        shotBullet(gl);
    }

    /*
     *  削除対象の弾の削除
     */
    private void removeBullets() {

        // 削除対象なければ、処理なし
        if (mRemoveBullets.size() == 0) {
            return;
        }

        //-------------
        // 削除
        //-------------
        for (Bullet bullet : mRemoveBullets) {
            // 削除対象
            Body bulletBody = bullet.getBody();

            // 削除
            mWorld.destroyBody(bulletBody);
            bulletBody.delete();
            mShotBullets.remove(bullet);
        }

        // 削除リストをクリア
        mRemoveBullets.clear();
    }

    /*
     *  銃弾全クリア
     */
    private void clearAllBullets() {

        //-----------------------
        // 発射済みの弾を全て削除
        //-----------------------
        for (Bullet bullet : mShotBullets) {
            // 削除対象
            Body bulletBody = bullet.getBody();
            // 削除
            mWorld.destroyBody(bulletBody);
            bulletBody.delete();
        }

        //-----------------------
        // クリア
        //-----------------------
        mShotBullets.clear();
        mRemoveBullets.clear();
    }

    /*
     *  弾の生成と発射
     */
    private void shotBullet(GL10 gl) {

        //-------------
        // 生成判定
        //-------------
        mBulletShotCycle++;
        if ((mBulletShotCycle % 10) != 0) {
            // 周期未達なら何もしない
            return;
        }

        // 周期リセット
        mBulletShotCycle = 0;

        //---------------
        // 弾の生成と発射
        //---------------
        // 発射位置：X座標　　！Y座標は発射位置固定としており、変換対象の値はなんでもよいため0としている
        float[] shotPosX = Conversion.convertPointScreenToWorld(mBulletShotPosX, 0, gl, mGLSurfaceView);
        float shotPosY = mBulletShotWorldPosY;

        // 弾用のテクスチャ生成
        if( mTextureID == NO_TEXTURE ){
            mTextureID = Conversion.makeTexture(gl, Bullet.TEXTURE_RESOUCE_ID, mGLSurfaceView.getContext());
        }
        // 弾を生成・発射
        Bullet bullet = new Bullet(mWorld, shotPosX[0], shotPosY, mTextureID);
        bullet.shotUp();

        // 生成済みリストに追加
        mShotBullets.add(bullet);
    }

    /*
     * 銃弾発射位置の制御
     */
    public boolean controlBulletShootPos(MotionEvent event) {

        switch (event.getAction()) {
            // タッチ移動
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                // タッチ位置のX座標から銃弾が発射されるようにする
                mBulletShotPosX = event.getX();
                break;

            // タッチ解除
            case MotionEvent.ACTION_UP:
            default:
                break;
        }

        return true;
    }

    /*
     * 銃弾発射位置（Y座標）の設定
     * 　　引数「画面下部座標」に対して、少し上の高さから発射する方針
     */
    public void setShootPosY( float screenBottomPosY ) {
        // 床があるため、画面最下部より少し高めの位置を設定する
        mBulletShotWorldPosY = screenBottomPosY + (BULLET_SIZE * 2);
    }


    /*
     *  銃弾発射中かどうか
     */
    public boolean onBullet() {
        return mOnBullet;
    }

    /*
     *  銃弾のOn/Offを切り替え
     */
    public boolean switchBulletOnOff() {
        // 現在状態から切り替え
        mOnBullet = !mOnBullet;

        //-----------
        // 大砲on
        //-----------
        if( mOnBullet ){
            // 銃弾発射サイクルリセット
            mBulletShotCycle = 0;
            // 発射位置（X座標）を画面中心位置で初期化
            mBulletShotPosX = mGLSurfaceView.getWidth() / 2f;
            // 切り替え後の状態を返す
            return mOnBullet;
        }

        //-----------
        // 大砲off
        //-----------
        // 発射済みの弾を全て削除
        clearAllBullets();

        // 切り替え後の状態を返す
        return mOnBullet;
    }
}
