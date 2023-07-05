package com.example.onlyTouch;

public class FlickObjectData {
    float posX;
    float posY;
    float deltaX;
    float deltaY;
    float velocityY;
    FlickFigureFragment.FlickShape shape;

    public FlickObjectData(float posX, float posY, float deltaX, float deltaY, float velocityY, FlickFigureFragment.FlickShape shape){
        this.posX = posX;
        this.posY = posY;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.velocityY = velocityY;
        this.shape = shape;
    }
}
