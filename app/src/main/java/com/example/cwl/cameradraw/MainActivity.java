package com.example.cwl.cameradraw;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.cwl.cameradraw.EGL.EGLRenderThread;
import com.example.cwl.cameradraw.EGL.EGLSurface;
import com.example.cwl.cameradraw.openGL.OpenGLUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

import static android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Button openBtn;

    private Button closeBtn;

    private Button switchBtn;

    private Button addBtn;

    private Button removeBtn;

    private SurfaceView previewSurfaceView;

    private GLSurfaceView glSurfaceView;

    private GLSurfaceViewRender glSurfaceViewRender;

    private EGLRenderThread eglRenderThread;

    private CameraEGLRender cameraEGLRender = null;

    private LinearLayout linearLayout;

    private LinkedList<SurfaceView> surfaceViewList = new LinkedList<>();

    private int textureId = OpenGLUtils.NO_TEXTURE;

    private SurfaceTexture surfaceTexture;

//    private EGLRenderThread eglRenderThread1;
//
//    private NormalEGLRender cameraEGLRender1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initListener();

        requestPerssion();

        CameraManager.show(this);

    }

    private void initView(){
        linearLayout = findViewById(R.id.surface_list);
        openBtn = findViewById(R.id.open_camera);
        closeBtn = findViewById(R.id.close_camera);
        switchBtn = findViewById(R.id.switch_camera);
        previewSurfaceView = findViewById(R.id.camera_preview);
        glSurfaceView = findViewById(R.id.camera_one);
        addBtn = findViewById(R.id.add_view);
        removeBtn = findViewById(R.id.remove_view);
    }

    @SuppressLint("WrongViewCast")
    private void initListener(){
        openBtn.setOnClickListener(this);
        closeBtn.setOnClickListener(this);
        switchBtn.setOnClickListener(this);
        addBtn.setOnClickListener(this);
        removeBtn.setOnClickListener(this);

        previewSurfaceView.setVisibility(View.VISIBLE);

//        CameraManager.setPreviewCallback(new Camera.PreviewCallback() {
//            @Override
//            public void onPreviewFrame(byte[] data, Camera camera) {
//                Log.e("previewData", "data.length="+ data.length);
//            }
//        });

//        eglRenderThread1 = new EGLRenderThread();
//        cameraEGLRender1 = new NormalEGLRender(eglRenderThread1,this);
//        eglRenderThread1.addEGLRender(cameraEGLRender1);

        glSurfaceView.setEGLContextFactory(new GLSurfaceView.EGLContextFactory() {
            @Override
            public javax.microedition.khronos.egl.EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
                int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2,
                        EGL10.EGL_NONE };

                javax.microedition.khronos.egl.EGLContext eglContext = ((EGL10)javax.microedition.khronos.egl.EGLContext.getEGL()).eglGetCurrentContext();

                javax.microedition.khronos.egl.EGLContext eglContext1 = egl.eglGetCurrentContext();

                return egl.eglCreateContext(display, eglConfig, eglContext1 ,
                        attrib_list);
            }

            @Override
            public void destroyContext(EGL10 egl, EGLDisplay display, javax.microedition.khronos.egl.EGLContext context) {
                if (!egl.eglDestroyContext(display, context)) {
                    Log.e("DefaultContextFactory", "display:" + display + " context: " + context);
                }
            }
        });

        glSurfaceViewRender = new GLSurfaceViewRender(glSurfaceView);



        eglRenderThread = new EGLRenderThread();
        cameraEGLRender = new CameraEGLRender(eglRenderThread);
        eglRenderThread.addEGLRender(cameraEGLRender);

        eglRenderThread.setOnCreateGLListener(new EGLRenderThread.OnCreateGLListener() {
            @Override
            public void onEGLCreated() {
//                eglRenderThread1.setSharedEGLContext(EGL14.eglGetCurrentContext());
//                eglRenderThread1.startRender();
                glSurfaceView.setEGLContextClientVersion(2);
                glSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
                glSurfaceView.setRenderer(glSurfaceViewRender);
                glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            }
        });
        eglRenderThread.startRender();











        previewSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            EGLSurface eglSurface1 = null;
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                eglSurface1 = new EGLSurface(holder.getSurface(),0,0,width,height,0 ,EGLSurface.TYPE_WINDOW_SURFACE);
                cameraEGLRender.addSurface(eglSurface1);
               cameraEGLRender.addSurface(new EGLSurface(width, height));

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraEGLRender.removeSurface(eglSurface1);
            }
        });

        cameraEGLRender.setOnDrawFrameListener(new CameraEGLRender.OnDrawFrameListener() {
            @Override
            public void onDrawFrame(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer) {
                        glSurfaceViewRender.setTextureId(textureId, cubeBuffer, textureBuffer);
//                    cameraEGLRender1.setTextureId(textureId);
                    Log.e("OnDrawFrameListener","textureId="+ textureId);
//                    cameraEGLRender1.requestRender();
                glSurfaceView.requestRender();
            }
        });

//        SurfaceView surfaceView = findViewById(R.id.camera_one);
//        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
//            EGLSurface eglSurface1 = null;
//            @Override
//            public void surfaceCreated(SurfaceHolder holder) {
//
//            }
//
//            @Override
//            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//                eglSurface1 = new EGLSurface(holder.getSurface(),0,0,width,height,0 ,EGLSurface.TYPE_WINDOW_SURFACE);
//                cameraEGLRender1.addSurface(eglSurface1);
//                cameraEGLRender1.addSurface(new EGLSurface(width, height));
//
//            }
//
//            @Override
//            public void surfaceDestroyed(SurfaceHolder holder) {
//                cameraEGLRender1.removeSurface(eglSurface1);
//            }
//        });

    }

    @Override
    protected void onResume() {
        super.onResume();
//        glSurfaceViewRender.attachFrameAvailableListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        glSurfaceViewRender.detachFrameAvailableListener();
    }

    public final int CAMERA_OK = 0x11;

    public final int SYSTEM_ALERT_WINDOW = 0x12;

    private void requestPerssion(){
        int permissionResult = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA);
        if(permissionResult != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_OK);
        }
        int permissionSystemAlert = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SYSTEM_ALERT_WINDOW);
        if(permissionSystemAlert != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SYSTEM_ALERT_WINDOW}, SYSTEM_ALERT_WINDOW);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case CAMERA_OK:
                if(grantResults != null && grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(MainActivity.this,"获取到摄像头权限", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(MainActivity.this,"打不开摄像头权限", Toast.LENGTH_SHORT).show();;
                }
                break;
            case SYSTEM_ALERT_WINDOW:
                if(grantResults != null && grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(MainActivity.this,"获取到系统Alert权限", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(MainActivity.this,"打不开系统Alert权限", Toast.LENGTH_SHORT).show();;
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.open_camera:
                textureId = OpenGLUtils.getExternalOESTextrueID();
                surfaceTexture = new SurfaceTexture(textureId);
                cameraEGLRender.setSurfaceTexture(surfaceTexture);
                cameraEGLRender.setTextureId(textureId);
//                cameraEGLRender1.setTextureId(textureId);
                surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        cameraEGLRender.requestRender();
//                        cameraEGLRender1.requestRender();
//                        glSurfaceView.requestRender();
                    }
                });

                CameraManager.setRequestPreviewSize(368,640);
                CameraManager.openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
                CameraManager.startPreview(surfaceTexture);
                Rect rect = CameraManager.getRatioPreviewSize();
                glSurfaceViewRender.setImageWidthAndHeight(rect.width(), rect.height());
                cameraEGLRender.setImageSize(rect.width(),rect.height());
//                cameraEGLRender1.setImageSize(rect.width(), rect.height());
                break;
            case R.id.close_camera:
                CameraManager.release();
                break;
            case R.id.switch_camera:
                CameraManager.switchCamera();
                break;
            case R.id.add_view:
                SurfaceView surfaceView = new SurfaceView(MainActivity.this);
                surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                    EGLSurface eglSurface1 = null;
                    @Override
                    public void surfaceCreated(SurfaceHolder holder) {

                    }

                    @Override
                    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                        eglSurface1 = new EGLSurface(holder.getSurface(),0,0,width,height,0 ,EGLSurface.TYPE_WINDOW_SURFACE);
                        cameraEGLRender.addSurface(eglSurface1);
                    }

                    @Override
                    public void surfaceDestroyed(SurfaceHolder holder) {
                        cameraEGLRender.removeSurface(eglSurface1);
                    }
                });
                linearLayout.addView(surfaceView,new LinearLayout.LayoutParams(300,300));
                surfaceViewList.add(surfaceView);
                break;
            case R.id.remove_view:
                SurfaceView surfaceView1 = surfaceViewList.pollFirst();
                if(surfaceView1 == null)
                    return ;
                linearLayout.removeView(surfaceView1);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CameraManager.dismiss();
        CameraManager.release();
        eglRenderThread.release();
        cameraEGLRender.onDestroy();
    }
}
