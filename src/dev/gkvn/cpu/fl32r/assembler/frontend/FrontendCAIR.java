package dev.gkvn.cpu.fl32r.assembler.frontend;

import java.util.Arrays;
import java.util.List;
import dev.gkvn.cpu.fl32r.assembler.frontend.core.Instruction;

public record FrontendCAIR(List<Instruction> instructions, IRDataSection dataSection) {
	public int estimateCodegenSize() {
		int total = dataSection.dataBytes().length;
		for (Instruction i : instructions) {
			total += i.getSize();
		}
		return total;
	}
	
	@Override
	public final String toString() {
		return "FL32RAsmCAIR{instructions=" + instructions + ", dataBytes=" + Arrays.toString(dataSection.dataBytes()) + "}";
	}
}
