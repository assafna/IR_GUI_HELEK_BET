package main;

/**
 * Represents a doc
 */
public class Doc {

    private String code;
    private String text;
    private int length;
    private int mostCommonTerm;
    private String date;
    private String file;
    private double squaredWeightSum;

    Doc(String code, String text, String date, String file) {
        this.code = code;
        this.text = text;
        this.date = date;
        this.file = file;

    }

    public Doc(String code, int length, int mostCommonTerm, String date, String file, double weight) {
        this.code = code;
        this.length = length;
        this.mostCommonTerm = mostCommonTerm;
        this.date = date;
        this.file = file;
        this.squaredWeightSum = weight;

    }

    public Doc(String code, String docString) {
        String[] splitString = docString.split("\t");
        this.code = code;
        this.length = Integer.parseInt(splitString[0]);
        this.date = splitString[1];
        this.file = splitString[2];
        if (splitString.length > 3) {
            this.mostCommonTerm = Integer.parseInt(splitString[3]);
            this.squaredWeightSum = Double.parseDouble(splitString[4]);
        }
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

    public double getSquaredWightSum() {
        return squaredWeightSum;
    }

    public void setSquaredWightSum(double wight) {
        this.squaredWeightSum = wight;
    }

    @Override
    public String toString() {
        DocNameHash docNameHash = new DocNameHash();
        return docNameHash.getDocNoFromHash(code);
    }
}