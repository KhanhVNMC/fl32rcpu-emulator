package dev.gkvn.cpu;

import static dev.gkvn.cpu.FL32RConstants.*;

// this CPU is BIG-ENDIAN, 32-bit based
public class FL32REmulator {
	// the internal CPU parts, registers, flags
	public boolean ZFL = false; // zero flag (for CMP)
	public boolean CFL = false; // carry flag = True; if the last arithmetic (including CMP) operation is negative (negative = smaller, positive = greater)
	public boolean OFL = false; // overflow flag
	public boolean HLP = true; // true for since the kernel is loaded first anyway, must be set later if desired
	
	public final int registers[] = new int[32]; // 32x 32 bits register
	
	// the CPU's memory (RAM)
	public final byte memory[];
	
	public FL32REmulator(int memorySize) {
		this.memory = new byte[memorySize];
		this.registers[REG_STACK_POINTER] = memorySize - 1;
	}
	
	public void raiseFault(FaultType faultType) {
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
	final void pushToStack(int value) {
		// example
		// 00 00 00 00 00 00 00 ...
		//                   ^^ begins
		// reserve 4 bytes for the number
		// 00 00 00 00 00 00 00 ...
		//       ^^ new stack head
		int target = readRegister(REG_STACK_POINTER) - 4; // reserve 4 bytes
		// reserve 4 bytes for the number
		// 00 00 00 CA FE BA BE ...
		//          ^^ begin to write here
		writeWordMemory(target + 1, value);
		writeRegister(REG_STACK_POINTER, target); // write the new head
	}
	
	final int popFromStack() {
		// example, current stack:
		// 00 00 00 CA FE BA BE ...
		//       ^^ HEAD
		int target = readRegister(REG_STACK_POINTER);
		// 00 00 00 CA FE BA BE ...
		//          ^^ READ HERE
		int value = readWordMemory(target + 1);
		// 00 00 00 CA FE BA BE ...
		//       ^^ OLD      ^^ new after POP
		writeRegister(REG_STACK_POINTER, target + 4); // consume the latest value
		return value;
	}
	
	public static void main(String[] args) {
		var a = new FL32REmulator(8192);
		for (int i = 0; i < 1_000; i++) {
			a.pushToStack(i);
		}
		
		for (int i = 0; i < 1_000; i++) {
			System.out.print(a.popFromStack() + " ");
		}
	}
}
