package de.velcommuta.denul.ui;

import de.velcommuta.denul.crypto.ECDHKeyExchange;
import de.velcommuta.denul.crypto.RSA;
import de.velcommuta.denul.data.StudyRequest;
import de.velcommuta.denul.database.Database;
import de.velcommuta.denul.database.SQLiteDatabase;
import de.velcommuta.denul.networking.DNSVerifier;
import de.velcommuta.denul.networking.HttpsVerifier;
import de.velcommuta.denul.util.AsyncKeyGenerator;
import de.velcommuta.denul.util.StudyManager;

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
import static de.velcommuta.denul.util.Input.readInt;
import static de.velcommuta.denul.util.Input.yes;
import static de.velcommuta.denul.util.Input.confirm;
import static de.velcommuta.denul.util.Input.confirmCancel;

/**
 * Text-based UI for use on the console
 */
public class TextUI {
    private StudyRequest request;
    private Database mDatabase;

    /**
     * Open the local database and ask for a password, if needed
     * @return true if the database was successfully opened, false otherwise
     */
    private boolean openDatabase() {
        mDatabase = new SQLiteDatabase();
        return true;
    }


    /**
     * Create a new request
     */
    public void newRequest() {
        // Start key generation in background
        FutureTask<KeyPair> rsagen = AsyncKeyGenerator.generateRSA(4096);
        FutureTask<ECDHKeyExchange> ecdhgen = AsyncKeyGenerator.generateECDH();

        // Create new study request
        request = new StudyRequest();
        // randomize Queue Identifier
        request.randomizeQueueIdentifier();

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
        println("You will now be asked to add at least one investigator to the study.\n");
        request.investigators.addAll(addInvestigators());

        // Add data requests
        println("Now, you will be asked to add at least one type of data you are interested in.\n");
        request.requests.addAll(addDataRequests());

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
            // Make sure we did not get null values
            assert keys != null;
            assert exchange != null;
        } catch (InterruptedException | ExecutionException e) {
            println("Something went wrong during key generation, aborting");
            e.printStackTrace();
            return;
        }
        request.pubkey = keys.getPublic();
        request.privkey = keys.getPrivate();
        request.exchange = exchange;

        // Get verification strategy
        request.verification = addVerificationStrategy();

        if (yes("Upload the study to the server now (otherwise it will be discarded)?")) {
            StudyManager.registerStudy(request, mDatabase);
        }
    }


    /**
     * Ask the user to input the details of at least one, potentially more, Investigator(s)
     * @return A List of {@link StudyRequest.Investigator}s
     */
    private List<StudyRequest.Investigator> addInvestigators() {
        // Prepare List to return
        List<StudyRequest.Investigator> rv = new LinkedList<>();
        // Create new investigator object
        StudyRequest.Investigator investigator = new StudyRequest.Investigator();

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
     * Ask the user to input the details of at least one, potentially more, types of data required for the study
     * @return A List of {@link de.velcommuta.denul.data.StudyRequest.DataRequest}s
     */
    private List<StudyRequest.DataRequest> addDataRequests() {
        // Prepare List to return
        List<StudyRequest.DataRequest> rv = new LinkedList<>();
        // Create DataRequest object
        StudyRequest.DataRequest request = new StudyRequest.DataRequest();

        // Query the user for the details
        request.type = readSelection("Please select which type of data you are interested in", StudyRequest.DataRequest.TYPES);
        if (request.type == StudyRequest.DataRequest.TYPE_GPS) {
            request.granularity = readSelection("Please select the granularity you need the data in", StudyRequest.DataRequest.GRANULARITIES_GPS);
        } else {
            println("NotImplemented :(");
        }
        // TODO Add frequency
        request.frequency = 0;
        rv.add(request);

        if (yes("Do you want to add another type of data?")) {
            println("");
            rv.addAll(addDataRequests());
        }
        // Return
        return rv;
    }

    /**
     * Ask the user which verification strategy she wants to use to authenticate the study request
     * @return One of the VERIFY_* constants defined in {@link StudyRequest} indicating the chosen verification strategy
     */
    private int addVerificationStrategy() {
        // Ensure values are sane
        assert request != null;
        assert request.pubkey != null;
        // Display and read selection
        String selection = readSelection("\nPlease select how you want to authenticate your request", StudyRequest.verificationOptions);
        // Ensure selection was sane
        assert selection != null;
        println("");
        // Call helper functions depending on selection
        switch (selection) {
            case StudyRequest.VERIFY_DNS_TITLE:
                return addVerifyDNS();
            case StudyRequest.VERIFY_FILE_TITLE:
                return addVerifyFile();
            case StudyRequest.VERIFY_META_TITLE:
                return addVerifyMeta();
            case StudyRequest.VERIFY_SKIP:
                println("Verification skipped on your request");
                return -1337;
            default:
                println("NotImplemented");
                return -42;
        }
    }


    /**
     * Inform the user about DNS verification, check if she still wants to use it, and verify that the verification
     * token was actually placed in the DNS
     * @return One of the VERIFY_* constants defined in {@link StudyRequest} indicating the chosen verification strategy
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
            // Give the user time to add the DNS entry
            confirm("Please add the DNS record now. Afterwards, hit enter to check if it works");
            // Check DNS entry, and keep checking until it works
            boolean okay = DNSVerifier.verify(request);
            while (!okay) {
                println("Something seems to be wrong, I could not find the correct TXT record...");
                if (confirmCancel("Please double-check and hit enter to try again")) {
                    break;
                }
                okay = DNSVerifier.verify(request);
            }
            if (okay) {
                // Verification was successful
                println("Verification successful.");
                return StudyRequest.VERIFY_DNS;
            } else {
                // Verification was cancelled
                println("Verification cancelled.\n");
                return addVerificationStrategy();
            }

        } catch (MalformedURLException e) {
            // This should not happen, as the URL has been verified already
            e.printStackTrace();
            println("This was unexpected. Starting over...\n");
        }
        return addVerificationStrategy();
    }


    /**
     * Inform the user about how file-based verification works, check if she still wants to use it, and verify that the
     * verification token was placed in the correct location
     * @return One of the VERIFY_* constants defined in {@link StudyRequest} indicating the chosen verification strategy
     */
    private int addVerifyFile() {
        assert request != null;
        assert request.pubkey != null;
        try {
            println(String.format(StudyRequest.VERIFY_FILE_DESC_LONG,
                    new URL(new URL(request.webpage), ".study.txt"),
                    RSA.fingerprint(request.pubkey) + " # " + request.name));
            // Ask if method should be used
            if (!yes("Are you sure you want to use this verification system?")) {
                return addVerificationStrategy();
            }
            // Give the user time to add the DNS entry
            confirm("Please add the file now. Afterwards, hit enter to check if it works");
            // Check DNS entry, and keep checking until it works
            boolean okay = HttpsVerifier.verifyFile(request);
            while (!okay) {
                println("Something seems to be wrong, I could not find the correct file...");
                if (confirmCancel("Please double-check and hit enter to try again")) {
                    break;
                }
                okay = HttpsVerifier.verifyFile(request);
            }
            if (okay) {
                // Verification was successful
                println("Verification successful.");
                return StudyRequest.VERIFY_FILE;
            } else {
                // Verification was cancelled
                println("Verification cancelled.\n");
                return addVerificationStrategy();
            }
        } catch (MalformedURLException e) {
            // This should not happen, as the URL was already verified before
            e.printStackTrace();
            println("This was unexpected, starting over...\n");
        }
        return addVerificationStrategy();
    }


    /**
     * Inform the user about how meta-tag-based verification works, check if she still wants to use it, and verify that
     * the verification token was placed in the correct location
     * @return One of the VERIFY_* constants defined in {@link StudyRequest} indicating the chosen verification strategy
     */
    private int addVerifyMeta() {
        assert request != null;
        assert request.pubkey != null;
        assert request.webpage != null;

        // Print information
        println(String.format(StudyRequest.VERIFY_META_DESC_LONG,
                request.webpage,
                RSA.fingerprint(request.pubkey)));

        // Ask if method should be used
        if (!yes("Are you sure you want to use this verification system?")) {
            return addVerificationStrategy();
        }
        // Give the user time to add the DNS entry
        confirm("Please add the <meta> tag now. Afterwards, hit enter to check if it works");
        // Check DNS entry, and keep checking until it works
        boolean okay = HttpsVerifier.verifyMeta(request);
        while (!okay) {
            println("Something seems to be wrong, I could not find the <meta> tag...");
            if (confirmCancel("Please double-check and hit enter to try again")) {
                break;
            }
            okay = HttpsVerifier.verifyMeta(request);
        }
        if (okay) {
            // Verification was successful
            println("Verification successful.");
            return StudyRequest.VERIFY_META;
        } else {
            // Verification was cancelled
            println("Verification cancelled.\n");
            return addVerificationStrategy();
        }
    }


    /**
     * View active studies
     */
    public void viewActiveStudies() {
        println("");
        println("        Your active studies:");
        List<StudyRequest> sr = StudyManager.getMyStudies(mDatabase);
        if (sr.size() == 0) {
            println("You do not have any active studies - returning to main menu");
            println("");
            return;
        }
        int i = 1;
        for (StudyRequest req : sr) {
            println(i + "  " + req.name + " (" + StudyManager.getStudyParticipants(req, mDatabase).size() + " Participants)");
            i = i+1;
        }
        println("");
        print("Please select a study by entering its number, or enter 0 to return to the main menu: ");
        int select = readInt();
        while (select <= 0 || select > sr.size()) {
            if (select == 0) {
                println("");
                return;
            } else {
                print("That's not a valid selection. Please enter a number between 0 and " + sr.size() + ": ");
                select = readInt();
            }
        }
        // select now contains the number of the selected study
        // TODO Check for new data is a debugging helper, this should happen automagically
        int action = readSelection("Please select an action:", new String[] {"(debug) Check for new data", "View Data", "Delete Study", "Return to main menu"});
        if (action == 0) { // Update data
            StudyManager.updateStudyData(mDatabase, sr.get(select -1));
            println("Data updated");
            // Recursively return to the study list
        } else if (action == 1) { // View data
            println("NotImplemented"); // FIXME NotImplemented
        } else if (action == 2) { // Delete Study
            if (yes("Are you sure? This cannot be undone, and all data will be deleted.")) {
                StudyManager.deleteStudy(sr.get(select - 1), mDatabase);
                println("Study deleted");
            }
        } else if (action == 3) { // Return to main menu
            return;
        }
        viewActiveStudies();
    }


    /**
     * View settings
     */
    public void viewSettings() {
        println("NotImplemented");
    }


    /**
     * Show the main menu of the application
     */
    public void mainMenu() {
        // Define the main menu options
        // TODO Add imprint with license information for used libraries
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
