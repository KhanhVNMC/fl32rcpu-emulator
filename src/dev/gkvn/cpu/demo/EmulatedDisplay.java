package dev.gkvn.cpu.demo;

import javax.swing.*;

import dev.gkvn.cpu.Calc;
import dev.gkvn.cpu.GenericCPUEmulator;
import dev.gkvn.cpu.ReadOnlyByteMemory;
import dev.gkvn.cpu.fl32r.FL32REmulator;
import dev.gkvn.cpu.fl32r.Utils;

import static dev.gkvn.cpu.fl32r.FL32RConstants.*;
import static dev.gkvn.cpu.fl32r.FL32RHelper.*;

import java.awt.*;
import java.awt.image.BufferedImage;

public class EmulatedDisplay extends JPanel implements Runnable {
	private static final long serialVersionUID = 1L;
	private static final int WIDTH = 640;
	private static final int HEIGHT = 480;

	private final GenericCPUEmulator emu;
	private final BufferedImage javaBuffer;
	private int currentBuffer = 0;
	
	// memory offset and size for VRAM
	private final int vramOffset;

	public EmulatedDisplay(GenericCPUEmulator emu) {
		this.emu = emu;
		long ramSize = this.emu.dumpMemory().length();
		long vramSize = ((WIDTH * HEIGHT * 4 * 2 /* 2 buffers, 24 bit color */) + 1);
		if (ramSize < vramSize) {
			throw new RuntimeException("cannot do this, need at least: " + vramSize + " bytes of ram");
		}
		this.vramOffset = (int) (ramSize - vramSize); // leave 1 byte at the top of VRAM for buffer idnexing
		((FL32REmulator) emu).randomize(vramOffset + 1);
		System.out.println(vramOffset);
		javaBuffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		// draw current buffer
		g.drawImage(javaBuffer, 0, 0, null);
	}
	
	private void updateBuffer() {
		ReadOnlyByteMemory memory = emu.dumpMemory();
		this.currentBuffer = 0;
		int pixelIndex = 0;
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				long addr = vramOffset + 1 + (currentBuffer == 0 ? 0 : WIDTH * HEIGHT * 4) + (pixelIndex * 4); // 3 bytes per pixel (RGB)
				if (addr + 3 >= memory.length()) {
					break;
				}
				int a = Byte.toUnsignedInt(memory.get(addr));
				int r = Byte.toUnsignedInt(memory.get(addr + 1));
				int g = Byte.toUnsignedInt(memory.get(addr + 2));
				int b = Byte.toUnsignedInt(memory.get(addr + 3));
				int rgb = (r << 16) | (g << 8) | b;
				javaBuffer.setRGB(x, y, rgb);
				pixelIndex++;
			}
		}
	}

	@Override
	public void run() {
		while (true) {
			updateBuffer();
			repaint();
			try {
				Thread.sleep(0); // ~60 FPS
			} catch (InterruptedException e) {
				break;
			}
		}
	}
	
	public static void main(String[] args) {
		FL32REmulator emu = new FL32REmulator(Calc.GB(0.5));
		emu.setFrequencyHz(1_000_000); // 1MHZ cpu
		
		int[] text = new int[] {
			U(LUI, 1, 534413312 >>> 16),
			I(ORI, 1, 534413312 & 0xFFFF),
//			U(LUI, 0, 0xFFFF),
//			I(ORI, 0, 0xFFFF & 0xFFFF),
			I(ADDI, 0, 0xff),
			M(STW, 0, 1, 0),
			I(ADDI, 1, 4),
			J(JMP, Utils.convertIntToU24(-16)),
			// LUI   R5, 0xABCD ; 16-bit upper bits
			U(LUI, 1, 0xFF),
			// MOV   R1, ZERO
			R(MOV, 1, REG_ZERO, 0),
			// ORI   R1, 5
			I(ORI, 1, 5),
			// ADDI  R1, 10
			I(ADDI, 1, 10),
			// ADDI  R1, 20
			I(ADDI, 1, 20),
			// MOV   R0, R1
			R(MOV, 0, 1, 0),
			// ADD   R2, R0, R1
			R(ADD, 2, 0, 1),
			// PUSH  R2
			R(PUSH, 2, 0, 0),
			// POP   R3
			R(POP, 3, 0, 0),
			// PUSH  R3
			R(PUSH, 3, 0, 0),
			// JMP   +4
			J(JMP, +4),
			// KILL
			J(KILL, 0),
			// MOV  R8, RPC
			R(MOV, 8, REG_PROGRAM_COUNTER, 0),
			// KILL
			J(KILL, 0), 
		};
		
		byte[] boot = new byte[text.length * 4];
		for (int i = 0; i < text.length; i++) {
			boot[i * 4 + 0] = (byte) ((text[i] >> 24) & 0xFF);
			boot[i * 4 + 1] = (byte) ((text[i] >> 16) & 0xFF);
			boot[i * 4 + 2] = (byte) ((text[i] >> 8) & 0xFF);
			boot[i * 4 + 3] = (byte) (text[i] & 0xFF);
		}
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
	}
}
