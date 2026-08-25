package com.imitatorModel.imitatorModel.onlineModel;

import com.imitatorModel.imitatorModel.Location;
import com.imitatorModel.imitatorModel.PTA;
public class OnlineUpdateTarget {

    private final PTA pta;
    private final PTADocument ptaDocument;
    private final ImitatorRenderer renderer;

    public OnlineUpdateTarget(
            PTA pta,
            PTADocument ptaDocument,
            ImitatorRenderer renderer) {

        this.pta = pta;
        this.ptaDocument = ptaDocument;
        this.renderer = renderer;
    }

    public PTA getPTA() {
        return pta;
    }

    public PTADocument getPTADocument() {
        return ptaDocument;
    }

    /**
     * Add a completely constructed location to both
     * the in-memory PTA and its rendered document.
     */
    public void addLocation(Location location) {

        // Update semantic model
        pta.addLocation(location);

        // Update rendered document
        LocationDocument locationDocument =
                renderer.renderLocation(location);

        ptaDocument.addLocation(locationDocument);
    }

    /**
     * Remove a waiting location from both
     * the in-memory PTA and its rendered document.
     */
    public void removeWaitingLocation(Location location) {

        String locationName = location.getName();

        // Update semantic model
        pta.deleteLocation(locationName);

        // Update rendered document
        ptaDocument.removeLocation(locationName);
    }

    /**
     * Replace an existing waiting location with a completely
     * constructed new location having the same name.
     */
    public void replaceWaitingLocation(Location location) {

        // Update semantic model
        pta.replaceLocation(location);

        // Update rendered document
        LocationDocument locationDocument =
                renderer.renderLocation(location);

        ptaDocument.replaceLocation(locationDocument);
    }
}
