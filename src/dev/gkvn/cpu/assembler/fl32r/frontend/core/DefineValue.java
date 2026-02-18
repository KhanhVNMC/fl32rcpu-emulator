package dev.gkvn.cpu.assembler.fl32r.frontend.core;

import dev.gkvn.cpu.assembler.fl32r.frontend.lexer.Token;

public final class DefineValue {
	private final String stringVal;
	private final int numberVal;
	private final Token owner;
	
	public DefineValue(Token owner, int value) {
		this.numberVal = value;
		this.stringVal = null;
		this.owner = owner;
	}
	
	public DefineValue(Token owner, String string) {
		this.numberVal = 0;
		this.stringVal = string;
		this.owner = owner;
	}
	
	public boolean isString() {
		return this.stringVal != null;
	}
	
	public Token owner() {
		return owner;
	}
	
	public String getString() {
		return stringVal;
	}
	
	public int getNumber() {
		return numberVal;
	}
	
	@Override
	public String toString() {
		if (isString()) return "DefineString(" + stringVal + ")";
		return "DefineNumeric(" + numberVal + ")";
	}
}
