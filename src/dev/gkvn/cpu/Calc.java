package dev.gkvn.cpu;

public class Calc {
	/** Converts kilobytes to bytes */
	public static long KB(double kb) {
		return (long) (kb * 1024L);
	}
	
	/** Converts megabytes to bytes */
	public static long MB(double mb) {
		return (long) (mb * 1024L * 1024L);
	}
	
	/** Converts gigabytes to bytes */
	public static long GB(double gb) {
		return (long) (gb * 1024L * 1024L * 1024L);
	}
}
