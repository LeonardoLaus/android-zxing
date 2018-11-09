package ext.android.zxing.widget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.google.zxing.Result;

import ext.android.zxing.Decoder;
import ext.android.zxing.R;

public class QRCodeScanActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, Decoder.DecodeCallback, View.OnClickListener {

    public static final String MODE = "SCAN_MODE";
    public static final String MODE_NONE = "MODE_NONE";
    public static final String MODE_PIC = "MODE_PIC";

    private static final String TAG = "QRCodeScanner";
    private TextureView textureView;
    private QRCodeScanner qrCodeScanner;
    private ImageView qrCodeView;
    private Dialog failureDialog;
    private Decoder decoder;
    private String mMode = MODE_NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.zxing_activity_qrcode_scan);
        mMode = getIntent().getStringExtra(MODE);
        if (mMode == null) {
            mMode = MODE_NONE;
        }
        decoder = new Decoder(getApplicationContext(), this);
        initViews();
        updateViews();
    }

    private void initViews() {
        qrCodeView = findViewById(R.id.qrcode);
        qrCodeScanner = findViewById(R.id.scanner);
        textureView = findViewById(R.id.texture);
        if (isDefaultMode()) {
            qrCodeScanner.setOnTorchClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (decoder != null) {
                        decoder.setTorch(!decoder.isTorchOn());
                    }
                }
            });
            textureView.setSurfaceTextureListener(this);
        }
        findViewById(R.id.menu_close).setOnClickListener(this);
        findViewById(R.id.menu_gallery).setOnClickListener(this);
    }

    private void updateViews() {
        if (isDefaultMode()) {
            qrCodeScanner.setVisibility(View.VISIBLE);
            textureView.setVisibility(View.VISIBLE);
            qrCodeView.setVisibility(View.GONE);
        } else {
            qrCodeScanner.setVisibility(View.GONE);
            textureView.setVisibility(View.GONE);
            qrCodeView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        Log.i(TAG, "[onSurfaceTextureAvailable] init QRCode Decoder.");
        qrCodeScanner.setFrameRect(decoder.getFramingRect());
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
        }
        qrCodeScanner.setFrameRect(null);
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (decoder != null) {
            decoder.destroy();
        }
    }

    @Override
    public void onSuccess(Result rawResult, byte[] barcode, float scaledFactor) {

    }

    @Override
    public void onFailure() {
        showFailureDialog();
    }

    private void showFailureDialog() {
        if (failureDialog == null) {
            View contentView = LayoutInflater.from(this).inflate(R.layout.zxing_dialog_scan_failure, null);
            contentView.findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    failureDialog.dismiss();
                    setResult(RESULT_CANCELED);
                    finish();
                }
            });
            failureDialog = new AlertDialog.Builder(this)
                    .setView(contentView)
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            failureDialog.dismiss();
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                    })
                    .create();
        }
        failureDialog.show();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.menu_close) {
            setResult(RESULT_CANCELED);
            finish();
        } else if (id == R.id.menu_gallery) {
            mMode = MODE_PIC;
            decoder.stop();
            updateViews();
        }
    }

    protected Decoder getDecoder() {
        return decoder;
    }

    private boolean isDefaultMode() {
        return MODE_NONE.equalsIgnoreCase(mMode);
    }
}
