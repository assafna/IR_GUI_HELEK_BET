package main;

import java.io.*;

/**
 * Used to create a hash code out of a string in order to use only 3 chars to represent a doc
 */
public class DocNameHash {

    private char startAsciiCode;
    private char endAsciiCode;

    private char aCode;
    private char bCode;
    private char cCode;

    private int hashIndex;

    public static String[] docNoHashArray = null;

    DocNameHash() {
        startAsciiCode = 33; //included
        endAsciiCode = 127; //not included

        aCode = startAsciiCode;
        bCode = startAsciiCode;
        cCode = startAsciiCode;
        hashIndex = 0;
        if (docNoHashArray == null) {
            docNoHashArray =
                    new String[(endAsciiCode - startAsciiCode) * (endAsciiCode - startAsciiCode) * (endAsciiCode - startAsciiCode)];
        }
    }

    /**
     * generates a hash code from a string simply by increasing counters
     *
     * @param docNo string that represents a doc, used to insert into relevant array
     * @return string of 3 chars representing the doc
     */
    public String getHashFromDocNo(String docNo) {
        //Maximum combinations = (endAsciiCode - startAsciiCode)^3
        //So, 830,584.

        //in case where (X, X, endAsciiCode)
        if (cCode == endAsciiCode) {
            bCode++;
            cCode = startAsciiCode; //(X, X + 1, startAsciiCode)

            //in case where (X, endAsciiCode, startAsciiCode)
            if (bCode == endAsciiCode) {
                aCode++;
                bCode = startAsciiCode; //(X + 1, startAsciiCode, startAsciiCode)
            }
        }

        docNoHashArray[hashIndex++] = docNo;
        return "" + aCode + bCode + cCode++;
    }

    /**
     * returns the relevant doc name by converting the 3 chars back to ascii
     * and by using the array
     *
     * @param hash 3 chars representing the doc in hash code
     * @return doc original name
     */
    public String getDocNoFromHash(String hash) {
        return docNoHashArray[(hash.charAt(0) - startAsciiCode) * (endAsciiCode - startAsciiCode) * (endAsciiCode - startAsciiCode) +
                (hash.charAt(1) - startAsciiCode) * (endAsciiCode - startAsciiCode) +
                (hash.charAt(2) - startAsciiCode)];
    }

    /**
     * writes hash to file
     *
     * @param file hash path
     * @throws IOException exception
     */
    public static void writeHashToFile(File file) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        String[] strings = DocNameHash.docNoHashArray;
        int index = 0;
        while (strings[index] != null)
            bw.write(strings[index++] + '\n');
        bw.close();
    }

    /**
     * reads hash to array
     *
     * @param file file path
     * @throws IOException exception
     */
    public static void readHashToArray(File file) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        int index = 0;
        while ((line = br.readLine()) != null)
            DocNameHash.docNoHashArray[index++] = line;
        br.close();
    }

}