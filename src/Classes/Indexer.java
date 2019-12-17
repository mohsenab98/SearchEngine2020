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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Indexer {
    /**
     * will Save the terms of the index and will save a pointer to the match posting file
     */
    private Map<String, String> mapDictionary;
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
    private final int MAX_POST_SIZE = 10000;

    /**
     * help us to save id/maxtf/counter for each doc in the posting
     */
    private Map<Integer, ArrayList<String>> mapDocID;

    private static int docIDCounter = 0;
    private static int postIdCounter = 0;

    public Indexer(String pathCorpus,String pathPosting, boolean isStem) {
        sizeDictionary = 0;
        docIDCounter = 0;
        postIdCounter = 0;
        this.mapDictionary = new LinkedHashMap<>();
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

        termDoc.remove("");
        for (String key : termDoc.keySet()) {
            try {
                if (this.mapSortedTerms.containsKey(key)) {
                    //chain the new list of term to the original one
                    String info = new StringBuilder().append(docIDCounter).append(":").append(termDoc.get(key)).append(";").toString();
                    ArrayList<String> originalList = mapSortedTerms.get(key);
                    // duplicates of docs 0:1;0:2 agent
                    if (!originalList.get(0).substring(0, originalList.get(0).indexOf(":")).equals(String.valueOf(docIDCounter))) {
                        String originalInfo = originalList.get(0) + info;
                        originalList = new ArrayList<>();
                        originalList.add(0, originalInfo);
                    } else {
                        originalList.clear();
                        originalList.add(0, info);
                    }
                    if (!key.contains(" ") && Character.isLowerCase(key.charAt(0)) && mapSortedTerms.containsKey(key.toUpperCase())) {
                        mapSortedTerms.remove(key.toUpperCase());
                        mapSortedTerms.put(key.toLowerCase(), originalList);
                    } else {
                        mapSortedTerms.put(key, originalList);
                    }
                } else {
                    //Add new term and it list of info to the Sorted map
                    ArrayList<String> listOfInfo = new ArrayList<>();
                    String info = new StringBuilder().append(docIDCounter).append(":").append(termDoc.get(key)).append(";").toString();
                    listOfInfo.add(0, info);
                    mapSortedTerms.put(key, listOfInfo);
                }
            }
            catch (Exception e){
                e.printStackTrace();
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
        usingBufferedWritter(mapToFormatString(text, this.pathPosting + "/" + postIdCounter), String.valueOf(postIdCounter));
        postIdCounter++;
        mapSortedTerms = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        // save to DOC file
        saveDocInfo();

    }
    // TODO : deal with java heap Exception
    private String mapToFormatString(Map<String, String> text, String path){
        StringBuilder textToPostFile = new StringBuilder();
        for (String key : text.keySet()) {
            if(textToPostFile.length() >= 200000000) {
                usingBufferedWritter(textToPostFile.toString(), path);
                textToPostFile.setLength(0);
            }
            textToPostFile.append(key).append("|").append(text.get(key)).append("\n");
        }
        return textToPostFile.toString();
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
    private void usingBufferedWritter(String text, String filename)
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

    private void saveDocInfo() {
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
        SortedMap<String, String> terms = new TreeMap<>();
        String filePath1 = this.pathPosting + "/" + stemFolder + "/";
        String filePath2 = this.pathPosting+ "/" + stemFolder + "/";
        String fileUrl1;
        String fileUrl2;
        int i;
        int numberOfposting = new File(this.pathPosting + "/" + stemFolder).listFiles().length;
        for( i = 0; numberOfposting - 1 > 2 ; i++){
            fileUrl1 = filePath1 + "/" + i;
            fileUrl2 = filePath2 + "/" + (i+1);
            SortedMap<String, String> rawTerms = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            termCounter = 0;
            Path path1 = Paths.get(fileUrl1);
            Path path2 = Paths.get(fileUrl2);
            try
            {
//                Stream<String> lines1 = Files.lines( path1, StandardCharsets.US_ASCII );
//                Stream<String> lines2 = Files.lines( path2, StandardCharsets.US_ASCII );
//
//                for( String line : (Iterable<String>) lines1::iterator ){
//                    String term = line.substring(0, line.indexOf("|"));
//                    String info = line.substring(line.indexOf("|") + 1);
//                    rawTerms.put(term, info);
//                }
//
//                for( String line : (Iterable<String>) lines2::iterator )
//                {
//                    terms = mergeTermsToMap(rawTerms, line);
//
//                }
                Stream<String> linesFile1Numbers = Files.lines( path1, StandardCharsets.US_ASCII );
                Stream<String> linesFile2Numbers = Files.lines( path2, StandardCharsets.US_ASCII );
                //////////////////////////////////////////////////
                // merge numbers
                List<String> listLinesFile1Numbers = linesFile1Numbers
                        .filter(s -> s.charAt(0) == '$' || Character.isDigit(s.charAt(0)))
                        .collect(Collectors.toList());
                List<String> listLinesFile2Numbers = linesFile2Numbers
                        .filter(s -> s.charAt(0) == '$' || Character.isDigit(s.charAt(0)))
                        .collect(Collectors.toList());
                SortedMap<String, String> termsNumbers = finaleMergeTermsFromTwoFilesToMap(rawTerms, listLinesFile1Numbers, listLinesFile2Numbers);
                usingBufferedWritter(mapToFormatString(termsNumbers, this.pathPosting + "/" + postIdCounter), String.valueOf(postIdCounter));
                /////////////////////////////////////////////////////////////////////
                for(int j = 'a'; j <= 'z'; j++) {
                    Stream<String> linesFile1AZ = Files.lines(path1, StandardCharsets.US_ASCII);
                    Stream<String> linesFile2AZ = Files.lines(path2, StandardCharsets.US_ASCII);
                    rawTerms = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                    int ch = j;
                    List<String> listLinesFile1AZ = linesFile1AZ
                            .filter(s -> s.toLowerCase().charAt(0) == (char) ch)
                            .collect(Collectors.toList());
                    List<String> listLinesFile2AZ = linesFile2AZ
                            .filter(s -> s.toLowerCase().charAt(0) == (char) ch)
                            .collect(Collectors.toList());
                    SortedMap<String, String> termsAB = finaleMergeTermsFromTwoFilesToMap(rawTerms, listLinesFile1AZ, listLinesFile2AZ);
                    usingBufferedWritter(mapToFormatString(termsAB, this.pathPosting + "/" + postIdCounter), String.valueOf(postIdCounter));

                }

                } catch (IOException ioe){
                ioe.printStackTrace();
            }
            File f1 = new File(fileUrl1);
            File f2 = new File(fileUrl2);
            f1.delete();
            f2.delete();
//            usingBufferedWritter(mapToFormatString(terms), String.valueOf(postIdCounter));

            postIdCounter++;
            terms = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            i++; // two files each time
            numberOfposting = new File(this.pathPosting + "/" + stemFolder).listFiles().length;
        }

        return i;
    }

    public void finalMerge(int intFileName) {
        String stemFolder;
        if(isStem){
            stemFolder = "stem";
        }else {
            stemFolder = "noStem";
        }

        SortedMap<String, String> rawTerms = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        termCounter = 0;
        Path path1 = Paths.get(this.pathPosting + "/" + stemFolder + "/" + (intFileName));
        Path path2 = Paths.get(this.pathPosting + "/" + stemFolder + "/" + (intFileName + 1));
        try
        {
            Stream<String> linesFile1Numbers = Files.lines( path1, StandardCharsets.US_ASCII );
            Stream<String> linesFile2Numbers = Files.lines( path2, StandardCharsets.US_ASCII );
            //////////////////////////////////////////////////
            // merge numbers
            List<String> listLinesFile1Numbers = linesFile1Numbers
                    .filter(s -> s.charAt(0) == '$' || Character.isDigit(s.charAt(0)))
                    .collect(Collectors.toList());
            List<String> listLinesFile2Numbers = linesFile2Numbers
                    .filter(s -> s.charAt(0) == '$' || Character.isDigit(s.charAt(0)))
                    .collect(Collectors.toList());
            SortedMap<String, String> termsNumbers = finaleMergeTermsFromTwoFilesToMap(rawTerms, listLinesFile1Numbers, listLinesFile2Numbers);
            String toFileNumbers = mapToFormatString(termsNumbers, "Numbers");
            usingBufferedWritter(toFileNumbers, "Numbers");
            termToDictionary(termsNumbers);
//////////////////////////////////////////////////////////////////////////////////////
            // merge a-z lower and upper cases
            for(int i = 'a'; i <= 'z'; i++) {
                Stream<String> linesFile1AZ = Files.lines( path1, StandardCharsets.US_ASCII );
                Stream<String> linesFile2AZ = Files.lines( path2, StandardCharsets.US_ASCII );
                rawTerms = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                int ch = i;
                List<String> listLinesFile1AZ = linesFile1AZ
                        .filter(s -> s.toLowerCase().charAt(0) == (char) ch)
                        .collect(Collectors.toList());
                List<String> listLinesFile2AZ = linesFile2AZ
                        .filter(s -> s.toLowerCase().charAt(0) == (char) ch)
                        .collect(Collectors.toList());
                SortedMap<String, String> termsAB = finaleMergeTermsFromTwoFilesToMap(rawTerms, listLinesFile1AZ, listLinesFile2AZ);

                // Names: check if a name in 2 or more docs
                // the iterator avoids: Exception in thread "JavaFX Application Thread" java.util.ConcurrentModificationException
                for(Iterator<Map.Entry<String, String>> it = termsAB.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<String, String> entry = it.next();
                    if(entry.getKey().contains(" ")){
                        int counter = 0;
                        Pattern dfPattern = Pattern.compile("(;)");
                        Matcher dfMatcher = dfPattern.matcher(entry.getValue());
                        while (dfMatcher.find()){
                            counter++;
                        }
                        if(counter < 2){
                            it.remove();
                        }
                    }
                }
                String toFileAB = mapToFormatString(termsAB, String.valueOf((char)i));
                usingBufferedWritter(toFileAB, String.valueOf((char)i));
                termToDictionary(termsAB);
            }

            File f1 = new File(this.pathPosting + "/" + stemFolder + "/" + (intFileName));
            File f2 = new File(this.pathPosting + "/" + stemFolder + "/" + (intFileName + 1));
            f1.delete();
            f2.delete();

        } catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    private SortedMap<String, String> mergeTermsToMap(SortedMap<String, String> rawTerms, String line) {
        String term = line.substring(0, line.indexOf("|"));
        String info = line.substring(line.indexOf("|") + 1);

        if(!rawTerms.containsKey(term)){
            rawTerms.put(term, info);
        }
        else{
            String preInfo = rawTerms.get(term);
            rawTerms.put(term, info + preInfo);
        }
        return rawTerms;
    }

    private SortedMap<String, String> finaleMergeTermsFromTwoFilesToMap(SortedMap<String, String> rawTerms, List<String> file1, List<String> file2){
        SortedMap<String, String> terms = new TreeMap<>();
        for(String line : file1) {
            String term = line.substring(0, line.indexOf("|"));
            String info = line.substring(line.indexOf("|") + 1);
            rawTerms.put(term, info);
        }
        for(String line : file2){
            terms = mergeTermsToMap(rawTerms, line);
        }
        return terms;
    }

    private void termToDictionary(Map<String, String> termsToDict) {
        Set<String> terms = termsToDict.keySet();
        int lineCounter = 1;
        for(String term : terms){
            int df = 0;
            String infoText = termsToDict.get(term);
            Pattern dfPattern = Pattern.compile("(;)");
            Matcher dfMatcher = dfPattern.matcher(infoText);
            while (dfMatcher.find()){
                df++;
            }

            String infoDic = "" + df + ";" + lineCounter;
            lineCounter ++;

            this.mapDictionary.put(term, infoDic);
            sizeDictionary++;
        }
        usingBufferedWritter(mapToFormatString(this.mapDictionary, this.pathPosting + "/" + "Dictionary"), "Dictionary");
        this.mapDictionary = new LinkedHashMap<>();
    }

    private static int sizeDictionary = 0;

    public int getDictionarySize(){
        return sizeDictionary;
    }
}
