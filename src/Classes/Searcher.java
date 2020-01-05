package Classes;

import Model.MyModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Dictionary(Search term ) ->>>>>> search in posting(a-z) the relative info of the term
 */
public class Searcher {
    private List<String> queryTerms;
    private String postingPath; // includes stem/nostem folder


    public Searcher(String query, String postingPath){
        this.queryTerms = Arrays.asList(query.split(" "));
        this.postingPath = postingPath;
    }

    public void search(){
        for (String term : queryTerms){
            String termLine = MyModel.mapDictionary.get(term);
            if(termLine.isEmpty()){
                continue;
            }
            List<Integer> termInfo = getTermInfo(termLine);
            String termPostingLine = getPostingLine(termInfo.get(2));
            Map<Integer,Integer> docTf = getDocTf(termPostingLine); ////????????????????????

        }

    }

    /**
     * get the doc number and tf of the term in each document in the posting
     * @param termPostingLine
     * @return
     */
    private Map<Integer, Integer> getDocTf(String termPostingLine) {
        return null;
    }

    /**
     * receive line number and return the posting line as a String
     * @param integer
     * @return
     */
    private String getPostingLine(int integer) {
        return null;

    }

    /**
     * get the term line info from the dictionary and return the [total appearance(in the corpus) + df + line number]
     * @param termLine
     * @return
     */
    private List<Integer> getTermInfo(String termLine) {
        return null;
    }


}
