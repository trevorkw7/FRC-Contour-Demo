/*----------------------------------------------------------------------------*/
/* Copyright (c) 2017-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot;

import java.util.ArrayList;

import org.opencv.core.*;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInLayouts;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardLayout;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.util.Color;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/**
 * Uses the CameraServer class to automatically capture video from a USB webcam
 * and send it to the FRC dashboard without doing any vision processing. This
 * is the easiest way to get camera images to the dashboard. Just add this to
 * the robotInit() method in your program.
 */
public class Robot extends TimedRobot {
  @Override
  public void robotInit() {


    // Create a new thread and run the following code.
    new Thread(() -> {
      // Create the GRIP pipeline.
      GripPipeline gripPipeline = new GripPipeline();

      // Start the Camera Server and set the resolution to 640x480.
      CameraServer cameraServer = CameraServer.getInstance();
      UsbCamera usbCamera = cameraServer.startAutomaticCapture(1);
      usbCamera.setResolution(640, 480);
      usbCamera.setVideoMode(usbCamera.enumerateVideoModes()[1]);

      // Get the default video source (defaults to the USB camera returned above).
      CvSink inputVideo = cameraServer.getVideo(usbCamera);

      // Create a new output 160x120 video source. (The input video was resized to 1/4
      // of the original size in the first step of the pipeline.)
      CvSource outputVideo = cameraServer.putVideo("Output", 160, 120);

      // Create a new Mat to receive an image from the USB camera.
      Mat input = new Mat();
      ArrayList<MatOfPoint> contourList;
      Mat hsvThreshold;
      Scalar color = new Scalar(0, 0, 255);
      // Loop until interrupted.
      while (!Thread.interrupted()) {

        // Get the next video image.
        if (inputVideo.grabFrame(input) == 0) {
          continue;
        }

         // Process the image using the pipeline and get list of found contours
         gripPipeline.process(input);
        contourList = gripPipeline.findContoursOutput();

         
        //Filter contours for the lowermost contour 

        //get first contour in list
        MatOfPoint firstContour = contourList.get(0); 

        //convert MatOfPoint to MatOfPoint2f (f - float values)
        MatOfPoint2f convertedFirstContour = new MatOfPoint2f(firstContour.toArray());
        
        //Set lowest circle size and radius to the min enclosing circle around first contour in the list
        Point lowestCircleCenter = new Point();
        float[] lowestCircleRadius = new float[1];
        Imgproc.minEnclosingCircle(convertedFirstContour, lowestCircleCenter, lowestCircleRadius);

        //linear search to find circle with the highest y coordinate (lowest on screen)
        for (MatOfPoint contour : contourList){

          MatOfPoint2f convertedContour = new MatOfPoint2f(contour.toArray());

          Point currentCircleCenter = new Point();
          float[] currentCircleRadius = new float[1];
          Imgproc.minEnclosingCircle(convertedContour, currentCircleCenter, currentCircleRadius);

          if (currentCircleCenter.y > lowestCircleCenter.y && currentCircleRadius[1] > 100) {
            lowestCircleCenter = currentCircleCenter;
            lowestCircleRadius = currentCircleRadius;
          }
        }

        //draw circles on input mat to show on output
        Imgproc.circle(input, lowestCircleCenter, (int) lowestCircleRadius[0], new Scalar(0, 255, 0), 1);


        // Put the output of the final stage
        outputVideo.putFrame(input);
      }
    }).start();
  }
}
 