package Classes;

import Model.MyModel;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
     * @param docQ
     * @return docID - score
     */
    public Map<Integer, Double> rankBM25(Map<Integer, ArrayList<String>> docQ) {

        Map<Integer, Double> bm25Result = new HashMap<>();
        Iterator<Map.Entry<Integer, ArrayList<String>>> it = docQ.entrySet().iterator();
        while (it.hasNext()) {

            Map.Entry<Integer, ArrayList<String>> pair = it.next();
            Integer docId = pair.getKey();
            ArrayList<String> queryInfo = pair.getValue();

            double score = 0;
            double IDF = 0;
            double numerator = 0;
            double denominator = 0;
            int tfi = 0;
            int dfi = 0;
            int total = 0; // |D|

            for(int i = 0; i <queryInfo.size() - 3; i = i+4){
                // Score(D,Q) -- BM25
                dfi = Integer.parseInt(queryInfo.get(i + 2));
                tfi = Integer.parseInt(queryInfo.get(i + 1));
                total = Integer.parseInt(queryInfo.get(i + 3));
                // log(N/dfi)
                IDF =  (Math.log((this.N / dfi)) / Math.log(2));

                numerator =  tfi * (this.k1 + 1);
                denominator = tfi + (this.k1) * (1 - this.b + (this.b * (total/this.avgdl)));

                score = score + IDF * (numerator / denominator);
            }
            bm25Result.put(docId, score);

        }// while END
        return null;
    }

    public ArrayList<String> LSA(String term){
        ArrayList<String> synonyms = new ArrayList<>();
        WS4JConfiguration.getInstance().setMFS(true);
        for(String dictTerm : MyModel.mapDictionary.keySet()) {
            double distance = new WuPalmer(db).calcRelatednessOfWords(term, dictTerm);
            if(distance >= 0.85){
                synonyms.add(dictTerm);
            }
        }

        return synonyms;
    }
}
