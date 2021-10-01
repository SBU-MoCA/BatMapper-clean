package com.ecemoca.zhoub.batmapper.tracking;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * BatMapper: Indoor Map Construction using Acoustics
 * Created by zhoub on 5/27/2017.
 */

public class DoorDetection {
    public Queue<Float> q;
    private int qsize;
    static final float CIRCLE_RADIUS = 1;

    public DoorDetection() {
//        this.q = q;
//        qsize = windowSize;
    }

    public ArrayList<Double> generateCircles(ArrayList<Double> wallCam, ArrayList<Double> wallMic, ArrayList<Double> circles) {
        double prevSlope=0;
        double nextSlope;
        double deltaX1=0;
        double deltaX2;
        double deltaY1;
        double deltaY2;
        int firstTime = 0;
        List<ArrayList<Double>> allLists = new ArrayList<>();
        allLists.add(wallCam);
        allLists.add(wallMic);
        for(ArrayList<Double> list:allLists) {
            for (int i = 3; i < (list.size() / 2)-10; i++) {
                if (firstTime == 0) {
                    deltaX1 = list.get((2 * i) - 2) - list.get((2 * i) - 4);
                    deltaY1 = list.get((2 * i) - 3) - list.get((2 * i) - 5);
                    prevSlope = deltaY1 / deltaX1;
                    firstTime++;
                }
                deltaX2 = list.get(2 * i) - list.get((2 * i)-2);
                deltaY2 = list.get((2 * i) - 1) - list.get((2 * i) - 3);
                nextSlope = deltaY2 / deltaX2;
                if (Math.abs(prevSlope - nextSlope) > .1) {
                    for(int m =0; m <= 2*Math.PI; m+=(Math.PI/16)) {
                        circles.add(list.get(2*i) + Math.cos(m));
                        circles.add(list.get(2*i) + Math.sin(m));
                    }
                }
                prevSlope = nextSlope;
            }
        }
        return circles;
    }

    public boolean getDoorState() {
        boolean door = false;
        if (q != null && q.size() == qsize) {
            Float[] d = new Float[qsize];
            q.toArray(d);
            float dPre = 0f, dNew = 0f;
            for (int i = 0; i < qsize / 2; i++) {
                dPre = dPre + d[i];
                dNew = dNew + d[i + qsize / 2];
            }
            dPre = dPre / (qsize/2);
            dNew = dNew / (qsize/2);

            if (dNew - dPre > 0.2 && dNew - dPre < 0.3) {
                door = true;
            }
            else if (dPre - dNew > 0.2) {
                door = false;
            }
        }
        return door;
    }

}
