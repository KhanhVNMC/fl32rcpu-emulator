package dev.gkvn.cpu.fl32r.assembler.backend;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;

import dev.gkvn.cpu.fl32r.assembler.backend.isa.CodegenTable;
import dev.gkvn.cpu.fl32r.assembler.frontend.FrontendCAIR;
import dev.gkvn.cpu.fl32r.assembler.frontend.core.Instruction;

public class BackendCodegen {
	private FrontendCAIR cair;
	private ByteArrayOutputStream code;
	
	public BackendCodegen(FrontendCAIR cair) {
		this.cair = cair;
		this.code = new ByteArrayOutputStream();
	}
	
	public void emit(int value) {
		code.write((value >>> 24) & 0xFF);
		code.write((value >>> 16) & 0xFF);
		code.write((value >>> 8) & 0xFF);
		code.write(value & 0xFF);
	}
	
	public byte[] generate() throws Exception {
		for (Instruction instr : this.cair.instructions()) {
			BiConsumer<BackendCodegen, Instruction> rule = CodegenTable.getRuleFor(instr.opcode);
			if (rule == null) {
				throw new RuntimeException("Codegen Error: Backend is not up-to-spec!");
			}
			rule.accept(this, instr);
		}
		code.write(cair.dataSection().dataBytes());
		return code.toByteArray();
	}
}
