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
        // 检查蓝牙权限
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.BLUETOOTH},
                    REQUEST_PERMISSION_CODE);
        }

        // 检查蓝牙扫描权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.BLUETOOTH_SCAN},
                    REQUEST_BLUETOOTH_SCAN_PERMISSION);
        }

        // 检查蓝牙连接权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_BLUETOOTH_CONNECT_PERMISSION);
        }

        // 如果是Android 6.0及以上，检查位置权限（蓝牙扫描需要位置权限）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION_CODE);
        }
    }

    // 处理权限请求回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 蓝牙权限被授予
                    Toast.makeText(this, "Bluetooth permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    // 蓝牙权限被拒绝
                    Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_BLUETOOTH_SCAN_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 蓝牙扫描权限被授予
                    Toast.makeText(this, "Bluetooth scan permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    // 蓝牙扫描权限被拒绝
                    Toast.makeText(this, "Bluetooth scan permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_BLUETOOTH_CONNECT_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 蓝牙连接权限被授予
                    Toast.makeText(this, "Bluetooth connect permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    // 蓝牙连接权限被拒绝
                    Toast.makeText(this, "Bluetooth connect permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
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
