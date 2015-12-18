package de.velcommuta.denul;

import de.velcommuta.denul.util.Input;

/**
 * Main Class
 */
public class Main {
    /**
     * Main function
     * @param args Arguments
     */
    public static void main(String[] args) {
        System.out.println(Input.readLines("Hello good sir"));
        System.out.println(Input.readSelection("Hi there", new String[] {"Hey", "Hoy", "Hi there"}));
        Input.yes("Annoyed yet?");
    }
}
