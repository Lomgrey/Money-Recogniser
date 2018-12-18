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
    private List<MatOfPoint> contoursPolyList;
    private int maxValIdx;
    private Mat contourMat;
    private Rect Rect;
    private Mat croppedMat;
    private Mat NormalizedMat;
    private Mat croppedNominalMat;
    private Mat nominalEdgesMat;
    private Mat[] templateMat = new Mat[3];
    private int index;

    private int MAX_THRESHOLD = 45;
    private Random rng = new Random(12345);

    public ImageRecognition(String filename) {
        //импорт изображения
        sourceMat = Imgcodecs.imread(filename);
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

        templateMat[0] = Imgcodecs.imread("D:\\Money images\\Template50.jpg");
        Imgproc.cvtColor(templateMat[0], templateMat[0], Imgproc.COLOR_BGR2GRAY);
        templateMat[1] = Imgcodecs.imread("D:\\Money images\\Template100.jpg");
        Imgproc.cvtColor(templateMat[1], templateMat[1], Imgproc.COLOR_BGR2GRAY);
//        template500Mat = Imgcodecs.imread("D:\\Money images\\Template500.jpg");
//        Imgproc.cvtColor(template500Mat, template500Mat, Imgproc.COLOR_BGR2GRAY);
    }

    public String FindContour() {
        //находим края по серому изображению, схораняем в лист контуров
        Mat cannyOutput = CannyEdges(sourceMat);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(cannyOutput, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

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
        Imgproc.drawContours(contourMat, contoursPolyList, maxValIdx, color);
        Imgproc.rectangle(contourMat, Rect.tl(), Rect.br(), color, 2);

        //хардкод пути сохранения, исправь какнить
        Imgcodecs.imwrite("D:\\Money images\\contourImage.jpg", cannyOutput);
        return "D:\\Money images\\contourImage.jpg";
    }

    public String CropImage() {
        //обычный, не крученый прямоугольник
        croppedMat = contourMat.submat(Rect);
        Imgcodecs.imwrite("D:\\Money images\\croppedImage.jpg", croppedMat);
        return "D:\\Money images\\croppedImage.jpg";
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
            if (GetDistance(point, 0, 0) < dist1) {
                dist1 = GetDistance(point, 0, 0);
                LeftUp = new Point(point.x - Rect.x, point.y - Rect.y);
            }
            if (GetDistance(point, Rect.width, 0) < dist2) {
                dist2 = GetDistance(point, Rect.width, 0);
                RightUp = new Point(point.x - Rect.x, point.y - Rect.y);
            }
            if (GetDistance(point, 0, Rect.height) < dist3) {
                dist3 = GetDistance(point, 0, Rect.height);
                LeftDown = new Point(point.x - Rect.x, point.y - Rect.y);
            }
            if (GetDistance(point, Rect.width, Rect.height) < dist4) {
                dist4 = GetDistance(point, Rect.width, Rect.height);
                RightDown = new Point(point.x - Rect.x, point.y - Rect.y);
            }
        }
        MatOfPoint2f src = new MatOfPoint2f(LeftUp, RightUp, LeftDown, RightDown);
        MatOfPoint2f dst = new MatOfPoint2f(new Point(0, 0), new Point(900, 0), new Point(0, 390), new Point(900, 390));

        //берем и делаем перспективное преобразование
        Mat PerspectiveTransformMat = getPerspectiveTransform(src, dst);
        NormalizedMat = new Mat();
        warpPerspective(croppedMat, NormalizedMat, PerspectiveTransformMat, new Size(900, 390));
        Imgcodecs.imwrite("D:\\Money images\\normalizedImage.jpg", NormalizedMat);
        return "D:\\Money images\\normalizedImage.jpg";
    }

    public String CropNominal() {
        Rect crop = new Rect(685, 240, 180, 120);
        croppedNominalMat = NormalizedMat.submat(crop);
        Imgcodecs.imwrite("D:\\Money images\\croppedNominalImage.jpg", croppedNominalMat);
        return "D:\\Money images\\croppedNominalImage.jpg";
    }

    public String NominalEdges() {
        nominalEdgesMat = CannyEdges(croppedNominalMat);
        Imgcodecs.imwrite("D:\\Money images\\NominalEdgesImage.jpg", nominalEdgesMat);
        return "D:\\Money images\\NominalEdgesImage.jpg";
    }

    public String TemplateMatching() {
        Mat[] resultMat = new Mat[3];
        Point[] matchLoc = new Point[3];
        index = 0;
        Mat img = nominalEdgesMat.clone();
        for (int i = 0; i < 2; ++i) {
            int result_cols = img.cols() - templateMat[i].cols() + 1;
            int result_rows = img.rows() - templateMat[i].rows() + 1;
            Mat tmp = new Mat();
            tmp.create(result_rows, result_cols, CvType.CV_32FC1);
            resultMat[i] = tmp.clone();
            int match_method = TM_SQDIFF_NORMED;

            Imgproc.matchTemplate(img, templateMat[i], resultMat[i], match_method);

            Core.normalize(resultMat[i], resultMat[i], 0, 1, Core.NORM_MINMAX, -1, new Mat());

            Core.MinMaxLocResult mmr = Core.minMaxLoc(resultMat[i]);
            matchLoc[i] = mmr.minLoc;
            if (matchLoc[i].x > matchLoc[index].x)
                index = i;
        }
        if (matchLoc[index].x == 0) {
            index = -1;
            return "";
        }

        Imgproc.rectangle(img, matchLoc[index], new Point(matchLoc[index].x + templateMat[index].cols(), matchLoc[index].y + templateMat[index].rows()),
                new Scalar(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256)));
        Imgcodecs.imwrite("D:\\Money images\\templateImage.jpg", img);
        return "D:\\Money images\\templateImage.jpg";
    }

    public String Nominal() {
        if (index == 0)
            return "50 рублей";
        else if (index == 1)
            return "100 рублей";
        else if (index == 2)
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

    private double GetDistance(Point point, int x, int y) {
        double distance = Math.sqrt(Math.pow(point.x - Rect.x - x, 2) + Math.pow(point.y - Rect.y - y, 2));
        return distance;
    }

    public Image MatToImage(Mat mat) {
        MatOfByte byteMat = new MatOfByte();
        Imgcodecs.imencode(".jpg", mat, byteMat);
        return new Image(new ByteArrayInputStream(byteMat.toArray()));
    }
}