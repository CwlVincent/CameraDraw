package com.example.cwl.cameradraw.EGL;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import javax.microedition.khronos.egl.EGL10;

/**
 * Created by cwl on 18/4/16.
 */

public class EGLRenderThread extends Thread {

    private final static String TAG = EGLRenderThread.class.getSimpleName();

    private EGLConfig eglConfig = null;

    private EGLDisplay eglDisplay = EGL14.EGL_NO_DISPLAY;

    private EGLContext eglContext = EGL14.EGL_NO_CONTEXT;

    private LinkedBlockingQueue<Event> eventQueue;

    private final SparseArray<EGLSurfaceRender> outputSurfaces;

    private boolean rendering;

    private boolean isRelease;

    private EGLContext sharedEGLContext = EGL14.EGL_NO_CONTEXT;

    private javax.microedition.khronos.egl.EGLContext sharedEGLContext10 = EGL10.EGL_NO_CONTEXT;

    public EGLRenderThread(){
        setName(TAG+"-"+getId());
        outputSurfaces = new SparseArray<EGLSurfaceRender>();
        rendering = false;
        isRelease = false;

        eventQueue = new LinkedBlockingQueue<Event>(1000);
    }

    private boolean makeOutputSurface(EGLSurface surface){
        try{
            switch (surface.type){
                case EGLSurface.TYPE_WINDOW_SURFACE:{
                    final int[] atrributes = {EGL14.EGL_NONE};
                    surface.eglSurface = EGL14.eglCreateWindowSurface(eglDisplay,eglConfig,surface.surface, atrributes, 0);
                }
                break;
                case EGLSurface.TYPE_PBUFFER_SURFACE:{
                    final int[] atrributes = {EGL14.EGL_WIDTH, surface.viewport.width,
                    EGL14.EGL_HEIGHT, surface.viewport.height, EGL14.EGL_NONE};
                    surface.eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig,atrributes, 0);
                }
                break;
                case EGLSurface.TYPE_PIXMAP_SURFACE:{
                    final int[] attributes = {EGL14.EGL_PIXMAP_BIT, surface.viewport.pixMap};
                    surface.eglSurface = EGL14.eglCreatePixmapSurface(eglDisplay, eglConfig, 0, attributes, 0);
                }
                break;

            }
        }catch (Exception e){
            surface.eglSurface = EGL14.EGL_NO_SURFACE;
            return false;
        }
        return false;
    }

    public void addSurface(@NonNull int renderId,@NonNull final EGLSurface surface){
        Event event = new Event(Event.ADD_SURFACE);
        event.param = surface;
        event.arg1 = renderId;
        if(!eventQueue.offer(event))
            Log.e(TAG, "queue full");
    }

    public void removeSurface(@NonNull int renderId,@NonNull final EGLSurface surface){
        Event event = new Event(Event.REMOVE_SURACE);
        event.param = surface;
        event.arg1 = renderId;
        if(!eventQueue.offer(event))
            Log.e(TAG, "queue full");
    }

    public void addEGLRender(@NonNull final EGLSurfaceRender eglSurfaceRender){
        Event event = new Event(Event.ADD_RENDER);
        event.param = eglSurfaceRender;
        if(!eventQueue.offer(event))
            Log.e(TAG, "queue full");
    }

    public void removeEGLRender(@NonNull final int renderId , @NonNull final EGLSurfaceRender eglSurfaceRender){
        Event event = new Event(Event.REMOVE_RENDER);
        event.arg1 = renderId;
        event.param = eglSurfaceRender;
        if(!eventQueue.offer(event)){
            Log.e(TAG, "queue full");
        }
    }

    public void startRender(){
        if(!eventQueue.offer(new Event(Event.START_RENDER)))
            Log.e(TAG, "queue full");
        if(getState() == State.NEW){
            super.start();
        }
    }

    public void stopRender(){
        if(!eventQueue.offer(new Event(Event.STOP_RENDER)))
            Log.e(TAG, "queue full");
    }

    public boolean postRunnable(@NonNull Runnable runnable){
        Event event = new Event(Event.RUNNABLE);
        event.param = runnable;
        if(!eventQueue.offer(event)){
            Log.e(TAG, "queue full");
            return false;
        }
        return true;
    }

    public void start(){
        Log.w(TAG, "Don't call this function");
    }

    public void requestRender(){
        eventQueue.offer(new Event(Event.REQ_RENDER));
    }

    public void setSharedEGLContext(EGLContext sharedEGLContext) {
        this.sharedEGLContext = sharedEGLContext;
    }

    public EGLContext getSharedEGLContext() {
        return sharedEGLContext;
    }

    private void createGL(EGLContext sharedEGLContext){
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        int[] version = new int[2];
        if(!EGL14.eglInitialize(eglDisplay, version, 0 ,version, 1)){
            throw new RuntimeException("EGL error" + EGL14.eglGetError());
        }
        int[] configAttribs = {
                EGL14.EGL_BUFFER_SIZE, 32,
                EGL14.EGL_ALPHA_SIZE,8,
                EGL14.EGL_BLUE_SIZE,8,
                EGL14.EGL_GREEN_SIZE,8,
                EGL14.EGL_RED_SIZE,8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                0x3142, 1,
                EGL14.EGL_NONE
        };
        int[] numConfigs = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        if(!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs,0, configs.length, numConfigs,0 )){
            throw new RuntimeException("EGL error "+ EGL14.eglGetError());
        }
        eglConfig = configs[0];
        int[] contextAttribs = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION,2,
                EGL14.EGL_NONE
        };
        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, sharedEGLContext, contextAttribs,0);
        if(eglContext == EGL14.EGL_NO_CONTEXT){
            throw  new RuntimeException("EGL error" + EGL14.eglGetError());
        }
        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, eglContext);
    }

    public EGLConfig getEglConfig() {
        return eglConfig;
    }

    private void destroyGL(){
        EGL14.eglDestroyContext(eglDisplay, eglContext);
        eglContext = EGL14.EGL_NO_CONTEXT;
        eglDisplay = EGL14.EGL_NO_DISPLAY;
    }

    private void render(){
        for(int index = 0 ; index < outputSurfaces.size(); index++){
            int renderId = outputSurfaces.keyAt(index);
            EGLSurfaceRender eglSurfaceRender = outputSurfaces.get(renderId);
            if(eglSurfaceRender.getOutputSurfaces() != null){
                for(EGLSurface output : eglSurfaceRender.getOutputSurfaces()){
                    if(output.eglSurface == EGL14.EGL_NO_SURFACE){
                        if(!makeOutputSurface(output))
                            continue;
                    }
                    EGL14.eglMakeCurrent(eglDisplay, output.eglSurface, output.eglSurface,eglContext);
                    GLES20.glViewport(output.viewport.x, output.viewport.y, output.viewport.width, output.viewport.height);
                    eglSurfaceRender.onDrawFrame(output);
//                    logCurrent();
                    EGL14.eglSwapBuffers(eglDisplay, output.eglSurface);
                }
            }
        }
    }

    public void logCurrent() {
        int msg = EGL14.eglGetError();
        EGLDisplay display;
        EGLContext context;
        android.opengl.EGLSurface surface;

        display = EGL14.eglGetCurrentDisplay();
        context = EGL14.eglGetCurrentContext();
        surface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
        Log.i(TAG, "Current EGL (" + msg + "): display=" + display + ", context=" + context +
                ", surface=");
    }

    @Override
    public void run() {
        Event event;

        Log.d(TAG, getName() + ":render create");
        createGL(sharedEGLContext);

        if(onCreateGLListener != null){
            onCreateGLListener.onEGLCreated();
        }

        while(!isRelease){
            try{
                event = eventQueue.take();
                switch (event.event){
                    case Event.ADD_SURFACE: {
                        EGLSurface surface = (EGLSurface) event.param;
                        int renderId = event.arg1;
                        Log.d(TAG, "add:" + surface);
                        makeOutputSurface(surface);
                        EGLSurfaceRender eglSurfaceRender = outputSurfaces.get(renderId);
                        if(eglSurfaceRender != null){
                            eglSurfaceRender.getOutputSurfaces().add(surface);
                        }else{
                            Log.e(TAG, "add surface error renderId not exits");
                        }
                        break;
                    }
                    case Event.REMOVE_SURACE:{
                        EGLSurface surface = (EGLSurface) event.param;
                        int renderId = event.arg1;
                        Log.d(TAG, "remove" + surface);
                        EGL14.eglDestroySurface(eglDisplay, surface.eglSurface);
                        surface.eglSurface = EGL14.EGL_NO_SURFACE;
                        EGLSurfaceRender eglSurfaceRender = outputSurfaces.get(renderId);
                        if(eglSurfaceRender != null){
                            eglSurfaceRender.getOutputSurfaces().remove(surface);
                        }else{
                            Log.e(TAG, "remove surface error renderId not exits");
                        }
                    }break;
                    case Event.ADD_RENDER:
                        EGLSurfaceRender surfaceRender = (EGLSurfaceRender) event.param;
                        surfaceRender.onCreated();
                        outputSurfaces.put(surfaceRender.getRenderId(), surfaceRender);
                        break;
                    case Event.REMOVE_RENDER:
                        int renderId =  event.arg1;
                        EGLSurfaceRender eglSurfaceRender = (EGLSurfaceRender) event.param;
                        eglSurfaceRender.onDestroy();
                        for(EGLSurface eglSurface: eglSurfaceRender.getOutputSurfaces()){
                            EGL14.eglDestroySurface(eglDisplay, eglSurface.eglSurface);
                            eglSurface.eglSurface = EGL14.EGL_NO_SURFACE;
                        }
                        outputSurfaces.remove(renderId);
                        break;
                    case Event.START_RENDER:
                        rendering = true;
                        break;
                    case Event.REQ_RENDER:
                        if(rendering){
                            onUpdate();
                            render();
                        }
                        break;
                    case Event.STOP_RENDER:
                        rendering = false;
                        break;
                    case Event.RUNNABLE:
                        ((Runnable)event.param).run();
                        break;
                    case Event.RELEASE:
                        isRelease = true;
                        break;
                    default:
                        Log.e(TAG, "event error" + event);
                        break;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        for(int index = 0 ; index < outputSurfaces.size(); index++){
            int renderId = outputSurfaces.keyAt(index);
            EGLSurfaceRender eglSurfaceRender = outputSurfaces.get(renderId);
            for(EGLSurface outputSurface:eglSurfaceRender.getOutputSurfaces()){
                EGL14.eglDestroySurface(eglDisplay, outputSurface.eglSurface);
                outputSurface.eglSurface = EGL14.EGL_NO_SURFACE;
            }
        }

        destroyGL();
        eventQueue.clear();
        Log.d(TAG, getName()+":render release");
    }

    public void release(){
        if(eventQueue.offer(new Event(Event.RELEASE))){
            while(isAlive()){
                try{
                    this.join(1000);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }

    private OnCreateGLListener onCreateGLListener;

    public interface OnCreateGLListener {
        public void onEGLCreated();
    }

    public void setOnCreateGLListener(OnCreateGLListener onCreateGLListener) {
        this.onCreateGLListener = onCreateGLListener;
    }

    private void onUpdate(){
        for(int index = 0 ; index < outputSurfaces.size(); index++){
            int renderId = outputSurfaces.keyAt(index);
            EGLSurfaceRender eglSurfaceRender = outputSurfaces.get(renderId);
            eglSurfaceRender.onUpdate();
        }
    }

    private static String getEGLErrorString(){
        return GLUtils.getEGLErrorString(EGL14.eglGetError());
    }

    private static class Event{
        static final int ADD_SURFACE = 1;//添加输出的surface
        static final int REMOVE_SURACE = 2;
        static final int START_RENDER = 3;
        static final int REQ_RENDER = 4;
        static final int STOP_RENDER = 5;
        static final int RUNNABLE = 6;
        static final int RELEASE = 7;
        static final int ADD_RENDER = 8;
        static final int REMOVE_RENDER = 9;

        final int event;
        Object param;
        int arg1;

        Event(int event){
            this.event = event;
        }
    }
}
