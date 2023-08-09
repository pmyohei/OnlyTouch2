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
//    private float mTexturePXHalfX;
//    private float mTexturePXHalfY;
//    private float mTexturePXWidth;
//    private float mTexturePXHeight;

    //-----------------------------
    // PoligonXML 解析データ
    //   PoligonXML から解析した図形情報
    //-----------------------------
    public class PolygonParseData {
        //-----------------------------
        // xml解析データ
        //-----------------------------
        public ByteBuffer mCoordinateBuff;     // 図形の座標バッファ
        public ByteBuffer mVertexeNumBuff;     // 図形毎の頂点数
        public int mShapeNum;                  // 図形の数

        //-----------------------------
        // UV座標
        //   解析した図形に対応するUV座標データ
        //-----------------------------
        public float mUVMinX;               // xmlの座標上で、最小の値をUV座標に変換したもの(x座標)
        public float mUVMaxY;               // xmlの座標上で、最大の値をUV座標に変換したもの(y座標)
        public float mUVMaxWidth;           // xmlの座標上で、最大widthをUV座標の単位にしたもの
        public float mUVMaxHeight;          // xmlの座標上で、最大heightをUV座標の単位にしたもの

        public PolygonParseData(ByteBuffer coordinateBuff, ByteBuffer vertexeNumBuff, int shapeNum) {
            // xml解析データ
            mCoordinateBuff = coordinateBuff;
            mVertexeNumBuff = vertexeNumBuff;
            mShapeNum = shapeNum;

            // 初期値はUV座標全体に画像がある場合とする
            mUVMinX = 0.0f;
            mUVMaxY = 1.0f;
            mUVMaxWidth = 1;
            mUVMaxHeight = 1;
        }
    }

    //-----------------------------
    // 図形頂点の最大値／最小値 格納位置
    //-----------------------------
    private final int VERTEX_POS_MIN_X = 0;
    private final int VERTEX_POS_MIN_Y = 1;
    private final int VERTEX_POS_MAX_X = 2;
    private final int VERTEX_POS_MAX_Y = 3;

    // 座標縮小値
    // !この値が大きい程、パーティクルも大きくなる
    private static final int LEVELING_SHAPE_SIZE_VALUE = 10;

    // 図形情報とポリゴンxmlの対応リスト
    private final HashMap<Integer, PolygonParseData> mMapPolygon;


    /*
     * コンストラクタ
     */
    public PolygonXmlDataManager(Context context, int polygonXml, int textureID) {

        //--------------
        // リスト
        //--------------
        // 図形情報とポリゴンxmlの対応リスト
        mMapPolygon = new HashMap<Integer, PolygonParseData>();

        // テクスチャ情報の設定
//        setTextureData(context, textureID);
        // 解析処理
        parsePoligonXmlShapes(context, polygonXml);
    }

    /*
     *　テクスチャサイズ情報の設定
     */
    private void setTextureData(Context context, int textureID) {

/*        // 指定リソースのPixelサイズを取得
        Resources resource = context.getResources();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resource, textureID, options);

        //----------------------------
        // テクスチャ（画像）のpxサイズ
        //----------------------------
        // 画像のピクセルサイズ
        mTexturePXWidth = options.outWidth;
        mTexturePXHeight = options.outHeight;
        // 画像のピクセルサイズの半分
        mTexturePXHalfX = mTexturePXWidth / 2.0f;
        mTexturePXHalfY = mTexturePXHeight / 2.0f;*/
    }


    /*
     *　形状設定
     *　　指定されたPoligonXMLから図形情報を解析。
     * 　 解析した図形データをParticleGroupDefにパーティクルの形状として設定する。
     */
    public PolygonParseData parsePoligonXmlShapes(Context context, int polygonXml) {

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
        final String TAG_FIXTURE = "fixture";
        final String TAG_POLYGON = "polygon";
        final String TAG_VERTEX  = "vertex";
        final String ATTR_NUMPOLYGONS = "numPolygons";
        final String ATTR_NUMVERTEXES = "numVertexes";
        final String ATTR_VERTEX_X = "x";
        final String ATTR_VERTEX_Y = "y";

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
                if (eventType != XmlPullParser.START_TAG) {
                    continue;
                }

                //---------------------------
                // fixtureタグ：図形数情報の取得
                //---------------------------
                if (parser.getName().equals(TAG_FIXTURE)) {
                    // 図形数の取得
                    int attrIntValue = parser.getAttributeIntValue(null, ATTR_NUMPOLYGONS, -1);

                    // 取得に失敗した場合は、パース終了
                    if (attrIntValue == -1) {
                        return null;
                    }

                    // 取得できたら次の解析へ
                    shapeNum = attrIntValue;
                    continue;
                }

                //-----------------------------------
                // polygonタグ：各図形の頂点数情報の取得
                //-----------------------------------
                if (parser.getName().equals(TAG_POLYGON)) {
                    // 図形の頂点数の取得
                    int attrIntValue = parser.getAttributeIntValue(null, ATTR_NUMVERTEXES, -1);
                    if (attrIntValue == -1) {
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
                if (parser.getName().equals(TAG_VERTEX)) {
                    //--------------
                    // x座標
                    //--------------
                    float attrFloatValue = parser.getAttributeFloatValue(null, ATTR_VERTEX_X, 0xFFFF);
                    if (attrFloatValue == 0xFFFF) {
                        // 取得に失敗した場合は、パース終了
                        return null;
                    }

                    // リストに格納
                    vertexCoordinateValue.add(attrFloatValue);

                    //--------------
                    // y座標
                    //--------------
                    attrFloatValue = parser.getAttributeFloatValue(null, ATTR_VERTEX_Y, 0xFFFF);
                    if (attrFloatValue == 0xFFFF) {
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

        //------------------------
        // xml上の座標値を縮小する
        //------------------------
        // xmlの図形頂点情報から、最大値・最小値を取得
        float[] vertexMixMax = getMinMaxVertexCoordinate(vertexCoordinateValue);
        //!xmlの座標値は間隔が広い（このままパーティクルグループを作成すると大きいサイズとなってしまう）ため、一定比率縮小する
        downsizingVertexCoordinate(vertexCoordinateValue, vertexMixMax);

        //----------------------
        // ByteBuffer 格納
        //----------------------
        // 座標をバイトバッファに格納
        // ！allocate()では落ちるため、注意！
        // ！リトルエンディアン指定すること！
        ByteBuffer vertexes = ByteBuffer.allocateDirect(Float.SIZE * vertexCoordinateValue.size());
        vertexes.order(ByteOrder.LITTLE_ENDIAN);
        for (Float pos : vertexCoordinateValue) {
            vertexes.putFloat(pos);
        }

        // 各図形の頂点数をバイトバッファに格納
        ByteBuffer vertexesNum = ByteBuffer.allocateDirect(Integer.SIZE * vertexNumValue.size());
        vertexesNum.order(ByteOrder.LITTLE_ENDIAN);
        for (Integer num : vertexNumValue) {
            vertexesNum.putInt(num);
        }

        //----------------------
        // リストに保持
        //----------------------
        // 解析データを生成
        PolygonParseData newPolygonXmlData = new PolygonParseData(vertexes, vertexesNum, shapeNum);
        // UVデータを設定
        setUVData( newPolygonXmlData, vertexMixMax );
        mMapPolygon.put(polygonXml, newPolygonXmlData);

        return newPolygonXmlData;
    }

    /*
     *　座標値の縮小
     */
    private float[] getMinMaxVertexCoordinate(ArrayList<Float> vertexCoordinateValue) {

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
        for (int i = 2; i < num; i++) {
            float value = vertexCoordinateValue.get(i);

            // 座標内の最小値と最大値の更新
            if ((i % 2) == 0) {
                // X座標
                minX = (Math.min(value, minX));
                maxX = (Math.max(value, maxX));
            } else {
                // Y座標
                minY = (Math.min(value, minY));
                maxY = (Math.max(value, maxY));
            }
        }

        // 最大値・最小値を返す
        float[] data = new float[4];
        data[VERTEX_POS_MIN_X] = minX;
        data[VERTEX_POS_MIN_Y] = minY;
        data[VERTEX_POS_MAX_X] = maxX;
        data[VERTEX_POS_MAX_Y] = maxY;

        return data;
    }

    /*
     *　座標値の縮小
     */
    private void downsizingVertexCoordinate(ArrayList<Float> vertexCoordinateValue, float[] vertexMixMax) {

        //--------------------
        // 図形の最大幅と最大高さ
        //--------------------
        float polygonMaxWidth = vertexMixMax[VERTEX_POS_MAX_X] - vertexMixMax[VERTEX_POS_MIN_X];
        float polygonMaxHeight = vertexMixMax[VERTEX_POS_MAX_Y] - vertexMixMax[VERTEX_POS_MIN_Y];

        //--------------------
        // 座標値の縮小
        //--------------------
        // 縮小率の算出
        float longerSide = Math.max(polygonMaxWidth, polygonMaxHeight);
        float rate = LEVELING_SHAPE_SIZE_VALUE / longerSide;

        // 座標を縮小する
        int num = vertexCoordinateValue.size();
        for (int i = 0; i < num; i++) {
            float value = vertexCoordinateValue.get(i);

            if ((i % 2) == 0) {
                // X座標
                vertexCoordinateValue.set(i, value * rate);
            } else {
                // Y座標
                vertexCoordinateValue.set(i, value * rate);
            }
        }
    }

    /*
     *　UV情報の保持
     */
    private void setUVData( PolygonParseData polygonXmlData , float[] vertexMixMax ){

        //--------------------
        // UV座標
        //--------------------
        // 図形座標をUV座標に変換
        float[] uvMinPos = convertUVPos(vertexMixMax[VERTEX_POS_MIN_X], vertexMixMax[VERTEX_POS_MIN_Y]);
        float[] uvMaxPos = convertUVPos(vertexMixMax[VERTEX_POS_MAX_X], vertexMixMax[VERTEX_POS_MAX_Y]);

        // 必要なデータを保持
        polygonXmlData.mUVMinX = uvMinPos[0];
        polygonXmlData.mUVMaxY = uvMaxPos[1];
        // UV座標上の最大幅・高さ
        polygonXmlData.mUVMaxWidth = uvMaxPos[0] - uvMinPos[0];
        polygonXmlData.mUVMaxHeight = uvMaxPos[1] - uvMinPos[1];
    }

    /*
     * 図形座標をUV座標へ変換
     */
    private float[] convertUVPos(float posX, float posY){

        //--------------
        // 画像サイズ
        //--------------
        // !図形xmlは、150px * 150pxの画像から生成している
        final int TEXTURE_PX_WIDTH = 150;
        final int TEXTURE_PX_HEIGHT = 150;
        final float TEXTURE_PX_HALF_WIDTH = TEXTURE_PX_WIDTH / 2f;
        final float TEXTURE_PX_HALF_HEIGHT = TEXTURE_PX_HEIGHT / 2f;

        //--------------
        // UV座標変換
        //--------------
        // 図形座標をUV座標に変換
        float[] uvPos = new float[2];
        uvPos[0] = (posX + TEXTURE_PX_HALF_WIDTH) / TEXTURE_PX_WIDTH;
        uvPos[1] = (posY + TEXTURE_PX_HALF_HEIGHT) / TEXTURE_PX_HEIGHT;

        return uvPos;
    }

}
