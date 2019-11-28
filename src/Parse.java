import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parse {
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    private Stemmer stemmer;
    private String stopWordsPath;
    private Set<String> setStopWords;
    private Set<String> setNames;
    private Set<String> setUpperCase;
    private Set<String> setAllTerms;
    private Map<String, Set<String>> mapTermsInDocs;


    private ArrayList<String> allDocs;
    private final int reOptions = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL;
    // well contain the docName and set of terms


    //Constructor
    public Parse(ArrayList<String> allDocs, String stopWordsPath){
            this.mapTermsInDocs = new LinkedHashMap<>();
            this.allDocs = allDocs;
            this.stopWordsPath = stopWordsPath;
            this.setAllTerms = new LinkedHashSet<>();
            this.setNames = new LinkedHashSet<>();
            this.setUpperCase = new LinkedHashSet<>();
            this.stemmer = new Stemmer();
        }

    /**
     * The main function for parse
     */
    public void Parser(){
        //Iterator<Map.Entry<String, String>> itr = this.allDocs.entrySet().iterator();
        try{
            String stopWords = new String ( Files.readAllBytes( Paths.get(stopWordsPath) ));
            this.setStopWords = stringToSetOfString(stopWords);

        }
        catch (Exception e){
            e.printStackTrace();
        }

        for (int i = 0; i < allDocs.size() - 1; i++) {
            //Map.Entry<String, String> entry = itr.next();

            String fullText = "";
            String docName ="";
            Pattern patternText = Pattern.compile("<DOCNO>\\s*([^<]+)\\s*</DOCNO>.+?<TEXT>(.+?)</TEXT>", reOptions);
            Matcher matcherText = patternText.matcher(allDocs.get(i));
            while (matcherText.find()){
                docName = matcherText.group(1);
                fullText = matcherText.group(2);
            }

            // fullText = entry.getValue();
            // LinkedList<String> listFullText = stringToLinkedList(fullText);
//            addNames(fullText);
            fullText = separateTermsFromText(fullText, docName);
//            fullText = deleteStopWords(this.stopWordsPath, fullText);
//            fullText = stemFulltext(fullText);
//            fullText = termFormat(fullText);
            //termsInDocs.put(entry.getKey(), s);
            //System.out.println("K");
//            addWordsToSetTerms(fullText);
        }
//        identifyUpperCases();
//        addNamesToSetTerms();
    }

    /**
     * Format terms to defined templates
     * @param fullText
     * @return
     */
    public String termFormat (String fullText){
        String term;
        // #1 change M/K/B
        Pattern patternNumbers = Pattern.compile("(\\d+(?:,\\d+)*)((?:\\D+(?:Thousand|Million|Billon))?(?:/\\d+)?(?:(?:\\.\\d+)*)?(?:-\\d+)?)", reOptions);
        Matcher matcherNumbers = patternNumbers.matcher(fullText);
        while (matcherNumbers.find()){
            term = matcherNumbers.group(1) + matcherNumbers.group(2);
            String str = numWithoutUnits(term);
            fullText = fullText.replace(term, str);
        }
        // #2 change %
        Pattern patternPercent = Pattern.compile("(\\d+(?:\\.\\d+)?)(\\s*)(%|(?:percentage?)|(?:percent))", reOptions);
        Matcher matcherPercent = patternPercent.matcher(fullText);
        while (matcherPercent.find()){
            term = matcherPercent.group(1) + matcherPercent.group(2) + matcherPercent.group(3);
            String str = numWithPercent(term);
            fullText = fullText.replace(term, str);
        }

        // #3 Dates
//        Pattern patternDate = Pattern.compile("\\d+\\s\\w+|\\w+\\s\\d+", reOptions);
        Pattern patternDate = Pattern.compile("\\d+(?:\\-\\d+)?(\\s*)\\w+", reOptions);
        Matcher matcherDate = patternDate.matcher(fullText);
        while (matcherDate.find()){
            term = matcherDate.group(1) + matcherDate.group();
            String str = numWithDates(term);
            fullText = fullText.replace(term, str);
        }

        // #4 Prices
        Pattern patternPrice = Pattern.compile("\\$?\\d+(?:.\\d+)?\\s*(?:(?:million)|(?:billion)|(?:trillion)|(?:m)|(?:bn))?\\s*(?:(?:Dollars)|(?:U.S.))?\\s*(?:(?:dollars))?", reOptions);
        Matcher matcherPrice = patternPrice.matcher(fullText);
        while (matcherPrice.find()){
            term = matcherPrice.group();
            String str = numWithPercent(term);
            fullText = fullText.replace(term, str);
        }

        return fullText;
    }

    /**
     * Add each term in doc into set of terms
     * @param fullText
     */
    private void addWordsToSetTerms(String fullText){
        Pattern patternAllTerms = Pattern.compile("\\s(\\S+)", reOptions);
        Matcher matcherAllTerms = patternAllTerms.matcher(fullText);
        while (matcherAllTerms.find()){
            this.setAllTerms.add(matcherAllTerms.group(1));
        }
    }

    /**
     * Add names of 2 or more docs from map of names to set of terms
     */
    /*
    private void addNamesToSetTerms(){
        Set<String> names = new LinkedHashSet<>(this.mapNames.keySet());

        // Clean map from names in one doc only
        for (String name : names) {
            if(this.mapNames.get(name) == 1){
                this.mapNames.remove(name);
            }
        }

        // Add names to terms
        this.setAllTerms.addAll(mapNames.keySet());
    }
    */

    /**
     * Search for names in fullText, add them to map of names and count how many time docs contain the names
     * @param fullText
     */
    private void addNames(String fullText){
        Set<String> setNames = new LinkedHashSet<>();
        Pattern patternNames = Pattern.compile("(?:[A-Z]+\\w*(?:-[A-Za-z]+)*(?:\\W|\\s+)){2,}", Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcherNames = patternNames.matcher(fullText);
        while (matcherNames.find()) {
            String match = matcherNames.group();
            // Clean the name
            match = match.replaceAll("[,:;()?!{}\\[\\]\"\'.]", "");
            // Stem the name
            match = stemFulltext(match).trim().toUpperCase();
            setNames.add(match);
        }

        /*
        // Add to map and count the names
        for (String name : setNames){
            if(!this.mapNames.containsKey(name)){
                this.mapNames.put(name, 1);
            }
            else{
                this.mapNames.put(name, this.mapNames.get(name) + 1);
            }
        }
    }
    */


    /**
     * Identify constant upper case words and separate them in set of terms
     */
    /*
    private void identifyUpperCases(){
        String strTerms = this.setAllTerms.toString();

        Pattern patternAllTerms = Pattern.compile("([A-Z])(\\w+)", Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcherAllTerms = patternAllTerms.matcher(strTerms);
        while (matcherAllTerms.find()){
            String upperCaseTerm = matcherAllTerms.group(1) + matcherAllTerms.group(2);
            String lowerCaseTerm = matcherAllTerms.group(1).toLowerCase() + matcherAllTerms.group(2).toLowerCase();
            if(this.setAllTerms.contains(lowerCaseTerm)){
                this.setAllTerms.remove(upperCaseTerm);
            }
            else {
                this.setAllTerms.remove(upperCaseTerm);
                this.setAllTerms.add(upperCaseTerm.toUpperCase());
            }
        }
        */

        // Test //

        /*
        String[] arrayTerms = this.setAllTerms.toArray(new String[0]);
        ArrayList<String> arrayListTerms = new ArrayList(Arrays.asList(arrayTerms));
        Collections.sort(arrayListTerms, String.CASE_INSENSITIVE_ORDER);
        return;
        */

    }

    /**
     * Remove all punctuation chars, dots, &amp, spaces and / with STRING
     * @param fullText
     * @return full text: words separated by space
     */
    private String separateTermsFromText(String fullText, String docName){
        this.mapTermsInDocs.put(docName, new LinkedHashSet<>());
        StringBuilder sbNames = new StringBuilder();
        int counterNames = 0;

        Pattern patternPunctuation = Pattern.compile("(\\w+(?:\\.\\d+)?(?:[/-]\\s*\\w+)*)(?:\\W|\\s+)", reOptions);
        Matcher matcherPunctuation = patternPunctuation.matcher(fullText);
        while (matcherPunctuation.find()) {
            String term = matcherPunctuation.group(1);
            // Stop words
            if(isStopWord(term)){
               continue;
            }
            // Stem
            term = this.stemmer.porterStemmer(term);

            this.setAllTerms.add(term);
            this.mapTermsInDocs.get(docName).add(term);
        }
        Pattern patternPunctuation = Pattern.compile("(\\w+(?:\\.\\d+)?(?:[/-]\\s*\\w+)*)(?:\\W|\\s+)", reOptions);
        Matcher matcherPunctuation = patternPunctuation.matcher(fullText);
        while (matcherPunctuation.find()) {

        int x = 0;
        return fullText;
    }

    private boolean isStopWord(String term){
        if(this.setStopWords.contains(term.toLowerCase())){
            return true;
        }
        return false;
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

        // Add term "Between number and number" to set of term before stop words cleaning
        addBetweenNumberAndNumberToSetTerms(fullText);

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
            capitalizeWord += first.toUpperCase() + afterFirst + "\n";
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
     * Add term "Between number and number" to set of terms
     * @param fullText
     */
    private void addBetweenNumberAndNumberToSetTerms(String fullText){
        Pattern patternPunctuation = Pattern.compile("between \\d+ and \\d+", reOptions);
        Matcher matcherPunctuation = patternPunctuation.matcher(fullText);
        while (matcherPunctuation.find()) {
            this.setAllTerms.add(matcherPunctuation.group());
        }
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
    private Set<String> stringToSetOfString(String fullText){
        Pattern pattern = Pattern.compile("\n", reOptions);
        Scanner sc2 = new Scanner(fullText).useDelimiter(pattern);
        Set<String> setString = new LinkedHashSet<>();
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

        if(term.contains("-") || Pattern.compile("\\.\\d+\\.").matcher(term).find() || term.contains("/")){
            return term;
        }

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
        Pattern patternAllTerms = Pattern.compile("percentage|percent", reOptions);
        Matcher matcherAllTerms = patternAllTerms.matcher(term);
        while (matcherAllTerms.find()){
            term = term.replaceAll(matcherAllTerms.group(), "%").replaceAll("\\s+", "");
        }

        return term;
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
        if(monthNumber == -1){
            return term;
        }
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
