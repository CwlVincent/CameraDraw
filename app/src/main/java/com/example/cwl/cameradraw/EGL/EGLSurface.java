package com.example.cwl.cameradraw.EGL;

import android.opengl.EGL14;
import android.view.Surface;

/**
 * Created by cwl on 18/4/16.
 */

public class EGLSurface {

    public static final int TYPE_WINDOW_SURFACE = 0;

    public static final int TYPE_PBUFFER_SURFACE = 1;

    public static final int TYPE_PIXMAP_SURFACE = 2;

    protected final int type;

    protected Object surface;

    protected android.opengl.EGLSurface eglSurface = EGL14.EGL_NO_SURFACE;

    public ViewPort viewport = new ViewPort();

    public EGLSurface(int width, int height){
        this(null, width,height);
    }

    public EGLSurface(Surface surface , int width, int height){
        this(surface, 0,0,width,height,0, TYPE_PBUFFER_SURFACE);
    }

    public EGLSurface(Surface surface, int x, int y, int width, int height,int pixMaps, int type){
        setViewport(x,y,width,height,pixMaps);
        this.surface = surface;
        this.type = type;
    }

    public void setViewport(int x, int y, int width, int height, int pixMaps){
        viewport.x = x;
        viewport.y = y;
        viewport.width = width;
        viewport.height = height;
        viewport.pixMap = pixMaps;
    }

    public void setViewport(ViewPort viewport){
        this.viewport = viewport;
    }

    public static class ViewPort{
        public int x;

        public int y;

        public int width;

        public int height;

        public int pixMap;
    }
}
