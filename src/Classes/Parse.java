package Classes;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.util.stream.Collectors.toMap;

public class Parse {
    private boolean stem; // flag to use stemming
    private Stemmer stemmer;
    private int counterMaxTf = 0; // counter for max frequency term in a doc
    private String termMaxTf = ""; // max frequency term in a doc
    private final int reOptions = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL; // definitions for regex

    /**
     * set of stop words
     */
    private Set<String> setStopWords;
    /**
     * dictionary with a-z sorted keys:
     *          Key: all original terms in a doc
     *          Value: TF in the doc
     */
    private Map<String, String> mapTerms;

    /**
     * dictionary with entities of the document:
     *          Key: TF
     *          Value: Entity
     */
    private Map<String, Integer> mapDocEntities;
    /**
     * the list of the properties of a doc
     */
    private ArrayList<String> docInfo;

    /**
     * String of stop words
     */
    String stopWords;
    /**
     *  words to numbers
     */
    private List<String> listNumbersAsWords;
    private Set<String> setNumbersAsWords;

    //Constructor
    public Parse(String stopWordsPath, boolean stem) {
        this.stem = stem;
        this.stemmer = new Stemmer();
        this.mapTerms = new TreeMap<>();
        this.mapDocEntities = new TreeMap<>();
        this.docInfo = new ArrayList<>();
        listNumbersAsWords = Arrays.asList
                (
                        "zero","one","two","three","four","five","six","seven",
                        "eight","nine","ten","eleven","twelve","thirteen","fourteen",
                        "fifteen","sixteen","seventeen","eighteen","nineteen","twenty",
                        "thirty","forty","fifty","sixty","seventy","eighty","ninety",
                        "hundred","thousand","million","billion","trillion"
                );

        setNumbersAsWords = new HashSet<>(listNumbersAsWords);
        // charge stop words from file on hard disk to set in RAM
        try {
            this.stopWords = new String(Files.readAllBytes(Paths.get(stopWordsPath)));
            this.setStopWords = stringToSetOfString(stopWords);
            // for words to numbers law
            this.setStopWords.removeAll(setNumbersAsWords);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * scan doc with regex -> split TEXT to tokens -> check token for laws -> add token to map as term
     * @param fullText - text for parse, name of doc of text
     */
    public void Parser(String fullText, String docName) {
        fullText = fullText + " ";
        this.docInfo.add(docName); // add doc's name in property-doc-list
        this.counterMaxTf = 0;
        this.termMaxTf = "";

        // between law values
        int betweenCounter = 0;
        String lawBetween = "";

        // text without punctuation for token format
        ArrayList<String> tokensFullText = new ArrayList<>();

        Pattern patternToken = Pattern.compile("([A-Za-z0-9%$]+(?:[.,]\\d+)?(?:[-/]\\s*\\w+)*(?:\\.\\w+)?)((?:\\W|\\s+))", reOptions);
        Matcher matcherToken = patternToken.matcher(fullText);
        while (matcherToken.find()) { // for each token
            String token = matcherToken.group(1);
            // Between law
            if (token.equalsIgnoreCase("between") || lawBetween.contains("between")) {
                if (betweenCounter == 1 && !Character.isDigit(token.charAt(0))) {
                    lawBetween = "";
                    betweenCounter = 0;
                } else if (betweenCounter < 3) {
                    lawBetween += token.toLowerCase() + " ";
                    betweenCounter++;
                    continue;
                } else {
                    lawBetween += token.toLowerCase() + " ";
                    addTermToMap(lawBetween.trim());
                    betweenCounter = 0;
                    lawBetween = "";
                    continue;
                }
            }

            // check stop words
            if (isStopWord(token)) {
                continue;
            }
            tokensFullText.add(token);
        }

        tokenFormat(tokensFullText); // dates, numbers, %, price, +2 ours laws
        searchNames(fullText); // Entity/Names law

        addInfoToDoc();// add term tf, maxTf, amount of terms, 5 common entities to doc

    }


    /**
     * laws: format tokens to defined templates
     *
     * @param fullText - text for parse
     */
    private void tokenFormat(ArrayList<String> fullText) {
        String preToken = "";
        String postToken;
        String token;
        int size = fullText.size();

        for (int i = 0; i < size; i++) {
            token = fullText.get(i);

            // Repair numbers with "O" instead 0
            if(token.matches(".*\\d.*") && token.toLowerCase().contains("o")){
                token = token.toLowerCase().replaceAll("o", "0");
            }


            if (Character.isDigit(token.charAt(0))) {
                if (i > 0) {
                    preToken = fullText.get(i - 1);
                }
                if(i < size - 1) {
                    postToken = fullText.get(i + 1);
                }
                else {
                    postToken = "";
                }
                // % law
                if(token.contains("%")){
                    addTermToMap(token);
                    continue;
                }
                else if (postToken.equalsIgnoreCase("percent")
                        || postToken.equalsIgnoreCase("percentage")) {
                    String percent = numWithPercent(token + " " + postToken);
                    addTermToMap(percent);
                    i++;
                    continue;

                }
                // Month Law
                else if (monthContains(preToken) != -1) {
                    String date = numWithDates(preToken + " " + token);
                    addTermToMap(date);
                    continue;
                }
                else if(monthContains(postToken) != -1){
                    String date = numWithDates(token + " " + postToken);
                    addTermToMap(date);
                    i++;
                    continue;
                }
                // prices
                else if(postToken.equalsIgnoreCase("dollars")){
                    String price = price(token + " " + postToken);
                    addTermToMap(price);
                    continue;
                }
                else if (i < size - 2 && (postToken.equalsIgnoreCase("m")
                        || postToken.equalsIgnoreCase("bn"))){
                    String postPostToken = fullText.get(fullText.indexOf(postToken) + 1);
                    if(postPostToken.equalsIgnoreCase("dollars")){
                        String price = price(token + " " + postToken + " " + postPostToken);
                        addTermToMap(price);
                        i = i + 2;
                        continue;
                    }
                }
                else if(i < size - 3 && (postToken.equalsIgnoreCase("million")
                        || postToken.equalsIgnoreCase("billion")
                        || postToken.equalsIgnoreCase("trillion"))){
                    String postPostToken = fullText.get(fullText.indexOf(postToken) + 1);
                    String postPostPostToken = fullText.get(fullText.indexOf(postToken) + 2);
                    if(postPostToken.equalsIgnoreCase("u.s")){
                        String price = price(token + " " + postToken + " " + postPostToken + " " + postPostPostToken);
                        addTermToMap(price);
                        i = i + 3;
                        continue;
                    }
                }
                // without units
                else{
                    String num = numWithoutUnits(token);
                    addTermToMap(num);
                    continue;
                }


            } // if  number END

            // words to numbers
            String numbers = "";
            if(setNumbersAsWords.contains(fullText.get(i))) {
                while (i < size && setNumbersAsWords.contains(fullText.get(i))) {
                    numbers += fullText.get(i) + " ";
                    i++;
                }
                i--;

                String tokenNum = wordsToNum(numbers.trim(), listNumbersAsWords);
                addTermToMap(numWithoutUnits(tokenNum));
                continue;
            }

            addTermToMap(token);
        }
    }

    /**
     * check if token(lower and upper cases) is stop word
     * @param token
     * @return true/false
     */
    private boolean isStopWord(String token) {
        return this.setStopWords.contains(token.toLowerCase());
    }

    /**
     * do string to be set by each word
     * @param fullText
     * @return setString that represent the strings of the text
     */
    private Set<String> stringToSetOfString(String fullText) {
        Pattern pattern = Pattern.compile("\n", reOptions);
        Scanner sc2 = new Scanner(fullText).useDelimiter(pattern);
        Set<String> setString = new LinkedHashSet<>();
        while (sc2.hasNext()) {
            String stopWord = sc2.next();
            stopWord = stopWord.replace("\r", "");
            setString.add(stopWord);
        }
        return setString;

    }

    /**
     * add term to map of terms of the doc as key
     * add list of properties of the doc as value
     * indexes of the list:
     *                      0 - term counter in Doc
     *                      1 - term with max TF
     *                      2 - max TF
     *
     *
     * @param term
     */
    private void addTermToMap(String term){
        term = cleanTerm(term);
        if(term.isEmpty()){
            return;
        }
        // Stemming
        if (Character.isUpperCase(term.charAt(0))) {
            if (this.stem) {
                term = this.stemmer.porterStemmer(term.toLowerCase());
            }
            term = term.toUpperCase();
        } else if (this.stem) {
            term = this.stemmer.porterStemmer(term);
        }

        // create term in the map
        if(!this.mapTerms.containsKey(term)) {
            this.mapTerms.put(term, "1"); // term counter = 1
            if(Character.isUpperCase(term.charAt(0))){
                this.mapDocEntities.put(term, 1);
            }
            if(this.counterMaxTf <= Integer.parseInt(this.mapTerms.get(term))){
                this.counterMaxTf = Integer.parseInt(this.mapTerms.get(term)); // Max Tf
                this.termMaxTf = term;
            }
            return;
        }

        int counter = Integer.parseInt(this.mapTerms.get(term)); // term with tf
        this.mapTerms.put(term, String.valueOf(counter + 1)); // term counter = 1

        // check for max TF
        if(this.counterMaxTf <= Integer.parseInt(this.mapTerms.get(term))){
            this.counterMaxTf = Integer.parseInt(this.mapTerms.get(term)); // Max Tf
            this.termMaxTf = term;
        }

        // put entity to entity dictionary
        if(Character.isUpperCase(term.charAt(0))){
            this.mapDocEntities.put(term, counter);
        }
    }

    /**
     * add term tf, maxTf, amount of terms, 5 common entities to doc
     */
    private void addInfoToDoc() {
        this.docInfo.add(this.termMaxTf); // term tf
        this.docInfo.add(String.valueOf(this.counterMaxTf)); // maxTf
        this.docInfo.add(String.valueOf(this.mapTerms.size())); // amount of terms


        //  entities to doc
        Map<String, Integer> sortedEntities = this.mapDocEntities
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(
                        toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                LinkedHashMap::new));

        Set<String> setEntity = sortedEntities.keySet();

        this.docInfo.addAll(setEntity);

    }

    /**
     * getter for map of terms
     * @return mapTerms
     */
    public Map<String, String> getMapTerms(){
        return this.mapTerms;
    }

    public String getStopWords(){
        return this.stopWords;
    }

    /**
     * list of doc's properties
     * DocInfo Index:
     *      0 - doc name
     *      1 - term max tf
     *      2 - count max tf
     *      3 - count uniq terms in doc
     * @return
     */
    public ArrayList<String> getDocInfo(){
        return this.docInfo;
    }

    /**
     * delete/clean data structures
     */
    public void cleanParse(){
        this.mapTerms = new TreeMap<>();
        this.mapDocEntities = new TreeMap<>();
        this.docInfo = new ArrayList<>();
    }

////////////////////////////// LAWS //////////////////////////////

    /**
     * #1 MILLION/BILLION/THOUSAND
     *
     * @param token
     * @return token
     */
    private String numWithoutUnits(String token) {
        int indexAfterDot;

        if (token.contains("-") || Pattern.compile("\\.\\d+\\.").matcher(token).find() || token.contains("/")) {
            return token;
        }

        float numberInTerm = Float.parseFloat(token.replaceAll("[\\D]", ""));
        if (token.contains(".")) {
            indexAfterDot = token.length() - (token.indexOf(".") + 1);
            numberInTerm = numberInTerm / (float) Math.pow(10, indexAfterDot);
        }

        int range = 0;
        if ((numberInTerm >= 1000 && numberInTerm < 1000000)) {
            range = 1;
        } else if ((numberInTerm >= 1000000 && numberInTerm < 1000000000)) {
            range = 2;
        } else if (numberInTerm >= 1000000000) {
            range = 3;
        } else if (token.toLowerCase().contains("thousand")) {
            range = 4;
        } else if (token.toLowerCase().contains("million")) {
            range = 5;
        } else if (token.toLowerCase().contains("billion")) {
            range = 6;
        }

        switch (range) {
            case 1:
                return new StringBuilder().append(String.format("%.03f", numberInTerm / 1000)).append("K").toString();
            case 2:
                return new StringBuilder().append(String.format("%.03f", numberInTerm / 1000000)).append("M").toString();
            case 3:
                return new StringBuilder().append(String.format("%.03f", numberInTerm / 1000000000)).append("B").toString();
            case 4:
                return new StringBuilder().append(numberInTerm).append("K").toString();
            case 5:
                return new StringBuilder().append(numberInTerm).append("M").toString();
            case 6:
                return new StringBuilder().append(numberInTerm).append("B").toString();
        }

        return token;
    }

    /**
     * #3 PERCENT
     *
     * @param token
     * @return
     */
    private String numWithPercent(String token) {
        Pattern patternAllTerms = Pattern.compile("percentage|percent", reOptions);
        Matcher matcherAllTerms = patternAllTerms.matcher(token);
        while (matcherAllTerms.find()) {
            token = token.replaceAll(matcherAllTerms.group(), "%").replaceAll("\\s+", "");
        }

        return token;
    }


    /**
     * #4 PRICES (DOLLARS)
     *
     * @param token
     * @return
     */
    private String price(String token) {

        if (token.contains("$")) {
            token = token.replace("$", "");
            token = new StringBuilder().append(token).append(" Dollars").toString();
        }

        if (token.contains("US")) {
            token = token.replace("US", "");
        }

        int indexAfterDot;
        float numberInTerm = Float.parseFloat(token.replaceAll("[\\D]", ""));
        if (token.contains(".")) {
            String numberInTermStr = numberInTerm + "";
            indexAfterDot = numberInTermStr.length() - (token.indexOf(".") + 1);
            numberInTerm = numberInTerm / (float) Math.pow(10, indexAfterDot);
        }

        if (numberInTerm >= 1000000) {
            return new StringBuilder().append(numberInTerm / 1000000).append(" M").append(" Dollars").toString();
        }

        if (token.contains("million") || token.contains("m ")) {
            return new StringBuilder().append(numberInTerm).append(" M").append(" Dollars").toString();
        }

        if (token.contains("billion") || token.contains("bn ")) {
            return new StringBuilder().append(numberInTerm * 1000).append(" M").append(" Dollars").toString();
        }

        return token;
    }

    /**
     * 5 DATES
     */
    enum Mounth {january, february, march, april, may, june, july, august, september, october, november, december}
    enum MountThreeChar {jan, feb, mar, apr, may, jun, jul, aug, sep, oct, nov, dec}

    private String numWithDates(String term) {
        String strWithDigitOnly = term.replaceAll("[^\\d]", "");
        String strWithCharOnly = term.replaceAll(strWithDigitOnly, "");
        if(strWithDigitOnly.isEmpty()){
            return "";
        }

        float numberInTerm = Float.parseFloat(strWithDigitOnly);
        if (numberInTerm < 10) {
            strWithDigitOnly = new StringBuilder().append("0").append(strWithDigitOnly).toString();
        }

        strWithCharOnly = strWithCharOnly.replaceAll("[\\s]", "");

        int monthNumber = monthContains(strWithCharOnly);
        if (monthNumber == -1) {
            return "";
        }
        String monthNumberStr = String.valueOf(monthNumber);
        if (monthNumber < 10) {
            monthNumberStr = new StringBuilder().append("0").append(monthNumber).toString();
        }
        // Month Number Could be a year
        if (strWithDigitOnly.contains("-") || Float.parseFloat(strWithDigitOnly) <= 31) {
            return new StringBuilder().append(monthNumberStr).append("-").append(strWithDigitOnly).toString();
        } else {
            return new StringBuilder().append(strWithDigitOnly).append("-").append(monthNumberStr).toString();
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
     */
    private void searchNames(String fullText) {Pattern patternName = Pattern.compile("(?:[A-Z]+\\w*(?:-[A-Za-z]+)*(?:\\W|\\s+)){2,4}", Pattern.MULTILINE);
        Matcher matcherName = patternName.matcher(fullText);
        while (matcherName.find()) {
            String name = matcherName.group();
            name =  Pattern.compile("[:-]", reOptions).matcher(name).replaceAll(" ");
            name = Pattern.compile("[,.;)?!}\\]\"'*|]", reOptions).matcher(name).replaceAll("");
            name = Pattern.compile("\n|\\s+", reOptions).matcher(name).replaceAll(" ").trim();

            if(stem && name.contains(" ")){
                String[] tokens = name.split(" ");
                String token1 = this.stemmer.porterStemmer(tokens[0].toLowerCase());
                String token2 = this.stemmer.porterStemmer(tokens[1].toLowerCase());
                name = token1 + " " + token2;
            }
            addTermToMap(name.toUpperCase());
        }
    }

    /**
     * Our's law: words to numbers
     * Source: https://stackoverflow.com/questions/26948858/converting-words-to-numbers-in-java
     * @param text
     * @param allowedStrings
     * @return
     */
    private String wordsToNum(String text, List<String> allowedStrings){
        boolean isValidInput = true;
        long result = 0;
        long finalResult = 0;

        String input = text;

        if(input != null && input.length()> 0)
        {
            input = input.replaceAll("-", " ");
            input = input.toLowerCase().replaceAll(" and", " ");
            String[] splittedParts = input.trim().split("\\s+");

            for(String str : splittedParts)
            {
                if(!allowedStrings.contains(str))
                {
                    isValidInput = false;
                    System.out.println("Invalid word found : "+str);
                    break;
                }
            }
            if(isValidInput)
            {
                for(String str : splittedParts)
                {
                    if(str.equalsIgnoreCase("zero")) {
                        result += 0;
                    }
                    else if(str.equalsIgnoreCase("one")) {
                        result += 1;
                    }
                    else if(str.equalsIgnoreCase("two")) {
                        result += 2;
                    }
                    else if(str.equalsIgnoreCase("three")) {
                        result += 3;
                    }
                    else if(str.equalsIgnoreCase("four")) {
                        result += 4;
                    }
                    else if(str.equalsIgnoreCase("five")) {
                        result += 5;
                    }
                    else if(str.equalsIgnoreCase("six")) {
                        result += 6;
                    }
                    else if(str.equalsIgnoreCase("seven")) {
                        result += 7;
                    }
                    else if(str.equalsIgnoreCase("eight")) {
                        result += 8;
                    }
                    else if(str.equalsIgnoreCase("nine")) {
                        result += 9;
                    }
                    else if(str.equalsIgnoreCase("ten")) {
                        result += 10;
                    }
                    else if(str.equalsIgnoreCase("eleven")) {
                        result += 11;
                    }
                    else if(str.equalsIgnoreCase("twelve")) {
                        result += 12;
                    }
                    else if(str.equalsIgnoreCase("thirteen")) {
                        result += 13;
                    }
                    else if(str.equalsIgnoreCase("fourteen")) {
                        result += 14;
                    }
                    else if(str.equalsIgnoreCase("fifteen")) {
                        result += 15;
                    }
                    else if(str.equalsIgnoreCase("sixteen")) {
                        result += 16;
                    }
                    else if(str.equalsIgnoreCase("seventeen")) {
                        result += 17;
                    }
                    else if(str.equalsIgnoreCase("eighteen")) {
                        result += 18;
                    }
                    else if(str.equalsIgnoreCase("nineteen")) {
                        result += 19;
                    }
                    else if(str.equalsIgnoreCase("twenty")) {
                        result += 20;
                    }
                    else if(str.equalsIgnoreCase("thirty")) {
                        result += 30;
                    }
                    else if(str.equalsIgnoreCase("forty")) {
                        result += 40;
                    }
                    else if(str.equalsIgnoreCase("fifty")) {
                        result += 50;
                    }
                    else if(str.equalsIgnoreCase("sixty")) {
                        result += 60;
                    }
                    else if(str.equalsIgnoreCase("seventy")) {
                        result += 70;
                    }
                    else if(str.equalsIgnoreCase("eighty")) {
                        result += 80;
                    }
                    else if(str.equalsIgnoreCase("ninety")) {
                        result += 90;
                    }
                    else if(str.equalsIgnoreCase("hundred")) {
                        result *= 100;
                    }
                    else if(str.equalsIgnoreCase("thousand")) {
                        result *= 1000;
                        finalResult += result;
                        result=0;
                    }
                    else if(str.equalsIgnoreCase("million")) {
                        result *= 1000000;
                        finalResult += result;
                        result=0;
                    }
                    else if(str.equalsIgnoreCase("billion")) {
                        result *= 1000000000;
                        finalResult += result;
                        result=0;
                    }
                    else if(str.equalsIgnoreCase("trillion")) {
                        result *= 1000000000000L;
                        finalResult += result;
                        result=0;
                    }
                }

                finalResult += result;
                return String.valueOf(finalResult);
            }
        }
        return text;
    }

    /**
     * clean law
     * @param term
     * @return
     */
    private String cleanTerm(String term){
        if(term.isEmpty()){
            return "";
        }

        term = term.replaceAll("^(?:\\w\\s)+", "");

        if(term.contains("_")) {
            Pattern startLine = Pattern.compile("^_\\s*");
            term = startLine.matcher(term).replaceAll("");
        }

        if(term.contains("_") || term.contains("/") || term.contains("-")) {
            Pattern endLine = Pattern.compile("[/_-]$");
            term = endLine.matcher(term).replaceAll("");
        }
        if(term.contains("\n")) {
            Pattern cleanLine = Pattern.compile("\\s*\n");
            term = cleanLine.matcher(term).replaceAll("");
        }
        if(term.contains("- ")) {
            Pattern cleanSpace = Pattern.compile("-\\s+");
            term = cleanSpace.matcher(term).replaceAll("-");
        }
        if(term.contains("-$")) {
            Pattern cleanSpace = Pattern.compile("-");
            term = cleanSpace.matcher(term).replaceAll("");
        }
        if(term.contains("$$")){
            Pattern cleanDollar = Pattern.compile("\\${2,}");
            term = cleanDollar.matcher(term).replaceAll("\\$");
        }
        if(term.contains("%%")){
            Pattern cleanPercent = Pattern.compile("%{2,}");
            term = cleanPercent.matcher(term).replaceAll("%");
        }
        if(term.length() == 1 && (term.charAt(0) == '$' ||term.charAt(0) == '%')){
            return "";
        }

        return term.trim();
    }
}