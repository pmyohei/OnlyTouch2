package com.example.onlyTouch;

import com.google.fpl.liquidfun.Vec2;

//交点情報
//  ペイントしたライン同士が交差したときの交差情報
public class PaintCrossInfo {

    /* LiquidFun ロード */
    static {
        System.loadLibrary("liquidfun");
        System.loadLibrary("liquidfun_jni");
    }

    //交差時の種別
    enum CLOSING_KIND{
        NONE,             //閉領域なし
        NEW,              //新規の閉領域の発生
        EXISTING,         //既存の閉領域の発生
    };

    private Vec2 point = new Vec2();            //タッチ座標
    private int  area;                          //閉領域
    private int  myIndex;                       //交差した線分を持つIndex（交差判定対象）
    private int  oppositeIndex;                 //交差した線分を持つIndex（交差判定対象と交差した座標（相手側））
    private int  lineIdentifier;                //ライン識別子
    private int  myLineIndex;                   //判定対象の点のラインIndex
    private int  oppositeLineIndex;             //判定対象と交差したラインのIndex
    private boolean isCrossedMyLine = false;    //自ライン上での交差かどうか
    private CLOSING_KIND closingKind;           //閉領域の種別

    public PaintCrossInfo(Vec2 pos, int my, int opposite) {
        this.point             = pos;
        this.myLineIndex       = my;
        this.oppositeLineIndex = opposite;

        this.closingKind       = CLOSING_KIND.NONE;

        //同じライン同士の交差か判定
        if( my == opposite ){
            isCrossedMyLine = true;
        }
    }

    public Vec2 getPoint() {
        return point;
    }

    public void setPoint(Vec2 point) {
        this.point = point;
    }

    public int getArea() {
        return area;
    }

    public void setArea(int area) {
        this.area = area;
    }

    public int getOppositeIndex() {
        return oppositeIndex;
    }

    public void setOppositeIndex(int oppositeIndex) {
        this.oppositeIndex = oppositeIndex;
    }

    public int getMyIndex() {
        return myIndex;
    }

    public void setMyIndex(int myIndex) {
        this.myIndex = myIndex;
    }

    //保持してる閉領域識別子と同じか判定
    public boolean isHasClosingArea(int area){

        if(this.area == area){
            return true;
        }

        return false;
    }

    public int getLineIdentifier() {
        return lineIdentifier;
    }

    public void setLineIdentifier(int lineIdentifier) {
        this.lineIdentifier = lineIdentifier;
    }

    public int getMyLineIndex() {
        return myLineIndex;
    }

    public void setMyLineIndex(int myLineIndex) {
        this.myLineIndex = myLineIndex;
    }

    public int getOppositeLineIndex() {
        return oppositeLineIndex;
    }

    public void setOppositeLineIndex(int oppositeLineIndex) {
        this.oppositeLineIndex = oppositeLineIndex;
    }

    public boolean isCrossedMyLine() {
        return isCrossedMyLine;
    }

    public void setCrossedMyLine(boolean crossedMyLine) {
        isCrossedMyLine = crossedMyLine;
    }

    public CLOSING_KIND getClosingKind() {
        return closingKind;
    }

    public void setClosingKind(CLOSING_KIND closingKind) {
        this.closingKind = closingKind;
    }
}
