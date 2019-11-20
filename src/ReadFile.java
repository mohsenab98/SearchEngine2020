import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadFile {
    // Fields
    private String path;

    // Constructor
    public ReadFile(String path){
        this.path = path;
    }

    /**
     * Envelope function for FilesSeparator(String path)
     */
    public void filesSeparator(){
        filesSeparator(this.path);
    }

    /**
     * Separate files and parse terms(?)
     * @param path
     */
    private void filesSeparator(String path){
        File files = new File(path);

        if(files.listFiles() != null) {
            for (File file : files.listFiles()) {
                if(file.isDirectory()){
                    filesSeparator(file.getPath());
                }
                else{
                    String fileString = fileIntoString(file);
                    String parentDirectoryPath = file.getParent();
                    Map<String, String> mapFilesNumberContent = separatedFilesToStringMap(fileString);

                    splitFiles( mapFilesNumberContent, parentDirectoryPath );
                }
            }
        }

    }

    /**
     * Create string from all content of a file
     *
     * @param file
     * @return strFile
     */
    private String fileIntoString(File file){
        String strFile = "";

        try{
            strFile = new String ( Files.readAllBytes( Paths.get(file.getPath()) ) );
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return strFile;
    }

    /**
     * Separate articles from all content string to dictionary(HashMap).
     * Key: number(id) of article; Value: content of article
     * @param fileString
     * @return mapFilesNumberContent
     */
    private Map<String, String> separatedFilesToStringMap(String fileString){
        Map<String, String> mapFilesNumberContent = new HashMap<>();
        Pattern patternFileNumber = Pattern.compile("<DOCNO>\\s*([^<]+?)\\s*</DOCNO>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcherFileNumber = patternFileNumber.matcher(fileString);
        Pattern patternFileContent = Pattern.compile("<DOC>.+?</DOC>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcherFileContent = patternFileContent.matcher(fileString);

        while (matcherFileNumber.find() && matcherFileContent.find()){
            mapFilesNumberContent.put(matcherFileNumber.group(1), matcherFileContent.group());
        }

        return mapFilesNumberContent;
    }

    /**
     * Create article files from the dictionary to every article in the directory of a source file
     * @param mapFiles
     * @param parentPath
     */
    private void splitFiles(Map<String, String> mapFiles, String parentPath){
        Iterator<Map.Entry<String, String>> itr = mapFiles.entrySet().iterator();

        while(itr.hasNext()) {
            Map.Entry<String, String> entry = itr.next();
            try {
                OutputStream os = new FileOutputStream( new File(parentPath + "/" + entry.getKey()) );

                /////
               // Parser(entry.getKey(), entry.getValue());
                /////

                os.write(entry.getValue().getBytes(), 0, entry.getValue().length());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * What to do:
     * Send 'text' to parse class and create terms from the 'text', we have to save the term in a file in the corpus.
     * Need to create some data structure for terms
     * Create 'term' as field in ReadFile and Parse
     * Add tag <TERM></TERM> into the files
     * @param fileName
     * @param content
     */
    private void parser(String fileName, String content){
        Pattern patternText = Pattern.compile("<TEXT>(.+?)</TEXT>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcherText = patternText.matcher(content);
        String text = matcherText.group(1);

        Parse parse = new Parse();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void printPath(){
        System.out.println(this.path);
    }


}
