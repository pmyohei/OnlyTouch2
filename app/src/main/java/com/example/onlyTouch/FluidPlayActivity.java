package com.example.onlyTouch;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.LinearLayout;

//import com.ogaclejapan.smarttablayout.SmartTabLayout;
//import com.ogaclejapan.smarttablayout.utils.v13.FragmentPagerItemAdapter;
//import com.ogaclejapan.smarttablayout.utils.v13.FragmentPagerItems;

import com.astuetz.PagerSlidingTabStrip;

public class FluidPlayActivity extends AppCompatActivity {

    private FluidGLSurfaceView glView;
    private MyApplication app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = (MyApplication)this.getApplication();
        Bitmap bitmap = app.getObj();
        MenuActivity.PictureButton select = app.getSelect();

        //流体画面を生成
        glView = new FluidGLSurfaceView(this, bitmap, select, null);
        //setContentView(glView);
        setContentView(R.layout.activity_fluid_play);
        LinearLayout root = findViewById(R.id.gl_view_root);
        root.addView(glView);

        //図形フリップ用のTab
        ViewPager pager = (ViewPager) findViewById(R.id.viewpager);
        pager.setAdapter(new FlickFragmentAdapter(getSupportFragmentManager()));

        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabs.setViewPager(pager);
    }

    @Override
    protected void onResume(){
        super.onResume();
        glView.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
        glView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        app.clearObj();
    }

    /**
     * 画面下のタブ
     *
     */
    private class FlickFragmentAdapter extends FragmentPagerAdapter {

        Resources res = getResources();
        private final String[] FLICK_TAB = {
                res.getString(R.string.flick_figure),
                res.getString(R.string.flick_line),
                res.getString(R.string.flick_control),
        };

        public FlickFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new FlickFigureFragment(glView);
                case 1:
                    return new FlickControlFragment(glView);
                case 2:
                    return new FlickActionFragment(glView);
            }
            return null;
        }

        @Override
        public int getCount() {
            return FLICK_TAB.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return FLICK_TAB[position];
        }
    }

}
