import org.tartarus.snowball.ext.PorterStemmer;
import java.util.regex.Pattern;

public class Stemmer {

    private PorterStemmer stemmer;
    public Stemmer(){
        this.stemmer = new PorterStemmer();
    }

    public String porterStemmer(String input){
        /* Process upper cases too //
        boolean firstLetterUpperCase = false;
        boolean allTermUpperCase = false;

        if(Pattern.compile("[A-Z][A-Z]+").matcher(input).find()){
            allTermUpperCase = true;
        }
        else if(Pattern.compile("[A-Z]\\w").matcher(input).find()){
            firstLetterUpperCase = true;
        }
        stemmer.setCurrent(input.toLowerCase());
        */

        stemmer.setCurrent(input);//set string you need to stem
        stemmer.stem();  //stem the word
        return stemmer.getCurrent(); //get the stemmed words


        // Process upper cases too //
        //String afterStem = stemmer.getCurrent();

        /*
        if(firstLetterUpperCase){
            afterStem = afterStem.substring(0,1).toUpperCase() + afterStem.substring(1, afterStem.length()-1);
        }
        else if(allTermUpperCase){
            afterStem = afterStem.toUpperCase();
        }
        */
        //return afterStem;
    }
}
