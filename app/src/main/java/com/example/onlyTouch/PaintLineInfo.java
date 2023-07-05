package com.example.onlyTouch;

import java.util.ArrayList;

//ライン情報リスト
//  タッチされたラインの情報を保持するクラス
public class PaintLineInfo {

    //ライン識別子
    private int lineIdentifier;

    //タッチ座標のリスト
    private ArrayList<PaintTouchInfo> touchInfo;

    //コンストラクタ
    public PaintLineInfo( int identifier, ArrayList<PaintTouchInfo> touchInfo ){
        this.lineIdentifier = identifier;
        this.touchInfo      = new ArrayList<PaintTouchInfo>(touchInfo);     //コール元のリストは、格納後クリアするため、ディープコピー
    }

    //タッチ座標を取得
    public ArrayList<PaintTouchInfo> getTouchInfo() {
        return touchInfo;
    }

    //タッチ座標の設定
    public void setTouchInfo(ArrayList<PaintTouchInfo> touchInfo) {
        this.touchInfo = touchInfo;
    }

    //ライン識別子の取得
    public int getLineIdentifier() {
        return lineIdentifier;
    }

    //ライン識別子の設定
    public void setLineIdentifier(int lineIdentifier) {
        this.lineIdentifier = lineIdentifier;
    }
}
