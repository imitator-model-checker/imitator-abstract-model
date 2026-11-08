package com.imitatorModel.imitatorModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of {@link ImitatorModel#toUppaalSpec}: the UPPAAL model (queries embedded in {@code <queries>}),
 * the same queries as a {@code .q} file, and what is needed to read the results back ({@link UppaalResult}).
 *
 * @param xml         UPPAAL project file ({@code verifyta model.xml} checks the embedded queries)
 * @param queries     one query per line, in the order of the properties ({@code verifyta model.xml model.q});
 *                    a {@link Property#value} property gives three lines
 * @param timeScale   every clock value and rational of the IMITATOR model is multiplied by this factor in UPPAAL
 * @param warnings    translations that are not exact (e.g. a broadcast that IMITATOR would block)
 * @param properties  the properties, in order
 * @param queryOwners for each query (0-based, as verifyta's {@code --query-index}), the index of its property
 * @param names       IMITATOR → UPPAAL names (UPPAAL renames clashes and reserved words, e.g. automaton
 *                    {@code log} → {@code log_1}): needed to read a verifyta trace back
 */
public record UppaalSpec(String xml, String queries, long timeScale, List<String> warnings,
                         List<Property> properties, List<Integer> queryOwners, Names names) {

    /**
     * IMITATOR name → UPPAAL name, for the variables, the automata (UPPAAL templates/processes) and the locations
     * of each automaton (keyed by the IMITATOR automaton name).
     */
    public record Names(Map<String, String> variables, Map<String, String> automata,
                        Map<String, Map<String, String>> locations) {
        public Names {
            variables = Collections.unmodifiableMap(new LinkedHashMap<>(variables));
            automata = Collections.unmodifiableMap(new LinkedHashMap<>(automata));
            Map<String, Map<String, String>> copy = new LinkedHashMap<>();
            locations.forEach((k, v) -> copy.put(k, Collections.unmodifiableMap(new LinkedHashMap<>(v))));
            locations = Collections.unmodifiableMap(copy);
        }

        /**
         * The names UPPAAL had to change (clash, reserved word, illegal character), one line each, for example
         * {@code automaton log -> log_1}: what a hand-written UPPAAL query must use instead.
         */
        public List<String> renamed() {
            List<String> out = new ArrayList<>();
            variables.forEach((imi, up) -> { if (!imi.equals(up)) out.add("variable " + imi + " -> " + up); });
            automata.forEach((imi, up) -> { if (!imi.equals(up)) out.add("automaton " + imi + " -> " + up); });
            locations.forEach((pta, locs) -> locs.forEach((imi, up) -> {
                if (!imi.equals(up)) out.add("location " + pta + "." + imi + " -> " + up);
            }));
            return out;
        }
    }

    public UppaalSpec {
        warnings = List.copyOf(warnings);
        properties = List.copyOf(properties);
        queryOwners = List.copyOf(queryOwners);
    }

    /** Indices (0-based) of the queries of {@code property}. */
    public List<Integer> queryIndices(Property property) {
        int p = properties.indexOf(property);
        if (p < 0) {
            throw new IllegalArgumentException("property not part of this specification: " + property);
        }
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < queryOwners.size(); i++) {
            if (queryOwners.get(i) == p) {
                indices.add(i);
            }
        }
        return indices;
    }

    /** Write {@code xml} (and {@code queries} when {@code queryFile} is not null). */
    public void writeTo(Path xmlFile, Path queryFile) throws IOException {
        Files.writeString(xmlFile, xml, StandardCharsets.UTF_8);
        if (queryFile != null) {
            Files.writeString(queryFile, queries, StandardCharsets.UTF_8);
        }
    }
}
