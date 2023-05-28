/*

	更新所量到的數據，顯示於手機第三個畫面
	
	主要動作為連到資料庫將所要的資料撈出來並顯示

*/

package com.example.luolab.measureppg;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.usb.driver.uart.ReadLisener;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.Inflater;


public class ShowData extends Fragment /*implements Update*/{

    private View ShowDataView;

    public static int tTime = 1;
    public static double[] HRData;
    public static double[] BRData;
    public static double[] SDNNData;
    public static double[] HFData;
    //public static double[] LF_HFData;

    private View dialogView;
    private AlertDialog Dialog;
    private AlertDialog.Builder Dialog_Builder;

    private View dialogView2;
    private TextView feedCountTv;
    private TextView maxSDNNTv;
    private TextView minBRTv;
    private TextView maxHFTv;
    private TextView ParaTv;
    private TextView debug_tv;
    private AlertDialog Dialog2;
    private AlertDialog.Builder Dialog_Builder2;

    private LayoutInflater LInflater;


    public static int feedbackCounter = 0;
    public static double maxSDNN = 0;
    public static double minBR = 0;
    public static double maxHF = 0;

    private int dataNum;
    private int graphUpper;
    private int graphLower;
    private Handler mHandler;

    private Handler recordsHandler;



    @SuppressLint("SetTextI18n")
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        recordsHandler = new Handler();
        ShowDataView = inflater.inflate(R.layout.showdata, container, false);
        ParaTv = ShowDataView.findViewById(R.id.textView4);
        feedCountTv = ShowDataView.findViewById(R.id.feedCount);
        feedCountTv.setText(Integer.toString(feedbackCounter));

        maxSDNNTv = ShowDataView.findViewById(R.id.SDNN_max);
        if(SDNNData != null){
            maxSDNNTv.setText(Double.toString(maxSDNN));
        }else{
            maxSDNNTv.setText("null");
        }
        minBRTv = ShowDataView.findViewById(R.id.br_max);
        if(BRData != null){
            minBRTv.setText(Double.toString(minBR));
        }else{
            minBRTv.setText("null");
        }
        maxHFTv = ShowDataView.findViewById(R.id.HF_max);
        if(HFData != null){
            maxHFTv.setText(Double.toString(maxHF));
        }else{
            maxHFTv.setText("null");
        }


       // minBRTv.setText(Double.toString(minBR));

       // maxHFTv.setText(Double.toString(maxHF));

        LInflater = inflater;

        //debug_tv = ShowDataView.findViewById(R.id.debug_tv);

        Button updateGraph_btn = ShowDataView.findViewById(R.id.update_graph_btn);
        GraphView graphHeartRate = ShowDataView.findViewById(R.id.paraGraph);
        GraphView graphPara = ShowDataView.findViewById(R.id.paraGraph3);

        if(HRData != null){
            updateGraph_btn.setEnabled(true);
        }else{
            updateGraph_btn.setEnabled(true); //false
        }

        updateGraph_btn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){

                final String[] parameter = {"SDNN","BreathRate", "HF"};
                final String[] parameterUnit = {"ms","BPM","ms^2"};
                AlertDialog.Builder dialog_list = new AlertDialog.Builder(LInflater.getContext());
                dialog_list.setTitle("請選擇生物回饋參數");
                dialog_list.setItems(parameter, new DialogInterface.OnClickListener(){
                    @Override

                    //只要你在onClick處理事件內，使用which參數，就可以知道按下陣列裡的哪一個了
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        Toast.makeText(LInflater.getContext(), "您已選擇" + parameter[which], Toast.LENGTH_SHORT).show();
                        switch (parameter[which]){
                            case "SDNN":
                                Log.d("TAG", "switch sdnn" );
                                if(SDNNData != null){
                                    Log.d("TAG", "sdnn append data" );
                                    graphPara.removeAllSeries();
                                    LineGraphSeries<DataPoint> seriesPara= new LineGraphSeries<>(data(SDNNData));
                                    graphPara.addSeries(seriesPara);
                                    graphPara.getViewport().setMaxX(tTime*60-170);
                                    graphPara.getViewport().setXAxisBoundsManual(true);
                                    graphPara.getViewport().setMaxY(maxSDNN+20);
                                    graphPara.getViewport().setMinY(20);
                                    graphPara.getViewport().setYAxisBoundsManual(true);
                                }
                                ParaTv.setText("SDNN");
                                break;

                            case "HF":
                                if(HFData != null){
                                    graphPara.removeAllSeries();
                                    LineGraphSeries<DataPoint> seriesPara= new LineGraphSeries<>(data(HFData));
                                    graphPara.addSeries(seriesPara);
                                    graphPara.getViewport().setMaxX(tTime*60-170);
                                    graphPara.getViewport().setXAxisBoundsManual(true);
                                    graphPara.getViewport().setMaxY(maxHF+200);
                                    graphPara.getViewport().setMinY(0);
                                    graphPara.getViewport().setYAxisBoundsManual(true);

                                }
                                ParaTv.setText("HF");
                                break;
                            /*
                            case "LF/HF":
                                if(LF_HFData != null){
                                    graphPara.removeAllSeries();
                                    LineGraphSeries<DataPoint> seriesPara= new LineGraphSeries<>(data(LF_HFData));
                                    graphPara.addSeries(seriesPara);
                                }
                                ParaTv.setText("LF/HF");
                                break;

                             */


                            case "BreathRate":
                                if(BRData != null){
                                    graphPara.removeAllSeries();
                                    LineGraphSeries<DataPoint> seriesPara= new LineGraphSeries<>(data(BRData));
                                    graphPara.addSeries(seriesPara);
                                    graphPara.getViewport().setMaxX(tTime*60-170);
                                    graphPara.getViewport().setXAxisBoundsManual(true);

                                    graphPara.getViewport().setMaxY(25);
                                    graphPara.getViewport().setMinY(0);
                                    graphPara.getViewport().setYAxisBoundsManual(true);
                                }
                                ParaTv.setText("BreathRate");
                                break;

                        }
                        if(HRData != null){
                            LineGraphSeries<DataPoint> seriesHR= new LineGraphSeries<>(data(HRData));
                            graphHeartRate.addSeries(seriesHR);
                            graphHeartRate.getViewport().setMaxY(120);
                            graphHeartRate.getViewport().setMaxX(tTime*60-170);
                            graphHeartRate.getViewport().setXAxisBoundsManual(true);
                        }

                    }
                });
                dialog_list.show();

            }
        });
        return ShowDataView;
    };


    public DataPoint[] data(double[] arr){
        DataPoint[] values = new DataPoint[arr.length];     //creating an object of type DataPoint[] of size 'n'
        for(int i=0;i<arr.length;i++){
            DataPoint v = new DataPoint(i,arr[i]);
            values[i] = v;
        }
        return values;
    };





}
