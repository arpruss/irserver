/*
 * IR Server copyright (c) 2014 Alexander R. Pruss based on Android Web Server code copyright (C) 2009-2010 Markus Bode Internetl�sungen (bolutions.com).
 *
 * Licensed under the GNU General Public License v3
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

/* Cross-Licensing Notice: This class file on its own is 
   (c) 2014 Alexander R. Pruss and is also available licensed 
   under the standard BSD 2 Clause license.  If you
   choose this option, make sure you include only the files that contain this
   cross-licensing notice. */


package mobi.omegacentauri.irserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;

import mobi.omegacentauri.irserver.R;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class StartActivity extends Activity {
    public static final int NOTIFICATION_ID = 1234;
	public static final String PREF_PORT = "port";
	public static final String PREF_ADDRESS = "address";
	private ToggleButton mToggleButton;
    private EditText portField;
    private Server server;
    private static TextView mLog;
    private static ScrollView mScroll;
    
    final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle b = msg.getData();
			log(b.getString("msg"));
		}
    };
	private SharedPreferences options;
	private Button mBrowseButton;
	private int port;
	private String ipAddress = null;

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mBrowseButton = (Button)findViewById(R.id.browse);
        mBrowseButton.setVisibility(View.INVISIBLE);
        options = PreferenceManager.getDefaultSharedPreferences(this);
        mToggleButton = (ToggleButton) findViewById(R.id.toggle);
        portField = (EditText) findViewById(R.id.port);
        portField.setText(""+options.getInt(PREF_PORT, 7080));
        portField.setSelection(portField.getText().length());
        portField.clearFocus();
        mLog = (TextView) findViewById(R.id.log);
        mScroll = (ScrollView) findViewById(R.id.ScrollView01);
        writeHTML(false);
        
        mToggleButton.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				mBrowseButton.setVisibility(View.INVISIBLE);
				if( mToggleButton.isChecked() ) {
					startServer();
				} else {
					stopServer();
				}
			}
		});
        log("");
    }
    
    private void copyAsset(File f, InputStream in) {
		byte[] buffer = new byte[4096];    	
		
    	try {
			FileOutputStream out = new FileOutputStream(f);
			
			int didRead;
			while ((didRead = in.read(buffer)) > 0)
				out.write(buffer, 0, didRead);
			
			in.close();
			out.close();
		} catch (IOException e) {
		}
		
	}

	@Override
    public void onResume() {
    	super.onResume();
        portField.clearFocus();
    	boolean running = isServiceRunning();
		mToggleButton.setChecked(running);
		if (running)
			startServer();
    }

    private void stopServer() {
//    	if( server != null ) {
//    		server.stopServer();
//    		server.interrupt();
//    		log("Server was killed.");
//    		mNotificationManager.cancelAll();
//    	}
//    	else
//    	{
//    		log("Cannot kill server!? Please restart your phone.");
//    	}
    	stopService();
    }
    
    public static void log( String s ) {
    	mLog.append(s + "\n");
    	mScroll.fullScroll(ScrollView.FOCUS_DOWN);
    }
    
    private void startServer() {
    	try {
			try {
				port = Integer.parseInt(this.portField.getText().toString());
			}
			catch (NumberFormatException e) {
				port = 7080;
			}
			options.edit().putInt(PREF_PORT, port).commit();

			Log.v("IRServer", "getting ip address");
    		ipAddress = Server.getIPAddress(this);
			Log.v("IRServer", "got ip address");
    		if( ipAddress == null ) {
    			Log.v("IRServer", "no ip address");
    			new AlertDialog.Builder(this).setTitle("Error").setMessage("Please connect to a WiFi-network for starting the webserver.").setPositiveButton("OK", null).show();
    			throw new Exception("Please connect to a WiFi-network.");
    		}
    		
    		options.edit().putString(PREF_ADDRESS, ipAddress).commit();
            
    		log("Starting server "+ipAddress + ":" + port + ".");
    		updateService(true);
    		mBrowseButton.setVisibility(View.VISIBLE);

//		    server = new Server(this,ipAddress,port,mHandler);
//		    server.start();
		   
//	        Intent i = new Intent(this, StartActivity.class);
//	        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);
//
//	        Notification notif = new Notification(R.drawable.icon, "Webserver is running", System.currentTimeMillis());
//	        notif.setLatestEventInfo(this, "Webserver", "Webserver is running", contentIntent);
//	        notif.flags = Notification.FLAG_NO_CLEAR;
//	        mNotificationManager.notify(1234, notif);
    	} catch (Exception e) {
    		log(e.getMessage());
    		mToggleButton.setChecked(false);
    	}
    	
    }

    void updateService(boolean value) {		
		if (value) {
			restartService();
    	}
		else {
			stopService();
		}
    }
    
	void restartService() {
		stopService();
		Log.v("IRServer", "starting service");
		Intent i = new Intent(this, ServerService.class);
		startService(i);
	}

	void stopService() {
		Log.v("IRServer", "stopService");
		
		stopService(new Intent(this, ServerService.class));
	}	
	
	private boolean isServiceRunning() {
		ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
		List<RunningServiceInfo> list = am.getRunningServices(Integer.MAX_VALUE);
		for (RunningServiceInfo s: list) {
			if (s.service.getClassName().equals(ServerService.class.getName())) {
				Log.v("IRServer", "Service already running");
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.overwrite:
			writeHTML(true);
			return true;
		case R.id.license:
			licenses();
			return true;
		case R.id.options:
			Intent i = new Intent(this, Options.class);
			startActivity(i);
			return true;
		}
		return false;
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		
		return true;
	}
	
	private void licenses() {
		AssetManager assets = getAssets();
		try {
			String licenses = "";
			BufferedReader reader = new BufferedReader(new InputStreamReader(assets.open("licenses.txt")));
			String line;
			while(null != (line = reader.readLine()))
				licenses += line;
			reader.close();
			new AlertDialog.Builder(this).setTitle("Licenses").setMessage(Html.fromHtml(licenses)).show();
		} catch (IOException e) {
		}
	}
	
	private void writeHTML(boolean overwrite) {
        String pfad = Environment.getExternalStorageDirectory().getPath()+"/mobi.omegacentauri.irserver";
        (new File(pfad)).mkdir();
       
        AssetManager assets = getAssets();
        String[] files;
		try {
			files = assets.list("html");
	        for (String s: files) {
	        	File f = new File(pfad + "/" +s);
	        	if (overwrite || ! f.exists()) {
	        		log("copying html/"+s);
	        		copyAsset(f, assets.open("html/"+s));
	        	}
	        }
		} catch (IOException e) {
		}
        

	}
	
	public void onBrowse(View v) {
		if (ipAddress == null) {
			mBrowseButton.setVisibility(View.INVISIBLE);
			return;
		}
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://"+ipAddress+":"+port)));
    }

}