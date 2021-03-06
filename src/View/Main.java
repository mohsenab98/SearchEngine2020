package View;

import Model.MyModel;
import ViewModel.MyViewModel;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.File;

import static javafx.scene.control.Alert.AlertType.CONFIRMATION;

/**
 * Information Retrieval - Search Engine
 * Created by Mohsen Abdalla & Evgeny Umansky. December 2019.
 */

public class Main extends Application {
    MyViewController mv;
    MyModel model;
    MyViewModel viewModel;

    @Override
    public void start(Stage primaryStage) throws Exception {
        mv = new MyViewController();
        //---------------
        model = new MyModel();
        viewModel = new MyViewModel(model);
        model.addObserver(viewModel);
        //--------------
        primaryStage.setTitle("Search Engine - Information Retrieval 2020");
        FXMLLoader fxmlLoader =  new FXMLLoader(Main.class.getResource("view.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root, 1100, 600);
//        scene.getStylesheets().add(getClass().getResource("/View/view.css").toExternalForm());

        primaryStage.setScene(scene);
        File file = new File("resources/glassSearch.png");
        Image image = new Image(file.toURI().toString());

        //--------------
        MyViewController view = fxmlLoader.getController();
        view.setViewModel(viewModel);
        view.setImage(image); // add image
//        view.add(new ImageView(image));
        viewModel.addObserver(view);
        //--------------
        SetStageCloseEvent(primaryStage);

        primaryStage.show();
    }

    private void SetStageCloseEvent(Stage primaryStage) {

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent windowEvent) {
                Alert alert = new Alert(CONFIRMATION);
                alert.setContentText("Are you sure you want to exit the game ? Press OK to EXIT!");
                alert.showAndWait();

                if (alert.getResult() == ButtonType.CANCEL) {
                    windowEvent.consume();
                }
                else{
//                    viewModel.exitProgram();
                }
            }
        });
    }


    public static void main(String[] args) {
        launch(args);
    }
}
