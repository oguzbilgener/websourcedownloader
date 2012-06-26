package com.oguzb.websourcedownloader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;

import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.oguzb.websourcedownloader.Utils.Commons;

public class DownloaderService extends Service implements Runnable
{
	private Context context;
	private Resources res;
	private PowerManager pm = null;
	private PowerManager.WakeLock wl = null;
	
	private Thread downloader;
	
	private String urlText;
	private String source = null;
	
	@Override
	public IBinder onBind(Intent intent) 
	{
		return null;
	}
	
	@Override
	public void onCreate() 
	{
		super.onCreate();
		
		// Initializations
		context = getApplicationContext();
		res = getResources();
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DownloadService");
		
	}
	
	@Override
	public void onStart(Intent intent,int startId) 
	{
		super.onStart(intent,startId);
		try
		{
			// test again
			urlText = intent.getStringExtra("url");
			if(urlText==null)
				throw new Exception(res.getString(R.string.error_no_url));
			if(urlText.equals(""))
	        	throw new Exception(res.getString(R.string.error_empty_url));
	        if(!urlText.matches(Commons.URL_PATTERN))
	        	throw new Exception(res.getString(R.string.error_invalid_url));
	        if(!Commons.canAccess(context))
	        	throw new Exception(res.getString(R.string.error_no_connection));
		}
		catch(Exception e)
		{
			Message resp = new Message();
			Bundle response = new Bundle();
			response.putInt("result", 0);
			response.putInt("status", 0);
			response.putString("message",e.getMessage());
			response.putString("source", null);
			resp.setData(response);
			handler.sendMessage(resp);
		}
		
		downloader = new Thread(this);
		
		downloader.start();
	}
	
	@Override
	public void onDestroy() 
	{
		downloader.interrupt();
		super.onDestroy();
	}
	
	/*
	 * Handler - Finalizes the task of the service and triggers sendResult()
	 */
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) 
		{
			Bundle response = msg.getData();
			sendResult(response.getInt("result"),response.getInt("status"),response.getString("message"),response.getString("source"));
		}
	};
	
	/*
	 * sendResult() method sends the response back to the activity
	 */
	private void sendResult(int result,int status,String message,String source) 
	{
		  Intent intent = new Intent(Commons.EVENT_NAME);
		  intent.putExtra("result", result);
		  intent.putExtra("status", status);
		  intent.putExtra("message", message);
		  intent.putExtra("source", source);
		  LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
	
	/*
	 * Thread's run() method. Calls getSourceBundle() and triggers the handler
	 */
	@Override
	public void run() 
	{
		wl.acquire();
			Bundle response = getResponseBundle(urlText);
			Message responseMessage = new Message();
			responseMessage.setData(response);
			handler.sendMessage(responseMessage);
		wl.release();
	}
	/*
	 * getResponseBundle() method calls downloadSource() and creates the response bundle according to it
	 */
	private Bundle getResponseBundle(String urlText)
	{
		Bundle response = new Bundle();
		int statusCode = 0, result = 0;
		String message="";
		try
		{
			// call downloadSource
			statusCode = downloadSource(urlText);
		}
		catch(IOException e)
		{
			result = 0;
			statusCode = 0;
			message = res.getString(R.string.error_unknown_host);
		}
		catch(Exception e)
		{
			// create the response with an error			
			result = 0;
			statusCode = 0;
			message = e.getMessage();
			source = null;
		}

			if(statusCode!=0)
			{
				// create the response without an error
				// however, the HTTP response code may be different than 200 - OK
				result = 1;
				message = "no error";
			}
			response.putInt("result", result);
			response.putInt("status", statusCode);
			response.putString("message", message);
			response.putString("source", source);		
		return response;
	}
	
	/*
	 * downloadSource() method downloads the source code from remote website, 
	 * saves it to the "source" variable and 
	 * returns http status code
	 */
	private int downloadSource(String urlText) throws IOException,Exception
	{
		int status = 0;
		
		URI url = new URI(urlText);
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse response = httpclient.execute(new HttpGet(url));
		StatusLine statusLine = response.getStatusLine();
		status = statusLine.getStatusCode();
		Log.d(Commons.TAG,"HTTP "+status);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		response.getEntity().writeTo(out);
		out.close();
		
		source = out.toString();
		
		return status;
	}

}
