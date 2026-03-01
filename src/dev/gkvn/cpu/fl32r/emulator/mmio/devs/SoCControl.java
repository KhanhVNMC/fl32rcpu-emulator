package dev.gkvn.cpu.fl32r.emulator.mmio.devs;

import dev.gkvn.cpu.fl32r.emulator.mmio.AbstractMMIODevice;
import dev.gkvn.cpu.fl32r.emulator.mmio.FL32RMMIO;

public final class SoCControl extends AbstractMMIODevice {
	// register offsets
	public static final int 
		REG_SOC_POWER_CONTROL = 0x00, 
		REG_SOC_TOTAL_MEMORY = 0x04,
		
		REG_CPUID_VENDOR = 0xF0, // 16 bytes
		REG_CPUID_STEPPINGS = REG_CPUID_VENDOR + 16, 
		REG_CPUID_BRAND_STRING = REG_CPUID_STEPPINGS + 1 // 48 bytes
	;
	
	public static final int 
		SOC_POWER_OFF = 1 << 0, 
		SOC_RESET = 1 << 1
	;
	
	private final CPUID cpuid;
	public SoCControl(FL32RMMIO mmio, int base, CPUID cpuid) {
		super(mmio, base, FL32RMMIO.MMIO_BASIC_REGION_SIZE);
		this.cpuid = cpuid;
	}

	@Override
	public byte readByte(int address) {
		int off = offset(address);

		// vendor string
		if (off >= REG_CPUID_VENDOR && off < REG_CPUID_VENDOR + 16) {
			return readStringByte(cpuid.vendor(), off - REG_CPUID_VENDOR);
		}

		// brand string
		if (off >= REG_CPUID_BRAND_STRING && off < REG_CPUID_BRAND_STRING + 48) {
			return readStringByte(cpuid.brandName(), off - REG_CPUID_BRAND_STRING);
		}
		
		return switch (off) {
			case REG_SOC_POWER_CONTROL -> 0;
			case REG_SOC_TOTAL_MEMORY -> (byte) (getEmulator().getMemory().length() & 0xFF);
			case REG_CPUID_STEPPINGS -> (byte) cpuid.steppings();
			default -> 0;
		};
	}
	
	@Override
	public void writeByte(int address, byte value) {
		if (offset(address) == REG_SOC_POWER_CONTROL) {
			if ((value & SOC_POWER_OFF) != 0) {
				System.out.println("\n[SOC VM] SoC Control powered off the platform");
				getEmulator().kill(); // bruh
				return;
			}
			
			if ((value & SOC_RESET) != 0) {
				System.out.println("\n[SOC VM] SoC Control restarted the platform");
				getEmulator().reset(false); // reset
				return;
			}
		}
	}
	
	@Override
	public int readWord(int address) {
		return Byte.toUnsignedInt(readByte(address));
	}
	
	@Override
	public void writeWord(int address, int value) {
		writeByte(address, (byte) value);
	}
	
	static byte readStringByte(String s, int i) {
		return (i >= 0 && i < s.length()) ? (byte) s.charAt(i) : 0;
	}
	
	public static record CPUID(String vendor, byte steppings, String brandName) {}
}