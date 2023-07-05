package com.example.onlyTouch;

import com.google.fpl.liquidfun.ParticleGroup;
import com.google.fpl.liquidfun.ParticleSystem;

import java.util.ArrayList;

public class ParticleData {
    long mID;
    //ParticleSystem particleSystem;
    ParticleGroup mParticleGroup;
    float mParticleRadius;                          //設定したradius
    float mParticleActualRadius;                    //初期配置位置から計算したradius
    int mTextureId;
    ArrayList<Integer> mTextureIdList;
    ArrayList<ArrayList<Integer>> mAllParticleLine; //ライン毎のパーティクルIndexリスト
    ArrayList<Integer> mBorderParticle;             //境界粒子

    public ParticleData(long id, ParticleSystem ps, ParticleGroup pg, float particleRadius, ArrayList<ArrayList<Integer>> allParticleLine, ArrayList<Integer> border, int textureId) {
        mID = id;
        mParticleGroup = pg;
        mTextureId = textureId;
        mParticleRadius = particleRadius;
        mAllParticleLine = allParticleLine;
        mBorderParticle = border;

        mParticleActualRadius = (ps.getParticlePositionX(1) - ps.getParticlePositionX(0)) / 2;
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

    public ArrayList<Integer> getBorder() {
        return mBorderParticle;
    }

}
