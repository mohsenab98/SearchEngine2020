package Classes;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    /**
     * save titles of docs to string builder: [docName|title]
     */
    private StringBuilder titles;

    private String pathPosting;
    /**
     * isStem variable that we git from the user ( the parse send it to here)
     */
    private boolean isStem;

    /**
     * will determinate the size of the posting (~10000 terms in posting file)
     */
    private final int MAX_POST_SIZE = 10000;
    /**
     * will write the string to a posting file if it big (~200MB)
     */
    private final int MAX_SIZE_STRING = 200000000;

    /**
     * help us to save id/maxtf/counter for each doc in the posting
     */
    private Map<Integer, ArrayList<String>> mapDocID;

    private static int docIDCounter = 0; // id of docs
    private static int postIdCounter = 0; // name to temp posting file
    private static int sizeDictionary = 0; // size of dictionary
    private static int sumdl = 0;
    private boolean isUpperLowerAction = false; // if was upper/lower action

    public Indexer(String pathPosting, boolean isStem) {
        sizeDictionary = 0;
        docIDCounter = 0;
        postIdCounter = 0;
        this.mapDictionary = new LinkedHashMap<>();
        this.pathPosting = pathPosting;
        this.mapSortedTerms = new TreeMap<>(this::compare);
        this.titles = new StringBuilder();
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
        if(mapSortedTerms.size() > MAX_POST_SIZE){
            reset();
        }
        mapDocID.put(docIDCounter, new ArrayList<>(docInfo)); // add doc info to mapDoc

        termDoc.remove("");
        for (String key : termDoc.keySet()) {
            if (this.mapSortedTerms.containsKey(key)) {

                //chain the new list of term to the original one
                String info = docIDCounter + ":" + termDoc.get(key) + ";";
                ArrayList<String> originalList = mapSortedTerms.get(key);
                if (!originalList.get(0).substring(0, originalList.get(0).indexOf(":")).equals(String.valueOf(docIDCounter))) {
                    String originalInfo = originalList.get(0) + info;
                    originalList = new ArrayList<>();
                    originalList.add(0, originalInfo);
                } else {
                    originalList.clear();
                    originalList.add(0, info);
                }
                mapSortedTerms.put(key, originalList);
            }
            else {
                //Add new term and it list of info to the Sorted map
                ArrayList<String> listOfInfo = new ArrayList<>();
                String info = new StringBuilder().append(docIDCounter).append(":").append(termDoc.get(key)).append(";").toString();
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
        //mapSortedTerms  ---- save to temp posting & clear
        SortedMap<String, String> terms = new TreeMap<>(this::compare);
        for(String term : mapSortedTerms.keySet()){
            ArrayList<String> listInfo = mapSortedTerms.get(term);
            String info= listInfo.get(0);
            terms.put(term, info);
        }

        writePosting(terms);
        mapSortedTerms = new TreeMap<>(this::compare);

        // save entities to Entities file
        saveEntitiesInfo();
        // save to DOC file
        saveDocInfo();
        // save titles to Titles file
        saveTitle();
    }

    private void saveTitle() {
        usingBufferedWritter(this.titles.toString(), "Titles");
        this.titles = new StringBuilder();
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
     * get string of terms and write it on the disk
     */
    private void writePosting(Map<String, String> terms){
        String toFile = mapToFormatString(terms, String.valueOf(postIdCounter));
        usingBufferedWritter(toFile, String.valueOf(postIdCounter));
        postIdCounter++;
    }

    /**
     * Append terms to string for writing on hard disk.
     * If string is very large, drop it straight on the disk
     */
    private String mapToFormatString(Map<String, String> text, String path){
        StringBuilder textToPostFile = new StringBuilder();
        for (String key : text.keySet()) {
            if(textToPostFile.length() >= MAX_SIZE_STRING) {
                usingBufferedWritter(textToPostFile.toString(), path);
                textToPostFile.setLength(0);
            }
            textToPostFile.append(key).append("|").append(text.get(key)).append("\n");
        }
        return textToPostFile.toString();
    }

    /**
     * append text to file
     * this function code was take from "https://howtodoinjava.com/java/io/java-append-to-file/"
     * @param text for the posting file
     * @throws IOException if the file or directory wasn't found
     */
    private void usingBufferedWritter(String text, String filename) {
        String stemFolder;
        if(isStem){
            stemFolder = "stem";
        }else {
            stemFolder = "noStem";
        }

        String fileUrl = this.pathPosting + "/" + stemFolder + "/" + filename;
        BufferedWriter writer;
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
     * merge posting files until 2 big files
     * @return
     */
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
        int fileCounterName;

        // for each temp posting file in the directory
        int numberOfposting = new File(this.pathPosting + "/" + stemFolder).listFiles().length;
        for( fileCounterName = 0; numberOfposting - 1 > 4 ; fileCounterName++){
            fileUrl1 = filePath1  + fileCounterName;
            fileUrl2 = filePath2 + (fileCounterName + 1);
            SortedMap<String, String> rawTerms = new TreeMap<>((o1, o2) -> compare(o1, o2));
            Path path1 = Paths.get(fileUrl1);
            Path path2 = Paths.get(fileUrl2);
            try
            {
                // read by line files for merging
                Stream<String> lines1 = Files.lines( path1, StandardCharsets.US_ASCII );
                Stream<String> lines2 = Files.lines( path2, StandardCharsets.US_ASCII );

                // charge the first file to map
                for( String line : (Iterable<String>) lines1::iterator ){
                    String term = line.substring(0, line.indexOf("|"));
                    String info = line.substring(line.indexOf("|") + 1);
                    rawTerms.put(term, info);
                }

                // merge the first file with the second by line with help-function
                for( String line : (Iterable<String>) lines2::iterator )
                {
                    terms = mergeTermsToMap(rawTerms, line, isUpperLowerAction);
                }

                lines1.close();
                lines2.close();

            } catch (IOException ioe){
                ioe.printStackTrace();
            }
            // delete merged files
            File f1 = new File(fileUrl1);
            File f2 = new File(fileUrl2);
            f1.delete();
            f2.delete();
            numberOfposting = numberOfposting - 2;

            // write merged terms on hard disk
            writePosting(terms);
            numberOfposting++;
            terms = new TreeMap<>(this::compare);
            fileCounterName++; // two files each time
//            numberOfposting = new File(this.pathPosting + "/" + stemFolder).listFiles().length;
        }

        return fileCounterName;
    }

    /**
     * merge to finale big temp posting files to A-Z posting files
     * @param intFileName - names of the 2 files
     */
    public void finalMerge(int intFileName) {
        String stemFolder;
        if(isStem){
            stemFolder = "stem";
        }else {
            stemFolder = "noStem";
        }


        usingBufferedWritter(docIDCounter + "\n" + sumdl/docIDCounter, "BM25Info");
        SortedMap<String, String> rawTerms = new TreeMap<>((o1, o2) -> compare(o1, o2));
        Path path1 = Paths.get(this.pathPosting + "/" + stemFolder + "/" + (intFileName));
        Path path2 = Paths.get(this.pathPosting + "/" + stemFolder + "/" + (intFileName + 1));
        if(!(new File(this.pathPosting + "/" + stemFolder + "/" + (intFileName + 1)).exists())){
            return;
        }

        try
        {
            //////////////////////  merge numbers  ////////////////////////////
            Stream<String> linesFile1Numbers = Files.lines( path1, StandardCharsets.US_ASCII );
            Stream<String> linesFile2Numbers = Files.lines( path2, StandardCharsets.US_ASCII );

            // reading terms from hard disk to lists
            List<String> listLinesFile1Numbers = linesFile1Numbers
                    .filter(s -> s.charAt(0) == '$' || Character.isDigit(s.charAt(0)))
                    .collect(Collectors.toList());
            List<String> listLinesFile2Numbers = linesFile2Numbers
                    .filter(s -> s.charAt(0) == '$' || Character.isDigit(s.charAt(0)))
                    .collect(Collectors.toList());
            // help-function to merge terms after reading them from hard disk
            SortedMap<String, String> termsNumbers = finaleMergeTermsFromTwoFilesToMap(rawTerms, listLinesFile1Numbers, listLinesFile2Numbers);
            String toFileNumbers = mapToFormatString(termsNumbers, "Numbers");
            linesFile1Numbers.close();
            linesFile2Numbers.close();
            usingBufferedWritter(toFileNumbers, "Numbers");
            termToDictionary(termsNumbers); // add terms to the dictionary and write them to the hard disk
            //////////////////////  merge a-z, lower and upper cases law  ////////////////////////////
            for(int i = 'a'; i <= 'z'; i++) {
                Stream<String> linesFile1AZ = Files.lines( path1, StandardCharsets.US_ASCII );
                Stream<String> linesFile2AZ = Files.lines( path2, StandardCharsets.US_ASCII );
                rawTerms = new TreeMap<>((o1, o2) -> compare(o1, o2));
                int ch = i; // name of file a-z
                // reading terms from hard disk to lists
                List<String> listLinesFile1AZ = linesFile1AZ
                        .filter(s -> s.toLowerCase().charAt(0) == (char) ch)
                        .collect(Collectors.toList());
                List<String> listLinesFile2AZ = linesFile2AZ
                        .filter(s -> s.toLowerCase().charAt(0) == (char) ch)
                        .collect(Collectors.toList());
                // help-function to merge terms after reading them from hard disk
                SortedMap<String, String> termsAB = finaleMergeTermsFromTwoFilesToMap(rawTerms, listLinesFile1AZ, listLinesFile2AZ);

                ////////////////////// Names: check if a name in 2 or more docs ////////////////////////////
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

                linesFile1AZ.close();
                linesFile2AZ.close();
                String toFileAB = mapToFormatString(termsAB, String.valueOf((char)i));
                usingBufferedWritter(toFileAB, String.valueOf((char)i)); // write a-z on the disk
                termToDictionary(termsAB); // write terms to dictionary
            }

            File f1 = new File(this.pathPosting + "/" + stemFolder + "/" + (intFileName));
            File f2 = new File(this.pathPosting + "/" + stemFolder + "/" + (intFileName + 1));
            // delete big temp posting files
            f1.delete();
            f2.delete();

        }
        catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    //////////////// Functions help to merge ////////////////
    /**
     * merge term and its info into map
     * @param rawTerms - terms to posting
     * @param line - term and info
     * @param finalM - is
     * @return
     */
    private SortedMap<String, String> mergeTermsToMap(SortedMap<String, String> rawTerms, String line, boolean finalM) {
        String term = line.substring(0, line.indexOf("|"));
        String info = line.substring(line.indexOf("|") + 1);

        if(!rawTerms.containsKey(term)){
            rawTerms.put(term, info);
        }
        // the term does not need upper/lower case actions
        else if(!finalM){
            String preInfo = rawTerms.get(term);
            rawTerms.put(term, info + preInfo);
        }
        // smart adding term after upper/lower case action
        else{
            String preInfo = rawTerms.get(term);

            Set<String> check = new HashSet<>();
            Pattern p = Pattern.compile("(\\d+):\\d+;");
            Matcher mTerm = p.matcher(term);
            while (mTerm.find()){
                check.add(mTerm.group(1));
            }

            Matcher mInfo = p.matcher(info);
            while (mInfo.find()){
                if(preInfo.contains(mInfo.group(1))){
                    return rawTerms;
                }
            }
            rawTerms.put(term, info + preInfo);
        }
        return rawTerms;
    }

    /**
     * check upper/lower case and merge terms to posting a-z files
     * @param rawTerms
     * @param file1
     * @param file2
     * @return
     */
    private SortedMap<String, String> finaleMergeTermsFromTwoFilesToMap(SortedMap<String, String> rawTerms, List<String> file1, List<String> file2){
        SortedMap<String, String> terms = new TreeMap<>();
        for(String line : file1) {
            String term = line.substring(0, line.indexOf("|"));
            String info = line.substring(line.indexOf("|") + 1);
            rawTerms.put(term, info);
        }
        for(String line : file2){
            rawTerms = upperToLowerCase(rawTerms, line.substring(0, line.indexOf("|"))); // upper/lower case
            terms = mergeTermsToMap(rawTerms, line, isUpperLowerAction);
        }
        return terms;
    }

    /**
     * upper/lower case law
     * @param rawTerms
     * @param term
     * @return
     */
    private SortedMap<String, String> upperToLowerCase(SortedMap<String, String> rawTerms, String term){
        String data;
        // lower after upper
        if(rawTerms.containsKey(term.toUpperCase()) && Character.isLowerCase(term.charAt(0))){
            isUpperLowerAction = true;
            data = rawTerms.get(term.toUpperCase());
            if(rawTerms.get(term.toLowerCase()) != null) {
                data += rawTerms.get(term.toLowerCase());
            }

            term = term.toLowerCase();
            rawTerms.put(term, data);
            rawTerms.remove(term.toUpperCase());
        }

        // upper after lower
        if(rawTerms.containsKey(term.toLowerCase()) && Character.isUpperCase(term.charAt(0))){
            isUpperLowerAction = true;
            data = rawTerms.get(term.toLowerCase());
            if(rawTerms.get(term.toUpperCase()) != null) {
                data += rawTerms.get(term.toUpperCase());
            }

            term = term.toLowerCase();
            rawTerms.put(term, data);
            rawTerms.remove(term.toUpperCase());
        }
        else {
            isUpperLowerAction = false;
        }

        return rawTerms;
    }

    /**
     * save and write info about docs of corpus
     */
    private void saveDocInfo() {
        StringBuilder text = new StringBuilder();
        StringBuilder entities = new StringBuilder();
        for (Integer key : mapDocID.keySet()) {
            ArrayList<String> listDocInfo = mapDocID.get(key); /// DOCID | DOCNAME ? Term : maxtf , counter(unique terms per doc)
            sumdl += Integer.parseInt(listDocInfo.get(3));
            text.append(key).append("|").append(listDocInfo.get(0).trim()).append("?").append(listDocInfo.get(1)).append(":").append(listDocInfo.get(2)).append(",").append(listDocInfo.get(3))
                    .append(";").append("\n");
        }
        usingBufferedWritter(text.toString(),"Doc");
        mapDocID = new LinkedHashMap<>();
    }

    /**
     * Save entities in format [DocId|entity1,entity2...] to posting file: "Entities"
     */
    private void saveEntitiesInfo(){
        StringBuilder entities = new StringBuilder();
        for (Integer key : mapDocID.keySet()) {
            ArrayList<String> listDocInfo = mapDocID.get(key); /// DOCID | DOCNAME ? Term : maxtf , counter(unique terms per doc)
            if(listDocInfo.get(0).equals("")){
                continue;
            }

            // in the listDocInfo, the entities start from  listDocInfo[4]
            int counter = 4;
            entities.append(listDocInfo.get(0)).append("|"); // append doc name
            while (counter < listDocInfo.size() - 1){
                entities.append(listDocInfo.get(counter)).append(",");
                counter++;
            }
            entities.append("\n");
        }

        usingBufferedWritter(entities.toString(),"Entities");
    }

    /**
     *    add title to posting file "Titles": [docName|title(lower case)]
     */
    public void addTitle(String title) {
        this.titles.append(docIDCounter - 1).append("|").append(title.trim()).append("\n");
    }

    /**
     * create the Dictionary and write in it all info
     * @param termsToDict
     */
    private void termToDictionary(Map<String, String> termsToDict) {
        Set<String> terms = termsToDict.keySet();
        int lineCounter = 1;
        int totalDocs;
        for(String term : terms){
            int df = 0;
            totalDocs = 0;
            String infoText = termsToDict.get(term);

            Pattern dfPattern = Pattern.compile("(\\d+);");
            Matcher dfMatcher = dfPattern.matcher(infoText);
            while (dfMatcher.find()){
                totalDocs += Integer.parseInt(dfMatcher.group(1));
                df++;
            }

            String infoDic = totalDocs + ":" + df + ";" + lineCounter;
            lineCounter ++;

            this.mapDictionary.put(term, infoDic);
            sizeDictionary++;
        }
        String toFile = mapToFormatString(this.mapDictionary, "Dictionary");
        usingBufferedWritter(toFile, "Dictionary");
        this.mapDictionary = new LinkedHashMap<>();
    }

    /**
     * Amount of uniq terms
     * @return
     */
    public int getDictionarySize(){
        return sizeDictionary;
    }


    /**
     * write stop words to posting file for Parser in Searcher.class
     * @param stopWords
     */
    public void writeStopWordsToPosting(String stopWords){
        usingBufferedWritter(stopWords, "stop_words.txt");
    }

    /**
     * give to each doc id
     * @param docIDCounter
     */
    public static void setDocIDCounter(int docIDCounter) {
        Indexer.docIDCounter = docIDCounter;
    }

    /**
     * get present doc id
     * @return
     */
    public static int getDocIDCounter() {
        return docIDCounter;
    }

    /**
     * comparator for case insensitive sort with uniq hash for upper and lower letters to sorted map
     * @param o1
     * @param o2
     * @return
     */
    private int compare(String o1, String o2) {
        int cmp = o1.compareToIgnoreCase(o2);
        if (cmp != 0) return cmp;

        return o1.compareTo(o2);
    }

}