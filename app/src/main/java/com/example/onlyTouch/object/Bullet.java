package com.example.onlyTouch.object;

import com.example.onlyTouch.R;
import com.example.onlyTouch.convert.Conversion;
import com.google.fpl.liquidfun.Body;
import com.google.fpl.liquidfun.BodyDef;
import com.google.fpl.liquidfun.BodyType;
import com.google.fpl.liquidfun.CircleShape;
import com.google.fpl.liquidfun.Vec2;
import com.google.fpl.liquidfun.World;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class Bullet {

    //-----------------
    // 定数
    //-----------------
    // レンダリングのテクスチャリソースID
    public static final int TEXTURE_RESOUCE_ID = R.drawable.texture_bullet_color;
    // 弾半径
    private final float BULLET_RADIUS = 0.4f;

    //-----------------
    // 変数
    //-----------------
    private Body mBody;
    private FloatBuffer mVertexBuffer;
    private FloatBuffer mUvBuffer;
    private int mVertexLen;
    private final int mTextureId;

    /*
     * コンストラクタ
     */
    public Bullet(World world, float posX, float posY, int textureID) {
        // Body生成
        createBody(world, posX, posY);
        // レンダリング頂点の算出
        calculateVertices();
        // レンダリングテクスチャ
        mTextureId = textureID;
    }

    /*
     * Body生成
     */
    private void createBody(World world, float posX, float posY) {
        // 定義
        BodyDef bodyDef = new BodyDef();
        bodyDef.setType(BodyType.dynamicBody);
        bodyDef.setPosition(posX, posY);

        // 生成
        Body body = world.createBody(bodyDef);
//        body.setGravityScale(2.0f);
        body.setGravityScale(1.0f);

        // フィクスチャをアタッチ
        CircleShape circle = new CircleShape();
        circle.setRadius(BULLET_RADIUS);
        body.createFixture(circle, 0);

        mBody = body;
    }

    /*
     * レンダリング頂点の計算
     */
    private void calculateVertices() {

        // 頂点計算
        float[] vertices = new float[32 * 2];
        float[] uv = new float[32 * 2];
        for (int i = 0; i < 32; ++i) {
            float a = ((float) Math.PI * 2.0f * i) / 32;
            vertices[i * 2] = BULLET_RADIUS * (float) Math.sin(a);
            vertices[i * 2 + 1] = BULLET_RADIUS * (float) Math.cos(a);

            uv[i * 2] = ((float) Math.sin(a) + 1.0f) / 2f;
            uv[i * 2 + 1] = (-1 * (float) Math.cos(a) + 1.0f) / 2f;
        }

        // 保持
        mVertexLen = vertices.length / 2;
        mVertexBuffer = Conversion.convertFloatBuffer(vertices);
        mUvBuffer = Conversion.convertFloatBuffer(uv);
    }

    /*
     * 発射（上方向）
     */
    public void shotUp() {
        // 発射：上方向の速度を設定
        final int LINEAR_VEROCITY_Y = 200;
        mBody.setLinearVelocity(new Vec2(0, LINEAR_VEROCITY_Y));
    }

    /*
     * 減速
     */
    public void loseSpeed() {
        // 速度を0にする
        mBody.setLinearVelocity(new Vec2(0, 0));
    }

    /*
     * 減速判定
     */
    public boolean isDeceleration() {
        // 減速判定ラインを下回ったかどうか
        float velocityY = mBody.getLinearVelocity().getY();
        return (velocityY < 100);
    }

    /*
     * レンダリング
     */
    public void draw(GL10 gl) {

        gl.glPushMatrix();
        {
            // レンダリング位置の移動
            gl.glTranslatef(mBody.getPositionX(), mBody.getPositionY(), 0);

            // テクスチャの指定
            gl.glActiveTexture(GL10.GL_TEXTURE0);
            gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureId);

            // UVバッファ：確保したメモリをOpenGLに渡す
            gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mUvBuffer);

            // 頂点バッファ
            FloatBuffer buff = mVertexBuffer;
            gl.glVertexPointer(2, GL10.GL_FLOAT, 0, buff);

            gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, mVertexLen);
        }
        gl.glPopMatrix();
    }


    /*
     * setter/getter
     */
    public Body getBody() {
        return mBody;
    }
}
