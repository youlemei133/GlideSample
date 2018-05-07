package com.hudawei.glidesample;

import android.graphics.BlurMaskFilter;
import android.graphics.MaskFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.MaskFilterSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.hudawei.glidesample.yta.LineImageSpan;
import com.hudawei.glidesample.yta.MathImageSpan;
import com.hudawei.glidesample.yta.YtaImageSpan;
import com.hudawei.glidesample.yta.YtaTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hudawei on 2018/4/27.
 * 使用ViewPager显示YtaTextView的Fragment
 */

public class ItemFragment extends Fragment {
    private View mRootView;
    private FrameLayout fl_loading;
    private FrameLayout fl_content;
    private List<YtaTextView> mTextViews;
    private int mPosition;

    public static ItemFragment newInstance(int position) {
        Bundle args = new Bundle();
        args.putInt("position", position);
        ItemFragment fragment = new ItemFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mPosition = getArguments().getInt("position");
        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.fragment_item, container, false);
            fl_content = mRootView.findViewById(R.id.fl_content);
            fl_loading = mRootView.findViewById(R.id.fl_loading);
            fl_content.setVisibility(View.GONE);
            fl_loading.setVisibility(View.VISIBLE);
            mTextViews = new ArrayList<>();
            //获取mRootView下所有的YtaTextView
            getYtaTextViews(mTextViews, (ViewGroup) mRootView);
        }
        return mRootView;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState == null)
            setTextViews();
    }

    /**
     * 取消YtaImageSpan中加载图片
     */
    public void clearLoadLineImages() {
        if (checkTextViewsEmpty())
            return;
        for (int i = 0; i < mTextViews.size(); i++) {
            YtaTextView textView = mTextViews.get(i);
            YtaImageSpan[] spans = textView.getYtaImageSpans();
            if (spans != null) {
                for (YtaImageSpan span : spans) {
                    span.clearRequest();
                }
            }
        }
    }

    /**
     * 调用YtaImageSpan中的加载图片操作
     */
    public void loadLineImages() {
        if (checkTextViewsEmpty())
            return;
        for (int i = 0; i < mTextViews.size(); i++) {
            YtaTextView textView = mTextViews.get(i);
            YtaImageSpan[] spans = textView.getYtaImageSpans();
            if (spans != null) {
                for (YtaImageSpan span : spans) {
                    span.requestBitmap();
                }
            }
        }
    }

    /**
     * 获取父View下面所有的YtaTextView
     *
     * @param results  一个size为0的集合
     * @param rootView 父View
     */
    private void getYtaTextViews(@NonNull List<YtaTextView> results, @NonNull ViewGroup rootView) {
        int count = rootView.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = rootView.getChildAt(i);
            if (view instanceof YtaTextView) {
                results.add((YtaTextView) view);
            } else if (view instanceof ViewGroup) {
                getYtaTextViews(results, (ViewGroup) view);
            }
        }
    }

    public void setTextViews() {
        if (checkTextViewsEmpty())
            return;
        for (int i = 0; i < mTextViews.size(); i++) {
            YtaTextView textView = mTextViews.get(i);
            setText(textView);
        }

        fl_content.setVisibility(View.VISIBLE);
        fl_loading.setVisibility(View.GONE);
    }

    private boolean checkTextViewsEmpty() {
        return mTextViews == null;
    }

    private void setText(YtaTextView textView) {
        String url = "https://tva1.sinaimg.cn/crop.0.467.2560.1440/90eb2137ly1fkeeqap1xfj21z41hcn7o.jpg";
        String str1 = mPosition + "哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈\n";
        String str2 = "<math src = '' width = '' height = ''>\n";
        String str3 = "笑嘻嘻笑嘻嘻笑嘻嘻笑嘻嘻笑嘻嘻笑嘻嘻笑嘻嘻笑嘻嘻笑嘻嘻";
        String str4 = "<math src = '' width = '' height = ''>";
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(str1).append(str2).append(str3);
        builder.append(str1).append(str2).append(str3);
        builder.append(str4).append(str3);

        YtaImageSpan span = new LineImageSpan(textView, url, 1002, 564);
        YtaImageSpan span2 = new LineImageSpan(textView, url, 1002, 564);
        YtaImageSpan span3 = new MathImageSpan(textView, url, /*119*/200, 88);

        builder.setSpan(span, str1.length(), str1.length() + str2.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(span2, 2 * str1.length() + str2.length() + str3.length(), 2 * str1.length() + 2 * str2.length() + str3.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(span3, 2 * str1.length() + 2 * str2.length() + 2 * str3.length(), 2 * str1.length() + 3 * str2.length() + 2 * str3.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

//        RichTextBean textBean = new RichTextBean();
//        textBean.type = 0;
//        textBean.bold = true;
//        textBean.underline = true;
//        textBean.color = "#FF444444";
//        YtaStyleSpan styleSpan = new YtaStyleSpan(textBean);
//        builder.setSpan(styleSpan, str1.length() + str2.length(), str1.length() + str2.length() + str3.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        //上标
//        SuperscriptSpan superscriptSpan = new SuperscriptSpan();
//        builder.setSpan(superscriptSpan, str1.length() + str2.length(), str1.length() + str2.length() + str3.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        //下标
//        SubscriptSpan subscriptSpan = new SubscriptSpan();
//        builder.setSpan(subscriptSpan, str1.length() + str2.length(), str1.length() + str2.length() + str3.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        //设置文本字体大小，相对于正常大小的2.5倍
//        RelativeSizeSpan relativeSizeSpan = new RelativeSizeSpan(2.5f);
//        builder.setSpan(relativeSizeSpan, str1.length() + str2.length(), str1.length() + str2.length() + str3.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        //指定文本字体大小
//        AbsoluteSizeSpan absoluteSizeSpan = new AbsoluteSizeSpan(30);
//        builder.setSpan(absoluteSizeSpan, str1.length() + str2.length(), str1.length() + str2.length() + str3.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        //字体水平方向拉伸收缩
//        ScaleXSpan scaleXSpan = new ScaleXSpan(0.5f);
//        builder.setSpan(scaleXSpan, str1.length() + str2.length(), str1.length() + str2.length() + str3.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        //链接
//        URLSpan urlSpan = new URLSpan("http://www.baidu.com");
//        builder.setSpan(urlSpan, str1.length() + str2.length(), str1.length() + str2.length() + str3.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        //背景色
//        BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(Color.GREEN);
//        builder.setSpan(backgroundColorSpan, str1.length() + str2.length(), str1.length() + str2.length() + str3.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        //前景色
//        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.GREEN);
//        builder.setSpan(foregroundColorSpan, str1.length() + str2.length(), str1.length() + str2.length() + str3.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        //模糊特效
        MaskFilter blurMaskFilter = new BlurMaskFilter(10, BlurMaskFilter.Blur.NORMAL);
        MaskFilterSpan maskFilterSpan = new MaskFilterSpan(blurMaskFilter);
        builder.setSpan(maskFilterSpan, str1.length() + str2.length(), str1.length() + str2.length() + str3.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        textView.setText(builder);
    }

}
