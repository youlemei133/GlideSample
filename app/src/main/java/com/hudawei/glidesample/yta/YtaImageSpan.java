package com.hudawei.glidesample.yta;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.style.ReplacementSpan;
import android.util.Log;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.hudawei.glidesample.GlideApp;

import java.lang.ref.WeakReference;

/**
 * Created by hudawei on 2018/5/2.
 * YtaImageSpan
 */

/**
 * Created by hudawei on 2018/4/24.
 * 加载TextView中的图片
 * <p>
 * 预加载宽高
 * 加载中图片
 * 加载失败图片
 */

public class YtaImageSpan extends ReplacementSpan {
    /**
     * 加载中
     */
    public final static int LOADING_STATE = 0;
    /**
     * 加载成功
     */
    public final static int LOADED_STATE = 1;
    /**
     * 加载失败
     */
    public final static int LOAD_ERROR_STATE = 2;
    /**
     * 设置span的目标TextView
     */
    YtaTextView mTextView;
    /**
     * 图片网络地址
     */
    String mUrl;
    /**
     * 图片引用
     */
    WeakReference<Bitmap> mBitmapRef;
    /**
     * 图片目标大小
     */
    int mWidth;
    /**
     * 图片目标大小
     */
    int mHeight;
    /**
     * 图片加载状态
     */
    int mState;
    /**
     * 该图片的矩形区域
     */
    Rect mTempRect;
    /**
     * 图片背景颜色
     */
    int mBackgroundColor;
    /**
     * Glide请求的Target
     */
    YtaImageTarget<Bitmap> mTarget;

    public YtaImageSpan(YtaTextView textView, String url) {
        this(textView, url, SimpleTarget.SIZE_ORIGINAL, SimpleTarget.SIZE_ORIGINAL);
    }

    /**
     * 预设图片所占宽高，在图片加载中或加载失败时，所占位置为设置的width和height
     *
     * @param textView YtaTextView
     * @param url      图片地址
     */
    public YtaImageSpan(YtaTextView textView, String url, int width, int height) {
        mTextView = textView;
        mUrl = url;
        mTempRect = new Rect();
        mBackgroundColor = Color.argb(0xFF, 0xEE, 0xEE, 0xEE);

        if (width == SimpleTarget.SIZE_ORIGINAL && height == SimpleTarget.SIZE_ORIGINAL) {
            mWidth = width;
            mHeight = height;
        } else {
            mWidth = width;
            mHeight = height;
        }
    }

    /**
     * 创建一个SimpleTarget<Drawable>用于Glide请求
     */
    void createTarget() {
        mTarget = new YtaImageTarget<Bitmap>(mWidth, mHeight) {
            @Override
            public void onLoadStarted(@Nullable Drawable placeholder) {
                super.onLoadStarted(placeholder);
                mState = LOADING_STATE;
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                super.onLoadFailed(errorDrawable);
                mState = LOAD_ERROR_STATE;
            }

            @Override
            public void onResourceReady(@NonNull Bitmap d, @Nullable Transition<? super Bitmap> transition) {
                if (getRefBitmap(mBitmapRef) == null) {
                    mBitmapRef = new WeakReference<>(d);
                }
                mState = LOADED_STATE;
                refreshTextView();
            }

        };
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

    /**
     * 获取绘制的Bitmap
     * 先从缓存中取，如果没有取到可用的Bitmap，则Glide请求
     *
     * @return Bitmap有可能为null
     */
    Bitmap getCachedBitmap() {
        Bitmap b = getRefBitmap(mBitmapRef);

        if (b == null) {
            requestBitmap();
        }

        return b;
    }

    /**
     * 获取软引用中的Bitmap
     *
     * @param bitmapRef Bitmap软引用
     * @return 如果该Bitmap不可用，返回null
     */
    Bitmap getRefBitmap(WeakReference<Bitmap> bitmapRef) {
        if (bitmapRef != null) {
            Bitmap b = bitmapRef.get();
            if (b != null && !b.isRecycled())
                return b;
        }
        return null;
    }

    /**
     * Glide请求图片，如果图片存在则结束
     */
    public synchronized void requestBitmap() {
        if (getRefBitmap(mBitmapRef) != null) {
            return;
        }
        if (mTarget == null) {
            createTarget();
        }
        int MaxWidth = mTextView.getWidth() - mTextView.getPaddingLeft() - mTextView.getPaddingRight();
        if (mWidth > MaxWidth && MaxWidth > 0) {
            mWidth = MaxWidth;
            mHeight = (int) (MaxWidth * 1.0f / mWidth * mHeight);
        }
        mTarget.resetSize(mWidth, mHeight);
        GlideApp.with(mTextView.getContext())
                .asBitmap()
                .fitCenter()
                .load(mUrl)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(mTarget);
    }

    /**
     * 取消图片请求
     */
    public synchronized void clearRequest() {
        GlideApp.with(mTextView.getContext()).clear(mTarget);
    }

    /**
     * 回收Bitmap
     */
    public void recycleBitmap() {
        if (mBitmapRef != null) {
            Bitmap bitmap = mBitmapRef.get();
            if (bitmap != null && !bitmap.isRecycled()) {
                Log.e("LineImageSpan", "recycleBitmap");
                bitmap.recycle();
                mBitmapRef = null;
            }
        }
    }

    /**
     * 获取矩形区域
     *
     * @return Rect
     */
    public Rect getRect() {
        return mTempRect;
    }

    /**
     * 获取图片加载状态
     *
     * @return 图片加载状态
     */
    public int getState() {
        return mState;
    }

    /**
     * 获取图片地址
     *
     * @return 图片地址
     */
    public String getUrl() {
        return mUrl;
    }

    /**
     * drawable 转换成 bitmap
     * 如果该Drawable的宽度超过了TextView的最大宽度，则将Bitmap宽度缩小至最大宽度
     *
     * @param drawable 需要转化成Bitmap的Drawable
     * @return 转换后的Bitmap
     */
    Bitmap drawableToBitmap(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();   // 取 drawable 的长宽
        int height = drawable.getIntrinsicHeight();

        float viewSpace = mTextView.getWidth() - mTextView.getPaddingRight() - mTextView.getPaddingLeft();
        float scale = width > viewSpace ? viewSpace / width : 1;

        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;         // 取 drawable 的颜色格式
        Bitmap bitmap = Bitmap.createBitmap((int) (width * scale), (int) (height * scale), config);     // 建立对应 bitmap

        Canvas canvas = new Canvas(bitmap);         // 建立对应 bitmap 的画布
        drawable.setBounds(0, 0, (int) (width * scale), (int) (height * scale));
        drawable.draw(canvas);      // 把 drawable 内容画到画布中
        return bitmap;
    }

    /**
     * 检查是否设置了合法的width和height
     */
    boolean checkSizeValid() {
        return mWidth > 0 && mHeight > 0;
    }

    /**
     * 刷新该Span所在区域的绘制
     */
    void refreshTextView() {
        mTextView.invalidate(mTempRect);
    }

}
