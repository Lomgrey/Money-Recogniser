package codesourse;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;


public class Controller {

    @FXML
    private GridPane rootGridPane;
    @FXML
    private ImageView recogniseImageView;


    public void initialize(){

    }

    public void mouseDragDropped(final DragEvent e) {
        final Dragboard db = e.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            success = true;
            // Only get the first file from the list
            final File file = db.getFiles().get(0);
            Platform.runLater( () -> {
                System.out.println(file.getAbsolutePath());
                try {
                    Image img = new Image(new FileInputStream(file.getAbsolutePath()));
                    recogniseImageView.setImage(img);

                } catch (FileNotFoundException ex) {
                    System.out.println(ex.getMessage());
                }
            });
        }
        e.setDropCompleted(success);
        e.consume();
    }

    public void mouseDragOver(final DragEvent e) {
        final Dragboard db = e.getDragboard();

        final boolean isAccepted = db.getFiles().get(0).getName().toLowerCase().endsWith(".png")
                || db.getFiles().get(0).getName().toLowerCase().endsWith(".jpeg")
                || db.getFiles().get(0).getName().toLowerCase().endsWith(".jpg");

        if (db.hasFiles()) {
            if (isAccepted) {
                rootGridPane.setStyle("-fx-border-color: red;"
                        + "-fx-border-width: 5;"
                        + "-fx-background-color: #C6C6C6;"
                        + "-fx-border-style: solid;");
                e.acceptTransferModes(TransferMode.COPY);
            }
        } else {
            e.consume();
        }
    }

    public void mouseDragExited(){
        rootGridPane.setStyle("-fx-border-color: #C6C6C6;");
    }
}
