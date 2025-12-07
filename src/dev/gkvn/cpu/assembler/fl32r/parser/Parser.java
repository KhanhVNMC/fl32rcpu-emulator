package dev.gkvn.cpu.assembler.fl32r.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.gkvn.cpu.assembler.fl32r.lexer.AsmLexer;
import dev.gkvn.cpu.assembler.fl32r.lexer.Token;
import dev.gkvn.cpu.assembler.fl32r.lexer.TokenType;

public class Parser {
	final static String DATA_SECTION = "@data";
	final static String TEXT_SECTION = "@text";
	
	private AsmLexer lexer;
	private TokenStream stream;
	
	public Parser(AsmLexer lexer) {
		this.lexer = lexer;
		this.lexer.scanTokens();
		this.stream = new TokenStream(this.lexer.tokens);
	}
	
	public List<TokenStream> lineTokens = new ArrayList<>();
	
	public void parse() {
		// loop until the token stream is exhaust or EOF
		while (!stream.isAtEnd() && !stream.consumeIfMatch(TokenType.EOF)) {
			List<Token> line = new ArrayList<>();
			// turn each line into its own token stream
			while (!stream.consumeIfMatch(TokenType.NEWLINE)) {
				Token next = stream.peek();
				if (next.type == TokenType.EOF) {
					break; // do not consume EOF, break to the outer loop
				}
				line.add(next);
				stream.advance();
			}
			lineTokens.add(new TokenStream(line)); // accumulates
		}
		this.parseLines();
	}
	
	private ParsingContext context = ParsingContext.NONE;
	private Map<String, Long> labelToAddress = new HashMap<>();
	private long currentAddress = 0;
	
	private void parseLines() {
		for (TokenStream line : lineTokens) {
			if (line.isEmpty()) continue;
			Token first = line.peek();
			// switch context on directive
			if (first.type == TokenType.DIRECTIVE_SECTION) {
				switch (first.literal) {
					case DATA_SECTION -> context = ParsingContext.DATA_SECTION;
					case TEXT_SECTION -> context = ParsingContext.TEXT_SECTION;
					default -> throw new RuntimeException("Unknown directive: " + first.literal);
				}
				continue;
			}
			// no directive, throw an error
			if (context == ParsingContext.NONE) {
				throw new RuntimeException(
					"Encountered '" + first.literal + "' before any section directive (@data or @text)."
				);
			}
			
			if (context == ParsingContext.DATA_SECTION) {
				this.parseDataSection(line);
				continue;
			}
			
			if (context == ParsingContext.TEXT_SECTION) {
				this.parseTextSection(line);
				continue;
			}
		}
	}
	
	private void parseDataSection(TokenStream line) {
	}
	
	private void parseTextSection(TokenStream line) {
	}
}
