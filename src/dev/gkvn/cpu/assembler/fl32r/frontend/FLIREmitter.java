package dev.gkvn.cpu.assembler.fl32r.frontend;

import static dev.gkvn.cpu.assembler.fl32r.frontend.utils.ConstantFolder.*;
import static dev.gkvn.cpu.assembler.fl32r.frontend.utils.FL32RSpecs.*;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.gkvn.cpu.assembler.fl32r.frontend.arch.FEOpCode;
import dev.gkvn.cpu.assembler.fl32r.frontend.arch.FEOperandType;
import dev.gkvn.cpu.assembler.fl32r.frontend.core.DataSymbol;
import dev.gkvn.cpu.assembler.fl32r.frontend.core.Instruction;
import dev.gkvn.cpu.assembler.fl32r.frontend.core.LabelText;
import dev.gkvn.cpu.assembler.fl32r.frontend.core.ParsingContext;
import dev.gkvn.cpu.assembler.fl32r.frontend.exceptions.AsmError;
import dev.gkvn.cpu.assembler.fl32r.frontend.lexer.AsmLexer;
import dev.gkvn.cpu.assembler.fl32r.frontend.lexer.Token;
import dev.gkvn.cpu.assembler.fl32r.frontend.lexer.TokenType;
import dev.gkvn.cpu.assembler.fl32r.frontend.operands.ImmLabel;
import dev.gkvn.cpu.assembler.fl32r.frontend.operands.ImmLiteral;
import dev.gkvn.cpu.assembler.fl32r.frontend.operands.ImmVariable;
import dev.gkvn.cpu.assembler.fl32r.frontend.operands.SizedMemoryOperand;
import dev.gkvn.cpu.assembler.fl32r.frontend.operands.MemoryOperand;
import dev.gkvn.cpu.assembler.fl32r.frontend.operands.Operand;
import dev.gkvn.cpu.assembler.fl32r.frontend.operands.RegisterOperand;
import dev.gkvn.cpu.assembler.fl32r.frontend.utils.FL32RSpecs;
import dev.gkvn.cpu.assembler.fl32r.frontend.utils.TokenStream;
import dev.gkvn.cpu.assembler.fl32r.frontend.utils.Try;

public class FLIREmitter {
	final static String DATA_SECTION = "@data";
	final static String TEXT_SECTION = "@text";
	
	// data type
	final static String 
		TYPE_CUSTOM_SIZE = ".size",
		TYPE_CUSTOM_SIZE_ALIAS = ".space",
		TYPE_BYTE = ".byte",
		TYPE_HALFWORD = ".half",
		TYPE_HW_ALIAS = ".hword",
		TYPE_WORD = ".word",
		TYPE_STRING = ".ascii",
		TYPE_NT_STRING = ".asciz",
		TYPE_EXTERNAL_BLOB = ".blob"
	;
	
	// internal backed data structs
	private AsmLexer lexer;
	private TokenStream stream;
	
	public FLIREmitter(AsmLexer lexer) {
		this.lexer = lexer;
		this.lexer.scanTokens();
		this.stream = new TokenStream(this.lexer.tokens);
	}
	
	public List<TokenStream> lineTokens = new ArrayList<>();
	
	public FrontendCAIR emit() throws AsmError {
		// loop until the token stream is exhausted or EOF
		while (!stream.isAtEnd() && !stream.consumeIfMatch(TokenType.EOF)) {
			List<Token> line = new ArrayList<>();
			// turn each line into its own token stream
			while (!stream.consumeIfMatch(TokenType.NEWLINE)) {
				Token next = stream.peek();
				if (next.type() == TokenType.EOF) {
					break; // do not consume EOF, break to the outer loop
				}
				line.add(next);
				stream.advance();
			}
			lineTokens.add(new TokenStream(line)); // accumulates
		}
		this.parseLines();
		// box the results and return for codegen
		return new FrontendCAIR(
			collectedInstructions, // instruction (text)
			new IRDataSection( // data
				this.dataSymbols, // we may need the table later
				dataSectionBytes.toByteArray()
			)
		);
	}
	
	// result of the IR emitter is here
	private List<Instruction> collectedInstructions = new ArrayList<>();
	ByteArrayOutputStream dataSectionBytes = new ByteArrayOutputStream();
	
	// bookkeeping and result
	private ParsingContext context = ParsingContext.NONE;
	private Map<String, LabelText> labelAddresses = new HashMap<>();
	
	private void parseLines() throws AsmError {		
		for (TokenStream line : lineTokens) {
			if (line.isEmpty()) continue;
			Token first = line.peek();
			// switch context on directive
			if (first.type() == TokenType.DIRECTIVE_SECTION) {
				switch (first.literal()) {
					case DATA_SECTION -> context = ParsingContext.DATA_SECTION;
					case TEXT_SECTION -> context = ParsingContext.TEXT_SECTION;
					default -> throw new AsmError("Unknown directive: " + first.literal(), first);
				}
				continue;
			}
			// no directive, throw an error
			if (context == ParsingContext.NONE) {
				throw new AsmError(
					"Context Error. Encountered '" + first.literal() + "' before any section directive (@data or @text).",
					first
				);
			}
			if (context == ParsingContext.DATA_SECTION) {
				// only a single pass is needed for data
				this.parseDataSection(line);
				continue;
			}
			if (context == ParsingContext.TEXT_SECTION) {
				// this will build the first pass instruction IR
				this.parseTextSection(this.collectedInstructions, line);
				continue;
			}
		}
		// final resolve, this will emit CONTEXT AWARE IR (CAIR) for the backend generation
		this.resolveLabelAndVariableAddresses();
	}
	
	// DATA SECTION PARSING
	private Map<String, DataSymbol> dataSymbols = new HashMap<>();
	private int dataActivePointer = 0; // data section address counter

	private void parseDataSection(TokenStream line) {
		Token nameTok = line.advance();
		if (nameTok.type() != TokenType.IDENTIFIER) {
			throw new AsmError(
				"Expected data symbol name, got " + nameTok, 
				nameTok
			);
		}
		String name = nameTok.literal();
		if (dataSymbols.containsKey(name)) {
			throw new AsmError(nameTok,
				"Data symbol '%s' already defined on line %d",
				dataSymbols.get(name).owner().line()
			);
		}
		Token dirTok = line.advance();
		if (dirTok.type() != TokenType.DIRECTIVE) {
			throw new AsmError("Expected data directive after symbol name", dirTok);
		}
		
		int elementSize;
		int elementCount = 0;
		switch (dirTok.literal()) {
			case TYPE_BYTE, TYPE_HALFWORD, TYPE_HW_ALIAS, TYPE_WORD -> {
				elementSize = switch (dirTok.literal()) {
					case TYPE_BYTE -> 1; // duh
					case TYPE_HALFWORD, TYPE_HW_ALIAS -> HWORD_SIZE;
					case TYPE_WORD -> WORD_SIZE;
					default -> throw new AssertionError();
				};
				elementCount = this.collectInitializers(line, elementSize);
				if (elementCount == 0) {
					throw new AsmError(
						"Data symbol '" + name + "' requires at least one initializer.",
						dirTok
					);
				}
			}
			case TYPE_CUSTOM_SIZE_ALIAS, TYPE_CUSTOM_SIZE -> {
				Try.absorbAsm(
					() -> line.consume(TokenType.LPAREN, "Expected '(' after .size/.space"),
					line.peekNotNull()
				);
				elementSize = Try.absorbAsm(
					() -> foldExpression(line),
					line.peekNotNull()
				);
				Try.absorbAsm(
					() -> line.consume(TokenType.RPAREN, "Expected ')'"),
					line.peekNotNull()
				);
				elementCount = this.collectInitializers(line, elementSize);
				
				// you SHOULD NEVER DO .size(0) and then supply data, its stupid! 
				if (elementSize == 0 && elementCount > 0) {
					throw new AsmError(
						"Element size must be greater than zero when initializers are present.", 
						dirTok
					);
				}
				
				// if empty
				if (elementCount == 0 && elementSize > 0) { // allow for aliasing
					elementCount = 1;
					emitIntBE(0, elementSize); // emit one size'ed 0
				}
			}
			case TYPE_STRING, TYPE_NT_STRING -> {
				Token strToken = line.advance();
				if (strToken.type() != TokenType.STRING) {
					throw new AsmError(strToken,
						"%s/%s expects a string literal", 
						TYPE_STRING, TYPE_NT_STRING
					);
				}
				// remove the '""'
				String string = strToken.literal().substring(1, strToken.literal().length() - 1);
				boolean isAsciiz = dirTok.literal().equals(TYPE_NT_STRING);
				elementSize = 1;
				elementCount = string.length() + (isAsciiz ? 1 : 0);
				// emit the data
				for (char c : string.toCharArray()) {
					emitIntBE(c, elementSize);
				}
				if (isAsciiz) {
					emitIntBE('\0', elementSize); // term byte 
				}
			}
			case TYPE_EXTERNAL_BLOB -> {
				Token filePath = line.advance();
				if (filePath.type() != TokenType.STRING) {
					throw new AsmError(filePath,
						"%s expects a string for file path", 
						TYPE_EXTERNAL_BLOB
					);
				}
				// remove the '""'
				String path = filePath.literal().substring(1, filePath.literal().length() - 1);
				// read file
				elementSize = 1;
				try {
					byte[] blob = Files.readAllBytes(Path.of(path));
					elementCount = blob.length;
					// emit the data
					dataSectionBytes.write(blob);
				} catch (Exception e) {
					throw new AsmError(filePath, "Failed to read external blob file '%s': %s", path, e.getMessage());
				}
			}
			default -> throw new AsmError(
				"Unknown data directive '" + dirTok.literal() + "'", 
				dirTok
			);
		}
		
		// assign the allocation table
		DataSymbol symbol = new DataSymbol(
			name, dataActivePointer, 
			elementSize, 
			elementCount, 
			nameTok
		);
		dataSymbols.put(name, symbol);
		dataActivePointer += symbol.totalSize();
	}
	
	private int collectInitializers(TokenStream line, int size) {
		int count = 0;
		while (!line.isAtEnd()) {
			// constexpr everywhere!
			this.emitIntBE(Try.absorbAsm(() -> 
				FL32RSpecs.requireFitBits(
					size * 8, // size is in bytes
					foldExpression(line)
				), 
				line.peekNotNull()
			), size);
			line.consumeIfMatch(TokenType.COMMA);
			count++;
		}
		return count;
	}
	
	private void emitIntBE(int value, int size) {
		for (int i = size - 1; i >= 0; i--) {
			dataSectionBytes.write((int) (urshift(value, (i * 8) & 0xFF)));
		}
	}
	
	// TEXT SECTION PARSING
	private int collectPC = 0; // 1st pass program counter (collecting)
	private void parseTextSection(List<Instruction> collected, TokenStream line) {
		// collect labels
		if (line.consumeIfMatch(TokenType.LABEL)) {
			String labelName = line.previous().literal(); // name
			LabelText label = labelAddresses.get(labelName);
			if (label != null) {
				throw new AsmError(
					"Label already defined at line " + label.owner().line() + "!", 
					line.previous()
				);
			}
			// collect the token for error reporting too
			labelAddresses.put(labelName, new LabelText(
				labelName, this.collectPC, line.previous()
			));
		}
		
		if (line.isAtEnd()) return; // the label could've exhausted the stream 
		
		// parse the actual instruction
		Token opToken = line.advance();
		if (opToken.type() != TokenType.IDENTIFIER) {
			throw new AsmError(
				"Expected an opcode identifier, got " + opToken.type() + ".", 
				opToken
			);
		}
		String opcode = opToken.literal().toUpperCase();
		FEOpCode blueprint = FEOpCode.get(opcode);
		if (blueprint == null) {
			throw new AsmError(
				"Unknown opcode '" + opcode + "'", 
				opToken
			);
		}

		var expectedOperands = blueprint.getOperandKinds();
		var parsedOperands = new ArrayList<List<Token>>(3); // expected at most 3 elements
		// collect
		while (!line.isAtEnd()) {
			List<Token> cluster = new ArrayList<>(5); // worst case [R1 + CONST] -> 5 tokens
			// parse until either EOL or a comma
			while (!line.isAtEnd() && !line.consumeIfMatch(TokenType.COMMA)) {
				cluster.add(line.advance());
			}
			parsedOperands.add(cluster);
		}
		
		if (expectedOperands.length != parsedOperands.size()) {
			throw new AsmError(
				"Opcode '" + opcode + "' expects " + expectedOperands.length 
				+ " operands, but got " + parsedOperands.size() + ".", 
				opToken
			);
		}
		
		// this is the smelly part
		Operand[] operands = new Operand[expectedOperands.length];
		
		for (int i = 0; i < expectedOperands.length; ++i) {
			FEOperandType expected = expectedOperands[i];
			List<Token> parsed = parsedOperands.get(i);
			if (parsed.isEmpty()) {
				throw new AsmError(
					"Missing operand for opcode '" + opcode + "'.", 
					opToken
				);
			}
 			
			// less smelly thanks to java
			operands[i] = switch (expected) {
				// REGISTER: R1, R2, R3, ...
				case REG: { 
					if (parsed.size() != 1) {
						throw new AsmError(
							"Expected a single register operand (e.g. R1, R2, ...).", 
							parsed.get(0)
						);
					}
					Token first = parsed.get(0);
					if (first.type() != TokenType.IDENTIFIER) {
						throw new AsmError(
							"Expected a register", 
							first
						);
					}
					yield new RegisterOperand(
						Try.absorbAsm(() -> parseRegister(first.literal()), first)
					);
				}
				// $var, $var[offset]
				case VARIABLE:
				// $var, $var[offset] or label
				case VARIABLE_OR_LABEL: {
					// this is for some shorthand pseudo-op
					Token varToken = parsed.get(0);
					if (varToken.type() != TokenType.VAR) {
						if (expected == FEOperandType.VARIABLE_OR_LABEL) {
							// special case for variable & label (accept a label too)
							if (parsed.size() == 1 && varToken.type() == TokenType.IDENTIFIER) {
								yield new ImmLabel(
									varToken.literal(),
									32, true, // max width & pc rel
									varToken
								);
							}
							throw new AsmError(
								"Expected a defined variable/const or a label",
								varToken
							);
						}
						// normal case (not V&L)
						throw new AsmError(
							"Expected a defined variable/const",
							varToken
						);
					}
					String varName = varToken.literal().substring(1); // strip the $
					int offset = 0;
					// optional bracket
					TokenStream bracket = new TokenStream(parsed); // like one below
					bracket.advance(); // consume the var name
					if (bracket.consumeIfMatch(TokenType.LSQUARE)) {
						offset = Try.absorbAsm(() -> foldExpression(bracket), bracket.peekNotNull());
						Try.absorbAsm(() -> bracket.consume(TokenType.RSQUARE, 
							"Expected a closing ']' in variable offset operand!"
						), bracket.peekNotNull());
					}
					// a variable is always pc relative, regardless
					yield new ImmVariable(
						varName, 
						offset, 
						32, true, // max width, true = pcRelative
						varToken
					);
				}
				case MEMORY: {
					// just the register, no offset at all
					// this is for the old spec: LDW/STW R1, R2
					if (parsed.size() == 1) {
						Token first = parsed.get(0);
						if (first.type() != TokenType.IDENTIFIER) {
							throw new AsmError(
								"Expected a single register operand (e.g. R1, R2, ...).", 
								first
							);
						}
						yield new MemoryOperand(new RegisterOperand(
							Try.absorbAsm(() -> parseRegister(first.literal()), first)
						), new ImmLiteral(0));
					}
					// bracket
					TokenStream bracket = new TokenStream(parsed); // special 
					Try.absorbAsm(
						() -> bracket.consume(TokenType.LSQUARE, "Expected '[' to start memory operand (e.g. [R1] or [R1 + 4])!"),
						bracket.peekNotNull()
					);
					// this will throw if mismatch
					Token baseRegToken = bracket.advance();
					RegisterOperand base = new RegisterOperand(
						Try.absorbAsm(() -> parseRegister(baseRegToken.literal()), baseRegToken)
					); 
					ImmLiteral offset = null;
					if (!bracket.peekMatch(TokenType.RSQUARE)) {
						Token offsetToken = bracket.advance(); // consume it
						if (!offsetToken.is(TokenType.PLUS, TokenType.MINUS)) {
							throw new AsmError(
								"Memory operand offset must use '+' or '-' relative to the base register",
								offsetToken
							);
						}
						// at this point the pointer is at the start of the equation to fold
						offset = new ImmLiteral(
							Try.absorbAsm(() -> foldExpression(bracket), bracket.peekNotNull()) * 
							(offsetToken.is(TokenType.MINUS) ? -1 : 1)
						); 
					} else {
						offset = new ImmLiteral(0); // no offset, standard syntax
					}
					Try.absorbAsm(
						() -> bracket.consume(TokenType.RSQUARE, "Expected a closing ']' in memory operand!"),
						bracket.peekNotNull()
					);
					yield new MemoryOperand(base, offset);
				}
				case IMM14_ABS: case IMM16_ABS: case IMM19_ABS:
				case IMM24_ABS: case IMM24_PC_REL: // PC relative
				case IMM32_ABS: {
					// if size == 1 & identifier -> must be label
					// or variable ($var) -> address to the variable
					if (parsed.size() == 1) { 
						Token label = parsed.get(0);
						if (label.type() == TokenType.IDENTIFIER) { // labels
							yield new ImmLabel(
								label.literal(),
								expected.bitWidth, // prevent overshooting
								expected.pcRelative,
								label
							);
						} else if (label.type() == TokenType.VAR) { // variables (ONLY address)
							yield new ImmVariable(
								label.literal().substring(1), 
								0, // cant do the offsetting here
								expected.bitWidth, // prevent overshooting
								expected.pcRelative, // yes you can jump into a variable (if you try)
								label
							);
						}
					}
					// inlined value (allow for expressions)
					int value = Try.absorbAsm(() -> 
						requireFitBits(
							expected.bitWidth, 
							foldExpression(new TokenStream(parsed))
						),
						parsed.get(0)
					);
					yield new ImmLiteral(value);
				}
				default: {
					throw new AsmError("Not implemented", opToken);
				} // never happen, istg
			};
		}
		
		// collect the parsed instruction IR
		Instruction instruction = new Instruction(blueprint, operands);
		collected.add(instruction);
		this.collectPC += instruction.getSize();
	}
	
	// FINALIZE (we dont have a linker so this is the final result)
	private void resolveLabelAndVariableAddresses() {
		int resolvePC = 0; // 2nd pass PC
		for (var instruction : this.collectedInstructions) {
			// FL32R spec: the cpu increments PC right after FETCH
			// so when it executes, the PC IS ALREADY ON THE NEXT INSTRUCTION
			int execPC = resolvePC + instruction.getSize(); // PC after fetch
			var operands = instruction.operands;
			for (int i = 0; i < operands.length; ++i) {
				var operand = operands[i];
				var operandType = instruction.opcode.getOperandKinds()[i];
				// dissolve labels
				if (operand instanceof ImmLabel ilabel) {
					String labelStr = ilabel.label();
					LabelText label = labelAddresses.get(labelStr); // resolve the label
					if (label == null) {
						throw new AsmError(
							"Unknown label: '" + labelStr + "'", 
							ilabel.owner()
						);
					}
					int labelValue;
					if (ilabel.pcRelative()) {
						// PC relative (rules apply)
						labelValue = label.address() - execPC;
					} else {
						// raw address
						labelValue = label.address();
					}
					// prevent value overshooting
					operands[i] = new ImmLiteral(Try.absorbAsm(() -> 
						requireFitBits(
							ilabel.expectedBitWidth(), 
							labelValue
						),
						ilabel.owner()
					));
					continue;
				}
				// dissolve variables
				if (operand instanceof ImmVariable ivar) {
					String varName = ivar.varname();
					DataSymbol symbol = dataSymbols.get(varName); // resolve the variable
					if (symbol == null) {
						throw new AsmError(
							"Unknown variable: '" + varName + "'", 
							ivar.owner()
						);
					}
					int elemOffset = (collectPC + symbol.addressOffset()) + ivar.offset() * symbol.elementSize();
					// note: only allow to dissolve to a memory operand
					// if the operand type is a real "variable"
					// ex: LD  R0, $variable
					if (operandType == FEOperandType.VARIABLE && ivar.pcRelative()) {
						// compute the pc relative for actual pc rel operands (and variable)
						int pcRelOffset = elemOffset - execPC;
						operands[i] = new SizedMemoryOperand(
							new MemoryOperand(
								RegisterOperand.PC_REG, 
								new ImmLiteral(pcRelOffset)
							),
							symbol.elementSize()
						);
					} else if (ivar.pcRelative()) { 
						// not variable optype BUT still pc rel, we just
						// dissolve into the same code as a label would
						// ex: JMP $variable ; what
						int pcRelOffset = elemOffset - execPC;
						operands[i] = new ImmLiteral(Try.absorbAsm(() -> 
							requireFitBits(
								ivar.expectedBitWidth(), 
								pcRelOffset
							),
							ivar.owner()
						));
					} else {
						// you can do non variable ops with a variable, 
						// like: "ADDI R0, $variable" 
						// its OK!, $variable is then treated as just a label (resolve
						// absolute address)
						operands[i] = new ImmLiteral(Try.absorbAsm(() -> 
							requireFitBits(
								ivar.expectedBitWidth(), 
								elemOffset
							),
							ivar.owner()
						));
					}
				}
			}
			resolvePC = execPC; // update the PC, this is like a smol emu
		}
	}
} 
