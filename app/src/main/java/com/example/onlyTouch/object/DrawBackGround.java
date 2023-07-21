package com.example.onlyTouch.object;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/*
 * タッチ画面背景レンダリング
 */
public class DrawBackGround {

    // 背景テクスチャID
    int mTextureId;
    // レンダリングバッファ
    public final FloatBuffer mVertexBuffer;
    public final FloatBuffer mUVBuffer;

    /*
     * コンストラクタ
     */
    public DrawBackGround(float[] worldPosMin, float[] worldPosMax, int textureID ) {

        // テクスチャID
        mTextureId = textureID;

        //--------------------
        // 描画頂点バッファ
        //--------------------
        // 画面座標（world変換済み）
        float minX = worldPosMin[0];
        float minY = worldPosMin[1];
        float maxX = worldPosMax[0];
        float maxY = worldPosMax[1];

        final float vertices[] = {
                minX, minY,
                maxX, minY,
                minX, maxY,
                minX, maxY,
                maxX, minY,
                maxX, maxY,
        };

        final float uv[] = {
                0.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                1.0f, 1.0f,
        };

        //--------------------
        // バッファ変換
        //--------------------
        // 頂点バッファ
        ByteBuffer vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4); // floatは32biteなのでx4.
        vertexBuffer.order(ByteOrder.nativeOrder());
        mVertexBuffer = vertexBuffer.asFloatBuffer();
        mVertexBuffer.put(vertices);
        mVertexBuffer.position(0);

        // UVバッファ
        ByteBuffer uvBuffer = ByteBuffer.allocateDirect(uv.length * 4); // floatは32biteなのでx4.
        uvBuffer.order(ByteOrder.nativeOrder());
        mUVBuffer = uvBuffer.asFloatBuffer();
        mUVBuffer.put(uv);
        mUVBuffer.position(0);
    }

    /*
     * レンダリング
     */
    public void draw(GL10 gl) {

        gl.glPushMatrix();
        {
            // テクスチャの指定
            gl.glActiveTexture(GL10.GL_TEXTURE0);
            gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureId);

            // バッファを渡して描画
            gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mUVBuffer);
            gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertexBuffer);
            gl.glDrawArrays(GL10.GL_TRIANGLES, 0, 6);
        }
        gl.glPopMatrix();
    }
}
