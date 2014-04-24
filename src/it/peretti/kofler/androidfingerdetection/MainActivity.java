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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;




public class MainActivity extends Activity implements CvCameraViewListener2, View.OnTouchListener
{private static final String TAG = "OCVSample::Activity";
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

private Scalar mBlobColorHsv;
private Scalar mBlobColorRgba;

private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
    @Override
    public void onManagerConnected(int status) {
        switch (status) {
            case LoaderCallbackInterface.SUCCESS:
            {
                Log.i(TAG, "OpenCV loaded successfully");
                mOpenCvCameraView.enableView();
                mOpenCvCameraView.setOnTouchListener(MainActivity.this);
            } break;
            default:
            {
                super.onManagerConnected(status);
            } break;
        }
    }
};
private org.opencv.core.Size SPECTRUM_SIZE;

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

    mOpenCvCameraView.setCvCameraViewListener(this);
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
    OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
}

public void onDestroy() {
    super.onDestroy();
    if (mOpenCvCameraView != null)
        mOpenCvCameraView.disableView();
}

public void onCameraViewStarted(int width, int height) {
	
	 this.mRgba = new Mat(width, height, CvType.CV_8UC4);
	    this.mDetector = new ColorBlobDetector();
	    this.mSpectrum = new Mat();
	    this.mBlobColorRgba = new Scalar(255.0D);
	    this.mBlobColorHsv = new Scalar(255.0D);
	    this.SPECTRUM_SIZE = new org.opencv.core.Size(200.0D, 64.0D);
	    this.CONTOUR_COLOR = new Scalar(255.0D, 0.0D, 0.0D, 255.0D);
}

public void onCameraViewStopped() {
}

public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
	this.mRgba = inputFrame.rgba();
   
    Mat localMat1;
    if (this.palmsample)
    {
      this.mRgba = palmoverlay(this.mRgba);
      localMat1 = this.mRgba;
    }
    else
    {
      
      if (this.palmdetected)
      {
        this.mDetector.process(this.mRgba);
        List localList = this.mDetector.getContours();
        Log.e("OCVSample::Activity", "Contours count: " + localList.size());
        Imgproc.drawContours(this.mRgba, localList, -1, this.CONTOUR_COLOR);
        this.mRgba.submat(4, 68, 4, 68).setTo(this.mBlobColorRgba);
        Mat localMat2 = this.mRgba.submat(4, 4 + this.mSpectrum.rows(), 70, 70 + this.mSpectrum.cols());
        this.mSpectrum.copyTo(localMat2);
        localMat1 = this.mRgba;
      }
      else
      {
        localMat1 = this.mRgba;
      }
    }
	return localMat1;
}

@Override
public boolean onCreateOptionsMenu(Menu menu) {
    List<String> effects = mOpenCvCameraView.getEffectList();

    if (effects == null) {
        Log.e(TAG, "Color effects are not supported by device!");
        return true;
    }

    mColorEffectsMenu = menu.addSubMenu("Color Effect");
    mEffectMenuItems = new MenuItem[effects.size()];

    int idx = 0;
    ListIterator<String> effectItr = effects.listIterator();
    while(effectItr.hasNext()) {
       String element = effectItr.next();
       mEffectMenuItems[idx] = mColorEffectsMenu.add(1, idx, Menu.NONE, element);
       idx++;
    }

    mResolutionMenu = menu.addSubMenu("Resolution");
    mResolutionList = mOpenCvCameraView.getResolutionList();
    mResolutionMenuItems = new MenuItem[mResolutionList.size()];

    ListIterator<Size> resolutionItr = mResolutionList.listIterator();
    idx = 0;
    while(resolutionItr.hasNext()) {
        Size element = resolutionItr.next();
        mResolutionMenuItems[idx] = mResolutionMenu.add(2, idx, Menu.NONE,
                Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString());
        idx++;
     }

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
  Imgproc.cvtColor(new Mat(1, 1, CvType.CV_8UC3, paramScalar), localMat, 71, 4);
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