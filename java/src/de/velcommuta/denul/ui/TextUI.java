package de.velcommuta.denul.ui;

import de.velcommuta.denul.crypto.ECDHKeyExchange;
import de.velcommuta.denul.crypto.RSA;
import de.velcommuta.denul.data.Investigator;
import de.velcommuta.denul.data.StudyRequest;
import de.velcommuta.denul.util.AsyncKeyGenerator;

import java.net.MalformedURLException;
import java.net.URL;
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
    private StudyRequest request;

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
        request = new StudyRequest();

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

        // Get verification strategy
        int vs = addVerificationStrategy();
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
     * Ask the user which verification strategy she wants to use to authenticate the study request
     * @return TODO
     * TODO Better language
     */
    private int addVerificationStrategy() {
        // Ensure values are sane
        assert request != null;
        assert request.pubkey != null;
        // Display and read selection
        String selection = readSelection("\nPlease select how you want to authenticate your request", StudyRequest.verificationOptions);
        // Ensure selection was sane
        assert selection != null;
        // Call helper functions depending on selection
        switch (selection) {
            case StudyRequest.VERIFY_DNS:
                return addVerifyDNS();
            case StudyRequest.VERIFY_FILE:
                return addVerifyFile();
            case StudyRequest.VERIFY_META:
                return addVerifyMeta();
            default:
                return -42;
        }
    }


    /**
     * Inform the user about DNS verification, check if she still wants to use it, and verify that the verification
     * token was actually placed in the DNS
     * @return TODO
     */
    private int addVerifyDNS() {
        // Ensure variables are sane
        assert request != null;
        assert request.pubkey != null;
        try {
            // Show instructions
            println(String.format(StudyRequest.VERIFY_DNS_DESC_LONG,
                    new URL(request.webpage).getHost(),
                    RSA.fingerprint(request.pubkey)));
            // Ask if method should be used
            if (!yes("Are you sure you want to use this verification system?")) {
                return addVerificationStrategy();
            }
            // TODO Implement actual logic
        } catch (MalformedURLException e) {
            // This should not happen, as the URL has been verified already
            e.printStackTrace();
        }
        return -42;
    }


    /**
     * Inform the user about how file-based verification works, check if she still wants to use it, and verify that the
     * verification token was placed in the correct location
     * @return TODO
     */
    private int addVerifyFile() {
        assert request != null;
        assert request.pubkey != null;
        // TODO
        println("NotImplemented");
        return addVerificationStrategy();
    }


    /**
     * Inform the user about how meta-tag-based verification works, check if she still wants to use it, and verify that
     * the verification token was placed in the correct location
     * @return TODO
     */
    private int addVerifyMeta() {
        assert request != null;
        assert request.pubkey != null;
        // TODO
        println("NotImplemented");
        return addVerificationStrategy();
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
