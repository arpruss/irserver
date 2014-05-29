/*
 * Copyright (C) 2009,2010 Markus Bode Internetlšsungen (bolutions.com)
 * Copyright (C) 2014 Omega Centauri Software
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
 * @version $Id: Server.java 727 2011-01-02 13:04:32Z markus $
 */

package mobi.omegacentauri.irserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.Format;
import java.util.Enumeration;
import java.util.LinkedList;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.util.Log;

public class Server extends Thread {
	private ServerSocket listener = null;
	private static Handler mHandler;
	private boolean running = true;
	private QueryHandler queryHandler;
	
	public static LinkedList<Socket> clientList = new LinkedList<Socket>();
	
    public Server(Context context, String ip, int port, Handler handler) throws IOException {
		super();
		mHandler = handler;
		queryHandler = new QueryHandler(context);
		InetAddress ipadr = InetAddress.getByName(ip);
		listener = new ServerSocket(port,0,ipadr);
		Log.v("IRServer", "listening on "+ip+":"+port);
	}
    
    private static void send(String s) {
    	Message msg = new Message();
    	Bundle b = new Bundle();
    	b.putString("msg", s);
    	msg.setData(b);
    	mHandler.sendMessage(msg);
    }
    
    private static void dead() {
    	Message msg = new Message();
    	Bundle b = new Bundle();
    	b.putString("dead", "dead");
    	msg.setData(b);
    	mHandler.sendMessage(msg);
    }
    
	@Override
	public void run() {
		while( running ) {
			try {
				send("Waiting for connections.");
				Socket client = listener.accept();
			    send("New connection from " + client.getInetAddress().toString());
				new ServerHandler(client, queryHandler).start();
				clientList.add(client);
			} catch (IOException e) {
				send(e.getMessage());
				if (listener.isClosed()) {
					Log.e("IRServer", "ServerSocket has closed");
				}
			}
		}
	}

	public void stopServer() {
		running = false;
		try {
			listener.close();
		} catch (IOException e) {
			send(e.getMessage());
		}
		queryHandler.stop();
	}
	
	public synchronized static void remove(Socket s) {
	    send("Closing connection: " + s.getInetAddress().toString());
        clientList.remove(s);      
    }

    public static String intToIp(int i) {
        return ((i       ) & 0xFF) + "." +
               ((i >>  8 ) & 0xFF) + "." +
               ((i >> 16 ) & 0xFF) + "." +
               ( i >> 24   & 0xFF);
    }
    
	public static String getIPAddress(Context context) {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (wifiManager != null) {
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			if (wifiInfo != null) {
				int a = wifiInfo.getIpAddress();
				if (a != 0)
					return Formatter.formatIpAddress(a);
			}
		}

		
		
//		Log.v("IRServer", "getting IP");
//		android.net.ConnectivityManager cm = 
//				(android.net.ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
//		if (cm == null)
//			return null;
//		Log.v("IRServer", "getting network info");
//		NetworkInfo netInfo = cm.getActiveNetworkInfo();
//		Log.v("IRServer", "got network info");
//		
//		if (netInfo == null || !netInfo.isConnected())
//			return null;

		Log.v("IRServer", "should be available");
		Enumeration<NetworkInterface> en;
		try {
			en = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			return null;
		}
		
		while(en.hasMoreElements()) {
			NetworkInterface i = en.nextElement();
			Enumeration<InetAddress> ia = i.getInetAddresses();
			while(ia.hasMoreElements()) {
				InetAddress a = ia.nextElement();
				if (!a.isLoopbackAddress())
					return a.getHostAddress().toString();
			}
		}
		
		return null;
	}

}