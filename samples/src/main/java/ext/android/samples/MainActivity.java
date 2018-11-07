package ext.android.samples;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_qrcode_scan).setOnClickListener(this);
        findViewById(R.id.btn_qrcode_create).setOnClickListener(this);
    }
}
