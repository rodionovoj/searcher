package com.rojsn.searchengine;

import java.net.URL;

/**
 *
 * @author sbt-rodionov-oy
 */
public class FormattedMatch {

    private int start;
    private int end;
    private int index;
    private String textMatch;
    private String fileName;
    private URL url;

    /**
     * @return the start
     */
    public int getStart() {
        return start;
    }

    /**
     * @param start the start to set
     */
    public void setStart(int start) {
        this.start = start;
    }

    /**
     * @return the end
     */
    public int getEnd() {
        return end;
    }

    /**
     * @param end the end to set
     */
    public void setEnd(int end) {
        this.end = end;
    }

    /**
     * @return the textMatch
     */
    public String getTextMatch() {
        return textMatch;
    }

    /**
     * @param textMatch the textMatch to set
     */
    public void setTextMatch(String textMatch) {
        this.textMatch = textMatch;
    }
    
    public String toString() {
        return ":\n Совпадение №" + this.getIndex() + " в диапазоне: от " +  this.getStart() + " до " + this.getEnd() + ":\n" + this.textMatch;        
    }

    /**
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * @param index the index to set
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return the url
     */
    public URL getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(URL url) {
        this.url = url;
    }
}
