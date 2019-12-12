package com.cygrp.rescueeye.fragment;

import android.app.Activity;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cygrp.rescueeye.R;
import com.cygrp.rescueeye.custom_views.CameraOverlay;
import com.cygrp.rescueeye.custom_views.CameraPreview;
import com.cygrp.rescueeye.imageprocessing.DLibLandmarkDetector;
import com.cygrp.rescueeye.imageprocessing.ImageProcessor;
import com.cygrp.rescueeye.imageprocessing.RescueEyeImageProcessor;
import com.cygrp.rescueeye.utility.Constants;
import com.cygrp.rescueeye.utility.ErrorDialog;
import com.cygrp.rescueeye.utility.RectUtilsKt;
import com.cygrp.rescueeye.utility.SoundUtility;
import com.cygrp.rescueeye.utility.Utility;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

public class FrontCameraFragment extends Fragment implements ActivityCompat.OnRequestPermissionsResultCallback, Camera.PreviewCallback, Camera.FaceDetectionListener {

    private static final String TAG = FrontCameraFragment.class.getName();
    private boolean mPermissionsGranted;
    private static final int PERMISSIONS_REQUEST_CODE = 1;

    private static final String HANDLE_FRAME_THREAD_NAME = "FrameProcessingBackground";
    private final Object lock = new Object();
    private boolean mRunFrameProcessing = false;
    private boolean mIsValidFaceNotDetected = false;
    private boolean mIsFirstTimeFaceDetected = false;
    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mFrameProcessingBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBeepSoundPeriodicHandler;
    private Handler mFrameProcessingBackgroundHandler;

    private ImageProcessor mImageProcesser;
    private byte[] mFrame = null;
    private CameraPreview mCameraPreview;
    private CameraOverlay mCameraOverlay;
    private TextView mInfoView;
    private DLibLandmarkDetector mDLibLandmarkDetector;
    private SoundUtility mSoundUtility;
    private long mLastFaceSeen;

    public FrontCameraFragment() {
        // Required empty public constructor
    }

    public static FrontCameraFragment newInstance() {
        FrontCameraFragment fragment = new FrontCameraFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSoundUtility = new SoundUtility();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.camera_fragment_layout, container, false);
    }

    /**
     * Load the model and labels.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    /**
     * Connect the buttons to their event handler.
     */
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        // Get references to widgets.
        mCameraPreview = view.findViewById(R.id.cameraPreview);
        mCameraOverlay = view.findViewById(R.id.cameraOverlay);
        mInfoView = view.findViewById(R.id.text_info);
        mCameraPreview.setFaceListener(this);
        mCameraPreview.setPreviewCallback(this);
        mCameraOverlay.preview = mCameraPreview;
    }

    @Override
    public void onStart() {
        super.onStart();
        mPermissionsGranted = Utility.allPermissionsGranted(getActivity());
        if (!mPermissionsGranted) {
            requestPermissions(Utility.getRequiredPermissions(getActivity()), PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startCameraAndThreads();
    }

    @Override
    public void onPause() {
        if (mPermissionsGranted) {
            stopBackgroundThread();
            mCameraPreview.stopPreview();
            mSoundUtility.stopBeep();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void startCameraAndThreads() {
        if (mPermissionsGranted) {
            startBackgroundThread();
            mCameraPreview.startPreview();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (!Utility.allPermissionsGranted(getActivity())) {
            ErrorDialog.newInstance(getString(R.string.request_permission))
                    .show(getActivity().getSupportFragmentManager(), TAG);
        } else {
            mPermissionsGranted = true;
            startCameraAndThreads();
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void startBackgroundThread() {
        //Thread handling background processing for Frame Processing
        mFrameProcessingBackgroundThread = new HandlerThread(HANDLE_FRAME_THREAD_NAME);
        mFrameProcessingBackgroundThread.start();
        mFrameProcessingBackgroundHandler = new Handler(mFrameProcessingBackgroundThread.getLooper());

        mBeepSoundPeriodicHandler = new Handler();
        mBeepSoundPeriodicHandler.post(periodicBeepSoundPlayStopListener);
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mFrameProcessingBackgroundThread.quitSafely();
        try {
            mFrameProcessingBackgroundThread.join();
            mFrameProcessingBackgroundThread = null;
            mFrameProcessingBackgroundHandler = null;

            mBeepSoundPeriodicHandler.removeMessages(0);
            mBeepSoundPeriodicHandler = null;
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted when stopping background thread", e);
        }
    }

    private Runnable periodicBeepSoundPlayStopListener =
            new Runnable() {
                @Override
                public void run() {
                    if (mIsValidFaceNotDetected ||
                            (mIsFirstTimeFaceDetected && (System.currentTimeMillis() - mLastFaceSeen) > Constants.FACE_SEEN_THRESHOLD)) {
                        mSoundUtility.playBeepSound();
                        mCameraOverlay.setFaceAndLandmarks(null, null);
                        mCameraOverlay.invalidate();
                    } else {
                        mSoundUtility.stopBeep();
                    }
                    mBeepSoundPeriodicHandler.postDelayed(periodicBeepSoundPlayStopListener, Constants.BEEP_SOUND_LISTENER_INTERVAL);
                }
            };

    @Override
    public void onFaceDetection(final Camera.Face[] faces, Camera camera) {
        if (mFrameProcessingBackgroundHandler != null) {
            synchronized (lock) {
                if (mRunFrameProcessing) {
                    return;
                }
            }

            mFrameProcessingBackgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        mRunFrameProcessing = true;
                    }

                    final byte[] frameData = mFrame;
                    if (frameData == null || faces.length == 0) {
                        resetFrameProcessing();
                        return;
                    }
                    try {
                        // Disable Image processor while updating
                        if (mImageProcesser == null || mDLibLandmarkDetector == null) {
                            mImageProcesser = new RescueEyeImageProcessor(FrontCameraFragment.this.getActivity());
                            mImageProcesser.setNumThreads(2);
                            mDLibLandmarkDetector = new DLibLandmarkDetector(getContext());
                            mInfoView.setText(R.string.initializing_text);
                            boolean modelLoaded = mDLibLandmarkDetector.loadDlibModel();
                            mInfoView.setText("");
                            if (modelLoaded) {
                                resetFrameProcessing();
                                return;
                            }
                        }

                        Camera.Face bestFace = null;
                        for (int i = 0; i < faces.length; i++) {
                            if (faces[i].score > 91) {
                                bestFace = faces[i];
                                break;
                            }
                        }
                        if (bestFace != null) {
                            mLastFaceSeen = System.currentTimeMillis();
                            mIsFirstTimeFaceDetected = true;
                            final Rect face = bestFace.rect;
                            int w = mCameraPreview.getPreviewWidth();
                            int h = mCameraPreview.getPreviewHeight();
                            int rotation = mCameraPreview.getDisplayRotation();
                            Rect rect = RectUtilsKt.mapTo(new Rect(face), w, h, rotation);
                            //Opencv will crash if face is outside preview width
                            if (rect.left > 0 && rect.right < h && rect.top > 0 && rect.bottom < w) {
                                final long[] landmarks = mDLibLandmarkDetector.detectLandMarks(frameData, w, h, rotation, rect);
                                if (landmarks != null && landmarks.length > 0) {
                                    getActivity().runOnUiThread(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    //show landmarks on overlay window
                                                    mCameraOverlay.setFaceAndLandmarks(face, landmarks);
                                                    mCameraOverlay.invalidate();
                                                }
                                            });

                                    ImageProcessor.ImageProcessorResponse response = mImageProcesser.classifyFrame(landmarks);
                                    showResultOnUI(response.getBuilder());
                                    mIsValidFaceNotDetected = response.isDistracted();
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        mImageProcesser = null;
                    } finally {
                        resetFrameProcessing();
                    }
                }
            });
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        mFrame = data;
    }

    private void resetFrameProcessing() {
        synchronized (lock) {
            mRunFrameProcessing = false;
        }
    }

    private void showResultOnUI(final SpannableStringBuilder builder) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            mInfoView.setText(builder, TextView.BufferType.SPANNABLE);
                        }
                    });
        }
    }
}

