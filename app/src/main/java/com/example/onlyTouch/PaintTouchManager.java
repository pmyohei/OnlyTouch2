package com.example.onlyTouch;

import android.util.Log;

import com.google.fpl.liquidfun.Vec2;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class PaintTouchManager {

    /* LiquidFun ロード */
    static {
        System.loadLibrary("liquidfun");
        System.loadLibrary("liquidfun_jni");
    }

    enum OVER_WRITE_MODE{
        CLEAR,        //
        SKIP,       //
        NEED        //
    };

    //ライン情報リスト
    private ArrayList<PaintLineInfo> mPaintLinesInfo = new ArrayList<PaintLineInfo>();

    //ペイントタッチ情報リスト(一時リスト)
    private ArrayList<PaintTouchInfo> mtmpPaintTouchInfo = new ArrayList<PaintTouchInfo>();

    //ペイントタッチ交点情報リスト
    private ArrayList<PaintCrossInfo> mPaintCrossInfo = new ArrayList<PaintCrossInfo>();

    //閉領域識別子（整数で管理）
    //0  ：閉領域なし
    //1～：閉領域識別子
    private int mClosingArea = 1;

    //ライン識別子
    private int mLineIdentifier = 0;

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
    public PaintTouchManager() {
        //this.mPrevClosingArea = PREV_NEW_CLOSED_AREA;
    }

    //ペイント情報の成形
    // para： ペイントしたラインの情報
    //
    public void moldingPaintInfo(ArrayList<PaintLineInfo> lineInfo ) {

        //ライン情報リスト
        this.mPaintLinesInfo = lineInfo;

        //描画したライン数分ループ
        int lineIndex = 0;
        for( PaintLineInfo line: mPaintLinesInfo){

            //dbg
            dbg_targetLine = lineIndex;
            //

            //ラインのタッチ情報リストを取得
            ArrayList<PaintTouchInfo> touchList = line.getTouchInfo();

            //タッチ座標数分ループ（一番初めのタッチ情報は処理不要）
            int touchNum = touchList.size();
            for( int touchIndex = 1; touchIndex < touchNum; touchIndex++ ){

                //dbg
                dbg_targetIndex = touchIndex;
                //

                //タッチ情報(1点)取得
                PaintTouchInfo touchPoint = touchList.get(touchIndex);

                Log.i("molding", "Index=" + touchIndex);

                //位置情報を取得
                Vec2 pos     = touchPoint.getTouchPos();
                Vec2 pre_pos = touchList.get(touchIndex - 1).getTouchPos();

                //傾きと切片を算出する
                calcSlopeIntercept(pos, pre_pos, touchPoint);

                //閉領域の判定
                checkClosingArea_new(pos, pre_pos, touchPoint.getSlope(), touchPoint.getIntercept(), lineIndex, touchIndex);
            }

            //対象のライン数を次へ
            lineIndex++;
        }

        //dbg
        int linenum = this.mPaintLinesInfo.size();
        for( int i = 0; i < linenum; i++ ){
            PaintLineInfo linetmp = this.mPaintLinesInfo.get(i);

            ArrayList<PaintTouchInfo> touch = linetmp.getTouchInfo();
            int count = touch.size();
            for (int j = 0; j < count; j++) {

                PaintTouchInfo test_info = touch.get(j);
                //Log.i("PaintInfo", j + "\tTimming=" + test_info.getEvent());
                Log.i("PaintInfo", "\tLine=" + i + "\tIndex=" + j + "\tArea=\t"  + test_info.getCloseArea() +
                           "\t" + test_info.getTouchPos().getX() + "\t" + test_info.getTouchPos().getY());
                Log.i("PaintInfo", j + "\tslope\t" + test_info.getSlope() + "\tintercept\t" + test_info.getIntercept());
            }
        }

        for (int i = 0; i < mPaintCrossInfo.size(); i++) {
            PaintCrossInfo test_info = this.mPaintCrossInfo.get(i);
            Log.i("cross", i + "\tpos\tx\t" + test_info.getPoint().getX() + "\ty\t" + test_info.getPoint().getY());
            Log.i("cross", i + "\tmyIndex\t" + test_info.getMyIndex() + "\toppsiteIndex\t" + test_info.getOppositeIndex());
            Log.i("cross", i + "\tmyLine\t" + test_info.getMyLineIndex() + "\toppsiteLine\t" + test_info.getOppositeLineIndex());
        }
        //dbg
    }

    //ペイント情報を追加
    // para：新規座標
    // para：タッチタイミング
    /*
    public void addPaintInfo(Vec2 pos, PaintTouchInfo.TOUCH_TIMMING timmming) {

        //渡された情報でペイントタッチ情報を生成
        PaintTouchInfo info = new PaintTouchInfo(pos, timmming);

        //タッチ開始以外
        if (timmming != PaintTouchInfo.TOUCH_TIMMING.START) {

            //現在保持している最後の点を保持
            int last         = this.mtmpPaintTouchInfo.size() - 1;
            Vec2 pre_add_pos = this.mtmpPaintTouchInfo.get(last).getTouchPos();

            //dbg
            dbg_new_pos = pre_add_pos;
            dbg_pre_pos = pos;

            dbg_pre_index = last;
            //dbg

            //傾きと切片を算出する
            float slope[]     = new float[1];
            float intercwpt[] = new float[1];
            calcSlopeIntercept(pos, pre_add_pos, slope, intercwpt);

            //算出した値を設定
            info.setSlope(slope[0]);
            info.setIntercept(intercwpt[0]);

            //閉領域の判定
            //int closearea = checkClosingArea(pos, pre_add_pos, slope[0], intercwpt[0] );
            checkClosingArea(pos, pre_add_pos, slope[0], intercwpt[0]);

            //所属する閉領域を設定
            //info.setCloseArea(closearea);
        }

        //ペイントタッチ情報を追加
        this.mtmpPaintTouchInfo.add(info);

        //dbg
        if (timmming == PaintTouchInfo.TOUCH_TIMMING.END) {

            int count = this.mtmpPaintTouchInfo.size();
            for (int i = 0; i < count; i++) {

                PaintTouchInfo test_info = this.mtmpPaintTouchInfo.get(i);
                Log.i("PaintInfo", i + "\tTimming=" + test_info.getEvent());
                Log.i("PaintInfo", i + "\tArea="  + test_info.getCloseArea() +
                                                  "\tpos\tx\t" + test_info.getTouchPos().getX() + "\ty\t" + test_info.getTouchPos().getY());
                Log.i("PaintInfo", i + "\tslope\t" + test_info.getSlope() + "\tintercept\t" + test_info.getIntercept());
            }

            for (int i = 0; i < mPaintCrossInfo.size(); i++) {
                PaintCrossInfo test_info = this.mPaintCrossInfo.get(i);
                Log.i("cross", i + "\tpos\tx\t" + test_info.point.getX() + "\ty\t" + test_info.point.getY());
                Log.i("cross", i + "\tIndex\tcrossed\t" + test_info.getOppositeIndex() + "\taddnew\t" + test_info.getMyIndex());
            }
        }
        //dbg

        //タッチ終了なら、ライン情報を生成する。
        if( timmming == PaintTouchInfo.TOUCH_TIMMING.END ){
            //ライン情報を追加
            PaintLineInfo line = new PaintLineInfo( mLineIdentifier, mtmpPaintTouchInfo );
            mPaintLineInfo.add(line);

            //タッチ点のリストはクリア
            mtmpPaintTouchInfo.clear();

            //識別子を更新
            mLineIdentifier++;
        }
    }
     */


    /**
     * 閉領域の判定・設定を行う
     *   para：判定対象の座標位置、判定対象直前の座標の位置
     *   para：傾き、切片
     *   para：判定対象のラインIndex、判定対象のタッチIndex(判定する範囲)
     */
    private void checkClosingArea_new(Vec2 pos, Vec2 pre_pos, float new_slope, float new_intercept, int targetLine, int targetTouch) {

        //初期ラインの場合、Indexが3以上でないと判定しない(2以下は交差しようがないため)
        if( targetLine == 0 && targetTouch < 3 ){
            return;
        }

        //X座標の範囲（新規座標～直前座標）
        float pos_x      = pos.getX();
        float pre_pos_x  = pre_pos.getX();
        float new_firstx = Math.min(pos_x, pre_pos_x);
        float new_lastx  = Math.max(pos_x, pre_pos_x);

        //所属しているライン情報までループ
        //（既存のラインから所属ラインまで、交差があるかチェック）
        for( int lineIndex = 0; lineIndex <= targetLine; lineIndex++ ){

            //ライン情報取得
            PaintLineInfo  lineInfo             = mPaintLinesInfo.get(lineIndex);
            //ラインを構成するタッチリストを取得
            ArrayList<PaintTouchInfo> touchInfo = lineInfo.getTouchInfo();

            //ライン毎に判定対象範囲を設定
            int max;
            if( lineIndex == targetLine ){
                //判定座標が所属するラインなら、その対象のIndexの2つ前まで
                max = targetTouch - 2;
            }else{
                //既存ラインなら、全タッチ座標（判定対象ラインが保持する座標の数）
                max = touchInfo.size();
            }

            //交差判定
            //（先頭のタッチ情報には、線分がないため、2点目からループ開始）
            for (int touchIndex = 1; touchIndex < max; touchIndex++) {

                //タッチ情報を保持（チェック対象とその直前）
                PaintTouchInfo info     = touchInfo.get(touchIndex);
                PaintTouchInfo pre_info = touchInfo.get(touchIndex - 1);

                //dbg
                dbg_front_pos = pre_info.getTouchPos();
                dbg_back_pos  = info.getTouchPos();

                dbg_front_index = touchIndex - 1;
                dbg_back_index  = touchIndex;
                //dbg

                //傾き・切片を取得
                float slope     = info.getSlope();
                float intercept = info.getIntercept();

                //X座標の範囲（交点判定対象の2点間）
                float x    = info.getTouchPos().getX();
                float prex = pre_info.getTouchPos().getX();

                float firstx = Math.min(x, prex);
                float lastx  = Math.max(x, prex);

                //交点算出
                Vec2 cross = calcCrossData(pos, new_slope, new_intercept, slope, intercept);
                if ( cross == null ) {
                    //交点ないなら、次のチェックへ
                    continue;
                }

                //交点が線分の範囲内かチェック（線分同士の交差かチェック）
                int area = checkLineInRange(cross.getX(), new_firstx, new_lastx, firstx, lastx, lineIndex, touchIndex);
                if (area == -1) {
                    //線分内で交わってないなら、次のチェックへ
                    continue;
                }

                Log.i("TRACE", "交差ありと判定 " + targetTouch + " " + touchIndex + " max=" + max);

                //交点情報をリストに追加(第3引数は、判定対象)
                addCrossData(cross.getX(), cross.getY(), touchIndex, targetTouch, targetLine, lineIndex);

                //閉領域が発生したか判定する
                boolean isClossed = isGenetateClosingArea(targetLine, lineIndex);
                if( isClossed ){
                    //閉領域の設定
                    setClosingArea(touchIndex, targetTouch, area, targetLine);

                    //交差判定終了
                    break;
                }

                //閉領域が出来ていないと判定されれば、次へ
                Log.i("isClossed", "交点追加時、閉領域なし");
            }
        }

        return;
    }

    /**
     * 閉領域の判定を行う
     */
    /*
    private int checkClosingArea(Vec2 add_pos, Vec2 pre_add_pos, float new_slope, float new_intercept) {

        //判定結果の初期値は「閉領域なし」
        int result = 0;

        float add_pos_x     = add_pos.getX();           //今追加された座標
        float pre_add_pos_x = pre_add_pos.getX();       //直前に追加された座標

        //X座標の範囲（新規座標～直前座標）
        float new_beginx = Math.min(add_pos_x, pre_add_pos_x);
        float new_endx   = Math.max(add_pos_x, pre_add_pos_x);

        //交点
        float cross_x[] = new float[1];
        float cross_y[] = new float[1];

        //タッチ済みの座標と交わっているかを判定
        //（2点目から線分情報があるため、2点目からループ開始）
        //(直前に追加された線分とは判定させないため、上限を-1する)
        int count = this.mtmpPaintTouchInfo.size() - 1;
        for (int i = 1; i < count; i++) {

            PaintTouchInfo info     = this.mtmpPaintTouchInfo.get(i);
            PaintTouchInfo pre_info = this.mtmpPaintTouchInfo.get(i - 1);

            //dbg
            dbg_front_pos = pre_info.getTouchPos();
            dbg_back_pos  = info.getTouchPos();

            dbg_front_index = i - 1;
            dbg_back_index  = i;
            //dbg

            //傾き・切片を取得
            float slope     = info.getSlope();
            float intercept = info.getIntercept();

            //X座標の範囲（交点判定対象の2点間）
            float x    = info.getTouchPos().getX();
            float prex = pre_info.getTouchPos().getX();

            float beginx = Math.min(x, prex);
            float endx   = Math.max(x, prex);

            //交点算出
            boolean ret = calcCrossData(add_pos, new_slope, new_intercept, slope, intercept, cross_x, cross_y);
            if (!ret) {
                //交点ないなら、次のチェックへ
                continue;
            }

            //交点が線分の範囲内かチェック（線分同士の交差かチェック）
            int area = checkLineInRange(cross_x[0], new_beginx, new_endx, beginx, endx, i);
            if (area == -1) {
                //線分内で交わってないなら、次のチェックへ
                continue;
            }

            //交点情報をリストに追加(第3引数は、最新の点に割り当てられる予定のIndex)
            addCrossData(cross_x[0], cross_y[0], i, count + 1);

            //閉領域を設定
            setClosingArea(i, count, area);

            //交点見つかっている状態のため、交差判定終了
            break;
        }

        return result;
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
        if (isRange(crossX, newFirstX, newEndX) && isRange(crossX, FirstX, endX)) {
            //交点が既に閉領域か
            area = this.mPaintLinesInfo.get(lineIndex).getTouchInfo().get(touchIndex).getCloseArea();
        }

        return area;
    }


    /**
     * 閉領域の設定
     *   para
     *     交差先タッチ座標のIndex
     *     判定中Index（最新交差の交差Index）
     *     交差した座標の閉領域識別子
     *     交差元の所属ライン(チェック中のラインIndex)
     */
    private void setClosingArea(int crossingIndex, int targetIndex, int area, int targetLine) {

        //dbg
        Log.i("trace", "setClosingArea 対象ライン=" + dbg_targetLine + "\t対象インデックス=" + dbg_targetIndex + "\t交点数=" + dbg_crossNum);
        //

        //今回の交点(最新交点)が自ラインとの交点かどうか
        boolean isCrossedMyLine = this.isLatestCrossedMyLine();

        //直前の交点に対して、今回の交点が前か後か
        boolean isFront = this.isFrontBack(isCrossedMyLine, targetLine);

        //設定する閉領域識別子を取得
        int setArea = this.getSetClosingAreaIdentifier(crossingIndex, area, isFront, targetLine);

        //閉領域識別子を最新の交点情報に設定
        this.setLatestCrossArea(setArea);

        //設定終了Index（自ライン用：判定対象Indexの1つ前までが設定範囲）
        int end = targetIndex - 1;

        //設定開始Index
        int first;
        if( isCrossedMyLine ){
            //交差先が自ライン

            //閉領域を開始するのは、交差先Indexから
            first = crossingIndex;

            Log.i("setClosingArea", "同ライン 開始Index=" + first + "\t終了Index=" + targetIndex);

            Log.i("【閉領域設定】【新規ライン】", "ライン=" + targetLine + "\t開始Index=\t" + first + "\t終了Index=\t" + targetIndex + "\t閉領域=" + setArea + "\tfront=" + isFront);

            //閉領域の設定
            this.setClosingAreaOnLine(first, end, setArea, targetLine);

        }else{
            //交差先が他ライン

            //設定開始Indexを取得(新規点用)
            first = getSetClosingAreaFirstIndexAnotherLine(crossingIndex, area, isFront, targetLine);

            Log.i("【閉領域設定】【新規ライン】", "ライン=" + targetLine + "\t開始Index=\t" + first + "\t終了Index=\t" + targetIndex + "\t閉領域=" + setArea + "\tfront=" + isFront);

            /*--- 判定中ラインに対する閉領域設定 ---*/

            //閉領域の設定→現在判定中ライン
            this.setClosingAreaOnLine(first, end, setArea, targetLine);

            /*--- 交差先ラインに対する閉領域設定 ---*/

            //交差先ラインの設定開始
            int oppsiteLineFirst, oppsiteLineEnd;
            if( isFront ){
                //今回の交点（最新交点）が直前交点よりも、前の部分で交差

                //設定開始Index → 最新交点の交差先Index
                oppsiteLineFirst = getLatestCrossOppsiteIndex();

                //設定終了Index → 『「交差先ラインに対して直前に交差した交点」の交差Index』の1つ前のIndex
                oppsiteLineEnd = getCrossIndexFromPrevCrossing( targetLine) - 1;

            }else{
                //最新交点が直前交点よりも、後の部分で交差している

                //設定開始Index → 交差先ラインに対して、直前に交差した交点の交差Index
                oppsiteLineFirst = getCrossIndexFromPrevCrossing( targetLine );

                //設定終了Index → 最新交点の交差先Index の一つ前のIndex
                oppsiteLineEnd = getLatestCrossOppsiteIndex() - 1;
            }

            //今回の交差の交差先ライン情報を保持
            int oppsiteLine = getLatestCrossLineIndex();

            //閉領域の設定→交差先ライン
            this.setClosingAreaOnLine(oppsiteLineFirst, oppsiteLineEnd, setArea, oppsiteLine);

            //既存点に対する閉領域の設定
            //setClosingAreaExistingPointAnotherLine(setArea, isFront, targetLine);
        }
    }

    /**
     * 閉領域識別子の取得
     *   設定すべき閉領域識別子を取得する。
     *   識別子は、直前に発生した交点が、閉領域と交わっていた場合、その閉領域識別子を取得する。
     *
     *   return
     *     設定する閉領域識別子
     */
    private int getSetClosingAreaIdentifier(int foundIndex, int area, boolean isFront, int targetLine) {

        int set_area;

        //閉領域の発生に関して
        if (area >= 1) {
            //既存の閉領域と交差

            //設定する閉領域は、交差先の閉領域識別子
            set_area = area;

            Log.i("Trace", "getSetClosingAreaIdentifier1");

            //最新交点に種別を設定
            setLatestCrossAreaKind(PaintCrossInfo.CLOSING_KIND.EXISTING);
        } else {
            //閉領域なしのラインと交差

            //直前交点の閉領域種別を取得
            PaintCrossInfo.CLOSING_KIND kind = getPrevCrossAreaKind();

            if(isFront){
                //直前交点よりも前で交差
                Log.i("Trace", "getSetClosingAreaIdentifier2");

                //既存閉領域判定・取得
                int existingArea = isExistingCloseAreaExistPoint(foundIndex, targetLine);

                if( existingArea == -1 ){
                    //既存閉領域なし

                    //新規閉領域が設定値
                    set_area = mClosingArea;
                    //次回用に更新
                    mClosingArea++;

                    Log.i("Trace", "getSetClosingAreaIdentifier 3 kind=" + kind);

                    //最新交点に種別を設定
                    setLatestCrossAreaKind(PaintCrossInfo.CLOSING_KIND.NEW);

                }else{
                    //既存閉領域あり

                    //その既存閉領域の識別子が設定値
                    set_area = existingArea;

                    //最新交点に種別を設定
                    setLatestCrossAreaKind(PaintCrossInfo.CLOSING_KIND.EXISTING);
                }

            }else{
                //直前交点よりも後で交差

                //新規の識別子を取得
                set_area = mClosingArea;
                //次回用に更新
                mClosingArea++;

                Log.i("Trace", "getSetClosingAreaIdentifier 3 kind=" + kind);

                //最新交点に種別を設定
                setLatestCrossAreaKind(PaintCrossInfo.CLOSING_KIND.NEW);
            }
        }

        Log.i("閉領域", "設定する閉領域=" + set_area);

        return set_area;
    }

    /**
     * 閉領域設定
     *   設定開始Index
     *   設定終了Index
     *   設定する閉領域識別子
     *　　 設定対象のライン
     */
    private void setClosingAreaOnLine(int first, int end, int setArea, int setLine ) {

        //上書きされる閉領域識別子リスト
        List<Integer> overWriteList = new ArrayList<Integer>();

        //上書きモード
        OVER_WRITE_MODE overWriteMode = OVER_WRITE_MODE.CLEAR;

        Log.i("setClosingAreaOnMyLine\t", "setArea=" + setArea + "------------------------------");

        //設定対象ラインのタッチリスト
        ArrayList<PaintTouchInfo> touchList = this.mPaintLinesInfo.get(setLine).getTouchInfo();

        //指定された範囲の閉領域の設定
        for (int i = first; i <= end; i++) {

            //設定対象のタッチ情報
            PaintTouchInfo setTarget = touchList.get(i);

            //現在設定されている閉領域
            int hasArea = setTarget.getCloseArea();

            Log.i("setClosingAreaOnMyLine", "\tIndex=" + i + "\thasArea" + hasArea + "\tsetArea=" + setArea + "\toverWriteMode=" + overWriteMode);

            if( hasArea == 0 ){
                /*--- 閉領域未設定の点 ---*/

                //閉領域設定
                setTarget.setCloseArea(setArea);

                //上書きモードクリア
                overWriteMode = OVER_WRITE_MODE.CLEAR;

            }else if( hasArea == setArea ){
                /*--- 閉領域設定済みの点（識別子同じ） ---*/

                //上書きモードクリア
                overWriteMode = OVER_WRITE_MODE.CLEAR;

            }else{
                /*--- 閉領域設定済みの点（別の識別子） ---*/

                //上書きモードに応じた処理
                if( overWriteMode == OVER_WRITE_MODE.CLEAR ){
                    /*--- 上書きモードクリア中 ---*/

                    //閉領域を上書きする必要があるか判定
                    boolean needsLast = needsOverwriteClosingArea(i, hasArea, touchList, setLine);
                    if(needsLast){
                        //上書き必要
                        overWriteMode = OVER_WRITE_MODE.NEED;

                        //閉領域上書き
                        setTarget.setCloseArea(setArea);

                    }else{
                        //上書き不要
                        overWriteMode = OVER_WRITE_MODE.SKIP;
                    }

                }else if( overWriteMode == OVER_WRITE_MODE.NEED ){
                    /*--- 上書き必要判定中 ---*/

                    //閉領域上書き
                    setTarget.setCloseArea(setArea);

                }else{
                    /*--- 上書き不要判定中(SKIP) ---*/

                    //上書きなし
                }


                /*--- 別ラインの閉領域識別子も更新 ---*/
                //更新のために、上書きされる閉領域識別子を保存
                overWriteList.add(hasArea);
            }
        }

        /*--- 別ラインの閉領域識別子も更新 ---*/
        //重複を削除したリストを生成
        List<Integer> list = new ArrayList<Integer>(new HashSet<>(overWriteList));

        //設定済み閉領域識別子
        for( int preArea : list ){
            this.renewClosingArea(preArea, setArea, setLine);
        }
    }

    /**
     * 閉領域が既に設定されているタッチ情報を、別の閉領域に変更する
     *   変更対象の閉領域識別子
     *   変更後の閉領域識別子
     *   変更ラインの上限（このラインの直前まで設定範囲）
     */
    private void renewClosingArea(int preArea, int setArea, int setLine) {

        /*--- タッチ情報の更新 ---*/

        //指定ラインの直前までが変更範囲
        for( int i = 0; i < setLine; i++ ){
            PaintLineInfo lineInfo = this.mPaintLinesInfo.get(i);

            //ラインのタッチ座標リスト
            ArrayList<PaintTouchInfo> touch = lineInfo.getTouchInfo();
            for( PaintTouchInfo target : touch ){

                //変更対象の閉領域識別子が設定されていれば、更新する
                if (target.getCloseArea() == preArea){
                    target.setCloseArea(setArea);
                }
            }
        }

        /*--- 交点情報の更新 ---*/
        for( PaintCrossInfo crossInfo: mPaintCrossInfo ){
            //変更対象の閉領域識別子が設定されていれば、更新する
            if (crossInfo.getArea() == preArea){
                crossInfo.setArea(setArea);
            }
        }
    }

    /**
     * 閉領域の設定（既存座標）：判定中のラインに対しての設定用
     *   追加済みの点に対して、閉領域を設定する。
     *   設定する範囲
     *     開始Index=最新交点の交差先Index
     *     終了Index=直前交点の交差先Index
     *     ※ただし、閉領域が既に設定されている点には設定しない
     *
     *   para
     *     ・設定する閉領域識別子
     *     ・最新交点が、直前交点よりも前か（前ならture）
     *     ・判定中のライン
     */
    private void setClosingAreaExistingPointSameLine(int area, int targetLine) {

        /*
        //閉領域の発生が初めてなら、何もする必要なし
        if( area == 1 ){
            //return;
        }
         */

        //現在判定中のライン情報を保持
        ArrayList<PaintTouchInfo> touchList = this.mPaintLinesInfo.get(targetLine).getTouchInfo();

        //設定開始Index(最新交点の交差先Index)
        int first = getLatestCrossOppsiteIndex();

        //直前交点の交差先Indexまで繰り返し
        int last = getPrevCrossOppsiteIndex();

        Log.i("【閉領域設定】【既存ライン】【同ライン】", "ライン=" + targetLine + "\t開始Index=" + first + "\t終了Index=" + last + "\t閉領域=" + area);

        for( ; first < last; first++ ){

            //設定対象のタッチ座標を取得
            PaintTouchInfo setInfo = touchList.get(first);

            //閉領域の設定あり
            if( setInfo.getCloseArea() > 0 ){
                //閉領域が既に設定されていれば、何もせず次の点へ
                continue;
            }

            //閉領域が設定されていない点に対して、設定する
            setInfo.setCloseArea(area);
        }
    }

    /**
     * 閉領域の設定（既存座標）：交差先のラインに対しての設定用
     *   追加済みの点に対して、閉領域を設定する。
     *   設定する範囲
     *     開始Index=最新交点の交差先Index
     *     終了Index=直前交点の交差先Index
     *     ※ただし、閉領域が既に設定されている点には設定しない
     */
    /*
    private void setClosingAreaExistingPointAnotherLine(int area, boolean front, int targetLine) {

        int first, last;
        if( front ){
            //最新交点が直前交点よりも、前の部分で交差している

            //設定開始Indexは、最新交点の交差先Index
            first = getLatestCrossOppsiteIndex();

            //設定終了Indexは、交差先ラインに対して、直前に交差した交点の交差Index
            last = getNewestCrossOppositeIndexSameLine( targetLine );
        }else{
            //最新交点が直前交点よりも、後の部分で交差している

            //設定開始Indexは、交差先ラインに対して、直前に交差した交点の交差Index
            first = getNewestCrossOppositeIndexSameLine( targetLine );

            //設定終了Indexは、最新交点の交差先Index
            last = getLatestCrossOppsiteIndex();
        }

        //交差先ライン情報を保持
        int oppsiteLine = getLatestCrossLineIndex();
        ArrayList<PaintTouchInfo> touchList = this.mPaintLinesInfo.get(oppsiteLine).getTouchInfo();

        Log.i("【閉領域設定】【既存ライン】【異ライン】", "ライン=" + oppsiteLine + "\t開始Index=" + first + "\t終了Index=" + last + "\t閉領域=" + area + "\tfront=" + front);

        //閉領域の設定
        for( ; first < last; first++ ){

            //設定対象のタッチ座標を取得
            PaintTouchInfo setInfo = touchList.get(first);

            //閉領域の設定がない点に対してのみ、設定する
            if( setInfo.getCloseArea() == 0 ){
                setInfo.setCloseArea(area);
            }
        }
    }
    */

    /**
     * 既存閉領域の有無判定
     *   指定されたラインの交点情報に、既存閉領域があるかどうかを判定し、
     *   あればその識別子を返す。
     *
     *   return
     *     -1以外：見つかった既存閉領域識別子
     *     -1　　　：既存閉領域なし
     */
    private int isExistingCloseAreaExistPoint( int foundIndex, int targetLine ) {

        //直前交点のIndex
        int prevIndex = this.mPaintCrossInfo.size() - 2;

        //交差先Indexから直前交点まで探索
        for( int crossIndex = foundIndex; crossIndex <= prevIndex; crossIndex++ ){

            PaintCrossInfo info = this.mPaintCrossInfo.get(crossIndex);

            //交差した際の自ラインを取得
            int myLine = info.getMyLineIndex();
            if( myLine == targetLine ){

                //既存閉領域があれば、その閉領域識別子を返す
                if( info.getClosingKind() == PaintCrossInfo.CLOSING_KIND.EXISTING ){
                    return info.getArea();
                }
            }
        }

        return -1;
    }


    /**
     * 閉領域上書き判定
     *   閉領域が設定済みの点に関して、上書きすべきか判定する
     *
     *   para
     *      設定済みの中での最初のIndex
     *      設定済み閉領域識別子
     *
     */
    private boolean needsOverwriteClosingArea(int baseFirst, int baseHasArea, ArrayList<PaintTouchInfo> touchList, int targetLine ) {

        //必ず更新される
        int end = 0;

        int size = touchList.size();

        //「閉領域が設定されている最後の点」の次の点を探す
        for (int i = baseFirst + 1; i < size; i++) {

            PaintTouchInfo info = touchList.get(i);

            int hasArea = info.getCloseArea();
            Log.i("hasArea=" + hasArea, "Index=" + i);
            if( hasArea != baseHasArea ){
                //異なる閉領域が見つかれば、終了
                end = i;
                break;
            }
        }

        //必ず更新される
        int crossOppsiteIndex = 0;

        //交差元Indexとして、見つかった値を持つ交点情報を取得
        for( PaintCrossInfo crossInfo: this.mPaintCrossInfo ){

            //交差元Indexが、判定中ラインの交点
            if( crossInfo.getMyLineIndex() == targetLine ){

                //閉領域が閉じられた時の交点あり
                if( crossInfo.getMyIndex() == end ){

                    //その交点の交差先Indexを取得
                    crossOppsiteIndex = crossInfo.getOppositeIndex();
                }
            }
        }

        boolean ret;
        if( crossOppsiteIndex == baseFirst ){
            //交差先Indexが、閉領域設定開始Indexと一致

            //上書き不要
            ret = false;
        }else{
            //交差先Indexが、閉領域設定開始Indexと一致しない

            //上書き必要
            ret = true;
        }

        Log.i("上書き判定結果=" + ret, "baseFirst=" + baseFirst + "\tend=" + end + "\tcheckArea=" + baseHasArea + "\tcrossOppsiteIndex=" + crossOppsiteIndex);

        return ret;
    }

    /**
     * 直前交点との前後判定
     *   最新交点が、直前交点よりも前にあるかを判定する
     *
     *   return
     *     true ：前にある
     *     false：後にあるor初回交差
     */
    private boolean isFrontBack(boolean isCrossedMyLine, int targetLine ) {

        //初回交差なら、false
        int size = this.mPaintCrossInfo.size();
        if(size == 1){
            return false;
        }

        //最新の交差情報
        int latest = size - 1;
        PaintCrossInfo latestInfo  = this.mPaintCrossInfo.get(latest);

        //直近交点の交差情報
        PaintCrossInfo prevInfo;
        if( isCrossedMyLine ){
            //交差先が自ライン
            prevInfo = this.mPaintCrossInfo.get(latest - 1);
        }else{
            //交差先が他ライン

            //他ラインとの交差の中で、一番直近の交点Indexを取得
            int prevIndex = getMostRecentCrossedIndexOppsiteLine(targetLine);
            prevInfo      = this.mPaintCrossInfo.get(prevIndex);
        }

        //最新の交点と直前の交点が、同じライン上にないなら、この判定自体が不要
        //(交点情報が持つ交差先のラインが同じか判定)
        /* 多分不要 　ここに来た時点で、既存の交差はあるため  2020.09.26
        if( latestInfo.getOppositeLineIndex() != prevInfo.getOppositeLineIndex() ){
            return false;
        }
        */

        //最新交点と交わっている既存の座標が、直前の交点よりも前か
        if( latestInfo.getOppositeIndex() <= prevInfo.getOppositeIndex() - 1 ){
            //前判定
            return true;
        }

        return false;
    }

    /**
     * 最新の交点が、同じラインとの交差かどうかを判定する
     *
     *   return
     *     true ：自ラインと交差
     *     false:他ラインと交差
     */
    private boolean isLatestCrossedMyLine() {

        //交差情報が一つのみなら、初回交差のため終了
        int size = this.mPaintCrossInfo.size();
        if(size == 0){
            //ここはフェールセーフのため、実際に通ることはない
            return false;
        }

        //最新の交差情報のIndex
        int latest = size - 1;
        PaintCrossInfo newInfo  = this.mPaintCrossInfo.get(latest);

        return newInfo.isCrossedMyLine();
    }

    /**
     * 最新の交点と直前の交点が、同ライン上にあるかを判定する
     *
     *   return
     *     true ：同じライン上にある
     *     false:別のライン上にある
     */
    private boolean isLatestPrevCrossSameLine() {

        //交差情報が一つのみなら、初回交差のため終了
        int size = this.mPaintCrossInfo.size();
        if(size < 2 ){
            //ここはフェールセーフのため、実際に通ることはない
            return false;
        }

        //最新の交差情報のIndex
        int latest = size - 1;
        PaintCrossInfo newInfo  = this.mPaintCrossInfo.get(latest);
        PaintCrossInfo prevInfo = this.mPaintCrossInfo.get(latest - 1);

        if( newInfo.getOppositeLineIndex() == prevInfo.getOppositeLineIndex() ){
            //最新も直前も、交差先が同じライン
            return true;
        }else{
            return false;
        }
    }

    /**
     * 閉領域を設定する開始Indexを取得する
     * （交差した座標が、判定対象と同ラインのとき用＝その交点が自身のライン内で完結しているとき用）
     *
     *   para
     *     交差した既存点のIndex
     *     交差した既存点に設定された閉領域
     *     直前の交点よりも前かどうか
     *
     *   return
     *     交差した既存点のIndex or 前回の交点
     */
    private int getSetClosingAreaFirstIndexSameLine(int foundIndex, int area, boolean front){

        PaintCrossInfo.CLOSING_KIND prevKind = getPrevCrossAreaKind();

        //最新=新規閉領域 かつ　直前=新規閉領域 （新規閉領域が連続発生）
        if( ( area == 0) && ( prevKind == PaintCrossInfo.CLOSING_KIND.NEW )){

            //最新交点が直前交点よりも後
            if (!front) {
                //設定開始Index=交差先Index
                return foundIndex;
            }
        }

        //上記以外は、設定開始Index=直前交点の交差先Index

        int inter_num = this.mPaintCrossInfo.size();
        if( inter_num > 1 ){
            //最新の一つ前の交点(今回追加された交点の直前の交点)から、次に設定を開始するIndexを取得
            return getPrevCrossMyIndex();
        }

        return foundIndex;
    }

    /**
     * 閉領域を設定する開始Indexを取得する
     * （交差した座標が、判定対象と別ラインのとき用＝その交点が別のラインと交わっているとき用）
     *
     *   para
     *     交差した既存点のIndex
     *     交差した既存点に設定された閉領域
     *     直前の交点よりも前かどうか
     *     判定対象中のライン
     *
     *   return
     *     交差した既存点のIndex or 前回の交点
     */
    private int getSetClosingAreaFirstIndexAnotherLine(int foundIndex, int area, boolean front, int targetLine){

        int setStartIndex = foundIndex;

        //最新交点と直前交点のどちらも、交差先のラインが同じであるかどうか
        boolean sameLine = isLatestPrevCrossSameLine();
        if( sameLine ){
            //直前の交差先ラインと同じ

            //設定開始Indexは、直前の交点の判定対象Index
            setStartIndex = getPrevCrossMyIndex();

        }else{
            //直前の交差先ラインと違う

            //判定対象が所属するラインの交点情報の中に、最新交点が交差したラインがあるか
            int latestCrossIndex = getMostRecentCrossedIndexOppsiteLine( targetLine );

            /*
            //なければ、閉領域の設定そのものが不要
            if( latestCrossIndex == 0 ){

                Log.i("不要処理チェック", "このルートは通らないはず");

                //戻り値に、閉領域不要を設定
                return -1;
            }
             */

            //あれば、その過去の交点Indexの中で一番新しいものから設定していく
            setStartIndex = this.mPaintCrossInfo.get(latestCrossIndex).getMyIndex();
        }

        return setStartIndex;
    }


    /**
     * 初交差判定
     *   最新交点の交差先ラインとの交差が初であるかを判定する。
     *
     *  　para
     *      判定中のラインIndex
     *   return
     *      一番最近そのライン（直近交点の交差先ライン）と交差した交差情報Index
     */
    private int getMostRecentCrossedIndexOppsiteLine(int targetLine ){

        //直前交差の交差先ラインを取得
        int latestIndex       = this.mPaintCrossInfo.size() - 1;
        int oppositeLineIndex = this.mPaintCrossInfo.get(latestIndex).getOppositeLineIndex();

        //判定結果のIndex  ※確実に更新されるため、0では返らない
        int retIndex = 0;

        //交差情報数 - 2 回ループ（直近と最新の交点は除く）
        int max = latestIndex - 1;
        for( int crossIndex = 0; crossIndex < max; crossIndex++ ){

            PaintCrossInfo info = this.mPaintCrossInfo.get(crossIndex);

            //交差判定した時の対象のラインを取得
            int myLine = info.getMyLineIndex();
            if( myLine != targetLine ){
                //現在判定中のラインと同じでないなら、確認対象外
                continue;
            }

            //このルートに来た場合は、現在判定中のラインと同じため、
            //次に交差先のラインIndexを確認する

            //既存交点の交差先のラインIndex
            int old = info.getOppositeLineIndex();
            if( old == oppositeLineIndex ){
                //直近交点の交差先ラインと同じ場合は、そのIndexで戻り値を更新
                retIndex = crossIndex;
            }
        }

        return retIndex;
    }

    /**
     *  指定ラインに対して、「直前交点の交差先Index」を取得
     */
    private int getCrossIndexFromPrevCrossing(int targetLine) {

        //直前交差の交差先ラインを取得
        int latestIndex       = this.mPaintCrossInfo.size() - 1;
        int oppositeLineIndex = this.mPaintCrossInfo.get(latestIndex).getOppositeLineIndex();

        //判定結果のIndex  ※確実に更新されるため、0では返らない
        int retIndex = 0;

        //交差情報数 - 2 回ループ（直近と最新の交点は除く）
        //int max = latestIndex - 1;
        //直前交点まで確認
        //int max = latestIndex;
        for( int crossIndex = 0; crossIndex < latestIndex; crossIndex++ ){

            PaintCrossInfo info = this.mPaintCrossInfo.get(crossIndex);

            //交差判定した時の対象のラインを取得
            int myLine = info.getMyLineIndex();
            if( myLine == targetLine ){
                /*--- その交点の所属ラインは、現在判定中のラインと同じ場合、次に交差先のラインIndexを確認する ---*/

                //交差先のラインIndex
                if( info.getOppositeLineIndex() == oppositeLineIndex ){
                    //直近交点の交差先ラインと同じ場合は、そのIndexで戻り値を更新
                    retIndex = crossIndex;
                }
            }
        }

        //交差先Index の一つ手前のIndexを返す
        return this.mPaintCrossInfo.get(retIndex).getOppositeIndex();
    }

    /**
     * 直前交差情報が持つ、判定対象のラインIndexを取得
     */
    private int getPrevCrossMyLineIndex() {

        //直前の交差情報のIndex
        int prev = this.mPaintCrossInfo.size() - 2;

        return this.mPaintCrossInfo.get(prev).getMyLineIndex();
    }

    /**
     * 直前交差情報が持つ、判定対象のIndexを取得
     */
    private int getPrevCrossMyIndex() {

        //直前の交差情報のIndex
        int prev = this.mPaintCrossInfo.size() - 2;

        return this.mPaintCrossInfo.get(prev).getMyIndex();
    }

    /**
     * 最新交差情報が持つ、交差相手の座標を取得
     */
    private int getLatestCrossOppsiteIndex() {

        //最新の交差情報のIndex
        int latest = this.mPaintCrossInfo.size() - 1;

        return this.mPaintCrossInfo.get(latest).getOppositeIndex();
    }

    /**
     * 最新交差情報が持つ、交差先のラインIndexを取得
     */
    private int getLatestCrossLineIndex() {

        //最新の交差情報のIndex
        int latest = this.mPaintCrossInfo.size() - 1;

        return this.mPaintCrossInfo.get(latest).getOppositeLineIndex();
    }

    /**
     * 直前交差情報が持つ、交差相手の座標を取得
     */
    private int getPrevCrossOppsiteIndex() {

        //直前の交差情報のIndex
        int prev = this.mPaintCrossInfo.size() - 2;

        return this.mPaintCrossInfo.get(prev).getOppositeIndex();
    }

    /**
     * 最新交差情報に、閉領域識別子を設定
     */
    private void setLatestCrossArea( int setArea ) {

        int latest = this.mPaintCrossInfo.size() - 1;
        this.mPaintCrossInfo.get(latest).setArea(setArea);
    }

    /**
     * 最新交差情報に、閉領域種別を設定
     */
    private void setLatestCrossAreaKind( PaintCrossInfo.CLOSING_KIND kind ) {

        int latest = this.mPaintCrossInfo.size() - 1;
        this.mPaintCrossInfo.get(latest).setClosingKind(kind);
    }

    /**
     * 直近交差情報の、閉領域種別を取得
     */
    private PaintCrossInfo.CLOSING_KIND getPrevCrossAreaKind() {

        int latest = this.mPaintCrossInfo.size() - 2;

        //直近交点なし
        if(latest < 0){
            //新規閉領域を返す
            return PaintCrossInfo.CLOSING_KIND.NEW;
        }

        return this.mPaintCrossInfo.get(latest).getClosingKind();
    }

    /**
     * 直近交差情報の、閉領域識別子を取得
     */
    private int getPrevCrossArea() {

        int prev = this.mPaintCrossInfo.size() - 2;
        return this.mPaintCrossInfo.get(prev).getArea();
    }

    /**
     * 交差情報の追加
     */
    private void addCrossData(float cross_x, float cross_y, int target, int end, int myLineIndex, int oppositeLineIndex) {

        //最新交点追加までの交点数
        int sizeBeforeAdd = this.mPaintCrossInfo.size();

        //dbg
        dbg_crossNum = sizeBeforeAdd + 1;
        //

        //範囲内とみなし、交点情報として保持
        Vec2 pos = new Vec2(cross_x, cross_y);

        PaintCrossInfo info = new PaintCrossInfo(pos, myLineIndex, oppositeLineIndex);
        info.setOppositeIndex(target);                //交差した線分を持つIndex（既存の点）
        info.setMyIndex(end);                         //交差した線分を持つIndex（判定対象としている点）

        this.mPaintCrossInfo.add(info);
    }

    /**
     * 閉領域発生判定
     *   閉領域が発生したかどうかを判定する
     *
     *   return
     *     true　：発生あり
     *     false：発生なし
     */
    private boolean isGenetateClosingArea(int myLineIndex, int oppositeLineIndex) {

        //判定中ラインが1本目なら、閉領域は確実にできる
        if( myLineIndex == 0 ){
            return true;
        }

        /*** 以降は、2本目以降の判定中ラインに対する処理 ***/

        //最新交点追加前の交点数
        int prevSize = this.mPaintCrossInfo.size() - 1;

        //全体として交差検出が初の場合
        if(prevSize == 0){
            Log.i("trace", "addCrossData sizeBeforeAdd 対象ライン=" + dbg_targetIndex + "\t対象インデックス=" + dbg_targetIndex + "\t交点数=" + dbg_crossNum);

            if( myLineIndex == oppositeLineIndex ){
                //初めての交差が自ラインなら、閉領域あり
                return true;
            }else{
                //初めての交差が自ラインなら、閉領域なし
                return false;
            }
        }

        //今回交差したラインと、過去に交差したことがあるかチェックする
        //(追加済みの交差情報をチェックしていく)
        for( int i = 0; i < prevSize; i++ ){
            PaintCrossInfo crossInfo = this.mPaintCrossInfo.get(i);

            //交点の所属ラインが、現在判定中のラインと同じであり、
            //かつ、交差先も同じ
            //今回交差したラインに対して、交差した情報があれば、閉領域発生
            if(    crossInfo.getMyLineIndex()       == myLineIndex
                && crossInfo.getOppositeLineIndex() == oppositeLineIndex ){
                return true;
            }
        }

        Log.i("trace", "addCrossData last 対象ライン=" + dbg_targetIndex + "\t対象インデックス=" + dbg_targetIndex + "\t交点数=" + dbg_crossNum);

        //このルートにくるのは、以下のケース
        //・判定対象ラインに関して、交点を初めて検出 or
        //・今回交差したラインとは、初めての交差
        return false;
    }

    /**
     * 範囲判定
     */
    private boolean isRange(float value, float first, float last) {
        return (value >= first && value <= last);
    }

    /**
     * 指定された閉領域識別子を持つ「PaintTouchInfo」を返す。
     */
    private PaintTouchInfo findTouchInfoHasClosingArea(int area) {

        for (PaintTouchInfo info : this.mtmpPaintTouchInfo) {
            //指定した閉領域識別子が見つかればそれを返す
            if (info.isHasClosingArea(area)) {
                return info;
            }
        }

        //なければnull
        return null;
    }

    /**
     * 指定された閉領域識別子を持つ最新の「交点情報」を返す。
     *
     */
    private PaintCrossInfo findInterInfoHasClosingArea(int area) {

        //リストの後ろから検索していく（一番新しい情報を取得するため）
        for(int i = this.mPaintCrossInfo.size() - 1; i >= 0; i-- ){
            PaintCrossInfo info = this.mPaintCrossInfo.get(i);
            if( info.isHasClosingArea(area)){
                return info;
            }
        }

        return null;
    }

    //2点を通る傾きと切片を算出する。
    private void calcSlopeIntercept(Vec2 pos1, Vec2 pos2, PaintTouchInfo touchPoint) {

        float x1 = pos1.getX();
        float x2 = pos2.getX();
        float y1 = pos1.getY();
        float y2 = pos2.getY();


        //傾き・切片
        float slope     = ((y2 - y1) / (x2 - x1));
        float intercept = y1 - (slope * x1);

        touchPoint.setSlope(slope);
        touchPoint.setIntercept(intercept);
    }

    /**
     * 交点の算出
     * 新規追加された座標
     * 新規線分の傾き、新規線分の切片、既存線分の傾き、既存線分の切片
     */
    private boolean calcCrossData(Vec2 add_pos,
                                  float new_slope, float new_intercept, float past_slope, float past_intercept,
                                  float cross_x[], float cross_y[]) {

        //傾きが平行しているなら、交点そのものがなし
        if (new_slope == past_slope) {
            return false;
        }

        //交点x座標
        // 線分１、線分２
        // ２の切片 - １の切片　／　１の傾き - ２の傾き
        cross_x[0] = ((past_intercept - new_intercept) / (new_slope - past_slope));

        //交点Y座標
        cross_y[0] = (new_slope * add_pos.getX()) + new_intercept;

        return true;
    }

    /**
     * 交点の算出
     * 新規追加された座標
     * 新規線分の傾き、新規線分の切片、既存線分の傾き、既存線分の切片
     */
    private Vec2 calcCrossData(Vec2 add_pos, float new_slope, float new_intercept, float past_slope, float past_intercept) {

        //傾きが平行しているなら、交点そのものがなし
        if (new_slope == past_slope) {
            return null;
        }

        //交点x座標
        // 線分１、線分２
        // ２の切片 - １の切片　／　１の傾き - ２の傾き
        float x = ((past_intercept - new_intercept) / (new_slope - past_slope));

        //交点Y座標
        float y = (new_slope * add_pos.getX()) + new_intercept;

        //交点を返す
        return new Vec2(x, y);
    }

}