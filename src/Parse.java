import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parse {
    private ExecutorService threadPool;
    private Stemmer stemmer;

    private String stopWordsPath;
    private Set<String> setStopWords;
    private Map<String, Map<String, Integer>> mapNames;
    private Set<String> setRawUpperCase;
    private Set<String> setUpperCase;
    private ArrayList<byte[]> allDocs;

    private final int reOptions = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL;

    //Constructor
    public Parse(List<byte[]> allDocs, String stopWordsPath) {
        threadPool = Executors.newCachedThreadPool();
        this.stemmer = new Stemmer();

        this.stopWordsPath = stopWordsPath;
        this.mapNames = new ConcurrentHashMap<>();
        this.setRawUpperCase = new LinkedHashSet<>();
        this.setUpperCase = new LinkedHashSet<>();
        this.allDocs = new ArrayList<>(allDocs);
    }

    /**
     * The main function for parse
     */
    public void Parser() {
//        int counter = 1;
        try {
            String stopWords = new String(Files.readAllBytes(Paths.get(stopWordsPath)));
            this.setStopWords = stringToSetOfString(stopWords);

        } catch (Exception e) {
            e.printStackTrace();
        }

        while (!allDocs.isEmpty()) {
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

//            System.out.println(counter);
//            counter ++;
        }

        // Add names to terms
        cleanNames();
        //this.mapTermsInDocs.putAll(this.mapNames);

        threadPool.shutdown();
        int x = 0;
    }

    /**
     * Format terms to defined templates
     *
     * @param fullText
     * @return
     */
    public void termFormat(String fullText, String docName) {
        String term;
        // #1 M/K/B
        Pattern patternNumbers = Pattern.compile("(\\d+(?:,\\d+)*)((?:\\D+(?:Thousand|Million|Billon))?(?:/\\d+)?(?:(?:\\.\\d+)*)?(?:-\\d+)?)", reOptions);
        Matcher matcherNumbers = patternNumbers.matcher(fullText);
        while (matcherNumbers.find()) {
            term = matcherNumbers.group(1) + matcherNumbers.group(2);
            term = numWithoutUnits(term);
           // addTermToMap(numWithoutUnits(term), docName);
        }
        // #3 %
        Pattern patternPercent = Pattern.compile("(\\d+(?:\\.\\d+)?)(\\s*)(%|(?:percentage?)|(?:percent))", reOptions);
        Matcher matcherPercent = patternPercent.matcher(fullText);
        while (matcherPercent.find()) {
            term = matcherPercent.group(1) + matcherPercent.group(2) + matcherPercent.group(3);
            term = numWithPercent(term);
           // addTermToMap(numWithPercent(term), docName);
        }

        // #4 Dates
//        Pattern patternDate = Pattern.compile("\\d+\\s\\w+|\\w+\\s\\d+", reOptions);
        Pattern patternDate = Pattern.compile("\\d+(?:-\\d+)?(\\s*)\\w+", reOptions);
        Matcher matcherDate = patternDate.matcher(fullText);
        while (matcherDate.find()) {
            term = matcherDate.group(1) + matcherDate.group();
           // addTermToMap(numWithDates(term), docName);
        }

        // #5 Prices
        Pattern patternPrice = Pattern.compile("\\$?\\d+(?:.\\d+)?\\s*(?:(?:million)|(?:billion)|(?:trillion)|(?:m)|(?:bn))?\\s*(?:(?:Dollars)|(?:U.S.))?\\s*(?:dollars)?", reOptions);
        Matcher matcherPrice = patternPrice.matcher(fullText);
        while (matcherPrice.find()) {
           // addTermToMap(Price(matcherPrice.group()), docName);
        }

    }

    /**
     * Remove all punctuation chars, dots, &amp, spaces and / with STRING
     *
     * @param fullText
     * @return full text: words separated by space
     */
    private void separateTermsFromText(String fullText, String docName) {
        int counterBetween = 0;
        StringBuilder between = new StringBuilder();
        StringBuilder tokens = new StringBuilder();
        Pattern patternTerm = Pattern.compile("(\\w+(?:\\.\\d+)?(?:[/-]\\s*\\w+)*)((?:\\W|\\s+))", reOptions);
        Matcher matcherTerm = patternTerm.matcher(fullText);
        while (matcherTerm.find()) {
            String term = matcherTerm.group(1);
            // Between
            if (term.equalsIgnoreCase("between") || counterBetween > 0) {
                between.append(term.toLowerCase()).append(" ");
                counterBetween++;
                if (counterBetween == 4) {
                    between.append(",");
                    counterBetween = 0;
                }
                continue;
            }
            // Stop words
            if (isStopWord(term)) {
                continue;
            }
            // Stem
            if (Character.isUpperCase(term.charAt(0))) {
                term = this.stemmer.porterStemmer(term.toLowerCase());
                term = term.toUpperCase();
            } else {
                term = this.stemmer.porterStemmer(term.toLowerCase());
            }

            tokens.append(term).append(" ");

            // Check Upper Case letters and add term -> doc to map
            termUpperCase(term);
        }

        if (!between.toString().isEmpty()) {
            between(between.toString(), docName);
        }

        threadPool.execute(() -> {
            termFormat(tokens.toString(), docName);
            searchNames(fullText, docName);
        });
        int x = 0;
    }

    private boolean isStopWord(String term) {
        if (this.setStopWords.contains(term.toLowerCase())) {
            return true;
        }
        return false;
    }

    private void between(String between, String docName) {
        String[] tokens = between.split("\\s*,");
        for (String term : tokens) {
            if (Pattern.compile("between \\d+ and \\d+").matcher(term).matches()) {
               // addTermToMap(term, docName);
            } else {
                Pattern patternTerm = Pattern.compile("\\w+", reOptions);
                Matcher matcherTerm = patternTerm.matcher(term);
                while (matcherTerm.find()) {
                   // addTermToMap(matcherTerm.group(), docName);
                }
            }
        }
    }

    private void termUpperCase(String term) {
        if (Character.isLowerCase(term.charAt(0)) && this.setUpperCase.contains(term.toUpperCase())) {
            this.setUpperCase.remove(term.toUpperCase());
            this.setRawUpperCase.add(term.toLowerCase());
        }
        else if (Character.isUpperCase(term.charAt(0)) && !this.setRawUpperCase.contains(term.toLowerCase())) {
            this.setUpperCase.add(term.toUpperCase());
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
            String name = matcherName.group();
            name = Pattern.compile("[,.:;)-?!}\\]\"\'*]", reOptions).matcher(name).replaceAll("");
            name = Pattern.compile("\n|\\s+", reOptions).matcher(name).replaceAll(" ").trim();
;
            if (this.mapNames.containsKey(name)) {
                Map<String, Integer> docMap = mapNames.get(name);
                if(docMap.containsKey(docName)) {
                    docMap.put(docName, docMap.get(docName) + 1);
                }
                else {
                    docMap.put(docName, 1);
                }
            }
            else{
                this.mapNames.put(name, new LinkedHashMap<>());
                this.mapNames.get(name).put(docName, 1);
            }
        }
    }

    private void cleanNames() {
        for (Map.Entry<String, Map<String, Integer>> name : this.mapNames.entrySet()) {
            if (name.getValue().size() <= 1) {
                this.mapNames.remove(name.getKey());
            }
        }
    }
}
