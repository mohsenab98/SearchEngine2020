package Classes;

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
        this.k1 = 1.2;
        this.b = 0.7;
        this.rawNarrative = rawNarrative;
    }



    /**
     * gets Doc(key) -- (qi - tfi - dfi)(value)
     * @param docTermInfo
     * @return docID - score
     */
    public Map<String, String> rankBM25(Map<String, String> docTermInfo, Map<String, String> docEntities, Map<String, String> docTitles) {

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
                int number =  valueUpBy(docTitles, docId, termsInfo[i + 3]);
                //System.out.println(number);
                //log(N/dfi)
                IDF =  (Math.log((this.N / dfi)) / Math.log(2));

                numerator =  tfi * (this.k1 + 1);
                denominator = tfi + (this.k1) * (1 - this.b + (this.b * (total/this.avgdl)));

                score = score + IDF * (numerator / denominator) * number;
            }
            bm25Result.put(docId, String.valueOf(score));

        }
        return bm25Result;
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

    private String[] getRelevant() {
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

        return new String[]{relevant.replaceAll("[!.,?/'\";:-]", " "), notRelevant.replaceAll("[!.,?/'\";:-]", " ")};
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
