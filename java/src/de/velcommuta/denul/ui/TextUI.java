package de.velcommuta.denul.ui;

// Static-import a bunch of methods for more concise code
import static de.velcommuta.denul.util.Output.println;
import static de.velcommuta.denul.util.Output.print;
import static de.velcommuta.denul.util.Input.readLine;
import static de.velcommuta.denul.util.Input.readLines;
import static de.velcommuta.denul.util.Input.readSelection;
import static de.velcommuta.denul.util.Input.yes;

/**
 * Text-based UI for use on the console
 */
public class TextUI {
    /**
     * Empty constructor - not very interesting
     */
    public TextUI() {}


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
        // TODO
        println("");
        println("All questions are modelled after the medical study information sheet of the");
        println("Office for the Protection of Research Subjects of the University of Sourthern");
        println("California (USC), which can be found at");
        println("http://oprs.usc.edu/files/2013/04/Informed-Consent-Booklet-4.4.13.pdf");
        println("Please refer to that document to learn more about the individual questions.");
        println("");
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
