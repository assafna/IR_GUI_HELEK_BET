package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

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

    public static void wikipedia(){
        String subject = "Ed Sheeran";
        try {
            URL url = new URL("https://en.wikipedia.org/w/index.php?action=raw&title=" + subject.replace(" ", "_"));
            String text = "";
            try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()))) {
                String line = null;
                while (null != (line = br.readLine())) {
                    line = line.trim();
                    if (!line.startsWith("|")
                            && !line.startsWith("{")
                            && !line.startsWith("}")
                            && !line.startsWith("<center>")
                            && !line.startsWith("---")) {
                        text += line;
                    }
                    if (text.length() > 200) {
                        break;
                    }
                }
            }
            System.out.println("text = " + text);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

}