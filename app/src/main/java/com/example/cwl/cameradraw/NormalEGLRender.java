package com.example.cwl.cameradraw;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES10;
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
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by cwl on 18/4/16.
 */

public class NormalEGLRender extends EGLSurfaceRender {

    private final static String TAG = NormalEGLRender.class.getSimpleName();

    private final GPUImageFilter gpuImageFilter;

    private FloatBuffer glCubeBuffer;

    private FloatBuffer glTextureBuffer;

    private int surfaceWidth;

    private int surfaceHeight;

    private int imageWidth;

    private int imageHeight;

    private SurfaceTexture surfaceTexture;

    private LinkedBlockingQueue<Integer> textureQueue;

    private int textureId = OpenGLUtils.NO_TEXTURE;

    private Context mContext;

    public NormalEGLRender(EGLRenderThread renderThread,Context context){
        super(renderThread);
        mContext = context;
        gpuImageFilter = new GPUImageFilter();
        glCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        glCubeBuffer.put(TextureRotationUtil.CUBE).position(0);

        glTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        glTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION_180).position(0);

        textureQueue = new LinkedBlockingQueue<>(1000);
    }


    @Override
    public void onCreated() {
        gpuImageFilter.init();
//        textureId = OpenGLUtils.loadTexture(mContext, "warm_layer1.jpg");

    }

    @Override
    public void onUpdate() {

    }

    @Override
    public void onDrawFrame(EGLSurface outputSurface) {
//        Integer textureId = -1;
//        try {
//            textureId = textureQueue.take();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        if(textureId == -1)
//            return ;

        updateSurfaceSize(outputSurface.viewport.width, outputSurface.viewport.height);

        GLES20.glClearColor(0,0,0,0);
        GLES20.glClear(GLES10.GL_COLOR_BUFFER_BIT| GLES10.GL_DEPTH_BUFFER_BIT);
        GLES20.glViewport(0,0,imageWidth, imageHeight);
        Log.e(TAG, "onDrawFrame width=" + outputSurface.viewport.width + " height="+outputSurface.viewport.height + " imageWidth="+ imageWidth + " imageHeight="+ imageHeight);
        gpuImageFilter.onDrawFrame(textureId, glCubeBuffer, glTextureBuffer);

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
        gpuImageFilter.onOutputSizeChanged(targetWidth, targetHeight);
        float[] rotated = TextureRotationUtil.cropSize(TextureRotationUtil.getTargetRatedText(),surfaceWidth, surfaceHeight, targetWidth, targetHeight);
    }

    @Override
    public void onDestroy() {
        if(surfaceTexture != null){
            if(surfaceTexture != null){
                surfaceTexture.detachFromGLContext();
            }
            surfaceTexture.release();
        }
        gpuImageFilter.destroy();
    }

    public void setTextureId(int textureId) {
//        this.textureQueue.offer(textureId);
//        if(this.textureId != -1)
//            return ;
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
