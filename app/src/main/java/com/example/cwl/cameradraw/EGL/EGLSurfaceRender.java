package com.example.cwl.cameradraw.EGL;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cwl on 18/4/17.
 */

public abstract class EGLSurfaceRender {

    private List<EGLSurface> outputSurfaces = new ArrayList<EGLSurface>();

    private final  EGLRenderThread eglRenderThread;

    private final int renderId;

    public EGLSurfaceRender(EGLRenderThread eglRenderThread){
        this.eglRenderThread = eglRenderThread;
        renderId = this.hashCode();
    }

    public abstract void onCreated();

    public abstract void onUpdate();

    public abstract void onDrawFrame(EGLSurface outputSurface);

    public abstract void onDestroy();

    public void addSurface(EGLSurface eglSurface){
        if(eglRenderThread != null){
            eglRenderThread.addSurface(renderId, eglSurface);
        }
    }

    public void removeSurface(EGLSurface eglSurface){
        if(eglRenderThread != null){
            eglRenderThread.removeSurface(renderId, eglSurface);
        }
    }

    public void requestRender(){
        if(eglRenderThread != null){
            eglRenderThread.requestRender();
        }
    }


    public List<EGLSurface> getOutputSurfaces() {
        return outputSurfaces;
    }

    public int getRenderId() {
        return renderId;
    }
}
