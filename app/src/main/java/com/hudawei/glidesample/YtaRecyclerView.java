package com.hudawei.glidesample;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.lang.reflect.Field;

/**
 * Created by hudawei on 2018/5/5.
 * a
 */

public class YtaRecyclerView extends RecyclerView {
    private Recycler mRecycler;

    public YtaRecyclerView(Context context) {
        super(context);
    }

    public YtaRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public YtaRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private Recycler getRecycler() {
        if (mRecycler == null) {
            try {
                Log.e("offsetChildrenVertical", "getRecycler");
                Field mRecyclerField = this.getClass().getSuperclass().getDeclaredField("mRecycler");
                mRecyclerField.setAccessible(true);
                mRecycler = (Recycler) mRecyclerField.get(this);
                Log.e("YtaRecyclerView", "mRecycler : " + mRecycler);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return mRecycler;
    }

//    @Override
    public void offsetChildrenVerticals(int dy) {
        final int childCount = getChildCount();
        Log.e("offsetChildrenVertical", "childCount : " + childCount);
        View child;
        int ottop;//老的加速度top
        double oscale;//对应老的手指移动top
        int nttop;//新的加速度top
        double nscale;//对应新的手指移动top
        double h = getHeight();

        for (int i = 0; i < childCount; i++) {
            child = getChildAt(i);
            ottop = child.getTop();
            //加速度公式的top开平方，等于正常的top
            oscale = Math.sqrt(Math.abs(ottop) / h);
            //符合与top保持一致
            oscale = ottop > 0 ? oscale : -oscale;
            //正常top加上手指移动距离
            nscale = oscale + dy / h;
            //手指移动的距离的2次方，为加速度对应的距离
//            nttop = (int) (h * Math.pow(nscale, 2));
//            nttop = nscale > 0 ? nttop : -nttop;
            nttop = (int) (
                    Math.pow(Math.pow(ottop * Math.pow(h, 2), 1.0 / 3) + dy, 3) /
                    Math.pow(h, 2)
            );
            child.offsetTopAndBottom(nttop - ottop);
//            int ctop = child.getTop();
//            if (ctop < 0) {
//                child.setTop(0);
//                child.setBottom(child.getBottom() - ctop);
//            }
            Log.e("offsetChildrenVertical", "child index " + ((TextView) child).getText() + "\tdy:"+dy+"\t top : " + child.getTop() + "\t destance :" + (nttop - ottop) + "\t height : " + (child.getBottom() - child.getTop()));
        }
        if (dy < 0)
            recycleViewsFromStart(dy);
    }

    private void recycleViewsFromStart(int dy) {
        final int childCount = getChildCount();
        for (int i = 1; i < childCount; i++) {
            View child = getChildAt(i);
            View preChild = getChildAt(i - 1);
            ViewHolder holder = getChildViewHolder(child);
            ViewHolder preHolder = getChildViewHolder(preChild);
            if (preHolder != null && holder != null &&
                    preChild.getTop() == 0 && child.getTop() > 0) {
                recycleViewByIndex(0, i - 1);
                return;
            }
        }
    }


    private void recycleViewByIndex(int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            Log.e("recycleViewByIndex", "recycleViewByIndex : " + i);
            getLayoutManager().removeAndRecycleViewAt(i, getRecycler());
        }
    }
}
