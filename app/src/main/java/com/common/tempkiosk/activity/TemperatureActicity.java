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
    private TextToSpeech mTextToSpeech; //TTS ??????
    private boolean Isneedtem = true;
    ThermalImageUtil temperatureUtil;

    private boolean Istts1 = false;
    private boolean Istts2 = false;
    private boolean Istts3 = false;
    private boolean IsNeedface = true;
    Timer  ttstimer1,temTimer,ttstimer2, ttstimer3;//TTS??????timer

    private Map<Integer, Integer> faceOffsetProcessCountMap = new ConcurrentHashMap();

    int faceid = -1;
    int faceid2 = -1;
    boolean Isnewperson = false;
    Rect newRect;
    Rect NewRectx;

    private int distancestatus = -1;// -1 ???????????? ???0 ?????????  1??????  2 ??????

    ImageView img_temp;
    TextView  tv_temp_detail,tv_temperature,masktxt;

    private static final int MAX_DETECT_NUM = 10;
    /**
     * ???FR??????????????????????????????FR?????????????????????
     */
    private static final int WAIT_LIVENESS_INTERVAL = 100;
    /**
     * ???????????????????????????ms???
     */
    private static final long FAIL_RETRY_INTERVAL = 1000;
    /**
     * ????????????????????????
     */
    private static final int MAX_RETRY_TIME = 3;

    private CameraHelper cameraHelper;
    private DrawHelper drawHelper;
    private Camera.Size previewSize;
    /**
     * ??????????????????????????????????????????????????????RGB??????????????????????????????????????????
     */
    private Integer rgbCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;

    /**
     * VIDEO??????????????????????????????????????????????????????
     */
    private FaceEngine ftEngine;
    /**
     * ???????????????????????????
     */
    private FaceEngine frEngine;
    /**
     * IMAGE????????????????????????????????????????????????????????????
     */
    private FaceEngine flEngine;

    private int ftInitCode = -1;
    private int frInitCode = -1;
    private int flInitCode = -1;
    private FaceHelper faceHelper;
    private List<CompareResult> compareResultList;
    private FaceSearchResultAdapter adapter;
    /**
     * ?????????????????????
     */
    private boolean livenessDetect = true;
    /**
     * ????????????????????????????????????
     */
    private static final int REGISTER_STATUS_READY = 0;
    /**
     * ?????????????????????????????????
     */
    private static final int REGISTER_STATUS_PROCESSING = 1;
    /**
     * ????????????????????????????????????????????????????????????
     */
    private static final int REGISTER_STATUS_DONE = 2;

    private int registerStatus = REGISTER_STATUS_DONE;
    /**
     * ????????????????????????????????????
     */
    private ConcurrentHashMap<Integer, Integer> requestFeatureStatusMap = new ConcurrentHashMap<>();
    /**
     * ????????????????????????????????????????????????
     */
    private ConcurrentHashMap<Integer, Integer> extractErrorRetryMap = new ConcurrentHashMap<>();
    /**
     * ?????????????????????
     */
    private ConcurrentHashMap<Integer, Integer> livenessMap = new ConcurrentHashMap<>();
    /**
     * ??????????????????????????????????????????
     */
    private ConcurrentHashMap<Integer, Integer> livenessErrorRetryMap = new ConcurrentHashMap<>();

    private CompositeDisposable getFeatureDelayedDisposables = new CompositeDisposable();
    private CompositeDisposable delayFaceTaskCompositeDisposable = new CompositeDisposable();
    /**
     * ????????????????????????????????????SurfaceView???TextureView
     */
    private View previewView;
    /**
     * ????????????????????????
     */
    private FaceRectView faceRectView;

    private Switch switchLivenessDetect;

    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    /**
     * ????????????
     */
    private static final float SIMILAR_THRESHOLD = 0.7f;
    /**
     * ???????????????????????????
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
        //????????????
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            getWindow().setAttributes(attributes);
        }

        // Activity???????????????????????????????????????
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        //????????????????????????
        FaceServer.getInstance().init(this);

        mTextToSpeech = new TextToSpeech(TemperatureActicity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    /*
                     */
                    // setLanguage????????????
                    //int result = mTextToSpeech.setLanguage(Locale.CHINESE);
                    int result = mTextToSpeech.setLanguage(Locale.getDefault());
                    // TextToSpeech.LANG_MISSING_DATA??????????????????????????????
                    // TextToSpeech.LANG_NOT_SUPPORTED????????????
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
        //???????????????????????????????????????
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
                                .getDataAndBitmap(rect, 0, frames, new HotImageCallback.Stub() {//??????????????????0
                                    @Override
                                    public void onTemperatureFail(String e) throws RemoteException {
                                        // TODO Auto-generated method stub
                                        //   Log.e("yw___15151515---", "onTemperatureFail " + e);
                                        //retry(tempretrynum);
                                        Isneedtem = true;// ??????????????????  ???onpreview ??????
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

                            Log.e("???????????????"," "+(totalMilliSeconds2-totalMilliSeconds1));

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
                        Log.e("????????????catch-", e.toString());
                        Isneedtem = true;
                        // retry(rect);
                    }

                }
            }).start();

        }
    }

    /**
     * ???????????????
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
     * ???????????????faceHelper??????????????????????????????????????????????????????????????????crash
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
     * ???????????????sheltered?????????sheltered
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
        // 0b111 ?????????????????????1??????true
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

            //??????FR?????????
            @Override
            public void onFaceFeatureInfoGet(@Nullable final FaceFeature faceFeature, final Integer requestId, final Integer errorCode) {
                //FR??????
                if (faceFeature != null) {
//                    Log.i(TAG, "onPreview: fr end = " + System.currentTimeMillis() + " trackId = " + requestId);
                    Integer liveness = livenessMap.get(requestId);
                    //??????????????????????????????????????????
                    if (!livenessDetect) {
                        searchFace(faceFeature, requestId);
                    }
                    //?????????????????????????????????
                    else if (liveness != null && liveness == LivenessInfo.ALIVE) {
                        searchFace(faceFeature, requestId);
                    }
                    //??????????????????????????????????????????????????????????????????
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
                //??????????????????
                else {
                    if (increaseAndGetValue(extractErrorRetryMap, requestId) > MAX_RETRY_TIME) {
                        extractErrorRetryMap.put(requestId, 0);

                        String msg;
                        // ?????????FaceInfo????????????????????????????????????????????????????????????RGB????????????????????????????????????
                        if (errorCode != null && errorCode == ErrorInfo.MERR_FSDK_FACEFEATURE_LOW_CONFIDENCE_LEVEL) {
                            msg = getString(R.string.low_confidence_level);
                        } else {
                            msg = "ExtractCode:" + errorCode;
                        }
                        faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, msg));
                        // ??????????????????????????????????????????????????????????????????????????????
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
                    // ??????????????????
                    if (liveness == LivenessInfo.NOT_ALIVE) {
                        faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, "NOT_ALIVE"));
                        // ?????? FAIL_RETRY_INTERVAL ??????????????????????????????UNKNOWN????????????????????????????????????????????????
                        retryLivenessDetectDelayed(requestId);
                    }
                } else {
                    if (increaseAndGetValue(livenessErrorRetryMap, requestId) > MAX_RETRY_TIME) {
                        livenessErrorRetryMap.put(requestId, 0);
                        String msg;
                        // ?????????FaceInfo????????????????????????????????????????????????????????????RGB????????????????????????????????????
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
                // ????????????????????????????????????????????????????????????
                if (faceHelper == null ||
                        lastPreviewSize == null ||
                        lastPreviewSize.width != previewSize.width || lastPreviewSize.height != previewSize.height) {
                    Integer trackedFaceCount = null;
                    // ??????????????????????????????
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
                        Rect originRect = facePreviewInfoList.get(i).getFaceInfo().getRect();//???????????????
                        //????????????  ???1/4   ????????????2/1
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

                        //??????????????????  ?????????????????????
                        if (distancestatus != 0) {
                            continue;
                        }

                        //????????????????????????

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

                        //????????????
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
                        //* ??????	??????	?????????
                        // * 0	0	???????????????
                        // * 0	1	???????????????
                        //  * 1	0	?????????????????????
                        //  * 1	1	????????????????????????

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
                         * ????????????????????????????????????????????????????????????????????????????????????????????????ANALYZING???????????????????????????ALIVE???NOT_ALIVE??????????????????????????????
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
                         * ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                         * ??????????????????????????????????????????{@link FaceListener#onFaceFeatureInfoGet(FaceFeature, Integer, Integer)}?????????
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
            // ?????????????????????????????????????????????
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
     * ???????????????????????????
     *
     * @param facePreviewInfoList ?????????trackId??????
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

                        //   Log.e("yw","????????????"+ "  similar = " + compareResult.getSimilar());
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
                                //??????????????????????????????????????????????????? MAX_DETECT_NUM ??????????????????????????????????????????????????????
                                if (compareResultList.size() >= MAX_DETECT_NUM) {
                                    compareResultList.remove(0);
                                    adapter.notifyItemRemoved(0);
                                }
                                //?????????????????????????????????trackId
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
                                                  //??????????????????????????????????????????????????? MAX_DETECT_NUM ??????????????????????????????????????????????????????
                                                  if (compareResultList.size() >= MAX_DETECT_NUM) {
                                                      compareResultList.remove(0);
                                                      adapter.notifyItemRemoved(0);
                                                      Log.v(TAG,"VISITOR NOT ADDED 1");
                                                  }
                                                  //?????????????????????????????????trackId
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
     * ??????????????????????????????{@link #REGISTER_STATUS_READY}
     *
     * @param view ????????????
     */
    public void register(View view) {
        if (registerStatus == REGISTER_STATUS_DONE) {
            registerStatus = REGISTER_STATUS_READY;
        }
    }

    /**
     * ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
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
     * ???{@link #previewView}????????????????????????????????????????????????????????????????????????????????????
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
     * ???map???key?????????value???1??????
     *
     * @param countMap map
     * @param key      key
     * @return ???1??????value
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
     * ?????? FAIL_RETRY_INTERVAL ????????????????????????
     *
     * @param requestId ??????ID
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
                        // ????????????????????????UNKNOWN????????????????????????????????????????????????
                        if (livenessDetect) {
                            faceHelper.setName(requestId, Integer.toString(requestId));
                        }
                        livenessMap.put(requestId, LivenessInfo.UNKNOWN);
                        delayFaceTaskCompositeDisposable.remove(disposable);
                    }
                });
    }

    /**
     * ?????? FAIL_RETRY_INTERVAL ????????????????????????
     *
     * @param requestId ??????ID
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
                        // ????????????????????????????????????FAILED????????????????????????????????????????????????
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


    //????????????????????????
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
     * ?????????????????????????????????????????? ????????????????????????????????????????????????????????????50
     *
     * @param trackId
     * @param rect
     * @return ?????????????????????null ,?????????????????????????????????rect
     */
    private Rect isOffset(int trackId, Rect rect) {
        Rect rect2 = drawHelper.adjustRect(rect);
        // Log.e(TAG, "rect2 :" + rect2);
        //Rect(352, 480 - 643, 742)
        float horizontalOffset = (rect2.left + rect2.right)  / 2.0f - previewView.getWidth()  / 2.0f;
        float verticalOffset = (rect2.top + rect2.bottom)  / 2.0f - previewView.getHeight()  / 2.0f;


        int processCount = increaseAndGetValue(faceOffsetProcessCountMap, trackId);
        int continueOffset = 0b111;//????????????

        //???????????????????????????50

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
                //????????????y????????????x?????????,???????????????????????????rect????????????????????????
                int horizontalOffset2 = (int)((horizontalOffset)/15);
                int verticalOffset2 = (int)((verticalOffset)/20);
                newRect.top  += horizontalOffset2;
                newRect.bottom  += horizontalOffset2;
                newRect.left  += verticalOffset2;
                newRect.right  += verticalOffset2;
            }else if (moudle==15){
                newRect = new Rect(12,12,20,20);
                //????????????y????????????x?????????,???????????????????????????rect????????????????????????
                int horizontalOffset2 = (int)((horizontalOffset)/20);
                int verticalOffset2 = (int)((verticalOffset)/20);
                newRect.top  += horizontalOffset2;
                newRect.bottom  += horizontalOffset2;
                newRect.left  += verticalOffset2;
                newRect.right  += verticalOffset2;
            }else if (moudle==8){//???????????? 950T  TX14
                newRect = new Rect(4,13,9,19);
                //????????????y????????????x?????????,???????????????????????????rect????????????????????????
                int horizontalOffset2 = (int)((horizontalOffset)/40);
                int verticalOffset2 = (int)((verticalOffset)/40);

                newRect.top  -= horizontalOffset2;
                newRect.bottom  -= horizontalOffset2;
                newRect.left  += verticalOffset2;
                newRect.right  += verticalOffset2;
            }else if (moudle==21){//980T 21
                newRect = new Rect(9,12,17,20);
                //????????????y????????????x?????????,???????????????????????????rect????????????????????????
                int horizontalOffset2 = (int)((horizontalOffset)/10);
                int verticalOffset2 = (int)((verticalOffset)/10);


                newRect.top  += horizontalOffset2;
                newRect.bottom  += horizontalOffset2;
                newRect.left  += verticalOffset2;
                newRect.right  += verticalOffset2;

            }else {//16
                newRect = new Rect(9,12,17,20);
                //????????????y????????????x?????????,???????????????????????????rect????????????????????????
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