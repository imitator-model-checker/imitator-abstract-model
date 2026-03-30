package com.imitatorModel.imitatorModel;

import java.util.List;

public class ListUpdates {
    private List<Update> updates;

    public ListUpdates(List<Update> updates) {
        this.updates = updates;
    }

    public ListUpdates(Update update) {
        this.updates = List.of(update);
    }

    public ListUpdates() {
        this.updates =  List.of(); 
    }

    public List<Update> getUpdates() {
        return updates;
    }

    public String toIMITATOR() {
        if(updates.isEmpty()){
            return "";
        }else{
            StringBuilder sb_updates = new StringBuilder();

            for (int i = 0; i < updates.size(); i++) {
                sb_updates.append(updates.get(i).toIMITATOR());

                if (i < updates.size() - 1) {
                    sb_updates.append("; ");
                }
            }
            String sb =  " do {" + sb_updates.toString() + "}";
            return sb;
        }  
    }
}
