package dev.gkvn.cpu.assembler.fl32r.frontend.exceptions;

import dev.gkvn.cpu.assembler.fl32r.frontend.lexer.Token;

public class AsmError extends RuntimeException {
    public final Token token;

    public AsmError(String message, Token token) {
        super(message);
        this.token = token;
    }
    
    public AsmError(Token token, String message, Object... fmt) {
        this(String.format(message, fmt), token);
    }
}
