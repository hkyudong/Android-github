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
	private static final String DIR = "WaveData";
	private static final String MYTAG = "readoldfile";
	private final String filenameString;
	private final Handler mHandler;
	private Scanner mScanner;
	private boolean wait = true;
	
	public ReadOldFile(Context context,Handler handler,String string) {
		// TODO Auto-generated constructor stub
		mHandler = handler;
		filenameString = string;
	}	
	public void set_wait(boolean newwait) {
		wait = newwait;
	}
	public void run() {
		
		int data;		
		File file = new File(Environment.getExternalStorageDirectory().toString()+File.separator+DIR+File.separator+filenameString);
		if (! file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		try {
			mScanner = new Scanner(file).useDelimiter(",");
			while (mScanner.hasNext()) {
				while (wait) {
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
				mScanner.close();
			}
		}

		
//		mScanner.close();
	}
}
