package com.hkyudong.BluetoothOscilloscope;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.R.integer;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class BluetoothOscilloscope extends Activity implements  Button.OnClickListener{
	
	// Run/Pause status
    private boolean bReady = false;//����״̬

    // Message types sent from the BluetoothRfcommClient Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int FILE_READ = 6;

    // Key names received from the BluetoothRfcommClient Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static final String DIR = "WaveData";
    
    
    private final String filepathString = Environment.getExternalStorageDirectory().toString()+File.separator+DIR+File.separator;
    private String newfilepathString = null;
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_CONNECT_FILE = 3;
    // bt-uart constants
    private static final int MAX_SAMPLES = 640;//���������أ�
    private static final int  MAX_LEVEL	= 240;//���ˮƽ���أ�
    private static final int  DATA_START = (MAX_LEVEL + 1);//��ʼ��־����
    private static final int  DATA_END = (MAX_LEVEL + 2);//������־����

    private static final byte  REQ_DATA = 0x00;
    private static final byte  ADJ_HORIZONTAL = 0x01;
    private static final byte  ADJ_VERTICAL = 0x02;
    private static final byte  ADJ_POSITION = 0x03;

    private static final byte  CHANNEL1 = 0x01;
    private static final byte  CHANNEL2 = 0x02;

	private static final String MYTAG = "hkyudong01";

	protected static final String USERNAME = "hkyudong";

	

    // Layout Views
	private TextView mfileStatus;
    private TextView mBTStatus;//����״̬TextView
    private TextView txtYshrink;//y������
    private TextView ch1_scale;//�ź�һ�����Ŀ̶ȼ��
    private TextView ch1pos_label;//�ź�һ�����ڻ����ϵ�����λ��
    private RadioButton rb1;//�ź�һ����ѡ��ѡ��
//    private Button timebase_inc, timebase_dec;
    private Button btn_scale_up, btn_scale_down;
    private Button btn_pos_up, btn_pos_down;//posλ��
    private Button mConnectButton;//����button
    
    private Button readfileButton;
    private Button uploadfileButton;
    private Button downloadButton;
    private ToggleButton run_buton;//����״̬ToggleButton
    
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter����������
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the RFCOMM services
    private BluetoothRfcommClient mRfcommClient = null;
    
    private ReadOldFile readOldFile = null;
    
    protected PowerManager.WakeLock mWakeLock;
    
    public WaveformView mWaveform = null;
    
    private PrintStream fileoutPrintStream = null;
    private String strNewFileName = null;
 //   private InputStream BTFileinpuStream = null;
//    static String[] timebase = {"5us", "10us", "20us", "50us", "100us", "200us", "500us", "1ms", "2ms", "5ms", "10ms", "20ms", "50ms" };
	static String[] ampscale = {"10mV", "20mV", "50mV", "100mV", "200mV", "500mV", "1V", "2V", "GND"};
	static byte timebase_index = 5;//����ʱ�����
	static byte ch1_index = 4;// ch2_index = 5;
	static byte ch1_pos = 20;// ch2_pos = 25;	// 0 to 40
	private int[] ch1_data = new int[MAX_SAMPLES/2];
//	private int[] ch2_data = new int[MAX_SAMPLES/2];
	
	private int dataIndex=0, dataIndex1=0, dataIndex2=0;
	private boolean bDataAvailable=false;

	//my
	private  int Yshrink = 1;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the window layout
        requestWindowFeature(Window.FEATURE_NO_TITLE);        
        setContentView(R.layout.main);
        
        mBTStatus = (TextView) findViewById(R.id.txt_btstatus);
        mfileStatus = (TextView) findViewById(R.id.txt_filestatus);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "����������ģ��", Toast.LENGTH_LONG).show();
            finish();
            return;
        }else{Toast.makeText(this, "��������ģ��", Toast.LENGTH_LONG).show();}
        // Prevent phone from sleeping
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag"); 
        this.mWakeLock.acquire();
        
//        //����Ϊ����һ��������ݵ��ļ����룬������һ��button�������������        
//        SimpleDateFormat    formatter    =   new    SimpleDateFormat    ("yyyyMMdd-hhmmss");     
//        Date    curDate    =   new    Date(System.currentTimeMillis());//��ȡ��ǰʱ��     
//        strNewFileName    =    formatter.format(curDate); 
//        newfilepathString = filepathString+strNewFileName+".txt";
//        File file = new File(newfilepathString);
//        Log.i(MYTAG, filepathString+strNewFileName+".txt");
//        if (! file.getParentFile().exists()) {//���ļ��в����ھͽ���һ��
//			file.getParentFile().mkdirs();
//		}
//        try {
//        
//        fileoutPrintStream = new PrintStream(new FileOutputStream(file));
//		} catch (Exception e) {
//			// TODO: handle exception
//			 Log.i(MYTAG, "�����ļ�ʧ��!");
//		}
//		
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupOscilloscope() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the Oscillosope session
        } else {
            if (mRfcommClient == null) setupOscilloscope();
            int i;
    		for(i=0; i<320; i++){
    			int a ;
    			a=30 + i%50;
//    			Log.i(MYTAG,Integer.toString(a), null);
    			mWaveform.set_data(a);
     			//fileoutPrintStream.print(a);
				//fileoutPrintStream.print(",");
    		}
    		
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mRfcommClient != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mRfcommClient.getState() == BluetoothRfcommClient.STATE_NONE) {
              // Start the Bluetooth  RFCOMM services
              mRfcommClient.start();
            }
        }
    }

    private void setupOscilloscope() {
        
       txtYshrink = (TextView)findViewById(R.id.txt_Yshrink);
       txtYshrink.setText(Integer.toString(Yshrink));
//        timebase_inc = (Button) findViewById(R.id.btn_timebase_increase);
//        timebase_dec = (Button) findViewById(R.id.btn_timebase_decrease);
//        timebase_inc.setOnClickListener(this);
//        timebase_dec.setOnClickListener(this);
        
        run_buton = (ToggleButton) findViewById(R.id.tbtn_runtoggle);
        run_buton.setOnClickListener(this);
        rb1 = (RadioButton)findViewById(R.id.rbtn_ch1);
//        rb2 = (RadioButton)findViewById(R.id.rbtn_ch2);
        
        ch1_scale = (TextView) findViewById(R.id.txt_ch1_scale);
//        ch2_scale = (TextView) findViewById(R.id.txt_ch2_scale);
        ch1_scale.setText(ampscale[ch1_index]);
//        ch2_scale.setText(ampscale[ch2_index]);
        
        btn_scale_up = (Button) findViewById(R.id.btn_scale_increase);
        btn_scale_down = (Button) findViewById(R.id.btn_scale_decrease);
        btn_scale_up.setOnClickListener(this);
        btn_scale_down.setOnClickListener(this);
        
//        btn_pos_up = (Button) findViewById(R.id.btn_position_up);
//        btn_pos_down = (Button) findViewById(R.id.btn_position_down);
//        btn_pos_up.setOnClickListener(this);
//        btn_pos_down.setOnClickListener(this);
        
//        ch1pos_label = (TextView) findViewById(R.id.txt_ch1pos);
//        ch2pos_label = (TextView) findViewById(R.id.txt_ch2pos);
//        ch1pos_label.setPadding(0, toScreenPos(ch1_pos), 0, 0);
//        ch2pos_label.setPadding(0, toScreenPos(ch2_pos), 0, 0);
        
        
        readfileButton = (Button) findViewById(R.id.button_readfile);
        readfileButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
				clear();
//				 String SDPATH = Environment.getExternalStorageDirectory().getPath();
//				 String filepath = SDPATH + "//" + "lph.txt";
				if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
//					readOldFile = new ReadOldFile(BluetoothOscilloscope.this, mHandler, "lph.txt");
//			        readOldFile.start();
					Intent filelistIntent = new Intent();
					filelistIntent.setClass(BluetoothOscilloscope.this, FileListActivity.class);
					filelistIntent.putExtra("username", USERNAME);
					startActivityForResult(filelistIntent, REQUEST_CONNECT_FILE);
				//	startActivity(filelistIntent);
					
				}else {
					Toast.makeText(getApplicationContext(), "��ȡʧ�ܣ�sd��������", Toast.LENGTH_LONG).show();
				}
			    
			}
		});
        
        mConnectButton = (Button) findViewById(R.id.button_connect);
        mConnectButton.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				clear();
				BTConnect();
			}
		});
        
        uploadfileButton = (Button) findViewById(R.id.button_NET_sendfile);
        uploadfileButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				clear();
				if (null != fileoutPrintStream) {//�ر�����ļ���
					fileoutPrintStream.close();
				}
				Intent uploadIntent = new Intent();
				uploadIntent.setClass(BluetoothOscilloscope.this, Uploadfile.class);
				uploadIntent.putExtra("filepath", newfilepathString);
				startActivityForResult(uploadIntent,222);
			}
		});

        // Initialize the BluetoothRfcommClient to perform bluetooth connections
        mRfcommClient = new BluetoothRfcommClient(this, mHandler);
        
        mWaveform = (WaveformView)findViewById(R.id.WaveformArea);
        
    }
    
    @Override
    public void  onClick(View v){
    	int buttonID;
    	buttonID = v.getId();
    	switch (buttonID){
//    	case R.id.btn_position_up :
//    		if(rb1.isChecked() && (ch1_pos<38) ){
//    			ch1_pos += 1; ch1pos_label.setPadding(0, toScreenPos(ch1_pos), 0, 0);
//    			sendMessage( new String(new byte[] {ADJ_POSITION, CHANNEL1, ch1_pos}) );
//    		}
///*    		else if(rb2.isChecked() && (ch2_pos<38) ){
//    			ch2_pos += 1; ch2pos_label.setPadding(0, toScreenPos(ch2_pos), 0, 0);
//    			sendMessage( new String(new byte[] {ADJ_POSITION, CHANNEL2, ch2_pos}) );
//    		}
//*/    		break;
//    	case R.id.btn_position_down :
//    		if(rb1.isChecked() && (ch1_pos>4) ){
//    			ch1_pos -= 1; ch1pos_label.setPadding(0, toScreenPos(ch1_pos), 0, 0);
//    			sendMessage( new String(new byte[] {ADJ_POSITION, CHANNEL1, ch1_pos}) );
//    		}
///*    		else if(rb2.isChecked() && (ch2_pos>4) ){
//    			ch2_pos -= 1; ch2pos_label.setPadding(0, toScreenPos(ch2_pos), 0, 0);
//    			sendMessage( new String(new byte[] {ADJ_POSITION, CHANNEL2, ch2_pos}) );
//    		}
// */   		break;
    	case R.id.btn_scale_increase :
    		if(1 < Yshrink){
    			Yshrink--;
    			txtYshrink.setText(Integer.toString(Yshrink));
    		}
/*    		else if(rb2.isChecked() && (ch2_index>0)){
    			ch2_scale.setText(ampscale[--ch2_index]);
    			sendMessage( new String(new byte[] {ADJ_VERTICAL, CHANNEL2, ch2_index}) );
    		}
*/    		break;
    	case R.id.btn_scale_decrease :
    		if(50 > Yshrink){
    			Yshrink++;
    			txtYshrink.setText(Integer.toString(Yshrink));
    		}
/*    		else if(rb2.isChecked() && (ch2_index<(ampscale.length-1))){
    			ch2_scale.setText(ampscale[++ch2_index]);
    			sendMessage( new String(new byte[] {ADJ_VERTICAL, CHANNEL2, ch2_index}) );
    		}
*/    		break;
/*    	case R.id.btn_timebase_increase :
    		if(timebase_index<(timebase.length-1)){
    			time_per_div.setText(timebase[++timebase_index]);
    			sendMessage( new String(new byte[] {ADJ_HORIZONTAL, timebase_index}) );
    		}
    		break;
    	case R.id.btn_timebase_decrease :
    		if(timebase_index>0){
    			time_per_div.setText(timebase[--timebase_index]);
    			sendMessage( new String(new byte[] {ADJ_HORIZONTAL, timebase_index}) );
    		}
    		break;
*/    	case R.id.tbtn_runtoggle :
    		if(run_buton.isChecked()){
/*    			sendMessage( new String(new byte[] {
    					ADJ_HORIZONTAL, timebase_index,
    					ADJ_VERTICAL, CHANNEL1, ch1_index,
    					ADJ_VERTICAL, CHANNEL2, ch2_index,
    					ADJ_POSITION, CHANNEL1, ch1_pos,
    					ADJ_POSITION, CHANNEL2, ch2_pos,
    					REQ_DATA}) );
*/    			if (null !=readOldFile) {
					readOldFile.set_wait(false);
				};	
				if (null != mRfcommClient) {
					mRfcommClient.set_wait(false);
				}
				
    			bReady = true;
    		}else{
    			//readOldFile.cancle();
    			
    			if (null !=readOldFile) {
					readOldFile.set_wait(true);
				};
				if (null != mRfcommClient) {
					mRfcommClient.set_wait(true);
				}
 //   			Log.i(MYTAG, "aaaaaaaa", null);
    			bReady = false;
    		}
    		break;
    	}
    }
    
    private void BTConnect(){
    	Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }
    
    private int toScreenPos(byte position){
    	//return ( (int)MAX_LEVEL - (int)position*6 );
    	return ( (int)MAX_LEVEL - (int)position*6 - 7);
    }

    @Override
    public void onDestroy() {    	
        super.onDestroy();
        clear();
        if (null != fileoutPrintStream) {//�ر�����ļ���
			fileoutPrintStream.close();
		}
        // release screen being on
        if (mWakeLock.isHeld()) { 
            mWakeLock.release();
        }
    }
    /**
     * �������ļ�״̬����
     * ����ǰ���Ѿ����ڵĶ��ļ��̺߳������߳�
     */
    private void clear() {
    	mWaveform.clearScreen();
    	run_buton.setChecked(false);
    	if (readOldFile != null) {
    		readOldFile.set_wait(true);
    		//readOldFile.cancel();
    		readOldFile = null;
    	}
        // Stop the Bluetooth RFCOMM services
        if (mRfcommClient != null) {mRfcommClient.stop();}
        
	}

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mRfcommClient.getState() != BluetoothRfcommClient.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothRfcommClient to write
            byte[] send = message.getBytes();
            mRfcommClient.write(send);
        }
    }

    // The Handler that gets information back from the BluetoothRfcommClient
    private final Handler mHandler = new Handler() {
    	
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothRfcommClient.STATE_CONNECTED:
                	run_buton.setChecked(true);
                    mBTStatus.setText(R.string.title_connected_to);
                    mBTStatus.append("\n" + mConnectedDeviceName);
                    //����Ϊ����һ��������ݵ��ļ�����  
                	SimpleDateFormat    formatter    =   new    SimpleDateFormat    ("yyyyMMdd-hhmmss");     
                	Date    curDate    =   new    Date(System.currentTimeMillis());//��ȡ��ǰʱ��     
                	strNewFileName    =    formatter.format(curDate); 
                	newfilepathString = filepathString+strNewFileName+".txt";
                	File file = new File(newfilepathString);
                	Log.i(MYTAG, filepathString+strNewFileName+".txt");
                	if (! file.getParentFile().exists()) {//���ļ��в����ھͽ���һ��
          			file.getParentFile().mkdirs();
          			}
                	try {
                  
                		fileoutPrintStream = new PrintStream(new FileOutputStream(file));
                	} catch (Exception e) {
                		// TODO: handle exception
                		Log.i(MYTAG, "�����ļ�ʧ��!");
                	}                	
                    break;
                case BluetoothRfcommClient.STATE_CONNECTING:
                	mBTStatus.setText(R.string.title_connecting);
                    break;
                //case BluetoothRfcommClient.STATE_LISTEN:
                case BluetoothRfcommClient.STATE_NONE:
                	mBTStatus.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                //byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                //String writeMessage = new String(writeBuf);
                //mBTStatus.setText(writeMessage);
                break;
            case MESSAGE_READ:
            	//Log.i(MYTAG, "MESSAGE_READ", null);
            	int raw, data_length;
                String readBuf = (String) msg.obj;
               // byte[] readBuf = (byte[]) msg.obj;
                //data_length = msg.arg1;
                int readBufint = Integer.parseInt(readBuf);
                //Log.i(MYTAG, readBuf, null);
                Log.i(MYTAG, Integer.toString(readBufint), null);
                fileoutPrintStream.print(readBufint);
                fileoutPrintStream.print(",");
                mWaveform.set_data(readBufint/Yshrink);                
                Log.i(MYTAG, Integer.toString(readBufint/Yshrink), null);
/*                 if (null == readOldFile) {
					readOldFile = new ReadOldFile(BluetoothOscilloscope.this, mHandler, null,newfilepathString);
					readOldFile.start();
					mfileStatus.setText("�����ӵ��ļ���"+strNewFileName);
					mfileStatus.setVisibility(View.VISIBLE);
				}
               
                String[] charsString = readBuf.split(","); 
                for(int x=0; x<charsString.length; x++){
                	fileoutPrintStream.print(charsString[x]);
					fileoutPrintStream.print(",");
//               	    raw = Integer.valueOf(charsString[x]);
//                	mWaveform.set_data(raw);
//                	Log.i(MYTAG, Integer.toString(raw), null);
//                	if (true) {//�����Ƿ񱣴�Ŀ���
//    					fileoutPrintStream.print(raw);
//    					fileoutPrintStream.print(",");
//    				}
                }
                 	
                	raw = UByte(readBuf[x]);
                	if( raw>MAX_LEVEL ){
                		if( raw==DATA_START ){
                    		bDataAvailable = true;
                    		dataIndex = 0; dataIndex1=0; dataIndex2=0;
                    	}
                		else if( (raw==DATA_END) || (dataIndex>=MAX_SAMPLES) ){
                			//���յ����ݴ��ͽ����ı�־���ݻ���ch1_data��ch2_data���ѽ�������������set_data����ͼ��surfview
                			//���ʣ���������dataIndex>=MAX_SAMPLES��ʹbDataAvailable��Ϊ false֮�����ݴ��仹û�����������bDataAvailable��Ϊ ture����
                    		bDataAvailable = false;
                    		dataIndex = 0; dataIndex1=0; dataIndex2=0;
                    		mWaveform.set_data(ch1_data);                      	
                        	if(bReady){ // send "REQ_DATA" again
                        		BluetoothOscilloscope.this.sendMessage( new String(new byte[] {REQ_DATA}) );//���ݽ�����ϣ�֪ͨ�豸������������
                        	}
                        	break;
                    	}                  	
                	}
                	else if( (bDataAvailable) && (dataIndex<(MAX_SAMPLES)) ){ // valid data��Ч������
                		if((dataIndex++)%2==0) ch1_data[dataIndex1++] = raw;	// even data ż������
//                		else ch2_data[dataIndex2++] = raw;	// odd data ��������
                	}
*/                  	
                
                
                break;
                
            //my
            case FILE_READ : 
            	//time_per_div.setText("aaa");
            	int readdata;
            	readdata = msg.arg1;
 //           	Log.i(MYTAG, Integer.toString(readdata), null);
            	mWaveform.set_data(readdata/Yshrink);            	
            	break;
            	
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
        private int UByte(byte b){  //���������յ�����ת��Ϊsurfview�ϵ����꣬��ֱֵ�ӷ��أ���ֵ���� ��Ȼ��ǿ��int����ת������
        	if(b<0) // if negative
        		return (int)( (b&0x7F) + 128 );
        	else
        		return (int)b;
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {            	
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mRfcommClient.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
             
            	// Bluetooth is now enabled, so set up the oscilloscope
            	setupOscilloscope();
            } else {
                // User did not enable Bluetooth or an error occured
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
        case REQUEST_CONNECT_FILE : 
        	if (Activity.RESULT_OK == resultCode) {
				String oldfilepathString = data.getExtras().getString(FileListActivity.EXTRA_FILE_PATH);
				Log.i(MYTAG, oldfilepathString);
				readOldFile = new ReadOldFile(BluetoothOscilloscope.this, mHandler, null,oldfilepathString);
			    readOldFile.start();
			    run_buton.setChecked(true);
			    readOldFile.set_wait(false);
			    mfileStatus.setText("�����ӵ��ļ���"+data.getExtras().getString(FileListActivity.EXTRA_FILE_NAME));
			    mfileStatus.setVisibility(View.VISIBLE);
			}
        }
    }
    public void onBackPressed() { 
    	clear();
    	new AlertDialog.Builder(this).setTitle("ȷ���˳���") 
    	    .setIcon(android.R.drawable.ic_dialog_info) 
    	    .setPositiveButton("ȷ��", new DialogInterface.OnClickListener() { 
    	 
    	        @Override 
    	        public void onClick(DialogInterface dialog, int which) {     	        	
    	        // �����ȷ�ϡ���Ĳ��� 
    	        	
//    	        	new AlertDialog.Builder(BluetoothOscilloscope.this) 
//    	        	.setTitle("��ʾ��")
//    	        	.setMessage("�Ƿ��ϴ����ݵ���������")
//    	        	.setPositiveButton("��", null)
//    	        	.setNegativeButton("��", null)
//    	        	.show();
//    	        	clear();
    	        	BluetoothOscilloscope.this.finish();    	 
    	        } 
    	    }) 
    	    .setNegativeButton("����", new DialogInterface.OnClickListener() { 
    	 
    	        @Override 
    	        public void onClick(DialogInterface dialog, int which) { 
    	        // ��������ء���Ĳ���,���ﲻ����û���κβ��� 
    	        } 
    	    }).show(); 
    	// super.onBackPressed(); 
    	   } 

}
