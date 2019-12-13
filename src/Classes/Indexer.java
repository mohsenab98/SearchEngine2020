package Classes;

import sun.misc.Cleaner;

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

import static jdk.nashorn.internal.objects.NativeArray.map;

public class Indexer {
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
    private String pathPosting;
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
    private final int MAX_POST_SIZE = 200000;

    /**
     * help us merge the posting
     */
    private Map<String, ArrayList<String>> postingMap;
    /**
     * help us to save id/maxtf/counter for each doc in the posting
     */
    private Map<Integer, ArrayList<String>> mapDocID;

    private static int docIDCounter = 0;


    public Indexer(String pathCorpus,String pathPosting, boolean isStem) {
        this.mapTermPosting = new LinkedHashMap<>();
        this.pathCorpus = pathCorpus;
        this.pathPosting = pathPosting;
        this.mapSortedTerms = new TreeMap<>();
        this.isStem = isStem;
        this.mapDocID = new LinkedHashMap<>();
        postingFilesCreate(pathPosting);
    }

    /**
     * get the term and its related information from the parser
     * send the info for the posting files
     * @param termDoc , map that contain the term and list{DOCID , pos1 , pos2...}
     */
    public void addTermToIndexer(Map<String, ArrayList<String>>termDoc, ArrayList<String> docInfo){
        int i = 0;
        if(mapSortedTerms.size() > MAX_POST_SIZE){
            reset();
        }
        // mapDocID.put(docIDCounter, new ArrayList<>(docInfo)); // add doc info to mapDoc

        for (String key : termDoc.keySet()) {
            if(this.mapSortedTerms.containsKey(key)){
                //chain the new list of term to the original one
                ArrayList<String> listOfInfo = termDoc.get(key);
                String info = docIDCounter + ":" + listOfInfo.get(0) + ";";
                ArrayList<String> originalList = mapSortedTerms.get(key);
                String originalInfo = originalList.get(0) + info;
//                originalList.clear();
                originalList = new ArrayList<>();
                originalList.add(0, originalInfo);
                mapSortedTerms.put(key, originalList);
            }
            else{
                //Add new term and it list of info to the Sorted map
                ArrayList<String> listOfInfo = termDoc.get(key);
                String info = docIDCounter + ":" + listOfInfo.get(0) + ";";
//                listOfInfo.clear();
                listOfInfo = new ArrayList<>();
                listOfInfo.add(0, info);
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
        String merged;
        String textToPostFile = "";
        String posting = "";
        SortedMap<String, ArrayList<String>> text = new TreeMap<>();

        if(mapSortedTerms.firstKey().equals("")){
            mapSortedTerms.remove(mapSortedTerms.firstKey());
        }

        posting = readFile("Numbers");
        while(isNumeric(mapSortedTerms.firstKey())){
            // textToPostFile += mapSortedTerms.firstKey() + "|" + textForPosting(mapSortedTerms.get(mapSortedTerms.firstKey())) + "\n";
            text.put(mapSortedTerms.firstKey(), mapSortedTerms.get(mapSortedTerms.firstKey()));
            mapSortedTerms.remove(mapSortedTerms.firstKey());
        }
        merged = merge(posting, text);
        usingBufferedWritter(merged, "Numbers");
//        text.clear();
        text = new TreeMap<>();

        /*
        posting = readFile("Names");
        Set<String> keys = new LinkedHashSet<>(mapSortedTerms.keySet());
        for(String key : keys) {
            if(Character.isLowerCase(key.charAt(0))){
                break;
            }
            if(!key.contains(" ")){
                continue;
            }
            //textToPostFile += mapSortedTerms.firstKey() + "|" + textForPosting(mapSortedTerms.get(mapSortedTerms.firstKey())) + "\n";
            text.put(key, mapSortedTerms.get(key));
            mapSortedTerms.remove(key);
        }
        keys.clear();
        merged = merge(posting, text);
        usingBufferedWritter(merged, "Names");
        text.clear();
*/
        for (int i = 'A'; i <= 'Z'; i++){
            posting = "";
            posting = readFile(String.valueOf(Character.toLowerCase((char)i)));
            while(mapSortedTerms.firstKey().charAt(0) == (char)i){
                //  textToPostFile += mapSortedTerms.firstKey() + "|" + textForPosting(mapSortedTerms.get(mapSortedTerms.firstKey())) + "\n";
                text.put(mapSortedTerms.firstKey(), mapSortedTerms.get(mapSortedTerms.firstKey()));
                mapSortedTerms.remove(mapSortedTerms.firstKey());
            }
            merged = merge(posting, text);
            usingBufferedWritter(merged, String.valueOf((char)i));
//            text.clear();
            text = new TreeMap<>();
        }

        for (int i = 'a'; i <= 'z'; i++){
            posting = "";
            posting = readFile(String.valueOf((char)i));
            while(!mapSortedTerms.isEmpty() && mapSortedTerms.firstKey().charAt(0) == (char)i){
                //  textToPostFile += mapSortedTerms.firstKey() + "|" + textForPosting(mapSortedTerms.get(mapSortedTerms.firstKey())) + "\n";
                text.put(mapSortedTerms.firstKey(), mapSortedTerms.get(mapSortedTerms.firstKey()));
                mapSortedTerms.remove(mapSortedTerms.firstKey());
            }
            merged = merge(posting, text);
            usingBufferedWritter(merged, String.valueOf((char)i));
//            text.clear();
            text = new TreeMap<>();
        }

        /*
        for (String key : mapSortedTerms.keySet()) {
            textToPostFile = textToPostFile + key + "|" + textForPosting(mapSortedTerms.get(key)) + "\n";
            mapTermPosting.put(key, String.valueOf(tempPostCounter));
        }
        mapSortedTerms.clear();
        */


        //tempPostCounter++;

    }

    private String mapToFormatString(Map<String, ArrayList<String>> text){
        String textToPostFile = "";
        for (String key : text.keySet()) {
            textToPostFile += key + "|" + text.get(key).get(0) + "\n";
            //mapTermPosting.put(key, String.valueOf(tempPostCounter));
        }
        return textToPostFile;
    }

    public String merge(String posting, SortedMap<String, ArrayList<String>> text){
        if(posting.isEmpty()){
            return mapToFormatString(text);
        }

        String[] arrPosting = posting.split("\n");
        for(String term : arrPosting){
            String[] termAndInfo = term.split("\\|");
            if(termAndInfo.length < 2){
                continue;
            }
            if(termAndInfo[0].contains(" ")){
                continue;
            }

            ArrayList<String> info = new ArrayList<>();
            if(text.containsKey(termAndInfo[0])){
                info = text.get(termAndInfo[0]);
                try {
                    info.add( termAndInfo[1]);
                }
                catch(Exception e){
                    System.out.println(termAndInfo[0] + " " + mapDocID.size());
                    e.printStackTrace();
                }
            }
            else{
                try {
//                    info.add(termAndInfo[1].substring(0, termAndInfo[1].indexOf(":")));
//                    info.add(termAndInfo[1].substring(termAndInfo[1].indexOf(":") + 1));
                    info.add(termAndInfo[1]);
                }
                catch(Exception e){
                    System.out.println(termAndInfo[0] + " " + mapDocID.size());
                    e.printStackTrace();
                }

            }
            text.put(termAndInfo[0], info);
        }

        return mapToFormatString(text);
    }

    private String readFile(String fileName){
        CharBuffer charBuffer = null;
        String stemFolder = "";
        if(isStem){
            stemFolder = "stem";
        }else {
            stemFolder = "noStem";
        }
        Path path = Paths.get(this.pathPosting + "/" +stemFolder + "/" + fileName);

        try{
            FileChannel fileChannel = (FileChannel) Files.newByteChannel(path, EnumSet.of(StandardOpenOption.READ));

            MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());

            if (mappedByteBuffer != null) {
                charBuffer = Charset.forName("ASCII").decode(mappedByteBuffer);
            }

            Cleaner cleaner = ((sun.nio.ch.DirectBuffer) mappedByteBuffer).cleaner();
            if (cleaner != null) {
                cleaner.clean();
            }

            PrintWriter writer = new PrintWriter(new File(path.toString()));
            writer.print("");
            writer.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return charBuffer.toString();
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
            stemFolder = "noStem";
        }

        String fileUrl = this.pathPosting + "/" + stemFolder + "/" + filename;
        //File file =  new File(fileUrl);
        BufferedWriter writer = null;
        try {
            // file.createNewFile();
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


    }
    public static boolean isNumeric(String str) {
        return Character.isDigit(str.charAt(0));
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
            File fileStem = new File(path+"/stem/" + "Numbers");
            File fileNames = new File(path+"/stem/" + "Names");
            File fileStemNoStem = new File(path+"/noStem/" + "Numbers");
            File fileNamesNoStem = new File(path+"/noStem/" + "Names");
            try {
                fileStem.createNewFile();
                fileNames.createNewFile();
                fileNamesNoStem.createNewFile();
                fileStemNoStem.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (int i = 'a'; i <= 'z'; i++){
                try {
                    fileStem = new File(path+"/stem/"+(char)i);
                    fileStem.createNewFile();
                    File fileNoStem = new File(path+"/noStem/"+(char)i);
                    fileNoStem.createNewFile();
//                    System.out.println("Empty File Created:- " + file.length());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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
//        mapTermPosting.clear();
        mapTermPosting = new LinkedHashMap<>();
    }

    public void saveDocInfo() {
        String text = "";
        for (Integer key : mapDocID.keySet()) {
            ArrayList<String> listDocInfo = mapDocID.get(key); /// DOCID | DOCNAME ? Term : maxtf , counter
            text = text + key + "|" + listDocInfo.get(0) + "?" + listDocInfo.get(1) + ":" +listDocInfo.get(2) + "," + listDocInfo.get(3) + ";" + "\n";
        }
        usingBufferedWritter(text,"Doc");
//        mapTermPosting.clear();
        mapTermPosting = new LinkedHashMap<>();
    }
}
