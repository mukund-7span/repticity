package com.common.tempkiosk.model;

import android.graphics.PointF;

import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.LandmarkInfo;
import com.arcsoft.face.LivenessInfo;

public class FacePreviewInfo {
    private FaceInfo faceInfo;
    private int trackId;
    private int mask;
    private int faceShelter;
    private LandmarkInfo landmarkInfo;

    public FacePreviewInfo(FaceInfo faceInfo, int faceShelter, int mask, int trackId) {
        this.faceInfo = faceInfo;
        this.faceShelter = faceShelter;
        this.mask = mask;
        this.trackId = trackId;
    }

    public FacePreviewInfo(FaceInfo faceInfo, LandmarkInfo landmarkInfo, int faceShelter, int mask, int trackId) {
        this.faceInfo = faceInfo;
        this.landmarkInfo = landmarkInfo;
        this.faceShelter = faceShelter;
        this.mask = mask;
        this.trackId = trackId;
    }

    public FaceInfo getFaceInfo() {
        return faceInfo;
    }

    public void setFaceInfo(FaceInfo faceInfo) {
        this.faceInfo = faceInfo;
    }


    public int getTrackId() {
        return trackId;
    }

    public void setTrackId(int trackId) {
        this.trackId = trackId;
    }

    public int getMask() {
        return mask;
    }

    public void setMask(int mask) {
        this.mask = mask;
    }

    public int getFaceShelter() {
        return faceShelter;
    }

    public void setFaceShelter(int faceShelter) {
        this.faceShelter = faceShelter;
    }

    public LandmarkInfo getLandmarkInfo() {
        return landmarkInfo;
    }

    public void setLandmarkInfo(LandmarkInfo landmarkInfo) {
        this.landmarkInfo = landmarkInfo;
    }
}
