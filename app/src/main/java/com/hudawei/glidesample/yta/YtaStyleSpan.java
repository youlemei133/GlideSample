package com.hudawei.glidesample.yta;

import android.graphics.Color;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.CharacterStyle;

/**
 * Created by hudawei on 2018/5/2.
 * 设置字体颜色、粗体、下划线
 */

public class YtaStyleSpan extends CharacterStyle {
    private boolean bold;
    private boolean underline;
    private String color;

    public YtaStyleSpan(RichTextBean bean) {
        bold = bean.bold;
        underline = bean.underline;
        color = bean.color;
    }


    @Override
    public void updateDrawState(TextPaint tp) {
        tp.setFakeBoldText(bold);
        tp.setUnderlineText(underline);
        if (!TextUtils.isEmpty(color))
            tp.setColor(Color.parseColor(color));
        tp.setStrikeThruText(true);
    }

}
