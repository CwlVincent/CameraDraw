package com.example.cwl.cameradraw;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.ViewGroup;

import com.example.cwl.cameradraw.openGL.CameraGPUImageFilter;
import com.example.cwl.cameradraw.openGL.GPUImageFilter;
import com.example.cwl.cameradraw.openGL.OpenGLUtils;
import com.example.cwl.cameradraw.openGL.Rotation;
import com.example.cwl.cameradraw.openGL.TextureRotationUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by cwl on 18/3/29.
 */

public class GLSurfaceViewRender implements GLSurfaceView.Renderer {

    private static final String TAG = GLSurfaceViewRender.class.getSimpleName();

    private GLSurfaceView glSurfaceView;

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

    public GLSurfaceViewRender(GLSurfaceView glSurfaceView){
        this.glSurfaceView = glSurfaceView;
        cameraImageFilter = new CameraGPUImageFilter();
        gpuImageFilter = new GPUImageFilter();

        glCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        glCubeBuffer.put(TextureRotationUtil.CUBE).position(0);

        glTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        glTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0);

        glCameraTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        glCameraTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0);

//        textureId = OpenGLUtils.getExternalOESTextrueID();
//        surfaceTexture = new SurfaceTexture(textureId);
//        surfaceTexture.setOnFrameAvailableListener(onFrameAvailableListener);


    }

    public void updatePreview(GLSurfaceView surfaceView){
        if(this.glSurfaceView != null){
            glSurfaceView.setRenderer(null);
        }
        this.glSurfaceView = surfaceView;
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
        glSurfaceView.setRenderer(this);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
//        GLES20.glDisable(GL10.GL_DITHER);
//        GLES20.glClearColor(0,0,0,0);
//        GLES20.glEnable(GL10.GL_CULL_FACE);
//        GLES20.glEnable(GL10.GL_DEPTH_TEST);

        cameraImageFilter.init();
        gpuImageFilter.init();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
//        if(surfaceTexture == null)
//            return ;

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

//        try{
//            surfaceTexture.updateTexImage();
//        }catch (Exception e){
//            Log.e("Render", e.getMessage());
//        }
//        float[] mtx = new float[16];
//        //对纹理进行采样的时候，应该首先使用getTransformMatrix查询得到的矩阵得到的矩阵来变换纹理坐标。每次
//        //调用updateTexImage()的时候，可能导致矩阵发生变化，因此在纹理图形中更新时需要重新查询。该矩阵将
//        //传统的2D OpenGL ES纹理坐标列向量(s,t,0,1),其中s,t∈[0,1],变换为纹理中对应的采样位置。该变换补偿了图像流中任何可能
//        //导致与传统OpenGL ES纹理有差异的属性。例如，从图形的左下角开始采样，可能通过使用查询得到的矩阵来变换列向量(0,0,0,1)
//        //而从右上角采样可以通过变换(1,1,0,1)来得到。
//        surfaceTexture.getTransformMatrix(mtx);
//        cameraImageFilter.setmTextureTransformMatrix(mtx);
//
//
//        int textureId = cameraImageFilter.onDrawToTexture(this.textureId);
        GLES20.glViewport(0,0,imageWidth, imageHeight );
        gpuImageFilter.onDrawFrame(textureId, glCubeBuffer, glTextureBuffer);
//        gpuImageFilter.onDrawFrame(textureId);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0,0, width, height);
        surfaceWidth = width;
        surfaceHeight = height;
        onFilterChanged();
        Log.e(TAG,"surfaceWidth=" + width + " surfaceHeight="+ height);
    }

    protected void onFilterChanged(){
        cameraImageFilter.onDisplaySizeChanged(imageWidth, imageHeight);
        Log.e(TAG,"imageWidth=" + imageWidth + " imageHeight="+ imageHeight);
        cameraImageFilter.initCameraFrameBuffer(imageWidth, imageHeight);
        cameraImageFilter.onOutputSizeChanged(imageWidth,imageHeight);
        gpuImageFilter.onOutputSizeChanged(imageWidth, imageHeight);
    }

    public void setTextureId(int textureId,FloatBuffer glCubeBuffer, FloatBuffer glTextureBuffer) {
        this.textureId = textureId;
//        this.glCubeBuffer.clear();
//        this.glCubeBuffer.put(glCubeBuffer).position(0);
//        this.glTextureBuffer.clear();
//        this.glTextureBuffer.put(glTextureBuffer).position(0);
        Log.e(TAG, "setTextureId="+ textureId);
    }

    private SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            glSurfaceView.requestRender();
            Log.e("onFrame","onFrameAvailable" + surfaceTexture);
        }
    };

    public void attachFrameAvailableListener(){
        surfaceTexture.setOnFrameAvailableListener(onFrameAvailableListener);
    }

    public void detachFrameAvailableListener(){
        surfaceTexture.setOnFrameAvailableListener(null);
    }

    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    public void setImageWidthAndHeight(int imageWidth, int imageHeight){
        float preivewRatio = imageWidth * 1.0f / imageHeight;
        int targetWidth = surfaceWidth;
        int targetHeight = (int)(surfaceWidth / preivewRatio);
        if(targetHeight < surfaceHeight){
            targetHeight = surfaceHeight;
            targetWidth = (int)(targetHeight * preivewRatio);
        }
        this.imageHeight = targetHeight;
        this.imageWidth = targetWidth;
        Log.e(TAG,"targetWidth=" + targetWidth + " targetHeight="+ targetHeight);
        cameraImageFilter.initCameraFrameBuffer(targetWidth, targetHeight);
        cameraImageFilter.onOutputSizeChanged(targetWidth,targetHeight);
        gpuImageFilter.onOutputSizeChanged(targetWidth, targetHeight);
    }

    public void updatePreviewSize(){
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) glSurfaceView.getLayoutParams();
        layoutParams.height = imageHeight;
        layoutParams.width = imageWidth;
        if(surfaceWidth > imageWidth){
            layoutParams.leftMargin = - (surfaceWidth - imageWidth) /2;
        }
        if(surfaceHeight > imageHeight){
            layoutParams.topMargin =  (surfaceHeight - imageHeight) /2;
        }
        glSurfaceView.setLayoutParams(layoutParams);
    }

//    private void addAdjust

    public void destroy(){
        gpuImageFilter.destroy();
        cameraImageFilter.destroy();
        cameraImageFilter.destoryFramebuffers();
        /**
         * 释放所有的缓冲区，将SurfaceTexture置为abandoned状态。一旦进入此状态，SurfaceTexture
         * 将不会离开此状态，一旦进入abandoned状态，ISurfaceTexture接口的所有方法都将返回NO_INIT错误。
         * 注意：调用此方法时，从SurfaceTexture的角度看，所有的缓冲区都被释放，如果仍然有其他的到缓冲区的引用
         * (如，一缓冲区被客户端引用，或者被OpenGL ES作为纹理引用)，那么该缓冲区将仍存在。当使用完SurfaceTexture之后，
         * 一定要调用次方法，否则将会导致资源重新分配被延误很长时间。
         */
        surfaceTexture.release();
    }

}
