package dev.gkvn.cpu.fl32r;

import static dev.gkvn.cpu.fl32r.FL32RConstants.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import dev.gkvn.cpu.ByteMemorySpace;
import dev.gkvn.cpu.GenericCPUEmulator;
import dev.gkvn.cpu.ImmutableByteSpace;

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
	private final ByteMemorySpace memory;
	
	// the CPU's read only memory ROM
	private final ByteMemorySpace readOnlyMemory;
	
	// cpu internal states
	private boolean cpuHalted = false;
	private int IPR = 0, IFR = 0; // interrupt saved program counter and flag (return)
	private boolean interruptMask = false; // mask == int not allowed
	
	// emulator parameter/controls
	private double nsPerCycle;
	private int frequencyHz;
	private boolean cpuKilled = false;
	private boolean cpuStarted = false;
	private boolean bootRomLoaded = false;
	private boolean singleStepMode = false;
	private Set<Long> breakpointsVirtual = new HashSet<>();
	private Set<Long> breakpointsPhysical = new HashSet<>();
	
	public FL32REmulator(long memorySize) {
		// clamp memorySize to 32-bit unsigned max
		if (memorySize < 0 || memorySize > 0xFFFFFFFFL) {
			throw new IllegalArgumentException("Memory size must be 0 -> 4GB");
		}
		this.setFrequencyHz(32_000_000); // 32 MHZ cpu
		this.memory = new ByteMemorySpace(memorySize);
		this.readOnlyMemory = new ByteMemorySpace(1024); // 1 byte of ROM (for boot code)
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
	public void loadBootROM(byte[] program) {
		if (bootRomLoaded) {
			throw new IllegalStateException("Boot ROM has already been loaded; cannot load twice!");
		}
		if (program.length > readOnlyMemory.length()) {
			throw new IllegalArgumentException(
				"Boot ROM size (" + program.length + " bytes) exceeds ROM size (" + readOnlyMemory.length() + " bytes)!"
			);
		}
		// copy over the program to rom
		for (int i = 0; i < program.length; i++) {
			readOnlyMemory.set(i, program[i]);
		}
		this.bootRomLoaded = true;
	}
	
	Random r = new Random();
	public void randomize(int startAt) {
		for (int i = startAt; i < memory.length(); i++) {
			memory.set(i, (byte) (r.nextInt(0, 255) & 0xFF));
		}
	}
	
	/**
	 * @return the current working memory, mutating it will
	 * change the state of the emulator
	 */
	public ByteMemorySpace getWorkingMemory() {
		return this.memory;
	}
	
	// MAIN CPU LOOP
	@Override
	public void start(boolean startInSingleStepMode) {
		if (this.cpuStarted) {
			throw new IllegalStateException("CPU has already been started; cannot start twice!");
		}
		this.cpuStarted = true;
		// start the cpu halted and in single step mode, needs manual stepping
		this.reset(startInSingleStepMode);
		
		// autonomous execution
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
	public synchronized void stepExecution() {
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
			boolean breakPointHit = this.isAtBreakpoint(currentPC);
			if (breakPointHit && !this.isOnSingleStepMode()) {
				this.activateSingleStepMode();
				return;
			}
			// step to the next instruction, since execution may alter PC, this must be incremented here
			this.writeRegister(REG_PROGRAM_COUNTER, currentPC + 4); // 4 bytes (32bit) instruction
			// ===== DECODE =====
			byte opcode = (byte) ((instruction >>> 24) & 0xFF); // 8 MSB
			int operand = instruction & 0xFFFFFF;
			// ===== EXECUTE =====
			long execStart = System.nanoTime();
//			System.out.println("INSTRUCTION: " + String.format("%32s", Integer.toBinaryString(instruction)).replace(' ', '0'));
//			System.out.println("OPCODE: " + String.format("%8s", Integer.toBinaryString(opcode)).replace(' ', '0'));
//			System.out.println("OPERAND: " + String.format("%24s", Integer.toBinaryString(operand)).replace(' ', '0') + "\n");

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
		} catch (FaultRaisedException ignored) {
			// dont catch this
		} // runtime exception will throw immediately, crashing the entire emulator (as it should)
	}
	
	/**
	 * Determines whether the current program counter (PC) matches ANY active breakpoint,
	 * either virtual or physical. This method is called after FETCH.
	 *
	 * @param currentVirtualPC the program counter value before executing the current instruction,
               interpreted as a 32-bit uint virtual address
	 * @return {@code true} if an enabled breakpoint exists at the current PC,
	 *         {@code false} otherwise
	 */
	public boolean isAtBreakpoint(int currentVirtualPC) {
		if (breakpointsVirtual.size() == 0 && breakpointsPhysical.size() == 0) {
			return false;
		}
		long virtualAddress = Integer.toUnsignedLong(currentVirtualPC);
		long physicalAddress = virtualToPhysicalAddress(currentVirtualPC);
		return breakpointsVirtual.contains(virtualAddress) || breakpointsPhysical.contains(physicalAddress);
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
	public void reset(boolean resetToSingleStepMode) {
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
		// jump to the VALUE of reset vector (inside the ROM)
		writeRegister(REG_PROGRAM_COUNTER, ROM_MMAP_START); 
		if (resetToSingleStepMode) {
			this.activateSingleStepMode();
			return;
		}
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
	public void addBreakpoint(long virtualAddress) {
		breakpointsVirtual.add(virtualAddress);
	}
	
	@Override
	public void removeBreakpoint(long virtualAddress) {
		breakpointsVirtual.remove(virtualAddress);
	}
	
	@Override
	public void addBreakpointPhysical(long physicalAddresss) {
		breakpointsPhysical.add(physicalAddresss);
	}
	
	@Override
	public void removeBreakpointPhysical(long physicalAddresss) {
		breakpointsPhysical.remove(physicalAddresss);
	}
	
	@Override
	public boolean isKilled() {
		return this.cpuKilled;
	}
	
	@Override
	public ByteMemorySpace getMemory() {
		return this.memory;
	}
	
	@Override
	public ByteMemorySpace getReadOnlyMemory() {
		return this.readOnlyMemory;
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
		// register operand could be interpreted differently
		int rOp0 = (operand >> 19) & 0b11111;
		int rOp1 = (operand >> 14) & 0b11111;
		int rOp2 = (operand >> 9) & 0b11111;
		int rDest = rOp0; // rOp0 could be interpreted as rDestination
		
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
			// load an immediate to the lower 16 bits of a register
			case LLI: {
				int immediate = (operand >> 3) & 0xFFFF;
				writeRegister(rDest, immediate);
				break;
			}
			// LOAD convention: LOAD A, B, OFFSET <=> READ B + OFFSET STORE TO A
			// load a word: rDest = Memory[rOp1]...[rOp1+3]
			case LDW: {
				int baseAddress = readRegister(rOp1);
				int offset = Utils.convertImm14ToInt(operand & 0x3FFF);
				writeRegister(rDest, readWordMemory(baseAddress + offset));
				break;
			}
			// load a byte: rDest = Memory[rOp1]
			case LDB: {
				int baseAddress = readRegister(rOp1);
				int offset = Utils.convertImm14ToInt(operand & 0x3FFF);
				// mask off the upper 24 bits (0xFF)
				writeRegister(rDest, readByteMemory(baseAddress + offset), 0xFF);
				break;
			}
			// STORE convention: STORE A, B, OFFSET <=> READ A STORE TO B + OFFSET
			// store a word: Memory[rMemDest...3] = rSrc
			case STW: {
				// disambiguation
				int rSrc = rOp0, baseAddress = readRegister(rOp1);
				int offset = Utils.convertImm14ToInt(operand & 0x3FFF);
				writeWordMemory(baseAddress + offset, readRegister(rSrc));
				break;
			}
			// store a byte: Memory[rMemDest] = rSrc
			case STB: {
				// disambiguation
				int rSrc = rDest, baseAddress = readRegister(rOp1);
				int offset = Utils.convertImm14ToInt(operand & 0x3FFF);
				byte value = (byte)(readRegister(rSrc) & 0xFF);
				writeByteMemory(baseAddress + offset, value);
				break;
			}
			// arithmetic operations: rDest = rOp1 [opcode] rOp2
			case ADD: case SUB:
			case MUL: case UMUL: 
			case DIV: case UDIV:
			case MOD: case UMOD: {
				int left = readRegister(rOp1), right = readRegister(rOp2);
				long uLeft = Integer.toUnsignedLong(left), uRight = Integer.toUnsignedLong(right);
				int result = switch (opcode) {
					case ADD -> left + right;
					case SUB -> left - right;
					// signed
					case MUL -> left * right;
					case DIV -> right != 0 ? (left / right) : raiseFault(FaultType.FAULT_DIVZERO);
					case MOD -> right != 0 ? (left % right) : raiseFault(FaultType.FAULT_DIVZERO);
					// unsigned (a mess)
					case UMUL -> (int)((uLeft * uRight) & 0xFFFFFFFFL);
					case UDIV -> uRight != 0 ? (int)((uLeft / uRight) & 0xFFFFFFFFL) : raiseFault(FaultType.FAULT_DIVZERO);
					case UMOD -> uRight != 0 ? (int)((uLeft % uRight) & 0xFFFFFFFFL) : raiseFault(FaultType.FAULT_DIVZERO);
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
			case SHL: case SHR: case SRA:
			case NOT: {
				int left = readRegister(rOp1), right = readRegister(rOp2);
				int result = switch (opcode) {
					case AND -> left & right;
					case OR -> left | right;
					case XOR -> left ^ right;
					case SHL -> left << right;
					case SHR -> left >>> right;
					case SRA -> left >> right;
					case NOT -> ~left;
					default -> raiseFault(FaultType.FAULT_ILLEGAL);
				};
				writeRegister(rDest, result);
				// for bitwise, only this flag is useful
				this.ZFL = result == 0;
				break;
			}
			// immediate arithmetic & bitwise ops (special ones)
			// rDest [ophere]= immediate (19 bits lsb; signed)
			case ADDI:
			case ANDI: case ORI: case XORI:
			case SHRI: case SRAI:
			case SHLI: {
				int current = readRegister(rDest); 
				int immediate = Utils.convertImm19ToInt(operand);
				int result = switch (opcode) {
					case ADDI -> current + immediate;
					case ANDI -> current & immediate;
					case ORI  -> current | immediate;
					case XORI -> current ^ immediate;
					case SHLI -> current << (immediate & 0b11111);
					case SHRI -> current >>> (immediate & 0b11111);
					case SRAI -> current >> (immediate & 0b11111);
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
			// COMPARE (like SUB): r0 (rOp0) - r1 (rOp1) [rOp2 is ignored]
			case CMP: {
				int left = readRegister(rOp0), right = readRegister(rOp1);
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
				absAddress += Utils.convertImm24ToInt(operand); // add the rel-jump 
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
				absAddress += Utils.convertImm24ToInt(operand); // add the rel-jump 
				writeRegister(REG_PROGRAM_COUNTER, absAddress);
				break;
			}
			// CLR (absolute call, jump to address (sits in RDEST register))
			case CLR: {
				pushToStack(readRegister(REG_PROGRAM_COUNTER));
				writeRegister(REG_PROGRAM_COUNTER, readRegister(rDest));
				break;
			}
			// ABSOLUTE JUMPS (jump straight to address (sits in RDEST register))
			case JR: {
				writeRegister(REG_PROGRAM_COUNTER, readRegister(rDest));
				break;
			}
			// RETURN (pop and jump)
			case RET: {
				writeRegister(REG_PROGRAM_COUNTER, popFromStack());
				break;
			}
			// INT (interrupt)
			case INT: {
				softwareIRQ(Utils.convertImm24ToInt(operand));
				break;
			}
			// HLP INSTRUCTIONS (Executed by the Kernel/HLP entities)
			case VMO: {
				// should use HR(x) (Host-Level Privilege dedicated APRs)
				// example:
				// LDI  HR0, 0xCAFEBABE
				// VMO  HR0
				// XOR  HR0, HR0, HR0 ; for safety
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
			// copy IFR into a general purpose reg
			case GTFS: {
				if (!this.HLP) raiseFault(FaultType.FAULT_PRIV);
				writeRegister(rDest, this.IFR);
				break;
			}
			// set the flags according to a gpr
			case STFS: {
				if (!this.HLP) raiseFault(FaultType.FAULT_PRIV);
				int packedFlags = readRegister(rOp1);
				this.ZFL = (packedFlags & Utils.FLAG_Z) == 1;
				this.NFL = (packedFlags & Utils.FLAG_N) == 1;
				this.OFL = (packedFlags & Utils.FLAG_O) == 1;
				break;
			}
			// copy IPR into a general purpose reg
			case GTPC: {
				if (!this.HLP) raiseFault(FaultType.FAULT_PRIV);
				writeRegister(rDest, this.IPR);
				break;
			}
			// allow for interrupts
			case STI: {
				if (!this.HLP) raiseFault(FaultType.FAULT_PRIV);
				this.interruptMask = false;
				break;
			}
			// disallow interrupts
			case CLI: {
				if (!this.HLP) raiseFault(FaultType.FAULT_PRIV);
				this.interruptMask = true;
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
		// enters the trap
		int toEnter = switch (faultType) {
			case FAULT_MEM -> FAULT_MEM_VECTOR;
			case FAULT_ILLEGAL -> FAULT_ILLEGAL_VECTOR;
			case FAULT_DIVZERO -> FAULT_DIVZERO_VECTOR;
			case FAULT_STACK_OVERFLOW -> FAULT_STACK_OVERFLOW_VECTOR;
			case FAULT_STACK_UNDERFLOW -> FAULT_STACK_UNDERFLOW_VECTOR;
			case FAULT_OVERFLOW -> FAULT_OVERFLOW_VECTOR;
			default -> throw new RuntimeException("Emulator not up to spec!"); 
		};
		System.out.printf("FAULT: %s, pc=0x%X, fault handler at 0x%X &(0x%X)\n", 
			faultType, readRegister(REG_PROGRAM_COUNTER) - 4, toEnter,
			readWordMemory(toEnter)
		);
		enterTrap(toEnter, false);
		System.exit(1);
		// abuse jvm exception latching
		throw new FaultRaisedException(); // interrupts current execute() (latch)
	}
	
	// CPU INTERRUPTS/TRAP HANDLE
	final void softwareIRQ(int type) {
		if (type < 0 || type >= SOFTWARE_INT_COUNT) {
			raiseFault(FaultType.FAULT_ILLEGAL);
			return;
		}
		// trap the interrupt
		enterTrap(SOFTWARE_INT_BASE + (WORD_SIZE * type), true);
	}
	
	public final void hardwareIRQ(int type) {
		if (type < 0 || type >= HARDWARE_INT_COUNT) {
			enterTrap(UNHANDLED_INTERRUPT_VECTOR, true);
			return;
		}
		// trap the interrupt
		enterTrap(HARDWARE_INT_BASE + (WORD_SIZE * type), true);
	}
	
	final void enterTrap(int vectorAddress, boolean isInterrupt) {
		// if its already in HLP and a fault fires, you are cooked
		if (this.HLP && !isInterrupt) {
			vectorAddress = PANIC_VECTOR; // panic
		}
		if (isInterrupt && interruptMask) {
			return; // ignore
		}
		this.IPR = readRegister(REG_PROGRAM_COUNTER);
		this.IFR = Utils.packFlags(ZFL, NFL, OFL);
		this.HLP = true;
		this.interruptMask = true; // do not allow interrupts from now on
		// resume to the full memory region (at 0x0000-HIMEM)
		writeRegister(REG_VMEM_OFFSET, 0x00);
		writeRegister(REG_VMEM_MAX_BOUND, 0x00);
		// jump to interrupt handle (HLP)
		int irqHandleAddress = readWordMemory(vectorAddress);
		if (irqHandleAddress == UNDEFINED_VECTOR) {
			switch (vectorAddress) {
				case PANIC_VECTOR:
				case UNHANDLED_INTERRUPT_VECTOR: {
					throw new RuntimeException("Critical vector missing: " + vectorAddress);
				}
				default: {
					irqHandleAddress = isInterrupt ? UNHANDLED_INTERRUPT_VECTOR : PANIC_VECTOR;
				}
			}
		}
		writeRegister(REG_PROGRAM_COUNTER, irqHandleAddress);
	}
	
	// REGISTER MANIPULATION
	final void writeRegister(int regIndex, int value) {
		this.writeRegister(regIndex, value, 0xFF_FF_FF_FF); // no-op mask
	}
	
	final void writeRegister(int regIndex, int value, int mask) {
		regIndex &= 0b11111;
		if (regIndex == REG_ZERO) return; // prevent writes to Zero reg
		// prevent accessing a forbidden register
		if (regIndex >= REG_VMEM_OFFSET && !HLP) {
			this.raiseFault(FaultType.FAULT_PRIV);
			return;
		}
		this.registers[regIndex] = value & mask; // write, zoop
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
		long physSize = this.memory.length() & 0xFFFFFFFF;
		if (size == 0) {
			return physSize;
		}
		return Math.min(physSize, Integer.toUnsignedLong(size));
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
		return (Integer.toUnsignedLong(this.registers[REG_VMEM_OFFSET]) 
			+ Integer.toUnsignedLong(virtualAddress)
		) & 0xFFFFFFFF;
	}
	
	/**
	 * Checks whether a virtual address lies outside the bounds of the allowed region.
	 * @see {@link FL32REmulator#getVirtualMemorySize()}
	 */
	final boolean isVirtualAddressOOB(int virtualAddress) {
		return Integer.toUnsignedLong(virtualAddress) >= getVirtualMemorySize();
	}
	
	final boolean isPhysicalAddressRAM(long pAddress) {
		return pAddress <= Integer.toUnsignedLong(RAM_WINDOW_END);
	}
	
	final boolean isPhysicalAddressROM(long pAddress) {
		return pAddress >= Integer.toUnsignedLong(ROM_MMAP_START) 
			&& pAddress < Integer.toUnsignedLong(MMIO_REGION_START);
	}
	
	final long pAddressToROMAddress(long pAddress) {
		return pAddress - Integer.toUnsignedLong(ROM_MMAP_START);
	}
	
	final boolean isPhysicalAddressMMIO(long pAddress) {
		return pAddress >= Integer.toUnsignedLong(MMIO_REGION_START);
	}
	
	/**
	 * Reads a single byte from virtual memory
	 *
	 * @param vAddress the virtual memory address to read from
	 * @return the byte value at the specified virtual address
	 */
	final byte readByteMemory(int vAddress) {
		long pAddress = virtualToPhysicalAddress(vAddress);
		if (isPhysicalAddressRAM(pAddress)) {
			// guarded normal accessing
			if (this.isVirtualAddressOOB(vAddress)) {
				this.raiseFault(FaultType.FAULT_MEM);
				return 0;
			}
			return this.memory.get(pAddress);
		}
		
		System.out.printf("read: 0x%X (%s)\n", pAddress, isPhysicalAddressROM(pAddress));
		// rom read is allowed, yk, "read only"
		if (isPhysicalAddressROM(pAddress)) { 
			System.out.printf("rom address: 0x%X, val: 0x%X\n", pAddressToROMAddress(pAddress), this.readOnlyMemory.get(pAddressToROMAddress(pAddress)));
			return this.readOnlyMemory.get(pAddressToROMAddress(pAddress));
		}
		
		if (isPhysicalAddressMMIO(pAddress)) {
			return -1; // TODO 
		}
		
		return 0;
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
		long pAddress = virtualToPhysicalAddress(vAddress);
		if (isPhysicalAddressRAM(pAddress)) {
			// guarded normal accessing
			if (this.isVirtualAddressOOB(vAddress)) {
				this.raiseFault(FaultType.FAULT_MEM);
				return;
			}
			this.memory.set(pAddress, data);
		}
		if (isPhysicalAddressMMIO(pAddress)) {
			// TODO
			System.out.println("CPU writes at mmio " + pAddress + " val: " + data);
		}
	}
	
	/**
	 * Writes a 32-bit word to virtual memory in BIG-ENDIAN order
	 *
	 * @param vAddress the starting virtual memory address to write the word
	 * @param data the 32-bit word to write
	 */
	final void writeWordMemory(int vAddress, int data) {
		// write from the furthest so if it fault, the operation
		// stays atomic (FL32R specs)
		writeByteMemory(vAddress + 3, (byte) (data & 0xFF));
		writeByteMemory(vAddress + 2, (byte) ((data >>> 8) & 0xFF));
		writeByteMemory(vAddress + 1, (byte) ((data >>> 16) & 0xFF));
		writeByteMemory(vAddress, (byte) ((data >>> 24) & 0xFF));
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
		// read from the furthest, same reason as above
		byte lsb = readByteMemory(vAddress + 3);
		byte mb1 = readByteMemory(vAddress + 2);
		byte mb2 = readByteMemory(vAddress + 1);
		byte msb = readByteMemory(vAddress);
		return Utils.beBytesToInt(
			msb, mb2, mb1, lsb
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
