package ext.android.samples;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_qrcode_scan).setOnClickListener(this);
        findViewById(R.id.btn_qrcode_create).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_qrcode_create:
                startActivity(new Intent(this, QRCodeActivity.class));
                break;
            case R.id.btn_qrcode_scan:
                startActivity(new Intent(this, ScanActivity.class));
                break;
        }
    }
}
