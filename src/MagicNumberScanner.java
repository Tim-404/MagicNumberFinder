import java.io.IOException;
import java.util.ArrayList;

/**
 * This is a functional interface for the scanner of the file. 
 * Different file scanners can be used for different languages.
 */
public interface MagicNumberScanner {
    void scan(String filename, ArrayList<ViolationReport.ViolationInfo> log) throws IOException;
}
