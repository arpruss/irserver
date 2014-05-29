package mobi.omegacentauri.irserver;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import android.util.Log;

public class IRToAudio {
    int carrierFreq;
    static final int MODE_UPTIME = 1;
    static final int SAMPLE_FREQ = 48000; // 44100;
    double carrierPulseTime;
    double sampleTime = 1./SAMPLE_FREQ;
    static final int BITS = 16;
    ByteArrayOutputStream samples;
	private int mode;
	private int oneMicrosecs;
	private int zeroMicrosecs;
	private int pauseMicrosecs;
	double currentTime;
	double phase;
	int halfFreq;
	private double phaseDelta;
	static final double[] phases = { 1., 0, -1., 0};
	
	public /*static*/ void main(String[] args) throws IOException
	  {
		FileOutputStream w = new FileOutputStream("test.wav");
		
		IRToAudio ir = new IRToAudio(38200, 1, 2272, 762, 734);
		// http://192.168.1.103:8080/ir.html?ir=raw:20:0:20000:38000:790:708:788:708:788:710:786:712:762:734:2274:734:762:734:2272
		int[] times = { 790, 708, 788, 708, 788,
				710, 786, 712, 762, 734, 2274,
				734, 762, 734, 2272 };
		
		for (int i = 0; i < 30; i++) {
//			for (int j = 0; j < times.length ; j++) {
//				ir.beep(times[j], j%2 == 0);
//			}
//			System.out.println("Done!");
			ir.bits(8, new byte[] { 5 });
			ir.beep(10000, false);
		}
		w.write(ir.getWAV());
		System.out.println("Done!");
	    w.close();
	  }
	
	public IRToAudio(IRCommand irCommand) {
		this.carrierFreq = irCommand.carrier;
		this.halfFreq = this.carrierFreq / 2;
		this.phaseDelta =  (double)this.halfFreq / SAMPLE_FREQ * 2 * Math.PI;
		
		clear();
		
		for (int i=0; i<irCommand.pulses.length; i++) 
			beep(irCommand.pulses[i].timeInMicroSeconds, irCommand.pulses[i].on);
		
		if (irCommand.repeatMode != IRCommand.PLAY_ONCE) 
			beep(irCommand.repeatPauseMicroseconds, false);
	}
	
	public IRToAudio(int carrierFreq, int mode, int oneMicrosecs, int zeroMicrosecs, int pauseMicrosecs) {
		this.carrierFreq = carrierFreq;
		this.halfFreq = carrierFreq / 2;
		this.mode = mode; 
		this.oneMicrosecs = oneMicrosecs;
		this.zeroMicrosecs = zeroMicrosecs;
		this.pauseMicrosecs = pauseMicrosecs;
		this.phaseDelta = (double)this.halfFreq / SAMPLE_FREQ * 2 * Math.PI;
	    System.out.println("phaseDelta="+phaseDelta);

		clear();
    }
	
	public void writeWord(ByteArrayOutputStream out, int w) {
		out.write(w & 0xFF);
		out.write((w >> 8) & 0xFF);
	}
	
	public void writeDWord(ByteArrayOutputStream out, int w) {
		out.write(w & 0xFF);
		w >>= 8;
		out.write(w & 0xFF);
		w >>= 8;
		out.write(w & 0xFF);
		w >>= 8;
		out.write(w & 0xFF);
	}
	
	public void writeString(ByteArrayOutputStream out, String s) {
		try {
			out.write(s.getBytes("US-ASCII"));
		}
		catch(Exception e) {
		}
	}
	
	public void clear() {
		samples = new ByteArrayOutputStream();
		currentTime = 0;
		phase = 0;
	}
	
	public void writeSampleValue(double v1, double v2) {
		if (BITS == 8) {
			samples.write(128 + (int)(126 * v1));
			samples.write(128 + (int)(126 * v2));
		}
		else {
//			if (v1 > .3)
//				v1 = 1;
//			else if (v1 < -.3)
//				v1 = -1;
//			else
//				v1 = 0;
//			if (v2 > .3)
//				v2 = 1;
//			else if (v2 < -.3)
//				v2 = -1;
//			else
//				v2 = 0;
			writeWord(samples,(int)(32000 * v1));
			writeWord(samples,(int)(32000 * v2));
		}
	}
	
	public void beep(long microsec, boolean sound) {
		long numSamples = microsec * SAMPLE_FREQ / 1000000;
		
		System.out.println("beep "+microsec+" "+sound);

		for (long t = 0 ; t < numSamples ; t++) {
			if (! sound)
				writeSampleValue(0., 0.);
			else {
				writeSampleValue(Math.cos(phase), Math.sin(phase));
			}
			
			phase += phaseDelta;
			
			if (phase > 2 * Math.PI) {
				phase -= 2 * Math.PI;
			}			
		}
	}

	public void bit(int value) {
	    System.out.println("value = "+value);
	    beep(value == 0 ? zeroMicrosecs : oneMicrosecs, true);
		beep(pauseMicrosecs, false);
	}
	
	public void bits(int bits, byte[] data) {
		for (int i=0; i<bits; i++) {
			bit(1 & (data[i/8] >> (7-i%8)));
		}
	}

	public byte[] getSamples() {
		Log.v("IRServer", "samples requested");
		return samples.toByteArray();
	}
	
	public long getSamplesTimeMicroseconds() {
		return samples.size() * (long)1000000 / ( 2 * BITS / 8 * SAMPLE_FREQ );
	}
	
	public byte[] getWAV() {
		byte[] sampleBytes = samples.toByteArray();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writeString(out, "RIFF");
		writeDWord(out,36 + sampleBytes.length);
		writeString(out, "WAVE");
		writeString(out, "fmt ");
		writeDWord(out, 16);
		writeWord(out, 1); // PCM
		writeWord(out, 2); // channels
		writeDWord(out, SAMPLE_FREQ); 
		writeDWord(out, 2 * BITS / 8 * SAMPLE_FREQ); // byte rate
		writeWord(out, 2 * BITS / 8); // block align
		writeWord(out, BITS); // bits per sample
		writeString(out, "data");
		writeDWord(out, sampleBytes.length);
		try {
			out.write(sampleBytes);
		} catch (IOException e) {
		}
		return out.toByteArray();
	}
}
