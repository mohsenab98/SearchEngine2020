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
            String termPostingLine = getPostingLine(termInfo.get(2), String.valueOf(termLine.charAt(0)));
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
    private String getPostingLine(int postingLineNumber, String postingName) {
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


    /**
     * get doc number(= (line number+1) in the posting) and return the 5 dominant entities in the doc
     * @param docNumber
     * @return
     */
    public List<String> getDocEntities(int docNumber){
        // call getPostingLine Function to get the specific line from the posting
        String line = getPostingLine(docNumber + 1, "Doc");
        //take the entites only from the specific line
        int semicolonIndex = line.indexOf(';');
        String entities = line.substring(semicolonIndex);

        List<String> listEntities = Arrays.asList(entities.split(","));
        return  listEntities;

    }


}
