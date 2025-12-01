package dev.gkvn.cpu.assembler.fl32r.lexer;

public enum TokenType {
	IDENTIFIER, // anything, even opcode
	NUMBER, // number, binary, hex, decimal.
	STRING, // "s t r i n g"
	CHAR, // 'c'
	LABEL, // label:
	DIRECTIVE_DATA, // .type 
	DIRECTIVE_SECTION, // @data, @text
	PLUS, MINUS, STAR, SLASH, MOD, // +-*/
	LSQUARE, // [
	RSQUARE, // ]
	COMMA, // ,
	NEWLINE, // \n
	EOF, // end of file
}
