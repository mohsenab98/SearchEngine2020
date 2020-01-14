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

    private Parse parse;
    private String query;
    private String queryNumber;
    private String pathStopWords;
    public Searcher(String query, String postingPath, boolean stem, boolean semantic, String narrative, String queryNumber, String pathStopWords){
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

        this.query = query;
        this.queryNumber = queryNumber;
        this.pathStopWords = pathStopWords;
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
       //parse the query -> get the query terms like we have parsed the corpus
        this.parse = new Parse(pathStopWords, isStem);
        parse.Parser(this.query, queryNumber);
        Map<String,String> queryTermsAfterPares = parse.getMapTerms();

        for (String term : queryTermsAfterPares.keySet()) {
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
              //  this.docAllEntities.put(m.group(1), ""); // add doc entities(key) to the map of entities
              //  this.docTitle.put(m.group(1), "");
                termInfo = getTermInfo(termLine).split(" "); // // total |D|, df, tf
                String termInfoInMap = "";
                //chaining the doc term info to the map
                if(docTermsInfo.containsKey(m.group(1))){
                    termInfoInMap =  docTermsInfo.get(m.group(1)) + " ";
                }
                //TODO : change termInfo[0]=total appereance of term in corpus -> |D| = number of words in docID
                docTermsInfo.put( m.group(1), termInfoInMap + termInfo[1] + " " + m.group(2) + " " + term); // total |D|, df, tf, term
            }
        }
        // send to ranker bm25 function
        docTermsInfo = readDocFromPosting(docTermsInfo, queryTermsAfterPares.size());


        Map<String, String> rankedDocs = sortDocsByRank(ranker.rankBM25(docTermsInfo, this.docAllEntities, this.docTitle));
        docTermsInfo = removeNNotRelevantDocs(docTermsInfo, rankedDocs, 2000); // N = 1000 : 32 rel doc; N = 2000 : 36 rel doc; N = 3000 : 36 rel doc; N = 4000 : 39 rel doc;
        addDocFromDoc("Titles", this.docTitle);// add entities(value) to the map of entities
        addDocFromDoc("Entities", this.docAllEntities); // add entities(value) to the map of entities
        rankedDocs = ranker.rankBM25(docTermsInfo, this.docAllEntities, this.docTitle);
        rankedDocs = sortDocsByRank(rankedDocs);
        rankedDocs = show50DocsGUI(rankedDocs);
        addDoc5Entities(); // add entities(value) to the map of entities
        return rankedDocs;

    }

    /**
     *
     * @param docTermsInfo < DocID, "total |D|, df, tf, term | ...."></>
     * @param queryLength
     * @return
     */
    private Map<String, String> readDocFromPosting(Map<String, String> docTermsInfo, int queryLength) {
        String stem;
        if(isStem){
            stem = "stem";
        }else{
            stem = "noStem";
        }
        Stream<String> lines = null;
        try {
            lines = Files.lines(Paths.get(this.postingPath + "/" + stem + "/" + "Doc"), StandardCharsets.US_ASCII );
        } catch (IOException e) {
            e.printStackTrace();
        }
        // get line with entities for a doc
        for( String line : (Iterable<String>) lines::iterator ) {
            String doc = line.substring(0, line.indexOf("|"));
            String D = line.substring(line.indexOf(",")+1, line.indexOf(";"));
            // check if doc from the posting in map of entities: not => see next doc
            if (!docTermsInfo.containsKey(doc)) {
                continue;
            }
//            String maxTf = getMaxTfFromDoc(line);
//
//            docTermsInfo.put(doc,  maxTf + " " + docTermsInfo.get(doc));
            docTermsInfo.put(doc,  queryLength + " " + D + " " + docTermsInfo.get(doc));
        }

        return docTermsInfo;
    }


    private Map<String, String> removeNNotRelevantDocs(Map<String, String> docTermsInfo, Map<String, String> rankedDocs, int N) {
        int counter = 1;
        Map<String, String> docTermsInfoN = new LinkedHashMap<>();
        for (String doc : rankedDocs.keySet()) {
            docTermsInfoN.put(doc, docTermsInfo.get(doc));
            this.docTitle.put(doc, "");
            this.docAllEntities.put(doc, "");
            if(counter == N){
                break;
            }
            counter++;
        }
        return docTermsInfoN;
    }


    /**
     * Get 5 dominating entities to field map doc5Entities
     */
    private void addDoc5Entities() {
        Set<String> docIdAndNameSet = new LinkedHashSet<>(this.doc5Entities.keySet()); // add possibility to change the original this.doc5Entities map
        this.doc5Entities = new LinkedHashMap<>(); // renew this.doc5Entities for adding 5 entities
        // for each on the set of relevant docs: docIdAndName in format <docID|docName>
        for(String docIdAndName : docIdAndNameSet){
            String docId = docIdAndName.substring(0, docIdAndName.indexOf("|")); // get docID
            String docName = docIdAndName.replaceAll(docId, "").replace("|" , ""); // get docName
            this.doc5Entities.put(docName, ""); // create key-value in the map: <docName, "">
            String[] entities = this.docAllEntities.get(docId).split(","); // get all entities to string array

            int counter = 0;
            StringBuilder fiveEntities = new StringBuilder();
            for(String entity : entities){ // to each entity
                // filter entities by dictionary (check if the "entity" is entity in the corpus)
                if(!MyModel.mapDictionary.containsKey(entity)){
                    continue;
                }

                fiveEntities.append(entity).append(", "); // String of entities in format: "entity1, entity2..."

                if(counter == 4){
                    break;
                }

                counter++;
            }
            // remove ", " in the end of the 5 entities
            if(!fiveEntities.toString().isEmpty()) {
                int lastCommaIndex = fiveEntities.length() - 2;
                this.doc5Entities.put(docName, fiveEntities.delete(lastCommaIndex, lastCommaIndex + 1).toString());
            }
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
                StringBuilder postingLine = new StringBuilder(line.substring(line.indexOf("|") + 1));
                this.parse = new Parse(pathStopWords, isStem);
                parse.Parser(postingLine.toString(), doc);
                postingLine = new StringBuilder();

                for(String term : parse.getMapTerms().keySet()){
                    postingLine.append(term).append(",");
                }

                postFileToProcess.put(doc, postingLine.toString());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets all relevant docs sorted by rank and chooses 50 most relevant
     * Adds the 50 relevant docs to the map this.doc5Entities
     * @param rankedDocs
     * @return
     */
    private Map<String, String> show50DocsGUI(Map<String, String> rankedDocs) {
        Map<String, String> rankedDocsN = new LinkedHashMap<>();
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

            rankedDocsN.put(docStr, rankedDocs.get(doc));
            this.doc5Entities.put(docId + "|" + docStr, "");
            if(counter == 49){
                break;
            }
            counter++;
        }
        return rankedDocsN;
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

    public String getMaxTfFromDoc(String termLine) {
        int start = termLine.indexOf(":");
        int end = termLine.lastIndexOf(",");
        return termLine.substring(start+1, end);

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
        return this.doc5Entities;
    }

}
