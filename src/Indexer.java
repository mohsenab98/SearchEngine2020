import java.io.*;
import java.util.*;

public class Indexer {
    /**
     * will Save the terms of the index and will save a pointer to the match posting file
     */
    private Map<String, String> mapTermPosting;
    /**
     * will save the term and its info for each doc in a sorted map (A-Z)
     */
    private SortedMap<String, ArrayList<String>> mapSortedTerms ;
    private String pathCorpus;
    /**
     * isStem variable that we git from the user ( the parse send it to here)
     */
    private boolean isStem;
    /**
     * number of docs that we want to index every time (נוסחה???)
     */
    private int numberOfDocs;
    /**
     * check the number of docs (LIMIT is numberOfDocs)
     */
    private int docCounter;
    /**
     * name of the temp file
     */
    private static int tempPostCounter = 0;

    public Indexer(String pathCorpus, boolean isStem, int numberOfDocs) {
        this.mapTermPosting = new LinkedHashMap<>();
        this.pathCorpus = pathCorpus;
        this.mapSortedTerms = new TreeMap<>();
        this.isStem = isStem;
        this.numberOfDocs = numberOfDocs;
        this.docCounter = 0;
        postingFilesCreate(pathCorpus);
    }

    /**
     * get the term and its related information from the parser
     * send the info for the posting files
     * @param termDoc , map that contain the term and list{DOCID , pos1 , pos2...}
     */
    public void addTermToIndexer(Map<String, ArrayList<String>>termDoc){
        if(docCounter > numberOfDocs){
            // intilazte the mapSorted / counter | update mapPosting
            reset(termDoc);
        }
        for (String key : termDoc.keySet()) {
            if(this.mapSortedTerms.containsKey(key)){
                //chain the new list of term to the original one
                ArrayList<String> listOfInfo = termDoc.get(key);
                ArrayList<String> originalList = mapSortedTerms.get(key);
                originalList.addAll(listOfInfo); // chain new list to the original (boolean)
                mapSortedTerms.put(key, originalList);

            }else{
                //Add new term and it list of info to the Sorted map
                ArrayList<String> listOfInfo = termDoc.get(key);
                mapSortedTerms.put(key, listOfInfo);
            }
        }
        docCounter++;

    }

    /**
     * delete the mapSorted data
     * docCounter = 0
     * tempPostCounter++
     * update the mapPosting
     * write the data to the posting file
     */
    private void reset(Map<String, ArrayList<String>> termDoc){
        String textToPostFile = "";
        this.docCounter = 0;

        for (String key : mapSortedTerms.keySet()) {
            textToPostFile = textToPostFile +"<" + key + "|" + textForPosting(mapSortedTerms.get(key)) + ">" +"\n";
            mapTermPosting.put(key, String.valueOf(tempPostCounter));
        }
        mapSortedTerms.clear();
        usingBufferedWritter(textToPostFile, tempPostCounter);

        tempPostCounter++;

        addTermToIndexer(termDoc); // return to add the doc info to the new Indexer

    }

    /**
     * func that turn the info of the term into string so we can write it to the posting file
     * @param listOfInfo
     * @return
     */
    private String textForPosting(ArrayList<String> listOfInfo){
        String textToAdd = "";
        textToAdd = textToAdd + listOfInfo.get(0) + ":"; // DOCID
        textToAdd = textToAdd + listOfInfo.get(1) + "?"; // tfi
        String temp = "";
        for (int i = 2; i < listOfInfo.size(); i++){
            temp = listOfInfo.get(i);
            //!isNumeric(temp)
            if((temp.charAt(0) >= 'a' && temp.charAt(0) <= 'z') || (temp.charAt(0) >= 'A' && temp.charAt(0) <= 'Z')){
                textToAdd = ";" + textToAdd + temp + ":"; // DOCID 2 +
                    i++;
                    textToAdd = textToAdd + listOfInfo.get(i) + "?"; // tfi

            }else{
                textToAdd = textToAdd + temp + ",";
            }
        }
        if(textToAdd.endsWith(","))
        {
            textToAdd = textToAdd.substring(0,textToAdd.length() - 1);
            textToAdd = textToAdd + ";";
        }else{
            textToAdd = textToAdd + ";";
        }
        return textToAdd;
    }



    /**
     * append text to file
     * this function code was take from "https://howtodoinjava.com/java/io/java-append-to-file/"
     * @param text
     * @throws IOException
     */
    public void usingBufferedWritter(String text, int filename)
    {
        String stemFolder = "";
        if(isStem){
            stemFolder = "stem";
        }else {
            stemFolder = "nostem";
        }

        String fileUrl = this.pathCorpus + "/" + stemFolder + "/" + filename;
        File file =  new File(fileUrl);
        BufferedWriter writer = null;
        try {
            file.createNewFile();
            writer = new BufferedWriter(
                    new FileWriter(fileUrl, true)  //Set true for append mode
            );
            writer.write(text);
//            writer.newLine();   //Add new line
            writer.close();
        } catch (IOException e) {
            System.out.println(filename);
            e.printStackTrace();
        }


    }
    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(String.valueOf(str.charAt(0)));
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }


    /**
     * Get the number of the line that the new term is going to be in the posting
     * @param path
     * @return
     * @throws IOException
     */
    private int lineOfNewTerm(String path) throws IOException {
        BufferedReader r = new BufferedReader(new FileReader(path));
        int i = 1;
        while (r.readLine() != null){
            i++;
        }
        return i;
    }

    /**
     * Creating two folders(with/without stemming) and in each folder we creat the posting files of the index
     * @param path
     */
    private void postingFilesCreate(String path){
        boolean folder1 = new File(path+"/stem").mkdir();
        boolean folder2 = new File(path+"/noStem").mkdir();
//        if(folder1 && folder2){
//            File fileStem = new File(path+"/stem/"+"Numbers");
//            try {
//                fileStem.createNewFile();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            for (int i = 'a'; i < 'z'; i++){
//                try {
//                     fileStem = new File(path+"/stem/"+(char)i);
//                    fileStem.createNewFile();
//                    File fileNoStem = new File(path+"/noStem/"+(char)i);
//                    fileNoStem.createNewFile();
////                    System.out.println("Empty File Created:- " + file.length());
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }

    }



}
