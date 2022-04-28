package com.common.tempkiosk.common;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.common.thermalimage.ThermalImageUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by tsy on 2016/12/6.
 */

public class Application extends android.app.Application {

    private static Application mInstance;

   // private DownloadMgr mDownloadMgr;
    public static boolean member=false;
    private List<AppCompatActivity> activityList = new LinkedList();
    private ThermalImageUtil temperatureUtil;
    SharedPreferences sp;

    @Override
    public void onCreate() {
        super.onCreate();
        sp = Util.getSharedPreferences(this);

       /* String url=Util.txt2String("/sdcard/faceurl.txt");
        Log.e("yw","url"+url);
        if (url.length()>9){
            Util.writeString(sp, GlobalParameters.BaseUrl,url);
        }*/


        mInstance = this;






        temperatureUtil = new ThermalImageUtil(this);
    }

    public static synchronized Application getInstance() {
        return mInstance;
    }



    // 添加Activity到容器中
    public void addActivity(AppCompatActivity activity) {
        activityList.add(activity);
    }

    public ThermalImageUtil getTemperatureUtil(){
        return temperatureUtil;
    }

    // 遍历所有Activity并finish
    public void exit() {
        for (AppCompatActivity activity : activityList) {
            if(!activity.isFinishing()) {
                if(!activity.isFinishing()) {
                    Log.e("exit---", activity.getLocalClassName());
                    activity.finish();
                }
            }
        }
        System.exit(0);
    }

}
