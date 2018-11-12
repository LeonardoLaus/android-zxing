package ext.android.zxing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import ext.android.zxing.utils.Utils;

public final class Decoder {
    private static final String TAG = "Decoder";

    private static final int MIN_FRAME_WIDTH = 240;
    private static final int MIN_FRAME_HEIGHT = 240;
    private static final int MAX_FRAME_WIDTH = 960; // = 5/8 * 1920
    private static final int MAX_FRAME_HEIGHT = 960; // = 5/8 * 1080

    private static final int MAX_RETRY_COUNT = 10;

    private final Context mContext;
    private final HandlerThread mDecodeThread;
    private final DecodeHandler mDecodeHandler;
    private final Handler mMainHandler;
    private final CameraManager mCameraManager;
    private final MultiFormatReader mMultiFormatReader;

    private boolean mWholeScene;
    private DecodeCallback mDecodeCallback;

    public Decoder(@NonNull Context context, DecodeCallback decodeCallback) {
        mContext = context;
        mDecodeThread = new HandlerThread("decode QR code");
        mDecodeThread.start();
        mCameraManager = new CameraManager(context);
        mMultiFormatReader = new MultiFormatReader();
        initFormatReader();
        mDecodeHandler = new DecodeHandler(mDecodeThread.getLooper());
        mMainHandler = new Handler(Looper.getMainLooper());
        this.mDecodeCallback = decodeCallback;
    }

    private static byte[] rotateIfNeed(byte[] data, int width, int height) {
        if (width < height) {
            long start = System.currentTimeMillis();
            byte[] rotatedData = new byte[data.length];
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    rotatedData[y * width + width - x - 1] = data[y + x * height];
                }
            }
            Log.e(TAG, "rotate time=" + (System.currentTimeMillis() - start) + "ms");
            return rotatedData;
        } else {
            return data;
        }
    }

    private static byte[] rotate90IfPortrait(byte[] data, int width, int height) {
        return width < height ? rotateYUV420Degree90(data, width, height) : data;
    }

    private static byte[] rotateYUV420Degree90(byte[] data, int width, int height) {
        long start = System.currentTimeMillis();
        int dWidth = height;
        int dHeight = width;
        int yLuminanceLength = dWidth * dHeight;
        int length = yLuminanceLength * 3 / 2;
        byte[] yuv = new byte[length];
        // Rotate the Y luma
        int i = 0;
        for (int x = 0; x < dWidth; x++) {
            for (int y = dHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * dWidth + x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = length - 1;
        for (int x = dWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < dHeight / 2; y++) {
                yuv[i] = data[yLuminanceLength + (y * dWidth) + x];
                i--;
                yuv[i] = data[yLuminanceLength + (y * dWidth) + (x - 1)];
                i--;
            }
        }
        Log.e(TAG, "rotateYUV420Degree90 spend time=" + (System.currentTimeMillis() - start) + "ms");
        return yuv;
    }

    private static int findDesiredDimensionInRange(int resolution, int hardMin, int hardMax) {
        int dim = resolution;
        if (dim < hardMin) {
            return hardMin;
        }
        if (dim > hardMax) {
            return hardMax;
        }
        return dim;
    }

    private static byte[] getBitmapByte(Bitmap bitmap, int quality) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
        return out.toByteArray();
    }

    private void initFormatReader() {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        Collection<BarcodeFormat> decodeFormats = new ArrayList<>();
        decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.PRODUCT_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.AZTEC_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.PDF417_FORMATS);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
        mMultiFormatReader.setHints(hints);
    }

    public void start(SurfaceTexture surfaceTexture) {
        try {
            mCameraManager.start(surfaceTexture);
            mCameraManager.requestPreviewFrame(mDecodeHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        mCameraManager.stop();
    }

    public void destroy() {
        mDecodeHandler.removeCallbacksAndMessages(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mDecodeThread.quitSafely();
        } else {
            mDecodeThread.quit();
        }
    }

    public boolean isTorchOn() {
        return mCameraManager.getTorchState();
    }

    public void setTorch(boolean on) {
        mCameraManager.setTorch(on);
    }

    public void decodeWholeScene(boolean wholeScene) {
        this.mWholeScene = wholeScene;
    }

    public void decodeBitmap(final Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        mDecodeHandler.obtainMessage(DecodeHandler.MSG_DECODE_BITMAP, bitmap).sendToTarget();
    }

    private PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = getFramingRectInPreview();
        if (rect == null) {
            return null;
        }
        Log.i(TAG, "Framing rect : " + rect);
        // Go ahead and assume it's YUV rather than die.
        if (mWholeScene) {
            return new PlanarYUVLuminanceSource(data, width, height,
                    0, 0, width, height, false);
        } else {
            return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
                    rect.width(), rect.height(), false);
        }
    }

    private Rect getFramingRectInPreview() {
        Rect framingRect = getFramingRect();
        Point cameraResolution = mCameraManager.getPreviewResolution();
        Point screenResolution = Utils.getScreenResolution(mContext);
        if (cameraResolution == null) {
            return null;
        }
        Rect rect = new Rect(framingRect);
        final float widthRatio;
        final float heightRatio;
        if (screenResolution.x > screenResolution.y) {
            widthRatio = cameraResolution.x / (float) screenResolution.x;
            heightRatio = cameraResolution.y / (float) screenResolution.y;
        } else {
            widthRatio = cameraResolution.y / (float) screenResolution.x;
            heightRatio = cameraResolution.x / (float) screenResolution.y;
        }
        rect.left = (int) (rect.left * widthRatio);
        rect.right = (int) (rect.right * widthRatio);
        rect.top = (int) (rect.top * heightRatio);
        rect.bottom = (int) (rect.bottom * heightRatio);
        return rect;
    }

    public Rect getFramingRect() {
        Point screenResolution = Utils.getScreenResolution(mContext);
        int w = findDesiredDimensionInRange(screenResolution.x, MIN_FRAME_WIDTH, MAX_FRAME_WIDTH);
        int h = findDesiredDimensionInRange(screenResolution.y, MIN_FRAME_HEIGHT, MAX_FRAME_HEIGHT);

        int length = (int) (Math.min(w, h) * 0.75f);
        int leftOffset = (screenResolution.x - length) / 2;
        int topOffset = (screenResolution.y - length) / 2;
        return new Rect(leftOffset, topOffset, leftOffset + length, topOffset + length);
    }

    private Result decodeLuminanceSource(LuminanceSource source) {
        Result rawResult = null;
        if (source != null) {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                rawResult = mMultiFormatReader.decodeWithState(bitmap);
            } catch (ReaderException re) {
                // continue
            } finally {
                mMultiFormatReader.reset();
            }
        }
        return rawResult;
    }

    private RGBLuminanceSource buildRGBLuminanceSource(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        return new RGBLuminanceSource(width, height, pixels);
    }

    private byte[] createThumbnail(PlanarYUVLuminanceSource source) {
        int[] pixels = source.renderThumbnail();
        int width = source.getThumbnailWidth();
        int height = source.getThumbnailHeight();
        Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
        return getBitmapByte(bitmap, 50);
    }

    private void notifyResult(final Result result, final byte[] barcode, final float scaledFactor) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mDecodeCallback != null) {
                    mDecodeCallback.onSuccess(result, barcode, scaledFactor);
                }
            }
        });
    }

    private void notifyFailure() {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mDecodeCallback != null) {
                    mDecodeCallback.onFailure();
                }
            }
        });
    }

    public interface DecodeCallback {
        void onSuccess(Result rawResult, byte[] barcode, float scaledFactor);

        void onFailure();
    }

    private class DecodeHandler extends Handler implements Camera.PreviewCallback {

        private static final int MSG_DECODE = 1;
        private static final int MSG_DECODE_BITMAP = 2;
        private static final int MSG_FAILED = 3;

        private int retry;

        DecodeHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_DECODE:
                    decode((byte[]) msg.obj, msg.arg1, msg.arg2);
                    break;
                case MSG_DECODE_BITMAP:
                    decodeBitmap((Bitmap) msg.obj);
                    break;
                case MSG_FAILED:
                    if (retry < MAX_RETRY_COUNT) {
                        mCameraManager.requestPreviewFrame(this);
                        retry++;
                    } else {
                        notifyFailure();
                    }
                    break;
            }
        }

        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            Point previewResolution = mCameraManager.getPreviewSizeOnScreen();
            if (previewResolution == null) {
                Log.d(TAG, "Got preview callback, but no handler or resolution available");
                return;
            }
            Message.obtain(this, MSG_DECODE, previewResolution.x, previewResolution.y, bytes).sendToTarget();
        }

        private void decodeBitmap(Bitmap bitmap) {
            long start = System.currentTimeMillis();
            RGBLuminanceSource source = buildRGBLuminanceSource(bitmap);
            Result rawResult = decodeLuminanceSource(source);
            if (rawResult != null) {
                // Don't log the barcode contents for security.
                long end = System.currentTimeMillis();
                Log.d(TAG, "Found barcode in " + (end - start) + " ms");
                final byte[] barcode = getBitmapByte(bitmap, 50);
                notifyResult(rawResult, barcode, 1);
                retry = 0;
            } else {
                Log.e(TAG, "Not Found barcode. Try Again!");
                sendEmptyMessage(MSG_FAILED);
            }
        }

        private void decode(byte[] data, int width, int height) {
//            byte[] realData = rotateIfNeed(data, width, height);
            byte[] realData = rotate90IfPortrait(data, width, height);

            long start = System.currentTimeMillis();

            PlanarYUVLuminanceSource source = buildLuminanceSource(realData, width, height);
            Result rawResult = decodeLuminanceSource(source);
            if (rawResult != null) {
                // Don't log the barcode contents for security.
                long end = System.currentTimeMillis();
                Log.d(TAG, "Found barcode in " + (end - start) + " ms");
                final byte[] barcode = createThumbnail(source);
                final float scaledFactor = (float) source.getThumbnailWidth() / source.getWidth();
                notifyResult(rawResult, barcode, scaledFactor);
                retry = 0;
            } else {
                Log.e(TAG, "Not Found barcode. Try Again!");
                sendEmptyMessage(MSG_FAILED);
            }
        }

    }
}
