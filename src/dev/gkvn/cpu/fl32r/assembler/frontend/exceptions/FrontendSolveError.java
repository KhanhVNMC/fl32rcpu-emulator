package dev.gkvn.cpu.fl32r.assembler.frontend.exceptions;

public class FrontendSolveError extends Exception {
	public FrontendSolveError(String string) {
		super(string);
	}
	
	public FrontendSolveError(String string, Object... format) {
		super(String.format(string, format));
	}
}
