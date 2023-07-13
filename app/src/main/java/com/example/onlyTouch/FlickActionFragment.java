package com.example.onlyTouch;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

@SuppressLint("ValidFragment")
public class FlickActionFragment extends Fragment {

    private FluidGLSurfaceView glView;

    public FlickActionFragment(FluidGLSurfaceView para){
        glView = para;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        //フラグメントとして割り当てるレイアウト
        View layout = inflater.inflate(R.layout.fragment_flick_action, null);

        Button cannon_bt = layout.findViewById(R.id.cannon);
        cannon_bt.setOnClickListener(new cannonButtonListener());

        return layout;
    }

    //大砲リスナー
    private class cannonButtonListener implements View.OnClickListener {

        private boolean enable = false;

        @Override
        public void onClick(View view) {

            //有効無効を逆転
            enable = !enable;

            //OpenGL側へ制御要求
            FluidWorldRenderer render = glView.getRenderer();
            render.switchBullet();
        }
    }

}