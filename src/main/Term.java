package main;

/**
 * Used to represent a term
 */
public class Term implements Comparable<Term> {

    private String term;
    private int importance;

    public Term(String term, int importance) {
        this.term = term;
        this.importance = importance;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public int getImportance() {
        return importance;
    }

    public void setImportance(int tfIdf) {
        this.importance = importance;
    }

    @Override
    public String toString() {
        return term;
    }

    @Override
    public int compareTo(Term o) {
        return Double.compare(o.getImportance(), importance);
    }
}
