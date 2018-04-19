package com.example.cwl.cameradraw.openGL;

import android.graphics.PointF;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;

import javax.microedition.khronos.opengles.GL;

/**
 * Created by cwl on 18/3/29.
 */

public class GPUImageFilter {

    /**
     * GLSL简单的数据类型
     * float
     * bool: flase or true
     * int
     * 向量
     * vec {2,3,4} 长度为2，3，4的float向量
     * bvec{2,3,4} 长度为2，3，4的bool向量
     * ivec{2,3,4} 长度为2，3，4的int向量
     * 矩阵：
     * mat2 2*2的浮点矩阵
     * mat3 3*3的浮点矩阵
     * mat4 4*4的浮点矩阵
     * 可用matm*n也能表示m*n的浮点矩阵
     *
     * 取样器-用于纹理采样
     * samplerND 访问一个N为纹理
     * samplerCube 访问一个立方体纹理
     * samplerNDShadow 访问一个带对比的N维深度纹理
     *
     * const-- 用于声明非写的编译时的常量变量
     * attribute-- 用于经常更改的信息，只可以在顶点着色器中使用
     * uniform --用于不经常更改的信息 ，用于顶点和片元着色器
     * varing--用于从顶点着色器传递到片元着色器的插值信息
     *
     *
     * //gl_Position	vec4	输出属性-变换后的顶点的位置，用于后面的固定的裁剪等操作。所有的顶点着色器都必须写这个值
     * gl_FragColor	vec4	输出的颜色用于随后的像素操作
     * https://www.cnblogs.com/mazhenyu/p/3804518.html
     *
     * 运算的精度 highp mediump lowp
     */
    public static final String NO_FILTER_VERTEX_SHADER = ""+
            "attribute vec4 position;\n"+
            "attribute vec4 inputTextureCoordinate;\n"+
            " \n"+
            "varying vec2 textureCoordinate;\n"+
            " \n"+
            "void main()\n"+
            "{\n"+
            "   gl_Position = position;\n"+
            "   textureCoordinate = inputTextureCoordinate.xy;\n"+
            "}";

    public static final String NO_FILTER_FRAMENT_SHADER = ""+
            "varying highp vec2 textureCoordinate;\n"+
            " \n"+
            "uniform sampler2D inputImageTexture;\n"+//uniform是外部程序传递给shader的变量，通过glUniform来赋值的
            " \n"+
            "void main()\n"+
            "{\n"+
            "   gl_FragColor = texture2D(inputImageTexture,textureCoordinate);\n"+
            "}";

    private final LinkedList<Runnable> mRunOnDraw;
    private final String mVertexShader;
    private final String mFragmentShader;
    protected int mGLProgId;
    protected int mGLAttribPosition;
    protected int mGLUniformTexture;
    protected int mGLAttribTextureCoordinate;
    protected int mGLStrengthLocation;
    protected int mOutputWidth;
    protected int mOutputHeight;
    protected boolean mIsInitialized;
    protected FloatBuffer mGLCubeBuffer;
    protected FloatBuffer mGLTextureBuffer;
    protected int mSurfaceWidth,mSurfaceHeight;

    public GPUImageFilter(){
        this(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAMENT_SHADER);
    }

    public GPUImageFilter(final String vertexShader, final String fragmentShader){
        mRunOnDraw = new LinkedList<Runnable>();
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;

        mGLCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length*4).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(TextureRotationUtil.CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLTextureBuffer.put(TextureRotationUtil.getTargetRatedText()).position(0);
    }

    public void init(){
        onInit();
        mIsInitialized = true;
        onInitialized();
    }

    protected void onInit(){
        mGLProgId = OpenGLUtils.loadProgram(mVertexShader, mFragmentShader);
        mGLAttribPosition = GLES20.glGetAttribLocation(mGLProgId,"position");
        mGLUniformTexture = GLES20.glGetUniformLocation(mGLProgId, "inputImageTexture");
        mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(mGLProgId,"inputTextureCoordinate");
        mGLStrengthLocation = GLES20.glGetUniformLocation(mGLProgId, "strength");
        mIsInitialized = true;
    }

    protected void onInitialized(){
        setFloat(mGLStrengthLocation, 1.0f);
    }

    public final void destroy(){
        mIsInitialized = false;
        GLES20.glDeleteProgram(mGLProgId);
        onDestory();
    }

    protected void onDestory(){

    }

    public void onOutputSizeChanged(final int width, final int height){
        mOutputWidth = width;
        mOutputHeight = height;
    }

    protected static int [] mFrameBuffers = null;
    protected static int[] mFrameBufferTextures = null;
    private int mFrameWidth = -1;
    private int mFrameHeight = -1;

    public int onDrawFrame(final int textureId, final FloatBuffer cubeBuffer, final FloatBuffer textureBuffer){
        try{
            GLES20.glUseProgram(mGLProgId);
            runPendingOnDrawTask();
            if(!mIsInitialized){
                return OpenGLUtils.NO_INIT;
            }

            cubeBuffer.position(0);
            GLES20.glVertexAttribPointer(mGLAttribPosition,2,GLES20.GL_FLOAT, false, 0, cubeBuffer);
            GLES20.glEnableVertexAttribArray(mGLAttribPosition);
            textureBuffer.position(0);
            GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,textureBuffer);
            GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
            if(textureId != OpenGLUtils.NO_TEXTURE){
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
                GLES20.glUniform1i(mGLUniformTexture, 0);
            }
            onDrawArraysPre();
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            GLES20.glDisableVertexAttribArray(mGLAttribPosition);

            GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
            onDrawArraysAfter();
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            return OpenGLUtils.ON_DRAW;
        }catch (Exception e){
            Log.e("GLES20_Live", "onDrawFrame Exception:" + e.getMessage());
        }

        return OpenGLUtils.ON_DRAW;
    }

    public int onDrawFrame(final int textureId){
        GLES20.glUseProgram(mGLProgId);
        runPendingOnDrawTask();
        if(!mIsInitialized)
            return OpenGLUtils.NO_INIT;
        mGLCubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition,2, GLES20.GL_FLOAT, false, 0, mGLCubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);

        mGLTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, mGLTextureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        if(textureId != OpenGLUtils.NO_TEXTURE){
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(mGLUniformTexture,0);
        }
        onDrawArraysPre();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        onDrawArraysAfter();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return OpenGLUtils.ON_DRAW;
    }

    private void runPendingOnDrawTask(){
        while(!mRunOnDraw.isEmpty()){
            mRunOnDraw.removeFirst().run();
        }
    }

    protected void onDrawArraysPre(){

    }

    protected void onDrawArraysAfter(){

    }

    public boolean isIsInitialized() {
        return mIsInitialized;
    }

    public int getOutputHeight() {
        return mOutputHeight;
    }

    public int getOutputWidth() {
        return mOutputWidth;
    }

    public int getProgram(){
        return mGLProgId;
    }

    public int getAttribPosition(){
        return mGLAttribPosition;
    }

    public int getAttribTextureCoordinate(){
        return mGLAttribTextureCoordinate;
    }

    public int getUniformTexture(){
        return mGLUniformTexture;
    }

    protected void setInt(final int location ,final int intValue){
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1i(location, intValue);
            }
        });
    }

    protected void setFloat(final int location , final float floatValue){
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1f(location, floatValue);
            }
        });
    }

    protected void setFloatVec2(final int location ,final float[] arrayValue){
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec3(final int location ,final float[] arrayValue){
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform3fv(location, 1,FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec4(final int location ,final float[] arrayValue){
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatArray(final int location, final float[] arrayValue){
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1fv(location,arrayValue.length,FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setPoint(final int location, final PointF pointF){
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                float[] vec2 = new float[2];
                vec2[0] = pointF.x;
                vec2[1] = pointF.y;
                GLES20.glUniform2fv(location,1,vec2,0);
            }
        });
    }

    protected void setUniformMaxtrix3f(final int location, final float[] matrix){
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniformMatrix3fv(location,1, false, matrix, 0);
            }
        });
    }

    protected void setUniformMaxtrix4f(final int location ,final float[] matrix){
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniformMatrix4fv(location,1,false,matrix,0);
            }
        });
    }

    protected void runOnDraw(final Runnable runnable){
        synchronized (mRunOnDraw){
            mRunOnDraw.addLast(runnable);
        }
    }

    public void onDisplaySizeChanged(final int width , final int height){
        mSurfaceHeight = height;
        mSurfaceWidth = width;
    }

    public void setOutputSize(int width, int height){
        GLES20.glViewport(0,0,width,height);
        initCameraFrameBuffer(width, height);
    }

    private void initCameraFrameBuffer(int width, int height){
        if(mFrameBuffers != null && (mFrameWidth != width || mFrameHeight != height))
            destroyFrameBuffers();
        if(mFrameBuffers == null){
            mFrameHeight = height;
            mFrameWidth = width;
            mFrameBuffers = new int[1];
            mFrameBufferTextures = new int[1];

            GLES20.glGenFramebuffers(1, mFrameBuffers, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0 , GLES20.GL_RGBA, width, height, 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0],0);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0);
        }
    }

    private void destroyFrameBuffers(){
        if(mFrameBufferTextures != null){
            GLES20.glDeleteTextures(1, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if(mFrameBuffers != null){
            GLES20.glDeleteFramebuffers(1, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
        mFrameWidth = -1;
        mFrameHeight = -1;
    }
}
