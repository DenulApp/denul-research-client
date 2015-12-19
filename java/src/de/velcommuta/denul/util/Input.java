package de.velcommuta.denul.util;

import de.velcommuta.denul.networking.HttpsConnection;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLProtocolException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.HashMap;

import static de.velcommuta.denul.util.Output.print;
import static de.velcommuta.denul.util.Output.println;

/**
 * Utility functions for interactive input from STDIN
 */
public class Input {
    /**
     * Read a single line from STDIN
     * @return The line, or null in case of an error
     */
    @NotNull
    private static String read() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        // Read until empty line is found
        try {
            return br.readLine();
        } catch (IOException e) {
            return "";
        }
    }


    /**
     * Read input from STDIN and make sure it's a number
     * @return The number that was entered
     */
    private static int readInt() {
        String rep = read();
        try {
            return Integer.parseInt(rep);
        } catch (NumberFormatException e) {
            print("That's not even a number. Try again: ");
            return readInt();
        }
    }


    /**
     * Read multiple lines from STDIN, terminated by an empty line
     * @param prompt The message that should be displayed as a question
     * @return The input lines
     */
    @NotNull
    public static String readLines(String prompt) {
        println(prompt + ":");
        println("(Finish your input with an empty line)");
        // Prepare variables
        StringBuilder input = new StringBuilder();
        String line;
        // Read until empty line is found
        while (!(line = read()).equals("")) {
            if (input.length() != 0) {
                input.append("\n");
            }
            input.append(line);
        }
        // return the result
        return input.toString();
    }


    /**
     * Read a single line from standard input
     * @param prompt The question to display. Will be postfixed with a ": " and displayed as the question
     * @return The input string
     */
    @NotNull
    public static String readLine(String prompt) {
        print(prompt + ": ");
        return read();
    }


    /**
     * Offer a number of options to the user and ask for a selection
     * @param prompt The prompt to display
     * @param options The options
     * @return The index of the selected option
     */
    public static int readSelection(String prompt, String[] options) {
        println(prompt);
        for (int i = 1; i <= options.length; i++) {
            println("  (" + i + ") " + options[i-1]);
        }
        print("Please select an option: ");
        int selection = readInt();
        while (selection - 1 >= options.length || selection - 1 < 0) {
            print("That's not an option. Try again: ");
            selection = readInt();
        }
        return selection - 1;
    }

    
    /**
     * Offer a number of options to the user and ask for a selection
     * @param prompt The prompt to display
     * @param options The options, mapping a short description string to a longer explanation string
     * @return The index of the selected option
     */
    public static String readSelection(String prompt, HashMap<String, String> options) {
        println(prompt);
        int i = 1;
        for (String key : options.keySet()) {
            println("  (" + i + ") " + key);
            println("      " + options.get(key));
            i++;
        }
        print("Please select an option: ");
        int selection = readInt();
        while (selection - 1 >= options.size() || selection - 1 < 0) {
            print("That's not an option. Try again: ");
            selection = readInt();
        }
        // Ugly way to determine the key associated with this index
        i = 1;
        for (String key : options.keySet()) {
            if (i == selection) return key;
            i++;
        }
        // Should be unreachable
        return null;
    }


    /**
     * Ask the user a yes/no question.
     * @param prompt The question. Will be postfixed with a "(y/n)"
     * @return True if the user selected y[es], false otherwise
     */
    public static boolean yes(String prompt) {
        print(prompt + " (y/n) ");
        String reply = read().toLowerCase();
        while (!(reply.equals("y") || reply.equals("n") || reply.equals("yes") || reply.equals("no"))) {
            reply = read().toLowerCase();
        }
        return (reply.equals("y") || reply.equals("yes"));
    }

    /**
     * Read a URL from stdin and ensure that it is a valid https-URL that does not 404
     * @param prompt The prompt to display. Will be postfixed with a ": "
     * @return The URL, guaranteed to be valid (at the time of the check)
     */
    public static String readHttpsURL(String prompt) {
        String url = readLine(prompt);
        try {
            if (HttpsConnection.exists(url)) {
                return url;
            } else {
                return readHttpsURL("The URL does not exist. Please enter a valid https:// URL");
            }
        } catch (MalformedURLException e) {
            return readHttpsURL("That is not a valid URL. The URL must begin with https://. Try again");
        } catch (SSLProtocolException e) {
            return readHttpsURL("Unable to establish a secure connection. Please ensure that you enter a URL\nwith a valid SSL certificate");
        } catch (UnknownHostException e) {
            return readHttpsURL("That domain does not exist. Try again");
        }
    }
}
