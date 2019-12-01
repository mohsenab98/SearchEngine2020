import java.util.*;

public class Indexer {
    private Map<String, Map<String, Integer>> mapDocsPerTerm;

    public Indexer() {
        this.mapDocsPerTerm = new LinkedHashMap<>();
    }

    public void addTermToIndexer(String term, String docName) {
        // add to the set of doc the docName

        if (mapDocsPerTerm.containsKey(term)) {
            Map<String, Integer> docMap = mapDocsPerTerm.get(term);
            if(docMap.containsKey(docName)){
                docMap.put(docName, docMap.get(docName)+1);
            }else{
                docMap.put(docName, 1);
            }
        }else{
            // creating new set if the term dosent exist
            Map<String, Integer> docMap = new LinkedHashMap<>();
            docMap.put(docName, 1);
            mapDocsPerTerm.put(term, docMap);
        }
    }

    public void addNameToIndexer(Map<String, Map<String, Integer>> names){
        mapDocsPerTerm.putAll(names);
    }

    public void addUpperCaseToIndexer(Set<String> strings){
        for (String upperCase : strings)
        {
            if(mapDocsPerTerm.containsKey(upperCase.toLowerCase())){
                Map<String, Integer> tempMapDocsPerTerm = mapDocsPerTerm.get(upperCase.toLowerCase());
                mapDocsPerTerm.remove(upperCase.toLowerCase());
                mapDocsPerTerm.put(upperCase, tempMapDocsPerTerm);
            }
        }
    }

}
