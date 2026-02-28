package dev.gkvn.cpu.fl32r.assembler.frontend.lexer;

import java.nio.file.Files;
import java.nio.file.Path;

import dev.gkvn.cpu.fl32r.assembler.backend.BackendCodegen;
import dev.gkvn.cpu.fl32r.assembler.frontend.LineStreamProvider;
import dev.gkvn.cpu.fl32r.assembler.frontend.FLIREmitter;
import dev.gkvn.cpu.fl32r.assembler.frontend.exceptions.AsmError;

public class Main {
	public static void main(String[] args) throws Exception {
		Path pt = Path.of("asm/vga.s");
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
