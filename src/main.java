import java.util.Set;

public class main {
    //ddddddddddddddd evgenyyyyyy
    public static void main(String[] args){

    /////// ReadFile tests ///////
        String path = "Jackpot!";
        ReadFile rd = new ReadFile(path);
        System.out.println(rd.getPath());
        rd.setPath("C:\\Users\\mohse\\Desktop\\corpusTest");
        System.out.println(rd.getPath());
        rd.printPath();
        rd.filesSeparator();
//l
    /////// Parse tests ///////
//        Stemmer s = new Stemmer();
//        String fullText = s.porterStemmer("revolution parking boys girls");
//        System.out.println(fullText);
        Parse p = new Parse(rd.getMapAllDocs(), "C:\\Users\\mohse\\Desktop\\corpusTest\\stopWords.txt");
        p.Parser();
//        p.termIdentifier("100 Million pepole in the world + 100 kkk 100,000,000,000,000 Billion 999.99");
        //System.out.println(p.NumWithoutUnits("1010.56"));
        //System.out.println(p.NumWithPercent("15 percent"));
        //System.out.println(p.NumWithDates("15 May"));
        //System.out.println(p.Price("20.6m Dollars"));

        // p.stringToSetOfString("a ab 1 abc 123 Ab2 abbb");
//        p.pathOfStopWordsToSetOfStrings("C:\\Users\\mohse\\Desktop\\corpusTest\\stopWords.txt");
         // Set<String> setString = p.deleteStopWords("C:\\Users\\mohse\\Desktop\\corpusTest\\stopWords.txt","apple a bannana any wish dad did");
         // System.out.println(setString.toString());
    }
}
