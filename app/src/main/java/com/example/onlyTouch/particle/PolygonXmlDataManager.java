package com.example.onlyTouch.particle;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

/*
 * Polygon xmlデータ管理
 */
public class PolygonXmlDataManager {

    // テクスチャサイズ情報 px単位
    private float mTexturePXMinX;
    private float mTexturePXMinY;
    private float mTexturePXWidth;
    private float mTexturePXHeight;

    //-----------------------------
    // PoligonXML 解析データ
    //   PoligonXML から解析した図形情報
    //-----------------------------
    public class PolygonParseData {
        public ByteBuffer mCoordinateBuff;     // 図形の座標バッファ
        public ByteBuffer mVertexeNumBuff;     // 図形毎の頂点数
        public int        mShapeNum;           // 図形の数

        public PolygonParseData(ByteBuffer coordinateBuff, ByteBuffer vertexeNumBuff, int shapeNum ){
            mCoordinateBuff = coordinateBuff;
            mVertexeNumBuff = vertexeNumBuff;
            mShapeNum = shapeNum;
        }
    }

    //-----------------------------
    // UV座標
    //   解析した図形に対応するUV座標データ
    //-----------------------------
    private float mUVMinX;           // xmlの座標上で、最小の値をUV座標に変換したもの(x座標)
    private float mUVMaxY;           // xmlの座標上で、最大の値をUV座標に変換したもの(y座標)
    private float mUVWidth;          // xmlの座標上で、最大widthをUV座標の単位にしたもの
    private float mUVHeight;         // xmlの座標上で、最大heightをUV座標の単位にしたもの

    // 座標縮小値
    // !この値が大きい程、パーティクルも大きくなる
    private static final int LEVELING_SHAPE_SIZE_VALUE = 8;

    // 図形情報とポリゴンxmlの対応リスト
    private final HashMap<Integer, PolygonParseData> mMapPolygon;


    /*
     * コンストラクタ
     */
    public PolygonXmlDataManager( Context context, int polygonXml, int textureID ){
        //--------------
        // UV座標
        //--------------
        // 初期値はUV座標全体に画像がある場合とする
        mUVMinX = 0.0f;
        mUVMaxY = 1.0f;
        mUVWidth = 1;
        mUVHeight = 1;

        //--------------
        // リスト
        //--------------
        // 図形情報とポリゴンxmlの対応リスト
        mMapPolygon = new HashMap<Integer, PolygonParseData>();


        // テクスチャ情報の設定
        setTextureData( context, textureID );
        // 解析処理
        parsePoligonXmlShapes( context, polygonXml );

    }



    /*
     *　形状設定
     *　　指定されたPoligonXMLから図形情報を解析。
     * 　 解析した図形データをParticleGroupDefにパーティクルの形状として設定する。
     */
    public PolygonParseData parsePoligonXmlShapes(Context context, int polygonXml){

        // 解析済みであれば、解析結果を返して終了
        PolygonParseData polygonXmlData = mMapPolygon.get(polygonXml);
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
        //!エラー対応不十分
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


        // xml上の座標値を縮小する
        downsizingVertexCoordinate( vertexCoordinateValue );

        //----------------------
        // ByteBuffer 格納
        //----------------------
        // 座標をバイトバッファに格納
        // ！allocate()では落ちるため、注意！
        // ！リトルエンディアン指定すること！
        ByteBuffer vertexes = ByteBuffer.allocateDirect(Float.SIZE * vertexCoordinateValue.size());
        vertexes.order(ByteOrder.LITTLE_ENDIAN);
        for( Float pos: vertexCoordinateValue ){
            vertexes.putFloat(pos);
        }

        // 各図形の頂点数をバイトバッファに格納
        ByteBuffer vertexesNum = ByteBuffer.allocateDirect(Integer.SIZE * vertexNumValue.size());
        vertexesNum.order(ByteOrder.LITTLE_ENDIAN);
        for( Integer num: vertexNumValue ){
            vertexesNum.putInt(num);
        }

        //----------------------
        // リストに保持
        //----------------------
        // 生成済みとする
        PolygonParseData newPolygonXmlData = new PolygonParseData( vertexes, vertexesNum, shapeNum );
        mMapPolygon.put( polygonXml, newPolygonXmlData);

        return newPolygonXmlData;
    }

    /*
     *　座標値の縮小
     *   xmlの座標値は間隔が広いため、一定比率縮小する
     */
    private void downsizingVertexCoordinate(ArrayList<Float> vertexCoordinateValue ){

        //-----------------------
        // 座標の最大値／最小値の算出
        //-----------------------
        // 先頭の値を暫定の最大値／最小値とする
        float minX = vertexCoordinateValue.get(0);
        float maxX = vertexCoordinateValue.get(0);
        float minY = vertexCoordinateValue.get(1);
        float maxY = vertexCoordinateValue.get(1);

        // 最大値と最小値の更新
        int num = vertexCoordinateValue.size();
        for(int i = 2; i < num; i++){
            float value = vertexCoordinateValue.get(i);

            // 座標内の最小値と最大値の更新
            if( (i % 2) == 0 ){
                // X座標
                minX = (Math.min(value, minX));
                maxX = (Math.max(value, maxX));
            }else{
                // Y座標
                minY = (Math.min(value, minY));
                maxY = (Math.max(value, maxY));
            }
        }

        //--------------------
        // 図形の最大幅と最大高さ
        //--------------------
        float width  = maxX - minX;
        float height = maxY - minY;

        //--------------------
        // 座標値の縮小
        //--------------------
        // 縮小率の算出
        float longerSide = Math.max( width, height );
        float rate       = LEVELING_SHAPE_SIZE_VALUE / longerSide;

        // 座標を縮小する
        for (int i = 0; i < num; i++){
            float value = vertexCoordinateValue.get(i);

            if( (i % 2) == 0 ){
                // X座標
                vertexCoordinateValue.set(i, value * rate);
            }else{
                // Y座標
                vertexCoordinateValue.set(i, value * rate);
            }
        }

        //--------------------
        // UV座標の情報を保持
        //--------------------
        setUVData(minX, minY, width, height);
    }

    /*
     *　UV座標の以下の情報を保持する。
     *  ・UV座標上での最小値（X座標）
     *  ・UV座標上での最大値（Y座標）
     *  ・UV座標上での最大幅
     *  ・UV座標上での最大高さ
     */
    private void setUVData(float minX, float minY, float width, float height){

        // X・Y座標上で最小の値を、UV座標に変換
        float uvMinX = (minX - mTexturePXMinX) / mTexturePXWidth;
        float uvMinY = (minY - mTexturePXMinY) / mTexturePXHeight;

        // X・Y座標上で最大の値を、UV座標に変換
        float uvMaxX = (minX + width  - mTexturePXMinX) / mTexturePXWidth;
        float uvMaxY = (minY + height - mTexturePXMinY) / mTexturePXHeight;

        // 保持
        mUVMinX = uvMinX;
        mUVMaxY = uvMaxY;

        // UV座標上の最大幅・高さ
        mUVWidth = uvMaxX - uvMinX;
        mUVHeight = uvMaxY - uvMinY;
    }

    /*
     *　テクスチャサイズ情報の設定
     */
    private void setTextureData( Context context, int textureID ){

        // 指定リソースのBitmapオブジェクトを生成
        Resources resource = context.getResources();
        Bitmap bitmap = BitmapFactory.decodeResource(resource, textureID);

        final float bitmapWidth = bitmap.getWidth();
        final float bitmapHeight = bitmap.getHeight();
        final float scale = resource.getDisplayMetrics().density;

        //----------------------------
        // テクスチャ（画像）のpxサイズ
        //----------------------------
        // 画像のピクセルサイズ
        mTexturePXWidth = bitmapWidth / scale;
        mTexturePXHeight = bitmapHeight / scale;
        // 画像の中央を原点とした時の画像の最小位置
        mTexturePXMinX = (mTexturePXWidth / 2.0f) * -1;
        mTexturePXMinY = (mTexturePXHeight / 2.0f) * -1;
    }


    /*
     * setter/getter
     */
    public float getUvMinX() {
        return mUVMinX;
    }
    public float getUvWidth() {
        return mUVWidth;
    }
    public float getUvMaxY() {
        return mUVMaxY;
    }
    public float getUvHeight() {
        return mUVHeight;
    }
}
