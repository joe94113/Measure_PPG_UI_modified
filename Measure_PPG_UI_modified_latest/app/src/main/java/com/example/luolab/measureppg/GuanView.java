/*
	
	此類別為量測 PPG 畫面，主要包含連接手機 serial port 以及接收 PPG 模組的資料

*/

package com.example.luolab.measureppg;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.Arrays;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.github.psambit9791.jdsp.signal.Detrend;
import com.github.psambit9791.jdsp.signal.Smooth;
import com.github.psambit9791.jdsp.signal.peaks.FindPeak;
import com.github.psambit9791.jdsp.signal.peaks.Peak;
import com.github.psambit9791.jdsp.windows.Hamming;
import com.google.firebase.database.DatabaseException;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.complex.Complex;
//import org.apache.pdfbox.contentstream.operator.text.ShowTextLineAndSpace;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.Stack;
import java.util.zip.Inflater;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import uk.me.berndporr.iirj.Butterworth;
import com.github.psambit9791.jdsp.windows.Hanning;
import com.github.psambit9791.jdsp.transform.FastFourier;



import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED;
import static java.lang.Double.NaN;

public class GuanView extends Fragment {
    private ArrayAdapter<String> deviceName;
    private ArrayAdapter<String> deviceId;
    private List<BleDevice> bleDeviceList = new ArrayList<>();
    private Button button_paired;
    private ListView event_listView;
    private BleDevice nowBleDevice;
    private final int SerialDataSize = 90060;
    private View GuanView;
    private View menu_dialogView;
    private View menu_dialogView2;

    private Spinner sp;

    private DoubleTwoDimQueue dataQ;
    private DoubleTwoDimQueue dataQ2;
    private DoubleTwoDimQueue HRDataSeries;
    private DoubleTwoDimQueue SDNNDataSeries;
    private DoubleTwoDimQueue HFDataSeries;
    //private DoubleTwoDimQueue LF_HFDataSeries;

    private DoubleTwoDimQueue BRDataSeries;
    private DoubleTwoDimQueue probeDataSeries;
    private DoubleTwoDimQueue probeaDataSeries;
    private DoubleTwoDimQueue probebDataSeries;
    private DoubleTwoDimQueue probecDataSeries;
    private DoubleTwoDimQueue probedDataSeries;
    private DoubleTwoDimQueue probeeDataSeries;
    private DoubleTwoDimQueue probefDataSeries;
    private DoubleTwoDimQueue probegDataSeries;

    //Global config for time-domain frequecy-domain analysis
    final double ppg_fs = 25;
    final double normal_breathing_time = 180;
    final double normal_breathing_size = (normal_breathing_time*ppg_fs);

    //Config for time-domain analysis
    public static double window_time = 170;
    private int FFT_window_size = 256;
    private int numOfsubwin = 200;
    private double upsample_factor = 4;
    final double sliding_window_size = (window_time*ppg_fs);

    private int voice_ctr;

    private double lastData;

    private boolean ascend = false;
    private boolean G2_flag = true;
    private boolean untrusted = false;
    private boolean start_flag = false;
    private boolean delay_flag = false;
    private double a_low_slow = 0.3;
    private double a_high_slow = 2.7;
    private double a_low_fast = 0.5;
    private double a_high_fast = 4.5;
    private double THlow = 35;
    private double THhigh = 700;


    int window_size = (int)(window_time*ppg_fs);// 194s*25Hz

    private double[] intervalMs;
    private double[] intervalXMs;
    private double[] arr_X_new;
    private double[] interpolateResult;
    private double[] ratio_arr = new double[3];
    private double[] SDNN_normal = new double[(int)window_time];
    private double[] RIAV_Result_arr;
    double[] SDNNtotalvals = new double[(int)window_time*10];
    int SDNNtotalvals_ctr = 0;
    static public String usrName = "未命名";
    static public String usrAge = "20";
    public static String selectedPara = "Breathing rate";
    private String selectedParaUnit = "BPM";
    private String windowed_ppg_str;
    private String raw_data0_str;
    private String raw_data1_str;

    private int SDNN_normal_ctr = 0;
    private int ringIdx = 0;
    private int ratioCtr = 0;
    private int upperFreqIdx = 24;
    private int lowerFreqIdx = 3;
    private int startPointer;
    private int endPointer;
    private int recievedDataPoint;
    private int dataPointCount;
    private int windowStartIdx = 0;

    private int windowEndIdx = (int)sliding_window_size;


    private short fillWindowPhase;
    final private int sampleRate = 25;
    final private int updateEveryXPoint = 25;
    final private int windowSize = 1024; //因為在資料點數達1024前是每次加256算一次，因此如需改windowSize，則handleInputData()必須修正


    final private int windowSizeBR = 4850; //194s*25Hz
    private double input_init_val;
    private double init_val = 6;
    private double target_val = 3;
    private double feedback_time = 0;
    private double inhale_time = 0;
    private double exhale_time = 0;
    private double RIFV_std = 0;
    private double normal_sdnn_upper = 80;

    private boolean arti_detected;
    private boolean sig_small;
    private boolean sig_big;
    private boolean error_flag;

    private double trigger_val;
    public double BPM;
    public double SDNN = 0;
    public double br_SDNN = 0;
    public double LF = 0;
    public double HF = 0;
    //public double LF_HF = 0;
    public double breathRate = 0;
    public double probe = 0;
    public double probea = 0;
    public double probeb = 0;
    public double probec = 0;
    public double probed = 0;
    public double probee = 0;
    public double probef = 0;
    public double probeg = 0;
    double ptr = 0;

    private int data;
    private int data2;

    private double RIFV_normal_slow_thres = 0;
    private double RIFV_normal_slow_thres_HF_LF = 0;


    private double RIFV_SDNN_thres = 0;
    private double RIFV_power_thres = 0;
    private double RIAV_power_thres = 0;
    private double RIFV_normal_slow_power_thres = 0;
    private double RIIV_normal_slow_thres = 0;
    private double RIAV_normal_slow_thres = 0;

    private int val_ctr = 0;
    private double[] last_five_BR = new double[5];
    private double sum_five_normal_amp = 0;
    private int first_five_normal_amp_ctr = 0;

    public String first_peaks_locs = "";
    public String first_peaks_locs2 = "";

    private double post_start_mpoint;
    private double finish_mpoint;


    private double para;
    private double tempMax=0;
    private double feedbackBase;
    private String debugging_probe1;
    private String debugging_probe2;
    private String debugging_probe3;
    private String debugging_probe4;
    private String debugging_probe5;
    private String debugging_probe6;
    private String debugging_probe7;

    private String artilist0_str;
    private String artilist1_str;
    private String artilist2_str;

    private String rrlist_str;
    private String RIIV_sliding_window_str;
    private String RIAV_sliding_window_str;
    private String RIFV_sliding_window_str;
    private String SDNN_thres_str;
    private String SDNN_remeasure_str;
    private String fft_window_SDNN_str;
    private String RIFV_power_thres_str;
    private String RIFV_power_each_window_thres_str;
    private String RIAV_power_thres_str;
    private String RIAV_power_each_window_thres_str;

    private String fft_window_ROI_lower_Idx_str;

    private String fft_window_RIIV_bpm_str;
    private String fft_window_RIAV_bpm_str;
    private String fft_window_RIFV_bpm_str;
    private String skip_window_str;

    private Stack<Long> timestampQ;

    private boolean disable_RIAV = false;
    private boolean disable_RIIV = false;

    private boolean contain_arti = false;

    private boolean slidingWindowIsFull;
    private boolean start_cal;

    private boolean keep_thread_running;
    private boolean sliding_window_enable;
    private boolean abortTraining;
    private boolean first_window = true;

    private boolean serialopen;

    private Handler fileHandler;
    private Handler mHandler;

    private GraphView G_Graph;
    private GraphView G_Graph2;

    private LineGraphSeries<DataPoint> G_Series;
    private LineGraphSeries<DataPoint> G_Series2;

    private int[] TempSize = new int[2];
    private int SizeIndex = 0;
    private String[] timeStamps = new String[1800];
    private int timeStampsCtr = 0;
    private double[] br_SDNN_arr = new double[1800];
    private int brSDNNCtr = 0;
    private byte[] SerialData_Queue = new byte[SerialDataSize];
    private int Queue_Index_Rear = 0;
    private int Queue_Index_Front = 0;


    private int Queue_Para_Index_Rear = 0;
    private int Queue_Para_Index_Front = 0;
    private int dataPointCtr = 0;
    private int mXPoint;
    private double mXPointPara = 170;
    public static int PPGTime = 1;
    public static int postRecordPPGTime;
    private int Scale = 150;
    private int Time_GET = 0;
    private int Min_Time_GET = 0;
    private int Min_Time_Flag = 0;
    private int arrayLen = 0;
    private int queueSize;

    final private double start_feedback_time = 300; //300s

    final double new_fs = ppg_fs*upsample_factor;

    private int RIIV_Arr_normal_ctr = 0;
    private double[] RIIV_Arr_normal = new double[180];

    private int RIAV_Arr_normal_ctr = 0;
    private double[] RIAV_Arr_normal = new double[180];

    private int RIFV_Arr_normal_ctr = 0;
    private double[] RIFV_Arr_normal = new double[180];

    private int RIFV_normal_std_ctr = 0;
    private double[] RIFV_normal_std_arr = new double[180];

    private int RIFV_normal_std_HF_LF_ctr = 0;
    private double[] RIFV_normal_std_HF_LF_arr = new double[180];

    private Thread findIntervalThread;

    private Calendar c;
    private SimpleDateFormat dateformat;

    private AlertDialog.Builder MenuDialog_Builder;
    private AlertDialog MenuDialog;

    private AlertDialog.Builder MenuDialog_Builder2;
    private AlertDialog MenuDialog2;

    private LayoutInflater LInflater;

    public static boolean SerialFlag = false;
    private boolean Stop_Flag = false;
    private boolean Preview_Flag = false;
    private boolean init_error = false;
    private boolean ticking_enable = false;

    private TextView ble_status;
    private TextView time_tv;
    private TextView console_tv;
    private TextView console_tv2;
    private TextView textView6_tv;
    private TextView textView12_tv;
    private TextView Minute_tv;
    private TextView ParaName_tv;
    private TextView ParaVal_tv;
    private TextView ParaUnit_tv;
    private TextView imgProcessed;
    private TextView tvPara2;
    private TextView tvAccName2;
    private TextView textView2_tv;
    private TextView textView18_tv;
    private TextView textView21_tv;
    //private TextView consoles_tv2;

    private double[][] last_five_val_HF = new double[10][5];
    private int last_five_val_HF_ctr = 0;
    private double[][] last_five_val_LF = new double[10][5];
    private int last_five_val_LF_ctr = 0;

    private short inhale_ctr = 0;
    private short exhale_ctr = 0;
    private RadioGroup radioGroup;
    private android.widget.RadioButton radioButton;

    public static Button start_btn;
    public static Button ticking_btn;
    private Button menu_btn;
    private Button testSound_btn;
    private Button Abort_btn;
    private Button buttonApply;

    private UiDataBundle appData;
    private UiDataBundle appData2;

    private MediaPlayer mp1;
    private MediaPlayer mp2;
    private MediaPlayer mp3;
    private MediaPlayer mp4;
    private MediaPlayer mp5;
    private MediaPlayer mp6;
    private MediaPlayer mp7;
    private MediaPlayer mp8;
    private MediaPlayer mp9;
    private MediaPlayer mp10;
    private MediaPlayer mp11;
    private MediaPlayer mp12;

    private MediaPlayer mpTick;
    private MediaPlayer mpInhale;
    private MediaPlayer mpExhale;

    private MediaPlayer mpRemeasure;

    private MediaPlayer mpGreeting;
    private MediaPlayer mpIntro;
    private MediaPlayer mpIntro2;
    private MediaPlayer mpInstruction;
    private MediaPlayer mpVoice1_1;
    private MediaPlayer mpVoice1_2;
    private MediaPlayer mpVoice2;
    private MediaPlayer mpVoice3;
    private MediaPlayer mpFinish;
    private MediaPlayer mpVoice2_control;

    private MediaPlayer mpSigBig;
    private MediaPlayer mpSigSmall;
    private MediaPlayer mpSigArti;

    private SimpleDateFormat sdf2;
    private Date current2;

    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";


    private static final Random RANDOM = new Random();
    private int lastX = 0;

    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference rawDataRef;

    private Handler mHandler2 = new Handler();

    // Serial port 有資料傳進來手機時就會被觸發此事件，可進行讀取資料
    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            handleData(arg0,arg0.length,LInflater);
        }
    };

//     進行 Serial port 設定
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
//        Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("onReceive", "onReceive");
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            SerialFlag = true;
                            setButtonEnable(SerialFlag);
                            Toast.makeText(LInflater.getContext(), "偵測到序列埠" , Toast.LENGTH_SHORT).show();

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart();
            }
            if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                Toast.makeText(LInflater.getContext(), "序列埠拔除" , Toast.LENGTH_SHORT).show();
                SerialFlag = false;
                setButtonEnable(SerialFlag);
            }

        };


    };

    // 設定 button Enable 以及 Disable
    private void setButtonEnable(boolean state){
        if(state){
            start_btn.setEnabled(true);
            Abort_btn.setEnabled(false);
            testSound_btn.setEnabled(true);
            menu_btn.setEnabled(true);
            buttonApply.setEnabled(true);

        }else{
            start_btn.setEnabled(false);
            buttonApply.setEnabled(false);
            if(SerialFlag){
                Abort_btn.setEnabled(true);
            }else{
                Abort_btn.setEnabled(false);
            }

            testSound_btn.setEnabled(false);
            menu_btn.setEnabled(false);
        }
    }

    // 與 Arduino beetle 進行連線
    private void onClickStart(){
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x2341)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(LInflater.getContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }
    }

    // 更新 UI 畫面
    private void UpdateUI() {
        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message inputMessage){
                UiDataBundle incoming = (UiDataBundle) inputMessage.obj;
                /*
                if(BPM > 0) {
                    if(recievedDataPoint < 1024){
                        imgProcessed.setTextColor(Color.rgb(100,100,200));
                    }
                    else{
                        imgProcessed.setTextColor(Color.rgb(100,200,100));
                    }
                    imgProcessed.setText(Double.toString(BPM));
                }

                 */
                double valShow;
                /*
                if(selectedPara == "HF") {
                    valShow = Math.round((HF)*100)/100;
                }else{
                    valShow = Math.round((breathRate)*100)/100;
                }

                 */
                switch(selectedPara){
                    case"HF":
                        valShow = Math.round((HF)*100.0)/100.0;
                        break;
                    case"SDNN":
                        valShow = Math.round((SDNN)*100.0)/100.0;
                        break;
                    /*
                    case"LF/HF":

                        valShow = Math.round((LF_HF)*100)/100;
                        break;

                     */
                    default:
                        valShow = Math.round((breathRate)*100.0)/100.0;

                }

                if(init_error){
                    menuDialog2();
                    MenuDialog2.show();
                    init_error = false;
                }



                Minute_tv.setText(Integer.toString(Min_Time_GET));
                time_tv.setText(Integer.toString(Time_GET));
                if(mXPoint>0 && mXPoint < (PPGTime*60*ppg_fs)) {
                    ParaVal_tv.setText(Double.toString(valShow));
                   // Log.d("TAG", "Double.toString(valShow): " + Double.toString(valShow));
                }


                /*
                if(mXPoint == (PPGTime*60*ppg_fs)){
                    ParaVal_tv.setText("0.0");
                    Log.d("TAG", "ParaVal_tv.setText(0.0);: " );
                }

                 */

                //textView6_tv.setText("init_val:"+debugging_probe1);
                //textView12_tv.setText("feedbackCounter"+debugging_probe7);
                //console_tv.setText("trigger_val:"+debugging_probe3);
                //textView2_tv.setText("para:"+debugging_probe2);
                //textView18_tv.setText("SDNN_thres_str:"+debugging_probe4);
               // console_tv2.setText("mxPoint:"+debugging_probe5);
                //textView21_tv.setText("feedback_time:"+debugging_probe6);
                //console_tv2.setText("feedbackBase:"+debugging_probe4);

            }
        };
    }

    public void checkButton(View v){
        int radioId = radioGroup.getCheckedRadioButtonId();
        radioButton = GuanView.findViewById(radioId);
        Toast.makeText(LInflater.getContext(), "vm03!", Toast.LENGTH_SHORT).show();
    }



    //Fragment產生時自動呼叫此函式
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
        voice_ctr = 0;

        PPGTime = 1;
        postRecordPPGTime = 0;
        post_start_mpoint = PPGTime*60*ppg_fs;
        finish_mpoint = ((PPGTime+postRecordPPGTime)*60*ppg_fs)-5;


        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("message");

        // Write to Firebase
        try {
            myRef.setValue("Hello, Firebase!");
            System.out.println("Data was successfully written to Firebase.");
        } catch (DatabaseException e) {
            System.out.println("Data could not be written to Firebase: " + e.getMessage());
        }

        GuanView = inflater.inflate(R.layout.guan, container, false);
        LInflater = inflater;
        fileHandler = new Handler();
        dateformat  = new SimpleDateFormat("yyyyMMddHHmmss");
        appData =new UiDataBundle();
        appData2 =new UiDataBundle();

        init_val = 6;
        input_init_val = 6;
        target_val = 3;
        feedback_time = 5;

        trigger_val = Math.abs(target_val - init_val)/feedback_time;
        selectedPara = "Breathing rate";

        radioGroup = GuanView.findViewById(R.id.radioGroupx);
        buttonApply = GuanView.findViewById(R.id.ok_btn);
        buttonApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int radioId = radioGroup.getCheckedRadioButtonId();
                radioButton = GuanView.findViewById(radioId);
                String para_str = String.valueOf(radioButton.getText());
                Toast.makeText(LInflater.getContext(), "已選擇"+para_str, Toast.LENGTH_LONG).show();

                switch (para_str){
                    case"SDNN":
                        init_val = 40;
                        input_init_val = 40;
                        target_val = 60;
                        Toast.makeText(LInflater.getContext(), "已選擇"+para_str+" 預設初值:"+init_val+" 預設目標值:"+target_val, Toast.LENGTH_LONG).show();
                        feedback_time = 10;
                        trigger_val = Math.abs(target_val - init_val)/feedback_time;
                        selectedPara = "SDNN";
                        break;
                    case"HF":
                        init_val = 500;
                        input_init_val = 500;
                        target_val = 2000;
                        Toast.makeText(LInflater.getContext(), "已選擇"+para_str+" 預設初值:"+init_val+" 預設目標值:"+target_val, Toast.LENGTH_LONG).show();
                        feedback_time = 10;
                        trigger_val = Math.abs(target_val - init_val)/feedback_time;
                        selectedPara = "HF";
                        break;
                    default:
                        init_val = 6;
                        input_init_val = 6;
                        target_val = 3;
                        Toast.makeText(LInflater.getContext(), "已選擇"+para_str+" 預設初值:"+init_val+" 預設目標值:"+target_val, Toast.LENGTH_LONG).show();
                        feedback_time = 5;
                        trigger_val = Math.abs(target_val - init_val)/feedback_time;
                        selectedPara = "Breathing rate";
                        break;
                }
            }
        });
        //建立序列阜管理物件，並註冊特定事件
        usbManager = (UsbManager) inflater.getContext().getSystemService(inflater.getContext().USB_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(ACTION_USB_DEVICE_DETACHED);
        inflater.getContext().registerReceiver(broadcastReceiver, filter);

        // 藍芽使用
        ble_status = GuanView.findViewById(R.id.ble_status);
        button_paired = (Button) GuanView.findViewById(R.id.btn_paired);
        event_listView = (ListView) GuanView.findViewById(R.id.Show_B_List);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            event_listView.setNestedScrollingEnabled(true);
        }
        deviceName = new ArrayAdapter<String>((Activity) LInflater.getContext(), android.R.layout.simple_expandable_list_item_1);
        deviceId = new ArrayAdapter<String>((Activity) LInflater.getContext(), android.R.layout.simple_expandable_list_item_1);
        button_paired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScan();
            }
        });

        //使用者介面物件宣告
        G_Graph = GuanView.findViewById(R.id.data_chart);
        G_Graph2 = GuanView.findViewById(R.id.data_chart2);
        console_tv = GuanView.findViewById(R.id.console_tv);
        console_tv2 = GuanView.findViewById(R.id.console_tv2);
        //textView6_tv = GuanView.findViewById(R.id.textView6);
        textView12_tv = GuanView.findViewById(R.id.textView12);
        textView2_tv = GuanView.findViewById(R.id.textView2);
       // imgProcessed = GuanView.findViewById(R.id.AvgBPM_tv);
        time_tv = GuanView.findViewById(R.id.time_tv);
        Minute_tv = GuanView.findViewById(R.id.Minute_tv);
        ParaName_tv = GuanView.findViewById(R.id.ParaName_tv);
        ParaVal_tv = GuanView.findViewById(R.id.ParaVal_tv);
        ParaUnit_tv = GuanView.findViewById(R.id.ParaUnit_tv);
        tvPara2 = GuanView.findViewById(R.id.tvPara2);
        //textView18_tv = GuanView.findViewById(R.id.textView18);
        textView21_tv = GuanView.findViewById(R.id.textView21);
        // consoles_tv2 = GuanView.findViewById(R.id.console_tv2);

        ParaName_tv.setText(selectedPara+" :");
        ParaUnit_tv.setText(selectedParaUnit);

        ticking_btn = GuanView.findViewById(R.id.ticking);
        start_btn = GuanView.findViewById(R.id.Start_btn);

        Abort_btn = GuanView.findViewById(R.id.Abort_btn);
        testSound_btn = GuanView.findViewById(R.id.testSound_btn);
        menu_btn = GuanView.findViewById(R.id.Option_btn);
        //upload_btn = GuanView.findViewById(R.id.upload_fb);
        //upload_btn.setEnabled(false);
        setButtonEnable(SerialFlag);

        G_Series2 = new LineGraphSeries<>();
        G_Graph2.addSeries(G_Series2);
        G_Graph2.getViewport().setMinX(0);
        G_Graph2.getViewport().setMaxX(500);
        G_Graph2.getViewport().setXAxisBoundsManual(true);
        //Log.d("TAG", "maxP: " + mXPoint);
        //if(mXPoint>sliding_window_size){
            addRandomDataPoint();
       // }


        //音量測試按鈕動作
        mp1 = MediaPlayer.create(inflater.getContext(),R.raw.singingbowla1);
        mp2 = MediaPlayer.create(inflater.getContext(),R.raw.singingbowla2);
        mp3 = MediaPlayer.create(inflater.getContext(),R.raw.singingbowla3);
        mp4 = MediaPlayer.create(inflater.getContext(),R.raw.singingbowla4);
        mp5 = MediaPlayer.create(inflater.getContext(),R.raw.singingbowla5);
        mp6 = MediaPlayer.create(inflater.getContext(),R.raw.singingbowla6);
        mp7 = MediaPlayer.create(inflater.getContext(),R.raw.singingbowla7);
        mp8 = MediaPlayer.create(inflater.getContext(),R.raw.singingbowla8);
        mp9 = MediaPlayer.create(inflater.getContext(),R.raw.singingbowla9);
        mp10 = MediaPlayer.create(inflater.getContext(),R.raw.singingbowla10);
        mp11 = MediaPlayer.create(inflater.getContext(),R.raw.singingbowla11);
        mp12 = MediaPlayer.create(inflater.getContext(),R.raw.singingbowla12);
        mpTick = MediaPlayer.create(inflater.getContext(),R.raw.onetick);
        mpInhale = MediaPlayer.create(inflater.getContext(),R.raw.inhale);
        mpExhale = MediaPlayer.create(inflater.getContext(),R.raw.exhale);
        mpRemeasure = MediaPlayer.create(inflater.getContext(),R.raw.warningmesg);

        mpGreeting = MediaPlayer.create(inflater.getContext(),R.raw.greeting2);
        mpIntro = MediaPlayer.create(inflater.getContext(),R.raw.intro1_1);
        mpIntro2 = MediaPlayer.create(inflater.getContext(),R.raw.intro1_2);
        mpInstruction = MediaPlayer.create(inflater.getContext(),R.raw.intructions);
        mpVoice1_1 = MediaPlayer.create(inflater.getContext(),R.raw.intro2);
        mpVoice1_2 = MediaPlayer.create(inflater.getContext(),R.raw.m1_1);
        mpVoice2 = MediaPlayer.create(inflater.getContext(),R.raw.m2);
        mpVoice2_control = MediaPlayer.create(inflater.getContext(),R.raw.m2_control);
        mpVoice3 = MediaPlayer.create(inflater.getContext(),R.raw.thirdmessage);
        mpFinish = MediaPlayer.create(inflater.getContext(),R.raw.finish2);

        mpSigBig = MediaPlayer.create(inflater.getContext(),R.raw.signal_big);
        mpSigSmall = MediaPlayer.create(inflater.getContext(),R.raw.signal_small);
        mpSigArti = MediaPlayer.create(inflater.getContext(),R.raw.noise_detect);

        testSound_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ringIdx = ringIdx%12;
                switch (ringIdx){
                    case 0:
                        mp1.start();
                        break;
                    case 1:
                        mp2.start();
                        break;
                    case 2:
                        mp3.start();
                        break;
                    case 3:
                        mp4.start();
                        break;
                    case 4:
                        mp5.start();
                        break;
                    case 5:
                        mp6.start();
                        break;
                    case 6:
                        mp7.start();
                        break;
                    case 7:
                        mp8.start();
                        break;
                    case 8:
                        mp9.start();
                        break;
                    case 9:
                        mp10.start();
                        break;
                    case 10:
                        mp11.start();
                        break;
                    case 11:
                        mp12.start();
                        break;
                }
                ringIdx++;
                //mp1.start();
            }
        });

        dataQ = new DoubleTwoDimQueue();
        dataQ2 = new DoubleTwoDimQueue();
        startPointer = 0;
        endPointer = 0;
        recievedDataPoint = 1024;
        dataPointCount = 0;
        slidingWindowIsFull = true;
        keep_thread_running = false;
        BPM = 0;
        fillWindowPhase = 0;
        timestampQ = new Stack<Long>();

        ticking_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ticking_enable == false){
                    ticking_enable = true;
                    Toast.makeText(LInflater.getContext(), "open ticking", Toast.LENGTH_SHORT).show();
                    ticking_btn.setText("關閉時間提示");

                }else{
                    ticking_enable = false;
                    Toast.makeText(LInflater.getContext(), "close ticking", Toast.LENGTH_SHORT).show();
                    ticking_btn.setText("開啟時間提示");
                }
            }
        });

        //開始訓練按鈕動作
        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(SerialFlag == true) {

                    VarReset();
                    ShowData.feedbackCounter = 0;
                    voice_ctr = 0;
                    breathRate = 0;
                    setButtonEnable(false);
                    ResetGraph();
                    abortTraining = false;
                    StartBtn_Click(inflater);
                    keep_thread_running = true;
                    /*
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for(int i = 0; i < 10000; i++){
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        addEntry();
                                    }
                                });
                                try{
                                    Thread.sleep(1000);
                                }catch (InterruptedException e){

                                }

                            }
                        }
                    }).start();

*/

                    calHeartRate();
                    findIntervalThread.start();
                    finishAndUpload();
                }
            }
        });

        Abort_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                keep_thread_running = false;
                abortTraining = true;
                G_Graph.removeCallbacks(findIntervalThread);
                findIntervalThread.interrupt();
                try {
                    findIntervalThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Stop_Flag = true;
                mXPoint = 0;
                mXPointPara = 170;
                dataPointCtr = 0;
                VarReset();
                ResetGraph();

                Process.killProcess(Process.myPid());
                System.exit(1);

            }
        });

        //設定參數按鈕動作

        menu_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuDialog.show();
            }
        });
        /*
        upload_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date current = new Date();
                uploadFirebase("Name:"+usrName+" arrayLen:"+arrayLen,"訓練日期:"+sdf.format(current));
                upload_btn.setEnabled(false);
            }
        });

         */

        menuDialog();
        onClickStart();
        UpdateUI();

        //UpdateUI2();

        return GuanView;
    }

    private  void addRandomDataPoint(){
        mHandler2.postDelayed(new Runnable() {
            @Override
            public void run() {
//                Toast.makeText(LInflater.getContext(), "test", Toast.LENGTH_SHORT).show();
//                Log.d("TAG", "maxP: " + sliding_window_size);
                //Log.d("TAG", "maxPBef: " + mXPoint);
                if(mXPoint>sliding_window_size){
                    //Log.d("TAG", "maxP: " + mXPoint);
                    if(!abortTraining && !(Stop_Flag && mXPoint != 0) ){


                        mXPointPara++;
                        if(mXPointPara>170){
                            G_Graph2.getViewport().setMinX(mXPointPara-170);
                            G_Graph2.getViewport().setMaxX(mXPointPara);

                            switch (selectedPara){
                                case"SDNN":
                                    G_Graph2.getViewport().setMaxY(150);
                                    G_Graph2.getViewport().setMinY(20);
                                    G_Graph2.getViewport().setYAxisBoundsManual(true);

                                    break;
                                case"HF":
                                    G_Graph2.getViewport().setMaxY(4000);
                                    G_Graph2.getViewport().setMinY(50);
                                    G_Graph2.getViewport().setYAxisBoundsManual(true);

                                    break;
                                default:
                                    G_Graph2.getViewport().setMaxY(20);
                                    G_Graph2.getViewport().setMinY(0);
                                    G_Graph2.getViewport().setYAxisBoundsManual(true);
                                    break;
                            }




                        }

                        //LineGraphSeries<DataPoint> seriesRIAVFFT= new LineGraphSeries<>(toData(RIAV_Result));
                        //G_Series2.resetData(generateData(toData(RIAV_Result)));


                        //G_Series2.setAnimated(true);

                    }
                 }
                addRandomDataPoint();

            }
        },1000);
    }

    public DataPoint[] toData(double[] arr){
        DataPoint[] values = new DataPoint[arr.length];     //creating an object of type DataPoint[] of size 'n'
        for(int i=0;i<arr.length;i++){
            DataPoint v = new DataPoint(i,arr[i]);
            values[i] = v;
        }
        return values;
    };


    //將資料轉成字串以便上傳
    public String PpgToString (double a[])
    {
        String ans=",";
        int l=a.length;
        for (int j = 0; j < l; j++) {
            ans = ans+a[j]+',';
        }
        ans=ans+']';
        return ans;
    }

    public void Upload_Firebase2(String t,String s,String d)
    {
        // Write to Firebase
        try {
            final FirebaseDatabase database=FirebaseDatabase.getInstance();//取得資料庫連結
            DatabaseReference myRef=database.getReference(t);//新增資料節點
            myRef.child(s).push().setValue(d);
            System.out.println("Data was successfully written to Firebase.");
        } catch (DatabaseException e) {
            final FirebaseDatabase database=FirebaseDatabase.getInstance();//取得資料庫連結
            DatabaseReference myRef=database.getReference("error");//新增資料節點
            myRef.setValue("Data could not be written to Firebase: " + e.getMessage());
            System.out.println("Data could not be written to Firebase: " + e.getMessage());
        }
    }




    //將資料轉成字串以便上傳
    public String stringArrayToString (String a[], int l)
    {
        String ans="";
        for (int j = 0; j < l; j++) {
            ans = ans+a[j]+',';
        }
        ans='['+ans+']';
        return ans;
    }

    //將資料轉成字串以便上傳
    public String arrayToString (double a[])
    {
        String ans="";
        int l=a.length;
        for (int j = 0; j < l; j++) {
            ans = ans+a[j]+',';
        }
        ans='['+ans+']';
        return ans;
    }



    // 設定 FFT 點數
    private void handleInputData() {
        if(dataPointCount == updateEveryXPoint && slidingWindowIsFull == true) mpGreeting.start();
        if(mXPoint == 575) mpIntro.start();
        if(mXPoint == 900) mpIntro2.start();
        if(mXPoint == 1600) mpInstruction.start();
        if(mXPoint == normal_breathing_size) {
            if(feedback_time == 0){
                mpVoice2_control.start();
            }else{
                mpVoice2.start();
            }

            init_val = input_init_val;
        }

        if(postRecordPPGTime>0 && mXPoint == post_start_mpoint) mpVoice3.start();
        if(mXPoint == finish_mpoint) mpFinish.start();




        //Triggering for FFT
        if(slidingWindowIsFull){
            if(dataPointCount >= windowSize) {
                //if(dataPointCount == windowSize && selectedPara == "SDNN") mpVoice3.start();
                recievedDataPoint = 1024;
                startPointer = 0;
                endPointer = dataPointCount - 1;
                start_cal = true;
                slidingWindowIsFull = false;
                dataPointCount = 0;
            }
            else if((dataPointCount >= 768) && (dataPointCount < 512) && (fillWindowPhase == 2)){

                fillWindowPhase++;
                recievedDataPoint = 768;
                endPointer = dataPointCount - 1;
                start_cal = true;
            } else if((dataPointCount >= 512) && (dataPointCount < 768) && (fillWindowPhase == 1)){

                fillWindowPhase++;
                recievedDataPoint = 512;
                endPointer = dataPointCount - 1;
                start_cal = true;
            } else if((dataPointCount >= 256) && (dataPointCount < 512) &&(fillWindowPhase == 0)){

                fillWindowPhase++;
                recievedDataPoint = 256;
                endPointer = dataPointCount - 1;
                start_cal = true;
            }
        } else {
            if(dataPointCount >= updateEveryXPoint){

                startPointer = startPointer  + dataPointCount;
                endPointer = endPointer + dataPointCount;
                start_cal = true;
                dataPointCount = 0;

                double paraData[][] = new double[1][2];
                if(BPM != 0) {
                    paraData[0][0] = BPM;
                    paraData[0][1] = 0;
                    HRDataSeries.Qpush(paraData);
                }
                //Log.d("Push", "handleInputData: ");



                String header = usrName +"_"+ selectedPara+"_" + PPGTime+"min"+"_"+sdf2.format(current2) ;

                if(sig_small && mXPoint<4500){
                    mpSigSmall.start();
                    sig_small= false;
                }
               /*
                if(arti_detected){
                    mpSigArti.start();
                    arti_detected = false;
                }




                if(sig_big){
                    mpSigBig.start();
                    sig_big= false;
                }
                */


//                if(SDNN != 0 ) {
                if(true) {
                    paraData[0][0] = SDNN;
                    paraData[0][1] = 0;
                    SDNNDataSeries.Qpush(paraData);
                    SimpleDateFormat sdf = new SimpleDateFormat("MM_dd HH:mm:ss");
                    Date current = new Date();
                    Upload_Firebase2(header,"timestr",sdf.format(current));
                    Upload_Firebase2(header,"ppgstr",windowed_ppg_str);
                    //Upload_Firebase2(header,"raw_data0",raw_data0_str);
                    //Upload_Firebase2(header,"raw_data1",raw_data1_str);
                    String config = "Inital value: "+Double.toString(init_val)+ ", Target value: "+Double.toString(target_val)+", Feedback time: "+Double.toString(feedback_time);
                    String vc_str = Integer.toString(voice_ctr);
                    Upload_Firebase2(header, "config", config);
                    Upload_Firebase2(header, "feedback counter", vc_str);


                    //for debugging
                    Upload_Firebase2(header,"artilist0_str",artilist0_str);
                    Upload_Firebase2(header,"artilist1_str",artilist1_str);
                    Upload_Firebase2(header,"artilist2_str",artilist2_str);
                    Upload_Firebase2(header,"rrlist_str",rrlist_str);
                    //Upload_Firebase2(header,"RIIV_sliding_window_str",RIIV_sliding_window_str);
                    //Upload_Firebase2(header,"RIAV_sliding_window_str",RIAV_sliding_window_str);
                    //Upload_Firebase2(header,"RIFV_sliding_window_str",RIFV_sliding_window_str);
                    Upload_Firebase2(header,"SDNN_thres_str",SDNN_thres_str);
                    Upload_Firebase2(header,"SDNN_remeasure_str",SDNN_remeasure_str);
                    Upload_Firebase2(header,"fft_window_SDNN_str",fft_window_SDNN_str);
                    Upload_Firebase2(header,"fft_window_ROI_lower_Idx_str",fft_window_ROI_lower_Idx_str);
                    Upload_Firebase2(header,"fft_window_RIIV_bpm_str",fft_window_RIIV_bpm_str);
                    Upload_Firebase2(header,"fft_window_RIAV_bpm_str",fft_window_RIAV_bpm_str);
                    Upload_Firebase2(header,"fft_window_RIFV_bpm_str",fft_window_RIFV_bpm_str);
                    Upload_Firebase2(header,"skip_window_str",skip_window_str);


                    //Upload_Firebase2(header,"RIFV_power_thres_str",RIFV_power_thres_str);
                    //Upload_Firebase2(header,"RIFV_power_each_window_thres_str",RIFV_power_each_window_thres_str);
                    //Upload_Firebase2(header,"RIAV_power_thres_str",RIAV_power_thres_str);
                    //Upload_Firebase2(header,"RIAV_power_each_window_thres_str",RIAV_power_each_window_thres_str);

                    //confi


                    String sdnn_str = Double.toString(SDNN);
                    Upload_Firebase2(header,"sdnn",sdnn_str);

                }
                if(HF != 0 ) {
                    paraData[0][0] = HF;
                    paraData[0][1] = 0;
                    HFDataSeries.Qpush(paraData);

                    String HF_str = Double.toString(HF);
                    Upload_Firebase2(header,"hf",HF_str);
                }
                /*
                if(LF_HF != 0) {
                    paraData[0][0] = LF_HF;
                    paraData[0][1] = 0;
                    LF_HFDataSeries.Qpush(paraData);
                    if(selectedPara == "LF/HF") G_Series2.appendData(new DataPoint(mXPointPara, LF_HF), false, 100);
                }

                 */
                //Log.d("TAG", "inhale_time:"+Double.toString(inhale_time) );
                //Log.d("TAG", "exhale_time:"+Double.toString(exhale_time) );
                if(!(postRecordPPGTime>0 && mXPoint > post_start_mpoint)  && (mXPoint > normal_breathing_size)){
                    if(ticking_enable){
                        mpTick.start();
                    }
                    if(inhale_time!=0 && exhale_time!=0){
                        if(exhale_ctr%(exhale_time) == 0){
                            exhale_ctr = 0;
                            if(inhale_ctr == 0 && mXPoint >= (normal_breathing_size + 500)) {
                                mpInhale.start();
                                Log.d("TAG", "mpInhale.start()");
                            }
                            inhale_ctr++;
                            Log.d("TAG", "inhale_ctr++" );
                        }
                        if(inhale_ctr%(inhale_time+1)==0){
                            inhale_ctr = 0;
                            if(exhale_ctr == 0 && mXPoint >= (normal_breathing_size + 500)) {
                                mpExhale.start();
                                Log.d("TAG", "mpExhale.start()");
                            }

                            exhale_ctr++;
                            Log.d("TAG", "exhale_ctr++" );
                        }
                        Log.d("TAG", "inhale_ctr:"+Double.toString(inhale_ctr) );
                        Log.d("TAG", "exhale_ctr:"+Double.toString(exhale_ctr) );
                    }

                }

                if(breathRate != 0){



                   // timeStamps[timeStampsCtr++] = sdf.format(current);
                    br_SDNN_arr[brSDNNCtr++] = br_SDNN;
                    paraData[0][0] = breathRate;
                    paraData[0][1] = 0;
                    BRDataSeries.Qpush(paraData);
                    paraData[0][0] = probe;
                    paraData[0][1] = 0;
                    probeDataSeries.Qpush(paraData);
                    paraData[0][0] = probea;
                    paraData[0][1] = 0;
                    probeaDataSeries.Qpush(paraData);
                    paraData[0][0] = probeb;
                    paraData[0][1] = 0;
                    probebDataSeries.Qpush(paraData);
                    paraData[0][0] = probec;
                    paraData[0][1] = 0;
                    probecDataSeries.Qpush(paraData);
                    paraData[0][0] = probed;
                    paraData[0][1] = 0;
                    probedDataSeries.Qpush(paraData);
                    paraData[0][0] = probee;
                    paraData[0][1] = 0;
                    probeeDataSeries.Qpush(paraData);
                    paraData[0][0] = probef;
                    paraData[0][1] = 0;
                    probefDataSeries.Qpush(paraData);
                    paraData[0][0] = probeg;
                    paraData[0][1] = 0;
                    probegDataSeries.Qpush(paraData);

                    //G_Series2.appendData(new DataPoint(mXPointPara, 0), false, 200);

                    String br_str = Double.toString(breathRate);
                    Upload_Firebase2(header,"br",br_str);

                }
                double valueToPlot = 0;

                switch (selectedPara){
                    case"SDNN":
                        valueToPlot = SDNN;
                        break;
                    case"HF":
                        valueToPlot = HF;
                        break;
                    default:
                        valueToPlot = breathRate;
                        break;
                }

                G_Series2.appendData(new DataPoint(mXPointPara, valueToPlot), false, 200);


                //G_Series_para = new LineGraphSeries<DataPoint>(dataPointPara());


/*
                switch(selectedPara) {
                    case "SDNN":
                        para = SDNN;
                        ascend = true;
                        break;
                    case "HF":
                        para = HF;
                        ascend = true;
                        break;
                    case "LF/HF":
                        para = LF/HF;
                        ascend = false;
                        break;
                    default:
                        para = breathRate;
                        ascend = false;
                        break;

                }
                */


                if( selectedPara == "SDNN" || selectedPara == "HF"){
                    if(selectedPara == "SDNN") para = SDNN;
                    else para = HF;

                    debugging_probe1 = Double.toString(init_val);
                    debugging_probe2 = Double.toString(para);
                    debugging_probe3 = Double.toString(trigger_val);



                    if (trigger_val > 0 && mXPoint > 4500 && para > init_val  && ((para - init_val) >= trigger_val) && voice_ctr<feedback_time && error_flag == false) {

                        ShowData.feedbackCounter++;
                        voice_ctr++;

                        ringIdx = ringIdx%12;
                        switch (ringIdx){
                            case 0:
                                mp1.start();
                                break;
                            case 1:
                                mp2.start();
                                break;
                            case 2:
                                mp3.start();
                                break;
                            case 3:
                                mp4.start();
                                break;
                            case 4:
                                mp5.start();
                                break;
                            case 5:
                                mp6.start();
                                break;
                            case 6:
                                mp7.start();
                                break;
                            case 7:
                                mp8.start();
                                break;
                            case 8:
                                mp9.start();
                                break;
                            case 9:
                                mp10.start();
                                break;
                            case 10:
                                mp11.start();
                                break;
                            case 11:
                                mp12.start();
                                break;
                        }
                        ringIdx++;
                        init_val += trigger_val;
                        Toast.makeText(LInflater.getContext(), "觸發音效!", Toast.LENGTH_SHORT).show();
                    }
                }




                //Reference
                //Effects of slow breathing rate on heart rate variability and arterial baroreflex sensitivity in essential hypertension
                if( selectedPara == "Breathing rate"){
                    para = breathRate;

                    debugging_probe1 = Double.toString(init_val);
                    debugging_probe2 = Double.toString(para);
                    debugging_probe3 = Double.toString(trigger_val);

                    debugging_probe5 = Double.toString(mXPoint);
                    debugging_probe6 = Double.toString(feedback_time);
                    debugging_probe7 = Double.toString(ShowData.feedbackCounter);
                    //Log.d("TAG", "voice_ctr:"+Double.toString(voice_ctr) );
                    if (trigger_val > 0 && mXPoint > 4500 && para <= init_val  && (((init_val+trigger_val) - para) >= trigger_val) && voice_ctr<feedback_time && error_flag == false) {



                        ShowData.feedbackCounter++;
                        voice_ctr++;
                        //Log.d("TAG", "((init_val+trigger_val) - para):"+Double.toString(((init_val+trigger_val) - para)) );
                        //Log.d("TAG", "trigger_val:"+Double.toString(trigger_val) );
                        //Log.d("TAG", "voice_ctr:"+Double.toString(voice_ctr) );
                        ringIdx = ringIdx%12;
                        switch (ringIdx){
                            case 0:
                                mp1.start();
                                break;
                            case 1:
                                mp2.start();
                                break;
                            case 2:
                                mp3.start();
                                break;
                            case 3:
                                mp4.start();
                                break;
                            case 4:
                                mp5.start();
                                break;
                            case 5:
                                mp6.start();
                                break;
                            case 6:
                                mp7.start();
                                break;
                            case 7:
                                mp8.start();
                                break;
                            case 8:
                                mp9.start();
                                break;
                            case 9:
                                mp10.start();
                                break;
                            case 10:
                                mp11.start();
                                break;
                            case 11:
                                mp12.start();
                                break;
                        }
                        ringIdx++;
                        init_val -= trigger_val;
                        Toast.makeText(LInflater.getContext(), "觸發音效!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private DataPoint[]  dataPointPara(){

        DataPoint[] points = new DataPoint[SDNNDataSeries.getQSize()];
        double[] tempPara = SDNNDataSeries.toArray(0,SDNNDataSeries.getQSize(),0);
        for(int i=0; i<SDNNDataSeries.getQSize(); i++) {
            DataPoint v = new DataPoint(i, tempPara[i]);
            points[i] = v;
        };
        return points;
    };

    // 計算 BPM
    private void calHeartRate()
    {
        findIntervalThread = new Thread(){
            @Override
            public void run(){

                while(keep_thread_running){
                    if(abortTraining){
                        break;
                    }

                    if (start_cal == false){

                        //Sleeping part may lead to timing problems
                        try {
                            Thread.sleep(100);
                        }catch (Exception e){
                            e.printStackTrace();
                        }

                    }
                    else {
                        //debugging_probe3 = Double.toString(trigger_val);
                        //debugging_probe4 = Double.toString(feedback_time);
                        //debugging_probe5 = Double.toString(target_val);

                        start_cal = false;

//                        long timeStart  = timestampQ.get(startPointer);
//                        long timeEnd    = timestampQ.get(endPointer);
                        queueSize = dataQ.getQSize();

                        if(queueSize <= sliding_window_size) continue;

                        try{

                            double total_point = PPGTime*60*ppg_fs;

                            if(dataQ.getQSize()>sliding_window_size && windowStartIdx<total_point-sliding_window_size) {

                                boolean contain_arti = false;

                                double[] cor_freq = new double[(int)(3*window_time)];
                                int cor_freq_ctr = 0;

                                double[] windowed_PPG_b = dataQ.toArray(windowStartIdx, windowEndIdx, 0);

                                windowed_ppg_str  = arrayToString(windowed_PPG_b);
                                //debugging_probe7 = Integer.toString((int)windowed_PPG_b[10]);

                                Detrend d2 = new Detrend(windowed_PPG_b, "constant");
                                double[] windowed_PPG = d2.detrendSignal();

                                //Interpolate to 500Hz to lower the error of HRV calculation

                                double[] upsampled_PPG_x = new double[windowed_PPG.length];
                                for(int qq=0; qq<windowed_PPG.length; qq++) upsampled_PPG_x[qq] = qq*upsample_factor;


                                UnivariateInterpolator splineIinterpolator_ppg = new SplineInterpolator();
                                UnivariateFunction calibrantInterpolator_ppg = splineIinterpolator_ppg.interpolate(upsampled_PPG_x, windowed_PPG);

                                double[] up_ppg = new double[(int)(windowed_PPG.length*upsample_factor)];
                                double[] up_x = new double[(int)(windowed_PPG.length*upsample_factor)];
                                for (int i=0; i<up_ppg.length-upsample_factor; i++) {
                                    up_x[i] = i;
                                    up_ppg[i] = calibrantInterpolator_ppg.value(i);
                                }


                                //Lowpass filter
                                Butterworth butterworthLowPass2 = new Butterworth();
                                butterworthLowPass2.lowPass(20,new_fs,5);
                                for(int j=0; j<up_ppg.length; j++) {
                                    up_ppg[j] = butterworthLowPass2.filter(up_ppg[j]);
                                }

                                //Sliding window
                                double[] one_line = new double[4]; //P1(x,y), P2(x,y)
                                double[][] lines = new double[up_ppg.length][4];
                                double[] alpha = new double[up_ppg.length];

                                //IMS algorithm
                                int seg = 1;
                                int l = 1;
                                int segInLine = 1;
                                one_line[0] = seg;
                                one_line[1] = up_ppg[seg];
                                one_line[2] = seg+1;
                                one_line[3] = up_ppg[seg+1];
                                lines[l] = one_line;
                                alpha[l] = up_ppg[seg+1]- up_ppg[seg];
                                //System.out.println("alpha[l]: "+alpha[l]);
                                boolean noHorizontal = true;
                                double[] arti_loc_arr = new double[up_ppg.length];
                                int arti_loc_arr_ptr = 0;
                                l = l + 1;
                                seg = seg + 1;
                                while(seg+1 < up_ppg.length) {
                                    double[] line2 = new double[4];
                                    line2[0] = seg;
                                    line2[1] = up_ppg[seg];
                                    line2[2] = seg+1;
                                    line2[3] = up_ppg[seg+1];
                                    lines[l] = line2;
                                    alpha[l] = up_ppg[seg+1]- up_ppg[seg];

                                    int horizontal_point_count = 1;
                                    if(alpha[l] == 0) {
                                        if(seg+1+horizontal_point_count<up_ppg.length) {
                                            while(up_ppg[seg+1+horizontal_point_count]-up_ppg[seg+1] == 0 ) {
                                                horizontal_point_count++;
                                                //System.out.println("****seg+1+horizontal_point_count: "+(seg+1+horizontal_point_count));
                                                if((seg+1+horizontal_point_count >= up_ppg.length-5)) {

                                                    break;
                                                }
                                            }
                                        }
                                        if(horizontal_point_count>100) {

                                            noHorizontal = false;
                                        }

                                    }

                                    if (alpha[l]*alpha[l-1] > 0 || (alpha[l] == 0 && (horizontal_point_count<=1000))) {   //have the same sign:  merge
                                        double[] line3 = new double[4];
                                        line3[0] = seg-segInLine;
                                        line3[1] = up_ppg[seg-segInLine];
                                        line3[2] = seg+1;
                                        line3[3] = up_ppg[seg+1];
                                        alpha[l-1] = (up_ppg[seg+1]- up_ppg[seg-segInLine])/(segInLine+1);
                                        lines[l-1] = line3;
                                        seg = seg + 1;
                                        segInLine = segInLine + 1;

                                    }else{
                                        if(alpha[l-1] != 0 && alpha[l] == 0 && (horizontal_point_count>100)) {
                                            contain_arti = true;
                                            //arti_loc_arr[arti_loc_arr_ptr] = s+seg;
                                            arti_loc_arr_ptr++;

                                        }

                                        l = l + 1;
                                        seg = seg + 1;
                                        segInLine = 1;

                                    }
                                }



                                double[] valley_loc_arr = new double[up_ppg.length];
                                double[] peak_loc_arr = new double[up_ppg.length];
                                double[] peak_amp_arr = new double[up_ppg.length];

                                int line_num = 0;

                                int valley_loc_arr_ptr = 0;
                                int peak_loc_arr_ptr = 0;
                                int peak_amp_arr_ptr = 0;

                                boolean valid_line = true;
                                double mean_line_amp;
                                double std_line_amp;
                                double line_amp;


                                //Collect positive alpha line
                                ArrayList<Line_t> pre_pos_alpha_line = new ArrayList<Line_t>();
                                for(int lineCtr = 0; lineCtr<l-1 ; lineCtr++) {
                                    if(alpha[lineCtr]>0) {
                                        Line_t pos_line = new Line_t(lines[lineCtr][0],lines[lineCtr][1],lines[lineCtr][2],lines[lineCtr][3]);
                                        pre_pos_alpha_line.add(pos_line);
                                    }
                                }

                                //Correct positive alpha
                                double pro = 0.05;
                                ArrayList<Line_t> pos_alpha_line = new ArrayList<Line_t>();
                                for(int lineCtr = 0; lineCtr<pre_pos_alpha_line.size()-1 ; lineCtr++) {
                                    if(pre_pos_alpha_line.get(lineCtr).p1_y<pre_pos_alpha_line.get(lineCtr+1).p1_y
                                            && pre_pos_alpha_line.get(lineCtr).p2_y<pre_pos_alpha_line.get(lineCtr+1).p2_y
                                            && pre_pos_alpha_line.get(lineCtr).p2_y>pre_pos_alpha_line.get(lineCtr+1).p1_y
                                            && ((pre_pos_alpha_line.get(lineCtr).p2_y-pre_pos_alpha_line.get(lineCtr+1).p1_y)/(pre_pos_alpha_line.get(lineCtr+1).p2_y-pre_pos_alpha_line.get(lineCtr).p1_y))<pro
                                    ) {
                                        //System.out.println("Fix line at: "+pre_pos_alpha_line.get(lineCtr).p1_x);

                                        Line_t new_pos_line = new Line_t(pre_pos_alpha_line.get(lineCtr).p1_x,pre_pos_alpha_line.get(lineCtr).p1_y,pre_pos_alpha_line.get(lineCtr+1).p2_x,pre_pos_alpha_line.get(lineCtr+1).p2_y);
                                        pos_alpha_line.add(new_pos_line);
                                        lineCtr++;
                                    /*//Do not correct the second wavelet(prevent correcting the minor peak)
                                    if(lineCtr+2<pre_pos_alpha_line.size()) {
                                        if(new_pos_line.p1_y<pre_pos_alpha_line.get(lineCtr+2).p1_y
                                            && new_pos_line.p2_y<pre_pos_alpha_line.get(lineCtr+2).p2_y
                                            && new_pos_line.p2_y>pre_pos_alpha_line.get(lineCtr+2).p1_y
                                            && ((new_pos_line.p2_y-pre_pos_alpha_line.get(lineCtr+2).p1_y)/(pre_pos_alpha_line.get(lineCtr+2).p2_y-new_pos_line.p1_y))<pro) {
                                            //two wavelet
                                            Line_t new_pos_line2 = new Line_t(pre_pos_alpha_line.get(lineCtr).p1_x,pre_pos_alpha_line.get(lineCtr).p1_y,pre_pos_alpha_line.get(lineCtr+2).p2_x,pre_pos_alpha_line.get(lineCtr+2).p2_y);
                                            pos_alpha_line.add(new_pos_line2);
                                            lineCtr+=2;
                                        }else {

                                            pos_alpha_line.add(new_pos_line);
                                            lineCtr++;
                                        }

                                    }else {//only one wavelet

                                        pos_alpha_line.add(new_pos_line);
                                        lineCtr++;
                                    }
                                    */
                                    }else {
                                        pos_alpha_line.add(pre_pos_alpha_line.get(lineCtr));
                                    }
                                }

                                int firstLine = 0;
                                //Get first major peak
                                for(int lineCtr = 1; lineCtr+1<pos_alpha_line.size(); lineCtr++) {
                                    double last_amp = pos_alpha_line.get(lineCtr-1).getY2() ;
                                    double this_amp = pos_alpha_line.get(lineCtr).getY2() ;
                                    double next_amp = pos_alpha_line.get(lineCtr+1).getY2() ;
                                    if(this_amp>=next_amp && this_amp>=last_amp) {
                                        firstLine = lineCtr;

                                        break;
                                    }
                                }


                                ArrayList<Line_t> major_pks_arrlst = new ArrayList<Line_t>();
                                ArrayList<Line_t> minor_pks_arrlst = new ArrayList<Line_t>();
                                ArrayList<Artifact> artifact_arrlst = new ArrayList<Artifact>();

                                final double min_RR_interval = 9*upsample_factor;
                                final double max_RR_interval = 35*upsample_factor;
                                //Calcalate the minor peaks metrics
                                boolean heading_artifact = true;
                                boolean artifact_start = true;
                                double artifact_start_idx = 0;
                                double artifact_end_idx = 0;
                                double amp_thres = 10;
                                for(int lineCtr = firstLine; lineCtr+6<pos_alpha_line.size(); ) {

                                    double amp0 = pos_alpha_line.get(lineCtr).getY2() ;
                                    double amp1 = pos_alpha_line.get(lineCtr+1).getY2() ;
                                    double amp2 = pos_alpha_line.get(lineCtr+2).getY2() ;
                                    double amp3 = pos_alpha_line.get(lineCtr+3).getY2() ;
                                    double amp4 = pos_alpha_line.get(lineCtr+4).getY2() ;
                                    double amp5 = pos_alpha_line.get(lineCtr+5).getY2() ;
                                    double amp6 = pos_alpha_line.get(lineCtr+6).getY2() ;
                                    double peak_distance1 = pos_alpha_line.get(lineCtr+2).getX2() - pos_alpha_line.get(lineCtr).getX2();
                                    double peak_distance2 = pos_alpha_line.get(lineCtr+3).getX2() - pos_alpha_line.get(lineCtr).getX2();
                                    double peak_distance3 = pos_alpha_line.get(lineCtr+4).getX2() - pos_alpha_line.get(lineCtr).getX2();
                                    double peak_distance4 = pos_alpha_line.get(lineCtr+5).getX2() - pos_alpha_line.get(lineCtr).getX2();

                                    if(amp0 > amp1 && amp2 > amp1 && amp2 > amp3
                                            && ((amp2-amp3)>amp_thres) && ((amp2-amp1)>amp_thres)
                                            && peak_distance1>min_RR_interval &&  peak_distance1<max_RR_interval) {
                                        //pos_alpha_line.get(lineCtr+2) is a major peak, pos_alpha_line.get(lineCtr+1) is a minor peak
                                        if(heading_artifact) heading_artifact = false;
                                        Line_t major_peak = new Line_t(pos_alpha_line.get(lineCtr+1).getX1(),pos_alpha_line.get(lineCtr+1).getY1()
                                                ,pos_alpha_line.get(lineCtr+2).getX2(),pos_alpha_line.get(lineCtr+2).getY2());
                                        major_pks_arrlst.add(major_peak);
                                        Line_t minor_peak = new Line_t(pos_alpha_line.get(lineCtr+1).getX1(),pos_alpha_line.get(lineCtr+1).getY1()
                                                ,pos_alpha_line.get(lineCtr+1).getX2(),pos_alpha_line.get(lineCtr+1).getY2());
                                        minor_pks_arrlst.add(minor_peak);


                                        if(artifact_start == false) {
                                            artifact_end_idx = pos_alpha_line.get(lineCtr+2).getX2();
                                            artifact_start = true;
                                            Artifact found_arti = new Artifact((int) artifact_start_idx, (int)artifact_end_idx);
                                            artifact_arrlst.add(found_arti);

                                            //System.out.println("+++++++1artifact found in :"+(int) artifact_start_idx+" to "+(int)artifact_end_idx);
                                        }
                                        //System.out.println("x,y is a peak1:"+pos_alpha_line.get(lineCtr+2).getX2()+","+pos_alpha_line.get(lineCtr+2).getY2());


                                        if(major_pks_arrlst.size()>1 && major_pks_arrlst.get(major_pks_arrlst.size()-1).getX2() > artifact_end_idx) {
                                            double dif = major_pks_arrlst.get(major_pks_arrlst.size()-1).getX2()-major_pks_arrlst.get(major_pks_arrlst.size()-2).getX2();
                                            cor_freq[cor_freq_ctr++] = (dif*1000)/new_fs;

                                        }

                                        lineCtr+=2;

                                    }else if(amp0 > amp1 && amp3 > amp2 && amp3 > amp1 && amp3 > amp4
                                            && ((amp3-amp4)>amp_thres) && ((amp3-amp2)>amp_thres)
                                            && peak_distance2>min_RR_interval &&  peak_distance2<max_RR_interval){
                                        //
                                        if(heading_artifact) heading_artifact = false;
                                        Line_t major_peak = new Line_t(pos_alpha_line.get(lineCtr+1).getX1(),pos_alpha_line.get(lineCtr+1).getY1()
                                                ,pos_alpha_line.get(lineCtr+3).getX2(),pos_alpha_line.get(lineCtr+3).getY2());
                                        major_pks_arrlst.add(major_peak);
                                        Line_t minor_peak = new Line_t(pos_alpha_line.get(lineCtr+1).getX1(),pos_alpha_line.get(lineCtr+1).getY1()
                                                ,pos_alpha_line.get(lineCtr+1).getX2(),pos_alpha_line.get(lineCtr+1).getY2());
                                        minor_pks_arrlst.add(minor_peak);



                                        if(artifact_start == false) {
                                            artifact_end_idx = pos_alpha_line.get(lineCtr+3).getX2();
                                            artifact_start = true;
                                            Artifact found_arti = new Artifact((int) artifact_start_idx, (int)artifact_end_idx);
                                            artifact_arrlst.add(found_arti);

                                            //System.out.println("+++++++2artifact found in :"+(int) artifact_start_idx+" to "+(int)artifact_end_idx);
                                        }

                                        //System.out.println("x,y is a peak2:"+pos_alpha_line.get(lineCtr+3).getX2()+","+pos_alpha_line.get(lineCtr+3).getY2());

                                        if(major_pks_arrlst.size()>1 && major_pks_arrlst.get(major_pks_arrlst.size()-1).getX2() > artifact_end_idx) {
                                            double dif = major_pks_arrlst.get(major_pks_arrlst.size()-1).getX2()-major_pks_arrlst.get(major_pks_arrlst.size()-2).getX2();
                                            cor_freq[cor_freq_ctr++] = (dif*1000)/new_fs;

                                        }

                                        lineCtr+=3;
                                    }else if(amp0 > amp1 && amp4 > amp2 && amp4 > amp3 && amp4 > amp1 && amp4 > amp5
                                            && ((amp4-amp5)>amp_thres) && ((amp4-amp3)>amp_thres)
                                            && peak_distance3>min_RR_interval &&  peak_distance3<max_RR_interval){
                                        if(heading_artifact) heading_artifact = false;
                                        Line_t major_peak = new Line_t(pos_alpha_line.get(lineCtr+1).getX1(),pos_alpha_line.get(lineCtr+1).getY1()
                                                ,pos_alpha_line.get(lineCtr+4).getX2(),pos_alpha_line.get(lineCtr+4).getY2());
                                        major_pks_arrlst.add(major_peak);
                                        Line_t minor_peak = new Line_t(pos_alpha_line.get(lineCtr+1).getX1(),pos_alpha_line.get(lineCtr+1).getY1()
                                                ,pos_alpha_line.get(lineCtr+1).getX2(),pos_alpha_line.get(lineCtr+1).getY2());
                                        minor_pks_arrlst.add(minor_peak);

                                        if(artifact_start == false) {
                                            artifact_end_idx = pos_alpha_line.get(lineCtr+4).getX2();
                                            artifact_start = true;
                                            Artifact found_arti = new Artifact((int) artifact_start_idx, (int)artifact_end_idx);
                                            artifact_arrlst.add(found_arti);

                                           // System.out.println("+++++++3artifact found in :"+(int) artifact_start_idx+" to "+(int)artifact_end_idx);
                                        }

                                        //System.out.println("x,y is a peak3:"+pos_alpha_line.get(lineCtr+4).getX2()+","+pos_alpha_line.get(lineCtr+4).getY2());

                                        if(major_pks_arrlst.size()>1 && major_pks_arrlst.get(major_pks_arrlst.size()-1).getX2() > artifact_end_idx) {
                                            double dif = major_pks_arrlst.get(major_pks_arrlst.size()-1).getX2()-major_pks_arrlst.get(major_pks_arrlst.size()-2).getX2();
                                            cor_freq[cor_freq_ctr++] = (dif*1000)/new_fs;

                                        }

                                        lineCtr+=4;
                                    }else if(amp0 > amp1 && amp5 > amp1 && amp5 > amp2 &&  amp5 > amp3 &&  amp5 > amp4  && amp5 > amp6
                                            && ((amp5-amp6)>amp_thres) && ((amp5-amp4)>amp_thres)
                                            &&peak_distance4>min_RR_interval &&  peak_distance4<max_RR_interval){
                                        if(heading_artifact) heading_artifact = false;
                                        Line_t major_peak = new Line_t(pos_alpha_line.get(lineCtr+1).getX1(),pos_alpha_line.get(lineCtr+1).getY1()
                                                ,pos_alpha_line.get(lineCtr+5).getX2(),pos_alpha_line.get(lineCtr+5).getY2());
                                        major_pks_arrlst.add(major_peak);
                                        Line_t minor_peak = new Line_t(pos_alpha_line.get(lineCtr+1).getX1(),pos_alpha_line.get(lineCtr+1).getY1()
                                                ,pos_alpha_line.get(lineCtr+1).getX2(),pos_alpha_line.get(lineCtr+1).getY2());
                                        minor_pks_arrlst.add(minor_peak);



                                        if(artifact_start == false) {
                                            artifact_end_idx = pos_alpha_line.get(lineCtr+5).getX2();
                                            artifact_start = true;
                                            Artifact found_arti = new Artifact((int) artifact_start_idx, (int)artifact_end_idx);
                                            artifact_arrlst.add(found_arti);

                                           // System.out.println("+++++++4artifact found in :"+(int) artifact_start_idx+" to "+(int)artifact_end_idx);
                                        }

                                        //System.out.println("x,y is a peak4:"+pos_alpha_line.get(lineCtr+5).getX2()+","+pos_alpha_line.get(lineCtr+5).getY2());

                                        if(major_pks_arrlst.size()>1 && major_pks_arrlst.get(major_pks_arrlst.size()-1).getX2() > artifact_end_idx) {
                                            double dif = major_pks_arrlst.get(major_pks_arrlst.size()-1).getX2()-major_pks_arrlst.get(major_pks_arrlst.size()-2).getX2();
                                            cor_freq[cor_freq_ctr++] = (dif*1000)/new_fs;


                                        }

                                        lineCtr+=5;
                                    }else{
                                        if(heading_artifact == false) {
                                            if(artifact_start && major_pks_arrlst.size()>0) {
                                                artifact_start = false;
                                                artifact_start_idx = major_pks_arrlst.get(major_pks_arrlst.size()-1).getX2();
                                            }

                                            //artifact_arrlst.add(found_arti);

                                        }

                                        lineCtr++;
                                    }
                                }
                                boolean containArtimorethan0 = false;
                                boolean containArtimorethan1 = false;
                                boolean containArtimorethan2 = false;

                                //Artifact detection
                                //artifact_arrlst0 for detecting artifact that caused by large amplitude ppg
                                ArrayList<Artifact> artifact_arrlst0 = new ArrayList<Artifact>();

                                String arti0_str = "";


                                double[] amp_arr = new double[major_pks_arrlst.size()];

                                for(int xx=0; xx<major_pks_arrlst.size(); xx++) {
                                    double amp = major_pks_arrlst.get(xx).getY2()-major_pks_arrlst.get(xx).getY1();
                                    amp_arr[xx] = amp;
                                    //System.out.println("xx:"+xx);
                                    //System.out.println("amp:"+amp);
                                }
                                double mean_amp_arr = calculateMean(amp_arr);
                                double std_amp_arr = calculateStdev(amp_arr);
                                //System.out.println("amp mean:"+mean_amp_arr);
                                //System.out.println("amp std:"+std_amp_arr);
                                double thres_amp_arr = (mean_amp_arr+3*std_amp_arr)*1.2;
                                //System.out.println("thres_amp_arr:"+thres_amp_arr);
                                boolean rewrite_peak_amp = false;
                                for(int xx=0; xx<amp_arr.length; xx++) {
                                    //if(s == windowSeeIdx)System.out.println("amp_arr[xx]:"+amp_arr[xx]);

                                    if(amp_arr[xx] > thres_amp_arr) {
                                        //System.out.println("{{{{{Amp of peak is to large:"+xx);
                                        if(xx == 0) {
                                            major_pks_arrlst.get(xx).setY1(major_pks_arrlst.get(xx+1).p1_y);
                                            major_pks_arrlst.get(xx).setY2(major_pks_arrlst.get(xx+1).p2_y);
                                            //System.out.println("Set the "+xx+ "peak Y2 to "+major_pks_arrlst.get(xx+1).p2_y);

                                        }else {
                                            major_pks_arrlst.get(xx).setY1(major_pks_arrlst.get(xx-1).p1_y);
                                            major_pks_arrlst.get(xx).setY2(major_pks_arrlst.get(xx-1).p2_y);
                                            //System.out.println("Set the "+xx+ "peak Y2 to "+major_pks_arrlst.get(xx-1).p2_y);
                                        }
                                        rewrite_peak_amp = true;

                                        Artifact large_amp_arti = new Artifact((int)major_pks_arrlst.get(xx).p1_x,(int)major_pks_arrlst.get(xx).p2_x);
                                        arti0_str += "[" + Double.toString(major_pks_arrlst.get(xx).p1_x)+ ","+Double.toString(major_pks_arrlst.get(xx).p2_x)+"], ";
                                        artifact_arrlst0.add(large_amp_arti);
                                        containArtimorethan0 = true;

                                    }
                                    //System.out.println("xx:"+xx);
                                    //System.out.println("amp_arr[xx]:"+amp_arr[xx]);

                                }

                                artilist0_str = arti0_str;

                                //Check the number of the peaks in 64 windows
                                //System.out.println("major_pks_arrlst.size():"+major_pks_arrlst.size());
                                if(major_pks_arrlst.size()<(int)(window_time/3)) {

                                    continue;
                                }
                                if(major_pks_arrlst.size()>(int)(window_time*2.5)) {

                                    continue;
                                }

                                double[] p_m_loc = new double[minor_pks_arrlst.size()];
                                double[] pks_m_intensities = new double[minor_pks_arrlst.size()];
                                for(int ht=0; ht<minor_pks_arrlst.size(); ht++) {
                                    p_m_loc[ht] = minor_pks_arrlst.get(ht).getX2();
                                    pks_m_intensities[ht] = minor_pks_arrlst.get(ht).getY2();
                                }
                                int points_m_RIIV = (int)((p_m_loc[p_m_loc.length-1]-p_m_loc[0])*1000/new_fs);
                                //Prevent the leading zero
                                int start_m_idx = (int)(p_m_loc[0])*(int)(1000/new_fs);

                                double[] RImV_X = linspace(p_m_loc[0],p_m_loc[p_m_loc.length-1],points_m_RIIV);
                                UnivariateInterpolator splineIinterpolator_m_RIIV = new SplineInterpolator();
                                UnivariateFunction calibrantInterpolator_m_RIIV = splineIinterpolator_m_RIIV.interpolate(p_m_loc,pks_m_intensities);
                                double[] interpolate_RImV_Result = new double[RImV_X.length];
                                for (int i=0; i<RImV_X.length; i++) {
                                    interpolate_RImV_Result[i] = calibrantInterpolator_m_RIIV.value(RImV_X[i]);
                                }
                                //System.out.println("start_idx:"+start_idx);
                                //System.out.println("points_RIIV:"+points_RIIV);
                                if(start_m_idx>points_m_RIIV) {
                                    //System.out.println("start_idx>points_RIIV Error!! ");
                                    continue;
                                }

                                double[] cut_interpolate_RImV_Result = Arrays.copyOfRange(interpolate_RImV_Result,start_m_idx,points_m_RIIV);
                                double[] cut_interpolate_RImV_X = Arrays.copyOfRange(interpolate_RImV_Result,start_m_idx,points_m_RIIV);

                                double[] filtered_interpolate_RImV_Result = new double[cut_interpolate_RImV_Result.length];
                                //Lowpass before downsampling to avoid aliasing
                                Butterworth butterworthLowPass_RImV = new Butterworth();
                                butterworthLowPass_RImV.lowPass(20,1000,2);
                                for(int j=0; j<cut_interpolate_RImV_Result.length; j++) {
                                    filtered_interpolate_RImV_Result[j] = butterworthLowPass_RImV.filter(cut_interpolate_RImV_Result[j]);
                                }

                                int cut_RImV_length = cut_interpolate_RImV_X.length;
                                //Down sampling to 4Hz
                                double[] RImV_4Hz_X = new double[(cut_RImV_length/250)+1];
                                double[] RImV_4Hz_Y = new double[(cut_RImV_length/250)+1];
                                for(int i=0; (i*250)<cut_RImV_length; i++) {
                                    RImV_4Hz_X[i] = cut_interpolate_RImV_X[i*250];
                                    RImV_4Hz_Y[i] = filtered_interpolate_RImV_Result[i*250];
                                }




                                //Respiratory-induced intensity variation (RIIV) waveforms
                                double[] p_loc = new double[major_pks_arrlst.size()];
                                double[] pks_intensities = new double[major_pks_arrlst.size()];
                                for(int ht=0; ht<major_pks_arrlst.size(); ht++) {
                                    p_loc[ht] = major_pks_arrlst.get(ht).getX2();
                                    pks_intensities[ht] = major_pks_arrlst.get(ht).getY2();
                                }
                                int points_RIIV = (int)((p_loc[p_loc.length-1]-p_loc[0])*1000/new_fs);
                                //Prevent the leading zero
                                int start_idx = (int)(p_loc[0])*(int)(1000/new_fs);

                                double[] RIIV_X = linspace(p_loc[0],p_loc[p_loc.length-1],points_RIIV);
                                UnivariateInterpolator splineIinterpolator_RIIV = new SplineInterpolator();
                                UnivariateFunction calibrantInterpolator_RIIV = splineIinterpolator_RIIV.interpolate(p_loc,pks_intensities);
                                double[] interpolate_RIIV_Result = new double[RIIV_X.length];
                                for (int i=0; i<RIIV_X.length; i++) {
                                    interpolate_RIIV_Result[i] = calibrantInterpolator_RIIV.value(RIIV_X[i]);
                                }
                                //System.out.println("start_idx:"+start_idx);
                                //System.out.println("points_RIIV:"+points_RIIV);
                                if(start_idx>points_RIIV) {
                                    //System.out.println("start_idx>points_RIIV Error!! ");
                                    continue;
                                }



                                double[] cut_interpolate_RIIV_Result = Arrays.copyOfRange(interpolate_RIIV_Result,start_idx,points_RIIV);
                                double[] cut_interpolate_RIIV_X = Arrays.copyOfRange(RIIV_X,start_idx,points_RIIV);





                                //Detrend detrend_RIIV = new Detrend(cut_interpolate_RIIV_Result, "constant");
                                //double[] detrend_interpolate_RIIV_Result = detrend_RIIV.detrendSignal();

                                double[] filtered_interpolate_RIIV_Result = new double[cut_interpolate_RIIV_Result.length];
                                //Lowpass before downsampling to avoid aliasing
                                Butterworth butterworthLowPass_RIIV = new Butterworth();
                                butterworthLowPass_RIIV.lowPass(20,1000,2);
                                for(int j=0; j<cut_interpolate_RIIV_Result.length; j++) {
                                    filtered_interpolate_RIIV_Result[j] = butterworthLowPass_RIIV.filter(cut_interpolate_RIIV_Result[j]);
                                }

                                //Down sampling to 4Hz
                                int cut_RIIV_length = cut_interpolate_RIIV_X.length;
                                double[] RIIV_4Hz_X = new double[(cut_RIIV_length/250)+1];
                                double[] RIIV_4Hz_Y = new double[(cut_RIIV_length/250)+1];
                                for(int i=0; (i*250)<cut_RIIV_length; i++) {
                                    RIIV_4Hz_X[i] = cut_interpolate_RIIV_X[i*250];
                                    RIIV_4Hz_Y[i] = filtered_interpolate_RIIV_Result[i*250];
                                }

                                String temp_riiv_str = "";
                                for(int se=0; se<RIIV_4Hz_Y.length; se++) temp_riiv_str += RIIV_4Hz_Y[se]+", ";
                                RIIV_sliding_window_str = temp_riiv_str;

                                //Respiratory-induced amplitude variation  (RIAV) waveforms
                                double[] RIAV_X = RIIV_X;
                                int points_RIAV = points_RIIV;
                                double[] pks_amplitude = new double[major_pks_arrlst.size()];
                                for(int ht=0; ht<major_pks_arrlst.size(); ht++)  pks_amplitude[ht] = major_pks_arrlst.get(ht).getY2()-major_pks_arrlst.get(ht).getY1();
                                UnivariateInterpolator splineIinterpolator_RIAV = new SplineInterpolator();
                                UnivariateFunction calibrantInterpolator_RIAV = splineIinterpolator_RIAV.interpolate(p_loc,pks_amplitude);
                                double[] interpolate_RIAV_Result = new double[RIAV_X.length];
                                for (int i=0; i<RIAV_X.length; i++) {
                                    interpolate_RIAV_Result[i] = calibrantInterpolator_RIAV.value(RIAV_X[i]);
                                }




                                double[] cut_interpolate_RIAV_Result = Arrays.copyOfRange(interpolate_RIAV_Result,start_idx,points_RIAV);
                                double[] cut_interpolate_RIAV_X = Arrays.copyOfRange(RIAV_X,start_idx,points_RIAV);

                                double[] filtered_interpolate_RIAV_Result = new double[cut_interpolate_RIAV_Result.length];

                                //Lowpass before downsampling to avoid aliasing
                                Butterworth butterworthLowPass_RIAV = new Butterworth();
                                butterworthLowPass_RIAV.lowPass(20,1000,2);
                                for(int j=0; j<cut_interpolate_RIAV_Result.length; j++) {
                                    filtered_interpolate_RIAV_Result[j] = butterworthLowPass_RIAV.filter(cut_interpolate_RIAV_Result[j]);
                                }

                                int cut_RIAV_length = cut_interpolate_RIAV_X.length;
                                //Down sampling to 4Hz
                                double[] RIAV_4Hz_X = new double[(cut_RIAV_length/250)+1];
                                double[] RIAV_4Hz_Y = new double[(cut_RIAV_length/250)+1];
                                for(int i=0; (i*250)<cut_RIAV_length; i++) {
                                    RIAV_4Hz_X[i] = cut_interpolate_RIAV_X[i*250];
                                    RIAV_4Hz_Y[i] = filtered_interpolate_RIAV_Result[i*250];
                                }

                                String temp_riav_str = "";
                                for(int se=0; se<RIAV_4Hz_Y.length; se++) temp_riav_str += RIAV_4Hz_Y[se]+", ";
                                RIAV_sliding_window_str = temp_riav_str;



                                double[] f_loc = new double[p_loc.length-1];
                                double[] frequencies = new double[p_loc.length-1];

                                int min_interval = (int)(0.24*new_fs);
                                //artifact_arrlst1 for detecting artifact that caused by irregular ppg waveform (not quite senitive)
                                ArrayList<Artifact> artifact_arrlst1 = new ArrayList<Artifact>();
                                String arti1_str = "";

                                //artifact_arrlst2 for detecting artifact that caused by invalid RR intervals (very sensitive)
                                ArrayList<Artifact> artifact_arrlst2 = new ArrayList<Artifact>();
                                String arti2_str = "";

                                if(artifact_arrlst.size()>0) {

                                    //Artifact aggregation by time
                                    int temp_start = artifact_arrlst.get(0).start_loc;
                                    int temp_end = artifact_arrlst.get(0).end_loc;
                                    for(int aa = 0; aa<artifact_arrlst.size()-1; aa++) {


                                        if((artifact_arrlst.get(aa+1).start_loc-artifact_arrlst.get(aa).end_loc)/new_fs < 5 ) {

                                            temp_end = artifact_arrlst.get(aa+1).end_loc;
                                        }else {
                                            //System.out.println("append artifact:"+temp_start+" to "+temp_end+" as arti");
                                            Artifact agg_arti = new Artifact(temp_start, temp_end);
                                            if(Math.abs(temp_start-temp_start)/new_fs>3) {
                                                //System.out.println("@@@@arti_acc_time > 3");
                                                containArtimorethan1 = true;
                                            }
                                            artifact_arrlst1.add(agg_arti);
                                            temp_start = artifact_arrlst.get(aa+1).start_loc;
                                            temp_end = artifact_arrlst.get(aa+1).end_loc;
                                        }
                                    }
                                    //System.out.println("append artifact:"+temp_start+" to "+temp_end+" as arti");
                                    Artifact agg_arti = new Artifact(temp_start, temp_end);
                                    if(Math.abs(temp_start-temp_start)/new_fs>3) {
                                        //System.out.println("@@@@arti_acc_time > 3");
                                        containArtimorethan1 = true;
                                    }
                                    artifact_arrlst1.add(agg_arti);

                                    for(int ee=0; ee<artifact_arrlst1.size(); ee++) arti1_str += "[" + Double.toString(artifact_arrlst1.get(ee).start_loc)+ ","+Double.toString(artifact_arrlst1.get(ee).end_loc)+"], ";


                                    for(int ht=0; ht<frequencies.length; ht++) {
                                        frequencies[ht] = (major_pks_arrlst.get(ht+1).getX2() - major_pks_arrlst.get(ht).getX2())*1000/new_fs;
                                        f_loc[ht] = major_pks_arrlst.get(ht).getX2();
                                        if(ht>0) {
                                            if(frequencies[ht] < frequencies[ht-1]*0.5 || frequencies[ht] < min_interval) {
                                                //System.out.println("@@@f_loc[ht-1]: "+f_loc[ht-1]+" to "+f_loc[ht]+" is artifact(< min interval)");
                                                //System.out.println("@@@frequencies[ht] is artifact:"+frequencies[ht]);
                                                Artifact found_arti = new Artifact((int) f_loc[ht-1], (int)f_loc[ht]);
                                                artifact_arrlst2.add(found_arti);
                                                containArtimorethan2 = true;
                                            }
                                        }
                                        //if(s == windowSeeIdx)System.out.println("frequencies[ht]:"+frequencies[ht]);

                                    }

                                    for(int ee=0; ee<artifact_arrlst2.size(); ee++) arti2_str += "[" + Double.toString(artifact_arrlst2.get(ee).start_loc)+ ","+Double.toString(artifact_arrlst2.get(ee).end_loc)+"], ";

                                }else { //Without any problem
                                    for(int ht=0; ht<frequencies.length; ht++) {
                                        frequencies[ht] = (major_pks_arrlst.get(ht+1).getX2() - major_pks_arrlst.get(ht).getX2())*1000/new_fs;
                                        f_loc[ht] = major_pks_arrlst.get(ht).getX2();
                                    }
                                }

                                artilist1_str = arti1_str;
                                artilist2_str = arti2_str;

                                double[] non_zero_freq = pruneZero(frequencies);
                                double[] non_zero_f_loc = pruneZero(f_loc);

                                //Correct the wrong RR intervals
                                double freqMean = calculateMean(non_zero_freq);
                                double freqStd = calculateStdev(non_zero_freq);
                                double freqThresUpper = freqMean+(4*freqStd);
                                double freqThresLower = freqMean-(4*freqStd);
                                for(int sde=0; sde<non_zero_freq.length; sde++) {
                                    if(non_zero_freq[sde]>freqThresUpper || non_zero_freq[sde]<freqThresLower) {
                                        if(sde==0 && non_zero_freq[1]!=0) {
                                            if(non_zero_freq[1]>freqThresUpper || non_zero_freq[1]<freqThresLower) {
                                                non_zero_freq[0] = non_zero_freq[1] = non_zero_freq[2];
                                            }else {
                                                non_zero_freq[0] = non_zero_freq[1];
                                            }

                                        }
                                        if(sde==non_zero_freq.length-1 && non_zero_freq[non_zero_freq.length-2]!=0) {
                                            if(non_zero_freq[non_zero_freq.length-2]>freqThresUpper || non_zero_freq[non_zero_freq.length-2]<freqThresLower) {
                                                non_zero_freq[non_zero_freq.length-1] = non_zero_freq[non_zero_freq.length-2] = non_zero_freq[non_zero_freq.length-3];
                                            }else {
                                                non_zero_freq[non_zero_freq.length-1] = non_zero_freq[non_zero_freq.length-2];
                                            }
                                        }
                                        if(sde!=non_zero_freq.length-1 && sde!=0 ) {

                                            non_zero_freq[sde] = (non_zero_freq[sde+1]+non_zero_freq[sde-1])/2;
                                            non_zero_f_loc[sde] = (non_zero_f_loc[sde+1]+non_zero_f_loc[sde-1])/2;


                                        }
                                        //System.out.println("non_zero_freq[sde]:"+non_zero_freq[sde]);
                                    }
                                }

                                String temp_rrlist_str = "";
                                for(int se=0; se<non_zero_freq.length; se++) temp_rrlist_str += non_zero_freq[se]+", ";
                                rrlist_str = temp_rrlist_str;

                                double std_rrlistx = calculateStdev(non_zero_freq);
                                //Log.d("TAG", "std_rrlistx:"+std_rrlistx );
                                //Log.d("TAG", "mXPoint:"+mXPoint );
                                if((windowStartIdx==50 || windowStartIdx==25) && mXPoint>0) {
                                    double std_rrlist = calculateStdev(non_zero_freq);
                                    SDNN_remeasure_str = Double.toString(std_rrlist);
                                    //Log.d("TAG", "std_rrlist: " + SDNN_remeasure_str);
                                    int age = 20;
                                    try {
                                        age = Integer.parseInt(usrAge);
                                    } catch (NumberFormatException e) {
                                        age = 20;
                                    }


                                    if(age>75){
                                        normal_sdnn_upper = 84.8;
                                    }else{
                                        if(age>65 && age<=75){
                                            normal_sdnn_upper = 78.1;
                                        }else{
                                            if(age>55 && age<=65){
                                                normal_sdnn_upper = 75.41;
                                            }else{
                                                if(age>45 && age<=55){
                                                    normal_sdnn_upper = 76.63;
                                                }else{
                                                    if(age>35 && age<=45){
                                                        normal_sdnn_upper = 80.1;
                                                    }else{
                                                        if(age>25 && age<=35){
                                                            normal_sdnn_upper = 83.86;
                                                        }else{
                                                            if(age>18 && age<=25){
                                                                normal_sdnn_upper = 85.72;
                                                            }else{
                                                                normal_sdnn_upper = 85.72;
                                                            }

                                                        }

                                                    }

                                                }

                                            }

                                        }

                                    }

                                    //normal_sdnn_upper = 55;
                                    //Log.d("TAG", "normal_sdnn_upper:"+normal_sdnn_upper );
                                    //Log.d("TAG", "std_rrlist(in if):"+std_rrlist );
                                    if(std_rrlist > normal_sdnn_upper){
                                        mpRemeasure.start();
                                        init_error = true;

                                    }


                                }

                                //Log.d("TAG", "age: " + age);
                                //Log.d("TAG", "usrAge: " + usrAge);
                                //Log.d("TAG", "normal_sdnn_upper: " + normal_sdnn_upper);
                                //Respiratory-induced frequency variation (RIFV) waveforms
                                int points_RIFV = (int)((non_zero_f_loc[non_zero_f_loc.length-1]-non_zero_f_loc[0])*1000/new_fs);
                                UnivariateInterpolator splineIinterpolator_RIFV = new SplineInterpolator();

                                double mean_freq = calculateMean(non_zero_freq);
                                double std_freq = calculateStdev(non_zero_freq);
                                //System.out.println("UNCORRECT SDNN:"+std_freq);

                                double[] RIFV_X = linspace(non_zero_f_loc[0], non_zero_f_loc[non_zero_f_loc.length-1], points_RIFV);
                                UnivariateFunction calibrantInterpolator_RIFV = splineIinterpolator_RIFV.interpolate(non_zero_f_loc,non_zero_freq);
                                double[] interpolate_RIFV_Result = new double[points_RIFV];
                                for (int i=0; i<points_RIFV; i++) {
                                    interpolate_RIFV_Result[i] = calibrantInterpolator_RIFV.value(RIFV_X[i]);
                                }



                                double[] cut_interpolate_RIFV_Result = Arrays.copyOfRange(interpolate_RIFV_Result,start_idx,points_RIFV);
                                double[] cut_interpolate_RIFV_X = Arrays.copyOfRange(RIFV_X,start_idx,points_RIFV);



                                double[] filtered_interpolate_RIFV_Result = new double[cut_interpolate_RIFV_Result.length];
                                //Lowpass before downsampling to avoid aliasing
                                Butterworth butterworthLowPass_RIFV = new Butterworth();
                                butterworthLowPass_RIFV.lowPass(20,1000,2);
                                for(int j=0; j<cut_interpolate_RIFV_Result.length; j++) {
                                    filtered_interpolate_RIFV_Result[j] = butterworthLowPass_RIFV.filter(cut_interpolate_RIFV_Result[j]);
                                }

                                int cut_RIFV_length = cut_interpolate_RIFV_X.length;
                                //Down sampling to 4Hz
                                double[] RIFV_4Hz_X = new double[(cut_RIFV_length/250)+1];
                                double[] RIFV_4Hz_Y = new double[(cut_RIFV_length/250)+1];
                                for(int i=0; (i*250)<cut_RIFV_length; i++) {
                                    RIFV_4Hz_X[i] = cut_interpolate_RIFV_X[i*250];
                                    RIFV_4Hz_Y[i] = filtered_interpolate_RIFV_Result[i*250];
                                }

                                String temp_rifv_str = "";
                                for(int se=0; se<RIFV_4Hz_Y.length; se++) temp_rifv_str += RIFV_4Hz_Y[se]+", ";
                                RIFV_sliding_window_str = temp_rifv_str;



                                int padding_num = 768;
                                final double freqBin = 4/(double)(FFT_window_size+padding_num);
                                final int upperFreqIdx = 23;

                                double[] RIAV_brs = new double[30];
                                double[] RIIV_brs = new double[30];
                                double[] RIFV_brs = new double[30];
                                double[] RIVV_brs = new double[30];
                                double[] sum_brs = new double[30];
                                int RIAV_brs_ctr = 0;
                                int RIIV_brs_ctr = 0;
                                int RIFV_brs_ctr = 0;

                                int maxIdx_RIAV = 0, maxIdx_RIIV = 0, maxIdx_RIFV = 0;



                                last_five_val_HF_ctr = last_five_val_HF_ctr%5;
                                last_five_val_LF_ctr = last_five_val_LF_ctr%5;
                                /*
                                //First window contains 3 minutes normal breathing calculate the threshold first
                                //debugging_probe4 = Double.toString(windowStartIdx);
                                if(windowStartIdx == 0 || windowStartIdx == 25) {
                                    double[] RIFV_std_arr = new double[50];
                                    int ctr_of_std = 0;
                                    for(int sk = 8; sk < RIFV_4Hz_Y.length-FFT_window_size; sk+=(FFT_window_size/8) ) {

                                        //Sample 64s FFT window
                                        double[] RIFV_4Hz_Y_subwindow1 = Arrays.copyOfRange(RIFV_4Hz_Y, sk, sk+FFT_window_size);
                                        double RIFV_std = calculateStdev(RIFV_4Hz_Y_subwindow1);

                                        RIFV_std_arr[ctr_of_std++] = RIFV_std;

                                    }
                                    double[] valid_RIFV_std = Arrays.copyOfRange(RIFV_std_arr, 0, ctr_of_std);
                                    double mean_RIFV_std = calculateMean(valid_RIFV_std);
                                    double std_RIFV_std = calculateStdev(valid_RIFV_std);
                                    RIFV_SDNN_thres = mean_RIFV_std+3*std_RIFV_std;
                                    //debugging_probe1 = Double.toString(RIFV_SDNN_thres);

                                }

                                 */

                                ArrayList<Artifact> convert_artifact_arrlst0 = new ArrayList<Artifact>();
                                ArrayList<Artifact> convert_artifact_arrlst1 = new ArrayList<Artifact>();
                                ArrayList<Artifact> convert_artifact_arrlst2 = new ArrayList<Artifact>();

                                for(int gan=0; gan<artifact_arrlst0.size(); gan++) {
                                    int start_loc0 = (((artifact_arrlst0.get(gan).start_loc*(int)(1000/new_fs)) - start_idx)/250)-2;
                                    int end_loc0 = (((artifact_arrlst0.get(gan).end_loc*(int)(1000/new_fs)) - start_idx)/250)-2;

                                    Artifact c_a0 = new Artifact(start_loc0, end_loc0);
                                    convert_artifact_arrlst0.add(c_a0);
                                    //artifact_arrlst0.get(gan).start_loc
                                }

                                for(int gan=0; gan<artifact_arrlst1.size(); gan++) {
                                    int start_loc1 = (((artifact_arrlst1.get(gan).start_loc*(int)(1000/new_fs)) - start_idx)/250)-2;
                                    int end_loc1 = (((artifact_arrlst1.get(gan).end_loc*(int)(1000/new_fs)) - start_idx)/250)-2;

                                    Artifact c_a1 = new Artifact(start_loc1, end_loc1);
                                    convert_artifact_arrlst1.add(c_a1);
                                    //artifact_arrlst0.get(gan).start_loc
                                }

                                for(int gan=0; gan<artifact_arrlst2.size(); gan++) {
                                    int start_loc2 = (((artifact_arrlst2.get(gan).start_loc*(int)(1000/new_fs)) - start_idx)/250)-2;
                                    int end_loc2 = (((artifact_arrlst2.get(gan).end_loc*(int)(1000/new_fs)) - start_idx)/250)-2;

                                    Artifact c_a2 = new Artifact(start_loc2, end_loc2);
                                    convert_artifact_arrlst2.add(c_a2);
                                    //artifact_arrlst0.get(gan).start_loc
                                }

                                double[] power_RIFV_arr = new double[50];
                                int ctr_power_RIFV_arr = 0;
                                double[] power_RIAV_arr = new double[50];
                                int ctr_power_RIAV_arr = 0;

                                double[] RIFV_std_arr = new double[50];
                                double[] RIFV_mean_arr = new double[50];
                                int ctr_of_std = 0;
                                int ctr_of_mean = 0;
                                boolean skip_window = false;

                                //SDNN_thres_str = Double.toString(RIFV_SDNN_thres);

                                //for UI debugging
                                String RIFV_str = "";
                                String RIIV_str = "";
                                String RIAV_str = "";

                                //for firebase debugging
                                String fire_RIFV_SDNN_str = "";

                                String fire_ROI_lower_idx_str = "";


                                String fire_RIFV_bpm_str = "";
                                String fire_RIIV_bpm_str = "";
                                String fire_RIAV_bpm_str = "";

                                String fire_skip_window_str = "";


                                String fire_RIFV_power_each_window_thres_str = "";
                                String fire_RIAV_power_each_window_thres_str = "";

                                String timings = "";

                                //find subwindow from the last of the signal
                                for(int x = RIAV_4Hz_Y.length, y = RIIV_4Hz_Y.length, z = RIFV_4Hz_Y.length
                                    ; x>24+FFT_window_size && y>24+FFT_window_size && z>24+FFT_window_size
                                        ; x-=(FFT_window_size/8), y-=(FFT_window_size/8), z-=(FFT_window_size/8)) {

                                    double window_timing = (x/4) + (windowStartIdx/25);
                                    timings = timings + window_timing + ",";

                                    double[] RIIV_4Hz_Y_subwindow2 = Arrays.copyOfRange(RIIV_4Hz_Y, z-FFT_window_size, z);
                                    double[] RImV_4Hz_Y_subwindow2 = Arrays.copyOfRange(RImV_4Hz_Y, z-FFT_window_size, z);
                                    double[] RIFV_4Hz_Y_subwindow2 = Arrays.copyOfRange(RIFV_4Hz_Y, z-FFT_window_size, z);
                                    double RIFV_std_x = calculateStdev(RIFV_4Hz_Y_subwindow2);
                                    double RIFV_mean_x = calculateMean(RIFV_4Hz_Y_subwindow2);
                                    if(windowStartIdx==0 || windowStartIdx==25) {
                                        if(RIFV_std_x>65) {
                                            //Need to remesure the breathing
                                            continue;
                                        }
                                    }else {
                                        if(RIFV_std_x>200) {

                                            continue;
                                        }
                                    }

                                    RIFV_std_arr[ctr_of_std++] = RIFV_std_x;
                                    RIFV_mean_arr[ctr_of_mean++] = RIFV_mean_x;

                                    double br_lower_bound = 7; //bpm
                                    int ROI_lower = (int)((br_lower_bound/60)/freqBin);
                                    double br_upper_bound = 22; //bpm
                                    int ROI_upper = (int)((br_upper_bound/60)/freqBin);



                                    if(containArtimorethan0 && convert_artifact_arrlst0.size()>0) {
                                        for(int ssg=0; ssg<convert_artifact_arrlst0.size(); ssg++) {
                                            int arti_start_idx_loc = convert_artifact_arrlst0.get(ssg).start_loc;
                                            int arti_end_idx_loc = convert_artifact_arrlst0.get(ssg).end_loc;
                                            if((arti_start_idx_loc < z && arti_start_idx_loc > (z-FFT_window_size)) || (arti_end_idx_loc < z && arti_end_idx_loc > (z-FFT_window_size))
                                                    || ((arti_end_idx_loc > z) && (arti_start_idx_loc < (z-FFT_window_size)))) {
                                                fire_skip_window_str += "skip by exceed amp thres, ";
                                                skip_window = true;
                                            }

                                        }
                                        if(skip_window) {

                                            skip_window = false;
                                            //System.out.println("from "+(z-FFT_window_size)+" to "+z);
                                            continue;
                                        }
                                    }


                                    if(containArtimorethan1 && convert_artifact_arrlst1.size()>0) {
                                        for(int ssg=0; ssg<convert_artifact_arrlst1.size(); ssg++) {
                                            int arti_start_idx_loc = convert_artifact_arrlst1.get(ssg).start_loc;
                                            int arti_end_idx_loc = convert_artifact_arrlst1.get(ssg).end_loc;
                                            if((arti_start_idx_loc < z && arti_start_idx_loc > (z-FFT_window_size)) || (arti_end_idx_loc < z && arti_end_idx_loc > (z-FFT_window_size))
                                                    || ((arti_end_idx_loc > z) && (arti_start_idx_loc < (z-FFT_window_size)))) {
                                                fire_skip_window_str += "skip by irregular pulse, ";
                                                skip_window = true;
                                            }

                                        }
                                        if(skip_window) {

                                            skip_window = false;
                                            continue;
                                        }
                                    }

                                    if(containArtimorethan2 && convert_artifact_arrlst2.size()>0) {
                                        for(int ssg=0; ssg<convert_artifact_arrlst2.size(); ssg++) {
                                            int arti_start_idx_loc = convert_artifact_arrlst2.get(ssg).start_loc;
                                            int arti_end_idx_loc = convert_artifact_arrlst2.get(ssg).end_loc;
                                            if((arti_start_idx_loc < z && arti_start_idx_loc > (z-FFT_window_size)) || (arti_end_idx_loc < z && arti_end_idx_loc > (z-FFT_window_size))
                                                    || ((arti_end_idx_loc > z) && (arti_start_idx_loc < (z-FFT_window_size)))) {
                                                fire_skip_window_str += "skip by invalid RR, ";
                                                skip_window = true;
                                            }

                                        }
                                        if(skip_window) {

                                            skip_window = false;
                                            continue;
                                        }
                                    }

                                    fire_skip_window_str += "ok, ";

                                    //Sample 64s FFT window
                                    double[] RIAV_4Hz_Y_subwindow1 = Arrays.copyOfRange(RIAV_4Hz_Y, x-FFT_window_size, x);
                                    double[] RIIV_4Hz_Y_subwindow1 = Arrays.copyOfRange(RIIV_4Hz_Y, y-FFT_window_size, y);
                                    double[] RIFV_4Hz_Y_subwindow1 = Arrays.copyOfRange(RIFV_4Hz_Y, z-FFT_window_size, z);



                                    //Detrend
                                    int dPower = 5;
                                    Detrend d_1RIAV = new Detrend(RIAV_4Hz_Y_subwindow1, dPower);
                                    double[] RIAV_4Hz_Y_subwindow = d_1RIAV.detrendSignal();
                                    Detrend d_1RIIV = new Detrend(RIIV_4Hz_Y_subwindow1, dPower);
                                    double[] RIIV_4Hz_Y_subwindow = d_1RIIV.detrendSignal();
                                    Detrend d_1RIFV = new Detrend(RIFV_4Hz_Y_subwindow1, dPower);
                                    double[] RIFV_4Hz_Y_subwindow = d_1RIFV.detrendSignal();



                                    //Normalization of RIIV
                                    double RIIV_max = findMax(RIIV_4Hz_Y_subwindow);
                                    double RIIV_min = findMin(RIIV_4Hz_Y_subwindow);
                                    double[] normalized_RIIV_4Hz_Y_subwindow = new double[RIIV_4Hz_Y_subwindow.length];
                                    for(int nr=0; nr<RIIV_4Hz_Y_subwindow.length; nr++) {
                                        normalized_RIIV_4Hz_Y_subwindow[nr] = (RIIV_4Hz_Y_subwindow[nr]-RIIV_min)/(RIIV_max-RIIV_min);
                                    }

                                    Detrend n_RIIV = new Detrend(normalized_RIIV_4Hz_Y_subwindow, "constant");
                                    double[] d_norm_RIIV_sub = n_RIIV.detrendSignal();

                                    //Normalization of RIAV
                                    double RIAV_max = findMax(RIAV_4Hz_Y_subwindow);
                                    double RIAV_min = findMin(RIAV_4Hz_Y_subwindow);
                                    double[] normalized_RIAV_4Hz_Y_subwindow = new double[RIAV_4Hz_Y_subwindow.length];
                                    for(int nr=0; nr<RIAV_4Hz_Y_subwindow.length; nr++) {
                                        normalized_RIAV_4Hz_Y_subwindow[nr] = (RIAV_4Hz_Y_subwindow[nr]-RIAV_min)/(RIAV_max-RIIV_min);
                                    }

                                    Detrend n_RIAV = new Detrend(normalized_RIAV_4Hz_Y_subwindow, "constant");
                                    double[] d_norm_RIAV_sub = n_RIAV.detrendSignal();

                                    //Normalization of RIFV
                                    double RIFV_max = findMax(RIFV_4Hz_Y_subwindow);
                                    double RIFV_min = findMin(RIFV_4Hz_Y_subwindow);
                                    double[] normalized_RIFV_4Hz_Y_subwindow = new double[RIFV_4Hz_Y_subwindow.length];
                                    for(int nr=0; nr<RIFV_4Hz_Y_subwindow.length; nr++) {
                                        normalized_RIFV_4Hz_Y_subwindow[nr] = (RIFV_4Hz_Y_subwindow[nr]-RIFV_min)/(RIFV_max-RIFV_min);
                                    }

                                    Detrend n_RIFV = new Detrend(normalized_RIFV_4Hz_Y_subwindow, "constant");
                                    double[] d_norm_RIFV_sub = n_RIFV.detrendSignal();



                                    Detrend d_RIAV = new Detrend(RIAV_4Hz_Y_subwindow, "constant");
                                    double[] d_RIAV_sub = d_RIAV.detrendSignal();
                                    Detrend d_RIIV = new Detrend(RIIV_4Hz_Y_subwindow, "constant");
                                    double[] d_RIIV_sub = d_RIIV.detrendSignal();
                                    Detrend d_RIFV = new Detrend(RIFV_4Hz_Y_subwindow, "constant");
                                    double[] d_RIFV_sub = d_RIFV.detrendSignal();

                                    //Apply Hamming window
                                    Hamming H_window_pt = new Hamming(FFT_window_size);
                                    double[] H_window = H_window_pt.getWindow();
                                    for(int a=0; a<FFT_window_size; a++) {
                                        d_RIAV_sub[a] =  d_RIAV_sub[a]*H_window[a];
                                        d_RIIV_sub[a] =  d_RIIV_sub[a]*H_window[a];
                                        d_RIFV_sub[a] =  d_RIFV_sub[a]*H_window[a];

                                        d_norm_RIAV_sub[a] = d_norm_RIAV_sub[a]*H_window[a];
                                        d_norm_RIIV_sub[a] = d_norm_RIIV_sub[a]*H_window[a];
                                        d_norm_RIFV_sub[a] = d_norm_RIFV_sub[a]*H_window[a];
                                    }

                                    double[] zero_pad_d_RIAV_sub = zeroPad(d_RIAV_sub,padding_num);
                                    double[] zero_pad_d_RIIV_sub = zeroPad(d_RIIV_sub,padding_num);
                                    double[] zero_pad_d_RIFV_sub = zeroPad(d_RIFV_sub,padding_num);

                                    double[] zero_pad_d_norm_RIAV_sub = zeroPad(d_norm_RIAV_sub,padding_num);
                                    double[] zero_pad_d_norm_RIIV_sub = zeroPad(d_norm_RIIV_sub,padding_num);
                                    double[] zero_pad_d_norm_RIFV_sub = zeroPad(d_norm_RIFV_sub,padding_num);

                                    //Get FFT result
                                    FastFourier fft_result_RIAV = new FastFourier(zero_pad_d_RIAV_sub);
                                    FastFourier fft_result_RIIV = new FastFourier(zero_pad_d_RIIV_sub);
                                    FastFourier fft_result_RIFV = new FastFourier(zero_pad_d_RIFV_sub);

                                    FastFourier fft_norm_result_RIAV = new FastFourier(zero_pad_d_norm_RIAV_sub);
                                    FastFourier fft_norm_result_RIIV = new FastFourier(zero_pad_d_norm_RIIV_sub);
                                    FastFourier fft_norm_result_RIFV = new FastFourier(zero_pad_d_norm_RIFV_sub);

                                    fft_result_RIAV.transform();
                                    fft_result_RIIV.transform();
                                    fft_result_RIFV.transform();

                                    fft_norm_result_RIAV.transform();
                                    fft_norm_result_RIIV.transform();
                                    fft_norm_result_RIFV.transform();

                                    Complex[] FFT_RIAV_Result_pt = fft_result_RIAV.getComplex(false);
                                    Complex[] FFT_RIIV_Result_pt = fft_result_RIIV.getComplex(false);
                                    Complex[] FFT_RIFV_Result_pt = fft_result_RIFV.getComplex(false);

                                    Complex[] FFT_norm_RIAV_Result_pt = fft_norm_result_RIAV.getComplex(false);
                                    Complex[] FFT_norm_RIIV_Result_pt = fft_norm_result_RIIV.getComplex(false);
                                    Complex[] FFT_norm_RIFV_Result_pt = fft_norm_result_RIFV.getComplex(false);

                                    double [] RIAV_Result = new double[FFT_RIAV_Result_pt.length];
                                    double [] RIIV_Result = new double[FFT_RIIV_Result_pt.length];
                                    double [] RIFV_Result = new double[FFT_RIFV_Result_pt.length];

                                    double [] RIAV_norm_Result = new double[FFT_norm_RIAV_Result_pt.length];
                                    double [] RIIV_norm_Result = new double[FFT_norm_RIIV_Result_pt.length];
                                    double [] RIFV_norm_Result = new double[FFT_norm_RIFV_Result_pt.length];

                                    for (int i = 0; i < FFT_window_size; i++) {
                                        RIAV_Result[i] = FFT_RIAV_Result_pt[i].abs();
                                        RIIV_Result[i] = FFT_RIIV_Result_pt[i].abs();
                                        RIFV_Result[i] = FFT_RIFV_Result_pt[i].abs();

                                        RIAV_norm_Result[i] = FFT_norm_RIAV_Result_pt[i].abs();
                                        RIIV_norm_Result[i] = FFT_norm_RIIV_Result_pt[i].abs();
                                        RIFV_norm_Result[i] = FFT_norm_RIFV_Result_pt[i].abs();
                                    }

                                    //////
                                    double lowerbpm = 10;
                                    double upperbpm = 25;
                                    int lowerIdx = (int)(lowerbpm/(60*freqBin));
                                    int upperIdx = (int)(upperbpm/(60*freqBin));

                                    //Select (10-25 bpm)
                                    double[] sel_RIIV = Arrays.copyOfRange(RIIV_norm_Result,lowerIdx ,upperIdx);
                                    double[] sel_RIAV = Arrays.copyOfRange(RIAV_norm_Result,lowerIdx,upperIdx);
                                    double[] sel_RIFV = Arrays.copyOfRange(RIFV_norm_Result,lowerIdx,upperIdx);

                                    //Sum of FFT coefficient
                                    double sum_sel_RIIV = arrSum(sel_RIIV);
                                    double sum_sel_RIAV = arrSum(sel_RIAV);
                                    double sum_sel_RIFV = arrSum(sel_RIFV);

                                    Detrend dx = new Detrend(RImV_4Hz_Y_subwindow2, "constant");
                                    double[] d_RImV_4Hz_Y_subwindow2 = dx.detrendSignal();



                                    Butterworth butterworthLowPassx = new Butterworth();
                                    butterworthLowPassx.lowPass(20,4,0.33);
                                    for(int j=0; j<RImV_4Hz_Y_subwindow2.length; j++) {
                                        d_RImV_4Hz_Y_subwindow2[j] = butterworthLowPassx.filter(d_RImV_4Hz_Y_subwindow2[j]);
                                    }



                                    String mode = "rectangular";
                                    int wsize = 5;
                                    Smooth s_RImV_4Hz_Y_subwindow = new Smooth(d_RImV_4Hz_Y_subwindow2, wsize, mode);
                                    double[] s_RImV_sub = s_RImV_4Hz_Y_subwindow.smoothSignal();

                                    Smooth s_RIFV_4Hz_Y_subwindow = new Smooth(RIFV_4Hz_Y_subwindow2, wsize, mode);
                                    double[] s_RIFV_sub = s_RIFV_4Hz_Y_subwindow.smoothSignal();

                                    FindPeak s_RIFV_fp = new FindPeak(s_RIFV_sub);
                                    Peak s_RIFV_out = s_RIFV_fp.detectPeaks();
                                    int[] s_RIFV_peaks = s_RIFV_out.getPeaks();
                                    double[] s_RIFV_outHeight = s_RIFV_out.getHeights();
                                    double[] s_RIFV_outWidth = s_RIFV_out.getWidth();
                                    double[] s_RIFV_outProminence = s_RIFV_out.getProminence();
                                    double[][] s_RIFV_outPromData = s_RIFV_out.getProminenceData();

                                    FindPeak s_RImV_fp = new FindPeak(s_RImV_sub);
                                    Peak s_RImV_out = s_RImV_fp.detectPeaks();
                                    int[] s_RImV_peaks = s_RImV_out.getPeaks();
                                    double[] s_RImV_outHeight = s_RImV_out.getHeights();
                                    double[] s_RImV_outWidth = s_RImV_out.getWidth();
                                    double[] s_RImV_outProminence = s_RImV_out.getProminence();
                                    double[][] s_RImV_outPromData = s_RImV_out.getProminenceData();




                                    int RIIV_pks_ctr = 0;
                                    int[] f_s_RIIV_peaks = s_RImV_out.filterByWidth(0.0, 20.0);


                                    int RIFV_pks_ctr = 0;
                                    int[] f_s_RIFV_peaks = s_RIFV_out.filterByWidth(0.0, 20.0);



                                    if(f_s_RIIV_peaks.length <=10 && (f_s_RIFV_peaks.length <=10 )) {
                                        for(int g=0; g<s_RImV_peaks.length; g++) {
                                            double pro_wid = (s_RImV_outPromData[2][g] - s_RImV_outPromData[1][g])/4;
                                            if(pro_wid>=10 && pro_wid<=35 && s_RImV_outWidth[g]>10) RIIV_pks_ctr++;
                                        }
                                    }

                                    power_RIFV_arr[ctr_power_RIFV_arr++] = sum_sel_RIFV;
                                    power_RIAV_arr[ctr_power_RIAV_arr++] = sum_sel_RIAV;

                                    fire_RIFV_power_each_window_thres_str += Double.toString(sum_sel_RIFV) + ", ";
                                    fire_RIAV_power_each_window_thres_str += Double.toString(sum_sel_RIAV) + ", ";

                                    //////
                                    double RIFV_std = calculateStdev(RIFV_4Hz_Y_subwindow1);
                                    fire_RIFV_SDNN_str += Double.toString(RIFV_std) + ", ";
                                    if( (RIFV_std > RIFV_SDNN_thres && RIFV_SDNN_thres!=0)
                                            || (RIFV_power_thres>0 && sum_sel_RIFV < RIFV_power_thres
                                            || (RIIV_pks_ctr>=1 && RIIV_pks_ctr<=6))){
                                        br_lower_bound = 1.85; //bpm

                                        ROI_lower = (int)((br_lower_bound/60)/freqBin);

                                    }else{
                                        br_lower_bound = 7; //bpm
                                        ROI_lower = (int)((br_lower_bound/60)/freqBin);;
                                    }

                                    double post_time = 1444;//23*60+64
                                    if(window_timing>=post_time) {
                                        br_lower_bound = 7;
                                        ROI_lower = (int)((br_lower_bound/60)/freqBin);;
                                    }else {
                                        if(window_timing>=180) {
                                            br_lower_bound = 1.85; //

                                            ROI_lower = (int)((br_lower_bound/60)/freqBin);
                                        }else {
                                            br_lower_bound = 7;
                                            ROI_lower = (int)((br_lower_bound/60)/freqBin);;
                                        }
                                    }

                                    fire_ROI_lower_idx_str += Double.toString(ROI_lower)+", ";


                                    maxIdx_RIAV = findMaxIndexInHalfArr(RIAV_Result,ROI_lower,ROI_upper);
                                    maxIdx_RIIV = findMaxIndexInHalfArr(RIIV_Result,ROI_lower,ROI_upper);
                                    maxIdx_RIFV = findMaxIndexInHalfArr(RIFV_Result,ROI_lower,ROI_upper);

                                    //Get FFT result
                                    FastFourier fft_result_RIFV_sub = new FastFourier(RIFV_4Hz_Y_subwindow);

                                    fft_result_RIFV_sub.transform();

                                    Complex[] FFT_complex_windowed_RIFV = fft_result_RIFV_sub.getComplex(false);
                                    double [] power_RIFV_Result = new double[FFT_complex_windowed_RIFV.length];


                                    //Find breathing rate
                                    double RIAV_br_result = ((double)maxIdx_RIAV)*freqBin*60;
                                    RIAV_brs[RIAV_brs_ctr] = RIAV_br_result;
                                    RIAV_brs_ctr++;
                                    double RIIV_br_result = ((double)maxIdx_RIIV)*freqBin*60;
                                    RIIV_brs[RIIV_brs_ctr] = RIIV_br_result;
                                    RIIV_brs_ctr++;
                                    double RIFV_br_result = ((double)maxIdx_RIFV)*freqBin*60;
                                    RIFV_brs[RIFV_brs_ctr] = RIFV_br_result;
                                    RIFV_brs_ctr++;

                                    RIFV_str = RIFV_str + Double.toString(RIFV_br_result) + " ";
                                    RIIV_str = RIIV_str + Double.toString(RIIV_br_result) + " ";
                                    RIAV_str = RIAV_str + Double.toString(RIAV_br_result) + " ";

                                    fire_RIFV_bpm_str += Double.toString(RIFV_br_result) + ", ";
                                    fire_RIIV_bpm_str += Double.toString(RIIV_br_result) + ", ";
                                    fire_RIAV_bpm_str += Double.toString(RIAV_br_result) + ", ";


                                }//End of the for loop of sampling 64s FFT window

                                //Log.d("TAG", "timings: " + timings);
                                //Log.d("TAG", "ROI lower: " + fire_ROI_lower_idx_str);

                                double[] valid_power_RIFV = Arrays.copyOfRange(power_RIFV_arr, 0, ctr_power_RIFV_arr);
                                double[] valid_power_RIAV = Arrays.copyOfRange(power_RIAV_arr, 0, ctr_power_RIAV_arr);
                                if(windowStartIdx==0 || windowStartIdx==25) {
                                    double mean_valid_power_RIFV = calculateMean(valid_power_RIFV);
                                    double std_valid_power_RIFV = calculateStdev(valid_power_RIFV);
                                    double mean_valid_power_RIAV = calculateMean(valid_power_RIAV);
                                    double std_valid_power_RIAV = calculateStdev(valid_power_RIAV);

                                    RIFV_power_thres = mean_valid_power_RIFV - 2.5*std_valid_power_RIFV;
                                    RIAV_power_thres = mean_valid_power_RIAV - 2.5*std_valid_power_RIAV;



                                }

                                RIFV_power_thres_str = Double.toString(RIFV_power_thres);
                                RIAV_power_thres_str = Double.toString(RIAV_power_thres);

                                double[] valid_RIFV_std = Arrays.copyOfRange(RIFV_std_arr, 0, ctr_of_std);
                                double[] valid_RIFV_mean = Arrays.copyOfRange(RIFV_mean_arr, 0, ctr_of_mean);
                                double mean_RIFV_std = calculateMean(valid_RIFV_std);   //SDNN
                                double mean_RIFV_mean = calculateMean(valid_RIFV_mean); //BPM
                                if(windowStartIdx==0 || windowStartIdx==25) {
                                    double std_RIFV_std = calculateStdev(valid_RIFV_std);

                                    //RIFV_SDNN_thres = mean_RIFV_std+3*std_RIFV_std;



                                    double[] temp_val = new double[valid_RIFV_std.length];
                                    int ctr_temp_val = 0;
                                    ///System.out.println("========");

                                    double temp_thres = mean_RIFV_std+2*std_RIFV_std;
                                    for(int wer=0; wer<valid_RIFV_std.length; wer++) {
                                        if(valid_RIFV_std[wer]<temp_thres) {
                                            temp_val[ctr_temp_val++] =  valid_RIFV_std[wer];
                                        }
                                    }

                                    double[] temp_arr = Arrays.copyOfRange(temp_val, 0, ctr_temp_val);
                                    double temp_mean = calculateMean(temp_arr);
                                    double temp_std = calculateStdev(temp_arr);
                                    //System.out.println("temp_mean:"+temp_mean);
                                    //System.out.println("temp_std:"+temp_std);
                                    double temp_thres2 = temp_mean+3*temp_std;
                                    RIFV_SDNN_thres = temp_thres2;
                                    //System.out.println("new thres:"+temp_thres2);

                                    //If the N too small
                                    if(ctr_of_std < 3) {
                                        //System.out.println("Too less window to calculate SDNN thres");
                                        RIFV_SDNN_thres = 45;
                                    }

                                }
                                //RIFV_SDNN_thres = 45;

                                SDNN_thres_str = Double.toString(RIFV_SDNN_thres);
                                debugging_probe4 = SDNN_thres_str;

                                //Handle SDNN calculation if there aren't any 64s FFT window availible
                                if(ctr_of_std != 0) {
                                    SDNN = mean_RIFV_std;

                                    //BPM = (int)(60000/mean_RIFV_mean);
                                }

                                //debugging_probe5 = RIFV_str;
                                //debugging_probe6 = RIIV_str;
                                //debugging_probe7 = RIAV_str;

                                fft_window_SDNN_str = fire_RIFV_SDNN_str ;

                                fft_window_ROI_lower_Idx_str = fire_ROI_lower_idx_str;


                                fft_window_RIFV_bpm_str = fire_RIFV_bpm_str ;
                                fft_window_RIIV_bpm_str = fire_RIIV_bpm_str ;
                                fft_window_RIAV_bpm_str = fire_RIAV_bpm_str ;

                                skip_window_str = fire_skip_window_str ;

                                RIFV_power_each_window_thres_str = fire_RIFV_power_each_window_thres_str;
                                RIAV_power_each_window_thres_str = fire_RIAV_power_each_window_thres_str;

                                //Merge the Breathing rates into one array to calculate mean and std.
                                int total_BR_num = (RIAV_brs_ctr+RIIV_brs_ctr+RIFV_brs_ctr);
                                double[] merged_BR_arr = new double[total_BR_num];
                                int merged_BR_arr_ctr = 0;

                                for(int t=0; t<RIAV_brs_ctr; t++) {

                                    merged_BR_arr[merged_BR_arr_ctr] = RIAV_brs[t];
                                    merged_BR_arr_ctr++;
                                }


                                for(int t=0; t<RIIV_brs_ctr; t++) {

                                    merged_BR_arr[merged_BR_arr_ctr] = RIIV_brs[t];
                                    merged_BR_arr_ctr++;
                                }

                                for(int t=0; t<RIFV_brs_ctr; t++) {

                                    merged_BR_arr[merged_BR_arr_ctr] = RIFV_brs[t];
                                    merged_BR_arr_ctr++;
                                }

                                double local_mean_BR = calculateMean(merged_BR_arr);
                                double local_std_BR = calculateStdev(merged_BR_arr);

                                //Exclude outlier
                                int acceptable_br_ctr = 0;
                                double sum_of_br = 0;
                                for(int p=0; p<RIAV_brs_ctr; p++) {
                                    if(Math.abs(RIAV_brs[p]-local_mean_BR)/local_std_BR > 3) continue;
                                    if(Math.abs(RIIV_brs[p]-local_mean_BR)/local_std_BR > 3) continue;
                                    if(Math.abs(RIFV_brs[p]-local_mean_BR)/local_std_BR > 3) continue;
                                    acceptable_br_ctr++;
                                    sum_of_br += (RIAV_brs[p]+RIIV_brs[p]+RIFV_brs[p])/3.0;
                                }

                                //Moving average the final breathing rate
                                double br=0;

                                if(val_ctr > 4) val_ctr=0;
                                if(RIAV_brs_ctr > 0 && RIIV_brs_ctr > 0 && RIFV_brs_ctr > 0) {

                                    if(windowStartIdx < 100) {
                                        breathRate = (double) (sum_of_br/ (double) acceptable_br_ctr);
                                        last_five_BR[val_ctr++] = breathRate;
                                    }else{

                                        last_five_BR[val_ctr] = sum_of_br / (double) acceptable_br_ctr;
                                        breathRate = calculateMean(last_five_BR);
                                        last_five_BR[val_ctr++] = breathRate;

                                    }
                                }




                                double FRF = breathRate/60;

                                //debugging_probe2 = Double.toString(FRF);

                                double[] rr_list = non_zero_freq;

                                double[] rr_x = new double[rr_list.length];
                                double sum = 0;
                                for(int i=0; i<rr_list.length; i++) {
                                    sum += rr_list[i];
                                    rr_x[i] = sum;
                                }



                                double[] rr_x_new  = linspace((int)rr_x[0], (int)rr_x[rr_x.length-1], (int)rr_x[rr_x.length-1]);
                                int datalen = rr_x_new.length;
                                //for(int i=0; i<10; i++)System.out.println("rr_x_new:"+rr_x_new[i]);

                                UnivariateInterpolator splineIinterpolator_RR = new SplineInterpolator();
                                UnivariateFunction calibrantInterpolator_RR = splineIinterpolator_RR.interpolate(rr_x, rr_list);

                                double[] rr_interp = new double[rr_x_new.length];
                                for (int i=0; i<rr_x_new.length; i++) {
                                    rr_interp[i] = calibrantInterpolator_RR.value(rr_x_new[i]);
                                }

                                //for(int i=rr_interp.length-10; i<rr_interp.length-1; i++)System.out.println("rr_interp:"+rr_interp[i]);

                                Hanning Han_window = new Hanning(rr_x_new.length);
                                double[] Han_window_d = Han_window.getWindow();



                                double[] windowed_data = new double[rr_x_new.length];

                                for(int i=0; i<rr_x_new.length; i++)windowed_data[i] = rr_interp[i]*Han_window_d[i];

                                int Fs = 1000;

                                FastFourier fft_result = new FastFourier(windowed_data);

                                fft_result.transform();
                                Complex[] fft_com = fft_result.getComplex(false);
                                //System.out.println("fft_result.length:"+fft_com.length);
                                int nfft = fft_com.length;
                                //System.out.println("nfft:"+nfft);

                                double[] fft_abs_square = new double[fft_com.length];
                                for(int i=0; i<fft_abs_square.length; i++) {
                                    fft_abs_square[i] = Math.pow(fft_com[i].abs(),2);
                                }
                                //for(int i=0; i<10; i++)System.out.println("fft_abs_square[i]:"+fft_abs_square[i]);

                                double factor = 0;
                                for(int i=0; i<Han_window_d.length; i++)factor+=Math.pow(Han_window_d[i],2);

                                for(int i=0; i<fft_abs_square.length; i++) {
                                    fft_abs_square[i] /= factor;
                                }

                                int numPts = nfft/2+1;
                                double[] mx = Arrays.copyOfRange(fft_abs_square, 0, numPts-1);


                                //System.out.println("mx.length:"+mx.length);
                                double[] Pxx = new double[mx.length];
                                for(int i=1; i<mx.length; i++) {
                                    mx[i] *= 2;
                                    Pxx[i] = mx[i]/ 1000;
                                }
                                //System.out.println("numPts-1:"+(numPts-1));
                                double[] Fx = linspace(0.0, (double)(numPts-1)*Fs/nfft, (numPts-1));

                                double lf_lower = 0.04;
                                double lf_upper = 0.15;
                                double hf_lower = FRF*0.65;
                                double hf_upper = FRF*1.35;

                                if(FRF>=0.13){
                                    hf_lower = 0.15;
                                    hf_upper = 0.4;
                                }
                                //double hf_lower = 0.15;
                                //double hf_upper = 0.4;

                                int lf_lower_idx = 0;
                                int lf_upper_idx = 0;
                                int hf_lower_idx = 0;
                                int hf_upper_idx = 0;

                                for(int i=0; i<Fx.length; i++) {
                                    if(Fx[i] < lf_lower)lf_lower_idx = i+1;
                                    if(Fx[i] < lf_upper)lf_upper_idx = i+1;
                                    if(Fx[i] < hf_lower)hf_lower_idx = i+1;
                                    if(Fx[i] < hf_upper)hf_upper_idx = i+1;
                                }

                                double[] lf_range_Fx2 = Arrays.copyOfRange(Fx,lf_lower_idx,lf_upper_idx);
                                double[] lf_range_Pxx2 = Arrays.copyOfRange(Pxx,lf_lower_idx,lf_upper_idx);
                                //debugging_probe3 = Double.toString(lf_range_Pxx2[0]);
                                double lf = trapzIntegral(lf_range_Pxx2,Fx[1]);

                                double[] hf_range_Fx2 = Arrays.copyOfRange(Fx,hf_lower_idx,hf_upper_idx);
                                double[] hf_range_Pxx2 = Arrays.copyOfRange(Pxx,hf_lower_idx,hf_upper_idx);
                                //for(int k=0; k<hf_range_Pxx2.length; k++)System.out.println("hf_range_Pxx2[k]:"+hf_range_Pxx2[k]);
                                HF = trapzIntegral(hf_range_Pxx2,Fx[1]);

                                //double lf_hf = LF/HF;

                                //System.out.println("Fx[1]:"+Fx[1]);

                                if( !(containArtimorethan1 || containArtimorethan2 && (artifact_arrlst1.size() > 0 || artifact_arrlst2.size() > 0))) {
                                    HF = trapzIntegral(hf_range_Pxx2,Fx[1]);
                                    BPM = (double)(major_pks_arrlst.size()-1)*60*ppg_fs*upsample_factor/(major_pks_arrlst.get(major_pks_arrlst.size()-1).getX2()- major_pks_arrlst.get(0).getX2());
                                }


                                //BPM = 60000/calculateMean(non_zero_freq);

                            }
                        }catch(Exception e){

                        }

                        if(endPointer > windowEndIdx){
                            windowStartIdx += 25;
                            windowEndIdx += 25;
                        }




                    }
                }
            }
        };
    }

    // 設定 傳送至 PPG 模組的指令
    private void StartBtn_Click(LayoutInflater inflater){
        byte[] Data = new byte[1];

        if(Preview_Flag == true)
            Data[0] = 0x30;
        else {

            byte base = 0x30;
            Data[0] = (byte)(base+PPGTime);

        }
        writeToBle(Data);
        notifyBle();
//        serialPort.write(Data);
    }

    // 將收到的 Serial 資料存入 Queue
    private void PushSerialData(byte[] data,int size)
    {
        for(int i = 0 ; i < size ; i++) {
            if(Queue_Index_Front == (SerialDataSize - 1))
                Queue_Index_Front = 0;
            SerialData_Queue[Queue_Index_Rear++] = data[i];
        }
    }

    // 將 Queue 裡的資料 pop出來
    private byte PopSerialData()
    {
        if(Queue_Index_Front == (SerialDataSize - 1))
            Queue_Index_Front = 0;
        return SerialData_Queue[Queue_Index_Front++];
    }

    // 處理資料並更新圖片
    private void handleData(byte[] buf,int size,LayoutInflater inflater){
        PushSerialData(buf,size);
        UpdateGraph(size,inflater);

    }

    // 合併每 2Bytes 的資料
    private void AppedSeriesData(int size,LayoutInflater inflater)
    {
        double queueData[][] = new double[1][2];
        double queueData2[][] = new double[1][2];
        if(size % 2 != 0){
            TempSize[SizeIndex] = size;
        }

        //Log.d("TAG", "data: " + data);
        //Log.d("TAG", "data2: " + data2);

        // G_Series_para.appendData(new DataPoint(dataPointCtr++,SDNN), true, 400);
        if(SizeIndex > 0 && (TempSize[0] + size) % 2 != 0){
            for(int i = 0; i < (size + TempSize[0] - 1) / 2; i++) {
                data = (int)(PopSerialData() << 8); //左方8個bit
                data2 =  (int)(PopSerialData());    //右方8個bit

                if(data2 < 0)
                    data2 += 256;




                G_Series.appendData(new DataPoint(mXPoint++,(data + data2)), true, 400);
                queueData[0][0] = (data + data2) ;
                queueData[0][1] = 0.0;

                dataQ.Qpush(queueData);


                dataPointCount++;
            }
            SizeIndex = 0;
            TempSize[0] = 1;
            TempSize[1] = 0;
        }
        else if(SizeIndex > 0 && (TempSize[0] + size) % 2 == 0){
            for(int i = 0; i < (size + TempSize[0]) / 2; i++) {
                data = (int)(PopSerialData() << 8);
                data2 =  (int)(PopSerialData());

                if(data2 < 0)
                    data2 += 256;



                G_Series.appendData(new DataPoint(mXPoint++,(data + data2)), true, 400);
                queueData[0][0] = (data + data2) ;
                queueData[0][1] = 0.0;

                dataQ.Qpush(queueData);

                dataPointCount++;
            }
            SizeIndex = -1;
            TempSize[0] = 0;
            TempSize[1] = 0;
        }
        else if(size % 2 == 0){
            for(int i = 0; i < size / 2; i++) {
                data = (int)(PopSerialData() << 8);
                data2 =  (int)(PopSerialData());

                if(data2 < 0)
                    data2 += 256;




                G_Series.appendData(new DataPoint(mXPoint++,(data + data2)), true, 400);
                queueData[0][0] = (data + data2) ;
                queueData[0][1] = 0.0;

                dataQ.Qpush(queueData);

                dataPointCount++;
            }
        }


        handleInputData();
        timestampQ.push((Long) System.currentTimeMillis());
        SizeIndex++;
    }

    // 將收到的 Serial 資料 進行畫圖呈現
    private void UpdateGraph(final int size, final LayoutInflater inflater){
        if(!abortTraining){
            G_Graph.post(new Runnable() {
                @Override
                public void run() {
                    AppedSeriesData(size, LInflater);
                    G_Graph.getViewport().setMaxX(mXPoint);
                    //G_Graph.getViewport().setMinX(0);
                    G_Graph.getViewport().setMinX(mXPoint - (Scale*3));
                    //mXPoint += 1;

                    Time_GET = (mXPoint / 25 - Min_Time_Flag)%60;

                    if(mXPoint >= 25) {
                        if (mXPoint % 1500 == 0) {
                            Min_Time_GET = (int)((double)mXPoint/1500);
                            Min_Time_Flag = (Min_Time_GET*60);
                        }
                    }
                    if(mXPoint >= (PPGTime * 1500)) {
                        Stop_Flag = true;
                    }
                    Message uiMessage = mHandler.obtainMessage(1, appData);
                    uiMessage.sendToTarget();
                }
            });
        }
    }

    // 所有變數進行重設
    private void VarReset()
    {
        inhale_ctr = 0;
        exhale_ctr = 0;

        data = 0;
        data2 = 0;
        //voice_ctr = 0;


        error_flag = false;
        arti_detected = false;
        sig_small = false;
        sig_big = false;
        sdf2 = new SimpleDateFormat("MM_dd HH_mm_ss");
        current2 = new Date();
        //LF_HF = 0;
        LF = 0;
        HF = 0;
        //breathRate = 0;
        SDNN = 0;

        RIFV_normal_slow_thres = 0;
        RIFV_normal_slow_thres_HF_LF = 0;

        last_five_val_HF = new double[10][5];
        last_five_val_HF_ctr = 0;
        last_five_val_LF = new double[10][5];
        last_five_val_LF_ctr = 0;
        /*
        selectedPara = "Breathing rate";
        init_val = 6;
        target_val = 4;
        feedback_time = 10;
        trigger_val = (init_val - target_val)/feedback_time;
         */
        SDNNtotalvals = new double[(int)window_time*10];
        SDNNtotalvals_ctr = 0;

        RIFV_std = 0;

        SDNN_normal_ctr = 0;


        RIIV_Arr_normal_ctr = 0;
        RIIV_Arr_normal = new double[180];

        RIAV_Arr_normal_ctr = 0;
        RIAV_Arr_normal = new double[180];

        RIFV_Arr_normal_ctr = 0;
        RIFV_Arr_normal = new double[180];

        RIFV_normal_std_ctr = 0;
        RIFV_normal_std_arr = new double[180];

        RIFV_normal_std_HF_LF_ctr = 0;
        RIFV_normal_std_HF_LF_arr = new double[180];

        RIFV_power_thres = 0;
        RIAV_power_thres = 0;

        RIFV_SDNN_thres = 0;
        RIFV_normal_slow_power_thres = 0;
        RIIV_normal_slow_thres = 0;
        RIAV_normal_slow_thres = 0;

        brSDNNCtr = 0;
        br_SDNN = 0;
        timeStampsCtr = 0;
        Stop_Flag = false;
        ptr = 0;
        dataPointCtr = 0;
        tempMax = 0;
        windowStartIdx = 0;
        windowEndIdx = (int)sliding_window_size;
        feedbackBase = 10;
        first_window = true;
        numOfsubwin = 8;

        //feedbackBase = 15;
        setButtonEnable(true);
        timestampQ = null;
        timestampQ = new Stack<Long>();
        dataQ = null;
        dataQ = new DoubleTwoDimQueue();
        dataQ2 = null;
        dataQ2 = new DoubleTwoDimQueue();
        HRDataSeries = null;
        HRDataSeries = new DoubleTwoDimQueue();
        SDNNDataSeries = null;
        SDNNDataSeries = new DoubleTwoDimQueue();
        HRDataSeries = null;
        HRDataSeries = new DoubleTwoDimQueue();
        SDNNDataSeries = null;
        SDNNDataSeries = new DoubleTwoDimQueue();
        HFDataSeries = null;
        HFDataSeries = new DoubleTwoDimQueue();
        //LF_HFDataSeries = null;
        //LF_HFDataSeries = new DoubleTwoDimQueue();
        BRDataSeries = null;
        BRDataSeries = new DoubleTwoDimQueue();
        probeDataSeries = null;
        probeDataSeries = new DoubleTwoDimQueue();
        probeaDataSeries = null;
        probeaDataSeries = new DoubleTwoDimQueue();
        probebDataSeries = null;
        probebDataSeries = new DoubleTwoDimQueue();
        probecDataSeries = null;
        probecDataSeries = new DoubleTwoDimQueue();
        probedDataSeries = null;
        probedDataSeries = new DoubleTwoDimQueue();
        probeeDataSeries = null;
        probeeDataSeries = new DoubleTwoDimQueue();
        probefDataSeries = null;
        probefDataSeries = new DoubleTwoDimQueue();
        probegDataSeries = null;
        probegDataSeries = new DoubleTwoDimQueue();
        start_cal = false;
        sliding_window_enable = false;
        startPointer = 0;
        endPointer = 0;
        recievedDataPoint = 1024;
        dataPointCount = 0;
        slidingWindowIsFull = true;
        keep_thread_running = false;
        Preview_Flag = false;
        BPM = 0;
        fillWindowPhase = 0;
        //Minute_tv.setText("0");
        time_tv.setText("0");
        //ParaVal_tv.setText("0.0");
        Log.d("TAG", "varreset! " );

    }

    // 將畫圖 初始化


    private void ResetGraph()
    {
        G_Graph.getViewport().setMaxX(5);
        //G_Graph.getViewport().setMaxY(255);
        G_Graph.getViewport().setMaxY(1000);
        G_Graph.getViewport().setMinY(200);
        //G_Graph.getViewport().setMinY(20);
        G_Graph.getViewport().setYAxisBoundsManual(true);
        G_Graph.getViewport().setMinX(0);
        G_Graph.getGridLabelRenderer().setHighlightZeroLines(false);
        G_Graph2.getGridLabelRenderer().setHighlightZeroLines(false);
//        G_Graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);
//        G_Graph.getGridLabelRenderer().setNumVerticalLabels(3);
//        G_Graph.getGridLabelRenderer().setPadding(15);
        G_Graph.getViewport().setXAxisBoundsManual(true);
        G_Graph.getGridLabelRenderer().reloadStyles();
        G_Graph2.getGridLabelRenderer().reloadStyles();
        G_Graph.removeAllSeries();
        G_Graph2.removeAllSeries();
        G_Series = new LineGraphSeries<DataPoint>();
        G_Series2 = new LineGraphSeries<DataPoint>();
        G_Graph.addSeries(G_Series);
        G_Graph2.addSeries(G_Series2);

        mXPoint = 0;
        mXPointPara = 170;
        Time_GET = 0;
        Min_Time_GET = 0;
        Min_Time_Flag = 0;

        Queue_Index_Rear = 0;
        Queue_Index_Front = 0;

        for(int i = 0 ; i < SerialData_Queue.length ; i++)
            SerialData_Queue[i] = 0;

        TempSize[0] = 0;
        TempSize[1] = 0;
        SizeIndex = 0;
    }

    private void menuDialog2()
    {

        menu_dialogView2 = View.inflate(LInflater.getContext(),R.layout.restart_menu,null);

        MenuDialog_Builder2 = new AlertDialog.Builder((Activity)LInflater.getContext())
                .setTitle("前3分鐘心率變化過大，請重新測量。")
                /*
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })

                 */
                .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        VarReset();
                        ResetGraph();
                        SerialFlag = false;
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(1);




                    }
                });
        MenuDialog2 = MenuDialog_Builder2.create();
        MenuDialog2.setView(menu_dialogView2);

    }



    // 參數設定
    private void menuDialog()
    {

        menu_dialogView = View.inflate(LInflater.getContext(),R.layout.menu,null);

        MenuDialog_Builder = new AlertDialog.Builder((Activity)LInflater.getContext())
                .setTitle("參數設定")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        /*
                        ParaName_tv.setText("Breathing rate:");
                        ParaUnit_tv.setText("BPM");
                        selectedPara = "Breathing rate";




                        final String[] parameter = {"SDNN","HF","Breathing rate"};
                        final String[] parameterUnit = {"ms","ms^2","BPM"};
                        AlertDialog.Builder dialog_list = new AlertDialog.Builder(LInflater.getContext());
                        dialog_list.setTitle("請選擇生物回饋參數");
                        dialog_list.setItems(parameter, new DialogInterface.OnClickListener(){
                            @Override

                            //只要你在onClick處理事件內，使用which參數，就可以知道按下陣列裡的哪一個了
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Auto-generated method stub
                                Toast.makeText(LInflater.getContext(), "您已選擇" + parameter[which], Toast.LENGTH_SHORT).show();

                                ParaName_tv.setText(parameter[which]+" :");
                                ParaUnit_tv.setText(parameterUnit[which]);
                                selectedPara = parameter[which];



                            }
                        });
                        dialog_list.show();



                        //default value must be recheck
                        switch (selectedPara){
                            case "HF":
                                init_val = 250;
                                target_val = 1000;
                                feedback_time = 10;
                                trigger_val = (target_val - init_val)/feedback_time;
                                break;
                            case "SDNN":
                                init_val = 45;
                                target_val = 60;
                                feedback_time = 10;
                                trigger_val = (target_val - init_val)/feedback_time;
                                break;
                            case "Breathing rate":
                                init_val = 6;
                                target_val = 3;
                                feedback_time = 10;
                                trigger_val = (init_val - target_val)/feedback_time;
                                break;
                        }

                        */


                        //TextView scale_tv = MenuDialog.findViewById(R.id.Scale_tv);
                        TextView ppgtime_tv = MenuDialog.findViewById(R.id.PPG_Time_tv);
                        TextView postRecordTime_tv = MenuDialog.findViewById(R.id.postRecord_time_tv);
                        EditText feedback_init_tv = MenuDialog.findViewById(R.id.feedback_init_tv);
                        EditText feedback_target_tv = MenuDialog.findViewById(R.id.feedback_target_tv);
                        TextView feedack_time_tv = MenuDialog.findViewById(R.id.feedack_time_tv);
                        TextView exhale_time_tv = MenuDialog.findViewById(R.id.exhale_time_tv);
                        TextView inhale_time_tv = MenuDialog.findViewById(R.id.inhale_time_tv);
                        SpannableString init_str = new SpannableString(Double.toString(init_val));
                        //feedback_init_tv.setHint(init_str);
                        SpannableString target_str = new SpannableString(Double.toString(target_val));
                        //feedback_target_tv.setHint(target_str);
                        //TextView triggerFeedbackVal_tv = MenuDialog.findViewById(R.id.triggerFeedbackVal_tv);
                        if(!feedback_init_tv.getText().toString().equals("")) {
                            init_val = Integer.parseInt(feedback_init_tv.getText().toString());
                            input_init_val = Integer.parseInt(feedback_init_tv.getText().toString());
                            // if (trigger_val > 100) trigger_val = 100;
                            // if (trigger_val < 5) trigger_val = 5;
                            // tvPara2.setText(Integer.toString(trigger_val));
                        }
                        if(!feedback_target_tv.getText().toString().equals("")) {
                            target_val = Integer.parseInt(feedback_target_tv.getText().toString());
                            // if (trigger_val > 100) trigger_val = 100;
                            // if (trigger_val < 5) trigger_val = 5;
                            // tvPara2.setText(Integer.toString(trigger_val));
                        }
                        if(!feedack_time_tv.getText().toString().equals("")) {
                            feedback_time = Integer.parseInt(feedack_time_tv.getText().toString());
                            // if (trigger_val > 100) trigger_val = 100;
                            // if (trigger_val < 5) trigger_val = 5;
                            // tvPara2.setText(Integer.toString(trigger_val));
                        }

                        if(!exhale_time_tv.getText().toString().equals("") && !inhale_time_tv.getText().toString().equals("")) {
                            exhale_time = Integer.parseInt(exhale_time_tv.getText().toString());
                            inhale_time = Integer.parseInt(inhale_time_tv.getText().toString());
                            // if (trigger_val > 100) trigger_val = 100;
                            // if (trigger_val < 5) trigger_val = 5;
                            // tvPara2.setText(Integer.toString(trigger_val));
                        }




                        trigger_val = Math.abs(target_val - init_val)/(feedback_time-1);
                        /*
                        if((init_val > 15 || init_val < 4) || (target_val > 15 || target_val < 4) || (init_val <= target_val)){
                            Toast.makeText(LInflater.getContext(), "輸入數值錯誤", Toast.LENGTH_SHORT).show();
                            init_val = 6;
                            target_val = 4;
                        }

                         */

                        String mes = "init:" + init_val + " targ:" + target_val+ " feed:" + feedback_time + " inh:"+ inhale_time + " exh:" + exhale_time;
                        Toast.makeText(LInflater.getContext(),mes  , Toast.LENGTH_SHORT).show();

                        if(feedback_time > 30 || feedback_time < 0) {
                            feedback_time = 5;
                            Toast.makeText(LInflater.getContext(), "輸入數值錯誤", Toast.LENGTH_SHORT).show();
                        }


                        //if(!scale_tv.getText().toString().equals(""))
                        //    Scale = Integer.parseInt(scale_tv.getText().toString());
                        if(!ppgtime_tv.getText().toString().equals("")) {
                            PPGTime = Integer.parseInt(ppgtime_tv.getText().toString());
                            if (PPGTime > 30) PPGTime = 30;
                            if (PPGTime < 1) PPGTime = 1;

                        }
                        //if(scale_tv.getText().toString().equals("") && ppgtime_tv.getText().toString().equals("")){
                        //    Scale = 150;
                        //   PPGTime = 1;
                        // }

                        if(!postRecordTime_tv.getText().toString().equals("")) {
                            postRecordPPGTime = Integer.parseInt(postRecordTime_tv.getText().toString());
                            post_start_mpoint = PPGTime*60*ppg_fs;

                            PPGTime += postRecordPPGTime;
                        }
                        finish_mpoint = (PPGTime*60*ppg_fs)-5;
                        ShowData.tTime = PPGTime;





                    }
                });
        MenuDialog = MenuDialog_Builder.create();
        MenuDialog.setView(menu_dialogView);

    }

    //完成訓練後資料上傳及參數重設
    private void finishAndUpload(){
        fileHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(Stop_Flag && mXPoint != 0) {
                    mXPoint = 0;
                    keep_thread_running = false;

                    new AlertDialog.Builder((Activity) LInflater.getContext()).setMessage("測量完畢，更新訓練紀錄!")
                            .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                            .create()
                            .show();

                    if(HFDataSeries.getQSize() > 0 && BRDataSeries.getQSize() > 0 && SDNNDataSeries.getQSize() > 0){
                        double[] hf_arr = HFDataSeries.toArray(0,HFDataSeries.getQSize()-1,0);
                        double[] br_arr = BRDataSeries.toArray(0,BRDataSeries.getQSize()-1,0);
                        double[] sdnn_arr = SDNNDataSeries.toArray(0,SDNNDataSeries.getQSize()-1,0);



                    ShowData.maxSDNN = Math.round(findMaxVal(sdnn_arr) * 100.0) / 100.0;
                    ShowData.minBR = Math.round(findMinVal(br_arr) * 100.0) / 100.0;
                    ShowData.maxHF = Math.round(findMaxVal(hf_arr) * 100.0) / 100.0;
                    arrayLen = (PPGTime*60*sampleRate)-1;

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    Date current = new Date();

                    String db_str = "sdnn[0]:" + Double.toString(sdnn_arr[0]);
                    //Log.d("TAG", db_str );
                    ShowData.SDNNData = sdnn_arr;
                    ShowData.HFData = hf_arr;
                    //ShowData.LF_HFData = LF_HFDataSeries.toArray(0,LF_HFDataSeries.getQSize()-1,0);
                    ShowData.BRData = br_arr;;
                    ShowData.HRData = HRDataSeries.toArray(0,HRDataSeries.getQSize()-1,0);
                    //upload_btn.setEnabled(true);
                     }
                    //uploadFirebase("arrayLen:"+arrayLen+','+usrName,"訓練日期: "+sdf.format(current));

                    //Toast.makeText(LInflater.getContext(), "資料已上傳Firebase" , Toast.LENGTH_SHORT).show();
                    VarReset();
                    //time_tv.setText(Integer.toString(0));
                }
                fileHandler.postDelayed(this, 1000);
            }
        },1000);
    }

    static class Artifact{
        private int start_loc;
        private int end_loc;
        public Artifact(int start, int end) {
            start_loc = start;
            end_loc = end;
        }
        public int getStart() {
            return start_loc;
        }
        public int getEnd() {
            return end_loc;
        }
    }

    static class Line_t{
        private double p1_x;
        private double p1_y;
        private double p2_x;
        private double p2_y;
        public Line_t(double x1, double y1, double x2, double y2) {
            p1_x = x1;
            p1_y = y1;
            p2_x = x2;
            p2_y = y2;
        }
        public double getX1() {
            return p1_x;
        }
        public double getY1() {
            return p1_y;
        }
        public double getX2() {
            return p2_x;
        }
        public double getY2() {
            return p2_y;
        }
        public void setX1(double val) {
            p1_x = val;
        }
        public void setY1(double val) {
            p1_y = val;
        }
        public void setX2(double val) {
            p2_x = val;
        }
        public void setY2(double val) {
            p2_y = val;
        }
    }

    public static double[] scale(double[] arr, double lower, double upper) {
        double[] scaled = new double[arr.length];
        double range_bound = upper-lower;

        double min = findMin(arr);
        double range_data = findMax(arr) - min;
        for(int i=0; i<arr.length; i++) scaled[i] = range_bound*(((arr[i]-min)/range_data) + lower);
        return scaled;
    }

    public static double findMax(double[] arr){
        double mean = calculateMean(arr);
        double std = calculateStdev(arr);
        double tempVal = -1000000;
        for(int i = 0; i<arr.length; i++) {
            if(arr[i] > tempVal && Math.abs(arr[i]-mean)/std<2) {
                tempVal = arr[i];

            }
        }
        return tempVal;
    }

    public static double findMin(double[] arr){
        double mean = calculateMean(arr);
        double std = calculateStdev(arr);
        double tempVal = 1000000;
        for(int i = 0; i<arr.length; i++) {
            if(arr[i] < tempVal && Math.abs(arr[i]-mean)/std<2) {
                tempVal = arr[i];

            }
        }
        return tempVal;
    }

    public static double arrSum(double[] arr){
        double sum = 0;
        for(int i = 0; i<arr.length; i++) {
            sum += arr[i];
        }
        return sum;
    }

    public static int findMaxIndexInHalfArr(double[] arr, int startIdx, int endIdx){
        double tempVal = 0;
        int tempIdx = 0;

        for(int i = startIdx; i<endIdx; i++) {
            if(arr[i] > tempVal) {
                tempVal = arr[i];
                tempIdx = i;
            }
        }

        return tempIdx;
    }

    public static int find2PeakInRange(double[] arr, int startIdx, int endIdx){
        double tempVal = 0;
        int tempIdx = 0;

        //Max
        for(int i = startIdx; i<endIdx; i++) {
            if(arr[i] > tempVal) {
                tempVal = arr[i];
                tempIdx = i;
            }
        }

        //Second
        double secondVal = 0;
        int secondtempIdx = 0;

        for(int i = startIdx; i<endIdx; i++) {
            if(arr[i] > secondVal && tempVal>secondVal) {
                secondVal = arr[i];
                secondtempIdx = i;
            }else if(tempVal == secondVal && secondtempIdx != tempIdx) {
                secondVal = arr[i];
                secondtempIdx = i;
            }
        }

        return tempIdx;
    }

    public static double[] findMaxIndexAmpInHalfArr(double[] arr, int startIdx, int endIdx){
        double tempVal = 0;
        int tempIdx = 0;

        for(int i = startIdx; i<endIdx; i++) {
            if(arr[i] > tempVal) {
                tempVal = arr[i];
                tempIdx = i;
            }
        }
        double[] max = new double[2];
        max[0] = tempIdx;
        max[1] = tempVal;
        return max;
    }



    public static double calculateMean(double numArray[]){
        double sum = 0.0;
        int length = numArray.length;
        for(double num : numArray) {
            sum += num;
        }
        double mean = sum/length;
        return mean;
    } //tested

    public static double calculateStdev(double numArray[]){
        double sum = 0.0, standardDeviation = 0.0;
        int length = numArray.length;
        double mean = calculateMean(numArray);
        for(double num: numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }
        return Math.sqrt(standardDeviation/(length-1));
    } //tested



    static double[] maxValue(double[] arr) {
        double max = 0;
        double index = 0;
        for (int ktr = 0; ktr < arr.length; ktr++) {
            if (arr[ktr] > max) {
                max = arr[ktr];
                index = ktr;
            }
        }
        double[] result = {max, index};
        return result;

    } //tested



    static double[] findDiff(double[] Arr) {
        double diff;
        double[] tempArr = new double[Arr.length-1];
        for(int i = 0; i < Arr.length - 1; i++ ){
            diff = (Arr[i+1] - Arr[i]);
            tempArr[i] = diff;
        }
        return tempArr;
    } //tested

    static double[] findPeakTroughDiff(double[][] Arr) {
        double diff;
        double[] tempArr = new double[Arr[0].length-1];
        for(int i = 0; i < Arr[0].length-1; i++ ){
            if((Arr[2][i] == 0 || Arr[2][i] == 1) && (Arr[2][i+1] == 0 || Arr[2][i+1] == 1)) {
                diff = Math.abs(Arr[1][i+1] - Arr[1][i]);

            }else {
                if((Arr[2][i] == 0 || Arr[2][i] == 1) && (Arr[2][i+1] == -1)) {
                    int counter = 0;
                    while(Arr[2][i+counter+1] == -1) {

                        if(i+counter+1 < Arr[2].length-1) {
                            counter++;

                        }else {
                            break;
                        }
                    }
                    diff = Math.abs(Arr[1][i+counter+1] - Arr[1][i]);

                }else {
                    diff = Double.MAX_VALUE;
                }
            }
            tempArr[i] = diff;
        }

        return tempArr;
    }

    static double[] sampleToTime(double[] Arr, double sampleRate) {
        double[] tempArr = new double[Arr.length];
        for(int i = 0; i < Arr.length ; i++ ){
            tempArr[i] = Arr[i]*1000/sampleRate;
        }
        return tempArr;
    } //tested


    public static double median(double[] arr) {
        Arrays.sort(arr);
        int middle = (int)(arr.length/2);
        if (arr.length%2 == 1) {
            return arr[middle];
        } else {
            return (arr[middle-1] +arr[middle]) / 2.0;
        }
    } //tested

    public static double[] pruneZero(double[] arr) {
        for(int i=arr.length-1; i>0; i--) {
            if(arr[i]!=0) {
                double[] nonZero = Arrays.copyOfRange(arr, 0, i+1);
                return nonZero;
            }

        }
        return arr;
    } //tested


    public static double[] removeDC(double[] arr) {
        double meanAmp = calculateMean(arr);
        double[] arrMax = maxValue(arr);
        for(int i=0; i<arr.length; i++) {
            double temp = arr[i];
            temp = Math.abs(temp - meanAmp);
            arr[i] = temp/arrMax[0]+temp;
        }
        return arr;
    }

    public static double[] zeroPadding(double[] arr) {
        double len = arr.length;
        int nextPow = (int)Math.ceil(Math.log(len) / Math.log(2));
        double[] newArr = new double[(int)Math.pow(2,nextPow)];

        for(int i=0; i<arr.length; i++) {
            newArr[i] = arr[i];
        }
        return newArr;
    } //tested

    public static double[] calPSD(Complex[] arr, double sampleRate) {
        double[] PSDResult = new double[arr.length/2];
        for(int i=0; i<arr.length/2; i++) {
            PSDResult[i] = Math.pow(arr[i].abs(),2)*2/(arr.length*sampleRate);
        }
        return PSDResult;
    }
    static double[] binNumToFreqUnit(double arrLen, double sampleRate) {
        double sampleSpacing = 1/sampleRate;
        int n;
        if(arrLen%2 == 0) {
            n =(int)arrLen/2;
        }else {
            n =(int)(arrLen+1)/2;
        }
        double[] arr = new double[n];
        for(int i=0; i<arr.length; i++) {
            arr[i] = i/(arrLen*sampleSpacing);
        }
        return arr;
    } //TESTED

    static double[] getFreqRangePSD(double[] amp, double[] freq, double lowerBound, double upperBound) {
        int lowerPointer=0;
        int upperPointer=0;
        for(int i=0; i<freq.length; i++) {
            if(freq[i]<lowerBound) {
                lowerPointer = i;
            }
            if(freq[i]<upperBound) {
                upperPointer = i;
            }
        }
        return Arrays.copyOfRange(amp, lowerPointer, upperPointer);
    }

    static double trapzIntegral(double[] amp, double unit) {
        double integral = 0;
        for(int i=0; i<amp.length-1; i++) {
            integral = integral + ((amp[i]+amp[i+1])*unit/2);
        }
        return integral;
    } // tested

    public static double[] linspace(double min, double max, int points) {
        double[] d = new double[points];
        for (int i = 0; i < points; i++){
            d[i] = min + i * (max - min) / (points - 1);
        }
        return d;
    } //tested

    public static double[] cutTailToPowerOfTwo(double[] arr) {
        double len = arr.length;
        int nextPow = (int)Math.pow(2, Math.floor(Math.log(len) / Math.log(2)));

        return Arrays.copyOfRange(arr, 0, nextPow);
    } //tested

    public static double[] cutHead(double[] arr,double val) {
        for(int i=0; i<arr.length; i++) {
            if(arr[i]>val) {
                return Arrays.copyOfRange(arr, i, arr.length);
            }
        }
        return arr;
    } //tested

    public static double[] peakRemove(double[] arr,double lower, double upper) {
        double[] temp = new double[arr.length];
        int counter = 0;
        for(int i=0; i<arr.length; i++) {
            if(lower>arr[i] || arr[i]>upper) {
                temp[counter] = arr[i];
                counter++;
            }
        }
        return pruneZero(temp);
    }

    public static double[] calIntervals(double[] arr,double[] lower, double[] upper) {
        int row = 0;
        int counter = 0;
        double[] temp = new double[arr.length];
        for(int i=0; i<arr.length-1; i++) {
            if(lower[row]>arr[i] && arr[i+1]>upper[row]) {
                if(row<lower.length-1) {
                    row = row+1;
                }
            }else {
                temp[counter] = (arr[i+1]-arr[i]);
                if(temp[counter]>1000) {

                }
                counter++;
            }
        }
        return pruneZero(temp);
    }

    //Return the index of local maxima
    public static double[] findMaxima(double[] arr) {
        int counter = 0;
        double[] temp = new double[arr.length];
        for(int i=1; i<arr.length-1; i++) {
            if(arr[i-1]<arr[i] && arr[i]>arr[i+1]) {
                temp[counter] = i;

                counter++;
            }
        }

        return pruneZero(temp);
    }

    public static double[] findMinima(double[] arr) {
        int counter = 0;
        double[] temp = new double[arr.length];
        for(int i=1; i<arr.length-1; i++) {
            if(arr[i-1]>arr[i] && arr[i]<arr[i+1]) {
                temp[counter] = i;
                counter++;
            }
        }
        return pruneZero(temp);
    }

    public static double thirdQuartile(double[] arr) {
        double[] tempArr = Arrays.copyOfRange(arr, 0, arr.length);
        Arrays.sort(tempArr);
        int halfLength = (int)(tempArr.length/2);
        if(tempArr.length % 2 == 0) {
            return median(Arrays.copyOfRange(tempArr, halfLength, tempArr.length));
        }else {
            return median(Arrays.copyOfRange(tempArr, halfLength+1, tempArr.length));
        }

    }//tested

    public static double[] vertDiffofSubsequentExtrema(double[] arr1, double[] arr2, double[] arr3) {
        double[] diff = new double[arr1.length+arr2.length];
        int counter = 0;

        for(int i=0; i<arr1.length; i++) {
            try {
                diff[counter] = Math.abs(arr3[(int)arr2[i]] - arr3[(int)arr1[i]]);
                counter++;
                if(i<arr1.length-1) {
                    diff[counter] = Math.abs(arr3[(int)arr1[i+1]] - arr3[(int)arr2[i]]);
                    counter++;
                }
            }catch(ArrayIndexOutOfBoundsException ex){
                return pruneZero(diff);
            }


        }
        return pruneZero(diff);
    }//tested

    public static int indexOfSmallest(double[] array){
        // add this
        if (array.length == 0)
            return -1;

        int index = 0;
        double min = array[index];

        for (int i = 1; i < array.length; i++){
            if (array[i] <= min){
                min = array[i];
                index = i;
            }
        }
        return index;
    }

    public static double[] removeEleInArr(double[] arr, int index){
        for(int i = index; i<arr.length-1; i++) {
            arr[i] = arr[i+1];
        }
        arr[arr.length-1] = 0;
        return pruneZero(arr);
    }


    public static double[] zeroPadding(double[] arr, int num){
        double[] newArr = new double[num];
        Arrays.fill(newArr,0);
        for(int i = 0; i<arr.length; i++) {
            newArr[i] = arr[i];
        }

        return newArr;
    }

    public static double[] removeLeadingSignal(double[] arr){


        for(int i = 1; i<arr.length; i++) {
            if(arr[i-1]<arr[i] && arr[i]>arr[i+1]) {
                return Arrays.copyOfRange(arr, i+1, arr.length-1);
            }
        }

        return arr;
    }

    public static int findMaxIndex(double[] arr){
        double tempVal = 0;
        int tempIdx = 0;

        for(int i = 0; i<arr.length; i++) {
            if(arr[i] > tempVal) {
                tempVal = arr[i];
                tempIdx = i;
            }
        }

        return tempIdx;
    }

    public static double findMinVal(double[] arr){
        double tempVal = arr[0];
        for(int i = 1; i<arr.length; i++) {
            if(arr[i] < tempVal) {
                tempVal = arr[i];
            }
        }
        return tempVal;
    }

    public static double findMaxVal(double[] arr){
        double tempVal = arr[0];
        for(int i = 1; i<arr.length; i++) {
            if(arr[i] > tempVal) {
                tempVal = arr[i];
            }
        }
        return tempVal;
    }

    public static double[] shift(double[] arr, double val) {
        for(int i = 0; i<arr.length; i++) {
            arr[i] = arr[i] + val;
        }
        return arr;
    }

    public static double[] frontZeroPadding(double[] arr, int zeroNum) {
        double[] pad_arr = new double[arr.length+zeroNum];

        for(int i = 0; i<pad_arr.length; i++) {
            if(i<zeroNum) pad_arr[i] = 0;
            else pad_arr[i] = arr[i-zeroNum];
        }
        return pad_arr;
    }

    public static double[] zeroPad(double[] arr, int zeroNum) {
        double[] pad_arr = new double[arr.length+zeroNum];

        for(int i = 0; i<pad_arr.length; i++) {
            if(i<arr.length) pad_arr[i] = arr[i];
            else pad_arr[i] = 0;
        }
        return pad_arr;
    }

    private void setScanRule() {
        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
//                .setDeviceMac("1C:BA:8C:1D:31:D5")                  // 只扫描指定mac的设备，可选
                .setScanTimeOut(10000)              // 扫描超时时间，可选，默认10秒
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);
    }
    private void startScan() {
        setScanRule();
        // Example usage of MyBleManager's public method
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                button_paired.setEnabled(false); // 禁用按钮
                button_paired.setText("搜尋中~~~"); // 设置按钮的新文字
                Log.d("onScanStarted", "成功");
            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
//                Log.d("onLeScan", bleDevice.getName());
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
//                Log.d("onScanning", bleDevice.getName());
                deviceName.add(bleDevice.getName() + "  " +bleDevice.getMac());
                deviceId.add(bleDevice.getMac());
                event_listView.setAdapter(deviceName);
                ble_status.setText("請等待掃描完畢後再點擊藍芽已連接");
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                bleDeviceList = scanResultList;
//                event_listView.setAdapter(deviceName);
                event_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> partent, View view, int position, long id) {
                        String choseDevice = deviceId.getItem(position);
                        for (BleDevice bleDevice : bleDeviceList) {
                            if (bleDevice.getMac().equals(choseDevice)) {
                                nowBleDevice = bleDevice;
                                connect(nowBleDevice);
                                break;
                            }
                        }
                        Toast.makeText((Activity)LInflater.getContext(), "選擇了:" + choseDevice, Toast.LENGTH_SHORT).show();
                        deviceName.clear();
                        deviceId.clear();
                    }
                });
                ble_status.setText("藍芽等待連接");
                button_paired.setEnabled(true); // 禁用按钮
                button_paired.setText("重新搜尋"); // 设置按钮的新文字
                Log.d("onScanFinished", "onScanFinished");
            }
        });
    }
    private void connect(final BleDevice bleDevice) {
        BleManager.getInstance().connect(bleDevice, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                button_paired.setEnabled(false); // 禁用按钮
                button_paired.setText("開始連接"); // 设置按钮的新文字
                Log.d("onStartConnect", "onStartConnect");
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                button_paired.setEnabled(true); // 禁用按钮
                button_paired.setText("搜尋藍芽"); // 设置按钮的新文字
                ble_status.setText("藍芽連接失敗");
                SerialFlag=false;
                setButtonEnable(SerialFlag);
                Toast.makeText((Activity)LInflater.getContext(), "藍芽連接失敗", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                button_paired.setEnabled(false); // 禁用按钮
                button_paired.setText("已成功連接" + bleDevice.getName() + ' ' + bleDevice.getMac()); // 设置按钮的新文字
                ble_status.setText("已連接" + bleDevice.getName() + ' ' + bleDevice.getMac());
                SerialFlag=true;
                setButtonEnable(SerialFlag);
                Log.d("onConnectSuccess", "onConnectSuccess");
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                if (isActiveDisConnected) {
                    Toast.makeText((Activity)LInflater.getContext(), "active_disconnected", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText((Activity)LInflater.getContext(), "disconnected", Toast.LENGTH_LONG).show();
                }
                button_paired.setEnabled(true); // 禁用按钮
                button_paired.setText("搜尋藍芽"); // 设置按钮的新文字
            }
        });
    }

    private void notifyBle(){
        String uuid_service = "0000dfb0-0000-1000-8000-00805f9b34fb";
        String uuid_characteristic_notify = "0000dfb1-0000-1000-8000-00805f9b34fb";
        BleManager.getInstance().notify(
                nowBleDevice,
                uuid_service,
                uuid_characteristic_notify,
                new BleNotifyCallback() {
                    @Override
                    public void onNotifySuccess() {
                        // 打开通知操作成功
                        Log.d("notify", "Success");
                        Toast.makeText((Activity)LInflater.getContext(), "開始接收藍芽資料", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onNotifyFailure(BleException exception) {
                        Log.d("notify", "Failure");
                        // 打开通知操作失败
//                        Toast.makeText((Activity)LInflater.getContext(), "接收藍芽資料失敗", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCharacteristicChanged(byte[] data) {
                        // 打开通知后，设备发过来的数据将在这里出现
//                        Log.d("data", String.valueOf(data));
                        handleData(data, data.length,LInflater);
                    }
                });
    }

    private void writeToBle(byte[] Data){
        String uuid_service = "0000dfb0-0000-1000-8000-00805f9b34fb";
        String uuid_characteristic_write = "0000dfb1-0000-1000-8000-00805f9b34fb";
        BleManager.getInstance().write(
                nowBleDevice,
                uuid_service,
                uuid_characteristic_write,
                Data,
                new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {
                        Log.d("onWriteSuccess", "current: " + String.valueOf(current) + "total: " + String.valueOf(total) + "justWrite: " + justWrite);
                    }

                    @Override
                    public void onWriteFailure(BleException exception) {
                        Log.d("onWriteSuccess", "onWriteFailure");
                    }
                });
    }
}
