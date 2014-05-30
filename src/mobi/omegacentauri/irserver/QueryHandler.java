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
