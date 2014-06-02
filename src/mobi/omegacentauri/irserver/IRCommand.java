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

import java.util.ArrayList;

import android.util.Log;

public class IRCommand {
	int carrier;
	Pulse[] pulses;
	public int repeatCount;
	public long repeatTimeMicroseconds;
	boolean valid;
	public int repeatPauseMicroseconds;
	public int playMode;
	static public final int PLAY_ONCE = 0;
	static public final int PLAY_COUNT = 1;
	static public final int PLAY_TIME = 2;
	static public final int PLAY_INFINITE = 3;
	static public final int PLAY_STOP = 4;
	static public final String ONCE = "once";
	static public final String COUNT = "count=";
	static public final String TIME = "time=";
	static public final String INFINITE = "infinite";
	static public final String STOP = "stop";
	
	public IRCommand(String irString) {
		valid = false;
		
		String[] commands = irString.split(":");
		
		int index = repeats(commands, 0);
		
		if (index < 0 || commands.length <= index || playMode == PLAY_STOP)
			return;
		
		if (commands[index].equals("roomba")) {
			translateRoomba(commands, index+1);
		}
		else if (commands[index].equals("tk")) {
			translateThamesKosmos(commands, index+1);
		}
		else if (commands[index].equals("raw")) {
			translateRaw(commands, index+1);
		}
		else if (commands[0].equals("pronto")) {
			translatePronto(commands, index+1);
		}
		
		if (!valid) {
			Log.e("IRServer", "invalid command "+irString);
		}
	}
	
	private void translateRoomba(String[] commands, int index) {
		carrier = 38000;

		repeatPauseMicroseconds = 20000;

		if (index < 0 || commands.length <= index)
			return;
		
		int b;
		
		try {
			b = Integer.parseInt(commands[index]);
		}
		catch (NumberFormatException e) {
			return;
		}
		
		pulses = new Pulse[2*8];
		for (int i=0 ; i<8; i++) {
			if (0 != (b & (1<<(7-i)))) {
				pulses[2*i] = new Pulse(3000, true);
				pulses[2*i+1] = new Pulse(1000, false);
			}
			else {
				pulses[2*i] = new Pulse(1000, true);
				pulses[2*i+1] = new Pulse(3000, false);
			}
		}
		
		valid = true;
	}
	
	private void translatePronto(String[] commands, int index) {
		repeatPauseMicroseconds = 20000;

		if (index < 0 || commands.length <= index)
			return;
		
		String cleaned = commands[index].replaceAll("^\\s+", "").replaceAll("\\s+$", "").replaceAll("\\s+", " ").replaceAll("\\+", " ").replaceAll("%20",  " ");
		String[] prontoData = cleaned.split(" ");
		
		if (prontoData.length < 5)
			return;
		
		int[] values = new int[prontoData.length];
		
		try {
			for (int i=0; i<prontoData.length; i++)
				values[i] = Integer.parseInt(prontoData[i], 16);
		}
		catch (Exception e) {
			return;
		}
			
		if (values[0] != 0 || values[1] == 0)
			return;
		
		carrier = (int)(1000000/(values[1] * .241246));
		
		int start;
		int count;
		
		if (playMode == PLAY_ONCE) {
			start = 4;
			if (values[2] == 0)
				count = values[3];
			else
				count = values[2];
		}
		else {
			if (values[3] == 0) {
				start = 4;
				count = values[2];
			}
			else {
				start = 4 + 2 * values[2];
				count = values[3];
			}
		}
		
		if (start + 2 * count > values.length)
			return;
		
		pulses = new Pulse[2*count];

		for (int i = 0; i < count ; i++) {
			pulses[2*i] = new Pulse(25 * values[start + 2*i], true);
			pulses[2*i+1] = new Pulse(25 * values[start + 2*i + 1], false);
		}

		valid = true;
	}
	
	private void translateThamesKosmos(String[] commands, int index) {
		repeatPauseMicroseconds = 20000;
		carrier = 38000;

		if (index < 0 || commands.length <= index)
			return;
		
		int b;
		
		try {
			b = Integer.parseInt(commands[index]);
		}
		catch (NumberFormatException e) {
			return;
		}
		
		makePulses(b, 2272, 762, 734);
		
		valid = true;
	}
	
	private void makePulses(int b, int one, int zero, int spacing) {
		pulses = new Pulse[8*2];
		for (int i = 0; i < 8 ; i++) {
			if ((b & (1<<(7-i))) != 0) {
				pulses[2*i] = new Pulse(one, true);
			}
			else {
				pulses[2*i] = new Pulse(zero, true);
			}
			pulses[2*i+1] = new Pulse(spacing, false);
		}
	}

	private void translateRaw(String[] commands, int index) {
		if (index < 0 || commands.length <= index)
			return;
		
		try {
			repeatPauseMicroseconds = Integer.parseInt(commands[index]);
		}
		catch (NumberFormatException e) {
			repeatPauseMicroseconds = 1000;
		}
		index++;
		
		if (commands.length <= index)
			return;
		
		try {
			carrier = Integer.parseInt(commands[index]);
		}
		catch (NumberFormatException e) {
			carrier = 38000;
		}
		index++;
		
		pulses = new Pulse[commands.length - index];
		
		boolean state = true;
		
		for (int i = 0 ; i < pulses.length ; i++) {
			pulses[i] = new Pulse();
			
			pulses[i].on = state;
			
			try {
				pulses[i].timeInMicroSeconds = Integer.parseInt(commands[index+i]);
			}
			catch (NumberFormatException e) {
				pulses[i].timeInMicroSeconds = 0;
			}
			
			state = ! state;
			
		}
		
		valid = true;
	}
	
	private int repeats(String[] commands, int index) {
		if (commands.length <= index)
			return -1;
		
		if (commands[index].equals(ONCE)) {
			playMode = PLAY_ONCE;
		}
		else if (commands[index].startsWith(COUNT)) {
			playMode = PLAY_COUNT;

			try {
				repeatCount = Integer.parseInt(commands[index].substring(COUNT.length()));
			}
			catch (NumberFormatException e) {
				Log.e("IRServer", "Error in "+commands[index]);
				return -1;
			}			
		}
		else if (commands[index].startsWith(TIME)) {
			playMode = PLAY_TIME;
			
			try {
				repeatTimeMicroseconds = Integer.parseInt(commands[index].substring(TIME.length()));
			}
			catch (NumberFormatException e) {
				return -1;
			}
		}
		else if (commands[index].equals(INFINITE)) {
			playMode = PLAY_INFINITE;
		}
		else if (commands[index].equals(STOP)) {
			playMode = PLAY_STOP;
			valid = true;
		}
		else {
			return -1;
		}
		
		index++;
		
		return index;
	}
	
	class Pulse {
		boolean on;
		int timeInMicroSeconds;

		public Pulse() {
			on = false;
			timeInMicroSeconds = 0;
		}
		
		public Pulse(int t, boolean b) {
			timeInMicroSeconds = t;
			on = b;
		}
	}
}
