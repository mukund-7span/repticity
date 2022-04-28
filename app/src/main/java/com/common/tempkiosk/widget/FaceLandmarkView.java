package com.common.tempkiosk.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.View;

import com.common.tempkiosk.util.DrawHelper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 用于显示人脸信息的控件
 */
public class FaceLandmarkView extends View {
    private CopyOnWriteArrayList<PointF[]> landmarkInfoCopyOnWriteArrayList = new CopyOnWriteArrayList<>();

    // 画笔，复用
    private Paint paint;

    private static final int DEFAULT_FACE_LANDMARK_POINT_STROKE_SIZE = 6;

    public FaceLandmarkView(Context context) {
        this(context, null);
    }

    public FaceLandmarkView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (landmarkInfoCopyOnWriteArrayList != null && landmarkInfoCopyOnWriteArrayList.size() > 0) {
            for (int i = 0; i < landmarkInfoCopyOnWriteArrayList.size(); i++) {
                DrawHelper.drawFaceLandmarks(canvas, landmarkInfoCopyOnWriteArrayList.get(i), DEFAULT_FACE_LANDMARK_POINT_STROKE_SIZE, paint);
            }
        }
    }

    public void clearLandmarkInfo() {
        landmarkInfoCopyOnWriteArrayList.clear();
        postInvalidate();
    }

    public void addLandmarkInfo(PointF[] landmarkInfo) {
        landmarkInfoCopyOnWriteArrayList.add(landmarkInfo);
        postInvalidate();
    }

    public void addLandmarkInfo(List<PointF[]> landmarkInfoList) {
        landmarkInfoCopyOnWriteArrayList.addAll(landmarkInfoList);
        postInvalidate();
    }
}