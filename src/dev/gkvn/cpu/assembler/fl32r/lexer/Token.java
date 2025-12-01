package dev.gkvn.cpu.assembler.fl32r.lexer;

public class Token {
    public final TokenType type;
    public final String literal;
    public final int line;
    public final int column;
    
    public Token(TokenType type, String lexeme, int line, int column) {
        this.type = type;
        this.literal = lexeme;
        this.line = line;
        this.column = column;
    }
    
    @Override
    public String toString() {
        return type + ": '" + literal + "'";
    }
}