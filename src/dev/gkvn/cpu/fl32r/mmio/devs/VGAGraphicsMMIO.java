package dev.gkvn.cpu.fl32r.mmio.devs;

import dev.gkvn.cpu.ByteMemorySpace;
import dev.gkvn.cpu.fl32r.mmio.AbstractMMIODevice;
import dev.gkvn.cpu.fl32r.mmio.FL32RMMIO;

public final class VGAGraphicsMMIO extends AbstractMMIODevice {	
	// VGA info
	public static final int VBLANK_IRQ  = 0x02;
	// 80 (horizontal) x 30 (vertical) for a total of 240 chars, 4 bytes each
	public static final int TEXT_MODE_SIZE = 80 * 30 * 4;
	public static final int RGB_MODE_SIZE = 640 * 480 * 4;
	
	// register offsets
	public static final int 
		VIDEO_CONTROL = 0x00,
		VIDEO_MODE = 0x04,
		VIDEO_STATUS = 0x08,
		FRAMEBUFF_WIDTH = 0x0C,
		FRAMEBUFF_HEIGHT = 0x10,
		FRAMEBUFFER_INDEX = 0x14,
		
		// vram offsets
		TEXT_MODE_REGION_BASE = 0x30,
		RGB_MODE_FB1_BASE = TEXT_MODE_REGION_BASE + TEXT_MODE_SIZE,
		RGB_MODE_FB2_BASE = RGB_MODE_FB1_BASE + RGB_MODE_SIZE
	;
	
	// "vram" lmfao
	private ByteMemorySpace textVRAM = new ByteMemorySpace(TEXT_MODE_SIZE);
	private ByteMemorySpace rgbVRAM  = new ByteMemorySpace(RGB_MODE_SIZE * 2);
	
	public VGAGraphicsMMIO(FL32RMMIO mmio, int base) {
		super(mmio, base, 32);
	}

	@Override
	public byte readByte(int address) {
		return 0;
	}

	@Override
	public int readWord(int address) {
		return 0;
	}
	
	@Override
	public void writeByte(int address, byte value) {
		
	}
	
	@Override
	public void writeWord(int address, int value) {
		
	}
}