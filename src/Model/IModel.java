package Model;

import javafx.scene.control.TextField;

import java.io.File;
/**
 * Created by Mohsen Abdalla & Evgeny Umansky. December 2019.
 */


public interface IModel {
    void loadDictionary(File file);

    void showDictionary();

    void resetProcess(TextField posting_text);

    boolean startIndexing(boolean selected, TextField corpus_text, TextField posting_text);
}
