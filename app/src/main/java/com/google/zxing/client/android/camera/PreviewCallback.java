/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android.camera;

import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

@SuppressWarnings("deprecation") // camera APIs
final class PreviewCallback implements Camera.PreviewCallback {

    private static final String TAG = PreviewCallback.class.getSimpleName();

    private final CameraConfigurationManager configManager;
    private Handler previewHandler;
    private int previewMessage;

    PreviewCallback(CameraConfigurationManager configManager) {
        this.configManager = configManager;
    }

    //TODO: rotateYUV
    private static byte[] rotateYUV(byte[] src, int width, int height) {
        byte[] dst = new byte[src.length];
        //Y
        System.arraycopy(src, 0, dst, 0, src.length);
        //UV
        return dst;
    }

    void setHandler(Handler previewHandler, int previewMessage) {
        this.previewHandler = previewHandler;
        this.previewMessage = previewMessage;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Point previewResolution = configManager.getPreviewSizeOnScreen();
        Handler thePreviewHandler = previewHandler;
        if (previewResolution != null && thePreviewHandler != null) {
            final int width = previewResolution.x;
            final int height = previewResolution.y;

            if (width < height) {
                long start = System.currentTimeMillis();
                byte[] rotatedData = new byte[data.length];
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        rotatedData[y * width + width - x - 1] = data[y + x * height];
                    }
                }
                Log.e(TAG, "rotate time=" + (System.currentTimeMillis() - start) + "ms");
                data = rotatedData;
            }

            Message message = thePreviewHandler.obtainMessage(previewMessage, width,
                    height, data);
            message.sendToTarget();
            previewHandler = null;
        } else {
            Log.d(TAG, "Got preview callback, but no handler or resolution available");
        }
    }
}
