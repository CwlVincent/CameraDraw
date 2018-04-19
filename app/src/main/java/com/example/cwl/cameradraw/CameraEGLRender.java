package com.example.cwl.cameradraw;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.util.Log;

import com.example.cwl.cameradraw.EGL.EGLRenderThread;
import com.example.cwl.cameradraw.EGL.EGLSurface;
import com.example.cwl.cameradraw.EGL.EGLSurfaceRender;
import com.example.cwl.cameradraw.openGL.CameraGPUImageFilter;
import com.example.cwl.cameradraw.openGL.GPUImageFilter;
import com.example.cwl.cameradraw.openGL.OpenGLUtils;
import com.example.cwl.cameradraw.openGL.TextureRotationUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by cwl on 18/4/16.
 */

public class CameraEGLRender extends EGLSurfaceRender {

    private final static String TAG = CameraEGLRender.class.getSimpleName();

    private final CameraGPUImageFilter cameraImageFilter;

    private final GPUImageFilter gpuImageFilter;

    private FloatBuffer glCubeBuffer;

    private FloatBuffer glTextureBuffer;

    private FloatBuffer glCameraTextureBuffer;

    private int surfaceWidth;

    private int surfaceHeight;

    private int imageWidth;

    private int imageHeight;

    private SurfaceTexture surfaceTexture;

    private int textureId = OpenGLUtils.NO_TEXTURE;

    public CameraEGLRender(EGLRenderThread renderThread){
        super(renderThread);
        cameraImageFilter = new CameraGPUImageFilter();
        gpuImageFilter = new GPUImageFilter();
        glCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        glCubeBuffer.put(TextureRotationUtil.CUBE).position(0);

        glTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        glTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0);

        glCameraTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();



    }


    @Override
    public void onCreated() {
        cameraImageFilter.init();
        gpuImageFilter.init();


    }

    @Override
    public void onUpdate() {

    }

    @Override
    public void onDrawFrame(EGLSurface outputSurface) {
        if(surfaceTexture == null)
            return ;
        updateSurfaceSize(outputSurface.viewport.width, outputSurface.viewport.height);

        GLES20.glClearColor(0,0,0,0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT| GLES20.GL_DEPTH_BUFFER_BIT);

        try{
            surfaceTexture.updateTexImage();
        }catch (Exception e){
            e.printStackTrace();
        }
        float[] mtxs = new float[16];
        surfaceTexture.getTransformMatrix(mtxs);
        cameraImageFilter.setmTextureTransformMatrix(mtxs);

        int tx = cameraImageFilter.onDrawToTexture(this.textureId);

        gpuImageFilter.onDrawFrame(tx, glCubeBuffer, glCameraTextureBuffer);

        if(onDrawFrameListener != null){
            onDrawFrameListener.onDrawFrame(tx, glCubeBuffer, glCameraTextureBuffer);
        }
    }

    private void updateSurfaceSize(int surfaceWidth, int surfaceHeight){
        if(surfaceHeight != this.surfaceHeight || surfaceWidth != this.surfaceWidth){
            updateImageWidthAndHeight(imageWidth, imageHeight,surfaceWidth, surfaceHeight);
        }
        this.surfaceWidth = surfaceWidth;
        this.surfaceHeight = surfaceHeight;
    }

    public void setImageSize(int imageWidth, int imageHeight){
        this.imageHeight = imageHeight;
        this.imageWidth = imageWidth;
    }

    private void updateImageWidthAndHeight(int imageWidth, int imageHeight, int surfaceWidth, int surfaceHeight){
        float preivewRatio = imageWidth * 1.0f / imageHeight;
        int targetWidth = surfaceWidth;
        int targetHeight = (int)(surfaceWidth / preivewRatio);
        if(targetHeight < surfaceHeight){
            targetHeight = surfaceHeight;
            targetWidth = (int)(targetHeight * preivewRatio);
        }
        this.imageHeight = targetHeight;
        this.imageWidth = targetWidth;
        cameraImageFilter.initCameraFrameBuffer(targetWidth, targetHeight);
        cameraImageFilter.onOutputSizeChanged(targetWidth,targetHeight);
        gpuImageFilter.onOutputSizeChanged(targetWidth, targetHeight);
        cameraImageFilter.onDisplaySizeChanged(surfaceWidth, surfaceWidth);
        float[] rotated = TextureRotationUtil.cropSize(TextureRotationUtil.getTargetRatedText(),surfaceWidth, surfaceHeight, targetWidth, targetHeight);
        glCameraTextureBuffer.put(rotated).position(0);
    }

    @Override
    public void onDestroy() {
        if(surfaceTexture != null){
            if(surfaceTexture != null){
                surfaceTexture.detachFromGLContext();
            }
            surfaceTexture.release();
        }
        cameraImageFilter.destoryFramebuffers();
        gpuImageFilter.destroy();
        cameraImageFilter.destroy();
    }

    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        this.surfaceTexture = surfaceTexture;
    }

    public void setTextureId(int textureId) {
        this.textureId = textureId;
    }

    public interface OnDrawFrameListener{
        public void onDrawFrame(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer);
    }

    private OnDrawFrameListener onDrawFrameListener;

    public void setOnDrawFrameListener(OnDrawFrameListener onDrawFrameListener) {
        this.onDrawFrameListener = onDrawFrameListener;
    }
}
