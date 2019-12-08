import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class main {
    //private ExecutorService threadPool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        double startTime = System.nanoTime();
//        Indexer n = new Indexer(pathCorpus, stem, 100);

        boolean stem = true;
        String pathCorpus = "C:\\Users\\mohse\\Desktop\\corpusTest2";
        //String pathCorpus = "D:\\corpusTestD";
        String pathStopWords = "C:\\Users\\mohse\\Desktop\\corpusTest1\\StopWords";

        ReadFile rd = new ReadFile();
//        Indexer n = new Indexer();
        rd.filesSeparator(pathCorpus);
        Parse p = new Parse(pathStopWords, stem);
        Indexer n = new Indexer(pathCorpus, stem, 10);
        // int counter = 1;
        while (!rd.getListAllDocs().isEmpty()) {
            String fullText = "";
            String docName = "";
            Pattern patternText = Pattern.compile("<DOCNO>\\s*([^<]+)\\s*</DOCNO>.+?<TEXT>(.+?)</TEXT>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            Matcher matcherText = patternText.matcher(new String(rd.getListAllDocs().get(0)));
            while (matcherText.find()) {
                docName = matcherText.group(1);
                fullText = matcherText.group(2);
            }

            p.Parser(fullText, docName);

            n.addTermToIndexer(p.getMapTerms());


            rd.getListAllDocs().remove(0);
            p.cleanTerms();

            //   System.out.println(counter);
            //   counter ++;
        }






/*
        ReadFile rd = new ReadFile();
        rd.filesSeparator("D:\\corpusTestD");
*/
    /*
    /////// ReadFile tests ///////
       // int k = new File("D:\\corpus").list().length;
       // int maxRun = k/6;
        Indexer n = new Indexer();
        Parse p = new Parse(rd.getListAllDocs(), "C:\\Users\\EvgeniyU\\Desktop\\ThirdYear\\DataRetrieval\\corpusTest\\StopWords", n);
        p.Parser();
        */


        double endTime = System.nanoTime();
        double totalTime = (endTime - startTime) / 1000000000;
        System.out.println((totalTime)/60+ "minutes");
    }
}