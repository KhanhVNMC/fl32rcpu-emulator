package dev.gkvn.cpu.assembler.fl32r.parser;

import dev.gkvn.cpu.assembler.fl32r.lexer.Token;

public class AsmError extends RuntimeException {
    public final Token token;

    public AsmError(String message, Token token) {
        super(message + " at " + token.line() + ":" + token.column());
        this.token = token;
    }
}
