package com.example.onlyTouch.particle;

/*
 * パーティクルタッチ情報
 */
public class ParticleTouchInfo {

    //-----------------------
    // 定数
    //-----------------------
    // パーティクルタッチ状態
    public enum ParticleTouchStatus {
        OUTSIDE,       // 流体外部
        INSIDE,        // 流体内部
        BORDER,        // 境界粒子
        TRACE          // 追随
    }

    // タッチ座標 無効値
    public static final int INVALID_TOUCH_POS = 0xFFFF;

    //-----------------------
    // 変数
    //-----------------------
    // タッチ状態
    public ParticleTouchStatus mStatus;
    // タッチしている境界パーティクル
    public int mBorderIndex;
    // タッチ座標
    public float mTouchPosX;
    public float mTouchPosY;
    // タッチ座標（world）
    public float mTouchPosWorldX;
    public float mTouchPosWorldY;

    /*
     * コンストラクタ
     */
    public ParticleTouchInfo() {
        mStatus = ParticleTouchStatus.OUTSIDE;
        mTouchPosX = INVALID_TOUCH_POS;
        mTouchPosY = INVALID_TOUCH_POS;
    }

    /*
     * タッチ座標（画面）設定
     */
    public void setTouchPos( float touchPosX, float touchPosY ){
        mTouchPosX = touchPosX;
        mTouchPosY = touchPosY;
    }

    /*
     * タッチ座標（world）設定
     */
    public void setTouchWorldPos( float posX, float posY ){
        mTouchPosWorldX = posX;
        mTouchPosWorldY = posY;
    }

    /*
     * タッチ情報クリア
     */
    public void clearTouchInfo(){
        mStatus = ParticleTouchStatus.OUTSIDE;
        mTouchPosX = INVALID_TOUCH_POS;
        mTouchPosY = INVALID_TOUCH_POS;
    }
}
