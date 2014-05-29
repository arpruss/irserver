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
	public int repeatMode;
	static public final int PLAY_ONCE = 0;
	static public final int PLAY_COUNT = 1;
	static public final int PLAY_TIME = 2;
	static public final int PLAY_INFINITE = 3;
	static public final String ONCE = "once";
	static public final String COUNT = "count=";
	static public final String TIME = "time=";
	static public final String INFINITE = "infinite";
	boolean stop;
	
	public IRCommand(String irString) {
		valid = false;
		
		stop = false;
		
		String[] commands = irString.split(":");
		
		if (commands[0].equals("roomba")) {
			translateRoomba(commands, repeats(commands, 1));
		}
		else if (commands[0].equals("tk")) {
			translateThamesKosmos(commands, repeats(commands, 1));
		}
		else if (commands[0].equals("raw")) {
			translateRaw(commands, repeats(commands, 1));
		}
		else if (commands[0].equals("stop")) {
			stop = true;
			valid = true;
		}
		else if (commands[0].equals("pronto")) {
			translatePronto(commands, repeats(commands, 1));
		}
		if (!valid) {
			Log.e("IRServer", "invalid command "+irString);
		}
	}
	
	private void translateRoomba(String[] commands, int index) {
		carrier = 38000;

		repeatPauseMicroseconds = 20000;
		Log.v("IRServer", "roomba");

		if (index < 0 || commands.length <= index)
			return;
		
		int b;
		
		try {
			b = Integer.parseInt(commands[index]);
		}
		catch (NumberFormatException e) {
			return;
		}
		
		Log.v("IRServer", "value = "+b);
		

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
		
		Log.v("IRServer", "pronto "+index+ " "+commands.length);

		String cleaned = commands[index].replaceAll("^\\s+", "").replaceAll("\\s+$", "").replaceAll("\\s+", " ").replaceAll("\\+", " ").replaceAll("%20",  " ");
		Log.v("IRServer", cleaned);
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
		
		if (repeatMode == PLAY_ONCE) {
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

		Log.v("IRServer", "pulses "+pulses.length);
		
		valid = true;
	}
	
	private void translateThamesKosmos(String[] commands, int index) {
		repeatPauseMicroseconds = 20000;
		carrier = 38000;

		Log.v("IRServer", "tk");

		if (index < 0 || commands.length <= index)
			return;
		
		int b;
		
		try {
			b = Integer.parseInt(commands[index]);
		}
		catch (NumberFormatException e) {
			return;
		}
		
		Log.v("IRServer", "value = "+b);

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
		
		Log.v("IRServer", "Translating raw");
		
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
		
		Log.v("IRServer", "pulses = "+pulses.length);
		for (int i = 0 ; i < pulses.length ; i++) {
			pulses[i] = new Pulse();
			
			pulses[i].on = state;
			
			try {
				pulses[i].timeInMicroSeconds = Integer.parseInt(commands[index+i]);
			}
			catch (NumberFormatException e) {
				pulses[i].timeInMicroSeconds = 0;
			}
			
			Log.v("IRServer", "PULSE "+pulses[i].timeInMicroSeconds+" "+pulses[i].on);
			
			state = ! state;
			
		}
		
		valid = true;
	}
	
	private int repeats(String[] commands, int index) {
		
		if (commands[index].equals(ONCE)) {
			repeatMode = PLAY_ONCE;
		}
		else if (commands[index].startsWith(COUNT)) {
			repeatMode = PLAY_COUNT;

			try {
				repeatCount = Integer.parseInt(commands[index].substring(COUNT.length()));
			}
			catch (NumberFormatException e) {
				Log.v("IRServer", "Ooops "+commands[index]);
				return -1;
			}			
		}
		else if (commands[index].startsWith(TIME)) {
			repeatMode = PLAY_TIME;
			
			try {
				repeatTimeMicroseconds = Integer.parseInt(commands[index].substring(TIME.length()));
			}
			catch (NumberFormatException e) {
				return -1;
			}
		}
		else if (commands[index].equals(INFINITE)) {
			repeatMode = PLAY_INFINITE;
		}
		else
			return -1;
		
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
