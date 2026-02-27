package dev.gkvn.cpu.fl32r.mmio.devs;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import dev.gkvn.cpu.ByteMemorySpace;
import dev.gkvn.cpu.fl32r.Utils;
import dev.gkvn.cpu.fl32r.mmio.AbstractMMIODevice;
import dev.gkvn.cpu.fl32r.mmio.FL32RMMIO;

public final class VGAGraphicsMMIO extends AbstractMMIODevice {	
	public static final int WIDTH = 640, HEIGHT = 480;
	// VGA info
	public static final int VBLANK_IRQ     = 0x02;
	// 80 (horizontal) x 30 (vertical) for a total of 240 chars, 4 bytes each
	public static final int TEXT_MODE_SIZE = 80 * 30 * 4;
	public static final int RGB_MODE_SIZE  = WIDTH * HEIGHT * 4;
	
	// register offsets
	public static final int 
		VIDEO_CONTROL = 0x00,
		VIDEO_MODE = 0x04,
		VIDEO_STATUS = 0x08,
		FRAMEBUFF_WIDTH = 0x0C,
		FRAMEBUFF_HEIGHT = 0x10,
		FRAMEBUFFER_INDEX = 0x14,
		REG_END = 0x14 + 4,
		
		// vram offsets
		RGB_MODE_FB1_BASE = 0xF0,
		RGB_MODE_FB2_BASE = RGB_MODE_FB1_BASE + RGB_MODE_SIZE,
		TEXT_MODE_BASE = RGB_MODE_FB2_BASE + RGB_MODE_SIZE
	;
	
    public static final int MODE_TEXT = 0;
    public static final int MODE_RGB_32BPP = 1;
    public static final int MODE_RGB_08BPP = 2;
	
	// "vram" lmfao
	static  final int VRAM_SIZE = TEXT_MODE_SIZE + RGB_MODE_SIZE * 2;
	private final ByteMemorySpace vram = new ByteMemorySpace(VRAM_SIZE);
	private final int[] palette = new int[256];
	
	// registers
	private int videoControl;
	private int videoMode;
	private int framebufferIndex;

	// emulator
	private final BufferedImage image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
	private volatile boolean running = true;
	
	public VGAGraphicsMMIO(FL32RMMIO mmio, int base) {
		super(mmio, base, 24 + VRAM_SIZE);
		
		JFrame frame = new JFrame("FL32R VGA");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel panel = new JPanel() {
			private static final long serialVersionUID = 1L;
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				if (!isVideoEnabled()) return;
				g.drawImage(image, 0, 0, null);
			}
		};

		panel.setPreferredSize(new Dimension(640, 480));
		frame.add(panel);
		frame.pack();
		frame.setResizable(false);
		frame.setVisible(true);

		Thread renderThread = new Thread(() -> {
			while (running) {
				//renderRGB();
				SwingUtilities.invokeLater(panel::repaint);
				try {
					Thread.sleep(16); // ~60 Hz
				} catch (InterruptedException ignored) {}
			}
		}, "VGA-Graphics");

		renderThread.setDaemon(true);
		renderThread.start();
	}

	@Override
	public int readWord(int address) {
		int off = offset(address);
		if (off < REG_END) {
			return switch (off) {
				case VIDEO_CONTROL -> videoControl;
				case VIDEO_MODE -> videoMode;
				case VIDEO_STATUS -> 0;
				case FRAMEBUFF_WIDTH -> 640;
				case FRAMEBUFF_HEIGHT -> 480;
				case FRAMEBUFFER_INDEX -> framebufferIndex;
				default -> 0;
			};
		}
		return Utils.beBytesToInt(
			vram.get(off), vram.get(off + 1), 
			vram.get(off + 2), vram.get(off + 3)
		);
	}

	@Override
	public void writeWord(int address, int value) {
		int off = offset(address);
		if (off < REG_END) {
			switch (off) {
				case VIDEO_CONTROL -> videoControl = value;
				case VIDEO_MODE -> videoMode = value;
				case FRAMEBUFFER_INDEX -> framebufferIndex = value & 1;
			}
			return;
		}
		vram.set32(off, value);
	}
	
	private boolean isVideoEnabled() {
		return (videoControl & 1) == 1;
	}
	
	private int getVRAMAddressBase() {
		return (framebufferIndex == 0) 
			? RGB_MODE_FB1_BASE 
			: RGB_MODE_FB2_BASE
		;
	}

	private void render32bppRGB() {
		int p = getVRAMAddressBase();
		for (int y = 0; y < 480; y++) {
			for (int x = 0; x < 640; x++) {
				int r = vram.get(p + 1) & 0xFF;
				int g = vram.get(p + 2) & 0xFF;
				int b = vram.get(p + 3) & 0xFF;
				image.setRGB(x, y, (r << 16) | (g << 8) | b);
				p += 4;
			}
		}
	}
	
	private void render8bppRGB() {
		int p = getVRAMAddressBase();
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				int idx = vram.get(p++) & 0xFF;
				image.setRGB(x, y, palette[idx]);
			}
		}
	}
	
	private void renderTextMode() {
		
	}
	
	
}