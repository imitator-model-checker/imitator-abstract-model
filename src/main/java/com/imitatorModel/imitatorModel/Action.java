package com.imitatorModel.imitatorModel;

import java.util.Objects;

public class Action {
   private String name;


   public Action(String name) {
       this.name = name;
   }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Action)) return false;

        Action other = (Action) o;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
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
