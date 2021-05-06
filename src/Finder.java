import java.util.Scanner;
import java.io.IOException;

/**
 * The execution point for the magic number finder.
 */
public class Finder {
    public static void main(String[] args) throws IOException {
        Scanner terminal = new Scanner(System.in);
        System.out.print("Enter file to scan: ");
        String file = terminal.nextLine();
        terminal.close();

        MagicNumReport report = new MagicNumReport(file);
        report.executeScan();
        System.out.println(report.toString());
    }
}