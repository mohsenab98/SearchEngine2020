import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class main {
    public static void main(String[] args){
        double startTime = System.nanoTime();

    /////// ReadFile tests ///////
       // int k = new File("D:\\corpus").list().length;
       // int maxRun = k/6;

        ReadFile rd = new ReadFile();
        //rd.filesSeparator("D:\\corpus");
        rd.filesSeparator("D:\\corpusTestD");
        //rd.filesSeparator("C:\\Users\\EvgeniyU\\Desktop\\ThirdYear\\DataRetrieval\\corpusTest");
        Indexer n = new Indexer();
        Parse p = new Parse(rd.getListAllDocs(), "C:\\Users\\EvgeniyU\\Desktop\\ThirdYear\\DataRetrieval\\corpusTest\\StopWords", n);
        p.Parser();

        double endTime   = System.nanoTime();
        double totalTime = (endTime - startTime) / 1000000000 ;
        System.out.println(totalTime + " sec");

    }
}
