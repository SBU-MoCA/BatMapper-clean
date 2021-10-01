package com.ecemoca.zhoub.batmapper.disGeneration;

import java.util.LinkedList;
import java.util.Queue;

/**
 * BatMapper: Indoor Map Construction using Acoustics
 * Created by zhoub on 5/28/2017.
 *
 * Input: ranked distance measurements
 * Output: single distance poll()
 */

public class OutlierRemoval {
    private float[] ds;
    private Queue<Float> qTop;     // queue of distances
    private int windowLength = 10;
    private float dMean = 0f;
    private float output = 0f;
    private boolean found = false;

    public OutlierRemoval() {
        qTop = new LinkedList<>();
    }

    public float getFinalDis(float[][] disAmp) {
        ds = new float[disAmp.length];
        for (int i = 0; i < disAmp.length; i++) {
            ds[i] = disAmp[i][0];
        }

        if (qTop == null) {
            qTop.offer(ds[0]);
            dMean = ds[0];
        }
        else if (qTop.size() < windowLength) {
            dMean = (dMean * qTop.size() + ds[0]) / (qTop.size() + 1);
            qTop.offer(ds[0]);
        }
        else {
            output = qTop.poll();
            for (float d : ds) {
                if (Math.abs(d - dMean) < 0.3) {
                    qTop.offer(d);
                    dMean = dMean + (d - output) / windowLength;
                    found = true;
                    return output;
                }
            }
            if (!found) {
                qTop.offer(dMean);
                dMean = dMean + (dMean - output) / windowLength;
            }
        }
        return output;
    }

}
