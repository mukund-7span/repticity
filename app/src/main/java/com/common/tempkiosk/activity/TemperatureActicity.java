package com.common.tempkiosk.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.common.tempkiosk.MainActivity;
import com.common.tempkiosk.R;
import com.common.tempkiosk.faceserver.CompareResult;
import com.common.tempkiosk.faceserver.FaceServer;
import com.common.tempkiosk.model.DrawInfo;
import com.common.tempkiosk.model.FacePreviewInfo;
import com.common.tempkiosk.util.ConfigUtil;
import com.common.tempkiosk.util.DrawHelper;
import com.common.tempkiosk.util.camera.CameraHelper;
import com.common.tempkiosk.util.camera.CameraListener;
import com.common.tempkiosk.util.face.FaceHelper;
import com.common.tempkiosk.util.face.FaceListener;
import com.common.tempkiosk.util.face.LivenessType;
import com.common.tempkiosk.util.face.RecognizeColor;
import com.common.tempkiosk.util.face.RequestFeatureStatus;
import com.common.tempkiosk.util.face.RequestLivenessStatus;
import com.common.tempkiosk.widget.FaceRectView;
import com.common.tempkiosk.widget.FaceSearchResultAdapter;
import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceShelterInfo;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.MaskInfo;
import com.arcsoft.face.enums.DetectFaceOrientPriority;
import com.arcsoft.face.enums.DetectMode;
import com.common.thermalimage.HotImageCallback;
import com.common.thermalimage.TemperatureBitmapData;
import com.common.thermalimage.TemperatureData;
import com.common.thermalimage.ThermalImageUtil;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.arcsoft.face.enums.DetectFaceOrientPriority.ASF_OP_ALL_OUT;

public class TemperatureActicity extends BaseActivity implements ViewTreeObserver.OnGlobalLayoutListener {
    private static final String TAG = "RegisterAndRecognize";
    boolean tem=true;
    int moudle=16;
    int frames=3;
    private final Object obj = new Object();
    private TextToSpeech mTextToSpeech; //TTS 引擎
    private boolean Isneedtem = true;
    ThermalImageUtil temperatureUtil;

    private boolean Istts1 = false;
    private boolean Istts2 = false;
    private boolean Istts3 = false;
    private boolean IsNeedface = true;
    Timer  ttstimer1,temTimer,ttstimer2, ttstimer3;//TTS引擎timer

    private Map<Integer, Integer> faceOffsetProcessCountMap = new ConcurrentHashMap();

    int faceid = -1;
    int faceid2 = -1;
    boolean Isnewperson = false;
    Rect newRect;
    Rect NewRectx;

    private int distancestatus = -1;// -1 是默认值 ，0 正常值  1太近  2 太远

    ImageView img_temp;
    TextView  tv_temp_detail,tv_temperature,masktxt;

    private static final int MAX_DETECT_NUM = 10;
    /**
     * 当FR成功，活体未成功时，FR等待活体的时间
     */
    private static final int WAIT_LIVENESS_INTERVAL = 100;
    /**
     * 失败重试间隔时间（ms）
     */
    private static final long FAIL_RETRY_INTERVAL = 1000;
    /**
     * 出错重试最大次数
     */
    private static final int MAX_RETRY_TIME = 3;

    private CameraHelper cameraHelper;
    private DrawHelper drawHelper;
    private Camera.Size previewSize;
    /**
     * 优先打开的摄像头，本界面主要用于单目RGB摄像头设备，因此默认打开前置
     */
    private Integer rgbCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;

    /**
     * VIDEO模式人脸检测引擎，用于预览帧人脸追踪
     */
    private FaceEngine ftEngine;
    /**
     * 用于特征提取的引擎
     */
    private FaceEngine frEngine;
    /**
     * IMAGE模式活体检测引擎，用于预览帧人脸活体检测
     */
    private FaceEngine flEngine;

    private int ftInitCode = -1;
    private int frInitCode = -1;
    private int flInitCode = -1;
    private FaceHelper faceHelper;
    private List<CompareResult> compareResultList;
    private FaceSearchResultAdapter adapter;
    /**
     * 活体检测的开关
     */
    private boolean livenessDetect = true;
    /**
     * 注册人脸状态码，准备注册
     */
    private static final int REGISTER_STATUS_READY = 0;
    /**
     * 注册人脸状态码，注册中
     */
    private static final int REGISTER_STATUS_PROCESSING = 1;
    /**
     * 注册人脸状态码，注册结束（无论成功失败）
     */
    private static final int REGISTER_STATUS_DONE = 2;

    private int registerStatus = REGISTER_STATUS_DONE;
    /**
     * 用于记录人脸识别相关状态
     */
    private ConcurrentHashMap<Integer, Integer> requestFeatureStatusMap = new ConcurrentHashMap<>();
    /**
     * 用于记录人脸特征提取出错重试次数
     */
    private ConcurrentHashMap<Integer, Integer> extractErrorRetryMap = new ConcurrentHashMap<>();
    /**
     * 用于存储活体值
     */
    private ConcurrentHashMap<Integer, Integer> livenessMap = new ConcurrentHashMap<>();
    /**
     * 用于存储活体检测出错重试次数
     */
    private ConcurrentHashMap<Integer, Integer> livenessErrorRetryMap = new ConcurrentHashMap<>();

    private CompositeDisposable getFeatureDelayedDisposables = new CompositeDisposable();
    private CompositeDisposable delayFaceTaskCompositeDisposable = new CompositeDisposable();
    /**
     * 相机预览显示的控件，可为SurfaceView或TextureView
     */
    private View previewView;
    /**
     * 绘制人脸框的控件
     */
    private FaceRectView faceRectView;

    private Switch switchLivenessDetect;

    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    /**
     * 识别阈值
     */
    private static final float SIMILAR_THRESHOLD = 0.7f;
    /**
     * 所需的所有权限信息
     */
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tem);
        //保持亮屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            getWindow().setAttributes(attributes);
        }

        // Activity启动后就锁定为启动时的方向
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        //本地人脸库初始化
        FaceServer.getInstance().init(this);

        mTextToSpeech = new TextToSpeech(TemperatureActicity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    /*
                     */
                    // setLanguage设置语言
                    //int result = mTextToSpeech.setLanguage(Locale.CHINESE);
                    int result = mTextToSpeech.setLanguage(Locale.getDefault());
                    // TextToSpeech.LANG_MISSING_DATA：表示语言的数据丢失
                    // TextToSpeech.LANG_NOT_SUPPORTED：不支持
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(getApplicationContext(), "TTS not support", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });


        moudle=  com.common.tempkiosk.common.Application.getInstance().getTemperatureUtil().getUsingModule()[0];

        if (moudle==4||moudle==8||moudle==9||moudle==10||moudle==15||moudle==16||moudle==17){
            frames=1;
        }else {
            frames=3;
        }



        initView();
    }

    private void initView() {
        previewView = findViewById(R.id.single_camera_texture_preview);
        img_temp = findViewById(R.id.iv_temp);
        tv_temp_detail = findViewById(R.id.tv_temp_detail);
        tv_temperature=findViewById(R.id.tv_temperature);
        masktxt = findViewById(R.id.masktxt);
        //在布局结束后才做初始化操作
        previewView.getViewTreeObserver().addOnGlobalLayoutListener(this);

        faceRectView = findViewById(R.id.single_camera_face_rect_view);
        switchLivenessDetect = findViewById(R.id.single_camera_switch_liveness_detect);
        switchLivenessDetect.setChecked(livenessDetect);
        switchLivenessDetect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                livenessDetect = isChecked;
            }
        });
        RecyclerView recyclerShowFaceInfo = findViewById(R.id.single_camera_recycler_view_person);
        compareResultList = new ArrayList<>();
        adapter = new FaceSearchResultAdapter(compareResultList, this);
        recyclerShowFaceInfo.setAdapter(adapter);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int spanCount = (int) (dm.widthPixels / (getResources().getDisplayMetrics().density * 100 + 0.5f));
        recyclerShowFaceInfo.setLayoutManager(new GridLayoutManager(this, spanCount));
        recyclerShowFaceInfo.setItemAnimator(new DefaultItemAnimator());
    }

    // Converts to fahrenheit
    private float convertCelsiusToFahrenheit(float celsius) {
        return ((celsius * 9) / 5) + 32;
    }

    public void runTemperature(final Rect rect) {


        synchronized (obj) {

            new Thread(new Runnable() {
                @Override
                public void run() {

                    try {

                        long totalMilliSeconds1 = System.currentTimeMillis();

                        //temperatureUtil = new ThermalImageUtil(TemperatureActicity.this);
                        Context appcon = getApplicationContext();
                        temperatureUtil = new ThermalImageUtil(appcon);
                        Log.e("yw___15151515---", "TUTIL " + temperatureUtil);
                        TemperatureData temperatureData = com.common.tempkiosk.common.Application.getInstance().getTemperatureUtil()
                                .getDataAndBitmap(rect, 0, frames, new HotImageCallback.Stub() {//国际需要传入0
                                    @Override
                                    public void onTemperatureFail(String e) throws RemoteException {
                                        // TODO Auto-generated method stub
                                        //   Log.e("yw___15151515---", "onTemperatureFail " + e);
                                        //retry(tempretrynum);
                                        Isneedtem = true;// 获取温度失败  在onpreview 重试
                                    }



                                    @Override
                                    public void getTemperatureBimapData(final TemperatureBitmapData data) throws RemoteException {


                                        // TODO Auto-generated method stub
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (img_temp != null && data != null) {
                                                    img_temp.setVisibility(View.VISIBLE);
                                                    img_temp.setImageBitmap(data.getBitmap());
                                                }
                                            }
                                        });
                                    }
                                });

                        if (temperatureData != null) {
                            temTimer = new Timer();
                            temTimer.schedule(new TimerTask() {
                                public void run() {
                                    Isneedtem = true;
                                    runOnUiThread(new
                                                          Runnable() {
                                                              @Override
                                                              public void run() {
                                                                  if (tv_temperature!=null){
                                                                      tv_temp_detail.setVisibility(View.GONE);
                                                                      tv_temperature.setVisibility(View.GONE);
                                                                      img_temp.setVisibility(View.GONE);
                                                                      masktxt.setText(" ");
                                                                      //Intent i =new Intent(TemperatureActicity.this,MainActivity.class);
                                                                      //startActivity(i);
                                                                      if (mTextToSpeech != null) {
                                                                          mTextToSpeech.stop();
                                                                          mTextToSpeech.shutdown();
                                                                          mTextToSpeech = null;
                                                                      }

                                                                      if (cameraHelper != null) {
                                                                          cameraHelper.release();
                                                                          cameraHelper = null;
                                                                      }

                                                                      unInitEngine();
                                                                      if (getFeatureDelayedDisposables != null) {
                                                                          getFeatureDelayedDisposables.clear();
                                                                      }
                                                                      if (delayFaceTaskCompositeDisposable != null) {
                                                                          delayFaceTaskCompositeDisposable.clear();
                                                                      }
                                                                      if (faceHelper != null) {
                                                                          faceHelper.release();
                                                                          faceHelper = null;
                                                                      }

                                                                      FaceServer.getInstance().unInit();
                                                                      unInitEngine();
                                                                      temperatureUtil.release();
                                                                      //getApplicationContext().unbindService(connection);
                                                                      Timer Finishit = new Timer();
                                                                      Finishit.schedule(new TimerTask() {
                                                                          @Override
                                                                          public void run() {
                                                                              finish();
                                                                          }

                                                                      }, 0, 5000);
                                                                  }


                                                              }
                                                          });
                                }
                            }, 5 * 800); // was 1000





                            // Temperature
                            final float temperature = temperatureData.getTemperature();
                            final float fartemp = convertCelsiusToFahrenheit(temperature);

                            // Surfice Temperature
                            final float stemperature = temperatureData.getTemperatureNoCorrect();
                            final float stemp = convertCelsiusToFahrenheit(stemperature);

                            // Environment Temperature
                            final String etemperature = temperatureData.getMap().get("environmentTem");
                            final float etemp = convertCelsiusToFahrenheit(Float.parseFloat(etemperature));

                            // AMT Temperature
                            final String atemperature = temperatureData.getMap().get("AMT");
                            final float amtemp = convertCelsiusToFahrenheit(Float.parseFloat(atemperature));

                            long totalMilliSeconds2 = System.currentTimeMillis();

                            Log.e("测温时间："," "+(totalMilliSeconds2-totalMilliSeconds1));

                            final String surfaceTemperature = stemp + " F";
                            final String environmentTemperature = etemp + " F";
                            final String AMT = amtemp + " F";
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (tv_temp_detail != null) {
                                        tv_temp_detail.setVisibility(View.VISIBLE);
                                        tv_temp_detail.setTextColor(getResources().getColor(R.color.red));
                                        tv_temp_detail.setText("Surface Temperature: " + surfaceTemperature + "\nEnvironment Temperature: " + environmentTemperature
                                                + "\nAMT: " + AMT
                                        );
                                    }
                                }
                            });


                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (tv_temperature!=null){
                                        tv_temperature.setVisibility(View.VISIBLE);
                                        tv_temperature.setText(fartemp+" F");
                                        tv_temperature.setBackgroundResource(R.drawable.shape_red);
                                        globals.newtemp = globals.globaluid+"--"+fartemp;
                                    }
                                }
                            });


                        } else {
                            Isneedtem = true;
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("测温结果catch-", e.toString());
                        Isneedtem = true;
                        // retry(rect);
                    }

                }
            }).start();

        }
    }

    /**
     * 初始化引擎
     */
    private void initEngine() {
        ftEngine = new FaceEngine();
        ftInitCode = ftEngine.init(this, DetectMode.ASF_DETECT_MODE_VIDEO, ASF_OP_ALL_OUT,
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_DETECT | FaceEngine.ASF_FACE_SHELTER | FaceEngine.ASF_MASK_DETECT | FaceEngine.ASF_FACELANDMARK);
        int code = ftEngine.setFaceShelterParam(0.8f);
        Log.i(TAG, "initEngine:  setFaceShelterParam   " + code);

        frEngine = new FaceEngine();
        frInitCode = frEngine.init(this, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_RECOGNITION);

        flEngine = new FaceEngine();
        flInitCode = flEngine.init(this, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                16, MAX_DETECT_NUM, FaceEngine.ASF_LIVENESS);

        Log.i(TAG, "initEngine:  init: " + ftInitCode);

        if (ftInitCode != ErrorInfo.MOK) {
            String error = getString(R.string.specific_engine_init_failed, "ftEngine", ftInitCode);
            Log.i(TAG, "initEngine: " + error);
            showToast(error);
        }
        if (frInitCode != ErrorInfo.MOK) {
            String error = getString(R.string.specific_engine_init_failed, "frEngine", frInitCode);
            Log.i(TAG, "initEngine: " + error);
            showToast(error);
        }
        if (flInitCode != ErrorInfo.MOK) {
            String error = getString(R.string.specific_engine_init_failed, "flEngine", flInitCode);
            Log.i(TAG, "initEngine: " + error);
            showToast(error);
        }
    }

    /**
     * 销毁引擎，faceHelper中可能会有特征提取耗时操作仍在执行，加锁防止crash
     */
    private void unInitEngine() {
        if (ftInitCode == ErrorInfo.MOK && ftEngine != null) {
            synchronized (ftEngine) {
                int ftUnInitCode = ftEngine.unInit();
                Log.i(TAG, "unInitEngine: " + ftUnInitCode);
            }
        }
        if (frInitCode == ErrorInfo.MOK && frEngine != null) {
            synchronized (frEngine) {
                int frUnInitCode = frEngine.unInit();
                Log.i(TAG, "unInitEngine: " + frUnInitCode);
            }
        }
        if (flInitCode == ErrorInfo.MOK && flEngine != null) {
            synchronized (flEngine) {
                int flUnInitCode = flEngine.unInit();
                Log.i(TAG, "unInitEngine: " + flUnInitCode);
            }
        }
    }


    @Override
    protected void onDestroy() {

        if (mTextToSpeech != null) {
            mTextToSpeech.stop();
            mTextToSpeech.shutdown();
            mTextToSpeech = null;
        }

        if (cameraHelper != null) {
            cameraHelper.release();
            cameraHelper = null;
        }

        unInitEngine();
        if (getFeatureDelayedDisposables != null) {
            getFeatureDelayedDisposables.clear();
        }
        if (delayFaceTaskCompositeDisposable != null) {
            delayFaceTaskCompositeDisposable.clear();
        }
        if (faceHelper != null) {
            ConfigUtil.setTrackedFaceCount(this, faceHelper.getTrackedFaceCount());
            faceHelper.release();
            faceHelper = null;
        }

        FaceServer.getInstance().unInit();
        super.onDestroy();
    }

    private static final int FACE_SHELTER_CACHE_SIZE = 3;
    private Map<Integer, Integer> faceShelterCacheMap = new ConcurrentHashMap();
    private Map<Integer, Integer> faceShelterProcessCountMap = new ConcurrentHashMap();

    /**
     * 连续三帧为sheltered才当做sheltered
     *
     * @param trackId
     * @param shelter
     * @return
     */
    private int isSheltered(int trackId, int shelter) {
        int processCount = increaseAndGetValue(faceShelterProcessCountMap, trackId);

        if (shelter != FaceShelterInfo.SHELTERED) {
            shelter = FaceShelterInfo.NOT_SHELTERED;
        }
        // 0b111 表示连续三帧是1才是true
        int requiredContinueSheltered = 0b111;
        Integer shelterCache = faceShelterCacheMap.get(trackId);
        if (shelterCache == null) {
            shelterCache = 0;
        }
        shelterCache <<= 1;
        shelterCache |= shelter;
        shelterCache &= requiredContinueSheltered;


        boolean sheltered = processCount > 3 && (shelterCache == requiredContinueSheltered);

        faceShelterCacheMap.put(trackId, shelterCache);

        int ret = processCount > 3 ? (sheltered ? FaceShelterInfo.SHELTERED : FaceShelterInfo.NOT_SHELTERED) : FaceShelterInfo.UNKNOWN;
        Log.i(TAG, "isSheltered: " + (processCount > 3));

        faceShelterCacheMap.put(trackId, shelterCache);
        Log.i(TAG, "isSheltered: ret = " + ret + " , shelter = " + shelter);
        return ret;
    }

    private void initCamera() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        final FaceListener faceListener = new FaceListener() {
            @Override
            public void onFail(Exception e) {
                Log.e(TAG, "onFail: " + e.getMessage());
            }

            //请求FR的回调
            @Override
            public void onFaceFeatureInfoGet(@Nullable final FaceFeature faceFeature, final Integer requestId, final Integer errorCode) {
                //FR成功
                if (faceFeature != null) {
//                    Log.i(TAG, "onPreview: fr end = " + System.currentTimeMillis() + " trackId = " + requestId);
                    Integer liveness = livenessMap.get(requestId);
                    //不做活体检测的情况，直接搜索
                    if (!livenessDetect) {
                        searchFace(faceFeature, requestId);
                    }
                    //活体检测通过，搜索特征
                    else if (liveness != null && liveness == LivenessInfo.ALIVE) {
                        searchFace(faceFeature, requestId);
                    }
                    //活体检测未出结果，或者非活体，延迟执行该函数
                    else {
                        if (requestFeatureStatusMap.containsKey(requestId)) {
                            Observable.timer(WAIT_LIVENESS_INTERVAL, TimeUnit.MILLISECONDS)
                                    .subscribe(new Observer<Long>() {
                                        Disposable disposable;

                                        @Override
                                        public void onSubscribe(Disposable d) {
                                            disposable = d;
                                            getFeatureDelayedDisposables.add(disposable);
                                        }

                                        @Override
                                        public void onNext(Long aLong) {
                                            onFaceFeatureInfoGet(faceFeature, requestId, errorCode);
                                        }

                                        @Override
                                        public void onError(Throwable e) {

                                        }

                                        @Override
                                        public void onComplete() {
                                            getFeatureDelayedDisposables.remove(disposable);
                                        }
                                    });
                        }
                    }

                }
                //特征提取失败
                else {
                    if (increaseAndGetValue(extractErrorRetryMap, requestId) > MAX_RETRY_TIME) {
                        extractErrorRetryMap.put(requestId, 0);

                        String msg;
                        // 传入的FaceInfo在指定的图像上无法解析人脸，此处使用的是RGB人脸数据，一般是人脸模糊
                        if (errorCode != null && errorCode == ErrorInfo.MERR_FSDK_FACEFEATURE_LOW_CONFIDENCE_LEVEL) {
                            msg = getString(R.string.low_confidence_level);
                        } else {
                            msg = "ExtractCode:" + errorCode;
                        }
                        faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, msg));
                        // 在尝试最大次数后，特征提取仍然失败，则认为识别未通过
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                        retryRecognizeDelayed(requestId);
                    } else {
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.TO_RETRY);
                    }
                }
            }

            @Override
            public void onFaceLivenessInfoGet(@Nullable LivenessInfo livenessInfo, final Integer requestId, Integer errorCode) {
                if (livenessInfo != null) {
                    int liveness = livenessInfo.getLiveness();
                    livenessMap.put(requestId, liveness);
                    // 非活体，重试
                    if (liveness == LivenessInfo.NOT_ALIVE) {
                        faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, "NOT_ALIVE"));
                        // 延迟 FAIL_RETRY_INTERVAL 后，将该人脸状态置为UNKNOWN，帧回调处理时会重新进行活体检测
                        retryLivenessDetectDelayed(requestId);
                    }
                } else {
                    if (increaseAndGetValue(livenessErrorRetryMap, requestId) > MAX_RETRY_TIME) {
                        livenessErrorRetryMap.put(requestId, 0);
                        String msg;
                        // 传入的FaceInfo在指定的图像上无法解析人脸，此处使用的是RGB人脸数据，一般是人脸模糊
                        if (errorCode != null && errorCode == ErrorInfo.MERR_FSDK_FACEFEATURE_LOW_CONFIDENCE_LEVEL) {
                            msg = getString(R.string.low_confidence_level);
                        } else {
                            msg = "ProcessCode:" + errorCode;
                        }
                        faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, msg));
                        retryLivenessDetectDelayed(requestId);
                    } else {
                        livenessMap.put(requestId, LivenessInfo.UNKNOWN);
                    }
                }
            }


        };


        CameraListener cameraListener = new CameraListener() {
            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                Camera.Size lastPreviewSize = previewSize;
                previewSize = camera.getParameters().getPreviewSize();
                drawHelper = new DrawHelper(previewSize.width, previewSize.height, previewView.getWidth(), previewView.getHeight(), displayOrientation
                        , cameraId, isMirror, true, false);
                Log.i(TAG, "onCameraOpened: " + drawHelper.toString());
                // 切换相机的时候可能会导致预览尺寸发生变化
                if (faceHelper == null ||
                        lastPreviewSize == null ||
                        lastPreviewSize.width != previewSize.width || lastPreviewSize.height != previewSize.height) {
                    Integer trackedFaceCount = null;
                    // 记录切换时的人脸序号
                    if (faceHelper != null) {
                        trackedFaceCount = faceHelper.getTrackedFaceCount();
                        faceHelper.release();
                    }
                    faceHelper = new FaceHelper.Builder()
                            .ftEngine(ftEngine)
                            .frEngine(frEngine)
                            .flEngine(flEngine)
                            .frQueueSize(MAX_DETECT_NUM)
                            .flQueueSize(MAX_DETECT_NUM)
                            .previewSize(previewSize)
                            .faceListener(faceListener)
                            .trackedFaceCount(trackedFaceCount == null ? ConfigUtil.getTrackedFaceCount(TemperatureActicity.this.getApplicationContext()) : trackedFaceCount)
                            .build();
                }
            }


            @Override
            public void onPreview(final byte[] nv21, Camera camera) {
                if (faceRectView != null) {
                    faceRectView.clearFaceInfo();
                }
                List<FacePreviewInfo> facePreviewInfoList = faceHelper.onPreviewFrame(nv21);
                if (facePreviewInfoList != null && faceRectView != null  && drawHelper != null) {
                    drawPreviewInfo(facePreviewInfoList);
                }
                registerFace(nv21, facePreviewInfoList);
                clearLeftFace(facePreviewInfoList);

                if (facePreviewInfoList != null && facePreviewInfoList.size() > 0 && previewSize != null) {
                    //if (previewSize != null) {
                    for (int i = 0; i < facePreviewInfoList.size(); i++) {
                        int trackId = facePreviewInfoList.get(i).getTrackId();
                        Integer status = requestFeatureStatusMap.get(trackId);
                        if (status != null && status == RequestFeatureStatus.SUCCEED) {
                            continue;
                        }
                        int mask = facePreviewInfoList.get(i).getMask();
                        int faceShelter = isSheltered(facePreviewInfoList.get(i).getTrackId(), facePreviewInfoList.get(i).getFaceShelter());// ? FaceShelterInfo.SHELTERED : FaceShelterInfo.NOT_SHELTERED;


                        //Log.e("mask","mask:"+mask+"   faceShelter:"+faceShelter);

                        //
                        Rect originRect = facePreviewInfoList.get(i).getFaceInfo().getRect();//人脸框坐标
                        //额头坐标  上1/4   左右中间2/1
                        //Log.e("yw_rect",originRect.toString());
                        //Log.e("yw_dis",""+15.00000 / (((originRect.right - originRect.left)*1.000000) /(previewSize.width*1.00000)) );

                        if ((originRect.right - originRect.left) != 0) {



                            if (15.00000 / (((originRect.right - originRect.left)*1.000000) /(previewSize.width*1.00000)) > 70) {
                                distancestatus = 2;

                                if (mTextToSpeech != null && !mTextToSpeech.isSpeaking() && !Istts1) {
                                    Istts1 = true;
                                    mTextToSpeech.speak("Please move closer", TextToSpeech.QUEUE_ADD, null, "near");
                                    ttstimer1 = new Timer();
                                    ttstimer1.schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            Istts1 = false;
                                        }
                                    }, 6 * 1000);
                                }

                            } else if (15.00000 / (((originRect.right - originRect.left)*1.000000) /(previewSize.width*1.00000)) <35) {
                                distancestatus = 1;
                                if (mTextToSpeech != null && !mTextToSpeech.isSpeaking() && !Istts2) {
                                    Istts2 = true;
                                    mTextToSpeech.speak("Please move a bit further", TextToSpeech.QUEUE_ADD, null, "far");
                                    ttstimer2 = new Timer();
                                    ttstimer2.schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            Istts2 = false;
                                        }
                                    }, 6 * 1000);
                                }
                            } else {
                                distancestatus = 0;
                            }
                        }

                        //距离在合适值  才启动人脸回调
                        if (distancestatus != 0) {
                            continue;
                        }

                        //测温额头补偿定位

                        faceid = trackId;
                        if (faceid == faceid2) {
                            //Log.e("yw", "same person  " + trackId);
                            Isnewperson = false;
                        } else {
                            //Log.e("yw", "new person  " + trackId);
                            Isnewperson = true;
                            faceid2 = trackId;
                        }


                        newRect = isOffset(trackId, originRect);

                        //额头坐标
                        Rect Foreheadrect=new Rect();
                        Foreheadrect.left=originRect.left+(originRect.right-originRect.left)/4;
                        Foreheadrect.right=originRect.left+(originRect.right-originRect.left)/4*3;
                        Foreheadrect.top=originRect.top;
                        Foreheadrect.bottom=originRect.top+(originRect.bottom-originRect.top)/4;


                        if (newRect != null) {

                            if (Isnewperson) {

                                Isneedtem = true;

                                //  Log.e("yw_person", "new person  111   " + trackId);
                                  /*  if (livenessDetect){
                                        if (livenessMap.get(trackId)!=null&&livenessMap.get(trackId)==1){
                                            runTemperature(newRect, trackId);
                                        }

                                    }else {*/
                                runTemperature(newRect);
                                // }


                            } else {

                                if (Isneedtem) {
                                    Isneedtem = false;

                                    runTemperature(newRect);

                                }
                            }



                        } else {

                            // Log.e("yw_person", "rect  null  111   " + trackId);
                            if (mTextToSpeech != null && !mTextToSpeech.isSpeaking() && !Istts3) {
                                Istts3 = true;
                                mTextToSpeech.speak("Please face the screen", TextToSpeech.QUEUE_ADD, null, "far");
                                ttstimer3 = new Timer();
                                ttstimer3.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        Istts3 = false;
                                    }
                                }, 6 * 1000);
                            }

                            continue;
                        }



                        int combined = (mask << 1) | faceShelter;
                        //Log.i(TAG, "onPreview mask: " + mask + "  " + faceShelter);
                        // *
                        //* 口罩	遮挡	提示语
                        // * 0	0	请佩戴口罩
                        // * 0	1	请佩戴口罩
                        //  * 1	0	请正确佩戴口罩
                        //  * 1	1	佩戴正确，可识别

                        boolean canAnalyze = false;
                        switch (combined) {
                            case 0:
                            case 1:
                                faceHelper.setName(trackId, "Please wear a mask");
                                masktxt.setText("Please wear a mask");
                                canAnalyze = true;
                                break;
                            case 0b10:
                                faceHelper.setName(trackId, "Please wear the mask correctly");
                                masktxt.setText("Please wear the mask correctly");
                                break;
                            case 0b11:
                                canAnalyze = true;
                                faceHelper.setName(trackId, "Mask worn");
                                masktxt.setText("Thank you for wearing a mask");
                                break;
                            default:
                                break;
                        }
                        if (!canAnalyze) {
                            continue;
                        }
                        /**
                         * 在活体检测开启，在人脸识别状态不为成功或人脸活体状态不为处理中（ANALYZING）且不为处理完成（ALIVE、NOT_ALIVE）时重新进行活体检测
                         */
                        if (livenessDetect && (status == null || status != RequestFeatureStatus.SUCCEED)) {
                            Integer liveness = livenessMap.get(facePreviewInfoList.get(i).getTrackId());
                            if (liveness == null
                                    || (liveness != LivenessInfo.ALIVE && liveness != LivenessInfo.NOT_ALIVE && liveness != RequestLivenessStatus.ANALYZING)) {
                                livenessMap.put(facePreviewInfoList.get(i).getTrackId(), RequestLivenessStatus.ANALYZING);
                                faceHelper.requestFaceLiveness(nv21, facePreviewInfoList.get(i).getFaceInfo(), previewSize.width, previewSize.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList.get(i).getTrackId(), LivenessType.RGB);
                            }
                        }
                        /**
                         * 对于每个人脸，若状态为空或者为失败，则请求特征提取（可根据需要添加其他判断以限制特征提取次数），
                         * 特征提取回传的人脸特征结果在{@link FaceListener#onFaceFeatureInfoGet(FaceFeature, Integer, Integer)}中回传
                         */
                        if (status == null
                                || status == RequestFeatureStatus.TO_RETRY) {
                            requestFeatureStatusMap.put(facePreviewInfoList.get(i).getTrackId(), RequestFeatureStatus.SEARCHING);
                            faceHelper.requestFaceFeature(nv21, facePreviewInfoList.get(i).getFaceInfo(), facePreviewInfoList.get(i).getMask(),
                                    previewSize.width, previewSize.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList.get(i).getTrackId());
                            //    Log.i(TAG, "onPreview: fr start = " + System.currentTimeMillis() + " trackId = " + facePreviewInfoList.get(i).getTrackedFaceCount());
                        }
                    }
                }
            }

            @Override
            public void onCameraClosed() {
                Log.i(TAG, "onCameraClosed: ");
            }

            @Override
            public void onCameraError(Exception e) {
                Log.i(TAG, "onCameraError: " + e.getMessage());
            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
                if (drawHelper != null) {
                    drawHelper.setCameraDisplayOrientation(displayOrientation);
                }
                Log.i(TAG, "onCameraConfigurationChanged: " + cameraID + "  " + displayOrientation);
            }
        };

        cameraHelper = new CameraHelper.Builder()
                .previewViewSize(new Point(previewView.getMeasuredWidth(), previewView.getMeasuredHeight()))
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .specificCameraId(rgbCameraID != null ? rgbCameraID : Camera.CameraInfo.CAMERA_FACING_FRONT)
                .isMirror(false)
                .previewOn(previewView)
                .cameraListener(cameraListener)
                .build();
        cameraHelper.init();
        cameraHelper.start();
    }

    private void registerFace(final byte[] nv21, final List<FacePreviewInfo> facePreviewInfoList) {
        if (registerStatus == REGISTER_STATUS_READY && facePreviewInfoList != null && facePreviewInfoList.size() > 0) {
            registerStatus = REGISTER_STATUS_PROCESSING;
            Observable.create(new ObservableOnSubscribe<Boolean>() {
                @Override
                public void subscribe(ObservableEmitter<Boolean> emitter) {

                    boolean success = FaceServer.getInstance().registerNv21(TemperatureActicity.this,
                            nv21.clone(), previewSize.width, previewSize.height,
                            facePreviewInfoList.get(0).getFaceInfo(),
                            facePreviewInfoList.get(0).getMask() & facePreviewInfoList.get(0).getFaceShelter(),
                            "registered " + faceHelper.getTrackedFaceCount());
                    emitter.onNext(success);
                }
            })
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Boolean>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(Boolean success) {
                            String result = success ? "register success!" : "register failed!";
                            showToast(result);
                            registerStatus = REGISTER_STATUS_DONE;
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            showToast("register failed!");
                            registerStatus = REGISTER_STATUS_DONE;
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }

    private void drawPreviewInfo(List<FacePreviewInfo> facePreviewInfoList) {
        List<DrawInfo> drawInfoList = new ArrayList<>();
        List<PointF[]> drawLandmarkInfo = new ArrayList<>();
        for (int i = 0; i < facePreviewInfoList.size(); i++) {
            //String name = faceHelper.getName(facePreviewInfoList.get(i).getTrackId());
            String name = " ";
            Integer liveness = livenessMap.get(facePreviewInfoList.get(i).getTrackId());
            Integer recognizeStatus = requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId());

            int sheltered = isSheltered(facePreviewInfoList.get(i).getTrackId(), facePreviewInfoList.get(i).getFaceShelter());
            // 根据识别结果和活体结果设置颜色
            int color = RecognizeColor.COLOR_UNKNOWN;
            color = sheltered == FaceShelterInfo.NOT_SHELTERED || facePreviewInfoList.get(i).getMask() == MaskInfo.NOT_WORN ? Color.RED : Color.YELLOW;
            if (recognizeStatus != null && recognizeStatus == RequestFeatureStatus.SUCCEED) {
                color = RecognizeColor.COLOR_SUCCESS;
            } else {
                //Button rface = findViewById(R.id.rface);
                //rface.performClick();
            }
//                if (recognizeStatus == RequestFeatureStatus.FAILED) {
//                    color = RecognizeColor.COLOR_FAILED;
//                }
//                if (recognizeStatus == RequestFeatureStatus.SUCCEED) {
//                    color = RecognizeColor.COLOR_SUCCESS;
//                }
//            }
//            if (liveness != null && liveness == LivenessInfo.NOT_ALIVE) {
//                color = RecognizeColor.COLOR_FAILED;
//            }

            drawInfoList.add(new DrawInfo(drawHelper.adjustRect(facePreviewInfoList.get(i).getFaceInfo().getRect()),
                    GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE,
                    liveness == null ? LivenessInfo.UNKNOWN : liveness, color,
                    name == null ? String.valueOf(facePreviewInfoList.get(i).getTrackId()) : name));
            drawLandmarkInfo.add(drawHelper.adjustPoint(facePreviewInfoList.get(i).getLandmarkInfo().getLandmarks()));
        }
        drawHelper.drawPreviewInfo(faceRectView, drawInfoList);
    }

    @Override
    protected void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            if (isAllGranted) {
                initEngine();
                initCamera();
            } else {
                showToast(getString(R.string.permission_denied));
            }
        }
    }

    /**
     * 删除已经离开的人脸
     *
     * @param facePreviewInfoList 人脸和trackId列表
     */
    private void clearLeftFace(List<FacePreviewInfo> facePreviewInfoList) {
        if (compareResultList != null) {
            for (int i = compareResultList.size() - 1; i >= 0; i--) {
                if (!requestFeatureStatusMap.containsKey(compareResultList.get(i).getTrackId())) {
                    compareResultList.remove(i);
                    adapter.notifyItemRemoved(i);
                }
            }
        }
        if (facePreviewInfoList == null || facePreviewInfoList.size() == 0) {
            requestFeatureStatusMap.clear();
            livenessMap.clear();
            livenessErrorRetryMap.clear();
            extractErrorRetryMap.clear();
            faceShelterProcessCountMap.clear();
            faceShelterCacheMap.clear();
            if (getFeatureDelayedDisposables != null) {
                getFeatureDelayedDisposables.clear();
            }
            return;
        }
        Enumeration<Integer> keys = requestFeatureStatusMap.keys();
        while (keys.hasMoreElements()) {
            int key = keys.nextElement();
            boolean contained = false;
            for (FacePreviewInfo facePreviewInfo : facePreviewInfoList) {
                if (facePreviewInfo.getTrackId() == key) {
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                requestFeatureStatusMap.remove(key);
                livenessMap.remove(key);
                livenessErrorRetryMap.remove(key);
                extractErrorRetryMap.remove(key);
                faceShelterProcessCountMap.remove(key);
                faceShelterCacheMap.remove(key);
            }
        }


    }

    private void searchFace(final FaceFeature frFace, final Integer requestId) {
        Observable
                .create(new ObservableOnSubscribe<CompareResult>() {
                    @Override
                    public void subscribe(ObservableEmitter<CompareResult> emitter) {
                        Log.i(TAG, "subscribe: fr search start = " + System.currentTimeMillis() + " trackId = " + requestId);
                        CompareResult compareResult = FaceServer.getInstance().getTopOfFaceLib(frFace);
                        Log.i(TAG, "subscribe: fr search end = " + System.currentTimeMillis() + " trackId = " + requestId);
                        emitter.onNext(compareResult);

                    }
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<CompareResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(CompareResult compareResult) {

                        //   Log.e("yw","开始搜索"+ "  similar = " + compareResult.getSimilar());
                        if (compareResult == null || compareResult.getUserName() == null) {
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                            faceHelper.setName(requestId, "VISITOR " + requestId);
                            return;
                        }
                        // Log.i(TAG, "onNext: fr search get result  = " + System.currentTimeMillis() + " trackId = " + requestId + "  similar = " + compareResult.getSimilar());
                        if (compareResult.getSimilar() > SIMILAR_THRESHOLD) {
                            boolean isAdded = false;
                            if (compareResultList == null) {
                                requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                                //faceHelper.setName(requestId, "VISITOR " + requestId);
                                globals.newtrackid = globals.globaluid+"--"+requestId;
                                return;
                            }
                            for (CompareResult compareResult1 : compareResultList) {
                                if (compareResult1.getTrackId() == requestId) {
                                    isAdded = true;
                                    globals.newtrackid = globals.globaluid+"--"+requestId;
                                    break;
                                }
                            }
                            if (!isAdded) {
                                //对于多人脸搜索，假如最大显示数量为 MAX_DETECT_NUM 且有新的人脸进入，则以队列的形式移除
                                if (compareResultList.size() >= MAX_DETECT_NUM) {
                                    compareResultList.remove(0);
                                    adapter.notifyItemRemoved(0);
                                }
                                //添加显示人员时，保存其trackId
                                globals.newtrackid = globals.globaluid+"--"+requestId;
                                compareResult.setTrackId(requestId);
                                compareResultList.add(compareResult);
                                adapter.notifyItemInserted(compareResultList.size() - 1);
                            }
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.SUCCEED);
                            globals.newtrackid = globals.globaluid+"--"+requestId;
                            //faceHelper.setName(requestId, "Recognition pass");
                            Log.v(TAG,"VISITOR RECOGNIZED!");
                        } else {
                            //faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, "NOT_REGISTERED"));
                            Log.v(TAG,"VISITOR NOT REGISTERED!");
                            Button rface = findViewById(R.id.rface);
                            rface.performClick();
                            retryRecognizeDelayed(requestId);
                            globals.newtrackid = globals.globaluid+"--"+requestId;
                        }
                    }
                    /*                  public void onNext(CompareResult compareResult) {
                                          if (compareResult == null || compareResult.getUserName() == null) {
                                              requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                                              faceHelper.setName(requestId, "VISITOR " + requestId);
                                              return;
                                          }
                  //                        Log.i(TAG, "onNext: fr search get result  = " + System.currentTimeMillis() + " trackId = " + requestId + "  similar = " + compareResult.getSimilar());
                                          if (compareResult.getSimilar() > SIMILAR_THRESHOLD) {
                                              boolean isAdded = false;
                                              if (compareResultList == null) {
                                                  requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                                                  faceHelper.setName(requestId, "VISITOR " + requestId);
                                                  Log.v(TAG,"VISITOR " + requestId);
                                                  return;
                                              }
                                              for (CompareResult compareResult1 : compareResultList) {
                                                  if (compareResult1.getTrackId() == requestId) {
                                                      isAdded = true;
                                                      Log.v(TAG,"VISITOR WAS ADDED " + requestId);
                                                      break;
                                                  }
                                              }
                                              if (!isAdded) {
                                                  //对于多人脸搜索，假如最大显示数量为 MAX_DETECT_NUM 且有新的人脸进入，则以队列的形式移除
                                                  if (compareResultList.size() >= MAX_DETECT_NUM) {
                                                      compareResultList.remove(0);
                                                      adapter.notifyItemRemoved(0);
                                                      Log.v(TAG,"VISITOR NOT ADDED 1");
                                                  }
                                                  //添加显示人员时，保存其trackId
                                                  compareResult.setTrackId(requestId);
                                                  compareResultList.add(compareResult);
                                                  adapter.notifyItemInserted(compareResultList.size() - 1);
                                                  Log.v(TAG,"VISITOR NOT ADDED 2");
                                              }
                                              requestFeatureStatusMap.put(requestId, RequestFeatureStatus.SUCCEED);
                                              faceHelper.setName(requestId, "Identified successfully");
                                              Log.v(TAG,"Identified successfully");
                                          } else {
                                              registerStatus = REGISTER_STATUS_READY;
                                              Log.v(TAG,"REGISTER_STATUS_READY");
                                              faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, "NOT_REGISTERED"));
                                              retryRecognizeDelayed(requestId);
                                          }
                                      }
                  */
                    @Override
                    public void onError(Throwable e) {
                        //faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, "NOT_REGISTERED"));
                        Button rface = findViewById(R.id.rface);
                        rface.performClick();
                        retryRecognizeDelayed(requestId);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


    /**
     * 将准备注册的状态置为{@link #REGISTER_STATUS_READY}
     *
     * @param view 注册按钮
     */
    public void register(View view) {
        if (registerStatus == REGISTER_STATUS_DONE) {
            registerStatus = REGISTER_STATUS_READY;
        }
    }

    /**
     * 切换相机。注意：若切换相机发现检测不到人脸，则极有可能是检测角度导致的，需要销毁引擎重新创建或者在设置界面修改配置的检测角度
     *
     * @param view
     */
    public void switchCamera(View view) {
        if (cameraHelper != null) {
            boolean success = cameraHelper.switchCamera();
            if (!success) {
                showToast(getString(R.string.switch_camera_failed));
            } else {
                showLongToast(getString(R.string.notice_change_detect_degree));
            }
        }
    }

    /**
     * 在{@link #previewView}第一次布局完成后，去除该监听，并且进行引擎和相机的初始化
     */
    @Override
    public void onGlobalLayout() {
        previewView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
        } else {
            initEngine();
            initCamera();
        }
    }

    /**
     * 将map中key对应的value增1回传
     *
     * @param countMap map
     * @param key      key
     * @return 增1后的value
     */
    public int increaseAndGetValue(Map<Integer, Integer> countMap, int key) {
        if (countMap == null) {
            return 0;
        }
        Integer value = countMap.get(key);
        if (value == null) {
            value = 0;
        }
        countMap.put(key, ++value);
        return value;
    }

    /**
     * 延迟 FAIL_RETRY_INTERVAL 重新进行活体检测
     *
     * @param requestId 人脸ID
     */
    private void retryLivenessDetectDelayed(final Integer requestId) {
        Observable.timer(FAIL_RETRY_INTERVAL, TimeUnit.MILLISECONDS)
                .subscribe(new Observer<Long>() {
                    Disposable disposable;

                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                        delayFaceTaskCompositeDisposable.add(disposable);
                    }

                    @Override
                    public void onNext(Long aLong) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        // 将该人脸状态置为UNKNOWN，帧回调处理时会重新进行活体检测
                        if (livenessDetect) {
                            faceHelper.setName(requestId, Integer.toString(requestId));
                        }
                        livenessMap.put(requestId, LivenessInfo.UNKNOWN);
                        delayFaceTaskCompositeDisposable.remove(disposable);
                    }
                });
    }

    /**
     * 延迟 FAIL_RETRY_INTERVAL 重新进行人脸识别
     *
     * @param requestId 人脸ID
     */
    private void retryRecognizeDelayed(final Integer requestId) {
        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
        Observable.timer(FAIL_RETRY_INTERVAL, TimeUnit.MILLISECONDS)
                .subscribe(new Observer<Long>() {
                    Disposable disposable;

                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                        delayFaceTaskCompositeDisposable.add(disposable);
                    }

                    @Override
                    public void onNext(Long aLong) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        // 将该人脸特征提取状态置为FAILED，帧回调处理时会重新进行活体检测
                        faceHelper.setName(requestId, Integer.toString(requestId));
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.TO_RETRY);
                        delayFaceTaskCompositeDisposable.remove(disposable);
                    }
                });
    }
    @Override
    protected void onResume() {
        super.onResume();
        resumeCamera();
    }

    private void resumeCamera() {
        if (cameraHelper != null) {
            cameraHelper.start();
        }
    }

    @Override
    protected void onPause() {
        pauseCamera();
        super.onPause();
    }

    private void pauseCamera() {
        if (cameraHelper != null) {
            cameraHelper.stop();
        }
    }


    //检测应用是否安装
    private boolean isInstalled(Context context, String packageName) {
        final PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        for (int i = 0; i < pinfo.size(); i++) {
            if (pinfo.get(i).packageName.equalsIgnoreCase(packageName))
                return true;
        }
        return false;
    }

    private Map<Integer, Integer> faceOffsetCacheMap = new ConcurrentHashMap();
    /**
     * 连续三帧位置偏移才当成是偏移 ，其中人脸位置校准时拟定中心最大偏移值为50
     *
     * @param trackId
     * @param rect
     * @return 位置偏移则返回null ,位置正确对应的热成像图rect
     */
    private Rect isOffset(int trackId, Rect rect) {
        Rect rect2 = drawHelper.adjustRect(rect);
        // Log.e(TAG, "rect2 :" + rect2);
        //Rect(352, 480 - 643, 742)
        float horizontalOffset = (rect2.left + rect2.right)  / 2.0f - previewView.getWidth()  / 2.0f;
        float verticalOffset = (rect2.top + rect2.bottom)  / 2.0f - previewView.getHeight()  / 2.0f;


        int processCount = increaseAndGetValue(faceOffsetProcessCountMap, trackId);
        int continueOffset = 0b111;//连续三帧

        //中心最大偏移值设为50

        int offset = Math.abs(horizontalOffset) <= 100 && Math.abs(verticalOffset) <= 100 ? 1 : 0;
        Integer offsetCache = faceOffsetCacheMap.get(trackId);
        if (offsetCache == null) {
            offsetCache = 0;
        }
        offsetCache <<= 1;
        offsetCache |= offset;
        offsetCache &= continueOffset;
        boolean isOffset = processCount > 3 && (offsetCache == continueOffset);
        faceOffsetCacheMap.put(trackId, offsetCache);

        Rect newRect = null;
        Log.v(TAG,"-------------------------- Module " + moudle + " -------------------------");
        if(isOffset){
            if (moudle==9){
                newRect = new Rect(5,12,11,20);
                //热成像图y轴向右，x轴向下,根据偏移系数对中心rect做对应的偏离补偿
                int horizontalOffset2 = (int)((horizontalOffset)/15);
                int verticalOffset2 = (int)((verticalOffset)/20);
                newRect.top  += horizontalOffset2;
                newRect.bottom  += horizontalOffset2;
                newRect.left  += verticalOffset2;
                newRect.right  += verticalOffset2;
            }else if (moudle==15){
                newRect = new Rect(12,12,20,20);
                //热成像图y轴向右，x轴向下,根据偏移系数对中心rect做对应的偏离补偿
                int horizontalOffset2 = (int)((horizontalOffset)/20);
                int verticalOffset2 = (int)((verticalOffset)/20);
                newRect.top  += horizontalOffset2;
                newRect.bottom  += horizontalOffset2;
                newRect.left  += verticalOffset2;
                newRect.right  += verticalOffset2;
            }else if (moudle==8){//兼容高通 950T  TX14
                newRect = new Rect(4,13,9,19);
                //热成像图y轴向右，x轴向下,根据偏移系数对中心rect做对应的偏离补偿
                int horizontalOffset2 = (int)((horizontalOffset)/40);
                int verticalOffset2 = (int)((verticalOffset)/40);

                newRect.top  -= horizontalOffset2;
                newRect.bottom  -= horizontalOffset2;
                newRect.left  += verticalOffset2;
                newRect.right  += verticalOffset2;
            }else if (moudle==21){//980T 21
                newRect = new Rect(9,12,17,20);
                //热成像图y轴向右，x轴向下,根据偏移系数对中心rect做对应的偏离补偿
                int horizontalOffset2 = (int)((horizontalOffset)/10);
                int verticalOffset2 = (int)((verticalOffset)/10);


                newRect.top  += horizontalOffset2;
                newRect.bottom  += horizontalOffset2;
                newRect.left  += verticalOffset2;
                newRect.right  += verticalOffset2;

            }else {//16
                newRect = new Rect(9,12,17,20);
                //热成像图y轴向右，x轴向下,根据偏移系数对中心rect做对应的偏离补偿
                int horizontalOffset2 = (int)((horizontalOffset)/10);
                int verticalOffset2 = (int)((verticalOffset)/10);


                newRect.top  += horizontalOffset2;
                newRect.bottom  += horizontalOffset2;
                newRect.left  += verticalOffset2;
                newRect.right  += verticalOffset2;
            }

        }
        return newRect;
    }
}