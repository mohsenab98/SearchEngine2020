import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parse {
    private boolean stem;
    private Stemmer stemmer;

    private Set<String> setStopWords;
    private SortedMap<String, ArrayList<String>> mapTerms;

    private final int reOptions = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL;

    //Constructor
    public Parse(String stopWordsPath, boolean stem) {
        this.stem = stem;
        this.stemmer = new Stemmer();
        this.mapTerms = new TreeMap<>();

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
        int counterBetween = 0;
        String between = "";
        StringBuilder tokensPossition = new StringBuilder();
        StringBuilder tokens = new StringBuilder();
        Map<String, ArrayList<Integer>> betweenPosition = new LinkedHashMap<>();
        Integer startBetween = 0;

        Pattern patternTerm = Pattern.compile("(\\w+(?:\\.\\d+)?(?:[-/]\\s*\\w+)*)((?:\\W|\\s+))", reOptions);
        Matcher matcherTerm = patternTerm.matcher(fullText);
        while (matcherTerm.find()) {
            String term = matcherTerm.group(1);
            // Between
            if (term.equalsIgnoreCase("between") || counterBetween > 0) {
                between = term.toLowerCase() + " ";
                if(counterBetween == 0){
                    startBetween = matcherTerm.start();
                }

                counterBetween++;
                if (counterBetween == 4) {
                    if(!betweenPosition.containsKey(between)) {
                        betweenPosition.put(between, new ArrayList<>());
                        betweenPosition.get(between).add(startBetween);
                        betweenPosition.get(between).add(matcherTerm.end(1));
                    }
                    between = "";
                    counterBetween = 0;
                }
                continue;
            }
            // Stop words
            if (isStopWord(term)) {
                continue;
            }

            // Stem
            if(this.stem) {
                if (Character.isUpperCase(term.charAt(0))) {
                    term = this.stemmer.porterStemmer(term.toLowerCase());
                    term = term.toUpperCase();
                } else {
                    term = this.stemmer.porterStemmer(term.toLowerCase());
                }
            }

            tokensPossition.append(term).append("(" + matcherTerm.start() + ":" + matcherTerm.end() + ")").append(" ");
            tokens.append(term).append(" ");
            addTermToMap(term.toLowerCase(), docName, matcherTerm.start(1), matcherTerm.end(1));
        }

        if (!between.isEmpty()) {
            between(betweenPosition, docName);
        }

        // threadPool.execute(() -> {
        termFormat(tokens.toString(), tokensPossition.toString(), docName);
        searchNames(fullText, docName);
        //  });
        int x = 0;
    }

    /**
     * Format terms to defined templates
     *
     * @param fullText
     * @return
     */
    public void termFormat(String fullText, String tokensPossition, String docName) {
        String term;
        // #1 M/K/B
        Pattern patternNumbers = Pattern.compile("(\\d+(?:,\\d+)*)\\((\\d+:\\d+)\\)((?:\\D+(?:Thousand|Million|Billon))?(?:/\\d+)?(?:(?:\\.\\d+)*)?(?:-\\d+)?)\\((\\d+:\\d+)\\)", reOptions);
        Matcher matcherNumbers = patternNumbers.matcher(fullText);
        while (matcherNumbers.find()) {
            term = matcherNumbers.group(1) + matcherNumbers.group(3);
            addTermToMap(term, docName, Integer.valueOf(Pattern.compile("(\\d+?):").matcher(matcherNumbers.group(2)).group(1)), Integer.valueOf(Pattern.compile(":(\\d+)").matcher(matcherNumbers.group(4)).group(1)));
        }
        // #3 %
        Pattern patternPercent = Pattern.compile("(\\d+(?:\\.\\d+)?)\\((\\d+:\\d+)\\)(\\s*)(%|(?:percentage?)|(?:percent))\\((\\d+:\\d+)\\)", reOptions);
        Matcher matcherPercent = patternPercent.matcher(fullText);
        while (matcherPercent.find()) {
            term = matcherPercent.group(1) + matcherPercent.group(3) + matcherPercent.group(4);
            addTermToMap(term, docName, Integer.valueOf(Pattern.compile("(\\d+?):").matcher(matcherPercent.group(2)).group(1)), Integer.valueOf(Pattern.compile(":(\\d+)").matcher(matcherPercent.group(5)).group(1)));
          //  indexer.addTermToIndexer(numWithPercent(term).toLowerCase(), docName);
        }

        // #4 Dates
//        Pattern patternDate = Pattern.compile("\\d+\\s\\w+|\\w+\\s\\d+", reOptions);
        Pattern patternDate = Pattern.compile("\\d+(?:-\\d+)?(\\s*)\\w+", reOptions);
        Matcher matcherDate = patternDate.matcher(fullText);
        while (matcherDate.find()) {
            term = matcherDate.group(1) + matcherDate.group();
         //   indexer.addTermToIndexer(numWithDates(term).toLowerCase(), docName);
        }

        // #5 Prices
        Pattern patternPrice = Pattern.compile("\\$?\\d+(?:.\\d+)?\\s*(?:(?:million)|(?:billion)|(?:trillion)|(?:m)|(?:bn))?\\s*(?:(?:Dollars)|(?:U.S.))?\\s*(?:dollars)?", reOptions);
        Matcher matcherPrice = patternPrice.matcher(fullText);
        while (matcherPrice.find()) {
            term = matcherPrice.group();
          //  indexer.addTermToIndexer(Price(matcherPrice.group()).toLowerCase(), docName);
        }

    }


    private boolean isStopWord(String term) {
        if (this.setStopWords.contains(term.toLowerCase())) {
            return true;
        }
        return false;
    }

    private void between(Map<String, ArrayList<Integer>> between, String docName) {
        for (String term : between.keySet()) {
            if (Pattern.compile("between \\d+ and \\d+").matcher(term).matches()) {
                addTermToMap(term, docName, between.get(term).get(0), between.get(term).get(1));

            }
            else {
                Pattern patternTerm = Pattern.compile("\\w+", reOptions);
                Matcher matcherTerm = patternTerm.matcher(term);
                while (matcherTerm.find()) {
                    addTermToMap(term, docName, between.get(term).get(0), between.get(term).get(1));
                }
            }
        }
    }


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

    private void addTermToMap(String term, String docName, int start, int end){
        if(!this.mapTerms.containsKey(term)) {
            this.mapTerms.put(term, new ArrayList<>());
            this.mapTerms.get(term).add(docName);
        }

        this.mapTerms.get(term).add(start + ":" + end);
    }

    public void cleanTerms(){
        this.mapTerms.clear();
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
        } else if (term.contains("Thousand")) {
            range = 4;
        } else if (term.contains("Million")) {
            range = 5;
        } else if (term.contains("Billion")) {
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

        String strWithDigitOnly = term.replaceAll("[\\D]", "");
        float numberInTerm = Float.parseFloat(strWithDigitOnly);
        String strWithCharOnly = term.replaceAll(strWithDigitOnly, "");
        if (numberInTerm < 10) {
            strWithDigitOnly = "0" + strWithDigitOnly;
        }
        strWithCharOnly = strWithCharOnly.replaceAll("[\\s]", "");

        int monthNumber = monthContains(strWithCharOnly);
        if (monthNumber == -1) {
            return term;
        }
        String monthNumberStr = String.valueOf(monthNumber);
        if (monthNumber < 10) {
            monthNumberStr = "0" + monthNumber;
        }
        // Month Number Could be a year
        if (numberInTerm <= 31) {
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

            addTermToMap(name, docName, matcherName.start(), matcherName.end());
        }
    }
}
