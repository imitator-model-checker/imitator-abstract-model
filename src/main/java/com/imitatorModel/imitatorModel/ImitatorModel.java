/**
 * Defines the ImitatorModel class, which represents a model for the IMITATOR tool. It contains a list of variables and a list of PTAs (Parametric Timed Automata). The class provides methods to add variables and PTAs, and to generate the IMITATOR representation of the model.
 */

package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ImitatorModel {
    private final Map<String, VariableType> variables;   // keyed by name, declaration order = insertion order
    private final List<PTA> ptas;
    private boolean headerTimestamp = true;

    public ImitatorModel() {
        this.variables = new LinkedHashMap<>();
        this.ptas = new ArrayList<PTA>();
    }

    /**
     * Declare a variable. If a variable with the same name and type is already declared, it is kept
     * and returned (the argument is ignored). If the name is used with another type, throws.
     * @return the variable now declared under that name
     */
    public VariableType addVariable(VariableType variable) {
        VariableType existing = variables.get(variable.getName());
        if (existing == null) {
            variables.put(variable.getName(), variable);
            return variable;
        }
        if (existing.getClass() != variable.getClass()) {
            throw new IllegalArgumentException(
                "Variable " + variable.getName() + " is already declared as "
                + existing.getIMITATORType() + ", cannot redeclare it as " + variable.getIMITATORType());
        }
        return existing;
    }

    /** Declare a variable, replacing any variable with the same name (whatever its type). */
    public VariableType replaceVariable(VariableType variable) {
        variables.put(variable.getName(), variable);
        return variable;
    }

    /**
     * Replace a same-name variable only if its type differs; a same-type variable is kept.
     * Used by {@link #mergeModel}. For an unconditional overwrite use {@link #replaceVariable}.
     * @return the variable now declared under that name
     */
    public VariableType addVariableOverwrite(VariableType variable) {
        VariableType existing = variables.get(variable.getName());
        if (existing != null && existing.getIMITATORType().equals(variable.getIMITATORType())) {
            return existing;
        }
        variables.put(variable.getName(), variable);
        return variable;
    }

    public void addVariables(Collection<? extends VariableType> variables) {
        variables.forEach(this::addVariable);
    }

    public void addPTA(PTA pta) {
        this.ptas.add(pta);
    }

    public void addPTAs(List<PTA> ptas) {
        this.ptas.addAll(ptas);
    }

    /** Create an automaton, add it to the model and return it. */
    public PTA automaton(String name) {
        if (ptas.stream().anyMatch(p -> p.getName().equals(name))) {
            throw new IllegalArgumentException("Automaton " + name + " already exists");
        }
        PTA pta = new PTA(name);
        addPTA(pta);
        return pta;
    }

    /** Variables in declaration order (read-only snapshot). */
    public Set<VariableType> getVariables() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(variables.values()));
    }

    public List<PTA> getPTAs() {
        return ptas;
    }

    public PTA getPTA(String name) {
        return ptas.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Unknown PTA: " + name));
    }

    /** Whether the generated header contains the generation date (true by default). Disable for reproducible output. */
    public ImitatorModel setHeaderTimestamp(boolean headerTimestamp) {
        this.headerTimestamp = headerTimestamp;
        return this;
    }

    // addVariable from name: return the declared variable (existing one if same name and type)
    public Clock addClock(String name) {
        return (Clock) addVariable(new Clock(name));
    }

    public Parameter addParameter(String name) {
        return (Parameter) addVariable(new Parameter(name));
    }

    public Rational addRational(String name) {
        return (Rational) addVariable(new Rational(name));
    }

    public Rational addRationalOverwrite(String name) {
        return (Rational) addVariableOverwrite(new Rational(name));
    }

    public Int addInt(String name) {
        return (Int) addVariable(new Int(name));
    }

    public ImiBoolean addImiBoolean(String name) {
        return (ImiBoolean) addVariable(new ImiBoolean(name));
    }

    /**
     * Variables of this model, then those of {@code other}: a Rational of {@code other} replaces a
     * same-name variable of another type; any other same-name variable of {@code other} is ignored.
     * PTAs of this model come first.
     */
    public ImitatorModel mergeModel(ImitatorModel other) {
        ImitatorModel result = new ImitatorModel();
        result.headerTimestamp = this.headerTimestamp;

        result.addVariables(this.variables.values());
        for (VariableType variable : other.variables.values()) {
            if (variable instanceof Rational) {
                result.addVariableOverwrite(variable);
            } else {
                result.variables.putIfAbsent(variable.getName(), variable);
            }
        }

        result.addPTAs(this.ptas);
        result.addPTAs(other.ptas);

        return result;
    }
    // The order in which we merge the order matters because it determine which variable is kept


    public String toIMITATOR() {
        StringBuilder sb = new StringBuilder();
        // Automata
        for (PTA pta : ptas) {
            sb.append(pta.toIMITATOR()).append("\n");  // Adding a newline after each PTA for readability
        }
        return this.generateHeader()+ this.generateVarDeclaration()+ sb.toString() + this.generateInitialization();
    }

    public String toOnlineIMITATOR(PTA targetPTA) {
        StringBuilder sb = new StringBuilder();
        // Automata
        for (PTA pta : ptas) {
            sb.append(pta.equals(targetPTA) ? pta.toOnlineIMITATOR() : pta.toIMITATOR()).append("\n");  // Adding a newline after each PTA for readability
        }
        return this.generateHeader()+ this.generateVarDeclaration()+ sb.toString() + this.generateInitialization();
    }

    public String generateHeader(){
        StringBuilder sb = new StringBuilder();
        // Get the current date and time
        ZonedDateTime now = ZonedDateTime.now();
        // Define the desired format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
        // Convert to string
        String formattedDateTime = now.format(formatter);

        // Header
        sb.append("(************************************************************\n");
        sb.append(headerTimestamp
                ? " * Model automatically generated (" + formattedDateTime + ")\n"
                : " * Model automatically generated\n");
        sb.append("************************************************************)\n");

        return sb.toString();
    }

    public String generateVarDeclaration (){
        StringBuilder sb = new StringBuilder();
		// Variables declaration
		sb.append("var");
        for (VariableType variable : variables.values()) {
            sb.append("\n\t")
            .append(variable.toIMITATOR());

            if (variable.valueToIMITATOR() != null) {
                sb.append(" = ").append(variable.valueToIMITATOR());
            }

            sb.append(": ").append(variable.getIMITATORType()).append(";");        
        }
        sb.append("\n");

        return sb.toString();
    }


    public String generateInitialization (){
        StringBuilder sb = new StringBuilder();
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
            if (pta.getInitialLocation() == null) {
                throw new IllegalStateException("Automaton " + pta.getName() + " has no initial location");
            }
            sb.append("\tloc[" + pta.getName() + "] <- " + pta.getInitialLocation().nameToIMITATOR() + ",\n");
        }
        sb.append("\n");


        sb.append("\t(*------------------------------------------------------------*)\n");
        sb.append("\t(* Initial discrete variables assignments *)\n");
        sb.append("\t(*------------------------------------------------------------*)\n");
        for (VariableType v : variables.values()) {
            if(v.is_discrete_initially_0()) {
                if (v instanceof ImiBoolean){
                    sb.append("\t " + v.toIMITATOR() + " <- False,\n");
                }
                else {
                    sb.append("\t " + v.toIMITATOR() + " <- 0,\n");
                }
            }
        }
        sb.append(";\n");

        sb.append("\tcontinuous =\n");
        sb.append("\t(*------------------------------------------------------------*)\n");
        sb.append("\t(* Initial clock constraints *)\n");
        sb.append("\t(*------------------------------------------------------------*)\n");
        for (VariableType v : variables.values()) {
            if(v instanceof Clock  && v.is_continuous_initially_0()) {
                sb.append("\t & " + v.toIMITATOR() + " = 0\n");
            }
        }

        sb.append("\t(*------------------------------------------------------------*)\n");
        sb.append("\t(* Parameter constraints *)\n");
        sb.append("\t(*------------------------------------------------------------*)\n");
        for (VariableType v : variables.values()) {
            if(v instanceof Parameter p && p.geConstraint() != null) {
                sb.append("\t & " + p.geConstraint().toIMITATOR() + "\n");
            }
        }
        sb.append(";\n");
        sb.append("}\n");

        return sb.toString();  // Remove the last newline
    }

    // public void export2File(String outputFilePath) {
    //     String stringModel = this.toIMITATOR();  

    //     try {
    //         Files.writeString(Path.of(outputFilePath), stringModel);
    //         System.out.println("Model exported to " + outputFilePath);
    //     } catch (IOException e) {
    //         System.err.println("Error writing to file: " + e.getMessage());
    //     }
    // }

}