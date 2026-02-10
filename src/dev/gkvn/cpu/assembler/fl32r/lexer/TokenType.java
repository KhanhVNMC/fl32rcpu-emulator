package dev.gkvn.cpu.assembler.fl32r.lexer;

public enum TokenType {
	IDENTIFIER, // anything, even opcode
	NUMBER, // number, binary, hex, decimal.
	STRING, // "s t r i n g"
	CHAR, // 'c'
	LABEL, // label:
	DOTTED_IDENTIFIER, // .identifier 
	DIRECTIVE_SECTION, // @data, @text
	VARIABLE, // $var
	PLUS, MINUS, STAR, SLASH, MOD, BSR, BSL, // +-*/
	LSQUARE, // [
	RSQUARE, // ]
	LPAREN, // (
	RPAREN, // )
	COMMA, // ,
	NEWLINE, // \n
	EOF, // end of file
}
