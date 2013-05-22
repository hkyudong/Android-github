package com.hkyudong.BluetoothOscilloscope;

import java.util.Random;

import android.R.integer;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

public class WaveformView extends SurfaceView implements SurfaceHolder.Callback{
	
	private WaveformPlotThread plot_thread;
	
	private static int WIDTH = 320;//320;
	private final static int HEIGHT = 320;

	private static final String MYTAG = "hkyudong";
	
	
	private int nowX = 0;
	private static int[]  data = new int[WIDTH];

//	private static int ch1_pos = 120, ch2_pos = 200;
	
	private Paint line_paint = new Paint();
//	private Paint ch2_color = new Paint();
	private Paint grid_paint = new Paint();
	private Paint cross_paint = new Paint();
	private Paint outline_paint = new Paint();
	private Paint nowX_paint = new Paint();
	
	public WaveformView(Context context, AttributeSet attrs) {  

		super(context, attrs);  
		//super(context);
		getHolder().addCallback(this);
/*	
		int i;
		for(i=0; i<WIDTH; i++){
			data[i] =30 + i%50;
//			ch2_data[i] = ch2_pos;
		}
*/		
		plot_thread = new WaveformPlotThread(getHolder(), this);
		//setFocusable(true);
		line_paint.setColor(Color.YELLOW);
//		ch2_color.setColor(Color.RED);
		grid_paint.setColor(Color.rgb(100, 100, 100));
		cross_paint.setColor(Color.rgb(70, 100, 70));
		outline_paint.setColor(Color.GREEN);
		nowX_paint.setColor(Color.RED);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		plot_thread.setRunning(true);
		plot_thread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
		plot_thread.setRunning(false);
		while (retry){
			try{
				plot_thread.join();
				retry = false;
			}catch(InterruptedException e){
				
			}
		}
		
	}
	
	@Override
	public void onDraw(Canvas canvas){
		PlotPoints(canvas);
		
	}
	public void clearScreen() {
		for(int i=0; i<WIDTH; i++){
			data[i] = 0;
			nowX = 0;
		}
	}
	public void set_data(int tempdata){
	       
//			plot_thread.setRunning(false);	
			int i = nowX;
			if(i < WIDTH){
				data[nowX] = HEIGHT-tempdata+1;
//				Log.i(MYTAG, Integer.toString(data[nowX]), null);
				nowX++;
				if (WIDTH == nowX) {
					nowX=0;
				}
				Log.i(MYTAG, Integer.toString(nowX), null);
			}
//			plot_thread.setRunning(true);
	}
	
	public void set_data(int[] tempdata){
//       Log.i(MYTAG, tempdata.toString(), null);
//		plot_thread.setRunning(false);
		int i;
		for(i = 0;i <tempdata.length;i++){
			if(nowX <= WIDTH){
				data[nowX] = HEIGHT-tempdata[i]+1;
				nowX++;
			}else {
//				plot_thread.setRunning(true);
				nowX=0;
				}
		}
/*		int x;
		plot_thread.setRunning(false);
		x = 0;
		while(x<WIDTH){
			if(x<(tempdata.length)){
				//ch1_data[x] = data1[x];
				data[x] = HEIGHT-tempdata[x]+1;
			}else{
//				ch1_data[x] = ch1_pos;
			}
			x++;
		}
		x = 0;
		while(x<WIDTH){
			if(x<(data2.length)){
				//ch2_data[x] = data2[x];				
				ch2_data[x] = HEIGHT-data2[x]+1;
			}else{
				ch2_data[x] = ch2_pos;
			}
			x++;
		}
*/		
//		plot_thread.setRunning(true);
	}
	
	public void PlotPoints(Canvas canvas){
		
		// clear screen
		canvas.drawColor(Color.rgb(20, 20, 20));
		
		// draw grids
	    for(int vertical = 1; vertical<10; vertical++){
	    	canvas.drawLine(
	    			vertical*(WIDTH/10)+1, 1,
	    			vertical*(WIDTH/10)+1, HEIGHT+1,
	    			grid_paint);
	    }	    	
	    for(int horizontal = 1; horizontal<10; horizontal++){
	    	canvas.drawLine(
	    			1, horizontal*(HEIGHT/10)+1,
	    			WIDTH+1, horizontal*(HEIGHT/10)+1,
	    			grid_paint);
	    }	    	
	    
	    // draw center cross
		canvas.drawLine(0, (HEIGHT/2)+1, WIDTH+1, (HEIGHT/2)+1, cross_paint);
		canvas.drawLine((WIDTH/2)+1, 0, (WIDTH/2)+1, HEIGHT+1, cross_paint);
		
		// draw outline
		canvas.drawLine(0, 0, (WIDTH+1), 0, outline_paint);	// top
		canvas.drawLine((WIDTH+1), 0, (WIDTH+1), (HEIGHT+1), outline_paint); //right
		canvas.drawLine(0, (HEIGHT+1), (WIDTH+1), (HEIGHT+1), outline_paint); // bottom
		canvas.drawLine(0, 0, 0, (HEIGHT+1), outline_paint); //left

		
//		Log.i(MYTAG, Integer.toString(nowX), null);
//		Log.i(MYTAG, "111", null);
		// plot data
		for(int x=0; x<(WIDTH-1); x++){			
			canvas.drawLine(x+1, data[x], x+2, data[x+1], line_paint);
//			canvas.drawLine(x+1, ch1_data[x], x+2, ch1_data[x+1], ch1_color);
//			Log.i(MYTAG, Integer.toString(data[x]), null);
		}
		canvas.drawLine(nowX, 0,nowX, HEIGHT, nowX_paint);//刷新分界线
	}

}
