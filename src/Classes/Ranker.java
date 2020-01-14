package Classes;

import Model.MyModel;
import com.medallia.word2vec.Searcher;
import com.medallia.word2vec.Word2VecModel;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ranker {

    private double N;
    private double avgdl;
    private double k1;
    private double b;
    private String rawNarrative;

    public Ranker(int n, int avgdl, String rawNarrative) {
        N = n;
        this.avgdl = avgdl;
        this.k1 = 1.7;
        this.b = 0.7;
        this.rawNarrative = rawNarrative;
    }



    /**
     * gets Doc(key) -- (qi - tfi - dfi)(value)
     * @param docTermInfo
     * @return docID - score
     */
    public Map<String, String> rankBM25(Map<String, String> docTermInfo, Map<String, String> docEntities, Map<String, String> docTitles) {
        String[] narrative = getRelevantFromQuery();
        Set<String> relevant = new HashSet<>(Arrays.asList(narrative[0].split(" ")));
        Set<String> notRelevant = new HashSet<>(Arrays.asList(narrative[1].split(" ")));
        Map<String, String> bm25Result = new HashMap<>();

        for(String docId : docTermInfo.keySet()){
            // total |D|, df, tf, term
            String [] termsInfo = docTermInfo.get(docId).split(" ");

            double cosSim = 0;
            double score = 0;
            double IDF;
            double numerator;
            double denominator;
            double tfi =1;
            int dfi;
            int total; // |D|

            for(int i = 2; i <termsInfo.length - 2; i = i + 3){
                // Score(D,Q) -- BM25
                int[] relevantOrNot = checkRelevantInDoc(relevant, notRelevant, Integer.parseInt(docId));
                int relevantNum = 1;
                int notRelevantNum = 1;
                total = Integer.parseInt(termsInfo[1]);
                dfi = Integer.parseInt(termsInfo[i]);
                try{
                    tfi = Integer.parseInt(termsInfo[i + 1]);

                }catch (Exception e){
                    System.out.println("xxxx");
                }
                String term = termsInfo[i + 2].toLowerCase();

                //TODO: Entities ????????????????
                int entitiesNum =  valueUpBy(docEntities, docId, termsInfo[i + 2]);

                //TODO: Title ????????????????
                if(!docTitles.isEmpty()) {
                    String[] title = docTitles.get(docId).split(",");
                    for (String termT : title) {
                        if (termT.equalsIgnoreCase(term)) {

                        }
                    }
                }
                //Normalization of tfi by maxTf = termsInfo[i+4]

//                int maxTf = Integer.parseInt(termsInfo[0]);
//                if(maxTf > 0){
//                    tfi = tfi / maxTf;
//                }
//                if(total > 0){
//                    tfi = tfi / total;
//                }
                //log(N/dfi)
                IDF =  (Math.log((this.N / dfi)) / Math.log(2));

                numerator =  tfi * (this.k1 + 1);
                denominator = tfi + (this.k1) * (1 - this.b + (this.b * (total/this.avgdl)));
                cosSim = cosSim + cosinSimilarity(tfi, 1, total, Integer.parseInt(termsInfo[0])); ///??????????????????/
                score = score + IDF * (numerator / denominator);
            }
            score = (score*0 + cosSim*10)/10;
            bm25Result.put(docId, String.valueOf(score));

        }
        return bm25Result;
    }

    /**
     *
     * @param dj = tf
     * @param q = how many times the term shown in the query
     * @param D = number of words in doc
     * @param Q = number of words in query
     * @return
     */
    private double cosinSimilarity(double dj, int q, int D, int Q){
        double numerator =  (dj*q);
        double denominator =(Math.sqrt(Math.pow(D,2)*Math.pow(Q,2)));
        return numerator / denominator;
    }
    private int[] checkRelevantInDoc(Set<String> relevant, Set<String> notRelevant, int docId) {
        int relevantNum = 1;
        int notRelevantNum = 1;
        relevant.removeIf(term -> !MyModel.mapDictionary.containsKey(term));
        notRelevant.removeIf(term -> !MyModel.mapDictionary.containsKey(term));

       /* Stream<String> lines = Files.lines(Paths.get(this.postingPath + "/" + stem + "/" + postingName), StandardCharsets.US_ASCII );
        // get line [#postingLineNumber] in posting file
        for( String line : (Iterable<String>) lines::iterator ){
            if(lineCounter == postingLineNumber){
                return line;
            }
            lineCounter++;
        }

        int[] result = {relevantNum, notRelevantNum};

        */
        return null;
    }

    /**
     * if the term the appear in the query is one of the most popular entities in the doc the return vale depends on it position
     * @param docId
     * @param term
     * @return value between [1 - 6]
     */

    public int valueUpBy(Map<String, String> docEntities, String docId, String term){
        int value = 1;
        if(docEntities.get(docId) == null){
            return value;
        }
        String[] entities = docEntities.get(docId).split(",");
        for(int i = 0 ; i < entities.length; i++){
            if(entities[i].equalsIgnoreCase(term)){
                value = value * 2;
            }
        }

        return value;
    }

    private String[] getRelevantFromQuery() {
        String relevant = "";
        String notRelevant = "";
        Pattern patternRelevant;
        Matcher matcherRelevant;

        patternRelevant = Pattern.compile("^Relevant (.+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL |Pattern.MULTILINE);
        matcherRelevant = patternRelevant.matcher(rawNarrative);
        while (matcherRelevant.find()){
            relevant += matcherRelevant.group(1);
        }

        patternRelevant = Pattern.compile("relevant:(.+)(?:not relevant:)?", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        matcherRelevant = patternRelevant.matcher(rawNarrative);
        while (matcherRelevant.find()){
            relevant += matcherRelevant.group(1);
        }

        patternRelevant = Pattern.compile("not relevant:(.+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        matcherRelevant = patternRelevant.matcher(rawNarrative);
        while (matcherRelevant.find()){
            notRelevant += matcherRelevant.group(1);
        }


        patternRelevant = Pattern.compile("(.+?)(\\w{3}) relevant", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        matcherRelevant = patternRelevant.matcher(rawNarrative);
        while (matcherRelevant.find()){
            if(!matcherRelevant.group(2).equalsIgnoreCase("not")){
                relevant += matcherRelevant.group(1) + matcherRelevant.group(2);
            }
            else {
                notRelevant += matcherRelevant.group(1);
            }
        }

        return new String[]{relevant.toLowerCase().replaceAll("[!.,?/'\";:-]", " "), notRelevant.toLowerCase().replaceAll("[!.,?/'\";:-]", " ")};
    }

    public Set<String> LSA(String term){
        Set<String> synonyms = new HashSet<>();
        try {
            Word2VecModel vecModel = Word2VecModel.fromBinFile(new File("resources/corpusVector150K.bin"));
            Searcher searcher = vecModel.forSearch();
            List<Searcher.Match> matches = searcher.getMatches(term.toLowerCase(), 5);

            int synonymCounter = 0;
            for (Searcher.Match match : matches){
                if(synonymCounter < 3){
                    synonyms.add(match.match());
                }
                synonymCounter++;
            }

        }
        catch (Exception e){
            synonyms.add(term);
        }

        return synonyms;
    }

}
