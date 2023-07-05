package com.example.onlyTouch;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

@SuppressLint("ValidFragment")
public class FlickControlFragment extends Fragment {

    private FluidGLSurfaceView glView;

    public FlickControlFragment(FluidGLSurfaceView para){
        glView = para;
    }

    //フリック物体の制御
    enum FlickControl {
        FIX,        //固定
        UNFIX,      //固定解除(フリック物体生成時と同じ状態にする)
        DELETE,     //削除
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        //フラグメントとして割り当てるレイアウト
        View layout = inflater.inflate(R.layout.fragment_flick_control, null);





        return layout;
    }


}