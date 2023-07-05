package com.example.onlyTouch;

import com.google.fpl.liquidfun.Vec2;

import java.util.ArrayList;

public class ScreenEdgeLineInfo {

    /* LiquidFun ロード */
    static {
        System.loadLibrary("liquidfun");
        System.loadLibrary("liquidfun_jni");
    }

    //画面端から伸ばす線の起点座標
    private ArrayList<Vec2> srcPointListFromEdge = new ArrayList<Vec2>();

    private final int SCREEN_X_MAX    = 2000;     //画面 X座標最大値
    private final int SCREEN_Y_MIN    = -3000;    //画面 Y座標最大値
    private final int SCREEN_INTERVAL = 100;      //線を引いていく間隔


    //コンストラクタ
    public ScreenEdgeLineInfo(){
        //外側から伸ばす線の座標リストを生成
        this.createCoordinateExtendEdgeList();
    }

    //起点座標の取得
    public ArrayList<Vec2> getSrcPointListFromEdge(){
        return this.srcPointListFromEdge;
    }


    //画面端から内側へ伸ばす線の開始座標リストの生成
    //
    //
    private void createCoordinateExtendEdgeList(){

        //-- 画面端からの延伸座標リストを設定する --//
        //4辺分生成
        //生成順は、左辺→下辺→右辺→上辺
        int x = 0;
        int y = -SCREEN_INTERVAL;

        //左辺（下へ向かう形で生成）
        for( ; y > SCREEN_Y_MIN ; y -= SCREEN_INTERVAL){
            srcPointListFromEdge.add(new Vec2(x, y));
            //Log.i("srcPoint", "srcPoint=" + x + ", " + y);
        }
        //Log.i("srcPoint", "---");

        //下辺（右へ向かう形で生成）
        x = SCREEN_INTERVAL;
        y = SCREEN_Y_MIN;
        for( ; x < SCREEN_X_MAX ; x += SCREEN_INTERVAL ){
            srcPointListFromEdge.add(new Vec2(x, y));
            //Log.i("srcPoint", "srcPoint=" + x + ", " + y);
        }
        //Log.i("srcPoint", "---");

        //右辺（上へ向かう形で生成）
        x = SCREEN_X_MAX;
        y = SCREEN_Y_MIN + SCREEN_INTERVAL;
        for( ; y < 0 ; y += SCREEN_INTERVAL ){
            srcPointListFromEdge.add(new Vec2(x, y));
            //Log.i("srcPoint", "srcPoint=" + x + ", " + y);
        }
        //Log.i("srcPoint", "---");

        //上辺（左へ向かう形で生成）
        x = SCREEN_X_MAX - SCREEN_INTERVAL;
        y = 0;
        for( ; x > 0 ; x -= SCREEN_INTERVAL ){
            srcPointListFromEdge.add(new Vec2(x, y));
            //Log.i("srcPoint", "srcPoint=" + x + ", " + y);
        }
        //Log.i("srcPoint", "---");
    }
}
