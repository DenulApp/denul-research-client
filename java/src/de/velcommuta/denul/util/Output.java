package de.velcommuta.denul.util;

/**
 * Wrapper functions for text output
 */
public class Output {
    /**
     * Print a String, followed by a linebreak
     * @param line The String
     */
    public static void println(String line) {
        assert line != null;
        System.out.println(line);
    }

    /**
     * Print a String, without a trailing newline
     * @param line The String
     */
    public static void print(String line) {
        assert line != null;
        System.out.print(line);
    }
}
