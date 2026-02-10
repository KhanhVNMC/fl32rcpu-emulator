package dev.gkvn.cpu.assembler.fl32r.lexer;

import java.nio.file.Files;
import java.nio.file.Path;

import dev.gkvn.cpu.assembler.fl32r.parser.FLIREmitter;

public class Main {
	public static void main(String[] args) throws Exception {
		AsmLexer lex = new AsmLexer(Files.readString(Path.of("test.s")));
		FLIREmitter p = new FLIREmitter(lex);
		p.parse();
	}
}
