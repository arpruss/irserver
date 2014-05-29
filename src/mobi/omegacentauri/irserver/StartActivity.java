/*
 * Copyright (C) 2009,2010 Markus Bode Internetlšsungen (bolutions.com)
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
 * @author Markus Bode
 * @version $Id: StartActivity.java 727 2011-01-02 13:04:32Z markus $
 */

package mobi.omegacentauri.irserver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class StartActivity extends Activity {
    public static final int NOTIFICATION_ID = 1234;
	public static final String PREF_PORT = "port";
	public static final String PREF_ADDRESS = "address";
	private ToggleButton mToggleButton;
    private EditText port;
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

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        options = PreferenceManager.getDefaultSharedPreferences(this);
        mToggleButton = (ToggleButton) findViewById(R.id.toggle);
        port = (EditText) findViewById(R.id.port);
        port.setText(""+options.getInt(PREF_PORT, 7080));
        mLog = (TextView) findViewById(R.id.log);
        mScroll = (ScrollView) findViewById(R.id.ScrollView01);
        
        String pfad = Environment.getExternalStorageDirectory().getPath()+"/mobi.omegacentauri.irserver";
        (new File(pfad)).mkdir();
        
        AssetManager assets = getAssets();
        String[] files;
		try {
			files = assets.list("html");
	        for (String s: files) {
	        	File f = new File(pfad + "/" +s);
//	        	if (! f.exists()) {
	        		Log.v("IRServer", "copying html/"+s);
	        		copyAsset(f, assets.open("html/"+s));
//	        	}
	        }
		} catch (IOException e) {
		}
        
        mToggleButton.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				if( mToggleButton.isChecked() ) {
					int portNum;
					try {
						portNum = Integer.parseInt(port.getText().toString());
					}
					catch (NumberFormatException e) {
						portNum = 7080;
					}
					options.edit().putInt(PREF_PORT, portNum).commit();
					startServer(portNum);
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
    	mToggleButton.setChecked(isServiceRunning());
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
    
    private void startServer(int port) {
    	try {
			Log.v("IRServer", "getting ip address");
    		String ipAddress = Server.getIPAddress(this);
			Log.v("IRServer", "got ip address");
    		if( ipAddress == null ) {
    			Log.v("IRServer", "no ip address");
    			new AlertDialog.Builder(this).setTitle("Error").setMessage("Please connect to a WiFi-network for starting the webserver.").setPositiveButton("OK", null).show();
    			throw new Exception("Please connect to a WiFi-network.");
    		}
    		
    		options.edit().putString(PREF_ADDRESS, ipAddress).commit();
            
    		log("Starting server "+ipAddress + ":" + port + ".");
    		updateService(true);

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
	
}