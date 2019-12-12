package com.cygrp.rescueeye.imageprocessing;

import android.app.Activity;

import java.io.IOException;

public class RescueEyeImageProcessor extends ImageProcessor {

    /** The mobile net requires additional normalization of the used input. */
    private static final float IMAGE_MEAN = 127.5f;

    private static final float IMAGE_STD = 127.5f;

    /**
     * An array to hold inference results, to be feed into Tensorflow Lite as outputs. This isn't part
     * of the super class, because we need a primitive array here.
     */
    private float[][] labelProbArray = null;

    /**
     * Initializes an {@code ImageClassifierFloatMobileNet}.
     *
     * @param activity
     */
    public RescueEyeImageProcessor(Activity activity) throws IOException {
        super(activity);
        labelProbArray = new float[1][getNumLabels()];
    }

    @Override
    protected String getModelPath() {
        //Its placed in assets
        return "face_tf_modeltf_model.tflite";
    }

    @Override
    protected String getLabelPath() {
        return "label.txt";
    }

    @Override
    public int getImageSizeX() {
        return 224;
    }

    @Override
    public int getImageSizeY() {
        return 224;
    }

    @Override
    public int getNumBytesPerChannel() {
        return 4;
    }

    @Override
    public void addPixelValue(int pixelValue) {
    }

    @Override
    public float getProbability(int labelIndex) {
        return labelProbArray[0][labelIndex];
    }

    @Override
    public void setProbability(int labelIndex, Number value) {
        labelProbArray[0][labelIndex] = value.floatValue();
    }

    @Override
    public float getNormalizedProbability(int labelIndex) {
        return labelProbArray[0][labelIndex];
    }

    @Override
    public void runInference() {
        tflite.run(imgData, labelProbArray);
    }
}
