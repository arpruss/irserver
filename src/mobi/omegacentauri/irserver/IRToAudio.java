package mobi.omegacentauri.irserver;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import android.util.Log;

public class IRToAudio {
	public int channels;
    static final int SAMPLE_FREQ = 48000; // 44100;
    public int bits;
    public int samplePairByteSize; 
    byte[] samples;
    int samplePosition;
	double phase;
	private double phaseDelta;
	int stereoMode;
	
public IRToAudio(IRCommand irCommand, int stereoMode, int bitMode) {
	Log.v("IRServer", "hello");

	phaseDelta =  (double)(irCommand.carrier / 2) / SAMPLE_FREQ * 2 * Math.PI;
		
		if (bitMode == Options.OPT_AUDIO_MODE_PCM16)
			bits = 16;
		else
			bits = 8;
		
		this.stereoMode = stereoMode;
		
		if (stereoMode == Options.OPT_STEREO_SAME)
			channels = 1;
		else
			channels = 2;
		
		samplePairByteSize = channels * bits / 8;

		int samplePairsCount = 0;
		
		for (int i=0; i<irCommand.pulses.length; i++) 
			samplePairsCount += beep(irCommand.pulses[i].timeInMicroSeconds, irCommand.pulses[i].on, false);
		
		if (irCommand.playMode != IRCommand.PLAY_ONCE) 
			samplePairsCount += beep(irCommand.repeatPauseMicroseconds, false, false);

		Log.v("IRServer", "generating "+samplePairsCount);
		
		samples = new byte[samplePairsCount * samplePairByteSize];
		samplePosition = 0;

		phase = 0;
		
		for (int i=0; i<irCommand.pulses.length; i++) 
			beep(irCommand.pulses[i].timeInMicroSeconds, irCommand.pulses[i].on, true);
		
		if (irCommand.playMode != IRCommand.PLAY_ONCE) 
			beep(irCommand.repeatPauseMicroseconds, false, true);
		
		Log.v("IRServer", "playing "+samples.length+" "+samplePosition);
	}
	
	public int beep(long microsec, boolean sound, boolean play) {
		int samplePairsCount = (int)(microsec * SAMPLE_FREQ / 1000000);
		
		if (play) {
			if (sound) {
				for (int t = 0 ; t < samplePairsCount ; t++) {
					double v1 = Math.cos(phase);
					
					if (bits == 8) {
						samples[samplePosition++] = (byte)(int)(128 + 126*v1);
					}
					else {
						short x = (short)(int)(32766 * v1);
						samples[samplePosition++] = (byte)x;
						samples[samplePosition++] = (byte)(x>>8);							
					}
					
					if (stereoMode != Options.OPT_STEREO_SAME) {
						double v2;
						
						if (stereoMode == Options.OPT_STEREO_90)
							v2 = Math.sin(phase);
						else
							v2 = -v1;
						
						if (bits == 8) {
							samples[samplePosition++] = (byte)(int)(128 + 126*v2);
						}
						else {
							short x = (short)(int)(32766 * v2);
							samples[samplePosition++] = (byte)x;
							samples[samplePosition++] = (byte)(x>>8);							
						}
					}
					
					phase += phaseDelta;
					
					if (phase > 2 * Math.PI) {
						phase -= 2 * Math.PI;
					}			
				}
			}
			else {
				if (bits == 8) {
					// 16-bit is filled to zero by default
					Arrays.fill(samples, samplePosition, samplePosition + samplePairsCount * samplePairByteSize, (byte)128);
				}
				samplePosition += samplePairsCount * samplePairByteSize;
				phase += phaseDelta * samplePairsCount;
				phase %= 2 * Math.PI;
			}
		}
		
		return samplePairsCount;
	}

	public byte[] getSamples() {
		Log.v("IRServer", "samples requested");
//		try {
//			FileOutputStream w = new FileOutputStream("/sdcard/data"+BITS+".wav");
//			w.write(samples);
//			w.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		return samples;
	}
	
	public long getSamplesTimeMicroseconds() {
		return samples.length * (long)1000000 / ( samplePairByteSize * SAMPLE_FREQ );
	}
}
