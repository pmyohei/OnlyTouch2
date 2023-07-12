package com.example.onlyTouch;

/*
 * パーティクルタッチ情報
 */
public class ParticleTouchInfo {

    // パーティクルタッチ状態
    public enum ParticleTouchStatus {
        OUTSIDE,        // 流体外部
        INSIDE,        // 流体内部
        BORDER,        // 境界粒子
        TRACE          // 追随
    }


    int borderIndex;
    ParticleTouchStatus status;
    float touchPosX;
    float touchPosY;
    float touchPosWorldX;
    float touchPosWorldY;

    /*
     * コンストラクタ
     */
    public ParticleTouchInfo(int border, ParticleTouchStatus status, float touchx, float touchy){
        this.borderIndex = border;
        this.status = status;
        this.touchPosX = touchx;
        this.touchPosY = touchy;
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
