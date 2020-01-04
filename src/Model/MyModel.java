package Model;

import Classes.Indexer;
import Classes.Parse;
import Classes.ReadFile;
import javafx.scene.control.TextField;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Mohsen Abdalla & Evgeny Umansky. December 2019.
 */

public class MyModel extends Observable implements IModel {
    /**
     * map to load the posting of the dictionary
     */
    public static Map<String,String> mapDictionary ;

    public MyModel() {
        this.mapDictionary = new LinkedHashMap<>();
    }

    /**
     * opens the file(Dictionary) and load it to the mapDictionary
     * @param file
     */
    @Override
    public void loadDictionary(File file) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader( new FileReader( file ));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String line = null;
        while(true){
            try {
                if (!((line = reader.readLine()) != null)) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            String[] arr = line.split( "\\|" );
            mapDictionary.put( arr[0], arr[1] );
        }
    }


    /**
     * delete all dataSet(Posting files) we have worked with
     * @param posting_text
     */
    @Override
    public void resetProcess(TextField posting_text) {

        try {
            Files.walk(Paths.get(posting_text.getText() + "/stem"))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

            Files.walk(Paths.get(posting_text.getText() + "/nostem"))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }catch (Exception o){
//            o.printStackTrace();
        }
    }


    private double timeForIndexing;
    private int docCounter;
    private int termNumbers;

    /**
     * Indexing process, include reading files, parsing, indexing...
     * @param selected
     * @param corpus_text
     * @param posting_text
     * @return
     */
    @Override
    public boolean startIndexing(boolean selected, TextField corpus_text, TextField posting_text) {
        double startTime = System.nanoTime();
        docCounter = 0;
        boolean stem = selected;
        String pathCorpus = corpus_text.getText();
        String pathStopWords;
        // search for stop words file
        File f = new File(pathCorpus);
        File[] matchingFiles = f.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().contains("stop") && name.toLowerCase().contains("words");
            }
        });
        if(matchingFiles != null && matchingFiles.length > 0){
            pathStopWords = matchingFiles[0].getPath();
        }
        else {
            pathStopWords = corpus_text.getText() + "/05 stop_words.txt";
        }
        String pathPosting = posting_text.getText(); /// need to use !!!!!!!!!!!!!!!!

        ReadFile readFile = new ReadFile(pathCorpus);
        readFile.filesSeparator();
        Parse parse = new Parse(pathStopWords, stem);
        Indexer indexer = new Indexer(pathPosting, stem);
        indexer.setDocIDCounter(0);
        while (!readFile.getListAllDocs().isEmpty()) {
            String fullText = "";
            String docName = "";
            Pattern patternText = Pattern.compile("<DOCNO>\\s*([^<]+)\\s*</DOCNO>.+?<TEXT>(.+?)</TEXT>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            Matcher matcherText = patternText.matcher(new String(readFile.getListAllDocs().get(0)));
            while (matcherText.find()) {
                docName = matcherText.group(1);
                fullText = matcherText.group(2);
            }

            parse.Parser(fullText, docName);
            indexer.addTermToIndexer(parse.getMapTerms(), parse.getDocInfo());

            readFile.getListAllDocs().remove(0);
            parse.cleanParse();

        }

        indexer.reset(); // check if there is stell terms in the sorted map
        int fileCounterName = indexer.merge(); //merge the temp sorted files 2 big files
        indexer.finalMerge(fileCounterName); // merge 2 final posting files to A-Z posting files
        termNumbers = indexer.getDictionarySize();
        double endTime = System.nanoTime();
        double totalTime = (endTime - startTime) / 1000000000;
        System.out.println((totalTime)/60+ " minutes. For Read/Parse/Indexing");
        System.out.println(termNumbers);
        System.out.println(indexer.getDocIDCounter());
        timeForIndexing = totalTime / 60;
        docCounter = indexer.getDocIDCounter();
        return true;
    }

    /**
     * @return time for indexing
     */
    public double getTimeForIndexing() {
        return timeForIndexing;
    }

    /**
     * @return number of docs indexed
     */
    public int getDocCounter() {
        return docCounter;
    }

    @Override
    public int getNumberOfTerms() {
        return termNumbers;
    }
}
