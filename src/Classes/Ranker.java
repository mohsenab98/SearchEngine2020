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
    public Map<String, String> rankBM25(Map<String, ArrayList<String>> docQ) {

        Map<String, String> bm25Result = new HashMap<>();
        Iterator<Map.Entry<String, ArrayList<String>>> it = docQ.entrySet().iterator();
        while (it.hasNext()) {

            Map.Entry<String, ArrayList<String>> pair = it.next();
            String docId = pair.getKey();
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
            bm25Result.put(docId, String.valueOf(score));

        }// while END
        return bm25Result;
    }

    public ArrayList<String> LSA(String term, String narrDescription){
        ArrayList<String> synonyms = new ArrayList<>();
        WS4JConfiguration.getInstance().setMFS(true);
        String[] queryDescrNarr = narrDescription.split(" ");
        for(String synonym : queryDescrNarr) {
            double distance = new WuPalmer(db).calcRelatednessOfWords(term, synonym.trim());
            if(distance >= 0.85){
                synonyms.add(synonym.trim());
            }
        }

        return synonyms;
    }
}
