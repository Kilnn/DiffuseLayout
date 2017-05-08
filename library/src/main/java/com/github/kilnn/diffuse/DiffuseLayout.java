package com.github.kilnn.diffuse;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Kilnn on 2017/3/20.
 * 参考：http://blog.csdn.net/airsaid/article/details/52683193
 */

public class DiffuseLayout extends LinearLayout {
    private int mCenterColor;//圆圈中心颜色
    private int mCenterRadius;//中心圆宽度（直径）

    private int mDiffuseColor;//扩散圆圈颜色
    private int mDiffuseWidth = 10;//扩散圆宽度 10px
    private int mDiffuseCount = 3;//扩散圆的最大数量，默认为3个
    private int mDiffuseStartAlpha = 255;//开始扩散Alpha值
    private int mDiffuseEndAlpha = 0;//结束扩散Alpha值
    private float mDiffuseEveryStep = 1;//每次扩散增加的像素，决定扩散的速度
    private int mDiffuseEveryTime = 0;//每次扩散的时间，决定扩散的速度

    private List<Float> mDiffuseAlphas = new ArrayList<>();//透明度集合
    private List<Float> mDiffuseWidths = new ArrayList<>();//扩散圆半径集合

    private boolean mIsDiffuse = true;//是否在做扩展动画
    private Paint mPaint;

    private int mDiffuseDelay;//下次扩散延迟时间,毫秒数，只有当diffuse_count==1时，才有效

    public DiffuseLayout(Context context) {
        super(context);
        init(context, null, 0);
    }

    public DiffuseLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public DiffuseLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DiffuseLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        Drawable bg = getBackground();
        if (bg != null && bg instanceof ColorDrawable) {
            mCenterColor = ((ColorDrawable) bg).getColor();
            setBackgroundDrawable(null);
        }
        mDiffuseColor = mCenterColor;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DiffuseLayout, defStyleAttr, 0);
            mDiffuseColor = a.getColor(R.styleable.DiffuseLayout_diffuse_color, mCenterColor);
            mDiffuseWidth = a.getDimensionPixelSize(R.styleable.DiffuseLayout_diffuse_width, mDiffuseWidth);
            mDiffuseCount = a.getInt(R.styleable.DiffuseLayout_diffuse_count, mDiffuseCount);

            mDiffuseStartAlpha = a.getInt(R.styleable.DiffuseLayout_diffuse_start_alpha, mDiffuseStartAlpha);
            mDiffuseEndAlpha = a.getInt(R.styleable.DiffuseLayout_diffuse_end_alpha, mDiffuseEndAlpha);
            mDiffuseEveryStep = a.getDimension(R.styleable.DiffuseLayout_diffuse_every_step, mDiffuseEveryStep);
            mDiffuseEveryTime = a.getInt(R.styleable.DiffuseLayout_diffuse_every_time, mDiffuseEveryTime);

            mDiffuseDelay = a.getInt(R.styleable.DiffuseLayout_diffuse_delay, mDiffuseDelay);

            if (mDiffuseCount <= 0) {
                mDiffuseCount = 1;
            }
            if (mDiffuseStartAlpha > 255) {
                mDiffuseStartAlpha = 255;
            }
            if (mDiffuseEndAlpha < 0) {
                mDiffuseEndAlpha = 0;
            }
            if (mDiffuseStartAlpha < mDiffuseEndAlpha) {
                mDiffuseStartAlpha = mDiffuseEndAlpha;
            }
            if (mDiffuseEveryStep > mDiffuseWidth) {
                mDiffuseEveryStep = mDiffuseWidth;
            }
            a.recycle();
        }

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mDiffuseAlphas.add((float) mDiffuseStartAlpha);
        mDiffuseWidths.add(0.0f);

        setWillNotDraw(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int measureSize = Math.max(getMeasuredHeight(), getMeasuredWidth());
        int size = measureSize + mDiffuseWidth * 2 * mDiffuseCount;
        mCenterRadius = measureSize / 2;
        setMeasuredDimension(size, size);

        if (mDiffuseEveryStep <= 0) {
            mDiffuseEveryStep = 1;
        }
        float stepCount = mDiffuseWidth * mDiffuseCount / mDiffuseEveryStep;
        mAlphaStep = (mDiffuseStartAlpha - mDiffuseEndAlpha) / stepCount;
        if (mAlphaStep <= 0) {
            mAlphaStep = 1;
        }
    }

    private float mAlphaStep;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 绘制扩散圆
        mPaint.setColor(mDiffuseColor);

        for (int i = 0; i < mDiffuseAlphas.size(); i++) {
            // 设置透明度
            Float alpha = mDiffuseAlphas.get(i);
            mPaint.setAlpha(alpha.intValue());
            // 绘制扩散圆
            Float width = mDiffuseWidths.get(i);
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, mCenterRadius + width, mPaint);

            if (alpha > 0 && mCenterRadius + width < getWidth() / 2) {
                mDiffuseAlphas.set(i, alpha - mAlphaStep);
                mDiffuseWidths.set(i, width + mDiffuseEveryStep);
            }
        }
        // 判断当扩散圆扩散到指定宽度时添加新扩散圆
        if (mDiffuseWidths.get(mDiffuseWidths.size() - 1) >= mDiffuseWidth) {
            mDiffuseAlphas.add((float) mDiffuseStartAlpha);
            mDiffuseWidths.add(0.0f);
        }

        boolean delay = false;
        // 超过mDiffuseCount个扩散圆，删除最外层
        if (mDiffuseWidths.size() > mDiffuseCount) {
            mDiffuseAlphas.remove(0);
            mDiffuseWidths.remove(0);
            if (mDiffuseCount == 1 && mDiffuseDelay > 0) delay = true;
        }

        // 绘制中心圆及图片
        mPaint.setAlpha(255);
        mPaint.setColor(mCenterColor);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, mCenterRadius, mPaint);

        if (mIsDiffuse) {
            if (delay) {
                postInvalidateDelayed(mDiffuseDelay);
            } else {
                if (mDiffuseEveryTime > 0) {
                    postInvalidateDelayed(mDiffuseEveryTime);
                } else {
                    postInvalidate();
                }
            }
        }
    }


    /**
     * 开始扩散
     */
    public void start() {
        mIsDiffuse = true;
        invalidate();
    }

    /**
     * 停止扩散
     */
    public void stop() {
        mIsDiffuse = false;
    }

    /**
     * 是否扩散中
     */
    public boolean isDiffuse() {
        return mIsDiffuse;
    }
}
