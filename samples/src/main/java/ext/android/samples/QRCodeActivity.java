package ext.android.samples;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import ext.android.zxing.QRCodeCreator;

public class QRCodeActivity extends AppCompatActivity {
    private EditText editText;
    private Button button;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);
        editText = findViewById(R.id.et);
        button = findViewById(R.id.btn);
        imageView = findViewById(R.id.iv);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(editText.getEditableText()))
                    return;
                String contents = editText.getEditableText().toString();
                final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                final int size = (int) (Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels) * 0.8);
                Bitmap bitmap = QRCodeCreator.createQRCode(contents, size, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                Log.i("Test", "bitmap=" + bitmap);
                imageView.setImageBitmap(bitmap);
            }
        });
    }
}
