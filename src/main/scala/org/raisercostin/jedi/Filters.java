package org.raisercostin.jedi;

import com.google.common.base.Splitter;
import com.google.common.collect.Streams;
import io.vavr.collection.Stream;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.io.filefilter.FileFilterUtils.nameFileFilter;

public class Filters {
    /**
     * Sample gitIgnores:
     * <pre>
     *     # for now only folders
     *     target
     *     .git
     *     .mvn
     * </pre>
     */
    public static PathMatcher createGitFilter(String gitIgnores, boolean ignoreCase) {
        return new FileTraversals.FileFilterPathMatcherAdapter(FileTraversals.createGitFilter(gitIgnores, ignoreCase));
    }

    /**
     * See https://www.concretepage.com/java/jdk7/example-pathmatcher-java-nio
     */
    public static PathMatcher createGlob(String globExpression) {
        return FileSystems.getDefault().getPathMatcher("glob:" + globExpression);
    }

    /**
     * From TotalCommander help
     * <pre>
     * In this field, you can enter the search mask for the files you want to find. A question mark ? stands for exacly one character, and an asterisk * stands for any number of characters. Multiple search masks can be entered, separated by spaces (see examples below). All masks after the pipe character | will be treated as exclude masks.
     * Note: Names with spaces do not need to be put in double quotes, e.g. Letter to Mr. Smith.doc. Total Commander now looks for both the entire name and the name parts individually. However, when the pipe character | is used for an exclude mask, names with spaces must be put in double quotes.
     * When the search string contains a dot, Total Commander will look for the exact name match.
     * Use Shift+Del to remove no longer wanted entries from the search history.
     * To include/exclude certain directories in the search, wildcards can be used in include/exclude directory names, and the names must have a trailing backslash \ .
     * Put '>' character in this field before saving the search to keep the "Search for" field unchanged when loading the saved search.
     * Examples:
     * *.ini finds for example win.ini
     * Smith finds "Letter to Mr. Smith.doc"
     * *.bak *.sik *.old  finds all backup files with these extensions
     * *n.ini now finds names which must contain an 'n' in front of the dot.
     * *wof*.doc finds all names containing "wof" in the name and an extension ".doc".
     * w*.*|*.bak *.old finds files, which start with w and do not end with .bak or .old.
     * *.ini | windows\ finds all ini files except those in directories called "Windows" and their subdirs.
     * *.htm? | _vti*\ finds all html files, except in subdirs starting with _vti (used by Frontpage)
     * windows\ system32\ *.ini  finds ini files only in windows\ and system32 dirs
     * Put "ev:" in front of the search string to pass it to Everything unchanged. You will then have to use the Everything search syntax, see www.voidtools.com.
     * New: Put "ed:" in front of the search string: Like "ev:", but search only in the directories specified in the "Search in" field. Also handles "Search subdirectories" option. This is handled by prefixing path:c:\path and optionally parents:<nr to the entered search string.
     * </pre>
     */
    public static PathMatcher createTotalCommanderExpression(String totalcmdExpression) {
        String glob = Stream.ofAll(Splitter.onPattern("\\s+").split(totalcmdExpression)).map(x->"**/"+x).mkString(",");
        return createGlob("{"+glob+"}");
    }
}
