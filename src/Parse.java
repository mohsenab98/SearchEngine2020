import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parse {
    private Stemmer stemmer;
    private String stopWordsPath;
    private Map<String, String> allDocs;
    private final int reOptions = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL;
    //well contain the docName and set of terms
    private Map<String, Set<String>> termsInDocs;
    // field set //

    //Constructor
    public Parse(Map<String, String> allDocs, String stopWordsPath){
            this.termsInDocs = new HashMap<>();
            this.allDocs = allDocs;
            this.stopWordsPath = stopWordsPath;
            this.stemmer = new Stemmer();
        }

    /**
     * The main function
     */
    public void Parser(){
        Iterator<Map.Entry<String, String>> itr = this.allDocs.entrySet().iterator();
        String fullText ="";
        while(itr.hasNext()) {
            Map.Entry<String, String> entry = itr.next();

            Pattern patternText = Pattern.compile("<TEXT>(.+?)</TEXT>", reOptions);
            Matcher matcherText = patternText.matcher(entry.getValue());
            while (matcherText.find()){
                fullText = matcherText.group(1);
            }

            fullText = removePunctuationAndSpacesString(fullText);
            fullText = deleteStopWords(this.stopWordsPath, fullText);
            fullText = stemFulltext(fullText);
            fullText = termIdentifier(fullText);
            //termsInDocs.put(entry.getKey(), s);
            System.out.println("K");
        }
    }
    //NumWithoutUnits

    public String termIdentifier (String fullText){
        //#1 change M/K/B
        String term = "";
        Pattern patternText = Pattern.compile("(\\d+(?:,\\d+)*)((?:\\D+(?:Thousand|Million|Billion))?(?:\\.\\d+)?)", reOptions);
        Matcher matcherText = patternText.matcher(fullText);
        while (matcherText.find()){
            term = matcherText.group(1) + matcherText.group(2);
            String str = numWithoutUnits(term);
            fullText = fullText.replaceAll(term, str);
        }
        return fullText;
    }

    /**
     * Remove all punctuation chars, dots, &amp, spaces and / with SET
     * @param setFullText
     * @return
     */
    private Set<String> removePunctuationAndSpacesSet(Set<String> setFullText){

        return  setFullText;
    }

    /**
     * Remove all punctuation chars, dots, &amp, spaces and / with STRING
     * @param fullText
     * @return full text: words separated by space
     */
    private String removePunctuationAndSpacesString(String fullText){
        // Clean new lines
        fullText = fullText.replace("\n", " ");
        fullText = fullText.replace("\r", " ");

        // Clean punctuations
        Pattern patternPunctuation = Pattern.compile("[,:;()?!{}\\[\\]\"\'*]", reOptions);
        Matcher matcherPunctuation = patternPunctuation.matcher(fullText);
        while (matcherPunctuation.find()) {
            fullText = matcherPunctuation.replaceAll("");
        }

        // CLean &amp
        Pattern patternAmpersand = Pattern.compile("&amp", reOptions);
        Matcher matcherAmpersand = patternAmpersand.matcher(fullText);
        while (matcherAmpersand.find()) {
            fullText = matcherAmpersand.replaceAll("&");
        }

        // Remove / and spaces between --
        Pattern patternOther = Pattern.compile("/|-\\s*-", reOptions);
        Matcher matcherOther = patternOther.matcher(fullText);
        while (matcherOther.find()) {
            fullText = matcherOther.replaceAll(" ");
        }

        // Remove spaces between -word
        Pattern patternHyphen = Pattern.compile("-\\s+?(\\w)", reOptions);
        Matcher matcherHyphen = patternHyphen.matcher(fullText);
        while (matcherHyphen.find()) {
            fullText = matcherHyphen.replaceAll("-" + matcherHyphen.group(1));
        }

        // Replace . to , in numbers
        Pattern patternDotsLetters = Pattern.compile("(\\d+?)\\.(\\d+?)", reOptions);
        Matcher matcherDotsLetters = patternDotsLetters.matcher(fullText);
        while (matcherDotsLetters.find()) {
            fullText = matcherDotsLetters.replaceFirst(matcherDotsLetters.group(1) + "," + matcherDotsLetters.group(2));
            matcherDotsLetters = patternDotsLetters.matcher(fullText);
        }

        // Remove dots
        fullText = fullText.replaceAll("\\.", "");
        // Replace , to . in numbers
        fullText = fullText.replaceAll(",", ".");

        // Removes spaces
        Pattern patternSpaces = Pattern.compile("\\s{2,}", reOptions);
        Matcher matcherSpaces = patternSpaces.matcher(fullText);
        while (matcherSpaces.find()) {
            fullText = matcherSpaces.replaceAll(" ");
        }

        return fullText.trim();
    }

    /**
     * DELETE StopWords
     * Creating two Sets of strings and delete fro the set of text the set of stopWords
     * @param path of the StopWords file & fullText that represent the text of the Doc We are parsing
     * @return setStringText that doesnt have stop words
     */
    private String deleteStopWords(String path, String fullText) {
//        Set<String> setStringText = stringToSetOfString(fullText);
//        Set<String> setStringStopWords = pathOfStopWordsToSetOfStrings(path);
//        // Need to check terms rules before deleting stopWords
//        setStringText.removeAll(setStringStopWords);
//        return setStringText;
        String stopWords = "";
        try{
            stopWords = new String ( Files.readAllBytes( Paths.get(path) ) );
        }
        catch (Exception e){
            e.printStackTrace();
        }

        String words[] = stopWords.split("\\n");
        String capitalizeWord = "";
        for(String w : words){
            String first = w.substring(0,1);
            String afterFirst = w.substring(1);
            capitalizeWord += first.toUpperCase() + afterFirst +"\\n";
        }


        String stopWordsUpperCase = stopWords.toUpperCase();
        stopWords = stopWords + capitalizeWord + stopWordsUpperCase;

        Pattern patternStopWords = Pattern.compile("\\w+(?:'\\w+)?", reOptions);
        Matcher matcherStopWords = patternStopWords.matcher(stopWords);
        while (matcherStopWords.find()) {
            fullText = fullText.replaceAll(" " + matcherStopWords.group() + " ", " ");
        }

        return fullText;
    }

    /**
     * Stem every word in the given string
     * @param fullText
     * @return
     */
    private String stemFulltext(String fullText){
        String newString = "";
        Pattern pattern = Pattern.compile("\\s+", reOptions);
        Scanner sc2 = new Scanner(fullText).useDelimiter(pattern);

        while(sc2.hasNext()){
            newString = newString + " " +this.stemmer.porterStemmer((sc2.next()));
        }
        return newString;
    }


    /**
     *
     * @param fullText
     * @return setString that represent the strings of the text
     */
    private Set stringToSetOfString(String fullText){
        Pattern pattern = Pattern.compile("\\s+", reOptions);
        Scanner sc2 = new Scanner(fullText).useDelimiter(pattern);
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
    private Set pathOfStopWordsToSetOfStrings(String path){
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Set<String> setString = new HashSet<>();
        while (scanner.hasNextLine()) {
            setString.add(scanner.nextLine());
        }
        return setString;
    }

    /**
     * #1
     * @param term
     * @return
     */
    public String numWithoutUnits(String term){
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

    public String numWithPercent(String term){
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
    public String numWithDates(String term){

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
    private int monthContains(String test) {
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

        if(term.contains("US")){
            term = term.replace("US", "");
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
