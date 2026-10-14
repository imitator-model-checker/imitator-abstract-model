package com.imitatorModel.imitatorModel;

import java.util.ArrayList;
import java.util.List;

public class ListUpdates {
    private ArrayList<Update> updates;

    public ListUpdates(List<Update> updates) {
        this.updates = new ArrayList<>(updates);
    }

    public ListUpdates(Update update) {
        this.updates = new ArrayList<Update>();
        this.updates.add(update);
    }

    public ListUpdates() {
        this.updates =  new ArrayList<Update>(); 
    }

    public List<Update> getUpdates() {
        return updates;
    }


    public ListUpdates addUpdate(Update update) {
        if (update == null) {
            return this;
        }

        List<Update> newUpdates = new ArrayList<>(this.updates);
        newUpdates.add(update);
        return new ListUpdates(newUpdates);
    }

    public String toIMITATOR() {
        if(updates.isEmpty()){
            return "";
        }else{
            StringBuilder sb_updates = new StringBuilder();

            for (int i = 0; i < updates.size(); i++) {
                sb_updates.append(updates.get(i).toIMITATOR());

                // "if ... end" is a complete instruction: IMITATOR rejects a ";" after it
                // ("if c then x <- 1 end y <- 2" is valid, "if c then x <- 1 end; y <- 2" is a parse error)
                if (i < updates.size() - 1 && updates.get(i).getCondition() == null) {
                    sb_updates.append("; ");
                }
            }
            String sb =  " do {" + sb_updates.toString() + "}";
            return sb;
        }  
    }
}
