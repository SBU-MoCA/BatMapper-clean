package com.ecemoca.zhoub.batmapper;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
//import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.Plot;
import com.androidplot.util.PixelUtils;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PanZoom;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.ecemoca.zhoub.batmapper.acoustic.Emitting;
import com.ecemoca.zhoub.batmapper.acoustic.Recording;
import com.ecemoca.zhoub.batmapper.settings.GuideLines;
import com.ecemoca.zhoub.batmapper.settings.SettingsActivity;
import com.ecemoca.zhoub.batmapper.tracking.DoorDetection;
import com.ecemoca.zhoub.batmapper.tracking.InertialTracking;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;

//import org.achartengine.ChartFactory;
//import org.achartengine.GraphicalView;
//import org.achartengine.chart.PointStyle;
//import org.achartengine.model.XYMultipleSeriesDataset;
//import org.achartengine.model.XYSeries;
//import org.achartengine.renderer.XYMultipleSeriesRenderer;
//import org.achartengine.renderer.XYSeriesRenderer;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
//import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
//CameraBridgeViewBase.CvCameraViewListener2
public class BatMapper extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener{
    private DecimalFormat format = new DecimalFormat("0.00");
    public final static String SHARED_PREFS_NAME="mapScannerSettings";
    //private String sourcePath = "android.resource://" + getPackageName() + "/" + R.raw.chirp;
    private String sourcePath = String.valueOf(R.raw.chirp);
    private static String TAG = "PermissionDemo";
    private static final String TAG1 = "MainActivity";
    private static final int RECORD_REQUEST_CODE = 101;
    private static final int CAMERA_REQUEST_CODE = 102;
    // Setting parameters
    private float mapRange;        // length/width for map graph
    private float stepLength;      // step length for walking
    private float dCam = 0f, dCamPre = 0f;     // distance pop out from queue
    private float dMic = 0f, dMicPre = 0f;

    // Door Detection Buffers and variables
    private ArrayList<Double> dCamDoorBuffer = new ArrayList<>();
    private ArrayList<Double> dMicDoorBuffer = new ArrayList<>();
    private ArrayList<Float> dCamBuffer = new ArrayList<>();
    private ArrayList<Float> dMicBuffer = new ArrayList<>();
    private boolean inDoorCam = false;
    private boolean inDoorMic = false;
    private int camCount = 0;
    private int micCount = 0;
    private static final double DOOR_DEPTH = .05;
    private static final int DOOR_COUNT_MAX = 20;
    private static final int DOOR_COUNT_MIN = 4;


    private boolean doorL = false, doorR = false;
    private Queue<Float> doorLQ = new LinkedList<>(), doorRQ = new LinkedList<>();
    private int sensorScanRate;       // Inertial sensor scan rate
    private int pingInterval;      // Interval between pings
    private int carrierFreqency=8000;   // Carrier frequency
    private int bandFreqency=2000;      // Frequency band for modulation
    private int emitNumber;      // Continuous emitting
    // Variables
    private boolean sensorRunFlag = false;
    public SharedPreferences mPrefs;
    private ArrayList<Double> trace = new ArrayList<>();
    private ArrayList<Double> wallCam = new ArrayList<>();
    private ArrayList<Double> wallMic= new ArrayList<>();
    private ArrayList<Double> doors = new ArrayList<>();
    private Emitting emitting = null;
    private Recording rc;
    private int doorWindow = 50;
    //private DoorDetection ddl = new DoorDetection(doorLQ, doorWindow), ddr = new DoorDetection(doorRQ, doorWindow);
    private SimpleXYSeries series1, series2, series3, doorSeries;
    private Redrawer redrawer;
    private DoorDetection doorDetection = new DoorDetection();

    Queue<Float> queueOrientation = new LinkedList<>();

    private XYPlot plot;
    private LineAndPointFormatter series1Format, series2Format, doorSeriesFormat;
//    Mat mRgba = null, mCanny = null, mLines = null;

    //private JavaCameraView javaCameraView;
    BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            System.out.println("Called OnManagerConnected()");
            //javaCameraView.enableView();
            switch (status) {
                case BaseLoaderCallback.SUCCESS: {
                    break;
                }
                default:{
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

//    static {
//        if(!OpenCVLoader.initDebug())
//            Log.i(TAG1, "OpenCV not loaded!");
//        else
//            Log.i(TAG1, "OpenCV loaded!");
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayShowHomeEnabled(true); //<< this
        setContentView(R.layout.activity_tracking);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mPrefs = BatMapper.this.getSharedPreferences(SHARED_PREFS_NAME,MODE_PRIVATE);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(mPrefs,null);

//        int permission = PermissionChecker.checkPermission(this, Manifest.permission.RECORD_AUDIO);
//        PermissionChecker.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);


//        settingsMenu = (DrawerLayout) findViewById(R.id.settings_menu);
//        settingsList = (ListView) findViewById(R.id.settings_list);
//
//        // Set the adapter for the list view
//        settingsList.setAdapter(new ArrayAdapter<String>(this,
//                R.layout.drawer_list_item, mPlanetTitles));
//        // Set the list's click listener
//        settingsList.setOnItemClickListener(new DrawerItemClickListener());

        int permission1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);

//        int permission2 = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        if (permission1 != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission to record denied");
            makeRequest("Audio");
        }
//        if(permission2 != PackageManager.PERMISSION_GRANTED) {
//            Log.i(TAG, "Permission to record denied");
//            makeRequest("Camera");
//        }

        // Acoustic measurement
        buttonListener();
        setPlot();

        // Vision
//        javaCameraView = (JavaCameraView) findViewById(R.id.java_cam);
//        javaCameraView.setVisibility(SurfaceView.VISIBLE);
//        javaCameraView.setMaxFrameSize(450,450);
//        javaCameraView.setCvCameraViewListener(this);

        redrawer = new Redrawer(Arrays.asList(new Plot[]{plot}), 20, false);
    }

    protected void makeRequest(String permission) {
        if(permission.equalsIgnoreCase("Audio")) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    RECORD_REQUEST_CODE);
        }
        if(permission.equalsIgnoreCase("Camera")) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RECORD_REQUEST_CODE: {

                if (grantResults.length == 0
                        || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {

                    Log.i(TAG, "Permission has been denied by user");
                } else {
                    Log.i(TAG, "Permission has been granted by user");
                }
                return;
            }
        }
    }

    // Option menu
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    // React to menu click
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.settings:
                Intent intent = new Intent(BatMapper.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.guidelines:
                startActivity(new Intent(BatMapper.this, GuideLines.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        mapRange = Float.valueOf(mPrefs.getString("mapSize","40"));
        stepLength = Float.valueOf(mPrefs.getString("stepLength","1"));
        sensorScanRate = Integer.valueOf(mPrefs.getString("sensorRate","20"));
        pingInterval = Integer.valueOf(mPrefs.getString("pingIntvl","50"));
        carrierFreqency = Integer.valueOf(mPrefs.getString("carriFreq","8000"));
        bandFreqency = Integer.valueOf(mPrefs.getString("bandFreq", "2000"));
        emitNumber = Integer.valueOf(mPrefs.getString("emitNumber","200"));
    }

//    @Override
//    public void onCameraViewStarted(int width, int height) {
//        mRgba = new Mat(height, width, CvType.CV_8UC4);
//        mCanny = new Mat(height, width, CvType.CV_8UC1);
//        mLines = new Mat(height, width, CvType.CV_8UC1);
//    }

//    @Override
//    public void onCameraViewStopped() {
//        mRgba.release();
//    }

//    @Override
//    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
//        mRgba = inputFrame.rgba();
//        if(mPrefs.getBoolean("computer_vision", false)) {
//            Imgproc.Canny(mRgba, mCanny, 50, 150);
//            Imgproc.HoughLinesP(mCanny, mLines, 1, Math.PI / 180, 100, 150, 20);
//            for (int x = 0; x < mLines.rows(); x++) {
//                double[] vec = mLines.get(x, 0);
//                double x1 = vec[0],
//                        y1 = vec[1],
//                        x2 = vec[2],
//                        y2 = vec[3];
//                //calculate angle in radian,  if you need it in degrees just do angle * 180 / PI
//                float angle = (float) (Math.atan2(y1 - y2, x1 - x2) * 180 / Math.PI);
//                //Log.i("Angle", "angle: " + angle);
//                if ((angle > -160 && angle < -130) || (angle > 130 && angle < 160)) {
//                    Point start = new Point(x1, y1);
//                    Point end = new Point(x2, y2);
//
//                    Imgproc.line(mRgba, start, end, new Scalar(255, 0, 0), 3);
//
//                }
//            }
//        }
//        return mRgba;
//    }

    // Sensor collect runnable
    public class sensorRun implements Runnable {
        private Thread t;
        private String threadName;
        private Context mContext;
        private SensorManager mSensorManager = null;
        private SensorEventListener mListener;
        private List<Sensor> currentDevice = new ArrayList<>();
        private float[] orientationVals = new float[3];
        private int steps;
        private int oneStep;

        public sensorRun(Context context,String name) {
            this.mContext = context;
            this.threadName = name;
        }

        public void run() {
            mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
            if (mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null)
                currentDevice.add(mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR));
            if (mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null)
                currentDevice.add(mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER));

            mListener = new SensorEventListener() {
                public void onSensorChanged(SensorEvent event) {
                    if (sensorRunFlag) {
                        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                            // Convert the rotation-vector to a 4x4 matrix.
                            float[] mRotationMatrix = new float[16];
                            SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
                            SensorManager.getOrientation(mRotationMatrix, orientationVals);
                            // Optionally convert the result from radians to degrees
                            orientationVals[0] = (float) Math.toDegrees(orientationVals[0]);
                            orientationVals[0] = smoothOrientation(orientationVals[0]);      // smooth orientation
                            orientationVals[1] = (float) Math.toDegrees(orientationVals[1]);
                            orientationVals[2] = (float) Math.toDegrees(orientationVals[2]);
                            TextView textX = (TextView) findViewById(R.id.textYaw);
                            textX.setText("Yaw: " + format.format(orientationVals[0]));
                            dCam = rc.getDisAmpL();
                            TextView text = (TextView) findViewById(R.id.textTop);
                            text.setText("L: " + format.format(dCam));
                            // door detection
                            if (doorLQ.size() < doorWindow)
                                doorLQ.offer(dCam);
                            else {
                                doorLQ.poll();
                                doorLQ.offer(dCam);
                            }

                            dMic = rc.getDisAmpR();
                            TextView text1 = (TextView) findViewById(R.id.textBot);
                            text1.setText("R: " + format.format(dMic));
                            // door detection
                            if (doorRQ.size() < doorWindow)
                                doorRQ.offer(dMic);
                            else {
                                doorRQ.poll();
                                doorRQ.offer(dMic);
                            }

                            // Show real time graph
                            if (trace.size() == 0) {
                                trace.add(0.0);
                                trace.add(0.0);
                                wallCam.add(0.0);
                                wallCam.add(0.0);
                                wallMic.add(0.0);
                                wallMic.add(0.0);
                                dCamBuffer.add(dCam);
                                dMicBuffer.add(dMic);
                                dCamBuffer.add(dCam);
                                dMicBuffer.add(dMic);
                                dCamBuffer.add(dCam);
                                dMicBuffer.add(dMic);
                            } else {
                                oneStep--;
                                if (oneStep > 0) {
                                    double x = trace.get(trace.size() - 2) + Math.cos(Math.toRadians(orientationVals[0])) / sensorScanRate * stepLength;
                                    double y = trace.get(trace.size() - 1) + Math.sin(Math.toRadians(orientationVals[0])) / sensorScanRate * stepLength;
                                    double xCam = 0f; double yCam = 0f; double xMic = 0f; double yMic = 0f;
                                    int delay = 5; // was 49
                                    if(trace.size()>delay){
                                        if(dCam!=0){
                                            xCam = trace.get(trace.size() - delay-1) - dCam*Math.sin(Math.toRadians(-orientationVals[0])+3.14);
                                            yCam = trace.get(trace.size() - delay) - dCam*Math.cos(Math.toRadians(-orientationVals[0])+3.14);
                                        }
                                        if(dMic!=0){
                                            xMic = trace.get(trace.size() - delay-1) + dMic*Math.sin(Math.toRadians(-orientationVals[0])+3.14);
                                            yMic = trace.get(trace.size() - delay) + dMic*Math.cos(Math.toRadians(-orientationVals[0])+3.14);
                                        }
                                    }
                                    trace.add(x);
                                    trace.add(y);
                                    wallCam.add(xCam);
                                    wallCam.add(yCam);
                                    wallMic.add(xMic);
                                    wallMic.add(yMic);


                                    // determines whether "in" door
                                    if(dCam - dCamBuffer.get(0) > DOOR_DEPTH && dCam - dCamBuffer.get(1) > DOOR_DEPTH && dCam - dCamBuffer.get(2) > DOOR_DEPTH) {
                                        inDoorCam = true;
                                    }
                                    if(dMic - dMicBuffer.get(0) > DOOR_DEPTH && dMic - dMicBuffer.get(1) > DOOR_DEPTH && dMic - dMicBuffer.get(2) > DOOR_DEPTH) {
                                        inDoorMic = true;
                                    }

                                    // adds points of potential door to buffer
                                    if(inDoorCam) {
                                        dCamDoorBuffer.add(xCam);
                                        dCamDoorBuffer.add(yCam);
                                        camCount++;
                                    }
                                    if(inDoorMic) {
                                        dMicDoorBuffer.add(xMic);
                                        dMicDoorBuffer.add(yMic);
                                        micCount++;
                                    }

                                    // determines whether "out" of door and makes sure door is not too large
                                    if(dCamBuffer.get(0) - dCam > DOOR_DEPTH && camCount < DOOR_COUNT_MAX && camCount > DOOR_COUNT_MIN && dCamBuffer.get(1) - dCam > DOOR_DEPTH) {
                                        inDoorCam = false;
                                        for(Double d: dCamDoorBuffer) {
                                            doors.add(d);
                                        }
                                        camCount = 0;
                                    }
                                    if(dMicBuffer.get(0) - dMic > DOOR_DEPTH && micCount < DOOR_COUNT_MAX && micCount > DOOR_COUNT_MIN && dMicBuffer.get(1) - dMic > DOOR_DEPTH) {
                                        inDoorMic = false;
                                        for(Double d: dMicDoorBuffer) {
                                            doors.add(d);
                                        }
                                        micCount = 0;
                                    }

                                    // clears door buffer if door is too large
                                    if(camCount > DOOR_COUNT_MAX) {
                                        dCamDoorBuffer.clear();
                                        camCount = 0;
                                        inDoorCam = false;
                                    }

                                    if(micCount > DOOR_COUNT_MAX) {
                                        dMicDoorBuffer.clear();
                                        micCount = 0;
                                        inDoorMic = false;
                                    }

                                    // add points to distance buffers and control length
                                    dCamBuffer.add(dCam);
                                    dMicBuffer.add(dMic);
                                    if(dCamBuffer.size() > 4) {
                                        dCamBuffer.remove(0);
                                        dMicBuffer.remove(0);
                                    }
                                    updatePlot();
                                }
                            }
                        }

                        if (event.sensor == mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)) {
                            steps++;
                            oneStep = sensorScanRate;
                            TextView textX = (TextView) findViewById(R.id.textStep);
                            textX.setText("Step: " + steps);
                        }
                    }
                }
                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            };

            if(sensorRunFlag){
                for (Sensor insert : currentDevice) {
                    mSensorManager.registerListener(mListener, insert, sensorScanRate, 100);
                }
            }

        }

        public void start() {
            if (t == null) {
                t = new Thread(this, threadName);
                t.start();
            }
        }

        public void stop(){
            for (Sensor insert : currentDevice) {
                mSensorManager.unregisterListener(mListener, insert);
                currentDevice.clear();
            }
        }
    }

    // Button listener
    private void buttonListener() {
        final Button contiButton = (Button) findViewById(R.id.rightDoor);
        final Button inertialButton = (Button) findViewById(R.id.buttonInertial);
        final Button saveButton = (Button) findViewById(R.id.buttonSave);
        final Button loopButton = (Button) findViewById(R.id.buttonLoop);

        // Continuous recording sound
        assert contiButton != null;
        contiButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
            }
        });

        inertialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                sensorRun sensor = new sensorRun(BatMapper.this,"sensorThread");
                if(!sensorRunFlag){
                    inertialButton.setText("STOP");
                    sensor.start();
                    emitting = new Emitting(BatMapper.this, sourcePath);
                    emitting.start();     // start sound emitting
                    rc = new Recording();
                    rc.start();
                    sensorRunFlag = true;
                }
                else{
                    inertialButton.setText("START");
                    emitting.pausePlayback();
                    rc.stopRecording();
                    sensorRunFlag = false;
                    sensor.stop();
                }

            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                new Thread(new Runnable() {
                    public void run() {
                        showToast("Saving Plot");
                        Bitmap plotBitmap;
                        plot.setDrawingCacheEnabled(true);
                        int width = plot.getWidth();
                        int height = plot.getHeight();
                        plot.measure(width, height);
                        plotBitmap = Bitmap.createBitmap(plot.getDrawingCache());
                        plot.setDrawingCacheEnabled(false);
                        Document doc = new Document();
                        try {
                            PackageManager m = getPackageManager();
                            String dir = getPackageName();
                            try {
                                PackageInfo p = m.getPackageInfo(dir, 0);
                                dir = p.applicationInfo.dataDir;
                            } catch (PackageManager.NameNotFoundException e) {
                                Log.w("SavePlots", "Error Package name not found ", e);
                            }
                            DateFormat dateFormat = new SimpleDateFormat("MM_dd_HH_mm_ss");
                            //get current date time with Date()
                            Date date = new Date();
                            File docsFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + File.separator); // getApplicationInfo().dataDir
                            Log.d("SavePlots","DOCS: " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + File.separator); // Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                            if (!docsFolder.exists()) {
                                docsFolder.mkdir();
                                Log.i("SavePlot", "Batmapper Directory created");
                            }
                            File pdfFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + File.separator + "plots");
                            Log.d("SavePlots","PDF: " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + File.separator + "plots");
                            if (!pdfFolder.exists()) {
                                pdfFolder.mkdir();
                                Log.i("SavePlot", "Plots Directory created");
                            }
                            File plotFile = new File(pdfFolder + File.separator + mPrefs.getString("saveName", "plot")
                                    + "-" + dateFormat.format(date) + ".pdf");
                            OutputStream output = new FileOutputStream(plotFile);
                            PdfWriter.getInstance(doc, output);
                            doc.open();
                            doc.newPage();
                            // Make sure doc isn't perceived as empty
                            doc.add(new Chunk(""));
                            width = plotBitmap.getWidth();
                            height = plotBitmap.getHeight();
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            plotBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                            Image plotImg = Image.getInstance(stream.toByteArray());
                            plotImg.setAlignment(Image.MIDDLE);
                            Log.d("DIM", "WIDTH: " + width);
                            Log.d("DIM", "HEIGHT: " + height);
                            // Scale plot to fit on pdf page
                            plotImg.scaleToFit(560, 800);
                            doc.add(plotImg);
                            showToast("Plot saved successfully");
                        } catch (DocumentException | IOException e) {
                            showToast("Could not save");
                            e.printStackTrace();
                        } finally {
                            doc.close();
                        }
                    }
                }).start();
            }
        });

        loopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                redrawer.pause();
                InertialTracking tracking = new InertialTracking(trace,wallCam,wallMic);
                trace.clear();
                trace = tracking.loopClosure();
                redrawer.start();
                updatePlot();
            }
        });

    }

    // Pause
    @Override
    protected void onPause() {
        redrawer.pause();
        if(emitting != null) {
            emitting.stopPlayback();
        }
        Button button = (Button) findViewById(R.id.buttonInertial);
        button.setText("START");
        if(rc != null) {
            rc.stopRecording();
        }
        sensorRunFlag = false;
        super.onPause();
//        if (javaCameraView != null) {
//            javaCameraView.disableView();
//        }
    }

    // Destroy
    @Override
    protected void onDestroy(){
        if(redrawer != null) {
            redrawer.finish();
        }
        if(emitting != null) {
            emitting.ditchMediaPlayer();
        }
        super.onDestroy();
//        if (javaCameraView != null) {
//            javaCameraView.disableView();
//        }
        sensorRunFlag = false;
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        this.finishAffinity();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if (!OpenCVLoader.initDebug()) {
//            Log.i(TAG1, "OpenCV not loaded!");
//            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
//        } else {
//            Log.i(TAG1, "OpenCV loaded!");
//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallBack);
//            }
        redrawer.start();
    }

    private void setPlot() {
        plot = (XYPlot) findViewById(R.id.plot);

        // create formatters to use for drawing a series using LineAndPointRenderer
        // and configure them from xml:
        series1Format = new LineAndPointFormatter(Color.RED, null, null, null);
        doorSeriesFormat = new LineAndPointFormatter(null, Color.YELLOW, null, null);
        doorSeriesFormat.getLinePaint().setStrokeWidth(2);

        series2Format = new LineAndPointFormatter(null, Color.BLACK, null, null);
        series2Format.getLinePaint().setStrokeWidth(2);
        // dotted line
        series1Format.getLinePaint().setPathEffect(new DashPathEffect(new float[] {
                PixelUtils.dpToPix(5), PixelUtils.dpToPix(5)}, 0));


        series1 = new SimpleXYSeries(trace, SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED, "Series1");
        series2 = new SimpleXYSeries(wallCam, SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED, "Series2");
        series3 = new SimpleXYSeries(wallMic, SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED, "Series3");
        doorSeries = new SimpleXYSeries(doors, SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED, "Doors");
        plot.addSeries(series1, series1Format);
        plot.addSeries(series2, series2Format);
        plot.addSeries(series3, series2Format);
        plot.addSeries(doorSeries, doorSeriesFormat);
        plot.addSeries(doorSeries, doorSeriesFormat);
        plot.setDomainBoundaries(-30, BoundaryMode.FIXED, 40, BoundaryMode.FIXED);
        plot.setRangeBoundaries(-30, BoundaryMode.FIXED, 40, BoundaryMode.FIXED);
        PanZoom.attach(plot, PanZoom.Pan.BOTH, PanZoom.Zoom.SCALE);

        // THIS STUFF BREAKS IT
        // just for fun, add some smoothing to the lines:
        // see: http://androidplot.com/smooth-curves-and-androidplot/
//        series1Format.setInterpolationParams(
//                new CatmullRomInterpolator.Params(15, CatmullRomInterpolator.Type.Centripetal));
//
//        series2Format.setInterpolationParams(
//                new CatmullRomInterpolator.Params(15, CatmullRomInterpolator.Type.Centripetal));
//        PanZoom.attach(plot);

    }

    private void updatePlot() {
        // turn the above arrays into XYSeries':
        // (Y_VALS_ONLY means use the element index as the x value)
        // add a new series' to the xyplot:
        series1.setModel(trace, SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED);
        series2.setModel(wallCam, SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED);
        series3.setModel(wallMic, SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED);
        doorSeries.setModel(doors, SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED);
    }

    float mean = 0, sum = 0, oldMean = 0, drift = 0, delay = 0, diffInputMean = 0, turnDrift = 0;
    List<Float> rawReading = new LinkedList<>();
    private float smoothOrientation(float input) {
        int numberOfPoints = 10;
        float ORIENTATION_TOLERANCE = 5;
        int BUFFER_LENGTH = 10;

        // Reset the initial orientation to zero
        if(queueOrientation.size() == 0) {
            drift = input;
            input = 0;
        }
        else {
            input = input - drift;
        }

        // If queue is less than 10 points, return input directly
        if (queueOrientation.size() < numberOfPoints) {
            queueOrientation.offer(input);
            rawReading.add(input);
            sum = sum + input;
            mean = 0;
            return input;
        }
        else {
            float sumBuffer = 0;
            for(int i = 1; i <= BUFFER_LENGTH; i++) {
                sumBuffer += rawReading.get(rawReading.size() - i);
            }
            float averageBuffer = sumBuffer/BUFFER_LENGTH;
            float sumVariance = 0;
            for(int i = 1; i <= BUFFER_LENGTH; i++) {
                sumVariance += Math.pow((rawReading.get(rawReading.size() - i) - averageBuffer), 2);
            }
            float averageVariance = sumVariance/BUFFER_LENGTH;
            if (averageVariance < ORIENTATION_TOLERANCE) {   // No turn detected
                queueOrientation.poll();
                queueOrientation.offer(mean);
                rawReading.add(input);
            }
            else {
                delay = 120;
            }
            if (delay >= 0) {                                                         // turn detected
                delay --;
                rawReading.add(input);
                diffInputMean = input-mean;
                // which way turn
                int direction = (diffInputMean > 0) ? 1 : -1;
                diffInputMean = Math.abs(diffInputMean);
                // see whether made a right angle turn
                if((90 - ORIENTATION_TOLERANCE) < diffInputMean && diffInputMean < (90 + ORIENTATION_TOLERANCE)) {
                    mean += direction * 90;
                    // add drift of sensor from right angle turn
                    turnDrift += mean - input;
                }
                return input;
            } else {
                // mean equal to input plus correction
                mean = input + turnDrift;
            }
        }
        return mean;
    }

    public void showToast(final String toast)
    {
        runOnUiThread(new Runnable() {
            public void run()
            {
                Toast.makeText(BatMapper.this, toast, Toast.LENGTH_SHORT).show();
            }
        });
    }


}

