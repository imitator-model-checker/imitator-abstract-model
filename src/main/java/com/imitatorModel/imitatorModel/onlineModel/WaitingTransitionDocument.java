package com.imitatorModel.imitatorModel.onlineModel;

public class WaitingTransitionDocument {

    private String guard;
    private String synchronization;
    private String assignment;

    private boolean completed = false;

    public WaitingTransitionDocument() {
    }

    public void update(
            String guard,
            String synchronization,
            String assignment) {

        this.guard = guard;
        this.synchronization = synchronization;
        this.assignment = assignment;

        this.completed = true;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String render() {

        StringBuilder sb = new StringBuilder();

        sb.append("\t\ttransition ");

        if (synchronization != null) {
            sb.append(synchronization);
        }

        if (guard != null) {
            sb.append(" {")
              .append(guard)
              .append("}");
        }

        if (assignment != null) {
            sb.append(" {")
              .append(assignment)
              .append("}");
        }

        sb.append(";\n");

        return sb.toString();
    }
}
