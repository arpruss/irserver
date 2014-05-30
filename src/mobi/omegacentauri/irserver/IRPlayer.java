package mobi.omegacentauri.irserver;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class IRPlayer {
	Context context;
	AudioTrack track;
	Thread writeThread = null;
	static final int REPEAT_NONE = 0;
	static final int REPEAT_COUNT = 1;
	static final int REPEAT_TIME = 2;
	int repeatMode;
	int count;
	long startTime;
	long endTime;
	int startVolume;
	private AudioManager audioManager;
	
	public IRPlayer(Context context) {
		this.context = context;
		track = null;
		audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
	}
	
	public void stop() {	
		stopPlaying();
		audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, startVolume, 0);
	}
	
	public void stopPlaying() {
		if (track != null)
			track.stop();
		if (writeThread != null)
			writeThread.interrupt();
	}
	
	public void play(final IRCommand command) {
		stopPlaying();

		if (command.playMode == IRCommand.PLAY_STOP) {
			Log.v("IRServer", "stop requested");
			return;
		}
		
		final IRToAudio converter = new IRToAudio(command);
		final byte[] samples = converter.getSamples();
		
		Log.v("IRServer", "playing "+samples.length+" for "+converter.getSamplesTimeMicroseconds()+" us");
		
		int format = (IRToAudio.BITS == 16)	 ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
		int bufferSize = AudioTrack.getMinBufferSize(IRToAudio.SAMPLE_FREQ, AudioFormat.CHANNEL_CONFIGURATION_STEREO, format);
		if (bufferSize < samples.length)
			bufferSize = samples.length;
		track = new AudioTrack(AudioManager.STREAM_MUSIC, IRToAudio.SAMPLE_FREQ, 
				AudioFormat.CHANNEL_CONFIGURATION_STEREO, 
				format, 
				bufferSize, AudioTrack.MODE_STREAM);
		track.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
		track.play();
		
		writeThread = new Thread(new Runnable(){
			public void run() {
				try {
					audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
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
