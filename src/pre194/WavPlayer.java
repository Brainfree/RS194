/*
 * Copyright (c) 2015. Dane
 */

package pre194;import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.File;
import java.io.InputStream;

/**
 * @author Dane
 */
public class WavPlayer {

	public static void main(String[] args) throws Exception {
		WavPlayer player = new WavPlayer();

		player.volume = 256;

		while (player.volume > 0) {
			Clip c = player.play(new File("death.wav"));
			Thread.sleep(c.getMicrosecondLength() / 1000);
			player.volume -= 32;
		}
	}

	public static final float MIN_VOLUME_DB = -80f;
	public static final int MAX_VOLUME = 256;

	private int volume = 256;

	public Clip play(File f) throws Exception {
		return this.play(AudioSystem.getAudioInputStream(f));
	}

	public Clip play(InputStream in) throws Exception {
		return this.play(AudioSystem.getAudioInputStream(in));
	}

	public Clip play(AudioInputStream stream) throws Exception {
		Clip clip = AudioSystem.getClip();
		clip.open(stream);

		FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

		// volume of 256 = 0.0
		// volume of 128 = -40.0
		// volume of 0 = -80
		gain.setValue((MIN_VOLUME_DB * (MAX_VOLUME - this.volume)) / MAX_VOLUME);

		clip.start();
		return clip;
	}

	public void setVolume(float volume) {
		this.setVolume((int) (256f * volume));
	}

	public void setVolume(int volume) {
		this.volume = volume;
	}

	public int getVolume() {
		return this.volume;
	}

}
