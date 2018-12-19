package codesourse;

import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.List;

public class IntermediateViewController {

    @FXML
    private TabPane imagesTabPane;


    public void initialize() {
        // init
    }

    public void addImages(List<Image> images){
        for (Image image: images)
            addImage(image);
    }

    public void addImage(Image image){
        ImageView imageView = new ImageView();
        imageView.setImage(image);
        imageView.setFitWidth(715);
        imageView.setFitHeight(560);
        imageView.setPreserveRatio(true);

        int tabIndex = imagesTabPane.getTabs().size();
        Tab tab = new Tab("Step " + (tabIndex + 1));
        tab.setContent(imageView);

        imagesTabPane.getTabs().add(tab);
    }

}
