package ext.android.zxing.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import ext.android.zxing.utils.Utils;

public class ViewfinderView extends View {
    private static final String TAG = "ViewfinderView";
    private final Paint mPaint;
    private final Paint mBoardPaint;
    private final Paint mTextPaint;
    @ColorInt
    private int mMaskColor = 0x60000000;
    @ColorInt
    private int mScannerColor;
    @ColorInt
    private int mCornerColor;

    private Rect mFrameRect;

    public ViewfinderView(Context context) {
        this(context, null);
    }

    public ViewfinderView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewfinderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(mMaskColor);
        mBoardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBoardPaint.setColor(Color.WHITE);
        mBoardPaint.setStyle(Paint.Style.STROKE);
        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(Color.WHITE);
    }

    public void setFrameRect(Rect frame) {
        this.mFrameRect = frame;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mFrameRect == null || getVisibility() != VISIBLE) {
            Log.w(TAG, "not ready yet, early draw before done configuring");
            return;
        }
        drawMask(canvas, mFrameRect);
        drawBorder(canvas, mFrameRect);
        drawCorner(canvas, mFrameRect);
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

        mBoardPaint.setStrokeWidth(strokeWidth);
        canvas.drawPath(path, mBoardPaint);
    }
}
