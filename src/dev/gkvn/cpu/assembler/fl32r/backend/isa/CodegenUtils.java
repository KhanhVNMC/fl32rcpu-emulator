package dev.gkvn.cpu.assembler.fl32r.backend.isa;

import static dev.gkvn.cpu.fl32r.FL32RHelper.I;
import static dev.gkvn.cpu.fl32r.FL32RHelper.J;
import static dev.gkvn.cpu.fl32r.FL32RHelper.M;
import static dev.gkvn.cpu.fl32r.FL32RHelper.NO;
import static dev.gkvn.cpu.fl32r.FL32RHelper.R;
import static dev.gkvn.cpu.fl32r.FL32RHelper.U;

import java.util.function.BiConsumer;

import dev.gkvn.cpu.assembler.fl32r.backend.BackendCodegen;
import dev.gkvn.cpu.assembler.fl32r.frontend.core.Instruction;
import dev.gkvn.cpu.assembler.fl32r.frontend.operands.ImmLiteral;
import dev.gkvn.cpu.assembler.fl32r.frontend.operands.MemoryOperand;
import dev.gkvn.cpu.assembler.fl32r.frontend.operands.RegisterOperand;

public class CodegenUtils {
	// no-type
	static BiConsumer<BackendCodegen, Instruction> noType(int opcode) {
		return (be, i) -> be.emit(NO(opcode));
	}

	// r-type
	static BiConsumer<BackendCodegen, Instruction> rType(int opcode) {
		return (be, i) -> be.emit(R(opcode, reg(i, 0), reg(i, 1), reg(i, 2)));
	}

	// j-type
	static BiConsumer<BackendCodegen, Instruction> jType(int opcode) {
		return (be, i) -> be.emit(J(opcode, literal(i, 0)));
	}

	// i-type
	static BiConsumer<BackendCodegen, Instruction> iType(int opcode) {
		return (be, i) -> be.emit(I(opcode, reg(i, 0), literal(i, 1)));
	}

	// u-type
	static BiConsumer<BackendCodegen, Instruction> uType(int opcode) {
		return (be, i) -> be.emit(U(opcode, reg(i, 0), literal(i, 1)));
	}

	// m-type
	static BiConsumer<BackendCodegen, Instruction> mType(int opcode) {
		return (be, i) -> {
			RegisterOperand rop;
			MemoryOperand mop;
			// order-agnostic operands
			if (i.operands[0] instanceof RegisterOperand r && i.operands[1] instanceof MemoryOperand m) {
				rop = r;
				mop = m;
			} else if (i.operands[1] instanceof RegisterOperand r && i.operands[0] instanceof MemoryOperand m) {
				rop = r;
				mop = m;
			} else {
				throw new IllegalArgumentException("This is not supposed to happen! FRONTEND_FAULT. IR: " + i);
			}
			be.emit(M(
				opcode, rop.register(), // rDest/Src
				mop.base().register(), // rBase
				((ImmLiteral) mop.offset()).value() // [rBase + offset]
			));
		};
	}

	static int literal(Instruction i, int index) {
		if (index >= i.operands.length) {
			throw new RuntimeException("This is not supposed to happen! FRONTEND_FAULT");
		}
		return ((ImmLiteral) i.operands[index]).value();
	}

	static int reg(Instruction i, int index) {
		if (index >= i.operands.length) {
			return 0;
		}
		return ((RegisterOperand) i.operands[index]).register();
	}
	
	static int HI(int i) {
		return i >>> 16;
	}
	
	static int LO(int i) {
		return i & 0xFFFF;
	}
}
