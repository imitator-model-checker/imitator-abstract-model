package com.imitatorModel.imitatorModel;

public enum Operator {
    LT("<"),
    LE("<="),
    EQ("="),
    GE(">="),
    GT(">");

    private final String symbol;

    Operator(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

	public String toIMITATOR(){
		return symbol;
	}
}
