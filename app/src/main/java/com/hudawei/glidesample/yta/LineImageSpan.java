package com.hudawei.glidesample.yta;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bumptech.glide.request.target.SimpleTarget;

/**
 * Created by hudawei on 2018/4/24.
 * 加载TextView中的图片
 * <p>
 * 预加载宽高
 * 加载中图片
 * 加载失败图片
 */

public class LineImageSpan extends YtaImageSpan {

    public LineImageSpan(YtaTextView textView, String url) {
        this(textView, url, SimpleTarget.SIZE_ORIGINAL, SimpleTarget.SIZE_ORIGINAL);
    }

    /**
     * 预设图片所占宽高，在图片加载中或加载失败时，所占位置为设置的width和height
     *
     * @param textView YtaTextView
     * @param url      图片地址
     */
    public LineImageSpan(YtaTextView textView, String url, int width, int height) {
        super(textView, url, width, height);
    }


    /**
     * TextView在Measured的时候会调用该方法
     */
    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm) {
        Bitmap b = null;
        int size;
        switch (mState) {
            case LOADING_STATE:
            case LOAD_ERROR_STATE:
                b = getRefBitmap(mTextView.getLoadingBitmapRef());
                break;
            case LOADED_STATE:
                b = getCachedBitmap();
                break;
        }

        //通过设置fm中的参数来设置图片所占高度大小
        if (fm != null) {
            fm.ascent = checkSizeValid() ? -mHeight :
                    b == null ? fm.ascent : -b.getHeight();
            fm.descent = 0;

            fm.top = fm.ascent;
            fm.bottom = 0;
        }

        //设置图片所占宽度
        size = checkSizeValid() ? mWidth :
                b == null ? 0 : b.getWidth();
        return size;
    }

    /**
     * 在TextView的onDraw执行该方法
     * 根据图片加载的状态，绘制相应图片到TextView中
     */
    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        Bitmap b = null;
        switch (mState) {
            case LOAD_ERROR_STATE:
            case LOADING_STATE:
                b = getRefBitmap(mTextView.getLoadingBitmapRef());
                break;
            case LOADED_STATE:
                b = getCachedBitmap();
                break;
        }

        if (b != null) {
            canvas.save();
            int transY = top + (bottom - top - b.getHeight()) / 2 /*bottom - b.getHeight()*/ - paint.getFontMetricsInt().descent;
            int transX = (mTextView.getWidth()
                    - mTextView.getPaddingLeft()
                    - mTextView.getPaddingRight()
                    - b.getWidth()) / 2;
            if (mState == LOADED_STATE) {
                mTempRect.set(transX, transY, transX + b.getWidth(), transY + b.getHeight());
            } else {
                mTempRect.set(mTextView.getPaddingLeft(), top, mTextView.getWidth()
                        - mTextView.getPaddingRight(), bottom);
            }
            if (mState != LOADED_STATE) {
                int oldColor = paint.getColor();
                paint.setColor(mBackgroundColor);
                canvas.drawRect(mTempRect, paint);
                paint.setColor(oldColor);
            }
            canvas.translate(transX, transY);
            if (b.getHeight() <= bottom - top)
                canvas.drawBitmap(b, 0, 0, null);
            canvas.restore();
        }
    }

}