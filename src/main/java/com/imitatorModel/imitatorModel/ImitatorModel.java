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
        if (existing.getClass() != variable.getClass()
                || !existing.getIMITATORType().equals(variable.getIMITATORType())) {
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

    /** Declare a FIFO queue {@code name : <elementType> queue;}, initially empty. */
    public ImiQueue addQueue(String name, ImiQueue.ElementType elementType) {
        return (ImiQueue) addVariable(new ImiQueue(name, elementType));
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
                sb.append("\t " + v.toIMITATOR() + " <- " + v.initialValueToIMITATOR() + ",\n");
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

    // ------------------------------------------------------------------
    // Parameters and choice of the model checker
    // ------------------------------------------------------------------

    /** Variables read or written anywhere in the automata: invariants, flows, guards, updates (and their conditions). */
    public Set<VariableType> getUsedVariables() {
        Set<VariableType> used = new LinkedHashSet<>();
        for (PTA pta : ptas) {
            for (Location l : pta.getLocations()) {
                used.addAll(l.getInvariant().getVariables());
                used.addAll(l.getStop());
                for (Pair<VariableType, LinearExpr> r : l.getRate()) {
                    used.add(r.getFirst());
                    used.addAll(r.getSecond().getCoefficients().keySet());
                }
                for (Transition t : l.getTransitions()) {
                    used.addAll(t.getGuard().getVariables());
                    for (Update u : t.getUpdates().getUpdates()) {
                        used.add(u.getVariable());
                        if (u.getCondition() != null) {
                            used.addAll(u.getCondition().getVariables());
                        }
                        collectTermVariables(u.getTerm(), used);
                    }
                }
            }
        }
        return used;
    }

    private static void collectTermVariables(UpdateTerm term, Set<VariableType> used) {
        if (term instanceof LinearExpr e) {
            used.addAll(e.getCoefficients().keySet());
        } else if (term instanceof BooleanExpr b && b.getVariable() != null) {
            used.add(b.getVariable());
        } else if (term instanceof QueueExpr q && q.getElement() != null) {
            collectTermVariables(q.getElement(), used);
        }
    }

    /** Parameters declared in the model or occurring in it (an undeclared one is still a parameter). */
    public Set<Parameter> getUsedParameters() {
        Set<Parameter> parameters = new LinkedHashSet<>();
        for (VariableType v : variables.values()) {
            if (v instanceof Parameter p) {
                parameters.add(p);
            }
        }
        for (VariableType v : getUsedVariables()) {
            if (v instanceof Parameter p) {
                parameters.add(p);
            }
        }
        return parameters;
    }

    /** True if the specification needs parameters, hence IMITATOR (UPPAAL has no parameter synthesis). */
    public boolean usesParameters() {
        return !getUsedParameters().isEmpty();
    }

    /** Why this model (with these properties) cannot go to UPPAAL; empty if it can. Parameters come first. */
    public List<String> uppaalProblems(Property... properties) {
        return UppaalContext.translate(this, UppaalOptions.defaults(), List.of(properties)).problems();
    }

    public boolean canUseUppaal(Property... properties) {
        return uppaalProblems(properties).isEmpty();
    }

    /**
     * IMITATOR as soon as a parameter is involved (model or property), or when UPPAAL cannot express the model;
     * UPPAAL otherwise.
     */
    public Backend preferredBackend(Property... properties) {
        if (usesParameters() || java.util.Arrays.stream(properties).anyMatch(Property::usesParameters)) {
            return Backend.IMITATOR;
        }
        return canUseUppaal(properties) ? Backend.UPPAAL : Backend.IMITATOR;
    }

    // ------------------------------------------------------------------
    // UPPAAL
    // ------------------------------------------------------------------

    /**
     * UPPAAL project file ({@code .xml}) standing for this model, the properties embedded as {@code <queries>}
     * (counterpart of {@link #toIMITATOR()} + the {@code .imiprop}).
     * @throws UppaalUnsupportedException if the model uses parameters or another feature UPPAAL lacks
     */
    public String toUppaal(Property... properties) {
        return toUppaalSpec(UppaalOptions.defaults(), properties).xml();
    }

    public String toUppaal(UppaalOptions options, Property... properties) {
        return toUppaalSpec(options, properties).xml();
    }

    /** UPPAAL query file ({@code .q}), one line per property (counterpart of the {@code .imiprop}). */
    public String toUppaalQueries(Property... properties) {
        return toUppaalSpec(UppaalOptions.defaults(), properties).queries();
    }

    /** Model, queries, time scale and warnings of the UPPAAL translation. */
    public UppaalSpec toUppaalSpec(UppaalOptions options, Property... properties) {
        UppaalContext.Result result = UppaalContext.translate(this, options, List.of(properties));
        if (result.spec() == null) {
            throw new UppaalUnsupportedException(result.problems());
        }
        return result.spec();
    }

    /** {xml, queries}: one rendering pass (called twice by {@link UppaalContext#translate}). */
    String[] renderUppaal(UppaalContext ctx, List<Property> properties) {
        // templates first: they register the queue types and the helper functions the declarations need
        StringBuilder templates = new StringBuilder();
        for (PTA pta : ptas) {
            templates.append(pta.toUppaal(ctx));
        }
        List<String> queries = new ArrayList<>();
        List<Integer> owners = new ArrayList<>();      // property index of each query
        for (int i = 0; i < properties.size(); i++) {
            for (String query : properties.get(i).toUppaalQueries(ctx)) {
                queries.add(query);
                owners.add(i);
            }
        }
        ctx.setQueryOwners(owners);
        StringBuilder declarations = new StringBuilder();
        for (VariableType v : variables.values()) {
            declarations.append(v.declarationToUppaal(ctx)).append("\n");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        sb.append("<!DOCTYPE nta PUBLIC '-//Uppaal Team//DTD Flat System 1.6//EN' 'http://www.it.uu.se/research/group/darts/uppaal/flat-1_6.dtd'>\n");
        sb.append("<nta>\n\t<declaration>");
        sb.append(UppaalContext.xml(generateUppaalDeclaration(ctx, declarations.toString())));
        sb.append("</declaration>\n");
        sb.append(templates);
        sb.append("\t<system>system ");
        sb.append(String.join(", ", ptas.stream().map(ctx::templateName).toList()));
        sb.append(";</system>\n");
        sb.append("\t<queries>\n");
        for (int i = 0; i < queries.size(); i++) {
            sb.append("\t\t<query>\n\t\t\t<formula>").append(UppaalContext.xml(queries.get(i))).append("</formula>\n");
            sb.append("\t\t\t<comment>").append(UppaalContext.xml(properties.get(owners.get(i)).toString())).append("</comment>\n");
            sb.append("\t\t</query>\n");
        }
        sb.append("\t</queries>\n</nta>\n");

        String q = queries.isEmpty() ? "" : String.join("\n", queries) + "\n";
        return new String[] {sb.toString(), q};
    }

    private String generateUppaalDeclaration(UppaalContext ctx, String variableDeclarations) {
        StringBuilder sb = new StringBuilder();
        sb.append(headerTimestamp
                ? "// Model automatically generated from an IMITATOR model ("
                  + ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")) + ")\n"
                : "// Model automatically generated from an IMITATOR model\n");
        sb.append("// Time scale: every clock value and rational is multiplied by ").append(ctx.timeScale()).append("\n");
        for (String note : ctx.notes()) {
            sb.append("// Note: ").append(note).append("\n");
        }
        for (String warning : new LinkedHashSet<>(ctx.warningsView())) {
            sb.append("// Warning: ").append(warning).append("\n");
        }

        if (!ctx.queueTypes().isEmpty()) {
            int cap = ctx.options().queueCapacity();
            sb.append("\n// IMITATOR queues: bounded arrays (a push on a full queue aborts the verification)\n");
            sb.append("const int ").append(UppaalContext.QUEUE_CAPACITY).append(" = ").append(cap).append(";\n");
            for (ImiQueue.ElementType type : ctx.queueTypes()) {
                String element = type == ImiQueue.ElementType.BOOL ? "bool" : ctx.intRange();
                String zero = type == ImiQueue.ElementType.BOOL ? "false" : "0";
                String t = ctx.queueType(type);
                sb.append("typedef struct { ").append(element).append(" data[").append(UppaalContext.QUEUE_CAPACITY)
                  .append("]; int[0,").append(UppaalContext.QUEUE_CAPACITY).append("] len; } ").append(t).append(";\n");
                sb.append("void ").append(ctx.queueFunction(type, "push")).append("(").append(t).append(" &q, ")
                  .append(type == ImiQueue.ElementType.BOOL ? "bool" : "int").append(" v) { q.data[q.len] = v; q.len++; }\n");
                sb.append("void ").append(ctx.queueFunction(type, "clear")).append("(").append(t)
                  .append(" &q) { for (i : int[0,").append(UppaalContext.QUEUE_CAPACITY).append("-1]) q.data[i] = ")
                  .append(zero).append("; q.len = 0; }\n");
            }
        }

        sb.append("\n// Variables\n").append(variableDeclarations);

        List<UppaalContext.Route> channels = ctx.routes().stream().filter(r -> r.channel() != null).toList();
        if (!channels.isEmpty()) {
            sb.append("\n// Actions shared by several automata (sender: the top-most automaton, as IMITATOR updates it first)\n");
            for (UppaalContext.Route r : channels) {
                sb.append(r.kind() == UppaalContext.RouteKind.BROADCAST ? "broadcast chan " : "chan ")
                  .append(r.channel()).append(";\n");
            }
        }

        if (!ctx.helperFunctions().isEmpty()) {
            sb.append("\n// Conditional updates (if ... then ... end)\n");
            ctx.helperFunctions().forEach(f -> sb.append(f).append("\n"));
        }
        return sb.toString();
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