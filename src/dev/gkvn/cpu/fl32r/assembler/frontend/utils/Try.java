package dev.gkvn.cpu.fl32r.assembler.frontend.utils;

import dev.gkvn.cpu.fl32r.assembler.frontend.exceptions.AsmError;
import dev.gkvn.cpu.fl32r.assembler.frontend.lexer.Token;

public final class Try {
	private Try() {}
	
	@FunctionalInterface
	public interface ThrowingSupplier<T> {
		T get() throws Throwable;
	}
	
	@FunctionalInterface
	public interface ExceptionMapper {
		RuntimeException map(Throwable t);
	}
	
	public static <T> T absorbAsm(ThrowingSupplier<T> body, Token atFault) {
		return absorb(body, (t) -> new AsmError(t.getMessage(), atFault));
	}
	
	public static void catchAsm(Runnable body, Token atFault) {
		absorb(() -> { body.run(); return null; }, (t) -> new AsmError(t.getMessage(), atFault));
	}
	
	public static <T> T absorb(ThrowingSupplier<T> body, ExceptionMapper mapper) {
		try {
			return body.get();
		} catch (Throwable t) {
			RuntimeException wrapped = mapper.map(t);
			if (wrapped != null) {
				throw wrapped;
			}
			return null; // swallowed
		}
	}
}
