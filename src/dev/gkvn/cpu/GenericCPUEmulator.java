package dev.gkvn.cpu;

public interface GenericCPUEmulator {
	public void setFrequencyHz(int hertz);
	public int getFrequencyHz();
	
	public void loadBootProgram(byte[] program);
	
	public void start();
	public void halt();
	public void kill();
	
	public int[] dumpRegisters();
	public boolean[] dumpFlags();
	public ReadOnlyByteMemory dumpMemory();
}
