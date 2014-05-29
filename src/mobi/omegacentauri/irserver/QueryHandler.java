package mobi.omegacentauri.irserver;

import android.content.Context;
import android.util.Log;

public class QueryHandler {
	static final String IR = "ir=";
	private IRPlayer irPlayer;
	
	public QueryHandler(Context context) {
		irPlayer = new IRPlayer(context);
	}
	
	public void query(String query) {
		String[] qq = query.split("&");
		for (String c: qq) {
			int equalsIndex = c.indexOf("=");
			if (equalsIndex >= 0) {
				if (c.startsWith(IR)) {
					IRCommand irCommand = new IRCommand(c.substring(equalsIndex+1));
					if (irCommand.valid)
						playIR(irCommand);
				}
			}
		}
	}
	
	private void playIR(IRCommand irCommand) {
		Log.v("IRServer", "sending command on carrier "+irCommand.carrier);
		irPlayer.play(irCommand);
	}

	public void stop() {
		irPlayer.stopPlaying();
	}
}
