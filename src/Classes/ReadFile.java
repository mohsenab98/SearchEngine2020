package Classes;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ReadFile{
    // Fields
    /**
     * content of all docs
     */
    private List<byte[]> allFiles;
    private String path;

    // Constructor
    public ReadFile(String corpusPath){
        this.path = corpusPath;
        this.allFiles = new ArrayList<>();
    }

    /**
     * Separate files
     *
     */
    public void filesSeparator(){
        // create file from path of corpus
        File files = new File(this.path);
        if (files.listFiles() == null) {
            return;
        }

        try  {
            // create paths of docs
            Stream<Path> paths = Files.walk(Paths.get(this.path));
            Path[] filesPaths = paths.filter(Files::isRegularFile).toArray(Path[]::new);

            // separate docs by paths
            for( Path fileP :  filesPaths) {
                String strFiles = fileIntoString(new File(fileP.toString()));
                separatedFilesToArrayList(strFiles); // separate docs to list: doc per index
            }
        }
        catch (Exception e){
            e.printStackTrace();
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
    private void separatedFilesToArrayList(String fileString){
        // Content

        Pattern patternFileContent = Pattern.compile("<DOC>.+?</DOC>",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcherFileContent = patternFileContent.matcher(fileString);
        while (matcherFileContent.find()) {
            String content = matcherFileContent.group();
            this.allFiles.add(content.getBytes(StandardCharsets.US_ASCII));
        }
    }

    // getter for docs
    public List<byte[]> getListAllDocs() {
        return allFiles;
    }

}
