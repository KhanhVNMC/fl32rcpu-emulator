package dev.gkvn.cpu.assembler.fl32r.frontend.utils;

import java.util.Map;

import dev.gkvn.cpu.assembler.fl32r.frontend.exceptions.FrontendSolveError;
import static dev.gkvn.cpu.fl32r.FL32RConstants.*;

public class FL32RSpecs {
	// static bruh
	public static final int FL32R_SIZE = 4; // 4 BYTES per instruction
	public static final int FL32R_REGISTERS_COUNT = 32;
	public static final int WORD_SIZE = 4;
	public static final int HWORD_SIZE = 2;
	
	private static final Map<String, Integer> NAMED_REGISTERS = Map.ofEntries(
		Map.entry("RZ", REG_ZERO),
		Map.entry("RZERO", REG_ZERO), // the FL32R spec is retarded
		Map.entry("RSP", REG_STACK_POINTER),
		Map.entry("RPC", REG_PROGRAM_COUNTER),
		Map.entry("HMO", REG_VMEM_OFFSET), 
		Map.entry("HMB", REG_VMEM_MAX_BOUND),
		Map.entry("HM1", REG_HLP_APR1), 
		Map.entry("HM2", REG_HLP_APR2), 
		Map.entry("HM3", REG_HLP_APR3),
		// older FL516-eX standard (that never materializes)
		Map.entry("RAX", 0), 
		Map.entry("RBX", 1), 
		Map.entry("RCX", 2),
		Map.entry("RDX", 3)
	);

	private static int parseNumericRegister(int n) throws FrontendSolveError {
		if (n >= 1 && n <= 24) return n - 1;
		if (n == 0) return REG_ZERO;
		throw new FrontendSolveError("Invalid register R" + n);
	}
	
	public static int parseRegister(String name) throws FrontendSolveError {
		name = name.toUpperCase(); // normalize
		Integer reg = NAMED_REGISTERS.get(name); // god help us, boxed int
		if (reg != null) {
			return reg.intValue();
		}
		// oh boy
		if (name.startsWith("R")) {
			try {
				return parseNumericRegister(Integer.parseInt(name.substring(1)));
			} catch (NumberFormatException e) {}
		}
		throw new FrontendSolveError("Unknown register file '" + name + "'");
	}
	
	public static int requireFitBits(int bitWidth, int value) throws FrontendSolveError {
		if (bitWidth <= 0 || bitWidth > 32) {
			throw new IllegalArgumentException("Invalid bit width, how did you get here? Width: " + bitWidth);
		}
		if (bitWidth < 32) {
			int mask = (1 << bitWidth) - 1;
			boolean fitsUnsigned = (value & ~mask) == 0;
			// what the fuck even is this
			boolean fitsSigned = ((value << (32 - bitWidth)) >> (32 - bitWidth)) == value;
			
			if (fitsUnsigned || fitsSigned) {
				return value & mask; // prevent java from sign extending the fuck out of it
			}
		} else {
			return value; // always fit, 32 bit, duh
		}
		throw new FrontendSolveError(
			"Value %d (0x%s) does not fit in %d bits", 
			value, Integer.toHexString(value), bitWidth
		);
	}

	public static int urshift(int value, int shift) {
		if (shift >= 32) return 0;
		return value >>> shift;
	}
	
	public static int toNumber(String str) throws FrontendSolveError {
		String s = str.trim();
		if (s.charAt(0) == '-') {
			throw new FrontendSolveError("Not allowed here");
		}
		try {
			if (s.startsWith("0x") || s.startsWith("0X")) {
				return Integer.parseUnsignedInt(s.substring(2), 16);
			}
			if (s.startsWith("0b") || s.startsWith("0B")) {
				return Integer.parseUnsignedInt(s.substring(2), 2);
			}
			return Integer.parseUnsignedInt(s, 10);
		} catch (NumberFormatException e) {
			throw new FrontendSolveError("Invalid numeric literal (pro tip: 32-bit is the max): " + str);
		}
	}
}
