package dev.gkvn.cpu;

public class ImmutableByteSpace {
	private final ByteMemorySpace inner;
	
	public ImmutableByteSpace(ByteMemorySpace inner) {
		this.inner = inner;
	}
	
	public long length() {
		return inner.length();
	}
	
	public byte get(long index) {
		return inner.get(index);
	}
}