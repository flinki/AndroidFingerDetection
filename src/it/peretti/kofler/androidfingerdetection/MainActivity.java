package it.peretti.kofler.androidfingerdetection;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Toast;
import it.peretti.kofler.fingerdetection.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.utils.Converters;
import org.opencv.video.BackgroundSubtractorMOG;
import org.opencv.video.BackgroundSubtractorMOG2;




public class MainActivity extends Activity implements CvCameraViewListener2, View.OnTouchListener
{private static final String TAG = "OCVSample::Activity";
private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
public static final int        JAVA_DETECTOR       = 0;
public static final int        NATIVE_DETECTOR     = 1;
private Scalar CONTOUR_COLOR;
private Tutorial3View mOpenCvCameraView;
private List<Size> mResolutionList;
private MenuItem[] mEffectMenuItems;
private SubMenu mColorEffectsMenu;
private MenuItem[] mResolutionMenuItems;
private SubMenu mResolutionMenu;
private Mat mRgba;
private Mat mSpectrum;
private boolean palmdetected = false;
private boolean palmsample = true;
private ColorBlobDetector mDetector;
private BackgroundSubtractorMOG mbackgroundsubstractor ;
private Scalar mBlobColorHsv;
private Scalar mBlobColorRgba;
private File mCascadeFile;
private CascadeClassifier mJavaDetector;
private DetectionBasedTracker mNativeDetector;
private int                    mDetectorType       = NATIVE_DETECTOR;
private String[]               mDetectorName;

private float                  mRelativeFaceSize   = 0.2f;
private int                    mAbsoluteFaceSize   = 0;



private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {



	@Override
	public void onManagerConnected(int status) {
		switch (status) {
		case LoaderCallbackInterface.SUCCESS:
		{
			Log.i(TAG, "OpenCV loaded successfully");

			// Load native library after(!) OpenCV initialization
			System.loadLibrary("detection_based_tracker");

			try {
				// load cascade file from application resources
				InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
				File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
				mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
				FileOutputStream os = new FileOutputStream(mCascadeFile);

				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = is.read(buffer)) != -1) {
					os.write(buffer, 0, bytesRead);
				}
				is.close();
				os.close();

				mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
				if (mJavaDetector.empty()) {
					Log.e(TAG, "Failed to load cascade classifier");
					mJavaDetector = null;
				} else
					Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

				mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);

				cascadeDir.delete();

			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
			}

			mOpenCvCameraView.enableView();
		} break;
		default:
		{
			super.onManagerConnected(status);
		} break;
		}
	}
};
private org.opencv.core.Size SPECTRUM_SIZE;
private SubMenu mCameraMenu;
private MenuItem[] mCameranMenuItems;
private Mat mGray;
private Mat fgmask;

public MainActivity() {
	Log.i(TAG, "Instantiated new " + this.getClass());
}

/** Called when the activity is first created. */
@Override
public void onCreate(Bundle savedInstanceState) {
	Log.i(TAG, "called onCreate");
	super.onCreate(savedInstanceState);
	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

	setContentView(R.layout.tutorial3_surface_view);

	mOpenCvCameraView = (Tutorial3View) findViewById(R.id.tutorial3_activity_java_surface_view);

	mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
	mOpenCvCameraView.setCameraIndex(1);
	mOpenCvCameraView.setCvCameraViewListener(this);
	//mOpenCvCameraView.setResolution(mOpenCvCameraView.getResolutionList().get(5));


}

@Override
public void onPause()
{
	super.onPause();
	if (mOpenCvCameraView != null)
		mOpenCvCameraView.disableView();
}

@Override
public void onResume()
{
	super.onResume();
	OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
}

public void onDestroy() {
	super.onDestroy();
	if (mOpenCvCameraView != null)
		mOpenCvCameraView.disableView();
}

public void onCameraViewStarted(int width, int height) {
	Size resolution = mOpenCvCameraView.getResolutionList().get(5);
	mOpenCvCameraView.setResolution(resolution);
	this.mRgba = new Mat(width, height, CvType.CV_8UC4);
	this.mDetector = new ColorBlobDetector();
	this.mSpectrum = new Mat();
	this.mBlobColorRgba = new Scalar(255.0D);
	this.mBlobColorHsv = new Scalar(255.0D);
	this.SPECTRUM_SIZE = new org.opencv.core.Size(200.0D, 64.0D);
	this.CONTOUR_COLOR = new Scalar(255.0D, 0.0D, 0.0D, 255.0D);
	//this.mbackgroundsubstractor= new BackgroundSubtractorMOG(3, 4, 0.8);
	this.mbackgroundsubstractor= new BackgroundSubtractorMOG(30,4,0.8);
	this.fgmask=new Mat();

}

public void onCameraViewStopped() {
}

public Mat onCameraFrame(CvCameraViewFrame inputFrame) {


	this.mRgba = inputFrame.rgba();
	this.mGray  = inputFrame.gray();



	//face detection 


	if (mAbsoluteFaceSize == 0) {
		int height = mGray.rows();
		if (Math.round(height * mRelativeFaceSize) > 0) {
			mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
		}
		mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
	}

	MatOfRect faces = new MatOfRect();

	if (mDetectorType == JAVA_DETECTOR) {
		if (mJavaDetector != null)
			mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
					new org.opencv.core.Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new org.opencv.core.Size());
	}
	else if (mDetectorType == NATIVE_DETECTOR) {
		if (mNativeDetector != null)
			mNativeDetector.detect(mGray, faces);
	}
	else {
		Log.e(TAG, "Detection method is not selected!");
	}

	Rect[] facesArray = faces.toArray();
	for (int i = 0; i < facesArray.length; i++){
		
		//draw face rectange
		//Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
		
		//set pixels of face to black
		mRgba.submat(facesArray[i]).setTo(new Scalar(255,255,255,255));
	
	}

	//if(!mRgba.empty()){
	//return mRgba;}







	//System.out.println(inputFrame.rgba().size());//cols/spalte/x 1280 rows/reihe/y 720
	
	Mat mhsv = new Mat();
	Mat mRGB = new Mat();
	Scalar  hsv_min = new Scalar(0, 30, 80, 0);
	Scalar  hsv_max = new Scalar(20, 150, 255, 0);

	//Scalar  hsv_min = new Scalar(0, 43, 80, 0);
	//Scalar  hsv_max = new Scalar(25, 255, 255, 0);




	//background substraction 
	Imgproc.cvtColor(mRgba, mhsv, Imgproc.COLOR_RGB2HSV);//opencv uses values for HSV H:0-180 S: 0 - 255, V: 0 - 255
	//Imgproc.cvtColor(mRgba, mRGB, Imgproc.COLOR_RGBA2RGB);
	//Imgproc.cvtColor(mRgba, mhsv, Imgproc.COLOR_RGB2HSV);
	//Mat mRgb2=new Mat();
	//Imgproc.cvtColor(mRgba, mRgb2, Imgproc.COLOR_RGB2HSV_FULL); //HSV H:0-255 S: 0 - 255, V: 0 - 255

	//	for(int i=0;i<3;i++){
	//		
	//		System.out.println(mRgb.get(0, 0)[i]+"-------"+mRgb2.get(0, 0)[i]);
	//	}
	
	
	//mhsv or mrgb
	//mbackgroundsubstractor.apply(mRGB, fgmask,0.001);
	//if(!fgmask.empty()){return fgmask;}
	//System.out.println(mRgb.toString());
	Mat hsv_mask=new Mat();
	//mbackgroundsubstractor.
	//Core.bitwise_not(fgmask, fgmask);
	
	
	
	//hsv_mask=fgmask;
	
	//colorrange 
	Core.inRange(mhsv, hsv_min, hsv_max, hsv_mask);
	mRGB.release();
	mhsv.release();
	Imgproc.GaussianBlur(hsv_mask, hsv_mask, new org.opencv.core.Size(3,3), 0);
	
	Imgproc.erode(hsv_mask, hsv_mask, new Mat());
	
	Imgproc.dilate(hsv_mask, hsv_mask, new Mat());

	
	
	
	
	List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
	//background substraction
	//Core.bitwise_not(hsv_mask, hsv_mask);
	//Imgproc.GaussianBlur(hsv_mask, hsv_mask, new org.opencv.core.Size(5,5), 0);
	//Imgproc.dilate(hsv_mask, hsv_mask, new Mat());
	//Imgproc.dilate(hsv_mask, hsv_mask, new Mat());
	Imgproc.findContours(hsv_mask.clone(), contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

	
	List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
	// Find max contour area
	double maxArea = 0;
	Iterator<MatOfPoint> each = contours.iterator();
	while (each.hasNext()) {
		MatOfPoint wrapper = each.next();
		double area = Imgproc.contourArea(wrapper);
		if (area > maxArea)
			maxArea = area;
	}

	// Filter contours by area and resize to fit the original image size



	double mMinContourArea=0.7;
	each = contours.iterator();
	while (each.hasNext()) {
		MatOfPoint contour = each.next();

		if (Imgproc.contourArea(contour) > mMinContourArea*maxArea) {
			//Core.multiply(contour, new Scalar(4,4), contour);

			mContours.add(contour);
		}
	}



	//List<MatOfPoint> contours = new Vector() ;
	//Imgproc.findContours(hsv_mask, contours, new Mat(), Imgproc.RETR_FLOODFILL, Imgproc.CHAIN_APPROX_SIMPLE);
	if(mContours.size()>0){
		//Imgproc.drawContours(mRgba,mContours , -1, new Scalar(0,0,255));
		Rect brect=Imgproc.boundingRect(mContours.get(0));
		Core.rectangle(mRgba, brect.tl(), brect.br(),new Scalar(0,255,0));


		//calc moments center point
		List<Moments> mu = new ArrayList<Moments>(mContours.size());
		for (int i = 0; i < mContours.size(); i++) {
			mu.add(i, Imgproc.moments(mContours.get(i), false));
			Moments p = mu.get(i);
			int x = (int) (p.get_m10() / p.get_m00());
			int y = (int) (p.get_m01() / p.get_m00());
			Core.circle(mRgba, new Point(x, y), 4, new Scalar(255,49,0,255));

		}

		MatOfPoint2f  pts = new MatOfPoint2f( mContours.get(0).toArray() );
		RotatedRect mrect=Imgproc.minAreaRect(pts);
		Point[] rect_points = new Point[4];
		mrect.points(rect_points);
		for( int j = 0; j < 4; j++ ){
			Core.line( mRgba, rect_points[j], rect_points[(j+1)%4], new Scalar(255,255,0));
		}
		 Core.circle(mRgba, mrect.center, (int) (mrect.size.height/2), new Scalar(255,0,255));

		//convex hull for biggest contour
		List<MatOfPoint> hullPoints = new ArrayList<MatOfPoint>();
		MatOfInt hullInt=new MatOfInt();
		Imgproc.convexHull(mContours.get(0), hullInt);
		List<Point> hullPointList=new ArrayList<Point>();
		for(int j=0; j < hullInt.toList().size(); j++){

			hullPointList.add( mContours.get(0).toList().get(hullInt.toList().get(j)));

		}

		MatOfPoint hullPointMat=new MatOfPoint();
		hullPointMat.fromList(hullPointList);
		hullPoints.add(hullPointMat);   

		//draw convex hull
		Imgproc.drawContours(mRgba, hullPoints, -1,  new Scalar(255,0,0, 255), 1);


		//convexity defect
		MatOfInt4 convexityDefects=new MatOfInt4();
		Imgproc.convexityDefects(mContours.get(0), hullInt, convexityDefects);


		List<Integer> dList = convexityDefects.toList();
		Point[] contourPts = mContours.get(0).toArray();
		for (int i = 0; i < dList.size(); i=i+4)
		{
			if(dList.size()>0){

				// start point, end point, farthest point, approximate distance to farthest point
				int id = i % 4;
				int startpointind=dList.get(i+id);
				int endpointind=dList.get(i+id+1);
				int farhestpointind=dList.get(i+id+2);
				int approxdistance=dList.get(i+id+3);
				Point startPoint = contourPts[startpointind];
				Point endPoint = contourPts[endpointind];
				Point farhtestPoint = contourPts[farhestpointind];
				List<Point> list=new ArrayList<Point>();
				list.add(startPoint);
				list.add(farhtestPoint);
				list.add(endPoint);

				MatOfPoint matofpoint=new MatOfPoint();
				matofpoint.fromList(list);
				List<MatOfPoint> listmop=new ArrayList<MatOfPoint>();
				for (Point p : list) {

					listmop.add(new MatOfPoint(p));
				}

				//Core.circle(mRgba, startPoint, 3, new Scalar(255,255,255));
				//Core.circle(mRgba, endPoint, 3, new Scalar(255,255,255));
				//Core.circle(mRgba, farhtestPoint, 3, new Scalar(255,255,0));
				//Core.polylines(mRgba, listmop , false, new Scalar(0,255,255));
				Core.line(mRgba, startPoint, farhtestPoint, new Scalar(0,255,255));
				Core.line(mRgba, farhtestPoint, endPoint, new Scalar(0,255,255));
			}



			//			if (id == 2) { //Defect point
			//				double depth = (double)dList.get(i+1)/256.0;
			//				curPoint = contourPts[dList.get(i)];
			//
			//				Point curPoint0 = contourPts[dList.get(i-2)];
			//				Point curPoint1 = contourPts[dList.get(i-1)];
			//				Point vec0 = new Point(curPoint0.x - curPoint.x, curPoint0.y - curPoint.y);
			//				Point vec1 = new Point(curPoint1.x - curPoint.x, curPoint1.y - curPoint.y);
			//				double dot = vec0.x*vec1.x + vec0.y*vec1.y;
			//				double lenth0 = Math.sqrt(vec0.x*vec0.x + vec0.y*vec0.y);
			//				double lenth1 = Math.sqrt(vec1.x*vec1.x + vec1.y*vec1.y);
			//				double cosTheta = dot/(lenth0*lenth1);
			//
			//				if ((depth > hg.inCircleRadius*0.7)&&(cosTheta>=-0.7)
			//						&& (!isClosedToBoundary(curPoint0, rgbaMat))
			//						&&(!isClosedToBoundary(curPoint1, rgbaMat))
			//						){
			//
			//
			//					hg.defectIdAfter.add((i));
			//
			//
			//					Point finVec0 = new Point(curPoint0.x-hg.inCircle.x,
			//							curPoint0.y-hg.inCircle.y);
			//					double finAngle0 = Math.atan2(finVec0.y, finVec0.x);
			//					Point finVec1 = new Point(curPoint1.x-hg.inCircle.x,
			//							curPoint1.y - hg.inCircle.y);
			//					double finAngle1 = Math.atan2(finVec1.y, finVec1.x);
			//
			//
			//
			//					if (hg.fingerTipsOrdered.size() == 0) {
			//						hg.fingerTipsOrdered.put(finAngle0, curPoint0);
			//						hg.fingerTipsOrdered.put(finAngle1, curPoint1);
			//
			//					} else {
			//
			//
			//						hg.fingerTipsOrdered.put(finAngle0, curPoint0);
			//
			//
			//						hg.fingerTipsOrdered.put(finAngle1, curPoint1);
			//
			//
			//					}
			//
			//				}
			//
			//
			//
			//			}
		}
	}

	//List<MatOfPoint> hullPoints = new ArrayList<MatOfPoint>();

	//convex hull of all contours
	//	for (int k=0; k < contours.size(); k++){
	//	    MatOfInt hullInt=new MatOfInt();
	//		Imgproc.convexHull(contours.get(k), hullInt);
	//
	//	    List<Point> hullPointList=new ArrayList<Point>();
	//		for(int j=0; j < hullInt.toList().size(); j++){
	//	        
	//	        	hullPointList.add( contours.get(k).toList().get(hullInt.toList().get(j)));
	//	        					
	//	    }
	//
	//	    MatOfPoint hullPointMat=new MatOfPoint();
	//		hullPointMat.fromList(hullPointList);
	//	    hullPoints.add(hullPointMat);   
	//	}
	//
	//	Imgproc.drawContours(mRgba, hullPoints, -1,  new Scalar(255,0,0, 255), 1);



	//Mat mask=new Mat();
	//Imgproc.cvtColor(fgmask, mask, Imgproc.COLOR_GRAY2RGB);
	//mRgba.copyTo(mRgba, mask);
	//mRgb.release();
	//mRgba=fgmask;


	//Gaussian blur

	//Imgproc.GaussianBlur(mRgb, mRgb, new org.opencv.core.Size(5,5), 0);

	//Imgproc.threshold(mRgb, mRgb, 80, 255, Imgproc.THRESH_OTSU+Imgproc.THRESH_BINARY_INV);



	//Core.bitwise_not(mRgb, mRgb);
	//Imgproc.cvtColor(mRgb, mRgb, Imgproc.COLOR_GRAY2RGBA);

	Imgproc.cvtColor(hsv_mask, hsv_mask, Imgproc.COLOR_GRAY2RGBA,4);
	//Imgproc.erode(hsv_mask, hsv_mask, new Mat());
	//Imgproc.dilate(hsv_mask, hsv_mask, new Mat());
	//Imgproc.GaussianBlur(hsv_mask, hsv_mask, new org.opencv.core.Size(5,5), 0);

	//Imgproc.resize(hsv_mask, hsv_mask,rect.size());
	//hsv_mask.copyTo(mRgb.colRange(0, 127),mRgb.rowRange(0,127));
	//Core.addWeighted(mRgba, 0.5, hsv_mask, 0.5, 0, mRgba);

	//Mat mat =new Mat(mRgba.rows(), mRgba.cols(), CvType.CV_8UC4);
	//Rect rect=new Rect(0, 0, mat.cols()/2, mat.rows()/2);
	//Mat roi=mat.submat(rect);

	Rect rect=new Rect(0, 0, mRgba.cols()/4, mRgba.rows()/4);
	Rect rect2=new Rect(mRgba.cols()/4, mRgba.rows()/4, mRgba.cols()/4*2, mRgba.rows()/4*2);
	Mat roi=mRgba.submat(rect);
	Mat roi2=mRgba.submat(rect2);
	//hsv_mask.copyTo(roi);
	//roi.setTo(new Scalar(255,255,255));
	//this.mOpenCvCameraView.enableFpsMeter();
	//	Imgproc.resize(mRgb2, mRgb2,roi2.size());

	Imgproc.resize(hsv_mask, hsv_mask,roi.size());
	hsv_mask.copyTo(roi);
	//mRgb2.copyTo(roi2);
	//mRgb2.release();

	return mRgba;


	//	Mat localMat1;
	//	if (this.palmsample)
	//	{
	//		this.mRgba = palmoverlay(this.mRgba);
	//		localMat1 = this.mRgba;
	//	}
	//	else
	//	{
	//
	//		if (this.palmdetected)
	//		{
	//			this.mDetector.process(this.mRgba);
	//			List localList = this.mDetector.getContours();
	//			Log.e("OCVSample::Activity", "Contours count: " + localList.size());
	//			Imgproc.drawContours(this.mRgba, localList, -1, this.CONTOUR_COLOR);
	//			this.mRgba.submat(4, 68, 4, 68).setTo(this.mBlobColorRgba);
	//			Mat localMat2 = this.mRgba.submat(4, 4 + this.mSpectrum.rows(), 70, 70 + this.mSpectrum.cols());
	//			this.mSpectrum.copyTo(localMat2);
	//			localMat1 = this.mRgba;
	//		}
	//		else
	//		{
	//			localMat1 = this.mRgba;
	//		}
	//	}
	//	Rect rect=new Rect(4, 4, 68, 68);
	//	System.out.println(rect.toString());
	//	// Mat roi = new Mat(localMat1,rect);
	//	Mat roi = mRgba.submat(4, 68, 4, 68);
	//	
	//	 Imgproc.resize(hsv_mask, hsv_mask, new org.opencv.core.Size(128,128));
	//	Imgproc.cvtColor(hsv_mask, hsv_mask, Imgproc.COLOR_GRAY2RGBA,4);
	//	
	//	hsv_mask.copyTo(mRgba.colRange(0, 127),mRgba.rowRange(0,127));
	//	 //roi.copyTo(hsv_mask);
	//	
	//	return mRgba;
}

@Override
public boolean onCreateOptionsMenu(Menu menu) {
	List<String> effects = mOpenCvCameraView.getEffectList();

	if (effects == null) {
		Log.e(TAG, "Color effects are not supported by device!");


	}
	else{
		mColorEffectsMenu = menu.addSubMenu("Color Effect");
		mEffectMenuItems = new MenuItem[effects.size()];

		int idx = 0;
		ListIterator<String> effectItr = effects.listIterator();
		while(effectItr.hasNext()) {
			String element = effectItr.next();
			mEffectMenuItems[idx] = mColorEffectsMenu.add(1, idx, Menu.NONE, element);
			idx++;
		}}

	mResolutionMenu = menu.addSubMenu("Resolution");
	mResolutionList = mOpenCvCameraView.getResolutionList();
	mResolutionMenuItems = new MenuItem[mResolutionList.size()];

	ListIterator<Size> resolutionItr = mResolutionList.listIterator();
	int idx = 0;
	while(resolutionItr.hasNext()) {
		Size element = resolutionItr.next();
		mResolutionMenuItems[idx] = mResolutionMenu.add(2, idx, Menu.NONE,
				Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString());
		idx++;
	}

	mCameraMenu = menu.addSubMenu("Camera");

	mCameranMenuItems = new MenuItem[2];
	mCameranMenuItems[0]=mCameraMenu.add(3,mOpenCvCameraView.CAMERA_ID_BACK, Menu.NONE,"BackCamera");
	mCameranMenuItems[1]=mCameraMenu.add(3,mOpenCvCameraView.CAMERA_ID_FRONT, Menu.NONE,"FrontCamera");



	return true;
}

public boolean onOptionsItemSelected(MenuItem item) {
	Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
	if (item.getGroupId() == 1)
	{
		mOpenCvCameraView.setEffect((String) item.getTitle());
		Toast.makeText(this, mOpenCvCameraView.getEffect(), Toast.LENGTH_SHORT).show();
	}
	else if (item.getGroupId() == 2)
	{
		int id = item.getItemId();
		Size resolution = mResolutionList.get(id);
		mOpenCvCameraView.setResolution(resolution);
		resolution = mOpenCvCameraView.getResolution();
		String caption = Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
		Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
	}

	else if ((item.getGroupId() == 3))
	{
		int id = item.getItemId();
		mOpenCvCameraView.setCameraIndex(id);
		mOpenCvCameraView.disableView();
		//mbackgroundsubstractor=new BackgroundSubtractorMOG();
		mOpenCvCameraView.enableView();

		
	}

	return true;
}


@Override
public boolean onTouch(View v, MotionEvent event) {
	int cols = mRgba.cols();
	int rows = mRgba.rows();

	int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
	int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

	int x = (int)event.getX() - xOffset;
	int y = (int)event.getY() - yOffset;


	if (!this.palmdetected)
	{
		this.palmdetected = !palmdetected;
		this.palmsample = false;
		this.mBlobColorHsv = HandDetection.calc_avg_palm_color(this.mRgba);
		this.mBlobColorRgba = converScalarHsv2Rgba(this.mBlobColorHsv);
		Log.i("OCVSample::Activity", "Touched rgba color: (" + this.mBlobColorRgba.val[0] + ", " + this.mBlobColorRgba.val[1] + ", " + this.mBlobColorRgba.val[2] + ", " + this.mBlobColorRgba.val[3] + ")");
		this.mDetector.setHsvColor(this.mBlobColorHsv);
		Imgproc.resize(this.mDetector.getSpectrum(), this.mSpectrum, this.SPECTRUM_SIZE);
		return false;
	}
	return false;
}
private Scalar converScalarHsv2Rgba(Scalar paramScalar)
{
	Mat localMat = new Mat();
	Imgproc.cvtColor(new Mat(1, 1, CvType.CV_8UC3, paramScalar), localMat,71, 4);
	return new Scalar(localMat.get(0, 0));
}

private Mat palmoverlay(Mat paramMat)
{
	if (HandDetection.roi.size() != 7)
	{
		Rect localRect1 = new Rect(new Point(paramMat.cols() / 3, paramMat.rows() / 6), new Point(10 + paramMat.cols() / 3, 10 + paramMat.rows() / 6));
		Rect localRect2 = new Rect(new Point(paramMat.cols() / 4, paramMat.rows() / 2), new Point(10 + paramMat.cols() / 4, 10 + paramMat.rows() / 2));
		Rect localRect3 = new Rect(new Point(paramMat.cols() / 3, paramMat.rows() / 1.5D), new Point(10 + paramMat.cols() / 3, 10.0D + paramMat.rows() / 1.5D));
		Rect localRect4 = new Rect(new Point(paramMat.cols() / 2, paramMat.rows() / 2), new Point(10 + paramMat.cols() / 2, 10 + paramMat.rows() / 2));
		Rect localRect5 = new Rect(new Point(paramMat.cols() / 2.5D, paramMat.rows() / 2.5D), new Point(10.0D + paramMat.cols() / 2.5D, 10.0D + paramMat.rows() / 2.5D));
		Rect localRect6 = new Rect(new Point(paramMat.cols() / 2, paramMat.rows() / 1.5D), new Point(10 + paramMat.cols() / 2, 10.0D + paramMat.rows() / 1.5D));
		Rect localRect7 = new Rect(new Point(paramMat.cols() / 2.5D, paramMat.rows() / 1.8D), new Point(10.0D + paramMat.cols() / 2.5D, 10.0D + paramMat.rows() / 1.8D));
		HandDetection.roi.add(localRect1);
		HandDetection.roi.add(localRect2);
		HandDetection.roi.add(localRect3);
		HandDetection.roi.add(localRect4);
		HandDetection.roi.add(localRect5);
		HandDetection.roi.add(localRect6);
		HandDetection.roi.add(localRect7);
	}
	for (int i = 0; ; i++)
	{
		if (i >= 7)
		{
			Log.i("OCVSample::Activity", "drawed on pic");
			return paramMat;
		}
		Core.rectangle(paramMat, ((Rect)HandDetection.roi.get(i)).tl(), ((Rect)HandDetection.roi.get(i)).br(), new Scalar(255.0D, 255.0D, 255.0D));
	}
}





}