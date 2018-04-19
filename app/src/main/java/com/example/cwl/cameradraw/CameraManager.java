package com.example.cwl.cameradraw;

import android.app.Application;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;

/**
 * Created by cwl on 18/3/28.
 */

public class CameraManager {

    private static Camera sCamera;

    private static int sCameraId;

    private static int sRequestPreviewW, sRequestPreviewH;

    private static boolean isCameraPreview;

    private static Camera.PreviewCallback sPreviewCallback;

    private static SurfaceTexture sSurfaceTexture;

    private static SurfaceHolder sSurfaceView;

    private static WindowManager sWindowManager;

    private static SurfaceView sDummySurfaceView;

    public static void openCamera(int cameraId){
        if(sCamera == null){
            try{
                sCameraId = cameraId;
                sCamera = android.hardware.Camera.open(cameraId);
                setDefaultParams();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static void show(Context context){
        if(context != null) {
            Context application = context.getApplicationContext();
            sWindowManager = (WindowManager) application.getSystemService(Context.WINDOW_SERVICE);
            sDummySurfaceView = new SurfaceView(application);
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.width = 1;
            layoutParams.height = 1;
            layoutParams.alpha = 0;
            layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            sWindowManager.addView(sDummySurfaceView, layoutParams);
        }
    }

    public static void dismiss(){
        try {
            if(sWindowManager != null && sDummySurfaceView != null){
                sWindowManager.removeView(sDummySurfaceView);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static SurfaceView getDummySurfaceView() {
        return sDummySurfaceView;
    }

    public static void setRequestPreviewSize(int requestPreviewW, int requestPreviewH){
        sRequestPreviewW = requestPreviewW;
        sRequestPreviewH = requestPreviewH;
    }

    public static void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        CameraManager.sPreviewCallback = previewCallback;
    }

    private static void setDefaultParams(){
        if(sCamera != null){
            Camera.Parameters parameters = sCamera.getParameters();

            setFocusMode(parameters);

            Rect rect = getSimilarCameraPreviewSize(parameters);

            parameters.setPreviewSize(rect.width(), rect.height());
            parameters.setPreviewFormat(ImageFormat.NV21);

            sCamera.setParameters(parameters);
        }
    }

    private static void setFocusMode(Camera.Parameters parameters){
        List<String> supportFocusModes = parameters.getSupportedFocusModes();
        if(supportFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)){
            parameters.setFlashMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }else if(supportFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
            parameters.setFlashMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }else {
            parameters.setFlashMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
    }

    private static Rect getSimilarCameraPreviewSize(Camera.Parameters parameters){
        Rect rect = new Rect();

        int minPreviewSize = -1;

        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        for(Camera.Size size: previewSizes){
            int previewW = size.width;
            int previewH = size.height;
            if(previewH < sRequestPreviewW || previewW < sRequestPreviewH){
                continue;
            }
            int previewSize = Math.abs((sRequestPreviewW + sRequestPreviewH) - (previewH + previewW));
            if(minPreviewSize == -1){
                minPreviewSize = previewSize;
                //摄像头不是垂直拍摄的，前置摄像头90°，后置摄像头270°
                rect.set(0,0,previewW, previewH);
            }
            if(minPreviewSize > previewSize){
                minPreviewSize = previewSize;
                //摄像头不是垂直拍摄的，前置摄像头90°，后置摄像头270°
                rect.set(0,0,previewW, previewH);
            }
        }

        return rect;
    }

    public static void startPreview(SurfaceTexture surfaceTexture){
        if(isCameraPreview || surfaceTexture == null){
            return;
        }
        if(sCamera != null){
            try {
                if(sPreviewCallback != null){
                    sCamera.setPreviewCallback(sPreviewCallback);
                }
                sSurfaceTexture = surfaceTexture;
                sCamera.setPreviewTexture(surfaceTexture);
                sCamera.setDisplayOrientation(sCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT ? 90 : 90);
                sCamera.startPreview();
                sCamera.cancelAutoFocus();
                sCamera.autoFocus(null);
                isCameraPreview = true;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static void startPreview(SurfaceHolder surfaceHoder){
        if(isCameraPreview || surfaceHoder == null){
            return;
        }
        if(sCamera != null){
            try {
                if(sPreviewCallback != null){
                    sCamera.setPreviewCallback(sPreviewCallback);
                }
                sSurfaceView = surfaceHoder;
                sCamera.setPreviewDisplay(surfaceHoder);
                sCamera.setDisplayOrientation(sCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT ? 90 : 90);
                sCamera.startPreview();
                sCamera.cancelAutoFocus();
                sCamera.autoFocus(null);
                isCameraPreview = true;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static void stopPreview(){
        if(isCameraPreview){
            sCamera.setPreviewCallback(null);
            sCamera.stopPreview();
            isCameraPreview = false;
        }
    }


    public static void release(){
        stopPreview();
        if(sCamera != null){
            sCamera.release();
            sCamera = null;
        }
    }

    public static void switchCamera(){
            sCameraId = sCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;
            release();
            openCamera(sCameraId);
            if(sSurfaceView != null){
                startPreview(sSurfaceView);
            }else{
                startPreview(sSurfaceTexture);
            }
    }

    public static Rect getRatioPreviewSize(){
        Rect rect = new Rect();
        if(sCamera != null ){
            Camera.Parameters parameters = sCamera.getParameters();
            Camera.Size size = parameters.getPreviewSize();
            int previewHeight = size.width;
            int previewWidth = size.height;
            rect.set(0,0, previewWidth , previewHeight);
        }
        return rect;
    }


}
