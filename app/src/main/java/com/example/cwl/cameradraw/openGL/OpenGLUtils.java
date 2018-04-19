package com.example.cwl.cameradraw.openGL;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by cwl on 18/3/29.
 */

public class OpenGLUtils {

    public static final int NO_TEXTURE = -1;
    public static final int NO_INIT = -1;
    public static final int ON_DRAW = 1;

    private static final String TAG = OpenGLUtils.class.getSimpleName();

    public static int loadTexture(final Bitmap img, final int usedTexId){
        return loadTexture(img, usedTexId, false);
    }

    public static int loadTexture(final Bitmap img, final int usedTexId, boolean recyled){
        if(img == null)
            return NO_TEXTURE;
        int textures[] = new int[1];
        if(usedTexId == NO_TEXTURE){
            //生成纹理的对象 , 1.纹理的数量，2.纹理的对象，3.纹理的偏移
            GLES20.glGenTextures(1, textures, 0);
            //绑定纹理对象，1.纹理被绑定的目标，2.纹理的Id，且不能再次使用了
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            //设置纹理被缩小(距离视点很远时被缩小)时候的滤波方式
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            //设置纹理被放大(距离十点很近时被放大)时候的滤波方式
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            //设置纹理S方向上的贴图模式，GL_CLAMP_TO_EDGE截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            //设置纹理T方向上的贴图模式，GL_CLAMP_TO_EDGE截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            //定义一个二维纹理的映射，target-操作的目标类型 level-纹理的级别 bitmap-图像 border-边框
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, img, 0);
        }else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId);
            //提供修改图像的函数，修改纹理比创建纹理的开销要小很多
            //target-操作目标类型 level-纹理的级别 xoffset,yoffset要修改图像的左下角
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0 ,0, img);
            textures[0] = usedTexId;
        }

        if(recyled){
            img.recycle();
        }

        return textures[0];
    }

    public static int loadTexture(final Buffer data, final int width, final int height, final int usedTexId){
        if(data == null)
            return NO_TEXTURE;
        int textures[] = new int[1];
        if(usedTexId == NO_TEXTURE){
            //创建一个纹理的对象
            GLES20.glGenTextures(1, textures, 0);
            //绑定纹理对象
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            //设置纹理被缩小时候的滤波方式
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            //设置纹理被放大时候的滤波方式
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            //设置纹理s方向的贴图模式
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            //设置纹理t方向的贴图模式
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            //定义纹理对象的映射
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0 , GLES20.GL_RGBA, width, height, 0 , GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, data);
        }else {
            //绑定纹理对象
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId);
            //修改纹理对象的映射
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0,0,width, height,GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, data);
            textures[0] = usedTexId;
        }

        return textures[0];
    }

    public static int loadTexture(final Context context, final String name){
        final int[] textureHandle = new int[1];
        //创建纹理对象
        GLES20.glGenTextures(1, textureHandle, 0);

        if(textureHandle[0] != 0){
            //从assets目录读取图片文件
            final Bitmap bitmap = getImageFromAssetsFile(context, name);
            //绑定纹理对象
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
            //设置纹理被缩小时候的滤波方式，线性滤波
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            //设置纹理被放大时候的滤波方式，线性滤波
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            //设置纹理在S方向上的贴图模式
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            //设置纹理在T方向上的贴图模式
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            //定义二维纹理坐标的映射
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0 ,bitmap, 0);
            bitmap.recycle();
        }

        if(textureHandle[0] == 0){
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

    private static Bitmap getImageFromAssetsFile(Context context, String fileName){
        Bitmap image = null;
        AssetManager am = context.getResources().getAssets();
        try {
            InputStream is = am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return image;
    }

    public static int loadProgram(final String strVSource, final String strFSource){
        int iVshader;
        int iFShader;
        int iProgId;
        int[] link = new int[1];
        iVshader = loadShader(strVSource, GLES20.GL_VERTEX_SHADER);
        if(iVshader == 0){
            return 0;
        }
        iFShader = loadShader(strFSource, GLES20.GL_FRAGMENT_SHADER);
        if(iFShader == 0){
            return 0;
        }
        iProgId = GLES20.glCreateProgram();

        GLES20.glAttachShader(iProgId, iVshader);
        GLES20.glAttachShader(iProgId, iFShader);

        GLES20.glLinkProgram(iProgId);

        GLES20.glGetProgramiv(iProgId, GLES20.GL_LINK_STATUS, link, 0);
        Log.e(TAG,"link" + link[0]);
        if(link[0] <= 0){
            Log.e(TAG,"link fail" + link[0]);
            return 0;
        }

        GLES20.glDeleteShader(iVshader);
        GLES20.glDeleteShader(iFShader);

        return iProgId;
    }

    private static int loadShader(final String strSource , final int iType){
        int[] complied = new int[1];
        //根据具体的着色器类型来创建着色器
        int iShader = GLES20.glCreateShader(iType);
        //加载着色器的代码
        GLES20.glShaderSource(iShader, strSource);
        //编译着色器的代码
        GLES20.glCompileShader(iShader);
        //获取着色器的编译的状态
        GLES20.glGetShaderiv(iShader, GLES20.GL_COMPILE_STATUS, complied, 0);
        if(complied[0] == 0) {
            Log.e(TAG, "Compilation" + GLES20.glGetShaderInfoLog(iShader));
            return 0;
        }

        return iShader;
    }

    public static int getExternalOESTextrueID(){
        int[] texture = new int[1];
        //创建纹理对象
        GLES20.glGenTextures(1, texture, 0);
        //绑定纹理对象到texture2D OES 从一个图像流中捕获图像帧作为OpenGL ES纹理
        /**
         * 纹理对象使用GL_TEXTURE_EXTERNAL_OES作为纹理目标，其是OpenGLES扩展GL_OES_EGL_image_external定义的。
         * 这种纹理目标会对纹理的使用造成一些限制。每次纹理绑定的时候，都要绑定到GL_TEXTURE_EXTERNAL_OES，而不是GL_TEXTURE_2D。
         * 而且，任何需要从纹理中采样的OpenGL ES2.0shader都需要声明其对此扩展的使用，例如需要使用指令
         * #extension GL_OES_EGL_image_external:require。这些shader也必要使用sampleExternalOES采样方式来访问纹理。
         */
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        //设置texture_external_os的纹理被缩小的时候的滤波方式
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        //设置texture_external_os的纹理被放大的时候的滤波方式
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        //设置texture_external_os的纹理s方向上的贴图模式
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        //设置texture_external_os的纹理T方向上的贴图方式
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        Log.e(TAG, "texture OES=" + texture[0]);
        return texture[0];
    }

    public static String readShaderFromRawResource(final Context context, final int resoureId){
        final InputStream inputStream = context.getResources().openRawResource(resoureId);
        final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        String nextLine;
        final StringBuilder body = new StringBuilder();

        try{
            while((nextLine = bufferedReader.readLine()) != null){
                body.append(nextLine);
                body.append('\n');
            }
        }catch (IOException e){
            return null;
        }

        return body.toString();
    }

}
