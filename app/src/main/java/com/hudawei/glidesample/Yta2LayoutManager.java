package com.hudawei.glidesample;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

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
     * 第一次布局的时候，需要显示Item的个数
     */
    private final int MAX_SHOW_COUNT = 5;
    private final int FIRST_ITEM_MIN_BOTTOM = 0;
    private final int LAST_ITEM_MAX_TOP = 300;

    private int mStartScrollYDv;//前2个item的ScrollY差值
    private int mEndScrollYDv;//后2个Item的ScrollY差值
    private int mBoundScrollY;//ScrolledY零界点，零界点前面使用直线公式，后面使用4次方公式
    private int mBoundTop;//Top零界点，零界点前面使用直线公式，后面使用4次方公式
    private int mMinScrolledY;//Item的ScrollY的最小值
    private int mMaxScrolledY;//Item的ScrollY的最大值
    private int mMaxTop;//Item的最大Top值
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

        int position = 0;
        int preScrollY = 0;
        int firstItemHeight = 0;
        int preTop = 0;
        List<Integer> tops = null;
        for (int i = 0; i < MAX_SHOW_COUNT; i++) {
            //从Recycler中获取一个View
            View view = recycler.getViewForPosition(position);

            YtaLayoutParams params = (YtaLayoutParams) view.getLayoutParams();
            position++;
            //添加该view到RecyclerView
            addView(view, 0);
            //测量View，会加上ItemDecoration中设置的Rect值
            measureChildWithMargins(view, 0, 0);
            //计算该view布局的位置
            int left, top, right, bottom;
            left = getPaddingLeft();
            //左内边距+带修饰View的宽度+view左右外边距
            right = left + getDecoratedMeasuredWidth(view) + params.leftMargin
                    + params.rightMargin;

            if (i == 0) {//如果是第一个Item
                firstItemHeight = view.getMeasuredHeight();
                //获取top值
                preTop = mBoundTop = top = getHeight() - firstItemHeight;
                mMaxTop = getHeight();
                //获取对应的ScrolledY值
                mBoundScrollY = preScrollY = params.mScrolledY = getScrolledYByTop(top);
                //计算各个Item的top值
                tops = calcItemTop(top);
            } else {
                //获取top值
                top = preTop - tops.get(i - 1);
                //获取对应的ScrolledY值
                params.mScrolledY = getScrolledYByTop(top);
                if (i == 1) {//计算开始2个Item的ScrollY差值
                    mStartScrollYDv = params.mScrolledY - preScrollY;
                    mMinScrolledY = preScrollY - mStartScrollYDv;
                } else if (i == MAX_SHOW_COUNT - 2) {//倒数第2个Item - 倒数第3个Item的ScrollY
                    mEndScrollYDv = params.mScrolledY - preScrollY;
                } else if (i == MAX_SHOW_COUNT - 1) {//计算最后2个Item的ScrollY差值
                    params.mScrolledY = preScrollY + mEndScrollYDv;
                    mMaxScrolledY = params.mScrolledY;
                }
                preScrollY = params.mScrolledY;
                preTop = top;
            }
            bottom = top + view.getMeasuredHeight();
            //调用view的layout布局方法
            layoutDecoratedWithMargins(view, left, top, right, bottom);

        }
        mLastItemMaxScrolledY = getScrolledYByTop(LAST_ITEM_MAX_TOP);
        mFirstItemMinScrolledY = getScrolledYByTop(getHeight() - FIRST_ITEM_MIN_BOTTOM - firstItemHeight);
    }


    /**
     * 直线公式
     * 通过当前ScrollY计算对应的Top值
     *
     * @param scrollY 当前ScrollY
     * @return 对应的Top值
     */
    private int calcTopByScrollY(int scrollY) {
        return (int) ((mBoundTop - mMaxTop) * (scrollY - mMinScrolledY) * 1.0f / (mBoundScrollY - mMinScrolledY) + mMaxTop);
    }

    /**
     * 直线公式
     * 通过当前Top计算对应的ScrollY值
     *
     * @param top 当前Top
     * @return 对应的ScrollY值
     */
    private int calcScrollYByTop(int top) {
        return (int) ((top - mMaxTop) * (mBoundScrollY - mMinScrolledY) * 1.0f / (mBoundTop - mMaxTop) + mMinScrolledY);
    }

    /**
     * 根据Item的手指滑动距离，求对应的Top值
     *
     * @param scrollY 当前Item手指滑动距离
     * @return 手指滑动scrollY的新Top值
     */
    private int getTopByScrolledY(int scrollY) {
        int h = getHeight();
        double result;
        if (scrollY > mBoundScrollY) {
            result = Math.pow((h - scrollY) * 1.0f / h, 4) * h;
        } else {
            result = calcTopByScrollY(scrollY);
        }
        return (int) result;
    }

    /**
     * 根据Item的Top值，求对应的手指滑动距离
     *
     * @param top 当前Item的top值
     * @return 对应滑动的距离mScrolledY
     */
    private int getScrolledYByTop(int top) {
        int result = 0;
        if (top > mBoundTop) {
            calcScrollYByTop(top);
        } else {
            int h = getHeight();
            result = (int) (h * (1 - Math.pow(top * 1.0f / h, 1.0f / 4)));
        }
        return result;
    }

    private void downScroll(int dy, RecyclerView.Recycler recycler,
                            RecyclerView.State state) {
        //1.计算需要添加的Item
        View endView = getChildClosestToEnd();
        YtaLayoutParams endParams = (YtaLayoutParams) endView.getLayoutParams();
        int endPosition = endParams.getViewAdapterPosition();
        //添加View
        if (getTopByScrolledY(endParams.mScrolledY + dy) > 0 && endPosition + 1 < state.getItemCount()) {
            View newView = recycler.getViewForPosition(endPosition + 1);
            YtaLayoutParams newParams = (YtaLayoutParams) newView.getLayoutParams();
            newParams.mScrolledY = endParams.mScrolledY + mEndScrollYDv;
            //添加该view到RecyclerView
            addView(newView, 0);
            //测量View，会加上ItemDecoration中设置的Rect值
            measureChildWithMargins(newView, 0, 0);
            //计算该view布局的位置
            int left, top, right, bottom;
            //获取对应的top值
            top = getTopByScrolledY(newParams.mScrolledY);
            left = getPaddingLeft();
            //左内边距+带修饰View的宽度+view左右外边距
            right = left + getDecoratedMeasuredWidth(newView) + newParams.leftMargin
                    + newParams.rightMargin;
            bottom = top + newParams.height;
            //调用view的layout布局方法
            layoutDecoratedWithMargins(newView, left, top, right, bottom);
            Log.e("downScroll", "下滑动添加：" + (endPosition + 1));
        }
        //2.滑动
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            YtaLayoutParams params = (YtaLayoutParams) child.getLayoutParams();
            int oldTop = child.getTop();
            int top = getTopByScrolledY(params.mScrolledY + dy);
            params.mScrolledY += dy;
            child.offsetTopAndBottom(top - oldTop);
        }
        //3.回收
        View startView = getChildClosestToStart();
        YtaLayoutParams startParams = (YtaLayoutParams) startView.getLayoutParams();
        int startPosition = startParams.getViewAdapterPosition();
        if (startParams.mScrolledY + dy < mMinScrolledY) {
            recycleChildren(recycler, getChildCount() - 1, getChildCount());
            Log.e("downScroll", "下滑动回收：" + startPosition);
        }
    }

    private void upScroll(int dy, RecyclerView.Recycler recycler,
                          RecyclerView.State state) {
        //1.计算需要添加的Item
        View startView = getChildClosestToStart();
        YtaLayoutParams startParams = (YtaLayoutParams) startView.getLayoutParams();
        int startPosition = startParams.getViewAdapterPosition();
        //添加View
        if (startParams.mScrolledY + dy >= mBoundScrollY && startPosition - 1 >= 0) {
            View newView = recycler.getViewForPosition(startPosition - 1);
            YtaLayoutParams newParams = (YtaLayoutParams) newView.getLayoutParams();
            newParams.mScrolledY = startParams.mScrolledY - mStartScrollYDv;
            //添加该view到RecyclerView
            addView(newView);
            //测量View，会加上ItemDecoration中设置的Rect值
            measureChildWithMargins(newView, 0, 0);
            //计算该view布局的位置
            int left, top, right, bottom;
            //获取对应的top值
            top = getTopByScrolledY(newParams.mScrolledY);
            left = getPaddingLeft();
            //左内边距+带修饰View的宽度+view左右外边距
            right = left + getDecoratedMeasuredWidth(newView) + newParams.leftMargin
                    + newParams.rightMargin;
            bottom = top + newParams.height;
            //调用view的layout布局方法
            layoutDecoratedWithMargins(newView, left, top, right, bottom);
            Log.e("downScroll", "上滑动添加：" + (startPosition - 1));
        }
        //2.滑动
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            YtaLayoutParams params = (YtaLayoutParams) child.getLayoutParams();
            int oldTop = child.getTop();
            int top = getTopByScrolledY(params.mScrolledY + dy);
            params.mScrolledY += dy;
            child.offsetTopAndBottom(top - oldTop);
        }
        //3.回收
        View endView = getChildClosestToEnd();
        YtaLayoutParams endParams = (YtaLayoutParams) endView.getLayoutParams();
        int endPosition = endParams.getViewAdapterPosition();
        if (endParams.mScrolledY + dy > mMaxScrolledY) {
            recycleChildren(recycler, 0, 1);
            Log.e("downScroll", "上滑动回收：" + endPosition);
        }
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
                if (params.mScrolledY <= mLastItemMaxScrolledY) {
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

        if (downScroll) {
            downScroll(dy, recycler, state);
        } else {
            upScroll(dy, recycler, state);
        }
        return dy;
    }


    /**
     * 从顶部往底部走，除了第一个Item(靠近RecyclerView的底部)
     * 计算每个Item的Top值
     *
     * @param totalSpace 第一个Item的Top距离recyclerView顶部的距离
     * @return 返回从Item1开始，各个Item的Top值
     */
    private List<Integer> calcItemTop(int totalSpace) {
        List<Integer> results = new ArrayList<>();
        List<Integer> tops = new ArrayList<>();
        int num;
        int preNum = 1;
        int count = 1;
        results.add(preNum);
        for (int i = MAX_SHOW_COUNT - 2; i >= 1; i--) {
            num = preNum * 2;
            preNum = num;
            count += num;
            results.add(0, num);
        }
        for (int i = 0; i < results.size(); i++) {
            tops.add((int) (results.get(i) * 1.0f / count * totalSpace));
        }
        return tops;
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
     * 获取接近RecyclerView底部的第一个Item
     *
     * @return 第一个Item
     */
    private View getChildClosestToStart() {
        return getChildAt(getChildCount() - 1);
    }

    /**
     * 获取接近RecyclerView顶部的第一个Item
     *
     * @return 最后一个Item
     */
    private View getChildClosestToEnd() {
        return getChildAt(0);
    }

    static class YtaLayoutParams extends RecyclerView.LayoutParams {
        /**
         * 手指滑动的距离
         * 在添加Item的时候初始化该值
         * 在滑动的时候修改该值
         */
        int mScrolledY;

        public YtaLayoutParams(int width, int height) {
            super(width, height);
        }

        public YtaLayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }
    }
}
