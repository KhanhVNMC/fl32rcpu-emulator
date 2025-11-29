package dev.gkvn.cpu;

/**
 * Generic interface for a CPU emulator.
 * 
 * It does not define any specific ISA; implementations may simulate any
 * instruction set, as long as they follow these lifecycle and timing rules.
 */
public interface GenericCPUEmulator {

	/**
	 * Sets the emulated CPU frequency.
	 *
	 * @param hertz The desired clock rate in Hz. A value of 0 disables timing
	 *              emulation and runs instructions as fast as the host JVM can.
	 */
	void setFrequencyHz(int hertz);

	/**
	 * @return The currently configured emulated CPU frequency in Hz.
	 */
	int getFrequencyHz();

	/**
	 * Loads the boot program into memory at address 0x00000000.
	 * 
	 * Must be called exactly once before {@link #start()}.
	 * 
	 * Implementations should: 
	 * - copy the provided byte array into RAM 
	 * - set initial PC, SP, and privilege state appropriately
	 * 
	 * @param program The raw binary program image.
	 * @throws IllegalStateException    if a boot program has already been loaded.
	 * @throws IllegalArgumentException if the program exceeds available memory.
	 */
	void loadBootProgram(byte[] program);

	/**
	 * Starts CPU execution on the current thread.
	 *
	 * The CPU enters its main FETCH/DECODE/EXECUTE loop and will continue running
	 * until {@link #kill()} is called or the host thread terminates.
	 *
	 * <p><b>Important notes:</b>
	 * <ul>
	 * 	 <li>Execution occurs on the <b>current thread</b> that calls this method.</li>
	 * 	 <li>If {@code startInSingleStepMode} is true, the CPU starts in single-step
	 *       mode: autonomous execution is disabled and instructions execute only when
	 *       {@link #stepExecution()} is invoked.
	 *   </li>
	 *   <li>If {@code startInSingleStepMode} is false, the CPU executes instructions autonomously.</li>
	 * </ul>
	 *
	 * @param startInSingleStepMode If true, CPU starts in single-step mode;
	 *                              otherwise, autonomous execution begins
	 *                              immediately.
	 * @throws IllegalStateException if the CPU has already been started.
	 */
	void start(boolean startInSingleStepMode);
	
	/**
	 * Halts the CPU.
	 * 
	 * The CPU stops executing instructions but remains powered on. Memory,
	 * registers, and state remain intact. Execution may be resumed with
	 * {@link #resume()} or via single-step debugging.
	 */
	void halt();
	
	/**
	 * Resumes a halted CPU.
	 * 
	 * No effect if the CPU is already running.
	 */
	void resume();

	/**
	 * Performs a CPU reset.
	 *
	 * Resets registers, flags, and internal state to their post-boot values. Memory
	 * contents are preserved.
	 *
	 * <p><b>Important notes!</b>
	 * <ul>
	 *    <li>If {@code resetToSingleStepMode} is true, the CPU enters single-step mode after reset.</li>
	 *    <li>If false, the CPU resumes autonomous execution after reset (if it was previously started).</li>
	 * </ul>
	 *
	 * @param resetToSingleStepMode Whether the CPU should start in single-step mode after reset.
	 * @throws IllegalStateException if the CPU has been permanently killed.
	 */
	void reset(boolean resetToInSingleStepMode);

	/**
	 * Permanently stops the CPU.
	 *
	 * After being killed: 
	 * - execution loops exit, 
	 * - the CPU cannot be restarted, 
	 * - {@link #reset()} is not allowed.
	 * 
	 * Memory remains readable for debugging.
	 */
	void kill();

	// === SINGLE STEP DEBUGGING ===

	/**
	 * Enables single-step mode.
	 * 
	 * Autonomous execution stops, and the CPU will execute instructions only when
	 * {@link #stepExecution()} is invoked.
	 * 
	 * @throws IllegalStateException if the CPU is not running or already in
	 *                               single-step mode.
	 */
	void activateSingleStepMode();

	/**
	 * Executes exactly one instruction.
	 *
	 * Requires single-step mode to be active.
	 *
	 * <p><b>Important note:</b> Execution may freeze the calling thread to simulate
	 * real CPU timing if the emulated frequency is low. This allows instruction
	 * execution to respect the configured clock rate.
	 *
	 * @throws IllegalStateException if single-step mode is not enabled.
	 */
	void stepExecution();

	/**
	 * Disables single-step mode and returns the CPU to normal execution.
	 *
	 * @throws IllegalStateException if single-step mode is not enabled.
	 */
	void deactivateSingleStepMode();
	
	/**
	 * Adds a breakpoint at the specified virtual address.
	 * <p>
	 * A virtual breakpoint triggers when the program counter (PC) reaches the given
	 * virtual address after address translation.
	 *
	 * @param virtualAddress the virtual address at which execution will pause
	 */
	void addBreakpoint(long virtualAddress);

	/**
	 * Removes the breakpoint set at the specified virtual address.
	 * 
	 * @param virtualAddress
	 */
	void removeBreakpoint(long virtualAddress);

	/**
	 * Adds a breakpoint at the specified physical memory address.
	 * <p>
	 * Physical breakpoints trigger when the CPU fetches an instruction located at
	 * the corresponding physical address.
	 *
	 * @param physicalAddress the physical address at which execution will pause
	 */
	void addBreakpointPhysical(long physicalAddresss);

	/**
	 * Removes the breakpoint set at the specified physical address.
	 *
	 * @param physicalAddress the physical address whose breakpoint should be
	 *                        removed
	 */
	void removeBreakpointPhysical(long physicalAddresss);
	
	// === STATE INSPECTION ===

	/**
	 * @return A defensive copy of all CPU general-purpose registers. The returned
	 *         array is safe to modify without affecting the emulator.
	 */
	int[] dumpRegisters();

	/**
	 * @return A snapshot of internal CPU flags (ZF/NF/OF/etc), in
	 *         implementation-defined order.
	 */
	boolean[] dumpFlags();

	/**
	 * @return A read-only view of the emulator's entire memory space. Mutating the
	 *         returned object must not affect emulator RAM.
	 */
	ReadOnlyByteMemory dumpMemory();

	// === GETTER BULLSHIT ===

	/**
	 * @return true if the CPU has been successfully started via {@link #start()},
	 *         and has not been reset or killed since. Once started, the CPU enters
	 *         its execution loop and may be running, halted, or in single-step
	 *         mode.
	 */
	boolean isStarted();

	/**
	 * @return true if the CPU has been permanently killed via {@link #kill()}. When
	 *         killed, the CPU can no longer execute or be restarted.
	 */
	boolean isKilled();
	
	/**
	 * @return true if the CPU is started and not killed, false otherwise
	 */
	default boolean isCPUAvailable() {
		return isStarted() && !isKilled();
	}

	/**
	 * Indicates whether the CPU is currently executing instructions autonomously
	 * (i.e., not halted and not in single-step mode).
	 *
	 * When this returns true: 
	 * - The CPU is running its main fetch/decode/execute
	 * loop. 
	 * - Timing may be governed by the configured frequency. 
	 * - {@link #stepExecution()} cannot be used.
	 *
	 * When false: - The CPU is either halted, in single-step mode, or killed.
	 *
	 * @return true if the CPU is actively executing instructions on its own.
	 */
	boolean isAutonomousExecutionEnabled();

	/**
	 * @return true if single-step mode has been activated via
	 * {@link #activateSingleStepMode()} and is currently active.
	 *
	 * In this mode: 
	 *  - The CPU will not execute autonomously.
	 *  - Instructions execute only when {@link #stepExecution()} is invoked.
	 */
	boolean isOnSingleStepMode();
}
