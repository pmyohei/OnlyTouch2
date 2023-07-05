package com.example.onlyTouch;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;

import com.example.onlyTouch.R;

@SuppressLint("ValidFragment")
public class FlickFigureFragment extends Fragment {

    private FluidGLSurfaceView glView;
    private GestureDetector gesDetectBox;
    private GestureDetector gesDetectTriangle;
    private GestureDetector gesDetectCircle;
    private float mBoxPosOffset;
    private float mTrianglePosOffset;
    private float mCirclePosOffset;

    public FlickFigureFragment(FluidGLSurfaceView para){
        glView = para;
    }

    //フリック物体
    enum FlickShape {
        BOX,
        TRIANGLE,
        CIRCLE,
        LINE
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        //フラグメントとして割り当てるレイアウト
        View layout = inflater.inflate(R.layout.fragment_flick_figure, null);

        //フリック検出リスナー
        gesDetectBox = new GestureDetector(container.getContext(), mOnGestureListenerBox);
        gesDetectTriangle = new GestureDetector(container.getContext(), mOnGestureListenerTriangle);
        gesDetectCircle = new GestureDetector(container.getContext(), mOnGestureListenerCircle);

/*        TextView tv = layout.findViewById(R.id.box);
        tv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gesDetectBox.onTouchEvent(motionEvent);
            }
        });
*/
        Button bt = layout.findViewById(R.id.box);
        bt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gesDetectBox.onTouchEvent(motionEvent);
            }
        });
        bt.getViewTreeObserver().addOnWindowFocusChangeListener(new onWindowFocus(bt));

        bt = layout.findViewById(R.id.triangle);
        bt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gesDetectTriangle.onTouchEvent(motionEvent);
            }
        });
        bt.getViewTreeObserver().addOnWindowFocusChangeListener(new onWindowFocus(bt));

        bt = layout.findViewById(R.id.circle);
        bt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gesDetectCircle.onTouchEvent(motionEvent);
            }
        });
        bt.getViewTreeObserver().addOnWindowFocusChangeListener(new onWindowFocus(bt));

        return layout;
    }

    /**
     * onWindowFocusChanged取得用
     */
    private class onWindowFocus implements ViewTreeObserver.OnWindowFocusChangeListener{

        View view;

        public onWindowFocus(View v){
            view = v;
        }

        @Override
        public void onWindowFocusChanged(boolean b) {
            switch (view.getId()){
                case R.id.box:
                    mBoxPosOffset = view.getLeft();
                    break;

                case R.id.triangle:
                    mTrianglePosOffset = view.getLeft();
                    Log.i("test", "mTrianglePosOffset=" + mTrianglePosOffset);
                    break;

                case R.id.circle:
                    mCirclePosOffset = view.getLeft();
                    Log.i("test", "mCirclePosOffset=" + mCirclePosOffset);
                    break;
            }
        }
    }

    //ActivityでいうonCreate()
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
    }

    //！共通化をその内検討！
    /**
     * タッチイベントリスナー(Box)
     */
    private final GestureDetector.SimpleOnGestureListener mOnGestureListenerBox = new GestureDetector.SimpleOnGestureListener() {

        // フリックイベント(指でサッとなぞると呼び出される)
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            Log.i("test", "e1 x=" + e1.getX() + " y=" + e1.getY());
            Log.i("test", "e2 x=" + e2.getX() + " y=" + e2.getY());
            Log.i("test", "velocityX=" + velocityX + " velocityY=" + velocityY);

            //下方向のフリックなら、何もしない(Y座標は原点が上のため、下方向がプラス)
            if(velocityY >= 0){
                return false;
            }

//            //フリックした物体をGL側に登録
//            FluidWorldRenderer render = glView.getRenderer();
//            render.reqEntryFlickObject(e2.getX() + mBoxPosOffset, e2.getY(), e2.getX() - e1.getX(), Math.abs(e2.getY() - e1.getY()), velocityY, FlickShape.BOX);

            return false;
        }
    };

    /**
     * タッチイベントリスナー(Triangle)
     */
    private final GestureDetector.SimpleOnGestureListener mOnGestureListenerTriangle = new GestureDetector.SimpleOnGestureListener() {

        // フリックイベント(指でサッとなぞると呼び出される)
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            Log.i("test", "e1 x=" + e1.getX() + " y=" + e1.getY());
            Log.i("test", "e2 x=" + e2.getX() + " y=" + e2.getY());
            Log.i("test", "velocityX=" + velocityX + " velocityY=" + velocityY);

            //下方向のフリックなら、何もしない(Y座標は原点が上のため、下方向がプラス)
            if(velocityY >= 0){
                return false;
            }

//            //フリックした物体をGL側に登録
//            FluidWorldRenderer render = glView.getRenderer();
//            render.reqEntryFlickObject(e2.getX() + mTrianglePosOffset, e2.getY(), e2.getX() - e1.getX(), Math.abs(e2.getY() - e1.getY()), velocityY, FlickShape.TRIANGLE);

            return false;
        }
    };

    /**
     * タッチイベントリスナー(Circle)
     */
    private final GestureDetector.SimpleOnGestureListener mOnGestureListenerCircle = new GestureDetector.SimpleOnGestureListener() {

        // フリックイベント(指でサッとなぞると呼び出される)
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            Log.i("test", "e1 x=" + e1.getX() + " y=" + e1.getY());
            Log.i("test", "e2 x=" + e2.getX() + " y=" + e2.getY());
            Log.i("test", "velocityX=" + velocityX + " velocityY=" + velocityY);

            //下方向のフリックなら、何もしない(Y座標は原点が上のため、下方向がプラス)
            if(velocityY >= 0){
                return false;
            }

//            //フリックした物体をGL側に登録
//            FluidWorldRenderer render = glView.getRenderer();
//            render.reqEntryFlickObject(e2.getX() + mCirclePosOffset, e2.getY(), e2.getX() - e1.getX(), Math.abs(e2.getY() - e1.getY()), velocityY, FlickShape.CIRCLE);

            return false;
        }
    };

}