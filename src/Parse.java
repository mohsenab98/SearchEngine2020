import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
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

            fullText = removePunctuationAndSpaces(fullText);

        }
    }

    private String removePunctuationAndSpaces(String fullText){
        // Clean punctuation
        fullText = fullText.replace("\n", " ");
        fullText = fullText.replace("\r", " ");

        Pattern patternPunctuation = Pattern.compile("[,:;()?!{}\\[\\]\"\']", reOptions);
        Matcher matcherPunctuation = patternPunctuation.matcher(fullText);
        while (matcherPunctuation.find()) {
            fullText = matcherPunctuation.replaceAll("");
        }

        // Remove spaces to one space only
        Pattern patternSpaces = Pattern.compile("\\s+|/|--", reOptions);
        Matcher matcherSpaces = patternSpaces.matcher(fullText);
        while (matcherSpaces.find()) {
            fullText = matcherSpaces.replaceAll(" ");
        }

        Pattern patternHyphen = Pattern.compile("-(\\s+?)\\w", reOptions);
        Matcher matcherHyphen = patternHyphen.matcher(fullText);
        while (matcherHyphen.find()) {
            fullText = matcherHyphen.replaceAll("-");
        }



        return fullText.trim();
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
