package dev.gkvn.cpu.assembler.fl32r.lexer;

import dev.gkvn.cpu.assembler.fl32r.parser.Parser;

public class Main {
	public static void main(String[] args) {
		AsmLexer lex = new AsmLexer("@data\nconst: .word (128 * 2)\n@text\nlabel: LUI R0, [R1+10]\nLOL R1, R2");
		Parser p = new Parser(lex);
		p.parse();
		System.out.println(p.lineTokens);
	}
}
