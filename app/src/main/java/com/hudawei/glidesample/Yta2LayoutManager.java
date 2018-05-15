package com.hudawei.glidesample;

import android.content.Context;
import android.support.v4.view.ViewCompat;
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
    private final int ITEM_MAX_MARGIN = 100;

    private int mBoundScrollY;//ScrolledY零界点，零界点前面使用直线公式，后面使用4次方公式
    private int mBoundTop;//Top零界点，零界点前面使用直线公式，后面使用4次方公式
    private int mMinScrolledY;//Item的ScrollY的最小值
    private int mMaxScrolledY;//Item的ScrollY的最大值
    private int mMaxTop;//Item的最大Top值
    private int mLastItemMaxScrolledY;
    private int mFirstItemMinScrolledY;
    private List<Integer> mScrolledYs;
    private boolean mScrollFlag;

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
        mScrollFlag = true;
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
            return;
        }
        if (mScrollFlag) {
            fillStart(0, recycler, state);
            return;
        }

        //将已经Attached的View变为Detach和Scrap
        detachAndScrapAttachedViews(recycler);

        int position = 0;
        int preScrollY = 0;
        int firstItemHeight = 0;
        int preTop = 0;
        List<Integer> tops = null;
        int left = 0;
        int top = getHeight();
        int right = 0;
        int bottom = 0;
        mScrolledYs = new ArrayList<>();
        while (top > 0) {
            //从Recycler中获取一个View
            View view = recycler.getViewForPosition(position);
            YtaLayoutParams params = (YtaLayoutParams) view.getLayoutParams();
            //添加该view到RecyclerView
            addView(view, 0);
            //测量View，会加上ItemDecoration中设置的Rect值
            measureChildWithMargins(view, 0, 0);
            //计算该view布局的位置
            left = getPaddingLeft();
            //左内边距+带修饰View的宽度+view左右外边距
            right = left + getDecoratedMeasuredWidth(view) + params.leftMargin
                    + params.rightMargin;

            if (position == 0) {//如果是第一个Item
                firstItemHeight = view.getMeasuredHeight();
                //获取top值
                preTop = mBoundTop = top = getHeight() - firstItemHeight;
                mMaxTop = getHeight();
                //获取对应的ScrolledY值
                mBoundScrollY = preScrollY = params.mScrolledY = getScrolledYByTop(top);
                //计算各个Item的top值
                tops = calcItemTop(top);
            } else {

                if (position == 1) {//计算开始2个Item的ScrollY差值
                    //获取top值
                    top = preTop - tops.get(position - 1);
                    //获取对应的ScrolledY值
                    params.mScrolledY = getScrolledYByTop(top);
                    int mStartScrollYDv = params.mScrolledY - preScrollY;
                    mMinScrolledY = preScrollY - mStartScrollYDv;
                } else if (position >= MAX_SHOW_COUNT - 1) {//计算最后2个Item的ScrollY差值
                    params.mScrolledY = mBoundScrollY + (preScrollY - mMinScrolledY);
                    mMaxScrolledY = params.mScrolledY;
                    top = getTopByScrolledY(params.mScrolledY);
                } else {
                    //获取top值
                    top = preTop - tops.get(position - 1);
                    //获取对应的ScrolledY值
                    params.mScrolledY = getScrolledYByTop(top);
                }
                preScrollY = params.mScrolledY;
                preTop = top;
            }
            bottom = top + view.getMeasuredHeight();

            int margin = getMarginByTop(top);
            view.setScaleX((view.getMeasuredWidth() - 2 * margin) * 1.0f / view.getMeasuredWidth());
            view.setScaleY((view.getMeasuredWidth() - 2 * margin) * 1.0f / view.getMeasuredWidth());
            //调用view的layout布局方法
            layoutDecoratedWithMargins(view, left, top, right, bottom);
            Log.e("onLayoutChildren", position + "\tscrollY : " + params.mScrolledY + "\ttop : " + getTopByScrolledY(params.mScrolledY));
            position++;
            mScrolledYs.add(params.mScrolledY);
        }
        //设置阴影
        setElevation();
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
        if (scrollY >= mMaxScrolledY) {
            result = 0;
        } else {
            if (scrollY > mBoundScrollY) {
                result = Math.pow((h - scrollY) * 1.0f / h, 4) * h;
            } else {
                result = calcTopByScrollY(scrollY);
            }
        }
        return (int) Math.ceil(result);
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
            result = calcScrollYByTop(top);
        } else {
            int h = getHeight();
            result = (int) (h * (1 - Math.pow(top * 1.0f / h, 1.0f / 4)));
        }
        return result;
    }

    private int getEndViewTop(int dy) {
        View endView = getChildClosestToEnd();
        YtaLayoutParams endParams = (YtaLayoutParams) endView.getLayoutParams();
        return getTopByScrolledY(endParams.mScrolledY + dy);
    }

    private int getEndViewAdapterPosition() {
        View endView = getChildClosestToEnd();
        YtaLayoutParams endParams = (YtaLayoutParams) endView.getLayoutParams();
        return endParams.getViewAdapterPosition();
    }

    private int getNextScrolledY(int curTop) {
        int curScrolledY = getScrolledYByTop(curTop);
        for (int i = 0; i < mScrolledYs.size(); i++) {
            if (curScrolledY < mScrolledYs.get(i)) {
                int nextScrolledY;
                if (i != mScrolledYs.size() - 1) {
                    nextScrolledY = mScrolledYs.get(i + 1) - mScrolledYs.get(i);
                } else {
                    nextScrolledY = mScrolledYs.get(mScrolledYs.size() - 1) -
                            mScrolledYs.get(mScrolledYs.size() - 2);
                }
                return nextScrolledY + curScrolledY;
            }
        }
        return -1;
    }

    private void downScroll(int dy, RecyclerView.Recycler recycler,
                            RecyclerView.State state) {
        //1.填充
        //如果endView滑动dy后的top值大于0，就需要考虑添加View
        //我们需要填充endView的Top上面的空间
        //如果新的endView的Top > RecyclerView高度，则填充整个Child
        //如果新的endView的Top <= RecyclerView高度，则按前面保存的mScrolledYs值填充

        fillStart(dy, recycler, state);

        //设置阴影
        setElevation();

        //2.滑动
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            YtaLayoutParams params = (YtaLayoutParams) child.getLayoutParams();
            int oldTop = child.getTop();
            int top = getTopByScrolledY(params.mScrolledY + dy);
            params.mScrolledY += dy;
            if (i == 0) {
                Log.e("downScroll", "下滑动移动：" + params.getViewAdapterPosition() + "\toldTop : " + oldTop + "\ttop : " + top + "\tdy : " + dy);
            }
            int margin = getMarginByTop(top);
            child.setScaleX((child.getMeasuredWidth() - 2 * margin) * 1.0f / child.getMeasuredWidth());
            child.setScaleY((child.getMeasuredWidth() - 2 * margin) * 1.0f / child.getMeasuredWidth());

            child.offsetTopAndBottom(top - oldTop);

        }

        //3.回收
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i).getTop() > getHeight()) {
                recycleChildren(recycler, i, getChildCount());
                break;
            }
        }
    }

    private void fillStart(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int startRemaining = getEndViewTop(dy);
        int endPosition = getEndViewAdapterPosition();
        while (startRemaining > 0 && ++endPosition != state.getItemCount()) {
            View newView = recycler.getViewForPosition(endPosition);
            YtaLayoutParams newParams = (YtaLayoutParams) newView.getLayoutParams();
            //添加该view到RecyclerView
            addView(newView, 0);
            //测量View，会加上ItemDecoration中设置的Rect值
            measureChildWithMargins(newView, 0, 0);
            //计算该view布局的位置
            int left, top, right, bottom;
            left = getPaddingLeft();
            //左内边距+带修饰View的宽度+view左右外边距
            right = left + getDecoratedMeasuredWidth(newView) + newParams.leftMargin
                    + newParams.rightMargin;
            //计算该view布局的位置
            if (startRemaining > getHeight()) {
                startRemaining -= newView.getMeasuredHeight();
                newParams.mScrolledY = getScrolledYByTop(startRemaining) - dy;
            } else {
                int scrolledY = getNextScrolledY(startRemaining);
                startRemaining = getTopByScrolledY(scrolledY);
                newParams.mScrolledY = scrolledY - dy;
            }
            top = getTopByScrolledY(newParams.mScrolledY);
            bottom = top + newView.getMeasuredHeight();
            int margin = getMarginByTop(top);
            newView.setScaleX((newView.getMeasuredWidth() - 2 * margin) * 1.0f / newView.getMeasuredWidth());
            newView.setScaleY((newView.getMeasuredWidth() - 2 * margin) * 1.0f / newView.getMeasuredWidth());
            //调用view的layout布局方法
            layoutDecoratedWithMargins(newView, left, top, right, bottom);
        }
    }

    private int getStartScrolledY(int dy) {
        View startView = getChildClosestToStart();
        YtaLayoutParams startParams = (YtaLayoutParams) startView.getLayoutParams();
        return startParams.mScrolledY + dy;
    }

    private int getStartAdapterPosition() {
        View startView = getChildClosestToStart();
        YtaLayoutParams startParams = (YtaLayoutParams) startView.getLayoutParams();
        return startParams.getViewAdapterPosition();
    }

    private int getStartBottom() {
        View startView = getChildClosestToStart();
        return startView.getBottom();
    }

    private void upScroll(int dy, RecyclerView.Recycler recycler,
                          RecyclerView.State state) {
        //1.填充
        //如果startView的mScrolledY + dy是否大于mBoundScrolledY，如果大于需要在后面添加新View
        //新View的Top值为前一个View的Bottom

        int startPosition = getStartAdapterPosition();
        while (getStartScrolledY(dy) > mBoundScrollY && --startPosition >= 0) {
            int startBottom = getStartBottom();
            View newView = recycler.getViewForPosition(startPosition);
            YtaLayoutParams newParams = (YtaLayoutParams) newView.getLayoutParams();
            //添加该view到RecyclerView
            addView(newView);
            //测量View，会加上ItemDecoration中设置的Rect值
            measureChildWithMargins(newView, 0, 0);
            //计算该view布局的位置
            int left, top, right, bottom;
            left = getPaddingLeft();
            //左内边距+带修饰View的宽度+view左右外边距
            right = left + getDecoratedMeasuredWidth(newView) + newParams.leftMargin
                    + newParams.rightMargin;

            top = startBottom;
            bottom = top + newView.getMeasuredHeight();

            int margin = getMarginByTop(top);
            newView.setScaleX((newView.getMeasuredWidth() - 2 * margin) * 1.0f / newView.getMeasuredWidth());
            newView.setScaleY((newView.getMeasuredWidth() - 2 * margin) * 1.0f / newView.getMeasuredWidth());
            newParams.mScrolledY = getScrolledYByTop(top);
            //调用view的layout布局方法
            layoutDecoratedWithMargins(newView, left, top, right, bottom);
        }

        //设置阴影
        setElevation();

        //2.滑动
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            YtaLayoutParams params = (YtaLayoutParams) child.getLayoutParams();
            int oldTop = child.getTop();
            int top = getTopByScrolledY(params.mScrolledY + dy);
            params.mScrolledY += dy;
            if (i == 0) {
                Log.e("downScroll", "下滑动移动：" + params.getViewAdapterPosition() + "\toldTop : " + oldTop + "\ttop : " + top + "\tdy : " + dy);
            }
            child.offsetTopAndBottom(top - oldTop);

            int margin = getMarginByTop(top);
            child.setScaleX((child.getMeasuredWidth() - 2 * margin) * 1.0f / child.getMeasuredWidth());
            child.setScaleY((child.getMeasuredWidth() - 2 * margin) * 1.0f / child.getMeasuredWidth());
        }

        //3.回收
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i).getTop() > 0) {
                //Top[i - 1] <= 0 ,Top[i - 2] <= 0
                recycleChildren(recycler, 0, i - 2);
                break;
            }
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
        Log.e("scrollBy", "dy : " + Math.abs(dy));
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
            view.setScaleX(1);
            view.setScaleY(1);
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
     * 通过当前Item的Top值，求左右边距值
     *
     * @param curTop Item的top值
     * @return 对应的Margin值
     */
    private int getMarginByTop(int curTop) {
        int result = (int) (ITEM_MAX_MARGIN * (mBoundTop - curTop) * 1.0f / mBoundTop);
        if (result < 0)
            result = 0;
        else if (result > ITEM_MAX_MARGIN)
            result = ITEM_MAX_MARGIN;
        return result;
    }

    /**
     * 设置阴影
     */
    private void setElevation() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            ViewCompat.setElevation(child, 2 * (i + 1));
        }
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
