package com.imitatorModel.imitatorModel.onlineModel;
import java.util.LinkedHashMap;
import java.util.Map;


public class PTADocument {

    private final String name;


    private final Map<String, LocationDocument> locations =
            new LinkedHashMap<>();

    public PTADocument(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }


    public void addLocation(LocationDocument location) {

        String name = location.getName();

        if (locations.containsKey(name)) {
            throw new IllegalArgumentException(
                    "Location already exists: " + name);
        }

        locations.put(name, location);
    }

    public void removeLocation(String locationName) {

        if (locations.remove(locationName) == null) {
            throw new IllegalArgumentException(
                    "Unknown location: " + locationName);
        }
    }

    public void replaceLocation(
            LocationDocument location) {

        String name = location.getName();

        if (!locations.containsKey(name)) {
            throw new IllegalArgumentException(
                    "Unknown location: " + name);
        }

        locations.put(name, location);
    }

    public LocationDocument getLocation(
            String locationName) {

        LocationDocument location =
                locations.get(locationName);

        if (location == null) {
            throw new IllegalArgumentException(
                    "Unknown location: " + locationName);
        }

        return location;
    }

    public String render() {

        StringBuilder sb = new StringBuilder();

        for (LocationDocument location :
                locations.values()) {

            sb.append(location.render());
            sb.append("\n");
        }

        return sb.toString();
    }
}