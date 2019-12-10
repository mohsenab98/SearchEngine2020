import java.io.*;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static jdk.nashorn.internal.objects.NativeArray.map;

public class Indexer {
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    /**
     * will Save the terms of the index and will save a pointer to the match posting file
     */
    private Map<String, String> mapTermPosting;
    /**
     * will save the term and its info for each doc in a sorted map (A-Z) list[0] = DocID (according to the mapDocID)
     * list[1] = tf . list[2] = delimiter
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

    /**
     * help us merge the posting
     */
    private Map<String, ArrayList<String>> postingMap;
    /**
     * help us to save id/maxtf/counter for each doc in the posting
     */
    private Map<Integer, ArrayList<String>> mapDocID;

    private static int docIDCounter = 0;


    public Indexer(String pathCorpus, boolean isStem) {
        this.mapTermPosting = new ConcurrentHashMap<>();
        this.pathCorpus = pathCorpus;
        this.mapSortedTerms = new TreeMap<>();
        this.isStem = isStem;
        this.mapDocID = new LinkedHashMap<>();
        postingFilesCreate(pathCorpus);
    }

    /**
     * get the term and its related information from the parser
     * send the info for the posting files
     * @param termDoc , map that contain the term and list{DOCID , pos1 , pos2...}
     */
    public void addTermToIndexer(Map<String, ArrayList<String>>termDoc, ArrayList<String> docInfo){
        if(mapSortedTerms.size() > MAX_POST_SIZE){
            reset();
        }
        mapDocID.put(docIDCounter, new ArrayList<>(docInfo)); // add doc info to mapDoc

        for (String key : termDoc.keySet()) {
            if(this.mapSortedTerms.containsKey(key)){
                //chain the new list of term to the original one
                ArrayList<String> listOfInfo = termDoc.get(key);
                listOfInfo.add(0, String.valueOf(docIDCounter));
                listOfInfo.add(";"); //delimiter
                ArrayList<String> originalList = mapSortedTerms.get(key);
                originalList.addAll(listOfInfo); // chain new list to the original (boolean)
                mapSortedTerms.put(key, originalList);


            }else{
                //Add new term and it list of info to the Sorted map
                ArrayList<String> listOfInfo = termDoc.get(key);
                listOfInfo.add(0, String.valueOf(docIDCounter));
                mapSortedTerms.put(key, listOfInfo);

            }
        }
        docIDCounter++;


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
            textToPostFile = textToPostFile + key + "|" + textForPosting(mapSortedTerms.get(key)) + "\n";
            mapTermPosting.put(key, String.valueOf(tempPostCounter));
        }
        mapSortedTerms.clear();
        usingBufferedWritter(textToPostFile, String.valueOf(tempPostCounter));

        tempPostCounter++;

    }

    /**
     * func that turn the info of the term into string so we can write it to the posting file
     * @param listOfInfo is the info of each term
     * @return text for the posting file
     */
    private String textForPosting(ArrayList<String> listOfInfo){
        StringBuilder textToAdd = new StringBuilder();
        textToAdd.append(listOfInfo.get(0)).append(":"); // DOCID
        textToAdd.append(listOfInfo.get(1)).append("?"); // tfi
        String temp;
        for (int i = 2; i < listOfInfo.size(); i++){
            temp = listOfInfo.get(i);
            //!isNumeric(temp)
            if(!(temp.charAt(0) == ';')){ // new Doc info
                textToAdd = new StringBuilder( textToAdd + temp + ":"); // DOCID 2 +
                    i++;
                    textToAdd.append(listOfInfo.get(i)).append("?"); // tfi

            }else{
//                textToAdd.append(temp).append(",");
            }
        }
//        if(textToAdd.toString().endsWith(","))
//        {
//            textToAdd = new StringBuilder(textToAdd.substring(0, textToAdd.length() - 1));
//            textToAdd.append(";");
//        }else{
//            textToAdd.append(";");
//        }
        return textToAdd.toString();
    }



    /**
     * append text to file
     * this function code was take from "https://howtodoinjava.com/java/io/java-append-to-file/"
     * @param text for the posting file
     * @throws IOException if the file or directory wasn't found
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
        /*
        File file =  new File(fileUrl);
        BufferedWriter writer = null;
        try {
            file.createNewFile();
            writer = new BufferedWriter(
                    new FileWriter(fileUrl, true)  //Set true for append mode
            );
            writer.write(text);
//            writer.newLine();   //Add new line
            writer.close();
        } catch (IOException e) {
            System.out.println(filename);
            e.printStackTrace();
        }
        */
        MappedByteBuffer mappedByteBuffer = null;
        CharBuffer charBuffer = CharBuffer
                .wrap(text);
//        Path pathToWrite = getFileURIFromResources("fileToWriteTo.txt");
        try(FileChannel fileChannel = FileChannel.open(
                Paths.get(fileUrl),
                StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            long size = fileChannel.size();
            mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, size);

        } catch (IOException e) {
            if (mappedByteBuffer != null) {
                mappedByteBuffer.put(
                        Charset.forName("ASCII").encode(charBuffer));
            }
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
     * @param path of the file
     * @return the line of the new term
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
     * @param path where we want to save the index files
     */
    private void postingFilesCreate(String path){
        boolean folder1 = new File(path+"/stem").mkdir();
        boolean folder2 = new File(path+"/noStem").mkdir();
        if(folder1 && folder2){
            File fileStem = new File(path+"/stem/"+"Numbers");
            try {
                fileStem.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        }

    }

    /**
     * merge the posting files into sorting a-z/A-Z/Number posting files
     */
    public void merge(){
        while (tempPostCounter > 1){
            for (int i = 0; i < tempPostCounter - 1; i = i+2){
                mergePosting(String.valueOf(i),String.valueOf(i+1));
            }
            tempPostCounter = tempPostCounter/2;
        }
    }

    /**
     * the main function of sorting two posting into one sorted file
     */
    private void mergePosting(String file1, String file2) {
        saveDictionary(); // saves the dictionary and delete the content
        String stemFolder = "";
        if(isStem){
            stemFolder = "stem";
        }else {
            stemFolder = "nostem";
        }
        String pathPosting = pathCorpus + "/" + stemFolder;
        // go throw all temp posting files and start the merging
//        for (int i = 0; i < tempPostCounter ; i = i++){
            try {
                // PrintWriter object for file3.txt
//                PrintWriter pw = new PrintWriter(pathPosting+"/"+"new "+i);

                // BufferedReader object for file1.txt
//                File file1 = new File(pathPosting+"/"+i);
//                File file2 = new File(pathPosting+"/"+(i+1));

//                BufferedReader br1 = new BufferedReader(new FileReader(pathPosting+"/"+file1));
//                BufferedReader br2 = new BufferedReader(new FileReader(pathPosting+"/"+file2));
                ArrayList<String> list = new ArrayList<>();
                Scanner sc1 = new Scanner(new File(pathPosting+"/"+file1));
                Scanner sc2 = new Scanner(new File(pathPosting+"/"+file2));
                sc1.useDelimiter("<.+\\|");
                sc2.useDelimiter("<.+\\|"); // need to clean first and last

                while (sc1 != null ){
                    postingMap.put(sc1.next(), list);
                }
                while (sc2 != null ){
                    postingMap.put(sc2.next(), list);
                }

//                String line1 = br1.readLine();
//                String line2 = br2.readLine();
//                char c = 0;
//                if(!(line1.charAt(0) >=0 && line1.charAt(0) <=9)){
//                    c = line1.toLowerCase().charAt(0);
//                }
//                PrintWriter pw = new PrintWriter(pathPosting+"/"+ c);
                // loop to copy lines of
                // file1.txt and file2.txt
                // to  file3.txt alternatively
//                while (line1 != null )
//                {
//                    if(line1 != null)
//                    {
//                        pw.println(line1);
//                        line1 = br1.readLine();
//                    }

//                    if(line2 != null)
//                    {
//                        pw.println(line2);
//                        line2 = br2.readLine();
//                    }
//                }
//
//                pw.flush();
//
//                // closing resources
//                br1.close();
////                br2.close();
//                pw.close();
//                file1.delete();
//                file2.delete();
            }catch (Exception o){

            }

//        }


    }

    /**
     * Save the term map at the end of the indexing because we need it in the second part when we will search
     */
    public void saveDictionary() {
        String text = "";
        for (String key : mapTermPosting.keySet()) {
            text = text + key + ":" +mapTermPosting.get(key)+";"+"\n";
        }
        threadPool.shutdown();
        usingBufferedWritter(text,"Dictionary");
        mapTermPosting.clear();
    }

    public void saveDocInfo() {
        String text = "";
        for (Integer key : mapDocID.keySet()) {
            ArrayList<String> listDocInfo = mapDocID.get(key); /// DOCID | DOCNAME ? Term : maxtf , counter
            text = text + key + "|" + listDocInfo.get(0) + "?" + listDocInfo.get(1) + ":" +listDocInfo.get(2) + "," + listDocInfo.get(3) + ";" + "\n";
        }
        usingBufferedWritter(text,"Doc");
        mapDocID.clear();
    }
}
