package com.oguzb.websourcedownloader;

import com.oguzb.websourcedownloader.Utils.Commons;
import com.oguzb.websourcedownloader.Utils.QuickToast;

import android.app.Activity;
import android.content.*;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/*
 * FormActivity class is used for the main launcher activity. This activity contains an URL input and send the URL input to DownloaderActivity.
 */

public class FormActivity extends Activity implements View.OnClickListener
{
	private Context context;
	private EditText urlInput;
	
	private Resources res;
	
	 @Override
	 public void onCreate(Bundle savedInstanceState) 
	 {
	     super.onCreate(savedInstanceState);
	     setContentView(R.layout.form);
	     context = getApplicationContext();
	     res = getResources();
	     
	     urlInput = (EditText) findViewById(R.id.urlInput);	     
	     Button submit = (Button) findViewById(R.id.submitButton);
	     submit.setOnClickListener(this);
	 }

	@Override
	public void onClick(View v) 
	{
		if(v.getId()==R.id.submitButton)
		{
			try 
			{
				// check if URL is valid
				String url = urlInput.getText().toString();
				if(url.equals(""))
					throw new Exception(res.getString(R.string.error_empty_url));
				if(!url.matches(Commons.URL_PATTERN))
					throw new Exception(res.getString(R.string.error_invalid_url));
				
				// start DownloaderActivity
				Intent goDownload = new Intent(this,DownloaderActivity.class);
				goDownload.putExtra(Commons.URL_KEY,url);
				goDownload.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				startActivity(goDownload);
			}
			catch(Exception e)
			{
				// warning
				new QuickToast(context,e.getMessage());
			}
		}
	}	
}
