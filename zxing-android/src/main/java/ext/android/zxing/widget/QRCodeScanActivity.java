package ext.android.zxing.widget;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.WindowManager;

import com.google.zxing.Result;

import ext.android.zxing.Decoder;
import ext.android.zxing.R;

public class QRCodeScanActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private static final String TAG = "QRCodeScanner";
    private TextureView textureView;
    private ViewfinderView viewfinderView;
    private Decoder decoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.zxing_activity_qrcode_scan);
        textureView = findViewById(R.id.texture);
        viewfinderView = findViewById(R.id.view_finder);
        textureView.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        Log.i(TAG, "[onSurfaceTextureAvailable] init QRCode Decoder.");
        decoder = new Decoder(getApplicationContext(), callback);
        viewfinderView.setFrameRect(decoder.getFramingRect());
        decoder.start(surfaceTexture);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        Log.d(TAG, "[onSurfaceTextureDestroyed] release QRCode Decoder.");
        if (decoder != null) {
            decoder.stop();
            decoder = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }


    private final Decoder.DecodeCallback callback = new Decoder.DecodeCallback() {
        @Override
        public void onSuccess(Result rawResult, byte[] barcode, float scaledFactor) {

        }

        @Override
        public void onFailure() {

        }
    };
}
