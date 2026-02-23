package dev.gkvn.cpu.fl32r.mmio.devs;

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
	public static final int DISK_IRQ     = 0x01;
	
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
		DISK_STATUS_READY = 0x00, 
		DISK_STATUS_BUSY = 0x01, 
		DISK_STATUS_ERROR = 0x0F
	;
	
	// status bits
	private static final int 
		DISK_CTL_IRQ_ENABLE = 1 << 0
	;
	
	// java stuff
	private final RandomAccessFile disk;
	private final EmulatedVirtualDisk evdk;
	
	// disk internal data
	private final long totalSectors; // logical block address (aka sector)
	private final long diskSize; // in bytes
	private int deviceControl;
	private int regStatus = DISK_STATUS_READY;
	// current cmd registers
	private int regLBA;
	private int regSectorCount;
	private int regDMAAddress;
	private int regDMALength;

	public DiskDriveMMIO(FL32RMMIO mmio, int base, EmulatedVirtualDisk evdk) {
		super(mmio, base, 32);
		Path host = evdk.backingFile();
		// initial checks (evdk format)
		if (evdk.brand().length() > 16) {
			throw new IllegalArgumentException("EVDK brand name too long");
		}
		if (evdk.model().length() > 32) {
			throw new IllegalArgumentException("EVDK model name too long");
		}
		if (evdk.serialNum().length() > 32) {
			throw new IllegalArgumentException("EVDK serial number identifier too long");
		}
		if (evdk.firmwareVer().length() > 16) {
			throw new IllegalArgumentException("EVDK firmware version too long");
		}
		
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
		this.totalSectors = this.diskSize / SECTOR_SIZE;
		this.evdk = evdk;
	}
	
	@Override
	public int readWord(int address) {
		return switch (offset(address)) {
			case REG_DISK_ID -> 0xBEEFCAFE; // magic
			case REG_DISK_STATUS -> regStatus;
			case REG_DISK_CONTROL -> deviceControl;
			case REG_DISK_COMMAND -> 0; // write-only
			case REG_DISK_LBA -> regLBA;
			case REG_SECTOR_COUNT -> regSectorCount;
			case REG_DMA_ADDRESS -> regDMAAddress;
			case REG_DMA_LENGTH -> regDMALength;
			default -> 0;
		};
	}
	
	@Override
	public void writeWord(int address, int value) {
		switch (offset(address)) {
			case REG_DISK_CONTROL -> deviceControl = value;
			case REG_DISK_LBA -> regLBA = value;
			case REG_SECTOR_COUNT -> regSectorCount = value;
			case REG_DMA_ADDRESS -> regDMAAddress = value;
			case REG_DMA_LENGTH -> regDMALength = value;
			case REG_DISK_COMMAND -> {
				// ignore commands while doing shit
				if ((this.regStatus & DISK_STATUS_BUSY) != 0) {
					return;
				}
				try {
					boolean irqEnabled = (deviceControl & DISK_CTL_IRQ_ENABLE) == 1;
					switch (value) {
						case DISK_CMD_READ -> {
							doRead();
							if (irqEnabled) interrupt(DISK_IRQ);
						}
						case DISK_CMD_WRITE -> {
							doWrite();
							if (irqEnabled) interrupt(DISK_IRQ);
						}
						case DISK_CMD_IDENTIFY -> {
							doIdentify();
							if (irqEnabled) interrupt(DISK_IRQ);
						}
						default -> regStatus = DISK_STATUS_ERROR;
					}
				} catch (IOException e) {
					// java being java
					regStatus = DISK_STATUS_ERROR;
				}
			}
			// readonly registers ignored
			case REG_DISK_ID, REG_DISK_STATUS -> {}
		}
	}
	
	private static final int
		IDENT_SECTOR_SIZE   = 0x00,
		IDENT_SECTOR_COUNT  = 0x04,
		IDENT_DISK_BYTES_HI = 0x08,
		IDENT_DISK_BYTES_LO = 0x0C,
		IDENT_DISK_BRAND    = 0x10,
		IDENT_DISK_MODEL    = 0x10 + 16,
		IDENT_DISK_SERIAL   = 0x10 + 16 + 32,
		IDENT_DISK_FIRMWARE = 0x10 + 16 + 32 + 32
		/**
		 * typedef struct {
		 * 	  uint32_t sector_size;
		 * 	  uint32_t sector_count;
		 *    uint32_t disk_bytes_hi;
		 *    uint32_t disk_bytes_lo;
		 *    char     disk_brand[16];
		 *    char     disk_model[32];
		 *    char     disk_serial[32];
		 *    char     disk_firmware[16];
		 * } DISK_IDENTIFY;
		 */
	;
	private void doIdentify() {
		// latch parameters
		int dmaAddr = regDMAAddress;
		
		// job started
		this.regStatus = DISK_STATUS_BUSY;
		
		ByteMemorySpace ram = getEmulator().getMemory(); // literally DMA
		ram.set32(dmaAddr + IDENT_SECTOR_SIZE, SECTOR_SIZE);
		ram.set32(dmaAddr + IDENT_SECTOR_COUNT, (int)(totalSectors & 0xFF_FF_FF_FFL));
		ram.set32(dmaAddr + IDENT_DISK_BYTES_HI, (int) (diskSize >>> 32));
		ram.set32(dmaAddr + IDENT_DISK_BYTES_LO, (int) (diskSize & 0xFF_FF_FF_FFL));
		writeString(ram, 16, evdk.brand().toCharArray(), dmaAddr + IDENT_DISK_BRAND);
		writeString(ram, 32, evdk.model().toCharArray(), dmaAddr + IDENT_DISK_MODEL);		
		writeString(ram, 32, evdk.serialNum().toCharArray(), dmaAddr + IDENT_DISK_SERIAL);		
		writeString(ram, 16, evdk.firmwareVer().toCharArray(), dmaAddr + IDENT_DISK_FIRMWARE);
		
		// finished the job
		this.regStatus = DISK_STATUS_READY;
	}
	
	private void writeString(ByteMemorySpace mem, int bufLen, char src[], long dest) {
		for (int i = 0; i < bufLen; i++) {
			byte toWrite;
			if (i >= src.length) {
				toWrite = 0x00;
			} else {
				toWrite = (byte) src[i];
			}
			mem.set(dest + i, toWrite);
		}
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
		if (lba + sectors > totalSectors) {
			return false;
		}
		if (dmaLen != sectors * SECTOR_SIZE) {
			return false;
		}
		return true;
	}
	
	public static record EmulatedVirtualDisk(
		Path   backingFile,
		String brand,
		String model,
		String serialNum,
		String firmwareVer
	) {}
	
	public static Path manifestEVDK(long capacityBytes, String fileName) {
		if (capacityBytes <= 0) {
			throw new IllegalArgumentException("Disk capacity must be > 0");
		}
		if (capacityBytes % SECTOR_SIZE != 0) {
			throw new IllegalArgumentException("Disk capacity must be aligned to 512-byte sectors");
		}
		if (!fileName.endsWith(".evdk")) {
			throw new IllegalArgumentException("Virtual disk file must end with .evdk");
		}

		Path path = Path.of(fileName);
		if (Files.exists(path)) {
			if (!Files.isRegularFile(path)) {
				throw new IllegalArgumentException("Path exists but is not a regular file: " + path);
			}
			return path;
		}
		try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw")) {
			raf.setLength(capacityBytes);
		} catch (IOException e) {
			throw new RuntimeException("Failed to create virtual disk file: " + path, e);
		}
		return path;
	}
}