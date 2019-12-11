import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ReadFile{
    // Fields
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    /**
     * content of all docs
     */
    private List<byte[]> allFiles;


    // Constructor
    public ReadFile(){
        this.allFiles = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Separate files
     * @param path - path of corpus
     */
    public void filesSeparator(String path){
        // create file from path of corpus
        File files = new File(path);
        if (files.listFiles() == null) {
            return;
        }

        try  {
            // create paths of docs
            Stream<Path> paths = Files.walk(Paths.get(path));
            Path[] filesPaths = paths.filter(Files::isRegularFile).toArray(Path[]::new);

            // separate docs by paths
            for( Path fileP :  filesPaths) {
                String strFiles = fileIntoString(new File(fileP.toString()));
                String strFilePath = fileP.toString();
                threadPool.execute(() -> {
                    separatedFilesToArrayList(strFiles, strFilePath); // separate docs to list: doc per index
                });
            }

            threadPool.awaitTermination(1, TimeUnit.SECONDS); // Wait for ending of threads
            threadPool.shutdown(); // stop thread pool

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
    private void separatedFilesToArrayList(String fileString, String pathDirectory){
        // Content

        Pattern patternFileContent = Pattern.compile("<DOC>.+?</DOC>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcherFileContent = patternFileContent.matcher(fileString);
        while (matcherFileContent.find()) {
            String content = matcherFileContent.group();
            content = writeDocName(content, Paths.get(pathDirectory).getFileName().toString());
            this.allFiles.add(content.getBytes(StandardCharsets.US_ASCII));
        }
    }

    /**
     * Save docs's folder's name to tag <DOCNO></DOCNO>
     * @param content
     * @param pathDirectory
     * @return
     */
    private String writeDocName(String content, String pathDirectory){
        Pattern patternFileContent = Pattern.compile("<DOCNO>\\s*([^<]+?)\\s*</DOCNO>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcherFileContent = patternFileContent.matcher(content);
        while (matcherFileContent.find()){
            if(!matcherFileContent.group(1).contains(pathDirectory)) {
                content = content.replaceAll("<DOCNO>[^<]+?</DOCNO>", "<DOCNO>" + pathDirectory + "-" + matcherFileContent.group(1) + "</DOCNO>");
            }
        }
        return content;
    }

    // getter for docs
    public List<byte[]> getListAllDocs() {
        return allFiles;
    }

}
