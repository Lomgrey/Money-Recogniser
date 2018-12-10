package codesourse.recognition;

import javafx.scene.image.Image;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.opencv.imgproc.Imgproc.*;

public class ImageRecognition {
    private Mat sourceMat;
    private Mat contourMat;
    private Mat croppedMat;
    private Mat croppedNominalMat;
    private Mat nominalEdgesMat;
    private Mat[] templateMat = new Mat[3];
    private Rect Rect;
    private int MAX_THRESHOLD = 50;
    private Random rng = new Random(12345);

    public ImageRecognition(String filename) {
        //импорт изображения
        sourceMat = Imgcodecs.imread(filename);
        if (sourceMat.empty()) {
            System.err.println("Cannot read image: " + filename);
            System.exit(0);
        }
        templateMat[0] = Imgcodecs.imread("D:\\Money images\\Template50.jpg");
        Imgproc.cvtColor(templateMat[0], templateMat[0], Imgproc.COLOR_BGR2GRAY);
        templateMat[1] = Imgcodecs.imread("D:\\Money images\\Template100.jpg");
        Imgproc.cvtColor(templateMat[1], templateMat[1], Imgproc.COLOR_BGR2GRAY);
//        template500Mat = Imgcodecs.imread("D:\\Money images\\Template500.jpg");
//        Imgproc.cvtColor(template500Mat, template500Mat, Imgproc.COLOR_BGR2GRAY);
    }

    public String FindContour() {
        //находим края по серому изображению, схораняем в лист!!! контуров
        Mat cannyOutput = CannyEdges(sourceMat);
//
//        for (int i = (int)(cannyOutput.rows()*0.35); i < (int)(cannyOutput.rows()*0.45); i++) {
//            for (int j = (int)(cannyOutput.cols()*0.25); j < (int)(cannyOutput.cols()*0.40); j++) {
//                double[] data = cannyOutput.get(i, j); //Stores element in an array
//                for (int k = 0; k < cannyOutput.channels(); k++) //Runs for the available number of channels
//                {
//                    data[k] = 0; //Pixel modification done here
//                }
//                cannyOutput.put(i, j, data); //Puts element back into matrix
//            }
//        }


        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(cannyOutput, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

//        Rect rRect = new Rect();
//        MatOfPoint2f contoursPoly = new MatOfPoint2f();
//            Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(maxValIdx).toArray()), contoursPoly, 3, true);
//            rRect = Imgproc.boundingRect(new MatOfPoint2f(contoursPoly.toArray()));
//
//
//
//        Rect = rRect;
//        contourMat = sourceMat.clone();
//
//        //рисуется прямоугольник, охватывающий контур
//        for (int i = 0; i < contours.size(); i++) {
//            Scalar color = new Scalar(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
//            Imgproc.rectangle(contourMat, rRect.tl(), rRect.br(), color, 2);
//        }


        Rect[] rRect = new Rect[contours.size()];
        MatOfPoint2f[] contoursPoly = new MatOfPoint2f[contours.size()];
        for (int i = 0; i < contours.size(); i++) {
            contoursPoly[i] = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), contoursPoly[i], 3, true);
            rRect[i] = Imgproc.boundingRect(new MatOfPoint2f(contoursPoly[i].toArray()));
        }


        //из листа контуров ищем номер максимального по площади
        double maxVal = 0;
        int maxValIdx = 0;
        for (int contourIdx = 0; contourIdx < rRect.length; contourIdx++) {
            double Area = rRect[contourIdx].height * rRect[contourIdx].width;
            if (maxVal < Area) {
                maxVal = Area;
                maxValIdx = contourIdx;
            }
        }


        List<MatOfPoint> contoursPolyList = new ArrayList<>(contoursPoly.length);
        for (MatOfPoint2f poly : contoursPoly)
            contoursPolyList.add(new MatOfPoint(poly.toArray()));

        Rect = rRect[maxValIdx];
        contourMat = sourceMat.clone();

        //рисуется прямоугольник, охватывающий контур

        Scalar color = new Scalar(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
        Imgproc.drawContours(contourMat, contoursPolyList, maxValIdx, color);
        Imgproc.rectangle(contourMat, Rect.tl(), Rect.br(), color, 2);


        //хардкод пути сохранения, исправь какнить
        Imgcodecs.imwrite("D:\\Money images\\contourImage.jpg", contourMat);
        return "D:\\Money images\\contourImage.jpg";
    }

//    public String RotateImage() {
//        //повернули изображение короче
//        rotatedMat = new Mat();
//        double angle = rotatedRect.angle;
//        if (rotatedRect.size.height > rotatedRect.size.width)
//            angle = 90 + angle;
//        Mat rotation = getRotationMatrix2D(rotatedRect.center, angle, 1);
//        warpAffine(sourceMat, rotatedMat, rotation, rotatedMat.size());
//
//        //повернули повернутый прямоугольник
//        normalizedRect = rotatedRect;
//        normalizedRect.angle -= angle;
//        //нарисовали прямоугольник
//        Point[] rectPoints = new Point[4];
//        normalizedRect.points(rectPoints);
//        if (rectPoints[0].y < rectPoints[3].y) {
//            Point tmp = rectPoints[0];
//            rectPoints[0] = rectPoints[3];
//            rectPoints[1] = rectPoints[2];
//            rectPoints[2] = rectPoints[3];
//            rectPoints[3] = tmp;
//        }
//
//        Scalar color = new Scalar(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
//        for (int j = 0; j < 4; j++) {
//            Imgproc.line(rotatedMat, rectPoints[j], rectPoints[(j + 1) % 4], color);
//        }
//
//        //хардкод пути сохранения, исправь какнить
//        Imgcodecs.imwrite("D:\\Money images\\rotatedImage.jpg", rotatedMat);
//        return "D:\\Money images\\rotatedImage.jpg";
//    }


    public String CropImage() {
        //обычный, не крученый прямоугольник
        //Rect rect = normalizedRect.boundingRect();
        croppedMat = contourMat.submat(Rect);
        Imgcodecs.imwrite("D:\\Money images\\croppedImage.jpg", croppedMat);
        return "D:\\Money images\\croppedImage.jpg";
    }

    public String CropNominal() {
        Rect crop = new Rect((int) (Rect.width * 0.75), (int) (Rect.height * 0.5), (int) (Rect.width * 0.25), (int) (Rect.height * 0.5));
        croppedNominalMat = croppedMat.submat(crop);
        Imgcodecs.imwrite("D:\\Money images\\croppedNominalImage.jpg", croppedNominalMat);
        return "D:\\Money images\\croppedNominalImage.jpg";
    }


    public Mat CannyEdges(Mat Input) {
        //серое изображение
        Mat srcGray = new Mat();
        Imgproc.cvtColor(Input, srcGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.blur(srcGray, srcGray, new Size(3, 3));
        //находим края по серому изображению, схораняем в лист!!! контуров
        Mat Output = new Mat();
        Imgproc.Canny(srcGray, Output, MAX_THRESHOLD, MAX_THRESHOLD * 2);
        return Output;
    }

    public String NominalEdges() {
        nominalEdgesMat = CannyEdges(croppedNominalMat);
        Imgcodecs.imwrite("D:\\Money images\\NominalEdgesImage.jpg", nominalEdgesMat);
        return "D:\\Money images\\NominalEdgesImage.jpg";
    }

    public String TemplateMatching() {
        Mat[] resultMat = new Mat[3];
        Point[] matchLoc = new Point[3];
        int index = 0;
        Mat img = nominalEdgesMat.clone();
        for (int i = 0; i < 2; ++i) {
            int result_cols = img.cols() - templateMat[i].cols() + 1;
            int result_rows = img.rows() - templateMat[i].rows() + 1;
            Mat tmp = new Mat();
            tmp.create(result_rows, result_cols, CvType.CV_32FC1);
            resultMat[i]=tmp.clone();
            int match_method = TM_SQDIFF_NORMED;

            Imgproc.matchTemplate(img, templateMat[i], resultMat[i], match_method);

            Core.normalize(resultMat[i], resultMat[i], 0, 1, Core.NORM_MINMAX, -1, new Mat());

            Core.MinMaxLocResult mmr = Core.minMaxLoc(resultMat[i]);
            matchLoc[i] = mmr.minLoc;
            if (matchLoc[i].x > matchLoc[index].x)
                index = i;
        }


        Imgproc.rectangle(img, matchLoc[index], new Point(matchLoc[index].x + templateMat[index].cols(), matchLoc[index].y + templateMat[index].rows()),
                new Scalar(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256)));
        Imgcodecs.imwrite("D:\\Money images\\templateImage.jpg", img);
        return "D:\\Money images\\templateImage.jpg";
    }

    public Image MatToImage(Mat mat) {
        MatOfByte byteMat = new MatOfByte();
        Imgcodecs.imencode(".jpg", mat, byteMat);
        return new Image(new ByteArrayInputStream(byteMat.toArray()));
    }


}