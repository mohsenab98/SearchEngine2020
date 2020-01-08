package Model;

import javafx.scene.control.TextField;

import java.io.File;
import java.util.List;

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

    void runQuery(String textQuery, boolean stem, boolean semantic);

    List<String> getDocEntitiesFromSearcher(int docId);
}
