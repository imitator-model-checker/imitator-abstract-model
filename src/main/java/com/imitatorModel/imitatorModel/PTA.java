package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.List;

public class PTA {
    private List<Location> locations;
    private List<Action> actions;
    private String name;
    private Location initial_location;

    public PTA(String name) {
		this.name = name;
        this.locations = new ArrayList<>();
        this.actions = new ArrayList<>();
    }

    public Location getInitialLocation(){
        return initial_location;
    }

    public void setInitialLocation(Location location){
        this.initial_location = location;
    }

    public void addLocation(Location location) {
        locations.add(location);
    }

    public void addAction(Action action) {
        actions.add(action);
    }

    public void addActions(List<Action> actions) {
        this.actions.addAll(actions);
    }

    public List<Location> getLocations() {
        return locations;
    }

    public List<Action> getActions() {
        return actions;
    }

    public String getName() {
        return name;
    }

    public String toIMITATOR() {
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

        // List of locations
        for (Location location : locations) {
            sb.append(location.toIMITATOR()).append("\n\n");  // Adding a newline after each location for readability
        }
		sb.append("end (* " + name + "*)");

        return sb.toString();
    }
}
