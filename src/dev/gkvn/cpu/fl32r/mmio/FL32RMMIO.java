package dev.gkvn.cpu.fl32r.mmio;

import java.util.ArrayList;
import java.util.List;

import dev.gkvn.cpu.fl32r.FL32RConstants;
import dev.gkvn.cpu.fl32r.FL32REmulator;

public final class FL32RMMIO {
	protected FL32REmulator emulator;
	private final List<MMIODevice> devices = new ArrayList<>();
	
	public FL32RMMIO(FL32REmulator emulator) {
		this.emulator = emulator;
	}
	
	public <T extends MMIODevice> T register(T device) {
		devices.add(device);
		return device;
	}
	
	public byte readByte(int address) {
		return (byte)(this.read(address, false) & 0xFF);
	}
	
	public void writeByte(int address, byte b) {
		this.write(address, false, b);
	}
	
	public int readWord(int address) {
		return this.read(address, true);
	}
	
	public void writeWord(int address, int word) {
		this.write(address, true, word);
	}
	
	int read(int address, boolean isWord) {
		for (MMIODevice d : devices) {
			if (address >= d.getBaseAddress() 
			 && address < d.getBaseAddress() + d.getSize()
			) {
				return isWord ? d.readWord(address) : d.readByte(address);
			}
		}
		emulator.warn("MMIO %s READ @ 0x%X (=0x%08X) is not mapped to any devices", 
			(isWord ? "WORD" : "BYTE"), 
			address, FL32RConstants.MMIO_REGION_START + address
		);
		return 0x00;
	}
	
	void write(int address, boolean isWord, int value) {
		for (MMIODevice d : devices) {
			if (address >= d.getBaseAddress() 
			 && address < d.getBaseAddress() + d.getSize()
			) {
				if (isWord) {
					d.writeWord(address, value);
				} else {
					d.writeByte(address, (byte)(value & 0xFF));
				}
				return;
			}
		}
		emulator.warn("MMIO %s WRITE @ 0x%X (=0x%08X) is not mapped to any devices", 
			(isWord ? "WORD" : "BYTE"), 
			address, FL32RConstants.MMIO_REGION_START + address
		);	
	}
}
