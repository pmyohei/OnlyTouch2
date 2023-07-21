package com.example.onlyTouch.particle;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;

import com.example.onlyTouch.R;
import com.google.fpl.liquidfun.ParticleGroupDef;

import org.xmlpull.v1.XmlPullParser;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

/*
 * ポリゴンdata管理
 */
public class PolygonListDataManager {

    /* plist最大値・最小値 */
    //128PX
    private static final float PLIST_MIN_128PX = -64.5f;
    private static final float PLIST_MAX_128PX = 64.5f;
    private static final float PLIST_LENGTH_128PX = 129.0f;

    /* UV座標 */
    private float UvMinX;           //plistの座標上で、最小の値をUV座標に変換したもの(x座標)
    private float UvWidth;          //plistの座標上で、最大横幅をUV座標の単位にしたもの(y座標)
    private float UvMaxY;           //plistの座標上で、最小の値をUV座標に変換したもの(y座標)
    private float UvHeight;         //plistの座標上で、最大横幅をUV座標の単位にしたもの(y座標)

    //座標調整値
    private static final int LEVELING_SHAPE_SIZE_VALUE = 8;  //最大長を必ずこの値とする

    //plistファイル指定
    public enum PLIST_KIND{
        PLIST_RABBIT,
        PLIST_CAT
    }

    public PolygonListDataManager(){
        //初期値はUV座標全体に画像がある場合とする
        this.UvMinX   = 0.0f;
        this.UvMaxY   = 1.0f;
        this.UvWidth  = 1;
        this.UvHeight = 1;
    }

    public float getUvMinX() {
        return UvMinX;
    }
    public float getUvWidth() {
        return UvWidth;
    }
    public float getUvMaxY() {
        return UvMaxY;
    }
    public float getUvHeight() {
        return UvHeight;
    }

    /*
     *　指定されたplistファイルから、図形生成に必要な情報を取得し、指定されたバッファに格納する。
     * @para   頂点座標バッファ
     * @para   図形頂点数バッファ
     * @para   指定plist
     * @return 図形数
     */
    public int setPlistBuffer(Context context, ParticleGroupDef pgd, PLIST_KIND kind){

        int shapeNum = -1;
        ArrayList<Integer> plistVerNums = new ArrayList<Integer>();
        ArrayList<Float> plistVer = new ArrayList<Float>();

        //plistの取得
        XmlPullParser parser = context.getResources().getXml(R.xml.star_test);

        try {
            int attr;
            float attrFloat;
            int eventType;

            eventType = parser.getEventType();
            //ファイル終了までループ
            while (eventType != XmlPullParser.END_DOCUMENT) {

                eventType = parser.next();
                if(eventType != XmlPullParser.START_TAG) {
                    //開始タグをみつけるまで、なにもしない
                    continue;
                }

                //fixtureタグ
                if( parser.getName().equals("fixture") ){
                    //図形数の取得
                    attr = ((XmlResourceParser) parser).getAttributeIntValue(null, "numPolygons", -1);

                    //取得に失敗した場合は、パース終了
                    if( attr == -1 ){
                        return -1;
                    }

                    //取得できたら次の解析へ
                    //Log.d("XmlPullParserSample", "numPolygons=" + attr);
                    shapeNum = attr;
                    continue;
                }

                //polygonタグ
                if( parser.getName().equals("polygon") ) {
                    //図形の頂点数の取得
                    attr = ((XmlResourceParser) parser).getAttributeIntValue(null, "numVertexes", -1);

                    //取得に失敗した場合は、パース終了
                    if( attr == -1 ){
                        return -1;
                    }

                    //Log.d("XmlPullParserSample", "numVertexes=" + attr);

                    //取得できたら次の解析へ
                    plistVerNums.add(attr);
                    continue;
                }

                //vertexタグ
                if( parser.getName().equals("vertex") ) {
                    //頂点座標の取得
                    attrFloat = ((XmlResourceParser) parser).getAttributeFloatValue(null, "x", 0xFFFF);

                    if( attrFloat == 0xFFFF ){
                        //取得に失敗した場合は、パース終了
                        return -1;
                    }
                    //Log.d("XmlPullParserSample", "x=" + attrFloat);
                    plistVer.add(attrFloat);

                    attrFloat = ((XmlResourceParser) parser).getAttributeFloatValue(null, "y", 0xFFFF);
                    if( attrFloat == 0xFFFF ){
                        //取得に失敗した場合は、パース終了
                        return -1;
                    }
                    //Log.d("XmlPullParserSample", "y=" + attrFloat);
                    attrFloat *= (-1);                                                  //上下反転。plistが上下反転した状態で出力されるため。
                    plistVer.add(attrFloat);
                }
            }
        } catch (Exception e) {
            Log.d("XmlPullParserSample", "Error");
        }

        //座標サイズを標準化する
        levelingPlist(plistVer);

        //！allocate()では落ちるため、注意！
        //！リトルエンディアン指定すること！

        //座標をバッファに格納
        ByteBuffer vertexes = ByteBuffer.allocateDirect(Float.SIZE * plistVer.size());
        vertexes.order(ByteOrder.LITTLE_ENDIAN);
        for( Float pos: plistVer ){
            vertexes.putFloat(pos);
            //Log.d("XmlPullParserSample", "pos=" + pos);
        }

        //各図形の頂点数をバッファに格納
        ByteBuffer vertexesNum = ByteBuffer.allocateDirect(Integer.SIZE * plistVerNums.size());
        vertexesNum.order(ByteOrder.LITTLE_ENDIAN);
        for( Integer num: plistVerNums ){
            vertexesNum.putInt(num);
            //Log.d("XmlPullParserSample", "num=" + num);
        }

        //plistの座標から図形を指定する
        pgd.setPolygonShapesFromVertexList( vertexes, vertexesNum, shapeNum );

        //dbg
        //int nums = pgd.getNums(0);
        //float nums1 = pgd.getPointsOne(0);
        //float nums2 = pgd.getPointsOne(1);

        return shapeNum;
    }

    /*
     *　座標値を一定のサイズになるよう平準化する。
     *  @para 座標配列
     */
    private void levelingPlist(ArrayList<Float> plistVer ){

        //必ず更新されるよう、ありえない値を初期値とする
        float xMin = 0xFFFF;
        float xMax = -(0xFFFF);
        float yMin = 0xFFFF;
        float yMax = -(0xFFFF);

        /* 最大値と最小値を見つける */
        int num = plistVer.size();
        for(int i = 0; i < num; i++){
            float value = plistVer.get(i);

            //座標内の最小値と最大値を保持する
            if( (i % 2) == 0 ){
                //X座標
                xMin = (Math.min(value, xMin));
                xMax = (Math.max(value, xMax));
            }else{
                //Y座標
                yMin = (Math.min(value, yMin));
                yMax = (Math.max(value, yMax));
            }
        }

        //図形の最大幅と最大高さ
        float width = xMax - xMin;
        float height = yMax - yMin;

        /* サイズを平準化するための値を算出 */
        float levelingX, levelingY;
        if( width > height ){
            levelingX = LEVELING_SHAPE_SIZE_VALUE / width;
            levelingY = levelingX;
        }else if(width < height){
            levelingY = LEVELING_SHAPE_SIZE_VALUE / height;
            levelingX = levelingY;
        }else{
            levelingX = LEVELING_SHAPE_SIZE_VALUE / width;
            levelingY = levelingX;
        }

        /* 座標値を平準化 */
        for (int i = 0; i < num; i++){
            float value = plistVer.get(i);

            if( (i % 2) == 0 ){
                //X座標
                plistVer.set(i, value * levelingX);
            }else{
                //Y座標
                plistVer.set(i, value * levelingY);
            }
        }

        //UV座標の情報を保持する
        this.setUVData(xMin, yMin, width, height);
    }

    /*
     *　UV座標の以下の情報を保持する。
     *  ・UV座標上での最小値（X座標）
     *  ・UV座標上での最大値（Y座標）
     *  ・UV座標上での最大幅
     *  ・UV座標上での最大高さ
     * @para
     */
    private void setUVData(float minX, float minY, float width, float height){

        //X・Y座標上で最小の値を、UV座標に変換
        this.UvMinX  = (minX - PLIST_MIN_128PX) / PLIST_LENGTH_128PX;
        float uvMinY = (minY - PLIST_MIN_128PX) / PLIST_LENGTH_128PX;

        //X・Y座標上で最大の値を、UV座標に変換
        float uvMaxX = (minX + width  - PLIST_MIN_128PX) / PLIST_LENGTH_128PX;
        this.UvMaxY  = (minY + height - PLIST_MIN_128PX) / PLIST_LENGTH_128PX;

        //UV座標上の最大幅・高さ
        this.UvWidth  = uvMaxX - UvMinX;
        this.UvHeight = UvMaxY - uvMinY;
    }
}
