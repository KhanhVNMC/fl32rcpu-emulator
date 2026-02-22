package dev.gkvn.cpu.fl32r.mmio.devs;

import dev.gkvn.cpu.fl32r.mmio.AbstractMMIODevice;
import dev.gkvn.cpu.fl32r.mmio.FL32RMMIO;

public final class HardwareTimerMMIO extends AbstractMMIODevice {
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
	
	public static final int TIMER_IRQ = 0x00;
	
	private final long startTimeNs;
	private long compare = Long.MAX_VALUE;
	private long period = 0;

	private int packedCtrl = 0;
	private boolean irqPending = false;
	
	// microsecond timer
	public HardwareTimerMMIO(FL32RMMIO mmio, int base) {
		super(mmio, base, 16);
		this.startTimeNs = System.nanoTime();
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
			case REG_COMPARE -> (int) this.compare;
			case REG_CTRL -> this.packedCtrl;
			default -> 0;
		};
	}
	
	@Override
	public void writeWord(int address, int value) {
		switch (offset(address)) {
			case REG_COMPARE -> {
				this.compare = Integer.toUnsignedLong(value);
				this.period = compare;
			}
			case REG_CTRL -> {
				// W1C semantics
				if ((value & CTRL_RESET) != 0) {
					resetCounter();
				}
				if ((value & CTRL_IRQ_ACK) != 0) {
					this.irqPending = false;
				}
				// persistent control bits
				this.packedCtrl = value & ~(CTRL_RESET | CTRL_IRQ_ACK);
			}
		}
	}

	private void resetCounter() {
		compare = Long.MAX_VALUE;
		irqPending = false;
	}

	public void tick() {
		if ((this.packedCtrl & CTRL_ENABLE) == 0) {
			return;
		}
		long cnt = counter();
		if (cnt >= compare && !irqPending) {
			this.irqPending = true;
			if ((this.packedCtrl & CTRL_IRQ_ENABLE) != 0) {
				this.interrupt(TIMER_IRQ);
			}
			if ((this.packedCtrl & CTRL_PERIODIC) != 0) {
				this.compare += period;
				this.irqPending = false;
			}
		}
	}
}