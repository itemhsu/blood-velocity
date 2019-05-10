
package com.xxmassdeveloper.mpchartexample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.LimitLine.LimitLabelPosition;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
//import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.formatter.ValueFormatter;

import com.xxmassdeveloper.mpchartexample.custom.MyMarkerView;
import com.xxmassdeveloper.mpchartexample.notimportant.DemoBase;
import com.xxmassdeveloper.mpchartexample.realm.FFT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.R.attr.angle;
import static android.R.attr.tag;
import static android.R.id.message;
import static java.lang.Math.cos;
import static java.lang.Math.abs;
import static java.lang.Math.floor;

/**
 * Example of a heavily customized {@link LineChart} with limit lines, custom line shapes, etc.
 *
 * @since 1.7.4
 * @version 3.1.0
 */


public class LineChartActivity1 extends DemoBase implements OnSeekBarChangeListener,
        OnChartGestureListener, OnChartValueSelectedListener {

    private LineChart mChart;
    private LineChart mChart2;
    private LineChart mChartStatistic;
    private LineChart mChartVel;
    private SeekBar mSeekBarX, mSeekBarY, mSeekBarZ ;
    private TextView tvX, tvY, tvZ, tvB;

    // Requesting permission to RECORD_AUDIO
    private AudioRecord recorder;
    private boolean recorderStarted;
    private Thread recordingThread;
    private int bufferSize = 1024;
    private short[][] buffers = new short[256][bufferSize];
    private int[] averages = new int[256];
    private int lastBuffer = 0;
    private float[] maxX = new float[1024];
    private float[] maxX2 = new float[1024];
    private double[] reOutIIR = new double[1024];
    private float[] bloods= new float[125];
    private float[] bloodsIIR= new float[125];
    int bloodCount=0;
    private float[] maxXstatistic = new float[50];
    private ArrayList<String> xLabel = new ArrayList<>();

    public double[] fftCalculator(double[] re) {
        double[] im= new double[re.length];
        for (int ii=0; ii<re.length ; ii++) im[ii]=0;
        FFT fft = new FFT(re.length);
        fft.fft(re, im);
        double[] fftMag = new double[re.length];
        double tmp;
        for (int i = 0; i < re.length; i++) {
            tmp = Math.pow(re[i], 2) + Math.pow(im[i], 2);
            fftMag[i]=Math.sqrt(tmp);
        }
        return fftMag;
    }

    protected void startListenToMicrophone() {
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);
        int MY_PERMISSIONS_REQUEST_READ_CONTACTS=0;

        if (permission
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this ,
                    Manifest.permission.RECORD_AUDIO)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this ,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS
                );

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        if (!recorderStarted) {

            recordingThread = new Thread() {
                @Override
                public void run() {
                    int minBufferSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);
                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, minBufferSize*2);
                    recorder.setPositionNotificationPeriod(bufferSize);
                    recorder.setRecordPositionUpdateListener(new AudioRecord.OnRecordPositionUpdateListener() {
                        @Override
                        public void onPeriodicNotification(AudioRecord recorder) {
                            short[] buffer = buffers[++lastBuffer % buffers.length];
                            float[] bufferFloat = new  float[1024];
                            recorder.read(buffer, 0, bufferSize);
                            long sum = 0;
                            for (int i = 0; i < bufferSize; ++i) {
                                sum += Math.abs(buffer[i]);
                                if (i<1024) bufferFloat[i]=(float)buffer[i];
                            }
                            averages[lastBuffer % buffers.length] = (int) (sum / bufferSize);
                            lastBuffer = lastBuffer % buffers.length;
                            Log.d("Wave =",  String.format("value = %d", sum/ bufferSize));
                            //setData2(45, 100);
                            float fx=(mSeekBarX.getProgress()*10f+1500f)*1000f;
                            float angle=mSeekBarY.getProgress()*0.1f+45f;
                            float Sv=mSeekBarZ.getProgress()+1520f;

                            double[] re= new double[1024];
                            for (int kk=0; kk<1024; kk++){
                                re[kk]=maxX2[kk];
                                re[kk]=(double)buffer[kk];
                            }

                            double[] reOut=fftCalculator(re);
                            for(int jj=0; jj<1024; jj++){
                                reOutIIR[jj]=reOut[jj]*0.1+reOutIIR[jj]*0.9;
                            }
                            float bloodVel=getBloodVel(reOut,angle,fx,Sv);
                            float bloodVelIIR=getBloodVel(reOutIIR,angle,fx,Sv);
                            bloods[bloodCount]=bloodVel;
                            bloodsIIR[bloodCount]=bloodVelIIR;
                            bloodCount++;
                            bloodCount%=125;
                            for (int kk=bloodCount; (kk<125) & (kk<(bloodCount+10)); kk++){
                                bloods[kk]=0f;
                                bloodsIIR[kk]=0f;
                            }
                            tvB.setText(String.format("%.2f",bloodVelIIR));
                            setFFTData(reOut, reOutIIR);
                            setTimeData(bufferFloat);
                            setDataStatistic(buffer);
                            setBloodData(bloods,bloodsIIR);

                            mChart2.postInvalidate();
                            mChart.postInvalidate();
                            mChartStatistic.postInvalidate();
                            mChartVel.postInvalidate();

                        }

                        @Override
                        public void onMarkerReached(AudioRecord recorder) {
                        }
                    });
                    recorder.startRecording();
                    short[] buffer = buffers[lastBuffer % buffers.length];
                    recorder.read(buffer, 0, bufferSize);
                    while (true) {
                        if (isInterrupted()) {
                            recorder.stop();
                            recorder.release();
                            break;
                        }
                    }
                }
            };
            recordingThread.start();

            recorderStarted = true;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_linechart);

        tvX = (TextView) findViewById(R.id.tvXMax);
        tvY = (TextView) findViewById(R.id.tvYMax);
        tvZ = (TextView) findViewById(R.id.tvZMax);
        tvB = (TextView) findViewById(R.id.tvBMax);

        mSeekBarX = (SeekBar) findViewById(R.id.seekBar1);
        mSeekBarY = (SeekBar) findViewById(R.id.seekBar2);
        mSeekBarZ = (SeekBar) findViewById(R.id.seekBar3);

        mSeekBarX.setProgress(50);
        mSeekBarY.setProgress(50);
        mSeekBarZ.setProgress(50);

        mSeekBarY.setOnSeekBarChangeListener(this);
        mSeekBarX.setOnSeekBarChangeListener(this);
        mSeekBarZ.setOnSeekBarChangeListener(this);

        mChart = (LineChart) findViewById(R.id.chart1);
        mChart.setOnChartGestureListener(this);
        mChart.setOnChartValueSelectedListener(this);
        mChart.setDrawGridBackground(false);

        // no description text
        mChart.getDescription().setEnabled(false);

        // enable touch gestures
        mChart.setTouchEnabled(true);

        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        // mChart.setScaleXEnabled(true);
        // mChart.setScaleYEnabled(true);

        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true);

        // set an alternative background color
        // mChart.setBackgroundColor(Color.GRAY);

        // create a custom MarkerView (extend MarkerView) and specify the layout
        // to use for it
        MyMarkerView mv = new MyMarkerView(this, R.layout.custom_marker_view);
        mv.setChartView(mChart); // For bounds control
        mChart.setMarker(mv); // Set the marker to the chart

        // x-axis limit line
        LimitLine llXAxis = new LimitLine(10f, "Index 10");
        llXAxis.setLineWidth(4f);
        llXAxis.enableDashedLine(10f, 10f, 0f);
        llXAxis.setLabelPosition(LimitLabelPosition.RIGHT_BOTTOM);
        llXAxis.setTextSize(10f);

        XAxis xAxis = mChart.getXAxis();
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        //xAxis.setValueFormatter(new MyCustomXAxisValueFormatter());
        //xAxis.addLimitLine(llXAxis); // add x-axis limit line


        Typeface tf = Typeface.createFromAsset(getAssets(), "OpenSans-Regular.ttf");

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines
        leftAxis.setAxisMaximum(0.5f);
        leftAxis.setAxisMinimum(-0.5f);
        //leftAxis.setYOffset(20f);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(false);

        // limit lines are drawn behind data (and not on top)
        leftAxis.setDrawLimitLinesBehindData(true);

        mChart.getAxisRight().setEnabled(false);

        //mChart.getViewPortHandler().setMaximumScaleY(2f);
        //mChart.getViewPortHandler().setMaximumScaleX(2f);

        // add data
        setTimeData(maxX2);

//        mChart.setVisibleXRange(20);
//        mChart.setVisibleYRange(20f, AxisDependency.LEFT);
//        mChart.centerViewTo(20, 50, AxisDependency.LEFT);

       // mChart.animateX(2500);
        //mChart.invalidate();

        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        // modify the legend ...
        l.setForm(LegendForm.LINE);

        /////////////////////////
        mChart2 = (LineChart) findViewById(R.id.chart2);
        mChart2.setOnChartGestureListener(this);
        mChart2.setOnChartValueSelectedListener(this);
        mChart2.setDrawGridBackground(false);

        // no description text
        mChart2.getDescription().setEnabled(false);

        // enable touch gestures
        mChart2.setTouchEnabled(true);

        // enable scaling and dragging
        mChart2.setDragEnabled(true);
        mChart2.setScaleEnabled(true);
        // mChart.setScaleXEnabled(true);
        // mChart.setScaleYEnabled(true);

        // if disabled, scaling can be done on x- and y-axis separately
        mChart2.setPinchZoom(true);

        // set an alternative background color
        // mChart.setBackgroundColor(Color.GRAY);

        // create a custom MarkerView (extend MarkerView) and specify the layout
        // to use for it
        //MyMarkerView mv = new MyMarkerView(this, R.layout.custom_marker_view);
        //mv.setChartView(mChart); // For bounds control
        //mChart.setMarker(mv); // Set the marker to the chart
        initMaxX();
        double[] iniFFT=new double[1024];
        setFFTData(iniFFT, iniFFT);


        /////////////////////////
        mChartStatistic = (LineChart) findViewById(R.id.chart3);
        mChartStatistic.setOnChartGestureListener(this);
        mChartStatistic.setOnChartValueSelectedListener(this);
        mChartStatistic.setDrawGridBackground(false);
        mChartStatistic.getDescription().setEnabled(false);
        mChartStatistic.setTouchEnabled(false);
        mChartStatistic.setDragEnabled(false);
        mChartStatistic.setScaleEnabled(false);
        mChartStatistic.setPinchZoom(false);

        //////////////////////////
        mChartVel = (LineChart) findViewById(R.id.chart4);
        mChartVel.setOnChartGestureListener(this);
        mChartVel.setOnChartValueSelectedListener(this);
        mChartVel.setDrawGridBackground(false);
        mChartVel.getDescription().setEnabled(false);
        mChartVel.setTouchEnabled(false);
        mChartVel.setDragEnabled(false);
        mChartVel.setScaleEnabled(false);
        mChartVel.setPinchZoom(false);


        // // dont forget to refresh the drawing
        // mChart.invalidate();
        startListenToMicrophone();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.line, menu);
        return true;
    }
    @Override
    protected void saveToGallery() {
        saveToGallery(mChart, "LineChartActivity1");
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.actionToggleValues: {
                List<ILineDataSet> sets = mChart.getData()
                        .getDataSets();

                for (ILineDataSet iSet : sets) {

                    LineDataSet set = (LineDataSet) iSet;
                    set.setDrawValues(!set.isDrawValuesEnabled());
                }

                mChart.invalidate();
                break;
            }
            case R.id.actionToggleIcons: {
                List<ILineDataSet> sets = mChart.getData()
                        .getDataSets();

                for (ILineDataSet iSet : sets) {

                    LineDataSet set = (LineDataSet) iSet;
                    set.setDrawIcons(!set.isDrawIconsEnabled());
                }

                mChart.invalidate();
                break;
            }
            case R.id.actionToggleHighlight: {
                if(mChart.getData() != null) {
                    mChart.getData().setHighlightEnabled(!mChart.getData().isHighlightEnabled());
                    mChart.invalidate();
                }
                break;
            }
            case R.id.actionToggleFilled: {

                List<ILineDataSet> sets = mChart.getData()
                        .getDataSets();

                for (ILineDataSet iSet : sets) {

                    LineDataSet set = (LineDataSet) iSet;
                    if (set.isDrawFilledEnabled())
                        set.setDrawFilled(false);
                    else
                        set.setDrawFilled(true);
                }
                mChart.invalidate();
                break;
            }
            case R.id.actionToggleCircles: {
                List<ILineDataSet> sets = mChart.getData()
                        .getDataSets();

                for (ILineDataSet iSet : sets) {

                    LineDataSet set = (LineDataSet) iSet;
                    if (set.isDrawCirclesEnabled())
                        set.setDrawCircles(false);
                    else
                        set.setDrawCircles(true);
                }
                mChart.invalidate();
                break;
            }
            case R.id.actionToggleCubic: {
                List<ILineDataSet> sets = mChart.getData()
                        .getDataSets();

                for (ILineDataSet iSet : sets) {

                    LineDataSet set = (LineDataSet) iSet;
                    set.setMode(set.getMode() == LineDataSet.Mode.CUBIC_BEZIER
                            ? LineDataSet.Mode.LINEAR
                            :  LineDataSet.Mode.CUBIC_BEZIER);
                }
                mChart.invalidate();
                break;
            }
            case R.id.actionToggleStepped: {
                List<ILineDataSet> sets = mChart.getData()
                        .getDataSets();

                for (ILineDataSet iSet : sets) {

                    LineDataSet set = (LineDataSet) iSet;
                    set.setMode(set.getMode() == LineDataSet.Mode.STEPPED
                            ? LineDataSet.Mode.LINEAR
                            :  LineDataSet.Mode.STEPPED);
                }
                mChart.invalidate();
                break;
            }
            case R.id.actionToggleHorizontalCubic: {
                List<ILineDataSet> sets = mChart.getData()
                        .getDataSets();

                for (ILineDataSet iSet : sets) {

                    LineDataSet set = (LineDataSet) iSet;
                    set.setMode(set.getMode() == LineDataSet.Mode.HORIZONTAL_BEZIER
                            ? LineDataSet.Mode.LINEAR
                            :  LineDataSet.Mode.HORIZONTAL_BEZIER);
                }
                mChart.invalidate();
                break;
            }
            case R.id.actionTogglePinch: {
                if (mChart.isPinchZoomEnabled())
                    mChart.setPinchZoom(false);
                else
                    mChart.setPinchZoom(true);

                mChart.invalidate();
                break;
            }
            case R.id.actionToggleAutoScaleMinMax: {
                mChart.setAutoScaleMinMaxEnabled(!mChart.isAutoScaleMinMaxEnabled());
                mChart.notifyDataSetChanged();
                break;
            }

            case R.id.actionSave: {
                if (mChart.saveToPath("title" + System.currentTimeMillis(), "")) {
                    Toast.makeText(getApplicationContext(), "Saving SUCCESSFUL!",
                            Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(getApplicationContext(), "Saving FAILED!", Toast.LENGTH_SHORT)
                            .show();

                // mChart.saveToGallery("title"+System.currentTimeMillis())
                break;
            }
        }
        return true;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        tvX.setText("" + (mSeekBarX.getProgress()*10+1500));
        tvY.setText("" + (mSeekBarY.getProgress()*0.1+45));
        tvZ.setText("" + (mSeekBarZ.getProgress()+1520));

        //setData(mSeekBarX.getProgress() + 1, mSeekBarY.getProgress());

        // redraw
        mChart.invalidate();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub

    }


    private void setDataStatistic(short[]  wavIn) {
        int count=1024;
        ArrayList<Entry> values = new ArrayList<Entry>();
        for(int j=0; j<50; j++){
            maxXstatistic[j]=0;
        }

        for (int i = 0; i < count; i++) {
            //int ind=(int)(abs(maxX2[i])*100f);
            int ind=(int)(abs(wavIn[i])/100);
            if (ind<50) {
                maxXstatistic[ind]++;
            }

        }

        for (int i = 0; i < 50; i++) {
            float val = maxXstatistic[i];
            values.add(new Entry(i, val, getResources().getDrawable(R.drawable.star)));
        }

        LineDataSet set1;


        if (mChartStatistic.getData() != null &&
                mChartStatistic.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet)mChartStatistic.getData().getDataSetByIndex(0);
            set1.setValues(values);
            mChartStatistic.getData().notifyDataChanged();
            mChartStatistic.notifyDataSetChanged();
        } else {
            // create a dataset and give it a type
            set1 = new LineDataSet(values, "Energy Dist.");

            set1.setDrawIcons(false);

            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.DKGRAY);
            set1.setCircleColor(Color.DKGRAY);
            set1.setLineWidth(1f);
            set1.setCircleRadius(1f);
            set1.setDrawCircleHole(false);
            set1.setValueTextSize(9f);
            set1.setDrawFilled(false);
            set1.setFormLineWidth(1f);
            set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            set1.setFormSize(15.f);
            set1.setDrawValues(false);

            if (Utils.getSDKInt() >= 18) {
                // fill drawable only supported on api level 18 and above
                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_red);
                set1.setFillDrawable(drawable);
            } else {
                set1.setFillColor(Color.BLACK);
            }

            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1); // add the datasets

            // create a data object with the datasets
            LineData data = new LineData(dataSets);

            // set data
            mChartStatistic.setData(data);

            XAxis xAxis = mChartStatistic.getXAxis();
            //final ArrayList<String> xLabel = new ArrayList<>();

            xAxis.setValueFormatter(new ValueFormatter() {

                @Override
                public String getFormattedValue(float value) {
                    return (Integer.toString((int)value/10)+"k");
                }
            });

 /*           xAxis.setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    //return xLabel.get((int)value);
                    return (Integer.toString((int)value/10)+"k");
                }
            });
*/
            YAxis yAxis = mChartStatistic.getAxisLeft();
            yAxis.setAxisMaximum(100f);
            YAxis yAxisR = mChartStatistic.getAxisRight();
            yAxisR.setAxisMaximum(100f);
            yAxisR.setEnabled(false);
        }


    }

    private void setTimeData(float[] timeData) {


        ArrayList<Entry> values = new ArrayList<Entry>();
        int count=1024;

        for (int i = 0; i < count; i++) {

            //float val = maxX2[i];
            float val = timeData[i];
            values.add(new Entry(i, val, getResources().getDrawable(R.drawable.star)));
        }

        LineDataSet set1;


        if (mChart.getData() != null &&
                mChart.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet)mChart.getData().getDataSetByIndex(0);
            set1.setValues(values);
            mChart.getData().notifyDataChanged();
            mChart.notifyDataSetChanged();
        } else {
            // create a dataset and give it a type
            set1 = new LineDataSet(values, "Sound wave");

            set1.setDrawIcons(false);

            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLUE);
            set1.setCircleColor(Color.BLUE);
            set1.setLineWidth(1f);
            set1.setCircleRadius(1f);
            set1.setDrawCircleHole(false);
            set1.setValueTextSize(9f);
            set1.setDrawFilled(false);
            set1.setFormLineWidth(1f);
            set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            set1.setFormSize(15.f);

            if (Utils.getSDKInt() >= 18) {
                // fill drawable only supported on api level 18 and above
                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_red);
                set1.setFillDrawable(drawable);
            }
            else {
                set1.setFillColor(Color.BLACK);
            }

            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1); // add the datasets

            // create a data object with the datasets
            LineData data = new LineData(dataSets);

            // set data
            mChart.setData(data);

            YAxis yAxis = mChart.getAxisLeft();
            yAxis.setAxisMaximum(3000f);
            yAxis.setAxisMinimum(-3000f);
        }
    }

    private void setBloodData(float[] bloods, float[] bloodsIIR) {

        ArrayList<Entry> values = new ArrayList<Entry>();
        ArrayList<Entry> valuesIIR = new ArrayList<Entry>();
        int count=125;

        for (int i = 0; i < count; i++) {
            float val = (float)bloods[i];
            values.add(new Entry(i, val, getResources().getDrawable(R.drawable.star)));
            float valIIR = (float)bloodsIIR[i];
            valuesIIR.add(new Entry(i, valIIR, getResources().getDrawable(R.drawable.star)));
        }

        LineDataSet set1;
        LineDataSet setIIR;

        /*if (mChart2.getData() != null &&
                mChart2.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet)mChart.getData().getDataSetByIndex(0);
            set1.setValues(values);
            mChart2.getData().notifyDataChanged();
            mChart2.notifyDataSetChanged();
        } else {*/
        if(true){
            // create a dataset and give it a type
            set1 = new LineDataSet(values, "Vel");

            set1.setDrawIcons(false);

            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLUE);
            set1.setCircleColor(Color.BLUE);
            set1.setLineWidth(1f);
            set1.setCircleRadius(1f);
            set1.setDrawCircleHole(false);
            set1.setValueTextSize(9f);
            set1.setDrawFilled(false);
            set1.setFormLineWidth(1f);
            set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            set1.setFormSize(15.f);


            if (Utils.getSDKInt() >= 18) {
                // fill drawable only supported on api level 18 and above
                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_red);
                set1.setFillDrawable(drawable);
            }
            else {
                set1.setFillColor(Color.BLACK);
            }

            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1); // add the datasets

            // create a data object with the datasets
            //////////////////LineData data = new LineData(dataSets);

            // set data
            //////////////////mChart2.setData(data);
            XAxis xAxis = mChartVel.getXAxis();
            //final ArrayList<String> xLabel = new ArrayList<>();

            xAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return (Integer.toString(Math.round(value/8000*1024))+"s");
                }
            });
/*
            xAxis.setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    //return xLabel.get((int)value);
                    return (Integer.toString(Math.round(value/8000*1024))+"s");
                }
            });
*/
//mChart2.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabel));

            //////////////////////////////////////////////
            setIIR = new LineDataSet(valuesIIR, "Vel IIR");

            setIIR.setDrawIcons(false);

            // set the line to be drawn like this "- - - - - -"
            setIIR.enableDashedLine(10f, 5f, 0f);
            setIIR.enableDashedHighlightLine(10f, 5f, 0f);
            setIIR.setColor(Color.RED);
            setIIR.setCircleColor(Color.RED);
            setIIR.setLineWidth(1f);
            setIIR.setCircleRadius(1f);
            setIIR.setDrawCircleHole(false);
            setIIR.setValueTextSize(9f);
            setIIR.setDrawFilled(false);
            setIIR.setFormLineWidth(1f);
            setIIR.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            setIIR.setFormSize(15.f);


            if (Utils.getSDKInt() >= 18) {
                // fill drawable only supported on api level 18 and above
                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_red);
                setIIR.setFillDrawable(drawable);
            }
            else {
                setIIR.setFillColor(Color.BLACK);
            }

            //ArrayList<ILineDataSet> dataSetsIIR = new ArrayList<ILineDataSet>();
            dataSets.add(setIIR); // add the datasets

            // create a data object with the datasets
            LineData dataIIR = new LineData(dataSets);

            //////////////////LineData data = new LineData(dataSets);

            // set data
            //////////////////mChart2.setData(data);

            // set data
            mChartVel.setData(dataIIR);
            YAxis yAxisR = mChartVel.getAxisRight();
            yAxisR.setEnabled(false);

        }
    }


    private void setFFTData(double[] re, double[] reIIR) {

        ArrayList<Entry> values = new ArrayList<Entry>();
        ArrayList<Entry> valuesIIR = new ArrayList<Entry>();
        int count=128;

        for (int i = 0; i < count; i++) {
            float val = (float)re[i];
            values.add(new Entry(i, val, getResources().getDrawable(R.drawable.star)));
            float valIIR = (float)reIIR[i];
            valuesIIR.add(new Entry(i, valIIR, getResources().getDrawable(R.drawable.star)));
        }

        LineDataSet set1;
        LineDataSet setIIR;

        /*if (mChart2.getData() != null &&
                mChart2.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet)mChart.getData().getDataSetByIndex(0);
            set1.setValues(values);
            mChart2.getData().notifyDataChanged();
            mChart2.notifyDataSetChanged();
        } else {*/
        if(true){
            // create a dataset and give it a type
            set1 = new LineDataSet(values, "FFT");

            set1.setDrawIcons(false);

            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLUE);
            set1.setCircleColor(Color.BLUE);
            set1.setLineWidth(1f);
            set1.setCircleRadius(1f);
            set1.setDrawCircleHole(false);
            set1.setValueTextSize(9f);
            set1.setDrawFilled(false);
            set1.setFormLineWidth(1f);
            set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            set1.setFormSize(15.f);

            //final ArrayList<String> xLabel = new ArrayList<>();
            for(int ii=0; ii<9; ii++)
            {
                xLabel.add(""+125*ii);
            }


            if (Utils.getSDKInt() >= 18) {
                // fill drawable only supported on api level 18 and above
                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_red);
                set1.setFillDrawable(drawable);
            }
            else {
                set1.setFillColor(Color.BLACK);
            }

            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1); // add the datasets

            // create a data object with the datasets
            //////////////////LineData data = new LineData(dataSets);

            // set data
            //////////////////mChart2.setData(data);
            XAxis xAxis = mChart2.getXAxis();
            //final ArrayList<String> xLabel = new ArrayList<>();

            xAxis.setValueFormatter(new ValueFormatter() {

                //private final DecimalFormat format = new DecimalFormat("###");

                @Override
                public String getFormattedValue(float value) {
                    return (Integer.toString(Math.round(value/256*2000))+"Hz");
                    //return format.format(value) + "-" + format.format(value + 10);
                }
            });

/*
            xAxis.setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    //return xLabel.get((int)value);
                    return (Integer.toString(Math.round(value/256*2000))+"Hz");
                }
            });
*/
            //mChart2.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabel));

            //////////////////////////////////////////////
            setIIR = new LineDataSet(valuesIIR, "FFT IIR");

            setIIR.setDrawIcons(false);

            // set the line to be drawn like this "- - - - - -"
            setIIR.enableDashedLine(10f, 5f, 0f);
            setIIR.enableDashedHighlightLine(10f, 5f, 0f);
            setIIR.setColor(Color.RED);
            setIIR.setCircleColor(Color.RED);
            setIIR.setLineWidth(1f);
            setIIR.setCircleRadius(1f);
            setIIR.setDrawCircleHole(false);
            setIIR.setValueTextSize(9f);
            setIIR.setDrawFilled(false);
            setIIR.setFormLineWidth(1f);
            setIIR.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            setIIR.setFormSize(15.f);


            if (Utils.getSDKInt() >= 18) {
                // fill drawable only supported on api level 18 and above
                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_red);
                setIIR.setFillDrawable(drawable);
            }
            else {
                setIIR.setFillColor(Color.BLACK);
            }

            //ArrayList<ILineDataSet> dataSetsIIR = new ArrayList<ILineDataSet>();
            dataSets.add(setIIR); // add the datasets

            // create a data object with the datasets
            LineData dataIIR = new LineData(dataSets);

            //////////////////LineData data = new LineData(dataSets);

            // set data
            //////////////////mChart2.setData(data);

            // set data
            mChart2.setData(dataIIR);
            YAxis yAxisR = mChart2.getAxisRight();
            yAxisR.setEnabled(false);

        }
    }


    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        Log.i("Gesture", "START, x: " + me.getX() + ", y: " + me.getY());
    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        Log.i("Gesture", "END, lastGesture: " + lastPerformedGesture);

        // un-highlight values after the gesture is finished and no single-tap
        if(lastPerformedGesture != ChartTouchListener.ChartGesture.SINGLE_TAP)
            mChart.highlightValues(null); // or highlightTouch(null) for callback to onNothingSelected(...)
    }

    @Override
    public void onChartLongPressed(MotionEvent me) {
        Log.i("LongPress", "Chart longpressed.");
    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {
        Log.i("DoubleTap", "Chart double-tapped.");
    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {
        Log.i("SingleTap", "Chart single-tapped.");
    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
        Log.i("Fling", "Chart flinged. VeloX: " + velocityX + ", VeloY: " + velocityY);
    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
        Log.i("Scale / Zoom", "ScaleX: " + scaleX + ", ScaleY: " + scaleY);
    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {
        Log.i("Translate / Move", "dX: " + dX + ", dY: " + dY);
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        Log.i("Entry selected", e.toString());
        Log.i("LOWHIGH", "low: " + mChart.getLowestVisibleX() + ", high: " + mChart.getHighestVisibleX());
        Log.i("MIN MAX", "xmin: " + mChart.getXChartMin() + ", xmax: " + mChart.getXChartMax() + ", ymin: " + mChart.getYChartMin() + ", ymax: " + mChart.getYChartMax());
    }

    @Override
    public void onNothingSelected() {
        Log.i("Nothing selected", "Nothing selected.");
    }

    public float getBloodVel( double[] fftResult,float angle,float fx, float Sv){
        for (int ii=0; ii<1024 ; ii++){
            maxX[ii]=(float)fftResult[ii];
        }
        float maxFreq=0;
        int maxFreaIndex=0;
        for (int ii=30; ii<128 ; ii++){
            if (maxX[ii]>maxFreq ){
                maxFreq=maxX[ii];
                maxFreaIndex=ii;
            }
        }
        //return maxFreaIndex*(float)0.0327*8000/1024;
        //return freq*/((fx*2*cos(angle))/Sv)*100
        float freq=maxFreaIndex*8000/1024;
        float angle_rad=(float)angle*3.1415f/180f;
        //fx=4000000;
        //Sv=1570;

        return freq/((fx*2*(float)cos(angle_rad))/Sv)*100;
    }

    public void initMaxX(){
        maxX[0]=(float)14.235;
        maxX[1]=(float)11.426;
        maxX[2]=(float)3.542;
        maxX[3]=(float)5.6862;
        maxX[4]=(float)4.82;
        maxX[5]=(float)3.9484;
        maxX[6]=(float)3.6591;
        maxX[7]=(float)2.0612;
        maxX[8]=(float)5.7237;
        maxX[9]=(float)5.5666;
        maxX[10]=(float)3.8697;
        maxX[11]=(float)2.3257;
        maxX[12]=(float)2.3917;
        maxX[13]=(float)2.4913;
        maxX[14]=(float)2.5148;
        maxX[15]=(float)2.6851;
        maxX[16]=(float)1.6539;
        maxX[17]=(float)2.0123;
        maxX[18]=(float)2.6383;
        maxX[19]=(float)2.0512;
        maxX[20]=(float)0.54121;
        maxX[21]=(float)2.4207;
        maxX[22]=(float)0.94117;
        maxX[23]=(float)2.0543;
        maxX[24]=(float)2.1835;
        maxX[25]=(float)1.8839;
        maxX[26]=(float)1.974;
        maxX[27]=(float)1.6036;
        maxX[28]=(float)2.887;
        maxX[29]=(float)2.0781;
        maxX[30]=(float)1.1156;
        maxX[31]=(float)2.3712;
        maxX[32]=(float)3.3399;
        maxX[33]=(float)2.514;
        maxX[34]=(float)0.62008;
        maxX[35]=(float)3.4424;
        maxX[36]=(float)3.1079;
        maxX[37]=(float)2.5591;
        maxX[38]=(float)3.6378;
        maxX[39]=(float)7.0355;
        maxX[40]=(float)3.2833;
        maxX[41]=(float)2.7194;
        maxX[42]=(float)3.5735;
        maxX[43]=(float)1.7653;
        maxX[44]=(float)2.4765;
        maxX[45]=(float)7.3353;
        maxX[46]=(float)9.3488;
        maxX[47]=(float)5.4494;
        maxX[48]=(float)8.613;
        maxX[49]=(float)14.848;
        maxX[50]=(float)13.465;
        maxX[51]=(float)22.985;
        maxX[52]=(float)70.405;
        maxX[53]=(float)19.183;
        maxX[54]=(float)12.805;
        maxX[55]=(float)8.1301;
        maxX[56]=(float)9.5022;
        maxX[57]=(float)10.927;
        maxX[58]=(float)4.8174;
        maxX[59]=(float)5.9809;
        maxX[60]=(float)8.4907;
        maxX[61]=(float)11.565;
        maxX[62]=(float)6.5719;
        maxX[63]=(float)6.0445;
        maxX[64]=(float)3.3865;
        maxX[65]=(float)8.9108;
        maxX[66]=(float)4.8951;
        maxX[67]=(float)2.1049;
        maxX[68]=(float)7.2886;
        maxX[69]=(float)4.6782;
        maxX[70]=(float)8.7579;
        maxX[71]=(float)2.8733;
        maxX[72]=(float)4.3184;
        maxX[73]=(float)1.9126;
        maxX[74]=(float)2.043;
        maxX[75]=(float)7.0567;
        maxX[76]=(float)2.3226;
        maxX[77]=(float)4.397;
        maxX[78]=(float)5.7632;
        maxX[79]=(float)3.8007;
        maxX[80]=(float)3.6739;
        maxX[81]=(float)1.0198;
        maxX[82]=(float)1.6108;
        maxX[83]=(float)0.5681;
        maxX[84]=(float)2.5636;
        maxX[85]=(float)2.9309;
        maxX[86]=(float)3.2156;
        maxX[87]=(float)2.6822;
        maxX[88]=(float)2.1269;
        maxX[89]=(float)6.0599;
        maxX[90]=(float)2.837;
        maxX[91]=(float)3.8465;
        maxX[92]=(float)3.9172;
        maxX[93]=(float)1.5902;
        maxX[94]=(float)5.8088;
        maxX[95]=(float)6.0361;
        maxX[96]=(float)3.32;
        maxX[97]=(float)4.2101;
        maxX[98]=(float)6.3864;
        maxX[99]=(float)10.591;
        maxX[100]=(float)8.1891;
        maxX[101]=(float)11.278;
        maxX[102]=(float)11.937;
        maxX[103]=(float)13.646;
        maxX[104]=(float)23.459;
        maxX[105]=(float)17.79;
        maxX[106]=(float)9.5859;
        maxX[107]=(float)9.6939;
        maxX[108]=(float)3.6187;
        maxX[109]=(float)8.4952;
        maxX[110]=(float)3.5446;
        maxX[111]=(float)0.87056;
        maxX[112]=(float)5.0253;
        maxX[113]=(float)9.1295;
        maxX[114]=(float)6.256;
        maxX[115]=(float)3.424;
        maxX[116]=(float)8.3206;
        maxX[117]=(float)4.178;
        maxX[118]=(float)0.92125;
        maxX[119]=(float)1.9133;
        maxX[120]=(float)3.1824;
        maxX[121]=(float)2.8938;
        maxX[122]=(float)2.0004;
        maxX[123]=(float)3.7148;
        maxX[124]=(float)2.2185;
        maxX[125]=(float)3.1537;
        maxX[126]=(float)1.0475;
        maxX[127]=(float)0.174;
        maxX[128]=(float)3.2169;
        maxX[129]=(float)1.6058;
        maxX[130]=(float)1.5236;
        maxX[131]=(float)0.14798;
        maxX[132]=(float)0.93188;
        maxX[133]=(float)2.2015;
        maxX[134]=(float)2.2967;
        maxX[135]=(float)1.0607;
        maxX[136]=(float)0.45625;
        maxX[137]=(float)2.3247;
        maxX[138]=(float)0.86796;
        maxX[139]=(float)0.75331;
        maxX[140]=(float)3.3265;
        maxX[141]=(float)0.6987;
        maxX[142]=(float)1.3372;
        maxX[143]=(float)1.9736;
        maxX[144]=(float)0.75551;
        maxX[145]=(float)1.6258;
        maxX[146]=(float)2.1535;
        maxX[147]=(float)1.1335;
        maxX[148]=(float)1.3149;
        maxX[149]=(float)0.75683;
        maxX[150]=(float)1.7554;
        maxX[151]=(float)0.37741;
        maxX[152]=(float)0.54528;
        maxX[153]=(float)0.16443;
        maxX[154]=(float)2.3487;
        maxX[155]=(float)2.2566;
        maxX[156]=(float)0.60466;
        maxX[157]=(float)1.0667;
        maxX[158]=(float)1.3689;
        maxX[159]=(float)1.4632;
        maxX[160]=(float)1.1394;
        maxX[161]=(float)1.953;
        maxX[162]=(float)1.7593;
        maxX[163]=(float)1.0576;
        maxX[164]=(float)1.5289;
        maxX[165]=(float)2.5047;
        maxX[166]=(float)1.6708;
        maxX[167]=(float)0.8183;
        maxX[168]=(float)0.78731;
        maxX[169]=(float)1.2266;
        maxX[170]=(float)0.71617;
        maxX[171]=(float)0.85414;
        maxX[172]=(float)0.60813;
        maxX[173]=(float)0.13044;
        maxX[174]=(float)1.1063;
        maxX[175]=(float)0.79547;
        maxX[176]=(float)0.90732;
        maxX[177]=(float)1.7149;
        maxX[178]=(float)0.54777;
        maxX[179]=(float)0.84014;
        maxX[180]=(float)0.36734;
        maxX[181]=(float)1.3174;
        maxX[182]=(float)1.5739;
        maxX[183]=(float)0.81716;
        maxX[184]=(float)0.77541;
        maxX[185]=(float)1.7771;
        maxX[186]=(float)0.073734;
        maxX[187]=(float)0.54157;
        maxX[188]=(float)0.70761;
        maxX[189]=(float)1.08;
        maxX[190]=(float)0.1229;
        maxX[191]=(float)0.85753;
        maxX[192]=(float)1.0257;
        maxX[193]=(float)1.2008;
        maxX[194]=(float)0.62246;
        maxX[195]=(float)0.64895;
        maxX[196]=(float)1.1727;
        maxX[197]=(float)0.88943;
        maxX[198]=(float)1.1644;
        maxX[199]=(float)1.2027;
        maxX[200]=(float)0.26573;
        maxX[201]=(float)0.6881;
        maxX[202]=(float)0.24049;
        maxX[203]=(float)0.8978;
        maxX[204]=(float)0.74051;
        maxX[205]=(float)0.93903;
        maxX[206]=(float)0.5025;
        maxX[207]=(float)0.49824;
        maxX[208]=(float)1.1666;
        maxX[209]=(float)0.78963;
        maxX[210]=(float)0.89234;
        maxX[211]=(float)0.94652;
        maxX[212]=(float)0.68089;
        maxX[213]=(float)0.60921;
        maxX[214]=(float)1.3386;
        maxX[215]=(float)0.62972;
        maxX[216]=(float)0.56796;
        maxX[217]=(float)0.16218;
        maxX[218]=(float)1.1966;
        maxX[219]=(float)0.22085;
        maxX[220]=(float)0.79282;
        maxX[221]=(float)0.61789;
        maxX[222]=(float)1.6166;
        maxX[223]=(float)0.19042;
        maxX[224]=(float)1.0838;
        maxX[225]=(float)0.87786;
        maxX[226]=(float)0.77607;
        maxX[227]=(float)0.30533;
        maxX[228]=(float)0.56141;
        maxX[229]=(float)1.0877;
        maxX[230]=(float)0.2416;
        maxX[231]=(float)0.95006;
        maxX[232]=(float)0.54109;
        maxX[233]=(float)0.87972;
        maxX[234]=(float)0.25111;
        maxX[235]=(float)0.80287;
        maxX[236]=(float)0.65299;
        maxX[237]=(float)0.73772;
        maxX[238]=(float)0.67711;
        maxX[239]=(float)0.50247;
        maxX[240]=(float)0.24759;
        maxX[241]=(float)1.3767;
        maxX[242]=(float)0.56126;
        maxX[243]=(float)0.64055;
        maxX[244]=(float)0.67313;
        maxX[245]=(float)0.39029;
        maxX[246]=(float)0.83064;
        maxX[247]=(float)0.32983;
        maxX[248]=(float)0.28156;
        maxX[249]=(float)0.69803;
        maxX[250]=(float)0.19324;
        maxX[251]=(float)0.56078;
        maxX[252]=(float)0.594;
        maxX[253]=(float)0.85651;
        maxX[254]=(float)0.47662;
        maxX[255]=(float)0.71029;
        maxX[256]=(float)0.30483;
        maxX[257]=(float)0.46392;
        maxX[258]=(float)0.3261;
        maxX[259]=(float)0.43624;
        maxX[260]=(float)0.30855;
        maxX[261]=(float)0.33488;
        maxX[262]=(float)0.55009;
        maxX[263]=(float)0.80751;
        maxX[264]=(float)0.74863;
        maxX[265]=(float)0.76557;
        maxX[266]=(float)0.38649;
        maxX[267]=(float)0.37623;
        maxX[268]=(float)0.89724;
        maxX[269]=(float)0.61108;
        maxX[270]=(float)0.37249;
        maxX[271]=(float)0.77802;
        maxX[272]=(float)0.37928;
        maxX[273]=(float)0.27839;
        maxX[274]=(float)0.62766;
        maxX[275]=(float)0.64635;
        maxX[276]=(float)0.87274;
        maxX[277]=(float)0.5586;
        maxX[278]=(float)0.63627;
        maxX[279]=(float)0.85703;
        maxX[280]=(float)0.38088;
        maxX[281]=(float)1.1259;
        maxX[282]=(float)0.237;
        maxX[283]=(float)0.18365;
        maxX[284]=(float)0.63568;
        maxX[285]=(float)0.3315;
        maxX[286]=(float)0.29547;
        maxX[287]=(float)0.29127;
        maxX[288]=(float)0.66844;
        maxX[289]=(float)0.51544;
        maxX[290]=(float)0.36043;
        maxX[291]=(float)0.33752;
        maxX[292]=(float)0.16506;
        maxX[293]=(float)0.34081;
        maxX[294]=(float)0.59224;
        maxX[295]=(float)0.38703;
        maxX[296]=(float)0.49743;
        maxX[297]=(float)0.4104;
        maxX[298]=(float)0.56377;
        maxX[299]=(float)0.77982;
        maxX[300]=(float)1.0939;
        maxX[301]=(float)0.24762;
        maxX[302]=(float)0.38918;
        maxX[303]=(float)0.39654;
        maxX[304]=(float)0.57405;
        maxX[305]=(float)0.068283;
        maxX[306]=(float)0.78539;
        maxX[307]=(float)0.39642;
        maxX[308]=(float)0.29499;
        maxX[309]=(float)0.48377;
        maxX[310]=(float)0.54461;
        maxX[311]=(float)0.39045;
        maxX[312]=(float)0.32178;
        maxX[313]=(float)0.399;
        maxX[314]=(float)0.46982;
        maxX[315]=(float)0.38507;
        maxX[316]=(float)0.20119;
        maxX[317]=(float)0.18906;
        maxX[318]=(float)0.35594;
        maxX[319]=(float)0.23962;
        maxX[320]=(float)0.676;
        maxX[321]=(float)0.57277;
        maxX[322]=(float)0.75149;
        maxX[323]=(float)0.59352;
        maxX[324]=(float)0.243;
        maxX[325]=(float)0.53469;
        maxX[326]=(float)0.61289;
        maxX[327]=(float)0.62852;
        maxX[328]=(float)0.31029;
        maxX[329]=(float)0.20475;
        maxX[330]=(float)0.42871;
        maxX[331]=(float)0.4157;
        maxX[332]=(float)0.33859;
        maxX[333]=(float)0.19495;
        maxX[334]=(float)0.7762;
        maxX[335]=(float)0.54397;
        maxX[336]=(float)0.12782;
        maxX[337]=(float)0.32404;
        maxX[338]=(float)0.14503;
        maxX[339]=(float)0.41999;
        maxX[340]=(float)0.22739;
        maxX[341]=(float)0.20134;
        maxX[342]=(float)1.035;
        maxX[343]=(float)0.3237;
        maxX[344]=(float)0.72449;
        maxX[345]=(float)0.38827;
        maxX[346]=(float)0.36555;
        maxX[347]=(float)0.2163;
        maxX[348]=(float)0.41722;
        maxX[349]=(float)0.31509;
        maxX[350]=(float)0.60562;
        maxX[351]=(float)0.88521;
        maxX[352]=(float)0.34617;
        maxX[353]=(float)0.28801;
        maxX[354]=(float)0.56228;
        maxX[355]=(float)0.46571;
        maxX[356]=(float)0.71461;
        maxX[357]=(float)0.22902;
        maxX[358]=(float)0.26213;
        maxX[359]=(float)0.81474;
        maxX[360]=(float)0.57063;
        maxX[361]=(float)0.22482;
        maxX[362]=(float)0.44931;
        maxX[363]=(float)0.44472;
        maxX[364]=(float)0.24142;
        maxX[365]=(float)0.11496;
        maxX[366]=(float)0.51429;
        maxX[367]=(float)0.40472;
        maxX[368]=(float)0.093346;
        maxX[369]=(float)0.62309;
        maxX[370]=(float)0.087636;
        maxX[371]=(float)0.061271;
        maxX[372]=(float)0.53614;
        maxX[373]=(float)0.13955;
        maxX[374]=(float)0.71852;
        maxX[375]=(float)0.23101;
        maxX[376]=(float)0.59022;
        maxX[377]=(float)0.64333;
        maxX[378]=(float)0.75359;
        maxX[379]=(float)0.36175;
        maxX[380]=(float)0.26703;
        maxX[381]=(float)0.67341;
        maxX[382]=(float)0.087992;
        maxX[383]=(float)0.5862;
        maxX[384]=(float)0.077735;
        maxX[385]=(float)0.12741;
        maxX[386]=(float)0.22369;
        maxX[387]=(float)0.2799;
        maxX[388]=(float)0.31521;
        maxX[389]=(float)0.39245;
        maxX[390]=(float)0.22253;
        maxX[391]=(float)0.25075;
        maxX[392]=(float)0.241;
        maxX[393]=(float)0.41759;
        maxX[394]=(float)0.26629;
        maxX[395]=(float)0.22823;
        maxX[396]=(float)0.65175;
        maxX[397]=(float)0.32123;
        maxX[398]=(float)0.10197;
        maxX[399]=(float)0.20435;
        maxX[400]=(float)0.61676;
        maxX[401]=(float)0.34713;
        maxX[402]=(float)0.2712;
        maxX[403]=(float)0.3262;
        maxX[404]=(float)0.43403;
        maxX[405]=(float)0.34559;
        maxX[406]=(float)0.17185;
        maxX[407]=(float)0.050925;
        maxX[408]=(float)0.44495;
        maxX[409]=(float)0.1563;
        maxX[410]=(float)0.4348;
        maxX[411]=(float)0.33843;
        maxX[412]=(float)0.24098;
        maxX[413]=(float)0.20257;
        maxX[414]=(float)0.47224;
        maxX[415]=(float)0.31601;
        maxX[416]=(float)0.069216;
        maxX[417]=(float)0.14158;
        maxX[418]=(float)0.27049;
        maxX[419]=(float)0.46966;
        maxX[420]=(float)0.20584;
        maxX[421]=(float)0.50154;
        maxX[422]=(float)0.047378;
        maxX[423]=(float)0.31352;
        maxX[424]=(float)0.63605;
        maxX[425]=(float)0.24509;
        maxX[426]=(float)0.14776;
        maxX[427]=(float)0.081591;
        maxX[428]=(float)0.22809;
        maxX[429]=(float)0.17187;
        maxX[430]=(float)0.29835;
        maxX[431]=(float)0.32655;
        maxX[432]=(float)0.14715;
        maxX[433]=(float)0.64834;
        maxX[434]=(float)0.20806;
        maxX[435]=(float)0.18944;
        maxX[436]=(float)0.43278;
        maxX[437]=(float)0.2608;
        maxX[438]=(float)0.29243;
        maxX[439]=(float)0.10769;
        maxX[440]=(float)0.34053;
        maxX[441]=(float)0.14489;
        maxX[442]=(float)0.32658;
        maxX[443]=(float)0.10221;
        maxX[444]=(float)0.053177;
        maxX[445]=(float)0.26187;
        maxX[446]=(float)0.31661;
        maxX[447]=(float)0.31836;
        maxX[448]=(float)0.24472;
        maxX[449]=(float)0.10747;
        maxX[450]=(float)0.37573;
        maxX[451]=(float)0.097788;
        maxX[452]=(float)0.21222;
        maxX[453]=(float)0.13717;
        maxX[454]=(float)0.17173;
        maxX[455]=(float)0.097531;
        maxX[456]=(float)0.097872;
        maxX[457]=(float)0.28205;
        maxX[458]=(float)0.18708;
        maxX[459]=(float)0.18403;
        maxX[460]=(float)0.13989;
        maxX[461]=(float)0.16867;
        maxX[462]=(float)0.031854;
        maxX[463]=(float)0.33243;
        maxX[464]=(float)0.076086;
        maxX[465]=(float)0.11414;
        maxX[466]=(float)0.10522;
        maxX[467]=(float)0.067625;
        maxX[468]=(float)0.068461;
        maxX[469]=(float)0.15507;
        maxX[470]=(float)0.17025;
        maxX[471]=(float)0.1731;
        maxX[472]=(float)0.15621;
        maxX[473]=(float)0.043042;
        maxX[474]=(float)0.06201;
        maxX[475]=(float)0.0070008;
        maxX[476]=(float)0.25681;
        maxX[477]=(float)0.13429;
        maxX[478]=(float)0.24037;
        maxX[479]=(float)0.18817;
        maxX[480]=(float)0.18196;
        maxX[481]=(float)0.17687;
        maxX[482]=(float)0.27124;
        maxX[483]=(float)0.10348;
        maxX[484]=(float)0.092793;
        maxX[485]=(float)0.13315;
        maxX[486]=(float)0.0044598;
        maxX[487]=(float)0.070282;
        maxX[488]=(float)0.052831;
        maxX[489]=(float)0.17753;
        maxX[490]=(float)0.035303;
        maxX[491]=(float)0.059112;
        maxX[492]=(float)0.14288;
        maxX[493]=(float)0.048258;
        maxX[494]=(float)0.11489;
        maxX[495]=(float)0.15098;
        maxX[496]=(float)0.10591;
        maxX[497]=(float)0.070011;
        maxX[498]=(float)0.097882;
        maxX[499]=(float)0.095817;
        maxX[500]=(float)0.16388;
        maxX[501]=(float)0.049738;
        maxX[502]=(float)0.12744;
        maxX[503]=(float)0.16281;
        maxX[504]=(float)0.1869;
        maxX[505]=(float)0.070807;
        maxX[506]=(float)0.09666;
        maxX[507]=(float)0.028483;
        maxX[508]=(float)0.15274;
        maxX[509]=(float)0.030143;
        maxX[510]=(float)0.062729;
        maxX[511]=(float)0.15235;
        maxX[512]=(float)0.13025;
        maxX[513]=(float)0.15235;
        maxX[514]=(float)0.062729;
        maxX[515]=(float)0.030143;
        maxX[516]=(float)0.15274;
        maxX[517]=(float)0.028483;
        maxX[518]=(float)0.09666;
        maxX[519]=(float)0.070807;
        maxX[520]=(float)0.1869;
        maxX[521]=(float)0.16281;
        maxX[522]=(float)0.12744;
        maxX[523]=(float)0.049738;
        maxX[524]=(float)0.16388;
        maxX[525]=(float)0.095817;
        maxX[526]=(float)0.097882;
        maxX[527]=(float)0.070011;
        maxX[528]=(float)0.10591;
        maxX[529]=(float)0.15098;
        maxX[530]=(float)0.11489;
        maxX[531]=(float)0.048258;
        maxX[532]=(float)0.14288;
        maxX[533]=(float)0.059112;
        maxX[534]=(float)0.035303;
        maxX[535]=(float)0.17753;
        maxX[536]=(float)0.052831;
        maxX[537]=(float)0.070282;
        maxX[538]=(float)0.0044598;
        maxX[539]=(float)0.13315;
        maxX[540]=(float)0.092793;
        maxX[541]=(float)0.10348;
        maxX[542]=(float)0.27124;
        maxX[543]=(float)0.17687;
        maxX[544]=(float)0.18196;
        maxX[545]=(float)0.18817;
        maxX[546]=(float)0.24037;
        maxX[547]=(float)0.13429;
        maxX[548]=(float)0.25681;
        maxX[549]=(float)0.0070008;
        maxX[550]=(float)0.06201;
        maxX[551]=(float)0.043042;
        maxX[552]=(float)0.15621;
        maxX[553]=(float)0.1731;
        maxX[554]=(float)0.17025;
        maxX[555]=(float)0.15507;
        maxX[556]=(float)0.068461;
        maxX[557]=(float)0.067625;
        maxX[558]=(float)0.10522;
        maxX[559]=(float)0.11414;
        maxX[560]=(float)0.076086;
        maxX[561]=(float)0.33243;
        maxX[562]=(float)0.031854;
        maxX[563]=(float)0.16867;
        maxX[564]=(float)0.13989;
        maxX[565]=(float)0.18403;
        maxX[566]=(float)0.18708;
        maxX[567]=(float)0.28205;
        maxX[568]=(float)0.097872;
        maxX[569]=(float)0.097531;
        maxX[570]=(float)0.17173;
        maxX[571]=(float)0.13717;
        maxX[572]=(float)0.21222;
        maxX[573]=(float)0.097788;
        maxX[574]=(float)0.37573;
        maxX[575]=(float)0.10747;
        maxX[576]=(float)0.24472;
        maxX[577]=(float)0.31836;
        maxX[578]=(float)0.31661;
        maxX[579]=(float)0.26187;
        maxX[580]=(float)0.053177;
        maxX[581]=(float)0.10221;
        maxX[582]=(float)0.32658;
        maxX[583]=(float)0.14489;
        maxX[584]=(float)0.34053;
        maxX[585]=(float)0.10769;
        maxX[586]=(float)0.29243;
        maxX[587]=(float)0.2608;
        maxX[588]=(float)0.43278;
        maxX[589]=(float)0.18944;
        maxX[590]=(float)0.20806;
        maxX[591]=(float)0.64834;
        maxX[592]=(float)0.14715;
        maxX[593]=(float)0.32655;
        maxX[594]=(float)0.29835;
        maxX[595]=(float)0.17187;
        maxX[596]=(float)0.22809;
        maxX[597]=(float)0.081591;
        maxX[598]=(float)0.14776;
        maxX[599]=(float)0.24509;
        maxX[600]=(float)0.63605;
        maxX[601]=(float)0.31352;
        maxX[602]=(float)0.047378;
        maxX[603]=(float)0.50154;
        maxX[604]=(float)0.20584;
        maxX[605]=(float)0.46966;
        maxX[606]=(float)0.27049;
        maxX[607]=(float)0.14158;
        maxX[608]=(float)0.069216;
        maxX[609]=(float)0.31601;
        maxX[610]=(float)0.47224;
        maxX[611]=(float)0.20257;
        maxX[612]=(float)0.24098;
        maxX[613]=(float)0.33843;
        maxX[614]=(float)0.4348;
        maxX[615]=(float)0.1563;
        maxX[616]=(float)0.44495;
        maxX[617]=(float)0.050925;
        maxX[618]=(float)0.17185;
        maxX[619]=(float)0.34559;
        maxX[620]=(float)0.43403;
        maxX[621]=(float)0.3262;
        maxX[622]=(float)0.2712;
        maxX[623]=(float)0.34713;
        maxX[624]=(float)0.61676;
        maxX[625]=(float)0.20435;
        maxX[626]=(float)0.10197;
        maxX[627]=(float)0.32123;
        maxX[628]=(float)0.65175;
        maxX[629]=(float)0.22823;
        maxX[630]=(float)0.26629;
        maxX[631]=(float)0.41759;
        maxX[632]=(float)0.241;
        maxX[633]=(float)0.25075;
        maxX[634]=(float)0.22253;
        maxX[635]=(float)0.39245;
        maxX[636]=(float)0.31521;
        maxX[637]=(float)0.2799;
        maxX[638]=(float)0.22369;
        maxX[639]=(float)0.12741;
        maxX[640]=(float)0.077735;
        maxX[641]=(float)0.5862;
        maxX[642]=(float)0.087992;
        maxX[643]=(float)0.67341;
        maxX[644]=(float)0.26703;
        maxX[645]=(float)0.36175;
        maxX[646]=(float)0.75359;
        maxX[647]=(float)0.64333;
        maxX[648]=(float)0.59022;
        maxX[649]=(float)0.23101;
        maxX[650]=(float)0.71852;
        maxX[651]=(float)0.13955;
        maxX[652]=(float)0.53614;
        maxX[653]=(float)0.061271;
        maxX[654]=(float)0.087636;
        maxX[655]=(float)0.62309;
        maxX[656]=(float)0.093346;
        maxX[657]=(float)0.40472;
        maxX[658]=(float)0.51429;
        maxX[659]=(float)0.11496;
        maxX[660]=(float)0.24142;
        maxX[661]=(float)0.44472;
        maxX[662]=(float)0.44931;
        maxX[663]=(float)0.22482;
        maxX[664]=(float)0.57063;
        maxX[665]=(float)0.81474;
        maxX[666]=(float)0.26213;
        maxX[667]=(float)0.22902;
        maxX[668]=(float)0.71461;
        maxX[669]=(float)0.46571;
        maxX[670]=(float)0.56228;
        maxX[671]=(float)0.28801;
        maxX[672]=(float)0.34617;
        maxX[673]=(float)0.88521;
        maxX[674]=(float)0.60562;
        maxX[675]=(float)0.31509;
        maxX[676]=(float)0.41722;
        maxX[677]=(float)0.2163;
        maxX[678]=(float)0.36555;
        maxX[679]=(float)0.38827;
        maxX[680]=(float)0.72449;
        maxX[681]=(float)0.3237;
        maxX[682]=(float)1.035;
        maxX[683]=(float)0.20134;
        maxX[684]=(float)0.22739;
        maxX[685]=(float)0.41999;
        maxX[686]=(float)0.14503;
        maxX[687]=(float)0.32404;
        maxX[688]=(float)0.12782;
        maxX[689]=(float)0.54397;
        maxX[690]=(float)0.7762;
        maxX[691]=(float)0.19495;
        maxX[692]=(float)0.33859;
        maxX[693]=(float)0.4157;
        maxX[694]=(float)0.42871;
        maxX[695]=(float)0.20475;
        maxX[696]=(float)0.31029;
        maxX[697]=(float)0.62852;
        maxX[698]=(float)0.61289;
        maxX[699]=(float)0.53469;
        maxX[700]=(float)0.243;
        maxX[701]=(float)0.59352;
        maxX[702]=(float)0.75149;
        maxX[703]=(float)0.57277;
        maxX[704]=(float)0.676;
        maxX[705]=(float)0.23962;
        maxX[706]=(float)0.35594;
        maxX[707]=(float)0.18906;
        maxX[708]=(float)0.20119;
        maxX[709]=(float)0.38507;
        maxX[710]=(float)0.46982;
        maxX[711]=(float)0.399;
        maxX[712]=(float)0.32178;
        maxX[713]=(float)0.39045;
        maxX[714]=(float)0.54461;
        maxX[715]=(float)0.48377;
        maxX[716]=(float)0.29499;
        maxX[717]=(float)0.39642;
        maxX[718]=(float)0.78539;
        maxX[719]=(float)0.068283;
        maxX[720]=(float)0.57405;
        maxX[721]=(float)0.39654;
        maxX[722]=(float)0.38918;
        maxX[723]=(float)0.24762;
        maxX[724]=(float)1.0939;
        maxX[725]=(float)0.77982;
        maxX[726]=(float)0.56377;
        maxX[727]=(float)0.4104;
        maxX[728]=(float)0.49743;
        maxX[729]=(float)0.38703;
        maxX[730]=(float)0.59224;
        maxX[731]=(float)0.34081;
        maxX[732]=(float)0.16506;
        maxX[733]=(float)0.33752;
        maxX[734]=(float)0.36043;
        maxX[735]=(float)0.51544;
        maxX[736]=(float)0.66844;
        maxX[737]=(float)0.29127;
        maxX[738]=(float)0.29547;
        maxX[739]=(float)0.3315;
        maxX[740]=(float)0.63568;
        maxX[741]=(float)0.18365;
        maxX[742]=(float)0.237;
        maxX[743]=(float)1.1259;
        maxX[744]=(float)0.38088;
        maxX[745]=(float)0.85703;
        maxX[746]=(float)0.63627;
        maxX[747]=(float)0.5586;
        maxX[748]=(float)0.87274;
        maxX[749]=(float)0.64635;
        maxX[750]=(float)0.62766;
        maxX[751]=(float)0.27839;
        maxX[752]=(float)0.37928;
        maxX[753]=(float)0.77802;
        maxX[754]=(float)0.37249;
        maxX[755]=(float)0.61108;
        maxX[756]=(float)0.89724;
        maxX[757]=(float)0.37623;
        maxX[758]=(float)0.38649;
        maxX[759]=(float)0.76557;
        maxX[760]=(float)0.74863;
        maxX[761]=(float)0.80751;
        maxX[762]=(float)0.55009;
        maxX[763]=(float)0.33488;
        maxX[764]=(float)0.30855;
        maxX[765]=(float)0.43624;
        maxX[766]=(float)0.3261;
        maxX[767]=(float)0.46392;
        maxX[768]=(float)0.30483;
        maxX[769]=(float)0.71029;
        maxX[770]=(float)0.47662;
        maxX[771]=(float)0.85651;
        maxX[772]=(float)0.594;
        maxX[773]=(float)0.56078;
        maxX[774]=(float)0.19324;
        maxX[775]=(float)0.69803;
        maxX[776]=(float)0.28156;
        maxX[777]=(float)0.32983;
        maxX[778]=(float)0.83064;
        maxX[779]=(float)0.39029;
        maxX[780]=(float)0.67313;
        maxX[781]=(float)0.64055;
        maxX[782]=(float)0.56126;
        maxX[783]=(float)1.3767;
        maxX[784]=(float)0.24759;
        maxX[785]=(float)0.50247;
        maxX[786]=(float)0.67711;
        maxX[787]=(float)0.73772;
        maxX[788]=(float)0.65299;
        maxX[789]=(float)0.80287;
        maxX[790]=(float)0.25111;
        maxX[791]=(float)0.87972;
        maxX[792]=(float)0.54109;
        maxX[793]=(float)0.95006;
        maxX[794]=(float)0.2416;
        maxX[795]=(float)1.0877;
        maxX[796]=(float)0.56141;
        maxX[797]=(float)0.30533;
        maxX[798]=(float)0.77607;
        maxX[799]=(float)0.87786;
        maxX[800]=(float)1.0838;
        maxX[801]=(float)0.19042;
        maxX[802]=(float)1.6166;
        maxX[803]=(float)0.61789;
        maxX[804]=(float)0.79282;
        maxX[805]=(float)0.22085;
        maxX[806]=(float)1.1966;
        maxX[807]=(float)0.16218;
        maxX[808]=(float)0.56796;
        maxX[809]=(float)0.62972;
        maxX[810]=(float)1.3386;
        maxX[811]=(float)0.60921;
        maxX[812]=(float)0.68089;
        maxX[813]=(float)0.94652;
        maxX[814]=(float)0.89234;
        maxX[815]=(float)0.78963;
        maxX[816]=(float)1.1666;
        maxX[817]=(float)0.49824;
        maxX[818]=(float)0.5025;
        maxX[819]=(float)0.93903;
        maxX[820]=(float)0.74051;
        maxX[821]=(float)0.8978;
        maxX[822]=(float)0.24049;
        maxX[823]=(float)0.6881;
        maxX[824]=(float)0.26573;
        maxX[825]=(float)1.2027;
        maxX[826]=(float)1.1644;
        maxX[827]=(float)0.88943;
        maxX[828]=(float)1.1727;
        maxX[829]=(float)0.64895;
        maxX[830]=(float)0.62246;
        maxX[831]=(float)1.2008;
        maxX[832]=(float)1.0257;
        maxX[833]=(float)0.85753;
        maxX[834]=(float)0.1229;
        maxX[835]=(float)1.08;
        maxX[836]=(float)0.70761;
        maxX[837]=(float)0.54157;
        maxX[838]=(float)0.073734;
        maxX[839]=(float)1.7771;
        maxX[840]=(float)0.77541;
        maxX[841]=(float)0.81716;
        maxX[842]=(float)1.5739;
        maxX[843]=(float)1.3174;
        maxX[844]=(float)0.36734;
        maxX[845]=(float)0.84014;
        maxX[846]=(float)0.54777;
        maxX[847]=(float)1.7149;
        maxX[848]=(float)0.90732;
        maxX[849]=(float)0.79547;
        maxX[850]=(float)1.1063;
        maxX[851]=(float)0.13044;
        maxX[852]=(float)0.60813;
        maxX[853]=(float)0.85414;
        maxX[854]=(float)0.71617;
        maxX[855]=(float)1.2266;
        maxX[856]=(float)0.78731;
        maxX[857]=(float)0.8183;
        maxX[858]=(float)1.6708;
        maxX[859]=(float)2.5047;
        maxX[860]=(float)1.5289;
        maxX[861]=(float)1.0576;
        maxX[862]=(float)1.7593;
        maxX[863]=(float)1.953;
        maxX[864]=(float)1.1394;
        maxX[865]=(float)1.4632;
        maxX[866]=(float)1.3689;
        maxX[867]=(float)1.0667;
        maxX[868]=(float)0.60466;
        maxX[869]=(float)2.2566;
        maxX[870]=(float)2.3487;
        maxX[871]=(float)0.16443;
        maxX[872]=(float)0.54528;
        maxX[873]=(float)0.37741;
        maxX[874]=(float)1.7554;
        maxX[875]=(float)0.75683;
        maxX[876]=(float)1.3149;
        maxX[877]=(float)1.1335;
        maxX[878]=(float)2.1535;
        maxX[879]=(float)1.6258;
        maxX[880]=(float)0.75551;
        maxX[881]=(float)1.9736;
        maxX[882]=(float)1.3372;
        maxX[883]=(float)0.6987;
        maxX[884]=(float)3.3265;
        maxX[885]=(float)0.75331;
        maxX[886]=(float)0.86796;
        maxX[887]=(float)2.3247;
        maxX[888]=(float)0.45625;
        maxX[889]=(float)1.0607;
        maxX[890]=(float)2.2967;
        maxX[891]=(float)2.2015;
        maxX[892]=(float)0.93188;
        maxX[893]=(float)0.14798;
        maxX[894]=(float)1.5236;
        maxX[895]=(float)1.6058;
        maxX[896]=(float)3.2169;
        maxX[897]=(float)0.174;
        maxX[898]=(float)1.0475;
        maxX[899]=(float)3.1537;
        maxX[900]=(float)2.2185;
        maxX[901]=(float)3.7148;
        maxX[902]=(float)2.0004;
        maxX[903]=(float)2.8938;
        maxX[904]=(float)3.1824;
        maxX[905]=(float)1.9133;
        maxX[906]=(float)0.92125;
        maxX[907]=(float)4.178;
        maxX[908]=(float)8.3206;
        maxX[909]=(float)3.424;
        maxX[910]=(float)6.256;
        maxX[911]=(float)9.1295;
        maxX[912]=(float)5.0253;
        maxX[913]=(float)0.87056;
        maxX[914]=(float)3.5446;
        maxX[915]=(float)8.4952;
        maxX[916]=(float)3.6187;
        maxX[917]=(float)9.6939;
        maxX[918]=(float)9.5859;
        maxX[919]=(float)17.79;
        maxX[920]=(float)23.459;
        maxX[921]=(float)13.646;
        maxX[922]=(float)11.937;
        maxX[923]=(float)11.278;
        maxX[924]=(float)8.1891;
        maxX[925]=(float)10.591;
        maxX[926]=(float)6.3864;
        maxX[927]=(float)4.2101;
        maxX[928]=(float)3.32;
        maxX[929]=(float)6.0361;
        maxX[930]=(float)5.8088;
        maxX[931]=(float)1.5902;
        maxX[932]=(float)3.9172;
        maxX[933]=(float)3.8465;
        maxX[934]=(float)2.837;
        maxX[935]=(float)6.0599;
        maxX[936]=(float)2.1269;
        maxX[937]=(float)2.6822;
        maxX[938]=(float)3.2156;
        maxX[939]=(float)2.9309;
        maxX[940]=(float)2.5636;
        maxX[941]=(float)0.5681;
        maxX[942]=(float)1.6108;
        maxX[943]=(float)1.0198;
        maxX[944]=(float)3.6739;
        maxX[945]=(float)3.8007;
        maxX[946]=(float)5.7632;
        maxX[947]=(float)4.397;
        maxX[948]=(float)2.3226;
        maxX[949]=(float)7.0567;
        maxX[950]=(float)2.043;
        maxX[951]=(float)1.9126;
        maxX[952]=(float)4.3184;
        maxX[953]=(float)2.8733;
        maxX[954]=(float)8.7579;
        maxX[955]=(float)4.6782;
        maxX[956]=(float)7.2886;
        maxX[957]=(float)2.1049;
        maxX[958]=(float)4.8951;
        maxX[959]=(float)8.9108;
        maxX[960]=(float)3.3865;
        maxX[961]=(float)6.0445;
        maxX[962]=(float)6.5719;
        maxX[963]=(float)11.565;
        maxX[964]=(float)8.4907;
        maxX[965]=(float)5.9809;
        maxX[966]=(float)4.8174;
        maxX[967]=(float)10.927;
        maxX[968]=(float)9.5022;
        maxX[969]=(float)8.1301;
        maxX[970]=(float)12.805;
        maxX[971]=(float)19.183;
        maxX[972]=(float)70.405;
        maxX[973]=(float)22.985;
        maxX[974]=(float)13.465;
        maxX[975]=(float)14.848;
        maxX[976]=(float)8.613;
        maxX[977]=(float)5.4494;
        maxX[978]=(float)9.3488;
        maxX[979]=(float)7.3353;
        maxX[980]=(float)2.4765;
        maxX[981]=(float)1.7653;
        maxX[982]=(float)3.5735;
        maxX[983]=(float)2.7194;
        maxX[984]=(float)3.2833;
        maxX[985]=(float)7.0355;
        maxX[986]=(float)3.6378;
        maxX[987]=(float)2.5591;
        maxX[988]=(float)3.1079;
        maxX[989]=(float)3.4424;
        maxX[990]=(float)0.62008;
        maxX[991]=(float)2.514;
        maxX[992]=(float)3.3399;
        maxX[993]=(float)2.3712;
        maxX[994]=(float)1.1156;
        maxX[995]=(float)2.0781;
        maxX[996]=(float)2.887;
        maxX[997]=(float)1.6036;
        maxX[998]=(float)1.974;
        maxX[999]=(float)1.8839;
        maxX[1000]=(float)2.1835;
        maxX[1001]=(float)2.0543;
        maxX[1002]=(float)0.94117;
        maxX[1003]=(float)2.4207;
        maxX[1004]=(float)0.54121;
        maxX[1005]=(float)2.0512;
        maxX[1006]=(float)2.6383;
        maxX[1007]=(float)2.0123;
        maxX[1008]=(float)1.6539;
        maxX[1009]=(float)2.6851;
        maxX[1010]=(float)2.5148;
        maxX[1011]=(float)2.4913;
        maxX[1012]=(float)2.3917;
        maxX[1013]=(float)2.3257;
        maxX[1014]=(float)3.8697;
        maxX[1015]=(float)5.5666;
        maxX[1016]=(float)5.7237;
        maxX[1017]=(float)2.0612;
        maxX[1018]=(float)3.6591;
        maxX[1019]=(float)3.9484;
        maxX[1020]=(float)4.82;
        maxX[1021]=(float)5.6862;
        maxX[1022]=(float)3.542;
        maxX[1023]=(float)11.426;

        maxX2[0]=(float)-0.0063171;
        maxX2[1]=(float)0.009491;
        maxX2[2]=(float)-0.015167;
        maxX2[3]=(float)-0.084961;
        maxX2[4]=(float)0.013153;
        maxX2[5]=(float)0.00085449;
        maxX2[6]=(float)-0.0081787;
        maxX2[7]=(float)-0.013641;
        maxX2[8]=(float)-0.081635;
        maxX2[9]=(float)-0.069153;
        maxX2[10]=(float)-0.015045;
        maxX2[11]=(float)0.013397;
        maxX2[12]=(float)-0.00067139;
        maxX2[13]=(float)-0.094086;
        maxX2[14]=(float)-0.20547;
        maxX2[15]=(float)-0.25632;
        maxX2[16]=(float)-0.26044;
        maxX2[17]=(float)-0.23694;
        maxX2[18]=(float)-0.16977;
        maxX2[19]=(float)-0.098083;
        maxX2[20]=(float)-0.0028076;
        maxX2[21]=(float)0.12439;
        maxX2[22]=(float)0.21503;
        maxX2[23]=(float)0.22662;
        maxX2[24]=(float)0.19574;
        maxX2[25]=(float)0.15396;
        maxX2[26]=(float)0.063782;
        maxX2[27]=(float)-0.0015564;
        maxX2[28]=(float)-0.048584;
        maxX2[29]=(float)-0.059906;
        maxX2[30]=(float)-0.023102;
        maxX2[31]=(float)-0.052338;
        maxX2[32]=(float)-0.096069;
        maxX2[33]=(float)-0.1188;
        maxX2[34]=(float)-0.19055;
        maxX2[35]=(float)-0.27429;
        maxX2[36]=(float)-0.25735;
        maxX2[37]=(float)-0.21384;
        maxX2[38]=(float)-0.11307;
        maxX2[39]=(float)0.052551;
        maxX2[40]=(float)0.1879;
        maxX2[41]=(float)0.27802;
        maxX2[42]=(float)0.32477;
        maxX2[43]=(float)0.31683;
        maxX2[44]=(float)0.26205;
        maxX2[45]=(float)0.15146;
        maxX2[46]=(float)0.02243;
        maxX2[47]=(float)-0.027588;
        maxX2[48]=(float)-0.059326;
        maxX2[49]=(float)-0.067383;
        maxX2[50]=(float)-0.049347;
        maxX2[51]=(float)-0.066467;
        maxX2[52]=(float)-0.095917;
        maxX2[53]=(float)-0.15881;
        maxX2[54]=(float)-0.2832;
        maxX2[55]=(float)-0.32477;
        maxX2[56]=(float)-0.27692;
        maxX2[57]=(float)-0.16565;
        maxX2[58]=(float)-0.012024;
        maxX2[59]=(float)0.13327;
        maxX2[60]=(float)0.2886;
        maxX2[61]=(float)0.33258;
        maxX2[62]=(float)0.29501;
        maxX2[63]=(float)0.25674;
        maxX2[64]=(float)0.15213;
        maxX2[65]=(float)0.051453;
        maxX2[66]=(float)-0.025696;
        maxX2[67]=(float)-0.063049;
        maxX2[68]=(float)-0.061493;
        maxX2[69]=(float)-0.075958;
        maxX2[70]=(float)-0.080505;
        maxX2[71]=(float)-0.14432;
        maxX2[72]=(float)-0.16907;
        maxX2[73]=(float)-0.1731;
        maxX2[74]=(float)-0.13937;
        maxX2[75]=(float)-0.096375;
        maxX2[76]=(float)-0.092072;
        maxX2[77]=(float)0.021942;
        maxX2[78]=(float)0.10532;
        maxX2[79]=(float)0.15976;
        maxX2[80]=(float)0.20697;
        maxX2[81]=(float)0.18066;
        maxX2[82]=(float)0.13324;
        maxX2[83]=(float)0.075836;
        maxX2[84]=(float)0.01651;
        maxX2[85]=(float)-0.023193;
        maxX2[86]=(float)-0.018951;
        maxX2[87]=(float)0.006897;
        maxX2[88]=(float)0.051147;
        maxX2[89]=(float)0.031891;
        maxX2[90]=(float)-0.062683;
        maxX2[91]=(float)-0.13593;
        maxX2[92]=(float)-0.17575;
        maxX2[93]=(float)-0.18948;
        maxX2[94]=(float)-0.16541;
        maxX2[95]=(float)-0.10086;
        maxX2[96]=(float)-0.011444;
        maxX2[97]=(float)0.076538;
        maxX2[98]=(float)0.16095;
        maxX2[99]=(float)0.18314;
        maxX2[100]=(float)0.16312;
        maxX2[101]=(float)0.10074;
        maxX2[102]=(float)0.00085449;
        maxX2[103]=(float)-0.051361;
        maxX2[104]=(float)-0.073181;
        maxX2[105]=(float)-0.064362;
        maxX2[106]=(float)-0.032227;
        maxX2[107]=(float)-0.039856;
        maxX2[108]=(float)-0.049744;
        maxX2[109]=(float)-0.066071;
        maxX2[110]=(float)-0.078156;
        maxX2[111]=(float)-0.097565;
        maxX2[112]=(float)-0.12466;
        maxX2[113]=(float)-0.1214;
        maxX2[114]=(float)-0.074097;
        maxX2[115]=(float)-0.014038;
        maxX2[116]=(float)0.057526;
        maxX2[117]=(float)0.10538;
        maxX2[118]=(float)0.1347;
        maxX2[119]=(float)0.11902;
        maxX2[120]=(float)0.070221;
        maxX2[121]=(float)0.023285;
        maxX2[122]=(float)-0.028839;
        maxX2[123]=(float)-0.0029602;
        maxX2[124]=(float)0.018555;
        maxX2[125]=(float)0.028625;
        maxX2[126]=(float)0.013;
        maxX2[127]=(float)-0.033417;
        maxX2[128]=(float)-0.092621;
        maxX2[129]=(float)-0.15082;
        maxX2[130]=(float)-0.15646;
        maxX2[131]=(float)-0.18204;
        maxX2[132]=(float)-0.19693;
        maxX2[133]=(float)-0.18195;
        maxX2[134]=(float)-0.10007;
        maxX2[135]=(float)0.00012207;
        maxX2[136]=(float)0.057922;
        maxX2[137]=(float)0.15015;
        maxX2[138]=(float)0.21783;
        maxX2[139]=(float)0.20816;
        maxX2[140]=(float)0.15707;
        maxX2[141]=(float)0.086639;
        maxX2[142]=(float)0.020416;
        maxX2[143]=(float)-0.030121;
        maxX2[144]=(float)-0.0020752;
        maxX2[145]=(float)0.021637;
        maxX2[146]=(float)0.0016785;
        maxX2[147]=(float)-0.021027;
        maxX2[148]=(float)-0.094971;
        maxX2[149]=(float)-0.1683;
        maxX2[150]=(float)-0.15652;
        maxX2[151]=(float)-0.13718;
        maxX2[152]=(float)-0.10107;
        maxX2[153]=(float)-0.043396;
        maxX2[154]=(float)0.028839;
        maxX2[155]=(float)0.098053;
        maxX2[156]=(float)0.15906;
        maxX2[157]=(float)0.1944;
        maxX2[158]=(float)0.18173;
        maxX2[159]=(float)0.14896;
        maxX2[160]=(float)0.089966;
        maxX2[161]=(float)0.063965;
        maxX2[162]=(float)0.071289;
        maxX2[163]=(float)0.05249;
        maxX2[164]=(float)0.00033569;
        maxX2[165]=(float)0.026489;
        maxX2[166]=(float)0.069397;
        maxX2[167]=(float)0.080017;
        maxX2[168]=(float)0.032532;
        maxX2[169]=(float)-0.071594;
        maxX2[170]=(float)-0.12839;
        maxX2[171]=(float)-0.16675;
        maxX2[172]=(float)-0.16827;
        maxX2[173]=(float)-0.11978;
        maxX2[174]=(float)-0.03186;
        maxX2[175]=(float)0.097778;
        maxX2[176]=(float)0.23938;
        maxX2[177]=(float)0.34402;
        maxX2[178]=(float)0.34848;
        maxX2[179]=(float)0.29724;
        maxX2[180]=(float)0.19623;
        maxX2[181]=(float)0.10925;
        maxX2[182]=(float)0.042633;
        maxX2[183]=(float)0.010864;
        maxX2[184]=(float)0.047699;
        maxX2[185]=(float)0.078308;
        maxX2[186]=(float)0.098785;
        maxX2[187]=(float)0.057831;
        maxX2[188]=(float)-0.057037;
        maxX2[189]=(float)-0.13046;
        maxX2[190]=(float)-0.1727;
        maxX2[191]=(float)-0.17429;
        maxX2[192]=(float)-0.11676;
        maxX2[193]=(float)-0.026123;
        maxX2[194]=(float)0.099792;
        maxX2[195]=(float)0.23334;
        maxX2[196]=(float)0.31598;
        maxX2[197]=(float)0.35666;
        maxX2[198]=(float)0.28543;
        maxX2[199]=(float)0.16135;
        maxX2[200]=(float)0.084442;
        maxX2[201]=(float)0.022705;
        maxX2[202]=(float)-0.010864;
        maxX2[203]=(float)0.0062866;
        maxX2[204]=(float)0.064453;
        maxX2[205]=(float)0.057098;
        maxX2[206]=(float)-0.024292;
        maxX2[207]=(float)-0.084625;
        maxX2[208]=(float)-0.10474;
        maxX2[209]=(float)-0.10739;
        maxX2[210]=(float)-0.089844;
        maxX2[211]=(float)-0.04892;
        maxX2[212]=(float)0.025055;
        maxX2[213]=(float)0.076935;
        maxX2[214]=(float)0.15222;
        maxX2[215]=(float)0.20078;
        maxX2[216]=(float)0.18103;
        maxX2[217]=(float)0.15814;
        maxX2[218]=(float)0.0979;
        maxX2[219]=(float)0.053589;
        maxX2[220]=(float)0.043182;
        maxX2[221]=(float)0.055115;
        maxX2[222]=(float)0.079956;
        maxX2[223]=(float)0.078491;
        maxX2[224]=(float)0.043945;
        maxX2[225]=(float)0.0036621;
        maxX2[226]=(float)-0.034363;
        maxX2[227]=(float)-0.043793;
        maxX2[228]=(float)-0.052063;
        maxX2[229]=(float)-0.05484;
        maxX2[230]=(float)-0.058899;
        maxX2[231]=(float)-0.097931;
        maxX2[232]=(float)-0.11804;
        maxX2[233]=(float)-0.10928;
        maxX2[234]=(float)-0.071869;
        maxX2[235]=(float)-0.0071106;
        maxX2[236]=(float)0.073364;
        maxX2[237]=(float)0.099792;
        maxX2[238]=(float)0.11084;
        maxX2[239]=(float)0.075531;
        maxX2[240]=(float)0.016785;
        maxX2[241]=(float)-0.0336;
        maxX2[242]=(float)-0.038422;
        maxX2[243]=(float)-0.0090332;
        maxX2[244]=(float)0.023041;
        maxX2[245]=(float)0.067993;
        maxX2[246]=(float)0.023071;
        maxX2[247]=(float)-0.076294;
        maxX2[248]=(float)-0.11697;
        maxX2[249]=(float)-0.14301;
        maxX2[250]=(float)-0.17029;
        maxX2[251]=(float)-0.14124;
        maxX2[252]=(float)-0.074432;
        maxX2[253]=(float)-0.027832;
        maxX2[254]=(float)0.040375;
        maxX2[255]=(float)0.15613;
        maxX2[256]=(float)0.21921;
        maxX2[257]=(float)0.23962;
        maxX2[258]=(float)0.2056;
        maxX2[259]=(float)0.10309;
        maxX2[260]=(float)-0.010315;
        maxX2[261]=(float)-0.086609;
        maxX2[262]=(float)-0.086151;
        maxX2[263]=(float)-0.04892;
        maxX2[264]=(float)-0.022278;
        maxX2[265]=(float)-0.011566;
        maxX2[266]=(float)0.013306;
        maxX2[267]=(float)-0.049194;
        maxX2[268]=(float)-0.13638;
        maxX2[269]=(float)-0.18457;
        maxX2[270]=(float)-0.23648;
        maxX2[271]=(float)-0.22992;
        maxX2[272]=(float)-0.11234;
        maxX2[273]=(float)0.0012817;
        maxX2[274]=(float)0.11649;
        maxX2[275]=(float)0.23801;
        maxX2[276]=(float)0.28967;
        maxX2[277]=(float)0.30292;
        maxX2[278]=(float)0.29053;
        maxX2[279]=(float)0.16608;
        maxX2[280]=(float)0.042267;
        maxX2[281]=(float)-0.0049744;
        maxX2[282]=(float)-0.013367;
        maxX2[283]=(float)-0.011139;
        maxX2[284]=(float)-0.019043;
        maxX2[285]=(float)-0.043823;
        maxX2[286]=(float)-0.094513;
        maxX2[287]=(float)-0.17599;
        maxX2[288]=(float)-0.26126;
        maxX2[289]=(float)-0.25183;
        maxX2[290]=(float)-0.23294;
        maxX2[291]=(float)-0.20337;
        maxX2[292]=(float)-0.08136;
        maxX2[293]=(float)0.099091;
        maxX2[294]=(float)0.24368;
        maxX2[295]=(float)0.30832;
        maxX2[296]=(float)0.31738;
        maxX2[297]=(float)0.28568;
        maxX2[298]=(float)0.17844;
        maxX2[299]=(float)0.076721;
        maxX2[300]=(float)0.033478;
        maxX2[301]=(float)0.00079346;
        maxX2[302]=(float)-0.04715;
        maxX2[303]=(float)-0.026367;
        maxX2[304]=(float)-0.036987;
        maxX2[305]=(float)-0.10367;
        maxX2[306]=(float)-0.12454;
        maxX2[307]=(float)-0.16443;
        maxX2[308]=(float)-0.17117;
        maxX2[309]=(float)-0.13153;
        maxX2[310]=(float)-0.10187;
        maxX2[311]=(float)-0.022583;
        maxX2[312]=(float)0.069672;
        maxX2[313]=(float)0.13712;
        maxX2[314]=(float)0.21509;
        maxX2[315]=(float)0.22037;
        maxX2[316]=(float)0.16797;
        maxX2[317]=(float)0.091064;
        maxX2[318]=(float)0.044403;
        maxX2[319]=(float)0.034485;
        maxX2[320]=(float)0.054443;
        maxX2[321]=(float)0.08316;
        maxX2[322]=(float)0.02478;
        maxX2[323]=(float)0.0054016;
        maxX2[324]=(float)-0.021545;
        maxX2[325]=(float)-0.095856;
        maxX2[326]=(float)-0.15268;
        maxX2[327]=(float)-0.17505;
        maxX2[328]=(float)-0.18906;
        maxX2[329]=(float)-0.18115;
        maxX2[330]=(float)-0.10657;
        maxX2[331]=(float)-0.0092773;
        maxX2[332]=(float)0.11191;
        maxX2[333]=(float)0.20139;
        maxX2[334]=(float)0.20999;
        maxX2[335]=(float)0.23376;
        maxX2[336]=(float)0.20782;
        maxX2[337]=(float)0.15125;
        maxX2[338]=(float)0.098145;
        maxX2[339]=(float)0.023193;
        maxX2[340]=(float)0.0089111;
        maxX2[341]=(float)0.023804;
        maxX2[342]=(float)-0.016083;
        maxX2[343]=(float)-0.018097;
        maxX2[344]=(float)0.026672;
        maxX2[345]=(float)0.029907;
        maxX2[346]=(float)0.0059204;
        maxX2[347]=(float)-0.051147;
        maxX2[348]=(float)-0.17874;
        maxX2[349]=(float)-0.23853;
        maxX2[350]=(float)-0.24661;
        maxX2[351]=(float)-0.19919;
        maxX2[352]=(float)-0.096222;
        maxX2[353]=(float)0.056305;
        maxX2[354]=(float)0.25266;
        maxX2[355]=(float)0.33255;
        maxX2[356]=(float)0.3208;
        maxX2[357]=(float)0.31879;
        maxX2[358]=(float)0.26239;
        maxX2[359]=(float)0.16589;
        maxX2[360]=(float)0.053497;
        maxX2[361]=(float)0.0038452;
        maxX2[362]=(float)-0.018036;
        maxX2[363]=(float)-0.065796;
        maxX2[364]=(float)-0.093201;
        maxX2[365]=(float)-0.084625;
        maxX2[366]=(float)-0.14545;
        maxX2[367]=(float)-0.21295;
        maxX2[368]=(float)-0.23764;
        maxX2[369]=(float)-0.25455;
        maxX2[370]=(float)-0.1965;
        maxX2[371]=(float)-0.095947;
        maxX2[372]=(float)0.053955;
        maxX2[373]=(float)0.20825;
        maxX2[374]=(float)0.30179;
        maxX2[375]=(float)0.32419;
        maxX2[376]=(float)0.29028;
        maxX2[377]=(float)0.21991;
        maxX2[378]=(float)0.12988;
        maxX2[379]=(float)0.076691;
        maxX2[380]=(float)0.054779;
        maxX2[381]=(float)0.034973;
        maxX2[382]=(float)0.068726;
        maxX2[383]=(float)0.10611;
        maxX2[384]=(float)0.072571;
        maxX2[385]=(float)0.0070801;
        maxX2[386]=(float)-0.036865;
        maxX2[387]=(float)-0.081238;
        maxX2[388]=(float)-0.12173;
        maxX2[389]=(float)-0.089783;
        maxX2[390]=(float)-0.096252;
        maxX2[391]=(float)-0.045776;
        maxX2[392]=(float)0.020966;
        maxX2[393]=(float)0.035126;
        maxX2[394]=(float)0.09903;
        maxX2[395]=(float)0.16562;
        maxX2[396]=(float)0.19711;
        maxX2[397]=(float)0.15338;
        maxX2[398]=(float)0.11282;
        maxX2[399]=(float)0.068024;
        maxX2[400]=(float)0.010712;
        maxX2[401]=(float)0.029785;
        maxX2[402]=(float)0.03418;
        maxX2[403]=(float)0.025116;
        maxX2[404]=(float)0.050568;
        maxX2[405]=(float)0.062653;
        maxX2[406]=(float)0.014954;
        maxX2[407]=(float)-0.088898;
        maxX2[408]=(float)-0.13348;
        maxX2[409]=(float)-0.15158;
        maxX2[410]=(float)-0.1456;
        maxX2[411]=(float)-0.10413;
        maxX2[412]=(float)-0.049469;
        maxX2[413]=(float)0.047791;
        maxX2[414]=(float)0.13519;
        maxX2[415]=(float)0.22147;
        maxX2[416]=(float)0.27103;
        maxX2[417]=(float)0.2486;
        maxX2[418]=(float)0.19232;
        maxX2[419]=(float)0.13016;
        maxX2[420]=(float)0.054688;
        maxX2[421]=(float)0.006958;
        maxX2[422]=(float)0.0072021;
        maxX2[423]=(float)0.0013733;
        maxX2[424]=(float)0.024719;
        maxX2[425]=(float)0.014587;
        maxX2[426]=(float)-0.040375;
        maxX2[427]=(float)-0.10767;
        maxX2[428]=(float)-0.22574;
        maxX2[429]=(float)-0.27307;
        maxX2[430]=(float)-0.26233;
        maxX2[431]=(float)-0.26456;
        maxX2[432]=(float)-0.17221;
        maxX2[433]=(float)-0.013885;
        maxX2[434]=(float)0.1597;
        maxX2[435]=(float)0.29898;
        maxX2[436]=(float)0.3187;
        maxX2[437]=(float)0.30569;
        maxX2[438]=(float)0.25839;
        maxX2[439]=(float)0.16949;
        maxX2[440]=(float)0.081085;
        maxX2[441]=(float)0.013489;
        maxX2[442]=(float)-0.035767;
        maxX2[443]=(float)-0.065674;
        maxX2[444]=(float)-0.079834;
        maxX2[445]=(float)-0.091553;
        maxX2[446]=(float)-0.15015;
        maxX2[447]=(float)-0.22601;
        maxX2[448]=(float)-0.24063;
        maxX2[449]=(float)-0.24124;
        maxX2[450]=(float)-0.17148;
        maxX2[451]=(float)-0.085297;
        maxX2[452]=(float)0.0092468;
        maxX2[453]=(float)0.13995;
        maxX2[454]=(float)0.24097;
        maxX2[455]=(float)0.28745;
        maxX2[456]=(float)0.2627;
        maxX2[457]=(float)0.22769;
        maxX2[458]=(float)0.2005;
        maxX2[459]=(float)0.16687;
        maxX2[460]=(float)0.1366;
        maxX2[461]=(float)0.090271;
        maxX2[462]=(float)0.058899;
        maxX2[463]=(float)0.028534;
        maxX2[464]=(float)0.01001;
        maxX2[465]=(float)-0.025543;
        maxX2[466]=(float)-0.075897;
        maxX2[467]=(float)-0.121;
        maxX2[468]=(float)-0.14679;
        maxX2[469]=(float)-0.17465;
        maxX2[470]=(float)-0.17892;
        maxX2[471]=(float)-0.14575;
        maxX2[472]=(float)-0.10709;
        maxX2[473]=(float)-0.0015564;
        maxX2[474]=(float)0.11414;
        maxX2[475]=(float)0.22659;
        maxX2[476]=(float)0.27551;
        maxX2[477]=(float)0.26886;
        maxX2[478]=(float)0.20596;
        maxX2[479]=(float)0.11523;
        maxX2[480]=(float)0.052856;
        maxX2[481]=(float)0.05011;
        maxX2[482]=(float)0.083344;
        maxX2[483]=(float)0.11349;
        maxX2[484]=(float)0.11884;
        maxX2[485]=(float)0.069611;
        maxX2[486]=(float)-0.0018311;
        maxX2[487]=(float)-0.10895;
        maxX2[488]=(float)-0.17484;
        maxX2[489]=(float)-0.18713;
        maxX2[490]=(float)-0.17767;
        maxX2[491]=(float)-0.1355;
        maxX2[492]=(float)-0.10159;
        maxX2[493]=(float)-0.017029;
        maxX2[494]=(float)0.09024;
        maxX2[495]=(float)0.15399;
        maxX2[496]=(float)0.21237;
        maxX2[497]=(float)0.21396;
        maxX2[498]=(float)0.16626;
        maxX2[499]=(float)0.14572;
        maxX2[500]=(float)0.081421;
        maxX2[501]=(float)0.010529;
        maxX2[502]=(float)0.0058899;
        maxX2[503]=(float)0.024689;
        maxX2[504]=(float)0.080658;
        maxX2[505]=(float)0.12259;
        maxX2[506]=(float)0.049103;
        maxX2[507]=(float)-0.057617;
        maxX2[508]=(float)-0.10706;
        maxX2[509]=(float)-0.14258;
        maxX2[510]=(float)-0.15253;
        maxX2[511]=(float)-0.10849;
        maxX2[512]=(float)-0.052673;
        maxX2[513]=(float)0.0067444;
        maxX2[514]=(float)0.12726;
        maxX2[515]=(float)0.19138;
        maxX2[516]=(float)0.20242;
        maxX2[517]=(float)0.21243;
        maxX2[518]=(float)0.16412;
        maxX2[519]=(float)0.093323;
        maxX2[520]=(float)0.037018;
        maxX2[521]=(float)0.051758;
        maxX2[522]=(float)0.092346;
        maxX2[523]=(float)0.037506;
        maxX2[524]=(float)-0.0021057;
        maxX2[525]=(float)0.01001;
        maxX2[526]=(float)0.088745;
        maxX2[527]=(float)0.10797;
        maxX2[528]=(float)0.018677;
        maxX2[529]=(float)-0.068939;
        maxX2[530]=(float)-0.14624;
        maxX2[531]=(float)-0.20236;
        maxX2[532]=(float)-0.19064;
        maxX2[533]=(float)-0.13461;
        maxX2[534]=(float)-0.065887;
        maxX2[535]=(float)0.019592;
        maxX2[536]=(float)0.14209;
        maxX2[537]=(float)0.23444;
        maxX2[538]=(float)0.26218;
        maxX2[539]=(float)0.25168;
        maxX2[540]=(float)0.18454;
        maxX2[541]=(float)0.09671;
        maxX2[542]=(float)-0.00039673;
        maxX2[543]=(float)-0.041565;
        maxX2[544]=(float)-0.008606;
        maxX2[545]=(float)0.029419;
        maxX2[546]=(float)0.041382;
        maxX2[547]=(float)-0.0075989;
        maxX2[548]=(float)-0.050568;
        maxX2[549]=(float)-0.089905;
        maxX2[550]=(float)-0.17929;
        maxX2[551]=(float)-0.22647;
        maxX2[552]=(float)-0.18253;
        maxX2[553]=(float)-0.099274;
        maxX2[554]=(float)-0.00021362;
        maxX2[555]=(float)0.1557;
        maxX2[556]=(float)0.29047;
        maxX2[557]=(float)0.32922;
        maxX2[558]=(float)0.32449;
        maxX2[559]=(float)0.29749;
        maxX2[560]=(float)0.20084;
        maxX2[561]=(float)0.092346;
        maxX2[562]=(float)0.042847;
        maxX2[563]=(float)0.027527;
        maxX2[564]=(float)0.0068359;
        maxX2[565]=(float)0.010864;
        maxX2[566]=(float)-0.0084839;
        maxX2[567]=(float)-0.085968;
        maxX2[568]=(float)-0.18167;
        maxX2[569]=(float)-0.24521;
        maxX2[570]=(float)-0.25107;
        maxX2[571]=(float)-0.21375;
        maxX2[572]=(float)-0.11276;
        maxX2[573]=(float)0.016205;
        maxX2[574]=(float)0.16983;
        maxX2[575]=(float)0.32043;
        maxX2[576]=(float)0.34955;
        maxX2[577]=(float)0.32935;
        maxX2[578]=(float)0.29355;
        maxX2[579]=(float)0.21671;
        maxX2[580]=(float)0.1366;
        maxX2[581]=(float)0.072632;
        maxX2[582]=(float)0.025726;
        maxX2[583]=(float)-0.012482;
        maxX2[584]=(float)-0.029297;
        maxX2[585]=(float)-0.06897;
        maxX2[586]=(float)-0.098785;
        maxX2[587]=(float)-0.12408;
        maxX2[588]=(float)-0.17673;
        maxX2[589]=(float)-0.16672;
        maxX2[590]=(float)-0.1088;
        maxX2[591]=(float)-0.048096;
        maxX2[592]=(float)0.050171;
        maxX2[593]=(float)0.14206;
        maxX2[594]=(float)0.22818;
        maxX2[595]=(float)0.30872;
        maxX2[596]=(float)0.32129;
        maxX2[597]=(float)0.25443;
        maxX2[598]=(float)0.16302;
        maxX2[599]=(float)0.09964;
        maxX2[600]=(float)0.076843;
        maxX2[601]=(float)0.059937;
        maxX2[602]=(float)0.075714;
        maxX2[603]=(float)0.11371;
        maxX2[604]=(float)0.057434;
        maxX2[605]=(float)-0.049591;
        maxX2[606]=(float)-0.10764;
        maxX2[607]=(float)-0.13309;
        maxX2[608]=(float)-0.10629;
        maxX2[609]=(float)-0.027649;
        maxX2[610]=(float)0.059265;
        maxX2[611]=(float)0.16577;
        maxX2[612]=(float)0.21814;
        maxX2[613]=(float)0.21454;
        maxX2[614]=(float)0.21625;
        maxX2[615]=(float)0.16409;
        maxX2[616]=(float)0.1239;
        maxX2[617]=(float)0.098785;
        maxX2[618]=(float)0.062347;
        maxX2[619]=(float)0.074554;
        maxX2[620]=(float)0.074188;
        maxX2[621]=(float)0.083069;
        maxX2[622]=(float)0.094482;
        maxX2[623]=(float)0.013672;
        maxX2[624]=(float)-0.068207;
        maxX2[625]=(float)-0.10513;
        maxX2[626]=(float)-0.18579;
        maxX2[627]=(float)-0.21457;
        maxX2[628]=(float)-0.17789;
        maxX2[629]=(float)-0.11874;
        maxX2[630]=(float)0.0056458;
        maxX2[631]=(float)0.13046;
        maxX2[632]=(float)0.2438;
        maxX2[633]=(float)0.33783;
        maxX2[634]=(float)0.34479;
        maxX2[635]=(float)0.28299;
        maxX2[636]=(float)0.21585;
        maxX2[637]=(float)0.13315;
        maxX2[638]=(float)0.041901;
        maxX2[639]=(float)-0.0027771;
        maxX2[640]=(float)-0.038849;
        maxX2[641]=(float)0.0028687;
        maxX2[642]=(float)-0.0041809;
        maxX2[643]=(float)-0.050201;
        maxX2[644]=(float)-0.074341;
        maxX2[645]=(float)-0.098053;
        maxX2[646]=(float)-0.12122;
        maxX2[647]=(float)-0.14206;
        maxX2[648]=(float)-0.15704;
        maxX2[649]=(float)-0.037811;
        maxX2[650]=(float)0.081116;
        maxX2[651]=(float)0.16537;
        maxX2[652]=(float)0.27338;
        maxX2[653]=(float)0.24319;
        maxX2[654]=(float)0.17511;
        maxX2[655]=(float)0.12524;
        maxX2[656]=(float)0.043457;
        maxX2[657]=(float)-0.022247;
        maxX2[658]=(float)-0.027466;
        maxX2[659]=(float)0.024017;
        maxX2[660]=(float)0.058289;
        maxX2[661]=(float)0.036499;
        maxX2[662]=(float)-0.038055;
        maxX2[663]=(float)-0.10434;
        maxX2[664]=(float)-0.14447;
        maxX2[665]=(float)-0.12622;
        maxX2[666]=(float)-0.1069;
        maxX2[667]=(float)-0.067047;
        maxX2[668]=(float)0.032166;
        maxX2[669]=(float)0.13226;
        maxX2[670]=(float)0.21625;
        maxX2[671]=(float)0.26904;
        maxX2[672]=(float)0.27155;
        maxX2[673]=(float)0.20346;
        maxX2[674]=(float)0.13867;
        maxX2[675]=(float)0.069794;
        maxX2[676]=(float)-0.0027466;
        maxX2[677]=(float)-0.027222;
        maxX2[678]=(float)-0.022583;
        maxX2[679]=(float)-0.02594;
        maxX2[680]=(float)-0.024994;
        maxX2[681]=(float)-0.095612;
        maxX2[682]=(float)-0.15903;
        maxX2[683]=(float)-0.22241;
        maxX2[684]=(float)-0.28033;
        maxX2[685]=(float)-0.26465;
        maxX2[686]=(float)-0.20789;
        maxX2[687]=(float)-0.08551;
        maxX2[688]=(float)0.075287;
        maxX2[689]=(float)0.203;
        maxX2[690]=(float)0.29404;
        maxX2[691]=(float)0.29135;
        maxX2[692]=(float)0.2695;
        maxX2[693]=(float)0.20752;
        maxX2[694]=(float)0.098755;
        maxX2[695]=(float)0.026764;
        maxX2[696]=(float)-0.0020752;
        maxX2[697]=(float)-0.01123;
        maxX2[698]=(float)-0.020111;
        maxX2[699]=(float)-0.023132;
        maxX2[700]=(float)-0.000091553;
        maxX2[701]=(float)-0.015564;
        maxX2[702]=(float)-0.11435;
        maxX2[703]=(float)-0.17487;
        maxX2[704]=(float)-0.21948;
        maxX2[705]=(float)-0.26428;
        maxX2[706]=(float)-0.24579;
        maxX2[707]=(float)-0.15878;
        maxX2[708]=(float)-0.071228;
        maxX2[709]=(float)0.10553;
        maxX2[710]=(float)0.27704;
        maxX2[711]=(float)0.35892;
        maxX2[712]=(float)0.38065;
        maxX2[713]=(float)0.31512;
        maxX2[714]=(float)0.24213;
        maxX2[715]=(float)0.1492;
        maxX2[716]=(float)0.033234;
        maxX2[717]=(float)-0.042023;
        maxX2[718]=(float)-0.043121;
        maxX2[719]=(float)0.0012512;
        maxX2[720]=(float)-0.0032654;
        maxX2[721]=(float)-0.040985;
        maxX2[722]=(float)-0.089905;
        maxX2[723]=(float)-0.15164;
        maxX2[724]=(float)-0.19678;
        maxX2[725]=(float)-0.23065;
        maxX2[726]=(float)-0.2247;
        maxX2[727]=(float)-0.15909;
        maxX2[728]=(float)-0.040863;
        maxX2[729]=(float)0.076935;
        maxX2[730]=(float)0.21179;
        maxX2[731]=(float)0.28522;
        maxX2[732]=(float)0.28378;
        maxX2[733]=(float)0.22592;
        maxX2[734]=(float)0.1626;
        maxX2[735]=(float)0.069763;
        maxX2[736]=(float)0.034363;
        maxX2[737]=(float)0.033478;
        maxX2[738]=(float)-0.02301;
        maxX2[739]=(float)-0.00177;
        maxX2[740]=(float)-0.00079346;
        maxX2[741]=(float)-0.041321;
        maxX2[742]=(float)-0.082977;
        maxX2[743]=(float)-0.14108;
        maxX2[744]=(float)-0.20331;
        maxX2[745]=(float)-0.26584;
        maxX2[746]=(float)-0.24408;
        maxX2[747]=(float)-0.18219;
        maxX2[748]=(float)-0.079224;
        maxX2[749]=(float)0.049927;
        maxX2[750]=(float)0.19342;
        maxX2[751]=(float)0.30142;
        maxX2[752]=(float)0.31696;
        maxX2[753]=(float)0.28708;
        maxX2[754]=(float)0.24509;
        maxX2[755]=(float)0.14914;
        maxX2[756]=(float)0.024414;
        maxX2[757]=(float)-0.048187;
        maxX2[758]=(float)-0.10425;
        maxX2[759]=(float)-0.16321;
        maxX2[760]=(float)-0.17563;
        maxX2[761]=(float)-0.1908;
        maxX2[762]=(float)-0.19919;
        maxX2[763]=(float)-0.18192;
        maxX2[764]=(float)-0.19312;
        maxX2[765]=(float)-0.16376;
        maxX2[766]=(float)-0.09201;
        maxX2[767]=(float)-0.026794;
        maxX2[768]=(float)0.055084;
        maxX2[769]=(float)0.12238;
        maxX2[770]=(float)0.15323;
        maxX2[771]=(float)0.10828;
        maxX2[772]=(float)0.046692;
        maxX2[773]=(float)0.013062;
        maxX2[774]=(float)-0.014404;
        maxX2[775]=(float)-0.0046082;
        maxX2[776]=(float)0.034973;
        maxX2[777]=(float)0.064941;
        maxX2[778]=(float)0.059448;
        maxX2[779]=(float)-0.023285;
        maxX2[780]=(float)-0.044312;
        maxX2[781]=(float)-0.062683;
        maxX2[782]=(float)-0.047455;
        maxX2[783]=(float)-0.020813;
        maxX2[784]=(float)-0.042877;
        maxX2[785]=(float)-0.056335;
        maxX2[786]=(float)-0.10556;
        maxX2[787]=(float)-0.17279;
        maxX2[788]=(float)-0.21643;
        maxX2[789]=(float)-0.23151;
        maxX2[790]=(float)-0.19913;
        maxX2[791]=(float)-0.1145;
        maxX2[792]=(float)0.019562;
        maxX2[793]=(float)0.18411;
        maxX2[794]=(float)0.26776;
        maxX2[795]=(float)0.24615;
        maxX2[796]=(float)0.21326;
        maxX2[797]=(float)0.14896;
        maxX2[798]=(float)0.085266;
        maxX2[799]=(float)0.022827;
        maxX2[800]=(float)-0.053894;
        maxX2[801]=(float)-0.11276;
        maxX2[802]=(float)-0.1358;
        maxX2[803]=(float)-0.14276;
        maxX2[804]=(float)-0.16641;
        maxX2[805]=(float)-0.20178;
        maxX2[806]=(float)-0.19553;
        maxX2[807]=(float)-0.19788;
        maxX2[808]=(float)-0.14703;
        maxX2[809]=(float)-0.034821;
        maxX2[810]=(float)0.055756;
        maxX2[811]=(float)0.14526;
        maxX2[812]=(float)0.2168;
        maxX2[813]=(float)0.19901;
        maxX2[814]=(float)0.13675;
        maxX2[815]=(float)0.073822;
        maxX2[816]=(float)0.011841;
        maxX2[817]=(float)0.0057983;
        maxX2[818]=(float)-0.0159;
        maxX2[819]=(float)0.0026855;
        maxX2[820]=(float)0.045471;
        maxX2[821]=(float)0.00039673;
        maxX2[822]=(float)-0.08844;
        maxX2[823]=(float)-0.16498;
        maxX2[824]=(float)-0.17517;
        maxX2[825]=(float)-0.13327;
        maxX2[826]=(float)-0.063629;
        maxX2[827]=(float)0.0029602;
        maxX2[828]=(float)0.078156;
        maxX2[829]=(float)0.13995;
        maxX2[830]=(float)0.15298;
        maxX2[831]=(float)0.13824;
        maxX2[832]=(float)0.12067;
        maxX2[833]=(float)0.096375;
        maxX2[834]=(float)0.016296;
        maxX2[835]=(float)0.011505;
        maxX2[836]=(float)0.0224;
        maxX2[837]=(float)0.072235;
        maxX2[838]=(float)0.12616;
        maxX2[839]=(float)0.074158;
        maxX2[840]=(float)-0.022034;
        maxX2[841]=(float)-0.10025;
        maxX2[842]=(float)-0.14346;
        maxX2[843]=(float)-0.1427;
        maxX2[844]=(float)-0.14117;
        maxX2[845]=(float)-0.1265;
        maxX2[846]=(float)-0.043701;
        maxX2[847]=(float)0.06601;
        maxX2[848]=(float)0.16565;
        maxX2[849]=(float)0.25705;
        maxX2[850]=(float)0.30502;
        maxX2[851]=(float)0.25034;
        maxX2[852]=(float)0.19229;
        maxX2[853]=(float)0.12573;
        maxX2[854]=(float)0.027679;
        maxX2[855]=(float)-0.022247;
        maxX2[856]=(float)0.0062256;
        maxX2[857]=(float)0.03183;
        maxX2[858]=(float)-0.0094299;
        maxX2[859]=(float)-0.057983;
        maxX2[860]=(float)-0.1265;
        maxX2[861]=(float)-0.19135;
        maxX2[862]=(float)-0.19943;
        maxX2[863]=(float)-0.18088;
        maxX2[864]=(float)-0.12537;
        maxX2[865]=(float)-0.048676;
        maxX2[866]=(float)0.037811;
        maxX2[867]=(float)0.13516;
        maxX2[868]=(float)0.17453;
        maxX2[869]=(float)0.20248;
        maxX2[870]=(float)0.18967;
        maxX2[871]=(float)0.12448;
        maxX2[872]=(float)0.05188;
        maxX2[873]=(float)-0.0080872;
        maxX2[874]=(float)-0.028412;
        maxX2[875]=(float)-0.012756;
        maxX2[876]=(float)0.02066;
        maxX2[877]=(float)-0.0032654;
        maxX2[878]=(float)-0.071869;
        maxX2[879]=(float)-0.14114;
        maxX2[880]=(float)-0.19827;
        maxX2[881]=(float)-0.22665;
        maxX2[882]=(float)-0.15924;
        maxX2[883]=(float)-0.08429;
        maxX2[884]=(float)-0.069336;
        maxX2[885]=(float)0.038879;
        maxX2[886]=(float)0.11786;
        maxX2[887]=(float)0.15399;
        maxX2[888]=(float)0.18747;
        maxX2[889]=(float)0.16278;
        maxX2[890]=(float)0.14325;
        maxX2[891]=(float)0.10474;
        maxX2[892]=(float)0.047668;
        maxX2[893]=(float)0.03595;
        maxX2[894]=(float)0.072021;
        maxX2[895]=(float)0.070801;
        maxX2[896]=(float)-0.012085;
        maxX2[897]=(float)-0.033844;
        maxX2[898]=(float)-0.037598;
        maxX2[899]=(float)-0.028717;
        maxX2[900]=(float)-0.026611;
        maxX2[901]=(float)-0.087006;
        maxX2[902]=(float)-0.14215;
        maxX2[903]=(float)-0.2197;
        maxX2[904]=(float)-0.23486;
        maxX2[905]=(float)-0.20395;
        maxX2[906]=(float)-0.12158;
        maxX2[907]=(float)-0.011322;
        maxX2[908]=(float)0.090759;
        maxX2[909]=(float)0.16132;
        maxX2[910]=(float)0.22504;
        maxX2[911]=(float)0.20859;
        maxX2[912]=(float)0.13843;
        maxX2[913]=(float)0.069733;
        maxX2[914]=(float)-0.00018311;
        maxX2[915]=(float)-0.016357;
        maxX2[916]=(float)-0.0092163;
        maxX2[917]=(float)-0.0050964;
        maxX2[918]=(float)-0.013977;
        maxX2[919]=(float)-0.020721;
        maxX2[920]=(float)-0.045929;
        maxX2[921]=(float)-0.13553;
        maxX2[922]=(float)-0.18124;
        maxX2[923]=(float)-0.22986;
        maxX2[924]=(float)-0.2088;
        maxX2[925]=(float)-0.16641;
        maxX2[926]=(float)-0.057831;
        maxX2[927]=(float)0.094757;
        maxX2[928]=(float)0.20444;
        maxX2[929]=(float)0.28268;
        maxX2[930]=(float)0.28394;
        maxX2[931]=(float)0.27347;
        maxX2[932]=(float)0.22818;
        maxX2[933]=(float)0.096252;
        maxX2[934]=(float)-0.014252;
        maxX2[935]=(float)-0.090454;
        maxX2[936]=(float)-0.10599;
        maxX2[937]=(float)-0.11453;
        maxX2[938]=(float)-0.097321;
        maxX2[939]=(float)-0.10168;
        maxX2[940]=(float)-0.15674;
        maxX2[941]=(float)-0.20496;
        maxX2[942]=(float)-0.18762;
        maxX2[943]=(float)-0.14493;
        maxX2[944]=(float)-0.11145;
        maxX2[945]=(float)-0.036621;
        maxX2[946]=(float)0.039337;
        maxX2[947]=(float)0.089722;
        maxX2[948]=(float)0.12198;
        maxX2[949]=(float)0.12744;
        maxX2[950]=(float)0.089905;
        maxX2[951]=(float)0.058594;
        maxX2[952]=(float)0.037018;
        maxX2[953]=(float)-0.02063;
        maxX2[954]=(float)-0.054199;
        maxX2[955]=(float)-0.016937;
        maxX2[956]=(float)0.037476;
        maxX2[957]=(float)0.027283;
        maxX2[958]=(float)-0.044952;
        maxX2[959]=(float)-0.07016;
        maxX2[960]=(float)-0.1116;
        maxX2[961]=(float)-0.12592;
        maxX2[962]=(float)-0.094604;
        maxX2[963]=(float)-0.083618;
        maxX2[964]=(float)-0.048706;
        maxX2[965]=(float)-0.016052;
        maxX2[966]=(float)0.038483;
        maxX2[967]=(float)0.071594;
        maxX2[968]=(float)0.085602;
        maxX2[969]=(float)0.083221;
        maxX2[970]=(float)0.024292;
        maxX2[971]=(float)-0.033539;
        maxX2[972]=(float)-0.088409;
        maxX2[973]=(float)-0.095734;
        maxX2[974]=(float)-0.044922;
        maxX2[975]=(float)-0.026886;
        maxX2[976]=(float)-0.023468;
        maxX2[977]=(float)0.012604;
        maxX2[978]=(float)-0.011993;
        maxX2[979]=(float)-0.069397;
        maxX2[980]=(float)-0.084473;
        maxX2[981]=(float)-0.085144;
        maxX2[982]=(float)-0.088135;
        maxX2[983]=(float)-0.032196;
        maxX2[984]=(float)0.022766;
        maxX2[985]=(float)0.082672;
        maxX2[986]=(float)0.12714;
        maxX2[987]=(float)0.16507;
        maxX2[988]=(float)0.16586;
        maxX2[989]=(float)0.11261;
        maxX2[990]=(float)0.070435;
        maxX2[991]=(float)0.026947;
        maxX2[992]=(float)-0.0024719;
        maxX2[993]=(float)-0.016541;
        maxX2[994]=(float)-0.0055542;
        maxX2[995]=(float)-0.038086;
        maxX2[996]=(float)-0.026886;
        maxX2[997]=(float)-0.035431;
        maxX2[998]=(float)-0.12347;
        maxX2[999]=(float)-0.14148;
        maxX2[1000]=(float)-0.1333;
        maxX2[1001]=(float)-0.12848;
        maxX2[1002]=(float)-0.04007;
        maxX2[1003]=(float)0.03479;
        maxX2[1004]=(float)0.083038;
        maxX2[1005]=(float)0.15894;
        maxX2[1006]=(float)0.18204;
        maxX2[1007]=(float)0.14923;
        maxX2[1008]=(float)0.11584;
        maxX2[1009]=(float)0.069641;
        maxX2[1010]=(float)0.025024;
        maxX2[1011]=(float)0.037354;
        maxX2[1012]=(float)0.01825;
        maxX2[1013]=(float)0.0070801;
        maxX2[1014]=(float)0.04187;
        maxX2[1015]=(float)0.11145;
        maxX2[1016]=(float)0.11841;
        maxX2[1017]=(float)-0.027283;
        maxX2[1018]=(float)-0.14624;
        maxX2[1019]=(float)-0.18097;
        maxX2[1020]=(float)-0.20761;
        maxX2[1021]=(float)-0.23212;
        maxX2[1022]=(float)-0.21344;
        maxX2[1023]=(float)-0.12192;

    }


}

