import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class main {
    public static void main(String[] args){
        double startTime = System.nanoTime();

    /////// ReadFile tests ///////
        int k = new File("C:\\Users\\mohse\\Desktop\\corpusTest").list().length;
        int maxRun = k/6;

        ReadFile rd = new ReadFile();
        rd.filesSeparator("C:\\Users\\mohse\\Desktop\\corpusTest");
        Indexer n = new Indexer();
        Parse p = new Parse(rd.getListAllDocs(), "C:\\Users\\mohse\\Desktop\\corpusTest\\StopWords", n);
        p.Parser();

        double endTime   = System.nanoTime();
        double totalTime = (endTime - startTime) / 1000000000 ;
        System.out.println(totalTime + " sec");

    }
}
