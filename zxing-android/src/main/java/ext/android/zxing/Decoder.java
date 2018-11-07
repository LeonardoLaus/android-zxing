package ext.android.zxing;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public class Decoder {

    private HandlerThread mDecodeThread;
    private Handler mHandler;

    public Decoder() {
        mDecodeThread = new HandlerThread("decode QR code");
        mDecodeThread.start();
        mHandler = new DecodeHandler(mDecodeThread.getLooper());
    }

    private class DecodeHandler extends Handler {

        DecodeHandler(Looper looper) {
            super(looper);
        }
    }
}
