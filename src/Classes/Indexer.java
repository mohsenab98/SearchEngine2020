package Classes;

import sun.misc.Cleaner;
import java.io.*;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Stream;

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
    private static int termCounter = 0;

    /**
     * will determinate the size of the posting (~50000 terms in posting file)
     */
    private final int MAX_POST_SIZE = 10000;

    /**
     * help us merge the posting
     */
    private Map<String, ArrayList<String>> postingMap;
    /**
     * help us to save id/maxtf/counter for each doc in the posting
     */
    private Map<Integer, ArrayList<String>> mapDocID;

    private static int docIDCounter = 0;
    private static int postIdCounter = 0;


    public Indexer(String pathCorpus,String pathPosting, boolean isStem) {
        this.mapTermPosting = new LinkedHashMap<>();
        this.pathCorpus = pathCorpus;
        this.pathPosting = pathPosting;
        this.mapSortedTerms = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.isStem = isStem;
        this.mapDocID = new LinkedHashMap<>();
        postingFilesCreate(pathPosting);
    }

    /**
     * get the term and its related information from the parser
     * send the info for the posting files
     * @param termDoc , map that contain the term and list{DOCID , pos1 , pos2...}
     */
    public void addTermToIndexer(Map<String, String>termDoc, ArrayList<String> docInfo){
        int i = 0;
        if(mapSortedTerms.size() > MAX_POST_SIZE){
            reset();
        }
         mapDocID.put(docIDCounter, new ArrayList<>(docInfo)); // add doc info to mapDoc

        for (String key : termDoc.keySet()) {
            if(this.mapSortedTerms.containsKey(key)){
                //chain the new list of term to the original one
//                ArrayList<String> listOfInfo = termDoc.get(key);
//                String info = new StringBuilder().append(docIDCounter).append(":").append(listOfInfo.get(0)).append(";").toString();
                String info = new StringBuilder().append(docIDCounter).append(":").append(termDoc.get(key)).append(";").toString();
                ArrayList<String> originalList = mapSortedTerms.get(key);
                // duplicates of docs 0:1;0:2 agent
                if(!originalList.get(0).substring(0, originalList.get(0).indexOf(":")).equals(String.valueOf(docIDCounter))) {
                    String originalInfo = originalList.get(0) + info;
//                originalList.clear();
                    originalList = new ArrayList<>();
                    originalList.add(0, originalInfo);
                }
                else {
                    originalList.clear();
                    originalList.add(0, info);
                }
                if(Character.isLowerCase(key.charAt(0)) && mapSortedTerms.containsKey(key.toUpperCase())) {
                    mapSortedTerms.remove(key.toUpperCase());
                    mapSortedTerms.put(key.toLowerCase(), originalList);
                }
                else {
                    mapSortedTerms.put(key, originalList);
                }
            }
            else{
                //Add new term and it list of info to the Sorted map
                ArrayList<String> listOfInfo = new ArrayList<>();
                String info = new StringBuilder().append(docIDCounter).append(":").append(termDoc.get(key)).append(";").toString();
//                listOfInfo.clear();
//                listOfInfo = new ArrayList<>();
                listOfInfo.add(0, info);
                mapSortedTerms.put(key, listOfInfo);
            }
        }
        docIDCounter++;
    }

    public static void setDocIDCounter(int docIDCounter) {
        Indexer.docIDCounter = docIDCounter;
    }

    public static int getDocIDCounter() {
        return docIDCounter;
    }

    public static void setTermCounter(int termCounter) {
        Indexer.termCounter = termCounter;
    }

    /**
     * delete the mapSorted data
     * docCounter = 0
     * tempPostCounter++
     * update the mapPosting
     * write the data to the posting file
     */
    public void reset(){
        //mapSortedTerms  ---- save & clear
        SortedMap<String, String> text = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for(String term : mapSortedTerms.keySet()){
            ArrayList<String> s = mapSortedTerms.get(term);
            String s1 = s.get(0);
            text.put(term, s1);
        }
        usingBufferedWritter(mapToFormatString(text), String.valueOf(postIdCounter));
        postIdCounter++;
        mapSortedTerms = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
//         * DocInfo Index:
//     *      0 - doc name
//                *      1 - term max tf
//     *      2 - count max tf
//     *      3 - count uniq terms in doc
        String textDoc = "";
        String posting ="";
        String merged = "";
        posting = readFile("Doc");
        for (Integer key : mapDocID.keySet()) {
            ArrayList<String> info = mapDocID.get(key);
            textDoc = textDoc + key + "|" + info.get(0) + ":" + info.get(1) + "?" +info.get(2) + "," + info.get(3)+ "\n";

        }
        mapDocID = new LinkedHashMap<>();
        merged = posting + textDoc;
        usingBufferedWritter(merged, "Doc");
        //tempPostCounter++;

    }

    private String mapToFormatString(Map<String, String> text){
        StringBuilder textToPostFile = new StringBuilder();
        for (String key : text.keySet()) {
            textToPostFile.append(key).append("|").append(text.get(key)).append("\n");
        }
        return textToPostFile.toString();
    }

    public static int getTermCounter() {
        return termCounter;
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

        String fileUrl = new StringBuilder().append(this.pathPosting).append("/").append(stemFolder).append("/").append(filename).toString();
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(
                    new FileWriter(fileUrl, true)  //Set true for append mode
            );
            writer.write(text);
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
            File fileDocStem = new File(path+"/Stem/" + "Doc");
            File fileDocNoStem = new File(path+"/noStem/" + "Doc");
            try {
                fileDocStem.createNewFile();
                fileDocNoStem.createNewFile();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Save the term map at the end of the indexing because we need it in the second part when we will search
     */
    public void saveDictionary() {
        String text = "";
        for (String key : mapTermPosting.keySet()) {
            text = new StringBuilder().append(text).append(key).append(":").append(mapTermPosting.get(key)).append(";").append("\n").toString();
        }
        usingBufferedWritter(text,"Dictionary");
        mapTermPosting = new LinkedHashMap<>();
    }

    public void saveDocInfo() {
        StringBuilder text = new StringBuilder();
        for (Integer key : mapDocID.keySet()) {
            ArrayList<String> listDocInfo = mapDocID.get(key); /// DOCID | DOCNAME ? Term : maxtf , counter(unique terms per doc)
            text.append(key).append("|").append(listDocInfo.get(0)).append("?").append(listDocInfo.get(1)).append(":").append(listDocInfo.get(2)).append(",").append(listDocInfo.get(3)).append(";").append("\n");
        }
        usingBufferedWritter(text.toString(),"Doc");
        mapTermPosting = new LinkedHashMap<>();
    }

    public void merge() {
        // postIdCounter --- number of posting
        String stemFolder = "";
        if(isStem){
            stemFolder = "stem";
        }else {
            stemFolder = "noStem";
        }
        StringBuilder fileUrl1 = new StringBuilder().append(this.pathPosting).append("/").append(stemFolder).append("/");
        StringBuilder fileUrl2 = new StringBuilder().append(this.pathPosting).append("/").append(stemFolder).append("/");
        int numberOfposting = new File(this.pathCorpus + "/" + stemFolder).listFiles().length;
        for(int i = 0; numberOfposting > 2 ; i++){
//            if(i >= postIdCounter){
//                i = 0;
//            }
            //TODO: counters !
            fileUrl1 = fileUrl1.append((i));
            fileUrl2 = fileUrl2.append((i+1));
            SortedMap<String, String> text = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            termCounter = 0;
            Path path1 = Paths.get(String.valueOf(fileUrl1));
            Path path2 = Paths.get(String.valueOf(fileUrl2));
            try
            {
                Stream<String> lines1 = Files.lines( path1, StandardCharsets.US_ASCII );
                Stream<String> lines2 = Files.lines( path2, StandardCharsets.US_ASCII );

                for( String line : (Iterable<String>) lines1::iterator ){
                    String term = line.substring(0, line.indexOf("|"));
                    String info = line.substring(line.indexOf("|") + 1);
                    text.put(term, info);
                }

                for( String line : (Iterable<String>) lines2::iterator )
                {
                    String term = line.substring(0, line.indexOf("|"));
                    String info = line.substring(line.indexOf("|") + 1);

                    if(!text.containsKey(term)){
                        text.put(term, info);
                    }
                    else{
                        String preInfo = text.get(term);
                        text.put(term, preInfo + info);
                    }
                }

            } catch (IOException ioe){
                ioe.printStackTrace();
            }
            File f1 = new File(String.valueOf(fileUrl1));
            File f2 = new File(String.valueOf(fileUrl2));
            f1.delete();
            f2.delete();
            usingBufferedWritter(mapToFormatString(text), String.valueOf(postIdCounter));
            postIdCounter++;
            text = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            i++; // two files each time

        }


    }

    public void finalPostingMerge() {
        //TODO : need to concatenate values if there is duplicatess
        String merged;
        String posting = "";
        SortedMap<String, String> text = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        termCounter = 0;
        posting = readFile("Numbers");
        String [] splitPosting = posting.split("\n");
        for (int i = 0; i < splitPosting.length; i++){
            String [] splitPostingLine = splitPosting[i].split("\\|");
            text.put(splitPostingLine[0], splitPostingLine[1]);
        }
//        merged = merge(posting, text);
        usingBufferedWritter(mapToFormatString(text), "Numbers");
        text = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

//        posting = readFile("Names");
//        mapfirstKey = mapSortedTerms.firstKey();
//        Set<String> keys = new LinkedHashSet<>(mapSortedTerms.keySet());
//        for(String key : keys) {
//            if(!key.contains(" ") || !Character.isUpperCase(key.charAt(0))){
//                continue;
//            }
//            text.put(key, mapSortedTerms.get(key).get(0));
//            mapSortedTerms.remove(key);
//        }
//        merged = merge(posting, text);
//        usingBufferedWritter(mapToFormatString(text), "Names");
//        text = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 'a'; i <= 'z'; i++){
            posting = "";
            posting = readFile(String.valueOf((char)i));
            splitPosting = posting.split("\n");
            for (int j = 0; j < splitPosting.length; j++){
                String [] splitPostingLine = splitPosting[j].split("\\|");
                text.put(splitPostingLine[0], splitPostingLine[1]);
            }
            usingBufferedWritter(mapToFormatString(text), String.valueOf((char)i));
//            text.clear();
            text = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        }
    }
}
