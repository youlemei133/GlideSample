package com.hudawei.glidesample;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.lang.reflect.Field;

/**
 * Created by hudawei on 2018/5/7.
 */

public class YtaLayoutManager extends LinearLayoutManager {
    public YtaLayoutManager(Context context) {
        super(context);
    }

    public YtaLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public YtaLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private int getScrollDirection() {
        int mLayoutDirection = 0;
        try {
            Field mLayoutStateField = this.getClass().getSuperclass().getDeclaredField("mLayoutState");
            mLayoutStateField.setAccessible(true);
            Object mLayoutState = mLayoutStateField.get(this);
            Field mLayoutDirectionField = mLayoutState.getClass().getDeclaredField("mLayoutDirection");
            mLayoutDirectionField.setAccessible(true);
            mLayoutDirection = (int) mLayoutDirectionField.get(mLayoutState);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return mLayoutDirection;
    }

    @Override
    public void addView(View child, int index) {
        View firstChild = getChildAt(0);
        View secondChild = getChildAt(1);
        if (getScrollDirection() < 0) {
            if (firstChild != null && firstChild.getTop() == 0 && secondChild.getTop() != 0) {
                Log.e("YtaLayoutManager", "addView " + "firstChild index " + ((TextView)firstChild).getText()+"\ttop :"+firstChild.getTop());
                super.addView(child, index);
            }
        } else {
            super.addView(child, index);
        }
    }
}
