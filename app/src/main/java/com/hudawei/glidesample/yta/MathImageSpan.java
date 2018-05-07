package com.hudawei.glidesample.yta;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by hudawei on 2018/5/2.
 * 嵌入在文本中的图片Span
 * 文本竖直居中对齐图片
 */

public class MathImageSpan extends YtaImageSpan {

    public MathImageSpan(YtaTextView textView, String url, int width, int height) {
        super(textView, url, width, height);
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm) {
        int size;
        //通过设置fm中的参数来设置图片所占高度大小
        if (fm != null) {
            int d = (mHeight - (fm.descent - fm.ascent)) / 2;
            fm.ascent -= d;
            fm.descent += d;

            fm.top -= d;
            fm.bottom += d;
        }

        //设置图片所占宽度
        size = mWidth;
        return size;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        mTempRect.set((int) x, top, (int) (x + mWidth), top + mHeight);
        Bitmap b = getCachedBitmap();
        if (b == null) {
            int oldColor = paint.getColor();
            paint.setColor(mBackgroundColor);
            canvas.drawRect(mTempRect, paint);
            paint.setColor(oldColor);
        } else {
            canvas.save();
            int transY = top + (mHeight - b.getHeight()) / 2;
            int transX = (int) (x + (mWidth - b.getWidth()) / 2);
            canvas.translate(transX, transY);
            canvas.drawBitmap(b, 0, 0, null);
            canvas.restore();
        }
    }


}
