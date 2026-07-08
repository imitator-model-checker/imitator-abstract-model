package com.imitatorModel.imitatorModel;

public enum Operator {
    LT("<"),
    LE("<="),
    EQ("="),
    GE(">="),
    GT(">"),
    NE("<>");

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

    public  Operator getInverse() {
        switch (this) {
            case LT: return GE;
            case LE: return GT;
            case EQ: return NE;
            case NE: return EQ;
            case GE: return LT;
            case GT: return LE;
            default: throw new IllegalStateException("Unexpected operator: " + this);
        }
    }
}
