package dev.gkvn.cpu.assembler.fl32r.frontend.lexer;

public record Token(TokenType type, String literal, int line, int column) {
	public boolean is(TokenType... types) {
		for (TokenType t : types) {
			if (type == t) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		return type + ": '" + literal + "'";
	}
}
