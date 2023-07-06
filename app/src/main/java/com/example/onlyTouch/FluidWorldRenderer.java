package com.example.onlyTouch;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.google.fpl.liquidfun.Body;
import com.google.fpl.liquidfun.BodyDef;
import com.google.fpl.liquidfun.BodyType;
import com.google.fpl.liquidfun.CircleShape;
import com.google.fpl.liquidfun.ParticleFlag;
import com.google.fpl.liquidfun.ParticleGroup;
import com.google.fpl.liquidfun.ParticleGroupDef;
import com.google.fpl.liquidfun.ParticleGroupFlag;
import com.google.fpl.liquidfun.ParticleSystem;
import com.google.fpl.liquidfun.ParticleSystemDef;
import com.google.fpl.liquidfun.PolygonShape;
import com.google.fpl.liquidfun.Vec2;
import com.google.fpl.liquidfun.World;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

/*
 * 物理世界で流体生成・レンダリング
 */
public class FluidWorldRenderer implements GLSurfaceView.Renderer, View.OnTouchListener {

    /* LiquidFun ロード */
    static {
        System.loadLibrary("liquidfun");
        System.loadLibrary("liquidfun_jni");
    }

    /* 物理関連 */
    private World mWorld = null;

    // パーティクル
    private ParticleSystem mParticleSystem;
    private ParticleData mParticleData;
    private int mRegenerationState;
    private long mSetParticleFlg;
    private float mSetParticleRadius;
    private int mSetParticleLifetime;
    ParticleTouchData mParticleTouchData = new ParticleTouchData(-1, -1, ParticleTouchStatus.OUTSIDE, 0xFFFF, 0xFFFF);

    // 静的物体
    private HashMap<Long, BodyData> mMapBodyData = new HashMap<Long, BodyData>();
    private long mBodyDataId = 1;

    // バレット
    private boolean mCannonCtrl = false;
    private HashMap<Long, BodyData> mMapCannonData = new HashMap<Long, BodyData>();
    private ArrayList<Long> mBulletDeleteList = new ArrayList<Long>();
    private int mCannonCreateCycle;

    /* 位置管理 */
    private float[] mWorldPosMax;
    private float[] mWorldPosMid;
    private float[] mWorldPosMin;

    //------------------
    // menu
    //------------------
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

    // 変換後
    private float mMenuCollapsedPosY;

    private float mMenuInitPosX;
    private float mMenuInitPosY;
    private float mMenuWidth;
    private float mMenuHeight;

    // メニュー初期位置設定完了フラグ
    private boolean mIsSetMenuRect = false;

    // アニメーションと一致するmenuの移動速度
    private Vec2 mMenuUpVelocity;
    private Vec2 mMenuDownVelocity;
    private Vec2 mMenuVelocity;

    // menu移動状態
    private int mMenuMoveState = MENU_MOVE_STATE_NOTHING;

    /* OpenGL */
    private FluidGLSurfaceView mMainGlView;
    private GLInitStatus glInitStatus = GLInitStatus.PreInit;

    private HashMap<Integer, Integer> mMapResIdToTextureId = new HashMap<Integer, Integer>();

    PlistDataManager mPlistManage;

    // 描画対象のバッファ
    private ArrayList<Integer> mRenderParticleBuff = new ArrayList<>();    // パーティクル(頂点)座標配列
    private ArrayList<Vec2> mRenderUVBuff = new ArrayList<>();             // UV座標配列
    private int mRenderPointNum;                                           // 描画対象の頂点数

    /* 定数 */
    private static final float TIME_STEP = 1 / 60f; // 60 fps
    private static final int VELOCITY_ITERATIONS = 6;       // 
    private static final int POSITION_ITERATIONS = 2;       // 
    private static final int PARTICLE_ITERATIONS = 1;       // 粒子シミュレーション 小さいほどやわらかくなる。5→固いゼリー

    private static final int CANNON_BULLET_SIZE = 1;
    private static final int CANNON_BULLET_HALF_SIZE = CANNON_BULLET_SIZE / 2;
    private static final int CANNON_BULLET_DOUBLE_SIZE = CANNON_BULLET_SIZE * 2;

    /* その他制御 */
    private boolean mIsCreate = false;                     // createなら、trueに変える

    // Body
    private Body mMenuBody;
    private Body mOverlapBody;

    // OpenGL 描画開始シーケンス
    enum GLInitStatus {
        PreInit,       // 初期化前
        FinInit,       // 初期化完了
        Drawable       // Draw開始
    }

    // パーティクル タッチ状態
    enum ParticleTouchStatus {
        // None,          // 未タッチ
        OUTSIDE,        // 粒子外
        INSIDE,        // 粒子内
        BORDER,        // 境界粒子
        TRACE          // 追随
    }

    // 生成する物体の種別
    enum CreateObjectType {
        BULLET,        // 大砲(弾)型
    }

    // パーティクル再生成シーケンス
    enum RegenerationState {
        DELETE,     // 削除
        CREATE,     // 生成
        OVERLAP,    // 重複物体あり
        END,        // 終了
    }
    // パーティクル再生成シーケンス
    public static final int PARTICLE_REGENE_STATE_DELETE = 0;
    public static final int PARTICLE_REGENE_STATE_CREATE = 1;
    public static final int PARTICLE_REGENE_STATE_OVERLAP = 2;
    public static final int PARTICLE_REGENE_STATE_END = 3;


    // menu背景物体の移動状態
    public static final int MENU_MOVE_STATE_NOTHING = 0;
    public static final int MENU_MOVE_STATE_UP = 1;
    public static final int MENU_MOVE_STATE_DOWN = 2;
    public static final int MENU_MOVE_STATE_KEEP = 3;
    public static final int MENU_MOVE_STATE_STOP = 4;

    /*
     * コンストラクタ
     */
    public FluidWorldRenderer(FluidGLSurfaceView mainGlView, Bitmap bmp, MenuActivity.PictureButton select, ArrayList<Vec2> touchList) {
        mMainGlView = mainGlView;

        // !リファクタリング
        // bmp未指定の場合、Createモードとみなす
        if (bmp == null) {
            mIsCreate = true;
        }

        //-----------------
        // パーティクルの設定
        //-----------------
        // !とりあえず固定で設定
        mSetParticleFlg = ParticleFlag.elasticParticle;
        mSetParticleRadius = 0.2f;  // この値をあげると固くなる
        mSetParticleLifetime = 0;


        // 物理世界生成
        mWorld = new World(0, -10);
        // plist管理クラス
        mPlistManage = new PlistDataManager();
    }

    /*
     *　Bodyの追加
     */
    private void addBodyData(Body body, float[] buffer, float[] uv, int drawMode, int textureId) {
        long id = mBodyDataId++;
        BodyData data = new BodyData(id, body, buffer, uv, drawMode, textureId);
        mMapBodyData.put(id, data);
    }

    /*
     * 銃弾用Bodyの追加
     */
    private void addBulletBodyData(Body body, float[] buffer, float[] uv, int drawMode, int textureId) {
        long id = mBodyDataId++;
        BodyData data = new BodyData(id, body, buffer, uv, drawMode, textureId);
        mMapCannonData.put(id, data);
    }

    /*
     * パーティクル情報の追加
     */
    private void addParticleData(GL10 gl, ParticleGroup pg, float particleRadius, ArrayList<ArrayList<Integer>> allParticleLine, ArrayList<Integer> border, int textureId) {
        mParticleData = new ParticleData(0, mParticleSystem, pg, particleRadius, allParticleLine, border, textureId);
    }

    /*
     *
     */
    public Body addCircle(GL10 gl, float r, float x, float y, float angle, BodyType type, float density, int resId, CreateObjectType object) {
        // Box2d用
        BodyDef bodyDef = new BodyDef();
        bodyDef.setType(type);
        bodyDef.setPosition(x, y);
        bodyDef.setAngle(angle);
        Body body = mWorld.createBody(bodyDef);
        CircleShape shape = new CircleShape();
        shape.setRadius(r);
        body.createFixture(shape, density);

        // OpenGL用
        float vertices[] = new float[32 * 2];
        float uv[] = new float[32 * 2];
        for (int i = 0; i < 32; ++i) {
            float a = ((float) Math.PI * 2.0f * i) / 32;
            vertices[i * 2] = r * (float) Math.sin(a);
            vertices[i * 2 + 1] = r * (float) Math.cos(a);

            uv[i * 2] = ((float) Math.sin(a) + 1.0f) / 2f;
            uv[i * 2 + 1] = (-1 * (float) Math.cos(a) + 1.0f) / 2f;
        }

        // テクスチャ生成
        int textureId = makeTexture(gl, resId);
        // 銃弾
        addBulletBodyData(body, vertices, uv, GL10.GL_TRIANGLE_FAN, textureId);

        return body;
    }

    /*
     * 四角形の物体生成
     */
    public Body addBox(float width, float height, float posx, float posy, float angle, BodyType type) {

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
     * 流体Bodyを生成
     */
    public void addFluidBody(GL10 gl, float hx, float hy, float cx, float cy, float particleRadius, int resId) {

        // パーティクルグループの生成
        ParticleGroupDef pgd = new ParticleGroupDef();
        setParticleGroupDef(pgd, hx, hy, cx, cy);
        ParticleGroup pg = mParticleSystem.createParticleGroup(pgd);

        // 行単位のパーティクルバッファを作成
        ArrayList<ArrayList<Integer>> allParticleLine = new ArrayList<>();
        generateParticleLineBuff(pg, allParticleLine);

        // パーティクルの直径
        float diameter = particleRadius * 2;

        // OpenGLに渡す三角形グルーピングバッファを作成
        generateBottomBaseRendererBuff(allParticleLine, diameter);  // 下辺を基準に(下辺が底辺となるように)グルーピング
        generateTopBaseRendererBuff(allParticleLine, diameter);     // 上辺を基準に(上辺が底辺となるように)グルーピング

        // 頂点数を保持
        mRenderPointNum = mRenderParticleBuff.size();

        // レンダリング用UVバッファを生成
        generateUVRendererBuff();

        // 境界パーティクルバッファを取得
        ArrayList<Integer> border = new ArrayList<>();
        border = generateBorderParticleBuff(allParticleLine);

        int textureId = makeTextureSoftCreate(gl, R.drawable.kitune_tanuki2);

        // パーティクル情報の追加
        addParticleData(gl, pg, particleRadius, allParticleLine, border, textureId);
    }

    /*
     * パーティクルシステムの生成
     */
    private void createParticleSystem(float particleRadius) {
        // パーティクルシステム定義
        ParticleSystemDef psd = new ParticleSystemDef();
        psd.setRadius(particleRadius);
        psd.setDampingStrength(0.2f);
        psd.setDensity(0.5f);
        psd.setGravityScale(0.4f);
        // psd.setGravityScale(2.0f);
        psd.setDestroyByAge(true);
        psd.setLifetimeGranularity(0.0001f);
        psd.setMaxCount(729);                     // 0以外の値を設定する。
        // psd.setMaxCount(1458);
        // psd.setLifetimeGranularity();

        // パーティクルシステムの生成
        mParticleSystem = mWorld.createParticleSystem(psd);
    }

    /*
     * パーティクルグループ定義の設定
     * @para パーティクル横幅、パーティクル縦幅、生成位置(x/y)
     */
    private void setParticleGroupDef(ParticleGroupDef pgd, float hx, float hy, float cx, float cy) {

        // !plistなしで固定
        if (true) {
            PolygonShape shape = new PolygonShape();
            shape.setAsBox(hx, hy, 0, 0, 0);
            pgd.setShape(shape);
        } else {
            // plistにある座標で図形を生成
            int shapenum = mPlistManage.setPlistBuffer(mMainGlView.getContext(), pgd, PlistDataManager.PLIST_KIND.PLIST_RABBIT);
            if (shapenum == -1) {
                // 取得エラーなら、終了
                return;
            }
        }

        pgd.setFlags(mSetParticleFlg);
        pgd.setGroupFlags(ParticleGroupFlag.solidParticleGroup);
        pgd.setPosition(cx, cy);
        pgd.setLifetime(mSetParticleLifetime);
    }

    /*
     * 同一行のパーティクルによるバッファ生成
     *  @para I:パーティクルグループ
     *  @para O:全パーティクルライン
     */
    private void generateParticleLineBuff(ParticleGroup pg, ArrayList<ArrayList<Integer>> allParticleLine) {

        // 対象のパーティクルグループのパーティクル数を算出
        int bufferIndex = pg.getBufferIndex();
        int groupParticleNum = pg.getParticleCount() - bufferIndex;

        // 先頭パーティクルのY座標を保持
        float py = mParticleSystem.getParticlePositionY(bufferIndex);
        ArrayList<Integer> line = new ArrayList<>();

        for (int i = bufferIndex; i < groupParticleNum; ++i) {
            // パーティクルのY座標
            float y = mParticleSystem.getParticlePositionY(i);

            // パーティクルが次のラインのものである場合
            if ((float) Math.abs(py - y) > 0.01f) {
                // パーティクル行を全パーティクルラインに追加し、新規ラインを生成
                allParticleLine.add(line);
                line = new ArrayList<>();
            }

            // ラインにパーティクルを追加し、Y座標を更新
            line.add(i);
            py = y;
        }

        // パーティクル行を全パーティクルラインに追加
        allParticleLine.add(line);
    }

    /*
     * レンダリング用バッファ生成：下辺側が底辺
     *  @para I:ライン毎のパーティクルIndexバッファ
     *  @para I:パーティクルの直径
     *
     * 　下辺が底辺、上辺が頂点となるように、三角形グループ単位でバッファに格納する
     *
     *  【バッファ生成イメージ】
     * 　＜パーティクルイメージ＞
     *    ⑩　⑪
     *　　 ①　②　③
     *
     *  【バッファイメージ】
     *    [0] [1] [2] [3] [4] [5]
     *　　 ①  ②  ⑩   ②  ③  ⑪
     */
    private void generateBottomBaseRendererBuff(ArrayList<ArrayList<Integer>> allParticleLine, float diameter) {

        // ループ数 = ライン数 - 1
        int lastLineIndex = allParticleLine.size() - 1;
        for (int lineIndex = 0; lineIndex < lastLineIndex; lineIndex++) {

            // 下辺と上辺（ある行とその上の行）
            ArrayList<Integer> bottom_line = allParticleLine.get(lineIndex);
            ArrayList<Integer> upper_line = allParticleLine.get(lineIndex + 1);

            // ライン先頭に格納されている「パーティクルシステム側のIndex」
            int bottom_top_ref_index = bottom_line.get(0);
            int upper_top_ref_index = upper_line.get(0);

            // ラインの最後のIndex（バッファ側のIndex）
            int bottom_buf_end_index = bottom_line.size() - 1;
            int upper_buf_end_index = upper_line.size() - 1;
            for (int bottom_offset = 0; bottom_offset < bottom_buf_end_index; bottom_offset++) {

                // 参照するIndex（パーティクルシステム側のIndex）
                int ref_index = bottom_top_ref_index + bottom_offset;

                float bottom_left_x = mParticleSystem.getParticlePositionX(ref_index);
                float bottom_right_x = mParticleSystem.getParticlePositionX(ref_index + 1);

                // 粒子が隣り合っていないなら、グルーピングしない(描画対象外)
                if ((bottom_right_x - bottom_left_x) > diameter) {
                    continue;
                }

                // 上辺側に、三角形の頂点たりうる粒子があるかチェック(左からチェック)
                int upper_offset;
                float upper_x;
                int belongs_col = -1;
                int ref_upper_index = 0;
                for (upper_offset = 0; upper_offset <= upper_buf_end_index; upper_offset++) {

                    // 参照するIndex（パーティクルシステム側のIndex）
                    ref_upper_index = upper_top_ref_index + upper_offset;

                    upper_x = mParticleSystem.getParticlePositionX(ref_upper_index);

                    // 下辺の左側の頂点の直上にあるかチェック
                    if (upper_x == bottom_left_x) {
                        belongs_col = bottom_offset;
                        break;
                    }

                    // 下辺の右側の頂点の直上にあるかチェック
                    if (upper_x == bottom_right_x) {
                        belongs_col = bottom_offset + 1;
                        break;
                    }
                }

                // 頂点に適した粒子がないなら、グルーピングしない(描画対象外)
                if (belongs_col == -1) {
                    continue;
                }

                // 3頂点を描画バッファに格納
                mRenderParticleBuff.add(ref_index);        // 底辺-左
                mRenderParticleBuff.add(ref_index + 1);    // 底辺-右
                mRenderParticleBuff.add(ref_upper_index);  // 頂点
            }
        }
    }

    /*
     * レンダリング用バッファ生成：上辺側が底辺
     *  @para I:ライン毎のパーティクルIndexバッファ
     *  @para I:パーティクルの直径
     *
     * 　上辺が底辺、下辺が頂点となるように、三角形グループ単位でバッファに格納する
     *
     *  【バッファ生成イメージ】
     * 　＜パーティクルイメージ＞
     *    ⑩　⑪  ⑫
     *　　 ①　②　③
     *
     *  【バッファイメージ】
     *    [0] [1] [2] [3] [4] [5]
     *　　 ⑩  ⑪  ②   ⑪   ⑫  ③
     */
    private void generateTopBaseRendererBuff(ArrayList<ArrayList<Integer>> allParticleLine, float diameter) {

        // ループ数 = ライン数 - 1
        int lastLineIndex = allParticleLine.size() - 1;
        for (int lineIndex = lastLineIndex; lineIndex > 0; lineIndex--) {

            // 下辺と上辺（ある行とその下の行）
            ArrayList<Integer> upper_line = allParticleLine.get(lineIndex);
            ArrayList<Integer> bottom_line = allParticleLine.get(lineIndex - 1);

            // List最後尾のIndex
            int upper_buf_end_index = upper_line.size() - 1;
            int bottom_buf_end_index = bottom_line.size() - 1;

            // 行の終端に位置するパーティクルIndex
            int upper_end_ref_index = upper_line.get(upper_buf_end_index);
            int bottom_end_ref_index = bottom_line.get(bottom_buf_end_index);

            // 行の右からチェックしていく
            for (int upper_offset = 0; upper_offset < upper_buf_end_index; upper_offset++) {

                // 参照するIndex（パーティクルシステム側のIndex）右からみていくため、減算していく。
                int ref_index = upper_end_ref_index - upper_offset;

                float upper_right_x = mParticleSystem.getParticlePositionX(ref_index);
                float upper_left_x = mParticleSystem.getParticlePositionX(ref_index - 1);

                // 粒子が隣り合っていないなら、グルーピングしない(描画対象外)
                if ((upper_right_x - upper_left_x) > diameter) {
                    continue;
                }

                // 下辺側に、三角形の頂点たりうる粒子があるかチェック(右からチェック)
                int bottom_offset;
                float bottom_x;
                int belongs_col = -1;
                int ref_bottom_index = 0;
                for (bottom_offset = 0; bottom_offset <= bottom_buf_end_index; bottom_offset++) {

                    // 参照するIndex（パーティクルシステム側のIndex）
                    ref_bottom_index = bottom_end_ref_index - bottom_offset;

                    bottom_x = mParticleSystem.getParticlePositionX(ref_bottom_index);

                    // 下辺の右側の頂点の直上にあるかチェック
                    if (bottom_x == upper_right_x) {
                        belongs_col = upper_buf_end_index - upper_offset;
                        break;
                    }

                    // 下辺の左側の頂点の直上にあるかチェック
                    if (bottom_x == upper_left_x) {
                        belongs_col = upper_buf_end_index - (upper_offset - 1);
                        break;
                    }
                }

                // 頂点に適した粒子がないなら、グルーピングしない(描画対象外)
                if (belongs_col == -1) {
                    continue;
                }

                // 3頂点をバッファに格納
                mRenderParticleBuff.add(ref_index);
                mRenderParticleBuff.add(ref_index - 1);
                mRenderParticleBuff.add(ref_bottom_index);

/*                if( ref_index >= 126 && ref_index <=128 ){
                    Log.i("test", "drawGroupParIndex=\t" + ref_index);
                    Log.i("test", "drawGroupParIndex=\t" + (ref_index - 1));
                    Log.i("test", "drawGroupParIndex=\t" + ref_bottom_index);
                    Log.i("test", "------------------");
                }*/
            }
        }
    }

    /*
     * レンダリング用UVバッファの生成
     *  @para I:なし
     */
    private void generateUVRendererBuff() {

        //-------------------------------------------------
        // パーティクルグループ内の粒子で最小位置と最大位置を取得する
        //-------------------------------------------------
        float minParticleX = 0xFFFF;
        float maxParticleX = -(0xFFFF);
        float minParticleY = 0xFFFF;
        float maxParticleY = -(0xFFFF);

        int particleNum = mParticleSystem.getParticleCount();
        for (int i = 0; i < particleNum; i++) {
            // X座標
            float pos = mParticleSystem.getParticlePositionX(i);
            minParticleX = (Math.min(pos, minParticleX));
            maxParticleX = (Math.max(pos, maxParticleX));

            // Y座標
            pos = mParticleSystem.getParticlePositionY(i);
            minParticleY = (Math.min(pos, minParticleY));
            maxParticleY = (Math.max(pos, maxParticleY));
        }

        // 横幅・縦幅を算出
        float particleMaxWidth = Math.abs(maxParticleX - minParticleX);
        float particleMaxHeight = Math.abs(maxParticleY - minParticleY);

        //-------------------------------------------------
        // UV座標をバッファに格納
        //-------------------------------------------------
        // UV座標の最大・最小・横幅・縦幅
        float minUvX = mPlistManage.getUvMinX();
        float maxUvY = mPlistManage.getUvMaxY();
        float UvMaxWidth = mPlistManage.getUvWidth();
        float UvMaxHeight = mPlistManage.getUvHeight();

        // UV座標を計算し、バッファに保持する
        for (int i : mRenderParticleBuff) {
            float x = mParticleSystem.getParticlePositionX(i);
            float y = mParticleSystem.getParticlePositionY(i);

            float vecx = minUvX + (((x - minParticleX) / particleMaxWidth) * UvMaxWidth);
            float vecy = maxUvY - (((y - minParticleY) / particleMaxHeight) * UvMaxHeight);

            // レンダリング用UVバッファに格納
            mRenderUVBuff.add(new Vec2(vecx, vecy));
        }
    }

    /*
     * 境界パーティクルバッファを取得
     *  @para I:なし
     */
    private ArrayList<Integer> generateBorderParticleBuff(ArrayList<ArrayList<Integer>> allParticleLine) {
        // 境界パーティクルバッファ
        ArrayList<Integer> border = new ArrayList<>();

        int lineNum = allParticleLine.size();
        int lastLineIndex = lineNum - 1;

        // 下辺と上辺は、全てのパーティクルが境界
        ArrayList<Integer> line = allParticleLine.get(0);
        border.addAll(line);
        line = allParticleLine.get(lastLineIndex);
        border.addAll(line);

        // 下辺上辺の間のライン（パーティクルの2ライン目から最終ラインの前のラインまで）
        for (int i = 1; i < lastLineIndex; i++) {
            line = allParticleLine.get(i);

            // 両サイドのパーティクル
            int leftParticleIndex = line.get(0);                   // 左端
            int rightParticleIndex = line.get(line.size() - 1);    // 右端
            // 両サイドのパーティクルが境界
            border.add(leftParticleIndex);
            border.add(rightParticleIndex);
        }

        return border;
    }

    /*
     * フレーム描画初期化処理
     *   true ：フレーム描画可
     *   false：フレーム描画不可
     */
    private boolean initDrawFrame(GL10 gl) {
        // 初期化完了していれば
        if (glInitStatus == GLInitStatus.FinInit) {
            // メニューのサイズが設定されるまで、処理なし
            if (!mIsSetMenuRect && !mIsCreate) {
                return false;
            }

            // world座標の計算
            calculateWorldPosition(gl);

            // 初期配置用の物理体生成
            createPhysicsObject(gl);

            // GL初期化状態を描画可能に更新
            glInitStatus = GLInitStatus.Drawable;

        } else if (glInitStatus == GLInitStatus.PreInit) {
            // 初期化コール前なら何もしない(セーフティ)
            return false;
        }

        return true;
    }


    /*
     * 描画のため繰り返し呼ばれる
     *  @param gl
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        // フレーム描画初期化処理
        boolean initFin = initDrawFrame(gl);
        if (!initFin) {
            return;
        }

        // long startTime = System.nanoTime();

        // 物理世界を更新
        mWorld.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS, PARTICLE_ITERATIONS);

        // 背景色を設定
        // ！これは毎回必要？
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        // ビューの変換行列の作成
        gl.glMatrixMode(GL10.GL_MODELVIEW);   // マトリクス(4x4の変換行列)の指定
        gl.glLoadIdentity();                   // 初期化

        // パーティクル情報
        ParticleGroup pg = mParticleData.getParticleGroup();
        // パーティクル再生成
        regenerationParticle(gl, pg);
        // パーティクル描画更新
        updateParticleDraw(gl, pg);

        //------------------
        // menu背景物体
        //------------------
        menuBodyControl();

        // 弾の管理
        bulletBodyManage(gl);

        // long endTime  = System.nanoTime();
        // Log.d("TIME", "経過時間　" + ((endTime - startTime) / 1000000) + "(ms)");
    }

    /*
     * world座標の計算
     */
    private void calculateWorldPosition(GL10 gl) {

        //---------------------------
        // 画面サイズ
        //---------------------------
        final int screenWidth = mMainGlView.getWidth();
        final int screenHeight = mMainGlView.getHeight();

        //---------------------------
        // 画面端の座標を物理座標に変換
        //---------------------------
        // 画面の端の位置を変換(Y座標は上が0)
        mWorldPosMax = convPointScreenToWorld(screenWidth, 0, gl);
        mWorldPosMid = convPointScreenToWorld(screenWidth / 2f, screenHeight / 2f, gl);
        mWorldPosMin = convPointScreenToWorld(0, screenHeight, gl);
    }

    /*
     * 各種物体生成
     */
    private void createPhysicsObject(GL10 gl) {

        //---------------
        // メニュー
        //---------------
        // メニュー背景の物体生成
        createMenuBody(gl);

        //---------------
        // パーティクル
        //---------------
        // パーティクルシステム生成
        createParticleSystem(mSetParticleRadius);
        // パーティクル生成
        addFluidBody(gl, 4, 4, mWorldPosMid[0], mWorldPosMid[1], mSetParticleRadius, R.drawable.kitune_tanuki2);

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
        float[] worldExpandedMenuTopLeft = convPointScreenToWorld(mExpandedMenuLeft, mExpandedMenuTop, gl);
        float[] worldExpandedMenuTopRight = convPointScreenToWorld(mExpandedMenuRight, mExpandedMenuTop, gl);
        float[] worldExpandedMenuBottomRight = convPointScreenToWorld(mExpandedMenuRight, mExpandedMenuBottom, gl);

        // メニュー（折りたたみ時）の座標変換
        float[] worldCollapsedMenuTopLeft = convPointScreenToWorld(mCollapsedMenuLeft, mCollapsedMenuTop, gl);
        float[] worldCollapsedMenuBottomRight = convPointScreenToWorld(mCollapsedMenuRight, mCollapsedMenuBottom, gl);

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
        mMenuBody = addBox(menuWidth, menuHeight, collapsedMenuPosRight, menuInitPosY, 0, BodyType.staticBody);

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
        Resources resources = mMainGlView.getContext().getResources();
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
        float[] worldMenuPosTopLeft = convPointScreenToWorld(mCollapsedMenuLeft, mCollapsedMenuTop, gl);

        // メニューの存在を考慮した横幅・X座標位置を計算する
        // ※メニュー物体とちょうどの位置だと下がるときうまくいかない時があるため、少し位置を左にする。
        float bottom_width = (worldMenuPosTopLeft[0] - mWorldPosMin[0]) / 2;
        float bottom_posX = mWorldPosMin[0] + bottom_width - 1;

        //---------------
        // 壁の生成
        //---------------
        // 天井
        addBox(mWorldPosMax[0], 1, mWorldPosMid[0], mWorldPosMax[1], 0, BodyType.staticBody);
        // 左
        addBox(1, mWorldPosMax[1], mWorldPosMin[0] - 1, mWorldPosMid[1], 0, BodyType.staticBody);
        // 右
        addBox(1, mWorldPosMax[1], mWorldPosMax[0] + 1, mWorldPosMid[1], 0, BodyType.staticBody);
        // 床(メニューの存在を考慮)
        addBox(bottom_width, 1, bottom_posX, mWorldPosMin[1] - 1, 0, BodyType.staticBody);
    }

    /*
     * パーティクル再生成
     */
    private void regenerationParticle(GL10 gl, ParticleGroup pg) {

        switch (mRegenerationState) {
            //---------------
            // パーティクル削除
            //---------------
            case PARTICLE_REGENE_STATE_DELETE:
                // パーティクルグループを削除(粒子とグループは次の周期で削除される)
                pg.destroyParticles();
                mRenderParticleBuff.clear();
                mRenderUVBuff.clear();

                // 再生成のシーケンスを生成に更新(次の周期で生成するため)
                mRegenerationState = PARTICLE_REGENE_STATE_CREATE;

                break;

            //---------------
            // パーティクル生成
            //---------------
            case PARTICLE_REGENE_STATE_CREATE:
                // パーティクル生成
                addFluidBody(gl, 4, 4, mWorldPosMid[0], mWorldPosMid[1], mSetParticleRadius, R.drawable.kitune_tanuki2);
                // オーバーラップ物体を生成
                mOverlapBody = addBox(1f, 1f, mWorldPosMid[0], mWorldPosMid[1], 0, BodyType.staticBody);
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
                mRegenerationState = PARTICLE_REGENE_STATE_END;

                break;

            //-----------------------
            // 終了
            //-----------------------
            case PARTICLE_REGENE_STATE_END:
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

            // 頂点バッファ・UVバッファを取得
            FloatBuffer vertexBuffer = getVertexBuffer();
            FloatBuffer uvBuffer = getUVBuffer();

            // バッファを渡して描画
            gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, uvBuffer);
            gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vertexBuffer);
            gl.glDrawArrays(GL10.GL_TRIANGLES, 0, mRenderPointNum);
        }
        // マトリクスを戻す
        gl.glPopMatrix();

        // タッチ判定処理
        traceTouchParticle(gl);
    }

    /*
     * 頂点座標バッファの取得
     */
    private FloatBuffer getVertexBuffer() {
        int buffSize = mRenderPointNum * 2;

        // 頂点座標配列
        float[] vertices = new float[buffSize];

        // レンダリングバッファのパーティクルの座標を配列に格納
        int count = 0;
        for (int index : mRenderParticleBuff) {
            vertices[count] = mParticleSystem.getParticlePositionX(index);
            count++;
            vertices[count] = mParticleSystem.getParticlePositionY(index);
            count++;
        }

        // FloatBufferに変換
        FloatBuffer vertexBuffer = convFloatBuffer(vertices);
        return vertexBuffer;
    }

    /*
     * UV座標バッファの取得
     */
    private FloatBuffer getUVBuffer() {
        int buffSize = mRenderPointNum * 2;

        // UV座標配列
        float[] uv = new float[buffSize];

        // レンダリングUVバッファのUV座標を配列に格納
        int count = 0;
        for (Vec2 Coordinate : mRenderUVBuff) {
            uv[count] = Coordinate.getX();
            count++;
            uv[count] = Coordinate.getY();
            count++;
        }

        // FloatBufferに変換
        FloatBuffer uvBuffer = convFloatBuffer(uv);
        return uvBuffer;
    }

    /*
     * menu背景物体の制御
     */
    private void menuBodyControl() {

        switch (mMenuMoveState){

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
                    mMenuBody = addBox(mMenuWidth, mMenuHeight, mMenuInitPosX, mMenuInitPosY, 0, BodyType.staticBody);
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
     *  弾の管理(生成・削除)
     */
    private void bulletBodyManage(GL10 gl) {
        // 大砲が有効なら、弾の生成と発射
        if (mCannonCtrl) {

            // 弾の生成(個/s)
            mCannonCreateCycle++;
            if ((mCannonCreateCycle % 12) == 0) {
                // 形状
                Body body;
                body = addCircle(gl, CANNON_BULLET_SIZE, mWorldPosMid[0], mWorldPosMin[1] + CANNON_BULLET_DOUBLE_SIZE, 0, BodyType.dynamicBody, 0, R.drawable.white, CreateObjectType.BULLET);

                // 上方向に発射
                Vec2 force = new Vec2(0, 10000);
                body.applyForceToCenter(force, true);
                body.setGravityScale(2.0f);

                mCannonCreateCycle = 0;
            }

            // 発射済みの弾の描画
            for (Long key : mMapCannonData.keySet()) {
                BodyData bd = mMapCannonData.get(key);

                gl.glPushMatrix();
                {
                    gl.glTranslatef(bd.getBody().getPositionX(), bd.getBody().getPositionY(), 0);
                    float angle = (float) Math.toDegrees(bd.getBody().getAngle());
                    gl.glRotatef(angle, 0, 0, 1);

                    // テクスチャの指定
                    gl.glActiveTexture(GL10.GL_TEXTURE0);
                    gl.glBindTexture(GL10.GL_TEXTURE_2D, bd.getTextureId());

                    // UVバッファ
                    gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, bd.getUvBuffer());              // 確保したメモリをOpenGLに渡す

                    // 頂点バッファ
                    FloatBuffer buff = bd.getVertexBuffer();
                    gl.glVertexPointer(2, GL10.GL_FLOAT, 0, buff);

                    gl.glDrawArrays(bd.getDrawMode(), 0, bd.getVertexLen());

                }
                gl.glPopMatrix();

                // 下方向に移動したタイミングで削除
                Body body = bd.getBody();
                Log.i("test", "velo X=" + body.getLinearVelocity().getX() + " : velo Y=" + body.getLinearVelocity().getY());
                if (body.getLinearVelocity().getY() < 0 || Math.abs(body.getLinearVelocity().getX()) > 15) {
                    mBulletDeleteList.add(key);
                }
            }

            // 削除対象とした弾を削除
            for (int i = 0; i < mBulletDeleteList.size(); i++) {
                long key = mBulletDeleteList.get(i);
                BodyData bd = mMapCannonData.get(key);
                mWorld.destroyBody(bd.getBody());
                bd.getBody().delete();
                mMapCannonData.remove(key);
            }
            mBulletDeleteList.clear();
        }
    }

    /*
     * 画面座標を物理座標へ変換
     */
    private float[] convPointScreenToWorld(float wx, float wy, GL10 gl) {

        //---------------------------
        // 座標変換のためのデータを取得
        //---------------------------
        GL11 gl11 = (GL11) gl;
        int[] bits = new int[16];
        float[] model = new float[16];
        float[] proj = new float[16];
        gl11.glGetIntegerv(gl11.GL_MODELVIEW_MATRIX_FLOAT_AS_INT_BITS_OES, bits, 0);
        for (int i = 0; i < bits.length; i++) {
            model[i] = Float.intBitsToFloat(bits[i]);
        }
        gl11.glGetIntegerv(gl11.GL_PROJECTION_MATRIX_FLOAT_AS_INT_BITS_OES, bits, 0);
        for (int i = 0; i < bits.length; i++) {
            proj[i] = Float.intBitsToFloat(bits[i]);
        }

        //---------------------
        // 画面サイズ
        //---------------------
        final int screenWidth = mMainGlView.getWidth();
        final int screenHeight = mMainGlView.getHeight();

        //---------------------
        // 座標変換
        //---------------------
        float[] ret = new float[4];
        GLU.gluUnProject(
                wx, (float) screenHeight - wy, 1f,
                model, 0, proj, 0,
                new int[]{0, 0, screenWidth, screenHeight}, 0,
                ret, 0);
        float x = ret[0] / ret[3];
        float y = ret[1] / ret[3];
        // float z = (float)(ret[2] / ret[3]);

        float[] position = {x, y};
        return position;
    }

    /*
     * 主に landscape と portraid の切り替え (縦向き、横向き切り替え) のときに呼ばれる
     * @param gl
     * @param width
     * @param height
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // 参考
        // 端末縦：1080/2042
        // 端末横：2280/861

        // 画面の範囲を指定
        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        GLU.gluPerspective(gl,                         // 画面のパースペクティブを登録し、どの端末でも同じように描画できるよう設定
                60f,                                // 縦の視野角を”度”単位で設定
                (float) width / height,           // 縦に対する横方向の視野角の倍率
                1f,                                // 一番近いZ位置を指定
                50f);                               // 一番遠いZ位置を指定

        GLU.gluLookAt(gl,                             // カメラの位置・姿勢を決定する
                0, 15, 50,            // カメラの位置
                0, 15, 0,       // カメラの注視点
                0, 1, 0                 // カメラの上方向
        );
    }

    // レンダークラスの初期化時に呼ばれる
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        // gl.glEnable(GL10.GL_DEPTH_TEST);
        // gl.glClearColor(0.0f, 0.0f, 1.0f, 1.0f);       // 背景色を指定して背景を描画    青
        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);       // 背景色を指定して背景を描画
        gl.glEnable(GL10.GL_LIGHTING);                                // ライティングを有効化
        gl.glEnable(GL10.GL_LIGHT0);                                   // 光源の指定。GL_LIGHT0 から GL_LIGHT7 番までの8つの光源がある。
        gl.glDepthFunc(GL10.GL_LEQUAL);                                // 深度値と深度バッファの震度を比較する関数の指定。GL_LEQUALは入って来る深度値が格納された深度値以下である時に通過
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);                 // 頂点座標のバッファをセットしたことをOpenGLに伝える
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);         // テクスチャのマッピング座標のバッファをセットしたことをOpenGLに伝える

        // テクスチャの有効化
        gl.glEnable(GL10.GL_TEXTURE_2D);                              // テクスチャの利用を有効にする
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL10.GL_BLEND);

        // ステータス更新
        glInitStatus = GLInitStatus.FinInit;
    }

    /*
     * テクスチャの生成
     * テクスチャは引数にて指定する。
     */
    private int makeTexture(GL10 gl10, int resId) {
        Integer texId = mMapResIdToTextureId.get(resId);
        if (texId != null) {
            return texId;
        }

        // リソースIDから、Bitmapオブジェクトを生成
        Resources r = mMainGlView.getContext().getResources();
        Bitmap bmp = BitmapFactory.decodeResource(r, resId);

        // テクスチャのメモリ確保
        int[] textureIds = new int[1];                  // テクスチャは一つ
        gl10.glGenTextures(1, textureIds, 0);   // テクスチャオブジェクトの生成。para2にIDが納められる。

        // テクスチャへのビットマップ指定
        gl10.glActiveTexture(GL10.GL_TEXTURE0);                                     // テクスチャユニットを選択
        gl10.glBindTexture(GL10.GL_TEXTURE_2D, textureIds[0]);                      // テクスチャIDとGL_TEXTURE_2Dをバインド
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);            // バインドされたテクスチャにBitmapをセットする

        // テクスチャのフィルタ指定
        gl10.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        gl10.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);

        // リソースIDとテクスチャIDを保持
        mMapResIdToTextureId.put(resId, textureIds[0]);
        return textureIds[0];
    }

    /*
     * テクスチャの生成(パーティクル用)
     *   テクスチャは、引数ではなく画面遷移時に指定されたBitmapを対象にする。
     */
    private int makeTextureSoftCreate(GL10 gl10, int resId) {
        Integer texId = mMapResIdToTextureId.get(resId);
        if (texId != null) {
            return texId;
        }

        // リソースIDから、Bitmapオブジェクトを生成
        Resources r = mMainGlView.getContext().getResources();
        Bitmap bmp = BitmapFactory.decodeResource(r, resId);

        // テクスチャのメモリ確保
        int[] textureIds = new int[1];                  // テクスチャは一つ
        gl10.glGenTextures(1, textureIds, 0);   // テクスチャオブジェクトの生成。para2にIDが納められる。

        // テクスチャへのビットマップ指定
        gl10.glActiveTexture(GL10.GL_TEXTURE0);                                     // テクスチャユニットを選択
        gl10.glBindTexture(GL10.GL_TEXTURE_2D, textureIds[0]);                      // テクスチャIDとGL_TEXTURE_2Dをバインド
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);            // バインドされたテクスチャにBitmapをセットする

        // テクスチャのフィルタ指定
        gl10.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        gl10.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);

        // リソースIDとテクスチャIDを保持
        mMapResIdToTextureId.put(resId, textureIds[0]);
        return textureIds[0];
    }

    /*
     * float配列をFloatBufferに変換
     */
    public static FloatBuffer convFloatBuffer(float[] array) {
        FloatBuffer fb = ByteBuffer.allocateDirect(array.length * 4).order(
                ByteOrder.nativeOrder()).asFloatBuffer();
        fb.put(array).position(0);
        return fb;
    }

    /*
     * タッチコールバック
     */
    public synchronized boolean onTouch(View v, MotionEvent event) {

        // !リファクタリング。同じ情報を複数で持っているため、一括管理したい。

        switch (event.getAction()) {
            // タッチ開始
            case MotionEvent.ACTION_DOWN:

                break;

            // タッチ解除
            case MotionEvent.ACTION_UP:
                // 粒子用：状態更新
                mParticleTouchData.setStatus(ParticleTouchStatus.OUTSIDE);
                mParticleTouchData.setBorderIndex(-1);
                mParticleTouchData.setFollowingIndex(-1);
                mParticleTouchData.setTouchPosX(0xFFFF);
                mParticleTouchData.setTouchPosY(0xFFFF);

                break;

            // タッチ移動
            case MotionEvent.ACTION_MOVE:
                // 粒子用：タッチ中の位置を更新
                mParticleTouchData.setTouchPosX(event.getX());
                mParticleTouchData.setTouchPosY(event.getY());
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

        // 未タッチなら処理なし
        if (mParticleTouchData.touchPosX == 0xFFFF) {
            return;
        }

        // 現在のタッチ状態を判定
        ParticleTouchStatus status = checkCurrentTouchStatus(gl);

        // タッチ位置にパーティクルを追随させるかどうか
        // 前回の判定結果が、「境界」or「追随」で、かつ、今回の判定結果が「外側」であれば、追随させる
        if (((mParticleTouchData.status == ParticleTouchStatus.BORDER) ||
                (mParticleTouchData.status == ParticleTouchStatus.TRACE))
                && (status == ParticleTouchStatus.OUTSIDE)) {

            // タッチ状態を追随に更新
            status = ParticleTouchStatus.TRACE;

            // 境界のパーティクルをタッチ位置に付随させる
            float tracePosX = mParticleTouchData.touchPosWorldX + 0.1f;
            float tracePosY = mParticleTouchData.touchPosWorldY + 0.1f;
            mParticleSystem.setParticlePosition(mParticleTouchData.borderIndex, tracePosX, tracePosY);
        }

        // 現状のタッチ状態を更新
        mParticleTouchData.status = status;
    }

    /*
     * 現在のパーティクルに対するタッチ状態を判定
     */
    private ParticleTouchStatus checkCurrentTouchStatus(GL10 gl) {

        // パーティクルの半径
        float radius = mParticleData.getParticleRadius();

        // タッチ判定範囲
        float[] touchPos = convPointScreenToWorld(mParticleTouchData.touchPosX, mParticleTouchData.touchPosY, gl);
        float minX = touchPos[0] - radius;
        float maxX = touchPos[0] + radius;
        float minY = touchPos[1] - radius;
        float maxY = touchPos[1] + radius;

        // タッチ位置のworld座標
        mParticleTouchData.touchPosWorldX = touchPos[0];
        mParticleTouchData.touchPosWorldY = touchPos[1];

        // 判定前はパーティクルの外側
        ParticleTouchStatus status = ParticleTouchStatus.OUTSIDE;

        // タッチ判定
        int particleNum = mParticleData.getParticleGroup().getParticleCount();
        for (int index = 0; index < particleNum; index++) {
            float x = mParticleSystem.getParticlePositionX(index);
            float y = mParticleSystem.getParticlePositionY(index);

            // タッチした箇所に粒子があるかを判定
            if ((x >= minX) && (x <= maxX) && (y >= minY) && (y <= maxY)) {
                // タッチ状態 → 粒子内部
                status = ParticleTouchStatus.INSIDE;

                // その粒子が境界粒子か判定
                if (mParticleData.getBorder().contains(index)) {
                    // タッチ状態 → 境界
                    mParticleTouchData.borderIndex = index;
                    status = ParticleTouchStatus.BORDER;
                }
                break;
            }
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
     * 大砲の制御要求
     */
    public void reqCannonCtrl(boolean enable){
        // 要求を保持
        mCannonCtrl = enable;

        // 大砲キャンセル時
        if(!enable){
            // 削除漏れに対応
            for( Long key: mMapCannonData.keySet() ){
                BodyData bd = mMapCannonData.get(key);
                mWorld.destroyBody(bd.getBody());
            }
            mMapCannonData.clear();
            mCannonCreateCycle = 0;
        }
    }

    /*
     * パーティクルの再生成要求
     */
    public void reqRegeneration(){
        mRegenerationState = PARTICLE_REGENE_STATE_DELETE;
    }

    /*
     * 重力を画面の向きに合わせて設定するかどうか
     */
    public void reqGravityDirection(boolean direction){

    }

    /*
     * ピン止め
     */
    public void reqSetPin(boolean pin){

    }


}
