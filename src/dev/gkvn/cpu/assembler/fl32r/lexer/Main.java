package dev.gkvn.cpu.assembler.fl32r.lexer;

public class Main {
	public static void main(String[] args) {
		AsmLexer lex = new AsmLexer("@data\nconst: .str \"hello world\\\"\"\n@text\ndata:\n LUI R0, [R1+10]\nLOL R1, R2");
		lex.scanTokens();
		System.out.println(lex.tokens);
	}
}
