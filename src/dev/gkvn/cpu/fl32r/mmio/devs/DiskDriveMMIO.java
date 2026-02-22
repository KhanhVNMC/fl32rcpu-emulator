package dev.gkvn.cpu.fl32r.mmio.devs;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

import dev.gkvn.cpu.ByteMemorySpace;
import dev.gkvn.cpu.fl32r.mmio.AbstractMMIODevice;
import dev.gkvn.cpu.fl32r.mmio.FL32RMMIO;

public final class DiskDriveMMIO extends AbstractMMIODevice {
	// disk info
	private static final int SECTOR_SIZE = 512; // 512 bytes per sector
	
	// register offsets
	private static final int 
		REG_DISK_ID  	 = 0x00,
		REG_DISK_STATUS  = 0x04,
		// the software can modify these
		REG_DISK_CONTROL = 0x08,
		REG_DISK_COMMAND = 0x0C,
		REG_DISK_LBA  	 = 0x10,
		REG_SECTOR_COUNT = 0x14,
		REG_DMA_ADDRESS	 = 0x18,
		REG_DMA_LENGTH	 = 0x1C
	;
	
	// commands
	private static final int
		DISK_CMD_READ     = 0x00,
		DISK_CMD_WRITE    = 0x01,
		DISK_CMD_IDENTIFY = 0x02
	;
	
	// status bits
	private static final int 
		DISK_STATUS_READY = 1 << 0, 
		DISK_STATUS_BUSY = 1 << 1, 
		DISK_STATUS_ERROR = 1 << 2
	;
	
	// status bits
	private static final int 
		DISK_CTL_IRQ_ENABLE = 1 << 0
	;
	
	// java stuff
	private final RandomAccessFile disk;
	private final long diskSize;
	
	// disk internal data
	private final long maxLBA; // logical block address (aka sector)
	private int regStatus = DISK_STATUS_READY;
	// current cmd registers
	private int regLBA;
	private int regSectorCount;
	private int regDMAAddress;
	private int regDMALength;

	public DiskDriveMMIO(FL32RMMIO mmio, int base, Path host) {
		super(mmio, base, 32);
		if (!host.toString().endsWith(".evdk")) {
			throw new IllegalArgumentException("EVDK backing file must end with .evdk: " + host);
		}
		// find & validate the backing file (the "disk", literally)
		if (Files.exists(host)) {
			if (!Files.isRegularFile(host)) {
				throw new IllegalArgumentException("EVDK path is not a regular file: " + host);
			}
		} else {
			throw new RuntimeException("Specified EVDK path not found: " + host);
		}
		// open the file as a random access thingy (ofc)
		try {
			this.disk = new RandomAccessFile(host.toFile(), "rw");
			this.diskSize = disk.length();
		} catch (IOException e) {
			throw new RuntimeException("Failed to open EVDK backing file: " + host, e);
		}
		// prevent stupid disks
		if (this.diskSize % SECTOR_SIZE != 0) {
			throw new RuntimeException("Invalid EVDK: size not aligned to 512B sectors");
		}
		this.maxLBA = this.diskSize / SECTOR_SIZE;
	}
	
	@Override
	public int readWord(int address) {
		
	}
	
	@Override
	public void writeWord(int address, int value) {

	}
	
	private void doRead() throws IOException {
	    // latch parameters
		long lba    = Integer.toUnsignedLong(regLBA);
		int sectors = regSectorCount;
		int dmaAddr = regDMAAddress;
		int dmaLen  = regDMALength;
	    
		// job started
		this.regStatus = DISK_STATUS_BUSY;
		
		if (!sanityChecks(lba, sectors, dmaLen)) {
			// job failed
			this.regStatus = DISK_STATUS_ERROR;
			return;
		}

		byte[] buf = new byte[dmaLen];
		ByteMemorySpace ram = getEmulator().getMemory();
		
		disk.seek(lba * SECTOR_SIZE);
		disk.readFully(buf);
		for (int i = 0; i < buf.length; i++) {
			ram.set(dmaAddr + i, buf[i]);
		}
		
		// finished the job
		this.regStatus = DISK_STATUS_READY;
	}
	
	private void doWrite() throws IOException {
	    // latch parameters
		long lba    = Integer.toUnsignedLong(regLBA);
		int sectors = regSectorCount;
		int dmaAddr = regDMAAddress;
		int dmaLen  = regDMALength;
	    
		// job started
		this.regStatus = DISK_STATUS_BUSY;
		
		if (!sanityChecks(lba, sectors, dmaLen)) {
			// job failed
			this.regStatus = DISK_STATUS_ERROR;
			return;
		}
		
		byte[] buf = new byte[dmaLen];
		ByteMemorySpace ram = getEmulator().getMemory();
		
		for (int i = 0; i < buf.length; i++) {
			buf[i] = ram.get(dmaAddr + i);
		}
		disk.seek(lba * SECTOR_SIZE);
		disk.write(buf);
		
		// finished the job
		this.regStatus = DISK_STATUS_READY;
	}
	
	private boolean sanityChecks(long lba, int sectors, int dmaLen) {
		if (lba < 0 || sectors <= 0) {
			return false;
		}
		if (lba + sectors > maxLBA) {
			return false;
		}
		if (dmaLen != sectors * SECTOR_SIZE) {
			return false;
		}
		return true;
	}
}