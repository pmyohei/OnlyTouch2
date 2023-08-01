package com.example.onlyTouch.object;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.example.onlyTouch.R;
import com.example.onlyTouch.convert.Conversion;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/*
 * タッチ画面背景レンダリング
 */
public class DrawBackGround {

    //---------------
    // リソースID
    //---------------
    public static final int BG_TEXTURE_RES = R.drawable.texture_background;

    // 背景テクスチャID
    int mTextureId;
    // レンダリングバッファ
    public final FloatBuffer mVertexBuffer;
    public final FloatBuffer mUVBuffer;

    /*
     * コンストラクタ
     */
    public DrawBackGround(Context context, float[] worldPosMin, float[] worldPosMax, int textureID) {

        // テクスチャID
        mTextureId = textureID;

        //-----------------
        // 画面サイズ
        //-----------------
        // 横幅を１とした時の高さの割合を計算
        final float heightRatio = calculateHeightRate( worldPosMin, worldPosMax );

        //-----------------
        // 背景画像サイズ
        //-----------------
        // 指定リソースのBitmapオブジェクトを生成
        Resources resource = context.getResources();
        Bitmap bitmap = BitmapFactory.decodeResource(resource, BG_TEXTURE_RES);

        final float bitmapWidth = bitmap.getWidth();
        final float bitmapHeight = bitmap.getHeight();
        final float scale = resource.getDisplayMetrics().density;

        // 画像のピクセルサイズ
        final float imageWidth = bitmapWidth / scale;
        final float imageHeight = bitmapHeight / scale;

        //-----------------
        // UV座標最大値の算出
        //-----------------
        float uvMaxWidth;
        float uvMaxHeight;

        // 横長の画像かどうか
        if (imageWidth >= imageHeight) {
            //-----------------
            // UV　X座標最大値
            //-----------------
            // 画像の可視領域:横幅
            float displayPxWidth = imageHeight / heightRatio;
            // 画像の可視領域:横幅のUV値
            uvMaxWidth = displayPxWidth / imageWidth;

            //-----------------
            // UV　Y座標最大値
            //-----------------
            uvMaxHeight = 1.0f;

        } else {
            //-----------------
            // UV　X座標最大値
            //-----------------
            uvMaxWidth = 1.0f;

            //-----------------
            // UV　Y座標最大値
            //-----------------
            // 画像に関して、横幅を１とした時の高さの割合
            float pxHeightRatio = imageHeight / imageWidth;
            // 「画面側の高さの割合」と「画像側の高さの割合」の小さい方を可視領域の高さの割合とする
            float displayRatio = Math.min(heightRatio, pxHeightRatio);

            // 画像の可視領域:高さ
            float displayPxheight = imageWidth / displayRatio;
            // 画像の可視領域:高さのUV値
            uvMaxHeight = displayPxheight / imageHeight;
        }

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
                0.0f, uvMaxHeight,
                uvMaxWidth, uvMaxHeight,
                0.0f, 0.0f,
                0.0f, 0.0f,
                uvMaxWidth, uvMaxHeight,
                uvMaxWidth, 0.0f,
        };

        //--------------------
        // バッファ変換
        //--------------------
        // 頂点バッファ
        mVertexBuffer = Conversion.convertFloatBuffer(vertices);
        // UVバッファ
        mUVBuffer = Conversion.convertFloatBuffer(uv);
    }

    /*
     * 横幅を１とした時の高さの割合を計算
     */
    private float calculateHeightRate(float[] posMin, float[] posMax ) {

        // 2点から横幅と高さを取得
        final float width = posMax[0] - posMin[0];
        final float height = posMax[1] - posMin[1];

        // 横幅を１とした時の高さの割合
        return height / width;
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
