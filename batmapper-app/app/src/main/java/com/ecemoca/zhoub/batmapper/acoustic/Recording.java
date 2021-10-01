package com.ecemoca.zhoub.batmapper.acoustic;

/**
 * BatMapper: Indoor Map Construction using Acoustics
 * Created by zhoub on 3/1/2017.
 */

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.ecemoca.zhoub.batmapper.disGeneration.OutlierRemoval;
import com.ecemoca.zhoub.batmapper.disGeneration.SignalProcessing;


public class Recording extends Thread {

    private final static String TAG="RecordingThread";
    private AudioRecord audioRecord;
    private int minBufferSize = 0;
    private boolean record = true;
    private final int SAMPLE_RATE = 48000;
    private final short CHANNEL_NUMBER = 2;
    private final short BITS_PER_SAMPLE = 16;
    private Short[] recordingL;
    private Short[] recordingR;
    private SignalProcessing signalProL = new SignalProcessing();
    private SignalProcessing signalProR = new SignalProcessing();
    public float[][] disAmpL, disAmpR;
    private float disLPre = 0f, disRPre = 0f, distR = 0f,distL = 0f;
    private int resetCntL = 0, resetCntR = 0, nToReset = 10;
    private OutlierRemoval removalR = new OutlierRemoval(), removalL = new OutlierRemoval();


    public Recording() {
//        minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
//                CHANNEL_NUMBER == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
//                BITS_PER_SAMPLE == 16 ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT);
        minBufferSize = 2 * 70 * SAMPLE_RATE / 1000;
        recordingL = new Short[minBufferSize / 2];
        recordingR = new Short[minBufferSize / 2];
    }

    private void initAudioRecord() {
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                CHANNEL_NUMBER == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                BITS_PER_SAMPLE == 16 ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT,
                minBufferSize
        );
    }


    public void run() {
        initAudioRecord();
        Log.d("Recording", "Started Recording Audio");
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            record = true;
            audioRecord.startRecording();
            try {
                short[] buffer = new short[minBufferSize];
                while (audioRecord.read(buffer, 0, minBufferSize) > 0 && record) {
                    for (int i = 0; i < minBufferSize / 2; i++) {
                        recordingR[i] = buffer[2*i];
                        recordingL[i] = buffer[2*i+1];
                    }
                    if (recordingL != null && recordingR != null) {
                        disAmpL = signalProL.Process(recordingL);
                        for (int i = 0; i < disAmpL.length; i++) {
                            if (disAmpL[i][0] < 0.95f && disAmpL[i][0] > 0.7f && Math.abs(disAmpL[i][0] - getDisAmpR()) > 0.2f) {
                                disLPre = disAmpL[0][0];
                                disAmpL[0][0] = disAmpL[i][0];
                                disAmpL[i][0] = disLPre;
                                distL = disAmpL[0][0];
                                break;
                            }
                        }
                        //distL = removalL.getFinalDis(disAmpL);

                        disAmpR = signalProR.Process(recordingR);
                        for (int i = 0; i < disAmpR.length; i++) {
                            if (disAmpR[i][0] > 1.3f && disAmpR[i][0] < 1.5f &&Math.abs(disAmpR[i][0] - getDisAmpL()) > 0.2f) {
                                disRPre = disAmpR[0][0];
                                disAmpR[0][0] = disAmpR[i][0];
                                disAmpR[i][0] = disRPre;
                                distR = disAmpR[0][0];
                                break;
                            }
                        }
                        //distR = removalR.getFinalDis(disAmpR);
                    }
                }
            } finally {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
            Log.i(TAG, "Stopped Recording");
        } else {
            Log.i(TAG, "Could not initialize AudioRecord");

        }
    }


    public void stopRecording() {
        record = false;
    }

    public float getDisAmpL() {
        return distL;
    }

    public float getDisAmpR() {
        return distR;
    }
}
