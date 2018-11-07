package ext.android.zxing;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;

public final class Decoder {

    private final HandlerThread mDecodeThread;
    private final Handler mHandler;
    private final CameraManager mCameraManager;

    public Decoder(@NonNull Context context) {
        mDecodeThread = new HandlerThread("decode QR code");
        mDecodeThread.start();
        mHandler = new DecodeHandler(mDecodeThread.getLooper());
        mCameraManager = new CameraManager(context);
    }

    private static class DecodeHandler extends Handler {
        DecodeHandler(Looper looper) {
            super(looper);
        }
    }
}
