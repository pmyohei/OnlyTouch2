package com.example.onlyTouch.particle;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;

import com.google.fpl.liquidfun.ParticleGroupDef;

import org.xmlpull.v1.XmlPullParser;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

/*
 * ポリゴンdata管理
 */
public class PolygonListDataManager {

    /* plist 最大値・最小値 */
    // 128PX
//     private static final float PLIST_MIN_128PX = -64.5f;
//     private static final float PLIST_MAX_128PX = 64.5f;
//     private static final float PLIST_LENGTH_128PX = 129.0f;
    // 150PX
//     private static final float PLIST_MIN_128PX = -75.5f;
//     private static final float PLIST_MAX_128PX = 75.5f;
//     private static final float PLIST_LENGTH_128PX = 151.0f;
    private static final float PLIST_MIN_128PX = -75f;
    private static final float PLIST_MAX_128PX = 75f;
    private static final float PLIST_LENGTH_128PX = 150.0f;

    // PoligonXML から取得した図形情報
    public class PolygonXmlData {
        public ByteBuffer mCoordinateBuff;     // 図形の座標バッファ
        public ByteBuffer mVertexeNumBuff;     // 図形毎の頂点数
        public int        mShapeNum;           // 図形の数

        public PolygonXmlData(ByteBuffer coordinateBuff, ByteBuffer vertexeNumBuff, int shapeNum ){
            mCoordinateBuff = coordinateBuff;
            mVertexeNumBuff = vertexeNumBuff;
            mShapeNum = shapeNum;
        }
    }

    // 図形情報とポリゴンxmlの対応リスト
    private final HashMap<Integer, PolygonXmlData> mMapPolygon;


    /* UV座標 */
    private float UvMinX;           // plistの座標上で、最小の値をUV座標に変換したもの(x座標)
    private float UvWidth;          // plistの座標上で、最大横幅をUV座標の単位にしたもの(y座標)
    private float UvMaxY;           // plistの座標上で、最小の値をUV座標に変換したもの(y座標)
    private float UvHeight;         // plistの座標上で、最大横幅をUV座標の単位にしたもの(y座標)

    // 座標調整値
    private static final int LEVELING_SHAPE_SIZE_VALUE = 8;  // 最大長を必ずこの値とする


    public PolygonListDataManager(){
        // 初期値はUV座標全体に画像がある場合とする
        this.UvMinX   = 0.0f;
        this.UvMaxY   = 1.0f;
        this.UvWidth  = 1;
        this.UvHeight = 1;

        // 図形情報とポリゴンxmlの対応リスト
        mMapPolygon = new HashMap<Integer, PolygonXmlData>();
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
     *　形状設定
     *　　指定されたPoligonXMLから図形情報を解析。
     * 　 解析した図形データをParticleGroupDefにパーティクルの形状として設定する。
     */
    public PolygonXmlData parsePoligonXmlShapes(Context context, int polygonXml){

        // 解析済みであれば、解析結果を返して終了
        PolygonXmlData polygonXmlData = mMapPolygon.get(polygonXml);
        if (polygonXmlData != null) {
            return polygonXmlData;
        }

        //----------------------
        // 解析
        //----------------------
        ArrayList<Float> vertexCoordinateValue = new ArrayList<Float>();
        ArrayList<Integer> vertexNumValue = new ArrayList<Integer>();
        int shapeNum = -1;


        // 解析対象のタグ
        final String TAG_FIXTURE      = "fixture";
        final String TAG_POLYGON      = "polygon";
        final String TAG_VERTEX       = "vertex";
        final String ATTR_NUMPOLYGONS = "numPolygons";
        final String ATTR_NUMVERTEXES = "numVertexes";
        final String ATTR_VERTEX_X    = "x";
        final String ATTR_VERTEX_Y    = "y";

        // 解析処理
        XmlResourceParser parser = context.getResources().getXml(polygonXml);
        try {
            // イベントタイプ
            int eventType;
            eventType = parser.getEventType();

            //--------------------
            // ファイル終了まで
            //--------------------
            while (eventType != XmlPullParser.END_DOCUMENT) {

                //---------------
                // 開始タグまで移動
                //---------------
                // 開始タグをみつけるまで、なにもしない
                eventType = parser.next();
                if(eventType != XmlPullParser.START_TAG) {
                    continue;
                }

                //---------------------------
                // fixtureタグ：図形数情報の取得
                //---------------------------
                if( parser.getName().equals( TAG_FIXTURE ) ){
                    // 図形数の取得
                    int attrIntValue = parser.getAttributeIntValue(null, ATTR_NUMPOLYGONS, -1);

                    // 取得に失敗した場合は、パース終了
                    if( attrIntValue == -1 ){
                        return null;
                    }

                    // 取得できたら次の解析へ
                    shapeNum = attrIntValue;
                    continue;
                }

                //-----------------------------------
                // polygonタグ：各図形の頂点数情報の取得
                //-----------------------------------
                if( parser.getName().equals(TAG_POLYGON) ) {
                    // 図形の頂点数の取得
                    int attrIntValue = parser.getAttributeIntValue(null, ATTR_NUMVERTEXES, -1);
                    if( attrIntValue == -1 ){
                        // 取得に失敗した場合は、パース終了
                        return null;
                    }

                    // リストに格納し、次の解析へ
                    vertexNumValue.add(attrIntValue);
                    continue;
                }

                //---------------------------
                // vertexタグ：頂点座標情報の取得
                //---------------------------
                if( parser.getName().equals( TAG_VERTEX ) ) {
                    //--------------
                    // x座標
                    //--------------
                    float attrFloatValue = parser.getAttributeFloatValue(null, ATTR_VERTEX_X, 0xFFFF);
                    if( attrFloatValue == 0xFFFF ){
                        // 取得に失敗した場合は、パース終了
                        return null;
                    }

                    // リストに格納
                    vertexCoordinateValue.add(attrFloatValue);

                    //--------------
                    // y座標
                    //--------------
                    attrFloatValue = parser.getAttributeFloatValue(null, ATTR_VERTEX_Y, 0xFFFF);
                    if( attrFloatValue == 0xFFFF ){
                        // 取得に失敗した場合は、パース終了
                        return null;
                    }

                    // 上下反転してリストに格納　※xmlでは、座標が上下反転した状態で出力されるため
                    attrFloatValue *= (-1);
                    vertexCoordinateValue.add(attrFloatValue);
                }
            }
        } catch (Exception e) {
            Log.d("XmlPullParserSample", "Error");
        }


        // xml上の座標値を標準化する
        levelingPlist( vertexCoordinateValue );


        // 座標をバッファに格納
        // ！allocate()では落ちるため、注意！
        // ！リトルエンディアン指定すること！
        ByteBuffer vertexes = ByteBuffer.allocateDirect(Float.SIZE * vertexCoordinateValue.size());
        vertexes.order(ByteOrder.LITTLE_ENDIAN);
        for( Float pos: vertexCoordinateValue ){
            vertexes.putFloat(pos);
            // Log.d("XmlPullParserSample", "pos=" + pos);
        }

        // 各図形の頂点数をバッファに格納
        ByteBuffer vertexesNum = ByteBuffer.allocateDirect(Integer.SIZE * vertexNumValue.size());
        vertexesNum.order(ByteOrder.LITTLE_ENDIAN);
        for( Integer num: vertexNumValue ){
            vertexesNum.putInt(num);
            // Log.d("XmlPullParserSample", "num=" + num);
        }

        // 生成済みとする
        PolygonXmlData newPolygonXmlData = new PolygonXmlData( vertexes, vertexesNum, shapeNum );
        mMapPolygon.put( polygonXml, newPolygonXmlData);

        return newPolygonXmlData;
    }

    /*
     *　座標値を一定のサイズになるよう平準化する。
     *  @para 座標配列
     */
    private void levelingPlist(ArrayList<Float> vertexCoordinateValue ){

        //---------------------------------------------
        // 必ず更新されるよう、座標としてありえない値を初期値とする
        //---------------------------------------------
        float xMin = 0xFFFF;
        float xMax = -(0xFFFF);
        float yMin = 0xFFFF;
        float yMax = -(0xFFFF);

        /* 最大値と最小値を見つける */
        int num = vertexCoordinateValue.size();
        for(int i = 0; i < num; i++){
            float value = vertexCoordinateValue.get(i);

            // 座標内の最小値と最大値を保持する
            if( (i % 2) == 0 ){
                // X座標
                xMin = (Math.min(value, xMin));
                xMax = (Math.max(value, xMax));
            }else{
                // Y座標
                yMin = (Math.min(value, yMin));
                yMax = (Math.max(value, yMax));
            }
        }



        // 図形の最大幅と最大高さ
        float width = xMax - xMin;
        float height = yMax - yMin;

        /* サイズを平準化するための値を算出 */
        float levelingX, levelingY;
        if( width >= height ){
            levelingX = LEVELING_SHAPE_SIZE_VALUE / width;
            levelingY = levelingX;
        } else {
            levelingY = LEVELING_SHAPE_SIZE_VALUE / height;
            levelingX = levelingY;
        }

        /* 座標値を平準化 */
        for (int i = 0; i < num; i++){
            float value = vertexCoordinateValue.get(i);

            if( (i % 2) == 0 ){
                // X座標
                vertexCoordinateValue.set(i, value * levelingX);
            }else{
                // Y座標
                vertexCoordinateValue.set(i, value * levelingY);
            }
        }

        // UV座標の情報を保持する
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

        // X・Y座標上で最小の値を、UV座標に変換
        this.UvMinX  = (minX - PLIST_MIN_128PX) / PLIST_LENGTH_128PX;
        float uvMinY = (minY - PLIST_MIN_128PX) / PLIST_LENGTH_128PX;

        // X・Y座標上で最大の値を、UV座標に変換
        float uvMaxX = (minX + width  - PLIST_MIN_128PX) / PLIST_LENGTH_128PX;
        this.UvMaxY  = (minY + height - PLIST_MIN_128PX) / PLIST_LENGTH_128PX;

        // UV座標上の最大幅・高さ
        this.UvWidth  = uvMaxX - UvMinX;
        this.UvHeight = UvMaxY - uvMinY;
    }
}
