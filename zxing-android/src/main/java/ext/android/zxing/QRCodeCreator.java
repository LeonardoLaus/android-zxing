package ext.android.zxing;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Map;

public final class QRCodeCreator {
    private static final int QR_CODE_SIZE = 500;

    private QRCodeCreator() {
        throw new AssertionError("no instance.");
    }

    @CheckResult
    public static Bitmap createQRCode(String contents) {
        return createQRCode(contents, QR_CODE_SIZE);
    }

    @CheckResult
    public static Bitmap createQRCode(String contents, int size) {
        try {
            Map<EncodeHintType, String> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            BitMatrix bitMatrix = new QRCodeWriter().encode(contents, BarcodeFormat.QR_CODE, size, size, hints);
            int[] pixels = new int[size * size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * size + x] = 0xff000000;
                    } else {
                        pixels[y * size + x] = 0xffffffff;
                    }
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size);
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

    @CheckResult
    public static Bitmap createQRCode(String contents, @NonNull Bitmap centerBitmap) {
        return createQRCode(contents, QR_CODE_SIZE, centerBitmap);
    }

    @CheckResult
    public static Bitmap createQRCode(String contents, int size, @NonNull Bitmap centerBitmap) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            /*
             * 设置容错级别，默认为ErrorCorrectionLevel.L
             * 因为中间加入logo所以建议你把容错级别调至H,否则可能会出现识别不了
             */
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

            BitMatrix bitMatrix = new QRCodeWriter().encode(contents, BarcodeFormat.QR_CODE, size, size, hints);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            int halfW = width / 2;
            int halfH = height / 2;
            final int halfImageSize = size / 10;
            Matrix m = new Matrix();
            float sx = 2 * halfImageSize / (float) centerBitmap.getWidth();
            float sy = 2 * halfImageSize / (float) centerBitmap.getHeight();
            m.setScale(sx, sy);
            centerBitmap = Bitmap.createBitmap(centerBitmap, 0, 0,
                    centerBitmap.getWidth(), centerBitmap.getHeight(), m, false);

            int[] pixels = new int[size * size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    if (x > halfW - halfImageSize
                            && x < halfW + halfImageSize
                            && y > halfH - halfImageSize
                            && y < halfH + halfImageSize) {
                        //该位置用于存放图片信息
                        //记录图片每个像素信息
                        pixels[y * width + x] = centerBitmap.getPixel(x - halfW + halfImageSize, y - halfH + halfImageSize);
                    } else {
                        if (bitMatrix.get(x, y)) {
                            pixels[y * size + x] = 0xff000000;
                        } else {
                            pixels[y * size + x] = 0xffffffff;
                        }
                    }
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size);
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }
}
