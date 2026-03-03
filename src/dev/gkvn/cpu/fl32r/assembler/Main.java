package dev.gkvn.cpu.fl32r.assembler;

import java.nio.file.Files;
import java.nio.file.Path;

import dev.gkvn.cpu.fl32r.assembler.backend.BackendCodegen;
import dev.gkvn.cpu.fl32r.assembler.frontend.FLIREmitter;
import dev.gkvn.cpu.fl32r.assembler.frontend.LineStreamProvider;
import dev.gkvn.cpu.fl32r.assembler.frontend.core.Instruction;
import dev.gkvn.cpu.fl32r.assembler.frontend.exceptions.AsmError;
import dev.gkvn.cpu.fl32r.assembler.frontend.lexer.AsmLexer;
import dev.gkvn.cpu.fl32r.assembler.frontend.lexer.Token;
import dev.gkvn.cpu.fl32r.emulator.FL32REmulator;
import dev.gkvn.cpu.utils.Calc;

public class Main {
	public static void main(String[] args) {
		String inputPath = null;
		String outputPath = "out.bin";
		boolean dumpCair = false;
		boolean startVM = false;
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
				case "-i" -> inputPath = args[++i];
				case "-o" -> outputPath = args[++i];
				case "--dump-cair" -> dumpCair = true;
				case "--start-vm" -> startVM = true;
				case "-h", "--help" -> {
					printHelp();
					return;
				}
				default -> System.err.println("Unknown argument: " + args[i]);
			}
		}
		if (inputPath == null) {
			System.err.println("Error: No input file specified. Use -i <file>");
			return;
		}
		Path pt = Path.of(inputPath);
		if (!Files.exists(pt)) {
			System.err.println("Error: Input file '" + pt + "' not found. Please try again.");
			return;
		}
		try {
			String source = Files.readString(pt);
			AsmLexer lex = new AsmLexer(pt, source);
			FLIREmitter emitter = new FLIREmitter(new LineStreamProvider(lex));
			var cair = emitter.emit();
			if (dumpCair) {
				System.out.println("--- CONTEXT-AWARE IR DUMP ---");
				int pc = 0x00;
				for (Instruction instr : cair.instructions()) {
					System.out.printf("%08X   %s\n", pc, instr.toString());
					pc += instr.getSize();
				}
				System.out.println("-----------------------------");
			}
			byte[] binary = new BackendCodegen(cair).generate();
			Files.write(Path.of(outputPath), binary);
			System.out.printf("Successfully assembled: %s -> %s (%d bytes)%n", inputPath, outputPath, binary.length);
			
			// start the emulator/vm
			if (startVM) {
				System.out.println("\n[VM] Starting FL32R-compliant CPU Emulator...");
				FL32REmulator emu = new FL32REmulator(Calc.MB(64));
				emu.setFrequencyHz(128_000_000); // 32MHZ cpu
				emu.loadBootROM(Files.readAllBytes(Path.of(outputPath)));
				emu.start(false);
				System.out.println("\n[VM] CPU Halted! Registers Dump:");
				int reg[] = emu.dumpRegisters();
				for (int i = 0; i < reg.length; i++) {
					System.out.printf("R%02d: 0x%08X | %d\n", (i + 1), reg[i], Integer.toUnsignedLong(reg[i]));
				}
				System.exit(1);
			}
		} catch (AsmError e) {
			reportError(e);
			System.exit(1);
		} catch (Exception e) {
			System.err.println("[INTERNAL ERROR]: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void printHelp() {
		System.out.println("FL32R Reference Assembler And Virtual Machine (flasm)");
		System.out.println("Usage: flasm -i <input.s> [options]");
		System.out.println("Options:");
		System.out.println("  -i <file>       Input assembly file");
		System.out.println("  -o <file>       Output binary (default: out.bin)");
		System.out.println("  --dump-cair     Print Context-Aware IR for debugging");
		System.out.println("  --start-vm      Start the FL32R VM after assembling");
		System.out.println("  -h, --help      Show this help message");
	}

	private static void reportError(AsmError e) {
		Token t = e.token;
		String sourcePath = t.lexer().getSourcePath().toString();
		int line = t.line() + 1;
		System.err.println("[Error] " + e.getMessage());
		System.err.printf("  at %s:%d:%d%n", sourcePath, line, t.column());
		System.err.println("    | ");
		System.err.printf (" %02d | %s\n", t.line() + 1, t.lexer().getSourceAtLine(t.line()));
		System.err.println("    | " + "~".repeat(t.column()) + "^".repeat(t.literal().length()) + "--- here");
	}
}