package dev.gkvn.cpu;

public class Utils {
	public static final int beBytesToInt(byte b0, byte b1, byte b2, byte b3) {
		return (b0 & 0xFF) << 24 | (b1 & 0xFF) << 16 | (b2 & 0xFF) << 8 | (b3 & 0xFF);
	}

	public static final int convertU24ToInt(int u24) {
		u24 &= 0xFFFFFF;
		if ((u24 & 0x800000) != 0) {
			return -(u24 & 0x7FFFFF); // lower 23 bits, negate
		} else {
			return u24 & 0x7FFFFF; // positive value
		}
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
