package com.example.onlyTouch;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * ライン情報
 */
public class LineInfo {

    //ライン数
    private int lineNum;
    //各ライン情報
    private List<OneLineData> lineList = new ArrayList<>();

    //コンストラクタ
    public LineInfo(){
        //ライン数
        this.lineNum = 0;
    }

    public int getLineNum() {
        return lineNum;
    }

    public void addLineData( OneLineData line ){
        lineList.add(line);
        lineNum++;
    }

    public List<OneLineData> getLineList(){
        return this.lineList;
    }

    /*
     * 自ラインとの交差数加算
     */
    public void IncrementCrossNumSelf( int lineID ){

        OneLineData lineData = this.lineList.get(lineID);

        //自ライン交差数を加算
        lineData.crossSelfNum++;

        //自ラインと交差していれば、領域の構成に関係ありとみなす
        lineData.isOutLine = true;

        Log.i("LineInfo", "myself isOutLine set true\t" + lineID);

        return;
    }

    /*
     * 他ラインとの交差数加算
     */
    public void IncrementCrossNumOther( int mylineID, int opplineID ){

        //自ラインの相手ライン交差数を更新
        OneLineData mylineData = this.lineList.get(mylineID);
        mylineData.crossOtherNum++;

        //他ラインの相手ライン交差数を更新
        OneLineData opplineData = this.lineList.get(opplineID);
        opplineData.crossOtherNum++;

        //他ラインと2か所以上で交差あり
        if( mylineData.crossOtherNum >= 2 ){
            //領域の構成に関係ありとみなす
            mylineData.isOutLine = true;

            Log.i("LineInfo", "other isOutLine set true\t" + mylineID);
        }
        //他ラインと2か所以上で交差あり
        if( opplineData.crossOtherNum >= 2 ){
            //領域の構成に関係ありとみなす
            opplineData.isOutLine = true;

            Log.i("LineInfo", "other isOutLine set true\t" + opplineID);
        }

        return;
    }

    /**
     * 輪郭線が1つでもあるか
     */
    public boolean hasOutLine(){

        for( OneLineData line: lineList ){

            //輪郭線である
            if( line.isOutLine ){
                //輪郭線あり
                return true;

            }
        }

        //輪郭線なし
        return false;
    }

    /*
     * 1ライン情報
     */
    public static class OneLineData{
        //ライン識別子
        private int lineID;
        //ライン先頭Index
        private int topIndex;
        //ライン終端Index
        private int lastIndex;
        //自ライン交差数
        private int crossSelfNum;
        //他ライン交差数
        private int crossOtherNum;
        /**
         * 輪郭線である可能性があるか否か
         *    自ラインとの交差あり or 他ラインとの交差が2つ以上あり ⇒ true
         */
        private boolean isOutLine;

        //コンストラクタ
        public OneLineData(int id, int topIndex, int lastIndex ){
            this.lineID    = id;
            this.topIndex  = topIndex;
            this.lastIndex = lastIndex;

            this.crossSelfNum = 0;
            this.crossOtherNum = 0;

            //輪郭線かどうか（初期値：輪郭線ではない）
            this.isOutLine = false;
        }

        //ライン識別子の取得
        public int getLineID() {
            return lineID;
        }

        //ライン識別子の設定
        public void setLineID(int lineID) {
            this.lineID = lineID;
        }

        public int getTopIndex() {
            return topIndex;
        }

        public int getLastIndex() {
            return lastIndex;
        }

        public boolean isOutline() {
            return isOutLine;
        }
    }
}
