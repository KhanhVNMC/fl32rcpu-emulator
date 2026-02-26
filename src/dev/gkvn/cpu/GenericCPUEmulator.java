package dev.gkvn.cpu;

/**
 * I will make this a bare class, fuck documentation.
 */
public interface GenericCPUEmulator {
	void setFrequencyHz(int hertz);
	int getFrequencyHz();
	void loadBootROM(byte[] program);
	void start(boolean startInSingleStepMode);
	void halt();
	void resume();
	void reset(boolean resetToInSingleStepMode);
	void kill();
	void activateSingleStepMode();
	void stepExecution();
	void deactivateSingleStepMode();
	void addBreakpointPhysical(long physicalAddresss);
	void removeBreakpointPhysical(long physicalAddresss);
	boolean[] dumpFlags();	
	int[] dumpRegisters();
	ByteMemorySpace getMemory();
	ByteMemorySpace getReadOnlyMemory(); // ROM
	boolean isStarted();
	boolean isKilled(); // what
	default boolean isCPUAvailable() {
		return isStarted() && !isKilled();
	}
	boolean isAutonomousExecutionEnabled();
	boolean isOnSingleStepMode();
}
