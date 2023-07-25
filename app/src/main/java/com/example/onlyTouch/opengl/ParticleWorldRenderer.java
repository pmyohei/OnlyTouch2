package com.example.onlyTouch.opengl;

import android.content.res.Resources;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.MotionEvent;
import android.view.View;

import com.example.onlyTouch.R;
import com.example.onlyTouch.convert.Conversion;
import com.example.onlyTouch.manage.BulletManager;
import com.example.onlyTouch.object.DrawBackGround;
import com.example.onlyTouch.particle.ParticleData;
import com.example.onlyTouch.particle.ParticleTouchInfo;
import com.example.onlyTouch.particle.PolygonXmlDataManager;
import com.google.fpl.liquidfun.Body;
import com.google.fpl.liquidfun.BodyDef;
import com.google.fpl.liquidfun.BodyType;
import com.google.fpl.liquidfun.ParticleFlag;
import com.google.fpl.liquidfun.ParticleGroup;
import com.google.fpl.liquidfun.ParticleGroupDef;
import com.google.fpl.liquidfun.ParticleGroupFlag;
import com.google.fpl.liquidfun.ParticleSystem;
import com.google.fpl.liquidfun.ParticleSystemDef;
import com.google.fpl.liquidfun.PolygonShape;
import com.google.fpl.liquidfun.Vec2;
import com.google.fpl.liquidfun.World;
import com.google.fpl.liquidfun.liquidfun;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/*
 * 物理世界で流体生成・レンダリング
 */
public class ParticleWorldRenderer implements GLSurfaceView.Renderer, View.OnTouchListener {

    /* LiquidFun ロード */
    static {
        System.loadLibrary("liquidfun");
        System.loadLibrary("liquidfun_jni");
    }


    //----------------
    // world
    //----------------
    private final World mWorld;
    // 座標
    private float[] mWorldPosMax;
    private float[] mWorldPosMid;
    private float[] mWorldPosMin;

    // 60fps
    private final float TIME_STEP = 1 / 60f;
    // 速度反復
    private final int VELOCITY_ITERATIONS = 6;
    // 位置反復
    private final int POSITION_ITERATIONS = 2;
    // 粒子反復（適切な値は b2CalculateParticleIterations() で算出）
    private int mParticleIterations;

    // 重力
    private int mGravity;
    public static final int GRAVITY_FLOAT   = 0;       // 浮く
    public static final int GRAVITY_FLUFFY  = 1;       // ふわふわ浮く
    public static final int GRAVITY_NONE    = 2;       // 重力なし
    public static final int GRAVITY_DEFAULT = 3;       // 落ちる（デフォルト）
    public static final int GRAVITY_STRONG  = 4;       // 強めの重力

    // （重力種別、重力値）
    private static final Map<Integer, Integer> mGravityScale = new HashMap<>();
    static {
        mGravityScale.put(GRAVITY_FLOAT,    30);
        mGravityScale.put(GRAVITY_FLUFFY,   10);
        mGravityScale.put(GRAVITY_NONE,      0);
        mGravityScale.put(GRAVITY_DEFAULT, -10);
        mGravityScale.put(GRAVITY_STRONG,  -30);
    }

    //----------------
    // パーティクル
    //----------------
    private final ParticleTouchInfo mParticleTouchInfo;
    private ParticleSystem mParticleSystem;
    private ParticleData mParticleData;
    private int mRegenerationState;
    private float mParticleRadius;

    // パーティクル再生成シーケンス
    public static final int PARTICLE_REGENE_STATE_NOTHING = 0;
    public static final int PARTICLE_REGENE_STATE_DELETE  = 1;
    public static final int PARTICLE_REGENE_STATE_CREATE  = 2;
    public static final int PARTICLE_REGENE_STATE_OVERLAP = 3;

    // パーティクルの柔らかさ
    private int mSoftness;
    public static final int SOFTNESS_SOFT        = 0;       // 柔らかめ
    public static final int SOFTNESS_NORMAL      = 1;       // ノーマル（デフォルト）
    public static final int SOFTNESS_LITTEL_HARD = 2;       // 少し固め

    // パーティクルの柔らかさパラメータ：デフォルト値
    private final float DEFAULT_RADIUS           = 0.3f;
    private final float DEFAULT_DENCITY          = 0.5f;
    private final float DEFAULT_ELASTIC_STRENGTH = 0.25f;

    // リソースID
    private final int TEXTURE_ID = R.drawable.texture_test_cat;
    private final int POLYGON_XML_ID = R.xml.test_cat_plist;

    //----------------
    // Body
    //----------------
    private Body mMenuBody;
    private Body mOverlapBody;

    //----------------
    // 管理
    //----------------
    private final BulletManager mBulletManager;

    //--------------------
    // 背景
    //--------------------
    private DrawBackGround mDrawBackGround;

    //------------------
    // menu
    //------------------
    // menu初期位置設定完了フラグ
    private boolean mIsSetMenuRect;

    // menu展開時のRect情報
    private float mExpandedMenuTop;
    private float mExpandedMenuLeft;
    private float mExpandedMenuRight;
    private float mExpandedMenuBottom;
    // menu折りたたみ時のRect情報
    private float mCollapsedMenuTop;
    private float mCollapsedMenuLeft;
    private float mCollapsedMenuRight;
    private float mCollapsedMenuBottom;

    // menu背景物体の情報
    private float mMenuInitPosX;
    private float mMenuInitPosY;
    private float mMenuWidth;
    private float mMenuHeight;
    private float mMenuCollapsedPosY;

    // アニメーションと連動するmenu背景物体の移動速度
    private Vec2 mMenuUpVelocity;
    private Vec2 mMenuDownVelocity;
    private Vec2 mMenuVelocity;

    // menu移動状態
    private int mMenuMoveState = MENU_MOVE_STATE_NOTHING;

    // menu背景物体の移動状態
    public static final int MENU_MOVE_STATE_NOTHING = 0;
    public static final int MENU_MOVE_STATE_UP = 1;
    public static final int MENU_MOVE_STATE_DOWN = 2;
    public static final int MENU_MOVE_STATE_KEEP = 3;
    public static final int MENU_MOVE_STATE_STOP = 4;

    //------------------
    // OpenGL
    //------------------
    private GLInitStatus mGlInitStatus;
    private final ParticleGLSurfaceView mGLSurfaceView;
    private final HashMap<Integer, Integer> mMapResourceTexture;
    private final PolygonXmlDataManager mPolygonListManage;

    // OpenGL 描画開始シーケンス
    enum GLInitStatus {
        PreInit,       // 初期化前
        FinInit,       // 初期化完了
        Drawable       // Draw開始
    }

    //--------------------
    // Rendererバッファ
    //--------------------
    // 描画順のパーティクルindexリスト（このリストから頂点バッファを生成する）
    private ArrayList<Integer> mRenderParticleOrderBuff;
    // UV座標配列
    private FloatBuffer mRenderUVBuff;
    // 描画対象の頂点数
    private int mRenderPointNum;

    // レンダリング対象の三角形グループの頂点該当なし
    private final int NOT_FOUND_TRIANGLE_APEX = -1;


    /*
     * コンストラクタ
     */
    public ParticleWorldRenderer(ParticleGLSurfaceView glSurfaceView) {
        mGLSurfaceView = glSurfaceView;

        //--------------
        // 物理世界生成
        //--------------
        // デフォルトの重力
        mGravity = GRAVITY_DEFAULT;
        int gravity = mGravityScale.get(mGravity);
        // world生成
        mWorld = new World(0, gravity);

        //-----------------
        // パーティクルの設定
        //-----------------
        mSoftness = SOFTNESS_NORMAL;
        mParticleRadius = DEFAULT_RADIUS;
        mRegenerationState = PARTICLE_REGENE_STATE_NOTHING;
        mParticleTouchInfo = new ParticleTouchInfo();
        // ポリゴンリストデータ管理クラス
        mPolygonListManage = new PolygonXmlDataManager( glSurfaceView.getContext(), POLYGON_XML_ID, TEXTURE_ID);
        // 適切な粒子反復を算出
        mParticleIterations = liquidfun.b2CalculateParticleIterations(gravity, mParticleRadius, TIME_STEP);

        //-----------------
        // 銃弾
        //-----------------
        mBulletManager = new BulletManager( mWorld, mGLSurfaceView );

        //-----------------
        // menu
        //-----------------
        // 未完了
        mIsSetMenuRect = false;

        //------------------
        // OpenGL
        //------------------
        mGlInitStatus = GLInitStatus.PreInit;
        mMapResourceTexture = new HashMap<Integer, Integer>();

        //--------------------
        // Rendererバッファ
        //--------------------
        mRenderParticleOrderBuff = new ArrayList<>();
    }

    /*
     * パーティクル情報の追加
     */
    private void addParticleData(GL10 gl, ParticleGroup pg, float particleRadius, ArrayList<ArrayList<Integer>> allParticleLine, ArrayList<Integer> border, int textureId) {
        mParticleData = new ParticleData(0, mParticleSystem, pg, particleRadius, allParticleLine, border, textureId);
    }

    /*
     * 四角形Bodyの生成
     */
    public Body createBoxBody(float width, float height, float posx, float posy, float angle, BodyType type) {

        //----------------
        // Body生成
        //----------------
        // 定義
        BodyDef bodyDef = new BodyDef();
        bodyDef.setType(type);
        bodyDef.setPosition(posx, posy);
        // 形状
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width, height, 0, 0, angle);    // para 3,4：ボックスの中心
        // 生成
        final float DENSITY = 10f;
        Body body = mWorld.createBody(bodyDef);
        body.createFixture(shape, DENSITY);

        return body;
    }


    /*
     * パーティクル生成
     */
    public void createFluidBody(GL10 gl, float posX, float posY, float particleRadius) {

        // パーティクルグループ生成
        ParticleGroup particleGroup = setupParticleGroup(posX, posY);

        //========================
        // ！粒子座標取得用！
//        int size = particleGroup.getParticleCount();
//        Log.i("PolygonList()=", "" + size);
//        for (int i = 0; i < size; i++) {
//            float x = mParticleSystem.getParticlePositionX(i);
//            float y = mParticleSystem.getParticlePositionY(i);
//            Log.i("PolygonList", "" + i + "\t" + x + "\t" + y);
//        }
        //========================


        // 行単位のパーティクルバッファを作成
        ArrayList<ArrayList<Integer>> allParticleLine = new ArrayList<>();
        generateParticleLineBuff(particleGroup, allParticleLine);

        // パーティクルの直径
        float diameter = particleRadius * 2;
        // OpenGLに渡す三角形グルーピングバッファを作成
        enqueRendererBuff(allParticleLine, diameter);
        // 頂点数を保持
        mRenderPointNum = mRenderParticleOrderBuff.size();

        // レンダリング用UVバッファを生成
        generateUVRendererBuff();

        // 境界パーティクルバッファを取得
        ArrayList<Integer> border = generateBorderParticleBuff(allParticleLine);

        // パーティクル情報の追加
        int textureId = getTexture(gl, TEXTURE_ID);
        addParticleData(gl, particleGroup, particleRadius, allParticleLine, border, textureId);
    }

    /*
     * パーティクルシステムの生成
     *   !パラメータ：柔らかさの決定因子
     */
    private void setupParticleSystem(float particleRadius, float particleDensity, float particleElasticStrength) {
        // パーティクルシステム定義
        ParticleSystemDef particleSystemDef = new ParticleSystemDef();
        particleSystemDef.setRadius(particleRadius);
        particleSystemDef.setDensity(particleDensity);
        particleSystemDef.setElasticStrength(particleElasticStrength);
        particleSystemDef.setDampingStrength(0.2f);
        particleSystemDef.setGravityScale(0.4f);
        particleSystemDef.setDestroyByAge(true);
        particleSystemDef.setLifetimeGranularity(0.0001f);
//        particleSystemDef.setMaxCount(729);

        // パーティクルシステムの生成
        mParticleSystem = mWorld.createParticleSystem(particleSystemDef);
    }

    /*
     * パーティクルグループ定義の設定
     * @para パーティクル横幅、パーティクル縦幅、生成位置(x/y)
     */
    private ParticleGroup setupParticleGroup(float posX, float posY) {

        //----------------------
        // ParticleGroup定義
        //----------------------
        ParticleGroupDef particleGroupDef = new ParticleGroupDef();
        particleGroupDef.setFlags(ParticleFlag.elasticParticle);
        particleGroupDef.setGroupFlags(ParticleGroupFlag.solidParticleGroup);
        particleGroupDef.setPosition(posX, posY);
        particleGroupDef.setLifetime(0);

        // PolygonXMLデータを取得
        PolygonXmlDataManager.PolygonParseData polygonXmlData = mPolygonListManage.parsePoligonXmlShapes(mGLSurfaceView.getContext(), POLYGON_XML_ID);
        // 形状設定
        particleGroupDef.setPolygonShapesFromVertexList( polygonXmlData.mCoordinateBuff, polygonXmlData.mVertexeNumBuff, polygonXmlData.mShapeNum );

/*            if (shapenum == -1) {
                // 取得エラーなら、終了
                Log.i("PolygonList", "setPolygonListBuffer() エラー");
                return null;
            }*/


        //----------------------
        // ParticleGroup生成
        //----------------------
        return mParticleSystem.createParticleGroup(particleGroupDef);
    }

    /*
     * 同一行のパーティクルによるバッファ生成
     *  @para I:パーティクルグループ
     *  @para O:全パーティクルライン
     */
    private void generateParticleLineBuff(ParticleGroup particleGroup, ArrayList<ArrayList<Integer>> allParticleLine) {

        // 1ライン分格納先
        ArrayList<Integer> line = new ArrayList<>();

        // 対象のパーティクルグループのパーティクル数を算出
        int bufferIndex = particleGroup.getBufferIndex();
        int groupParticleNum = particleGroup.getParticleCount() - bufferIndex;

        // 先頭パーティクルのY座標を格納中ラインのY座標とする
        float linePosY = mParticleSystem.getParticlePositionY(bufferIndex);

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
    }

    /*
     * パーティクルをレンダリングバッファに格納
     */
    private void enqueRendererBuff(ArrayList<ArrayList<Integer>> allParticleLine, float diameter) {

        // ループ数 = ライン数 - 1
        int lastLineIndex = allParticleLine.size() - 1;
        for (int lineIndex = 0; lineIndex < lastLineIndex; lineIndex++) {

            // 下ライン／上ライン
            ArrayList<Integer> bottomLine = allParticleLine.get(lineIndex);
            ArrayList<Integer> topLine = allParticleLine.get(lineIndex + 1);

            // 下ライン／上ラインを底辺とした三角形グループをバッファに格納
            enqueParticleBaseBottomLine(bottomLine, topLine, diameter);
            enqueParticleBaseTopLine(bottomLine, topLine, diameter);
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
        float particleMaxWidth  = Math.abs(maxParticleX - minParticleX);
        float particleMaxHeight = Math.abs(maxParticleY - minParticleY);

        //-------------------------------
        // UV座標をバッファに格納
        //-------------------------------
        // UV座標の最大・最小・横幅・縦幅
        final float minUvX = mPolygonListManage.getUvMinX();
        final float maxUvY = mPolygonListManage.getUvMaxY();
        final float UvMaxWidth  = mPolygonListManage.getUvWidth();
        final float UvMaxHeight = mPolygonListManage.getUvHeight();

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
            uvCoordinate.add( new Vec2(vecx, vecy) );
        }

        //---------------------------------
        // UV座標をバッファに格納
        //---------------------------------
        mRenderUVBuff = getUVBuffer( uvCoordinate );
    }

    /*
     * 境界パーティクルバッファを取得
     *   全パーティクルの中で、外側に面しているパーティクルをバッファに格納する
     */
    private ArrayList<Integer> generateBorderParticleBuff(ArrayList<ArrayList<Integer>> allParticleLine) {
        // 境界パーティクルバッファ
        ArrayList<Integer> borderBuff = new ArrayList<>();

        int lineNum = allParticleLine.size();
        int lastLineIndex = lineNum - 1;

        //--------------------
        // 最下ラインと最上ライン
        //--------------------
        // 全てのパーティクルが境界
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
            ArrayList<Integer> line = allParticleLine.get(i);
            int lastIndex = line.size() - 1;

            // ラインの両サイドにあるパーティクル
            int leftParticleIndex  = line.get(0);
            int rightParticleIndex = line.get(lastIndex);
            // 格納
            borderBuff.add(leftParticleIndex);
            borderBuff.add(rightParticleIndex);
        }

        return borderBuff;
    }

    /*
     * フレーム描画初期化処理
     *   true ：フレーム描画可
     *   false：フレーム描画不可
     */
    private boolean initDrawFrame(GL10 gl) {

        //--------------------------
        // 初期化コール前
        //--------------------------
        if (mGlInitStatus == GLInitStatus.PreInit) {
            // 何もしない：セーフティ
            return false;
        }

        //--------------------------
        // 描画可能
        //--------------------------
        if (mGlInitStatus == GLInitStatus.Drawable) {
            return true;
        }

        //--------------------------
        // 初期化完了
        //--------------------------
        // メニューサイズ設定未完了なら、フレーム描画不可
        if (!mIsSetMenuRect) {
            return false;
        }

        //--------------------------
        // 初期処理
        //--------------------------
        // world座標の計算
        calculateWorldPosition(gl);
        // 初期配置用の物体生成
        createPhysicsObject(gl);

        // GL初期化状態を描画可能に更新
        mGlInitStatus = GLInitStatus.Drawable;

        // フレーム描画可
        return true;
    }

    /*
     * 現在のフレームを描画するためにコールされる
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onDrawFrame(GL10 gl) {
        //--------------------
        // フレーム描画初期化処理
        //--------------------
        boolean initFin = initDrawFrame(gl);
        if (!initFin) {
            // 初期化未完了なら、何もしない
            return;
        }

        // 物理世界を更新
        mWorld.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS, mParticleIterations);

        //------------------
        // OpenGL
        //------------------
        // ビューの変換行列の作成
        gl.glMatrixMode(GL10.GL_MODELVIEW);   // マトリクス(4x4の変換行列)の指定
        gl.glLoadIdentity();                  // 初期化

        //------------------
        // 背景
        //------------------
        mDrawBackGround.draw(gl);

        //------------------
        // 物体関連
        //------------------
        // menu背景物体
        menuBodyControl();
        // 弾 !パーティクルよりも先に描画すること（パーティクル内部に弾が描画されることがあるため）
        mBulletManager.bulletManage(gl, mParticleSystem ,mParticleData);

        //------------------
        // パーティクル
        //------------------
        ParticleGroup particleGroup = mParticleData.getParticleGroup();
        // パーティクル再生成
        regenerationParticle(gl, particleGroup);
        // パーティクル描画更新
        updateParticleDraw(gl, particleGroup);
    }

    /*
     * world座標の計算
     */
    private void calculateWorldPosition(GL10 gl) {

        //---------------------------
        // 画面サイズ
        //---------------------------
        final int screenWidth = mGLSurfaceView.getWidth();
        final int screenHeight = mGLSurfaceView.getHeight();

        //---------------------------
        // 画面端の座標を物理座標に変換
        //---------------------------
        // 画面の端の位置を変換(Y座標は上が0)
        mWorldPosMax = Conversion.convertPointScreenToWorld(screenWidth, 0, gl, mGLSurfaceView);
        mWorldPosMid = Conversion.convertPointScreenToWorld(screenWidth / 2f, screenHeight / 2f, gl, mGLSurfaceView);
        mWorldPosMin = Conversion.convertPointScreenToWorld(0, screenHeight, gl, mGLSurfaceView);
    }

    /*
     * 各種物体生成
     */
    private void createPhysicsObject(GL10 gl) {

        //---------------
        // 背景
        //---------------
        int textureID = getTexture(gl, R.drawable.texture_background);
        mDrawBackGround = new DrawBackGround( mWorldPosMin, mWorldPosMax, textureID );

        //---------------
        // メニュー
        //---------------
        // メニュー背景の物体生成
        createMenuBody(gl);

        //---------------
        // パーティクル
        //---------------
        // パーティクルシステム生成
        setupParticleSystem(mParticleRadius, DEFAULT_DENCITY, DEFAULT_ELASTIC_STRENGTH);
        // パーティクル生成
        createFluidBody(gl, mWorldPosMid[0], mWorldPosMid[1], mParticleRadius);

        //---------------
        // 壁
        //---------------
        createWall(gl);
    }

    /*
     * メニュー背後の物体生成
     */
    private void createMenuBody(GL10 gl) {

        //-------------------------------
        // 座標変換
        //-------------------------------
        // メニュー（展開後）の座標変換
        float[] worldExpandedMenuTopLeft = Conversion.convertPointScreenToWorld(mExpandedMenuLeft, mExpandedMenuTop, gl, mGLSurfaceView);
        float[] worldExpandedMenuTopRight = Conversion.convertPointScreenToWorld(mExpandedMenuRight, mExpandedMenuTop, gl, mGLSurfaceView);
        float[] worldExpandedMenuBottomRight = Conversion.convertPointScreenToWorld(mExpandedMenuRight, mExpandedMenuBottom, gl, mGLSurfaceView);

        // メニュー（折りたたみ時）の座標変換
        float[] worldCollapsedMenuTopLeft = Conversion.convertPointScreenToWorld(mCollapsedMenuLeft, mCollapsedMenuTop, gl, mGLSurfaceView);
        float[] worldCollapsedMenuBottomRight = Conversion.convertPointScreenToWorld(mCollapsedMenuRight, mCollapsedMenuBottom, gl, mGLSurfaceView);

        //-------------------------------
        // サイズ・位置
        //-------------------------------
        // menuサイズ  !メニュービューの半分のサイズ（半分にすると適切なサイズに調整されるのは要調査）
        float menuWidth = (worldExpandedMenuTopRight[0] - worldExpandedMenuTopLeft[0]) / 2;
        float expandedMenuHeight = (worldExpandedMenuTopRight[1] - worldExpandedMenuBottomRight[1]) / 2;
        float collapsedHeight = (worldCollapsedMenuTopLeft[1] - worldCollapsedMenuBottomRight[1]) / 2;
        float menuHeight = expandedMenuHeight + collapsedHeight;

        // menu折りたたみ位置
        float collapsedMenuPosRight = worldCollapsedMenuTopLeft[0] + menuWidth;
        float collapsedMenuPosTop = worldCollapsedMenuBottomRight[1] + collapsedHeight;
        // menu背景物体の初期位置（menu折りたたみViewと物体上部が重なるY位置）
        float menuInitPosY = worldCollapsedMenuTopLeft[1] - menuHeight;

        //-------------------------------
        // 生成
        //-------------------------------
        mMenuBody = createBoxBody(menuWidth, menuHeight, collapsedMenuPosRight, menuInitPosY, 0, BodyType.staticBody);

        //-------------------------------
        // 保持
        //-------------------------------
        // 位置情報
        mMenuInitPosX = collapsedMenuPosRight;
        mMenuInitPosY = menuInitPosY;
        mMenuWidth = menuWidth;
        mMenuHeight = menuHeight;
        // menu（折りたたみ時）の位置
        mMenuCollapsedPosY = collapsedMenuPosTop;

        //------------------
        // メニュー移動速度
        //------------------
        // メニュー操作時のアニメーション時間(ms)
        Resources resources = mGLSurfaceView.getContext().getResources();
        int upDuration = resources.getInteger(R.integer.menu_up_anim_duration);
        int downDuration = resources.getInteger(R.integer.menu_down_anim_duration);

        // メニュー背景物体の移動速度を計算：Up
        float millSecond = (float) upDuration / 1000f;
        float ratioToSecond = 1.0f / millSecond;
        float speed = expandedMenuHeight * ratioToSecond * 1.32f;   // !1.32f の理由・妥当性はその内調査。
        mMenuUpVelocity = new Vec2(0, speed);

        // メニュー背景物体の移動速度を計算：Down
        millSecond = (float) downDuration / 1000f;
        ratioToSecond = 1.0f / millSecond;
        speed = expandedMenuHeight * ratioToSecond * 1.32f;         // !1.32f の理由・妥当性はその内調査。
        mMenuDownVelocity = new Vec2(0, -(speed));
    }

    /*
     * 壁の生成
     */
    private void createWall(GL10 gl) {

        //---------------
        // 床の座標計算
        //---------------
        // メニュー下部(初期)の四隅の座標を変換
        float[] worldMenuPosTopLeft = Conversion.convertPointScreenToWorld(mCollapsedMenuLeft, mCollapsedMenuTop, gl, mGLSurfaceView);

        // メニューの存在を考慮した横幅・X座標位置を計算する
        // ※メニュー物体とちょうどの位置だと下がるときうまくいかない時があるため、少し位置を左にする。
        float bottom_width = (worldMenuPosTopLeft[0] - mWorldPosMin[0]) / 2;
        float bottom_posX = mWorldPosMin[0] + bottom_width - 1;

        //---------------
        // 壁の生成
        //---------------
        // 天井
        createBoxBody(mWorldPosMax[0], 1, mWorldPosMid[0], mWorldPosMax[1], 0, BodyType.staticBody);
        // 左
        createBoxBody(1, mWorldPosMax[1], mWorldPosMin[0] - 1, mWorldPosMid[1], 0, BodyType.staticBody);
        // 右
        createBoxBody(1, mWorldPosMax[1], mWorldPosMax[0] + 1, mWorldPosMid[1], 0, BodyType.staticBody);
        // 床(メニューの存在を考慮)
        createBoxBody(bottom_width, 1, bottom_posX, mWorldPosMin[1] - 1, 0, BodyType.staticBody);
    }

    /*
     * パーティクル再生成
     */
    private void regenerationParticle(GL10 gl, ParticleGroup particleGroup) {

        switch (mRegenerationState) {

            //-----------------------
            // 処理なし
            //-----------------------
            case PARTICLE_REGENE_STATE_NOTHING:
                break;

            //---------------
            // パーティクル削除
            //---------------
            case PARTICLE_REGENE_STATE_DELETE:
                // パーティクルグループを削除(粒子とグループは次の周期で削除される)
                particleGroup.destroyParticles();
                mRenderParticleOrderBuff.clear();

                // 再生成のシーケンスを生成に更新(次の周期で生成するため)
                mRegenerationState = PARTICLE_REGENE_STATE_CREATE;

                break;

            //---------------
            // パーティクル生成
            //---------------
            case PARTICLE_REGENE_STATE_CREATE:
                // パーティクル生成
                createFluidBody(gl, mWorldPosMid[0], mWorldPosMid[1], mParticleRadius);
                // オーバーラップ物体を生成
                mOverlapBody = createBoxBody(1f, 1f, mWorldPosMid[0], mWorldPosMid[1], 0, BodyType.staticBody);
                // オーバーラップ物体ありに更新
                mRegenerationState = PARTICLE_REGENE_STATE_OVERLAP;

                break;

            //-----------------------
            // オーバーラップ物体あり
            //-----------------------
            case PARTICLE_REGENE_STATE_OVERLAP:
                // オーバーラップ物体を削除
                mWorld.destroyBody(mOverlapBody);
                // オーバーラップシーケンス終了。重複物体を削除した状態。
                mRegenerationState = PARTICLE_REGENE_STATE_NOTHING;

                break;

            default:
                break;
        }
    }

    /*
     * パーティクルの描画情報の更新(頂点バッファ/UVバッファ)
     */
    private void updateParticleDraw(GL10 gl, ParticleGroup pg) {
        /* 粒子がない場合、何もしない */
        if (pg.getParticleCount() == 0) {
            return;
        }

        // 描画情報の更新
        updateParticleCreateDraw(gl);
    }

    /*
     * パーティクル描画更新（Createモード用）
     */
    private void updateParticleCreateDraw(GL10 gl) {
        // マトリクス記憶
        gl.glPushMatrix();
        {
            // テクスチャの指定
            int textureId = mParticleData.getTextureId();
            gl.glActiveTexture(GL10.GL_TEXTURE0);
            gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId);

            // 現時点のパーティクル座標から頂点バッファを計算
            FloatBuffer vertexBuffer = getVertexBuffer();

            // バッファを渡して描画
            gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mRenderUVBuff);
            gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vertexBuffer);
            gl.glDrawArrays(GL10.GL_TRIANGLES, 0, mRenderPointNum);
        }
        // マトリクスを戻す
        gl.glPopMatrix();

        // パーティクルタッチ判定処理
        traceTouchParticle(gl);
    }

    /*
     * 頂点座標バッファの取得
     */
    private FloatBuffer getVertexBuffer() {

        // 頂点座標配列
        int buffSize = mRenderPointNum * 2;
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
        int buffSize = mRenderPointNum * 2;
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

    /*
     * menu背景物体の制御
     */
    private void menuBodyControl() {

        switch (mMenuMoveState) {

            //------------
            // 上／下
            //------------
            case MENU_MOVE_STATE_UP:
            case MENU_MOVE_STATE_DOWN:
                //----------
                // 移動開始
                //----------
                mMenuBody.setType(BodyType.dynamicBody);
                mMenuBody.setLinearVelocity(mMenuVelocity);

                // 移動状態：移動継続
                mMenuMoveState = MENU_MOVE_STATE_KEEP;

                break;

            //------------
            // 移動継続
            //------------
            case MENU_MOVE_STATE_KEEP:
                // 停止要求がくるまで、速度を維持し続ける
                mMenuBody.setLinearVelocity(mMenuVelocity);

                break;

            //------------
            // 停止
            //------------
            case MENU_MOVE_STATE_STOP:
                //----------
                // 移動終了
                //----------
                // bodyをstaticに戻す
                mMenuBody.setType(BodyType.staticBody);

                //------------
                // 位置リセット
                //------------
                // 微妙なズレの蓄積を防ぐため、初期位置に戻ったタイミングで、menu背景物体を再生成
                if (mMenuCollapsedPosY > mMenuBody.getPositionY()) {
                    mWorld.destroyBody(mMenuBody);
                    mMenuBody = createBoxBody(mMenuWidth, mMenuHeight, mMenuInitPosX, mMenuInitPosY, 0, BodyType.staticBody);
                }

                // 移動状態：なし
                mMenuMoveState = MENU_MOVE_STATE_NOTHING;

                break;

            //------------
            // なし
            //------------
            case MENU_MOVE_STATE_NOTHING:
            default:
                break;
        }

    }


    /*
     * 主に landscape と portraid の切り替え (縦向き、横向き切り替え) のときに呼ばれる
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // !==================
        // 参考
        // 端末縦：1080/2042
        // 端末横：2280/861
        // !==================

        // 画面の範囲を指定
        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();

        // 画面のパースペクティブを登録し、どの端末でも同じように描画できるよう設定
        GLU.gluPerspective(gl,
                60f,                          // 縦の視野角を”度”単位で設定
                (float) width / height,            // 縦に対する横方向の視野角の倍率
                1f,                                // 一番近いZ位置を指定
                50f);                              // 一番遠いZ位置を指定

        // カメラの位置・姿勢を決定
        GLU.gluLookAt(gl,
                0, 15, 50,         // カメラの位置（視点）
                0, 15, 0,           // カメラの注視点（カメラが見ているところ）
                0, 1, 0                      // カメラの上方向
        );
    }

    /*
     * レンダークラスの初期化時に呼ばれる
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        //------------------
        // GL
        //------------------
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);               // 頂点座標のバッファをセットしたことをOpenGLに伝える
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);        // テクスチャのマッピング座標のバッファをセットしたことをOpenGLに伝える

        // テクスチャの有効化
        gl.glEnable(GL10.GL_TEXTURE_2D);
        // !これをしないと、画像テクスチャの背景が透過されない
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL10.GL_BLEND);

        //------------------
        // ステータス
        //------------------
        // GLステータス更新
        mGlInitStatus = GLInitStatus.FinInit;
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
        textureId = Conversion.makeTexture( gl10, resourceId, mGLSurfaceView.getContext() );

        //-------------------
        // テクスチャを保持
        //-------------------
        // リソースIDとテクスチャIDをMapとして保持する
        mMapResourceTexture.put(resourceId, textureId);

        return textureId;
    }

    /*
     * onTouch
     */
    public synchronized boolean onTouch(View v, MotionEvent event) {

        boolean onBullet = mBulletManager.onBullet();
        if ( onBullet ) {
            //-------------------
            // 銃弾方向の制御
            //-------------------
            return mBulletManager.controlBulletShootPos(event);

        } else {
            //-------------------
            // パーティクルタッチ
            //-------------------
            return touchParticle(event);

        }
    }

    /*
     * パーティクルタッチ処理
     */
    private boolean touchParticle(MotionEvent event) {

        switch (event.getAction()) {

            // タッチ解除
            case MotionEvent.ACTION_UP:
                // 粒子用：状態更新
                mParticleTouchInfo.setStatus(ParticleTouchInfo.ParticleTouchStatus.OUTSIDE);
                mParticleTouchInfo.setTouchPosX( ParticleTouchInfo.INVALID_TOUCH_POS );
                mParticleTouchInfo.setTouchPosY( ParticleTouchInfo.INVALID_TOUCH_POS );
                break;

            // タッチ移動
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                // 粒子用：タッチ中の位置を更新
                // !world座標への変換は、必要なタイミングで実施する（この時点ではGL10がないため）
                mParticleTouchInfo.setTouchPosX(event.getX());
                mParticleTouchInfo.setTouchPosY(event.getY());

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
    private void traceTouchParticle(GL10 gl) {

        // 銃弾発射中なら処理なし
        boolean onBullet = mBulletManager.onBullet();
        if ( onBullet ){
            return;
        }
        // 未タッチなら処理なし
        if (mParticleTouchInfo.touchPosX == mParticleTouchInfo.INVALID_TOUCH_POS) {
            return;
        }

        //------------------------
        // パーティクル追随判定
        //------------------------
        // 現在のタッチ状態
        ParticleTouchInfo.ParticleTouchStatus currentStatus = getCurrentTouchStatus(gl);

        // 前回のタッチ状態が「境界」「追随」でなければ
        if ((mParticleTouchInfo.status != ParticleTouchInfo.ParticleTouchStatus.BORDER) &&
            (mParticleTouchInfo.status != ParticleTouchInfo.ParticleTouchStatus.TRACE)) {
            // 現状のタッチ状態を更新して終了
            mParticleTouchInfo.status = currentStatus;
            return;
        }

        // 今回のタッチ状態が「外側」以外の場合
        if (currentStatus != ParticleTouchInfo.ParticleTouchStatus.OUTSIDE) {
            // 現状のタッチ状態を更新して終了
            mParticleTouchInfo.status = currentStatus;
            return;
        }

        //------------------------
        // パーティクル追随
        //------------------------
        // 境界のパーティクルをタッチ位置に追随させる
        // （タッチ座標から少しずらした位置に、パーティクルの位置を変更する）
        float tracePosX = mParticleTouchInfo.touchPosWorldX + 0.1f;
        float tracePosY = mParticleTouchInfo.touchPosWorldY + 0.1f;
        mParticleSystem.setParticlePosition(mParticleTouchInfo.borderIndex, tracePosX, tracePosY);

        // 現状のタッチ状態を更新
        mParticleTouchInfo.status = ParticleTouchInfo.ParticleTouchStatus.TRACE;
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
        float range = mParticleData.getParticleRadius() * 2;

        // タッチ判定範囲を算出
        float[] touchPos = Conversion.convertPointScreenToWorld(mParticleTouchInfo.touchPosX, mParticleTouchInfo.touchPosY, gl, mGLSurfaceView);
        float touchMinX = touchPos[0] - range;
        float touchMaxX = touchPos[0] + range;
        float touchMinY = touchPos[1] - range;
        float touchMaxY = touchPos[1] + range;

        // タッチ位置のworld座標を保持
        mParticleTouchInfo.touchPosWorldX = touchPos[0];
        mParticleTouchInfo.touchPosWorldY = touchPos[1];

        //----------------------
        // タッチ判定
        //----------------------
        // 判定前はパーティクルの外側
        ParticleTouchInfo.ParticleTouchStatus status = ParticleTouchInfo.ParticleTouchStatus.OUTSIDE;

        // 全パーティクルを対象にタッチ判定
        int particleNum = mParticleData.getParticleGroup().getParticleCount();
        int index;
        for (index = 0; index < particleNum; index++) {
            // パーティクル位置
            float x = mParticleSystem.getParticlePositionX(index);
            float y = mParticleSystem.getParticlePositionY(index);

            // タッチ範囲にパーティクルあるか
            if ( (x >= touchMinX) && (x <= touchMaxX) && (y >= touchMinY) && (y <= touchMaxY) ) {
                // タッチ状態：パーティクル内部
                status = ParticleTouchInfo.ParticleTouchStatus.INSIDE;
                break;
            }
        }

        //---------------------------
        // タッチしているパーティクルなし
        //---------------------------
        if( status == ParticleTouchInfo.ParticleTouchStatus.OUTSIDE ){
            return status;
        }

        //---------------------------
        // タッチしているパーティクルあり
        //---------------------------
        if ( mParticleData.isBorderParticle(index) ) {
            // タッチ中のパーティクルを保持
            mParticleTouchInfo.setBorderIndex(index);
            // タッチ状態：パーティクル境界
            status = ParticleTouchInfo.ParticleTouchStatus.BORDER;
        }

        return status;
    }

    /*
     * メニューView（展開時）Rect情報の設定
     */
    public void setExpandedMenuRect(float top, float left, float right, float bottom) {
        // メニューが開いている状態の座標情報
        mExpandedMenuTop = top;
        mExpandedMenuLeft = left;
        mExpandedMenuRight = right;
        mExpandedMenuBottom = bottom;
    }

    /*
     * メニューView（折りたたみ時）Rect情報の設定
     */
    public void setCollapsedMenuRect(float top, float left, float right, float bottom){
        // メニューが閉じている状態の座標情報
        mCollapsedMenuTop = top;
        mCollapsedMenuLeft = left;
        mCollapsedMenuRight = right;
        mCollapsedMenuBottom = bottom;
    }

    /*
     * メニューViewRect設定完了
     */
    public void finishSetMenuRect(){
        // メニュー座標設定済み
        mIsSetMenuRect = true;
    }

    /*
     * menu動作の制御要求
     */
    public void moveMenuBody(int direction){

        if( direction == MENU_MOVE_STATE_UP){
            // 上方向を保持
            mMenuVelocity = mMenuUpVelocity;
        }else if( direction == MENU_MOVE_STATE_DOWN ){
            // 下方向を保持
            mMenuVelocity = mMenuDownVelocity;
        }

        // 制御情報を保持
        mMenuMoveState = direction;
    }


    /*
     * 銃弾OnOff切り替え
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void switchBullet(){
        // 発射On/Offを切り替え
        boolean onBullet = mBulletManager.switchBulletOnOff();

        //-----------
        // 銃弾On
        //-----------
        if( onBullet ){
            // 保持している境界パーティクルの位置情報を更新
            mParticleData.updateBorderParticlePosY( mParticleSystem );
            // 画面下部座標値を渡し、発射位置を設定
            mBulletManager.setShootPosY( mWorldPosMin[1] );
        }
    }

    /*
     * パーティクルを中心に再生成
     *
     */
    public void regenerationAtCenter(){
        // 状態を更新し、次の周期で再生成されるようにする
        mRegenerationState = PARTICLE_REGENE_STATE_DELETE;
    }

    /*
     * パーティクルの柔らかさの変更
     */
    public void setSoftness(int softness){
        mSoftness = softness;

        // 指定された柔さでパーティクル再生成
        changeParticleSoftness( softness );
    }

    /*
     * パーティクルの再生成
     * 　ユーザー指定（柔らかさ）に従い、パーティクルを再生成する
     *
     */
    public void changeParticleSoftness(int softness){

        //-------------------------
        // 柔らかさを決めるパラメータ定数
        //-------------------------
        // SOFT
        final float SOFT_RADIUS           = 0.2f;
        final float SOFT_DENCITY          = 0.1f;
        final float SOFT_ELASTIC_STRENGTH = 0.2f;
        // LITTLE_HARD
        final float LITTLE_HARD_RADIUS           = 0.4f;
        final float LITTLE_HARD_DENCITY          = 1.0f;
        final float LITTLE_HARD_ELASTIC_STRENGTH = 1.0f;

        //-------------------------
        // 柔らかさを決めるパラメータ
        //-------------------------
        float radius;
        float dencity;
        float elasticStrength;

        // ユーザー指定に応じて、パラメータを設定
        switch( softness ) {
            case ParticleWorldRenderer.SOFTNESS_SOFT:
                radius          = SOFT_RADIUS;
                dencity         = SOFT_DENCITY;
                elasticStrength = SOFT_ELASTIC_STRENGTH;
                break;

            case ParticleWorldRenderer.SOFTNESS_NORMAL:
                radius          = DEFAULT_RADIUS;
                dencity         = DEFAULT_DENCITY;
                elasticStrength = DEFAULT_ELASTIC_STRENGTH;
                break;

            case ParticleWorldRenderer.SOFTNESS_LITTEL_HARD:
                radius          = LITTLE_HARD_RADIUS;
                dencity         = LITTLE_HARD_DENCITY;
                elasticStrength = LITTLE_HARD_ELASTIC_STRENGTH;
                break;

            default:
                radius          = DEFAULT_RADIUS;
                dencity         = DEFAULT_DENCITY;
                elasticStrength = DEFAULT_ELASTIC_STRENGTH;
                break;
        }

        //-------------------------
        // パーティクル生成
        //-------------------------
        // 半径は保持
        mParticleRadius = radius;
        // パーティクルシステムの生成
        setupParticleSystem( radius, dencity, elasticStrength );
        // 中心に再生成
        regenerationAtCenter();
    }

    /*
     * パーティクルの柔らかさ取得
     */
    public int getSoftness(){
        return mSoftness;
    }

    /*
     * 重力の変更
     */
    public void setGravity(int gravity){

        // 指定された重力を保持
        mGravity = gravity;

        //-----------
        // 重力の設定
        //-----------
        // 設定する重力値
        int gravityScaleY = mGravityScale.get( gravity );
        // 重力を変更
        mWorld.setGravity( 0f, gravityScaleY );

        //-----------
        // 粒子反復
        //-----------
        // 適切な粒子反復を算出 !worldのstepで利用
        mParticleIterations = liquidfun.b2CalculateParticleIterations( gravityScaleY, mParticleRadius, TIME_STEP );
    }

    /*
     * 重力種別を取得
     */
    public int getGravity(){
        return mGravity;
    }
}
