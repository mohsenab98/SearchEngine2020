package View;

import ViewModel.MyViewModel;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import java.io.*;
import java.util.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView ;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

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
                l.setText("Number of Docs that has been indexed:  " +viewModel.getDocCounter()+"\n"+ "\n" +
                        "Time for indexing the corpus:  " +viewModel.getTimeForIndexing() + " minutes"
                        +"\n"+ "\n" + "Numbers of terms: " + viewModel.getNumberOfTerms());
                stage.setTitle("Indexing Results!");

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
     * when we close the new window:
     * 1. reShow the start button
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
     * 1. set the view model
     * 2. start the indexing if the start button is pressed
     * @param viewModel
     */
    public void setViewModel(MyViewModel viewModel) {
        this.viewModel = viewModel;
        // when start button is pressed start indexing
        start_button.setOnAction(e ->{
            //if the path given by the user is right
            if(isTruePath()) {
                start_button.setDisable(true);
                //if the indexing process has done
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
     *
     * @return true if the path of posting and corpus is good
     */
    private boolean isTruePath(){
        File file1 = new File(corpus_text.getText());
        File file2 = new File(posting_text.getText());
        if(file1.exists() && file2.exists()) {
            return true;
        }else{
            return false;
        }
    }

    /**
     * when reset button is pressed
     * @param actionEvent
     */
    public void resetProcess(ActionEvent actionEvent) {
        if(!isTruePath()){
            return;
        }
        viewModel.resetProcess(posting_text);
    }

    /**
     * when show Dictionary button is pressed
     * using JTable to show the dictionary in new window as a table
     * @param actionEvent
     */
    public void showDictionary(ActionEvent actionEvent) {
        try {
            String stemFolder = "";
            if(stem.isSelected()){
                stemFolder = "stem";
            }else{
                stemFolder = "noStem";
            }
            File file = new File (posting_text.getText() + "\\" + stemFolder + "\\Dictionary");
            //checks if the path gives is right and checks if there is a dictionary inside the posting path given
            if(!isTruePath() || !file.exists()){
                return;
            }
            Map<String,String> mapDictionary = new LinkedHashMap<>();
            BufferedReader reader = null;
            try {
                reader = new BufferedReader( new FileReader( file ));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            String line = null;
            while(true){
                try {
                    if (!((line = reader.readLine()) != null)) break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String[] arr = line.split( "\\|" );
                mapDictionary.put( arr[0], arr[1].split(":")[0]);
            }
            JTable table=new JTable(toTableModel(mapDictionary)); //receiving the table from toTbleModel function
            JFrame frame=new JFrame();
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);//set closing behavior
            frame.add(new JScrollPane(table)); // adding scrollbar
            frame.setSize(400,600);
            frame.setLocationRelativeTo(null);//center the jframe
            frame.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * table that represent the dictionary
     * Source : "https://stackoverflow.com/questions/2257309/how-to-use-hashmap-with-jtable"
     * @param map
     * @return table model
     */
    public static TableModel toTableModel(Map<String,String> map) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[] { "Term", "Total Appearance" }, 0
        );
        for (Map.Entry<String,String> entry : map.entrySet()) {
            model.addRow(new Object[] { entry.getKey(), entry.getValue() });
        }
        return model;
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
     * load the the dictionary file that located in the stem/noStem folder and load it to the main memory
     * @param actionEvent
     */
    public void loadFile(ActionEvent actionEvent) {
        if(posting_text.getText().equals("")){
            return;
        }
        String stemFolder = "";
        if(stem.isSelected()){
            stemFolder = "stem";
        }else{
            stemFolder = "noStem";
        }
        File file = new File(posting_text.getText() + "/" + stemFolder + "/Dictionary");
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
