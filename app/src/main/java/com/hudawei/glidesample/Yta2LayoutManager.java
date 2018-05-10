package com.hudawei.glidesample;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by hudawei on 2018/5/7.
 * <p>
 * LayoutManger
 * 1.onLayoutChild方法，用于决定如何布局Item
 * 里面的Recycler参数，提供了获取和回收Item的方法
 * <p>
 * 2.scrollVerticallyBy方法，用于在竖直滑动的时候，
 * 决定如何滑动、回收、添加Item
 * <p>
 * <p>
 * ******************Top值和手指滑动距离mScrolledY的关系******************
 * Top =  ( (h - mScrolledY)/ h )^4 * h;                                 *
 * h代表RecyclerView的高度                                               *
 * ***********************************************************************
 */

public class Yta2LayoutManager extends RecyclerView.LayoutManager {
    /**
     * Item的LayoutParams的mScrolledY值未设置标志
     */
    private static final int INVALID_SCROLLED_Y = Integer.MIN_VALUE;
    /**
     * 第一次布局的时候，需要显示Item的个数
     */
    private final int MAX_SHOW_COUNT = 5;
    private final int FIRST_ITEM_MIN_BOTTOM = 0;
    private final int LAST_ITEM_MAX_TOP = 300;
    private int mLastItemMaxScrolledY;
    private int mFirstItemMinScrolledY;

    /**
     * 当Adapter在onCreateView调用LayoutInflater加载布局设置parent为null时，
     * 会调用该方法，为Item生成默认的LayoutParams
     * 例如 ：LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recycle, null, false);
     */
    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new YtaLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /**
     * 当Adapter在onCreateView调用LayoutInflater加载布局设置parent为RecyclerView时，
     * 会调用RecyclerView的generateLayoutParams(Context c, AttributeSet attrs)方法，
     * 而该方法又会调用LayoutManager的generateLayoutParams(Context c, AttributeSet attrs)方法，
     * 为Item生成默认的LayoutParams
     * 所以这里我们需要重写该方法
     * 例如 ：LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recycle, recyclerView, false);
     */
    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new YtaLayoutParams(c,
                attrs);
    }

    /**
     * 是否使用RecyclerView自动的测量方法，RecyclerView在onMeasure的时候会调用该方法
     */
    @Override
    public boolean isAutoMeasureEnabled() {
        return true;
    }

    /**
     * RecyclerView是否可以竖直滑动，RecyclerView在onTouchEvent的MOVE_ACTION时，会调用该方法判断
     */
    @Override
    public boolean canScrollVertically() {
        return true;
    }

    /**
     * RecyclerView在onTouchEvent的MOVE_ACTION，如果判断能竖直滑动，那么会调用该方法进行滑动
     *
     * @param dy       手指滑动的距离
     * @param recycler RecyclerView.Recycler
     * @param state    RecyclerView.State
     * @return 消耗的距离
     */
    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                                  RecyclerView.State state) {
        return scrollBy(dy, recycler, state);
    }

    /**
     * RecyclerView布局的时候会回调该方法
     *
     * @param recycler RecyclerView.Recycler包含获取和回收Item的策略方法
     * @param state    RecyclerView.State包含Adapter中元素的个数
     */
    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.getItemCount() == 0) {
            //如过Adapter中的item个数为0，就回收Recycler中所有的View
            removeAndRecycleAllViews(recycler);
        }

        //将已经Attached的View变为Detach和Scrap
        detachAndScrapAttachedViews(recycler);

        int preTop = 0;//前一个Item的top值为下一个Item的Bottom值
        int stepTop = 0;
        int position = 0;
        for (int i = 0; i < MAX_SHOW_COUNT; i++) {
            //从Recycler中获取一个View
            View view = recycler.getViewForPosition(position);

            YtaLayoutParams params = (YtaLayoutParams) view.getLayoutParams();
            position++;
            //添加该view到RecyclerView
            addView(view);
            //测量View，会加上ItemDecoration中设置的Rect值
            measureChildWithMargins(view, 0, 0);
            //计算该view布局的位置
            int left, top, right, bottom;
            left = getPaddingLeft();
            //左内边距+带修饰View的宽度+view左右外边距
            right = left + getDecoratedMeasuredWidth(view) + params.leftMargin
                    + params.rightMargin;

            if (i == 0) {//如果是第一个Item
                //设置preTop和top值，preTop为第二个Item的Bottom值
                preTop = top = getHeight() - view.getMeasuredHeight();
                stepTop = top / ((1 + MAX_SHOW_COUNT - 1) * (MAX_SHOW_COUNT - 1) / 2);
                /*
                 * 第一个Item的Top从0滑动到Bottom值为RecyclerView高度值，对应手指滑动的距离
                 * 利用该值确定其余Item的滑动距离，进而算出实际的Top值，用于布局
                 */
                params.mScrolledY = getScrolledYByTop(top);
                //Bottom值为RecyclerView高度值
                bottom = getHeight();
            } else {
                top = preTop - stepTop * (MAX_SHOW_COUNT - i);
                if (i == MAX_SHOW_COUNT - 1)
                    top = 0;
                params.mScrolledY = getScrolledYByTop(top);
                bottom = preTop;
                preTop = top;
            }
            //调用view的layout布局方法
            layoutDecoratedWithMargins(view, left, top, right, bottom);
        }
        calcFirstItemMinScrolledY();
        calcLastItemMaxScrolledY();
    }

    /**
     * 根据Item的手指滑动距离，求对应的Top值
     * Item保存了新的手指滑动距离
     *
     * @param child Item
     * @param dy    在Item原来滑动距离上在滑动dy距离
     * @return 滑动后的新Top值
     */
    private int getTopByScrolling(View child, int dy) {
        YtaLayoutParams params = (YtaLayoutParams) child.getLayoutParams();
        int scrollY = params.mScrolledY + dy;
        int h = getHeight();
        if (scrollY < 0)
            scrollY = 0;
        if (scrollY > h)
            scrollY = h;
        double result = Math.pow((h - scrollY) * 1.0f / h, 4) * h;
        if (result < 0)
            result = 0;
        if (result > h)
            result = h;
        return (int) result;
    }

    /**
     * 根据Item的Top值，求对应的手指滑动距离
     *
     * @param top 当前Item的top值
     * @return 对应滑动的距离mScrolledY
     */
    private int getScrolledYByTop(int top) {
        int h = getHeight();
        int result = (int) (h * (1 - Math.pow(top * 1.0f / h, 1.0f / 4)));
        if (result < 0)
            result = 0;
        if (result > h)
            result = h;
        return result;
    }

    /**
     * 1.首先修改所有Item的Top值和Bottom值，
     * <p>
     * <p>
     * 2.回收Item
     * <p>
     * 3.添加Item
     */
    int scrollBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0 || dy == 0) {
            return 0;
        }
        boolean downScroll = dy < 0;
        /*
         * TODO:禁止滑动事件
         * 向上滑动时
         * 第一个Item的Bottom值距RecyclerView的Bottom不小于FIRST_ITEM_BOTTOM_MIN
         * 向下滑动时
         * 最后一个Item的Top值距离RecyclerView的顶部不大于LAST_ITEM_TOP_MAX
         *
         * 在手指松开，且RecyclerView滑动状态为空闲时，恢复为零界状态
         */
        boolean interceptScrollFlag = false;//是否禁止滑动
        if (downScroll) {
            View lastView = getChildClosestToEnd();
            YtaLayoutParams params = (YtaLayoutParams) lastView.getLayoutParams();
            int lastPosition = params.getViewAdapterPosition();
            if (lastPosition == state.getItemCount() - 1) {//最后一个Item
                if (params.mScrolledY == mLastItemMaxScrolledY) {
                    //禁止滑动
                    interceptScrollFlag = true;
                } else if (params.mScrolledY + dy < mLastItemMaxScrolledY) {
                    //将mScrolledY置为mLastItemMaxScrolledY
                    dy = mLastItemMaxScrolledY - params.mScrolledY;
                }
            }
        } else {
            View firstView = getChildClosestToStart();
            YtaLayoutParams params = (YtaLayoutParams) firstView.getLayoutParams();
            int firstPosition = params.getViewAdapterPosition();
            if (firstPosition == 0) {//第一个Item
                if (params.mScrolledY == mFirstItemMinScrolledY) {
                    //禁止滑动
                    interceptScrollFlag = true;
                } else if (params.mScrolledY + dy > mFirstItemMinScrolledY) {
                    //将mScrolledY置为mFirstItemMinScrolledY
                    dy = mFirstItemMinScrolledY - params.mScrolledY;
                }
            }
        }


        if (interceptScrollFlag) {
            return 0;
        }

        /*
         * 1.修改各个Item的top和bottom重新layout
         * 如果是第一个Item
         * 那么移动该Item的top值，让后根据top值和LayoutParams中的height值，算出bottom值
         * 后面Item
         * Top值根据LayoutParams中的mScrolledY值，计算出新的Top值，而Bottom值为前一个Item的top值
         * 然后重新测量，布局
         */
        int preViewTop = INVALID_SCROLLED_Y;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int top = getTopByScrolling(child, dy);
            YtaLayoutParams params = (YtaLayoutParams) child.getLayoutParams();
            params.mScrolledY += dy;
            if (preViewTop == INVALID_SCROLLED_Y) {
                preViewTop = params.height + top;
            }
            layoutChild(child, top, preViewTop);
            preViewTop = top;
        }
        if(false)
            return dy;

         /*
         * 2.回收item
         * 向上滑动，从后往前遍历，找到第一个top值大于0的位置position，在position后面找到第一个为0的位置startPosition，
         * 回收startPosition+1到getChildCount - 1的View
         *
         * 向下滑动，从后往前遍历，找到第一个bottom值为height的View,然后回收前面的View
         */
        int startIndex = 0;
        int endIndex = 0;
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View view = getChildAt(i);
            if (view == null)
                continue;
            int top = view.getTop();
            int bottom = view.getBottom();
            if (downScroll) {
                if (bottom == getHeight()) {
                    endIndex = i;
                    startIndex = 0;
                    break;
                }
            } else {
                if (top > 0) {
                    startIndex = i + 2;
                    endIndex = getChildCount();
                    break;
                }
            }
        }
        recycleChildren(recycler, startIndex, endIndex);

        /*
         * 3.添加View
         * 向上滑动，如果第一个View的bottom小于recyclerView的时候，则添加新View，并且设置mScrolledY值为0
         * 向下滑动，如果最后一个View的top值大于0，则添加新的View,并且设置mScrolledY值为getHeight
         */
        if (downScroll) {
            View view = getChildClosestToEnd();
            YtaLayoutParams params = (YtaLayoutParams) view.getLayoutParams();
            int position = params.getViewAdapterPosition();
            if (view.getTop() > 0 && position + 1 < state.getItemCount()) {
                View newView = recycler.getViewForPosition(position + 1);
                YtaLayoutParams newParams = (YtaLayoutParams) newView.getLayoutParams();
                newParams.mScrolledY = getHeight();
                addView(newView);
                layoutChild(newView, 0, view.getTop());
            }
        } else {
            View view = getChildClosestToStart();
            YtaLayoutParams params = (YtaLayoutParams) view.getLayoutParams();
            int position = params.getViewAdapterPosition();
            if (view.getBottom() < getHeight() && position - 1 >= 0) {
                View newView = recycler.getViewForPosition(position - 1);
                YtaLayoutParams newParams = (YtaLayoutParams) newView.getLayoutParams();
                newParams.mScrolledY = 0;
                addView(newView, 0);
                layoutChild(newView, view.getBottom(), getHeight());
            }
        }
        return dy;
    }

    /**
     * 计算最后一个Item的ScrolledY的最大值
     */
    private void calcLastItemMaxScrolledY() {
        mLastItemMaxScrolledY = getScrolledYByTop(LAST_ITEM_MAX_TOP);
    }

    /**
     * 计算第一个Item的ScrolledY的最小值
     */
    private void calcFirstItemMinScrolledY() {
        View firstView = getChildAt(0);
        YtaLayoutParams params = (YtaLayoutParams) firstView.getLayoutParams();
        mFirstItemMinScrolledY = getScrolledYByTop(getHeight() - FIRST_ITEM_MIN_BOTTOM - params.height);
    }

    /**
     * 回收Item，回收范围为[startIndex ,endIndex)
     *
     * @param recycler   RecyclerView.Recycler
     * @param startIndex 开始位置
     * @param endIndex   结束位置
     */
    private void recycleChildren(RecyclerView.Recycler recycler, int startIndex, int endIndex) {
        if (startIndex >= endIndex) {
            return;
        }
        for (int i = endIndex - 1; i >= startIndex; i--) {
            final View view = getChildAt(i);
            removeViewAt(i);
            try {
                recycler.recycleView(view);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 测量布局Child
     *
     * @param view   需要测量布局的Item
     * @param top    该Item的top值，也就是顶部y值
     * @param bottom 该Item的bottom值，也就是底部y值
     */
    private void layoutChild(View view, int top, int bottom) {
        YtaLayoutParams params = (YtaLayoutParams) view.getLayoutParams();
        //添加该view到RecyclerView
//        addView(view);
        //测量View，会加上ItemDecoration中设置的Rect值
        measureChildWithMargins(view, 0, 0);
        //计算该view布局的位置
        int left, right;
        left = getPaddingLeft();
        //左内边距+带修饰View的宽度+view左右外边距
        right = left + getDecoratedMeasuredWidth(view) + params.leftMargin
                + params.rightMargin;
        //调用view的layout布局方法
        layoutDecoratedWithMargins(view, left, top, right, bottom);
    }

    /**
     * 获取接近RecyclerView底部的第一个Item
     *
     * @return 第一个Item
     */
    private View getChildClosestToStart() {
        return getChildAt(0);
    }

    /**
     * 获取接近RecyclerView顶部的第一个Item
     *
     * @return 最后一个Item
     */
    private View getChildClosestToEnd() {
        return getChildAt(getChildCount() - 1);
    }

    static class YtaLayoutParams extends RecyclerView.LayoutParams {
        /**
         * 手指滑动的距离
         * 在添加Item的时候初始化该值
         * 在滑动的时候修改该值
         */
        int mScrolledY = INVALID_SCROLLED_Y;

        public YtaLayoutParams(int width, int height) {
            super(width, height);
        }

        public YtaLayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }
    }
}
