package com.example.onlyTouch;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/*
 * お試し
 */
public class Draw2DObject {

    public final FloatBuffer mmVertexBuffer;

    public Draw2DObject() {
        float vertices[] = {
                0.0f, 0.5f,
                4.5f, -4.5f,
                -8.5f, -6.5f,
        };

        ByteBuffer vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4); // floatは32biteなのでx4.
        vertexBuffer.order(ByteOrder.nativeOrder());
        mmVertexBuffer = vertexBuffer.asFloatBuffer();
        mmVertexBuffer.put(vertices);
        mmVertexBuffer.position(0);
    }

    public void draw(GL10 gl) {

        gl.glPushMatrix();
        {
//            gl.glTranslatef(0, 0, 0);

//            gl.glEnable(GL10.GL_TEXTURE_2D);

//            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mmVertexBuffer);

            // 描画処理.
            gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
            gl.glDrawArrays(GL10.GL_TRIANGLES, 0, 3);
        }
        gl.glPopMatrix();
    }
}
