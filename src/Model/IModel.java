package Model;

import javafx.scene.control.TextField;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created by Mohsen Abdalla & Evgeny Umansky. December 2019.
 */


public interface IModel {
    void loadDictionary(File file);

    void resetProcess(TextField posting_text);

    boolean startIndexing(boolean selected, TextField corpus_text, TextField posting_text);

    double getTimeForIndexing();

    int getDocCounter();

    int getNumberOfTerms();

    Map<String, Map<String, String>> runQuery(String textQuery, boolean stem, boolean semantic,String posting);

    Map<String, Map<String, String>> runQueryFile(String text, boolean stem, boolean semantic, String Posting);
}
