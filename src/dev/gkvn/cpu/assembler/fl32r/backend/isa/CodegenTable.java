package dev.gkvn.cpu.assembler.fl32r.backend.isa;

import static dev.gkvn.cpu.assembler.fl32r.frontend.arch.FEOpCode.*;
import static dev.gkvn.cpu.assembler.fl32r.backend.isa.PseudoInstructions.*;
import static dev.gkvn.cpu.assembler.fl32r.backend.isa.CodegenUtils.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import dev.gkvn.cpu.assembler.fl32r.backend.BackendCodegen;
import dev.gkvn.cpu.assembler.fl32r.frontend.arch.FEOpCode;
import dev.gkvn.cpu.assembler.fl32r.frontend.core.Instruction;
import dev.gkvn.cpu.fl32r.FL32RConstants;

public class CodegenTable {
	private static final Map<FEOpCode, BiConsumer<BackendCodegen, Instruction>> emitRuleset = new HashMap<>();
	
	// define the table
	static {
		// real hardware instructions
		entry(MOV,   rType(FL32RConstants.MOV));
		entry(LUI,   uType(FL32RConstants.LUI));
		entry(LLI,   uType(FL32RConstants.LLI));
		entry(LDW,   mType(FL32RConstants.LDW));
		entry(LDB,   mType(FL32RConstants.LDB));
		entry(STW,   mType(FL32RConstants.STW));
		entry(STB,   mType(FL32RConstants.STB));
		
		entry(ADD,   rType(FL32RConstants.ADD));
		entry(SUB,   rType(FL32RConstants.SUB));
		entry(MUL,   rType(FL32RConstants.MUL));
		entry(UMUL,  rType(FL32RConstants.UMUL));
		entry(DIV,   rType(FL32RConstants.DIV));
		entry(UDIV,  rType(FL32RConstants.UDIV));
		entry(MOD,   rType(FL32RConstants.MOD));
		entry(UMOD,  rType(FL32RConstants.UMOD));
		entry(AND,   rType(FL32RConstants.AND));
		entry(OR,    rType(FL32RConstants.OR));
		entry(XOR,   rType(FL32RConstants.XOR));
		entry(SHR,   rType(FL32RConstants.SHR));
		entry(SRA,   rType(FL32RConstants.SRA));
		entry(NOT,   rType(FL32RConstants.NOT));
		
		entry(ADDI,  iType(FL32RConstants.ADDI));
		entry(ORI,   iType(FL32RConstants.ORI));
		entry(XORI,  iType(FL32RConstants.XORI));
		entry(ANDI,  iType(FL32RConstants.ANDI));
		entry(SHLI,  iType(FL32RConstants.SHLI));
		entry(SRAI,  iType(FL32RConstants.SRAI));
		entry(SHRI,  iType(FL32RConstants.SHRI));
		
		entry(PUSH,  rType(FL32RConstants.PUSH));
		entry(POP,   rType(FL32RConstants.POP));
		
		entry(JMP,   jType(FL32RConstants.JMP));
		entry(JR,    rType(FL32RConstants.JR));
		entry(CMP,   rType(FL32RConstants.CMP));
		entry(JEQ,   jType(FL32RConstants.JEQ));
		entry(JNE,   jType(FL32RConstants.JNE));
		entry(JLT,   jType(FL32RConstants.JLT));
		entry(JGT,   jType(FL32RConstants.JGT));
		entry(JLE,   jType(FL32RConstants.JLE));
		entry(JGE,   jType(FL32RConstants.JGE));
		entry(JOF,   jType(FL32RConstants.JOF));
		entry(JNO,   jType(FL32RConstants.JNO));
		entry(CALL,  jType(FL32RConstants.CALL));
		entry(CLR,   rType(FL32RConstants.CLR));
		entry(RET,   noType(FL32RConstants.RET));
		
		entry(NOP,   noType(FL32RConstants.NOP));
		entry(KILL,  noType(FL32RConstants.KILL));
		
		// pseudo-ops (Expand into one or more actual instructions)
		entry(LD,    loadVariable());
		entry(ST,    storeVariable());
		entry(LDI,   loadImmediate());
	}
	
	private static void entry(FEOpCode op, BiConsumer<BackendCodegen, Instruction> rule) {
		emitRuleset.put(op, rule);
	}
	
	public static BiConsumer<BackendCodegen, Instruction> getRuleFor(FEOpCode op) {
		return emitRuleset.get(op);
	}
}
