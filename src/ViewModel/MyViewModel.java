package ViewModel;

import Model.IModel;
import javafx.scene.control.TextField;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by Mohsen Abdalla & Evgeny Umansky. December 2019.
 */

public class MyViewModel extends Observable implements Observer {
    private  IModel model;

    public MyViewModel(IModel model){
        this.model = model;
    }

    @Override
    public void update(Observable o, Object arg) {

    }

    public void loadDictionary(File file) {
        model.loadDictionary(file);
    }

    public void resetProcess(TextField posting_text){
        model.resetProcess(posting_text);
    }

    public boolean startIndexing(boolean selected, TextField corpus_text, TextField posting_text) {
        return model.startIndexing(selected, corpus_text, posting_text);
    }

    public double getTimeForIndexing() {
        return model.getTimeForIndexing();
    }

    public int getDocCounter() {
        return model.getDocCounter();
    }


    public int getNumberOfTerms() {
        return model.getNumberOfTerms();
    }

    ///////////// 2nd Part ///////////////////////////


    public Map<String, String> runQuery(String textQuery, boolean stem, boolean semantic, String posting) {
        return model.runQuery(textQuery, stem, semantic, posting);
    }

    public List<String> getDocEntitiesFromSearcher(int docId) {
        return model.getDocEntitiesFromSearcher(docId);
    }

    public Map<String, String> runQueryFile(String text, boolean stem, boolean semantic, String posting) {
        return model.runQueryFile(text, stem, semantic, posting);
    }
}
