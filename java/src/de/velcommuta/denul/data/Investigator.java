package de.velcommuta.denul.data;

/**
 * Data holder class for Investigators associated with a study
 */
public class Investigator {
    public String name;
    public String institution;
    public String group;
    public String position;

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Investigator Name: ");
        builder.append(name);

        builder.append("\nInstitution: ");
        builder.append(institution);

        builder.append("\nGroup: ");
        builder.append(group);

        builder.append("\nPosition: ");
        builder.append(position);

        builder.append("\n");
        return builder.toString();
    }
}
