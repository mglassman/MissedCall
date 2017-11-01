package com.example.micha.missedcall;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        if (ActivityCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
//
//            this.requestPermissions(new String[] {Manifest.permission.READ_CALL_LOG}, 1);
//        }
//
//        if (ActivityCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
//
//            this.requestPermissions(new String[] {Manifest.permission.READ_PHONE_STATE}, 2);
//        }
//
//        if (ActivityCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
//
//            this.requestPermissions(new String[] {Manifest.permission.READ_CONTACTS}, 3);
//        }

        View view = findViewById(R.id.parent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (view != null) {
                view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        setContentView(R.layout.activity_main);
    }
}
