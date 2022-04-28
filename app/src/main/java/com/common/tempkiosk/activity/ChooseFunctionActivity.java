package com.common.tempkiosk.activity;

import static com.arcsoft.face.enums.DetectFaceOrientPriority.ASF_OP_ALL_OUT;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.core.app.ActivityCompat;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.VersionInfo;
import com.arcsoft.face.model.ActiveDeviceInfo;
import com.common.tempkiosk.MainActivity;
import com.common.tempkiosk.R;
import com.common.tempkiosk.common.Util;
import com.common.tempkiosk.fragment.ChooseDetectDegreeDialog;
import com.common.tempkiosk.util.ConfigUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class ChooseFunctionActivity extends BaseActivity {
    private static final String TAG = "ChooseFunctionActivity";
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;


    // 离线激活所需的权限
    private static final String[] NEEDED_PERMISSIONS_OFFLINE = new String[]{
            //Manifest.permission.READ_PHONE_STATE,
            //Manifest.permission.READ_EXTERNAL_STORAGE,
            //Manifest.permission.WRITE_EXTERNAL_STORAGE
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE

    };
    boolean libraryExists = true;
    // Demo 所需的动态库文件
    private static final String[] LIBRARIES = new String[]{
            // 人脸相关
            "libarcsoft_face_engine.so",
            "libarcsoft_face.so",
            // 图像库相关
            "libarcsoft_image_util.so",
    };
    // 修改配置项的对话框
    ChooseDetectDegreeDialog chooseDetectDegreeDialog;
    String isonline;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_function);
        LinearLayout nocon = findViewById(R.id.nocon);
        nocon.setVisibility(View.INVISIBLE);
        LinearLayout cblayout = findViewById(R.id.cblayout);
        cblayout.setVisibility(View.INVISIBLE);
        libraryExists = checkSoFile(LIBRARIES);
        ApplicationInfo applicationInfo = getApplicationInfo();
        Log.i(TAG, "onCreate: " + applicationInfo.nativeLibraryDir);
        if (!libraryExists) {
            showToast(getString(R.string.library_not_found));
        } else {
            VersionInfo versionInfo = new VersionInfo();
            //int code = FaceEngine.getVersion(versionInfo);
            //Log.i(TAG, "onCreate: getVersion, code is: " + code + ", versionInfo is: " + versionInfo+  "version BuildDate: "+versionInfo.getBuildDate()+"version CopyRight: "+versionInfo.getCopyRight()+"version : "+versionInfo.getVersion());
        }

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        StringBuilder sb = new StringBuilder();
        HttpURLConnection urlConnection = null;
        isonline = "0";
        try {
            URL url = new URL("https://www.repticity.com/beta/api/v1/online");
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.setDoOutput(true);
            urlConnection.connect();
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            String jsonString = sb.toString();
            isonline = jsonString;
        } catch (IOException e) {
            isonline = "0";
        }

//        int ison = isonline.isEmpty()?0 : Integer.parseInt(isonline);
        int ison = 1; //temp varialble
        if(ison > 0) {
            //showToast("Kiosk Online - Connected");
            cblayout.setVisibility(View.VISIBLE);
            activeEngineOffline(null);
        } else {
            //showToast("Kiosk Offline - Check Connection");
            nocon.setVisibility(View.VISIBLE);
            cblayout.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * 检查能否找到动态链接库，如果找不到，请修改工程配置
     *
     * @param libraries 需要的动态链接库
     * @return 动态库是否存在
     */
    private boolean checkSoFile(String[] libraries) {
        File dir = new File(getApplicationInfo().nativeLibraryDir);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return false;
        }
        List<String> libraryNameList = new ArrayList<>();
        for (File file : files) {
            libraryNameList.add(file.getName());
        }
        boolean exists = true;
        for (String library : libraries) {
            exists &= libraryNameList.contains(library);
        }
        return exists;
    }

    /**
     * 打开相机，显示年龄性别
     *
     * @param view
     */
    public void jumpToPreviewActivity(View view) {
        checkLibraryAndJump(FaceAttrPreviewActivity.class);
    }

    /**
     * 处理单张图片，显示图片中所有人脸的信息，并且一一比对相似度
     *
     * @param view
     */
    public void jumpToSingleImageActivity(View view) {
        checkLibraryAndJump(SingleImageActivity.class);
    }

    /**
     * 选择一张主照，显示主照中人脸的详细信息，然后选择图片和主照进行比对
     *
     * @param view
     */
    public void jumpToMultiImageActivity(View view) {
        checkLibraryAndJump(MultiImageActivity.class);
    }

    /**
     * 打开相机，RGB活体检测，人脸注册，人脸识别
     *
     * @param view
     */
    public void jumpToFaceRecognizeActivity(View view) {
        checkLibraryAndJump(RegisterAndRecognizeActivity.class);
    }

    /**
     * 打开相机，IR活体检测，人脸注册，人脸识别
     *
     * @param view
     */
    public void jumpToIrFaceRecognizeActivity(View view) {
        checkLibraryAndJump(IrRegisterAndRecognizeActivity.class);
    }

    /**
     * 批量注册和删除功能
     *
     * @param view
     */
    public void jumpToBatchRegisterActivity(View view) {
        checkLibraryAndJump(FaceManageActivity.class);
    }

    /**
     * 额头测温演示
     *
     * @param view
     */
    public void temActivity(View view) {
        checkLibraryAndJump(TemperatureActicity.class);
    }



    /**
     * 离线激活
     *
     * @param view
     */
    public void activeEngineOffline(View view) {
        ConfigUtil.setFtOrient(this, ASF_OP_ALL_OUT);
        if (!libraryExists) {
            showToast(getString(R.string.library_not_found));
            return;
        }
        if (!checkPermissions(NEEDED_PERMISSIONS_OFFLINE)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS_OFFLINE, ACTION_REQUEST_PERMISSIONS);
            return;
        }
        boolean engineOffline = Util.activeEngineOffline(getApplicationContext());
        if (engineOffline) {
            Log.v(TAG,"Repticity has been Activated - New Activation");
            checkLibraryAndJump(MainActivity.class);
        } else  {
            Log.v(TAG,"Repticity is Online - Already Activated");
            checkLibraryAndJump(MainActivity.class);
        }
    }

    @Override
    protected void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            if (isAllGranted) {
                activeEngineOffline(null);
            } else {
                showToast(getString(R.string.permission_denied));
            }
        }
    }

    void checkLibraryAndJump(Class activityClass) {
        if (!libraryExists) {
            showToast(getString(R.string.library_not_found));
            return;
        }
        startActivity(new Intent(this, activityClass));
    }


    public void copyActiveDeviceInfo(View view) {
        ActiveDeviceInfo activeDeviceInfo = new ActiveDeviceInfo();
        int code = FaceEngine.getActiveDeviceInfo(this, activeDeviceInfo);
        if (code == ErrorInfo.MOK) {
            StringBuilder stringBuilder = new StringBuilder();
            int length = activeDeviceInfo.getDeviceInfo().length();
            if (length > 20) {
                stringBuilder.append(activeDeviceInfo.getDeviceInfo().substring(0, 10));
                stringBuilder.append("......");
                stringBuilder.append(activeDeviceInfo.getDeviceInfo().substring(length - 10, length));
            }
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("finger", activeDeviceInfo.getDeviceInfo());
            clipboardManager.setPrimaryClip(clipData);
            showLongToast(getString(R.string.device_info_copied, stringBuilder.toString()));
        } else {
            showToast(getString(R.string.get_device_finger_failed, code));
        }
    }


    public void chooseDetectDegree(View view) {
        if (chooseDetectDegreeDialog == null) {
            chooseDetectDegreeDialog = new ChooseDetectDegreeDialog();
        }
        if (chooseDetectDegreeDialog.isAdded()) {
            chooseDetectDegreeDialog.dismiss();
        }
        chooseDetectDegreeDialog.show(getSupportFragmentManager(), ChooseDetectDegreeDialog.class.getSimpleName());
    }

}
