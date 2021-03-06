package codesourse;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import nu.pattern.OpenCV;
import org.opencv.core.Core;

import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource("root.fxml")));
        primaryStage.setTitle("Money Recogniser");
        primaryStage.setScene(new Scene(root, 577, 496));
        primaryStage.show();
    }


    public static void main(String[] args) {
        OpenCV.loadShared();
        launch(args);
    }
}
