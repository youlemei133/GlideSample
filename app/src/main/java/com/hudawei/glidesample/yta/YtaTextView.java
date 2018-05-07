package com.hudawei.glidesample.yta;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.hudawei.glidesample.R;

import java.lang.ref.WeakReference;

/**
 * Created by hudawei on 2018/4/24.
 * 重复设置setText()有问题
 */

public class YtaTextView extends AppCompatTextView implements View.OnClickListener, OnYtaImageClickListener {
    /**
     * 点击x
     */
    private int mPointX;
    /**
     * 点击y
     */
    private int mPointY;
    /**
     * 加载中图片资源
     */
    private static int mLoadingResource;
    /**
     * 加载中图片引用
     */
    private static WeakReference<Bitmap> mLoadingBitmapRef;
    /**
     * 图片点击回调
     */
    private OnYtaImageClickListener mImgListener;
    /**
     * 缓存该TextView中所有的YtaImageSpan
     */
    private SpannableStringBuilder mBuilder;

    public YtaTextView(Context context) {
        super(context);
    }

    /**
     * 设置点击事件
     * 设置YtaImageSpan所在矩形区域点击事件
     * 设置图片加载占位资源
     *
     * @param context Context
     * @param attrs   AttributeSet
     */
    public YtaTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
        setOnImageClickListener(this);
        mLoadingResource = R.mipmap.pic_pencil_default;
        setLoadingResource(mLoadingResource, false);
    }

    /**
     * 设置图片点击函数
     *
     * @param listener 回调函数
     */
    public void setOnImageClickListener(OnYtaImageClickListener listener) {
        mImgListener = listener;
    }

    /**
     * 设置加载中的资源
     *
     * @param loadingResource 加载中的资源
     * @param force           强制重新创建弱引用
     */
    public void setLoadingResource(int loadingResource, boolean force) {
        if (mLoadingResource != loadingResource || force) {
            mLoadingResource = loadingResource;
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), mLoadingResource);
            mLoadingBitmapRef = new WeakReference<>(bitmap);
        }
    }

    /**
     * 获取所有的YtaImageSpan
     *
     * @return YtaImageSpan数组，有可能为null
     */
    public YtaImageSpan[] getYtaImageSpans() {
        return getYtaImageSpans(mBuilder);
    }

    /**
     * 获取所有的YtaImageSpan，在setText的时候调用该方法
     *
     * @return YtaImageSpan数组，有可能为null
     */
    private YtaImageSpan[] getYtaImageSpans(SpannableStringBuilder builder) {
        CharSequence mText = getText();
        YtaImageSpan[] spans = null;
        if (builder != null && mText != null) {
            spans = builder.getSpans(0, mText.length(), YtaImageSpan.class);
        }
        return spans;
    }


    /**
     * 获取加载中的Bitmap软引用
     *
     * @return 加载中的Bitmap软引用
     */
    WeakReference<Bitmap> getLoadingBitmapRef() {
        if (getRefBitmap(mLoadingBitmapRef) == null)
            setLoadingResource(mLoadingResource, true);
        return mLoadingBitmapRef;
    }

    /**
     * 获取软引用中的Bitmap
     *
     * @param bitmapRef Bitmap软引用
     * @return 如果该Bitmap不可用，返回null
     */
    private Bitmap getRefBitmap(WeakReference<Bitmap> bitmapRef) {
        if (bitmapRef != null) {
            Bitmap b = bitmapRef.get();
            if (b != null && !b.isRecycled())
                return b;
        }
        return null;
    }

    /**
     * 重写onTouchEvent，记录点击的坐标点，用于计算点击是否在图片上
     *
     * @param event MotionEvent
     * @return boolean
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mPointX = (int) event.getX();
        mPointY = (int) event.getY();
        return super.onTouchEvent(event);
    }

    /**
     * 当调用setText(SpannableStringBuilder)时
     * 拦截setText方法，用于获取SpannableStringBuilder
     *
     * @param text CharSequence
     * @param type BufferType
     */
    @Override
    public void setText(CharSequence text, BufferType type) {
        if (BufferType.NORMAL == type && text instanceof SpannableStringBuilder) {
            mBuilder = (SpannableStringBuilder) text;
        }
        super.setText(text, type);
    }

    /**
     * 如果点击在图片上，则回调OnImageClickListener的onImageClick方法
     *
     * @param v View
     */
    @Override
    public void onClick(View v) {
        if (mBuilder != null) {
            YtaImageSpan[] mYtaImageSpans = getYtaImageSpans();
            for (YtaImageSpan span : mYtaImageSpans) {
                if (span != null) {
                    Rect rect = span.getRect();
                    if (rect.contains(mPointX, mPointY) && mImgListener != null) {
                        mImgListener.onImageClick(span);
                    }
                }
            }
        }
    }


    /**
     * 点击ImageSpan矩形区域回调方法
     *
     * @param span YtaImageSpan
     */
    @Override
    public void onImageClick(YtaImageSpan span) {
        switch (span.getState()) {
            case YtaImageSpan.LOADING_STATE:
                Toast.makeText(getContext(), "加载中 :" + span.getUrl(), Toast.LENGTH_SHORT).show();
                break;
            case YtaImageSpan.LOAD_ERROR_STATE:
                Toast.makeText(getContext(), "加载失败 :" + span.getUrl(), Toast.LENGTH_SHORT).show();
                span.requestBitmap();
                break;
            case YtaImageSpan.LOADED_STATE:
                Toast.makeText(getContext(), "已加载 :" + span.getUrl(), Toast.LENGTH_SHORT).show();
                break;
        }
    }


}
