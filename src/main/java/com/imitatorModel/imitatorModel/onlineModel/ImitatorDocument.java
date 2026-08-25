package com.imitatorModel.imitatorModel.onlineModel;

import java.util.*;

public class ImitatorDocument {

    private String header = "";
    private String variables = "";
    private String initialState = "";

    private final Map<String, PTADocument> ptas =
            new LinkedHashMap<>();

    public void setHeader(String header) {
        this.header = header;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }

    public void setInitialState(String initialState) {
        this.initialState = initialState;
    }

    public void addPTA(
            String name,
            PTADocument pta) {

        if (ptas.containsKey(name)) {
            throw new IllegalArgumentException(
                    "PTA already exists: " + name);
        }

        ptas.put(name, pta);
    }

    public PTADocument getPTA(String name) {

        PTADocument pta = ptas.get(name);

        if (pta == null) {
            throw new IllegalArgumentException(
                    "Unknown PTA: " + name);
        }

        return pta;
    }

    public String render() {

        StringBuilder sb = new StringBuilder();

        sb.append(header);
        sb.append(variables);

        for (PTADocument pta : ptas.values()) {
            sb.append(pta.render());
            sb.append("\n");
        }

        sb.append(initialState);

        return sb.toString();
    }
}