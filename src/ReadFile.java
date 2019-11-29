import sun.awt.Mutex;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ReadFile extends Thread{
    // Fields
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    private ArrayList<byte[]> allFiles;

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
        if (files.listFiles() == null) {
            return;
        }

        try  {
            Stream<Path> paths = Files.walk(Paths.get(path));
            Path[] filesPaths = paths.filter(Files::isRegularFile).toArray(Path[]::new);
           // Future<?> future = null;
                for(Path fileP : filesPaths) {

                    String str = fileIntoString(new File(fileP.toString()));
                    String str2 = fileP.toString();
                  //  future = threadPool.submit( () -> {
                    separatedFilesToArrayList(str, str2);
                   // });
                }

           // future.get();
           // threadPool.shutdown();

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
            writeDocName(matcherFileContent.group(), Paths.get(pathDirectory).getFileName().toString());
            this.allFiles.add(matcherFileContent.group().getBytes(StandardCharsets.US_ASCII));
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

    public ArrayList<byte[]> getListAllDocs() {
        return allFiles;
    }

}
