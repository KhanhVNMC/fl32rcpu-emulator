package dev.gkvn.cpu.fl32r;

public class Utils {
	public static final int beBytesToInt(byte b0, byte b1, byte b2, byte b3) {
		return (b0 & 0xFF) << 24 | (b1 & 0xFF) << 16 | (b2 & 0xFF) << 8 | (b3 & 0xFF);
	}
	
	/**
	 * THIS IS SIGN-MAGNITUDE, NOT 2'S COMPLEMENT
	 * <p>
	 * The MOST SIGNIFICANT bit is the sign (0 = positive, 1 = negative), and the
	 * lower N - 1 bits represent the magnitude
	 */
	public static final int convertU24ToInt(int u24) {
		u24 &= 0xFFFFFF;
		if ((u24 & 0x800000) != 0) {
			return -(u24 & 0x7FFFFF); // lower 23 bits, negate
		} else {
			return u24 & 0x7FFFFF; // positive value
		}
	}
	
	/**
	 * THIS IS SIGN-MAGNITUDE, NOT 2'S COMPLEMENT
	 */
	public static final int convertIntToU24(int number) {
		if (number < 0) {
			return 0x800000 | (-number & 0x7FFFFF);
		} else {
			return number & 0x7FFFFF;
		}
	}
	
	/**
	 * THIS IS SIGN-MAGNITUDE, NOT 2'S COMPLEMENT
	 */
	public static final int convertU14ToInt(int u14) {
		u14 &= (1 << 14) - 1;
		if ((u14 & 0x2000) != 0) {
			return -(u14 & 0x1FFF); // lower 13 bits, negate
		} else {
			return u14 & 0x1FFF; // positive value
		}
	}
	
	/**
	 * THIS IS SIGN-MAGNITUDE, NOT 2'S COMPLEMENT
	 */
	public static final int convertIntToU14(int number) {
		if (number < 0) {
			return 0x2000 | (-number & 0x1FFF);
		} else {
			return number & 0x1FFF;
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
