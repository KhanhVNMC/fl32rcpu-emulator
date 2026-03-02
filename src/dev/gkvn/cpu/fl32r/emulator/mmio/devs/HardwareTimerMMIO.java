package dev.gkvn.cpu.fl32r.emulator.mmio.devs;

import dev.gkvn.cpu.fl32r.emulator.mmio.AbstractMMIODevice;
import dev.gkvn.cpu.fl32r.emulator.mmio.FL32RMMIO;

public final class HardwareTimerMMIO extends AbstractMMIODevice {
	public static final int TIMER_IRQ = 0x00;
	
	public static final int 
		REG_COUNT_LO = 0x00,
		REG_COUNT_HI = 0x04,
		REG_COMPARE = 0x08,
		REG_CTRL = 0x0C
	;
	
	public static final int 
		CTRL_ENABLE = 1 << 0, // timer running
		CTRL_IRQ_ENABLE = 1 << 1, // raise IRQ on compare
		CTRL_PERIODIC = 1 << 2, // auto reload
		CTRL_IRQ_ACK = 1 << 3, // W1C: clear IRQ
		CTRL_RESET = 1 << 4 // W1C: reset counter
	;
		
	// flagkeeper
	private long startTimeNs;
	
	private boolean enabled = false;
	private boolean irqEnabled = false;
	private boolean irqPending = false;
	
	// period setting
	private long deadlineUs = Long.MAX_VALUE; // us = microsec
	private boolean periodic = false;
	private long periodUs = 0;
	
	// microsecond timer
	public HardwareTimerMMIO(FL32RMMIO mmio, int base) {
		super(mmio, base, FL32RMMIO.MMIO_BASIC_REGION_SIZE);
		this.resetCounter(); // doubles as an init
	}

	private long counter() {
		return (System.nanoTime() - startTimeNs) / 1_000L; // ns to microsec
	}
	
	@Override
	public int readWord(int address) {
		long cnt = counter();
		return switch (offset(address)) {
			case REG_COUNT_LO -> (int) (cnt & 0xFF_FF_FF_FFL);
			case REG_COUNT_HI -> (int) (cnt >>> 32);
			case REG_COMPARE -> (int) this.periodUs;
			default -> 0;
		};
	}
	
	@Override
	public void writeWord(int address, int value) {
		switch (offset(address)) {
			case REG_COMPARE -> {
				long period = Integer.toUnsignedLong(value);
				this.periodUs = period;
				this.deadlineUs = counter() + period;
			}
			case REG_CTRL -> {
				// W1C semantics
				if ((value & CTRL_RESET) != 0) {
					resetCounter();
				}
				if ((value & CTRL_IRQ_ACK) != 0) {
					this.irqPending = false;
				}
				// persistent controls
				enabled = (value & CTRL_ENABLE) != 0;
				irqEnabled = (value & CTRL_IRQ_ENABLE) != 0;
				periodic = (value & CTRL_PERIODIC) != 0;
			}
		}
	}
	
	// completely rewrites the shit
	private void resetCounter() {
		this.startTimeNs = System.nanoTime();
		this.deadlineUs = Long.MAX_VALUE;
		this.periodUs = 0;
		this.irqPending = false;
		this.enabled = false;
		this.irqEnabled = false;
		this.periodic = false;
	}
	
	// the software must acknowledges before the next IRQ can fire
	public void irqAck() {
		if (periodic) {
			this.deadlineUs += periodUs;
		}
		this.irqPending = false;
	}
	
	public void tick() {
		if (!enabled || irqPending) return;
		// fire IRQ (if enabled) on the next deadline
		if (irqEnabled && counter() > deadlineUs) {
			this.interrupt(TIMER_IRQ);
		}
	}
}