package com.example.onlyTouch;

import com.google.fpl.liquidfun.Vec2;

public class PaintTouchInfo {

    /* LiquidFun ロード */
    static {
        System.loadLibrary("liquidfun");
        System.loadLibrary("liquidfun_jni");
    }

    public boolean isOutline() {
        return outline;
    }

    public void setOutline(boolean outline) {
        this.outline = outline;
    }

    public CROSS_INFO getCrossInfo() {
        return crossInfo;
    }

    public void setCrossInfo(CROSS_INFO crossInfo) {
        this.crossInfo = crossInfo;
    }

    //閉領域の種別
    enum CLOSING_KIND{
        NONE,             //閉領域なし
        NEW,              //新規閉領域
        EXISTING,         //既存閉領域
    };

    //閉領域の種別
    enum CROSS_INFO{
        NONE,             //交差なし
        FRONT,            //交差あり（前方）
        BACK,             //交差あり（後方）
    };

    private Vec2 touchPos = new Vec2();     //タッチ座標
    private int event;                      //タッチタイミング
    private int closeArea;                  //閉領域
    private float slope;                    //前の点との線分の傾き
    private float intercept ;               //前の点との線分の切片
    private CLOSING_KIND closingKind;       //閉領域の種別
    private CROSS_INFO crossInfo;           //交差の有無

    private boolean outline;

    //コンストラクタ
    public PaintTouchInfo( Vec2 pos, int event){
        this.touchPos = pos;
        this.event    = event;

        //初期値は「所属閉領域なし」
        this.closeArea   = 0;
        this.closingKind = CLOSING_KIND.NEW;

        //輪郭点ではない
        this.outline = false;

        //傾き・切片
        this.slope     = 0;
        this.intercept = 0;

        //交差の有無
        this.crossInfo = CROSS_INFO.NONE;
    }

    //座標取得
    public Vec2 getTouchPos() {
        return touchPos;
    }

    //所属する閉領域を設定
    public void setCloseArea(int value){
        this.closeArea = value;
    }

    public int getCloseArea() {
        return closeArea;
    }

    public int getEvent() {
        return event;
    }

    public float getSlope() {
        return slope;
    }

    public void setSlope(float slope) {
        this.slope = slope;
    }

    public float getIntercept() {
        return intercept;
    }

    public void setIntercept(float intercept) {
        this.intercept = intercept;
    }

    public CLOSING_KIND getClosingKind() {
        return closingKind;
    }

    public void setClosingKind(CLOSING_KIND closingKind) {
        this.closingKind = closingKind;
    }

    //保持してる閉領域識別子と同じか判定
    public boolean isHasClosingArea(int area){

        if(this.closeArea == area){
            return true;
        }

        return false;
    }

}
