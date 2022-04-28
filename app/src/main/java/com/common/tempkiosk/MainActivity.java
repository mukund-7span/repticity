package com.common.tempkiosk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.VersionInfo;
import com.arcsoft.face.model.ActiveDeviceInfo;
import com.common.tempkiosk.activity.FaceAttrPreviewActivity;
import com.common.tempkiosk.activity.FaceManageActivity;
import com.common.tempkiosk.activity.IrRegisterAndRecognizeActivity;
import com.common.tempkiosk.activity.MultiImageActivity;
import com.common.tempkiosk.activity.RegisterAndRecognizeActivity;
import com.common.tempkiosk.activity.SingleImageActivity;
import com.common.tempkiosk.activity.TemperatureActicity;
import com.common.tempkiosk.activity.globals;
import com.common.tempkiosk.common.Util;
import com.common.tempkiosk.fragment.ChooseDetectDegreeDialog;
import com.common.thermalimage.ThermalImageUtil;
import com.google.zxing.other.BeepManager;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

//
//import com.brother.ptouch.sdk.PrinterInfo;
//import com.brother.ptouch.sdk.PrinterInfo.ErrorCode;
//import com.brother.ptouch.sdk.PrinterInfo.Model;
//import com.brother.ptouch.sdk.PrinterInfo.Orientation;
//import com.brother.ptouch.sdk.PrinterInfo.Port;
//import com.brother.ptouch.sdk.PrinterInfo.PrintMode;
//import com.brother.ptouch.sdk.PrinterStatus;

public class MainActivity extends AppCompatActivity {
    //ThermalImageUtil temperatureUtilBtn;
    TextView tiptv;
    View prime;
    View primelayout;
    String uuid;
    //com.makeramen.roundedimageview.RoundedImageView image;
    ImageView image;
    ImageView replogo;
    ImageView replogo2;
    EditText et_distance;
    EditText kioskcode;
    EditText uidfield;
    EditText uid;
    WebView myWebView;
    Button popbtn;
    Button tempbtn;
    Button validbtn;
    TextView pl;
    PopupWindow popupWindow;
    PopupWindow plv;
    Toolbar toolbar;
    int validated;
    int puid;
    Timer temptime;
    private BeepManager mnormalBeep;
    private BeepManager manormalyBeep;
    private static final String TAG = "myApp";

    int isprint;
    int apprnt;
    ThermalImageUtil temperatureUtil;

    @SuppressLint("SetJavaScriptEnabled")

    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;

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


    protected void showToast(final String s) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            //Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
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
                    //Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
                }
            });
        }
    }



    // 离线激活所需的权限
    private static final String[] NEEDED_PERMISSIONS_OFFLINE = new String[]{
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

    public class AppInstallReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            PackageManager manager = context.getPackageManager();
            if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED") && intent.getData().getSchemeSpecificPart().equals("com.telpo.temperatureservice")) {
                MainActivity.this.temperatureUtil = new ThermalImageUtil(MainActivity.this);
            }
        }
    }

    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            this.mContext = c;
        }

        @JavascriptInterface
        public void interfaced(String pro_cat_id) {
            Integer pro_cat_idi = Integer.valueOf(Integer.parseInt(pro_cat_id));
            Log.v("Interfaced Function", pro_cat_idi.toString() + " and isprint is " + MainActivity.this.isprint);
            if (pro_cat_idi.intValue() != 9) {
                final Timer myTimer = new Timer();
                myTimer.scheduleAtFixedRate(new TimerTask() {
                    public void run() {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                String newtemp = globals.newtemp;
                                String newtrackid = globals.newtrackid;
                                ((Button) MainActivity.this.findViewById(R.id.gettempe)).performClick();
                                String str = MainActivity.TAG;
                                Log.v(str, "Timer is running..");
                                if (newtemp != null) {
                                    Log.v(str, "We got a new temp of " + newtemp);
                                    MainActivity.this.ptwa(newtemp);
                                    globals.newtemp = null;
                                    myTimer.cancel();
                                }
                            }
                        });
                    }
                }, 0, 2000);
                Log.v("From web", pro_cat_id);
            } else if (Integer.valueOf(MainActivity.this.isprint) > 0) {
//                MainActivity.this.bprinter();
            }
        }

        public void showToast(String toast) {
            Toast.makeText(this.mContext, toast, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        libraryExists = checkSoFile(LIBRARIES);
        ApplicationInfo applicationInfo = getApplicationInfo();
        Log.i(TAG, "onCreate: " + applicationInfo.nativeLibraryDir);
        if (!libraryExists) {
            Log.v(TAG,"No Lib");
        } else {
            VersionInfo versionInfo = new VersionInfo();
            int code = FaceEngine.getVersion(versionInfo);
            Log.i(TAG, "onCreate: getVersion, code is: " + code + ", versionInfo is: " + versionInfo+  "version BuildDate: "+versionInfo.getBuildDate()+"version CopyRight: "+versionInfo.getCopyRight()+"version : "+versionInfo.getVersion());
        }

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.activity_main);
        //temperatureUtilBtn = new ThermalImageUtil(this);
        myWebView = (WebView) findViewById(R.id.webapp);
        toolbar = findViewById(R.id.toolbar_top);
        tiptv = findViewById(R.id.tip);
        image = findViewById(R.id.image);
        replogo = findViewById(R.id.replogo);
        popbtn = findViewById(R.id.popbtn);
        tempbtn = findViewById(R.id.gettempe);
        validbtn = findViewById(R.id.validbtn);
        setSupportActionBar(findViewById(R.id.toolbar_top));
        prime = findViewById(R.id.primeview);
        primelayout = findViewById(R.id.primelayout);
        Picasso.get().load("https://repticity.com/img/gc2.png").into(replogo);
        //Picasso.get().load("https://repticity.com/screen/beta.png").into(replogo);


        VersionInfo versionInfo = new VersionInfo();
        int code = FaceEngine.getVersion(versionInfo);
        Log.v(TAG,"Version info detection: " + code);


        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        puid = pref.getInt("puid", 0);
        Log.v(TAG, "Loaded PUID is " + puid);
        globals.globaluid = ""+puid;
        TextView myAwesomeTextView = (TextView)findViewById(R.id.uidfield);
        myAwesomeTextView.setText(""+puid);


        Util.activeEngineOffline(getApplicationContext());


        EditText passtemp = (EditText)findViewById(R.id.passtemp);

        passtemp.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {}

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                Log.v(TAG,"wtf");
            }
        });



        final ImageView RepimageView = (ImageView) findViewById(R.id.replogo);
        RepimageView.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                final Context curcontext = RepimageView.getContext();
                AlertDialog.Builder builder1 = new AlertDialog.Builder(curcontext);
                builder1.setMessage("Would you like to deregister this Kiosk?");
                builder1.setCancelable(true);

                builder1.setPositiveButton(
                        "Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                SharedPreferences pref =
                                        PreferenceManager.getDefaultSharedPreferences(curcontext);
                                SharedPreferences.Editor edit = pref.edit();
                                edit.putInt("puid", 0);
                                edit.commit();

                                tempbtn.setVisibility(View.INVISIBLE);
                                toolbar.setVisibility(View.INVISIBLE);
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        pop(0);
                                    }
                                }, 1000);
                            }
                        });

                builder1.setNegativeButton(
                        "No",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                builder1.setNeutralButton(
                        "Advanced",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //checkLibraryAndJump(TemperatureActicity.class);
                                //setContentView(R.layout.activity_tem);
                                //checkLibraryAndJump(TemperatureActicity.class);
                                //setContentView(R.layout.activity_choose_function);
                                finish();
                            }
                        });


                AlertDialog alert11 = builder1.create();
                alert11.show();

                Log.v(TAG, "RepimageView Long Click");
                return true;
            }
        });

        Log.v(TAG, "Main App Loaded");


        if(puid < 1) {
            tempbtn.setVisibility(View.INVISIBLE);
            toolbar.setVisibility(View.INVISIBLE);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    pop(0);
                }
            }, 1000);
        } else {
            WebSettings settings = myWebView.getSettings();
            settings.setJavaScriptEnabled(true);
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(prime.getWindowToken(), 0);
            imm.hideSoftInputFromWindow(primelayout.getWindowToken(), 0);
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            myWebView.getSettings().setLoadsImagesAutomatically(true);
            myWebView.getSettings().setJavaScriptEnabled(true);
            myWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            myWebView.setWebViewClient(new WebViewClient()
            {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url)
                {
                    //view.loadUrl(url);
                    System.out.println("hello");
                    return true;
                }
            });
            myWebView.getSettings().setJavaScriptEnabled(true);
            myWebView.getSettings().setDomStorageEnabled(true);
            myWebView.addJavascriptInterface(new WebAppInterface(this), "Android");
            myWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            myWebView.loadUrl("https://repticity.com/screen/v2.php?puid=" + puid);
            myWebView.setWebViewClient(new WebViewClient() {
                public void onPageFinished(WebView view, String url) {
                    LayoutInflater inflater = (LayoutInflater) MainActivity.this
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View popupView = inflater.inflate(R.layout.pop, null);
                    Button close = (Button) popupView.findViewById(R.id.popout);
                    Button valbtn = (Button) popupView.findViewById(R.id.validbtn);
                    valbtn.setVisibility(View.INVISIBLE);
                    close.performClick();
                    myWebView.setVisibility(View.VISIBLE);
                    toolbar.setVisibility(View.VISIBLE);
                    popupView.setVisibility(View.INVISIBLE);
                    Log.v(TAG, "Web App Loaded (Logged in)");
                    plv.dismiss();
                    tempbtn.setVisibility(View.VISIBLE);
                }
            });
        }

        myWebView = (WebView) findViewById(R.id.webapp);
        myWebView.getSettings().setLoadsImagesAutomatically(true);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        //myWebView.loadUrl("https://repticity.com/screen/v2.php?puid=27");

        myWebView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                Log.v(TAG, "Web App Loaded");
                //pop();
            }
        });

        mnormalBeep = new BeepManager(this, R.raw.normal);
        manormalyBeep = new BeepManager(this, R.raw.anormaly);
        et_distance=(EditText) findViewById(R.id.et_distance);

        /*
        if(!isInstalled(MainActivity.this,"com.telpo.temperatureservice")){
            Log.v(TAG,"-------------------------- Activation Failed  - Missing Service ----------------------");
        }

        activeEngineOffline(null);
        if (!libraryExists) {
            Log.v(TAG,"-------------------------- Activation Failed  - No Library ----------------------");
            return;
        }
        if (!checkPermissions(NEEDED_PERMISSIONS_OFFLINE)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS_OFFLINE, ACTION_REQUEST_PERMISSIONS);
            Log.v(TAG,"-------------------------- Activation Failed  - No Permissions ----------------------");
            return;
        }
        boolean engineOffline = Util.activeEngineOffline(getApplicationContext());
        if (engineOffline) {
            Log.v(TAG,"-------------------------- Activation Success ----------------------");
        } else  {
            Log.v(TAG,"-------------------------- Already Activated ----------------------");
        }
        activeEngineOffline(null);
         */

        //注册监听temperatureservice.apk重装广播
        AppInstallReceiver apkInstallListener = new AppInstallReceiver();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addDataScheme("package");
        registerReceiver(apkInstallListener, intentFilter);
    }



    private void setSupportActionBar(View viewById) {
    }

    private void showTip(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tiptv.setText(msg);
            }
        });
    }

    public void playAbnormalSound() {
//        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.abnormal);
//        mediaPlayer.start(); // no need to call prepare(); create() does that for you

        manormalyBeep.playBeepSoundAndVibrate();
    }

    public void playNormalSound() {
//        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.normal);
//        mediaPlayer.start(); // no need to call prepare(); create() does that for you

        mnormalBeep.playBeepSoundAndVibrate();
    }

    // Converts to fahrenheit
    private float convertCelsiusToFahrenheit(float celsius) {
        return ((celsius * 9) / 5) + 32;
    }


    public void pop(int state){
        myWebView.setVisibility(View.INVISIBLE);
        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.pop, null);

        // create the popup window
        int width = 1500;
        int height = 600;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        //final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);
        final PopupWindow popupWindow = new PopupWindow(popupView, ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT, focusable);
        popupWindow.setFocusable(true);
        //final PopupWindow popupWindow = new PopupWindow(popupView, ActionBar.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT,true);
        if(state < 1) {
            // show the popup window
            // which view you pass in doesn't matter, it is only used for the window token
            View view = (View) findViewById(R.id.primelayout);
            popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
            plv = popupWindow;

            replogo2 = popupView.findViewById(R.id.replogo2);
            Picasso.get().load("https://repticity.com/img/gc2.png").into(replogo2);
            pl = popupView.findViewById(R.id.poplabel);
            validbtn = popupView.findViewById(R.id.validbtn);
            //TextView pl = popupView.findViewById(R.id.poplabel);
            //pl.setText("test");
            kioskcode = popupView.findViewById(R.id.kioskcode);
            kioskcode.setFocusableInTouchMode(true);
            kioskcode.setFocusable(true);
            kioskcode.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(kioskcode, InputMethodManager.SHOW_IMPLICIT);
            MainActivity.this.setFinishOnTouchOutside(false);
            popupWindow.setOutsideTouchable(false);
            popupWindow.setFocusable(false);
        }

        Log.v(TAG, "IMM Loaded");

        if(state > 0){
            // dismiss the popup window when touched
            popupView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    popupWindow.dismiss();
                    myWebView.setVisibility(View.VISIBLE);
                    toolbar.setVisibility(View.VISIBLE);
                    return true;
                    //return false;
                }
            });
        }

        /*/ dismiss the popup window when touched
        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                myWebView.setVisibility(View.VISIBLE);
                return true;
            }
        });*/
    }

//    public void bprinter() {
//        new Handler(Looper.getMainLooper()).post(new Runnable() {
//            public void run() {
//                final Printer printer = new Printer();
//                PrinterInfo settings = new PrinterInfo();
//                UsbManager usbManager = (UsbManager) MainActivity.this.getSystemService("usb");
//                usbManager.requestPermission(printer.getUsbDevice(usbManager), PendingIntent.getBroadcast(MainActivity.this, 0, new Intent(MainActivity.ACTION_USB_PERMISSION), 67108864));
//                ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, 1);
//                Picasso.get().load("https://repticity.com/screen/print/" + MainActivity.this.puid + ".jpg").networkPolicy(NetworkPolicy.NO_CACHE, new NetworkPolicy[0]).memoryPolicy(MemoryPolicy.NO_CACHE, new MemoryPolicy[0]).into(MainActivity.this.prnt);
//                settings = printer.getPrinterInfo();
//                settings.printerModel = Model.QL_820NWB;
//                settings.port = Port.USB;
//                settings.workPath = MainActivity.this.getApplicationContext().getCacheDir().getAbsolutePath() + "/";
//                settings.orientation = Orientation.LANDSCAPE;
//                settings.labelNameIndex = 15;
//                settings.printMode = PrintMode.FIT_TO_PAGE;
//                settings.printMode = PrintMode.FIT_TO_PAPER;
//                settings.pjDensity =  10;
//                settings.rjDensity = 10;
//                settings.isAutoCut = true;
//                printer.setPrinterInfo(settings);
//                Log.v("Printer USB Settings", settings.toString());
//                MainActivity.this.runOnUiThread(new Runnable() {
//                    public void run() {
//                        new Thread(new Runnable() {
//                            public void run() {
//                                if (printer.startCommunication()) {
//                                    try {
//                                        Thread.sleep(2000);
//                                        Bitmap bm = ((BitmapDrawable) MainActivity.this.prnt.getDrawable()).getBitmap();
//                                        MainActivity.this.prnt.setVisibility(4);
//                                        MainActivity.this.prnt.setImageBitmap(null);
//                                        PrinterStatus result = printer.printImage(bm);
//                                        if (result.errorCode != ErrorCode.ERROR_NONE) {
//                                            Log.d("TAG", "ERROR - " + result.errorCode);
//                                        }
//                                        printer.endCommunication();
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                            }
//                        }).start();
//                    }
//                });
//            }
//        });
//    }


    public static JSONObject getJSONObjectFromURL(String urlString) throws IOException, JSONException {
        HttpURLConnection urlConnection = null;
        URL url = new URL(urlString);
        urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setReadTimeout(10000 /* milliseconds */ );
        urlConnection.setConnectTimeout(15000 /* milliseconds */ );
        urlConnection.setDoOutput(true);
        urlConnection.connect();

        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }
        br.close();

        String jsonString = sb.toString();
        System.out.println("JSON: " + jsonString);

        return  (jsonString.isEmpty()) ? new JSONObject(): new JSONObject(jsonString);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.gettempe:
                //showall();;
                //pop();
/*
                float distance=50;
                if(et_distance.length()!=0){
                    distance=Float.valueOf(et_distance.getText().toString());
                }
                final float distances = distance;


                //  Log.e("distances---",distances+"");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        TemperatureData temperatureData = temperatureUtilBtn.getDataAndBitmap(distances,true, new HotImageCallback.Stub() {
                            @Override
                            public void onTemperatureFail(String e) {
                                Log.i("getDataAndBitmap", "onTemperatureFail " + e);
                                showTip("Failed to get temperature:  " + e);
                            }

                            @Override
                            public void getTemperatureBimapData(final TemperatureBitmapData data) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        image.setImageBitmap(data.getBitmap());
                                    }
                                });
                            }

                        });
                        if (temperatureData != null) {
                            //image.setVisibility(View.VISIBLE);
                            float celtemp = temperatureData.getTemperature();
                            float fartemp = convertCelsiusToFahrenheit(celtemp);
                            // String text = temperatureData.isUnusualTem()?"Abnormal body temperature!":"Normal body temperature";
                            String text = "";
                            if(temperatureData.isUnusualTem()){
                                text = "Temperature anormaly!";
                                playAbnormalSound();
                            }else {
                                text = "Temperature normal";
                                playNormalSound();
                            }
                            //showTip(text+"\nTemperature: " + temperatureData.getTemperature()+" ℃");
                            showTip(fartemp +" F  ");
                            EditText uidfield = findViewById(R.id.uidfield);
                            final String uuid = uidfield.getText().toString();
                            OkHttpClient client = new OkHttpClient();
                            Request request = new Request.Builder()
                                    .url("https://ps.pndsn.com/publish/pub-c-e06d3f0c-130a-4377-a0ba-eefd9a4d0559/sub-c-550a901e-13a6-11eb-a3e5-1aef4acbe547/0/guestscreen/myCallback/%7B%22text%22%3A%22"+uuid+"-"+fartemp+"%22%7D")
                                    .build();
                            try {
                                Response response = client.newCall(request).execute();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            new Timer().schedule(
                                    new TimerTask(){

                                        @Override
                                        public void run(){
                                            //String ico = "https://i7.uihere.com/icons/990/824/958/assistive-listening-systems-81ebe87a91717a451dec3df1619f44c1.png";
                                            //image.setImageURI(Uri.parse(ico));
                                            //Picasso.get().load("http://i.imgur.com/DvpvklR.png").into(image);
                                            ImageView img = findViewById(R.id.image2);
                                            img.setVisibility(View.INVISIBLE);
                                            showTip("");

                                        }

                                    }, 5000);

                        }
                    }
                }).start();
*/
                break;

            case R.id.popbtn:
                Log.v(TAG, "clicked");

                break;

            case R.id.popout:
                LayoutInflater inflater = (LayoutInflater) MainActivity.this
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View popupView = inflater.inflate(R.layout.pop, null);
                Button close = (Button) popupView.findViewById(R.id.popout);
                Log.v(TAG, "dimissed");
                pop(1);
                close.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View popupView) {
                        popupWindow.dismiss();
                        Log.v(TAG, "dimissed");
                        pop(1);
                    }
                });

                break;

            case R.id.validbtn:
                Log.v(TAG, "valid click");
                final Editable keycode = kioskcode.getText();
                int codeLength = keycode.length();
                if(codeLength > 0){
                    Log.v(TAG, "code: " + keycode + " | length: " + codeLength);

                    validated = 0;





                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try  {
                                try{
                                    JSONObject jsonObject = getJSONObjectFromURL("https://repticity.com/v3/engine/app/kiosk.php?code=" + keycode);
                                    boolean suc = !jsonObject.isNull("success") && (boolean) jsonObject.get("success");
                                    int uid = (int) jsonObject.get("uid");

                                    if(uid > 0){
                                        Log.v(TAG, "Success! " + uid);
                                        String company = (String) jsonObject.get("company");
                                        pl.setText("Welcome " + company);
                                        kioskcode.setVisibility(View.INVISIBLE);
                                        validbtn.setVisibility(View.INVISIBLE);
                                        validated = uid;

                                        TextView myAwesomeTextView = (TextView)findViewById(R.id.uidfield);
                                        myAwesomeTextView.setText(""+uid);

                                    } else {
                                        Log.v(TAG, "Error! " + uid);
                                        pl.setText("Please enter a valid Kiosk Code");
                                        validated = uid;
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });


/*
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try  {
                                try{
                                    JSONObject jsonObject = getJSONObjectFromURL("http://repticity.com/v3/engine/app/kiosk.php?code=" + keycode);
                                    boolean suc = (boolean) jsonObject.get("success");
                                    int uid = (int) jsonObject.get("uid");

                                    if(uid > 0){
                                        Log.v(TAG, "Success! " + uid);
                                        String company = (String) jsonObject.get("company");
                                        pl.setText("Welcome " + company);
                                        validated = 1;
                                    } else {
                                        Log.v(TAG, "Error! " + uid);
                                        pl.setText("Please enter a valid Kiosk Code");
                                        validated = 0;
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    thread.start();
*/
                    Log.v(TAG, "validated code =  " + validated);
                    if(validated > 0){
                        SharedPreferences pref =
                                PreferenceManager.getDefaultSharedPreferences(this);
                        SharedPreferences.Editor edit = pref.edit();
                        edit.putInt("puid", validated);
                        edit.commit();
                        globals.globaluid = ""+validated;
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        WebSettings settings = myWebView.getSettings();
                        settings.setJavaScriptEnabled(true);
                        myWebView.getSettings().setLoadsImagesAutomatically(true);
                        myWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
                        myWebView.getSettings().setJavaScriptEnabled(true);
                        myWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
                        myWebView.setWebViewClient(new WebViewClient()
                        {
                            @Override
                            public boolean shouldOverrideUrlLoading(WebView view, String url)
                            {
                                //view.loadUrl(url);
                                System.out.println("hello");
                                return true;
                            }
                        });
                        myWebView.getSettings().setJavaScriptEnabled(true);
                        myWebView.getSettings().setDomStorageEnabled(true);
                        myWebView.addJavascriptInterface(new WebAppInterface(this), "Android");
                        myWebView.loadUrl("https://repticity.com/screen/v2.php?puid=" + validated);
                        myWebView.setWebViewClient(new WebViewClient() {
                            public void onPageFinished(WebView view, String url) {
                                LayoutInflater inflater = (LayoutInflater) MainActivity.this
                                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                View popupView = inflater.inflate(R.layout.pop, null);
                                Button close = (Button) popupView.findViewById(R.id.popout);
                                Button valbtn = (Button) popupView.findViewById(R.id.validbtn);
                                valbtn.setVisibility(View.INVISIBLE);
                                close.performClick();
                                myWebView.setVisibility(View.VISIBLE);
                                toolbar.setVisibility(View.VISIBLE);
                                popupView.setVisibility(View.INVISIBLE);
                                Log.v(TAG, "Web App Loaded (Logged in)");
                                plv.dismiss();
                                //tempbtn.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }
                break;

            case R.id.check:
                /*temperatureUtilBtn.calibrationTem_DAT(new CalibrationCallBack.Stub() {
                    @Override
                    public void onCalibrating() {
                        Log.i("calibrationTem_DAT", "onCalibrating");
                        showTip("Calibrating...");
                    }

                    @Override
                    public void onSuccess() {
                        Log.i("calibrationTem_DAT", "onSuccess");
                        showTip("Calibration success!");
                    }

                    @Override
                    public void onFail(final String errmsg) {
                        Log.i("calibrationTem_DAT", "onFail" + errmsg);
                        showTip("Calibration failed! " + errmsg);
                    }
                });*/
                break;
        }
    }

    private boolean isInstalled(Context context, String packageName) {
        final PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        for (int i = 0; i < pinfo.size(); i++) {
            if (pinfo.get(i).packageName.equalsIgnoreCase(packageName))
                return true;
        }
        return false;
    }

    public void ptwa(String newtemp){
        myWebView.loadUrl("javascript:passtemp('"+newtemp+"')");
    }
    public void ptid(String tid){
        myWebView.loadUrl("javascript:passtrackid('"+tid+"')");
    }


    public void popClick(View view) {
        Log.v(TAG, "popview click");
        //pop();
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
            showToast(getString(R.string.active_success));
        } else  {
            showToast(getString(R.string.already_activated));

        }
    }


    public void afterRequestPermission(int requestCode, boolean isAllGranted) {
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

