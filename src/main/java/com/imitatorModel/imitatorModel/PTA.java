package com.imitatorModel.imitatorModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
// import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;


import java.nio.file.Paths;


public class PTA {
    private Set<Location> locations;
    private Set<Action> actions;
    private String name;
    private Location initial_location;

    private Queue<Location> pendingOnlineLocations =
        new ArrayDeque<>();

    private String separateFileName = null;

    public PTA(String name) {
		this.name = name;
        this.locations = new LinkedHashSet<>();
        this.actions = new HashSet<>();
    }

    public void setSeparateFileName (String filename){
        separateFileName = filename;
    }

    public boolean equals(PTA other){
        return this.name.equals(other.name);
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

    public void addLocation(Location location) {
        if (!locations.add(location)) {
            throw new IllegalArgumentException("Location already exists");
        }
        locations.add(location);
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
        return locations;
    }

    public Location getLocation(String name) {
        return locations.stream()
                .filter(location -> location.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + name));
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
            for (Location location : locations) {
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
        sb.append("actions: ");
        for (Action action : actions) {
            sb.append(action.toIMITATOR()).append(", ");  // Adding a newline after each location for readability
        }
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
