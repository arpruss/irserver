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

/* Cross-Licensing Notice: This class file on its own is
   (c) 2014 Alexander R. Pruss and is also available licensed
   under the standard BSD 2 Clause license.  If you
   choose this option, make sure you include only the files that contain this
   cross-licensing notice. */

package mobi.omegacentauri.irserver;

import android.content.Context;
import android.util.Log;

public class QueryHandler {
	static final String IR = "ir=";
	private IRPlayer irPlayer;
	private boolean irEnabled;
	
	public QueryHandler(Context context) {
		irPlayer = new IRPlayer(context);
		irEnabled = true;
	}
	
	public void query(String query) {
		String[] qq = query.split("&");
		for (String c: qq) {
			int equalsIndex = c.indexOf("=");
			if (equalsIndex >= 0) {
				if (irEnabled && c.startsWith(IR)) {
					Log.v("IRServer", "ir = "+c.substring(equalsIndex+1));
					handleIR(c.substring(equalsIndex+1));
				}
			}
		}
	}
	
	private synchronized void handleIR(String c) {
		IRCommand irCommand = new IRCommand(c);
		if (irCommand.valid)
			irPlayer.play(irCommand);
	}

	public void stop() {
		irPlayer.stopPlaying();
		irEnabled = false;
	}
}
