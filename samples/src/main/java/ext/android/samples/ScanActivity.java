package ext.android.samples;

import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.Result;

import ext.android.zxing.widget.QRCodeScanActivity;

public class ScanActivity extends QRCodeScanActivity {

    @Override
    public void onSuccess(Result rawResult, byte[] barcode, float scaledFactor) {
        super.onSuccess(rawResult, barcode, scaledFactor);
        Toast.makeText(this, rawResult.toString(), Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        switch (view.getId()) {
            case R.id.menu_gallery:
                if (getDecoder() != null) {
                    ImageView qrcode = findViewById(R.id.qrcode);
                    qrcode.setImageResource(R.drawable.qrcode);
                    getDecoder().decodeBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.qrcode));
                }
                break;
        }
    }
}
