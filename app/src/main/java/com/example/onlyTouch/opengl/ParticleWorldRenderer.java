package com.example.onlyTouch.opengl;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.view.MotionEvent;
import android.view.View;

import com.example.onlyTouch.convert.Conversion;
import com.example.onlyTouch.object.BulletManager;
import com.example.onlyTouch.object.DrawBackGround;
import com.example.onlyTouch.particle.ParticleManager;
import com.google.fpl.liquidfun.Body;
import com.google.fpl.liquidfun.BodyDef;
import com.google.fpl.liquidfun.BodyType;
import com.google.fpl.liquidfun.PolygonShape;
import com.google.fpl.liquidfun.Vec2;
import com.google.fpl.liquidfun.World;
import com.google.fpl.liquidfun.liquidfun;

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
    private final ParticleManager mParticleManager;
    private int mParticleCreateFlow;

    // パーティクル生成フロー
    public static final int PARTICLE_GENERATION_FLOW_NOTHING = 0;
    public static final int PARTICLE_GENERATION_FLOW_DELETE = 1;
    public static final int PARTICLE_GENERATION_FLOW_CREATE = 2;
    public static final int PARTICLE_GENERATION_FLOW_OVERLAP = 3;

    //----------------
    // Body
    //----------------
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

    // menu折りたたみ時のRect情報
    private float mCollapsedMenuTop;
    private float mCollapsedMenuLeft;
    private float mCollapsedMenuRight;
    private float mCollapsedMenuBottom;

    //------------------
    // OpenGL
    //------------------
    private GLInitStatus mGlInitStatus;
    private final ParticleGLSurfaceView mGLSurfaceView;
    private final HashMap<Integer, Integer> mMapResourceTexture;

    // OpenGL 描画開始シーケンス
    enum GLInitStatus {
        PreInit,       // 初期化前
        FinInit,       // 初期化完了
        Drawable       // Draw開始
    }


    /*
     * コンストラクタ
     */
    public ParticleWorldRenderer(ParticleGLSurfaceView glSurfaceView) {
        mGLSurfaceView = glSurfaceView;

        //--------------
        // 物理世界生成
        //--------------
        // world生成
        mGravity = GRAVITY_DEFAULT;
        int gravity = mGravityScale.get(mGravity);
        mWorld = new World(0, gravity);

        //-----------------
        // パーティクルの設定
        //-----------------
        mParticleManager = new ParticleManager( glSurfaceView, mWorld );
        // パーティクル生成状態を「生成」とし、新規生成されるようにする
        mParticleCreateFlow = PARTICLE_GENERATION_FLOW_CREATE;

        // 適切な粒子反復を算出
        mParticleIterations = liquidfun.b2CalculateParticleIterations(gravity, ParticleManager.DEFAULT_RADIUS, TIME_STEP);

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
    }

    /*
     * 四角形Bodyの生成
     */
    public Body createBoxBody(float width, float height, Vec2 pos, Vec2 center, BodyType type) {

        //----------------
        // Body生成
        //----------------
        // 定義
        BodyDef bodyDef = new BodyDef();
        bodyDef.setType(type);
        bodyDef.setPosition(pos.getX(), pos.getY());
        // 形状
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width, height, center.getX(), center.getY(), 0);    // para 3,4：ローカル座標におけるボックスの中心

        // 生成
        final float DENSITY = 10f;
        Body body = mWorld.createBody(bodyDef);
        body.createFixture(shape, DENSITY);

        return body;
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
        if ( !initFin ) {
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
        // パーティクル生成フローの途中でなければ、銃弾処理を行う
        if( mParticleCreateFlow == PARTICLE_GENERATION_FLOW_NOTHING ){
            // 弾 !パーティクルよりも先に描画すること（パーティクル内部に弾が描画されることがあるため）
            mBulletManager.bulletManage(gl, mParticleManager);
        }

        //------------------
        // パーティクル
        //------------------
        // パーティクル再生成制御
        createParticleFlow(gl);
        // パーティクル描画更新
        mParticleManager.draw(gl);

        // パーティクルタッチ追随処理
        if( !mBulletManager.onBullet() ){
            mParticleManager.traceTouchParticle(gl);
        }
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
        int textureID = getTexture(gl, DrawBackGround.BG_TEXTURE_RES);
        mDrawBackGround = new DrawBackGround( mGLSurfaceView.getContext(), mWorldPosMin, mWorldPosMax, textureID );

        //---------------
        // メニュー
        //---------------
        // メニュー背景の物体生成
        createMenuBody(gl);

        //---------------
        // パーティクル
        //---------------
        // !パーティクル生成は、パーティクル生成シーケンス上で行うため、ここでは実施しない

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
        // メニュー（折りたたみ時）の座標変換
        float[] worldCollapsedMenuTopLeft = Conversion.convertPointScreenToWorld(mCollapsedMenuLeft, mCollapsedMenuTop, gl, mGLSurfaceView);
        float[] worldCollapsedMenuBottomRight = Conversion.convertPointScreenToWorld(mCollapsedMenuRight, mCollapsedMenuBottom, gl, mGLSurfaceView);

        //-------------------------------
        // サイズ・位置
        //-------------------------------
        // menuサイズ  !メニュービューの半分のサイズ（半分にすると適切なサイズに調整されるのは要調査）
        float menuWidth = (worldCollapsedMenuBottomRight[0] - worldCollapsedMenuTopLeft[0]) / 2;
        float collapsedHeight = (worldCollapsedMenuTopLeft[1] - worldCollapsedMenuBottomRight[1]) / 2;

        // menuサイズ：半分
        float menuHalfWidth = menuWidth / 2f;
        float collapsedHalfHeight = collapsedHeight / 2f;

        // 位置・ボックスの中心
        Vec2 pos = new Vec2( worldCollapsedMenuTopLeft[0] + menuHalfWidth, mWorldPosMin[1] + collapsedHalfHeight );
        Vec2 center = new Vec2( menuHalfWidth, collapsedHalfHeight );

        //-------------------------------
        // 生成
        //-------------------------------
        createBoxBody(menuWidth, collapsedHeight, pos, center, BodyType.staticBody);
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
        Vec2 boxCenter = new Vec2( 0, 0 );
        // サイズ：短い方
        final float WALL_SIZE = 1f;
        // 位置
        Vec2 topPos    = new Vec2( mWorldPosMid[0], mWorldPosMax[1] );
        Vec2 leftPos   = new Vec2( mWorldPosMin[0] - WALL_SIZE, mWorldPosMid[1] );
        Vec2 rightPos  = new Vec2( mWorldPosMax[0] + WALL_SIZE, mWorldPosMid[1] );
        Vec2 bottomPos = new Vec2( bottom_posX, mWorldPosMin[1] - WALL_SIZE );

        // 天井
        createBoxBody(mWorldPosMax[0], WALL_SIZE, topPos, boxCenter, BodyType.staticBody);
        // 左
        createBoxBody(WALL_SIZE, mWorldPosMax[1], leftPos, boxCenter, BodyType.staticBody);
        // 右
        createBoxBody(WALL_SIZE, mWorldPosMax[1], rightPos, boxCenter, BodyType.staticBody);
        // 床(メニューの存在を考慮)
        createBoxBody(bottom_width, WALL_SIZE, bottomPos, boxCenter, BodyType.staticBody);
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
     * パーティクル生成フロー制御
     */
    private void createParticleFlow(GL10 gl) {

        //------------------------
        // パーティクル生成シーケンス
        //------------------------
        switch (mParticleCreateFlow) {

            //-----------------------
            // 処理なし
            //-----------------------
            case PARTICLE_GENERATION_FLOW_NOTHING:
                break;

            //---------------
            // パーティクル削除
            //---------------
            case PARTICLE_GENERATION_FLOW_DELETE:
                // パーティクルグループを削除(粒子とグループは次の周期で削除される)
                mParticleManager.destroyParticle();

                // 再生成のシーケンスを生成に更新(次の周期で生成するため)
                mParticleCreateFlow = PARTICLE_GENERATION_FLOW_CREATE;

                break;

            //---------------
            // パーティクル生成
            //---------------
            case PARTICLE_GENERATION_FLOW_CREATE:

                //-----------------
                // パーティクル生成
                //-----------------
                // 画面の中心
                final float screenMiddlePosX = mWorldPosMid[0];
                final float screenMiddlePosY = mWorldPosMid[1];

                // パーティクル生成
                mParticleManager.createParticleBody(gl, screenMiddlePosX, screenMiddlePosY);

                //----------------------
                // オーバーラップ物体
                //----------------------
                // サイズ
                final float OVERLAP_BODY_WIDTH = 1.4f;
                final float OVERLAP_BODY_HEIGHT = 1.4f;

                // オーバーラップ物体を生成
                Vec2 pos = new Vec2( screenMiddlePosX, screenMiddlePosY );
                Vec2 center = new Vec2( OVERLAP_BODY_WIDTH / 2, OVERLAP_BODY_HEIGHT / 2 );
                mOverlapBody = createBoxBody(OVERLAP_BODY_WIDTH, OVERLAP_BODY_HEIGHT, pos, center, BodyType.staticBody);

                // オーバーラップ物体ありに更新
                mParticleCreateFlow = PARTICLE_GENERATION_FLOW_OVERLAP;

                break;

            //-----------------------
            // オーバーラップ物体あり
            //-----------------------
            case PARTICLE_GENERATION_FLOW_OVERLAP:
                // オーバーラップ物体を削除
                mWorld.destroyBody(mOverlapBody);

                // オーバーラップシーケンス終了。重複物体を削除した状態。
                mParticleCreateFlow = PARTICLE_GENERATION_FLOW_NOTHING;

                break;

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
            return mParticleManager.touchParticle(event);
        }
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
            mParticleManager.updateBorderParticlePosY();
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
        mParticleCreateFlow = PARTICLE_GENERATION_FLOW_DELETE;
    }

    /*
     * パーティクルの柔らかさの変更
     */
    public void setSoftness(int softness){

        // 変更されていなければ、何もしない
        int currentSoftness = mParticleManager.getSoftness();
        if( softness == currentSoftness ){
            return;
        }

        // 指定された柔さでパーティクル再生成
        changeParticleSoftness( softness );
    }

    /*
     * パーティクルの再生成
     * 　ユーザー指定（柔らかさ）に従い、パーティクルを再生成する
     *
     */
    public void changeParticleSoftness(int softness){

        // 柔らかさ因子の変更
        mParticleManager.setSoftnessFactor( softness );

        //-------------------------
        // パーティクル再生成
        //-------------------------
        // パーティクルシステムの生成
        mParticleManager.setParticleSystem();
        // 中心に再生成
        regenerationAtCenter();
    }

    /*
     * パーティクルの柔らかさ取得
     */
    public int getSoftness(){
        return mParticleManager.getSoftness();
    }

    /*
     * 重力の変更
     */
    public void setGravity(int gravity){

        // 変更されていなければ、何もしない
        if( gravity == mGravity ){
            return;
        }

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
        float radius = mParticleManager.getParticleRadius();
        mParticleIterations = liquidfun.b2CalculateParticleIterations( gravityScaleY, radius, TIME_STEP );


        // 指定された重力を保持
        mGravity = gravity;
    }

    /*
     * 重力種別を取得
     */
    public int getGravity(){
        return mGravity;
    }
}
