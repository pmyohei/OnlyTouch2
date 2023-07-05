package com.example.onlyTouch;

public class ParticleTouchData {

    int borderIndex;
    int followingIndex;
    FluidWorldRenderer.ParticleTouchStatus status;
    float touchPosX;
    float touchPosY;
    float touchPosWorldX;
    float touchPosWorldY;

    public ParticleTouchData(int border, int following, FluidWorldRenderer.ParticleTouchStatus status, float touchx, float touchy){
        this.borderIndex = border;
        this.followingIndex = following;
        this.status = status;
        this.touchPosX = touchx;
        this.touchPosY = touchy;
    }

    public int getBorderIndex() {
        return borderIndex;
    }

    public void setBorderIndex(int borderIndex) {
        this.borderIndex = borderIndex;
    }

    public int getFollowingIndex() {
        return followingIndex;
    }

    public void setFollowingIndex(int followingIndex) {
        this.followingIndex = followingIndex;
    }

    public FluidWorldRenderer.ParticleTouchStatus getStatus() {
        return status;
    }

    public void setStatus(FluidWorldRenderer.ParticleTouchStatus status) {
        this.status = status;
    }

    public float getTouchPosX() {
        return touchPosX;
    }

    public void setTouchPosX(float touchPosX) {
        this.touchPosX = touchPosX;
    }

    public float getTouchPosY() {
        return touchPosY;
    }

    public void setTouchPosY(float touchPosY) {
        this.touchPosY = touchPosY;
    }


}
