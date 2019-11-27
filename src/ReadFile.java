import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadFile {
    // Fields
    private ArrayList<String> allFiles;

    // Constructor
    public ReadFile(){
        this.allFiles = new ArrayList<>();
    }

    /**
     * Separate files and parse terms(?)
     * @param path
     */
    public void filesSeparator(String path){
        File files = new File(path);

        if(files.listFiles() == null) {
            return;
        }

        for (File file : files.listFiles()) {
            if(file.isDirectory()){
                filesSeparator(file.getPath());
            }
            else{
                separatedFilesToArrayList(fileIntoString(file), file.getParent());
            }
        }

    }

    /**
     * Create string from all content of a file
     * @param file
     * @return strFile
     */
    private String fileIntoString(File file){
        String fileString = "";

        try{
            fileString = new String ( Files.readAllBytes( Paths.get(file.getPath()) ) );
        }
        catch (Exception e){
            System.out.println(file.getPath());
            e.printStackTrace();
        }

        return fileString;
    }

    /**
     * Separate articles from all content string to array list.
     * @param fileString
     * @return mapFilesNumberContent
     */
    private void separatedFilesToArrayList(String fileString, String pathDirectory){
        // Content
        Pattern patternFileContent = Pattern.compile("<DOC>.+?</DOC>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcherFileContent = patternFileContent.matcher(fileString);
        while (matcherFileContent.find()){
            writeDocName(matcherFileContent.group(), Paths.get(pathDirectory).getFileName().toString());
            this.allFiles.add(matcherFileContent.group());
        }
    }

    private void writeDocName(String content, String pathDirectory){
        Pattern patternFileContent = Pattern.compile("<DOCNO>\\s*([^<]+?)\\s*</DOCNO>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcherFileContent = patternFileContent.matcher(content);
        while (matcherFileContent.find()){
            if(!matcherFileContent.group(1).contains(pathDirectory)) {
                content = content.replaceAll("<DOCNO>[^<]+?</DOCNO>", "<DOCNO>" + pathDirectory + "-" + matcherFileContent.group(1) + "</DOCNO>");
            }
        }
    }

    public ArrayList<String> getListAllDocs() {
        return allFiles;
    }


}
