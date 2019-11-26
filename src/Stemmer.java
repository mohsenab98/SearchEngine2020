import org.tartarus.snowball.ext.PorterStemmer;

public class Stemmer {

    private PorterStemmer stemmer;
    public Stemmer(){
        this.stemmer = new PorterStemmer();
    }

    public String porterStemmer(String input){
        stemmer.setCurrent(input); //set string you need to stem
        stemmer.stem();  //stem the word
        return stemmer.getCurrent();//get the stemmed words
    }
}
