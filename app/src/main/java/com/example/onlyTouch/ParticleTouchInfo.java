package com.example.onlyTouch;

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
    ParticleTouchStatus status;
    // タッチしている境界パーティクル
    int borderIndex;
    // タッチ座標
    float touchPosX;
    float touchPosY;
    // タッチ座標（world）
    float touchPosWorldX;
    float touchPosWorldY;

    /*
     * コンストラクタ
     */
    public ParticleTouchInfo(){
        this.status = ParticleTouchStatus.OUTSIDE;
        this.touchPosX = INVALID_TOUCH_POS;
        this.touchPosY = INVALID_TOUCH_POS;
    }

    /*
     * setting
     */
    public void setBorderIndex(int index) {
        this.borderIndex = index;
    }
    public void setStatus(ParticleTouchStatus status) {
        this.status = status;
    }
    public void setTouchPosX(float touchPosX) {
        this.touchPosX = touchPosX;
    }
    public void setTouchPosY(float touchPosY) {
        this.touchPosY = touchPosY;
    }


}
