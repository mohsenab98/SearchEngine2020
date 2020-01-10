package Classes;

import Model.MyModel;
import com.medallia.word2vec.Searcher;
import com.medallia.word2vec.Word2VecModel;
import com.medallia.word2vec.thrift.Word2VecModelThrift;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;

import java.io.File;
import java.util.*;

public class Ranker {

    private double N;
    private double avgdl;
    private double k1;
    private double b;

    // LSA
    private static ILexicalDatabase db = new NictWordNet();

    public Ranker(int n, int avgdl) {
        N = n;
        this.avgdl = avgdl;
        this.k1 = 1.2;
        this.b = 0.7;
    }



    /**
     * gets Doc(key) -- (qi - tfi - dfi)(value)
     * @param docTermInfo
     * @return docID - score
     */
    public Map<String, String> rankBM25(Map<String, String> docTermInfo) {

        Map<String, String> bm25Result = new HashMap<>();
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
            Word2VecModel vecModel = Word2VecModel.fromTextFile(new File("C:\\Users\\EvgeniyU\\Desktop\\ThirdYear\\DataRetrieval\\ir2020\\out\\word2vec.c.output.model.txt"));
            Searcher searcher = vecModel.forSearch();
            List<Searcher.Match> matches = searcher.getMatches(term, 100);

            for (Searcher.Match match : matches){
                if(match.distance() >= 0.95){
                    synonyms.add(match.match());
                }
            }

        }
        catch (Exception e){
            e.printStackTrace();
        }

        synonyms.add(term);
        return synonyms;
    }
}
