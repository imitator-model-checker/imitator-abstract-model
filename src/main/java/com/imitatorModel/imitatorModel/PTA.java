package com.imitatorModel.imitatorModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;


import java.nio.file.Paths;


public class PTA {
    private final Map<String, Location> locations;   // keyed by location name, insertion order
    private final Set<Action> actions;
    private String name;
    private Location initial_location;

    private Queue<Location> pendingOnlineLocations =
        new ArrayDeque<>();

    private String separateFileName = null;

    // When true, transitions built with Location.on(...).goTo(...) declare their actions on this PTA
    private boolean autoDeclareActions = true;

    public PTA(String name) {
		this.name = name;
        this.locations = new LinkedHashMap<>();
        this.actions = new LinkedHashSet<>();
    }

    public void setSeparateFileName (String filename){
        separateFileName = filename;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PTA other)) return false;
        return this.name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public Location getInitialLocation(){
        return initial_location;
    }

    public void setInitialLocation(Location location){
        this.initial_location = location;
    }

    public void addAction(Action action) {
        actions.add(action);
    }

    public void addActions(Set<Action> actions) {
        this.actions.addAll(actions);
    }

    public Set<Action> getActions() {
        return actions;
    }

    public String getName() {
        return name;
    }
    // Manipulating location list //////////////////////////////

    /**
     * Add a location. Its name must be new in this PTA, except that a waiting placeholder
     * (online mode) is replaced by a location of the same name.
     */
    public void addLocation(Location location) {
        Location existing = locations.get(location.getName());
        if (existing != null && (existing == location || !existing.getIsWaiting())) {
            throw new IllegalArgumentException(
                "Location " + location.getName() + " already exists in automaton " + name);
        }
        locations.put(location.getName(), location);
        location.setOwner(this);
    }

    // ignore adding waiting location... 
    public void addOnelineLocation(Location location) {
        addLocation(location);
        if (!location.getIsWaiting()){
            pendingOnlineLocations.add(location);
        }       
    }

    ///// getter for locations
    public Set<Location> getLocations() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(locations.values()));
    }

    public Location getLocation(String name) {
        Location location = locations.get(name);
        if (location == null) {
            throw new IllegalArgumentException("Location not found: " + name);
        }
        return location;
    }

    public boolean hasLocation(String name) {
        return locations.containsKey(name);
    }

    // Fluent builder methods ///////////////////////////////////

    /** The location called {@code name}, created and added if it does not exist yet. */
    public Location loc(String name) {
        Location location = locations.get(name);
        if (location == null) {
            location = new Location(name);
            addLocation(location);
        }
        return location;
    }

    public PTA initial(Location location) {
        setInitialLocation(location);
        return this;
    }

    /**
     * Whether builder transitions ({@code loc.on(a).goTo(l)}) declare their actions on this PTA (default true).
     * Disable it when the declared actions must be exactly a given set, e.g. a monitor that mirrors the
     * events of a log: a sync on an event the log does not have must stay undeclared rather than become
     * an action the monitor can take on its own.
     */
    public PTA setAutoDeclareActions(boolean autoDeclareActions) {
        this.autoDeclareActions = autoDeclareActions;
        return this;
    }

    public boolean isAutoDeclareActions() {
        return autoDeclareActions;
    }

    public PTA actions(Action... actions) {
        this.actions.addAll(Arrays.asList(actions));
        return this;
    }


    public String toIMITATOR() {
        try {
            if (separateFileName != null) {
                this.generateToSeparateFile();
                return "#include <" + separateFileName + ">";
            }
            return generatePrefix()+ generateLocations() + generateSuffix();
        } catch (IOException e) {
            throw new RuntimeException("Error rendering PTA to IMITATOR format", e);
        }
    }

    public String toOnlineIMITATOR() {
        try {
            if (separateFileName != null) {
                this.generateToSeparateFile();
                return "#include <" + separateFileName + ">";
            }
            return generatePrefix()+ generateLocations() ;
        } catch (IOException e) {
            throw new RuntimeException("Error rendering PTA to IMITATOR format", e);
        }
    }

    public String generateLocations(){
            StringBuilder sb = new StringBuilder();
            // List of locations
            for (Location location : locations.values()) {
                sb.append(location.toIMITATOR()).append("\n\n");  // Adding a newline after each location for readability
            }
            return sb.toString();
    }

    public String generatePrefix(){
        StringBuilder sb = new StringBuilder();
        sb.append("(*------------------------------------------------------------*)\n");
        sb.append("automaton " + name + "\n");
        sb.append("(*------------------------------------------------------------*)\n");
        // List of Actions
        // Sorted by name so that the output does not depend on the order actions were added
        sb.append("actions: ");
        sb.append(actions.stream()
                .map(Action::toIMITATOR)
                .sorted()
                .collect(Collectors.joining(", ")));
        sb.append(";\n");
        return sb.toString(); 
    }

    public String generateSuffix(){
        return "end (* " + name + "*)" ;
    }

    public void generateToSeparateFile()
            throws IOException {
        if (separateFileName != null){
            Files.writeString(
                    Paths.get(this.separateFileName),
                    this.generatePrefix() + this.generateLocations() ,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    // the side effect of this function is that it will consume the queue
    public String drainPendingLocations() {
        StringBuilder sb = new StringBuilder();

        Location location;
        while ((location = pendingOnlineLocations.poll()) != null) {
            sb.append(location.toIMITATOR()).append('\n');
        }

        return sb.toString();
    }

    public void appendToSeparateFile(String content) throws IOException {
        Files.writeString(
            Paths.get(separateFileName),
            content,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    }

    public void addSuffixToSeparateFile() throws IOException {
        if (separateFileName != null){
            Files.writeString(
                    Paths.get(this.separateFileName),
                    this.generateSuffix(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        }
    }
}
