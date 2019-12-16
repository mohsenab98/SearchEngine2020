package Classes;

import Classes.Stemmer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
////////
public class Parse {
    //private ExecutorService threadPool = Executors.newCachedThreadPool();
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
     *          Value: list of properties of a doc
     */
    private SortedMap<String, ArrayList<String>> mapTerms;
    /**
     * the list of the properties of a doc
     */
    private ArrayList<String> docInfo;

    //Constructor
    public Parse(String stopWordsPath, boolean stem) {
        this.stem = stem;
        this.stemmer = new Stemmer();
        this.mapTerms = new TreeMap<>();
        this.docInfo = new ArrayList<>();

        // charge stop words from file on hard disk to set in RAM
        try {
            String stopWords = new String(Files.readAllBytes(Paths.get(stopWordsPath)));
            this.setStopWords = stringToSetOfString(stopWords);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * scan doc with regex -> split TEXT to tokens -> check token for laws -> add token to map as term
     * @param fullText - text for parse, name of doc of text
     */
    public void Parser(String fullText, String docName) {
        this.docInfo.add(docName); // add doc's name in property-doc-list

        // between law values
        int betweenCounter = 0;
        String lawBetween = "";

        // text without punctuation for token format
        ArrayList<String> tokensFullText = new ArrayList<>();

        Pattern patternToken = Pattern.compile("([A-Za-z0-9%$]+(?:\\.\\d+)?(?:[-/]\\s*\\w+)*(?:\\.\\w+)?)((?:\\W|\\s+))", reOptions);
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


           // addTermToMap(token); // add token as term to map
        }

        tokenFormat(tokensFullText); // dates, numbers, %, price, +2 ours laws
        //searchNames(fullText); // Entity/Names law

        // add properties to property-doc-list
        this.docInfo.add(this.termMaxTf);
        this.docInfo.add(String.valueOf(this.counterMaxTf));
        this.docInfo.add(String.valueOf(this.mapTerms.size()));
    }

    /**
     * laws: format tokens to defined templates
     *
     * @param fullText - text for parse
     */
    private void tokenFormat(ArrayList<String> fullText) {
        String preToken = "";

        String postToken = "";
        String token = "";
        String number = "";
        int size = fullText.size();

        for (int i = 0; i < size; i++) {
            token = fullText.get(i);
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
            //TODO:add names !
//            if(!postToken.equals("") && Character.isUpperCase(token.charAt(0)) && Character.isUpperCase(postToken.charAt(0))){
//                if(i + 2 < size){
//                    String postPostTerm = fullText.get(i + 2);
//                    if(Character.isUpperCase(postPostTerm.charAt(0))){
//                        if(i + 3 < size){
//                            String postPostPostTerm = fullText.get(i + 3);
//                            if(Character.isUpperCase(postPostPostTerm.charAt(0))){
//                                String name = token + " " + postToken + " " + postPostTerm + " " + postPostPostTerm;
//                                addTermToMap(name);
//                                i = i + 3;
//                                continue;
//                            }
//                        } // if name Size 4 END
//                        String name = token + " " + postToken + " " + postPostTerm;
//                        addTermToMap(name);
//                        i = i + 2;
//                        continue;
//                    }
//
//                }// if name Size 3 END
//                String name = token + " " + postToken ;
//                addTermToMap(name);
//                i = i + 1;
//                continue;
//            } // If names END


            addTermToMap(token);


            ///namessss
        }
    }


        /*
        String token;
        // #4 Dates
        Pattern patternDate = Pattern.compile("(?:(?:\\d{1,2}-)?\\d{1,2}\\s*)(?:jan\\w*|feb\\w*|mar\\w*|apr\\w*|may|jun\\w?|jul\\w?|aug\\w*|sep\\w*|oct\\w*|nov\\w*|dec\\w*)|(?:jan\\w+|feb\\w*|mar\\w*|apr\\w*|may|jun\\w?|jul\\w?|aug\\w*|sep\\w*|oct\\w*|nov\\w*|dec\\w*)(?:\\s*\\d{1,4})", reOptions);
        Matcher matcherDate = patternDate.matcher(fullText);
        while (matcherDate.find()) {
            token = matcherDate.group().trim();
            addTermToMap(numWithDates(token));
        }
        // #1 M/K/B
        Pattern patternNumbers = Pattern.compile("(\\d+(?:[.,-]\\d+)*)((?:\\s*thousand)?(?:\\s*million)?(?:\\s*billion)?)", reOptions);
        Matcher matcherNumbers = patternNumbers.matcher(fullText);
        while (matcherNumbers.find()) {
            token = (matcherNumbers.group(1) + " " + matcherNumbers.group(2)).trim();
            if(token.contains("-")){
                continue;
            }
            addTermToMap(numWithoutUnits(token));
        }
        // #3 %
        Pattern patternPercent = Pattern.compile("(\\d+(?:\\.\\d+)?)(\\s*)(%|(?:percentage?)|(?:percent))", reOptions);
        Matcher matcherPercent = patternPercent.matcher(fullText);
        while (matcherPercent.find()) {
            token = (matcherPercent.group(1) + matcherPercent.group(2) + matcherPercent.group(3)).trim();
            addTermToMap(numWithPercent(token));
        }
        // #5 Prices: 2 patterns
        Pattern patternPrice = Pattern.compile("\\$\\d+(?:[,.]+\\d+)?\\s*(?:(?:million)|(?:billion)|(?:trillion)|(?:m)|(?:bn))?", reOptions);
        Matcher matcherPrice = patternPrice.matcher(fullText);
        while (matcherPrice.find()) {
            token = matcherPrice.group().trim();
            addTermToMap(Price(token));
        }
        patternPrice = Pattern.compile("\\d+(?:.\\d+)?\\s*(?:(?:million)|(?:billion)|(?:trillion)|(?:m)|(?:bn))?\\s*(?:U.S.)?\\s*(?:dollars)", reOptions);
        matcherPrice = patternPrice.matcher(fullText);
        while (matcherPrice.find()) {
            token = matcherPrice.group().trim();
            addTermToMap(Price(token));
        }
    }
*/
    /**
     * check if token(lower and upper cases) is stop word
     * @param token
     * @return true/false
     */
    private boolean isStopWord(String token) {
        if (this.setStopWords.contains(token.toLowerCase())) {
            return true;
        }
        return false;
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
            setString.add(sc2.next());
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
        term = term.replaceAll("\\s*\n", "");
        term = term.replaceAll("^_", "");
        term = term.replaceAll("^\\w\\s", "");

        if(term.equals("") || term.charAt(0) == '%'){
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
            this.mapTerms.put(term, new ArrayList<>());
            this.mapTerms.get(term).add(String.valueOf(1)); // term counter = 1
            return;
        }

        int counter = Integer.parseInt(this.mapTerms.get(term).get(0)); // term with maxTF
        this.mapTerms.get(term).set(0, String.valueOf(counter + 1)); // maxTF

        // check for max TF
        if(this.counterMaxTf < Integer.parseInt(this.mapTerms.get(term).get(0))){
            this.counterMaxTf = Integer.parseInt(this.mapTerms.get(term).get(0));
            this.termMaxTf = term;
        }
    }


    /**
     * getter for map of terms
     * @return mapTerms
     */
    public Map<String, ArrayList<String>> getMapTerms(){
        return this.mapTerms;
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
//        this.mapTerms.clear();
//        this.docInfo.clear();
        this.mapTerms = new TreeMap<>();
        this.docInfo = new ArrayList<>();
        //threadPool.shutdown();
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
    private void searchNames(String fullText) {Pattern patternName = Pattern.compile("(?:[A-Z]+\\w*(?:-[A-Za-z]+)*(?:\\W|\\s+)){2}", Pattern.MULTILINE);
        Matcher matcherName = patternName.matcher(fullText);
        while (matcherName.find()) {
            String name = matcherName.group();
            //String name = matcherName.group().toUpperCase();
            name = Pattern.compile("[,.:;)-?!}\\]\"\'*]", reOptions).matcher(name).replaceAll("");
            name = Pattern.compile("\n|\\s+", reOptions).matcher(name).replaceAll(" ").trim();
            name = name.replaceFirst("^\\w\\s", "");

            if(stem && name.contains(" ")){
                String[] tokens = name.split(" ");
                String token1 = this.stemmer.porterStemmer(tokens[0].toLowerCase());
                String token2 = this.stemmer.porterStemmer(tokens[1].toLowerCase());
                name = token1 + " " + token2;
            }

            addTermToMap(name.toUpperCase());
        }

    }
}
