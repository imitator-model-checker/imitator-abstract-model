package com.imitatorModel.imitatorModel;


public class Action {
   private String name;


   public Action(String name) {
       this.name = name;
   }


    public String getName() {
        return name;
    }


   public String toIMITATOR(){
       return getName();
   }
    
   @Override
    public String toString() {
        return getName();
    }
}
