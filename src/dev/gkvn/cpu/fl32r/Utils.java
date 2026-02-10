package dev.gkvn.cpu.fl32r;

public class Utils {
	public static final int beBytesToInt(byte b0, byte b1, byte b2, byte b3) {
		return (b0 & 0xFF) << 24 | (b1 & 0xFF) << 16 | (b2 & 0xFF) << 8 | (b3 & 0xFF);
	}
	
	public static final int convertNBitNumToInt(int n, int value) {
		final int mask = (1 << n) - 1;
		value &= mask;
		if ((value >>> (n - 1)) != 0) { // holy fuck
			return (0xFFFFFFFF << n) | (value & mask);
		}
		return value;
	}
	
	public static final int convertImm14ToInt(int imm14) {
		return convertNBitNumToInt(14, imm14);
	}
	
	public static final int convertImm19ToInt(int imm19) {
		return convertNBitNumToInt(19, imm19);
	}
	
	public static final int convertImm24ToInt(int imm24) {
		return convertNBitNumToInt(24, imm24);
	}
	
	/**
	 * Detect signed overflow for ADD (32-bit, 2's complement)
	 *
	 * Overflow can only happen if: both inputs have the same sign 
	 * but result has the opposite sign
	 *
	 * this checks the sign bit (31th): (a ^ result) 
	 * and (b ^ result). If both say yes, then overflow (add)
	 */
	public static boolean detectAddOverflow(int a, int b, int result) {
		return ((a ^ result) & (b ^ result) & 0x80000000) != 0;
	}
	
	/**
	 * Same trick as the {@link Utils.detectAddOverflow}
	 */
	public static boolean detectSubOverflow(int a, int b, int result) {
		return ((a ^ b) & (a ^ result) & 0x80000000) != 0;
	}
}
