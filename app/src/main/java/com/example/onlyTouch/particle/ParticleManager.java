package com.example.onlyTouch.particle;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.MotionEvent;

import com.example.onlyTouch.R;
import com.example.onlyTouch.convert.Conversion;
import com.example.onlyTouch.opengl.ParticleGLSurfaceView;
import com.google.fpl.liquidfun.ParticleFlag;
import com.google.fpl.liquidfun.ParticleGroup;
import com.google.fpl.liquidfun.ParticleGroupDef;
import com.google.fpl.liquidfun.ParticleGroupFlag;
import com.google.fpl.liquidfun.ParticleSystem;
import com.google.fpl.liquidfun.ParticleSystemDef;
import com.google.fpl.liquidfun.Vec2;
import com.google.fpl.liquidfun.World;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.microedition.khronos.opengles.GL10;

/*
 * パーティクル管理
 */
public class ParticleManager {

    //-------------------------
    // 柔らかさ関連パラメータ
    //-------------------------
    // SOFT
    public static final float SOFT_RADIUS = 0.2f;
    public static final float SOFT_DENCITY = 0.1f;
    public static final float SOFT_ELASTIC_STRENGTH = 0.2f;
    // デフォルト値
    public static final float DEFAULT_RADIUS = 0.3f;
    public static final float DEFAULT_DENCITY = 0.5f;
    public static final float DEFAULT_ELASTIC_STRENGTH = 0.25f;
    // LITTLE HARD
    public static final float LITTLE_HARD_RADIUS = 0.4f;
    public static final float LITTLE_HARD_DENCITY = 1.0f;
    public static final float LITTLE_HARD_ELASTIC_STRENGTH = 1.0f;

    // パーティクルの柔らかさ
    private int mSoftness;
    public static final int SOFTNESS_SOFT = 0;
    public static final int SOFTNESS_NORMAL = 1;
    public static final int SOFTNESS_LITTEL_HARD = 2;

    //---------------------------
    // パーティクルの柔らかさ決定因子
    //---------------------------
    private float mParticleRadius;
    private float mParticleDencity;
    private float mParticleElasticStrength;

    //---------------
    // リソースID
    //---------------
    public static final int TEXTURE_ID = R.drawable.e_cat_kinchaku_1;
    public static final int POLYGON_XML_ID = R.xml.e_cat_kinchaku_1;
    // 生成済みテクスチャ
    private final HashMap<Integer, Integer> mMapResourceTexture;

    //---------------------------
    // 管理データ
    //---------------------------
    private final World mWorld;
    private final ParticleGLSurfaceView mGLSurfaceView;
    private final PolygonXmlDataManager mPolygonListManage;
    private int mTextureId;
    private ParticleSystem mParticleSystem;
    private ParticleGroup mParticleGroup;

    //---------------------------
    // パーティクル
    //---------------------------
    // 位置が急上昇したパーティクルなし
    public final int NOTHING_TOO_RISE = -1;

    // 境界パーティクルIndexとY座標
    private HashMap<Integer, Float> mBorderParticle;
    // パーティクルタッチ情報
    private final ParticleTouchInfo mParticleTouchInfo;

    //--------------------
    // Rendererバッファ
    //--------------------
    // 描画順のパーティクルindexリスト（このリストから頂点バッファを生成する）
    private final ArrayList<Integer> mRenderParticleOrderBuff;
    // UV座標配列
    private FloatBuffer mRenderUVBuff;

    // レンダリング対象の三角形グループの頂点該当なし
    private final int NOT_FOUND_TRIANGLE_APEX = -1;


    /*
     * コンストラクタ
     */
    public ParticleManager(ParticleGLSurfaceView glSurfaceView, World world) {

        // タッチ画面のビューコンテナ
        mGLSurfaceView = glSurfaceView;
        mWorld = world;

        // 柔らかさデフォルト値
        mSoftness = SOFTNESS_NORMAL;
        mParticleRadius = DEFAULT_RADIUS;
        mParticleDencity = DEFAULT_DENCITY;
        mParticleElasticStrength = DEFAULT_ELASTIC_STRENGTH;

        // タッチ情報
        mParticleTouchInfo = new ParticleTouchInfo();

        // Rendererバッファ
        mRenderParticleOrderBuff = new ArrayList<>();
        mMapResourceTexture = new HashMap<Integer, Integer>();

        // パーティクルシステムをデフォルトで設定
        setParticleSystem();
        // ポリゴンリストデータ管理クラス
        mPolygonListManage = new PolygonXmlDataManager(glSurfaceView.getContext(), ParticleManager.POLYGON_XML_ID, ParticleManager.TEXTURE_ID);
    }

    /*
     * getter/setter
     */
    public void setParticleSystem(ParticleSystem particleSystem) {
        mParticleSystem = particleSystem;
    }

    public ParticleSystem getParticleSystem() {
        return mParticleSystem;
    }

    public ParticleGroup getParticleGroup() {
        return mParticleGroup;
    }

    public void setParticleGroup(ParticleGroup particleGroup) {
        mParticleGroup = particleGroup;
    }

    public int getTextureId() {
        return mTextureId;
    }

    public float getParticleRadius() {
        return mParticleRadius;
    }

    public void setParticleRadius(float particleRadius) {
        mParticleRadius = particleRadius;
    }

    public void setTextureId(int textureId) {
        mTextureId = textureId;
    }

    public float getParticleDencity() {
        return mParticleDencity;
    }

    public float getParticleElasticStrength() {
        return mParticleElasticStrength;
    }

    public int getSoftness() {
        return mSoftness;
    }

    public void setSoftness(int softness) {
        mSoftness = softness;
    }


    /*
     * パーティクルシステムの生成
     */
    public void setParticleSystem() {

        //------------
        // 設定値
        //------------
        // 柔らかさの決定因子
        float radius = mParticleRadius;
        float dencity = mParticleDencity;
        float elasticStrength = mParticleElasticStrength;
        // 固定値
        final float DAMPING_STRENGTH = 0.2f;
        final float GRAVITY_SCALE = 0.4f;
        final float LIFETIME_GRANULARITY = 0.0001f;

        //------------------------
        // パーティクルシステム
        //------------------------
        // パーティクルシステム定義
        ParticleSystemDef particleSystemDef = new ParticleSystemDef();
        particleSystemDef.setRadius(radius);
        particleSystemDef.setDensity(dencity);
        particleSystemDef.setElasticStrength(elasticStrength);
        particleSystemDef.setDampingStrength(DAMPING_STRENGTH);
        particleSystemDef.setGravityScale(GRAVITY_SCALE);
        particleSystemDef.setDestroyByAge(true);
        particleSystemDef.setLifetimeGranularity(LIFETIME_GRANULARITY);

        // パーティクルシステム生成
        mParticleSystem = mWorld.createParticleSystem(particleSystemDef);
    }

    /*
     * パーティクルグループ定義の設定
     */
    private ParticleGroup createParticleGroup(float posX, float posY) {

        //----------------------
        // ParticleGroup定義
        //----------------------
        ParticleGroupDef particleGroupDef = new ParticleGroupDef();
        particleGroupDef.setFlags(ParticleFlag.elasticParticle);
        particleGroupDef.setGroupFlags(ParticleGroupFlag.solidParticleGroup);
        particleGroupDef.setPosition(posX, posY);
        particleGroupDef.setLifetime(0);

        // PolygonXMLデータから形状情報を取得
        PolygonXmlDataManager.PolygonParseData polygonXmlData = mPolygonListManage.parsePoligonXmlShapes(mGLSurfaceView.getContext(), ParticleManager.POLYGON_XML_ID);
        // 形状設定
        particleGroupDef.setPolygonShapesFromVertexList(polygonXmlData.mCoordinateBuff, polygonXmlData.mVertexeNumBuff, polygonXmlData.mShapeNum);

        // !エラー判定必要


        //----------------------
        // ParticleGroup生成
        //----------------------
        return mParticleSystem.createParticleGroup(particleGroupDef);
    }


    /*
     * パーティクル生成
     */
    public void createParticleBody(GL10 gl, float posX, float posY) {

        //-----------------------------
        // パーティクルグループ生成
        //-----------------------------
        mParticleGroup = createParticleGroup(posX, posY);

        //========================
        // ！粒子座標取得用！
//        int size = mParticleGroup.getParticleCount();
//        Log.i("PolygonList()=", "" + size);
//        for (int i = 0; i < size; i++) {
//            float x = mParticleSystem.getParticlePositionX(i);
//            float y = mParticleSystem.getParticlePositionY(i);
//            Log.i("パーティクル座標", "" + i + "\t" + x + "\t" + y);
//        }
        //========================


        //-------------
        // バッファ
        //-------------
        // 行単位のパーティクルバッファを生成
        ArrayList<ArrayList<Integer>> allParticleLine = generateParticleLineBuff(mParticleGroup);
        // OpenGLに渡す三角形グルーピングバッファをエンキュー
        enqueRendererBuff(allParticleLine);
        // レンダリング用UVバッファを生成
        generateUVRendererBuff();

        //-----------------------
        // パーティクル情報を保持
        //-----------------------
        // テクスチャ生成
        mTextureId = getTexture(gl, ParticleManager.TEXTURE_ID);

        // 境界パーティクル保持情報を初期化
        initBorderParticle(allParticleLine);
    }

    /*
     * テクスチャ生成
     */
    private int getTexture(GL10 gl10, int resourceId) {

        //------------------
        // 生成済み判定
        //------------------
        // 指定リソースのテクスチャが既にあれば、それを返して終了
        Integer textureId = mMapResourceTexture.get(resourceId);
        if (textureId != null) {
            return textureId;
        }

        //------------------
        // テクスチャ生成
        //------------------
        textureId = Conversion.makeTexture(gl10, resourceId, mGLSurfaceView.getContext());

        //-------------------
        // テクスチャを保持
        //-------------------
        // リソースIDとテクスチャIDをMapとして保持する
        mMapResourceTexture.put(resourceId, textureId);

        return textureId;
    }


    /*
     * 同一行のパーティクルによるバッファ生成
     *  @para I:パーティクルグループ
     *  @para O:全パーティクルライン
     */
    private ArrayList<ArrayList<Integer>> generateParticleLineBuff(ParticleGroup particleGroup) {

        // 1ライン分格納先
        ArrayList<Integer> line = new ArrayList<>();

        // 対象のパーティクルグループのパーティクル数を算出
        int bufferIndex = particleGroup.getBufferIndex();
        int groupParticleNum = particleGroup.getParticleCount() - bufferIndex;

        // 先頭パーティクルのY座標を格納中ラインのY座標とする
        float linePosY = mParticleSystem.getParticlePositionY(bufferIndex);

        // 格納先リスト
        ArrayList<ArrayList<Integer>> allParticleLine = new ArrayList<>();

        //------------------------
        // 全パーティクルを格納
        //------------------------
        for (int i = bufferIndex; i < groupParticleNum; ++i) {

            //-------------------
            // ライン切り替わり判定
            //-------------------
            // パーティクルのY座標
            float y = mParticleSystem.getParticlePositionY(i);

            // パーティクルが次のラインのものの場合
            // !0.01fは適当に定めた値（とりあえずこれ以上離れていればラインが変わったと判断する）
            final float LINE_DIFF = 0.01f;
            float distanceLines = Math.abs(linePosY - y);
            if (distanceLines > LINE_DIFF) {
                // パーティクルラインを全パーティクルラインに追加
                allParticleLine.add(line);
                // 新規ラインを用意
                line = new ArrayList<>();
                // 格納中ラインのY座標を更新
                linePosY = y;
            }

            //-------------------------------
            // ラインバッファにパーティクルを追加
            //-------------------------------
            // ラインにパーティクルを追加し、Y座標を更新
            line.add(i);
        }

        // パーティクル行を全パーティクルラインに追加
        allParticleLine.add(line);

        return allParticleLine;
    }

    /*
     * パーティクルをレンダリングバッファに格納
     */
    private void enqueRendererBuff(ArrayList<ArrayList<Integer>> allParticleLine) {

        // パーティクルの直径
        float particleDiameter = mParticleRadius * 2;

        // ループ数 = ライン数 - 1
        int lastLineIndex = allParticleLine.size() - 1;
        for (int lineIndex = 0; lineIndex < lastLineIndex; lineIndex++) {

            // 下ライン／上ライン
            ArrayList<Integer> bottomLine = allParticleLine.get(lineIndex);
            ArrayList<Integer> topLine = allParticleLine.get(lineIndex + 1);

            // 下ライン／上ラインを底辺とした三角形グループをバッファに格納
            enqueParticleBaseBottomLine(bottomLine, topLine, particleDiameter);
            enqueParticleBaseTopLine(bottomLine, topLine, particleDiameter);
        }
    }

    /*
     * 下ラインを底辺とする3角形グループバッファの生成
     *
     * 　下辺が底辺、上辺が頂点となるように、三角形グループ単位でバッファに格納する
     *
     *  【バッファ生成イメージ】
     * 　＜パーティクルイメージ＞
     *    ⑩　⑪
     *　　 ①　②　③
     *
     *  【バッファ格納イメージ】
     *    [0] [1] [2] [3] [4] [5]
     *　　 ①  ②  ⑩   ②  ③  ⑪
     */
    private void enqueParticleBaseBottomLine(ArrayList<Integer> bottomLine, ArrayList<Integer> topLine, float diameter) {

        // ライン先頭に格納されている「パーティクルシステム側のIndex」
        int bottomFirstParticleIndex = bottomLine.get(0);
        // ライン末尾Index「バッファ側のIndex」
        int bottomLastIndex = bottomLine.size() - 1;

        //--------------------------------------
        // 1ライン分、三角形グループをバッファに格納
        //--------------------------------------
        for (int refPosition = 0; refPosition < bottomLastIndex; refPosition++) {

            // 参照Index（パーティクルシステム側のIndex）
            int refIndex = bottomFirstParticleIndex + refPosition;

            // 参照中パーティクルのX位置
            float posX = mParticleSystem.getParticlePositionX(refIndex);
            float nextPosX = mParticleSystem.getParticlePositionX(refIndex + 1);

            // 粒子が隣り合っていない（一定以上の距離がある）なら、グルーピングしない(描画対象外)
            if ((nextPosX - posX) > diameter) {
                continue;
            }

            // 上ラインから、三角形の頂点たりうるパーティクルを取得
            int refTopParticleIndex = getApexInTopLine(topLine, posX, nextPosX);
            if (refTopParticleIndex == NOT_FOUND_TRIANGLE_APEX) {
                // 該当なしなら、グルーピングしない(描画対象外)
                continue;
            }

            // 3頂点を描画バッファに格納
            mRenderParticleOrderBuff.add(refIndex);        // 底辺-左
            mRenderParticleOrderBuff.add(refIndex + 1);    // 底辺-右
            mRenderParticleOrderBuff.add(refTopParticleIndex);  // 頂点
        }
    }

    /*
     * 上ラインを底辺とする3角形グループバッファの生成
     *
     * 　上辺が底辺、下辺が頂点となるように、三角形グループ単位でバッファに格納する
     *
     *  【バッファ生成イメージ】
     * 　＜パーティクルイメージ＞
     *    ⑩　⑪  ⑫
     *　　 ①　②　③
     *
     *  【バッファ格納イメージ】
     *    [0] [1] [2] [3] [4] [5]
     *　　 ⑫  ⑪  ③   ⑪  ⑫  ②
     */
    private void enqueParticleBaseTopLine(ArrayList<Integer> bottomLine, ArrayList<Integer> topLine, float diameter) {

        // ライン末尾に格納されている「パーティクルシステム側のIndex」
        int topLastIndex = topLine.size() - 1;
        int topLastParticleIndex = topLine.get(topLastIndex);

        //--------------------------------------
        // 1ライン分、三角形グループをバッファに格納
        //--------------------------------------
        for (int refPosition = 0; refPosition < topLastIndex; refPosition++, topLastParticleIndex--) {

            // 参照Index（パーティクルシステム側のIndex）
            float posX = mParticleSystem.getParticlePositionX(topLastParticleIndex - 1);
            float nextPosX = mParticleSystem.getParticlePositionX(topLastParticleIndex);

            // 粒子が隣り合っていないなら、グルーピングしない(描画対象外)
            if ((nextPosX - posX) > diameter) {
                continue;
            }

            // 下ラインから、三角形の頂点たりうるパーティクルを取得
            int refBottomParticleIndex = getApexInBottomLine(bottomLine, posX, nextPosX);
            if (refBottomParticleIndex == NOT_FOUND_TRIANGLE_APEX) {
                // 該当なしなら、グルーピングしない(描画対象外)
                continue;
            }

            // 3頂点をバッファに格納
            mRenderParticleOrderBuff.add(topLastParticleIndex);
            mRenderParticleOrderBuff.add(topLastParticleIndex - 1);
            mRenderParticleOrderBuff.add(refBottomParticleIndex);
        }
    }

    /*
     * 上ラインから三角形の頂点を取得
     */
    private int getApexInTopLine(ArrayList<Integer> topLine, float posX, float nextPosX) {

        int topFirstParticleIndex = topLine.get(0);
        int upLastIndex = topLine.size() - 1;

        // 上辺側に、三角形の頂点たりうる粒子があるかチェック(左からチェック)
        for (int topRefPosition = 0; topRefPosition <= upLastIndex; topRefPosition++) {

            // 参照パーティクルのX位置
            int refTopParticleIndex = topFirstParticleIndex + topRefPosition;
            float topPosX = mParticleSystem.getParticlePositionX(refTopParticleIndex);

            // 底辺とするパーティクルのどちらかの真上にあれば、三角形の頂点として採用する
            if ((topPosX == posX) || (topPosX == nextPosX)) {
                return refTopParticleIndex;
            }
        }

        // 該当なし
        return NOT_FOUND_TRIANGLE_APEX;
    }

    /*
     * 下ラインから三角形の頂点を取得
     */
    private int getApexInBottomLine(ArrayList<Integer> bottomLine, float posX, float nextPosX) {

        // ライン末尾に格納されたパーティクルIndex
        int lastIndex = bottomLine.size() - 1;
        int bottomLastIndex = bottomLine.get(lastIndex);

        // 下ライン側に、三角形の頂点たりうる粒子があるかチェック(右からチェック)
        for (int bottomRefPosition = 0; bottomRefPosition <= lastIndex; bottomRefPosition++, bottomLastIndex--) {

            // 参照中パーティクルIndexのX位置座標
            float bottomPosX = mParticleSystem.getParticlePositionX(bottomLastIndex);

            // 底辺とするパーティクルのどちらかの真下にあれば、三角形の頂点として採用する
            if ((bottomPosX == nextPosX) || (bottomPosX == posX)) {
                return bottomLastIndex;
            }
        }

        // 該当なし
        return NOT_FOUND_TRIANGLE_APEX;
    }

    /*
     * レンダリング用UVバッファの生成
     */
    private void generateUVRendererBuff() {

        //-------------------------------------------------
        // パーティクルグループ内の粒子で最小位置と最大位置を取得する
        //-------------------------------------------------
        // 先頭のパーティクルを暫定で最大値・最小値とする
        float minParticleX = mParticleSystem.getParticlePositionX(0);
        float maxParticleX = minParticleX;
        float minParticleY = mParticleSystem.getParticlePositionY(0);
        float maxParticleY = minParticleY;

        // 全パーティクルの中で、X/Y座標の最大値最小値を算出
        int particleNum = mParticleSystem.getParticleCount();
        for (int i = 1; i < particleNum; i++) {
            // X座標
            float posX = mParticleSystem.getParticlePositionX(i);
            minParticleX = Math.min(posX, minParticleX);
            maxParticleX = Math.max(posX, maxParticleX);

            // Y座標
            float posY = mParticleSystem.getParticlePositionY(i);
            minParticleY = Math.min(posY, minParticleY);
            maxParticleY = Math.max(posY, maxParticleY);
        }

        // 横幅・縦幅を算出
        float particleMaxWidth = Math.abs(maxParticleX - minParticleX);
        float particleMaxHeight = Math.abs(maxParticleY - minParticleY);

        //-------------------------------
        // UV座標をバッファに格納
        //-------------------------------
        // UV座標の最大・最小・横幅・縦幅
        final float minUvX = mPolygonListManage.getUvMinX();
        final float maxUvY = mPolygonListManage.getUvMaxY();
        final float UvMaxWidth = mPolygonListManage.getUvMaxWidth();
        final float UvMaxHeight = mPolygonListManage.getUvMaxHeight();

        // 各パーティクル位置に対応するUV座標を計算し、リストに格納する
        ArrayList<Vec2> uvCoordinate = new ArrayList<>();
        for (int i : mRenderParticleOrderBuff) {
            // パーティクル座標
            float x = mParticleSystem.getParticlePositionX(i);
            float y = mParticleSystem.getParticlePositionY(i);

            // UV座標
            float vecx = minUvX + (((x - minParticleX) / particleMaxWidth) * UvMaxWidth);
            float vecy = maxUvY - (((y - minParticleY) / particleMaxHeight) * UvMaxHeight);

            // レンダリング用UVバッファに格納
            uvCoordinate.add(new Vec2(vecx, vecy));
        }

        //---------------------------------
        // UV座標をバッファに格納
        //---------------------------------
        mRenderUVBuff = getUVBuffer(uvCoordinate);
    }

    /*
     * 境界パーティクルバッファを取得
     *   全パーティクルの中で、外側に面しているパーティクル（境界パーティクル）をバッファに格納する
     */
    private ArrayList<Integer> getBorderParticle(ArrayList<ArrayList<Integer>> allParticleLine) {
        // 境界パーティクルバッファ
        ArrayList<Integer> borderBuff = new ArrayList<>();

        int lineNum = allParticleLine.size();
        int lastLineIndex = lineNum - 1;

        //--------------------
        // 最下ラインと最上ライン
        //--------------------
        // ライン上の全パーティクルが境界パーティクル
        ArrayList<Integer> bottomLine = allParticleLine.get(0);
        ArrayList<Integer> topLine = allParticleLine.get(lastLineIndex);
        borderBuff.addAll(bottomLine);
        borderBuff.addAll(topLine);

        //--------------------------------------------
        // 最下ラインと最上ラインの間のライン
        // （パーティクルの2ライン目から最終ラインの前のラインまで）
        //--------------------------------------------
        // ライン上の両サイドにあるパーティクルが境界パーティクルとなる
        for (int i = 1; i < lastLineIndex; i++) {
            ArrayList<Integer> targetTopLine = allParticleLine.get(i - 1);
            ArrayList<Integer> betweenLine = allParticleLine.get(i);
            ArrayList<Integer> targetBottomLine = allParticleLine.get(i + 1);

            //----------------------------
            // 両端
            //----------------------------
            storeLineEdges(betweenLine, borderBuff);

            //----------------------------------------
            // 上下端
            //----------------------------------------
            storeRowEdges(betweenLine, targetTopLine, targetBottomLine, borderBuff);
        }

        return borderBuff;
    }

    /*
     * 境界パーティクルバッファ格納：ラインの両端
     */
    private void storeLineEdges(ArrayList<Integer> line, ArrayList<Integer> storeBuff) {

        int lastIndex = line.size() - 1;

        //----------------------------
        // ラインの両サイドにあるパーティクル
        //----------------------------
        int leftParticleIndex = line.get(0);
        int rightParticleIndex = line.get(lastIndex);
        // 格納
        storeBuff.add(leftParticleIndex);
        storeBuff.add(rightParticleIndex);
    }

    /*
     * 境界パーティクルバッファ格納：上下端
     */
    private void storeRowEdges(ArrayList<Integer> line, ArrayList<Integer> topLine, ArrayList<Integer> bottomLine, ArrayList<Integer> storeBuff) {

        int lastIndex = line.size() - 1;

        // !ラインの両端は格納済みのため、両端は除いて判定と格納を行う
        for( int i = 1; i < lastIndex; i++ ){

            // 上下端判定
            int particleIndex = line.get(i);
            boolean isRowEdges = isRowEdges( topLine, bottomLine, particleIndex );
            if( isRowEdges ){
                // 上下端（上下どちらかにパーティクルがない場合）、境界とみなす
                storeBuff.add(particleIndex);
            }
        }
    }

    /*
     * 上下端判定
     *    指定パーティクルが上下端の存在であるかを判定
     * 　　（上下ライン上で、指定パーティクルと同一列にパーティクルがあるかどうかで判定）
     *    true：上下どちらかでも同一列にパーティクルなし（外側にあるパーティクルとみなす）
     */
    private boolean isRowEdges(ArrayList<Integer> topLine, ArrayList<Integer> bottomLine, int particleIndex) {

        // 判定パーティクルのＸ座標
        float posX = mParticleSystem.getParticlePositionX( particleIndex );

        //------------------
        // 上ライン
        //------------------
        // 初期値は同一列になしとする
        boolean noneTop = true;
        for( int i : topLine ){
            float verticlePosX = mParticleSystem.getParticlePositionX( i );
            if( posX == verticlePosX ){
                // 同一列にあり
                noneTop = false;
                break;
            }
        }

        //------------------
        // 下ライン
        //------------------
        // 初期値は同一列になしとする
        boolean noneBottom = true;
        for( int i : bottomLine ){
            float verticlePosX = mParticleSystem.getParticlePositionX( i );
            if( posX == verticlePosX ){
                // 同一列にあり
                noneBottom = false;
                break;
            }
        }

        // 上下ラインどちらかでもなければ、true(上下端とみなす)を返す
        return ( noneTop || noneBottom );
    }


    /*
     * 境界パーティクル情報初期化
     */
    public void initBorderParticle(ArrayList<ArrayList<Integer>> allParticleLine) {

        // 境界パーティクルをリストとして取得
        ArrayList<Integer> border = getBorderParticle(allParticleLine);

        //----------------------
        // 境界パーティクル情報の生成
        //----------------------
        mBorderParticle = new HashMap<Integer, Float>();

        for (int particleIndex : border) {
            // 境界パーティクルのY座標
            float posY = mParticleSystem.getParticlePositionY(particleIndex);
            // indexとY座標をペアで保持
            mBorderParticle.put(particleIndex, posY);
        }
    }

    /*
     * 位置が急上昇した境界パーティクルの取得
     *  　本メソッドがコールされたタイミングで、位置が急上昇したパーティクルがあればそれを返す
     */
    public int getTooRiseBorderParticle() {

        // 急上昇判定値
        final float TOO_RISE = 1.8f;

        //-----------
        // 検索
        //-----------
        // 全境界パーティクルの位置を確認
        for (int borderIndex : mBorderParticle.keySet()) {

            // 位置
            float preY = mBorderParticle.get(borderIndex);
            float currentY = mParticleSystem.getParticlePositionY(borderIndex);

            // 急上昇したとみなせる程、前回位置よりも上にある場合
            if ((currentY - preY) >= TOO_RISE) {
                return borderIndex;
            }
        }

        // なし
        return NOTHING_TOO_RISE;
    }

    /*
     * 保持している境界パーティクルのY座標を更新
     *   !実物の位置を更新しているわけではない。
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void updateBorderParticlePosY() {

        // 保持中の境界パーティクルの位置情報をコール時点の情報に更新
        for (int borderIndex : mBorderParticle.keySet()) {
            float currentY = mParticleSystem.getParticlePositionY(borderIndex);
            mBorderParticle.replace(borderIndex, currentY);
        }
    }

    /*
     * 境界パーティクル判定
     *   指定されたIndexが境界パーティクルであるか判定する
     */
    public boolean isBorderParticle(int searchIndex) {

        // 境界パーティクルの中から検索する
        for (int borderIndex : mBorderParticle.keySet()) {
            if (borderIndex == searchIndex) {
                return true;
            }
        }

        // なし
        return false;
    }

    /*
     * 柔らかさ因子の設定
     */
    public void setSoftnessFactor(int softness) {

        //-------------------------
        // 柔らかさの決定因子
        //-------------------------
        float radius;
        float dencity;
        float elasticStrength;

        // 指定に応じて、パラメータを設定
        switch (softness) {
            case SOFTNESS_SOFT:
                radius = SOFT_RADIUS;
                dencity = SOFT_DENCITY;
                elasticStrength = SOFT_ELASTIC_STRENGTH;
                break;

            case SOFTNESS_NORMAL:
                radius = DEFAULT_RADIUS;
                dencity = DEFAULT_DENCITY;
                elasticStrength = DEFAULT_ELASTIC_STRENGTH;
                break;

            case SOFTNESS_LITTEL_HARD:
                radius = LITTLE_HARD_RADIUS;
                dencity = LITTLE_HARD_DENCITY;
                elasticStrength = LITTLE_HARD_ELASTIC_STRENGTH;
                break;

            default:
                radius = DEFAULT_RADIUS;
                dencity = DEFAULT_DENCITY;
                elasticStrength = DEFAULT_ELASTIC_STRENGTH;
                break;
        }

        // 因子を更新
        mParticleRadius = radius;
        mParticleDencity = dencity;
        mParticleElasticStrength = elasticStrength;

        mSoftness = softness;
    }

    /*
     * パーティクルタッチ処理
     */
    public boolean touchParticle(MotionEvent event) {

        switch (event.getAction()) {

            // タッチ解除
            case MotionEvent.ACTION_UP:
                // 粒子用：状態更新
                mParticleTouchInfo.clearTouchInfo();
                break;

            // タッチ移動
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                // 粒子用：タッチ中の位置を設定
                // !world座標への変換は、必要なタイミングで実施する（この時点ではGL10がないため）
                mParticleTouchInfo.setTouchPos(event.getX(), event.getY());
                break;

            default:
                break;
        }

        return true;
    }

    /*
     * パーティクルタッチ追随処理
     *   パーティクルに対するタッチ判定を行い、タッチされていればパーティクルを追随させる
     */
    public void traceTouchParticle(GL10 gl) {

        //----------------
        // 処理なし
        //----------------
        // パーティクルなし
        if (mParticleGroup.getParticleCount() == 0) {
            return;
        }
        // 未タッチ
        if (mParticleTouchInfo.mTouchPosX == ParticleTouchInfo.INVALID_TOUCH_POS) {
            return;
        }

        //------------------------
        // パーティクル追随判定
        //------------------------
        // 現在のタッチ状態
        ParticleTouchInfo.ParticleTouchStatus currentStatus = getCurrentTouchStatus(gl);

        // 前回のタッチ状態が「境界」「追随」でなければ
        if ((mParticleTouchInfo.mStatus != ParticleTouchInfo.ParticleTouchStatus.BORDER) &&
                (mParticleTouchInfo.mStatus != ParticleTouchInfo.ParticleTouchStatus.TRACE)) {
            // 現状のタッチ状態を更新して終了
            mParticleTouchInfo.mStatus = currentStatus;
            return;
        }

        // 今回のタッチ状態が「外側」以外の場合
        if (currentStatus != ParticleTouchInfo.ParticleTouchStatus.OUTSIDE) {
            // 現状のタッチ状態を更新して終了
            mParticleTouchInfo.mStatus = currentStatus;
            return;
        }

        //------------------------
        // パーティクル追随
        //------------------------
        // 境界のパーティクルをタッチ位置に追随させる
        // （タッチ座標から少しずらした位置に、パーティクルの位置を変更する）
        float tracePosX = mParticleTouchInfo.mTouchPosWorldX + 0.1f;
        float tracePosY = mParticleTouchInfo.mTouchPosWorldY + 0.1f;
        mParticleSystem.setParticlePosition(mParticleTouchInfo.mBorderIndex, tracePosX, tracePosY);

        // 現状のタッチ状態を更新
        mParticleTouchInfo.mStatus = ParticleTouchInfo.ParticleTouchStatus.TRACE;
    }

    /*
     * 現在のパーティクルに対するタッチ状態を更新
     */
    private ParticleTouchInfo.ParticleTouchStatus getCurrentTouchStatus(GL10 gl) {

        //----------------------
        // タッチ判定範囲
        //----------------------
        // タッチ範囲
        // ！パーティクル半径 * 2　としておく（タッチを簡単にできるようにするため）
        float range = mParticleRadius * 2;

        // タッチ判定範囲を算出
        float[] touchPos = Conversion.convertPointScreenToWorld(mParticleTouchInfo.mTouchPosX, mParticleTouchInfo.mTouchPosY, gl, mGLSurfaceView);
        float touchMinX = touchPos[0] - range;
        float touchMaxX = touchPos[0] + range;
        float touchMinY = touchPos[1] - range;
        float touchMaxY = touchPos[1] + range;

        // タッチ位置のworld座標を保持
        mParticleTouchInfo.setTouchWorldPos(touchPos[0], touchPos[1]);

        //----------------------
        // タッチ判定
        //----------------------
        // 判定前はパーティクルの外側
        ParticleTouchInfo.ParticleTouchStatus status = ParticleTouchInfo.ParticleTouchStatus.OUTSIDE;

        // 全パーティクルを対象にタッチ判定
        int particleNum = mParticleGroup.getParticleCount();
        int index;
        for (index = 0; index < particleNum; index++) {
            // パーティクル位置
            float x = mParticleSystem.getParticlePositionX(index);
            float y = mParticleSystem.getParticlePositionY(index);

            // タッチ範囲にパーティクルあるか
            if ((x >= touchMinX) && (x <= touchMaxX) && (y >= touchMinY) && (y <= touchMaxY)) {
                // タッチ状態：パーティクル内部
                status = ParticleTouchInfo.ParticleTouchStatus.INSIDE;
                break;
            }
        }

        //---------------------------
        // タッチしているパーティクルなし
        //---------------------------
        if (status == ParticleTouchInfo.ParticleTouchStatus.OUTSIDE) {
            return status;
        }

        //---------------------------
        // タッチしているパーティクルあり
        //---------------------------
        if (isBorderParticle(index)) {
            // タッチ中のパーティクルを保持
            mParticleTouchInfo.mBorderIndex = index;
            // タッチ状態：パーティクル境界
            status = ParticleTouchInfo.ParticleTouchStatus.BORDER;
        }

        return status;
    }

    /*
     * パーティクル削除
     */
    public void destroyParticle() {
        mParticleGroup.destroyParticles();
        mRenderParticleOrderBuff.clear();
    }

    /*
     * パーティクルの描画情報の更新(頂点バッファ/UVバッファ)
     */
    public void draw(GL10 gl) {

        // 粒子がない場合、何もしない
        if (mParticleGroup.getParticleCount() == 0) {
            return;
        }

        //---------------
        // レンダリング
        //---------------
        // マトリクス記憶
        gl.glPushMatrix();
        {
            // テクスチャの指定
            gl.glActiveTexture(GL10.GL_TEXTURE0);
            gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureId);

            // 現時点のパーティクル座標から頂点バッファを計算
            FloatBuffer vertexBuffer = getVertexBuffer();
            // レンダリング頂点数
            int renderPointNum = mRenderParticleOrderBuff.size();

            // バッファを渡して描画
            gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mRenderUVBuff);
            gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vertexBuffer);
            gl.glDrawArrays(GL10.GL_TRIANGLES, 0, renderPointNum);
        }
        // マトリクスを戻す
        gl.glPopMatrix();
    }

    /*
     * 頂点座標バッファの取得
     */
    private FloatBuffer getVertexBuffer() {

        // 頂点座標配列
        int buffSize = mRenderParticleOrderBuff.size() * 2;
        float[] vertices = new float[buffSize];

        // レンダリングバッファのパーティクルの座標を配列に格納
        int count = 0;
        for (int index : mRenderParticleOrderBuff) {
            vertices[count] = mParticleSystem.getParticlePositionX(index);
            count++;
            vertices[count] = mParticleSystem.getParticlePositionY(index);
            count++;
        }

        // FloatBufferに変換
        return Conversion.convertFloatBuffer(vertices);
    }

    /*
     * UV座標バッファの取得
     */
    private FloatBuffer getUVBuffer( ArrayList<Vec2> uvCoordinate ) {

        // UV座標配列
        int buffSize = mRenderParticleOrderBuff.size() * 2;
        float[] uv = new float[buffSize];

        // レンダリングUVバッファのUV座標を配列に格納
        int count = 0;
        for (Vec2 Coordinate : uvCoordinate) {
            uv[count] = Coordinate.getX();
            count++;
            uv[count] = Coordinate.getY();
            count++;
        }

        // FloatBufferに変換
        return Conversion.convertFloatBuffer(uv);
    }


}
