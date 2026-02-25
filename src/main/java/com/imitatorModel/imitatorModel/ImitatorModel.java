/**
 * Defines the ImitatorModel class, which represents a model for the IMITATOR tool. It contains a list of variables and a list of PTAs (Parametric Timed Automata). The class provides methods to add variables and PTAs, and to generate the IMITATOR representation of the model.
 */

package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.List;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ImitatorModel {
    private List<Variable> variables;
    private List<PTA> ptas;

    public ImitatorModel() {
        this.variables = new ArrayList<>();
        this.ptas = new ArrayList<>();
    }

    public void addVariable(Variable variable) {
        variables.add(variable);
    }

    public void addVariables(List<Variable> variables) {
        variables.addAll(variables);
    }

    public void addPTA(PTA pta) {
        ptas.add(pta);
    }

    public List<Variable> getVariables() {
        return variables;
    }

    public List<PTA> getPTAs() {
        return ptas;
    }

    public String toIMITATOR() {
        StringBuilder sb = new StringBuilder();
        // Get the current date and time
        ZonedDateTime now = ZonedDateTime.now();
        // Define the desired format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
        // Convert to string
        String formattedDateTime = now.format(formatter);

        // Header
        sb.append("(************************************************************\n");
        sb.append(" * Model automatically generated (" + formattedDateTime + ")\n");
        sb.append("************************************************************)\n");

		// Variables declaration
		sb.append("var");
        for (Variable variable : variables) {
            sb.append("\n\t" + variable.toIMITATOR() + ": " + variable.getIMITATORType() + ";");  // Adding a newline after each location for readability
        }
        sb.append("\n");

        // Automata
        for (PTA pta : ptas) {
            sb.append(pta.toIMITATOR()).append("\n");  // Adding a newline after each PTA for readability
        }

        // Initial state
        sb.append("(************************************************************)\n");
        sb.append("(* Initial state *)\n");
        sb.append("(************************************************************)\n");
        sb.append("init = {\n");
        sb.append("\tdiscrete =\n");
        sb.append("\t(*------------------------------------------------------------*)\n");
        sb.append("\t(* Initial location *)\n");
        sb.append("\t(*------------------------------------------------------------*)\n");
        for (PTA pta : ptas) {
            sb.append("\tloc[" + pta.getName() + "] <- " + pta.getInitialLocation().nameToIMITATOR() + ",\n");
        }
        sb.append("\n");

// 		loc[pta] <- l1,

        sb.append("\t(*------------------------------------------------------------*)\n");
        sb.append("\t(* Initial discrete variables assignments *)\n");
        sb.append("\t(*------------------------------------------------------------*)\n");
        for (Variable v : variables) {
            if(v.is_discrete_initially_0()) {
                sb.append("\t " + v.toIMITATOR() + " <- 0,\n");
            }
        }
        sb.append(";\n");

        sb.append("\tcontinuous =\n");
        sb.append("\t(*------------------------------------------------------------*)\n");
        sb.append("\t(* Initial clock constraints *)\n");
        sb.append("\t(*------------------------------------------------------------*)\n");
        for (Variable v : variables) {
            if(v.is_continuous_initially_0()) {
                sb.append("\t & " + v.toIMITATOR() + " = 0\n");
            }
        }

        sb.append("\t(*------------------------------------------------------------*)\n");
        sb.append("\t(* Parameter constraints *)\n");
        sb.append("\t(*------------------------------------------------------------*)\n");
        sb.append(";\n");

        sb.append("}\n");

        return sb.toString();  // Remove the last newline
    }
}
