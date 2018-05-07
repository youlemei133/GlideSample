package com.hudawei.glidesample.yta;

/**
 * Created by hudawei on 2018/5/2.
 * 图文混排实例
 */

public class RichTextBean {
    /**
     * 样式
     * 0文本 1图片 2数学公式
     */
    public int type;
    /**
     * 内容
     * 为图片或数学公式时表示图片地址
     */
    public String content;
    /**
     * 内容为文本时，代表粗体
     * updateDrawState
     * paint.setFakeBoldText(true);
     */
    public boolean bold;
    /**
     * 内容为文本时，代表下划线
     * updateDrawState
     * ds.setUnderlineText(true);
     */
    public boolean underline;
    /**
     * 代表文本颜色
     * updateDrawState
     * ds.setColor(mColor);
     */
    public String color;

    /**
     * 图片宽度
     */
    public int width;
    /**
     * 图片高度
     */
    public int height;

}
