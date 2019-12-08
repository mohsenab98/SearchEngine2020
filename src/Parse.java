import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parse {
    // private ExecutorService threadPool = new ThreadPoolExecutor(3, 3, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    private boolean stem;
    private Stemmer stemmer;

    private Set<String> setStopWords;
    private SortedMap<String, ArrayList<String>> mapTerms;
    private int positionCounter = 1;

    //   private Map<String, ArrayList<String>> concurrentMap;

    private final int reOptions = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL;

    //Constructor
    public Parse(String stopWordsPath, boolean stem) {
        this.stem = stem;
        this.stemmer = new Stemmer();
        this.mapTerms = new TreeMap<>();
        //    this.concurrentMap = new ConcurrentHashMap<>();

        try {
            String stopWords = new String(Files.readAllBytes(Paths.get(stopWordsPath)));
            this.setStopWords = stringToSetOfString(stopWords);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The main function for parse
     */
    public void Parser(String fullText, String docName) {
        int betweenCounter = 0;
        String lawBetween = "";
        String tokensFullText = "";


        Pattern patternTerm = Pattern.compile("(\\w+(?:\\.\\d+)?(?:[-/]\\s*\\w+)*)((?:\\W|\\s+))", reOptions);
        Matcher matcherTerm = patternTerm.matcher(fullText);
        while (matcherTerm.find()) {
            String term = matcherTerm.group(1);
            // Between
            if(term.equalsIgnoreCase("between") || lawBetween.contains("between")){
                if(betweenCounter == 1 && !Character.isDigit(term.charAt(0))){
                    lawBetween = "";
                    betweenCounter = 0;
                }
                else if(betweenCounter < 3) {
                    lawBetween += term.toLowerCase() + " ";
                    betweenCounter++;
                    continue;
                }
                else{
                    lawBetween += term.toLowerCase() + " ";
                    addTermToMap(lawBetween.trim(), docName);
                    betweenCounter = 0;
                    lawBetween = "";
                    continue;
                }
            }

            // Stop words
            if (isStopWord(term)) {
                continue;
            }

            tokensFullText += term + " ";

            // Stem
            if (Character.isUpperCase(term.charAt(0))) {
                if(this.stem) {
                    term = this.stemmer.porterStemmer(term.toLowerCase());
                }
                term = term.toUpperCase();
            }
            else if(this.stem) {
                term = this.stemmer.porterStemmer(term);
            }

            addTermToMap(term, docName);
        }

        /*
        if (!between.isEmpty()) {
            between(between, docName);
        }
        */

        //threadPool.execute(() -> {
        termFormat(tokensFullText, docName);
        //  });
        // searchNames(fullText, docName);
        //this.mapTerms.putAll(this.concurrentMap);
        int x = 0;

    }

    /**
     * Format terms to defined templates
     *
     * @param  docName
     * @return
     */
    public String termFormat(String fullText, String docName) {
        String lawText = fullText;
        String term;
        // #4 Dates
        //Pattern patternDate = Pattern.compile("\\d+\\s\\w+|\\w+\\s\\d+", reOptions);
        Pattern patternDate = Pattern.compile("(?:(?:\\d{1,2}-)?\\d{1,2}\\s*)(?:jan\\w*|feb\\w*|mar\\w*|apr\\w*|may|jun\\w?|jul\\w?|aug\\w*|sep\\w*|oct\\w*|nov\\w*|dec\\w*)|(?:jan\\w+|feb\\w*|mar\\w*|apr\\w*|may|jun\\w?|jul\\w?|aug\\w*|sep\\w*|oct\\w*|nov\\w*|dec\\w*)(?:\\s*\\d{1,4})", reOptions);
        Matcher matcherDate = patternDate.matcher(fullText);
        while (matcherDate.find()) {
            term = matcherDate.group().trim();
            addFormatToMap(numWithDates(term), docName);
            // indexer.addTermToIndexer(numWithDates(term).toLowerCase(), docName);
        }

        // #1 M/K/B
        Pattern patternNumbers = Pattern.compile("(\\d+(?:[.,-]\\d+)*)((?:\\s*thousand)?(?:\\s*million)?(?:\\s*billion)?)", reOptions);
        Matcher matcherNumbers = patternNumbers.matcher(fullText);
        while (matcherNumbers.find()) {
            term = (matcherNumbers.group(1) + " " + matcherNumbers.group(2)).trim();
            if(term.contains("-")){
                continue;
            }
            addFormatToMap(numWithoutUnits(term), docName);
            //  indexer.addTermToIndexer(numWithoutUnits(term).toLowerCase(), docName);

        }
        // #3 %
        Pattern patternPercent = Pattern.compile("(\\d+(?:\\.\\d+)?)(\\s*)(%|(?:percentage?)|(?:percent))", reOptions);
        Matcher matcherPercent = patternPercent.matcher(fullText);
        while (matcherPercent.find()) {
            term = (matcherPercent.group(1) + matcherPercent.group(2) + matcherPercent.group(3)).trim();
            addFormatToMap(numWithPercent(term), docName);
            //  indexer.addTermToIndexer(numWithPercent(term).toLowerCase(), docName);
        }

        // #5 Prices
        Pattern patternPrice = Pattern.compile("\\$\\d+(?:[,.]+\\d+)?\\s*(?:(?:million)|(?:billion)|(?:trillion)|(?:m)|(?:bn))?", reOptions);
        Matcher matcherPrice = patternPrice.matcher(fullText);
        while (matcherPrice.find()) {
            term = matcherPrice.group().trim();
            addFormatToMap(Price(term), docName);
            // indexer.addTermToIndexer(Price(matcherPrice.group()).toLowerCase(), docName);
        }
        patternPrice = Pattern.compile("\\d+(?:.\\d+)?\\s*(?:(?:million)|(?:billion)|(?:trillion)|(?:m)|(?:bn))?\\s*(?:U.S.)?\\s*(?:dollars)", reOptions);
        matcherPrice = patternPrice.matcher(fullText);
        while (matcherPrice.find()) {
            term = matcherPrice.group().trim();
            addFormatToMap(Price(term), docName);
            // indexer.addTermToIndexer(Price(matcherPrice.group()).toLowerCase(), docName);
        }

        return lawText;
    }

    /*
    public void addTermToConcurrentMap(String term, String docName, int start, int end){
        if(!this.concurrentMap.containsKey(term)) {
            this.concurrentMap.put(term, new ArrayList<>());
            this.concurrentMap.get(term).add(docName);
        }

        this.concurrentMap.get(term).add(start + ":" + end);
    }
    */

    private boolean isStopWord(String term) {
        if (this.setStopWords.contains(term.toLowerCase())) {
            return true;
        }
        return false;
    }

    /*
    private void between(String between, String docName) {
        String[] tokens = between.split("\\s*,");
        for (String term : tokens) {
            if (Pattern.compile("between \\d+ and \\d+").matcher(term).matches()) {
                addFormatToMap();ToMap(term, docName);
                // indexer.addTermToIndexer(term.toLowerCase(), docName);

            } else {
                Pattern patternTerm = Pattern.compile("\\w+", reOptions);
                Matcher matcherTerm = patternTerm.matcher(term);
                while (matcherTerm.find()) {
                    addTermToMap(matcherTerm.group(), docName);
                    //indexer.addTermToIndexer(matcherTerm.group().toLowerCase(), docName);
                }
            }
        }
    }
    */



    /**
     * @param fullText
     * @return setString that represent the strings of the text
     */
    private Set<String> stringToSetOfString(String fullText) {
        Pattern pattern = Pattern.compile("\n", reOptions);
        Scanner sc2 = new Scanner(fullText).useDelimiter(pattern);
        Set<String> setString = new LinkedHashSet<>();
        while (sc2.hasNext()) {
            setString.add(sc2.next());
        }
        return setString;

    }

    /**
     * index:
     *       0 - DocID
     *       2 - amount in Doc
     *       3 - positions on Doc
     *
     * @param term
     * @param docName
     */
    private void addTermToMap(String term, String docName){
        term = term.trim();
        if(!this.mapTerms.containsKey(term)) {
            this.mapTerms.put(term, new ArrayList<>());
            this.mapTerms.get(term).add(docName);
            this.mapTerms.get(term).add(String.valueOf(1));
            this.mapTerms.get(term).add(String.valueOf(this.positionCounter));
            this.positionCounter ++;
            return;
        }

        int counter = Integer.parseInt(this.mapTerms.get(term).get(1));
        this.mapTerms.get(term).set(1, String.valueOf(counter + 1));
        this.mapTerms.get(term).add(String.valueOf(this.positionCounter));
        this.positionCounter ++;
    }

    private void addFormatToMap(String term, String docName){
        term = term.trim();
        if(!this.mapTerms.containsKey(term)) {
            this.mapTerms.put(term, new ArrayList<>());
            this.mapTerms.get(term).add(docName);
            this.mapTerms.get(term).add(String.valueOf(1));
            this.mapTerms.get(term).add(String.valueOf(-1));
            return;
        }

        int counter = Integer.parseInt(this.mapTerms.get(term).get(1));
        this.mapTerms.get(term).set(1, String.valueOf(counter + 1));
    }

    public Map<String, ArrayList<String>> getMapTerms(){
        return this.mapTerms;
    }

    public void cleanTerms(){
        this.mapTerms.clear();
        //threadPool.shutdown();
    }




////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * #1 MILLION/BILLION/THOUSAND
     *
     * @param term
     * @return
     */
    public String numWithoutUnits(String term) {
        int indexAfterDot;

        if (term.contains("-") || Pattern.compile("\\.\\d+\\.").matcher(term).find() || term.contains("/")) {
            return term;
        }

        float numberInTerm = Float.parseFloat(term.replaceAll("[\\D]", ""));
        if (term.contains(".")) {
            indexAfterDot = term.length() - (term.indexOf(".") + 1);
            numberInTerm = numberInTerm / (float) Math.pow(10, indexAfterDot);
        }

        int range = 0;
        if ((numberInTerm >= 1000 && numberInTerm < 1000000)) {
            range = 1;
        } else if ((numberInTerm >= 1000000 && numberInTerm < 1000000000)) {
            range = 2;
        } else if (numberInTerm >= 1000000000) {
            range = 3;
        } else if (term.toLowerCase().contains("thousand")) {
            range = 4;
        } else if (term.toLowerCase().contains("million")) {
            range = 5;
        } else if (term.toLowerCase().contains("billion")) {
            range = 6;
        }

        switch (range) {
            case 1:
                return String.format("%.03f", numberInTerm / 1000) + "K";
            case 2:
                return String.format("%.03f", numberInTerm / 1000000) + "M";
            case 3:
                return String.format("%.03f", numberInTerm / 1000000000) + "B";
            case 4:
                return numberInTerm + "K";
            case 5:
                return numberInTerm + "M";
            case 6:
                return numberInTerm + "B";
        }

        return term;
    }

    /**
     * #3 PERCENT
     *
     * @param term
     * @return
     */
    public String numWithPercent(String term) {
        Pattern patternAllTerms = Pattern.compile("percentage|percent", reOptions);
        Matcher matcherAllTerms = patternAllTerms.matcher(term);
        while (matcherAllTerms.find()) {
            term = term.replaceAll(matcherAllTerms.group(), "%").replaceAll("\\s+", "");
        }

        return term;
    }


    /**
     * #4 PRICES (DOLLARS)
     *
     * @param term
     * @return
     */
    public String Price(String term) {
        if (term.contains("$")) {
            term = term.replace("$", "");
            term = term + " Dollars";
        }

        if (term.contains("US")) {
            term = term.replace("US", "");
        }

        int indexAfterDot;
        float numberInTerm = Float.parseFloat(term.replaceAll("[\\D]", ""));
        if (term.contains(".")) {
            String numberInTermStr = numberInTerm + "";
            indexAfterDot = numberInTermStr.length() - (term.indexOf(".") + 1);
            numberInTerm = numberInTerm / (float) Math.pow(10, indexAfterDot);
        }

        if (numberInTerm >= 1000000) {
            return numberInTerm / 1000000 + " M" + " Dollars";
        }

        if (term.contains("million") || term.contains("m ")) {
            return numberInTerm + " M" + " Dollars";
        }

        if (term.contains("billion") || term.contains("bn ")) {
            return numberInTerm * 1000 + " M" + " Dollars";
        }

        return term;
    }



    /**
     * 5 DATES
     */
    enum Mounth {january, february, march, april, may, june, july, august, september, october, november, december}

    enum MountThreeChar {jan, feb, mar, apr, may, jun, jul, aug, sep, oct, nov, dec}

    public String numWithDates(String term) {

        String strWithDigitOnly = term.replaceAll("[^\\d]", "");
        String strWithCharOnly = term.replaceAll(strWithDigitOnly, "");
        if(strWithDigitOnly.isEmpty()){
            return "";
        }

        float numberInTerm = Float.parseFloat(strWithDigitOnly);
        if (numberInTerm < 10) {
            strWithDigitOnly = "0" + strWithDigitOnly;
        }

        strWithCharOnly = strWithCharOnly.replaceAll("[\\s]", "");

        int monthNumber = monthContains(strWithCharOnly);
        if (monthNumber == -1) {
            return "";
        }
        String monthNumberStr = String.valueOf(monthNumber);
        if (monthNumber < 10) {
            monthNumberStr = "0" + monthNumber;
        }
        // Month Number Could be a year
        if (strWithDigitOnly.contains("-") || Float.parseFloat(strWithDigitOnly) <= 31) {
            return monthNumberStr + "-" + strWithDigitOnly;
        } else {
            return strWithDigitOnly + "-" + monthNumberStr;
        }
    }

    /**
     * #5.1 Help Function
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
        i = 1;
        for (MountThreeChar m : MountThreeChar.values()) {
            if (m.name().equals(test.toLowerCase())) {
                return i;
            }
            i++;
        }

        return -1;
    }


    /**
     * #6 NAMES
     * @param fullText
     * @param docName
     */
    private void searchNames(String fullText, String docName) {
        Pattern patternName = Pattern.compile("(?:[A-Z]+\\w*(?:-[A-Za-z]+)*(?:\\W|\\s+)){2,}", Pattern.MULTILINE);
        Matcher matcherName = patternName.matcher(fullText);
        while (matcherName.find()) {
            String name = matcherName.group().toUpperCase();
            name = Pattern.compile("[,.:;)-?!}\\]\"\'*]", reOptions).matcher(name).replaceAll("");
            name = Pattern.compile("\n|\\s+", reOptions).matcher(name).replaceAll(" ").trim();

            // addTermToMap(name, docName);
        }
    }

}
