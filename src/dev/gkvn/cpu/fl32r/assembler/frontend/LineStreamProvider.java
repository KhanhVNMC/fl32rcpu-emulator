package dev.gkvn.cpu.fl32r.assembler.frontend;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

import dev.gkvn.cpu.fl32r.assembler.frontend.lexer.AsmLexer;
import dev.gkvn.cpu.fl32r.assembler.frontend.lexer.Token;
import dev.gkvn.cpu.fl32r.assembler.frontend.lexer.TokenType;
import dev.gkvn.cpu.fl32r.assembler.frontend.utils.TokenStream;

public class LineStreamProvider {
	private final Stack<Stack<TokenStream>> streams = new Stack<>();
	private final Stack<Path> includeStack = new Stack<>();
	private final Set<Path> activeIncludes = new HashSet<>();
	
	private AsmLexer initial;
	/**
	 * Creates a provider from an initial lexer source
	 */
	public LineStreamProvider(AsmLexer initial) {
		this.pushSource(this.initial = initial);
	}
	
	public AsmLexer getInitial() {
		return initial;
	}
	
	/**
	 * Pushes a new lexer source onto the provider stack
	 */
	public boolean pushSource(AsmLexer newSource) {
		Path path = newSource.getSourcePath();
		if (activeIncludes.contains(path)) {
			StringBuilder sb = new StringBuilder("Circular .include found: ");
			for (Path p : includeStack) {
				sb.append(p).append(" -> ");
			}
			sb.append(path);
			throw new RuntimeException(sb.toString());
		}
		
		streams.push(tokenStreamsFromLexer(newSource));
		includeStack.push(path);
		activeIncludes.add(path);
		return true;
	}
	
	/**
	 * Removes and returns the current source stack
	 */
	private void popSource() {
		streams.pop();
		activeIncludes.remove(includeStack.pop());
	}

	/**
	 * Returns the next available logical line as a {@link TokenStream}
	 */
	public TokenStream nextLine() {
		if (isExhausted()) {
			throw new NoSuchElementException("Malformed implementation");
		}
		Stack<TokenStream> current = streams.peek();
		var toReturn = current.pop();
		if (current.isEmpty()) {
			this.popSource();
		}
		return toReturn;
	}
	
	/**
	 * @return {@code true} if no more sources or lines remain
	 */
	public boolean isExhausted() {
		return streams.isEmpty();
	}
	
	/**
	 * Converts a provided lexer into a stack of {@link TokenStream}s.
	 * Each line corresponds to tokens between newline boundaries
	 */
	private Stack<TokenStream> tokenStreamsFromLexer(AsmLexer lexer) {
		TokenStream stream = new TokenStream(lexer.scanTokens());
		List<TokenStream> lineTokens = new ArrayList<>();
		
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
		
		// god fucking help me
		Collections.reverse(lineTokens);
		Stack<TokenStream> stack = new Stack<>();
		lineTokens.forEach(stack::push);
		
		return stack;
	}
}
