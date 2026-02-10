package dev.gkvn.cpu.assembler.fl32r.frontend.lexer;

import java.util.ArrayList;
import java.util.List;

public class AsmLexer {
	private final String source;
	public final String[] sourceLines;
	public final List<Token> tokens = new ArrayList<>();

	// bookkeeping
	private int start = 0; // the beginning of a token
	private int current = 0;
	
	private int line = 1;
	
	private int startColumn = 0;
	private int column = 0;

	// assign a source for this lexer
	public AsmLexer(String source) {
		this.source = source;
		this.sourceLines = source.split("\\R", -1);
	}

	public void scanTokens() {
		// loop until the thing has not ended, if not ended, assign the
		// current char index to the current one, and then scan the token
		while (!isAtEnd()) {
			start = current;
			startColumn = column;
			scanToken();
		}
		
		// end of file
		tokens.add(new Token(TokenType.EOF, "", line, column));
	}
	
	private void scanToken() {
		char c = advance();
		switch (c) {
			case '(':
				addToken(TokenType.LPAREN);
				break;
			case ')':
				addToken(TokenType.RPAREN);
				break;
			case '[':
				addToken(TokenType.LSQUARE);
				break;
			case ']':
				addToken(TokenType.RSQUARE);
				break;
			case '+':
				addToken(TokenType.PLUS);
				break;
			case '-':
				addToken(TokenType.MINUS);
				break;
			case '*':
				addToken(TokenType.STAR);
				break;
			case '/':
				addToken(TokenType.SLASH);
				break;
			case '%':
				addToken(TokenType.MOD);
				break;
			case '>':
				if (match('>')) {
					addToken(TokenType.BSR);
				}
				break;
			case '<':
				if (match('<')) {
					addToken(TokenType.BSL);
				}
				break;
			case ',':
				addToken(TokenType.COMMA);
				break;
			case ' ':
			case '\r':
			case '\t':
				break; // ignore whitespaces
			case '\n':
				addToken(TokenType.NEWLINE);
				column = 0;
				line++; // next line
				break;
			case '\'':
				charLiteral();
				break;
			case '"':
				stringLiteral();
				break;
			case '@':
				directive();
				break;
			case '.':
				dottedIdentifier();
				break;
			case '$':
				variable();
				break;
			default: {
				// if the value is digit, hand over the control to number();
				if (isDigit(c)) {
					number();
				} else if (isAlpha(c)) { // if found char, handle identifier
					identifier();
				} else {
					throw new RuntimeException("Unexpected character: " + c); // TODO
				}
			}
		}
	}
	
	/**
	 * Scan the next directive
	 */
	private void dottedIdentifier() {
		while ( isAlphaNumeric(peek()) ) {
			advance();
		}
		addToken(TokenType.DIRECTIVE);
	}
	
	/**
	 * Scan the next directive
	 */
	private void variable() {
		while ( isAlphaNumeric(peek()) ) {
			advance();
		}
		// at this point, the current pointer would point at
		// @hello|
		//       ^ here
		addToken(TokenType.VARIABLE);
	}
	
	/**
	 * Scan the next directive
	 */
	private void directive() {
		while ( isAlphaNumeric(peek()) ) {
			advance();
		}
		// at this point, the current pointer would point at
		// @hello|
		//       ^ here
		addToken(TokenType.DIRECTIVE_SECTION);
	}
	
	/**
	 * Scan the next identifier
	 */
	private void identifier() {
		while ( isAlphaNumeric(peek()) ) {
			advance();
		}
		// at this point, the current pointer would point at
		// hello|
		//      ^ here
		if (peek() == ':') { // Label found!
			addToken(TokenType.LABEL);
			advance(); // drop the ':' after capturing the label
			return;
		}
		addToken(TokenType.IDENTIFIER);
	}
	
	/**
	 * Scan the next number
	 */
	private void number() {
	    if (peek() == 'x' || peek() == 'X') {
	        // handle hexadecimal
	        advance(); // consume 'x' or 'X'
	        while (isHexDigit(peek())) {
	            advance();
	        }
	    } else if (peek() == 'b' || peek() == 'B') {
	        // handle binary
	        advance(); // consume 'b' or 'B'
	        while (isBinaryDigit(peek())) {
	            advance();
	        }
	    } else {
	        // handle normal decimal
	        while (isDigit(peek())) {
	            advance();
	        }
	    }
        addToken(TokenType.NUMBER);
	}
	
	private void charLiteral() {
		if (isAtEnd()) throw new RuntimeException("Unterminated character literal");
		char c = advance();
		// handle escape characters like '\n' or '\''
		if (c == '\\') {
			if (isAtEnd()) throw new RuntimeException("Unterminated escape in character literal");
			advance(); // skip the escaped char
		}
		if (peek() != '\'') throw new RuntimeException("Unterminated character literal or too many characters");
		advance(); // consume closing '
		addToken(TokenType.CHAR);
	}
	
	private void stringLiteral() {
		while (!isAtEnd() && peek() != '"') {
			if (peek() == '\n') line++; // allow multi-line strings if desired
			if (peek() == '\\') advance(); // skip escape prefix
			advance();
		}
		if (isAtEnd()) throw new RuntimeException("Unterminated string literal");
		advance(); // consume closing quote
		addToken(TokenType.STRING);
	}
	
	// Utility functions
	private boolean isAtEnd() {
		return current >= source.length();
	}
	
	/**
	 * @return same thing as peek(), but advances the pointer
	 */
	private char advance() {
		column++;
		return source.charAt(current++);
	}
	
	/**
	 * @return the character at the current pointer, nullchar (\0) if at end
	 */
	private char peek() {
		return isAtEnd() ? '\0' : source.charAt(current);
	}
	
	/**
	 * If the character matches, advance forward (using advance())
	 * @param expected
	 * @return true if matches
	 */
	public boolean match(char expected) {
		if (isAtEnd() || peek() != expected) {
			return false;
		}
		this.advance();
		return true;
	}

	private void addToken(TokenType type) {
		// "start" is the beginning of a token (set at each interval of scanTokens())
		String literal = source.substring(start, current);
		tokens.add(new Token(type, literal, line, startColumn));
	}

	/**
	 * @return true if number (digit 0-9)
	 */
	private boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}
	
	/**
	 * @return true if A-F or a-f
	 */
	private boolean isHexDigit(char c) {
	    return (c >= '0' && c <= '9') ||
	           (c >= 'a' && c <= 'f') ||
	           (c >= 'A' && c <= 'F');
	}
	
	/**
	 * @return true if 0 or 1
	 */
	private boolean isBinaryDigit(char c) {
	    return c == '0' || c == '1';
	}

	
	/**
	 * @return true if character (non-whitespace)
	 */
	private boolean isAlpha(char c) {
		return Character.isLetter(c) || c == '_';
	}
	
	/**
	 * @return true if either character or digit (a-Z, 0-9)
	 */
	private boolean isAlphaNumeric(char c) {
		return isAlpha(c) || isDigit(c);
	}
}
