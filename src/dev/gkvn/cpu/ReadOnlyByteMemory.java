package dev.gkvn.cpu;

public class ReadOnlyByteMemory {
	public final ByteMemory inner;
	
	public ReadOnlyByteMemory(ByteMemory inner) {
		this.inner = inner;
	}
	
	public long length() {
		return inner.length();
	}
	
	public byte get(long index) {
		return inner.get(index);
	}
}