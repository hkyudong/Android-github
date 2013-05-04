package com.hkyudong.BluetoothOscilloscope;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

public class ReadOldFile extends Thread{
//	private static final String DIR = "WaveData";
	private static final String MYTAG = "readoldfile";
	private final String filepathString;
	private final String filenameString;
	private final Handler mHandler;
	private Scanner mScanner = null;
	private boolean wait = true;
	private File file = null;
	
	public ReadOldFile(Context context,Handler handler,String filename,String filepath) {//�����ļ��ľ���·��ʱʹ�õĹ��캯��
		// TODO Auto-generated constructor stub
		mHandler = handler;
		filenameString = filename;
		filepathString = filepath;
		file = new File(filepathString);
	}	
	public ReadOldFile(Context context,Handler handler,String filename) {//ֻ�����ļ��������ع��캯����Ĭ��·��Ϊ��/mnt/sdcard/WaveData/+filename
		// TODO Auto-generated constructor stub
		mHandler = handler;
		filenameString = filename;
		filepathString = null;
		file = new File(Environment.getExternalStorageDirectory().toString()+File.separator+BluetoothOscilloscope.DIR+File.separator+filenameString);
		if (! file.getParentFile().exists()) {//���ļ��в����ھͽ���һ��
			file.getParentFile().mkdirs();
		}
	}
	public void set_wait(boolean newwait) {//��ͣ���ñ�־
		wait = newwait;
	}
	public void run() {
		try {
			mScanner = new Scanner(file).useDelimiter(",");//�Ѷ�����Ϊ���������зֱ�׼			
		} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
			System.out.println("����Scanerʧ��");
		}
		while (true) {
			if (wait) {
				try {
					sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else if (mScanner.hasNext()) {
				int data1;
				data1 = mScanner.nextInt();
				Log.i(MYTAG, Integer.toString(data1), null);
				mHandler.obtainMessage(BluetoothOscilloscope.FILE_READ, data1,-1).sendToTarget();
			}
		}
		
/*		try {
			mScanner = new Scanner(file).useDelimiter(",");//�Ѷ�����Ϊ���������зֱ�׼			
			while (mScanner.hasNext()) {
				while (wait) {//�ж��Ƿ���ͣ
					try {
						sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}			
				}
				int data1;
				data1 = mScanner.nextInt();
				Log.i(MYTAG, Integer.toString(data1), null);
				mHandler.obtainMessage(BluetoothOscilloscope.FILE_READ, data1,-1).sendToTarget();
			}
		} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
			System.out.println("����Scanerʧ��");
		}finally{
			if (mScanner !=null) {
				mScanner.close();//�ر��ļ�
			}
		}
*/		
//		mScanner.close();
	}
	public void cancel() {
		if (null != mScanner) {
			mScanner.close();
		}
	}
}

