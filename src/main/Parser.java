package main;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Used to parse a doc into terms
 */
public class Parser {

    private char[] docArray;
    private int index = 0;
    private int arrayLength;
    private static HashSet<String> stopWords;

    Parser(HashSet<String> stopWordsMap) {
        stopWords = stopWordsMap;
    }

    Parser() {
    }

    /**
     * parse the doc array according to the given rules
     *
     * @param array char array to parse
     * @return terms in the doc
     */
    public ArrayList<String> parse(char[] array) {
        //initial class fields
        index = 0;
        docArray = array;
        arrayLength = docArray.length;
        ArrayList<String> terms = new ArrayList<>();
        String term;
        while (index < arrayLength) {
            if (isDigit(docArray[index]) || isLowerCaseLetter() || isCapitalLetter() || isQuotationMark() || docArray[index] == 36) {
                boolean isDollar = false;
                if (docArray[index] == 36) {
                    isDollar = true;
                    index++;
                }
                //save for later
                int originalIndex = index;
                String month;


                //digits
                if (isDigit(docArray[index])) {
                    term = parseNumber();

                    //dollars condition
                    if (isDollar)
                        term = term + " dollars";
                    skipWhiteSpace();
                    if (!isDollar && index < arrayLength && isPercent())
                        term = term + " percent";
                    terms.add(term);
                    continue;
                }

                //dates that start with a month
                if ((month = getMonth()) != null) {
                    if ((term = parseDateStartsWithMonth(month)) != null) {
                        terms.add(term);
                        continue;

                    } else
                        index = originalIndex;
                } else
                    index = originalIndex;

                //quotation marks
                if (index != 0 && isQuotationMark() && docArray[index - 1] == 32) {
                    ArrayList<String> expressions = parseQuote();
                    if (expressions != null) {
                        terms.addAll(expressions);
                    }
                    continue;
                }

                //capital letters
                if (isCapitalLetter()) {
                    ArrayList<String> expressions = parseExpressions();
                    if (expressions != null) {
                        terms.addAll(expressions);
                    }
                    continue;
                }

                //stop words
                skipDelimiters();
                StringBuilder stringBuilder = new StringBuilder();
                while (index < arrayLength && (isCapitalLetter() || isLowerCaseLetter() || docArray[index] == 39))
                    stringBuilder.append(Character.toLowerCase(docArray[index++]));

                String s = stringBuilder.toString();
                if (!isStopWord(s) && stringBuilder.length() > 1)
                    terms.add(s);

            } else
                index++;
        }

        return terms;

    }

    /**
     * find the number from the given array. The number can contain the characters: 0-9, ',, '.'
     *
     * @return string that represents the number
     */
    private String parseNumber() {
        boolean isDecimalNumber = false;
        StringBuilder str = new StringBuilder();

        //not decimal numbers
        while (index < arrayLength) {
            while (index < arrayLength && isDigit(docArray[index])) {
                str.append(docArray[index++]);
            }

            if (index < arrayLength && isComma(docArray[index])) {
                index++;
            } else
                break;
        }

        //decimal numbers
        if (index < arrayLength && isDot(docArray[index]) && index < arrayLength - 1 && isDigit(docArray[index + 1])) {
            str.append(docArray[index++]);
            str.append(docArray[index++]);
            isDecimalNumber = true;
            if (index < arrayLength && isDigit(docArray[index])) {
                if (index < arrayLength - 1 && isDigit(docArray[index + 1])) {
                    str.append((int) docArray[index] + 1);
                    index = index + 2;
                } else
                    str.append(docArray[index++]);
                //skip the next digits
                while (index < arrayLength && isDigit(docArray[index]))
                    index++;
            }
            //index--;
        }
        if (!isDecimalNumber && str.length() == 2) {
            int originalIndex = index;

            //check if not null, if null then illegal date and return
            String date;
            if ((date = parseDateStartsWithTwoDigits(str)) != null)
                return date;
            else index = originalIndex;
        }

        return str.toString();
    }

    /**
     * find the next expression in the doc.
     * The expression can be a sequence of words that starts with capital letters,
     * or an expression that contains capital letters only
     *
     * @return list of the expression
     */
    private ArrayList<String> parseExpressions() {
        boolean allCapitalLetter = false;
        ArrayList<String> terms = new ArrayList<>();
        StringBuilder str = new StringBuilder();
        int wordsCounter = 0;
        String prevWord = "";

        //while the first letter is capital letter
        while (isCapitalLetter()) {
            index++;

            //prev word if full capital letters word and current word has lowercase letter
            if (allCapitalLetter && isLowerCaseLetter()) {
                index--;
                break;
            }

            //append the first char of the term
            str.append(Character.toLowerCase(docArray[index - 1]));
            wordsCounter++;

            //if the next letter is lowercase
            while (isLowerCaseLetter() && !allCapitalLetter) {
                str.append(docArray[index++]);

            }

            //if the next letter is capital letter
            while (isCapitalLetter()) {
                str.append(Character.toLowerCase(docArray[index++]));
                if (!allCapitalLetter)
                    allCapitalLetter = true;
            }

            //add the term to the list
            String currentWord = str.toString();
            if (wordsCounter > 1)
                terms.add(prevWord + " " + currentWord);
            if (!isStopWord(currentWord))
                terms.add(currentWord);

            //if the delimiter is " " or ' or -, continue the term
            if (index < arrayLength && (skipWhiteSpace() || skipHyphenAndApostrophe())) {
                if (isCapitalLetter()) {
                    prevWord = currentWord;
                    str = new StringBuilder();
                }
            } else
                break;
        }
        return terms;

    }

    private boolean skipHyphenAndApostrophe() {
        boolean ans = false;
        while (index < arrayLength && (docArray[index] == 45 || docArray[index] == 39)) {
            index++;
            ans = true;
        }
        return ans;

    }

    /**
     * find the next quote in the doc.
     * the quote must be less than 5 words.
     *
     * @return list of terms
     */
    private ArrayList<String> parseQuote() {
        StringBuilder word = new StringBuilder();
        StringBuilder fullQuote = new StringBuilder();
        ArrayList<String> terms = new ArrayList<>();
        index++;
        int wordsCounter = 0;
        while (wordsCounter < 5 && index < arrayLength && !isQuotationMark()) {

            if (isDelimiter()) {
                wordsCounter++;
                String wordS = word.toString();
                if (!isStopWord(wordS) && wordS.length() > 1)
                    terms.add(wordS);
                word = new StringBuilder();
                fullQuote.append(" ");
            } else {
                word.append(Character.toLowerCase(docArray[index]));
                fullQuote.append(Character.toLowerCase(docArray[index++]));
            }
        }
        if (wordsCounter < 5)
            terms.add(fullQuote.toString());
        return terms;

    }

    /**
     * Called when input is two digits, checks if it's a date of the following pattern:
     * DD (*ST/ND/RD/TH*) *SPACE* *MONTH* (*COMMA*) *SPACE* (YY)YY
     * DD (*ST/ND/RD/TH*) *SPACE* *MONTH* (*COMMA*) -NO YEAR-
     *
     * @param stringBuilder the two digits of the date
     * @return string that represent the date in DD/MM/YYYY Format, otherwise returns null
     */
    private String parseDateStartsWithTwoDigits(StringBuilder stringBuilder) {
        //DD *ST/ND/RD/TH*
        if (index + 2 >= arrayLength) return null; //out of bound check
        if ((docArray[index] == 's' && docArray[index + 1] == 't') ||
                (docArray[index] == 'n' && docArray[index + 1] == 'd') ||
                (docArray[index] == 'r' && docArray[index + 1] == 'd') ||
                (docArray[index] == 't' && docArray[index + 1] == 'h'))
            index += 2;

        //DD (*ST/ND/RD/TH*) *SPACE*
        if (index >= arrayLength) return null; //out of bound check
        if (docArray[index] == 32) {
            index++;
            //DD (*ST/ND/RD/TH*) *SPACE* *MONTH*
            String m;
            if ((m = getMonth()) != null) {
                index++;
                skipLetters(); //continue running until end of word

                //Check if it's a comma
                if (index >= arrayLength) return null; //out of bound check
                if (docArray[index] == 44)
                    index++;

                //DD (*ST/ND/RD/TH*) *SPACE* *MONTH* (*COMMA*) *SPACE*
                if (index >= arrayLength) return null; //out of bound check
                if (docArray[index] == 32) {
                    index++;
                    //DD (*ST/ND/RD/TH*) *SPACE* *MONTH* (*COMMA*) *SPACE* YYYY
                    if (index + 4 < arrayLength && isDigit(docArray[index]) && isDigit(docArray[index + 1]) &&
                            isDigit(docArray[index + 2]) && isDigit(docArray[index + 3])) {
                        index += 4;
                        //15 May 1917 => 15/05/1917
                        //15 May, 1917 => 15/05/1917
                        //15th May 1917 => 15/05/1917
                        //15th May, 1917 => 15/05/1917
                        return stringBuilder.toString() + '/' + m + '/' + docArray[index - 4] + docArray[index - 3] + docArray[index - 2] + docArray[index - 1];
                    }
                    //DD (*ST/ND/RD/TH*) *SPACE* *MONTH* (*COMMA*) *SPACE* YY
                    else if (index + 1 < arrayLength && isDigit(docArray[index]) && isDigit(docArray[index + 1])) {
                        index += 2;
                        //15 May 17 => 15/05/1917
                        //15 May, 17 => 15/05/1917
                        //15th May 17 => 15/05/1917
                        //15th May, 17 => 15/05/1917
                        return stringBuilder.toString() + '/' + m + "/19" + docArray[index - 2] + docArray[index - 1];
                    }
                }

                //(*ST/ND/RD/TH*) *SPACE* *MONTH* (*COMMA*) -NO YEAR-
                return stringBuilder.toString() + '/' + m;
            }
        }

        return null;
    }

    /**
     * Called when input is a month, checks if it's a date of the following pattern:
     * *MONTH* YYYY
     * *MONTH* DD (*ST/ND/RD/TH*)
     *
     * @param m month as string
     * @return string that represent the date in MM/YYYY or DD/MM Format, otherwise returns null
     */
    private String parseDateStartsWithMonth(String m) {

        //skip letters of month name
        skipLetters();

        //*MONTH *SPACE*
        if (index >= arrayLength) return null; //out of bound check
        if (docArray[index] == 32) {
            index++;
            //*MONTH* *SPACE* YYYY
            if (index + 4 < arrayLength && isDigit(docArray[index]) && isDigit(docArray[index + 1]) && isDigit(docArray[index + 2]) && isDigit(docArray[index + 3])) {
                index += 4;
                //June 1994 => 06/1994
                return m + '/' + docArray[index - 4] + docArray[index - 3] + docArray[index - 2] + docArray[index - 1];
            }
            //*MONTH* *SPACE* DD
            else if (index + 2 < arrayLength && isDigit(docArray[index]) && isDigit(docArray[index + 1])) {
                index += 2;
                //June 05 => 05/06
                return "" + docArray[index - 2] + docArray[index - 1] + '/' + m;
            }
        }

        return null;
    }

    /**
     * Checks if a string is a month and returns it
     *
     * @return int to represent the month, 0 otherwise
     */
    private String getMonth() {

        if (index + 3 >= arrayLength) return null; //out of bound check

        //a
        if (docArray[index] == 'a' || docArray[index] == 'A') {
            index++;
            //apr / april
            if ((docArray[index] == 'p' || docArray[index] == 'P') &&
                    (docArray[index + 1] == 'r' || docArray[index + 1] == 'R')) {
                index++;
                return "04";
            }
            //aug / august
            if ((docArray[index] == 'u' || docArray[index] == 'U') &&
                    (docArray[index + 1] == 'g' || docArray[index + 1] == 'G')) {
                index++;
                return "08";
            }
        }
        //d
        else if (docArray[index] == 'd' || docArray[index] == 'D') {
            index++;
            //dec / december
            if ((docArray[index] == 'e' || docArray[index] == 'E') &&
                    (docArray[index + 1] == 'c' || docArray[index + 1] == 'C')) {
                index++;
                return "12";
            }
        }
        //f
        else if (docArray[index] == 'f' || docArray[index] == 'F') {
            index++;
            //feb / february
            if ((docArray[index] == 'e' || docArray[index] == 'E') &&
                    (docArray[index + 1] == 'b' || docArray[index + 1] == 'B')) {
                index++;
                return "02";
            }
        }
        //j
        else if (docArray[index] == 'j' || docArray[index] == 'J') {
            index++;
            //jan / january
            if ((docArray[index] == 'a' || docArray[index] == 'A') &&
                    (docArray[index + 1] == 'n' || docArray[index + 1] == 'N')) {
                index++;
                return "01";
            }
            //ju
            if ((docArray[index] == 'u' || docArray[index] == 'U')) {
                index++;
                //jun / june
                if ((docArray[index] == 'n' || docArray[index] == 'N'))
                    return "06";
                //jul / july
                if ((docArray[index] == 'l' || docArray[index] == 'L'))
                    return "07";
            }
        }
        //ma
        else if ((docArray[index] == 'm' || docArray[index] == 'M') &&
                (docArray[index + 1] == 'a') || (docArray[index + 1] == 'A')) {
            index += 2;
            //mar / march
            if ((docArray[index] == 'r') || (docArray[index] == 'R'))
                return "03";
            //may
            if ((docArray[index] == 'y') || (docArray[index] == 'Y'))
                return "05";
        }
        //n
        else if ((docArray[index] == 'n') || (docArray[index] == 'N')) {
            index++;
            //nov / november
            if ((docArray[index] == 'o' || docArray[index] == 'O') &&
                    (docArray[index + 1] == 'v' || docArray[index + 1] == 'V')) {
                index++;
                return "11";
            }
        }
        //o
        else if (docArray[index] == 'o' || docArray[index] == 'O') {
            index++;
            //oct / october
            if ((docArray[index] == 'c' || docArray[index] == 'C') &&
                    (docArray[index + 1] == 't' || docArray[index + 1] == 'T')) {
                index++;
                return "10";
            }
        }
        //s
        else if (docArray[index] == 's' || docArray[index] == 'S') {
            index++;
            //sep / september
            if ((docArray[index] == 'e' || docArray[index] == 'E') &&
                    (docArray[index + 1] == 'p' || docArray[index + 1] == 'P')) {
                index++;
                return "09";
            }
        }

        return null;
    }

    /**
     * checks if the word is a stop word
     *
     * @param str ord to check
     * @return true if the word is stop word
     */
    private boolean isStopWord(String str) {
        return stopWords.contains(str);
    }

    /**
     * check if the next char is a quotation mark
     *
     * @return true if the char is a quotation mark
     */
    private boolean isQuotationMark() {
        return docArray[index] == 34;
    }

    /**
     * check if the next char is a delimiter
     *
     * @return true if the char is a delimiter
     */
    private boolean isDelimiter() {
        return docArray[index] >= 32 && docArray[index] <= 47;
    }

    /**
     * check if the char is a digit
     *
     * @param c digit to check
     * @return true if the char is a digit
     */
    private boolean isDigit(char c) {
        return c >= 48 && c <= 57;
    }

    /**
     * check if the char is a dot
     *
     * @param c digit to check
     * @return true if the char is a dot
     */
    private boolean isDot(char c) {
        return c == 46;
    }

    /**
     * check if the char is a comma
     *
     * @param c digit to check
     * @return true if the char is a comma
     */
    private boolean isComma(char c) {
        return c == 44;
    }

    /**
     * check if the char '%' is the next char or if the word "percent" is the next term in the doc
     *
     * @return true, if the char is '%' or if the next word is "percent".
     */
    private boolean isPercent() {
        if (docArray[index] == 37) //check if the char is '%'
            return true;
        //check if the next word is "percent"
        if (index < docArray.length - 7 && docArray[index] == 'p' && docArray[index + 1] == 'e' && docArray[index + 2] == 'r' &&
                docArray[index + 3] == 'c' && docArray[index + 4] == 'e' && docArray[index + 5] == 'n' && docArray[index + 6] == 't') {
            index = index + 7;
            while (index < arrayLength && docArray[index] != 32)//skip until the end of the word
                index++;
            return true;
        }
        return false;
    }

    /**
     * check if the next char in the doc is capital letter
     *
     * @return true, if the next char in the doc is capital letter
     */
    private boolean isCapitalLetter() {
        return index < arrayLength && docArray[index] >= 65 && docArray[index] <= 90;
    }

    /**
     * check if the next char in the doc is lowercase letter
     *
     * @return true, if the next char in the doc is lowercase letter
     */
    private boolean isLowerCaseLetter() {
        return index < arrayLength && docArray[index] >= 97 && docArray[index] <= 122;
    }

    /**
     * skipping the delimiters chars is the array
     *
     * @return true, if there is a delimiter
     */
    private void skipDelimiters() {
        while (index < arrayLength && (!(docArray[index] >= 48 && docArray[index] <= 57) &&
                !(docArray[index] >= 65 && docArray[index] <= 90) &&
                !(docArray[index] >= 97 && docArray[index] <= 122))) {
            index++;
        }
    }


    /**
     * skipping the white spaces chars is the array
     *
     * @return true, if there is a space
     */
    private boolean skipWhiteSpace() {
        boolean ans = false;
        while (index < arrayLength && (docArray[index] == 32 || docArray[index] == '\t')) {
            index++;
            ans = true;
        }
        return ans;
    }

    /**
     * skipping the hyphen('-') chars is the array
     *
     * @return true, if there is a hyphen
     */
    private boolean skipHyphen() {
        boolean ans = false;
        while (index < arrayLength && docArray[index] == 45) {
            index++;
            ans = true;
        }
        return ans;
    }

    /**
     * Skips all letters A-Z and a-z and promotes the index accordingly
     */
    private void skipLetters() {
        while (index < arrayLength && ((docArray[index] >= 65 && docArray[index] <= 90) || //A-Z
                (docArray[index] >= 97 && docArray[index] <= 122))) //a-z
            index++;
    }


}