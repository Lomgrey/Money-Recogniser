package codesourse;

import codesourse.recognition.ImageRecognition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
    private ImageView outputImageView;

    private Image imageForRecognising;

    //Исходное изображение денежки, исправь хардкод какнить
    private String sourceFilename = "D:\\Money images\\sourceImage.jpg";
    private String ContourFilename;
    private String CroppedFilename;
    private String NormalizedFileName;
    private String CroppedNominalFilename;
    private String NominalEdgesFilename;
    private String TemplateMatchingFFilename;
    private String Nominal;

    private final String sourceFiles = "D:\\Money images";
    private final String initImagePath = sourceFiles.toString() + "/sourceImage.jpg";

    public void initialize() {

    }

    public void mouseDragDropped(final DragEvent e) {

        final Dragboard db = e.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            success = true;
            // получаем только первое изображение
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
        CroppedFilename = imageRecognition.CropImage();
        NormalizedFileName = imageRecognition.NormalizeImage();
        CroppedNominalFilename = imageRecognition.CropNominal();
        NominalEdgesFilename = imageRecognition.NominalEdges();
        TemplateMatchingFFilename = imageRecognition.TemplateMatching();//вернет пустоту, если не распознан
        Nominal = imageRecognition.Nominal();// тут типа номинал

        File file = new File(TemplateMatchingFFilename);
        outputImageView.setImage(new Image((file.toURI().toString())));


        e.setDropCompleted(success);
        e.consume();

        showIntermediateImages();
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

    private void showIntermediateImages() {
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
            stage.setScene(new Scene(root, 600, 400));
            stage.show();

            return fxmlLoader.getController();
        } catch (IOException e){
            System.out.println(e.getMessage());
            throw e;
        }
    }

    private List<Image> getImageList(){
        List<Image> images = new LinkedList<>();
        try {
            images.add(new Image(new FileInputStream(sourceFiles + "/contourImage.jpg")));
            images.add(new Image(new FileInputStream(sourceFiles + "/croppedImage.jpg")));
            images.add(new Image(new FileInputStream(sourceFiles + "/normalizedImage.jpg")));
        } catch (FileNotFoundException e){
            System.out.println(e.getMessage());
        }

        return images;
    }
}
