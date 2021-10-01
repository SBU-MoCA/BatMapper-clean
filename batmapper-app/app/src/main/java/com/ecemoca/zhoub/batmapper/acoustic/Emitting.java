package com.ecemoca.zhoub.batmapper.acoustic;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Environment;
import android.util.Log;

import com.ecemoca.zhoub.batmapper.BatMapper;
import com.ecemoca.zhoub.batmapper.R;

import java.io.IOException;

import static java.security.AccessController.getContext;

/**
 * BatMapper: Indoor Map Construction using Acoustics
 * Created by zhoub on 3/1/2017.
 */

public class Emitting extends Thread {
    private MediaPlayer mPlayer = null;
    private static String mFileName = null;
    private Context context = null;

    public Emitting(Context context, String path) {
        mFileName = path;
        this.context = context;
    }

    public void run() {
        Log.d("Emitting", "DIRECTORY: " + Environment.getExternalStorageDirectory());
        playRecording();
    }

    // Sound Play methods
    private void playRecording() {
        ditchMediaPlayer();
        try {
            // TODO: get rid of source path argument passed to constructor
            mPlayer = MediaPlayer.create(context, R.raw.chirp);
            //mPlayer.setDataSource(mFileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*try {
            mPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } */
        mPlayer.start();
    }

    public void ditchMediaPlayer() {
        if (mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
        }
    }

    public void stopPlayback() {
        if (mPlayer != null)
            mPlayer.stop();
    }

    public void pausePlayback() {
        if (mPlayer != null) {
            mPlayer.pause();
        }
    }
}
