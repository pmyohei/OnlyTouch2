package com.example.onlyTouch;

import com.google.fpl.liquidfun.Vec2;

import java.util.ArrayList;
import java.util.List;


/*
 * 交点情報マネージャ
 *   ペイントしたライン同士が交差したときの交差情報
 */
public class CrossInfoManager {

    //-- LiquidFun ロード
    static {
        System.loadLibrary("liquidfun");
        System.loadLibrary("liquidfun_jni");
    }

    //-- ある点と交差している2点
    public class CrossedBothPoint {
        public int front;
        public int back;

        public CrossedBothPoint(int front, int back){
            this.front = front;
            this.back  = back;
        }
    }

    private int             crossNum;                           //交差数
    private List<CrossInfo> crossInfoList = new ArrayList<>();  //交差情報リスト
    private CrossInfo       _tmpSearchedInfo;                   //直前に検索された交点情報
                                                                //！getNearCrossBothIdx()で見つかった情報を保持する


    /*
     * コンストラクタ
     */
    public CrossInfoManager() {
        this.crossNum = 0;
    }

    /*
     * 交差数の取得
     */
    public int getCrossNum() {
        return crossNum;
    }

    /*
     * 交点リストの取得
     */
    public List<CrossInfo> getCrossInfoList() {
        return crossInfoList;
    }

    /*
     * 交点情報追加
     *   para2：交差する側　：前
     *   para3：交差される側：前
     */
    public void addCrossInfo( Vec2 point, int crossing, int crossed ){

        //交点情報の生成とリストへ追加
        CrossInfo info = new CrossInfo(point, crossing, crossing + 1, crossed, crossed + 1);
        crossInfoList.add(info);

        //交差数を加算
        crossNum++;
    }

    /*
     * 指定した座標にもっとも近い交点座標の交差情報を取得する
     */
    public CrossedBothPoint getNearCrossBothIdx(int idx, Vec2 pos) {

        //指定点の座標
        float x = pos.getX();
        float y = pos.getY();

        //指定点と交点座標の距離
        double distance = 9999;

        //最小距離の交点Index
        CrossInfo targetInfo = null;

        //交点情報数分繰り返し
        for( CrossInfo info : this.crossInfoList ){

            //「交差する側-前」の交点情報を探す
            if( (idx == info.crossingFront) || (idx == info.crossingBack)
                    || (idx == info.crossedFront) || (idx == info.crossedBack) ){

                //交点座標を取得
                float crossx = info.getPoint().getX();
                float crossy = info.getPoint().getY();

                //距離を計算
                double tmp = Math.sqrt((x - crossx) * (x - crossx) + (y - crossy) * (y - crossy));
                if( tmp < distance ){
                    //最小距離を更新
                    distance = tmp;

                    //Index更新
                    targetInfo = info;
                }
            }
        }

        //見つからなければ、終了
        if( targetInfo == null ){
            //ここにくることはない
            return null;
        }

        //対応する交差先：前方・後方
        int front;
        int back;
        if( (idx == targetInfo.crossingFront) || (idx == targetInfo.crossingBack) ){
            //交差した側の場合
            front = targetInfo.getCrossedFront();
            back  = targetInfo.getCrossedBack();

        } else {
            //交差された側の場合
            front = targetInfo.getCrossingFront();
            back  = targetInfo.getCrossingBack();
        }

        //交点情報を保持
        _tmpSearchedInfo = targetInfo;

        return new CrossedBothPoint(front, back);
    }

    /*
     * getNearCrossBothIdx()で見つかった交点情報の取得
     * ！必ず、getNearCrossBothIdx()コール後にコールすること。
     */
    public CrossInfo getTmpSearchedInfo(){
        return _tmpSearchedInfo;
    }


    /*
     * 指定Indexは交差しているか
     */
    public boolean isCrossing( int idx ){

        //交点情報数
        for( CrossInfo info : this.crossInfoList ){

            //「交差する側-前」の交点情報を探す
            if( (idx == info.getCrossingFront()) || (idx == info.getCrossingBack())
                    || (idx == info.getCrossedFront()) || (idx == info.getCrossedBack()) ){

                //あり
                return true;
            }
        }

        //なし
        return false;
    }



    /*
     * 交点情報
     */
    public class CrossInfo {

        private Vec2 point = null;          //交点座標
        private int crossingFront;          //交差した側-前
        private int crossingBack;           //交差した側-後
        private int crossedFront;           //交差された側-前
        private int crossedBack;            //交差された側-後
        private boolean isCrossedMyLine;    //自ライン上での交差かどうか

        public CrossInfo(Vec2 point, int crossingFront, int crossingBack, int crossedFront, int crossedBack) {
            this.point           = point;
            this.crossingFront   = crossingFront;
            this.crossingBack    = crossingBack;
            this.crossedFront    = crossedFront;
            this.crossedBack     = crossedBack;
            this.isCrossedMyLine = false;
        }

        public Vec2 getPoint() {
            return point;
        }

        public int getCrossingFront() {
            return crossingFront;
        }

        public int getCrossingBack() {
            return crossingBack;
        }

        public int getCrossedFront() {
            return crossedFront;
        }

        public int getCrossedBack() {
            return crossedBack;
        }

        public boolean isCrossedMyLine() {
            return isCrossedMyLine;
        }

        public void setCrossedMyLine(boolean crossedMyLine) {
            isCrossedMyLine = crossedMyLine;
        }

    }

}




