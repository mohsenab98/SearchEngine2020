package Classes;

import java.util.ArrayList;
import java.util.Map;

public class Ranker {
    private int N;
    private int avgdl;

    public Ranker(int n, int avgdl) {
        N = n;
        this.avgdl = avgdl;
    }



    /**
     * gets Doc(key) -- (qi - tfi - dfi)(value)
     * @param docQ
     * @return docID - score
     */
    public Map<Integer, Integer> rankBM25(Map<Integer, ArrayList<String>> docQ) {

        return null;
    }
}
