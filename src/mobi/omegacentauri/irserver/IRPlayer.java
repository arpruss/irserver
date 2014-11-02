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
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.preference.PreferenceManager;
import android.util.Log;

public class IRPlayer {
	Context context;
	AudioTrack track;
	Thread writeThread = null;
	static final int STREAM = AudioManager.STREAM_MUSIC;
	int count;
	long startTime;
	long endTime;
	int startVolume;
	int stereoMode;
	int pcmMode;
	private AudioManager audioManager;
	
	public IRPlayer(Context context) {
		this.context = context;
		SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(context);
		stereoMode = Integer.parseInt(options.getString(Options.PREF_STEREO, ""+Options.OPT_STEREO_SAME));
		pcmMode = Integer.parseInt(options.getString(Options.PREF_AUDIO_MODE, ""+Options.OPT_AUDIO_MODE_PCM16));
		track = null;
		audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		startVolume = audioManager.getStreamVolume(STREAM);
	}
	
	public void stop() {	
		stopPlaying();
		audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, startVolume, 0);
	}
	
	public void stopPlaying() {
		Log.v("IRServer", "track.stop");
		if (track != null) {
			try {
				track.flush();
			}
			catch (Exception e) {
				Log.e("IRServer", "flushing track error "+e);
			}
			try {
				track.stop();
			}
			catch (Exception e) {
				Log.e("IRServer", "stopping track error "+e);
			}
			try {
				track.release();
			}
			catch (Exception e) {
				Log.e("IRServer", "releasing track error "+e);
			}
			track = null;
		}
		Log.v("IRServer", "interrupt writing thread");
		if (writeThread != null) {
			try {
			    writeThread.interrupt();
			}
			catch (Exception e) {
				Log.e("IRServer", "interrupting thread error "+e);
			}
			writeThread = null;
		}
	}
	
	public void play(final IRCommand command) {
		Log.v("IRServer", "play:stop");
		stopPlaying();

		Log.v("IRServer", "play mode "+command.playMode);
		if (command.playMode == IRCommand.PLAY_STOP) {
			return;
		}
		
		Log.v("IRServer", "playing on carrier "+command.carrier);
		
		final IRToAudio converter = new IRToAudio(command, stereoMode, pcmMode);
		final byte[] samples = converter.getSamples();
		
		int format = (converter.bits == 16) ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
		int bufferSize = AudioTrack.getMinBufferSize(IRToAudio.SAMPLE_FREQ, AudioFormat.CHANNEL_CONFIGURATION_STEREO, format);
		if (bufferSize < samples.length)
			bufferSize = samples.length;
		try {
			track = new AudioTrack(STREAM, IRToAudio.SAMPLE_FREQ, 
					(converter.channels == 2) ? AudioFormat.CHANNEL_CONFIGURATION_STEREO : AudioFormat.CHANNEL_CONFIGURATION_MONO, 
					format, 
					bufferSize, AudioTrack.MODE_STREAM);
		}
		catch (Exception e) {
			Log.e("IRRoomba", "IRPlayer error "+e);
			track = null;
		}
		track.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
		track.play();
		
		writeThread = new Thread(new Runnable(){
			public void run() {
				try {
					audioManager.setStreamVolume(STREAM, audioManager.getStreamMaxVolume(STREAM), 0);
					long time = 0;
					int count = 0;
					while(!done(count, time)) {
						track.write(samples, 0, samples.length);
						time += converter.getSamplesTimeMicroseconds();
						count++;
					}
				}
				catch(Exception e) {
				}
			}

			private boolean done(int count, long time) {
				if (Thread.interrupted())
					return true;
				switch(command.playMode) {
				case IRCommand.PLAY_INFINITE:
					return false;
				case IRCommand.PLAY_ONCE:
					return count > 0;
				case IRCommand.PLAY_COUNT:
					return count >= command.repeatCount;
				case IRCommand.PLAY_TIME:
					return time >= command.repeatTimeMicroseconds;
				default:
					return true;
				}
			}});
		
		writeThread.start();		
	}	
}
