package com.rojsn.searchengine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
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
import org.apache.logging.log4j.Logger;

public class SearchEngine {

    public static boolean isCaseSensitiveValue() {
        return CASE_SENSITIVE_VALUE;
    }

    public static void setCaseSensitiveValue(boolean aCASE) {
        CASE_SENSITIVE_VALUE = aCASE;
    }

    private static final Logger LOG = LogManager.getLogger(SearchEngine.class);
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
    private final String CASE_SENSITIVE = "case_sensitive";
    private static int fileNumber;
    private Map extentionMap = new HashMap();
    static boolean workAllowed = false;

    public SearchEngine() {
        init();
    }

    public Map<String, List<FormattedMatch>> getMapFiles() {
        return mapOfFiles;
    }

    private void init() {
        try {
            InputStream cfg = new FileInputStream("config.xml");
            Properties pref = new Properties();
            pref.loadFromXML(cfg);
            BASE_FOLDER = (String) pref.getProperty(BASE_DOC_FOLDER);
            LEFT_OFFSET_SEARCH = Integer.parseInt((String) pref.getProperty(LEFT_OFFSET, "200"));
            RIGHT_OFFSET_SEARCH = Integer.parseInt((String) pref.getProperty(RIGHT_OFFSET, "200"));
            MAX_SIZE_OF_TEXT = Integer.parseInt((String) pref.getProperty(MAX_SIZE, "10000000"));
            encoding = System.lineSeparator().equals("\r\n") ? "windows-1251" : "UTF-8";
            setCaseSensitiveValue(Boolean.parseBoolean((String) pref.getProperty(CASE_SENSITIVE, "true")));
            workAllowed = true;

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
        if (workAllowed) {
            for (File file : list) {
                if (file.isFile()) {
                    fileNumber++;
                    System.out.println("fileNumber = " + fileNumber);
                    System.out.println("extension = " + getExtention(file.getName()));
                    fillExtentionMap(getExtention(file.getName()));
                    extractContentDocx(new ArrayList<>(), file.getAbsolutePath(), regexp);
                } else {
                    fillOperatedFileNames(file.getAbsoluteFile(), regexp);
                }
            }
        }
    }

    private void extractContentDocx(List<FormattedMatch> list, String fullFileName, String regexp) {
        try {
            search(list, regexp, fullFileName);
        } catch (IOException | SAXException | TikaException e) {
            LOG.error(e.getMessage());
        }
    }

    private void search(List<FormattedMatch> matches, String regexp, String fileName) throws UnsupportedEncodingException, IOException, SAXException, TikaException {
        String text = parseToPlainText(fileName);
        Pattern pattern = Pattern.compile(CASE_SENSITIVE_VALUE ? regexp : regexp.toUpperCase());
        Matcher matcher = pattern.matcher(CASE_SENSITIVE_VALUE ? text : text.toUpperCase());
        String startHTML = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body>";
        String endHTML = "</body></html>";
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
            String ddd = dd.replace(regexp, "<b><i>" + regexp + "</i></b>");
            fm.setTextMatch(startHTML + ddd + endHTML);
//            fm.setTextMatch(startHTML + "<a href=\"" + dd + "\">" + dd + "</a>" + endHTML);

            try (BufferedWriter bw = new BufferedWriter(new FileWriter("FILENAME.html"))) {
                bw.write(startHTML + ddd + endHTML);

//            } catch (FileAlreadyExistsException x) {
//                System.err.format("file named %s"
//                        + " already exists%n", fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
            URL u = new File("FILENAME.html").toURL();
            fm.setUrl(u);
            fm.setTextMatch(dd);
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

    private String getExtention(String name) {
        int position = name.lastIndexOf(".");
        if (position > 0) {
            return name.substring(position + 1);
        } else {
            return "";
        }        
    }

    private void fillExtentionMap(String extension) {
        String extentionNumber = (String) extentionMap.get(extension);
        if (extentionNumber == null) {
            extentionNumber = "0";
        }
        extentionMap.put(extension , String.valueOf(Integer.parseInt(extentionNumber)+1));        
    }
}
