import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class main {
    //private ExecutorService threadPool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        double startTime = System.nanoTime();

        boolean stem = true;
        //String pathCorpus = "D:\\corpus";
        String pathCorpus = "D:\\corpusTestD";
        //String pathCorpus = "C:\\Users\\EvgeniyU\\Desktop\\ThirdYear\\DataRetrieval\\corpusTest";
        String pathStopWords = "C:\\Users\\EvgeniyU\\Desktop\\ThirdYear\\DataRetrieval\\corpusTest\\StopWords";

        ReadFile rd = new ReadFile();
        rd.filesSeparator(pathCorpus);
        Parse p = new Parse(pathStopWords, stem);
        Indexer n = new Indexer(pathCorpus, stem);
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
            p.getDocInfo();

            //n.addTermToIndexer(p.getMapTerms());

            rd.getListAllDocs().remove(0);
            p.cleanTerms();

        }



      //  n.reset(); // check if there is stell terms in the sorted map
       // n.merge(); //merge the temp sorted files into A-Z sorted files
       // n.saveDictionary();

        double endTime = System.nanoTime();
        double totalTime = (endTime - startTime) / 1000000000;
        System.out.println(totalTime + " sec");
        System.out.println((totalTime)/60 + " minutes");
    }
}