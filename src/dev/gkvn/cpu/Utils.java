package dev.gkvn.cpu;

public class Utils {
	public static final int beBytesToInt(byte b0, byte b1, byte b2, byte b3) {
		return (b0 & 0xFF) << 24 
			 | (b1 & 0xFF) << 16 
			 | (b2 & 0xFF) << 8
			 | (b3 & 0xFF)
		;
	}
}
