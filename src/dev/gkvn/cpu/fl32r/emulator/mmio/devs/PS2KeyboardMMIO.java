package dev.gkvn.cpu.fl32r.emulator.mmio.devs;

import dev.gkvn.cpu.fl32r.emulator.mmio.AbstractMMIODevice;
import dev.gkvn.cpu.fl32r.emulator.mmio.FL32RMMIO;
import dev.gkvn.cpu.utils.PS2Mappings;
import dev.gkvn.cpu.utils.SingletonEventSource;

public final class PS2KeyboardMMIO extends AbstractMMIODevice {
	public static final int PS2_KBD_IRQ = 0x02;
	private static final int DEV_FIFO_SIZE = 32;
	
	// register offsets
	public static final int 
		KBD_STATUS	= 0x0,
		KBD_DATA 	= 0x4,
		KBD_CONTROL = 0x8
	;
	
	public static final int // flags
		DATA_READY = 1 << 0,
		DATA_OVERFLOW = 1 << 1
	;
	
	public static final int // control
		IRQ_ENABLE = 1 << 0,
		OVERFLOW_CLEAR = 1 << 1
	;
	
	private boolean irqEnabled = false;
	private boolean overflow = false;
	
	// FIFO queue, ring buffer
	private final int[] fifo = new int[DEV_FIFO_SIZE];
	private int head = 0; // read position
	private int tail = 0; // write position
	private int fifoSize = 0; // size
	
	public PS2KeyboardMMIO(FL32RMMIO mmio, int base, 
		SingletonEventSource<Integer> keyDown,
		SingletonEventSource<Integer> keyUp
	) {
		super(mmio, base, FL32RMMIO.MMIO_BASIC_REGION_SIZE);
		keyDown.setListener(this::handleKeyDown);
		keyUp.setListener(this::handleKeyUp);
	}
	
	// JAVA SWING DRIVER
	public void handleKeyDown(int vkey) {
		int[] scancodes = PS2Mappings.VK_TO_PS2.get(vkey);
		if (scancodes == null) {
			getEmulator().warn("[PS/2 KeyDown] Unable to map Java Virtual Key '%d' to PS/2 scancode!", vkey);
			return;
		}
		for (int sc : scancodes) {
			push(sc);
		}
	}
	
	public void handleKeyUp(int vkey) {
		int[] scancodes = PS2Mappings.VK_TO_PS2.get(vkey);
		if (scancodes == null) {
			getEmulator().warn("[PS/2 KeyUp] Unable to map Java Virtual Key '%d' to PS/2 scancode!", vkey);
			return;
		}
		for (int i = 0; i < scancodes.length; i++) {
			int sc = scancodes[i];
			if (i == scancodes.length - 1) {
				// this flip the first byte, marking RELEASE
				push(sc | 0x80); // only last byte gets it: UP{ E0 75 } -> {E0, F5 < flip}
			} else {
				push(sc); // prefixes intact
			}
		}
	}
	// END OF JAVA SWING
	
	private void push(int sc) {
		if (fifoSize == DEV_FIFO_SIZE) {
			this.overflow = true;
			return;
		}
		boolean wasEmpty = fifoSize == 0;
		fifo[tail] = sc;
		tail = (tail + 1) % DEV_FIFO_SIZE;
		fifoSize++;
		// only IRQ when it switches from [] -> [sth]
		if (wasEmpty && irqEnabled) {
			this.interrupt(PS2_KBD_IRQ);
		}
	}
	
	private int pop() {
		if (fifoSize == 0) {
			return 0;
		}
		int sc = fifo[head];
		head = (head + 1) % DEV_FIFO_SIZE;
		fifoSize--;
		return sc;
	}
	
	@Override
	public void writeWord(int address, int value) {
		int off = offset(address);
		switch (off) {
			case KBD_CONTROL: {
				this.irqEnabled = (value & IRQ_ENABLE) != 0;
				if ((value & OVERFLOW_CLEAR) != 0) this.overflow = false; // ACK
				
				// special case: when enables IRQ and there's a key, interrupt
				if (this.irqEnabled && fifoSize > 0) {
					this.interrupt(PS2_KBD_IRQ);
				}
			}
		}
	}
	
	@Override
	public int readWord(int address) {
		int off = offset(address);
		return switch (off) {
			case KBD_STATUS -> {
				int status = 0;
				if (fifoSize > 0) {
					status |= DATA_READY;
				}
				if (overflow) {
					status |= DATA_OVERFLOW;
				}
				yield status;
			}
			case KBD_DATA -> pop();
			default -> 0;
		};
	}
}