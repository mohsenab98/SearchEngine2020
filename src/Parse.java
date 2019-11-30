import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parse {
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    private Stemmer stemmer;
    private String stopWordsPath;
    private Set<String> setStopWords;
    private Map<String, Set<String>> mapNames;
    private Set<String> setUpperCase;
    private Map<String, Set<String>> mapTermsInDocs;


    private ArrayList<byte[]> allDocs;
    private final int reOptions = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL;
    // well contain the docName and set of terms


    //Constructor
    public Parse(List<byte[]> allDocs, String stopWordsPath){
        this.mapTermsInDocs = new LinkedHashMap<>();
        this.stopWordsPath = stopWordsPath;
        this.mapNames = new ConcurrentHashMap<>();
        this.setUpperCase = new LinkedHashSet<>();
        this.stemmer = new Stemmer();
        this.allDocs = new ArrayList<>(allDocs);
    }

    /**
     * The main function for parse
     */
    public void Parser() {
        //Iterator<Map.Entry<String, String>> itr = this.allDocs.entrySet().iterator();
       // int counter = 1;
        try {
            String stopWords = new String(Files.readAllBytes(Paths.get(stopWordsPath)));
            this.setStopWords = stringToSetOfString(stopWords);

        } catch (Exception e) {
            e.printStackTrace();
        }

        while (!allDocs.isEmpty()){
            String fullText = "";
            String docName = "";
            Pattern patternText = Pattern.compile("<DOCNO>\\s*([^<]+)\\s*</DOCNO>.+?<TEXT>(.+?)</TEXT>", reOptions);
            Matcher matcherText = patternText.matcher(new String(allDocs.get(0)));
            while (matcherText.find()) {
                docName = matcherText.group(1);
                fullText = matcherText.group(2);
            }

            separateTermsFromText(fullText, docName);
            this.allDocs.remove(0);

         //   System.out.println(counter);
         //   counter ++;
        }

        // Add names to terms
        cleanNames();
        this.mapTermsInDocs.putAll(this.mapNames);

        threadPool.shutdown();
        int x = 0;
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
     * Remove all punctuation chars, dots, &amp, spaces and / with STRING
     * @param fullText
     * @return full text: words separated by space
     */
    private void separateTermsFromText(String fullText, String docName){
        Pattern patternTerm = Pattern.compile("(\\w+(?:\\.\\d+)?(?:[/-]\\s*\\w+)*)((?:\\W|\\s+))", reOptions);
        Matcher matcherTerm = patternTerm.matcher(fullText);
        while (matcherTerm.find()) {
            String term = matcherTerm.group(1);
            // Stop words
            if(isStopWord(term)){
                continue;
            }
            // Stem
            term = this.stemmer.porterStemmer(term);

            if(this.mapTermsInDocs.containsKey(term)){
                this.mapTermsInDocs.get(term).add(docName);
            }
            else {
                this.mapTermsInDocs.put(term, new LinkedHashSet<>());
                this.mapTermsInDocs.get(term).add(docName);
            }
        }

        threadPool.execute( () -> {
            searchNames(fullText, docName);
        });
        int x = 0;
    }

    private boolean isStopWord(String term){
        if(this.setStopWords.contains(term.toLowerCase())){
            return true;
        }
        return false;
    }

    private void searchNames(String fullText, String docName){
        Pattern patternName = Pattern.compile("(?:[A-Z]+\\w*(?:-[A-Za-z]+)*(?:\\W|\\s+)){2,}", Pattern.MULTILINE);
        Matcher matcherName = patternName.matcher(fullText);
        while (matcherName.find()) {
            String name = matcherName.group();
            name = Pattern.compile("[,.:;)-?!}\\]\"\'*]", reOptions).matcher(name).replaceAll("");
            name = Pattern.compile("\n|\\s+", reOptions).matcher(name).replaceAll(" ").trim();
            // docNames.add(name);

            if(this.mapNames.containsKey(name)) {
                this.mapNames.get(name).add(docName);
            }
            else {
                this.mapNames.put(name, new LinkedHashSet<>());
                this.mapNames.get(name).add(docName);
            }
        }
    }

    private void cleanNames(){
        for (Map.Entry<String,Set<String>> name : this.mapNames.entrySet()){
            if(name.getValue().size() <= 1){
                this.mapNames.remove(name.getKey());
            }
        }
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
