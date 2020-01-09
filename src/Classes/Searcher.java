package Classes;
import Model.MyModel;

import java.io.File;
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
    private String postingPath; // includes stem/nostem folder
    private Ranker ranker;
    private boolean isStem;
    private boolean isSemantic;
    private Stemmer stemmer;
    private String narrDesc;



    public Searcher(String query, String postingPath, boolean stem, boolean semantic){
        this.queryTerms = Arrays.asList(query.split(" "));
        this.postingPath = postingPath;
        this.ranker = setRanker();
        this.isStem = stem;
        this.isSemantic = semantic;
        this.stemmer = new Stemmer();
    }

    public Searcher(String query, String postingPath, boolean stem, boolean semantic, String narrDesc){
        this.queryTerms = Arrays.asList(query.split(" "));
        this.postingPath = postingPath;
        this.ranker = setRanker();
        this.isStem = stem;
        this.isSemantic = semantic;
        this.stemmer = new Stemmer();
        this.narrDesc = narrDesc;
    }

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
            return new Ranker(info[0], info[1]);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<String, String> search(){
        // user chose search with semantic treatment
        if(isSemantic && narrDesc != null){
            List<String> queryTermsLSA = new ArrayList<>();
            for(String term : queryTerms){
                List<String> synonyms = new ArrayList<>(ranker.LSA(term.toLowerCase(), narrDesc));
                queryTermsLSA.addAll(synonyms);
            }

            queryTerms = queryTermsLSA;
        }

        Map<String, String> docTf = new LinkedHashMap<>();
        Map<String, ArrayList<String>> docQ = new HashMap<>(); // ArrayList: i: queryTerm, i + 1: tf, i + 2: df
        for (String term : queryTerms) {
            // check upper and lower cases
            if (this.isStem) {
                term = this.stemmer.porterStemmer(term);
            }


            //
            String termLine = MyModel.mapDictionary.get(term.toLowerCase());
            if (termLine == null) {
                termLine = MyModel.mapDictionary.get(term.toUpperCase());
                if (termLine == null) {
                    continue;
                }
            }

            String[] termInfo = getTermInfo(termLine).split(" "); // // total |D|, df, tf
            String termPostingLine = getPostingLine(Integer.parseInt(termInfo[2]), String.valueOf(term.charAt(0)));

            Pattern p = Pattern.compile("(\\d+):(\\d+)");
            Matcher m = p.matcher(termPostingLine);
            while (m.find()) {
                termInfo = getTermInfo(termLine).split(" "); // // total |D|, df, tf
                docTf.put( m.group(1), termInfo[0] + " " + termInfo[1] + " " + m.group(2) + " " + term); // total |D|, df, tf, term
            }
        }


        // get line as string using the pointer above
        //Map<String,String> docTf = getDocTf(termPostingLine);// get the doc_i & tf_i per term

        // save as : Doc(key) -- (qi - tfi - dfi)(value)
        Iterator<Map.Entry<String, String>> it = docTf.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> pair = it.next();
            String docId = pair.getKey();
            String[] termInfo = pair.getValue().split(" "); // total |D|, df, tf, term
            String totalD = termInfo[0];
            String df = termInfo[1];
            String tf = termInfo[2];
            String term = termInfo[3];
            if(!docQ.containsKey(docId)){
                ArrayList<String> queryTfDf = new ArrayList<>();
                queryTfDf.add(totalD); // total |D|
                queryTfDf.add(df); // df_i
                queryTfDf.add(tf); // tf_i
                queryTfDf.add(term); // query_i
                docQ.put(docId, queryTfDf);
            }else{
                ArrayList<String> queryTfDf = docQ.get(docId);
                queryTfDf.add(totalD); // total |D|
                queryTfDf.add(df); // df_i
                queryTfDf.add(tf); // tf_i
                queryTfDf.add(term); // query_i
                docQ.put(docId, queryTfDf);
            }

        }// while END

        // send to ranker bm25 function

        Map<String, String> rankedDocs = sortDocsByRank(ranker.rankBM25(docQ));
        rankedDocs = get50Docs(rankedDocs);
        return rankedDocs;

    }

    private Map<String, String> get50Docs(Map<String, String> rankedDocs) {
        Map<String, String> rankedDocs50 = new LinkedHashMap<>();
        int counter = 0;
        for(String doc : rankedDocs.keySet()){
            int lineCounter = 0;
            int docId = Integer.parseInt(doc);
            String docStr = "";
            String stem = "";
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
     * get the doc number and tf of the term in each document in the posting
     * @param termPostingLine
     * @return
     */
    private Map<String, String> getDocTf(String termPostingLine) {
        SortedMap<String, String> docTf = new TreeMap<>();
        Pattern p = Pattern.compile("(\\d+):(\\d+)");
        Matcher m = p.matcher(termPostingLine);
        while (m.find()){
            String lineDoc = getPostingLine(Integer.parseInt(m.group(1)), "Doc");
            int start = lineDoc.indexOf('|');
            int end = lineDoc.indexOf('?');
            //TODO : debug
            String docName = lineDoc.substring(start + 1, end );
            docTf.put(docName.trim(), m.group(2));
        }
        return docTf;
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

}
