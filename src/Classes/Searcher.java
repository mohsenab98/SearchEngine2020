package Classes;
import Model.MyModel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
            String termPostingLine = getPostingLine(termInfo.get(2), termLine.charAt(0));
            Map<Integer,Integer> docTf = getDocTf(termPostingLine); ////????????????????????

        }

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
        listTermInfo.add(Integer.parseInt(termLine.substring(colonIndex, semicolonIndex)));
        //add LineCounter
        listTermInfo.add(Integer.parseInt(termLine.substring(semicolonIndex)));

        return listTermInfo;
    }

    /**
     * receive line number and return the posting line as a String
     * @param postingLineNumber
     * @return
     */
    private String getPostingLine(int postingLineNumber, char postingName) {
        int lineCounter = 0;
        try {
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
        return null;
    }


}
