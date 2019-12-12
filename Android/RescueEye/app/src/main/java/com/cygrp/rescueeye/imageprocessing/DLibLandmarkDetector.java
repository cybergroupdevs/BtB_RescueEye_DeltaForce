package com.cygrp.rescueeye.imageprocessing;

import android.content.Context;
import android.graphics.Rect;

import com.cygrp.rescueeye.Native;
import com.cygrp.rescueeye.R;
import com.cygrp.rescueeye.utility.Constants;
import com.cygrp.rescueeye.utility.Utility;

import java.io.File;

public class DLibLandmarkDetector {

    private Context mContext;

    public DLibLandmarkDetector(Context context){
        mContext = context;
    }

    public boolean loadDlibModel() throws Exception {

        try {
            String modelFilePath = Utility.getFaceShapeModelPath(Constants.DLIB_FACE_MODEL_NAME);
            if (!new File(modelFilePath).exists()) {
                Utility.copyFileFromRawToOthers(mContext, R.raw.shape_predictor_68_face_landmarks, modelFilePath);
            }
            Native.loadModel(modelFilePath);
            return true;
        } catch (Exception e){
            throw e;
        }
    }

    public long[] detectLandMarks(
            byte[] mFrame, int width, int height, int rotation, Rect rect) throws Exception {
        try {
            return Native.analiseFrame(mFrame, rotation, width, height, rect);
        } catch (Exception e) {
            throw e;
        }
    }
}
