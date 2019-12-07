import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class main {
    public static void main(String[] args){
        double startTime = System.nanoTime();

        boolean stem = true;
     //   String pathCorpus = "D:\\corpus";
        String pathCorpus = "D:\\corpusTestD";
        String pathStopWords = "C:\\Users\\EvgeniyU\\Desktop\\ThirdYear\\DataRetrieval\\corpusTest\\StopWords";

        ReadFile rd = new ReadFile();
        Indexer n = new Indexer();
        rd.filesSeparator(pathCorpus);
        Parse p = new Parse(pathStopWords, stem);

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

            p.getMapTerms();

            rd.getListAllDocs().remove(0);
            p.cleanTerms();

            //   System.out.println(counter);
            //   counter ++;
        }



        double endTime   = System.nanoTime();
        double totalTime = (endTime - startTime) / 1000000000 ;
        System.out.println(totalTime + " sec");

    }
}
