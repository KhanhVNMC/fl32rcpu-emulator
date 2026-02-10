package dev.gkvn.cpu.assembler.fl32r.frontend.utils;

import java.util.List;

import dev.gkvn.cpu.assembler.fl32r.frontend.lexer.Token;
import dev.gkvn.cpu.assembler.fl32r.frontend.lexer.TokenType;

public class TokenStream {
	private int currentToken;
	private List<Token> tokens;
	
	public TokenStream(List<Token> tokenList) {
		this.tokens = tokenList;
	}
	
	public List<Token> getTokensList() {
		return tokens;
	}
	
	public int getPointer() {
		return currentToken;
	}
	
	public void setPointer(int pointer) {
		this.currentToken = pointer;
	}
	
	public boolean isEmpty() {
		return this.tokens.isEmpty();
	}
	
	public int length() {
		return this.tokens.size();
	}
	
	/**
	 * @return true if the pointer is at the end of the token stream
	 */
	public boolean isAtEnd() {
		return this.currentToken >= tokens.size();
	}
	
	/**
	 * @return the previous token (the one before the current pointer)
	 */
	public Token previous() {
		if (currentToken == 0) throw new IllegalArgumentException("Pointer at 0!");
		return tokens.get(currentToken - 1);
	}
	
	/**
	 * @return the token at the current pointer, null if at end
	 */
	public Token peek() {
		return peekAhead(0);
	}
	
	/**
	 * @return the token at the current pointer, the previous token if at end
	 */
	public Token peekNotNull() {
		var peek = peekAhead(0);
		return peek != null ? peek : previous();
	}
	
	/**
	 * @param steps steps to look ahead, 0 is the same as peek()
	 * @return the token at the current pointer, null if at end
	 */
	public Token peekAhead(int steps) {
		return currentToken + steps >= this.tokens.size() ? null : this.tokens.get(currentToken + steps);
	}
	
	/**
	 * @return same as peek(), but advances the pointer
	 */
	public Token advance() {
		return this.tokens.get(currentToken++);
	}
	
	/**
	 * @return true if the token at the pointer is the same as the given
	 * type, false if the pointer is at the end of the stream
	 * 
	 * Same as peek().type == type
	 */
	public boolean check(TokenType type) {
		return !isAtEnd() && peek().type() == type;
	}
	
	/**
	 * Checks whether the upcoming tokens (starting at the current pointer) match
	 * the exact sequence of given types.
	 *
	 * Example: peekMatch(TokenType.IDENTIFIER, TokenType.LPAREN) -> returns true if
	 * the current token is IDENTIFIER and the next is LPAREN.
	 *
	 * @param types the sequence of token types to test against
	 * @return true if the next tokens exactly match the given sequence, false
	 *         otherwise
	 */
	public boolean peekMatch(TokenType... types) {
		int look = currentToken; 
		for (TokenType type : types) {
			if (look >= tokens.size() || tokens.get(look).type() != type) {
				return false;
			}
			look++;
		}
		return true;
	}
	
	/**
	 * If the current token matches one of the given types, 
	 * advance the pointer and return true. Otherwise, return false.
	 */
	public boolean consumeIfMatch(TokenType... types) {
		for (TokenType type : types) {
			if (check(type)) {
				advance();
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Consume a token (advances pass it), if does not match, throw an error
	 * 
	 * @param type the type of token expected to consume
	 * @param errorIfNotMatch error message
	 * @return the consumed token
	 */
	public Token consume(TokenType type, String errorIfNotMatch) {
		// if token matches
		if (check(type)) {
			// advance pointer
			return advance();
		}
		throw new RuntimeException(errorIfNotMatch);
	}
	
	@Override
	public String toString() {
		return tokens.toString();
	}
}
