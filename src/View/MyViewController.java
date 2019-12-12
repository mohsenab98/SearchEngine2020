package View;

import ViewModel.MyViewModel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;

import java.io.File;

import java.net.URL;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView ;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Created by Mohsen Abdalla & Evgeny Umansky. December 2019.
 */


public class MyViewController extends Canvas implements Observer, Initializable {
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

    @Override
    public void update(Observable o, Object arg) {

    }

    public void setViewModel(MyViewModel viewModel) {
        this.viewModel = viewModel;

        start_button.setOnAction(e ->{
            File file1 = new File(corpus_text.getText());
            File file2 = new File(posting_text.getText());
            if(file1.exists() && file2.exists()) {
                start_button.setDisable(true);
                viewModel.startIndexing(stem.isSelected(), corpus_text, posting_text);

            }else{
                showAlert("Enter valid locations !!");
            }
        });

    }

    private void showAlert(String alertMessage) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(alertMessage);
        alert.show();

    }


    public void setStart_button() {
        this.start_button.setDisable(false);
    }

    public void resetProcess(ActionEvent actionEvent) {
        viewModel.resetProcess(posting_text);
    }

    public void showDictionary(ActionEvent actionEvent) {
        viewModel.showDictionary();

    }

    public void loadDirectory2(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(new Stage());
        // file == Dictionary
        if(selectedDirectory != null) {
             viewModel.loadDictionary(selectedDirectory);
             posting_text.clear();
             posting_text.appendText(selectedDirectory.getPath());

        }
//        mazeWindow(actionEvent, maze);
    }

    public void loadDirectory1(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(new Stage());
        // file == Dictionary
        if(selectedDirectory != null) {
            viewModel.loadDictionary(selectedDirectory);
            corpus_text.clear();
            corpus_text.appendText(selectedDirectory.getPath());

        }
//        mazeWindow(actionEvent, maze);
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        File file = new File("src/resources/glassSearch.png");
        Image image = new Image(file.toURI().toString());
        image_view.setImage(image);
    }

    public void start(ActionEvent actionEvent) {
        if( corpus_text.getText() != "" && posting_text.getText() != null) {
        }

    }


    public void loadFile(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Load Dictionary");
        File file = fc.showOpenDialog(new Stage());
        // file == Dictionary
        if(file != null) {
            viewModel.loadDictionary(file);
        }
//        mazeWindow(actionEvent, maze);
    }
}
