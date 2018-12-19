package codesourse;

import codesourse.recognition.ImageRecognition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Controller {

    @FXML
    private GridPane rootGridPane;
    @FXML
    private ImageView recogniseImageView;
    @FXML
    private Label infoLabel;

    private Image imageForRecognising;

    //Исходное изображение денежки, исправь хардкод какнить
    private File sourceFile;

//    private String sourceFiles = "D:\\Money images";
//    private final String initImagePath = sourceFiles.toString() + "/sourceImage.jpg";

    public void initialize() {

    }

    public void mouseDragDropped(final DragEvent e) {

        final Dragboard db = e.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            infoLabel.setVisible(false);
            success = true;
            // получаем только первое изображение
            sourceFile = db.getFiles().get(0);
            Platform.runLater(() -> {
                try {
                    imageForRecognising = new Image(new FileInputStream(sourceFile.getAbsolutePath()));
                    recogniseImageView.setImage(imageForRecognising);

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

    public void mouseDragExited() {
        rootGridPane.setStyle("-fx-border-color: #C6C6C6;");

        //вот я добавил это
        ImageRecognition imageRecognition = new ImageRecognition(sourceFile);
        String contourFilename = imageRecognition.FindContour();
        String croppedFilename = imageRecognition.CropImage();
        String normalizedFileName = imageRecognition.NormalizeImage();
        String croppedNominalFilename = imageRecognition.CropNominal();
        String nominalEdgesFilename = imageRecognition.NominalEdges();
        String templateMatchingFilename = imageRecognition.TemplateMatching();//вернет пустоту, если не распознан
        String nominal = imageRecognition.Nominal();// тут типа номинал

//        showIntermediateImages();

        if (templateMatchingFilename.equals("")) {
            System.out.println("Купюра не распознана");
            showAlert(Alert.AlertType.INFORMATION, "Купюра не распознана");
        }
        else {
            System.out.println("Ваша купюра номиналом " + nominal + " рублей");
            showAlert(Alert.AlertType.INFORMATION, "Ваша купюра номиналом " + nominal);
        }
    }

    public void showIntermediateImages() {
        IntermediateViewController controller;
        try {
             controller = loadViewFromResource("intermediateView.fxml");
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return;
        }

        // todo брать пути из того, что возвращает ImageRecognition
        List<Image> images = getImageList();

        controller.addImages(images);
    }

    private IntermediateViewController loadViewFromResource(String pathForResource) throws IOException {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource(pathForResource));
            Parent root = fxmlLoader.load();

            Stage stage = new Stage();
            stage.setTitle("Intermediate Images");
            stage.setScene(new Scene(root, 723, 586));
            stage.show();

            return fxmlLoader.getController();
        } catch (IOException e){
            System.out.println(e.getMessage());
            throw e;
        }
    }

    private List<Image> getImageList(){
        List<Image> images = new LinkedList<>();
        String sourcePath = sourceFile.getParent();


        for (ImageRecognition.IntermediateFiles fileName : ImageRecognition.IntermediateFiles.values()) {
            try {
                images.add(new Image(new FileInputStream(sourcePath + fileName)));
            } catch (FileNotFoundException e) {
                System.out.println(e.getMessage());
            }
        }

        return images;
    }

    private void showAlert(Alert.AlertType alertType, String message){
        Alert alert = new Alert(alertType);
        alert.setTitle("Message");
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.show();
    }
}