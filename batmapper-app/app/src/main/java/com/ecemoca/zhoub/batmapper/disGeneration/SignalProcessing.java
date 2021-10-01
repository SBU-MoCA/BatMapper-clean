package com.ecemoca.zhoub.batmapper.disGeneration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhoub on 11/19/2016.
 * Input: queue of sound recording
 * Output: distance measurement candidates into another queue for distance-object association
 */
public class SignalProcessing {
    private int overLap = 40 * 48000 / 1000;   // 30ms overlap
    private int sRate = 48000;
    private int begin;
    private int nCandidates = 8;
    private float[] chirp = {0, -0.0026132f, -0.0042297f, 0.034795f, -0.059418f, 0.02043366f, 0.0929047f, -0.195618f, 0.1606969f, 0.0527688f, -0.3189f, 0.412045f, -0.1913417f, -0.248626f, 0.60615f, -0.584082f, 0.130236f, 0.487933f, -0.84944f, 0.675767f, -0.040697f, -0.663296f, 0.9829f, -0.705999f, -3.919488e-15f, 0.70215938f, -0.98290f, 0.696682f, -0.040697f, -0.589379f, 0.849443f, -0.63949f, 0.130236f, 0.369866f, -0.60615f, 0.50764897f, -0.19134f, -0.13862f, 0.3189f, -0.304114f, 0.1606969f, -0.000533f, -0.0929047f, 0.10128f, -0.059418f, 0.01542f, 0.00422975f, -0.00339f, 0};
    private float[] recFloat;
    private float[][] distanceFinal;
    private float minRange = 0.3f;
    private float maxRange = 5f;
    private static float[] coeffA = {1f, -5.3191965f, 12.7014f, -17.702244f, 15.70216f, -9.05636137f, 3.31085209f, -0.70045f, 0.0655805f};
    private static float[] coeffB = {0.2560869f, -2.048695f, 7.1704337f, -14.3408674f, 17.926084266f, -14.3408674f, 7.1704337f, -2.048695f, 0.2560869f};

    public SignalProcessing() {     // short[] mRecording should be queue
    }


    public float[][] Process(Short[] recording) {
        // Step 1: Bandpass filter
        recFloat = bandPassFilter(recording, coeffA, coeffB);
        // Step 2: Cross-correlation
        recFloat = crossCorrelate(recFloat, chirp);
        // Step 3: Smoothing
        smooth(recFloat, 5);
        // Step 4: Find beginning points
        begin = findBeginning(recFloat, overLap);
        // Step 5: For each begin location, get distance with maximum power intensity
        distanceFinal = distanceGeneration(begin, recFloat, minRange, maxRange, sRate);
//      Log.d("distanceFinal", distanceFinal.length + "");
        return distanceFinal;
    }

    private float[] bandPassFilter(Short[] x, float[] a, float[] b) {
        float[] y = new float[x.length];
        y[0] = b[0] * x[0];
        for (int i = 1; i < a.length; i++) {
            y[i] = b[0] * x[i];
            for (int j = 1; j <= i; j++) {
                y[i] += b[j] * x[i - j] - a[j] * y[i - j];
            }
        }
        for (int i = a.length; i < x.length - 1; i++) {
            y[i] = b[0] * x[i];
            for (int j = 1; j < a.length; j++) {
                y[i] += b[j] * x[i - j] - a[j] * y[i - j];
            }
        }
        y[y.length - 1] = 0;
        return y;
    }

    private float[] crossCorrelate(float[] f, float[] g) {
        float[] res = new float[f.length];
        //res has to be of the size fSize-gSize+1 > 0, returns the max of the cross-correlation
        for (int T = 0; T < f.length - g.length; T++) {
            res[T] = 0;
            for (int t = 0; t < g.length; t++) {
                res[T] += f[t + T] * g[t];
            }
        }
        return res;
    }

    private void smooth(float[] buffer, int amount) {
        float[] temp = buffer.clone();
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = 0;
            for (int j = -2 * amount + i; j <= 2 * amount + i; j++)
                if (j >= 0 && j < buffer.length)
                    buffer[i] += Math.abs(temp[j]) * Math.exp(-Math.pow((j - i) / amount, 2) / 2.f);
        }
    }

    private int findBeginning(float[] rec, int limit) {
        float max = 0.f;
        int idx = 0;
        for (int T = 0; T < limit; T++) {
            if (Math.abs(rec[T]) > max) {
                max = Math.abs(rec[T]);
                idx = T;
            }
        }
        return idx;
    }

    private float[][] distanceGeneration(int begin, float[] rec, float min, float max, int rate) {
        int minPoints = (int) (2 * min / 346 * rate);
        int maxPoints = (int) (2 * max / 346 * rate);
        List<Float> dis = new ArrayList<>();
        List<Float> amp = new ArrayList<>();
        float[][] result = new float[nCandidates][2];

        boolean localMax;
        for (int i = begin + minPoints; i < begin + maxPoints; i++) {
            localMax = true;
            for (int j = -2; j < 2; j++) {
                if (rec[i + j] > rec[i])
                    localMax = false;
            }
            if (localMax) {
                dis.add(173.0f * (i - begin) / ((float) rate));
                amp.add(rec[i]);
            }
        }
        for (int i = 0; i < nCandidates; i++) {
            int maxIdx = findMaxIndex(amp);
            result[i][0] = dis.get(maxIdx);
            result[i][1] = amp.get(maxIdx);
            amp.set(maxIdx, Float.MIN_VALUE);
        }

        return result;
    }

    public float getFinalDistance() {
        if (distanceFinal != null)
            return distanceFinal[0][0];
        else
            return 0f;
    }

    private int findMaxIndex(List<Float> list) {
        int idx = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) > list.get(idx))
                idx = i;
        }
        return idx;
    }
}