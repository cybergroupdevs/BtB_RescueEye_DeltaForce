package com.cygrp.rescueeye.utility;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.util.Log;

public class SoundUtility {

    private ToneGenerator mToneGenerator = null;
    private Handler mHandler = null;

    public SoundUtility() {

    }

    public void playBeepSound() {
        if (mHandler != null) {
            return;
        }
        if (mToneGenerator == null){
            mToneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        }
        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mToneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100);
                mHandler.postDelayed(this,250);
            }
        },100);
    }

    public void stopBeep(){
        if (mHandler != null) {
            mHandler.removeMessages(0);
            mHandler = null;
        }
    }
}
