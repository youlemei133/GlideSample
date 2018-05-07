package com.hudawei.glidesample;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ViewPager viewPager;
    private MyPagerAdapter mAdapter;
    //用于记录当前页的Fragment
    private ItemFragment mCurFragment;
//    private String url = "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1524215488479&di=8248a23793b15fd253482068bf2e9160&imgtype=0&src=http%3A%2F%2Fimgsrc.baidu.com%2Fimage%2Fc0%253Dshijue1%252C0%252C0%252C294%252C40%2Fsign%3D742c802fc3ea15ce55e3e84ade695086%2F37d3d539b6003af34d9032ca3f2ac65c1038b676.jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewPager = findViewById(R.id.viewPager);
        setPager();
    }


    private void setPager() {
        List<ItemFragment> fragments = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            fragments.add(ItemFragment.newInstance(i));
        }
        mAdapter = new MyPagerAdapter(getSupportFragmentManager(), fragments);
        viewPager.setAdapter(mAdapter);
        mCurFragment = (ItemFragment) mAdapter.getItem(viewPager.getCurrentItem());
        viewPager.post(new Runnable() {
            @Override
            public void run() {
                mCurFragment.loadLineImages();
            }
        });
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mCurFragment.clearLoadLineImages();
                mCurFragment = (ItemFragment) mAdapter.getItem(position);
                mCurFragment.loadLineImages();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    public void test(View view) {
        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
