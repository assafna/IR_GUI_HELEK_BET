package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static Time time = new Time();

    public static void main(String[] args) {
        //set launch start time
        time.setLaunchStartTime(System.currentTimeMillis());
        System.out.println("App launched");

        //start
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("MainWindow.fxml"));
        primaryStage.setTitle("Moogle");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

}