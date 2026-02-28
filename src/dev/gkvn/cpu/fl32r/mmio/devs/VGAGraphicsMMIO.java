package dev.gkvn.cpu.fl32r.mmio.devs;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
			FONT_GLYPHS = Files.readAllBytes(Path.of("blobs/VGA8.F16"));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	// VGA info
	public static final int VBLANK_IRQ     = 0x02;
	// 80 (horizontal) x 30 (vertical) for a total of 240 chars, 4 bytes each
	public static final int TEXT_MODE_SIZE = 80 * 30 * 4;
	public static final int RGB32_MODE_SIZE  = WIDTH * HEIGHT * 4;
	
	public static final int 
		VIDEO_CTRL_ENABLE = 1 << 0,
		VIDEO_CTRL_IRQ_VBLANK = 1 << 1,
	
		CURSOR_CTRL_ON = 1 << 0,
		CURSOR_CTRL_BLINK = 1 << 1,
		CURSOR_CTRL_BLOCK = 1 << 2
	;
	
	// register offsets
	public static final int 
		VIDEO_CONTROL = 0x00,
		VIDEO_MODE = 0x04,
		VIDEO_STATUS = 0x08,
		FRAMEBUFF_WIDTH = 0x0C,
		FRAMEBUFF_HEIGHT = 0x10,
		FRAMEBUFFER_INDEX = 0x14,
		
		CURSOR_X = 0x18, 
		CURSOR_Y = 0x1C, 
		CURSOR_CONTROL = 0x20,
		
		PALETTE_INDEX = 0x24,
		PALETTE_DATA = 0x28,
		
		// end
		REG_END = 0x28 + 4
	;
	
	public static final int 
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
	
	// palette
	private int paletteIndex = 0;
	private final int[] palette = new int[256];
	
	// registers
	private boolean videoEnable;
	private boolean vblankIrqEnable;
	private int videoMode;
	private int framebufferIndex;
	
	// text mode cursors
	private int cursorX = 0; // 0..79
	private int cursorY = 0; // 0..29
	private boolean cursorOn;
	private boolean cursorBlink;
	private boolean blockCursorMode;

	// emulated VGA display states (CRT-ish)
	private final BufferedImage image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
	private volatile boolean running = true;
	private volatile boolean vblank = false;
	private volatile boolean blinkOn = true;
	
	public VGAGraphicsMMIO(FL32RMMIO mmio, int base) {
		super(mmio, base, 44 + VRAM_SIZE);	
		// default palette
		System.arraycopy(FL32RConstants.VGA_STD_PALETTE, 0, palette, 0, 256);
		
		// swing boilerplate
		JFrame frame = new JFrame("FL32R VGA Display (640x480)");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel panel = new JPanel() {
			private static final long serialVersionUID = 1L;
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				if (!videoEnable) return;
				g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
			}
		};
		panel.setPreferredSize(new Dimension(640, 480));
		frame.add(panel);
		frame.pack();
		frame.setResizable(true);
		frame.setVisible(true);
		Thread renderThread = new Thread(() -> {
			final long frameTimeNs = 16_666_667L; // 60hz
			final long vblankTimeNs = 1_000_000L; // 1ms
			int blinkCounter = 0; // blonk
		    
			while (running) {
				long frameStart = System.nanoTime();
				this.vblank = false; // scanning
	            switch (videoMode) {
	                case MODE_TEXT -> renderTextMode();
	                case MODE_RGB_32BPP -> render32bppRGB();
	                case MODE_RGB_08BPP -> render8bppRGB();
	            }
	            SwingUtilities.invokeLater(panel::repaint);
				// emulate the crt beam zapping through the screen
				while (System.nanoTime() - frameStart < frameTimeNs - vblankTimeNs) {
					Thread.onSpinWait();
				}
	            // crt beam turned off
	            this.vblank = true;
	            // blinking timer
				if (++blinkCounter >= 30) { // 2 times per sec (60hz)
					blinkOn = !blinkOn;
					blinkCounter = 0;
				}
	            // vblank
	            if (vblankIrqEnable) {
	                this.interrupt(VBLANK_IRQ); // "pull the pin" typashit
	            }
				long vbStart = System.nanoTime();
				while (System.nanoTime() - vbStart < vblankTimeNs) {
					Thread.onSpinWait();
				}
			}
		}, "Emulated VGA Device");
		renderThread.setDaemon(true);
		renderThread.start();
	}

	@Override
	public int readWord(int address) {
		int off = offset(address);
		if (off < REG_END) {
			return switch (off) {
				case VIDEO_CONTROL -> 
					(videoEnable ? VIDEO_CTRL_ENABLE : 0) |
					(vblankIrqEnable ? VIDEO_CTRL_IRQ_VBLANK : 0)
				;
				case VIDEO_MODE -> videoMode;
				case VIDEO_STATUS -> (vblank ? 1 : 0);
				case FRAMEBUFF_WIDTH -> WIDTH;
				case FRAMEBUFF_HEIGHT -> HEIGHT;
				case FRAMEBUFFER_INDEX -> framebufferIndex;
				case CURSOR_X -> cursorX;
				case CURSOR_Y -> cursorY;
				case CURSOR_CONTROL -> 
					(cursorOn ? CURSOR_CTRL_ON : 0) |
					(cursorBlink ? CURSOR_CTRL_BLINK : 0) | 
					(blockCursorMode ? CURSOR_CTRL_BLOCK : 0)
				;
				case PALETTE_INDEX -> paletteIndex;
				case PALETTE_DATA -> palette[paletteIndex];
				default -> 0;
			};
		}
		
		int vro = off - VRAM_BASE;
		return Utils.beBytesToInt(
			vram.get(vro), vram.get(vro + 1),
			vram.get(vro + 2), vram.get(vro + 3)
		);
	}

	@Override
	public void writeWord(int address, int value) {
		int off = offset(address);
		if (off < REG_END) { // registers
			switch (off) {
				case VIDEO_CONTROL -> {
					videoEnable = (value & VIDEO_CTRL_ENABLE) != 0;
					vblankIrqEnable = (value & VIDEO_CTRL_IRQ_VBLANK) != 0;
				}
				case VIDEO_MODE -> videoMode = value;
				case FRAMEBUFFER_INDEX -> framebufferIndex = value & 1;
				// cursor position
				case CURSOR_X -> cursorX = Math.max(0, Math.min(79, value));
				case CURSOR_Y -> cursorY = Math.max(0, Math.min(29, value));
				// cursor control
				case CURSOR_CONTROL -> {
					cursorOn = (value & CURSOR_CTRL_ON) != 0;
					cursorBlink = (value & CURSOR_CTRL_BLINK) != 0;
					blockCursorMode = (value & CURSOR_CTRL_BLOCK) != 0;
				}
				// palette (8bpp mode)
				case PALETTE_INDEX -> paletteIndex = value & 0xFF;
				case PALETTE_DATA -> {
					palette[paletteIndex] = value & 0x00FFFFFF;
				}
			}
			return;
		}
		vram.set32(off - VRAM_BASE, value);
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
				// glyph metadata
				int glyphIdx = codePoint * GLYPH_HEIGHT;
				boolean drawGlyph = !(blink && !blinkOn);
				// draw the cursor
				boolean isCursorCell = (x == cursorX && y == cursorY);
				boolean cursorVisible = cursorOn && (!cursorBlink || blinkOn);
				// draw the glyph
				for (int gy = 0; gy < GLYPH_HEIGHT; gy++) {
					byte horz = FONT_GLYPHS[glyphIdx + gy]; // horizontal line
					boolean underlineRow = underline && gy == GLYPH_HEIGHT - 1;
					boolean cursorRow = cursorVisible && isCursorCell 
						&& (blockCursorMode || (gy >= GLYPH_HEIGHT - 2));
					// scan horizontally
					for (int gx = 0; gx < GLYPH_WIDTH; gx++) {
						// each = 1 in the F16 is a ON (fore)
						boolean on = ((horz >>> (7 - gx)) & 1) != 0;
						int color;
						if (cursorRow) {
							// invert RGB evil type shit (yes this is accurate)
							color = (on ? palette[foreground] : palette[background]) ^ 0xFFFFFF; 
						} else if (underlineRow || (on && drawGlyph)) {
							color = palette[foreground];
						} else {
							color = palette[background];
						}
						image.setRGB(x * GLYPH_WIDTH + gx, y * GLYPH_HEIGHT + gy, color);
					}
				}
			}
		}		
	}
	
}