package com.hkyudong.BluetoothOscilloscope;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class Uploadfile extends Activity {
	/** Called when the activity is first created. */
	/**
	 * Upload file to web server with progress status, client: android;
	 * server:php
	 * **/

	private TextView mtv1 = null;
	private TextView mtv2 = null;
	private Button bupload = null;
	private Button backbtn = null;

	private String uploadFilepath = null;
	private String actionUrl = "http://192.168.1.223/electrocardiogram/uploadfile.php";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.upload);
		
		
		Intent newintent=getIntent();
		Bundle bundle=newintent.getExtras();
		uploadFilepath = bundle.getString("filepath");
		
		mtv1 = (TextView) findViewById(R.id.txt_filepath);
		mtv1.setText("文件路径：\n" + uploadFilepath);
		mtv2 = (TextView) findViewById(R.id.txt_actionpath);
		mtv2.setText("上传地址：\n" + actionUrl);
		bupload = (Button) findViewById(R.id.btn_sendfile);
		bupload.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				FileUploadTask fileuploadtask = new FileUploadTask();
				fileuploadtask.execute();
			}
		});
		backbtn = (Button) findViewById(R.id.btn_uploadback);
		backbtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent2 = new Intent();
				setResult(333,intent2);
				finish();
			}
		});
	}

	// show Dialog method
	private void showDialog(String mess) {
		new AlertDialog.Builder(Uploadfile.this).setTitle("Message")
				.setMessage(mess)
				.setNegativeButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
	}

	class FileUploadTask extends AsyncTask<Object, Integer, Void> {

		private ProgressDialog dialog = null;
		HttpURLConnection connection = null;
		DataOutputStream outputStream = null;
		DataInputStream inputStream = null;
		//the file path to upload
		String pathToOurFile = uploadFilepath;
		//the server address to process uploaded file
		String urlServer = actionUrl;
		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary = "*****";

		File uploadFile = new File(pathToOurFile);
		long totalSize = uploadFile.length(); // Get size of file, bytes

		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(Uploadfile.this);
			dialog.setMessage("正在上传...");
			dialog.setIndeterminate(false);
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dialog.setProgress(0);
			dialog.show();
		}

		@Override
		protected Void doInBackground(Object... arg0) {

			long length = 0;
			int progress;
			int bytesRead, bytesAvailable, bufferSize;
			byte[] buffer;
			int maxBufferSize = 256 * 1024;// 256KB

			try {
				FileInputStream fileInputStream = new FileInputStream(new File(
						pathToOurFile));

				URL url = new URL(urlServer);
				connection = (HttpURLConnection) url.openConnection();

				// Set size of every block for post
				connection.setChunkedStreamingMode(256 * 1024);// 256KB

				// Allow Inputs & Outputs
				connection.setDoInput(true);
				connection.setDoOutput(true);
				connection.setUseCaches(false);

				// Enable POST method
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Connection", "Keep-Alive");
				connection.setRequestProperty("Charset", "UTF-8");
				connection.setRequestProperty("Content-Type",
						"multipart/form-data;boundary=" + boundary);

				outputStream = new DataOutputStream(
						connection.getOutputStream());
				outputStream.writeBytes(twoHyphens + boundary + lineEnd);
				outputStream
						.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\""
								+ pathToOurFile + "\"" + lineEnd);
				outputStream.writeBytes(lineEnd);

				bytesAvailable = fileInputStream.available();
				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				buffer = new byte[bufferSize];

				// Read file
				bytesRead = fileInputStream.read(buffer, 0, bufferSize);

				while (bytesRead > 0) {
					outputStream.write(buffer, 0, bufferSize);
					length += bufferSize;
					progress = (int) ((length * 100) / totalSize);
					publishProgress(progress);

					bytesAvailable = fileInputStream.available();
					bufferSize = Math.min(bytesAvailable, maxBufferSize);
					bytesRead = fileInputStream.read(buffer, 0, bufferSize);
				}
				outputStream.writeBytes(lineEnd);
				outputStream.writeBytes(twoHyphens + boundary + twoHyphens
						+ lineEnd);
				publishProgress(100);

				// Responses from the server (code and message)
				int serverResponseCode = connection.getResponseCode();
				String serverResponseMessage = connection.getResponseMessage();

				/* 将Response显示于Dialog */
				// Toast toast = Toast.makeText(UploadtestActivity.this, ""
				// + serverResponseMessage.toString().trim(),
				// Toast.LENGTH_LONG);
				// showDialog(serverResponseMessage.toString().trim());
				/* 取得Response内容 */
				// InputStream is = connection.getInputStream();
				// int ch;
				// StringBuffer sbf = new StringBuffer();
				// while ((ch = is.read()) != -1) {
				// sbf.append((char) ch);
				// }
				//
				// showDialog(sbf.toString().trim());

				fileInputStream.close();
				outputStream.flush();
				outputStream.close();

			} catch (Exception ex) {
				// Exception handling
				// showDialog("" + ex);
				// Toast toast = Toast.makeText(UploadtestActivity.this, "" +
				// ex,
				// Toast.LENGTH_LONG);

			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			dialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(Void result) {
			try {
				dialog.dismiss();
				// TODO Auto-generated method stub
			} catch (Exception e) {
			}
		}

	}
}
