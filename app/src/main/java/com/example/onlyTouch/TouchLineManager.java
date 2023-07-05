package com.example.onlyTouch;

import android.util.Log;
import android.view.MotionEvent;

import com.google.fpl.liquidfun.Vec2;

import java.util.ArrayList;
import java.util.List;

public class TouchLineManager {

    /* LiquidFun ロード */
    static {
        System.loadLibrary("liquidfun");
        System.loadLibrary("liquidfun_jni");
    }

    //--画面情報
    private final int SCREEN_X_MAX    = 2000;     //画面 X座標最大値
    private final int SCREEN_Y_MIN    = -3000;    //画面 Y座標最大値
    private final int SCREEN_INTERVAL = 100;      //線を引いていく間隔

    //-- 輪郭点の設定の進む方向
    enum ADVANCE_DIRECTION {
        WHICH_ONE,
        CROSS_FRONT,
        CROSS_BACK,
        NEXT
    }

    /*
    //-- 交差先の領域構成点パターン
    enum CROSSED_AREA_PATTERN {
        BOTH_,
        BOTH_,
    }
     */


    //ライン情報
    private LineInfo mLineInfo;

    //タッチ点情報リスト
    private ArrayList<TouchPointInfo> mTouchPointList = new ArrayList<TouchPointInfo>();

    //交点情報リスト
    private CrossInfoManager mCrossInfoManager = new CrossInfoManager();

    //ライン情報リスト
    private ArrayList<PaintLineInfo> mPaintLinesInfo = new ArrayList<PaintLineInfo>();

    //ペイントタッチ交点情報リスト
    private ArrayList<PaintCrossInfo> mPaintCrossInfo = new ArrayList<PaintCrossInfo>();

    //ライン識別子
    private int mLineIdentifier = 0;

    //輪郭点リスト
    private ArrayList<Vec2> mOutlinePointList = new ArrayList<>();



    //直前の閉領域識別子の情報
    //※直前の交差判定で、新規であれば、「0xFFFF」
    //private int mPrevClosingArea;
    //private static final int PREV_NEW_CLOSED_AREA = 0xFFFF;

    //dbg
    private Vec2 dbg_new_pos   = new Vec2();
    private Vec2 dbg_pre_pos   = new Vec2();
    private Vec2 dbg_front_pos = new Vec2();
    private Vec2 dbg_back_pos  = new Vec2();

    private int dbg_new_index;
    private int dbg_pre_index;
    private int dbg_front_index;
    private int dbg_back_index;

    //その時点の確認用
    private int dbg_targetLine;
    private int dbg_targetIndex;
    private int dbg_crossNum;
    //dbg

    //コンストラクタ
    public TouchLineManager(LineInfo lineInfo, ArrayList<TouchPointInfo> touchPointList) {
        //ライン情報
        this.mLineInfo = lineInfo;
        //タッチ点情報リスト
        this.mTouchPointList = touchPointList;
    }

    //輪郭線の検証
    // para： ペイントしたラインの情報
    //   タッチラインに対して、輪郭線にあたる座標を判定する
    //
    public void verifyOutLine() {

        //線分情報の算出
        this.setLineSegmentData();

        //外側に面した座標を抽出（この時点では完全に抽出はできない）
        //this.setOuterPointCertain();

        //輪郭点に続いている座標も輪郭点として更新
        //this.setOuterPointLink_old();

        Log.i("outTest", "pre hasOutLine");

        //輪郭線があるか
        if( !this.mLineInfo.hasOutLine() ){
            Log.i("outTest", "root");

            //ないなら、終了
            return ;
        }

        Log.i("outTest", "pre exceptPointOfArea");

        //輪郭点になりえないラインに所属する点を非領域構成点とする
        this.exceptPointOfArea();

        Log.i("outTest", "pre getOutlineOnePoint");

        //輪郭点となるIndexを取得
        int oneOutlineIndex = this.getOutlineOnePoint();

        Log.i("outTest", "pre setOuterPointAlongLink=" + oneOutlineIndex);

        //輪郭線に沿い、輪郭点を設定する
        this.setOuterPointAlongLink(oneOutlineIndex);

        //--dbg
        /*
        for( TouchPointInfo info: this.mTouchPointList ){
            Log.i("Last outer", "x\t" + info.getTouchPoint().getX() + "\t" + info.getTouchPoint().getY() + "\tisoutLine\t" + info.isOutline() + "\tisPointOfArea\t" + info.isPointOfArea());
        }
         */

        for( Vec2 info: mOutlinePointList ){
            //


            Log.i("Last outer", "x\t" + info.getX() + "\t" + info.getY());
        }

        //交点情報
        Log.i("Last cross", "num=" + this.mCrossInfoManager.getCrossNum());
        for( CrossInfoManager.CrossInfo cInfo : this.mCrossInfoManager.getCrossInfoList() ){
            Log.i("Last cross", "\t" + cInfo.getCrossingFront() + "\t" + cInfo.getCrossingBack() + "\t" + cInfo.getCrossedFront() + "\t" + cInfo.getCrossedBack());
        }
        //--dbg

        return;
    }


    //線分情報の追加
    //  タッチ情報に、連続する2点間の線分情報を追加する
    //  線分は、次の点との線分とする。
    //  また、既存の線分と交差しているかを判定する。
    private void setLineSegmentData(){

        //最後の点から1つ手前まで、線分を計算
        int last = this.mTouchPointList.size() - 1;
        for( int i = 0; i < last; i++ )
        {
            //タッチ情報
            TouchPointInfo touchInfo     = this.mTouchPointList.get(i);
            TouchPointInfo touchNextInfo = this.mTouchPointList.get(i + 1);

            //ライン最後の交点なら、次の点はないためスキップ
            if( touchInfo.getEvent() == MotionEvent.ACTION_UP ){
                continue;
            }

            //座標
            Vec2 point = touchInfo.getTouchPoint();
            //次の座標
            Vec2 nextPoint = touchNextInfo.getTouchPoint();

            //傾きと切片の算出・設定
            this.calcSlopeIntercept(point, nextPoint, touchInfo);

            //交差したか判定
            boolean isCross = isCrossing(point, touchInfo.getSlope(), touchInfo.getIntercept(), i);
            if( isCross ){
                //交差していれば、交差情報を更新
                //touchInfo.setCrossInfo( TouchPointInfo.CROSS_INFO.FRONT );
                //touchNextInfo.setCrossInfo( TouchPointInfo.CROSS_INFO.BACK );
            }
        }
    }



    /**
     * 描画された線の情報から、交点情報を算出する
     *   para：判定対象の座標位置
     *   para：傾き、切片
     *   para：交差判定のIndex(確認終了Index)
     */
    private boolean isCrossing(Vec2 point, float newSlope, float newIntercept, int checkIndex) {

        boolean isCross = false ;

        //交点が発生するのは、2点目から
        if( checkIndex < 2 ){
            return isCross;
        }

        //X座標の範囲（新規座標～次点の座標）
        float x       = point.getX();
        float nextX   = this.mTouchPointList.get(checkIndex + 1).getTouchPoint().getX();
        float newMinX = Math.min(x, nextX);
        float newMaxX = Math.max(x, nextX);

        //判定中座標の2つ前までチェック
        for( int i = 0; i < checkIndex - 1; i++ )
        {
            //交差判定対象のタッチ情報
            TouchPointInfo extInfo = this.mTouchPointList.get(i);
            //ライン最後の交点なら、線分はないためスキップ
            if( extInfo.getEvent() == MotionEvent.ACTION_UP ){
                continue;
            }

            //次のタッチ情報
            TouchPointInfo extInfoNext = this.mTouchPointList.get(i + 1);

            //交差判定する線分情報
            float extSlope     = extInfo.getSlope();
            float extIntercept = extInfo.getIntercept();

            //X座標の範囲（交点判定対象の2点間）
            x     = extInfo.getTouchPoint().getX();
            nextX = extInfoNext.getTouchPoint().getX();

            float extMinX = Math.min(x, nextX);
            float extMaxX = Math.max(x, nextX);

            //交差判定
            Vec2 cross = this.calcCrossData(point, newSlope, newIntercept, extSlope, extIntercept, newMinX, newMaxX, extMinX, extMaxX);
            if ( cross == null ) {
                //線分内での交差がないなら、次の点へ
                continue;
            }

            Log.i("outTest", "交差あり=" + cross.getX() + "\tminX=" + extMinX + "\tmaxX=" + extMaxX);

            //既存点に、交点情報を設定
            //extInfo.setCrossInfo(TouchPointInfo.CROSS_INFO.FRONT);
            //extInfoNext.setCrossInfo(TouchPointInfo.CROSS_INFO.BACK);

            //交点情報をリストに追加
            this.addCrossData(cross.getX(), cross.getY(), checkIndex, i);

            //-- ライン情報の交差ライン数を更新
            //交差先は自分自身か
            this.updateCrossNum( checkIndex, i );

            //交差あり
            isCross = true;
        }

        return isCross;
    }


    /*
     * 2つの点が同一ライン上の点か判定
     */
    private void updateCrossNum(int idx1, int idx2){

        //ラインID取得
        int lineId1 = this.mTouchPointList.get(idx1).getLineId();
        int lineId2 = this.mTouchPointList.get(idx2).getLineId();

        //同じラインかどうか
        if( lineId1 == lineId2 ){
            //自ラインとの交差数をインクリメント
            this.mLineInfo.IncrementCrossNumSelf( lineId1 );

        } else {
            //他ラインとの交差数をインクリメント
            this.mLineInfo.IncrementCrossNumOther( lineId1, lineId2 );
        }
    }

    /**
     *  「非領域構成点」の設定
     *  　・輪郭線になりえないラインに所属する点
     *  　・交点からはみ出した点
     */
    private void exceptPointOfArea() {

        //ラインリスト取得
        List<LineInfo.OneLineData> lineList = this.mLineInfo.getLineList();

        //ライン数分ループ
        for( LineInfo.OneLineData line: lineList ){

            //非輪郭線の開始・終了Indexを取得
            int top  = line.getTopIndex();
            int last = line.getLastIndex();

            Log.i("exceptPointOfArea", "top last");

            //輪郭線ではない場合
            if( !line.isOutline() ){

                //対象ラインに所属するすべての点
                for( int i = top; i <= last; i++ )
                {
                    //「非領域構成点」とする
                    this.mTouchPointList.get(i).setPointOfArea(false);

                    //所属ラインのID無効化
                    this.mTouchPointList.get(i).invalidLineId();
                }

            //輪郭線の場合
            } else {

                //-- 交点からはみ出した点を「非領域構成点」とする

                //先頭から参照
                for( int i = top; i <= last ; i++ )
                {
                    Log.i("exceptPointOfArea", "先頭");

                    TouchPointInfo point = this.mTouchPointList.get(i);

                    //「非領域構成点」とする
                    point.setPointOfArea(false);

                    //交点なら、ここで終了
                    if( mCrossInfoManager.isCrossing(i) ){
                        break;
                    }

                    Log.i("exceptPointOfArea", "先頭 end");
                }

                //後方から参照
                for( int i = last; i >= top; i-- )
                {
                    Log.i("exceptPointOfArea", "後方");

                    TouchPointInfo point = this.mTouchPointList.get(i);

                    //「非領域構成点」とする
                    point.setPointOfArea(false);

                    //交点なら、ここで終了
                    if( mCrossInfoManager.isCrossing(i) ){
                        break;
                    }

                    Log.i("exceptPointOfArea", "後方 end");
                }
            }
        }

        return;
    }


    /**
     *  任意の輪郭点を1点取得する
     */
    private int getOutlineOnePoint() {

        //-- Y座標の区切りの中で、一番左の点（X座標が最小の点）を取得 --//

        int index = -1;

        //初期値(左下から上へ)
        int start = SCREEN_Y_MIN;
        int end   = SCREEN_Y_MIN + SCREEN_INTERVAL;
        for( ; start <= 0; start += SCREEN_INTERVAL, end += SCREEN_INTERVAL ){

            //指定範囲の中で、最大座標と最小座標を、輪郭点として更新
            index =  this.getHorizMinPointIndex(start, end);
            if( index >= 0 ){
                //取得できたら、終了
                break;
            }
        }

        return index;
    }



    /**
     *  指定されたY軸範囲内で、X軸が最小値のタッチ点Indexを取得する
     *    return：-１：該当データなし
     */
    private int getHorizMinPointIndex(int start, int end) {

        //最小値初期値
        float min = SCREEN_X_MAX;
        int   retIndex = -1;

        int size = this.mTouchPointList.size();
        for( int i = 0 ; i < size ; i++ ){

            TouchPointInfo point = this.mTouchPointList.get(i);

            //タッチ点が、領域構成と関係なければ、判定対象外
            if( !point.isPointOfArea() ){
                continue;
            }

            //より小さいX座標の値が見つかれば
            float xValue = point.getTouchPoint().getX();
            if( min > xValue ){
                //最小値更新
                min = xValue;
                //index保持
                retIndex = i;
            }
        }

        //X座標値が最小のIndexを返す
        return retIndex;
    }



    //外側にある点の判定（一定数分）
    //  ある範囲の中で、X座標Y座標がそれぞれ最大最小となっている座標を
    //　　輪郭点とする
    /*
    private void setOuterPointCertain(){

        //-- X座標の区切りの中で、高さが最小・最大になっている点を更新 --//

        //初期値(左下から右へ)
        int start = 0;
        int end   = SCREEN_INTERVAL;
        for( ; start <= SCREEN_X_MAX; start += SCREEN_INTERVAL, end += SCREEN_INTERVAL ){

            //指定範囲の中で、最大座標と最小座標を、輪郭点として更新
            this.renewOutLineInfo(start, end, true);
        }

        //-- Y座標の区切りの中で、高さが最小・最大になっている点を更新 --//

        //初期値(左下から上へ)
        start = SCREEN_Y_MIN;
        end   = SCREEN_Y_MIN + SCREEN_INTERVAL;
        for( ; start <= 0; start += SCREEN_INTERVAL, end += SCREEN_INTERVAL ){

            //指定範囲の中で、最大座標と最小座標を、輪郭点として更新
            this.renewOutLineInfo(start, end, false);
        }
    }
     */


    /*
     *  ラインに沿って、輪郭点を設定する
     */
    private void setOuterPointAlongLink(int startIndex ) {

        //設定開始index
        int i = startIndex;

        //リンクを辿る方向を取得
        int direction = this.whichDirectionCounterclockwise(i);

        Log.i("setOuterPointAlongLink", "point size=" + this.mTouchPointList.size());

        //リンク変更フラグ
        // true:変更、false:未変更（リンク継続）
        boolean linkSwitch = false;

        //1周するまで繰り返し
        for( ; ; ){

            //---- 輪郭点の設定
            Log.i("setOuterPointAlongLink", "loop top index\t" + i);

            //点を取得
            TouchPointInfo point = this.mTouchPointList.get(i);

            if( point.isOutline() ){
                //輪郭点が設定されている点まできたら、終了（1周したとき終了）
                break;
            }

            //輪郭点に設定
            point.setOutline(true);

            //輪郭点リストに追加
            mOutlinePointList.add( point.getTouchPoint() );

            //---- 辿る先を決定

            //リンクが切り替わった直後 or 設定した点が交点ではない
            if( linkSwitch || !this.mCrossInfoManager.isCrossing(i)){
                //リンク切り替えなし（リンク継続中）
                linkSwitch = false;

                //そのまま次の点へ
                i += direction;

                continue;
            }

            //-- 辿る先が変わる可能性あり

            //交差先の情報の内、この点と最も近い交点の情報を取得
            CrossInfoManager.CrossedBothPoint crossedBoth
                    = mCrossInfoManager.getNearCrossBothIdx(i, point.getTouchPoint());

            //交点情報取得
            Vec2 crossPoint = mCrossInfoManager.getTmpSearchedInfo().getPoint();

            //交差先の前方・後方の点
            int front = crossedBoth.front;
            int back  = crossedBoth.back;

            //外側にある方の点を取得
            int outer = this.getOuterPointOfCrossed(i, direction, front, back);

            //辿る先
            ADVANCE_DIRECTION advance_direction = getNextOuterDirection( outer, front, back, i + direction );

            //交差先がどちらか
            if( advance_direction == ADVANCE_DIRECTION.WHICH_ONE ){

                //-- 辿る先：外側

                //外側の方の点を次の辿る点とする
                i = outer;

                Log.i("setOuterPointAlongLink", "chg both index=" + i + "\tfront\t" + front + "\tback\t" + back);

                //辿る方向を更新
                direction = (i == front ? -1 : 1 );

                //リンク変化
                linkSwitch = true;

            //「交差先-前」
            } else if ( advance_direction == ADVANCE_DIRECTION.CROSS_FRONT ){

                //-- 辿る先：領域構成点側

                i = front;

                Log.i("setOuterPointAlongLink", "chg front index=\t" + i);

                //辿る方向：逆順
                direction = -1;

                //リンク変化
                linkSwitch = true;

            //「交差先-後」
            } else if ( advance_direction == ADVANCE_DIRECTION.CROSS_BACK ){

                //-- 辿る先：領域構成点側
                i = back;

                Log.i("setOuterPointAlongLink", "chg back index=\t" + i);

                //辿る方向：順
                direction = 1;

                //リンク変化
                linkSwitch = true;


            //辿る先の変更なし
            } else {

                Log.i("setOuterPointAlongLink", "none");

                //辿る先：変更なし
                //次の点へ進める
                i += direction;

                //リンク未変化
                linkSwitch = false;
            }


            //リンクが変わった場合、交点も輪郭点に追加
            if( linkSwitch ){
                //交点情報を、輪郭点リストに追加
                mOutlinePointList.add( crossPoint );
            }


        }
    }

    /*
     * 輪郭点の設定の方向
     */
    private ADVANCE_DIRECTION getNextOuterDirection(int outer, int front, int back, int setNextIdx) {

        boolean isAreaFront   = this.mTouchPointList.get(front).isPointOfArea();        //「交差先-前」が領域かどうか
        boolean isAreaBack    = this.mTouchPointList.get(back).isPointOfArea();         //「交差先-後」が領域かどうか
        boolean isAreaSetNext = this.mTouchPointList.get(setNextIdx).isPointOfArea();   //「設定中の点の次の点」が領域かどうか

        //交差先がどちらも領域構成点
        if( isAreaFront && isAreaBack ){

            //交差先のどちらか
            return ADVANCE_DIRECTION.WHICH_ONE;

        //「交差される側-前」のみ領域構成点、かつ、その点が外側
        //「交差される側-前」のみ領域構成点、かつ、切り替え直前の次の点が非領域
        } else if ( isAreaFront && ((front == outer) || !isAreaSetNext) ){

            //「交差先：前」
            return ADVANCE_DIRECTION.CROSS_FRONT;

        //「交差される側-後」のみ領域構成点、かつ、その点が外側
        //「交差される側-後」のみ領域構成点、かつ、切り替え直前の次の点が非領域
        } else if ( isAreaBack && ((back == outer) || !isAreaSetNext) ){

            //「交差先：後」
            return ADVANCE_DIRECTION.CROSS_BACK;

        //どちらも領域構成点ではない
        } else {

            //リンク変更はなし
            return ADVANCE_DIRECTION.NEXT;
        }
    }


    /*
     * 外側点の判定・取得（交差先の2つの点の内から）
     */
    private int getOuterPointOfCrossed(int settingIdx, int direction, int csgFrontIdx, int csgBackIdx){

        //-- 輪郭点を設定している点の位置関係から、交差先の2つの点のどちらが外側の点かを判定する

        int headIdxA = settingIdx;              //輪郭点の設定が分岐する直前のIndex
        int tailIdxB = headIdxA + direction;    //(設定上)次の点にあたるIndex

        //線分情報を持つIndex（Indexが前方の線分情報が判定に必要）
        int hasSegInfoIdx;
        if( headIdxA < tailIdxB ){
            hasSegInfoIdx = headIdxA;
        } else {
            hasSegInfoIdx = tailIdxB;
        }

        //-- 位置情報の取得
        Vec2 pointHeadA    = this.mTouchPointList.get(headIdxA).getTouchPoint();
        Vec2 pointTailB    = this.mTouchPointList.get(tailIdxB).getTouchPoint();

        Vec2 csgFrontPoint = this.mTouchPointList.get(csgFrontIdx).getTouchPoint();
        Vec2 csgBackPoint  = this.mTouchPointList.get(csgBackIdx).getTouchPoint();

        //-- Y座標
        float y_headA = pointHeadA.getY();
        float y_tailB = pointTailB.getY();

        float y_csgFront  = csgFrontPoint.getY();
        float y_csgBack   = csgBackPoint.getY();

        //-- X座標
        float x_headA = pointHeadA.getX();
        float x_tailB = pointTailB.getX();

        float x_csgFront = csgFrontPoint.getX();
        float x_csgBack  = csgBackPoint.getX();

        //-- 傾き
        float slope = this.mTouchPointList.get(hasSegInfoIdx).getSlope();

        //---- 輪郭点設定中の位置関係に応じた判定

        Log.i("getOuterPointOfCrossed", "headIdxA\t" + headIdxA + "tailIdxB\t" + tailIdxB);
        Log.i("getOuterPointOfCrossed", "csgFrontIdx\t" + csgFrontIdx + "csgBackIdx\t" + csgBackIdx);

        //外側点の初期値を「前」とする
        int outerIdx = csgFrontIdx;

        //-- 縦軸で切り分け
        //下方向へ移動
        if(y_headA > y_tailB){

            //-- 傾きで切り分け
            if( slope > 0 ) {
                //傾き：正

                //-- 外側点：上
                //「前」が下なら、上に位置するのは「後」のため、更新
                if( this.isBelowLine(hasSegInfoIdx, csgFrontIdx) ){
                    outerIdx = csgBackIdx;
                }

                Log.i("getOuterPointOfCrossed", "0:0\t" + outerIdx);

            } else if( slope < 0 ){
                //傾き：負

                //-- 外側点：下
                //「後」が下なら、外側点を更新
                if( this.isBelowLine(hasSegInfoIdx, csgBackIdx) ){
                    outerIdx = csgBackIdx;
                }

                Log.i("getOuterPointOfCrossed", "0:1\t" + outerIdx);

            } else{
                //傾き：0 水平

                //-- 外側点：左
                //「後」が左なら、外側点を更新
                if( x_csgBack < x_tailB ){
                    outerIdx = csgBackIdx;
                }
                //通ることはない
                Log.i("getOuterPointOfCrossed", "0:2\t" + outerIdx);
            }

        //上方向へ移動
        } else if (y_headA < y_tailB){

            //-- 傾きで切り分け
            if( slope > 0 ) {
                //傾き：正

                //-- 外側点：下
                //「後」が下なら、外側点を更新
                if( this.isBelowLine(hasSegInfoIdx, csgBackIdx) ){
                    outerIdx = csgBackIdx;
                }

                Log.i("getOuterPointOfCrossed", "1:0\t" + outerIdx);

            } else if( slope < 0 ){
                //傾き：負

                //-- 外側点：上
                //「前」が下なら、外側点を更新
                if( this.isBelowLine(hasSegInfoIdx, csgFrontIdx) ){
                    outerIdx = csgBackIdx;
                }

                Log.i("getOuterPointOfCrossed", "1:1\t" + outerIdx);

            } else{
                //傾き：0 水平

                //-- 外側点：右
                //「後」が右なら、外側点を更新
                if( x_csgBack > x_tailB ){
                    outerIdx = csgBackIdx;
                }

                //通ることはない
                Log.i("getOuterPointOfCrossed", "1:2\t" + outerIdx);
            }

        //横方向へ移動
        } else{

            //-- X座標で切り分け
            //右方向
            if( x_headA < x_tailB ){

                //外側点：下。「後」が下なら外側点を更新
                if( y_csgFront > y_csgBack ){
                    outerIdx = csgBackIdx;
                }

                Log.i("getOuterPointOfCrossed", "2:0\t" + outerIdx);

            //左方向
            } else {

                //外側点：上。「後」が上なら外側点を更新
                if( y_csgFront < y_csgBack ){
                    outerIdx = csgBackIdx;
                }

                Log.i("getOuterPointOfCrossed", "2:1\t" + outerIdx);
            }
        }

        //外側の点を返す
        return outerIdx;
    }


    /*
     * ラインの下にあるか判定
     */
    private boolean isBelowLine(int lineInfoidx, int checkPosIdx){

        //-- 線分情報を取得
        //傾き
        float slope = this.mTouchPointList.get(lineInfoidx).getSlope();
        //切片
        float intercept = this.mTouchPointList.get(lineInfoidx).getIntercept();

        //-- 判定対象の座標
        float x = this.mTouchPointList.get(checkPosIdx).getTouchPoint().getX();
        float y = this.mTouchPointList.get(checkPosIdx).getTouchPoint().getY();

        //-- 上下関係の判定
        //線分上のYの値：y = ax + b
        float onLineY = (slope * x) + intercept;

        //線分よりも下にあれば
        if( y < onLineY ){

            //下にあり
            return true;
        }

        //下になし
        return false;
    }

    /*
     * 左周りの方向はどちらか判定
     */
    private int whichDirectionCounterclockwise(int idx){

        Log.i("whichDirectionCo", "idx=" + idx);

        //前後の点のＹ座標を取得
        float pre  = this.mTouchPointList.get(idx - 1).getTouchPoint().getY();
        float next = this.mTouchPointList.get(idx + 1).getTouchPoint().getY();

        //-- 左周りとなるのは、高さの低い方
        int direction;

        if( pre < next ){
            //前の点の方が低い場合、前へ辿ると左周り
            direction = -1;

        } else {
            //次の点の方が低い場合、次へ辿ると左周り
            direction = 1;
        }

        return direction;
    }



    // 指定されたX座標(Y座標)の範囲において、Y座標（X座標）が最小もしくは最大にある
    // タッチ座標情報を、輪郭点として更新する
    //
    /*
    private void renewOutLineInfo(int start, int end, boolean xFlg) {

        //初期値
        final int INIT_MIN = 9999;
        final int INIT_MAX = -9999;

        //暫定の最大値・最小値
        float tmpMax = INIT_MAX;
        float tmpMin = INIT_MIN;

        //初回更新が入ったら、falseへ更新
        boolean initFlgMin = true;
        boolean initFlgMax = true;

        //既に更新済みか
        boolean alreadyMin = false;
        boolean alreadyMax = false;

        //更新対象(初期値は暫定)
        PaintTouchInfo updateInfoMin = this.mPaintLinesInfo.get(0).getTouchInfo().get(0);
        PaintTouchInfo updateInfoMax = this.mPaintLinesInfo.get(0).getTouchInfo().get(0);

        //bdg------------------
        ArrayList<Float> minList = new ArrayList<Float>();;
        ArrayList<Float> maxList = new ArrayList<Float>();;
        //bdg------------------

        //描画したライン数分ループ
        int lineIndex = 0;
        for( PaintLineInfo line: this.mPaintLinesInfo){

            //ラインのタッチ情報リストを取得
            ArrayList<PaintTouchInfo> touchList = line.getTouchInfo();

            //タッチ座標数分ループ（一番初めのタッチ情報は処理不要）
            int touchNum = touchList.size();
            for( int touchIndex = 0; touchIndex < touchNum; touchIndex++ ){

                float inRange;
                float position;
                //タッチ情報のX座標
                if( xFlg ){
                    //範囲：X座標、　最大最小チェック：高さ
                    inRange  = touchList.get(touchIndex).getTouchPos().getX();
                    position = touchList.get(touchIndex).getTouchPos().getY();
                }else{
                    //範囲：Y座標、　最大最小チェック：左右
                    inRange  = touchList.get(touchIndex).getTouchPos().getY();
                    position = touchList.get(touchIndex).getTouchPos().getX();
                }

                //チェックする範囲内かどうか
                if( ( inRange >= start ) && (inRange < end) ){

                    //現在の最小値よりも小さい
                    if( position < tmpMin ){
                        //暫定値更新
                        tmpMin = position;
                        Log.i("renewOutLineInfo", "tmpMin=" + tmpMin);

                        //見つけたのが初回
                        if( initFlgMin ){
                            initFlgMin = false;

                        } else if ( alreadyMin ){

                            //do nothing

                        } else {
                            //前回更新したものを差し戻し
                            updateInfoMin.setOutline(false);
                            Log.i("renewOutLineInfo", "false→" + updateInfoMin.getTouchPos().getY());
                        }

                        //今回発見したものを輪郭点として更新
                        updateInfoMin = touchList.get(touchIndex);

                        if( updateInfoMin.isOutline() ){
                            //更新対象が既に更新済み
                            alreadyMin = true;
                        }else{
                            alreadyMin = false;
                            updateInfoMin.setOutline(true);
                        }


                        //dbg-------
                        minList.add(tmpMin);

                    }
                    //現在の最大値よりも大きい
                    else if( position > tmpMax ){
                        //暫定値更新
                        tmpMax = position;
                        Log.i("renewOutLineInfo", "tmpMax=" + tmpMax);

                        //見つけたのが初回
                        if( initFlgMax ){
                            initFlgMax = false;

                        } else if ( alreadyMax ){

                            //do nothing

                        } else{
                            //前回更新したものを差し戻し
                            Log.i("renewOutLineInfo", "更新→" + updateInfoMax.getTouchPos().getY());
                            Log.i("renewOutLineInfo", "更新 更新前→" + updateInfoMax.isOutline());
                            updateInfoMax.setOutline(false);
                            Log.i("renewOutLineInfo", "更新 更新後→" + updateInfoMax.isOutline());
                        }

                        //今回発見したものを輪郭点として更新
                        updateInfoMax = touchList.get(touchIndex);
                        if( updateInfoMax.isOutline() ){
                            //更新対象が既に更新済み
                            alreadyMax = true;

                        }else{
                            alreadyMax = false;
                            updateInfoMax.setOutline(true);
                        }

                        //dbg-------
                        maxList.add(tmpMax);

                    } else {
                        //do nothing
                    }

                }
            }

            //対象のライン数を次へ
            lineIndex++;
        }

        //dbg
        //描画したライン数分ループ
        lineIndex = 0;
        int outCount = 0;
        for( PaintLineInfo line: this.mPaintLinesInfo) {
            ArrayList<PaintTouchInfo> touchList = line.getTouchInfo();

            int touchNum = touchList.size();
            for (int touchIndex = 0; touchIndex < touchNum; touchIndex++) {

                if( touchList.get(touchIndex).isOutline() ){
                    outCount++;
                }
            }

            lineIndex++;
        }

        return;
    }
     */


    /**
     * 交点が線分の範囲内かチェック（線分同士で交わっているかチェック）
     * 交わっていれば、交点情報を保持する。
     *   return
     *     交わりあり：閉領域識別子
     *     交わりなし：-1
     *   para
     *     交点座標(X座標)
     *     新規の線分のX座標の範囲（最小値・最大値）
     *     既存の線分のX座標の範囲（最小値・最大値）
     *     確認対象の線分を持つIndex,
     *
     *   return
     *     1以上 ：既存の閉領域
     *     -1　　　：閉領域なし
     *     0 　　　：新規閉領域
     */
    private int checkLineInRange(float crossX,
                                 float newFirstX, float newEndX,
                                 float FirstX, float endX,
                                 int   lineIndex, int touchIndex) {

        //戻り値の初期値
        int area = -1;

        //交点が線分の範囲内かチェック
        if (isInSegment(crossX, newFirstX, newEndX) && isInSegment(crossX, FirstX, endX)) {
            //交点が既に閉領域か
            area = this.mPaintLinesInfo.get(lineIndex).getTouchInfo().get(touchIndex).getCloseArea();
        }

        return area;
    }

    /**
     * 範囲判定
     */
    private boolean isInSegment(float value, float first, float last) {
        return (value >= first && value <= last);
    }

    /**
     * 交差情報の追加
     */
    private void addCrossData(float cross_x, float cross_y, int crossingFront, int crossedBack) {

        //交点情報を生成
        Vec2 pos = new Vec2(cross_x, cross_y);

        //交点情報を追加
        //※交差する側とされる側の後のIndexは、メソッド内で設定される
        this.mCrossInfoManager.addCrossInfo(pos, crossingFront, crossedBack);

        return;
    }


    /*
     * 2点を通る傾きと切片の算出・設定
     */
    private void calcSlopeIntercept(Vec2 pos1, Vec2 pos2, TouchPointInfo touchInfo) {

        //位置情報
        float x1 = pos1.getX();
        float x2 = pos2.getX();
        float y1 = pos1.getY();
        float y2 = pos2.getY();

        //傾き・切片
        float slope     = ((y2 - y1) / (x2 - x1));
        float intercept = y1 - (slope * x1);

        touchInfo.setSlope(slope);
        touchInfo.setIntercept(intercept);

        //-- dbg
        if( x1 == x2 ){
            Log.i("calcSlopeIntercept", "x same=" + x1 + "\t" + x2);
        }
        if( y1 == y2 ){
            //Log.i("calcSlopeIntercept", "y same=" + y1 + "\t" + y2);
        }
        //-- dbg

        return;
    }


    /**
     * 交点の算出
     * 　新規追加された座標
     * 　新規線分の傾き、新規線分の切片、既存線分の傾き、既存線分の切片
     */
    private Vec2 calcCrossData(Vec2 addPos, float newSlope, float newIntercept, float extSlope, float extIntercept, float min1, float max1, float min2, float max2) {

        //傾きが平行しているなら、交点そのものがなし
        if (newSlope == extSlope) {
            return null;
        }

        //-- 交点x座標
        //線分１、線分２
        //２の切片 - １の切片　／　１の傾き - ２の傾き
        float x = ((extIntercept - newIntercept) / (newSlope - extSlope));

        //交点が線分の範囲内か
        if ( isInSegment(x, min1, max1) && isInSegment(x, min2, max2) ) {

            //交点Y座標を計算
            float y = (newSlope * addPos.getX()) + newIntercept;

            //交点を返す
            return new Vec2(x, y);
        }

        //Log.i("outTest", "交差なし=" + x + "\t1X=" + min1 + "～" + max1 + "\t2X=" + min2+ "～" + max2);

        //線分内の交点なし
        return null;
    }
}