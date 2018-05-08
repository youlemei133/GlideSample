package com.hudawei.glidesample;

import android.graphics.Rect;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import static android.support.v7.widget.RecyclerView.NO_POSITION;

/**
 * Created by hudawei on 2018/5/7.
 * <p>
 * LayoutManager
 *
 * 1.isAutoMeasureEnabled()
 * 2.scrollVerticallyBy 滑动的时候调用
 * 3.onLayoutChildren   布局的时候调用
 */

public class YtaLayoutManager extends RecyclerView.LayoutManager {
    public static final int INVALID_OFFSET = Integer.MIN_VALUE;
    private LayoutState mLayoutState;
    final AnchorInfo mAnchorInfo = new AnchorInfo();
    private final LayoutChunkResult mLayoutChunkResult = new LayoutChunkResult();

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public boolean isAutoMeasureEnabled() {
        return true;
    }

    boolean shouldMeasureTwice() {
        return getHeightMode() != View.MeasureSpec.EXACTLY
                && getWidthMode() != View.MeasureSpec.EXACTLY
                && hasFlexibleChildInBothOrientations();
    }

    boolean hasFlexibleChildInBothOrientations() {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final ViewGroup.LayoutParams lp = child.getLayoutParams();
            if (lp.width < 0 && lp.height < 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                                  RecyclerView.State state) {
        return scrollBy(dy, recycler, state);
    }

    int scrollBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0 || dy == 0) {
            return 0;
        }
        mLayoutState.mRecycle = true;
        ensureLayoutState();
        final int layoutDirection = dy > 0 ? LayoutState.LAYOUT_END : LayoutState.LAYOUT_START;
        final int absDy = Math.abs(dy);
        updateLayoutState(layoutDirection, absDy, true, state);
        final int consumed = mLayoutState.mScrollingOffset
                + fill(recycler, mLayoutState, state, false);
        if (consumed < 0) {
            return 0;
        }
        final int scrolled = absDy > consumed ? layoutDirection * consumed : dy;
        offsetChildrenVertical(-scrolled);
        mLayoutState.mLastScrollDelta = scrolled;
        return scrolled;
    }

    private void updateLayoutState(int layoutDirection, int requiredSpace,
                                   boolean canUseExistingSpace, RecyclerView.State state) {
        // If parent provides a hint, don't measure unlimited.
        mLayoutState.mInfinite = resolveIsInfinite();
        mLayoutState.mExtra = getExtraLayoutSpace(state);
        mLayoutState.mLayoutDirection = layoutDirection;
        int scrollingOffset;
        if (layoutDirection == LayoutState.LAYOUT_END) {
            mLayoutState.mExtra += getPaddingBottom();
            // get the first child in the direction we are going
            final View child = getChildClosestToEnd();
            // the direction in which we are traversing children
            mLayoutState.mItemDirection = LayoutState.ITEM_DIRECTION_HEAD;
            mLayoutState.mCurrentPosition = getPosition(child) + mLayoutState.mItemDirection;
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                    child.getLayoutParams();
            mLayoutState.mOffset = getDecoratedBottom(child) + params.bottomMargin;
            // calculate how much we can scroll without adding new children (independent of layout)
            scrollingOffset = mLayoutState.mOffset
                    - (getHeight() - getPaddingBottom());

        } else {
            final View child = getChildClosestToStart();
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                    child.getLayoutParams();
            mLayoutState.mExtra += getPaddingTop();
            mLayoutState.mItemDirection = LayoutState.ITEM_DIRECTION_TAIL;
            mLayoutState.mCurrentPosition = getPosition(child) + mLayoutState.mItemDirection;
            mLayoutState.mOffset = getDecoratedTop(child) - params.topMargin;
            scrollingOffset = -mLayoutState.mOffset
                    + getPaddingTop();
        }
        mLayoutState.mAvailable = requiredSpace;
        if (canUseExistingSpace) {
            mLayoutState.mAvailable -= scrollingOffset;
        }
        mLayoutState.mScrollingOffset = scrollingOffset;
    }

    private View getChildClosestToEnd() {
        return getChildAt(0);
    }

    private View getChildClosestToStart() {
        return getChildAt(getChildCount() - 1);
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);
        mAnchorInfo.reset();
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);
        if (state.getItemCount() == 0) {
            //如过Adapter中的item个数为0，就回收Recycler中所有的View
            removeAndRecycleAllViews(recycler);
        }
        ensureLayoutState();
        mLayoutState.mRecycle = false;
        if (!mAnchorInfo.mValid) {
            mAnchorInfo.reset();
            //从底部开始布局
            mAnchorInfo.mLayoutFromEnd = true;
            // calculate anchor position and coordinate
            //开始位置
            mAnchorInfo.mPosition = 0;
            //起点为底部开始
            mAnchorInfo.mCoordinate = getHeight() - getPaddingBottom();
            mAnchorInfo.mValid = true;
        }
        int extraForStart;
        int extraForEnd;
        final int extra = getExtraLayoutSpace(state);
        if (mLayoutState.mLastScrollDelta >= 0) {
            extraForEnd = extra;
            extraForStart = 0;
        } else {
            extraForStart = extra;
            extraForEnd = 0;
        }
        extraForStart += getPaddingTop();
        extraForEnd += getPaddingBottom();
        //省略...
        int startOffset;
        int endOffset;
        //将已经Attached的View变为Detach和Scrap
        detachAndScrapAttachedViews(recycler);
        //mInfinite，是否无限布局
        mLayoutState.mInfinite = resolveIsInfinite();
        //是否预布局
        mLayoutState.mIsPreLayout = state.isPreLayout();

        // fill towards start
        //更新mLayoutState中的mAvaliable和mOffset为mAnchorInfo.mCoordinate
        updateLayoutStateToFillStart(mAnchorInfo);
        mLayoutState.mExtra = extraForStart;
        //从底部开始，向上布局
        fill(recycler, mLayoutState, state, false);
        //可见Item中最后一个Item的top值
        startOffset = mLayoutState.mOffset;
        //第一个可见元素的位置为当前位置
        final int firstElement = mLayoutState.mCurrentPosition;
        if (mLayoutState.mAvailable > 0) {
            extraForEnd += mLayoutState.mAvailable;
        }
        // fill towards end
        //这里几乎没执行，只是改变了一下mLayoutState
        updateLayoutStateToFillEnd(mAnchorInfo);
        mLayoutState.mExtra = extraForEnd;
        mLayoutState.mCurrentPosition += mLayoutState.mItemDirection;
        fill(recycler, mLayoutState, state, false);
        //计算endOffset为0
        endOffset = mLayoutState.mOffset;

        if (mLayoutState.mAvailable > 0) {
            // end could not consume all. add more items towards start
            extraForStart = mLayoutState.mAvailable;
            updateLayoutStateToFillStart(firstElement, startOffset);
            mLayoutState.mExtra = extraForStart;
            fill(recycler, mLayoutState, state, false);
            startOffset = mLayoutState.mOffset;
        }

        if (getChildCount() > 0) {
            int fixOffset = fixLayoutEndGap(endOffset, recycler, state, true);
            startOffset += fixOffset;
            endOffset += fixOffset;
            fixOffset = fixLayoutStartGap(startOffset, recycler, state, false);
            startOffset += fixOffset;
            endOffset += fixOffset;
        }

    }

    /**
     * @return The final offset amount for children
     */
    private int fixLayoutEndGap(int endOffset, RecyclerView.Recycler recycler,
                                RecyclerView.State state, boolean canOffsetChildren) {
        int gap = getHeight() - getPaddingBottom() - endOffset;
        int fixOffset = 0;
        if (gap > 0) {
            fixOffset = -scrollBy(-gap, recycler, state);
        } else {
            return 0; // nothing to fix
        }
        // move offset according to scroll amount
        endOffset += fixOffset;
        if (canOffsetChildren) {
            // re-calculate gap, see if we could fix it
            gap = getHeight() - getPaddingBottom() - endOffset;
            if (gap > 0) {
                offsetChildrenVertical(gap);
                return gap + fixOffset;
            }
        }
        return fixOffset;
    }

    /**
     * @return The final offset amount for children
     */
    private int fixLayoutStartGap(int startOffset, RecyclerView.Recycler recycler,
                                  RecyclerView.State state, boolean canOffsetChildren) {
        int gap = startOffset - getPaddingTop();
        int fixOffset = 0;
        if (gap > 0) {
            // check if we should fix this gap.
            fixOffset = -scrollBy(gap, recycler, state);
        } else {
            return 0; // nothing to fix
        }
        startOffset += fixOffset;
        if (canOffsetChildren) {
            // re-calculate gap, see if we could fix it
            gap = startOffset - getPaddingTop();
            if (gap > 0) {
                offsetChildrenVertical(-gap);
                return fixOffset - gap;
            }
        }
        return fixOffset;
    }


    int fill(RecyclerView.Recycler recycler, LayoutState layoutState,
             RecyclerView.State state, boolean stopOnFocusable) {
        // max offset we should set is mFastScroll + available
        final int start = layoutState.mAvailable;
        if (layoutState.mScrollingOffset != LayoutState.SCROLLING_OFFSET_NaN) {
            // TODO ugly bug fix. should not happen
            if (layoutState.mAvailable < 0) {
                layoutState.mScrollingOffset += layoutState.mAvailable;
            }
            recycleByLayoutState(recycler, layoutState);
        }
        //剩余竖直方向可用空间
        int remainingSpace = layoutState.mAvailable + layoutState.mExtra;
        LayoutChunkResult layoutChunkResult = mLayoutChunkResult;
        //如果竖直方向仍有剩余，并且当前还有item
        while ((layoutState.mInfinite || remainingSpace > 0) && layoutState.hasMore(state)) {
            //重置layoutChunkResult
            layoutChunkResult.resetInternal();
            //从Recycler中获取一个View,将该View添加到RecyclerView中，并且测量，布局该View
            layoutChunk(recycler, state, layoutState, layoutChunkResult);
            if (layoutChunkResult.mFinished) {
                break;
            }
            //修改偏移量，由于上面布局了一个新的View,所以mOffset需要减去该View所占高度
            layoutState.mOffset += layoutChunkResult.mConsumed * layoutState.mLayoutDirection;
            /**
             * Consume the available space if:
             * * layoutChunk did not request to be ignored
             * * OR we are laying out scrap children
             * * OR we are not doing pre-layout
             */
            if (!layoutChunkResult.mIgnoreConsumed || mLayoutState.mScrapList != null
                    || !state.isPreLayout()) {
                //可用空间 需要减去上面view消耗的空间
                layoutState.mAvailable -= layoutChunkResult.mConsumed;
                // we keep a separate remaining space because mAvailable is important for recycling
                //总的空间减去上面销毁的空间
                remainingSpace -= layoutChunkResult.mConsumed;
            }

            if (layoutState.mScrollingOffset != LayoutState.SCROLLING_OFFSET_NaN) {
                layoutState.mScrollingOffset += layoutChunkResult.mConsumed;
                if (layoutState.mAvailable < 0) {
                    layoutState.mScrollingOffset += layoutState.mAvailable;
                }
                recycleByLayoutState(recycler, layoutState);
            }
            if (stopOnFocusable && layoutChunkResult.mFocusable) {
                break;
            }
        }
        //开始的总空间 将去 剩余可用空间，mAvailable可能为负数，应为最后一个View布局可能超过了start
        return start - layoutState.mAvailable;
    }

    private void recycleByLayoutState(RecyclerView.Recycler recycler, LayoutState layoutState) {
        if (!layoutState.mRecycle || layoutState.mInfinite) {
            return;
        }
        if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
            recycleViewsFromEnd(recycler, layoutState.mScrollingOffset);
        } else {
            recycleViewsFromStart(recycler, layoutState.mScrollingOffset);
        }
    }

    private void recycleViewsFromEnd(RecyclerView.Recycler recycler, int dt) {
        final int childCount = getChildCount();
        if (dt < 0) {
            return;
        }
        final int limit = getHeight() - dt;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            Rect mTmpRect = new Rect();
            getTransformedBoundingBox(child, true, mTmpRect);
            if (getDecoratedTop(child) - params.topMargin < limit
                    || mTmpRect.top < limit) {
                // stop here
                recycleChildren(recycler, 0, i);
                return;
            }
        }
    }

    private void recycleViewsFromStart(RecyclerView.Recycler recycler, int dt) {
        if (dt < 0) {
            return;
        }
        // ignore padding, ViewGroup may not clip children.
        final int limit = dt;
        final int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View child = getChildAt(i);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            Rect mTmpRect = new Rect();
            getTransformedBoundingBox(child, true, mTmpRect);
            if (getDecoratedBottom(child) + params.bottomMargin > limit
                    || mTmpRect.bottom > limit) {
                // stop here
                recycleChildren(recycler, childCount - 1, i);
                return;
            }
        }
    }

    private void recycleChildren(RecyclerView.Recycler recycler, int startIndex, int endIndex) {
        if (startIndex == endIndex) {
            return;
        }
        if (endIndex > startIndex) {
            for (int i = endIndex - 1; i >= startIndex; i--) {
                removeAndRecycleViewAt(i, recycler);
            }
        } else {
            for (int i = startIndex; i > endIndex; i--) {
                removeAndRecycleViewAt(i, recycler);
            }
        }
    }

    void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state,
                     LayoutState layoutState, LayoutChunkResult result) {
        //从Recycler中获取一个View
        View view = layoutState.next(recycler);
        //如果获取View失败，则结束布局
        if (view == null) {
            // if we are laying out views in scrap, this may return null which means there is
            // no more items to layout.
            result.mFinished = true;
            return;
        }
        //获取新View的LayoutParams
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        if (layoutState.mScrapList == null) {
            if(layoutState.mLayoutDirection
                    == LayoutState.LAYOUT_START){
                addView(view);//添加该view到RecyclerView
            } else {
                addView(view, 0);
            }
        }
        //测量View，会加上ItemDecoration中设置的Rect值
        measureChildWithMargins(view, 0, 0);
        //设置mConsumed为该view所占空间，包括高度、上下边距、上下修饰Rect
        result.mConsumed = getDecoratedMeasuredHeight(view) + params.topMargin
                + params.bottomMargin;

        //计算该view布局的位置
        int left, top, right, bottom;

        left = getPaddingLeft();
        //左内边距+带修饰View的宽度+view左右外边距
        right = left + getDecoratedMeasuredWidth(view) + params.leftMargin
                + params.rightMargin;
        if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
            //第一次时mOffset为RecyclerView的高度，所以第一个View的bottom为mOffset
            bottom = layoutState.mOffset;
            //mConsumed为该view所占高度大小
            top = layoutState.mOffset - result.mConsumed;
        } else {
            top = layoutState.mOffset;
            bottom = layoutState.mOffset + result.mConsumed;
        }
        // We calculate everything with View's bounding box (which includes decor and margins)
        // To calculate correct layout position, we subtract margins.
        //调用view的layout布局方法
        layoutDecoratedWithMargins(view, left, top, right, bottom);
        // Consume the available space if the view is not removed OR changed
        if (params.isItemRemoved() || params.isItemChanged()) {
            result.mIgnoreConsumed = true;
        }
        result.mFocusable = view.hasFocusable();
    }

    private void updateLayoutStateToFillStart(AnchorInfo anchorInfo) {
        updateLayoutStateToFillStart(anchorInfo.mPosition, anchorInfo.mCoordinate);
    }

    private void updateLayoutStateToFillStart(int itemPosition, int offset) {
        mLayoutState.mAvailable = offset - getPaddingTop();
        mLayoutState.mCurrentPosition = itemPosition;
        mLayoutState.mItemDirection = LayoutState.ITEM_DIRECTION_TAIL;
        mLayoutState.mLayoutDirection = LayoutState.LAYOUT_START;
        mLayoutState.mOffset = offset;
        mLayoutState.mScrollingOffset = LayoutState.SCROLLING_OFFSET_NaN;

    }

    private void updateLayoutStateToFillEnd(AnchorInfo anchorInfo) {
        updateLayoutStateToFillEnd(anchorInfo.mPosition, anchorInfo.mCoordinate);
    }

    private void updateLayoutStateToFillEnd(int itemPosition, int offset) {
        mLayoutState.mAvailable = getHeight() - getPaddingBottom() - offset;
        mLayoutState.mItemDirection = LayoutState.ITEM_DIRECTION_HEAD;
        mLayoutState.mCurrentPosition = itemPosition;
        mLayoutState.mLayoutDirection = LayoutState.LAYOUT_END;
        mLayoutState.mOffset = offset;
        mLayoutState.mScrollingOffset = LayoutState.SCROLLING_OFFSET_NaN;
    }

    boolean resolveIsInfinite() {
        return getHeightMode() == View.MeasureSpec.UNSPECIFIED
                && getHeight() == 0;
    }

    protected int getExtraLayoutSpace(RecyclerView.State state) {
        if (state.hasTargetScrollPosition()) {
            return getHeight() - getPaddingBottom() - getPaddingTop();
        } else {
            return 0;
        }
    }

    void ensureLayoutState() {
        if (mLayoutState == null) {
            mLayoutState = new LayoutState();
        }
    }

    static class LayoutState {

        static final String TAG = "LLM#LayoutState";

        static final int LAYOUT_START = -1;

        static final int LAYOUT_END = 1;

        static final int INVALID_LAYOUT = Integer.MIN_VALUE;

        static final int ITEM_DIRECTION_HEAD = -1;

        static final int ITEM_DIRECTION_TAIL = 1;

        static final int SCROLLING_OFFSET_NaN = Integer.MIN_VALUE;

        /**
         * We may not want to recycle children in some cases (e.g. layout)
         */
        boolean mRecycle = true;

        /**
         * Pixel offset where layout should start
         */
        int mOffset;

        /**
         * Number of pixels that we should fill, in the layout direction.
         */
        int mAvailable;

        /**
         * Current position on the adapter to get the next item.
         */
        int mCurrentPosition;

        /**
         * Defines the direction in which the data adapter is traversed.
         * Should be {@link #ITEM_DIRECTION_HEAD} or {@link #ITEM_DIRECTION_TAIL}
         */
        int mItemDirection;

        /**
         * Defines the direction in which the layout is filled.
         * Should be {@link #LAYOUT_START} or {@link #LAYOUT_END}
         */
        int mLayoutDirection;

        /**
         * Used when LayoutState is constructed in a scrolling state.
         * It should be set the amount of scrolling we can make without creating a new view.
         * Settings this is required for efficient view recycling.
         */
        int mScrollingOffset;

        /**
         * Used if you want to pre-layout items that are not yet visible.
         * The difference with {@link #mAvailable} is that, when recycling, distance laid out for
         * {@link #mExtra} is not considered to avoid recycling visible children.
         */
        int mExtra = 0;

        /**
         * Equal to {@link RecyclerView.State#isPreLayout()}. When consuming scrap, if this value
         * is set to true, we skip removed views since they should not be laid out in post layout
         * step.
         */
        boolean mIsPreLayout = false;

        int mLastScrollDelta;

        /**
         * When LLM needs to layout particular views, it sets this list in which case, LayoutState
         * will only return views from this list and return null if it cannot find an item.
         */
        List<RecyclerView.ViewHolder> mScrapList = null;

        /**
         * Used when there is no limit in how many views can be laid out.
         */
        boolean mInfinite;

        /**
         * @return true if there are more items in the data adapter
         */
        boolean hasMore(RecyclerView.State state) {
            return mCurrentPosition >= 0 && mCurrentPosition < state.getItemCount();
        }

        /**
         * Gets the view for the next element that we should layout.
         * Also updates current item index to the next item, based on {@link #mItemDirection}
         *
         * @return The next element that we should layout.
         */
        View next(RecyclerView.Recycler recycler) {
            if (mScrapList != null) {
                return nextViewFromScrapList();
            }
            final View view = recycler.getViewForPosition(mCurrentPosition);
            mCurrentPosition += mItemDirection;
            return view;
        }

        /**
         * Returns the next item from the scrap list.
         * <p>
         * Upon finding a valid VH, sets current item position to VH.itemPosition + mItemDirection
         *
         * @return View if an item in the current position or direction exists if not null.
         */
        private View nextViewFromScrapList() {
            final int size = mScrapList.size();
            for (int i = 0; i < size; i++) {
                final View view = mScrapList.get(i).itemView;
                final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
                if (lp.isItemRemoved()) {
                    continue;
                }
                if (mCurrentPosition == lp.getViewLayoutPosition()) {
                    assignPositionFromScrapList(view);
                    return view;
                }
            }
            return null;
        }

        public void assignPositionFromScrapList() {
            assignPositionFromScrapList(null);
        }

        public void assignPositionFromScrapList(View ignore) {
            final View closest = nextViewInLimitedList(ignore);
            if (closest == null) {
                mCurrentPosition = NO_POSITION;
            } else {
                mCurrentPosition = ((RecyclerView.LayoutParams) closest.getLayoutParams())
                        .getViewLayoutPosition();
            }
        }

        public View nextViewInLimitedList(View ignore) {
            int size = mScrapList.size();
            View closest = null;
            int closestDistance = Integer.MAX_VALUE;
            for (int i = 0; i < size; i++) {
                View view = mScrapList.get(i).itemView;
                final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
                if (view == ignore || lp.isItemRemoved()) {
                    continue;
                }
                final int distance = (lp.getViewLayoutPosition() - mCurrentPosition)
                        * mItemDirection;
                if (distance < 0) {
                    continue; // item is not in current direction
                }
                if (distance < closestDistance) {
                    closest = view;
                    closestDistance = distance;
                    if (distance == 0) {
                        break;
                    }
                }
            }
            return closest;
        }

        void log() {
            Log.d(TAG, "avail:" + mAvailable + ", ind:" + mCurrentPosition + ", dir:"
                    + mItemDirection + ", offset:" + mOffset + ", layoutDir:" + mLayoutDirection);
        }
    }

    static class AnchorInfo {
        OrientationHelper mOrientationHelper;
        int mPosition;
        int mCoordinate;
        boolean mLayoutFromEnd;
        boolean mValid;

        AnchorInfo() {
            reset();
        }

        void reset() {
            mPosition = NO_POSITION;
            mCoordinate = INVALID_OFFSET;
            mLayoutFromEnd = false;
            mValid = false;
        }

        /**
         * assigns anchor coordinate from the RecyclerView's padding depending on current
         * layoutFromEnd value
         */
        void assignCoordinateFromPadding() {
            mCoordinate = mLayoutFromEnd
                    ? mOrientationHelper.getEndAfterPadding()
                    : mOrientationHelper.getStartAfterPadding();
        }

        @Override
        public String toString() {
            return "AnchorInfo{"
                    + "mPosition=" + mPosition
                    + ", mCoordinate=" + mCoordinate
                    + ", mLayoutFromEnd=" + mLayoutFromEnd
                    + ", mValid=" + mValid
                    + '}';
        }

        boolean isViewValidAsAnchor(View child, RecyclerView.State state) {
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
            return !lp.isItemRemoved() && lp.getViewLayoutPosition() >= 0
                    && lp.getViewLayoutPosition() < state.getItemCount();
        }

        public void assignFromViewAndKeepVisibleRect(View child, int position) {
            final int spaceChange = mOrientationHelper.getTotalSpaceChange();
            if (spaceChange >= 0) {
                assignFromView(child, position);
                return;
            }
            mPosition = position;
            if (mLayoutFromEnd) {
                final int prevLayoutEnd = mOrientationHelper.getEndAfterPadding() - spaceChange;
                final int childEnd = mOrientationHelper.getDecoratedEnd(child);
                final int previousEndMargin = prevLayoutEnd - childEnd;
                mCoordinate = mOrientationHelper.getEndAfterPadding() - previousEndMargin;
                // ensure we did not push child's top out of bounds because of this
                if (previousEndMargin > 0) { // we have room to shift bottom if necessary
                    final int childSize = mOrientationHelper.getDecoratedMeasurement(child);
                    final int estimatedChildStart = mCoordinate - childSize;
                    final int layoutStart = mOrientationHelper.getStartAfterPadding();
                    final int previousStartMargin = mOrientationHelper.getDecoratedStart(child)
                            - layoutStart;
                    final int startReference = layoutStart + Math.min(previousStartMargin, 0);
                    final int startMargin = estimatedChildStart - startReference;
                    if (startMargin < 0) {
                        // offset to make top visible but not too much
                        mCoordinate += Math.min(previousEndMargin, -startMargin);
                    }
                }
            } else {
                final int childStart = mOrientationHelper.getDecoratedStart(child);
                final int startMargin = childStart - mOrientationHelper.getStartAfterPadding();
                mCoordinate = childStart;
                if (startMargin > 0) { // we have room to fix end as well
                    final int estimatedEnd = childStart
                            + mOrientationHelper.getDecoratedMeasurement(child);
                    final int previousLayoutEnd = mOrientationHelper.getEndAfterPadding()
                            - spaceChange;
                    final int previousEndMargin = previousLayoutEnd
                            - mOrientationHelper.getDecoratedEnd(child);
                    final int endReference = mOrientationHelper.getEndAfterPadding()
                            - Math.min(0, previousEndMargin);
                    final int endMargin = endReference - estimatedEnd;
                    if (endMargin < 0) {
                        mCoordinate -= Math.min(startMargin, -endMargin);
                    }
                }
            }
        }

        public void assignFromView(View child, int position) {
            if (mLayoutFromEnd) {
                mCoordinate = mOrientationHelper.getDecoratedEnd(child)
                        + mOrientationHelper.getTotalSpaceChange();
            } else {
                mCoordinate = mOrientationHelper.getDecoratedStart(child);
            }

            mPosition = position;
        }
    }

    protected static class LayoutChunkResult {
        public int mConsumed;
        public boolean mFinished;
        public boolean mIgnoreConsumed;
        public boolean mFocusable;

        void resetInternal() {
            mConsumed = 0;
            mFinished = false;
            mIgnoreConsumed = false;
            mFocusable = false;
        }
    }
}
