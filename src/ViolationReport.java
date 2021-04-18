import java.util.ArrayList;
import java.io.IOException;

/**
 * This class organizes the data for "magic number" violations in.
 * It also contains the method to scan the code for such violations. 
 * The method assumes that the code compiles.
 */
public class ViolationReport {
    private static final String CLEAN_MSG = "%s has no violations.";
    private static final String DIRTY_INTRO_MSG = "%s has %d violations:";
    private static final String DIRTY_LOG_MSG = "\n\t@ Line %d: %s";
    private static final String NOT_SCANNED_MSG = "%s not scanned.";

    private final String filename;
    private boolean isScanned;
    private ArrayList<ViolationInfo> violationsLog;
    private final MagicNumberScanner scanner;

    /**
     * Constructor for the report.
     * @param file The file to scan.
     */
    public ViolationReport(String file) {
        filename = file;
        isScanned = false;

        if (filename.indexOf(".java") == filename.length() - 5) {
            scanner = new JavaScanner();
        }
        else {
            scanner = null;
        }
    }

    /**
     * @return the report in an organized String format.
     */
    public String toString() {
        if (isClean()) {
            return String.format(CLEAN_MSG, filename);
        }
        else if (isDirty()) {
            StringBuilder report = new StringBuilder(String.format(DIRTY_INTRO_MSG, filename, violationsLog.size()));
            for (ViolationInfo entry : violationsLog) {
                report.append(String.format(DIRTY_LOG_MSG, entry.line, entry.violation));
            }
            return report.toString();
        }
        else {
            return String.format(NOT_SCANNED_MSG, filename);
        }
    }

    /**
     * Scans the file for "magic numbers".
     * @throws IOException for the FileReader.
     */
    public void executeScan() throws IOException {
        violationsLog = new ArrayList<>();
        try {
            scanner.scan(filename, violationsLog);
        }
        catch (IOException e) {
            System.out.println(e);
        }
        isScanned = true;
    }

    private boolean isClean() {
        return isScanned && violationsLog.size() == 0;
    }

    private boolean isDirty() {
        return isScanned && violationsLog.size() > 0;
    }

    /**
     * Keeps track of the lines with violations. Some lines might have multiple
     * instances.
     */
    static class ViolationInfo {
        int line;
        String violation;

        public ViolationInfo(int _line, String _violation)  {
            line = _line;
            violation = _violation;
        }
    }
}