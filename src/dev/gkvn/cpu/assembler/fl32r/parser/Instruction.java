package dev.gkvn.cpu.assembler.fl32r.parser;

import java.util.HashSet;
import java.util.Set;

public class Instruction {
	public final FrontendOp opcode;
	public Operand[] operands;
	private Set<Object> metadata = new HashSet<>(); 

	public Instruction(FrontendOp opcode, Operand... operands) {
		this.opcode = opcode;
		this.operands = operands;
	}
	
	public int getSize() {
		return opcode.getSize();
	}
	
	public void attachMetadata(Object meta) {
		this.metadata.add(meta);
	}
	
	public boolean hasMetadata(Object meta) {
		return this.metadata.contains(meta);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(opcode);
		if (metadata.size() > 0) {
			sb.append("{meta=").append(metadata).append("}");
		}
		if (operands != null && operands.length > 0) {
			sb.append(' ');
			for (int i = 0; i < operands.length; i++) {
				if (i > 0) sb.append(", ");
				sb.append(operands[i]);
			}
		}
		return sb.toString();
	}
}
