package dev.gkvn.cpu.fl32r;

import static dev.gkvn.cpu.fl32r.FL32RConstants.*;

import java.util.Arrays;

import dev.gkvn.cpu.ByteMemory;
import dev.gkvn.cpu.GenericCPUEmulator;
import dev.gkvn.cpu.ReadOnlyByteMemory;

// this CPU is BIG-ENDIAN, 32-bit processor with primitive MMU
// FL32RCPU -> Fixed Length 32-bit RISC CPU
public class FL32REmulator implements GenericCPUEmulator {
	// the internal CPU parts, registers, flags
	private boolean ZFL = false; // zero flag (for CMP)
	private boolean NFL = false; // negative flag; if the last arithmetic (including CMP) operation is negative
	private boolean OFL = false; // overflow flag
	private boolean HLP = true; // true for since the kernel is loaded first anyway, must be set later if desired
	
	private final int registers[] = new int[32]; // 32x 32 bits register
	
	// the CPU's memory (RAM)
	private final ByteMemory memory;
	private final ReadOnlyByteMemory readonlyMemory;
	
	// cpu internal states
	private boolean cpuHalted = false;
	
	// emulator parameter/controls
	private double nsPerCycle;
	private int frequencyHz;
	private boolean cpuKilled = false;
	private boolean cpuStarted = false;
	private boolean bootProgramLoaded = false;
	private boolean singleStepMode = false;
	
	public FL32REmulator(long memorySize) {
		// clamp memorySize to 32-bit unsigned max
		if (memorySize < 0 || memorySize > 0xFFFFFFFFL) {
			throw new IllegalArgumentException("Memory size must be 0 -> 4GB");
		}
		this.setFrequencyHz(32_000_000); // 32 MHZ cpu
		this.memory = new ByteMemory(memorySize);
		this.readonlyMemory = new ReadOnlyByteMemory(this.memory);
	}
	
	@Override
	public void setFrequencyHz(int hertz) {
		if (hertz < 0) {
			this.nsPerCycle = -1; // runs AS FAST AS POSSIBLE, period
			this.frequencyHz = Integer.MAX_VALUE;
			return;
		}
		this.frequencyHz = hertz;
		this.nsPerCycle = 1_000_000_000.0 / hertz;
	}
	
	@Override
	public int getFrequencyHz() {
		return this.frequencyHz;
	}
	
	@Override
	public void loadBootProgram(byte[] program) {
		if (bootProgramLoaded) {
			throw new IllegalStateException("Boot program has already been loaded; cannot load twice!");
		}
		if (program.length > memory.length()) {
			throw new IllegalArgumentException(
				"Boot program size (" + program.length + " bytes) exceeds available memory (" + memory.length() + " bytes)!"
			);
		}
		// copy over the program to memory (0x0)
		for (int i = 0; i < program.length; i++) {
			memory.set(i, program[i]);
		}
		// setup the boot process
		this.HLP = true; // always start at the highest privilege level
		writeRegister(REG_PROGRAM_COUNTER, 0x0);
		writeRegister(REG_STACK_POINTER, (int)((memory.length() - 1) & 0xFFFFFFFFL));
		this.bootProgramLoaded = true;
	}
	
	// MAIN CPU LOOP
	@Override
	public void start() {
		if (this.cpuStarted) {
			throw new IllegalStateException("CPU has already been started; cannot start twice!");
		}
		this.cpuStarted = true;
		while (true) {
			if (this.cpuKilled) break; // stop the cpu immediately (basically powered off)
			if (this.singleStepMode || this.cpuHalted) {
				Thread.onSpinWait(); // basically skip autonomous execution
				continue;
			}
			// normal execution
			this.stepNextInstruction();
		}
	}
	
	@Override
	public boolean isStarted() {
		return this.cpuStarted;
	}
	
	@Override
	public boolean isAutonomousExecutionEnabled() {
		return isCPUAvailable() && !singleStepMode && !cpuHalted;
	}
	
	@Override
	public void activateSingleStepMode() {
		if (!this.isCPUAvailable()) {
			throw new IllegalStateException("CPU not available!");
		}
		if (this.singleStepMode) {
			throw new IllegalStateException("Single step mode is already activated!");
		}
		this.singleStepMode = true;
		this.halt(); // halt initially
	}
	
	@Override
	public void stepExecution() {
		if (!this.isCPUAvailable()) {
			throw new IllegalStateException("CPU not available!");
		}
		if (!this.singleStepMode) {
			throw new IllegalStateException("Single step mode is not activated!");
		}
		this.resume(); // the halted flag may be used in the future, so do this sequence
		this.stepNextInstruction();
		this.halt(); // halt so it wont overrun (doesnt matter)
	}
	
	@Override
	public void deactivateSingleStepMode() {
		if (!this.isCPUAvailable()) {
			throw new IllegalStateException("CPU not available!");
		}
		if (!this.singleStepMode) {
			throw new IllegalStateException("Single step mode is not activated!");
		}
		this.singleStepMode = false;
		this.resume(); // resume the CPU regardless
	}
	
	@Override
	public boolean isOnSingleStepMode() {
		return this.singleStepMode;
	}
	
	/**
	 * Does what it says, a full cycle of FETCH -> DECODE -> EXECUTE and simulate
	 * real hardware speed with spin wait.
	 */
	private synchronized final void stepNextInstruction() {
		try {
			// ===== FETCH =====
			int currentPC = this.readRegister(REG_PROGRAM_COUNTER);
			int instruction = this.readWordMemory(currentPC);
			// step to the next instruction, since execution may alter PC, this must be incremented here
			this.writeRegister(REG_PROGRAM_COUNTER, currentPC + 4); // 4 bytes (32bit) instruction
			// ===== DECODE =====
			byte opcode = (byte) ((instruction >> 24) & 0xFF); // 8 MSB
			int operand = instruction & 0xFFFFFF;
			// ===== EXECUTE =====
			long execStart = System.nanoTime();
			System.out.println("INSTRUCTION: " + String.format("%32s", Integer.toBinaryString(instruction)).replace(' ', '0'));
			System.out.println("OPCODE: " + String.format("%8s", Integer.toBinaryString(opcode)).replace(' ', '0'));
			System.out.println("OPERAND: " + String.format("%24s", Integer.toBinaryString(operand)).replace(' ', '0') + "\n");

			execute(opcode, operand);
			// LIMIT CPU FREQ FIRST (without this the CPU would run at extreme speed)
			// compensate for the time that it takes to actually execute the instruction in the JVM
			if (nsPerCycle > 0) {
				long jvmExecTime = System.nanoTime() - execStart;
				long emulatedExecTime = (long)(FL32RCycleTable.COST_TABLE[opcode] * nsPerCycle);
				long waitForNs = Math.max(0, emulatedExecTime - jvmExecTime);
				if (waitForNs > 0) {
					long spinUntil = System.nanoTime() + waitForNs;
					while (System.nanoTime() < spinUntil) {} // spin wait
				}
			}
		} catch (FaultRaisedException ignored) {}
	}
 	
	@Override
	public void halt() {
		this.cpuHalted = true;
	}
	
	@Override
	public void resume() {
		this.cpuHalted = false;
	}
	
	@Override
	public void reset() {
		if (!this.isCPUAvailable()) {
			throw new IllegalStateException("CPU not available!");
		}
		// reset all registers and flags
		this.singleStepMode = false;
		Arrays.fill(this.registers, 0x00);
		this.ZFL = false;
		this.NFL = false;
		this.OFL = false;
		this.HLP = true; // start in the highest level privilege
		// jump to the post-reset vector, 0x0 for now
		writeRegister(REG_PROGRAM_COUNTER, 0x0); // reset vector (0x0 for now)
		// put the stack pointer to the initial position
		writeRegister(REG_STACK_POINTER, (int)((memory.length() - 1) & 0xFFFFFFFFL));
		this.resume();
	}
	
	@Override
	public void kill() {
		if (!this.isCPUAvailable()) {
			throw new IllegalStateException("CPU not available!");
		}
		this.halt();
		this.cpuKilled = true;
	}
	
	@Override
	public boolean isKilled() {
		return this.cpuKilled;
	}
	
	@Override
	public ReadOnlyByteMemory dumpMemory() {
		return this.readonlyMemory;
	}
	
	@Override
	public int[] dumpRegisters() {
		return Arrays.copyOf(this.registers, this.registers.length);
	}
	
	@Override
	public boolean[] dumpFlags() {
		return new boolean[] { ZFL, NFL, OFL, HLP };
	}
	
	final void execute(byte opcode, int operand) {
		// operand could be interpreted differently
		int rDest = (operand >> 19) & 0b11111;
		int rOp1 = (operand >> 14) & 0b11111;
		int rOp2 = (operand >> 9) & 0b11111;
		
		switch (opcode) {
			case NOP: { break; }
			case MOV: {
				// simplest instruction
				writeRegister(rDest, readRegister(rOp1));
				break;
			}
			// load an immediate to the top 16 bits of a register
			case LUI: {
				int immediate = (operand >> 3) & 0xFFFF;
				writeRegister(rDest, immediate << 16); // pads 16 lower bits
				break;
			}
			// load a word: rDest = Memory[rOp1]...[rOp1+3]
			case LDW: {
				int addr = readRegister(rOp1); // fetch mem address
				writeRegister(rDest, readWordMemory(addr));
				break;
			}
			// load a byte: rDest = Memory[rOp1]
			case LDB: {
				int addr = readRegister(rOp1);
				writeRegister(rDest, readByteMemory(addr));
				break;
			}
			// store a word: rSrc = Memory[rMemDest...3]
			case STW: {
				// disambiguation
				int rSrc = rDest, rMemDest = rOp1;
				// value from 2nd operand
				int value = readRegister(rSrc);
				writeWordMemory(readRegister(rMemDest), value);
				break;
			}
			// store a byte: rSrc = Memory[rMemDest]
			case STB: {
				// disambiguation
				int rSrc = rDest, rMemDest = rOp1;
				// value from 2nd operand
				byte value = (byte)(readRegister(rSrc) & 0xFF);
				writeByteMemory(readRegister(rMemDest), value);
				break;
			}
			// arithmetic operations: rDest = rOp1 [opcode] rOp2
			case ADD: case SUB:
			case MUL: case DIV: 
			case MOD: {
				int left = readRegister(rOp1), right = readRegister(rOp2);
				int result = switch (opcode) {
					case ADD -> left + right;
					case SUB -> left - right;
					case MUL -> left * right;
					case DIV -> right != 0 ? (left / right) : raiseFault(FaultType.FAULT_DIVZERO);
					case MOD -> right != 0 ? (left % right) : raiseFault(FaultType.FAULT_DIVZERO);
					default -> raiseFault(FaultType.FAULT_ILLEGAL);
				};
				writeRegister(rDest, result);
				// set the flags
				this.ZFL = result == 0;
				this.NFL = (result < 0);
				if (opcode == ADD) {
					this.OFL = Utils.detectAddOverflow(left, right, result);
				} else if (opcode == SUB) {
					this.OFL = Utils.detectSubOverflow(left, right, result);
				}
				break;
			}
			// bitwise operations:
			case AND:
			case OR: case XOR:
			case SHL: case SHR: 
			case NOT: {
				int left = readRegister(rOp1), right = readRegister(rOp2);
				int result = switch (opcode) {
					case AND -> left & right;
					case OR -> left | right;
					case XOR -> left ^ right;
					case SHL -> left << right;
					case SHR -> left >>> right;
					case NOT -> ~left;
					default -> raiseFault(FaultType.FAULT_ILLEGAL);
				};
				writeRegister(rDest, result);
				// for bitwise, only this flag is useful
				this.ZFL = result == 0;
				break;
			}
			// immediate arithmetic & bitwise ops (special ones)
			// rDest [ophere]= immediate (19 bits lsb)
			case ADDI:
			case ANDI:
			case XORI:
			case ORI: {
				int current = readRegister(rDest); 
				int immediate = operand & ((1 << 19) - 1);
				int result = switch (opcode) {
					case ADDI -> current + immediate;
					case ANDI -> current & immediate;
					case ORI  -> current | immediate;
					case XORI -> current ^ immediate;
					default -> raiseFault(FaultType.FAULT_ILLEGAL);
				};
				writeRegister(rDest, result);					
				// this flag must be set universally for all
				this.ZFL = result == 0;
				// set the flags for addi excl.
				if (opcode == ADDI) {
					this.NFL = (result < 0);
					this.OFL = Utils.detectAddOverflow(current, immediate, result);
				}
				break;
			}
			// stack operations push(rDest)
			case PUSH: {
				pushToStack(readRegister(rDest));
				break;
			}
			case POP: {
				writeRegister(rDest, popFromStack());
				break;
			}
			// COMPARE (like SUB): r0 - r1
			case CMP: {
				int left = readRegister(rOp1), right = readRegister(rOp2);
				int result = left - right;
				// set the flags
				this.ZFL = result == 0;
				this.NFL = (result < 0);
				this.OFL = Utils.detectSubOverflow(left, right, result);
				break;
			}
			// FLOW CONTROLS (relative-to-pc jumps: RJUMP)
			case JMP: 
			case JEQ: case JNE:
			case JLT: case JGT:
			case JGE: case JLE: 
			case JOF: case JNO: {
				// read the current PC
				int absAddress = readRegister(REG_PROGRAM_COUNTER);
				absAddress += Utils.convertU24ToInt(operand); // add the rel-jump 
				boolean shouldJump = switch (opcode) {
					case JMP -> true;
					case JEQ -> ZFL; // a - b == 0 <-> a == b
					case JNE -> !ZFL; // a - b != 0 <-> a != b
					case JGT -> !NFL && !ZFL; // a - b > 0 <-> a > b
					case JLT -> NFL; // a - b < 0 <-> a < b
					case JGE -> !NFL || ZFL; // a >= b
					case JLE -> NFL || ZFL; // a <= b
					case JOF -> OFL; // overflow
					case JNO -> !OFL; // not ovfl
					default -> false;
				};
				// jump to it
				if (shouldJump) {
					writeRegister(REG_PROGRAM_COUNTER, absAddress);
				}
				break;
			}
			// CALL (RELATIVE) [push the return address to the stack, so RET can find its way back]
			case CALL: {
				pushToStack(readRegister(REG_PROGRAM_COUNTER));
				// read the current PC and jump
				int absAddress = readRegister(REG_PROGRAM_COUNTER);
				absAddress += Utils.convertU24ToInt(operand); // add the rel-jump 
				writeRegister(REG_PROGRAM_COUNTER, absAddress);
				break;
			}
			// CLR (absolute call, jump to address (sits in register))
			case CLR: {
				pushToStack(readRegister(REG_PROGRAM_COUNTER));
				writeRegister(REG_PROGRAM_COUNTER, operand & 0xFFFFFF); // treat as unsigned 24-bit
				break;
			}
			// ABSOLUTE JUMPS (jump straight to address (sits in register))
			case JR: {
				writeRegister(REG_PROGRAM_COUNTER, operand & 0xFFFFFF); // treat as unsigned 24-bit
				break;
			}
			// RETURN (pop and jump)
			case RET: {
				writeRegister(REG_PROGRAM_COUNTER, popFromStack());
				break;
			}
			// HLP INSTRUCTIONS (Executed by the Kernel/HLP entities)
			case VMO: {
				// should use HR(x) (Host-Level Privilege dedicated APRs)
				// example:
				// LDI  HR0, 0xCAFEBABE
				// VMO  HR0
				// XOR  HRO, HRO, HRO ; for safety
				writeRegister(REG_VMEM_OFFSET, readRegister(rDest));
				break;
			}
			case VMB: {
				// should use HR(x) (Host-Level Privilege dedicated APRs)
				// example:
				// LDI  HR1, 0xFFFF
				// VMB  HR1
				// XOR  HR1, HR1, HR1 ; for safety
				writeRegister(REG_VMEM_MAX_BOUND, readRegister(rDest));
				break;
			}
			case HLR: {
				// should use HR(x) (Host-Level Privilege dedicated APRs)
				if (!this.HLP) {
					raiseFault(FaultType.FAULT_PRIV);
				}
				int resumeAddress = readRegister(rDest);
				this.HLP = false; // de-escalation, clear HLR
				writeRegister(REG_PROGRAM_COUNTER, resumeAddress); // and return to the address
				break;
			}
			// Halt the cpu until an interrupt happens
			case HLT: {
				if (!this.HLP) raiseFault(FaultType.FAULT_PRIV);
				this.halt();
				break;
			}
			// Kills the cpu immediately (emulator-only instruction)
			case KILL: { 
				if (!this.HLP) raiseFault(FaultType.FAULT_PRIV);
				this.kill();
				break;
			}
			default: { // unknown instruction
				this.raiseFault(FaultType.FAULT_ILLEGAL);
				break;
			}
		}
	}
	
	/**
	 * This function is called whenever there's a violation (fault) during a cycle
	 */
	final int raiseFault(FaultType faultType) {
		// halt the CPU and may be in the future, 
		// jump to a vector table and escalate back to HLP
		this.halt();
		System.out.println("CPU AT FAULT:" + faultType + " PC: " + readRegister(REG_PROGRAM_COUNTER));
		// TODO
		throw new FaultRaisedException(); // interrupts current execute()
	}
	
	// REGISTER MANIPULATION
	final void writeRegister(int regIndex, int value) {
		regIndex &= 0b11111;
		if (regIndex == REG_ZERO) return; // prevent writes to Zero reg
		// prevent accessing a forbidden register
		if (regIndex >= REG_VMEM_OFFSET && !HLP) {
			this.raiseFault(FaultType.FAULT_PRIV);
			return;
		}
		this.registers[regIndex] = value; // write, zoop
	}
	
	final int readRegister(int regIndex) {
		regIndex &= 0b11111;
		if (regIndex >= REG_VMEM_OFFSET && !HLP) {
			// prevent accessing a forbidden register
			this.raiseFault(FaultType.FAULT_PRIV);
			return 0;
		}
		return (regIndex == REG_ZERO) ? 0 : registers[regIndex];
	}
	
	// MEMORY MANIPULATION
	/**
	 * Returns the size of the virtual memory region, interpreted as an UINT32!
	 * <p>
	 * The size is determined by the {@code REG_VMEM_MAX_BOUND} register.
	 * If that register contains {@code 0}, the size defaults to the physical memory
	 * length (HLP).
	 *
	 * @return the virtual memory size as an unsigned 32-bit value represented in a
	 *         {@code long}.
	 */
	final long getVirtualMemorySize() {
		int size = this.registers[REG_VMEM_MAX_BOUND];
		if (size == 0) {
			return this.memory.length() & 0xFFFFFFFF;
		}
		return Integer.toUnsignedLong(size);
	}
	
	/**
	 * Translates a virtual address into a physical address using UINT32
	 * wraparound semantics.
	 * <p>
	 * The translation is performed by adding the VMEM offset register to the 
	 * unsigned value of {@code virtualAddress}. The result is then truncated to 32 bits
	 *
	 * @param virtualAddress a 32-bit virtual address (interp as unsigned)
	 * @return the computed 32-bit physical address as a long
	 */
	final long virtualToPhysicalAddress(int virtualAddress) {
		return (this.registers[REG_VMEM_OFFSET] + Integer.toUnsignedLong(virtualAddress)) & 0xFFFFFFFF;
	}
	
	/**
	 * Checks whether a virtual address lies outside the bounds of the allowed region.
	 * @see {@link FL32REmulator#getVirtualMemorySize()}
	 */
	final boolean isVirtualAddressOOB(int virtualAddress) {
		return Integer.toUnsignedLong(virtualAddress) >= getVirtualMemorySize();
	}
	
	/**
	 * Writes a single byte to virtual memory.
	 * <p>
	 * The virtual address is first checked against the VMEM bounds. If
	 * the address is out of bounds, a memory fault is raised and the write is
	 * ignored. Otherwise, the virtual address is translated to a physical address
	 * using {@link #virtualToPhysicalAddress}, and the byte is written to
	 * memory.
	 *
	 * @param vAddress the virtual memory address to write to
	 * @param data     the byte value to write
	 */
	final void writeByteMemory(int vAddress, byte data) {
		if (this.isVirtualAddressOOB(vAddress)) {
			this.raiseFault(FaultType.FAULT_MEM);
			return;
		}
		this.memory.set(virtualToPhysicalAddress(vAddress), data);
	}
	
	/**
	 * Writes a 32-bit word to virtual memory in BIG-ENDIAN order
	 *
	 * @param vAddress the starting virtual memory address to write the word
	 * @param data the 32-bit word to write
	 */
	final void writeWordMemory(int vAddress, int data) {
		writeByteMemory(vAddress, (byte) ((data >> 24) & 0xFF));
		writeByteMemory(vAddress + 1, (byte) ((data >> 16) & 0xFF));
		writeByteMemory(vAddress + 2, (byte) ((data >> 8) & 0xFF));
		writeByteMemory(vAddress + 3, (byte) (data & 0xFF));
	}
	
	/**
	 * Reads a single byte from virtual memory
	 *
	 * @param vAddress the virtual memory address to read from
	 * @return the byte value at the specified virtual address
	 */
	final byte readByteMemory(int vAddress) {
		if (this.isVirtualAddressOOB(vAddress)) {
			this.raiseFault(FaultType.FAULT_MEM);
			return 0;
		}
		return this.memory.get(virtualToPhysicalAddress(vAddress));
	}
	
	/**
	 * Reads a 32-bit word from virtual memory in BIG-ENDIAN order
	 * <p>
	 * Four consecutive bytes are read starting at {@code vAddress} 
	 * and smashed into a 32-bit word.
	 *
	 * @param vAddress the starting virtual memory address to read from
	 * @return the 32-bit word read from virtual memory
	 */
	final int readWordMemory(int vAddress) {
		return Utils.beBytesToInt(
			readByteMemory(vAddress), 
			readByteMemory(vAddress + 1),
			readByteMemory(vAddress + 2),
			readByteMemory(vAddress + 3)
		);
	}
	
	// STACK MANIPULATION
	// STACK CONVENTION: INDUSTRY STANDARD
	final void pushToStack(int value) {
		// example
		// 00 00 00 00 00 00 00 ...
		//                   ^^ begins
		// reserve 4 bytes for the number
		// 00 00 CA FE BA BE 00 ...
		//       ^^ new stack head, begins to write here
		// the last byte is intentionally left blank
		int target = readRegister(REG_STACK_POINTER) - 4; // reserve 4 bytes
		writeWordMemory(target, value);
		writeRegister(REG_STACK_POINTER, target); // write the new head
	}
	
	final int popFromStack() {
		// example, current stack:
		// 00 00 CA FE BA BE 00 ...
		//       ^^ HEAD, read there
		int target = readRegister(REG_STACK_POINTER);
		int value = readWordMemory(target);
		// 00 00 CA FE BA BE 00 ...
		//       ^^ OLD      ^^ new HEAD after POP
		writeRegister(REG_STACK_POINTER, target + 4); // consume the latest value
		return value;
	}
}
