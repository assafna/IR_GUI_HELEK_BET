package main;

/**
 * Represents a doc
 */
public class Doc {

    private String name;
    private String code;
    private String text;
    private int length;
    private int mostCommonTerm;
    private String date;
    private String file;




    Doc(String name, String code,String text, String date, String file) {
        this.name = name;
        this.code = code;
        this.text = text;
        this.date = date;
        this.file = file;

    }

    Doc(String name, String code, int length, int mostCommonTerm, String date, String file) {
        this.name = name;
        this.code = code;
        this.length = length;
        this.mostCommonTerm= mostCommonTerm;
        this.date = date;
        this.file = file;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getMostCommonTerm() {
        return mostCommonTerm;
    }

    public void setMostCommonTerm(int mostCommonTerm) {
        this.mostCommonTerm = mostCommonTerm;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return name;
    }
}