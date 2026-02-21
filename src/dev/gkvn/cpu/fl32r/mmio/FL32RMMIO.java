package dev.gkvn.cpu.fl32r.mmio;

import java.util.ArrayList;
import java.util.List;

import dev.gkvn.cpu.fl32r.FL32REmulator;

public final class FL32RMMIO {
	protected FL32REmulator emulator;
	private final List<MMIODevice> devices = new ArrayList<>();
	
	public FL32RMMIO(FL32REmulator emulator) {
		this.emulator = emulator;
	}

	public void register(MMIODevice device) {
		devices.add(device);
	}

	public int read32(int address) {
		for (MMIODevice d : devices) {
			if (address >= d.getBaseAddress() 
			 && address < d.getBaseAddress() + d.getSize()
			) {
				return d.read32(address);
			}
		}
		emulator.warn("MMIO READ @ 0x%08X is not mapped to any devices");
		return 0x00;
	}

	public void write32(int address, int value) {
		for (MMIODevice d : devices) {
			if (address >= d.getBaseAddress() 
			 && address < d.getBaseAddress() + d.getSize()
			) {
				d.write32(address, value);
				return;
			}
		}
		emulator.warn("MMIO WRITE @ 0x%08X is not mapped to any devices");
	}
}
