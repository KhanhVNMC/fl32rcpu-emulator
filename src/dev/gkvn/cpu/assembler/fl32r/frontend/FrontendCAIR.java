package dev.gkvn.cpu.assembler.fl32r.frontend;

import java.util.Arrays;
import java.util.List;
import dev.gkvn.cpu.assembler.fl32r.frontend.core.Instruction;

public record FrontendCAIR(List<Instruction> instructions, byte[] dataSection) {
	public int estimateCodegenSize() {
		int total = dataSection.length;
		for (Instruction i : instructions) {
			total += i.getSize();
		}
		return total;
	}
	
	@Override
	public final String toString() {
		return "FL32RAsmCAIR{instructions=" + instructions + ", dataBytes=" + Arrays.toString(dataSection) + "}";
	}
}
