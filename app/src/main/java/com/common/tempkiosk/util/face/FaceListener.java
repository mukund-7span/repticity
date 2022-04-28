package com.common.tempkiosk.util.face;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.LivenessInfo;

/**
 * 人脸处理回调
 */
public interface FaceListener {
    /**
     * 当出现异常时执行
     *
     * @param e 异常信息
     */
    void onFail(Exception e);


    /**
     * 请求人脸特征后的回调
     *
     * @param faceFeature 人脸特征数据
     * @param requestId   请求码
     * @param errorCode   错误码
     */
    void onFaceFeatureInfoGet(@Nullable FaceFeature faceFeature, Integer requestId, Integer errorCode);

    /**
     * 请求活体检测后的回调
     *
     * @param livenessInfo 活体检测结果
     * @param requestId    请求码
     * @param errorCode    错误码
     */
    void onFaceLivenessInfoGet(@Nullable LivenessInfo livenessInfo, Integer requestId, Integer errorCode);
}
