package dev.gkvn.cpu.demo;

import static dev.gkvn.cpu.fl32r.Utils.*;

public class NBitConverterTest {
	public static void main(String[] args) {
		test14Bit();
		test24Bit();
		testEdgeCases();
		System.out.println("All tests completed.");
	}

	private static void test14Bit() {
		System.out.println("Testing 14-bit values...");
		int n = 14;

		// positive numbers
		for (int i = 0; i < 8192; i++) { // 0 to 2^13-1
			int decoded = convertNBitNumToInt(n, i);
			assertEqual(i, decoded, "14-bit positive: " + i);
		}

		// negative numbers (sign bit set)
		for (int i = 8192; i < 16384; i++) { // 2^13 to 2^14-1
			int decoded = convertNBitNumToInt(n, i);
			int expected = i - (1 << 14);
			assertEqual(expected, decoded, "14-bit negative: " + i);
		}
	}

	private static void test24Bit() {
		System.out.println("Testing 24-bit values...");
		int n = 24;

		// Positive numbers
		int step = 0x1000000 / 16; // test 16 points across range
		for (int i = 0; i < 0x800000; i += step) { // 0 to 2^23-1
			int decoded = convertNBitNumToInt(n, i);
			assertEqual(i, decoded, "24-bit positive: " + i);
		}

		// Negative numbers
		for (int i = 0x800000; i < 0x1000000; i += step) { // 2^23 to 2^24-1
			int decoded = convertNBitNumToInt(n, i);
			int expected = i - (1 << 24);
			assertEqual(expected, decoded, "24-bit negative: " + i);
		}
	}
	
	private static void testEdgeCases() {
		System.out.println("Testing edge cases...");
		int n = 4;
		int[] values = { 0, 7, 8, 15 }; // min, max positive, min negative, max negative
		int[] expected = { 0, 7, -8, -1 };
		for (int i = 0; i < values.length; i++) {
			int decoded = convertNBitNumToInt(n, values[i]);
			assertEqual(expected[i], decoded, "4-bit edge: " + values[i]);
		}
	}
	
	private static void assertEqual(int expected, int actual, String message) {
		if (expected != actual) {
			System.err.println("FAILED: " + message + " | Expected: " + expected + ", Got: " + actual);
			System.exit(1);
		} else {
			System.out.println("PASSED: " + message + " | Value: " + actual);
		}
	}
}
