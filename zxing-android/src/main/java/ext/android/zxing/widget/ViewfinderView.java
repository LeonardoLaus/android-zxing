package ext.android.zxing.widget;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import ext.android.zxing.R;
import ext.android.zxing.utils.Utils;

public class ViewfinderView extends View {
    private static final String TAG = "ViewfinderView";
    private static final float RATIO_SCALED = 0.9f;
    private final Paint mPaint;
    private final Paint mBoardPaint;
    private final Paint mTextPaint;
    @ColorInt
    private int mMaskColor = 0x60000000;
    @ColorInt
    private int mScannerColor = Color.WHITE;
    private long mScannerDuration = 1500;
    @ColorInt
    private int mCornerColor = Color.WHITE;
    @ColorInt
    private int mBoardColor = Color.WHITE;
    @ColorInt
    private int mTextColor = Color.WHITE;
    private float mTextSize = 24;
    private String mText;

    private Rect mFrameRect;
    private Rect mScannerRect;
    private ValueAnimator mAnimator;

    public ViewfinderView(Context context) {
        this(context, null);
    }

    public ViewfinderView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewfinderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(mMaskColor);
        mBoardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBoardPaint.setStyle(Paint.Style.STROKE);
        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mScannerRect = new Rect();
        if (TextUtils.isEmpty(mText)) {
            mText = getResources().getString(R.string.zxing_default_qrcode_scan);
        }
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ViewfinderView);
        if (typedArray != null) {
            mMaskColor = typedArray.getColor(R.styleable.ViewfinderView_maskColor, mMaskColor);
            mCornerColor = typedArray.getColor(R.styleable.ViewfinderView_cornerColor, mCornerColor);
            mBoardColor = typedArray.getColor(R.styleable.ViewfinderView_boardColor, mBoardColor);
            mScannerColor = typedArray.getColor(R.styleable.ViewfinderView_scannerColor, mScannerColor);
            mScannerDuration = typedArray.getInteger(R.styleable.ViewfinderView_scannerDuration, (int) mScannerDuration);
            mTextColor = typedArray.getColor(R.styleable.ViewfinderView_android_textColor, mTextColor);
            mTextSize = typedArray.getDimension(R.styleable.ViewfinderView_android_textSize, mTextSize);
            mText = typedArray.getString(R.styleable.ViewfinderView_android_text);
            typedArray.recycle();
        }
    }

    public void setFrameRect(Rect frame) {
        this.mFrameRect = frame;
        if (mFrameRect != null) {
            float scannerWidth = mFrameRect.width() * RATIO_SCALED;
            float widthOffset = (mFrameRect.width() - scannerWidth) / 2;
            mScannerRect.left = (int) (mFrameRect.left + widthOffset);
            mScannerRect.right = (int) (mFrameRect.right - widthOffset);
            if (mAnimator != null) {
                mAnimator.start();
            }
        } else if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        startScannerAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
    }

    private void startScannerAnimation() {
        mAnimator = ValueAnimator.ofFloat(0, 1);
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.setRepeatMode(ValueAnimator.REVERSE);
        mAnimator.setInterpolator(new CosineInterpolator());
        mAnimator.setDuration(mScannerDuration);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float ratio = (float) valueAnimator.getAnimatedValue();
                if (mFrameRect == null) {
                    return;
                }
                float totalHeight = mFrameRect.height() * RATIO_SCALED;
                int offset = (int) ((mFrameRect.height() - totalHeight) / 2);
                int position = (int) (totalHeight * ratio);
                mScannerRect.top = mFrameRect.top + offset + position;
                mScannerRect.bottom = mFrameRect.top + offset + position + 2;
                postInvalidate();
            }
        });
        if (mFrameRect != null) {
            mAnimator.start();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mFrameRect == null || getVisibility() != VISIBLE) {
            Log.w(TAG, "not ready yet, early draw before done configuring");
            return;
        }
        drawMask(canvas, mFrameRect);
        mBoardPaint.setStyle(Paint.Style.STROKE);
        drawBorder(canvas, mFrameRect);
        drawCorner(canvas, mFrameRect);
        drawScanner(canvas, mScannerRect);
        drawText(canvas);
    }

    /**
     * transparent mask
     *
     * @param canvas
     * @param rect
     */
    private void drawMask(Canvas canvas, Rect rect) {
        final int width = canvas.getWidth();
        final int height = canvas.getHeight();
        canvas.drawRect(0, 0, width, rect.top, mPaint);
        canvas.drawRect(0, rect.top, rect.left, rect.bottom + 1, mPaint);
        canvas.drawRect(rect.right + 1, rect.top, width, rect.bottom + 1, mPaint);
        canvas.drawRect(0, rect.bottom + 1, width, height, mPaint);
    }

    /**
     * draw frame
     *
     * @param canvas
     * @param frame
     */
    private void drawBorder(Canvas canvas, Rect frame) {
        mBoardPaint.setColor(mBoardColor);
        mBoardPaint.setStrokeWidth(1);
        canvas.drawRect(frame.left, frame.top, frame.right, frame.bottom, mBoardPaint);
    }

    /**
     * Draw Corner
     *
     * @param canvas
     * @param frame
     */
    private void drawCorner(Canvas canvas, Rect frame) {
        final int lineLength = (int) Utils.dp2px(getContext(), 20);
        final float strokeWidth = Utils.dp2px(getContext(), 3);
        Path path = new Path();
        //Left-Top
        path.moveTo(frame.left + lineLength, frame.top);
        path.lineTo(frame.left, frame.top);
        path.lineTo(frame.left, frame.top + lineLength);
        // Right-Top
        path.moveTo(frame.right - lineLength, frame.top);
        path.lineTo(frame.right, frame.top);
        path.lineTo(frame.right, frame.top + lineLength);
        //Right-Bottom
        path.moveTo(frame.right, frame.bottom - lineLength);
        path.lineTo(frame.right, frame.bottom);
        path.lineTo(frame.right - lineLength, frame.bottom);
        //Left-Bottom
        path.moveTo(frame.left + lineLength, frame.bottom);
        path.lineTo(frame.left, frame.bottom);
        path.lineTo(frame.left, frame.bottom - lineLength);

        mBoardPaint.setColor(mCornerColor);
        mBoardPaint.setStrokeWidth(strokeWidth);
        canvas.drawPath(path, mBoardPaint);
    }

    private void drawScanner(Canvas canvas, Rect rect) {
        mBoardPaint.setColor(mScannerColor);
        mBoardPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(rect, mBoardPaint);
    }

    private void drawText(Canvas canvas) {
        float x = (mFrameRect.left + mFrameRect.right) / 2;
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        float textHeight = fontMetrics.bottom - fontMetrics.top;
        float baseline = 10 + mFrameRect.bottom + textHeight - fontMetrics.bottom;
        canvas.drawText(mText, x, baseline, mTextPaint);
    }

    private static class CosineInterpolator implements TimeInterpolator {

        @Override
        public float getInterpolation(float v) {
            return (float) (Math.cos(2 * Math.PI * v) + 1) / 2;
        }
    }
}
