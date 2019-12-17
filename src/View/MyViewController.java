package View;

import ViewModel.MyViewModel;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;

import java.io.File;

import java.net.URL;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView ;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * Created by Mohsen Abdalla & Evgeny Umansky. December 2019.
 */


public class MyViewController extends Canvas implements Observer {
    @FXML public ImageView image_view;
    @FXML public Button start_button;
    @FXML public CheckBox stem;
    @FXML public Button  corpus_button;
    @FXML public Button  posting_button;
    @FXML public TextField corpus_text;
    @FXML public TextField posting_text;
    @FXML public Button  reset_button;
    @FXML public Button  show_Dic_button;
    @FXML public Button  load_Dic_button;
    private MyViewModel viewModel;
    static boolean isnewWindow = false;

    /**
     * opens new window when the indexing process is over and print:
     * 1.Number of docs
     * 2.Time that takes for indexing
     * 3.Number of terms in our dictionary
     * @param actionEvent
     */
    private void newWindow(ActionEvent actionEvent){
        if(!isnewWindow) {
            isnewWindow = true;
            start_button.setDisable(true);
            try {
                Stage stage = new Stage();
                Label l =  new Label();
                l.setText("Number of Docs that has indexed:  " +viewModel.getDocCounter()+"\n"+ "\n" +
                        "The time that it tooks:  " +viewModel.getTimeForIndexing() + " minutes"
                        +"\n"+ "\n" + "Numbers of terms: " + viewModel.getNumberOfTerms());
                stage.setTitle("Indexing Info!");

                Scene scene = new Scene(l, 600, 400);
                stage.setScene(scene);
                SetStageCloseEvent(stage);
                stage.show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * when we close the new window it reopen the start button
     * @param stage
     */
    private void SetStageCloseEvent(Stage stage) {
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent windowEvent) {
               start_button.setDisable(false);
                isnewWindow = false;
            }
        });
    }
    @Override
    public void update(Observable o, Object arg) {

    }

    /**
     * set the view model
     * @param viewModel
     */
    public void setViewModel(MyViewModel viewModel) {
        this.viewModel = viewModel;

        // when start button is pressed start indexing
        start_button.setOnAction(e ->{
            File file1 = new File(corpus_text.getText());
            File file2 = new File(posting_text.getText());
            if(file1.exists() && file2.exists()) {
                start_button.setDisable(true);
                if(viewModel.startIndexing(stem.isSelected(), corpus_text, posting_text)){
                    ActionEvent event = new ActionEvent();
                    newWindow(event);
                }

            }else{
                showAlert("Enter valid locations !!");
            }
        });

    }

    /**
     * show the content of the message as an alert message to the user
     * @param alertMessage
     */
    private void showAlert(String alertMessage) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(alertMessage);
        alert.show();

    }

    /**
     * when reset button is pressed
     * @param actionEvent
     */
    public void resetProcess(ActionEvent actionEvent) {
        viewModel.resetProcess(posting_text);
    }
    /**
     * when reset show Dictionary is pressed
     * @param actionEvent
     */
    public void showDictionary(ActionEvent actionEvent) {
        viewModel.showDictionary(posting_text, stem.isSelected());

    }
    /**
     * load the text field of the posting path
     * @param actionEvent
     */
    public void loadDirectory2(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File postingDirectory = directoryChooser.showDialog(new Stage());
        if(postingDirectory != null) {
             posting_text.clear();
             posting_text.appendText(postingDirectory.getPath());

        }
    }
    /**
     * load the text field of the corpus path
     * @param actionEvent
     */
    public void loadDirectory1(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File corpusDirectory = directoryChooser.showDialog(new Stage());
        if(corpusDirectory != null) {
            corpus_text.clear();
            corpus_text.appendText(corpusDirectory.getPath());

        }
    }



    /**
     * load the the dictionary file that the user choose and load it to the main memory
     * @param actionEvent
     */
    public void loadFile(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Load Dictionary");
        File file = fc.showOpenDialog(new Stage());
        if(file != null) {
            viewModel.loadDictionary(file);
        }
    }
    /**
     * load the image that show on our main window
     * @param image
     */
    public void setImage(Image image) {
        image_view.setImage(image);
    }
}
