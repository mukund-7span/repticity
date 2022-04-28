package com.common.tempkiosk.activity;

import android.content.pm.PackageManager;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {
    /**
     * 权限检查
     *
     * @param neededPermissions 需要的权限
     * @return 是否全部被允许
     */
    protected boolean checkPermissions(String[] neededPermissions) {
        if (neededPermissions == null || neededPermissions.length == 0) {
            return true;
        }
        boolean allGranted = true;
        for (String neededPermission : neededPermissions) {
            allGranted &= ContextCompat.checkSelfPermission(this, neededPermission) == PackageManager.PERMISSION_GRANTED;
        }
        return allGranted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isAllGranted = true;
        for (int grantResult : grantResults) {
            isAllGranted &= (grantResult == PackageManager.PERMISSION_GRANTED);
        }
        afterRequestPermission(requestCode, isAllGranted);
    }

    /**
     * 请求权限的回调
     *
     * @param requestCode  请求码
     * @param isAllGranted 是否全部被同意
     */
    protected abstract void afterRequestPermission(int requestCode, boolean isAllGranted);

    protected void showToast(final String s) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    protected void showLongToast(final String s) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

}
