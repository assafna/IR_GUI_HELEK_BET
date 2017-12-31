package main;

/**
 * Represents a doc
 */
public class Doc {

    private String name;
    private String date;
    private String text;
    private String file;
    private int length;
    private int mostCommonTerm;



    Doc(String name, String date, String text, String file) {
        this.name = name;
        this.date = date;
        this.text = text;
        this.file = file;

    }

    Doc(String name, int length, int mostCommonTerm, String date, String file) {
        this.name = name;
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public String toString() {
        return name;
    }
}