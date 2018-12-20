package codesourse.recognition;

import javafx.scene.image.Image;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.opencv.imgproc.Imgproc.*;

public class ImageRecognition {
    private Mat sourceMat;
    private Mat cannyMat;
    private List<MatOfPoint> contoursPolyList;
    private int maxValIdx;
    private Mat contourMat;
    private Rect Rect;
    private Mat croppedMat;
    private Mat NormalizedMat;
    private Mat croppedNominalMat;
    private Mat nominalEdgesMat;
    //   private Mat[] templateMat = new Mat[3];
    private ArrayList<Mat[]> templatesMat = new ArrayList<>(3);
    private int index1, index2;

    private int MAX_THRESHOLD = 35;
    private Random rng = new Random(12345);

    private final String tempImagesFolderPath;

    public ImageRecognition(File filename) {
        tempImagesFolderPath = filename.getParent();

        //импорт изображения
        sourceMat = Imgcodecs.imread(filename.getAbsolutePath());
        if (sourceMat.empty()) {
            System.err.println("Cannot read image: " + filename);
            System.exit(0);
        }
        if (sourceMat.rows() > 1100 && (double) (sourceMat.rows()) / (double) (sourceMat.cols()) == 0.5625) {
            Size size = new Size(1920, 1080);
            resize(sourceMat, sourceMat, size);
        }
        if (sourceMat.rows() > 1300 && (double) (sourceMat.rows()) / (double) (sourceMat.cols()) == 0.75) {
            Size size = new Size(1600, 1200);
            resize(sourceMat, sourceMat, size);
        }

//        templateMat[0] = Imgcodecs.imread(tempImagesFolderPath + "/Template50.jpg");
//        Imgproc.cvtColor(templateMat[0], templateMat[0], Imgproc.COLOR_BGR2GRAY);
//
//        templateMat[1] = Imgcodecs.imread(tempImagesFolderPath + "/Template100.jpg");
//        Imgproc.cvtColor(templateMat[1], templateMat[1], Imgproc.COLOR_BGR2GRAY);
//
//        templateMat[2] = Imgcodecs.imread(tempImagesFolderPath + "/Template200.jpg");
//        Imgproc.cvtColor(templateMat[2], templateMat[2], Imgproc.COLOR_BGR2GRAY);
        templatesMat.add(new Mat[43]);//200
        templatesMat.add(new Mat[38]);//100
        templatesMat.add(new Mat[30]);//50



        for (int i = 0; i < templatesMat.get(0).length; i++) {
            templatesMat.get(0)[i] = Imgcodecs.imread(tempImagesFolderPath + "/Templates200/Template200_" + (i + 1) + ".jpg");
            Imgproc.cvtColor(templatesMat.get(0)[i], templatesMat.get(0)[i], Imgproc.COLOR_BGR2GRAY);
        }

        for (int i = 0; i < templatesMat.get(1).length; i++) {
            templatesMat.get(1)[i] = Imgcodecs.imread(tempImagesFolderPath + "/Templates100/Template100_" + (i + 1) + ".jpg");
            Imgproc.cvtColor(templatesMat.get(1)[i], templatesMat.get(1)[i], Imgproc.COLOR_BGR2GRAY);
        }

        for (int i = 0; i < templatesMat.get(2).length; i++) {
            templatesMat.get(2)[i] = Imgcodecs.imread(tempImagesFolderPath + "/Templates50/Template50_" + (i + 1) + ".jpg");
            Imgproc.cvtColor(templatesMat.get(2)[i], templatesMat.get(2)[i], Imgproc.COLOR_BGR2GRAY);
        }
    }

    public String CannyEdge() {
        cannyMat = CannyEdges(sourceMat);
        String imgFile = tempImagesFolderPath + IntermediateFiles.CANNY;
        Imgcodecs.imwrite(imgFile, cannyMat);
        return imgFile;
    }

    public String FindContour() {
        //находим края по серому изображению, схораняем в лист контуров


        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(cannyMat, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        //обводим прямоугольниками
        Rect[] rRect = new Rect[contours.size()];
        MatOfPoint2f[] contoursPoly = new MatOfPoint2f[contours.size()];
        for (int i = 0; i < contours.size(); i++) {
            contoursPoly[i] = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), contoursPoly[i], 3, true);
            rRect[i] = Imgproc.boundingRect(new MatOfPoint(contoursPoly[i].toArray()));
        }

        //из листа контуров ищем номер максимального по площади
        double maxVal = 0;
        maxValIdx = 0;
        for (int contourIdx = 0; contourIdx < rRect.length; contourIdx++) {
            double Area = rRect[contourIdx].height * rRect[contourIdx].width;
            if (maxVal < Area) {
                maxVal = Area;
                maxValIdx = contourIdx;
            }
        }

        contoursPolyList = new ArrayList<>(contoursPoly.length);
        for (MatOfPoint2f poly : contoursPoly)
            contoursPolyList.add(new MatOfPoint(poly.toArray()));

        Rect = rRect[maxValIdx];
        contourMat = sourceMat.clone();

        //рисуется прямоугольник, охватывающий контур
        Scalar color = new Scalar(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
        Imgproc.drawContours(contourMat, contoursPolyList, maxValIdx, color, 5);
        Imgproc.rectangle(contourMat, Rect.tl(), Rect.br(), color, 2);

        //хардкод пути сохранения, исправь какнить
        String imgFile = tempImagesFolderPath + IntermediateFiles.CONTOUR;
        Imgcodecs.imwrite(imgFile, contourMat);
        return imgFile;
    }

    public String CropImage() {
        //обычный, не крученый прямоугольник
        croppedMat = contourMat.submat(Rect);

        String imgFile = tempImagesFolderPath + IntermediateFiles.CROPPED;
        Imgcodecs.imwrite(imgFile, croppedMat);

        return imgFile;
    }

    public String NormalizeImage() {
        //ебаные углы купюры
        Point LeftUp = new Point();
        double dist1 = Double.MAX_VALUE;
        Point RightUp = new Point();
        double dist2 = Double.MAX_VALUE;
        Point LeftDown = new Point();
        double dist3 = Double.MAX_VALUE;
        Point RightDown = new Point();
        double dist4 = Double.MAX_VALUE;

        //находим ебаные углы купюры по минимальному расстоянию до точек углов изображения
        for (Point point : contoursPolyList.get(maxValIdx).toList()) {
            if (GetDistance(point, 0, 0) +
                    GetDistance(point, 0, 0, 0, Rect.height) +
                    GetDistance(point, 0, 0, Rect.width, 0) < dist1) {
                dist1 = GetDistance(point, 0, 0) +
                        GetDistance(point, 0, 0, 0, Rect.height) +
                        GetDistance(point, 0, 0, Rect.width, 0);
                LeftUp = new Point(point.x - Rect.x, point.y - Rect.y);
            }
            if (GetDistance(point, Rect.width, 0) +
                    GetDistance(point, Rect.width, 0, 0, 0) +
                    GetDistance(point, Rect.width, 0, Rect.width, Rect.height) < dist2) {
                dist2 = GetDistance(point, Rect.width, 0) +
                        GetDistance(point, Rect.width, 0, 0, 0) +
                        GetDistance(point, Rect.width, 0, Rect.width, Rect.height);
                RightUp = new Point(point.x - Rect.x, point.y - Rect.y);
            }
            if (GetDistance(point, 0, Rect.height) +
                    GetDistance(point, 0, Rect.height, 0, 0) +
                    GetDistance(point, 0, Rect.height, Rect.width, Rect.height) < dist3) {
                dist3 = GetDistance(point, 0, Rect.height) +
                        GetDistance(point, 0, Rect.height, 0, 0) +
                        GetDistance(point, 0, Rect.height, Rect.width, Rect.height);
                LeftDown = new Point(point.x - Rect.x, point.y - Rect.y);
            }
            if (GetDistance(point, Rect.width, Rect.height) +
                    GetDistance(point, Rect.width, Rect.height, Rect.width, 0) +
                    GetDistance(point, Rect.width, Rect.height, 0, Rect.height) < dist4) {
                dist4 = GetDistance(point, Rect.width, Rect.height) +
                        GetDistance(point, Rect.width, Rect.height, Rect.width, 0) +
                        GetDistance(point, Rect.width, Rect.height, 0, Rect.height);
                RightDown = new Point(point.x - Rect.x, point.y - Rect.y);
            }
        }
        MatOfPoint2f src = new MatOfPoint2f(LeftUp, RightUp, LeftDown, RightDown);
        MatOfPoint2f dst = new MatOfPoint2f(new Point(0, 0), new Point(900, 0), new Point(0, 390), new Point(900, 390));

        //берем и делаем перспективное преобразование
        Mat PerspectiveTransformMat = getPerspectiveTransform(src, dst);
        NormalizedMat = new Mat();
        warpPerspective(croppedMat, NormalizedMat, PerspectiveTransformMat, new Size(900, 390));

        String imgFile = tempImagesFolderPath + IntermediateFiles.NORMALIZED;
        Imgcodecs.imwrite(imgFile, NormalizedMat);

        return imgFile;
    }

    public String CropNominal() {
        Rect crop = new Rect(600, 210, 300, 150);
        croppedNominalMat = NormalizedMat.submat(crop);

        String imgFile = tempImagesFolderPath + IntermediateFiles.CROPPED_NORMAL;
        Imgcodecs.imwrite(imgFile, croppedNominalMat);

        return imgFile;
    }

    public String NominalEdges() {
        nominalEdgesMat = CannyEdges(croppedNominalMat);

        String imgFile = tempImagesFolderPath + IntermediateFiles.NOMINAL_EDGES;
        Imgcodecs.imwrite(imgFile, nominalEdgesMat);

        return imgFile;
    }

//    public String TemplateMatching() {
//        Mat[] resultMat = new Mat[3];
//        Point[] matchLoc = new Point[3];
//        index = 0;
//        Mat img = nominalEdgesMat.clone();
//        for (int i = 0; i < templateMat.length; ++i) {
//            int result_cols = img.cols() - templateMat[i].cols() + 1;
//            int result_rows = img.rows() - templateMat[i].rows() + 1;
//            Mat tmp = new Mat();
//            tmp.create(result_rows, result_cols, CvType.CV_32FC1);
//            resultMat[i] = tmp.clone();
//            int match_method = TM_SQDIFF_NORMED;
//
//            Imgproc.matchTemplate(img, templateMat[i], resultMat[i], match_method);
//
//            Core.normalize(resultMat[i], resultMat[i], 0, 1, Core.NORM_MINMAX, -1, new Mat());
//
//            Core.MinMaxLocResult mmr = Core.minMaxLoc(resultMat[i]);
//            matchLoc[i] = mmr.minLoc;
//            if (matchLoc[i].x > matchLoc[index].x)
//                index = i;
//        }
//        if (matchLoc[index].x == 0) {
//            index = -1;
//            return "";
//        }
//
//        Imgproc.rectangle(img, matchLoc[index], new Point(matchLoc[index].x + templateMat[index].cols(), matchLoc[index].y + templateMat[index].rows()),
//                new Scalar(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256)));
//
//        String imgFile = tempImagesFolderPath + IntermediateFiles.TEMPLATE;
//        Imgcodecs.imwrite(imgFile, img);
//
//        return imgFile;
//    }

    public String TemplateMatching() {

        ArrayList<Mat[]> resultsMat = new ArrayList<>(3);
        resultsMat.add(new Mat[templatesMat.get(0).length]);
        resultsMat.add(new Mat[templatesMat.get(1).length]);
        resultsMat.add(new Mat[templatesMat.get(2).length]);
        ArrayList<Point[]> matchLocs = new ArrayList<>(3);
        matchLocs.add(new Point[templatesMat.get(0).length]);
        matchLocs.add(new Point[templatesMat.get(1).length]);
        matchLocs.add(new Point[templatesMat.get(2).length]);

        index1 = 0;
        index2 = 0;
        Mat img = nominalEdgesMat.clone();

        for (int i = 0; i < templatesMat.size(); i++) {
            for (int j = 0; j < templatesMat.get(i).length; j++) {
                int result_cols = img.cols() - templatesMat.get(i)[j].cols() + 1;
                int result_rows = img.rows() - templatesMat.get(i)[j].rows() + 1;
                Mat tmp = new Mat();
                tmp.create(result_rows, result_cols, CvType.CV_32FC1);
                resultsMat.get(i)[j] = tmp.clone();
                int match_method = TM_SQDIFF_NORMED;

                Imgproc.matchTemplate(img, templatesMat.get(i)[j], resultsMat.get(i)[j], match_method);

                Core.normalize(resultsMat.get(i)[j], resultsMat.get(i)[j], 0, 1, Core.NORM_MINMAX, -1, new Mat());

                Core.MinMaxLocResult mmr = Core.minMaxLoc(resultsMat.get(i)[j]);
                matchLocs.get(i)[j] = mmr.minLoc;
                if (matchLocs.get(i)[j].x > matchLocs.get(index1)[index2].x) {
                    index1 = i;
                    index2 = j;
                }
            }
        }

        if (matchLocs.get(index1)[index2].x == 0) {
            index1 = -1;
            index2 = -1;
            return "";
        }

        Imgproc.rectangle(img, matchLocs.get(index1)[index2],
                new Point(matchLocs.get(index1)[index2].x + templatesMat.get(index1)[index2].cols(),
                        matchLocs.get(index1)[index2].y + templatesMat.get(index1)[index2].rows()),
                new Scalar(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256)));


        String imgFile = tempImagesFolderPath + IntermediateFiles.TEMPLATE;
        Imgcodecs.imwrite(imgFile, img);
        return imgFile;
    }

    public String Nominal() {
        if (index1 == 2)
            return "50 рублей";
        else if (index1 == 1)
            return "100 рублей";
        else if (index1 == 0)
            return "200 рублей";
        else
            return "Не распознано";
    }

    private Mat CannyEdges(Mat Input) {
        //серое изображение
        Mat srcGray = new Mat();
        Imgproc.cvtColor(Input, srcGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.blur(srcGray, srcGray, new Size(3, 3));
        //находим края по серому изображению, схораняем в лист!!! контуров
        Mat Output = new Mat();
        Imgproc.Canny(srcGray, Output, MAX_THRESHOLD, MAX_THRESHOLD * 3);
        return Output;
    }

    private double GetDistance(Point point, int x1, int y1, int x2, int y2) {
        double top = Math.abs((y2 - y1) * (point.x - Rect.x) - (x2 - x1) * (point.y - Rect.y) + x2 * y1 - y2 * x1);
        double bottom = Math.sqrt(Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2));
        return top / bottom;
    }

    private double GetDistance(Point point, int x, int y) {
        double distance = Math.sqrt(Math.pow(point.x - Rect.x - x, 2) + Math.pow(point.y - Rect.y - y, 2));
        return distance;
    }

    public Image MatToImage(Mat mat) {
        MatOfByte byteMat = new MatOfByte();
        Imgcodecs.imencode(".jpg", mat, byteMat);
        return new Image(new ByteArrayInputStream(byteMat.toArray()));
    }

    public enum IntermediateFiles {
        CANNY("/cannyImage.jpg"),
        CONTOUR("/contourImage.jpg"),
        CROPPED("/croppedImage.jpg"),
        NORMALIZED("/normalizedImage.jpg"),
        CROPPED_NORMAL("/croppedNominalImage.jpg"),
        NOMINAL_EDGES("/nominalEdgesImage.jpg"),
        TEMPLATE("/templateImage.jpg");

        private String fileName;

        IntermediateFiles(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public String toString() {
            return fileName;
        }
    }
}