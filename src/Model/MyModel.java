package Model;

import Classes.Indexer;
import Classes.Parse;
import Classes.ReadFile;
import javafx.scene.control.TextField;

import java.awt.*;
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
    private Map<String,String> mapDictionary ;

    public MyModel() {
        this.mapDictionary = new LinkedHashMap<>();
    }

    /**
     * opens the file we choose and load it to the mapDictionary
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
     * opens the file that contains the dictionary in new window
     * @param posting_text
     * @param isSelected
     */
    @Override
    public void showDictionary(TextField posting_text, boolean isSelected) {
        String stemFolder = "";
        if(isSelected){
            stemFolder = "stem";
        }else{
            stemFolder = "noStem";
        }
        File file = new File (posting_text.getText() + "\\" + stemFolder + "\\Dictionary");
        Desktop desktop = Desktop.getDesktop();
        try {
            desktop.open(file);
        } catch (IOException e) {
            // if path isnt right or there no stem/nostem directories dont do anything
        }
    }

    /**
     * delete all dataSet we have worked with
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
            // if path isnt right or there no stem/nostem directories dont do anything
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
        String pathStopWords = corpus_text.getText()+"/StopWords";
        String pathPosting = posting_text.getText(); /// need to use !!!!!!!!!!!!!!!!

        ReadFile rd = new ReadFile(pathCorpus);
        rd.filesSeparator();
        Parse p = new Parse(pathStopWords, stem);
        Indexer n = new Indexer(pathCorpus, pathPosting, stem);
        n.setDocIDCounter(0);
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
            n.addTermToIndexer(p.getMapTerms(), p.getDocInfo());

            rd.getListAllDocs().remove(0);
            p.cleanParse();

        }

        n.reset(); // check if there is stell terms in the sorted map
        int i = n.merge(); //merge the temp sorted files into A-Z sorted files
        n.finalMerge(i);
        termNumbers = n.getDictionarySize();
        double endTime = System.nanoTime();
        double totalTime = (endTime - startTime) / 1000000000;
        System.out.println((totalTime)/60+ " minutes. For Read/Parse/Indexing");
        System.out.println(termNumbers);
        System.out.println(n.getDocIDCounter());
        timeForIndexing = totalTime / 60;
        docCounter = n.getDocIDCounter();
        return true;////////
    }

    /**
     *
     * @return time for indexing
     */
    public double getTimeForIndexing() {
        return timeForIndexing;
    }

    /**
     *
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
