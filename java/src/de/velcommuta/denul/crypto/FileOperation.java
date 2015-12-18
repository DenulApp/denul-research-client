package de.velcommuta.denul.crypto;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Wrapper for the horrible BouncyCastle / SpongyCastle API
 */
public class FileOperation {

    private static final Logger logger = Logger.getLogger(FileOperation.class.getName());


    /**
     * Securely (or at least: as securely as we can manage) delete a file
     * @param f The file object to be deleted
     * @return True if the deletion succeeded, false otherwise
     */
    public static boolean secureDelete(java.io.File f) {
        try {
            logger.fine("secureDelete: Beginning deletion");
            RandomAccessFile fobj = new RandomAccessFile(f, "rw");
            long length = fobj.length(); // Cast to int as the file will most likely not be too large
            fobj.close();
            if (length < Integer.MAX_VALUE) {
                int len = (int) length;
                for (int i = 0; i < 50; i++) {
                    // Open the file for writing
                    fobj = new RandomAccessFile(f, "rw");
                    // Get ourselves some random bytes
                    byte[] random = new byte[len];
                    new Random().nextBytes(random);
                    // Overwrite the current contents of the file
                    fobj.write(random, 0, random.length);
                    fobj.close();
                }
                logger.fine("secureDelete: Overwrite cycles finished, deleting file");
                return f.delete();
            } else {
                // This case is extremely unlikely
                // TODO Implement deletion for very long files
                logger.severe("secureDelete: File is too long, aborting :(");
                return false;
            }
        } catch (FileNotFoundException e) {
            logger.severe("secureDelete: File does not exist");
            return false;
        } catch (IOException e) {
            logger.severe("secureDelete: IOError while attempting deletion: " + e.getMessage());
            return false;
        }
    }
}
