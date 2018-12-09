package codesourse.recognition;

import javafx.scene.image.Image;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.opencv.imgproc.Imgproc.getRotationMatrix2D;
import static org.opencv.imgproc.Imgproc.warpAffine;

public class ImageRecognition {
    private Mat sourceMat;
    private Mat contourMat;
    private Mat rotatedMat;
    private Mat croppedMat;
    private RotatedRect rotatedRect;
    private RotatedRect normalizedRect;
    private int MAX_THRESHOLD = 50;
    private Random rng = new Random(12345);

    public ImageRecognition(String filename) {
        //импорт изображения
        sourceMat = Imgcodecs.imread(filename);
        if (sourceMat.empty()) {
            System.err.println("Cannot read image: " + filename);
            System.exit(0);
        }
    }

    public String FindContour() {
        //серое изображение
        Mat srcGray = new Mat();
        Imgproc.cvtColor(sourceMat, srcGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.blur(srcGray, srcGray, new Size(3, 3));

        //находим края по серому изображению, схораняем в лист!!! контуров
        Mat cannyOutput = new Mat();
        Imgproc.Canny(srcGray, cannyOutput, MAX_THRESHOLD, MAX_THRESHOLD * 2);
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(cannyOutput, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        //из листа контуров ищем номер максимального по площади
        double maxVal = 0;
        int maxValIdx = 0;
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
            double contourArea = Imgproc.contourArea(contours.get(contourIdx));
            if (maxVal < contourArea) {
                maxVal = contourArea;
                maxValIdx = contourIdx;
            }
        }

        //какой-то аппроксимирующих контур и прямоугольник, охватывающий этот контур
        MatOfPoint2f[] contoursPoly = new MatOfPoint2f[1];
        RotatedRect[] rRect = new RotatedRect[1];

        contoursPoly[0] = new MatOfPoint2f();
        Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(maxValIdx).toArray()), contoursPoly[0], 3, true);
        rRect[0] = Imgproc.minAreaRect(new MatOfPoint2f(contoursPoly[0].toArray()));

        //короче это точки нашего контура
        rotatedRect = rRect[0];

        //преобразование аппроксимирующего контура, который хуй знает какой тип, в массив хуй знает чего
        List<MatOfPoint> contoursPolyList = new ArrayList<>(contoursPoly.length);
        for (MatOfPoint2f poly : contoursPoly) {
            contoursPolyList.add(new MatOfPoint(poly.toArray()));
        }
        //наше будущее изображение с контуром
        contourMat = sourceMat.clone();

        //рисуется контур
        Scalar color = new Scalar(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
        Imgproc.drawContours(contourMat, contoursPolyList, 0, color);

        //рисуется прямоугольник, охватывающий контур
        Point[] rectPoints = new Point[4];
        rotatedRect.points(rectPoints);
        for (int j = 0; j < 4; j++) {
            Imgproc.line(contourMat, rectPoints[j], rectPoints[(j + 1) % 4], color);
        }

        //хардкод пути сохранения, исправь какнить
        Imgcodecs.imwrite("D:\\Money images\\contourImage.jpg", contourMat);
        return "D:\\Money images\\contourImage.jpg";
    }

    public String RotateImage() {
        //повернули изображение короче
        rotatedMat = new Mat();
        Mat rotation = getRotationMatrix2D(rotatedRect.center, rotatedRect.angle, 1);
        warpAffine(sourceMat, rotatedMat, rotation, rotatedMat.size());

        //повернули повернутый прямоугольник
        normalizedRect = rotatedRect;
        normalizedRect.angle -= rotatedRect.angle;

        //нарисовали прямоугольник
        Point[] rectPoints = new Point[4];
        normalizedRect.points(rectPoints);
        Scalar color = new Scalar(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
        for (int j = 0; j < 4; j++) {
            Imgproc.line(rotatedMat, rectPoints[j], rectPoints[(j + 1) % 4], color);
        }

        //хардкод пути сохранения, исправь какнить
        Imgcodecs.imwrite("D:\\Money images\\rotatedImage.jpg", rotatedMat);
        return "D:\\Money images\\rotatedImage.jpg";
    }


    public String CropImage() {
        //обычный, не крученый прямоугольник
        Rect rect = normalizedRect.boundingRect();
        //Imgproc.rectangle(rotatedMat, rect.tl(), rect.br(), color, 2);
        croppedMat = rotatedMat.submat(rect);
        Imgcodecs.imwrite("D:\\Money images\\croppedImage.jpg", croppedMat);
        return "D:\\Money images\\croppedImage.jpg";
    }


    public Image MatToImage(Mat mat) {
        MatOfByte byteMat = new MatOfByte();
        Imgcodecs.imencode(".jpg", mat, byteMat);
        return new Image(new ByteArrayInputStream(byteMat.toArray()));
    }
}