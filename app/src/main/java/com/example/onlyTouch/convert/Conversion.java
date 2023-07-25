package com.example.onlyTouch.convert;

import static com.example.onlyTouch.opengl.ParticleWorldRenderer.convFloatBuffer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.view.View;

import com.example.onlyTouch.R;
import com.google.fpl.liquidfun.Body;
import com.google.fpl.liquidfun.BodyDef;
import com.google.fpl.liquidfun.BodyType;
import com.google.fpl.liquidfun.CircleShape;
import com.google.fpl.liquidfun.Vec2;
import com.google.fpl.liquidfun.World;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

/*
 * 変換関連
 */
public class Conversion {

    //-----------------
    // 定数
    //-----------------

    //-----------------
    // 変数
    //-----------------

    /*
     * 画面座標を物理座標へ変換
     */
    public static float[] convPointScreenToWorld(float wx, float wy, GL10 gl, final View screenView ) {

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
        final int screenWidth = screenView.getWidth();
        final int screenHeight = screenView.getHeight();

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

        float[] position = {x, y};
        return position;
    }

    /*
     * テクスチャ生成
     */
    public static int makeTexture(GL10 gl10, int resourceId, Context context) {

        //----------------------------
        // テクスチャオブジェクトの生成
        //----------------------------
        // テクスチャ用のメモリを確保
        final int TEXTURE_NUM = 1;
        int[] textureIds = new int[TEXTURE_NUM];
        // テクスチャオブジェクトの生成（第2引数にテクスチャIDが格納される）
        gl10.glGenTextures(TEXTURE_NUM, textureIds, 0);

        //----------------------------
        // テクスチャへのビットマップ指定
        //----------------------------
        // 指定リソースのBitmapオブジェクトを生成
        Resources resource = context.getResources();
        Bitmap bitmap = BitmapFactory.decodeResource(resource, resourceId);

        // テクスチャユニットを選択
        gl10.glActiveTexture(GL10.GL_TEXTURE0);
        // テクスチャIDとGL_TEXTURE_2Dをバインド
        gl10.glBindTexture(GL10.GL_TEXTURE_2D, textureIds[0]);
        // バインドされたテクスチャにBitmapをセットする
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

        //----------------------------
        // テクスチャのフィルタ指定
        //----------------------------
        gl10.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        gl10.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);

        return textureIds[0];
    }


}
