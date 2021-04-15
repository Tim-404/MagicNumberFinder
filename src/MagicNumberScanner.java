import java.io.IOException;
import java.util.ArrayList;

public interface MagicNumberScanner {
    void scan(String filename, ArrayList<ViolationReport.ViolationInfo> log) throws IOException;
}
