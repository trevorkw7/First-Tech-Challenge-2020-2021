/* Uses contouring to actively identify and draw a box around the largest ring.
Then uses the Cb channel value of the area to check for orange color and
the aspect ratio of the drawn box to build a "confidence value" from 0 to 1 for ring positions
FOUR and ONE and a hesitance value for ring position NONE. These confidence values
are then used to determine the most accurate Ring Position.

 */



package org.firstinspires.ftc.teamcode.Vision;

import com.acmerobotics.dashboard.config.Config;

import org.apache.commons.math3.fraction.Fraction;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.Common.UtilMethods;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openftc.easyopencv.OpenCvPipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Determine distance, yaw, and pitch
 *
 */
@Config
public class BlueGoalVisionPipeline extends OpenCvPipeline {



    //Pinhole Camera Variables
    protected double centerX;
    protected double centerY;

    public static  double CAMERA_HEIGHT; //camera height inches for distance calculation
    public static  double HIGH_GOAL_HEIGHT; //camera height inches for distance calculation

    public int imageWidth;
    public int imageHeight;

    //Aspect ratio (3 by 2 by default)
    public static int horizontalRatio;
    public static int verticalRatio;


    private double fov;
    private double horizontalFocalLength;
    private double verticalFocalLength;

    public static int cameraPitchOffset;
    public static int cameraYawOffset;
    public static double centerXOffset;
    public static double centerYOffset;



    //Boundary Line (Only detects above this to eliminate field tape)
    public static int BOUNDARY;


    //Mask constants to isolate blue coloured subjects
    public static double upperBlueThreshhold;
    public static double lowerBlueThreshhold;

    //Countour Filter Constants
    public static double CONTOUR_AREA_MIN;
    public static double CONTOUR_ASPECT_RATIO_MIN;
    public static double CONTOUR_ASPECT_RATIO_MAX;


    //working mat variables
    public Mat YCrCbFrame;
    public Mat CbFrame;
    public Mat MaskFrame;
    public Mat ContourFrame;


    //Contour Analysis Variables
    public List<MatOfPoint> blueContours;
    public MatOfPoint biggestBlueContour;
    public Rect blueRect;

    Point upperLeftCorner;
    Point upperRightCorner;
    Point lowerLeftCorner;
    Point lowerRightCorner;

    Point upperMiddle;
    Point lowerMiddle;
    Point leftMiddle;
    Point rightMiddle;

    //Viewfinder tracker
    public int viewfinderIndex;

    private Telemetry telemetry;


    public BlueGoalVisionPipeline (Telemetry telemetry) {

        class Fraction {
            private int numerator, denominator;

            Fraction(long a, long b) {
                numerator = (int) (a / gcd(a, b));
                denominator = (int) (b / gcd(a, b));
            }

            private long gcd(long a, long b) {
                return b == 0 ? a : gcd(b, a % b);
            }
        }

        this.telemetry = telemetry;


        //camera calculation offsets

        //TODO: Set these with logitech camera
        //Field of View (angle)
        fov = 90;

        // (angles)
        cameraPitchOffset = 0;
        cameraYawOffset = 0;

        //TODO: Set these with shooter
        // (pixels)
        centerXOffset = 0;
        centerYOffset = 0;

        //Boundary Line (Only detects above this to eliminate field tape)
        BOUNDARY = 160;


        //Mask constants to isolate blue coloured subjects
        upperBlueThreshhold = 200;
        lowerBlueThreshhold = 140;

        //Countour Filter Constants
        CONTOUR_AREA_MIN = 30;
        CONTOUR_ASPECT_RATIO_MIN = 1;
        CONTOUR_ASPECT_RATIO_MAX = 2;


        YCrCbFrame = new Mat();
        CbFrame = new Mat();
        MaskFrame = new Mat();
        ContourFrame = new Mat();

        blueContours = new ArrayList<MatOfPoint>();
        biggestBlueContour = new MatOfPoint();
        blueRect =  new Rect();

        upperLeftCorner = new Point();
        upperRightCorner = new Point();
        lowerLeftCorner = new Point();
        lowerRightCorner = new Point();

        upperMiddle  = new Point();;
        lowerMiddle  = new Point();;
        leftMiddle = new Point();
        rightMiddle = new Point();

        viewfinderIndex = 0;

    }

    @Override
    public void init(Mat mat) {
        super.init(mat);
        //Pinhole Camera Variables
        CAMERA_HEIGHT = 8.25; //camera height inches for distance calculation
        HIGH_GOAL_HEIGHT = 35.875; //camera height inches for distance calculation
        fov = 90;

        imageWidth = mat.width();
        imageHeight = mat.height();

        //Center of frame
        centerX = ((double) imageWidth / 2) - 0.5;
        centerY = ((double) imageHeight / 2) - 0.5;


        // pinhole model calculations
        double diagonalView = Math.toRadians(this.fov);
        Fraction aspectFraction = new Fraction(this.imageWidth, this.imageHeight);
        int horizontalRatio = aspectFraction.getNumerator();
        int verticalRatio = aspectFraction.getDenominator();
        double diagonalAspect = Math.hypot(horizontalRatio, verticalRatio);
        double horizontalView = Math.atan(Math.tan(diagonalView / 2) * (horizontalRatio / diagonalAspect)) * 2;
        double verticalView = Math.atan(Math.tan(diagonalView / 2) * (verticalRatio / diagonalAspect)) * 2;
        horizontalFocalLength = this.imageWidth / (2 * Math.tan(horizontalView / 2));
        verticalFocalLength = this.imageHeight / (2 * Math.tan(verticalView / 2));

    }

    @Override
    public Mat processFrame(Mat input) {

        //Convert to YCrCb
        Imgproc.cvtColor(input, YCrCbFrame, Imgproc.COLOR_RGB2YCrCb);

        //Extract Cb channel (how close to blue)
        Core.extractChannel(YCrCbFrame, CbFrame, 2);

        //Threshhold using Cb channel
        Imgproc.threshold(CbFrame, MaskFrame, lowerBlueThreshhold, upperBlueThreshhold, Imgproc.THRESH_BINARY);

        //clear previous contours to remove false positives if goal isn't in frame
        blueContours.clear();

        Imgproc.findContours(MaskFrame, blueContours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        blueContours = blueContours.stream().filter(i -> {
            boolean appropriateAspect = ((double) Imgproc.boundingRect(i).width / Imgproc.boundingRect(i).height > CONTOUR_ASPECT_RATIO_MIN)
                    && ((double) Imgproc.boundingRect(i).width / Imgproc.boundingRect(i).height < CONTOUR_ASPECT_RATIO_MAX);
            return filterContours(i) && appropriateAspect;
        }).collect(Collectors.toList());



        if (blueContours.size() != 0) {
            // Comparing width instead of area because wobble goals that are close to the camera tend to have a large area
            biggestBlueContour = Collections.max(blueContours, (t0, t1) -> {
                return Double.compare(Imgproc.boundingRect(t0).width, Imgproc.boundingRect(t1).width);
            });
            blueRect = Imgproc.boundingRect(biggestBlueContour);
            findCorners();
        } else {
            blueRect = null;
        }

        //-------------------DRAW ONTO FRAME -------------------------

        //draw center
        Imgproc.line(input, new Point(centerX, centerY+5), new Point(centerX, centerY-5), new Scalar(0,0,0));  //crosshair vertical
        Imgproc.line(input, new Point(centerX+5, centerY), new Point(centerX-5, centerY), new Scalar(0,0,0));  //crosshair horizontal

        //draw center offset (where you want the goal to be)
        Imgproc.line(input, new Point(centerX + centerXOffset, centerY + centerYOffset + 5), new Point(centerX + centerXOffset, centerY + centerYOffset-5), new Scalar(255,0,0));  //crosshair vertical
        Imgproc.line(input, new Point(centerX + centerXOffset + 5, centerY + centerYOffset), new Point(centerX + centerXOffset - 5, centerY + centerYOffset), new Scalar(255,0,0));  //crosshair horizontal


        if(isGoalVisible()){
            //draw contours
            Imgproc.drawContours(input, blueContours, -1, new Scalar(255, 255, 0));

            //draw bounding box
            Imgproc.rectangle(input, blueRect, new Scalar(0,255,0), 1);

            //Draw Corners and Middle
            Imgproc.circle(input, upperLeftCorner,2, new Scalar(0,255,0),2);
            Imgproc.circle(input, upperRightCorner,2, new Scalar(0,255,0),2);
            Imgproc.circle(input, lowerLeftCorner,2, new Scalar(0,255,0),2);
            Imgproc.circle(input, lowerRightCorner,2, new Scalar(0,255,0),2);

            Imgproc.circle(input, upperMiddle,2, new Scalar(255,0,0),2);
            Imgproc.circle(input, lowerMiddle,2, new Scalar(255,0,0),2);

            //draw center line
            Imgproc.line(input, lowerMiddle, upperMiddle,  new Scalar(255,0,0));

        }


        telemetry.addData("Goal Visibility", isGoalVisible());
        telemetry.addData("Distance (in)", getXDistance());
        telemetry.addData("Goal Height (px)", getGoalHeight());
        telemetry.addData("Goal Pitch (degrees)", getPitch());
        telemetry.addData("Goal Yaw (degrees)", getYaw());
        telemetry.addData("Width:", input.width());
        telemetry.addData("Height:", input.height());



        telemetry.update();


        //Return MaskFrame for tuning purposes
//        return MaskFrame;
        if(viewfinderIndex % 2 == 0){
            return input;
        } else {
            return MaskFrame;
        }
    }


    public boolean filterContours(MatOfPoint contour) {
        return Imgproc.contourArea(contour) > CONTOUR_AREA_MIN;
    }

    //helper method to check if rect is found
    public boolean isGoalVisible(){
        return blueRect != null;
    }

    public boolean isCornersVisible(){
        return upperLeftCorner != null && upperRightCorner != null && lowerLeftCorner != null && lowerRightCorner != null && upperMiddle != null && lowerMiddle != null;
    }

    public double getXDistance(){
        if (!isGoalVisible()){
            return 0;
        }
        return (3386/getGoalHeight()) - 7.56;
    }

    //    "Real Height" gives returns an accurate goal height in pixels even when goal is viewed at an angle by calculating distance between middle two points
    public double getGoalHeight(){
        if (!isGoalVisible() && isCornersVisible()){
            return 0;
        }
        return Math.sqrt(Math.pow(Math.abs(lowerMiddle.y - rightMiddle.y), 2) + Math.pow(Math.abs(lowerMiddle.x - rightMiddle.x), 2));
    }

    public double getYaw() {
        if (!isGoalVisible()){
            return 0;
        }

        double targetCenterX = getCenterofRect(blueRect).x;

        return Math.toDegrees(
                Math.atan((targetCenterX - (centerX + centerXOffset)) / horizontalFocalLength)
        );
    }


    public double getPitch() {
        if (!isGoalVisible()){
            return 0;
        }

        double targetCenterY = getCenterofRect(blueRect).y;

        return -Math.toDegrees(
                Math.atan((targetCenterY - (centerY + centerYOffset)) / verticalFocalLength)
        );
    }



    /*
     * Changing the displayed color space on the viewport when the user taps it
     */
    @Override
    public void onViewportTapped() {
        viewfinderIndex++;
    }

    public void findCorners(){
        RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(biggestBlueContour.toArray()));

        Point[] corners = new Point[4];
        rect.points(corners);

        if (UtilMethods.inRange(rect.angle, -90.9, -45)){
            lowerLeftCorner = corners[1];
            upperLeftCorner = corners[2];
            upperRightCorner = corners[3];
            lowerRightCorner = corners[0];
        } else {
            lowerLeftCorner = corners[0];
            upperLeftCorner = corners[1];
            upperRightCorner = corners[2];
            lowerRightCorner = corners[3];
        }


        //Calculate "middle points" for true height calculation
        upperMiddle = new Point((upperLeftCorner.x + upperRightCorner.x) / 2, (upperLeftCorner.y + upperRightCorner.y) / 2);
        lowerMiddle = new Point((lowerLeftCorner.x + lowerRightCorner.x) / 2, (lowerLeftCorner.y + lowerRightCorner.y) / 2);

        leftMiddle = new Point((upperLeftCorner.x + lowerLeftCorner.x) / 2, (upperLeftCorner.y + lowerLeftCorner.y) / 2);
        rightMiddle = new Point((upperRightCorner.x + lowerRightCorner.x) / 2, (upperRightCorner.y + lowerRightCorner.y) / 2);

    }

    public Point getCenterofRect(Rect rect) {
        if (rect == null) {
            return new Point(centerX, centerY);
        }
        return new Point(rect.x + rect.width / 2.0, rect.y + rect.height / 2.0);
    }









}



