package com.mad.qut.budgetr.utils;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class ImageUtils {

    private static final String TAG = ImageUtils.class.getSimpleName();

    private static Point computeIntersect(Point l1, Point l2, Point l3, Point l4) {
        float d = (float) (((l1.x - l2.x) * (l3.y - l4.y)) - ((l1.y - l2.y) * (l3.x - l4.x)));
        if (d != 0) {
            Point p = new Point();
            p.x = ((l1.x * l2.y - l1.y * l2.x) * (l3.x - l4.x) - (l1.x - l2.x) * (l3.x * l4.y - l3.y * l4.x)) / d;
            p.y = ((l1.x * l2.y - l1.y * l2.x) * (l3.y - l4.y) - (l1.y - l2.y) * (l3.x * l4.y - l3.y * l4.x)) / d;
            return p;
        }
        return new Point(-1, -1);
    }

    public static Mat toGrayScale(Mat image) {
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        return gray;
    }

    public static Mat resize(Mat image, int factor) {
        Mat resized = new Mat();
        Imgproc.resize(image, resized, new Size(image.cols()/factor, image.rows()/factor));
        return resized;
    }

    public static Mat gaussianBlur(Mat image) {
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(image, blurred, new Size(3, 3), 0);
        return blurred;
    }

    public static List<MatOfPoint> findContourLines(Mat image) {
        Mat processed = toGrayScale(image);
        processed = gaussianBlur(processed);
        Imgproc.threshold(processed, processed, 0, 255, Imgproc.THRESH_OTSU);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(processed, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        return contours;
    }

    public static int findBestContourLine(List<MatOfPoint> contours) {
        double maxArea = 0;
        int bestContour = 0;
        for (int i = 0; i < contours.size(); i++) {
            double area = Imgproc.contourArea(contours.get(i));
            if (area > maxArea) {
                maxArea = area;
                bestContour = i;
            }
        }
        return bestContour;
    }

    public static Mat getMask(Mat image, List<MatOfPoint> contours, int i) {
        Mat mask = new Mat(new Size(image.cols(), image.rows()), CvType.CV_8UC1);
        mask.setTo(new Scalar(0.0));
        Imgproc.drawContours(mask, contours, i, new Scalar(255, 255, 255), -1);
        Imgproc.drawContours(mask, contours, i, new Scalar(255, 255, 255), 2);
        return mask;
    }

    public static Mat mask(Mat image, List<MatOfPoint> contours, int i) {
        Mat masked = new Mat();
        Mat mask = getMask(image, contours, i);
        Core.bitwise_and(image, mask, image);
        return image;
    }

    public static Point[] approxContour(List<MatOfPoint> contours, int i) {
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());
        double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
        Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);
        return approxCurve.toArray();
    }

    public static RotatedRect getMinAreaRect(MatOfPoint2f points) {
        return Imgproc.minAreaRect(points);
    }

    public static List<Point> findCorners(Mat image, Point[] points) {
        List<Point> corners = new ArrayList<Point>();
        Point tl = new Point(image.cols(), image.rows());
        Point tr = new Point(0, image.rows());
        Point br = new Point(0, 0);
        Point bl = new Point(image.cols(), 0);
        for (int i=0; i < points.length; i++) {
            Point p = points[i];
            if (0.5*p.x+0.5*p.y < 0.5*tl.x+0.5*tl.y) {
                tl = p;
            }
            if (-0.5*p.x+0.5*p.y < -0.5*tr.x+0.5*tr.y) {
                tr = p;
            }
            if (0.5*p.x+0.5*p.y > 0.5*br.x+0.5*br.y) {
                br = p;
            }
            if (0.5*p.x-0.5*p.y < 0.5*bl.x-0.5*bl.y) {
                bl = p;
            }
        }
        corners.add(tl);
        corners.add(tr);
        corners.add(br);
        corners.add(bl);
        return corners;
    }

    public static double distance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
    }

    public static Mat transform(Mat image, Mat src, Size s) {
        Mat result = new Mat(s, image.type());
        result.setTo(new Scalar(0.0));

        List<Point> destPoints = new ArrayList<Point>();
        destPoints.add(new Point(0, 0));
        destPoints.add(new Point(result.cols(), 0));
        destPoints.add(new Point(result.cols(), result.rows()));
        destPoints.add(new Point(0, result.rows()));
        Mat dest = Converters.vector_Point2f_to_Mat(destPoints);

        Mat transMat = Imgproc.getPerspectiveTransform(src, dest);

        Imgproc.warpPerspective(image, result, transMat, s);
        return result;
    }

    public static Mat thresholdOtsuInv(Mat image) {
        Mat thresh = new Mat();
        //Imgproc.adaptiveThreshold(image, thresh, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 5, 4);
        Imgproc.threshold(image, thresh, -1, 255, Imgproc.THRESH_BINARY_INV+Imgproc.THRESH_OTSU);
        return thresh;
    }

    public static Mat divide(Mat image) {
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(9,9));
        Mat temp = new Mat();
        Imgproc.resize(image, temp, new Size(image.cols()/4, image.rows()/4));
        Imgproc.morphologyEx(temp, temp, Imgproc.MORPH_CLOSE, kernel);
        Imgproc.resize(temp, temp, new Size(image.cols(), image.rows()));

        Mat closed = new Mat(); // closed will have type CV_32F
        Core.divide(image, temp, closed, 1, CvType.CV_32F);
        Core.normalize(closed, closed, 0, 255, Core.NORM_MINMAX, CvType.CV_8U);
        return closed;
    }

    public static Bitmap process(Bitmap bmpOriginal) {
        Mat imageMat = new Mat();
        Utils.bitmapToMat(bmpOriginal, imageMat);

        Mat imResized = resize(imageMat, 4);
        List<MatOfPoint> contours = findContourLines(imResized);
        int i = findBestContourLine(contours);
        Point[] contourPoints = approxContour(contours, i);
        List<Point> corners = findCorners(imResized, contourPoints);

        ListIterator<Point> iterator = corners.listIterator();
        while (iterator.hasNext()) {
            Point p = iterator.next();
            p.x = p.x*4;
            p.y = p.y*4;
        }

        int targetWidth = (int) distance(corners.get(0), corners.get(1));
        int targetHeight = (int) distance(corners.get(0), corners.get(3));
        Mat result = transform(imageMat, Converters.vector_Point2f_to_Mat(corners), new Size(targetWidth, targetHeight));
        result = toGrayScale(result);
        result = divide(result);
        result = gaussianBlur(result);
        //Core.addWeighted(result, 1.5, blur, -0.5, 0, result);
        result = thresholdOtsuInv(result);

        Bitmap processedBmp = Bitmap.createBitmap(result.width(), result.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, processedBmp);

        Log.d(TAG, processedBmp.getWidth() + "x" + processedBmp.getHeight());

        return processedBmp;
    }

}

/*Mat canny = new Mat();
        Imgproc.Canny(mask, canny, 100, 100, 3, true);
        Mat lines = new Mat();
        Imgproc.HoughLinesP(canny, lines, 1, Math.PI/180, 70, 30, 10);

        MatOfPoint2f corners = new MatOfPoint2f();
        for (int i = 0; i < lines.cols(); i++) {
            double[] vec = lines.get(0, i);
            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];
            Point l1 = new Point(x1, y1);
            Point l2 = new Point(x2, y2);

            for (int j = i+1; j < lines.cols(); j++) {
                double[] vec2 = lines.get(0, j);
                double x3 = vec2[0],
                        y3 = vec2[1],
                        x4 = vec2[2],
                        y4 = vec2[3];
                Point l3 = new Point(x3, y3);
                Point l4 = new Point(x4, y4);
                Point corner = computeIntersect(l1, l2, l3, l4);
                //Log.d(TAG, corner.toString());
                if (corner.x >= rectangle[1].x && corner.x <= rectangle[3].x
                        && corner.y >= rectangle[1].y && corner.y <= rectangle[3].y) {
                    Core.circle(imageMat, corner, 10, new Scalar(255, 0, 0), 4);
                }
                corners.push_back(new MatOfPoint2f(corner));
            }

            //Core.line(gray, start, end, new Scalar(255,255,255), 5);
        }*/
