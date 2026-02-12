package dev.gkvn.cpu.assembler.fl32r.backend;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.swing.JFrame;

import dev.gkvn.cpu.Calc;
import dev.gkvn.cpu.assembler.fl32r.backend.isa.CodegenTable;
import dev.gkvn.cpu.assembler.fl32r.frontend.FrontendCAIR;
import dev.gkvn.cpu.demo.EmulatedDisplay;
import dev.gkvn.cpu.fl32r.FL32REmulator;

public class BackendCodegen {
	private FrontendCAIR cair;
	private ByteArrayOutputStream code;
	
	public BackendCodegen(FrontendCAIR cair) {
		this.cair = cair;
		this.code = new ByteArrayOutputStream();
	}
	
	public void emit(int value) {
		String binary = String.format("%32s", Integer.toBinaryString(value)).replace(' ', '0');
		StringBuilder sb = new StringBuilder(binary);
		sb.insert(8, '|');
		System.out.println("emitted " + sb);
		code.write((value >>> 24) & 0xFF);
		code.write((value >>> 16) & 0xFF);
		code.write((value >>> 8) & 0xFF);
		code.write(value & 0xFF);
	}
	
	private static final int WIDTH = 640;
	private static final int HEIGHT = 480;
	public void gen() throws IOException {
		FL32REmulator emu = new FL32REmulator(Calc.GB(0.5));
		emu.setFrequencyHz(50_000_000); // 1MHZ cpu
		this.cair.instructions().forEach(i -> {
			var a = CodegenTable.getRuleFor(i.opcode);
			if (a == null) {
				System.out.print(i + " -> ");
				System.out.println("unimplemented");
				return;
			}
			System.out.print(i + " -> ");
			try {
				a.accept(this, i);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		code.write(cair.dataSection().dataBytes());
		Files.write(Path.of("test.bin"), code.toByteArray());
		
		byte[] boot = code.toByteArray();
//		for (int i = 0; i < text.length; i++) {
//			boot[i * 4 + 0] = (byte) ((text[i] >> 24) & 0xFF);
//			boot[i * 4 + 1] = (byte) ((text[i] >> 16) & 0xFF);
//			boot[i * 4 + 2] = (byte) ((text[i] >> 8) & 0xFF);
//			boot[i * 4 + 3] = (byte) (text[i] & 0xFF);
//			System.out.printf("%08X | %s\n", text[i], String.format("%32s", Integer.toBinaryString(text[i])).replace(' ', '0'));
//		}
		emu.loadBootProgram(boot);
		EmulatedDisplay screen = new EmulatedDisplay(emu);
		
		JFrame frame = new JFrame("CPU Display");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(WIDTH, HEIGHT);
		frame.add(screen);
		frame.setVisible(true);
		
		new Thread(screen).start();
		emu.start(false);
		int reg[] = emu.dumpRegisters();
		for (int i = 0; i < reg.length; i++) {
			System.out.println("R" + i + ": 0x" + Integer.toHexString(reg[i]) + " | " + Integer.toUnsignedLong(reg[i]));
		}
		Files.write(Path.of("dump.bin"), emu.dumpMemory().inner.chunks[0]);
	}
}
