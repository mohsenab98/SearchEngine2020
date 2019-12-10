import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Indexer {
    private ExecutorService threadPool = Executors.newCachedThreadPool();


    /**
     * will Save the terms of the index and will save a pointer to the match posting file
     */
    private Map<String, String> mapTermPosting;
    /**
     * will save the term and its info for each doc in a sorted map (A-Z)
     */
    private SortedMap<String, ArrayList<String>> mapSortedTerms ;
    private String pathCorpus;
    /**
     * isStem variable that we git from the user ( the parse send it to here)
     */
    private boolean isStem;

    /**
     * name of the temp file
     */
    private static int tempPostCounter = 0;

    /**
     * will determinate the size of the posting (~3000 terms in posting file)
     */
    private final int MAX_POST_SIZE = 3000;

    public Indexer(String pathCorpus, boolean isStem) {
        this.mapTermPosting = new LinkedHashMap<>();
        this.pathCorpus = pathCorpus;
        this.mapSortedTerms = new TreeMap<>();
        this.isStem = isStem;
        postingFilesCreate(pathCorpus);
    }

    /**
     * get the term and its related information from the parser
     * send the info for the posting files
     * @param termDoc , map that contain the term and list{DOCID , pos1 , pos2...}
     */
    public void addTermToIndexer(Map<String, ArrayList<String>>termDoc){

        if (mapSortedTerms.size() > MAX_POST_SIZE) {
            reset();
        }

        for (String key : termDoc.keySet()) {
            if(this.mapSortedTerms.containsKey(key)){
                //chain the new list of term to the original one
                ArrayList<String> listOfInfo = termDoc.get(key);
                ArrayList<String> originalList = mapSortedTerms.get(key);
                originalList.addAll(listOfInfo); // chain new list to the original (boolean)
                mapSortedTerms.put(key, originalList);

            }else{
                //Add new term and it list of info to the Sorted map
                ArrayList<String> listOfInfo = termDoc.get(key);
                mapSortedTerms.put(key, listOfInfo);
            }
        }


    }

    /**
     * delete the mapSorted data
     * docCounter = 0
     * tempPostCounter++
     * update the mapPosting
     * write the data to the posting file
     */
    public void reset(){
        String textToPostFile = "";

        for (String key : mapSortedTerms.keySet()) {
            textToPostFile = textToPostFile +"<" + key + "|" + textForPosting(mapSortedTerms.get(key)) + ">" +"\n";
            mapTermPosting.put(key, String.valueOf(tempPostCounter));
        }
        mapSortedTerms.clear();

        usingBufferedWritter(textToPostFile, String.valueOf(tempPostCounter));


        tempPostCounter++;

    }

    /**
     * func that turn the info of the term into string so we can write it to the posting file
     * @param listOfInfo
     * @return
     */
    private String textForPosting(ArrayList<String> listOfInfo){
        String textToAdd = "";
        textToAdd = textToAdd + listOfInfo.get(0) + ":"; // DOCID
        textToAdd = textToAdd + listOfInfo.get(1) + "?"; // tfi
        String temp = "";
        for (int i = 2; i < listOfInfo.size(); i++){
            temp = listOfInfo.get(i);
            //!isNumeric(temp)
            if((temp.charAt(0) >= 'a' && temp.charAt(0) <= 'z') || (temp.charAt(0) >= 'A' && temp.charAt(0) <= 'Z')){
                textToAdd = ";" + textToAdd + temp + ":"; // DOCID 2 +
                    i++;
                    textToAdd = textToAdd + listOfInfo.get(i) + "?"; // tfi

            }else{
                textToAdd = textToAdd + temp + ",";
            }
        }
        if(textToAdd.endsWith(","))
        {
            textToAdd = textToAdd.substring(0,textToAdd.length() - 1);
            textToAdd = textToAdd + ";";
        }else{
            textToAdd = textToAdd + ";";
        }
        return textToAdd;
    }



    /**
     * append text to file
     * this function code was take from "https://howtodoinjava.com/java/io/java-append-to-file/"
     * @param text
     * @throws IOException
     */
    public void usingBufferedWritter(String text, String filename)
    {
        String stemFolder = "";
        if(isStem){
            stemFolder = "stem";
        }else {
            stemFolder = "nostem";
        }

        String fileUrl = this.pathCorpus + "/" + stemFolder + "/" + filename;

        try {

            File f = new File(fileUrl);
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));
            ZipEntry e = new ZipEntry(filename);
            out.putNextEntry(e);

            out.write(text.getBytes(), 0, text.length());
            out.closeEntry();

            out.close();




/*
            File file =  new File (fileUrl);
            BufferedWriter writer = null;
            file.createNewFile();
            writer = new BufferedWriter(
                    new FileWriter(fileUrl, true)  //Set true for append mode
            );
            writer.write(text);
//            writer.newLine();   //Add new line
            writer.close();

            */
            } catch (IOException e) {
                System.out.println(filename);
                e.printStackTrace();
            }



    }
    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(String.valueOf(str.charAt(0)));
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }


    /**
     * Get the number of the line that the new term is going to be in the posting
     * @param path
     * @return
     * @throws IOException
     */
    private int lineOfNewTerm(String path) throws IOException {
        BufferedReader r = new BufferedReader(new FileReader(path));
        int i = 1;
        while (r.readLine() != null){
            i++;
        }
        return i;
    }

    /**
     * Creating two folders(with/without stemming) and in each folder we creat the posting files of the index
     * @param path
     */
    private void postingFilesCreate(String path){
        boolean folder1 = new File(path+"/stem").mkdir();
        boolean folder2 = new File(path+"/noStem").mkdir();
//        if(folder1 && folder2){
//            File fileStem = new File(path+"/stem/"+"Numbers");
//            try {
//                fileStem.createNewFile();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            for (int i = 'a'; i < 'z'; i++){
//                try {
//                     fileStem = new File(path+"/stem/"+(char)i);
//                    fileStem.createNewFile();
//                    File fileNoStem = new File(path+"/noStem/"+(char)i);
//                    fileNoStem.createNewFile();
////                    System.out.println("Empty File Created:- " + file.length());
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }

    }


    public void merge() {
        String stemFolder = "";
        if(isStem){
            stemFolder = "stem";
        }else {
            stemFolder = "nostem";
        }
        String pathPosting = pathCorpus + "/" + stemFolder;



    }

    /**
     * Save the term map at the end of the indexing because we need it in the second part when we will search
     */
    public void saveDictionary() {
        String text = "";
        for (String key : mapTermPosting.keySet()) {
            text = text + key + ":" +mapTermPosting.get(key)+";"+"\n";
        }
        usingBufferedWritter(text,"Dictionary");
    }

    public void shutThreads(){
        threadPool.shutdown();
    }
}
