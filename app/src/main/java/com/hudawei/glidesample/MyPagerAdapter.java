package com.hudawei.glidesample;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.List;

/**
 * Created by hudawei on 2018/4/27.
 */

public class MyPagerAdapter extends FragmentStatePagerAdapter {
    private List<ItemFragment> mFragments;
    public MyPagerAdapter(FragmentManager fm, List<ItemFragment> fragments) {
        super(fm);
        mFragments = fragments;
    }

    @Override
    public Fragment getItem(int position) {
        return mFragments.get(position);
    }

    @Override
    public int getCount() {
        return mFragments == null ? 0 : mFragments.size();
    }
}
