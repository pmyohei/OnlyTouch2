package com.example.onlyTouch;

import com.google.fpl.liquidfun.Vec2;

public class TouchPointInfo {

    /* LiquidFun ロード */
    static {
        System.loadLibrary("liquidfun");
        System.loadLibrary("liquidfun_jni");
    }

    //交差種別
    enum CROSS_POSITION {
        NONE,             //交差なし
        FRONT,            //交差あり（前方）
        BACK,             //交差あり（後方）
    };

    //所属ラインID-無効値（非輪郭線）
    public static final int NOT_OUTLINE = -1;

    private Vec2        touchPos;          //タッチ座標
    private int         event;             //タッチタイミング

    private int         lineId;            //所属ラインID
    private float       slope;             //前の点との線分の傾き
    private float       intercept ;        //前の点との線分の切片

    private boolean     outline;            //輪郭点
    private boolean     isPointOfArea;      //領域の構成点か否か
    private CROSS_POSITION crossInfo;          //交差情報

    /*
     * コンストラクタ
     */
    public TouchPointInfo(Vec2 pos, int event, int lineNum){
        //パラメータの設定
        this.touchPos = pos;
        this.event    = event;
        this.lineId = lineNum;

        //輪郭点ではない
        this.outline = false;
        //未交差(生成時点では判定しない)
        this.crossInfo = CROSS_POSITION.NONE;

        //傾き・切片
        this.slope     = 0.0f;
        this.intercept = 0.0f;

        //初めは、領域の構成点とみなしておく
        this.isPointOfArea = true;
    }

    //-- メソッド --

    public Vec2 getTouchPoint() {
        return touchPos;
    }

    public int getEvent() {
        return event;
    }

    //ラインIDを非輪郭線に設定
    public void invalidLineId() {
        this.lineId = NOT_OUTLINE;
        return ;
    }

    public int getLineId() { return lineId; }

    public boolean isOutline() {
        return outline;
    }
    public void setOutline(boolean outline) {
        this.outline = outline;
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

    public CROSS_POSITION getCrossInfo() {
        return this.crossInfo;
    }
    public void setCrossInfo(CROSS_POSITION crossInfo) {
        this.crossInfo = crossInfo;
    }

    public boolean isPointOfArea() {
        return isPointOfArea;
    }
    public void setPointOfArea(boolean pointOfArea) {
        isPointOfArea = pointOfArea;
    }
}
