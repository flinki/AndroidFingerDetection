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


//Used Opencv Tutorial example Code
public class MainActivity extends Activity implements CvCameraViewListener2,
View.OnTouchListener {
	private static final String TAG = "OCVSample::Activity";
	private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
	public static final int JAVA_DETECTOR = 0;
	public static final int NATIVE_DETECTOR = 1;
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

	private BackgroundSubtractorMOG mbackgroundsubstractor;
	private Scalar mBlobColorHsv;
	private Scalar mBlobColorRgba;
	private File mCascadeFile;
	private CascadeClassifier mJavaDetector;
	private DetectionBasedTracker mNativeDetector;
	private int mDetectorType = NATIVE_DETECTOR;
	private String[] mDetectorName;
	private boolean fastmode=false;
	private float mRelativeFaceSize = 0.2f;
	private int mAbsoluteFaceSize = 0;
	private double[] skincolorHSV=new double[3];
	private Audioplayer audioPlayer=new Audioplayer(this);


	// Simon
	private BinaryImageGenerator binaryImageGenerator = null;
	public static final int BACKGROUND_MODE = 1;
	public static final int SAMPLE_MODE = 2;
	public static final int DETECTION_MODE = 3;
	// Initial mode is BACKGROUND_MODE to presample the colors of the hand
	private int mode = BACKGROUND_MODE;
	private Mat interMat ;;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");

				// Load native library after(!) OpenCV initialization
				System.loadLibrary("detection_based_tracker");

				try {
					// load cascade file from application resources
					InputStream is = getResources().openRawResource(
							R.raw.lbpcascade_frontalface);
					File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
					mCascadeFile = new File(cascadeDir,
							"lbpcascade_frontalface.xml");
					FileOutputStream os = new FileOutputStream(mCascadeFile);

					byte[] buffer = new byte[4096];
					int bytesRead;
					while ((bytesRead = is.read(buffer)) != -1) {
						os.write(buffer, 0, bytesRead);
					}
					is.close();
					os.close();

					mJavaDetector = new CascadeClassifier(
							mCascadeFile.getAbsolutePath());
					if (mJavaDetector.empty()) {
						Log.e(TAG, "Failed to load cascade classifier");
						mJavaDetector = null;
					} else
						Log.i(TAG, "Loaded cascade classifier from "
								+ mCascadeFile.getAbsolutePath());

					mNativeDetector = new DetectionBasedTracker(
							mCascadeFile.getAbsolutePath(), 0);

					cascadeDir.delete();

				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
				}

				binaryImageGenerator = new BinaryImageGenerator();
				mOpenCvCameraView.enableView();
			}
			break;
			default: {
				super.onManagerConnected(status);
			}
			break;
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
		skincolorHSV[0] = 20;
		skincolorHSV[1]=150;
		skincolorHSV[2]=255;
		mOpenCvCameraView = (Tutorial3View) findViewById(R.id.tutorial3_activity_java_surface_view);

		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCameraIndex(1);
		mOpenCvCameraView.setCvCameraViewListener(this);
		mOpenCvCameraView.setOnTouchListener(this);

		// mOpenCvCameraView.setResolution(mOpenCvCameraView.getResolutionList().get(5));



	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this,
				mLoaderCallback);
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
		interMat=new Mat();



		// Simon

	}

	public void onCameraViewStopped() {


	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

		this.mRgba = inputFrame.rgba();
		this.mGray = inputFrame.gray();
		Mat hsv_mask=new Mat();


		// face detection OpenCV example code
		//if(mode==DETECTION_MODE){
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
				mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, 
						// objdetect.CV_HAAR_SCALE_IMAGE
						new org.opencv.core.Size(mAbsoluteFaceSize,
								mAbsoluteFaceSize), new org.opencv.core.Size());
		} else if (mDetectorType == NATIVE_DETECTOR) {
			if (mNativeDetector != null)
				mNativeDetector.detect(mGray, faces);
		} else {
			Log.e(TAG, "Detection method is not selected!");
		}
		Rect[] facesArray = faces.toArray();
		for (int i = 0; i < facesArray.length; i++) {
			// draw face rectange
			// Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(),
			// FACE_RECT_COLOR, 3);

			// set pixels of face to black
			mRgba.submat(facesArray[i]).setTo(new Scalar(255, 255, 255));

		}




		// simon


		if(!fastmode){

			if (mode == BACKGROUND_MODE) { 
				return binaryImageGenerator.presampleBackground(mRgba);
			} else if (mode == SAMPLE_MODE) { 
				return binaryImageGenerator.presampleHand(mRgba);
			} else if (mode == DETECTION_MODE) { 



				hsv_mask= binaryImageGenerator.produceBinaryImage(mRgba);
			}
		}
		else{
			Mat mhsv=new Mat();


			Imgproc.cvtColor(mRgba, mhsv, Imgproc.COLOR_RGB2HSV);
			//Scalar hsv_min = new Scalar(0, 30, 80, 0);
			//Scalar hsv_max = new Scalar(20, 150, 255, 0);

			//Scalar hsv_min = new Scalar(0, 43, 80, 0);
			//Scalar hsv_max = new Scalar(25, 255, 255, 0);


			Scalar hsv_min = new Scalar((skincolorHSV[0]), skincolorHSV[1], skincolorHSV[2], 0);
			Scalar hsv_max = new Scalar(skincolorHSV[0]+10, skincolorHSV[1]+10, skincolorHSV[2]+10, 0);
			// colorrange
			Core.inRange(mhsv, hsv_min, hsv_max, hsv_mask);
			mhsv.release();
			//Imgproc.GaussianBlur(hsv_mask, hsv_mask,
			//		new org.opencv.core.Size(3, 3), 0);

			Imgproc.erode(hsv_mask, hsv_mask, new Mat());

			Imgproc.dilate(hsv_mask, hsv_mask, new Mat());
			mode=DETECTION_MODE;

		}



		// if(!mRgba.empty()){
		// return mRgba;}

		// System.out.println(inputFrame.rgba().size());//cols/spalte/x 1280
		// rows/reihe/y 720

		if(mode==DETECTION_MODE){
			Point mompoint = new Point();
			List<Finger> potentialfingerL = new ArrayList<Finger>();
			//reset 
			Finger.reset();

			List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

			Imgproc.findContours(hsv_mask.clone(), contours, new Mat(),
					Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

			List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
			// Find max contour area Opencv example code
			double maxArea = 0;
			Iterator<MatOfPoint> each = contours.iterator();
			while (each.hasNext()) {
				MatOfPoint wrapper = each.next();
				double area = Imgproc.contourArea(wrapper);
				if (area > maxArea)
					maxArea = area;
			}


			// Filter contours by area and resize to fit the original image size

			double mMinContourArea = 0.7;
			each = contours.iterator();
			while (each.hasNext()) {
				MatOfPoint contour = each.next();





				if (Imgproc.contourArea(contour) > mMinContourArea * maxArea) {

					mContours.add(contour);
				}
			}


			//Iterate over all contours /hands
			for (int g = 0; g < mContours.size(); g++) {
				if (mContours.size() > 0) {


					//Invrease Hand count
					Finger.numOfHands++;
					// Imgproc.drawContours(mRgba,mContours , -1, new Scalar(0,0,255));

					//find bounding box of hand
					Rect brect = Imgproc.boundingRect(mContours.get(g));
					Core.rectangle(mRgba, brect.tl(), brect.br(), new Scalar(0, 255, 0));

					// calc moments center point

					Moments momP = Imgproc.moments(mContours.get(g),false);
					int px = (int) (momP.get_m10() / momP.get_m00());
					int py = (int) (momP.get_m01() / momP.get_m00());
					mompoint=new Point(px,py);
					Core.circle(mRgba, new Point(px, py), 4, new Scalar(255, 49, 0,
							255));

					//}


					MatOfPoint2f mMOP2f1 = new MatOfPoint2f();
					mContours.get(g).convertTo(mMOP2f1, CvType.CV_32FC2);
					//MatOfPoint2f pts = new MatOfPoint2f(mContours.get(g).toArray());
					//find min area rectangle
					RotatedRect mrect = Imgproc.minAreaRect(mMOP2f1);

					Point[] rect_points = new Point[4];
					mrect.points(rect_points);

					//draw rectangle
					for (int j = 0; j < 4; j++) {
						Core.line(mRgba, rect_points[j], rect_points[(j + 1) % 4],
								new Scalar(255, 255, 0));
					}
					Core.circle(mRgba, mrect.center, (int) (mrect.size.height / 2),
							new Scalar(255, 0, 255));

					// convex hull for biggest contours
					List<MatOfPoint> hullPoints = new ArrayList<MatOfPoint>();
					MatOfInt hullInt = new MatOfInt();
					Imgproc.convexHull(mContours.get(g), hullInt);
					List<Point> hullPointList = new ArrayList<Point>();
					for (int j = 0; j < hullInt.toList().size(); j++) {

						hullPointList.add(mContours.get(g).toList()
								.get(hullInt.toList().get(j)));

					}

					MatOfPoint hullPointMat = new MatOfPoint();
					hullPointMat.fromList(hullPointList);
					hullPoints.add(hullPointMat);

					// draw convex hull
					Imgproc.drawContours(mRgba, hullPoints, -1, new Scalar(255, 0, 0,
							255), 1);


					//find  convexity defect
					MatOfInt4 convexityDefects = new MatOfInt4();
					if(hullInt.rows() > 3){
						Imgproc.convexityDefects(mContours.get(g), hullInt,
								convexityDefects);
					}else break;
					List<Integer> dList = convexityDefects.toList();
					Point[] contourPts = mContours.get(g).toArray();


					//retreive all start points, end points, farthest points, approximate distances
					for (int i = 0; i < dList.size(); i = i + 4) {
						if (dList.size() > 0) {


							int startpointind = dList.get(i );
							int endpointind = dList.get(i  + 1);
							int farhestpointind = dList.get(i  + 2);
							int approxdistance = dList.get(i  + 3);
							Point startPoint = contourPts[startpointind];
							Point endPoint = contourPts[endpointind];
							Point farhtestPoint = contourPts[farhestpointind];
							List<Point> list = new ArrayList<Point>();
							list.add(startPoint);
							list.add(farhtestPoint);
							list.add(endPoint);

							MatOfPoint matofpoint = new MatOfPoint();
							matofpoint.fromList(list);

							List<MatOfPoint> listmop = new ArrayList<MatOfPoint>();
							for (Point pi : list) {

								listmop.add(new MatOfPoint(pi));
							}


							//	if((mrect.size.height / 2)>approxdistance/256 && approxdistance/256>Finger.mindefectdepth){

							//filter defects for min defectdepth
							if(approxdistance/256>Finger.mindefectdepth ){

								//calc angle between vectors 
								Point vecFtoS= new Point(farhtestPoint.x-startPoint.x,farhtestPoint.y-startPoint.y);
								Point vecFtoE= new Point(farhtestPoint.x-endPoint.x,farhtestPoint.y-endPoint.y);


								double dot=(vecFtoS.dot(vecFtoE));
								double betrag=Math.sqrt(vecFtoS.x*vecFtoS.x+(vecFtoS.y*vecFtoS.y))*
										Math.sqrt(vecFtoE.x*vecFtoE.x+(vecFtoE.y*vecFtoE.y));
								double angle = Math.acos(dot/betrag);
								double angleD = Math.toDegrees(angle);

								if(angleD<110 && angleD>15 ){

									//																		Core.line(mRgba, startPoint, farhtestPoint, new Scalar(0,
									//																				255, 255));
									//																		Core.line(mRgba, farhtestPoint, endPoint, new Scalar(0,
									//																				255, 0));
									//	Core.putText(mRgba, Integer.toString(((int)angleD)), farhtestPoint, Core.FONT_ITALIC, 0.5, new Scalar(255, 255, 255));
									double flaeche = betrag*Math.sin(angle);
									potentialfingerL.add(new Finger(startPoint,farhtestPoint,endPoint,approxdistance/256,angleD,flaeche,g));

								}
							}
						}





					}
				}
			}

			//Iterate over finger
			Iterator<Finger> iter = potentialfingerL.iterator();
			while (iter.hasNext()) {
				Finger f=iter.next();
				if(f.distance<Finger.maxdistance*0.4){
					iter.remove();
				}
				else{

					if(f.area==Finger.maxarea){f.isthumb=true;}

					f.draw(mRgba);
					//draw angle
					//					Core.putText(mRgba, Integer.toString((int)f.angleD), f.far, Core.FONT_ITALIC, 0.5, new Scalar(255, 255, 255));
					//					Core.line(mRgba, f.start, f.far, new Scalar(0,
					//							255, 255));
					//					Core.line(mRgba, f.far, f.end, new Scalar(0,
					//							255, 0));
				}
			}
			System.out.println(Finger.maxdistance);
			Core.circle(mRgba, mompoint, (int)Finger.maxdistance, new Scalar(125, 125, 0));
			int fingertips=Finger.calcnumbfingertips();
			String log="Fingertips:"+Integer.toString(fingertips)+"--"+Finger.numOfHands+"   ";
			for(int o =0; o<Finger.handId.length;o++){

				if(Finger.handId[o]>0)
					log=log+ "Hand:"+o+" Defects:"+Finger.handId[o];
			}
			Core.putText(mRgba, log, new Point(0,mRgba.rows()-10),Core.FONT_ITALIC , 0.5,new Scalar(255, 255, 255));
			if(Finger.numOfHands>0){
			audioPlayer.add(fingertips, 3);
			}

			Imgproc.cvtColor(hsv_mask, hsv_mask, Imgproc.COLOR_GRAY2RGBA, 4);


			this.mOpenCvCameraView.enableFpsMeter();

			Rect rect=new Rect(0, 0, mRgba.cols()/4, mRgba.rows()/4);

			Mat roi=mRgba.submat(rect);


			Imgproc.resize(hsv_mask, hsv_mask,roi.size());
			hsv_mask.copyTo(roi);
			return mRgba;
		}
		else{return mRgba;}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		List<String> effects = mOpenCvCameraView.getEffectList();

		if (effects == null) {
			Log.e(TAG, "Color effects are not supported by device!");

		} else {
			mColorEffectsMenu = menu.addSubMenu("Color Effect");
			mEffectMenuItems = new MenuItem[effects.size()];

			int idx = 0;
			ListIterator<String> effectItr = effects.listIterator();
			while (effectItr.hasNext()) {
				String element = effectItr.next();
				mEffectMenuItems[idx] = mColorEffectsMenu.add(1, idx,
						Menu.NONE, element);
				idx++;
			}
		}

		mResolutionMenu = menu.addSubMenu("Resolution");
		mResolutionList = mOpenCvCameraView.getResolutionList();
		mResolutionMenuItems = new MenuItem[mResolutionList.size()];

		ListIterator<Size> resolutionItr = mResolutionList.listIterator();
		int idx = 0;
		while (resolutionItr.hasNext()) {
			Size element = resolutionItr.next();
			mResolutionMenuItems[idx] = mResolutionMenu.add(2, idx, Menu.NONE,
					Integer.valueOf(element.width).toString() + "x"
							+ Integer.valueOf(element.height).toString());
			idx++;
		}

		mCameraMenu = menu.addSubMenu("Camera");

		mCameranMenuItems = new MenuItem[2];
		mCameranMenuItems[0] = mCameraMenu.add(3,
				mOpenCvCameraView.CAMERA_ID_BACK, Menu.NONE, "BackCamera");
		mCameranMenuItems[1] = mCameraMenu.add(3,
				mOpenCvCameraView.CAMERA_ID_FRONT, Menu.NONE, "FrontCamera");

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
		if (item.getGroupId() == 1) {
			mOpenCvCameraView.setEffect((String) item.getTitle());
			Toast.makeText(this, mOpenCvCameraView.getEffect(),
					Toast.LENGTH_SHORT).show();
		} else if (item.getGroupId() == 2) {
			int id = item.getItemId();
			Size resolution = mResolutionList.get(id);
			mOpenCvCameraView.setResolution(resolution);
			resolution = mOpenCvCameraView.getResolution();
			String caption = Integer.valueOf(resolution.width).toString() + "x"
					+ Integer.valueOf(resolution.height).toString();
			Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
		}

		else if ((item.getGroupId() == 3)) {
			int id = item.getItemId();
			mOpenCvCameraView.setCameraIndex(id);
			mOpenCvCameraView.disableView();
			// mbackgroundsubstractor=new BackgroundSubtractorMOG();
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

		int x = (int) event.getX() - xOffset;
		int y = (int) event.getY() - yOffset;
		Mat mhsv=new Mat();
		Imgproc.cvtColor(mRgba, mhsv, Imgproc.COLOR_RGB2HSV);
		skincolorHSV = mhsv.get(x, y);

		switch (mode) {
		case (BACKGROUND_MODE):
			// rgbaMat.copyTo(backMat);
			// this.binaryImageGenerator.setBackMat(rgbaMat);
			mode = SAMPLE_MODE;
		Toast.makeText(getApplicationContext(), "Background sampled",
				Toast.LENGTH_LONG).show();
		return false;
		case (SAMPLE_MODE):
			Toast.makeText(getApplicationContext(), "Sampling finished",
					Toast.LENGTH_LONG).show();
		mode = DETECTION_MODE;
		return false;
		default: {
			mode = BACKGROUND_MODE;
			return true;
		}

		}

		// if (!this.palmdetected)
		// {
		// this.palmdetected = !palmdetected;
		// this.palmsample = false;
		// this.mBlobColorHsv = HandDetection.calc_avg_palm_color(this.mRgba);
		// this.mBlobColorRgba = converScalarHsv2Rgba(this.mBlobColorHsv);
		// Log.i("OCVSample::Activity", "Touched rgba color: (" +
		// this.mBlobColorRgba.val[0] + ", " + this.mBlobColorRgba.val[1] + ", "
		// + this.mBlobColorRgba.val[2] + ", " + this.mBlobColorRgba.val[3] +
		// ")");
		// this.mDetector.setHsvColor(this.mBlobColorHsv);
		// Imgproc.resize(this.mDetector.getSpectrum(), this.mSpectrum,
		// this.SPECTRUM_SIZE);
		// return false;
		// }
		// return false;
	}

	private Scalar converScalarHsv2Rgba(Scalar paramScalar) {
		Mat localMat = new Mat();
		Imgproc.cvtColor(new Mat(1, 1, CvType.CV_8UC3, paramScalar), localMat,
				71, 4);
		return new Scalar(localMat.get(0, 0));
	}





	static class  Finger{

		static final int font=Core.FONT_ITALIC;
		static final float size=0.5f;
		static final Scalar colortext = new Scalar(255, 255, 255);
		static final Scalar startcolor=new Scalar(0,255, 255);
		static final Scalar endcolor=new Scalar(0,255, 0);
		static final Scalar fingertip=new Scalar(255,255, 255);


		static int numOfHands=0;
		static final int mindefectdepth=20;
		static double maxarea;
		static double maxdistance=0;
		static int numberOfdefects=0;





		public Point start;
		public Point far;
		public Point end;
		public double distance;
		public double angleD;
		public double area;
		public boolean isthumb=false;
		public static int handId[]=new int[5];
		public int handID=0;

		Finger(Point a,Point b,Point c,double d,double angle,double flaeche,int id){

			start=a;
			far=b;
			end=c;
			distance=d;
			if(maxdistance<distance){maxdistance=distance;};
			angleD=angle;
			area=flaeche;
			if(maxarea<flaeche){maxarea=flaeche;};
			if(id<5){
				handId[id]=0;
				handID=id;
			}
		}


		void draw(Mat image){

			Core.putText(image, Integer.toString((int)this.angleD), this.far,Finger.font , 0.5,Finger.colortext);
			Core.line(image, this.start, this.far,Finger.startcolor );
			Core.line(image, this.far, this.end, Finger.endcolor);
			Core.circle(image, end, 2,Finger.fingertip , -1);
			Finger.numberOfdefects++;
			handId[handID]++;
			//if(this.isthumb){

			//Core.circle(image, start, 2,Finger.fingertip , -1);
			//Core.putText(image, "Thumb", this.start,Finger.font , 0.5,Finger.colortext);




		}

		static void reset(){

			Finger.maxarea=0;
			Finger.maxdistance=0;
			Finger.numberOfdefects=0;
			Finger.numOfHands=0;
			for(int i=0;i<Finger.handId.length;i++)
				Finger.handId[i]=0;


		}

		static int calcnumbfingertips(){

			int f=0;
			for(int o =0; o<Finger.handId.length;o++){

				if(Finger.handId[o]>0)
				{
					f=f+handId[o]+1;
				}
			}
			return f;

		}
	};

}

