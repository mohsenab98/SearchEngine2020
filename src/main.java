import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Stream;

public class main {
    //private ExecutorService threadPool = Executors.newCachedThreadPool();

    public static void main(String[] args){
        double startTime = System.nanoTime();

        boolean stem = true;
        String pathCorpus = "D:\\corpus";
        //String pathCorpus = "D:\\corpusTestD";
        String pathStopWords = "C:\\Users\\EvgeniyU\\Desktop\\ThirdYear\\DataRetrieval\\corpusTest\\StopWords";

        ArrayList<Path> listPath = new ArrayList<>();
        int docsLimiter = 200;

        try {
            Stream<Path> paths = Files.walk(Paths.get(pathCorpus));
            Collections.addAll(listPath, paths.filter(Files::isRegularFile).toArray(Path[]::new));
           // Path[] filesPaths = paths.filter(Files::isRegularFile).toArray(Path[]::new);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        while (!listPath.isEmpty()){
            if(listPath.size() < docsLimiter){
                docsLimiter = listPath.size();
            }

            Path[] filePaths = new Path[docsLimiter];

            for(int i = 0; i < docsLimiter && !listPath.isEmpty(); i++){
                filePaths[i] = listPath.get(0);
                listPath.remove(0);
            }
            ReadFile rd = new ReadFile();
            rd.filesSeparator(filePaths);
            Indexer n = new Indexer();
            Parse p = new Parse(pathStopWords, stem, n);
            p.Parser(rd.getListAllDocs());
        }



/*
        ReadFile rd = new ReadFile();
        rd.filesSeparator("D:\\corpusTestD");
*/
    /*
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
        */



        double endTime   = System.nanoTime();
        double totalTime = (endTime - startTime) / 1000000000 ;
        System.out.println(totalTime + " sec");

    }
}
