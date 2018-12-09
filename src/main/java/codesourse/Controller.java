package codesourse;

import codesourse.recognition.ImageRecognition;
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
    @FXML
    private ImageView outputImageView;

    private Image imageForRecognising;

    //Исходное изображение денежки, исправь хардкод какнить
    private String sourceFilename = "D:\\Money images\\sourceImage.jpg";
    private String ContourFilename;
    private String RotatedFilename;
    private String CroppedFilename;

    public void initialize() {

    }

    public void mouseDragDropped(final DragEvent e) {

        final Dragboard db = e.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            success = true;
            // Only get the first file from the list
            final File file = db.getFiles().get(0);
            Platform.runLater(() -> {

                System.out.println(file.getAbsolutePath());
                try {
                    imageForRecognising = new Image(new FileInputStream(file.getAbsolutePath()));
                    recogniseImageView.setImage(imageForRecognising);

                } catch (FileNotFoundException ex) {
                    System.out.println(ex.getMessage());
                }
            });
        }

        //вот я добавил это
        ImageRecognition imageRecognition = new ImageRecognition(sourceFilename);
        ContourFilename = imageRecognition.FindContour();
        RotatedFilename = imageRecognition.RotateImage();
        CroppedFilename = imageRecognition.CropImage();

        File file = new File(CroppedFilename);
        outputImageView.setImage(new Image((file.toURI().toString())));


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

    public void mouseDragExited() {
        rootGridPane.setStyle("-fx-border-color: #C6C6C6;");
    }


}
