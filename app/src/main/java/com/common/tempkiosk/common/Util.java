package com.common.tempkiosk.common;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

//工具类  目前有获取sharedPreferences 方法
public class Util {
    public static SharedPreferences getSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return sharedPreferences;
    }

    //sp写入string
    public static void writeString(SharedPreferences sp, String key, String value) {
        SharedPreferences.Editor edit = sp.edit();
        edit.putString(key, value);
        edit.commit();
    }

    //sp写入int
    public static void writeInt(SharedPreferences sp, String key, int value) {
        SharedPreferences.Editor edit = sp.edit();
        edit.putInt(key, value);
        edit.commit();
    }

    //sp写入int
    public static void writeFloat(SharedPreferences sp, String key, float value) {
        SharedPreferences.Editor edit = sp.edit();
        edit.putFloat(key, value);
        edit.commit();
    }

    //sp写入boolean
    public static void writeBoolean(SharedPreferences sp, String key, Boolean value) {
        SharedPreferences.Editor edit = sp.edit();
        edit.putBoolean(key, value);
        edit.commit();
    }





    // 获取sn号
    public static String getSerialNumber() {
        String serial = null;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            serial = (String) get.invoke(c, "ro.serialno");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serial;
    }

    /**
     * 获取SN号
     *
     * @return
     */
    public static String getSNCode() {
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT <= 28) {
            return Build.SERIAL;
        } else {
            return getSerialNumber();
        }
    }

    private static Toast toast = null;

    public static void showToast(Context context, String s) {
        if (toast == null) {
            toast = Toast.makeText(context, s, Toast.LENGTH_SHORT);
            toast.show();
        } else {
            toast.setText(s);
            toast.show();
        }
    }

    /**
     * 图片旋转
     *
     * @param tmpBitmap
     * @param degrees
     * @return
     */
    public static Bitmap rotateToDegrees(Bitmap tmpBitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setRotate(degrees);
        return tmpBitmap =
                Bitmap.createBitmap(tmpBitmap, 0, 0, tmpBitmap.getWidth(), tmpBitmap.getHeight(), matrix,
                        true);
    }

    //保存bitmap
    public static String saveBitmapFile(Bitmap bm, String fileName) throws IOException {//将Bitmap类型的图片转化成file类型，便于上传到服务器
        String path = Environment.getExternalStorageDirectory() + "/pic/";
        File dirFile = new File(path);
        if (!dirFile.exists()) {
            dirFile.mkdir();
        }
        File myCaptureFile = new File(path + fileName);
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
            bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return myCaptureFile.getPath();

    }

    //获取带0的数值
    public static String getnumberString(int number) {
        String numberstr = "";
        try {
            if (number >= 0 && number < 10) {
                numberstr = "0" + number;
            } else {
                numberstr = number + "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return numberstr;
    }

    /**
     * 缩放Bitmap图片
     **/
    public static Bitmap zoomBitmap(Bitmap bitmap, int width, int height) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidth = ((float) width / w);
        float scaleHeight = ((float) height / h);
        matrix.postScale(scaleWidth, scaleHeight);// 利用矩阵进行缩放不会造成内存溢出
        Bitmap newbmp = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
        return newbmp;
    }

    /**
     * 以最省内存的方式读取本地资源的图片
     */
    public static Bitmap readBitMap(String path) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inPurgeable = true;
        opt.inInputShareable = true;
        // 获取资源图片
        return BitmapFactory.decodeFile(path, opt);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    // 如果是放大图片，filter决定是否平滑，如果是缩小图片，filter无影响
    public static Bitmap createScaleBitmap(Bitmap src, int dstWidth, int dstHeight) {
        Bitmap dst = Bitmap.createScaledBitmap(src, dstWidth, dstHeight, false);
        if (src != dst) { // 如果没有缩放，那么不回收
            src.recycle(); // 释放Bitmap的native像素数组
        }
        return dst;
    }

    // 从Resources中加载图片
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options); // 读取图片长款
        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight); // 计算inSampleSize
        options.inJustDecodeBounds = false;
        Bitmap src = BitmapFactory.decodeResource(res, resId, options); // 载入一个稍大的缩略图
        return createScaleBitmap(src, reqWidth, reqHeight); // 进一步得到目标大小的缩略图
    }

    // 从sd卡上加载图片
    public static Bitmap decodeSampledBitmapFromSD(String pathName, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        Bitmap src = BitmapFactory.decodeFile(pathName, options);
        return createScaleBitmap(src, reqWidth, reqHeight);
    }


    public static boolean isDateOneBigger(String str1, String str2) {
        boolean isBigger = false;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date dt1 = null;
        Date dt2 = null;
        try {
            dt1 = sdf.parse(str1);
            dt2 = sdf.parse(str2);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (dt1.getTime() > dt2.getTime()) {
            isBigger = true;
        } else if (dt1.getTime() < dt2.getTime()) {
            isBigger = false;
        }
        return isBigger;
    }

    /**
     * 隐藏软键盘(只适用于Activity，不适用于Fragment)
     */
    public static void hideSoftKeyboard(AppCompatActivity activity) {
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }





    /**
     *
     * 判断某activity是否处于栈顶
     * @return  true在栈顶 false不在栈顶
     */
    public static boolean isActivityTop(Class cls,Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        String name = manager.getRunningTasks(1).get(0).topActivity.getClassName();
        return name.equals(cls.getName());
    }


    /**
     * 检查时间格式是否正确
     *
     * @param str       时间格式字符
     */
    public static boolean isValidDate(String str,String pattern) {
        boolean convertSuccess = true;
//        String pattern = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        try {
            // 设置lenient为false. 否则SimpleDateFormat会比较宽松地验证日期，比如2007/02/29会被接受，并转换成2007/03/01
            if(str.length() != pattern.length()) return false;

            format.setLenient(false);
            format.parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
            // 如果throw java.text.ParseException或者NullPointerException，就说明格式不对
            convertSuccess = false;
        }
        return convertSuccess;
    }

 //读取url
 public static String txt2String(String path){

     File file = new File(path);
     String result = "";
     if (file.exists()){
         try{
             BufferedReader br = new BufferedReader(new FileReader(file));//构造一个BufferedReader类来读取文件
             result=br.readLine();
            /* String s = null;
             while((s = br.readLine())!=null){//使用readLine方法，一次读一行
                 result = result + "\n" +s;
             }*/
             br.close();
         }catch(Exception e){
             e.printStackTrace();
         }
     }


              return result;
         }


    //摄氏度转华氏摄氏度
    public static double celsiusToFahrenheit(float temperature){
        BigDecimal b = new BigDecimal(temperature*1.8+32);
        double f1 = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        return f1;
    }

    //将摄像头的nv21数据流转为图片保存
    public static void nv21ToFile(byte[] data,Camera camera,String filename){
        try{
            //相机预览获取到的data数据不能直接转为bitmap存储，因为该数据是YUV格式的，需要进行数据转换
            Camera.Size size = camera.getParameters().getPreviewSize();//获得预览图像设置的尺寸
            YuvImage img = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            img.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
            Bitmap bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            bitmap = rotateToDegrees(bitmap,90);
            saveBitmapFile(bitmap,filename);
            stream.close();
        }catch(Exception ex){
            Log.e("Camera PreviewFrame","Error:"+ex.getMessage());
        }
    }





    public static boolean copyFile(String filePath, String destPath) {
        File originFile = new File(filePath);

        if (!originFile.exists()) {
            Log.e("yw_lisence","lisence not exist");
            return false;
        }
        File destFile = new File(destPath);
        BufferedInputStream reader = null;
        BufferedOutputStream writer = null;
        try {
            if (!destFile.exists()) {
                destFile.createNewFile();
            }
            reader = new BufferedInputStream(new FileInputStream(originFile));
            writer = new BufferedOutputStream(new FileOutputStream(destFile));
            byte[] buffer = new byte[1024];
            int length;
            while ((length = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, length);
            }
        } catch (Exception exception) {
            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }
        }
        return true;
    }



    public static boolean activeEngineOffline(Context context){
        boolean result=false;
        String path=Environment.getExternalStorageDirectory() + "/active_result.dat";
        String path1=Environment.getExternalStorageDirectory() + "/ArcFacePro32.dat";
        String path2=context.getApplicationContext().getFilesDir() + "/ArcFacePro32.dat";

        File file2=new File(path2);
        if (file2.exists()){
            result=true;
        }else {
            File file=new File(path);
            if (file.exists()){
                int activeCode = FaceEngine.activeOffline(context,
                        path);
                if (activeCode == ErrorInfo.MOK) {
                    result=true;
                    Log.e("active_result","true  1");
                } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
                    result=true;
                    Log.e("active_result","true  2");
                } else {
                    result=false;
                    Log.e("active_result","false  1");
                }
            }else {
                File file1=new File(path1);
                if (file1.exists()){
                    copyFile(path1,path2);
                    if (file2.exists()){
                        result=true;
                        Log.e("active_result","true  3");
                    }
                }else {
                    Log.e("active_result","false  no .dat file");
                }
            }
        }



        return result;
    }
//检测指定包名的应用是否安装
    public static boolean hasApplication(Context context, String packageName){
        PackageManager packageManager = context.getPackageManager();

        //获取系统中安装的应用包的信息
        List<PackageInfo> listPackageInfo = packageManager.getInstalledPackages(0);
        for (int i = 0; i < listPackageInfo.size(); i++) {
            if(listPackageInfo.get(i).packageName.equalsIgnoreCase(packageName)){
                return true;
            }
        }
        return false;

    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("0x");
        if (src == null || src.length <= 0) {
            return null;
        }
        char[] buffer = new char[2];
        for (int i = 0; i < src.length; i++) {
            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
            stringBuilder.append(buffer);
        }
        return stringBuilder.toString();
    }
}
