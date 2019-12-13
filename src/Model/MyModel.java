package Model;

import Classes.Indexer;
import Classes.Parse;
import Classes.ReadFile;
import javafx.scene.control.TextField;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Observable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Mohsen Abdalla & Evgeny Umansky. December 2019.
 */

public class MyModel extends Observable implements IModel {
    @Override
    public void loadDictionary(File file) {

    }

    @Override
    public void showDictionary() {

    }

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


        /// Clear Memory
    }


    private double timeForIndexing;
    private int docCounter;
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
            docCounter++;
        }



        n.reset(); // check if there is stell terms in the sorted map
        //n.merge(); //merge the temp sorted files into A-Z sorted files
//        n.saveDictionary();
        n.saveDocInfo();
//        n.mergeFiles();

        double endTime = System.nanoTime();
        double totalTime = (endTime - startTime) / 1000000000;
        System.out.println((totalTime)/60+ " minutes. For Read/Parse/Indexing");
        timeForIndexing = totalTime / 60;
        return true;
    }

    public double getTimeForIndexing() {
        return timeForIndexing;
    }

    public int getDocCounter() {
        return docCounter;
    }
}
