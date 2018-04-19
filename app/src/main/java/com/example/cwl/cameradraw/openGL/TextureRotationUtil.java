package com.example.cwl.cameradraw.openGL;

/**
 * Created by cwl on 18/3/29.
 */

public class TextureRotationUtil {

    public static final float TEXTURE_NO_ROTATION[] = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
    };

    public static final float TEXTURE_NO_ROTATION_90[] = {
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
    };

    public static final float TEXTURE_NO_ROTATION_180[] = {
            1.0f, 0.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
    };

    public static final float TEXTURE_NO_ROTATION_270[] = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };

    public static final float CUBE[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };

    private TextureRotationUtil(){}

    public static float[] getRotation(final Rotation rotation, final boolean
                                      flipHorizontal, final boolean flipViertical){
        float[] rotatedTex;
        switch (rotation){
            case ROTATION_90:
                rotatedTex = TEXTURE_NO_ROTATION_90;
                break;
            case ROTATION_180:
                rotatedTex = TEXTURE_NO_ROTATION_180;
                break;
            case ROTATION_270:
                rotatedTex = TEXTURE_NO_ROTATION_270;
                break;
            case NORMAL:
                default:
                    rotatedTex = TEXTURE_NO_ROTATION;
                    break;
        }
        if(flipHorizontal){
            rotatedTex = new float[]{
                            flip(rotatedTex[0]), flip(rotatedTex[1]),
                            flip(rotatedTex[2]), flip(rotatedTex[3]),
                            flip(rotatedTex[4]), flip(rotatedTex[5]),
                            flip(rotatedTex[6]), flip(rotatedTex[7]),
            };
        }

        if(flipViertical){
            rotatedTex = new float[]{
                    flip(rotatedTex[0]), flip(rotatedTex[1]),
                    flip(rotatedTex[2]), flip(rotatedTex[3]),
                    flip(rotatedTex[4]), flip(rotatedTex[5]),
                    flip(rotatedTex[6]), flip(rotatedTex[7])
            };
        }

        return rotatedTex;
    }

    public static float[] getTargetRatedText(){
        float[] rotated = new float[]{
                0.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f
        };
        return rotated;
    }

    public static float[] cropSize(float[] rotated, int surfaceW, int surfaceH, int imageH, int imageW){
        int cropX = Math.abs(imageW - surfaceW)/2;
        int cropY = Math.abs(imageH - surfaceH)/2;
        float pointCropX = cropX*1.0f/imageW;
        float pointCropY = cropY*1.0f/imageH;

        rotated = new float[]{
            rotated[0] + pointCropX, rotated[1] + pointCropY,
            rotated[2] - pointCropX, rotated[3] + pointCropY,
            rotated[4] + pointCropX, rotated[5] - pointCropY,
            rotated[6] - pointCropX, rotated[7] - pointCropY
        };

        return rotated;
    }

    public static float[] getCripCube(float cripSize, boolean vertical){
        float[] cripCenter = TextureRotationUtil.CUBE;
        if(cripSize < 0 || cripSize > 1){
            return cripCenter;
        }
        if(vertical){
            cripCenter = new float[]{
                    cripCenter[0], cripCenter[1] + cripSize,
                    cripCenter[2], cripCenter[3] + cripSize,
                    cripCenter[4], cripCenter[5] - cripSize,
                    cripCenter[6], cripCenter[7] - cripSize
            };
        }else {
            cripCenter = new float[]{
                    cripCenter[0] + cripSize, cripCenter[1],
                    cripCenter[2] - cripSize, cripCenter[3],
                    cripCenter[4] + cripSize, cripCenter[5],
                    cripCenter[6] - cripSize, cripCenter[7]
            };
        }
        return cripCenter;
    }

    private static float flip(final float i){
        if(i == 0.0f){
            return 1.0f;
        }
        return 0.0f;
    }
}
