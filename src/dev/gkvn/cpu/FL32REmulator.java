package dev.gkvn.cpu;

import static dev.gkvn.cpu.FL32RConstants.*;

import java.util.Arrays;

// this CPU is BIG-ENDIAN, 32-bit based
public class FL32REmulator {
	// the internal CPU parts, registers, flags
	public boolean ZFL = false; // zero flag (for CMP)
	public boolean NFL = false; // negative flag; if the last arithmetic (including CMP) operation is negative
	public boolean OFL = false; // overflow flag
	public boolean HLP = true; // true for since the kernel is loaded first anyway, must be set later if desired
	
	public final int registers[] = new int[32]; // 32x 32 bits register
	
	// the CPU's memory (RAM)
	public final byte memory[];
	
	// emulator
	private double nsPerCycle;
	
	public FL32REmulator(int memorySize) {
		this.memory = new byte[memorySize];
		this.registers[REG_STACK_POINTER] = memorySize - 1;
	}
	
	public void setFrequency(int hertz) {
		this.nsPerCycle = 1_000_000_000.0 / hertz;
	}
	
	// MAIN CPU LOOP
	public void start() {
		while (true) {
			// FETCH
			int currentPC = this.readRegister(REG_PROGRAM_COUNTER);
			// step to the next instruction, since execution may alter PC, this must be incremented here
			this.writeRegister(REG_PROGRAM_COUNTER, currentPC + 4); // 4 bytes instruction
			int instruction = this.readWordMemory(currentPC);
			// DECODE
			byte opcode = (byte)((instruction >> 24) & 0xFF); // 8 MSB
			int operand = instruction & 0xFFFFFF;
			// EXECUTE
			execute(opcode, operand);
		}
	}
	
	public void execute(byte opcode, int operand) {
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
			case ADD:
			case SUB:
			case MUL:
			case DIV:
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
			// bitwise operations (except not):
			case AND:
			case OR:
			case XOR:
			case SHL:
			case SHR: {
				int left = readRegister(rOp1), right = readRegister(rOp2);
				int result = switch (opcode) {
					case AND -> left & right;
					case OR -> left | right;
					case XOR -> left ^ right;
					case SHL -> left << right;
					case SHR -> left >>> right;
					default -> raiseFault(FaultType.FAULT_ILLEGAL);
				};
				writeRegister(rDest, result);
				break;
			}
			// a special bitwise one
			case NOT: {
				writeRegister(rDest, ~readRegister(rOp1));
				return;
			}
			// immediate arithmetic & bitwise ops (special ones)
			// rDest [ophere]= immediate
			case ADDI:
			case ANDI:
			case ORI: {
				int current = readRegister(rDest); 
				int immediate = operand & 0x7FFFFF; 
				int result = switch (opcode) {
					case ADDI -> current + immediate;
					case ANDI -> current & immediate;
					case ORI  -> current | immediate;
					default -> raiseFault(FaultType.FAULT_ILLEGAL);
				};
				writeRegister(rDest, result);
				// set the flags
				if (opcode == ADDI) {
					this.ZFL = result == 0;
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
			case JGE: case JLE: {
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
					default -> false;
				};
				// jump to it
				if (shouldJump) {
					writeRegister(REG_PROGRAM_COUNTER, absAddress);
				}
				break;
			}
			// ABSOLUTE JUMPS (jump straight to address)
			case JR: {
				writeRegister(REG_PROGRAM_COUNTER, operand & 0xFFFFFF); // treat as unsigned 24-bit
				break;
			}
			default: {
				this.raiseFault(FaultType.FAULT_ILLEGAL);
				break;
			}
		}
	}
	
	public int raiseFault(FaultType faultType) {
		// TODO temporary
		throw new RuntimeException("[FAULT] CPU HALTED! Fault: " + faultType
			+ ". VMEM OFFSET: " + this.registers[REG_VMEM_OFFSET]
			+ ". PROGRAM COUNTER: " + this.registers[REG_PROGRAM_COUNTER]
		);
	}
	
	// REGISTER MANIPULATION
	final void writeRegister(int regIndex, int value) {
		if (regIndex == REG_ZERO) return; // prevent writes to Zero reg
		// prevent accessing a forbidden register
		if (regIndex >= REG_VMEM_OFFSET && !HLP) {
			this.raiseFault(FaultType.FAULT_PRIV);
			return;
		}
		this.registers[regIndex] = value; // write, zoop
	}
	
	final int readRegister(int regIndex) {
		if (regIndex >= REG_VMEM_OFFSET && !HLP) {
			// prevent accessing a forbidden register
			this.raiseFault(FaultType.FAULT_PRIV);
			return 0;
		}
		return (regIndex == REG_ZERO) ? 0 : registers[regIndex];
	}
	
	// MEMORY MANIPULATION
	final int getVirtualMemorySize() {
		int size = this.registers[REG_VMEM_MAX_BOUND];
		if (size == 0) {
			return this.memory.length;
		}
		return size;
	}
	
	final int virtualToPhysicalAddress(int virtualAddress) {
		return this.registers[REG_VMEM_OFFSET] + virtualAddress;
	}
	
	final boolean isVirtualAddressOOB(int virtualAddress) {
		return virtualAddress < 0 || virtualAddress >= getVirtualMemorySize();
	}
	
	final void writeByteMemory(int vAddress, byte data) {
		if (this.isVirtualAddressOOB(vAddress)) {
			this.raiseFault(FaultType.FAULT_MEM);
			return;
		}
		this.memory[virtualToPhysicalAddress(vAddress)] = data;
	}
	
	final void writeWordMemory(int vAddress, int data) {
		writeByteMemory(vAddress, (byte) ((data >> 24) & 0xFF));
		writeByteMemory(vAddress + 1, (byte) ((data >> 16) & 0xFF));
		writeByteMemory(vAddress + 2, (byte) ((data >> 8) & 0xFF));
		writeByteMemory(vAddress + 3, (byte) (data & 0xFF));
	}
	
	final byte readByteMemory(int vAddress) {
		if (this.isVirtualAddressOOB(vAddress)) {
			this.raiseFault(FaultType.FAULT_MEM);
			return 0;
		}
		return this.memory[virtualToPhysicalAddress(vAddress)];
	}
	
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
	
	
	public static void main(String[] args) {
		var a = new FL32REmulator(100);
		for (int i = 0; i < 20; i++) {
			a.pushToStack(i);
		}
		
		for (int i = 0; i < 20; i++) {
			System.out.print(a.popFromStack() + " ");
		}
		
		System.out.println("\n" + Arrays.toString(a.memory));
	}
}
