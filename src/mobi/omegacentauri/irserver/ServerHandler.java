/*
 * IR Server copyright (c) 2014 Alexander R. Pruss based on Android Web Server code copyright (C) 2009-2010 Markus Bode Internetlšsungen (bolutions.com).
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

package mobi.omegacentauri.irserver;
import java.io.*;
import java.net.*;

import android.os.Environment;
import android.util.Log;

class ServerHandler extends Thread {
	private BufferedReader in;
	private PrintWriter out;
	private Socket toClient;
	private QueryHandler queryHandler;
	private String docroot;

	ServerHandler(Socket s, QueryHandler queryHandler) {
		toClient = s;
		this.queryHandler = queryHandler;
		docroot = Environment.getExternalStorageDirectory().getPath()+"/mobi.omegacentauri.irserver/";
	}

	public void run() {
		try {
			toClient.setSoTimeout(60000);
			in = new BufferedReader(new InputStreamReader(toClient.getInputStream()));
			Log.v("IRSserver", "socket opened");
			while (handleRequest()) {}
		} catch (Exception e) {
		}		

		Server.remove(toClient);
		try {
			toClient.close();
		}
		catch (Exception ex){}
	}

	// returns true if connection is to be kept alive
	public boolean handleRequest() {
		String document = "";
		boolean head = false;    
		boolean keepAlive = true;

		try {
			// Receive data
			while (true) {
				String s = in.readLine().trim();

				if (s.equals("")) {
					break;
				}
				
				if (s.toLowerCase().matches("^connection:\\s+close")) 
					keepAlive = true;
				else if (s.startsWith("GET") || s.startsWith("HEAD")) {
					head = s.startsWith("HEAD");
					int requestOffset = s.indexOf(" HTTP/");
					if (requestOffset < 0)
						throw new Exception();
					int queryOffset = s.indexOf("?");
					Log.v("IRServer", s);
					if (queryOffset < 0) {
						document = s.substring(5,requestOffset);
					}
					else {
						document = s.substring(5,queryOffset);
						queryHandler.query(s.substring(queryOffset+1, requestOffset));
					}
					document = document.replaceAll("[/]+","/");
				}
			}
		}
		catch (Exception e) {
			return false;
		}

		// Standard-Doc
		if (document.equals("")) document = "index.html";

		// Don't allow directory traversal
		if (document.indexOf("..") != -1) document = "403.html";

		// Search for files in docroot
		document = docroot + document;
		document = document.replaceAll("[/]+","/");
		if(document.charAt(document.length()-1) == '/') document = docroot + "404.html";

		String headerBase = "HTTP/1.1 %code%\n"+
				"Server: Bolutions/1\n"+
				"Content-Length: %length%\n"+
				"Connection: Keep-Alive\n"+
				"Content-Type: text/html; charset=iso-8859-1\n\n";

		String header = headerBase;
		header = header.replace("%code%", "403 Forbidden");

		try {
			File f = new File(document);
			if (!f.exists()) {
				header = headerBase;
				header = header.replace("%code%", "404 File not found");
				document = "404.html";
			}
		}
		catch (Exception e) {}

		if (!document.equals(docroot + "403.html")) {
			header = headerBase.replace("%code%", "200 OK");
		}

		try {
			File f = new File(document);
			if (f.exists()) {
				ByteArrayOutputStream tempOut = new ByteArrayOutputStream();

				if (! document.equals(docroot + "empty.html")) {
					BufferedInputStream in = new BufferedInputStream(new FileInputStream(document));

					byte[] buf = new byte[4096];
					int count = 0;
					while ((count = in.read(buf)) != -1){
						tempOut.write(buf, 0, count);
					}
					tempOut.flush();

					in.close();
				}

				header = header.replace("%length%", ""+tempOut.size());

				BufferedOutputStream out = new BufferedOutputStream(toClient.getOutputStream());
				out.write(header.getBytes());
				if (! head)
					out.write(tempOut.toByteArray());
				out.flush();
			}
			else
			{
				// Send HTML-File (Ascii, not as a stream)
				header = headerBase;
				header = header.replace("%code%", "404 File not found");	    	  
				header = header.replace("%length%", ""+"404 - File not Found".length());	    	  
				out = new PrintWriter(toClient.getOutputStream(), true);
				out.print(header);
				out.print("404 - File not Found");
				out.flush();
			}

			return keepAlive;
		}
		catch (Exception e) {
			return false;
		}
	}
}
