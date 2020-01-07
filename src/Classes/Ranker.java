package Classes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Ranker {
    private double N;
    private int avgdl;
    private double k1;
    private double b;

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
            for(int i = 0; i <queryInfo.size() - 2; i = i+3){
                // Score(D,Q) -- BM25
                // log(N/dfi)
                dfi = Integer.parseInt(queryInfo.get(i + 2));
                tfi = Integer.parseInt(queryInfo.get(i + 1));
                IDF =  (Math.log((this.N / dfi)) / Math.log(2));
                numerator =  tfi * (this.k1 +1);
                /// ?????????? |D| ???
//                denominator = tfi + (this.k1) * (1 - this.b + (this.b * ()));
                score = score + IDF*(numerator/ denominator);
            }
            bm25Result.put(docId, score);

        }// while END
        return null;
    }
}
