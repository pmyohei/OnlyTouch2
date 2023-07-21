package com.example.onlyTouch.particle;

import android.os.Build;
import android.support.annotation.RequiresApi;

import com.google.fpl.liquidfun.ParticleGroup;
import com.google.fpl.liquidfun.ParticleSystem;

import java.util.ArrayList;
import java.util.HashMap;

public class ParticleData {

    public final int NOT_FOUND = -1;


    long mID;
    int mTextureId;
    ParticleGroup mParticleGroup;
    ArrayList<Integer> mTextureIdList;
    ArrayList<ArrayList<Integer>> mAllParticleLine;     // ライン毎のパーティクルIndexリスト
    private HashMap<Integer, Float> mBorderParticle;    // 境界粒子

    // 設定したradius
    float mParticleRadius;
    // 初期配置位置から計算したradius
    float mParticleActualRadius;


    public ParticleData(long id, ParticleSystem ps, ParticleGroup pg, float particleRadius, ArrayList<ArrayList<Integer>> allParticleLine, ArrayList<Integer> border, int textureId) {
        mID = id;
        mParticleGroup = pg;
        mTextureId = textureId;
        mParticleRadius = particleRadius;
        mAllParticleLine = allParticleLine;

        // 初期配置位置から計算したradius
        mParticleActualRadius = (ps.getParticlePositionX(1) - ps.getParticlePositionX(0)) / 2;
        // 境界パーティクルのY座標を保持
        initBorderParticle(ps, border);
    }

    public long getId() {
        return mID;
    }

    public ParticleGroup getParticleGroup() {
        return mParticleGroup;
    }
    public void setParticleGroup(ParticleGroup particleGroup) {
        mParticleGroup = particleGroup;
    }

    public int getTextureId() { return mTextureId;}

    public ArrayList<Integer> getTextureIdList() {
        return mTextureIdList;
    }
    public void setTextureIdList(ArrayList<Integer> textureIdList) {
        mTextureIdList = textureIdList;
    }

    public float getParticleRadius() { return mParticleRadius;}

    public float getParticleActualRadius() { return mParticleActualRadius;}

    public ArrayList<ArrayList<Integer>> getAllParticleLine() { return mAllParticleLine;}
    public void setAllParticleLine(ArrayList<ArrayList<Integer>> allParticleLine) {
        mAllParticleLine = allParticleLine;
    }



    /*
     * 境界パーティクル情報初期化
     */
    private void initBorderParticle(ParticleSystem ps, ArrayList<Integer> border) {

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
}
