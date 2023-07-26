package com.example.onlyTouch.particle;

import android.os.Build;
import android.support.annotation.RequiresApi;

import com.example.onlyTouch.R;
import com.example.onlyTouch.opengl.ParticleWorldRenderer;
import com.google.fpl.liquidfun.ParticleGroup;
import com.google.fpl.liquidfun.ParticleSystem;

import java.util.ArrayList;
import java.util.HashMap;

/*
 * パーティクル情報
 */
public class ParticleData {

    public final int NOT_FOUND = -1;

    //-------------------------
    // 柔らかさを決めるパラメータ定数
    //-------------------------
    // SOFT
    public static final float SOFT_RADIUS           = 0.2f;
    public static final float SOFT_DENCITY          = 0.1f;
    public static final float SOFT_ELASTIC_STRENGTH = 0.2f;
    // パーティクルの柔らかさパラメータ：デフォルト値
    public static final float DEFAULT_RADIUS           = 0.3f;
    public static final float DEFAULT_DENCITY          = 0.5f;
    public static final float DEFAULT_ELASTIC_STRENGTH = 0.25f;
    // LITTLE_HARD
    public static final float LITTLE_HARD_RADIUS           = 0.4f;
    public static final float LITTLE_HARD_DENCITY          = 1.0f;
    public static final float LITTLE_HARD_ELASTIC_STRENGTH = 1.0f;

    // パーティクルの柔らかさ
    private int mSoftness;
    public static final int SOFTNESS_SOFT        = 0;       // 柔らかめ
    public static final int SOFTNESS_NORMAL      = 1;       // ノーマル（デフォルト）
    public static final int SOFTNESS_LITTEL_HARD = 2;       // 少し固め


    private int mTextureId;
    private ParticleGroup mParticleGroup;
    // 境界パーティクル
    private HashMap<Integer, Float> mBorderParticle;

    // パーティクルの柔らかさ決定因子
    private float mParticleRadius;
    private float mParticleDencity;
    private float mParticleElasticStrength;

    // リソースID
    public static final int TEXTURE_ID = R.drawable.texture_test_cat;
    public static final int POLYGON_XML_ID = R.xml.test_cat_plist;


    /*
     * コンストラクタ
     */
    public ParticleData() {

        mSoftness = SOFTNESS_NORMAL;
        mParticleRadius = DEFAULT_RADIUS;
        mParticleDencity = DEFAULT_DENCITY;
        mParticleElasticStrength = DEFAULT_ELASTIC_STRENGTH;
    }

    /*
     * getter/setter
     */
    public ParticleGroup getParticleGroup() {
        return mParticleGroup;
    }
    public void setParticleGroup(ParticleGroup particleGroup) {
        mParticleGroup = particleGroup;
    }
    public int getTextureId() { return mTextureId;}
    public float getParticleRadius() { return mParticleRadius;}
    public void setParticleRadius(float particleRadius) {
        mParticleRadius = particleRadius;
    }
    public void setTextureId(int textureId) {
        mTextureId = textureId;
    }
    public float getParticleDencity() { return mParticleDencity;}
    public float getParticleElasticStrength() { return mParticleElasticStrength;}
    public int getSoftness() {
        return mSoftness;
    }
    public void setSoftness(int softness) {
        mSoftness = softness;
    }

    /*
     * 境界パーティクル情報初期化
     */
    public void initBorderParticle(ParticleSystem ps, ArrayList<Integer> border) {

        mBorderParticle = new HashMap<Integer, Float>();

        for( int particleIndex : border ){
            // 境界パーティクルのY座標
            float posY = ps.getParticlePositionY(particleIndex);
            // indexとY座標をペアで保持
            mBorderParticle.put(particleIndex, posY);
        }
    }

    /*
     * 境界パーティクルが急上昇したかどうか
     *  　本メソッドがコールされたタイミングで、位置が急上昇したパーティクルがあればそれを返す
     */
    public int tooRiseBorderParticle(ParticleSystem ps) {

        // 急上昇判定値
        final float TOO_RISE = 1.8f;

        //-----------
        // 検索
        //-----------
        // 全境界パーティクルの位置を確認
        for (int borderIndex : mBorderParticle.keySet()) {

            // 位置
            float preY = mBorderParticle.get(borderIndex);
            float currentY = ps.getParticlePositionY(borderIndex);

            // 急上昇したとみなせる程、前回位置よりも上にある場合
            if( (currentY - preY) >= TOO_RISE ){
                return borderIndex;
            }
        }

        // なし
        return NOT_FOUND;
    }

    /*
     * 境界パーティクルのY座標を保持
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void updateBorderParticlePosY(ParticleSystem ps) {

        // 保持中の境界パーティクルの位置情報をコール時点の情報に更新
        for (int borderIndex : mBorderParticle.keySet()) {
            float currentY = ps.getParticlePositionY(borderIndex);
            mBorderParticle.replace( borderIndex, currentY );
        }
    }

    /*
     * 境界パーティクル判定
     *   指定されたIndexが境界パーティクルであるか判定する
     */
    public boolean isBorderParticle( int searchIndex) {

        // 境界パーティクルの中から検索する
        for (int borderIndex : mBorderParticle.keySet()) {
            if( borderIndex == searchIndex ){
                return true;
            }
        }

        // なし
        return false;
    }

    /*
     * 柔らかさ因子の設定
     */
    public void setSoftnessFactor( int softness ){

        //-------------------------
        // 柔らかさの決定因子
        //-------------------------
        float radius;
        float dencity;
        float elasticStrength;

        // 指定に応じて、パラメータを設定
        switch( softness ) {
            case SOFTNESS_SOFT:
                radius          = SOFT_RADIUS;
                dencity         = SOFT_DENCITY;
                elasticStrength = SOFT_ELASTIC_STRENGTH;
                break;

            case SOFTNESS_NORMAL:
                radius          = DEFAULT_RADIUS;
                dencity         = DEFAULT_DENCITY;
                elasticStrength = DEFAULT_ELASTIC_STRENGTH;
                break;

            case SOFTNESS_LITTEL_HARD:
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

        // 因子を更新
        mParticleRadius = radius;
        mParticleDencity = dencity;
        mParticleElasticStrength = elasticStrength;

        mSoftness = softness;
    }
}
