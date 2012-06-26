package com.oguzb.websourcedownloader;

import java.io.*;

import com.oguzb.websourcedownloader.Utils.Commons;
import com.oguzb.websourcedownloader.Utils.QuickToast;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import android.util.Log;
import android.view.*;

/*
 * EditorActivity is itself a very simple text editor. If user does not have any other text editor, this one will be used.
 */

public class EditorActivity extends Activity implements View.OnClickListener
{
	private Resources res;
	private EditText editor;
	private String filepath;
	private String source;
	@Override
	 public void onCreate(Bundle savedInstanceState) 
	 {
	     super.onCreate(savedInstanceState);
	     setContentView(R.layout.editor);
	     
	     // get the file from ACTION_VIEW intent
	     res = getResources();
	     Intent intent = getIntent();
	     Uri fileUri = intent.getData();
	     try
	     {
		     if(fileUri==null)
		    	 throw new Exception(res.getString(R.string.editor_error_no_file));
		     filepath = fileUri.getPath();
		     new QuickToast(this,filepath);
		     if (filepath==null)
		    	 throw new Exception(res.getString(R.string.editor_error_empty_filename));
	     }
	     catch(Exception e)
	     {
	    	 // no intent? bye!
	    	 new QuickToast(this,e.getMessage());
	    	 finish();
	     }
	     
	     this.setTitle(filepath);
	     
	     source = new String();
	     editor = (EditText) findViewById(R.id.editorInput);
	     Button save = (Button) findViewById(R.id.editorSave);
	     save.setOnClickListener(this);
	     
	     // read file content
	     try
	     {	    	 
	    	 File f = new File(filepath);	    	 
	    	 FileInputStream fileIS = new FileInputStream(f); 
	    	 BufferedReader buf = new BufferedReader(new InputStreamReader(fileIS));    	 
	    	   String readString = new String();
	    	   while((readString = buf.readLine())!= null)
	    		   source += readString;
	    	 
	     }
	     catch (FileNotFoundException e) 
	     {
	    	 new QuickToast(this,res.getString(R.string.editor_error_file_not_found));
	     } 
	     catch (IOException e)
	     {
	    	 new QuickToast(this,"error: "+e.toString());
	     }
	     
	     // set the content to the EditText 
	     editor.setText(source);
	 }
	@Override
	public void onConfigurationChanged(Configuration config)
	{
		super.onConfigurationChanged(config);
	}
	@Override
	public void onClick(View v) 
	{
		switch(v.getId())
		{
			case R.id.editorSave:
				// when user clicks save button, save the file.
				source = editor.getText().toString();
				File file = new File(filepath);
		    	FileOutputStream fos;
		    	byte[] data = source.getBytes();
		    	try 
		    	{
		    	    fos = new FileOutputStream(file);
		    	    fos.write(data);
		    	    fos.flush();
		    	    fos.close();
		    	    
		    	    new QuickToast(this,res.getString(R.string.editor_success_saved));
		    	} 
		    	catch (FileNotFoundException e) 
		    	{
		    		new QuickToast(this,res.getString(R.string.editor_error_file_not_found));
		    	    Log.e(Commons.TAG,"file not found "+e.toString());
		    	} 
		    	catch (IOException e) 
		    	{
		    		new QuickToast(this,"error: "+e.toString());
		    	    Log.e(Commons.TAG,"could not write file");
		    	}
		}
	}
}
