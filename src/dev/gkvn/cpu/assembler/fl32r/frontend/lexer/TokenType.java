package dev.gkvn.cpu.assembler.fl32r.frontend.lexer;

public enum TokenType {
	IDENTIFIER, // anything, even opcode
	NUMBER, // number, binary, hex, decimal.
	STRING, // "s t r i n g"
	CHAR, // 'c'
	LABEL, // label:
	DEFINE_REF, // =defineRef
	DIRECTIVE, // .directive 
	DIRECTIVE_SECTION, // @data, @text
	VAR, // $var
	PLUS, MINUS, STAR, SLASH, MOD, BSR, BSL, // +-*/
	LSQUARE, // [
	RSQUARE, // ]
	LPAREN, // (
	RPAREN, // )
	COMMA, // ,
	NEWLINE, // \n
	EOF, // end of file
}
