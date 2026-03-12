/**
 * This is the main class for testing the IMITATOR Model. It creates a simple model with one PTA, two locations, and some variables, and then prints the IMITATOR representation of the model.
 */

package com.imitatorModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.imitatorModel.bigFraction.BigFraction;
import com.imitatorModel.imitatorModel.*;

public class Main {
    public static final String output_file_name = "output.imi";


    /**
     * The main method is the entry point of the program. It creates a simple IMITATOR model with one PTA, two locations, and some variables, and then prints the IMITATOR representation of the model.
     * @param args
     */
    public static void main(String[] args) {

        System.out.println("Hi there!");

    //     // Testing the IMITATOR Model
    //     ImitatorModel toy_model = new ImitatorModel();

    //     Clock clock1 = new Clock("x1");
    //     Parameter param1 = new Parameter("p1");
    //     toy_model.addVariable(clock1);
    //     toy_model.addVariable(param1);

    //     PTA pta = new PTA("pta1");

    //     LinearExpr lt1 = new LinearExpr(new BigFraction(5));
    //     lt1.addTerm(clock1, new BigFraction(3, 2)); // 1.5 as a fraction
    //     LinearExpr lt2 = new LinearExpr(new BigFraction(5));
    //     lt2.addTerm(param1, new BigFraction(3, 2)); // 1.5 as a fraction
    //     List<Constraint> constraints = new ArrayList<>();
    //     constraints.add(new Constraint(lt1, Operator.LESS_THAN_OR_EQUAL));
    //     constraints.add(new Constraint(lt2, Operator.GREATER_THAN));
    //     ConjunctionOfConstraints invariant = new ConjunctionOfConstraints(constraints);

    //     Location loc1 = new Location("Loc1");
    //     loc1.setInvariant(invariant);
    //     loc1.addRate(clock1, new LinearExpr(new BigFraction(3, 2))); // 1.5 as a fraction
    //     loc1.setUrgent();

    //     Location loc2 = new Location("Loc2");

    //     pta.addLocation(loc1);
    //     pta.addLocation(loc2);
    //     pta.setInitialLocation(loc1);
    //     pta.addAction(new Action("a"));
    //     pta.addAction(new Action("b"));
    //     toy_model.addPTA(pta);

    //     List<Update> updates = new ArrayList<Update>();
    //     updates.add(new Update(clock1, lt1));
    //     updates.add(new Update(clock1, lt2));

    //     loc1.addTransition(new Transition(new ConjunctionOfConstraints(), new Action("a"), updates, loc2));
    //     loc2.addTransition(new Transition(new ConjunctionOfConstraints(), new Action("b"), new ArrayList<Update>(), loc1));

    //     System.out.println("Toy model with variables and locations added successfully.");
    //     String toy_imitator_model = toy_model.toIMITATOR();
    //     System.out.println(toy_imitator_model);

    //     // Export the model to a file named "output.imi"
    //     try {
    //        Files.writeString(Path.of(output_file_name), toy_imitator_model);
    //        System.out.println("Model exported to " + output_file_name);
    //     } catch (IOException e) {
    //        System.err.println("Error writing to file: " + e.getMessage());
    //     } 

    }
}
