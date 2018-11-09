package ext.android.zxing;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import java.io.IOException;
import java.util.List;

import ext.android.zxing.utils.CameraUtils;
import ext.android.zxing.utils.Utils;

final class CameraManager {

    private static final String TAG = "CameraManager";

    private final Context mContext;
    private final Camera.CameraInfo mCameraInfo;

    private int mCameraId;
    private Camera mCamera;
    private AutoFocusManager mAutoFocusManager;

    private SurfaceTexture surfaceTexture;
    private boolean isPreviewing;

    CameraManager(Context context) {
        this.mContext = context.getApplicationContext();
        this.mCameraInfo = new Camera.CameraInfo();
        this.mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    }

    public void start(SurfaceTexture surfaceTexture) throws IOException {
        if (isOpened()) {
            return;
        }
        this.surfaceTexture = surfaceTexture;
        open();
        startPreview();
    }

    public void stop() {
        stopPreview();
        close();
    }

    public boolean isOpened() {
        return mCamera != null;
    }

    public void setTorch(boolean on) {
        if (mCamera != null && on != getTorchState()) {
            boolean hasAutoFocusManager = mAutoFocusManager != null;
            if (hasAutoFocusManager) {
                mAutoFocusManager.stop();
                mAutoFocusManager = null;
            }
            setTorchInternal(on);
            if (hasAutoFocusManager) {
                mAutoFocusManager = new AutoFocusManager(mCamera);
                mAutoFocusManager.start();
            }
        }
    }

    public void requestPreviewFrame(Camera.PreviewCallback previewCallback) {
        if (mCamera != null && isPreviewing) {
            mCamera.setOneShotPreviewCallback(previewCallback);
        }
    }

    public Point getPreviewResolution() {
        if (mCamera == null) {
            return null;
        }
        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
        return new Point(previewSize.width, previewSize.height);
    }

    public Point getPreviewSizeOnScreen() {
        if (mCamera == null) {
            return null;
        }
        Point screenResolution = Utils.getScreenResolution(mContext);
        boolean isScreenPortrait = screenResolution.x < screenResolution.y;

        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
        boolean isPreviewPortrait = previewSize.width < previewSize.height;
        if (isScreenPortrait == isPreviewPortrait) {
            return new Point(previewSize.width, previewSize.height);
        } else {
            return new Point(previewSize.height, previewSize.width);
        }
    }

    private void open() throws IOException {
        this.mCamera = Camera.open(this.mCameraId);
        Camera.Parameters parameters = this.mCamera.getParameters();
        setupCameraParameters(parameters);
        this.mCamera.setParameters(parameters);
        int orientation = CameraUtils.getDisplayOrientation(mContext, mCameraId);
        this.mCamera.setDisplayOrientation(orientation);
        this.mCamera.setPreviewTexture(surfaceTexture);
    }

    private void close() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    private void startPreview() {
        if (mCamera != null && !isPreviewing) {
            mCamera.startPreview();
            isPreviewing = true;
            mAutoFocusManager = new AutoFocusManager(mCamera);
        }
    }

    private void stopPreview() {
        if (mAutoFocusManager != null) {
            mAutoFocusManager.stop();
            mAutoFocusManager = null;
        }
        if (mCamera != null && isPreviewing) {
            mCamera.stopPreview();
            isPreviewing = false;
        }
    }

    private void setupCameraParameters(Camera.Parameters parameters) {
        parameters.setPreviewFormat(ImageFormat.NV21);
        parameters.setPictureFormat(ImageFormat.JPEG);
        parameters.setRotation(90);

        resolveFocusMode(parameters);

        final List<Camera.Size> supportPictureSizes = parameters.getSupportedPictureSizes();
        double screenRatio = CameraUtils.findFullscreenRatio(mContext, supportPictureSizes);
        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            Log.e(TAG, "support picture size=" + size.width + "x" + size.height);
        }
        //预览尺寸
        resolvePreviewSize(parameters);
        //照片尺寸
        Camera.Size pictureSize = CameraUtils.getOptimalPictureSize(supportPictureSizes, screenRatio);
        parameters.setPictureSize(pictureSize.width, pictureSize.height);
        Log.e(TAG, "optimal pic size: " + pictureSize.width + "x" + pictureSize.height);
    }

    private void resolveFocusMode(Camera.Parameters parameters) {
        List<String> supportedFocusModes = parameters.getSupportedFocusModes();
        String focusMode = CameraUtils.findSettableValue("focus mode", supportedFocusModes,
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
                Camera.Parameters.FOCUS_MODE_AUTO);
        if (focusMode != null) {
            if (focusMode.equals(parameters.getFocusMode())) {
                Log.i(TAG, "Focus mode already set to " + focusMode);
            } else {
                parameters.setFocusMode(focusMode);
            }
        }
    }

    private void resolvePreviewSize(Camera.Parameters parameters) {
        final List<Camera.Size> supportPreviewSizes = parameters.getSupportedPreviewSizes();
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            Log.e(TAG, "support preview size=" + size.width + "x" + size.height);
        }

        final List<Camera.Size> supportPictureSizes = parameters.getSupportedPictureSizes();
        double screenRatio = CameraUtils.findFullscreenRatio(mContext, supportPictureSizes);
        Camera.Size previewSize = CameraUtils.getOptimalPreviewSize(mContext, supportPreviewSizes, screenRatio, false);
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        Log.e(TAG, "optimal preview size: " + previewSize.width + "x" + previewSize.height);
    }

    private void setTorchInternal(boolean on) {
        Camera.Parameters parameters = mCamera.getParameters();
        final List<String> supportedFlashModes = parameters.getSupportedFlashModes();
        String flashMode;
        if (on) {
            flashMode = CameraUtils.findSettableValue("flash mode", supportedFlashModes,
                    Camera.Parameters.FLASH_MODE_TORCH,
                    Camera.Parameters.FLASH_MODE_ON);
        } else {
            flashMode = CameraUtils.findSettableValue("flash mode", supportedFlashModes,
                    Camera.Parameters.ANTIBANDING_OFF);
        }
        if (flashMode != null) {
            if (flashMode.equals(parameters.getFlashMode())) {
                Log.i(TAG, "Flash mode already set to " + flashMode);
            } else {
                Log.i(TAG, "Setting flash mode to " + flashMode);
                parameters.setFlashMode(flashMode);
            }
        }
        mCamera.setParameters(parameters);
    }

    public boolean getTorchState() {
        if (this.mCamera == null)
            return false;
        Camera.Parameters parameters = this.mCamera.getParameters();
        if (parameters != null) {
            String flashMode = parameters.getFlashMode();
            return Camera.Parameters.FLASH_MODE_ON.equals(flashMode)
                    || Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode);
        }
        return false;
    }
}
