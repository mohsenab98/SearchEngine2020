package Classes;

import sun.misc.Cleaner;

import java.awt.*;
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
import java.util.List;
import java.util.stream.Collectors;
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

    //TODO : delete path if we dont use it
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
    private final int MAX_POST_SIZE = 15000;

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
                String info = new StringBuilder().append(docIDCounter).append(":").append(termDoc.get(key)).append(";").toString();
                ArrayList<String> originalList = mapSortedTerms.get(key);
                // duplicates of docs 0:1;0:2 agent
                if(!originalList.get(0).substring(0, originalList.get(0).indexOf(":")).equals(String.valueOf(docIDCounter))) {
                    String originalInfo = originalList.get(0) + info;
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
        //mapSortedTerms  ---- save to temp posting & clear
        SortedMap<String, String> text = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for(String term : mapSortedTerms.keySet()){
            ArrayList<String> s = mapSortedTerms.get(term);
            String s1 = s.get(0);
            text.put(term, s1);
        }
        usingBufferedWritter(mapToFormatString(text), String.valueOf(postIdCounter));
        postIdCounter++;
        mapSortedTerms = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        // save to DOC file
        saveDocInfo();

    }
    // TODO : deal with java heap Exception
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


    /**
     * Creating two folders(with/without stemming) and in each folder we creat the posting files of the index
     * @param path where we want to save the index files
     */
    private void postingFilesCreate(String path){
        boolean folder1 = new File(path+"/stem").mkdir();
        boolean folder2 = new File(path+"/noStem").mkdir();
        if(folder1 && folder2){
            File fileDocStem = new File(path+"/stem/" + "Doc");
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
        mapDocID = new LinkedHashMap<>();
    }

    public int merge() {
        String stemFolder;
        if(isStem){
            stemFolder = "stem";
        }else {
            stemFolder = "noStem";
        }
        String filePath1 = this.pathPosting + "/" + stemFolder + "/";
        String filePath2 = this.pathPosting+ "/" + stemFolder + "/";
        String fileUrl1 = "";
        String fileUrl2 = "";
        int i;
        int numberOfposting = new File(this.pathPosting + "/" + stemFolder).listFiles().length;
        for( i = 0; numberOfposting - 1 > 2 ; i++){
            fileUrl1 = filePath1 + "/" + i;
            fileUrl2 = filePath2 + "/" + (i+1);
            SortedMap<String, String> text = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            termCounter = 0;
            Path path1 = Paths.get(fileUrl1);
            Path path2 = Paths.get(fileUrl2);
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
                        text.put(term, info + preInfo);
                    }
                }

            } catch (IOException ioe){
                ioe.printStackTrace();
            }
            File f1 = new File(fileUrl1);
            File f2 = new File(fileUrl2);
            f1.delete();
            f2.delete();
            // TODO : deal with java heap Exception
            usingBufferedWritter(mapToFormatString(text), String.valueOf(postIdCounter));
            postIdCounter++;
            text = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            i++; // two files each time
            numberOfposting = new File(this.pathPosting + "/" + stemFolder).listFiles().length;
        }

        return i;
    }

    public void finalMerge(int intFileName) {
        // TODO : fill the function
        createAZPostingFiles();
        String stemFolder;
        if(isStem){
            stemFolder = "stem";
        }else {
            stemFolder = "noStem";
        }

        SortedMap<String, String> text = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        termCounter = 0;
        Path path1 = Paths.get(this.pathPosting + "/" + stemFolder + "/" + (intFileName));
        Path path2 = Paths.get(this.pathPosting + "/" + stemFolder + "/" + (intFileName + 1));
        try
        {
            Stream<String> lines1 = Files.lines( path1, StandardCharsets.US_ASCII );
            Stream<String> lines2 = Files.lines( path2, StandardCharsets.US_ASCII );
            //////////////////////////////////////////////////

            List<String> listLines1 = lines1
                    .filter(s -> s.charAt(0) == '$' || Character.isDigit(s.charAt(0)))
                    .collect(Collectors.toList());
            List<String> listLines2 = lines2
                    .filter(s -> s.charAt(0) == '$' || Character.isDigit(s.charAt(0)))
                    .collect(Collectors.toList());
            for(String line : listLines1) {
                String term = line.substring(0, line.indexOf("|"));
                String info = line.substring(line.indexOf("|") + 1);
                text.put(term, info);
            }
            for(String line : listLines2){
                String term = line.substring(0, line.indexOf("|"));
                String info = line.substring(line.indexOf("|") + 1);

                if(!text.containsKey(term)){
                    text.put(term, info);
                }
                else{
                    String preInfo = text.get(term);
                    text.put(term, info + preInfo);
                }
            }
            usingBufferedWritter(mapToFormatString(text), "Numbers");
//////////////////////////////////////////////////////////////////////////////////////
            for(int i = 'a'; i <= 'z'; i++) {
                Stream<String> lines11 = Files.lines( path1, StandardCharsets.US_ASCII );
                Stream<String> lines22 = Files.lines( path2, StandardCharsets.US_ASCII );
                text = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                int ch = i;
                List<String> listLines11 = lines11
                        .filter(s -> s.toLowerCase().charAt(0) == (char) ch)
                        .collect(Collectors.toList());
                List<String> listLines22 = lines22
                        .filter(s -> s.toLowerCase().charAt(0) == (char) ch)
                        .collect(Collectors.toList());

                for(String line : listLines11) {
                    String term = line.substring(0, line.indexOf("|"));
                    String info = line.substring(line.indexOf("|") + 1);
                    text.put(term, info);
                }
                for(String line : listLines22){
                    String term = line.substring(0, line.indexOf("|"));
                    String info = line.substring(line.indexOf("|") + 1);

                    if(!text.containsKey(term)){
                        text.put(term, info);
                    }
                    else{
                        String preInfo = text.get(term);
                        text.put(term, info + preInfo);
                    }
                }
                usingBufferedWritter(mapToFormatString(text), String.valueOf((char)i));

            }

            File f1 = new File(this.pathPosting + "/" + stemFolder + "/" + (intFileName));
            File f2 = new File(this.pathPosting + "/" + stemFolder + "/" + (intFileName + 1));
            f1.delete();
            f2.delete();

        } catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    private void createAZPostingFiles() {

    }
}
