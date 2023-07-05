package com.example.onlyTouch;

import com.google.fpl.liquidfun.Body;

import java.nio.FloatBuffer;

import static com.example.onlyTouch.FluidWorldRenderer.convFloatBuffer;

public class BodyData {

    long id;
    Body body;
    FloatBuffer vertexBuffer;
    FloatBuffer uvBuffer;
    int vertexLen;
    int drawMode;
    int textureId;
    boolean touched;                   //タッチされているか

    public BodyData(long id, Body body, float[] buffer, float[] uv, int drawMode, int textureId) {
        this.id = id;
        this.body = body;
        this.vertexBuffer = convFloatBuffer(buffer);
        this.uvBuffer = convFloatBuffer(uv);
        this.vertexLen = buffer.length / 2;
        this.drawMode = drawMode;
        this.textureId = textureId;
        this.touched = false;
    }

    public long getId() {
        return this.id;
    }

    public Body getBody() {
        return this.body;
    }

    public FloatBuffer getVertexBuffer() {
        return this.vertexBuffer;
    }

    public FloatBuffer getUvBuffer() { return this.uvBuffer;}

    public int getDrawMode() { return this.drawMode;}

    public int getVertexLen() { return this.vertexLen;}

    public int getTextureId() { return this.textureId;}
}
