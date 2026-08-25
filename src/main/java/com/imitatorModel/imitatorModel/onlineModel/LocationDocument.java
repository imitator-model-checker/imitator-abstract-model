package com.imitatorModel.imitatorModel.onlineModel;

public class LocationDocument {

    private final String name;
    private final String content;

    public LocationDocument(
            String name,
            String content) {

        this.name = name;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public String render() {
        return content;
    }
}
