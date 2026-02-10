package dev.gkvn.cpu.assembler.fl32r.frontend.utils;

import dev.gkvn.cpu.assembler.fl32r.frontend.exceptions.BackendError;
import dev.gkvn.cpu.assembler.fl32r.frontend.lexer.Token;
import dev.gkvn.cpu.assembler.fl32r.frontend.lexer.TokenType;

public class ConstantFolder {
	public static int foldExpression(TokenStream stream) throws BackendError {
		return parseBitShifting(stream);
	}
	
	private static int parseBitShifting(TokenStream stream) throws BackendError {
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
	
	private static int parseAdditive(TokenStream stream) throws BackendError {
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
	
	private static int parseMultiplicative(TokenStream stream) throws BackendError {
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
	
	private static int parseUnary(TokenStream stream) throws BackendError {
		if (stream.consumeIfMatch(TokenType.PLUS)) return parseUnary(stream);
		if (stream.consumeIfMatch(TokenType.MINUS)) return -parseUnary(stream);
		return parsePrimary(stream);
	}
	
	private static int parsePrimary(TokenStream stream) throws BackendError {
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
