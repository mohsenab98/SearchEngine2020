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



    public Searcher(String query, String postingPath, boolean stem, boolean semantic){
        this.queryTerms = Arrays.asList(query.split(" "));
        this.postingPath = postingPath;
        this.ranker = setRanker();
        this.isStem = stem;
        this.isSemantic = semantic;
        this.stemmer = new Stemmer();
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
        //TODO : delete
//        MyModel m = new MyModel();
//        m.loadDictionary(new File("D:\\corpusResults\\noStem\\Dictionary"));

        // user chose search with semantic treatment
        if(isSemantic){
            List<String> queryTermsLSA = new ArrayList<>();
            for(String term : queryTerms){
                List<String> synonyms = ranker.LSA(term.toLowerCase());
                queryTermsLSA.addAll(synonyms);
            }

            queryTerms = queryTermsLSA;
        }


        Map<String, ArrayList<String>> docQ = new HashMap<>(); // ArrayList: i: queryTerm, i + 1: tf, i + 2: df
        for (String term : queryTerms){
            // check upper and lower cases
            if (this.isStem) {
                term = this.stemmer.porterStemmer(term);
            }


            //
            String termLine = MyModel.mapDictionary.get(term.toLowerCase());
            if(termLine == null){
                termLine = MyModel.mapDictionary.get(term.toUpperCase());
                if(termLine == null) {
                    continue;
                }
            }

            List<Integer> termInfo = getTermInfo(termLine); // get term info( total - df - pointer)
            String termPostingLine = getPostingLine(termInfo.get(2), String.valueOf(term.charAt(0))); // get line as string using the pointer above
            Map<String,String> docTf = getDocTf(termPostingLine);// get the doc_i & tf_i per term

            // save as : Doc(key) -- (qi - tfi - dfi)(value)
            Iterator<Map.Entry<String, String>> it = docTf.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> pair = it.next();
                String docId = pair.getKey();
                if(!docQ.containsKey(docId)){
                    ArrayList<String> queryTfDf = new ArrayList<>();
                    queryTfDf.add(term); // query_i
                    queryTfDf.add(String.valueOf(pair.getValue())); // tf_i
                    queryTfDf.add(String.valueOf(termInfo.get(1))); // df_i
                    queryTfDf.add(String.valueOf(termInfo.get(0))); // total |D|
                    docQ.put(docId, queryTfDf);
                }else{
                    ArrayList<String> queryTfDf = docQ.get(docId);
                    queryTfDf.add(term); // query_i
                    queryTfDf.add(String.valueOf(pair.getValue())); // tf_i
                    queryTfDf.add(String.valueOf(termInfo.get(1))); // df_i
                    queryTfDf.add(String.valueOf(termInfo.get(0))); // total |D|
                    docQ.put(docId, queryTfDf);
                }

            }// while END

        }// for While

        // send to ranker bm25 function
       return ranker.rankBM25(docQ);


    }


    /**
     * get the term line info from the dictionary and return the [total appearance(in the corpus) + df + line number]
     * @param termLine
     * @return
     */
    private List<Integer> getTermInfo(String termLine) {
        int colonIndex = termLine.indexOf(':');
        int semicolonIndex = termLine.indexOf(';');
        List<Integer> listTermInfo = new ArrayList<>();

        //add totalDocs
        listTermInfo.add(Integer.parseInt(termLine.substring(0, colonIndex)));
        //add df
        listTermInfo.add(Integer.parseInt(termLine.substring(colonIndex + 1, semicolonIndex)));
        //add LineCounter
        listTermInfo.add(Integer.parseInt(termLine.substring(semicolonIndex + 1)));

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


}
