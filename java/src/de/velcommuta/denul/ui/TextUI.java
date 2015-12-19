package de.velcommuta.denul.ui;

import de.velcommuta.denul.crypto.ECDHKeyExchange;
import de.velcommuta.denul.data.Investigator;
import de.velcommuta.denul.data.StudyRequest;
import de.velcommuta.denul.networking.HttpsConnection;
import de.velcommuta.denul.util.AsyncKeyGenerator;

import java.security.KeyPair;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

// Static-import a bunch of methods for more concise code
import static de.velcommuta.denul.util.Output.println;
import static de.velcommuta.denul.util.Output.print;
import static de.velcommuta.denul.util.Input.readLine;
import static de.velcommuta.denul.util.Input.readLines;
import static de.velcommuta.denul.util.Input.readSelection;
import static de.velcommuta.denul.util.Input.readHttpsURL;
import static de.velcommuta.denul.util.Input.yes;

/**
 * Text-based UI for use on the console
 */
public class TextUI {
    /**
     * Open the local database and ask for a password, if needed
     * @return true if the database was successfully opened, false otherwise
     */
    public boolean openDatabase() {
        // TODO
        return true;
    }


    /**
     * Create a new request
     */
    public void newRequest() {
        // Start key generation in background
        FutureTask<KeyPair> rsagen = AsyncKeyGenerator.generateRSA();
        FutureTask<ECDHKeyExchange> ecdhgen = AsyncKeyGenerator.generateECDH();

        // Create new study request
        StudyRequest request = new StudyRequest();

        // Give basic information
        println("");
        println("All questions are modelled after the medical study information sheet of the");
        println("Office for the Protection of Research Subjects of the University of Sourthern");
        println("California (USC), which can be found at");
        println("http://oprs.usc.edu/files/2013/04/Informed-Consent-Booklet-4.4.13.pdf");
        println("Please refer to that document to learn more about the individual questions.");
        println("");

        // Ask the questions for the form
        request.institution = readLine("Name of your institution");
        request.name        = readLine("Title of study");
        // TODO Commented out actual validation for testing - put this back in for production use
        // request.webpage     = readHttpsURL("Web page of study - must be reachable via HTTPS");
        request.webpage     = readLine("Web page of study - must be reachable via HTTPS");
        request.description = readLines("Please give a short description of the study");
        request.purpose     = readLines("Please explain the purpose of your study");
        request.procedures  = readLines("Please explain the procedures of this study - what data will be collected, and why");
        request.risks       = readLines("Please explain any risks associated with this study");
        request.benefits    = readLines("Please explain the benefits that will come of this study");
        request.payment     = readLines("Please explain if participants will be paid");
        request.conflicts   = readLines("Please explain any potential conflicts of interest in this work");
        request.confidentiality = readLines("Please explain your data security and confidentiality policy");
        request.participationAndWithdrawal = readLines("Please explain your policy on participating and withdrawing from the study");
        request.rights      = readLines("Please describe the rights of study participants");

        // Add investigator details
        println("You will now be asked to add at least one investigator to the study.");
        println("");
        request.investigators.addAll(addInvestigators());

        // Review information
        println("\n\nPlease review the following information:\n");
        println(request.toString());
        if (!yes("Is this information correct?")) {
            rsagen.cancel(true);
            ecdhgen.cancel(true);
            newRequest();
            return;
        }

        // TODO Add key verification method choice
        // Retrieve keys from background tasks
        KeyPair keys;
        ECDHKeyExchange exchange;
        if (!rsagen.isDone() || !ecdhgen.isDone()) {
            println("Waiting for key generation to complete...");
        }
        try {
            keys = rsagen.get();
            exchange = ecdhgen.get();
        } catch (InterruptedException | ExecutionException e) {
            println("Something went wrong during key generation, aborting");
            e.printStackTrace();
            return;
        }
        request.pubkey = keys.getPublic();
        request.privkey = keys.getPrivate();
        request.exchange = exchange;
    }


    /**
     * Ask the user to input the details of at least one, potentially more, Investigator(s)
     * @return A List of {@link Investigator}s
     */
    public List<Investigator> addInvestigators() {
        // Prepare List to return
        List<Investigator> rv = new LinkedList<>();
        // Create new investigator object
        Investigator investigator = new Investigator();

        // Query the user for the details
        investigator.name = readLine("Please enter the investigators full name");
        investigator.institution = readLine("Please enter the institution of the investigator");
        investigator.group = readLine("Please enter the group of the investigator");
        investigator.position = readLine("Please enter the position of the investigator");
        // Add investigator to return list
        rv.add(investigator);

        // Check if more investigators should be added
        if (yes("Do you want to add additional Investigators?")) {
            println("");
            // Recursively add further investigator(s)
            rv.addAll(addInvestigators());
        }
        // return
        return rv;
    }


    /**
     * View active studies
     */
    public void viewActiveStudies() {
        println("NotImplemented");
    }


    /**
     * View settings studies
     */
    public void viewSettings() {
        println("NotImplemented");
    }


    /**
     * Show the main menu of the application
     */
    public void mainMenu() {
        // Define the main menu options
        String[] menuOptions = {"New research request", "View active research data", "Settings", "Quit"};
        // Print the program header
        println("    Denul Research Client\n");
        // Main loop
        boolean running = openDatabase();
        while (running) {
            // Ask for menu selection
            int selection = readSelection("Welcome to the Denul Research Client. Please choose what you want to do:", menuOptions);
            switch (selection) {
                case 0:
                    // New research request
                    newRequest();
                    break;
                case 1:
                    // View existing research data
                    viewActiveStudies();
                    break;
                case 2:
                    // View the settings
                    viewSettings();
                    break;
                case 3:
                    running = false;
                    break;
                default:
                    println("WARN: " + menuOptions[selection] + " not implemented in switch statement");
            }
        }
    }
}
