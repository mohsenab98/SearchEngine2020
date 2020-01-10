package View;

import Model.MyModel;
import ViewModel.MyViewModel;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;

import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;


import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView ;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javafx.scene.control.CheckBox;


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
                l.setText("Number of indexed Docs:  " + viewModel.getDocCounter() + "\n" + "\n" +
                        "Run Time of indexing the corpus:  " + viewModel.getTimeForIndexing() + " minutes"
                        + "\n" + "\n" + "Amount of terms: " + viewModel.getNumberOfTerms());
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
        if(!new File(posting_text.getText()).exists()){
            showAlert("The chosen posting path doesn't exist!");
            return;
        }
        if(!new File(posting_text.getText()+"/stem").exists()){
            showAlert("You have already deleted the posting files!");
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
        if(!posting_text.getText().equals("")) {
            if (new File(posting_text.getText()).exists()) {
                if (!MyModel.mapDictionary.isEmpty()) {
                    try {
                        String stemFolder = "";
                        if (stem.isSelected()) {
                            stemFolder = "stem";
                        } else {
                            stemFolder = "noStem";
                        }
                        File file = new File(posting_text.getText() + "\\" + stemFolder + "\\Dictionary");
                        //checks if the path gives is right and checks if there is a dictionary inside the posting path given
                        if (!file.exists()) {
                            return;
                        }
                        Map<String, String> mapDictionary = new LinkedHashMap<>();
                        BufferedReader reader = null;
                        try {
                            reader = new BufferedReader(new FileReader(file));
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        String line = null;
                        while (true) {
                            try {
                                if (!((line = reader.readLine()) != null)) break;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            String[] arr = line.split("\\|");
                            mapDictionary.put(arr[0], arr[1].split(":")[0]);
                        }

                        //show dictionary as table
                        JTable table = new JTable(toTableModel(mapDictionary)); //receiving the table from toTbleModel function
                        showTable(table);
                        return;

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        showAlert("PLEASE CHECK :\n"+
                "1-Enter valid location of the posting files!!\n"+
                "2-Load the dictionary\n"+
                "3-Wait up to 10 seconds to load the dictionary");

    }

    /**
     * show the dictionary in new windows as a table using Jfram & Jtable
     * @param table
     */
    private void showTable(JTable table){
//        JTable table=new JTable(toTableModel(result)); //receiving the table from toTbleModel function
        JFrame frame=new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);//set closing behavior
        frame.add(new JScrollPane(table)); // adding scrollbar
        frame.setSize(600,800);
        frame.setLocationRelativeTo(null);//center the jframe
        frame.setVisible(true);


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
        if(posting_text.getText().equals("") || !new File(posting_text.getText()).exists()){
            showAlert("PLEASE CHECK :\n"+
                    "1-Enter valid location of the posting files!! so the load process can be done! \n");
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

    // ------------------------------- Second Part Functions ---------------------------------------------

    @FXML public Button entities_button;
    @FXML public CheckBox semantic;
    @FXML public Button query_button;
    @FXML public Button chooseQuires_button;
    @FXML public TextField query_text;
    @FXML public TextField chooseQuires_text;
    Map<String, Map<String, String>> resultQuery;
    /**
     * Run one query button is pressed -> search for the most 50 relevant docs in that answers the query and show it to the user in new window
     * @param actionEvent
     */
    public void runQuery(ActionEvent actionEvent) {
        if(!query_text.getText().equals("")){
            if(new File(posting_text.getText()).exists()){
                if(!MyModel.mapDictionary.isEmpty()){
                    resultQuery = new LinkedHashMap<>();
                    resultQuery = viewModel.runQuery(query_text.getText(), stem.isSelected(), semantic.isSelected(), posting_text.getText());
                    JTable table=new JTable(toTableModelQuery(resultQuery)); //receiving the table from toTbleModel function
                    showQueryTable(table);
                    return;
                }
            }
        }
        showAlert("PLEASE CHECK :\n"+
                "1-Enter valid query !!\n"+
                "2-Enter valid location of the posting files!!\n"+
                "3-Load the dictionary");

    }
    /**
     * browse query file button is pressed -> select the wanted file
     * @param actionEvent
     */
    public void browseQueryFile(ActionEvent actionEvent) {
        //get the query results
        try {
            FileChooser FileChooser = new FileChooser();
            File queryFile = FileChooser.showOpenDialog(new Stage());
            chooseQuires_text.clear();
            chooseQuires_text.appendText(queryFile.getPath());

        }
        catch (Exception e){
//            e.printStackTrace();
            showAlert("Enter valid location of the query file !!");
            return;
        }

    }



    public void showEntities(ActionEvent actionEvent) {
        //MyModel.docEntities;


    }




    /**
     * Code was taken from : "https://stackoverflow.com/questions/17225988/how-to-add-jbutton-after-a-jtable"
     * Jfram with Jtable & Jbutton
     * @param table
     */
    private static boolean mouse = false;
    private void showQueryTable(JTable table) {
        JFrame frame = new JFrame(); // frame
        frame.setLayout(new BorderLayout());

        JPanel btnPnl = new JPanel(new BorderLayout());
        JPanel bottombtnPnl = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton button = new JButton("Save - Results");
        bottombtnPnl.add(button); // new button
        btnPnl.add(bottombtnPnl, BorderLayout.CENTER);

        frame.add(table.getTableHeader(), BorderLayout.NORTH);
        frame.add(table, BorderLayout.CENTER);
        frame.add(btnPnl, BorderLayout.SOUTH);
        frame.setSize(600,800);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(new JScrollPane(table));

        frame.pack();
        frame.setVisible(true);

        // Button is Selected? -> Save file
        button.addActionListener(e -> {
          saveResults(frame);
        });

        // row cell is selected? -> show entities of the selected doc row
        table.getSelectionModel().addListSelectionListener(event -> {
            // print first column value from selected row
            if(!mouse){
                Platform.runLater(() ->{
                    showAlert(MyModel.getDocEntities().get(table.getValueAt(table.getSelectedRow(), 2).toString()));
                });
                mouse = true;
            }else{
                mouse = false;
            }
        });
    }

    /**
     * opens file dialog infront of user to he can choose the dirictory and name of the file then the results is saved to file
     * @param frame
     */
    private void saveResults(JFrame frame){
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Specify a file to save");

        int userSelection = fileChooser.showSaveDialog(frame);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
//            String results = "Mohsen Da King";
            String results = mapToFormatString(resultQuery);
            System.out.println("Save as file: " + fileToSave.getAbsolutePath());
            BufferedWriter writer;
            try {
                writer = new BufferedWriter(
                        new FileWriter(fileToSave.getAbsolutePath()+".txt", true)  //Set true for append mode
                );
                writer.write(results);
                writer.close();
            } catch (IOException eo) {
                System.out.println(results);
                eo.printStackTrace();
            }
        }

    }

    private String mapToFormatString(Map<String, Map<String,String>> text){
        StringBuilder textToPostFile = new StringBuilder();
        for (String key : text.keySet()) {
            Map<String, String> tempMap = text.get(key);
            for (String keyDocId : tempMap.keySet()) {
                textToPostFile.append(key).append(" ").append("5").append(" ")
                        .append(keyDocId).append(" ").append("19").append(" ").append("11.3")
                        .append(" ").append("mt").append("\n");
            }
        }
        return textToPostFile.toString();
    }

    /**
     * table that represent the query info
     * Source : "https://stackoverflow.com/questions/2257309/how-to-use-hashmap-with-jtable"
     * @param map
     * @return table model
     */
    public static TableModel toTableModelQuery(Map<String, Map<String, String>> map) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[] { "#", "QueryNUM", "DocID", "Rank" }, 0
        );
        int counterNO = 1;
        for (Map.Entry<String, Map<String, String>> entryUserDocs : map.entrySet()) {
            Map<String, String> docRank = entryUserDocs.getValue();
            for(Map.Entry<String, String> entryDocRank : docRank.entrySet()) {
                model.addRow(new Object[]{counterNO, entryUserDocs.getKey(), entryDocRank.getKey(), entryDocRank.getValue()});
                counterNO++;
            }
        }
        return model;
    }

    /**
     * run query is pressed when user entered the path of the query file -> search for the most 50 relevant docs in that answers each query in the file and show it to the user in new window
     * @param actionEvent
     */
    public void runQueries(ActionEvent actionEvent) {
        if(!chooseQuires_text.getText().equals("")) {
            if (new File(posting_text.getText()).exists()) {
                if (!MyModel.mapDictionary.isEmpty()) {
                    resultQuery = viewModel.runQueryFile(chooseQuires_text.getText(), stem.isSelected(), semantic.isSelected(), posting_text.getText());
                    JTable table = new JTable(toTableModelQuery(resultQuery)); //receiving the table from toTbleModel function
                    showQueryTable(table);
                    return;
                }
            }
        }
        showAlert("PLEASE CHECK :\n"+
                "1-Enter valid location of the query file !!\n"+
                "2-Enter valid location of the posting files!!\n"+
                "3-Load the dictionary");

    }
}
