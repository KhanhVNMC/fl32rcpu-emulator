package dev.gkvn.cpu.assembler.fl32r.backend.isa;

import static dev.gkvn.cpu.fl32r.FL32RHelper.*;
import static dev.gkvn.cpu.assembler.fl32r.backend.isa.CodegenUtils.*;

import java.util.function.BiConsumer;

import dev.gkvn.cpu.assembler.fl32r.backend.BackendCodegen;
import dev.gkvn.cpu.assembler.fl32r.frontend.core.Instruction;
import dev.gkvn.cpu.assembler.fl32r.frontend.operands.ImmLiteral;
import dev.gkvn.cpu.assembler.fl32r.frontend.operands.RegisterOperand;
import dev.gkvn.cpu.assembler.fl32r.frontend.operands.SizedMemoryOperand;
import dev.gkvn.cpu.fl32r.FL32RConstants;

public class PseudoInstructions {
	static BiConsumer<BackendCodegen, Instruction> loadImmediate() {
		return (be, i) -> {
			int reg = reg(i, 0), value = literal(i, 1);
			int low = LO(value), high = HI(value);
			// micro-optimization
			if (high == 0) {
				// MOV rDest, rZero ; MUCH cheaper than LUI
				be.emit(R(FL32RConstants.MOV, reg, FL32RConstants.REG_ZERO, 0));
			} else {
				be.emit(U(FL32RConstants.LUI, reg, high));
			}
			// another micro optimization
			if (low == 0) {
				be.emit(NO(FL32RConstants.NOP)); // cheaper
				return;
			}
			be.emit(I(FL32RConstants.ORI, reg, low));
		};
	}
	
	static BiConsumer<BackendCodegen, Instruction> loadVariable() {
		return loadStoreVariable(false);
	}
	
	static BiConsumer<BackendCodegen, Instruction> storeVariable() {
		return loadStoreVariable(true);
	}
	
	private static BiConsumer<BackendCodegen, Instruction> loadStoreVariable(boolean store) {
		return (be, i) -> {
			RegisterOperand rop;
			SizedMemoryOperand mop;
			// order-agnostic operands
			if (i.operands[0] instanceof RegisterOperand r 
			 && i.operands[1] instanceof SizedMemoryOperand m) {
				rop = r;
				mop = m;
			} else if (i.operands[1] instanceof RegisterOperand r 
					&& i.operands[0] instanceof SizedMemoryOperand m) {
				rop = r;
				mop = m;
			} else {
				throw new IllegalArgumentException("This is not supposed to happen! FRONTEND_FAULT. IR: " + i);
			}
			
			// decide which opcode to use
			int opcode;
			if (store) {
				opcode = mop.sizeByte() == 4 ? FL32RConstants.STW : FL32RConstants.STB;
			} else {
				opcode = mop.sizeByte() == 4 ? FL32RConstants.LDW : FL32RConstants.LDB;
			}
			
			be.emit(M(
				opcode, 
				rop.register(), // rDest/Src
				mop.memop().base().register(), // rBase
				((ImmLiteral) mop.memop().offset()).value() // [rBase + offset]
			));
		};
	}
	
	static BiConsumer<BackendCodegen, Instruction> loadEffectiveAddress() {
		return (be, i) -> {
			RegisterOperand rop = (RegisterOperand) i.operands[0];
			SizedMemoryOperand mop = (SizedMemoryOperand) i.operands[1];
			
			int reg = rop.register();
			// emit the MOV (pc)
			be.emit(R(FL32RConstants.MOV, reg, FL32RConstants.REG_PROGRAM_COUNTER, 0));
			// note: since this is a pseudo-op and PC relative 
			// memop().offset() will resolve to the topmost instruction that "represent"
			// this cluster (aka the anchor instruction)
			
			// in this case, we expand by 2, so add +4 to the lower one
			// > PSEUDO $variable 
			// expands to
			// > REAL  ... << the anchor of this one
			// > REAL2 ...
			be.emit(I(FL32RConstants.ADDI, reg, 
				((ImmLiteral) mop.memop().offset()).value() + 4
			));
		};
	}
}
