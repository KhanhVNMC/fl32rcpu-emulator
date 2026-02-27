package dev.gkvn.cpu.fl32r.mmio.devs;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import dev.gkvn.cpu.ByteMemorySpace;
import dev.gkvn.cpu.fl32r.FL32RConstants;
import dev.gkvn.cpu.fl32r.Utils;
import dev.gkvn.cpu.fl32r.mmio.AbstractMMIODevice;
import dev.gkvn.cpu.fl32r.mmio.FL32RMMIO;

public final class VGAGraphicsMMIO extends AbstractMMIODevice {	
	public static final int WIDTH = 640, HEIGHT = 480;
	public static byte FONT_GLYPHS[] = null;
	
	// burh
	static {
		try {
			FONT_GLYPHS = Files.readAllBytes(Path.of("binaries/PGC.F16"));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void main(String[] args) {
		var a = new VGAGraphicsMMIO(null, RGB_MODE_FB1_BASE);
		String eliteBall = "tbridjasidjsaiajdi";
		
		int row = 0;
		int col = 0;
		for (char c : eliteBall.toCharArray()) {
			if (c == '\n') {
				col = 0;
				row++;
				continue;
			}
			a.vram.set32(
				TEXT_MODE_BASE + ((row * 80 + col) * 4), 
				(((0 << 25) | (0 << 24) | ((15 % 16) << 8) | (c % 256)))
			);
			col++;
			if (col == 80) {
				row++;
				col = 0;
			}
		}
//		for (int i = 0;; i++) {
//			if (i >= 80 * 30) {
//				i = 0;
//			}
//			a.vram.set32(
//				TEXT_MODE_BASE + (i * 4), 
//				(((1 << 25) | (0 << 24) | ((i % 16) << 8) | (i % 256)))
//			);
//			try {
//				Thread.sleep(10);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
	}
	
	// VGA info
	public static final int VBLANK_IRQ     = 0x02;
	// 80 (horizontal) x 30 (vertical) for a total of 240 chars, 4 bytes each
	public static final int TEXT_MODE_SIZE = 80 * 30 * 4;
	public static final int RGB32_MODE_SIZE  = WIDTH * HEIGHT * 4;
	
	// register offsets
	public static final int 
		VIDEO_CONTROL = 0x00,
		VIDEO_MODE = 0x04,
		VIDEO_STATUS = 0x08,
		FRAMEBUFF_WIDTH = 0x0C,
		FRAMEBUFF_HEIGHT = 0x10,
		FRAMEBUFFER_INDEX = 0x14,
		REG_END = 0x14 + 4,
		VRAM_BASE = 0xF0,
		
		// vram offsets
		RGB_MODE_FB1_BASE = 0x00,
		RGB_MODE_FB2_BASE = RGB_MODE_FB1_BASE + RGB32_MODE_SIZE,
		TEXT_MODE_BASE = RGB_MODE_FB2_BASE + RGB32_MODE_SIZE
	;
	
    public static final int MODE_TEXT = 0;
    public static final int MODE_RGB_32BPP = 1;
    public static final int MODE_RGB_08BPP = 2;
	
	// "vram" lmfao
	static  final int VRAM_SIZE = TEXT_MODE_SIZE + RGB32_MODE_SIZE * 2;
	private final ByteMemorySpace vram = new ByteMemorySpace(VRAM_SIZE);
	private final int[] palette = new int[256];
	
	// registers
	private int videoControl;
	private int videoMode;
	private int framebufferIndex;

	// emulator
	private final BufferedImage image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
	private volatile boolean running = true;
	
	private boolean blinkOn = true;
	
	public VGAGraphicsMMIO(FL32RMMIO mmio, int base) {
		super(mmio, base, 24 + VRAM_SIZE);
		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
			blinkOn = !blinkOn;
		}, 500, 500, TimeUnit.MILLISECONDS);
		// default
		System.arraycopy(FL32RConstants.VGA_STD_PALETTE, 0, palette, 0, 256);
		
		JFrame frame = new JFrame("FL32R VGA Display (640x480)");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel panel = new JPanel() {
			private static final long serialVersionUID = 1L;
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				//if (!isVideoEnabled()) return;
				g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
			}
		};

		panel.setPreferredSize(new Dimension(640, 480));
		frame.add(panel);
		frame.pack();
		frame.setResizable(true);
		frame.setVisible(true);

		Thread renderThread = new Thread(() -> {
			while (running) {
				renderTextMode();
				SwingUtilities.invokeLater(panel::repaint);
				try {
					Thread.sleep(16); // ~60 Hz
				} catch (InterruptedException ignored) {}
			}
		}, "nigga");

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
				case FRAMEBUFF_WIDTH -> WIDTH;
				case FRAMEBUFF_HEIGHT -> HEIGHT;
				case FRAMEBUFFER_INDEX -> framebufferIndex;
				default -> 0;
			};
		}
		int vramOffset = off - VRAM_BASE;
		return Utils.beBytesToInt(
			vram.get(vramOffset), vram.get(vramOffset + 1), 
			vram.get(vramOffset + 2), vram.get(vramOffset + 3)
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
		vram.set32(off - VRAM_BASE, value);
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
	
	static final int GLYPH_WIDTH = 8, GLYPH_HEIGHT = 16;
	private void renderTextMode() {
		int p = TEXT_MODE_BASE;
		
		for (int y = 0; y < 30; y++) {
			for (int x = 0; x < 80; x++) {
				// fetch char
				int screenChar = Utils.beBytesToInt(
					vram.get(p), 
					vram.get(p + 1), 
					vram.get(p + 2), 
					vram.get(p + 3)
				);
				p += 4; // word-by-word
				
				// custom font format (NVGA - Nibba VGA)
				// [31..26 reserved][25 blink][24 underline][23..16 background][15...8 foreground][7..0 codepoint]
				int codePoint  = screenChar & 0xFF; // 8 bit
				int foreground = (screenChar >>> 8) & 0xFF; // 8 bit 
				int background = (screenChar >>> 16) & 0xFF; // 8 bit 
				boolean underline = ((screenChar >>> 24) & 1) == 1; // 1 bit
				boolean blink = ((screenChar >>> 25) & 1) == 1; // 1 bit
				
				// draw
				int glyphIdx = codePoint * GLYPH_HEIGHT;
				boolean drawGlyph = !(blink && !blinkOn);

				for (int gy = 0; gy < GLYPH_HEIGHT; gy++) {
					boolean underlineRow = underline && gy == 15;
					byte horz = FONT_GLYPHS[glyphIdx + gy];
					for (int gx = 0; gx < GLYPH_WIDTH; gx++) {
						boolean fore = ((horz >>> (7 - gx)) & 1) != 0;
				        int color;
				        if (underlineRow || (fore && drawGlyph)) {
				            color = palette[foreground];
				        } else {
				            color = palette[background];
				        }
				        
						image.setRGB(
							x * GLYPH_WIDTH + gx, 
							y * GLYPH_HEIGHT + gy, 
							color
						);
					}
				}
			}
		}		
		
	}
	
}