// SPDX-License-Identifier: Unlicense OR MIT

package org.gioui;

import android.app.Activity;
import android.os.Bundle;
import android.content.res.Configuration;
import android.view.ViewGroup;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public final class GioActivity extends Activity {
	private GioView view;
	public FrameLayout layer;
    private static final int REQUEST_PERMISSION_CODE = 1;
    private static final int REQUEST_BLUETOOTH_SCAN_PERMISSION = 2;
    private static final int REQUEST_BLUETOOTH_CONNECT_PERMISSION = 3;

	@Override public void onCreate(Bundle state) {
            super.onCreate(state);

            // 调用权限请求
            checkAndRequestPermissions();

            layer = new FrameLayout(this);            
            view = new GioView(this);

            view.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ));
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);

            layer.addView(view);
            setContentView(layer);
	}

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.BLUETOOTH_SCAN}, 
                REQUEST_CODE_BLUETOOTH_SCAN);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 
                REQUEST_CODE_BLUETOOTH_CONNECT);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                REQUEST_CODE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        switch (requestCode) {
            case REQUEST_CODE_BLUETOOTH_SCAN:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "REQUEST_CODE_BLUETOOTH_SCAN Bluetooth permission granted", Toast.LENGTH_SHORT).show();
                } else {
                        Toast.makeText(this, "REQUEST_CODE_BLUETOOTH_SCAN Bluetooth permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_CODE_BLUETOOTH_CONNECT:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "REQUEST_CODE_BLUETOOTH_CONNECT Bluetooth permission granted", Toast.LENGTH_SHORT).show();
                } else {
                        Toast.makeText(this, "REQUEST_CODE_BLUETOOTH_CONNECT Bluetooth permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_CODE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "REQUEST_CODE_LOCATION Bluetooth permission granted", Toast.LENGTH_SHORT).show();
                } else {
                        Toast.makeText(this, "REQUEST_CODE_LOCATION Bluetooth permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

	@Override public void onDestroy() {
		view.destroy();
		super.onDestroy();
	}

	@Override public void onStart() {
		super.onStart();
		view.start();
	}

	@Override public void onStop() {
		view.stop();
		super.onStop();
	}

	@Override public void onConfigurationChanged(Configuration c) {
		super.onConfigurationChanged(c);
		view.configurationChanged();
	}

	@Override public void onLowMemory() {
		super.onLowMemory();
		GioView.onLowMemory();
	}

	@Override public void onBackPressed() {
		if (!view.backPressed())
			super.onBackPressed();
	}
}
