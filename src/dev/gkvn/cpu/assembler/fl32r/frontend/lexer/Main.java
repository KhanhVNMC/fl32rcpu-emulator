package dev.gkvn.cpu.assembler.fl32r.frontend.lexer;

import java.nio.file.Files;
import java.nio.file.Path;

import dev.gkvn.cpu.assembler.fl32r.frontend.FLIREmitter;
import dev.gkvn.cpu.assembler.fl32r.frontend.exceptions.AsmError;

public class Main {
	public static void main(String[] args) throws Exception {
		AsmLexer lex = new AsmLexer(Files.readString(Path.of("test.s")));
		FLIREmitter p = new FLIREmitter(lex);
		try {
			System.out.println(p.emit().toString());
		} catch (AsmError e) {
			reportError(lex, e);
		}
	}
	
	private static void reportError(AsmLexer lex, AsmError e) {
		Token t = e.token;
		System.err.println("[ASM ERROR!] " + e.getMessage() + " at line " + (t.line() + 1) + ", column " + t.column());
		System.err.println("" + lex.sourceLines[t.line()]);
		System.err.println("~".repeat(t.column()) + "^");
	}
}
