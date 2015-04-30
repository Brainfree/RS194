/*
 * Copyright (c) 2015. Dane
 */

package pre194;

import java.util.zip.GZIPInputStream;

/**
 * Created by Sven on 4/20/2015.
 */
public class Audio implements Runnable {

	public MidiPlayer midiPlayer;

	public int volume = 256;

	private String next = null;
	private String current = null;

	public Audio() {
		try {
			midiPlayer = new MidiPlayer();
			midiPlayer.setVolume(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void playImmediately() {
		current = next;

		// play non-null
		if (current != null) {
			// gzipped music with .mid.gz extension
			try (GZIPInputStream gzis = new GZIPInputStream(ClassLoader.getSystemResourceAsStream("midi/" + current + ".mid.gz"))) {
				midiPlayer.play(gzis, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void fadeOut() {
		// reduce volume if it's not 0
		if (!midiPlayer.isMuted()) {
			midiPlayer.adjustVolume(-1);
		}
	}

	@Override
	public void run() {
		if (midiPlayer == null) {
			return;
		}

		while (true) {
			// if no music is playing, just stay faded out
			if (Signlink.midi == null) {
				fadeOut();
			} else {
				// we're changing songs
				if (next != Signlink.midi) {
					String last = next;
					next = Signlink.midi;

					// if we didn't have anything playing already, just immediately play the next.
					if (last == null) {
						playImmediately();
					}
				}

				// we haven't played the next yet, we're still fading out.
				if (current != next) {
					// reduce volume
					fadeOut();

					// if our volume is finally 0 then start our next song
					if (midiPlayer.isMuted()) {
						playImmediately();
					}
				} else {
					int volume = midiPlayer.getVolume();

					// the midi player volume should always be 0 by this point since we've faded out.
					// increase it to our current volume.
					if (volume < this.volume) {
						midiPlayer.adjustVolume(1);
					}
				}
			}

			try {
				Thread.sleep(20); // 50fps lel!!!!
			} catch (Exception ignored) {
			}
		}
	}
}
