import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class main {
    public static void main(String[] args){
        double startTime = System.nanoTime();

    /////// ReadFile tests ///////
        ReadFile rd = new ReadFile();
     //   rd.filesSeparator("D:\\corpus");
     //   rd.filesSeparator("D:\\corpusTestD");

        rd.filesSeparator("C:\\Users\\EvgeniyU\\Desktop\\ThirdYear\\DataRetrieval\\corpusTest");


    /////// Parse tests ///////

        Parse p = new Parse(rd.getListAllDocs(), "C:\\Users\\EvgeniyU\\Desktop\\ThirdYear\\DataRetrieval\\corpusTest\\StopWords.txt");
        p.Parser();
        //p.termIdentifier("123.123.123 100 million");
        //System.out.println(p.NumWithoutUnits("1010.56"));
        //System.out.println(p.NumWithPercent("15 percent"));
        //System.out.println(p.NumWithDates("15 May"));
        //System.out.println(p.Price("20.6m Dollars"));

        // p.stringToSetOfString("a ab 1 abc 123 Ab2 abbb");
//        p.pathOfStopWordsToSetOfStrings("C:\\Users\\mohse\\Desktop\\corpusTest\\stopWords.txt");
         // Set<String> setString = p.deleteStopWords("C:\\Users\\mohse\\Desktop\\corpusTest\\stopWords.txt","apple a bannana any wish dad did");
         // System.out.println(setString.toString());

        double endTime   = System.nanoTime();
        double totalTime = (endTime - startTime) / 1000000000 ;
        System.out.println(totalTime + " sec");

    }
}
