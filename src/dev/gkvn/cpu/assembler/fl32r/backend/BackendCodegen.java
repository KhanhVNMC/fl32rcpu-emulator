package dev.gkvn.cpu.assembler.fl32r.backend;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import dev.gkvn.cpu.assembler.fl32r.frontend.FrontendCAIR;
import dev.gkvn.cpu.assembler.fl32r.frontend.arch.FEOpCode;
import static dev.gkvn.cpu.assembler.fl32r.frontend.arch.FEOpCode.*;
import dev.gkvn.cpu.assembler.fl32r.frontend.core.Instruction;
import dev.gkvn.cpu.assembler.fl32r.frontend.operands.ImmLiteral;
import dev.gkvn.cpu.assembler.fl32r.frontend.operands.MemoryOperand;
import dev.gkvn.cpu.assembler.fl32r.frontend.operands.Operand;
import dev.gkvn.cpu.assembler.fl32r.frontend.operands.RegisterOperand;
import dev.gkvn.cpu.fl32r.FL32RConstants;

import static dev.gkvn.cpu.fl32r.FL32RHelper.*;

public class BackendCodegen {
	private FrontendCAIR cair;
	private ByteArrayOutputStream code;
	
	public BackendCodegen(FrontendCAIR cair) {
		this.cair = cair;
	}
	
	static final Map<FEOpCode, BiConsumer<BackendCodegen, Instruction>> emitRuleset = new HashMap<>();
	// define
	static {
		// real hardware instructions
		addRule(MOV,   rType(FL32RConstants.MOV));
		addRule(LUI,   uType(FL32RConstants.LUI));
		addRule(LLI,   uType(FL32RConstants.LLI));
		addRule(LDW,   mType(FL32RConstants.LDW));
		addRule(LDB,   mType(FL32RConstants.LDW));
		addRule(STW,   mType(FL32RConstants.STW));
		addRule(STB,   mType(FL32RConstants.STB));
		
		addRule(ADD,   rType(FL32RConstants.ADD));
		addRule(SUB,   rType(FL32RConstants.SUB));
		addRule(MUL,   rType(FL32RConstants.MUL));
		addRule(UMUL,  rType(FL32RConstants.UMUL));
		addRule(DIV,   rType(FL32RConstants.DIV));
		addRule(UDIV,  rType(FL32RConstants.UDIV));
		addRule(MOD,   rType(FL32RConstants.MOD));
		addRule(UMOD,  rType(FL32RConstants.UMOD));
		addRule(AND,   rType(FL32RConstants.AND));
		addRule(OR,    rType(FL32RConstants.OR));
		addRule(XOR,   rType(FL32RConstants.XOR));
		addRule(SHR,   rType(FL32RConstants.SHR));
		addRule(SRA,   rType(FL32RConstants.SRA));
		addRule(NOT,   rType(FL32RConstants.NOT));
		
		addRule(ADDI,  iType(FL32RConstants.ADDI));
		addRule(ORI,   iType(FL32RConstants.ORI));
		addRule(XORI,  iType(FL32RConstants.XORI));
		addRule(ANDI,  iType(FL32RConstants.ANDI));
		addRule(SHLI,  iType(FL32RConstants.SHLI));
		addRule(SRAI,  iType(FL32RConstants.SRAI));
		addRule(SHRI,  iType(FL32RConstants.SHRI));
		
		addRule(PUSH,  rType(FL32RConstants.PUSH));
		addRule(POP,   rType(FL32RConstants.POP));
		
		addRule(JMP,   jType(FL32RConstants.JMP));
		addRule(JR,    rType(FL32RConstants.JR));
		addRule(CMP,   rType(FL32RConstants.CMP));
		addRule(JEQ,   jType(FL32RConstants.JEQ));
		addRule(JNE,   jType(FL32RConstants.JNE));
		addRule(JLT,   jType(FL32RConstants.JLT));
		addRule(JGT,   jType(FL32RConstants.JGT));
		addRule(JLE,   jType(FL32RConstants.JLE));
		addRule(JGE,   jType(FL32RConstants.JGE));
		addRule(JOF,   jType(FL32RConstants.JOF));
		addRule(JNO,   jType(FL32RConstants.JNO));
		addRule(CALL,  jType(FL32RConstants.CALL));
		addRule(CLR,   rType(FL32RConstants.CLR));
		addRule(RET,   noType(FL32RConstants.RET));
		
		addRule(NOP,   noType(FL32RConstants.NOP));
		addRule(KILL,  noType(FL32RConstants.KILL));
		
		// pseudo-ops
		addRule(LD,   (be, i) -> {
			
		});
		addRule(LDI,  (be, i) -> {
			int reg = reg(i, 0), value = literal(i, 1);
			int low = LO(value), high = HI(value);
			// micro-optimization
			if (high == 0) {
				// MOV rDest, rZero ; MUCH cheaper than LUI
				be.emit(R(FL32RConstants.MOV, reg, FL32RConstants.REG_ZERO, 0));
			} else {
				be.emit(U(FL32RConstants.LUI, reg, high));
			}
			be.emit(I(FL32RConstants.ORI, reg, low));
		});
	}
	
	static void addRule(FEOpCode op, BiConsumer<BackendCodegen, Instruction> rule) {
		emitRuleset.put(op, rule);
	}
	
	// no-type
	static BiConsumer<BackendCodegen, Instruction> noType(int opcode) {
		return (be, i) -> be.emit(
			NO(opcode)
		);
	}
	
	// r-type
	static BiConsumer<BackendCodegen, Instruction> rType(int opcode) {
		return (be, i) -> be.emit(
			R(opcode, reg(i, 0), reg(i, 1), reg(i, 2))
		);
	}
	
	// j-type
	static BiConsumer<BackendCodegen, Instruction> jType(int opcode) {
		return (be, i) -> be.emit(
			J(opcode, literal(i, 0))
		);
	}
	
	// i-type
	static BiConsumer<BackendCodegen, Instruction> iType(int opcode) {
		return (be, i) -> be.emit(
			I(opcode, reg(i, 0), literal(i, 1))
		);
	}
	
	// u-type
	static BiConsumer<BackendCodegen, Instruction> uType(int opcode) {
		return (be, i) -> be.emit(
			U(opcode, reg(i, 0), literal(i, 1))
		);
	}
	
	// m-type
	static BiConsumer<BackendCodegen, Instruction> mType(int opcode) {
		return (be, i) -> {
			RegisterOperand rop;
			MemoryOperand mop;
			// order-agnostic operands
			if (i.operands[0] instanceof RegisterOperand r 
			 && i.operands[1] instanceof MemoryOperand m) {
				rop = r;
				mop = m;
			} else if (i.operands[1] instanceof RegisterOperand r 
					&& i.operands[0] instanceof MemoryOperand m) {
				rop = r;
				mop = m;
			} else {
				throw new IllegalArgumentException("This is not supposed to happen! FRONTEND_FAULT. IR: " + i);
			}
			be.emit(
				M(opcode, 
					rop.register(), // rDest/Src
					mop.base().register(), // rBase
					((ImmLiteral) mop.offset()).value() // [rBase + offset]
				)
			);
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

	
	public void emit(int value) {
		String binary = String.format("%32s", Integer.toBinaryString(value)).replace(' ', '0');
		StringBuilder sb = new StringBuilder(binary);
		sb.insert(8, '|');
		System.out.println("emitted " + sb);
	}
	
	static int HI(int i) {
		return i >>> 16;
	}
	
	static int LO(int i) {
		return i & 0xFFFF;
	}
	
	public void gen() {
		this.cair.instructions().forEach(i -> {
			var a = emitRuleset.get(i.opcode);
			if (a == null) {
				System.out.print(i + " -> ");
				System.out.println("unimplemented");
				return;
			}
			System.out.print(i + " -> ");
			a.accept(this, i);
		});
	}
}
