package mobi.omegacentauri.irserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.EventLog;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class ServerService extends Service
{	
	private Server server;
	private Handler mHandler;
	private SharedPreferences options;
	private NotificationManager mNotificationManager;


	@SuppressLint("NewApi")
	@Override
	public void onCreate() {
		Log.v("IRServer", "onCreate()");
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    
		options = PreferenceManager.getDefaultSharedPreferences(this);
				Notification n = new Notification(
						R.drawable.icon, 
						"IRServer", 
						System.currentTimeMillis());
		String address = options.getString(StartActivity.PREF_ADDRESS, "127.0.0.1");
		Intent i = new Intent(this, StartActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		n.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT; 
		n.setLatestEventInfo(this, "IRServer", 
				"IRServer running", 
				PendingIntent.getActivity(this, 0, i, 0));
		
		if (Build.VERSION.SDK_INT >= 5)
			startForeground(StartActivity.NOTIFICATION_ID, n);
		else
			mNotificationManager.notify(StartActivity.NOTIFICATION_ID, n);

		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();

//		String ipAddress = StartActivity.intToIp(wifiInfo.getIpAddress());
//
//		if( wifiInfo.getSupplicantState() != SupplicantState.COMPLETED) {
//			new AlertDialog.Builder(this).setTitle("Error").setMessage("Please connect to a WIFI-network for starting the webserver.").setPositiveButton("OK", null).show();
//			stopSelf();
//		}

		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				Bundle b = msg.getData();
				if (b.getString("dead") != null) {
					stopSelf();
					Log.e("IRServer", "terminated");
				}
				else {
					Log.v("IRServer", b.getString("msg"));
				}
			}
		};

		try {
			server = new Server(this,address,options.getInt(StartActivity.PREF_PORT, 7080),mHandler);
			server.start();
			Log.v("IRServer", "started server on "+address);
		} catch (IOException e) {
			stopSelf();
		}
	}

	@SuppressLint("NewApi")
	@Override
	public void onDestroy() {
		
		if (server != null) {
			server.stopServer();
			server.interrupt();
			server = null;
		}
		if (Build.VERSION.SDK_INT >= 5)
			stopForeground(true);
		else
			mNotificationManager.cancelAll();
	}

	@Override
	public void onStart(Intent intent, int flags) {
		Log.v("IRServer", "onStart");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v("IRServer", "onStartCommand");
		onStart(intent, flags);
		return START_STICKY;
	}


	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}


}
