package it.peretti.kofler.androidfingerdetection;

import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class HandDetection
{
	static final int NofSamples = 7;
	static int[][] avgColor;
	static int[][] c_lower;
	static int[][] c_upper;
	static ArrayList<Rect> roi = new ArrayList();

	static
	{
		int[] arrayOfInt1 = { 7, 3 };
		avgColor = (int[][])Array.newInstance(Integer.TYPE, arrayOfInt1);
		int[] arrayOfInt2 = { 7, 3 };
		c_lower = (int[][])Array.newInstance(Integer.TYPE, arrayOfInt2);
		int[] arrayOfInt3 = { 7, 3 };
	}

	
	

	public static Mat blur(Mat paramMat)
	{
		Imgproc.blur(paramMat, paramMat, new Size(3.0D, 3.0D));
		return paramMat;
	}

	public static Scalar calc_avg_palm_color(Mat paramMat)
	{
		Mat localMat=new Mat();
		Imgproc.cvtColor(paramMat, localMat, Imgproc.COLOR_RGB2HSV);
		ArrayList localArrayList = new ArrayList();
		int i = 0;
		if (i >= roi.size())
			return getmedianhslvalue(localArrayList);
		Scalar localScalar = Core.sumElems(localMat.submat((Rect)roi.get(i)));
		int j = ((Rect)roi.get(i)).width * ((Rect)roi.get(i)).height;
		for (int k = 0; ; k++)
		{
			if (k >= localScalar.val.length)
			{
				localArrayList.add(localScalar.clone());
				i++;
				break;
			}
			double[] arrayOfDouble = localScalar.val;
			arrayOfDouble[k] /= j;


		}
		return getmedianhslvalue(localArrayList);
	}

	private static Scalar getmedianhslvalue(ArrayList<Scalar> paramArrayList)
	{
		paramArrayList.toArray();
		System.out.println(paramArrayList.toArray().toString());
		paramArrayList.toArray();


		Collections.sort(paramArrayList, new Comparator<Scalar>() {
			@Override
			public int compare(Scalar z1, Scalar z2) {
				if (z1.val[0] > z2.val[0] )
					return 1;
				if (z1.val[0]  < z2.val[0] )
					return -1;
				return 0;
			}
		});


		double H = paramArrayList.get(0).val[0];
		Collections.sort(paramArrayList, new Comparator<Scalar>() {
			@Override
			public int compare(Scalar z1, Scalar z2) {
				if (z1.val[1] > z2.val[1] )
					return 1;
				if (z1.val[1]  < z2.val[1] )
					return -1;
				return 0;
			}
		});
		double S =  paramArrayList.get(0).val[1];

		Collections.sort(paramArrayList, new Comparator<Scalar>() {
			@Override
			public int compare(Scalar z1, Scalar z2) {
				if (z1.val[2] > z2.val[2] )
					return 1;
				if (z1.val[2]  < z2.val[2] )
					return -1;
				return 0;
			}
		});

		double V =  paramArrayList.get(0).val[2];

		return new Scalar(H,S,V);
	}
}