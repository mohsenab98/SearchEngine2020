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

    public Ranker(int n, int avgdl) {
        N = n;
        this.avgdl = avgdl;
        this.k1 = 1.7;
        this.b = 0.7;
    }



    /**
     * gets Doc(key) -- (qi - tfi - dfi)(value)
     * @param docTermInfo
     * @return docID - score
     */
    public Map<String, String> rankBM25(Map<String, String> docTermInfo, Map<String, String> docEntities, Map<String, String> docTitles, Map<String, Double>synonyms) {
        Map<String, String> bm25Result = new HashMap<>();
        for(String docId : docTermInfo.keySet()){
            // |Q|, |D|, maxTfTerm, df, tf, term
            String [] termsInfo = docTermInfo.get(docId).split("!");
            double score = 0;
            double IDF;
            double numerator;
            double denominator;
            double tfi =1;
            int dfi;
            int total; // |D|
            int titleScore = 0;
            int entitiesScore = 0;
            int maxTfScore = 0;
            for(int i = 3; i <termsInfo.length - 2; i = i + 3){
                // Score(D,Q) -- BM25
                total = Integer.parseInt(termsInfo[1]);
                dfi = Integer.parseInt(termsInfo[i]);
                tfi = Integer.parseInt(termsInfo[i + 1]);

                String maxTfTerm = termsInfo[2];
                String term = termsInfo[i + 2].toLowerCase();
                if(MyModel.mapDictionary.containsKey(term.toUpperCase())){
                    entitiesScore = 100;
                }
                if(maxTfTerm.equals(term)){
                    maxTfScore = 10;
                }

                //Title
                if(!docTitles.isEmpty()) {
                    String[] title = docTitles.get(docId).split(",");
                    for (String termT : title) {
                        if (termT.equalsIgnoreCase(term)) {
                            titleScore += 10;
                        }
                    }
                }

                IDF =  (Math.log((this.N / dfi)) / Math.log(2));

                numerator =  tfi * (this.k1 + 1);
                denominator = tfi + (this.k1) * (1 - this.b + (this.b * (total/this.avgdl)));
                if(synonyms.containsKey(term)){
                    score = score + IDF * (numerator / denominator)*synonyms.get(term);
                }else{
                    score = score + IDF * (numerator / denominator);
                }
            }
            // 1.3 + 0.3 = 155
            bm25Result.put(docId, String.valueOf(1.3*score + 0.3*(entitiesScore + titleScore + maxTfScore)));

        }
        return bm25Result;
    }


    public Map<String, Double> semanticSearchFunction(String term){
//        Set<String> synonyms = new HashSet<>();
        Map<String, Double> synonyms = new HashMap<>();
        try {
            Word2VecModel vecModel = Word2VecModel.fromBinFile(new File("resources/corpusVector150K.bin"));
            Searcher searcher = vecModel.forSearch();
            List<Searcher.Match> matches = searcher.getMatches(term.toLowerCase(), 2);

            int synonymCounter = 0;
            for (Searcher.Match match : matches){
                if(synonymCounter < 3){
//                    synonyms.add(match.match());
                    if(match.match().equalsIgnoreCase(term)){
                        synonyms.put(match.match(), 1.0);
                    }else{
                        synonyms.put(match.match(), 0.5);
                    }
                }
                synonymCounter++;
            }

        }
        catch (Exception e){
            synonyms.put(term, 1.0);
        }

        return synonyms;
    }

}
