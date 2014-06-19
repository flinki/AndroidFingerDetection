package it.peretti.kofler.androidfingerdetection;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class BinaryImageGenerator {
	private static final int SAMPLE_NUMBER = 7;
	private Point[][] samplePoints = null;
	private double[][] averageHandColor = null;
	private double[][] averageBackgroundColor = null;

	private double[][] colorLower = new double[SAMPLE_NUMBER][3];
	private double[][] colorUpper = new double[SAMPLE_NUMBER][3];
	private double[][] colorLowerBackground = new double[SAMPLE_NUMBER][3];
	private double[][] colorUpperBackground = new double[SAMPLE_NUMBER][3];

	private Scalar lowerBoundary = new Scalar(0, 0, 0);
	private Scalar upperBoundary = new Scalar(0, 0, 0);
	private Mat[] sampleMats = new Mat[SAMPLE_NUMBER];

	public BinaryImageGenerator() {
		super();
		samplePoints = new Point[SAMPLE_NUMBER][2];
		for (int i = 0; i < SAMPLE_NUMBER; i++) {
			for (int j = 0; j < 2; j++) {
				samplePoints[i][j] = new Point();
			}
		}

		averageBackgroundColor = new double[SAMPLE_NUMBER][3];
		averageHandColor = new double[SAMPLE_NUMBER][3];
		initcolorLowerUpper(50, 50, 10, 10, 10, 10);
		initcolorLowerBackgroundUpper(50, 50, 3, 3, 3, 3);

		for (int i = 0; i < SAMPLE_NUMBER; i++) {
			sampleMats[i] = new Mat();
		}
	}
	public Mat presampleBackground(Mat img) {
		int columns = img.cols();
		int rows = img.rows();
		int squareLength = rows / 20;
		samplePoints[0][0].x = columns / 6;
		samplePoints[0][0].y = rows / 6;
		samplePoints[1][0].x = columns / 6;
		samplePoints[1][0].y = rows * 2 / 3;
		samplePoints[2][0].x = columns / 2;
		samplePoints[2][0].y = rows / 6;
		samplePoints[3][0].x = columns / 2;
		samplePoints[3][0].y = rows / 2;
		samplePoints[4][0].x = columns / 2;
		samplePoints[4][0].y = rows * 5 / 6;
		samplePoints[5][0].x = columns * 5 / 6;
		samplePoints[5][0].y = rows / 6;
		samplePoints[6][0].x = columns * 5 / 6;
		samplePoints[6][0].y = rows * 2 / 3;
		for (int i = 0; i < SAMPLE_NUMBER; i++) {
			samplePoints[i][1].x = samplePoints[i][0].x + squareLength;
			samplePoints[i][1].y = samplePoints[i][0].y + squareLength;
		}
		for (int i = 0; i < SAMPLE_NUMBER; i++) {
			Core.rectangle(img, samplePoints[i][0], samplePoints[i][1], new Scalar(0, 0, 255, 255),
					1);
		}

		Mat temp=new Mat();
		//Imgproc.GaussianBlur(img, temp, new org.opencv.core.Size(5, 5), 5, 5);
		//Imgproc.pyrDown(img, temp);
		//Imgproc.cvtColor(img, temp, Imgproc.COLOR_RGBA2RGB);
		Imgproc.cvtColor(img, temp, Imgproc.COLOR_RGB2Lab);
		//
		for (int i = 0; i < SAMPLE_NUMBER; i++) {
			for (int j = 0; j < 3; j++) {
				averageBackgroundColor[i][j] = (temp.get(
						(int) (samplePoints[i][0].y + squareLength / 2),
						(int) (samplePoints[i][0].x + squareLength / 2)))[j];
			}
		}
		return img;
	}
	public Mat presampleHand(Mat img) {
		int columns = img.cols();
		int rows = img.rows();
		int squareLength = rows / 20;

		samplePoints[0][0].x = columns / 2;
		samplePoints[0][0].y = rows / 3;
		samplePoints[1][0].x = columns * 5 / 14;
		samplePoints[1][0].y = rows * 5 / 14;
		samplePoints[2][0].x = columns * 8 / 12;
		samplePoints[2][0].y = rows * 6 / 12;
		samplePoints[3][0].x = columns / 2;
		samplePoints[3][0].y = rows * 7 / 12;
		samplePoints[4][0].x = columns / 1.5;
		samplePoints[4][0].y = rows * 7 / 12;
		samplePoints[5][0].x = columns * 4 / 9;
		samplePoints[5][0].y = rows * 3 / 4;
		samplePoints[6][0].x = columns * 5 / 9;
		samplePoints[6][0].y = rows * 3 / 4;

		for (int i = 0; i < SAMPLE_NUMBER; i++) {
			samplePoints[i][1].x = samplePoints[i][0].x + squareLength;
			samplePoints[i][1].y = samplePoints[i][0].y + squareLength;
		}

		for (int i = 0; i < SAMPLE_NUMBER; i++) {
			Core.rectangle(img, samplePoints[i][0], samplePoints[i][1], new Scalar(0, 0, 255, 255),
					1);
		}


		Mat temp=new Mat();
		//Imgproc.GaussianBlur(img, temp, new org.opencv.core.Size(5, 5), 5, 5);
		//Imgproc.pyrDown(img, temp);
		//Imgproc.cvtColor(img, temp, Imgproc.COLOR_RGBA2RGB);
		Imgproc.cvtColor(img, temp, Imgproc.COLOR_RGB2Lab);
		
		for (int i = 0; i < SAMPLE_NUMBER; i++) {
			for (int j = 0; j < 3; j++) {
				averageHandColor[i][j] = (temp.get(
						(int) (samplePoints[i][0].y + squareLength / 2),
						(int) (samplePoints[i][0].x + squareLength / 2)))[j];
			}
		}
		return img;
	}
	public Mat produceBinaryImage(Mat img) {
		boundariesCorrection();
		Mat imgOut = new Mat();


		Mat temp=new Mat();
		//Imgproc.GaussianBlur(img, temp, new org.opencv.core.Size(5, 5), 5, 5);
		// Imgproc.pyrDown(img, temp);
		//Imgproc.cvtColor(img, temp, Imgproc.COLOR_RGBA2RGB);
		Imgproc.cvtColor(img, temp, Imgproc.COLOR_RGB2Lab);
		
		Mat binaryHandImage = produceBinaryHandImage(temp);
		Mat binaryBackgroundImage = produceBinBackImg(temp);
		Core.bitwise_and(binaryHandImage, binaryBackgroundImage, imgOut);
		
		
		Imgproc.erode(imgOut, imgOut, new Mat());

		Imgproc.dilate(imgOut, imgOut, new Mat());
		return imgOut;
	}
	private void initcolorLowerUpper(double cl1, double cu1, double cl2, double cu2, double cl3,
			double cu3)
	{
		colorLower[0][0] = cl1;
		colorUpper[0][0] = cu1;
		colorLower[0][1] = cl2;
		colorUpper[0][1] = cu2;
		colorLower[0][2] = cl3;
		colorUpper[0][2] = cu3;
	}
	private void initcolorLowerBackgroundUpper(double cl1, double cu1, double cl2, double cu2, double cl3,
			double cu3)
	{
		colorLowerBackground[0][0] = cl1;
		colorUpperBackground[0][0] = cu1;
		colorLowerBackground[0][1] = cl2;
		colorUpperBackground[0][1] = cu2;
		colorLowerBackground[0][2] = cl3;
		colorUpperBackground[0][2] = cu3;
	}

	private void boundariesCorrection() {
		for (int i = 1; i < SAMPLE_NUMBER; i++) {
			for (int j = 0; j < 3; j++) {
				colorLower[i][j] = colorLower[0][j];
				colorUpper[i][j] = colorUpper[0][j];

				colorLowerBackground[i][j] = colorLowerBackground[0][j];
				colorUpperBackground[i][j] = colorUpperBackground[0][j];
			}
		}

		for (int i = 0; i < SAMPLE_NUMBER; i++) {
			for (int j = 0; j < 3; j++) {
				if (averageHandColor[i][j] - colorLower[i][j] < 0)
					colorLower[i][j] = averageHandColor[i][j];

				if (averageHandColor[i][j] + colorUpper[i][j] > 255)
					colorUpper[i][j] = 255 - averageHandColor[i][j];

				if (averageBackgroundColor[i][j] - colorLowerBackground[i][j] < 0)
					colorLowerBackground[i][j] = averageBackgroundColor[i][j];

				if (averageBackgroundColor[i][j] + colorUpperBackground[i][j] > 255)
					colorUpperBackground[i][j] = 255 - averageBackgroundColor[i][j];
			}
		}
	}
	private Mat produceBinaryHandImage(Mat img) {
		Mat imgOut = new Mat();
		for (int i = 0; i < SAMPLE_NUMBER; i++) {
			lowerBoundary.set(new double[] { averageHandColor[i][0] - colorLower[i][0],
					averageHandColor[i][1] - colorLower[i][1],
					averageHandColor[i][2] - colorLower[i][2] });
			upperBoundary.set(new double[] { averageHandColor[i][0] + colorUpper[i][0],
					averageHandColor[i][1] + colorUpper[i][1],
					averageHandColor[i][2] + colorUpper[i][2] });

			Core.inRange(img, lowerBoundary, upperBoundary, sampleMats[i]);

		}

		imgOut.release();
		sampleMats[0].copyTo(imgOut);

		for (int i = 1; i < SAMPLE_NUMBER; i++) {
			Core.add(imgOut, sampleMats[i], imgOut);
		}

		Imgproc.medianBlur(imgOut, imgOut, 3);
		return imgOut;
	}

	private Mat produceBinBackImg(Mat img) {
		Mat imgOut = new Mat();
		for (int i = 0; i < SAMPLE_NUMBER; i++) {

			lowerBoundary.set(new double[] {
					averageBackgroundColor[i][0] - colorLowerBackground[i][0],
					averageBackgroundColor[i][1] - colorLowerBackground[i][1],
					averageBackgroundColor[i][2] - colorLowerBackground[i][2] });
			upperBoundary.set(new double[] {
					averageBackgroundColor[i][0] + colorUpperBackground[i][0],
					averageBackgroundColor[i][1] + colorUpperBackground[i][1],
					averageBackgroundColor[i][2] + colorUpperBackground[i][2] });

			Core.inRange(img, lowerBoundary, upperBoundary, sampleMats[i]);
		}

		imgOut.release();
		sampleMats[0].copyTo(imgOut);

		for (int i = 1; i < SAMPLE_NUMBER; i++) {
			Core.add(imgOut, sampleMats[i], imgOut);
		}

		Core.bitwise_not(imgOut, imgOut);

		Imgproc.medianBlur(imgOut, imgOut, 7);
		return imgOut;

	}
}
