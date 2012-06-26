package com.oguzb.websourcedownloader;

import java.io.*;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.oguzb.websourcedownloader.Utils.BadStatusException;
import com.oguzb.websourcedownloader.Utils.Commons;

public class DownloaderActivity extends Activity implements View.OnClickListener
{
	// "active" variable is used by the downloader service to determine if activity is running or not
	public boolean active;
	private Context context;
	private Resources res;
	
	// url text to be sent to the service
	private String urlText;
	
	// three info boxes
	private Loading loading;
	private Warning warning;
	private Error error;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.downloader);
        active = true;
        context = getApplicationContext();
        res = getResources();
        final Intent intent = getIntent();
        final Bundle extras = intent.getExtras();
        
        // Register a Local Broadcast Receiver to 
        LocalBroadcastManager.getInstance(this).registerReceiver(DownloadResult, new IntentFilter(Commons.EVENT_NAME));
        
        View parentView = findViewById(R.id.parent);
        parentView.setOnClickListener(this);
        
        // Initialize UI elements
        loading = new Loading();
        warning = new Warning();
        error = new Error();
        
        try
        {
	        // Get the URL from intent extras
	        if(extras!=null) 
	        {
				if (intent.getStringExtra(Intent.EXTRA_TEXT)!=null) 
					urlText = intent.getStringExtra(Intent.EXTRA_TEXT);
				else 
					urlText = extras.getString(Commons.URL_KEY);
				
				if(urlText==null)
					throw new Exception(res.getString(R.string.error_no_url));
	        }
	        else
	        {
	        	// no URL? quit.
	        	throw new Exception(res.getString(R.string.error_no_url));
	        }
	        
	        if(urlText.equals(""))
	        	throw new Exception(res.getString(R.string.error_empty_url));
	        if(!urlText.matches(Commons.URL_PATTERN))
	        	throw new Exception(res.getString(R.string.error_invalid_url));
	        if(!Commons.canAccess(context))
	        	throw new Exception(res.getString(R.string.error_no_connection));
	        
        }
        catch(Exception e)
        {
        	// Stop here. Do not start downloading
        	error.setMsg(e.getMessage());
        	error.show();
        	return;
        }
        
        loading.setLoadingUrl(urlText);
        
        // Start downloading
        loading.show();
        Intent service = new Intent(this,DownloaderService.class);
        service.putExtra("url", urlText);
        startService(service);
    }
    
    @Override
    public void onResume()
    {
    	super.onResume();
    	active = true;
    }
    
    @Override
    public void onPause()
    {
    	active = true;
    	super.onPause();
    }
    
    @Override
    public void onDestroy()
    {
    	// Destroy the local broadcast receiver
    	LocalBroadcastManager.getInstance(this).unregisterReceiver(DownloadResult);
    	try
    	{
	    	Intent stop = new Intent(context,DownloaderService.class);
	    	stopService(stop);
    	}
    	catch(Exception e)
    	{
    		Log.w(Commons.TAG,"cannot stop service");
    	}
    	  super.onDestroy();
    }
    
    /*
     * Do not create the activity again in case of orientation change
     */
    @Override
    public void onConfigurationChanged(Configuration config)
    {
    	super.onConfigurationChanged(config);
    	active = true;
    }
    
    /*
     * Box class is the parent class for 3 boxes: Loading, Success, Error
     * Controls animations
     */
    private abstract class Box implements Animation.AnimationListener
    {
    	View box;
    	int resourceId;
    	Boolean fadeInProgress = false;
    	Boolean fadeOutProgress = false;
    	Animation fadeIn;
    	Animation fadeOut;
    	
    	public abstract int onCreate();
    	
    	public Box()
    	{
    		resourceId = onCreate();
    		box = findViewById(resourceId);
    		fadeIn = AnimationUtils.loadAnimation(context, R.anim.fadein);
    		fadeOut = AnimationUtils.loadAnimation(context, R.anim.fadeout);
    		fadeIn.setAnimationListener(this);
    		fadeOut.setAnimationListener(this);
    	}
    	public void show()
    	{
    		box.setVisibility(View.VISIBLE);
    		startAnim("fadeIn");
    	}
    	public void show(Boolean anim)
    	{
    		box.setVisibility(View.VISIBLE);
    	}
    	public void hide()
    	{
    		startAnim("fadeOut");
    	}
    	public void hide(Boolean anim)
    	{
    		box.setVisibility(View.GONE);
    	}
    	
    	public void startAnim(String type)
    	{
    		if(type.equals("fadeIn"))
    		{
    			fadeInProgress = true;
    			fadeIn.reset();
    			box.startAnimation(fadeIn);
    		}
    		else
    		{
    			fadeOutProgress = true;
    			fadeOut.reset();
    			box.startAnimation(fadeOut);
    		}
    			
    	}
    	public void stopAnim(String type)
    	{
    		if(type.equals("fadeIn"))
    			fadeIn.cancel();
    		else
    			fadeIn.cancel();
    	}
    	public boolean isAnimRunning(String type)
    	{
    		if(type.equals("fadeIn"))
    			return fadeInProgress;
        	else
        		return fadeOutProgress;
    	}
    	public boolean isVisible()
    	{
    		if(box.getVisibility()==View.VISIBLE)
    			return true;
    		return false;
    	}
    	
    	 @Override
    	public void onAnimationEnd(Animation anim) 
    	{
    		if(fadeOutProgress)
    		{
    			box.setVisibility(View.GONE);
    			fadeOutProgress = false;
    		}
    		if(fadeInProgress)
    			fadeInProgress = false;
    		
    	}
    	@Override
    	public void onAnimationRepeat(Animation anim) 
    	{
    		
    	}
    	@Override
    	public void onAnimationStart(Animation anim) 
    	{

    	}
    }
    
    /*
     * Loading class manages the loading box. Extended from Box class
     */
    protected class Loading extends Box
    {
    	View loader;
    	@Override
    	public int onCreate()
    	{
    		int resid = R.id.loadingBox;
    		loader = findViewById(resid);
    		return resid;
    	}
    	public void setLoadingUrl(String t)
    	{
    		((TextView) loader.findViewById(R.id.loadingDesc)).setText(t);
    	}	
    }
    /*
     * Warning class manages the warning box. Extended from Box class
     */
    private class Warning extends Box implements View.OnClickListener
    {
    	View warning;
    	String source;
    	@Override
    	public int onCreate()
    	{
    		int resid = R.id.warningBox;
    		warning = findViewById(resid);
    		return resid;
    	}
    	public void setTitle(String t)
    	{
    		((TextView) warning.findViewById(R.id.warningTitle)).setText(t);
    	}
    	public void setMsg(String t)
    	{
    		((TextView) warning.findViewById(R.id.warningDesc)).setText(t);
    	}
    	public void setClicktoView(String src)
    	{
    		warning.setClickable(true);
    		warning.setOnClickListener(this);
    		source = src;
    	}
		@Override
		public void onClick(View v) 
		{
			prepareFile(source);
		}
    }
    /*
     * Error class manages the error box. Extended from Box class
     */
    private class Error extends Box
    {
    	View error;
    	@Override
    	public int onCreate()
    	{
    		int resid = R.id.errorBox;
    		error = findViewById(resid);
    		return resid;
    	}
    	public void setTitle(String t)
    	{
    		((TextView) error.findViewById(R.id.errorTitle)).setText(t);
    	}	
    	public void setMsg(String t)
    	{
    		((TextView) error.findViewById(R.id.errorDesc)).setText(t);
    	}	
    }
    
    /*
     * displayResponse() method gets the response as a parameter
     * and displays an error or a warning if exists
     * if there is no error, it directly calls prepareFile() to save the file and share the file
     */
    private void displayResponse(Bundle response)
    {
    	if(loading.isVisible())
    		loading.hide();
    	try
    	{
    		Integer result = response.getInt("result");
    		Integer status = response.getInt("status");
    		String message = response.getString("message");
    		String source = response.getString("source");
    		
    		if(result == null || status == null || message == null)
	    		throw new Exception(res.getString(R.string.error_unknown));
    		
    		if(result==0)
    			throw new Exception(message);
    		
    		if(status!=200 && status!=301)
    			throw new BadStatusException(status);
    		
    		if(source==null)
    			throw new Exception(message);
    		
    		// all set!
    		prepareFile(source);
    	}
    	catch(BadStatusException b)
    	{
    		warning.setTitle(b.getStatus()+" - "+Commons.getStatusText(b.getStatus()));
    		warning.setMsg(res.getString(R.string.warning_click_to_view));
    		warning.setClicktoView(response.getString("source"));
    		warning.show();
    	}
    	catch(Exception e)
    	{
    		error.setMsg(e.toString());
    		error.show();
    	}
    }
    
    /*
     * shareSource() method opens a share intent
     */
    private void shareSource(String file) 
    {
    	Intent share = new Intent(Intent.ACTION_VIEW); 
    	Uri uri = Uri.parse("file://"+file); 
    	share.setDataAndType(uri, "text/plain");
    	startActivity(share);
	}
    
    /*
     * saveSource() method literally saves the file to the sd card
     */
    private String saveSource(String url,String source)
    {
    	// first, create a directory
    	String dirstr = Environment.getExternalStorageDirectory()+"/"+Commons.SD_DIR;
    	File dir = new File(dirstr);
    	dir.mkdirs();
    	
    	// make a safe file name
    	String filename = url;
    	filename = filename.replace("http://", "");
    	filename = filename.replace("https://", "");
    	filename = filename.replace(".","_");
    	filename = filename.replace("/","-");
    	filename = filename.replace("?","-");
    	filename = filename.replace("&","_");
    	filename = filename.replace("=","-");
    	filename = filename.replace("%","");
    	filename = filename.replace("[^a-zA-Z-_]","");
    	int len = filename.length();
    	if(len>30)len = 30;
    	filename = filename.substring(0, len);
    	filename = filename + ".html";
    	
    	// save the file to the specified directory in sd card
    	File file = new File(dirstr, filename);
    	FileOutputStream fos;
    	byte[] data = source.getBytes();
    	try 
    	{
    	    fos = new FileOutputStream(file);
    	    fos.write(data);
    	    fos.flush();
    	    fos.close();
    	    return dirstr+"/"+filename;
    	} 
    	catch (FileNotFoundException e) 
    	{
    	    Log.w(Commons.TAG,"file not found "+e.toString());
    	} 
    	catch (IOException e) 
    	{
    	    Log.w(Commons.TAG,"could not write file");
    	}
    	// if fails, return null
    	// it'll show an error
    	return null;
    }
    /*
     * prepareFile() method first tries to save the file via saveSource() 
     * and then calls shareSource, to open the share intent
     */
    private void prepareFile(String source)
    {
    	String file;
    	try
    	{
    		if(source==null)
    			throw new Exception();
	    	 file = saveSource(urlText,source);
	    	if(file==null)
		    	throw new Exception();
    	}
    	catch(Exception e)
    	{
    		displayUnknownError();
    		return;
    	}
    	shareSource(file);
    	finish();
    }
    
    /*
     * Receives the source from service
     */
    private BroadcastReceiver DownloadResult = new BroadcastReceiver() {
    	  @Override
    	  public void onReceive(Context context, Intent intent) 
    	  {
    	    // Get extra data included in the Intent
    		if(intent!=null)
    			displayResponse(intent.getExtras());
    		else
    			displayUnknownError();
    	  }
    };
    
    private void displayUnknownError()
    {
    	if(loading.isVisible())
    		loading.hide();
    	error.setMsg(res.getString(R.string.error_unknown));
    	error.show();
    }

	@Override
	public void onClick(View v) 
	{
		switch(v.getId())
		{
			case R.id.parent:
				// clicked on transparent surface. finish!
				finish();
			break;
		}
	}   
}