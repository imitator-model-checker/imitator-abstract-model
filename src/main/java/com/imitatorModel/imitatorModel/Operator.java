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

    public  Operator getInverse() {
        switch (this) {
            case LT: return GT;
            case LE: return GE;
            case EQ: return EQ;
            case GE: return LE;
            case GT: return LT;
            default: throw new IllegalStateException("Unexpected operator: " + this);
        }
    }
}
