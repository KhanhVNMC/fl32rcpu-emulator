package dev.gkvn.cpu;

public class ByteMemorySpace {
	private static final int CHUNK_SIZE = 1 << 30; // 1 gb per chunk
	private final byte[][] chunks;
	private final long length;
	
	public ByteMemorySpace(long size) {
		if (size < 0) {
			throw new IllegalArgumentException("Size must be >= 0");
		}
		this.length = size;

		int numFullChunks = (int) (size / CHUNK_SIZE);
		int remaining = (int) (size % CHUNK_SIZE);
		int totalChunks = remaining > 0 ? numFullChunks + 1 : numFullChunks;

		chunks = new byte[totalChunks][];
		for (int i = 0; i < numFullChunks; i++) {
			chunks[i] = new byte[CHUNK_SIZE];
		}
		if (remaining > 0) {
			chunks[totalChunks - 1] = new byte[remaining];
		}
	}

	public byte get(long index) {
		checkIndex(index);
		int chunk = (int) (index / CHUNK_SIZE);
		int offset = (int) (index % CHUNK_SIZE);
		return chunks[chunk][offset];
	}

	public void set(long index, byte value) {
		checkIndex(index);
		int chunk = (int) (index / CHUNK_SIZE);
		int offset = (int) (index % CHUNK_SIZE);
		chunks[chunk][offset] = value;
	}

	public long length() {
		return length;
	}

	private void checkIndex(long index) {
		if (index >= 0 && index < length) return;
		throw new IndexOutOfBoundsException("Index " + index + " out of bounds of " + length);
	}
}
