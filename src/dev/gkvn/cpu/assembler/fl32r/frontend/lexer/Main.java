package dev.gkvn.cpu.assembler.fl32r.frontend.lexer;

import java.nio.file.Files;
import java.nio.file.Path;

import dev.gkvn.cpu.assembler.fl32r.backend.BackendCodegen;
import dev.gkvn.cpu.assembler.fl32r.frontend.LineStreamProvider;
import dev.gkvn.cpu.assembler.fl32r.frontend.FLIREmitter;
import dev.gkvn.cpu.assembler.fl32r.frontend.exceptions.AsmError;

public class Main {
	public static void main(String[] args) throws Exception {
		Path pt = Path.of("asm/fool_test.s");
		AsmLexer lex = new AsmLexer(pt, Files.readString(pt));
		FLIREmitter p = new FLIREmitter(new LineStreamProvider(lex));
		try {
			new BackendCodegen(p.emit()).gen();
		} catch (AsmError e) {
			reportError(e);
		}
	}
	
	private static void reportError(AsmError e) {
		Token t = e.token;
		System.err.println("[ASM ERROR!] " + e.getMessage() + " in \"" + t.lexer().getSourcePath() + "\" at line " + (t.line() + 1) + ", column " + t.column());
		System.err.println("" + t.lexer().getSourceAtLine(t.line()));
		System.err.println("~".repeat(t.column()) + "^");
	}
}
