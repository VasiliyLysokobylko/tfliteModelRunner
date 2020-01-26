package com.onpositive.dldemos.detection;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.SystemClock;
import android.os.Trace;

import com.onpositive.dldemos.data.TFLiteItem;
import com.onpositive.dldemos.interpreter.BaseInterpreter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageDetector extends BaseInterpreter {
    private ByteBuffer imgData = null;
    // outputLocations: array of shape [Batchsize, NUM_DETECTIONS,4]
    // contains the location of detected boxes
    private float[][][] outputLocations;
    // outputClasses: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the classes of detected boxes
    private float[][] outputClasses;
    // outputScores: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the scores of detected boxes
    private float[][] outputScores;
    // numDetections: array of shape [Batchsize]
    // contains the number of detected boxes
    private float[] numDetections;

    public ImageDetector(Activity activity, TFLiteItem tfLiteItem) throws IOException {
        super(activity, tfLiteItem);
        log.log(this.getClass().getSimpleName() + " initialized.");
    }

    public List<ObjectDetection> recognizeImage(final Bitmap bitmap) {
        Trace.beginSection("recognizeImage");

        Trace.beginSection("preprocessBitmap");
        Bitmap resizedImage = Bitmap.createScaledBitmap(bitmap, SIZE_X, SIZE_Y, true);
        imgData = convertBitmapToByteBuffer(resizedImage);
        Trace.endSection();

        Trace.beginSection("runDetection");
        long startTime = SystemClock.uptimeMillis();
        runInference();
        long endTime = SystemClock.uptimeMillis();
        Trace.endSection();
        log.log("Time cost to run model inference: " + (endTime - startTime));

        final ArrayList<ObjectDetection> recognitions = new ArrayList<>(MAX_RESULTS);
        for (int i = 0; i < MAX_RESULTS; ++i) {
            final RectF detection =
                    new RectF(
                            outputLocations[0][i][1] * bitmap.getWidth(),
                            outputLocations[0][i][0] * bitmap.getHeight(),
                            outputLocations[0][i][3] * bitmap.getWidth(),
                            outputLocations[0][i][2] * bitmap.getHeight());
            // SSD Mobilenet V1 Model assumes class 0 is background class
            // in label file and class labels start from 1 to number_of_classes+1,
            // while outputClasses correspond to class index from 0 to number_of_classes
            int labelOffset = 0;
            recognitions.add(
                    new ObjectDetection(
                            labels.get((int) outputClasses[0][i] + labelOffset),
                            outputScores[0][i],
                            detection));
        }
        Trace.endSection(); // "recognizeImage"
        log.log("ObjectDetection successful. Results count: " + recognitions.size());
        return recognitions;
    }

    protected void runInference() {
        outputLocations = new float[1][MAX_RESULTS][4];
        outputClasses = new float[1][MAX_RESULTS];
        outputScores = new float[1][MAX_RESULTS];
        numDetections = new float[1];

        Object[] inputArray = {imgData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputLocations);
        outputMap.put(1, outputClasses);
        outputMap.put(2, outputScores);
        outputMap.put(3, numDetections);

        interpreter.runForMultipleInputsOutputs(inputArray, outputMap);
    }

    public static class ObjectDetection implements Comparable<ObjectDetection> {

        private final String title;
        private final Float confidence;
        private RectF location;

        public ObjectDetection(final String title, final Float confidence, final RectF location) {
            this.title = title;
            this.confidence = confidence;
            this.location = location;
        }

        public String getTitle() {
            return title;
        }

        public Float getConfidence() {
            return confidence;
        }

        public RectF getLocation() {
            return new RectF(location);
        }

        public void setLocation(RectF location) {
            this.location = location;
        }

        @Override
        public String toString() {
            String resultString = "";

            if (title != null) {
                resultString += title + " ";
            }

            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f);
            }

            return resultString.trim();
        }

        @Override
        public int compareTo(ObjectDetection objectDetection) {
            return this.confidence.compareTo(objectDetection.confidence);
        }
    }
}