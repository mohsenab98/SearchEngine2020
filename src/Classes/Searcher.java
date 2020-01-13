package Classes;
import Model.MyModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

/**
 * Dictionary(Search term ) ->>>>>> search in posting(a-z) the relative info of the term
 */
public class Searcher {
    private List<String> queryTerms;
    private Map<String, String> docAllEntities;
    private Map<String, String> doc5Entities;
    private Map<String, String> docTitle;
    private String postingPath; // includes stem/nostem folder
    private String narrative;
    private Ranker ranker;
    private boolean isStem;
    private boolean isSemantic;
    private Stemmer stemmer;

    public Searcher(String query, String postingPath, boolean stem, boolean semantic, String narrative){
        this.queryTerms = Arrays.asList(query.split(" "));
        this.docAllEntities = new LinkedHashMap<>();
        this.doc5Entities = new LinkedHashMap<>();
        this.docTitle = new LinkedHashMap<>();
        this.postingPath = postingPath;
        this.narrative = narrative;
        this.ranker = setRanker();
        this.isStem = stem;
        this.isSemantic = semantic;
        this.stemmer = new Stemmer();
    }

    /**
     * Creates new Ranker Instance with info taken from "BM25Info" posting file (|D| & avgId)
     * @return
     */
    private Ranker setRanker() {
        try {
            String stem = "";
            if(isStem){
                stem = "stem";
            }else{
                stem = "noStem";
            }
            Stream<String> lines = Files.lines(Paths.get(this.postingPath + "/" + stem + "/" + "BM25Info"), StandardCharsets.US_ASCII );
            // get line [#postingLineNumber] in posting file
            int[] info = new int[2];
            int i = 0;
            for( String line : (Iterable<String>) lines::iterator ){
                info[i] = Integer.parseInt(line);
                i++;
            }
            return new Ranker(info[0], info[1], narrative);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * main function of this class -- search for 50 most relevant files to the query given by the user
     * @return
     */
    public Map<String, String> search(){
        // user chose search with semantic treatment
        if(isSemantic){
            List<String> queryTermsLSA = new ArrayList<>();
            for(String term : queryTerms){
                List<String> synonyms = new ArrayList<>(ranker.LSA(term.toLowerCase()));
                queryTermsLSA.addAll(synonyms);
            }
            queryTerms = queryTermsLSA;
        }

        Map<String, String> docTermsInfo = new LinkedHashMap<>(); // save <DocID , <Term1 Info> <Term 2 Info>... >
        for (String term : queryTerms) {
            // check upper and lower cases
            if (this.isStem) {
                term = this.stemmer.porterStemmer(term);
            }

            // term line from dictionary (term | totalDocs |D| :  df ; lineCounter)
            String termLine = MyModel.mapDictionary.get(term.toLowerCase());
            if (termLine == null) {
                termLine = MyModel.mapDictionary.get(term.toUpperCase());
                if (termLine == null) {
                    // Stop words and other words that the dictionary doesnt contain
                    continue;
                }
            }

            String[] termInfo = getTermInfo(termLine).split(" "); // // total |D|, df, line counter
            //get posting line from the posting file a-z using the function "getPostingLine(line number , char(file Name))"
            String termPostingLine = getPostingLine(Integer.parseInt(termInfo[2]), String.valueOf(term.charAt(0)));

            Pattern p = Pattern.compile("(\\d+):(\\d+)");
            Matcher m = p.matcher(termPostingLine);
            while (m.find()) {
                this.docAllEntities.put(m.group(1), ""); // add doc entities(key) to the map of entities
                this.docTitle.put(m.group(1), "");
                termInfo = getTermInfo(termLine).split(" "); // // total |D|, df, tf
                String termInfoInMap = "";
                //chaining the doc term info to the map
                if(docTermsInfo.containsKey(m.group(1))){
                    termInfoInMap =  docTermsInfo.get(m.group(1)) + " ";
                }
                docTermsInfo.put( m.group(1), termInfoInMap + termInfo[0] + " " + termInfo[1] + " " + m.group(2) + " " + term); // total |D|, df, tf, term
            }
        }
        // send to ranker bm25 function
        addDocFromDoc("Titles", this.docTitle);// add entities(value) to the map of entities
        addDocFromDoc("Entities", this.docAllEntities); // add entities(value) to the map of entities
        Map<String, String> rankedDocs = sortDocsByRank(ranker.rankBM25(docTermsInfo, this.docAllEntities, this.docTitle));
        rankedDocs = get50Docs(rankedDocs);
        addDoc5Entities(); // add entities(value) to the map of entities
        return rankedDocs;

    }

    private void addDoc5Entities() {
        int counter = 0;
        Set<String> docIdAndNameSet = new LinkedHashSet<>(this.doc5Entities.keySet()); //
        for(String docIdAndName : docIdAndNameSet){

        }

    }


    /**
     * The function gets postingFileName: "Titles" or "Entities" and field's map postFileToProcess in format <postingFileName, "">,
     * reads the info according to postingFileName,
     * filters the words of the info by the dictionary,
     * write the filtered info to field's postFileToProcess in format: <postingFileName, "word1,word2, ...,wordN">
     * @param postingFileName
     * @param postFileToProcess
     */
    private void addDocFromDoc(String postingFileName, Map<String, String> postFileToProcess){
        try {
            String stem;
            if(isStem){
                stem = "stem";
            }else{
                stem = "noStem";
            }
            Stream<String> lines = Files.lines(Paths.get(this.postingPath + "/" + stem + "/" + postingFileName), StandardCharsets.US_ASCII );
            // get line with entities for a doc
            for( String line : (Iterable<String>) lines::iterator ){
                String doc = line.substring(0, line.indexOf("|"));
                // check if doc from the posting in map of entities: not => see next doc
                if(!postFileToProcess.containsKey(doc)){
                    continue;
                }

                String postingLine = line.substring(line.indexOf("|") + 1).replaceAll("[!.,?/'\";:]", " ").replaceAll("\\s+", ",");
                String[] rawPosting = postingLine.split(","); // get all entities for the doc(sorted by dominated ent. up -> down)
                // check each entity in the dictionary and get 5 most relevant
               // int counter = 0;
                for(String posting : rawPosting){
                    if(MyModel.mapDictionary.containsKey(posting.toUpperCase()) || MyModel.mapDictionary.containsKey(posting.toLowerCase())){
                        if(postFileToProcess.get(doc).isEmpty()){
                            postFileToProcess.put(doc, posting);
                        }
                        else {
                            postFileToProcess.put(doc, postFileToProcess.get(doc) + "," + posting);
                        }
                        /*
                        counter++;
                        if(counter == 5){
                            break;
                        }
                         */
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> get50Docs(Map<String, String> rankedDocs) {
        Map<String, String> rankedDocs50 = new LinkedHashMap<>();
        int counter = 0;
        for(String doc : rankedDocs.keySet()){
            int lineCounter = 0;
            int docId = Integer.parseInt(doc);
            String docStr = "";
            String stem;
            if(isStem){
                stem = "stem";
            }else{
                stem = "noStem";
            }
            try {
                Stream<String> lines = Files.lines(Paths.get(this.postingPath + "/" + stem + "/" + "Doc"), StandardCharsets.US_ASCII );
                // get line [#postingLineNumber] in posting file
                for( String line : (Iterable<String>) lines::iterator ){
                    if(lineCounter == docId){
                        docStr = line.substring(line.indexOf("|") + 1, line.indexOf("?"));
                    }
                    lineCounter++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            rankedDocs50.put(docStr, rankedDocs.get(doc));
            this.doc5Entities.put(docId + "|" + docStr, "");
            //this.mapPosting.put(docStr, "");
            if(counter == 49){
                break;
            }
            counter++;
        }
        return rankedDocs50;
    }


    /**
     * get the term line info from the dictionary and return the [total appearance(in the corpus) + df + line number]
     * @param termLine
     * @return
     */
    private String getTermInfo(String termLine) {
        int colonIndex = termLine.indexOf(':');
        int semicolonIndex = termLine.indexOf(';');
        String listTermInfo;

        //add totalDocs
        listTermInfo = termLine.substring(0, colonIndex);
        //add df
        listTermInfo += " " + termLine.substring(colonIndex + 1, semicolonIndex);
        //add LineCounter
        listTermInfo += " " + termLine.substring(semicolonIndex + 1);

        return listTermInfo;
    }

    /**
     * receive line number and return the posting line as a String
     * @param postingLineNumber
     * @return
     */
    private String getPostingLine(int postingLineNumber, String postingName) {
        int lineCounter = 1;
        String stem = "";
        if(isStem){
            stem = "stem";
        }else{
            stem = "noStem";
        }
        try {
            if(Character.isDigit(postingName.charAt(0))){
                postingName = "Numbers";
            }
            Stream<String> lines = Files.lines(Paths.get(this.postingPath + "/" + stem + "/" + postingName), StandardCharsets.US_ASCII );
            // get line [#postingLineNumber] in posting file
            for( String line : (Iterable<String>) lines::iterator ){
                if(lineCounter == postingLineNumber){
                    return line;
                }
                lineCounter++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * get doc number(= (line number+1) in the posting) and return the 5 dominant entities in the doc
     * @param docNumber
     * @return
     */
    public List<String> getDocEntities(int docNumber){
        // call getPostingLine Function to get the specific line from the posting
        String line = getPostingLine(docNumber, "Doc");
        //take the entites only from the specific line
        int semicolonIndex = line.indexOf(';');
        String entities = line.substring(semicolonIndex);

        List<String> listEntities = Arrays.asList(entities.split(","));
        return  listEntities;

    }

    /**
     * Sort map by rank(value)
     * @param rankedDocs
     * @return
     */
    private Map<String, String> sortDocsByRank(Map<String, String> rankedDocs){
        Map<String, Double> sortedDocs = new LinkedHashMap<>();

        for(String doc : rankedDocs.keySet()){
            sortedDocs.put(doc, Double.parseDouble(rankedDocs.get(doc)));
        }

        Map<String, Double> sortedEntities = sortedDocs
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(
                        toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                LinkedHashMap::new));

        rankedDocs = new LinkedHashMap<>();
        for(String doc : sortedEntities.keySet()){
            rankedDocs.put(doc, String.valueOf(sortedEntities.get(doc)));
        }

        return rankedDocs;
    }

    public Map<String, String> getEntities(){
        return this.docAllEntities;
    }

}
