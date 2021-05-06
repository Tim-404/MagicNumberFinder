import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * The Java version of MagicNumberScanner.
 * 
 * Algorithm summary:
 * - scan each statement for double quotes, singles quotes or numbers
 * - when found, search for constant identifiers "static" and "final"
 *   - if constant identifiers not found, log line number and instance
 *   - jump to end of instance and continue scanning
 */
public class JavaScanner implements MagicNumberScanner {
    private int lineNum;

    /**
     * Scans the file and reports magic numbers in the log.
     * 
     * @param filename the name of the file to scan
     * @param log the log to report any violations
     * @return the number of violations in the file.
     * @throws IOException if the file is not read properly.
     */
    public void scan(String filename, ArrayList<MagicNumReport.MagicNumInfo> log) throws IOException {
        lineNum = 1;
        // assumes the file isn't large
        // may rewrite this in case the above isn't true (someone tries to troll)
        String code = new String(Files.readAllBytes(Paths.get(filename)));
        scanSection(code, 0, code.length(), log);
    }

    /**
     * Scans a subsection of the code. Used when braces are encountered.
     * 
     * @param code the code in a string
     * @param start the start position, inclusive
     * @param end the end position, exclusive
     * @param log the log to report any violations
     * @return the number of violations found in the section.
     */
    private void scanSection(String code, int start, int end, ArrayList<MagicNumReport.MagicNumInfo> log) {
        int beginStatement = start;
        int currTracker = start;

        while (currTracker < end) {
            char c = code.charAt(currTracker);
            switch (c) {
                case '\n':
                    ++lineNum;
                    ++currTracker;
                    break;

                case '/':
                    currTracker = detectAndSkipComment(code, currTracker, true);
                    break;

                case '{':
                    int afterBrace = currTracker + 1;
                    currTracker = skipPairedBrace(code, currTracker);
                    // avoid unnecessary recursion
                    if (!isInSafeContext(code, beginStatement, afterBrace - 2)) {
                        scanSection(code, afterBrace, currTracker - 1, log);
                    }
                    beginStatement = currTracker;

                    break;

                case ';':
                    ++currTracker;
                    beginStatement = currTracker;
                    break;

                case '"':
                    int firstQuote = currTracker;
                    currTracker = findCloseQuote('"', code, currTracker, true);
                    // handle magic num case
                    if (!isInSafeContext(code, beginStatement, currTracker)) {
                        log.add(new MagicNumReport.MagicNumInfo(lineNum, code.substring(firstQuote, currTracker)));
                    }
                    break;

                case '\'':
                    int firstSingleQuote = currTracker;
                    currTracker = findCloseQuote('\'', code, currTracker, true);
                    // handle magic num case
                    if (!isInSafeContext(code, beginStatement, currTracker)) {
                        log.add(new MagicNumReport.MagicNumInfo(lineNum, code.substring(firstSingleQuote, currTracker)));
                    }
                    break;

                default:
                    // do further analysis
                    if (Character.isDigit(c)) {
                        if (!isPartOfIdentifier(code, currTracker) 
                                && !isValidStrayNum(code, currTracker) 
                                && !isInSafeContext(code, beginStatement, currTracker)) {
                            // get magic number and create report
                            log.add(new MagicNumReport.MagicNumInfo(lineNum, code.substring(currTracker, skipNumber(code, currTracker))));
                            currTracker = skipNumber(code, currTracker);
                        }
                        else {
                            currTracker = skipIdentifier(code, currTracker);
                        }
                    }
                    else {
                        ++currTracker;
                    }
            }
        }
    }


    /* Code jumping */

    /**
     * Finds the brace that corresponds to the brace at the given position.
     * Does NOT update the line count.
     * 
     * @param code the code in a string
     * @param pos the position of the brace
     * @return the positions of the corresponding brace.
     */
    private int skipPairedBrace(String code, int pos) {
        int nestLevel = 1;
        ++pos;
        while (nestLevel > 0) {
            char c = code.charAt(pos);
            switch (c) {
                case '/':
                    pos = detectAndSkipComment(code, pos, false);
                    break;
                case '"':
                    pos = findCloseQuote('"', code, pos, false);
                    break;
                case '\'':
                    pos = findCloseQuote('\'', code, pos, false);
                    break;
                case '{':
                    ++nestLevel;
                    ++pos;
                    break;
                case '}':
                    --nestLevel;
                    ++pos;
                    break;
                default:
                    ++pos;
            }
        }

        return pos;
    }

    /**
     * Finds the next instance of the given character in the code. 
     * Intended for single and double quotes, as the method will not skip over them.
     * 
     * @param c the character to search for
     * @param code the code in a string
     * @param pos the position of the previous instance of the character 
     * @return the position after the next identical character.
     */
    private int findCloseQuote(char c, String code, int pos, boolean trackLine) {
        ++pos;
        while (pos < code.length()) {
            char curr = code.charAt(pos);
            if (curr == '\n' && trackLine) {
                ++lineNum;
            }
            if (curr == '/') {
                pos = detectAndSkipComment(code, pos, trackLine);
                continue;
            }
            if (curr == '\\') {
                ++pos;
            }
            if (curr == c) {
                return pos + 1;
            }
            ++pos;
        }

        return pos;
    }


    /* Handle comments */

    /**
     * Determines if the characters are a comment, then redirects to the appropriate method.
     * 
     * @param code the code in a string
     * @param pos the position of the first slash
     * @return the position after the end of the comment. If there is a false alarm, the next 
     * position is returned.
     */
    private int detectAndSkipComment(String code, int pos, boolean trackLine) {
        if (code.charAt(pos) == '/') {
            if (code.charAt(pos + 1) == '/') {
                return skipSingleLineComment(code, pos + 2, trackLine);
            }
            if (code.charAt(pos + 1) == '*') {
                return skipMultLineComment(code, pos + 2, trackLine);
            }
        }

        return pos + 1;
    }

    /**
     * Jumps over a single line comment.
     * 
     * @param code the code in a string
     * @param pos the position where the comment starts
     * @return the position where the comment ends.
     */
    private int skipSingleLineComment(String code, int pos, boolean trackLine) {
        if (trackLine) {
            ++lineNum;
        }
        int index = code.indexOf('\n', pos);
        return index == -1 ? code.length() : index + 1;
    }

    /**
     * Jumps over a multiline comment.
     * 
     * @param code the code in a string
     * @param pos the position where the comment starts
     * @return the position where the comment ends.
     */
    private int skipMultLineComment(String code, int pos, boolean trackLine) {
        int endComment = code.indexOf("*/", pos);
        while (trackLine && pos < endComment) {
            if (code.charAt(pos) == '\n') {
                ++lineNum;
            }
            ++pos;
        }
        return endComment + 2;
    }

    private int skipIdentifier(String code, int pos) {
        do {
            ++pos;
        } while (isValidIdentifierChar(code.charAt(pos)));
        return pos;
    }

    private int skipNumber(String code, int pos) {
        do {
            ++pos;
        } while (isValidNumChar(code.charAt(pos)));
        return pos;
    }

    /* Determine if number is part of an identifier */

    /**
     * Determines if a number is part of an identifier (variable/method name).
     * 
     * @param code the code in a string
     * @param pos the position of the number
     * @return true if the number is part of an identifier.
     */
    private boolean isPartOfIdentifier(String code, int pos) {
        char lastChar = code.charAt(pos);
        while (isValidIdentifierChar(code.charAt(--pos))) {
            lastChar = code.charAt(pos);
            if (pos == 0) break;
        }

        return isValidIdentifierCharNotNum(lastChar);
    }

    private boolean isValidIdentifierChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    private boolean isValidIdentifierCharNotNum(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private boolean isValidNumChar(char c) {
        return Character.isDigit(c) 
                || c == '.'                 // decimal points
                || c == 'x' || c == 'b'     // bin and hex
                || c == 'f' || c == 'L';    // floats and longs
    }

    /**
     * Determines if a number found in the code is an exception.
     * 
     * @param code the code in a string
     * @param pos the position of the first digit
     * @return true if the number is valid.
     */
    private boolean isValidStrayNum(String code, int pos) {
        char digit = code.charAt(pos);
        char nextChar = code.charAt(pos + 1);
        if (Character.isDigit(nextChar) 
                || (digit == '0' && (nextChar == 'x' || nextChar == 'b'))   /* check for binary or hex */
				|| nextChar == '.'	/* check for decimal point */	) {
            return false;
        }
        return digit == '1' || digit == '0';
    }


    /* Skipping over private static final vars. */

    /**
     * Determines is the statement has the modifiers "static" and "final".
     * 
     * @param code the code in a string
     * @param start the start of the statement
     * @param end the right bound to check for the modifiers
     * @return true if the magic number is in the proper context.
     */
    private boolean isInSafeContext(String code, int start, int end) {
        return searchCode("static", code, start, end) && searchCode("final", code, start, end);
    }

    /**
     * Searches the code for a certain word within a given range.
     * 
     * @param word the word to search for
     * @param code the code as a string
     * @param start the left bound of the search
     * @param end the right bound of the search
     * @return true if the word is found, otherwise false.
     */
    private boolean searchCode(String word, String code, int start, int end) {
        for (int i = start; i < end - word.length() + 1; ++i) {
            char c = code.charAt(i);
            if (c == '/') {
                i = detectAndSkipComment(code, i, false) - 1;
                continue;
            }
            if (c == '\"') {
                i = findCloseQuote('\"', code, i, false) - 1;
                continue;
            }
            if (c == word.charAt(0) && !isValidIdentifierChar(code.charAt(i - 1))) {    // check for start of word
                int step = wordMatch(word, code, i, end);
                if (step == word.length() && !isValidIdentifierChar(code.charAt(i + step))) {   // check for end of word
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines if a word matches lexicographically with the code starting from a given position.
     * 
     * @param word the word to match
     * @param code the code as a string
     * @param codePos the position to start at
     * @param codeEnd the last valid position in the code to match
     * @return the number of letters that match up.
     */
    private int wordMatch(String word, String code, int codePos, int codeEnd) {
        int i = 0;
        for (; i < word.length() && codePos + i <= codeEnd; ++i) {
            if (word.charAt(i) != code.charAt(codePos + i)) {
                break;
            }
        }
        return i;
    }
}
