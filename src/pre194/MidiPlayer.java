/*
 * Copyright (c) 2015. Dane
 */

package pre194;

import javax.sound.midi.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A midi player.
 *
 * @author Dane
 */
public class MidiPlayer implements Receiver {

	public static final int PROGRAM_0 = 0;

	public static final int MSB_CHANNEL_VOLUME = 7;
	public static final int LSB_CHANNEL_VOLUME = 39;

	public static final int MSB_BANK_SELECT = 0;
	public static final int LSB_BANK_SELECT = 32;

	public static final int ALL_SOUND_OFF = 120;
	public static final int RESET_ALL_CONTROLLERS = 121;
	public static final int ALL_NOTES_OFF = 123;

	private final int[] channelVolume = new int[]{
			12800, 12800, 12800, 12800, 12800, 12800,
			12800, 12800, 12800, 12800, 12800, 12800,
			12800, 12800, 12800, 12800
	};

	private int volume = 256;

	private Receiver receiver;
	private Sequencer sequencer;

	public MidiPlayer() throws MidiUnavailableException {
		this.receiver = MidiSystem.getReceiver();
		this.sequencer = MidiSystem.getSequencer(false);
		this.sequencer.getTransmitter().setReceiver(this); // used to override messages
		this.sequencer.open();
	}

	public Sequence play(byte[] src) throws InvalidMidiDataException, IOException {
		Sequence s;
		try (ByteArrayInputStream bais = new ByteArrayInputStream(src)) {
			s = this.play(bais);
		}
		return s;
	}

	public Sequence play(InputStream in) throws InvalidMidiDataException, IOException {
		return this.play(in, false);
	}

	public Sequence play(InputStream in, boolean loop) throws InvalidMidiDataException, IOException {
		if (this.sequencer == null) {
			return null;
		}

		Sequence s = MidiSystem.getSequence(in);
		this.sequencer.setSequence(s);
		this.sequencer.setLoopCount(loop ? Sequencer.LOOP_CONTINUOUSLY : 0);
		this.sequencer.start();
		return s;
	}

	public void setVolume(int volume) {
		if (this.sequencer == null) {
			return;
		}

		if (volume < 0) {
			volume = 0;
		} else if (volume > 256) {
			volume = 256;
		}

		if (volume != this.volume) {
			this.volume = volume;

			for (int c = 0; c < 16; c++) {
				int data = this.getChannelVolume(c);
				this.send(c + ShortMessage.CONTROL_CHANGE, MSB_CHANNEL_VOLUME, data >> 7);
				this.send(c + ShortMessage.CONTROL_CHANGE, LSB_CHANNEL_VOLUME, data & 0x7F);
			}
		}
	}

	public boolean isMuted() {
		return this.volume <= 0;
	}

	public void adjustVolume(int adjustment) {
		this.setVolume(this.volume + adjustment);
	}

	public int getVolume() {
		return this.volume;
	}

	public void stop() {
		if (this.sequencer != null) {
			this.sequencer.close();
			this.reset(-1L);
		}
	}

	private void reset(long timeStamp) {
		for (int c = 0; c < 16; c++) {
			send(c + ShortMessage.CONTROL_CHANGE, ALL_NOTES_OFF, 0);
		}

		for (int c = 0; c < 16; c++) {
			send(c + ShortMessage.CONTROL_CHANGE, ALL_SOUND_OFF, 0);
		}

		for (int c = 0; c < 16; c++) {
			send(c + ShortMessage.CONTROL_CHANGE, RESET_ALL_CONTROLLERS, 0);
		}

		for (int c = 0; c < 16; c++) {
			send(c + ShortMessage.CONTROL_CHANGE, MSB_BANK_SELECT, 0);
		}

		for (int c = 0; c < 16; c++) {
			send(c + ShortMessage.CONTROL_CHANGE, LSB_BANK_SELECT, 0);
		}

		for (int c = 0; c < 16; c++) {
			send(c + ShortMessage.PROGRAM_CHANGE, PROGRAM_0, 0);
		}
	}

	@Override
	public void send(MidiMessage m, long timeStamp) {
		byte[] data = m.getMessage();

		// override messages
		if (data.length < 3 || !send0(data[0], data[1], data[2])) {
			this.receiver.send(m, timeStamp);
		}
	}

	@Override
	public void close() {
		if (this.sequencer != null) {
			this.sequencer.close();
			this.sequencer = null;
		}

		if (this.receiver != null) {
			this.receiver.close();
			this.receiver = null;
		}
	}

	private void send(int status, int data1, int data2) {
		try {
			this.receiver.send(new ShortMessage(status, data1, data2), -1);
		} catch (InvalidMidiDataException ignored) {
		}
	}

	private boolean send0(int status, int data1, int data2) {
		if ((status & 0xF0) == ShortMessage.CONTROL_CHANGE) {
			if (data1 == RESET_ALL_CONTROLLERS) {
				send(status, data1, data2);

				int channel = status & 0xF;
				this.channelVolume[channel] = 12800;
				int data = this.getChannelVolume(channel);

				this.send(status, MSB_CHANNEL_VOLUME, data >> 7);
				this.send(status, LSB_CHANNEL_VOLUME, data & 0x7f);
				return true;
			}

			if (data1 == MSB_CHANNEL_VOLUME || data1 == LSB_CHANNEL_VOLUME) {
				int channel = status & 0xF;

				if (data1 == MSB_CHANNEL_VOLUME) {
					this.channelVolume[channel] = (this.channelVolume[channel] & 0x7F) + (data2 << 7);
				} else {
					this.channelVolume[channel] = (this.channelVolume[channel] & 0x3F80) + data2;
				}

				int data = this.getChannelVolume(channel);
				this.send(status, MSB_CHANNEL_VOLUME, data >> 7);
				this.send(status, LSB_CHANNEL_VOLUME, data & 0x7f);
				return true;
			}
		}
		return false;
	}

	private int getChannelVolume(int channel) {
		int data = this.channelVolume[channel];
		data = ((data * this.volume) >> 8) * data;
		return (int) (Math.sqrt(data) + 0.5);
	}

}
