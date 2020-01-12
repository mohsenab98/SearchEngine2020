package Classes;

import com.medallia.word2vec.Searcher;
import com.medallia.word2vec.Word2VecModel;
import java.io.File;
import java.util.*;

public class Ranker {

    private double N;
    private double avgdl;
    private double k1;
    private double b;
    private String rawNarrative;

    public Ranker(int n, int avgdl, String rawNarrative) {
        N = n;
        this.avgdl = avgdl;
        this.k1 = 1.2;
        this.b = 0.7;
        this.rawNarrative = rawNarrative;
    }



    /**
     * gets Doc(key) -- (qi - tfi - dfi)(value)
     * @param docTermInfo
     * @return docID - score
     */
    public Map<String, String> rankBM25(Map<String, String> docTermInfo) {

        Map<String, String> bm25Result = new HashMap<>();
        String[] narative = getRelevant();
        String relevant;
        String notRelevant;
        for(String docId : docTermInfo.keySet()){
            // total |D|, df, tf, term
            String [] termsInfo = docTermInfo.get(docId).split(" ");
            double score = 0;
            double IDF;
            double numerator;
            double denominator;
            int tfi;
            int dfi;
            int total; // |D|

            for(int i = 0; i <termsInfo.length - 3; i = i + 4){
                // Score(D,Q) -- BM25
                total = Integer.parseInt(termsInfo[i]);
                dfi = Integer.parseInt(termsInfo[i + 1]);
                tfi = Integer.parseInt(termsInfo[i + 2]);
                //TODO:termInfo[i+3] = term ???????????

                //log(N/dfi)
                IDF =  (Math.log((this.N / dfi)) / Math.log(2));

                numerator =  tfi * (this.k1 + 1);
                denominator = tfi + (this.k1) * (1 - this.b + (this.b * (total/this.avgdl)));

                score = score + IDF * (numerator / denominator);
            }
            bm25Result.put(docId, String.valueOf(score));

        }
        return bm25Result;
    }

    public Set<String> LSA(String term){
        Set<String> synonyms = new HashSet<>();
        try {
            Word2VecModel vecModel = Word2VecModel.fromBinFile(new File("resources/corpusVector150K.bin"));
            Searcher searcher = vecModel.forSearch();
            List<Searcher.Match> matches = searcher.getMatches(term.toLowerCase(), 100);

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

    private String[] getRelevant() {

        return null;
    }
}
