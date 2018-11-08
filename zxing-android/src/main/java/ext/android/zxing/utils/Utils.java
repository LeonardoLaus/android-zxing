package ext.android.zxing.utils;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

public final class Utils {
    private Utils() {
        throw new AssertionError("no instance.");
    }

    @NonNull
    public static <T> T getSystemService(Context context, @NonNull String serviceName) {
        return (T) context.getSystemService(serviceName);
    }

    @NonNull
    public static Point getRealSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point point = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealSize(point);
            return point;
        } else {
            try {
                Display.class.getMethod("getRealSize", Point.class).invoke(display, point);
            } catch (Exception e) {
                display.getSize(point);
            }
            return point;
        }
    }

    @NonNull
    public static Point getScreenResolution(Context context) {
        Point screenResolution = new Point();
        WindowManager windowManager = getSystemService(context, Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getSize(screenResolution);
        return screenResolution;
    }

    public static float dp2px(Context context, float dpVal) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpVal, context.getResources().getDisplayMetrics());
    }
}
