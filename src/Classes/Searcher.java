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


    public Searcher(String query, String postingPath){
        this.queryTerms = Arrays.asList(query.split(" "));
        this.postingPath = postingPath;
        this.ranker = setRanker();
    }

    private Ranker setRanker() {
        try {
            Stream<String> lines = Files.lines(Paths.get(this.postingPath + "/" + "BM25Info"), StandardCharsets.US_ASCII );
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

    public void search(){
        //TODO : delete
        MyModel m = new MyModel();
        m.loadDictionary(new File("C:\\Users\\mohse\\Desktop\\corpusTest6\\noStem\\Dictionary"));


        Map<Integer, ArrayList<String>> docQ = new HashMap<>(); // ArrayList: i: queryTerm, i + 1: tf, i + 2: df
        for (String term : queryTerms){
            String termLine = MyModel.mapDictionary.get(term);
            if(termLine == null){
                continue;
            }
            List<Integer> termInfo = getTermInfo(termLine); // get term info( total - df - pointer)
            String termPostingLine = getPostingLine(termInfo.get(2), String.valueOf(term.charAt(0))); // get line as string using the pointer above
            Map<Integer,Integer> docTf = getDocTf(termPostingLine);// get the doc_i & tf_i per term

            // save as : Doc(key) -- (qi - tfi - dfi)(value)
            Iterator<Map.Entry<Integer, Integer>> it = docTf.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, Integer> pair = it.next();
                Integer docId = pair.getKey();
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
        ranker.rankBM25(docQ);
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
        try {
            if(Character.isDigit(postingName.charAt(0))){
                postingName = "Numbers";
            }
            Stream<String> lines = Files.lines(Paths.get(this.postingPath + "/" + postingName), StandardCharsets.US_ASCII );
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
    private Map<Integer, Integer> getDocTf(String termPostingLine) {
        SortedMap<Integer, Integer> docTf = new TreeMap<>();
        Pattern p = Pattern.compile("(\\d+):(\\d+)");
        Matcher m = p.matcher(termPostingLine);
        while (m.find()){
            docTf.put(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
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
