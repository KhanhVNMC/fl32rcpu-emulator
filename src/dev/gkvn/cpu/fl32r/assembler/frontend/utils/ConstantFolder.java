package dev.gkvn.cpu.fl32r.assembler.frontend.utils;

import dev.gkvn.cpu.fl32r.assembler.frontend.FLIREmitter;
import dev.gkvn.cpu.fl32r.assembler.frontend.core.DataSymbol;
import dev.gkvn.cpu.fl32r.assembler.frontend.core.DefineValue;
import dev.gkvn.cpu.fl32r.assembler.frontend.exceptions.AsmError;
import dev.gkvn.cpu.fl32r.assembler.frontend.exceptions.FrontendSolveError;
import dev.gkvn.cpu.fl32r.assembler.frontend.lexer.Token;
import dev.gkvn.cpu.fl32r.assembler.frontend.lexer.TokenType;

import static dev.gkvn.cpu.fl32r.assembler.frontend.FLIREmitter.*;

public class ConstantFolder {
	private FLIREmitter context;
	
	public ConstantFolder(FLIREmitter context) {
		this.context = context;
	}
	
	public ConstantFolder() {
		this(null);
	}
	
	public int foldExpression(TokenStream stream) throws FrontendSolveError {
		return parseBitwiseOperations(stream);
	}
	
	private int parseBitwiseOperations(TokenStream stream) throws FrontendSolveError {
		int lvalue = parseBitShifting(stream);
		while (stream.consumeIfMatch(TokenType.AND, TokenType.OR, TokenType.XOR)) {
			Token op = stream.previous();
			int rvalue = parseBitShifting(stream);
			lvalue = switch (op.type()) {
				case AND -> lvalue & rvalue;
				case OR -> lvalue | rvalue;
				case XOR -> lvalue ^ rvalue;
				default -> throw new AssertionError();
			};
		}
		return lvalue;
	}
	
	private int parseBitShifting(TokenStream stream) throws FrontendSolveError {
		int lvalue = parseAdditive(stream);
		while (stream.consumeIfMatch(TokenType.BSR, TokenType.BSL)) {
			Token op = stream.previous();
			int rvalue = parseAdditive(stream);
			lvalue = switch (op.type()) {
				case BSR -> lvalue >>> rvalue;
				case BSL -> lvalue << rvalue;
				default -> throw new AssertionError();
			};
		}
		return lvalue;
	}
	
	private int parseAdditive(TokenStream stream) throws FrontendSolveError {
		int lvalue = parseMultiplicative(stream);
		while (stream.consumeIfMatch(TokenType.PLUS, TokenType.MINUS)) {
			Token op = stream.previous();
			int rvalue = parseMultiplicative(stream);
			lvalue = switch (op.type()) {
				case PLUS -> lvalue + rvalue;
				case MINUS -> lvalue - rvalue;
				default -> throw new AssertionError();
			};
		}
		return lvalue;
	}
	
	private int parseMultiplicative(TokenStream stream) throws FrontendSolveError {
		int lvalue = parseUnary(stream);
		while (stream.consumeIfMatch(
			TokenType.STAR, TokenType.SLASH, TokenType.MOD
		)) {
			Token op = stream.previous();
			int rvalue = parseUnary(stream);
			lvalue = switch (op.type()) {
				case STAR -> lvalue * rvalue;
				case SLASH -> lvalue / rvalue;
				case MOD -> lvalue % rvalue;
				default -> throw new AssertionError();
			};
		}
		return lvalue;
	}
	
	private int parseUnary(TokenStream stream) throws FrontendSolveError {
		if (stream.consumeIfMatch(TokenType.PLUS)) return parseUnary(stream);
		if (stream.consumeIfMatch(TokenType.MINUS)) return -parseUnary(stream);
		if (stream.consumeIfMatch(TokenType.NOT)) return ~parseUnary(stream);
		return parsePrimary(stream);
	}
	
	private int parsePrimary(TokenStream stream) throws FrontendSolveError {
		// macro functions for shit and giggles
		if (stream.consumeIfMatch(TokenType.IDENTIFIER)) {
			String macroFunc = stream.previous().literal();
			if (macroFunc.equals(SIZEOF_FUNCTION) || macroFunc.equals(LENGTH_FUNCTION)) {
				stream.consume(TokenType.LPAREN, "Expected '(' after macro function '" + macroFunc + "'");
				String varName = stream.consume(
					TokenType.VAR, "Expected a defined variable in macro function '" + macroFunc + "'"
				).literal().substring(1);
				DataSymbol symbol = context.dataSymbols.get(varName); // resolve the variable
				if (symbol == null) {
					throw new AsmError(
						"Unknown variable: '" + varName + "'", 
						stream.previous()
					);
				}
				stream.consume(TokenType.RPAREN, "Expected ')' to close '" + macroFunc + "'");
				return macroFunc.equals(LENGTH_FUNCTION) ? symbol.elementCount() : symbol.elementSize();
			}
		}
		if (stream.consumeIfMatch(TokenType.DEFINE_REF)) {
			String symName = stream.previous().literal();
			symName = symName.substring(1, symName.length());
			// read and match the symbol
			DefineValue value = context.definedValues.get(symName);
			if (value == null) {
				throw new RuntimeException("Undefined symbol '" + symName + "'.");
			}
			if (value.isString()) {
				throw new RuntimeException("Defined symbol '" + symName + "' is a string and cannot be used in a numeric expression.");
			}
			return value.getNumber();
		}
		if (stream.consumeIfMatch(TokenType.NUMBER)) {
			return FL32RSpecs.toNumber(stream.previous().literal());
		}
		if (stream.consumeIfMatch(TokenType.CHAR)) {
			return stream.previous().literal().charAt(1);
		}
		if (stream.consumeIfMatch(TokenType.LPAREN)) {
			int value = foldExpression(stream);
			stream.consume(TokenType.RPAREN, "Expected ')'");
			return value;
		}
	    throw new RuntimeException("Expected constant integer!");
	}
}
