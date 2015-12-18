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
    public boolean OpenDatabase() {
        // TODO
        return true;
    }

    /**
     * Create a new request
     */
    public void NewRequest() {
        // TODO
    }

    /**
     * Show the main menu of the application
     */
    public void MainMenu() {
        // Define the main menu options
        String[] menuOptions = {"New research request", "View active research data", "Quit"};
        // Print the program header
        println("    Denul Research Client\n");
        // Main loop
        boolean running = OpenDatabase();
        while (running) {
            // Ask for menu selection
            int selection = readSelection("Welcome to the Denul Research Client. Please choose what you want to do:", menuOptions);
            switch (selection) {
                case 0:
                    // New research request
                    NewRequest();
                    break;
                case 1:
                    // View existing research requests
                    println("NotImplemented");
                    break;
                case 2:
                    running = false;
                    break;
                default:
                    println("NotImplemented - wtf?");
            }
        }
    }
}
