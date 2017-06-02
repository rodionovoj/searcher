package com.rojsn.searchengine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.tree.DefaultMutableTreeNode;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;
import org.apache.logging.log4j.LogManager;

public class SearchEngine {

    private static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(SearchEngine.class);
    public static final String BASE_DOC_FOLDER = "base_folder";
    public static String BASE_FOLDER;
    private int LEFT_OFFSET_SEARCH;
    private int RIGHT_OFFSET_SEARCH;
    private final String WIDTH = "width";
    private final String LEFT_OFFSET = "left_offset";
    private final String RIGHT_OFFSET = "right_offset";
    private int MAX_SIZE_OF_TEXT;
    private final String MAX_SIZE = "max_size";
    private final String MASKS_ALIAS = "masks";
    private static String encoding = "windows-1251";
    private Map<String, List<FormattedMatch>> mapOfFiles = new HashMap<>();
    private static boolean CASE_SENSITIVE_VALUE = true;
    private static boolean WHOLE_WORD_VALUE = true;
    private final String CASE_SENSITIVE = "case_sensitive";
    private final String WHOLE_WORD = "whole_word";    
    private final String whitespace_chars =  ""       /* dummy empty string for homogeneity */
                        + "\\u0009" // CHARACTER TABULATION
                        + "\\u000A" // LINE FEED (LF)
                        + "\\u000B" // LINE TABULATION
                        + "\\u000C" // FORM FEED (FF)
                        + "\\u000D" // CARRIAGE RETURN (CR)
                        + "\\u0020" // SPACE
                        + "\\u0085" // NEXT LINE (NEL) 
                        + "\\u00A0" // NO-BREAK SPACE
                        + "\\u1680" // OGHAM SPACE MARK
                        + "\\u180E" // MONGOLIAN VOWEL SEPARATOR
                        + "\\u2000" // EN QUAD 
                        + "\\u2001" // EM QUAD 
                        + "\\u2002" // EN SPACE
                        + "\\u2003" // EM SPACE
                        + "\\u2004" // THREE-PER-EM SPACE
                        + "\\u2005" // FOUR-PER-EM SPACE
                        + "\\u2006" // SIX-PER-EM SPACE
                        + "\\u2007" // FIGURE SPACE
                        + "\\u2008" // PUNCTUATION SPACE
                        + "\\u2009" // THIN SPACE
                        + "\\u200A" // HAIR SPACE
                        + "\\u2028" // LINE SEPARATOR
                        + "\\u2029" // PARAGRAPH SEPARATOR
                        + "\\u202F" // NARROW NO-BREAK SPACE
                        + "\\u205F" // MEDIUM MATHEMATICAL SPACE
                        + "\\u3000" // IDEOGRAPHIC SPACE
                        ;        

    public SearchEngine() {
        init();
    }

    public Map<String, List<FormattedMatch>> getMapFiles() {
        return mapOfFiles;
    }

    private void init() {
        try {
            InputStream cfg = new FileInputStream("app-config.xml");
            Properties pref = new Properties();
            pref.loadFromXML(cfg);
            BASE_FOLDER = (String) pref.getProperty(BASE_DOC_FOLDER);
            LEFT_OFFSET_SEARCH = Integer.parseInt((String) pref.getProperty(LEFT_OFFSET, "200"));
            RIGHT_OFFSET_SEARCH = Integer.parseInt((String) pref.getProperty(RIGHT_OFFSET, "200"));
            MAX_SIZE_OF_TEXT = Integer.parseInt((String) pref.getProperty(MAX_SIZE, "10000000"));
            encoding = System.lineSeparator().equals("\r\n") ? "windows-1251" : "UTF-8";
            CASE_SENSITIVE_VALUE = Boolean.parseBoolean((String) pref.getProperty(CASE_SENSITIVE));
            WHOLE_WORD_VALUE = Boolean.parseBoolean((String) pref.getProperty(WHOLE_WORD));
            mapOfFiles.clear();
        } catch (IOException | NumberFormatException e) {
            LOG.error("count=" + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SearchEngine we = new SearchEngine();
        we.init();
        File baseFile = new File(BASE_FOLDER);
        if (baseFile.isDirectory()) {
            we.fillOperatedFileNames(baseFile, "чебурек");
        }
    }

    public void fillOperatedFileNames(File baseFile, String regexp) {
        List<File> list = Arrays.asList(baseFile.listFiles());
        list.forEach((file) -> {
            if (file.isFile()) {
                extractContent(new ArrayList<>(), file.getAbsolutePath(), regexp);
            } else {
                fillOperatedFileNames(file.getAbsoluteFile(), regexp);
            }
        });
    }

    private void extractContent(List<FormattedMatch> list, String fullFileName, String regexp) {
        try {
            search(list, regexp, fullFileName);
        } catch (IOException | SAXException | TikaException e) {
            LOG.error(e.getMessage());
        }
    }

    private void search(List<FormattedMatch> matches, String regexp, String fileName) throws UnsupportedEncodingException, IOException, SAXException, TikaException {
        String text = parseToPlainText(fileName);        
       // init();
        Pattern pattern = null;
        if (WHOLE_WORD_VALUE) {
            regexp = "\\\\u0020" + regexp + "\\\\u0020";
        }
        if (!CASE_SENSITIVE_VALUE) {
            pattern = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.UNICODE_CASE);
        } else {           
            pattern = Pattern.compile(regexp);
        }
        Matcher matcher = pattern.matcher(text);
        int index = 1;
        while (matcher.find()) {
            FormattedMatch fm = new FormattedMatch();
            fm.setStart((matcher.start() < LEFT_OFFSET_SEARCH ? 0 : (matcher.start() - LEFT_OFFSET_SEARCH)));
            fm.setEnd((matcher.end() < RIGHT_OFFSET_SEARCH) ? matcher.end() : RIGHT_OFFSET_SEARCH + matcher.end());
            String dd = "";
            if (fm.getEnd() > text.length()) {
                dd = text.substring(fm.getStart());
            } else {
                dd = text.substring(fm.getStart(), fm.getEnd());
            }
            String ddd = dd.replaceAll(regexp, "<span>" + regexp + "</span>");
//            StringBuffer sb = new StringBuffer(dd);            
//            sb.insert(dd.indexOf(regexp), "<span>");
//            sb.insert(dd.indexOf(regexp) + regexp.length(), "</span>");
            fm.setTextMatch(ddd);
            fm.setIndex(index);
            matches.add(fm);
            index++;
            mapOfFiles.put(fileName, matches);
        }
        List<FormattedMatch> foundMatches = mapOfFiles.get(fileName);
        if (foundMatches != null && foundMatches.isEmpty()) {
            LOG.info("Nothing found for " + fileName);
        }
    }

    public void createNodes(DefaultMutableTreeNode top) {

        DefaultMutableTreeNode document = null;
        DefaultMutableTreeNode matchNode = null;
        Set keySet = mapOfFiles.keySet();
        Iterator it = keySet.iterator();
        while (it.hasNext()) {
            String fileName = (String) it.next();
            document = new DefaultMutableTreeNode(fileName);
            top.add(document);
            List<FormattedMatch> matches = mapOfFiles.get(fileName);
            for (FormattedMatch match : matches) {
                match.setFileName(fileName);
                matchNode = new DefaultMutableTreeNode(match);
                document.add(matchNode);
            }
        }
        if (mapOfFiles.isEmpty()) {
            document = new DefaultMutableTreeNode("В " + BASE_FOLDER + " выражение не найдено");
            top.add(document);
        }
    }
    
    boolean isEmpty() {
        return mapOfFiles.isEmpty();
    }

    public String parseToPlainText(String fileName) throws IOException, SAXException, TikaException {
        TikaConfig tikaConfig = new TikaConfig("tika-config.xml");
        BodyContentHandler handler = new BodyContentHandler(MAX_SIZE_OF_TEXT);
        AutoDetectParser parser = new AutoDetectParser(tikaConfig);
        Metadata metadata = new Metadata();
        try (InputStream stream = new FileInputStream(fileName)) {
            parser.parse(stream, handler, metadata);
            return handler.toString();
        }
    }

    /**
     * @return the WHOLE_WORD_VALUE
     */
    public static boolean isWholeWordValue() {
        return WHOLE_WORD_VALUE;
    }

    /**
     * @param aWHOLE_WORD_VALUE the WHOLE_WORD_VALUE to set
     */
    public static void setWholewWordValue(boolean aWHOLE_WORD_VALUE) {
        WHOLE_WORD_VALUE = aWHOLE_WORD_VALUE;
    }

    public static boolean isCaseSensitiveValue() {
        return CASE_SENSITIVE_VALUE;
    }

    public static void setCaseSensitiveValue(boolean aCASE) {
        CASE_SENSITIVE_VALUE = aCASE;
    }
}
