package dev.gkvn.cpu.assembler.fl32r.parser;

import dev.gkvn.cpu.assembler.fl32r.lexer.Token;

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
