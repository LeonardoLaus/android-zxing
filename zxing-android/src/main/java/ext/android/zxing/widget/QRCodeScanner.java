package ext.android.zxing.widget;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.FrameLayout;

public class QRCodeScanner extends FrameLayout {

    private CheckBox torchView;
    private ViewfinderView viewfinderView;
    private Rect frameRect;

    public QRCodeScanner(@NonNull Context context) {
        this(context, null);
    }

    public QRCodeScanner(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QRCodeScanner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        viewfinderView = new ViewfinderView(context, attrs, defStyleAttr);
        addView(viewfinderView, 0, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        torchView = new CheckBox(context, attrs, defStyleAttr);
        addView(torchView, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    public void setFrameRect(Rect frame) {
        this.frameRect = frame;
        viewfinderView.setFrameRect(frame);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        viewfinderView.onFinishInflate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (frameRect != null) {
            int torchWidth = torchView.getWidth();
            int torchHeight = torchView.getHeight();
            int torchX = (frameRect.left + frameRect.right) / 2;
            int torchY = frameRect.bottom - torchHeight / 2 - 10;
            torchView.layout(torchX - torchWidth / 2, torchY - torchHeight / 2,
                    torchX + torchWidth / 2, torchY + torchHeight / 2);
        }
    }

    public void setOnTorchClickListener(OnClickListener listener) {
        torchView.setOnClickListener(listener);
    }
}
