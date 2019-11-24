import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

public class Parse {

    // fields
    private Map<String, String> allDocs;
    private final int reOptions = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL;
    // field set //

    //Constructor
    public Parse(Map<String, String> allDocs){
        this.allDocs = allDocs;
    }
    /**
     * DELETE StopWords
     * Creating two Sets of strings and delete fro the set of text the set of stopWords
     * @param path of the StopWords file & fullText that represent the text of the Doc We are parsing
     * @return setStringText that doesnt have stop words
     */
    public Set deleteStopWords(String path, String fullText) {
        Set<String> setStringText = stringToSetOfString(fullText);
        Set<String> setStringStopWords = pathOfStopWordsToSetOfStrings(path);
        // Need to check terms rules before deleting stopWords
        setStringText.removeAll(setStringStopWords);
        return setStringText;
    }

    /**
     *
     * @param fullText
     * @return setString that represent the strings of the text
     */
    public Set stringToSetOfString(String fullText){
        Scanner sc2 = new Scanner(fullText).useDelimiter(" ");
        Set<String> setString = new HashSet<String>();
        while(sc2.hasNext()){
            setString.add(sc2.next());
        }
        return setString;
    }

    /**
     *
     * @param path
     * @return setString that represent the stopWords from the text in the path was given
     */
    public Set pathOfStopWordsToSetOfStrings(String path){
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Set<String> setString = new HashSet<String>();
        while (scanner.hasNextLine()) {
            setString.add(scanner.nextLine());
        }
        return setString;
    }

    public String NumWithoutUnits(String term){
            int indexAfterDot;
            float numberInTerm = Float.parseFloat(term.replaceAll("[\\D]", ""));
            if(term.contains(".")){
                indexAfterDot = term.length() - (term.indexOf(".") + 1);
                numberInTerm = numberInTerm/(float)Math.pow(10, indexAfterDot);
            }

            int range = 0;
            if((numberInTerm >= 1000 && numberInTerm < 1000000)){
                range = 1;
            }else if((numberInTerm >= 1000000 && numberInTerm < 1000000000)){
                range = 2;
            }else if(numberInTerm >= 1000000000 ){
                range = 3;
            }else if(term.contains("Thousand")){
                range = 4;
            }else if( term.contains("Million")){
                range = 5;
            }else if(term.contains("Billion")){
                range = 6;
            }

            switch (range){
                case 1:
                    return String.format("%.03f",numberInTerm/1000) +"K";
                case 2:
                    return String.format("%.03f", numberInTerm/1000000) +"M";
                case 3:
                    return String.format("%.03f",numberInTerm/1000000000) +"B";
                case 4:
                    return numberInTerm +"K";
                case 5:
                    return numberInTerm +"M";
                case 6:
                    return numberInTerm +"B";
            }

            return term;
    }

    public String NumWithPercent(String term){
        String replacedStr = term;
        if(term.contains("percentage")){
            replacedStr  = term.replaceAll("percentage", "%");
        }
        else if(term.contains("percent")){
            replacedStr = term.replaceAll("percent", "%");
        }
        replacedStr = replacedStr.replaceAll("[\\s]","");

        return replacedStr;
    }

    /**
     * 5th term rule // Dates
     */
    enum Mounth {january , february, march, april, may, june, july, august, september, october, november, december}
    enum MountThreeChar {jan , feb, mar, apr, may, jun, jul, aug, sep, oct, nov, dec}
    public String NumWithDates(String term){

        String strWithDigitOnly = term.replaceAll("[\\D]","");
        float numberInTerm = Float.parseFloat(strWithDigitOnly);
        String strWithCharOnly  = term.replaceAll(strWithDigitOnly, "");
        if(numberInTerm<10){
            strWithDigitOnly = "0" + strWithDigitOnly ;
        }
        strWithCharOnly = strWithCharOnly.replaceAll("[\\s]","");

        int monthNumber = monthContains(strWithCharOnly);
        String monthNumberStr = String.valueOf(monthNumber);
        if(monthNumber < 10){
            monthNumberStr = "0" + monthNumber;
        }
        // Month Number Could be a year
        if(numberInTerm <= 31){
            return monthNumberStr+"-"+strWithDigitOnly;
        }else {
            return strWithDigitOnly+"-"+monthNumberStr;
        }
    }

    /**
     *
     * @param test
     * @return the number that represent each month
     */
    public int monthContains(String test) {
        int i = 1;
        for (Mounth m : Mounth.values()) {
            if (m.name().equals(test.toLowerCase())) {
                return i;
            }
            i++;
        }
        i=1;
        for (MountThreeChar m : MountThreeChar.values()) {
            if (m.name().equals(test.toLowerCase())) {
                return i;
            }
            i++;
        }

        return -1;
    }

    public String Price(String term){
        if(term.contains("$")){
            term = term.replace("$", "");
            term = term + " Dollars";
        }

        if(term.contains("U.S.")){
            term = term.replace("U.S.", "");
        }

        int indexAfterDot;
        float numberInTerm = Float.parseFloat(term.replaceAll("[\\D]", ""));
        if(term.contains(".")){
            String numberInTermStr = numberInTerm + "";
            indexAfterDot = numberInTermStr.length() - (term.indexOf(".") + 1);
            numberInTerm = numberInTerm/(float)Math.pow(10, indexAfterDot);
        }

        if(numberInTerm >= 1000000){
            return numberInTerm/1000000 + " M" + " Dollars";
        }

        if(term.contains("million") || term.contains("m ")){
            return numberInTerm + " M" + " Dollars";
        }

        if(term.contains("billion") || term.contains("bn ")){
            return numberInTerm * 1000 + " M" + " Dollars";
        }

        return term;
    }
}
