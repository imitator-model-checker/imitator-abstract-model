package com.imitatorModel.imitatorModel.onlineModel;

import com.imitatorModel.imitatorModel.ImitatorModel;
import com.imitatorModel.imitatorModel.Location;
import com.imitatorModel.imitatorModel.PTA;
import com.imitatorModel.imitatorModel.VariableType;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ImitatorRenderer {

    public ImitatorDocument render(
            ImitatorModel model) {

        ImitatorDocument document =
                new ImitatorDocument();

        renderHeader(document);
        renderVariables(document, model);

        for (PTA pta : model.getPTAs()) {

            PTADocument ptaDocument =
                    renderPTA(pta);

            document.addPTA(
                    pta.getName(),
                    ptaDocument);
        }

        renderInitialState(document, model);

        return document;
    }

        public LocationDocument renderLocation(Location location) {

        return new LocationDocument(
                location.getName(),
                location.toIMITATOR()
        );
        }

    private PTADocument renderPTA(PTA pta) {

        PTADocument document =
                new PTADocument(pta.getName());


        for (Location location :
                pta.getLocations()) {

            String locationText =
                    location.toIMITATOR();

            LocationDocument locationDocument =
                    new LocationDocument(
                            location.getName(),
                            locationText);

            document.addLocation(
                    locationDocument);
        }

        return document;
    }


    private void renderHeader(
            ImitatorDocument document) {

        ZonedDateTime now =
                ZonedDateTime.now();

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "yyyy-MM-dd HH:mm:ss z");

        document.setHeader(
                "(************************************************************\n"
                + " * Model automatically generated ("
                + now.format(formatter)
                + ")\n"
                + "************************************************************)\n");
    }

    private void renderVariables(
            ImitatorDocument document,
            ImitatorModel model) {

        StringBuilder sb = new StringBuilder();

        sb.append("var\n");

        for (VariableType variable :
                model.getVariables()) {

            sb.append("\t")
              .append(variable.toIMITATOR());

            if (variable.getValue() != null) {
                sb.append(" = ")
                  .append(variable.getValue());
            }

            sb.append(": ")
              .append(variable.getIMITATORType())
              .append(";\n");
        }

        document.setVariables(sb.toString());
    }

    private void renderInitialState(
            ImitatorDocument document,
            ImitatorModel model) {

        // Keep your existing initial-state generation here.

        StringBuilder sb = new StringBuilder();

        // ...

        document.setInitialState(sb.toString());
    }
}