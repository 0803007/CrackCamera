package nchc.crackcamera;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.ExifTagConstants;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.apache.sanselan.formats.tiff.constants.TiffFieldTypeConstants;
import org.apache.sanselan.formats.tiff.write.TiffOutputDirectory;
import org.apache.sanselan.formats.tiff.write.TiffOutputField;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.PointF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Build;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import nchc.crackcamera.SquareView;

public class MainActivity extends Activity implements SensorEventListener {

	private static final int MENU_OPENCLOSE_SENSOR = Menu.FIRST,
			MENU_INIT_AB = Menu.FIRST + 1, MENU_GET_AB = Menu.FIRST + 2,
			MENU_TAKE_PICTURE = Menu.FIRST + 3,
			MENU_SHOW_PICTURE = Menu.FIRST + 4,
	        MENU_LASER_MEASURE = Menu.FIRST + 5,
			MENU_GYRO_ORI_SWITCH = Menu.FIRST + 6,
	        MENU_SAVE_REMOTEANGLE = Menu.FIRST + 7;

	// 基本原件
	private CameraPreview mCamPreview;
	private ImageButton buttonCapture;
	private ImageButton buttonFocus;
	private ImageButton buttonInit;
	private ImageButton mEVAddImageButton;
	private ImageButton mEVSubstractImageButton;
	private TextView mLogText;
	private TextView mLog2Text;
	private TextView mRotText;
	private TextView mAccText;
	private TextView mAngText;
	private ZoomControls zoomControls;
	private DrawImageView mImageView;
	
	//上面抽屜
	private CheckBox mGreenCheckBox;
	private SeekBar mRSeekbar;
	private SeekBar mRRGBSeekbar;
	private SeekBar mGRGBSeekbar;
	private SeekBar mBRGBSeekbar;
	private SeekBar mAreaSeekbar;
	private EditText mRText;
	private EditText mRRGBText;
	private EditText mGRGBText;
	private EditText mBRGBText;
	private EditText mAreaText;
	private EditText mWidthText;
	private EditText mHeightText;
	private Button mSetWHButton;
	private TextView RTextView;
	private TextView RRGBTextView;
	private TextView GRGBTextView;

	// Sensor values
	private SensorManager sensorManager;
	private double aAngle = 0.0;
	private double bAngle = 0.0;
	private float timestamp;
	private static final float NS2S = 1.0f / 1000000000.0f;
	float[] magneticFieldValues = new float[3];
	float[] accelerometerValues = new float[3];
	float[] gyroscopeValues = new float[3];
	float[] orientationValues = new float[3];
	float[] gyroscopeAngles = new float[3];
	float[] currentRotationMatrix = new float[9];

	// Update Rotation 資訊
	boolean bShowSensor = false;
	Handler mHandler;
	Timer updateTimer;
	TimerTask mClock;

	// Angle Data
	private int count1 = 0;
	private int count2 = 0;
	private int count3 = 0;
	private double tmp_aAngle = 0.0;
	private double tmp_bAngle = 0.0;
	private double aInitAngle = 0.0;
	private double bInitAngle = 0.0;
	private double aDiffAngle = 0.0;
	private double bDiffAngle = 0.0;
	private String posA; // wyc1,20140622
	private String posB; // wyc1,20140622
	private String posC; // wyc1,20140622
	private String posD; // wyc1,20140622
	private String tmpString; // wyc1,20140622
	private boolean key1 = false; // 完成getAngle後，key =
									// true，並顯示"Alpha,Beta 寫入檔案成功"到TextView
	private double W = 155.0; //139 mm 128mm 155mm
	private double H = 94.0; // 83mm   58mm  94mm

	// other
	private boolean bLaserMeasure = false;;
	private boolean bGreenLaser = true;
	private boolean bSamsungGC200 = false;
	private boolean bGyroSwitch = true;
	ContentResolver cr;
	String mFileName;
	File mDIRPath;
	int maxZoomLevel = 0;
	int currentZoomLevel = 0;
	Camera camera;
	public SquareView mSquareView;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 設置橫放
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		// 設置view
		setContentView(R.layout.activity_main);
		// 解析路徑
		cr = this.getContentResolver();
		/*
		 * getWindow().setFormat(PixelFormat.TRANSLUCENT);
		 * requestWindowFeature(Window.FEATURE_NO_TITLE);
		 * getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		 * WindowManager.LayoutParams.FLAG_FULLSCREEN);
		 */
		//init Rota
		initMath();

		//視窗元件
		mImageView = (DrawImageView) findViewById(R.id.drawImageView);
		mCamPreview = new CameraPreview(this, camPreviewCallback);
		((FrameLayout) findViewById(R.id.preview)).addView(mCamPreview);
		mSquareView = new SquareView(this);
		((FrameLayout) findViewById(R.id.preview)).addView(mSquareView);

		mGreenCheckBox = (CheckBox)findViewById(R.id.greenCheckBox);
		mRSeekbar = (SeekBar)findViewById(R.id.RSeekBar);
		mRRGBSeekbar = (SeekBar)findViewById(R.id.RRGBSeekBar);
		mGRGBSeekbar = (SeekBar)findViewById(R.id.GRGBSeekBar);
		mBRGBSeekbar = (SeekBar)findViewById(R.id.BRGBseekBar);
		mAreaSeekbar = (SeekBar)findViewById(R.id.AREAseekBar); 
		mRText = (EditText)findViewById(R.id.REditText);
		mRRGBText = (EditText)findViewById(R.id.RRGBEditText);
		mGRGBText = (EditText)findViewById(R.id.GRGBEditText);
		mBRGBText = (EditText)findViewById(R.id.BRGBEditText);
		mAreaText = (EditText)findViewById(R.id.AreaEditText);
		RTextView = (TextView)findViewById(R.id.RTextView);
		RRGBTextView = (TextView)findViewById(R.id.RRGBTextView);
		GRGBTextView = (TextView)findViewById(R.id.GRGBTextView);

		mLogText = (TextView) findViewById(R.id.textViewLog);
		mLog2Text = (TextView) findViewById(R.id.textViewLog2);
		mRotText = (TextView) findViewById(R.id.textViewRot);
		mAccText = (TextView) findViewById(R.id.textViewAcc);
		mAngText = (TextView) findViewById(R.id.textViewAng);
		
		mRSeekbar.setOnSeekBarChangeListener(rseekBarOnChangeLis);
		mRRGBSeekbar.setOnSeekBarChangeListener(rrgbseekBarOnChangeLis);
		mGRGBSeekbar.setOnSeekBarChangeListener(grgbseekBarOnChangeLis);
		mBRGBSeekbar.setOnSeekBarChangeListener(brgbseekBarOnChangeLis);
		mAreaSeekbar.setOnSeekBarChangeListener(areaseekBarOnChangeLis);
		mWidthText =  (EditText)findViewById(R.id.widthEditText);
		mHeightText =  (EditText)findViewById(R.id.heightEditText);
		mWidthText.setText(Integer.toString((int)W));//初始化
		mHeightText.setText(Integer.toString((int)H));

		//Zoom in out事件
		zoomControls = (ZoomControls) findViewById(R.id.CAMERA_ZOOM_CONTROLS);
		zoomControls.setIsZoomInEnabled(true);
		zoomControls.setIsZoomOutEnabled(true);
		zoomControls.setOnZoomInClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (currentZoomLevel < maxZoomLevel) {
					currentZoomLevel++;
					Parameters para = mCamPreview.camera.getParameters();
					para.setZoom(currentZoomLevel);
					mCamPreview.camera.setParameters(para);
					mLogText.setText( "Zoom:" + Integer.toString(currentZoomLevel) ); 
					// mCamPreview.camera.startSmoothZoom(currentZoomLevel);
				}
			}
		});
		zoomControls.setOnZoomOutClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (currentZoomLevel > 0) {
					currentZoomLevel--;
					Parameters para = mCamPreview.camera.getParameters();
					para.setZoom(currentZoomLevel);
					mCamPreview.camera.setParameters(para);
					mLogText.setText( "Zoom:" + Integer.toString(currentZoomLevel) ); 
					// mCamPreview.camera.startSmoothZoom(currentZoomLevel);
				}
			}
		});

		//拍照事件
		buttonCapture = (ImageButton) findViewById(R.id.imageButtonCapture);
		buttonCapture.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mCamPreview.hasStartPreview == false) {
					Toast.makeText(MainActivity.this, "還不能拍照！",
							Toast.LENGTH_SHORT);
					return;
				}
				//寫入AlpahBeta
				getAB();
				//拍照
				mCamPreview.hasStartPreview = false;
				mCamPreview.camera.takePicture(camShutterCallback,
						camRawDataCallback, camJpegCallback);
			}
		});
		//Focus事件
		buttonFocus = (ImageButton) findViewById(R.id.imageButtonFocus);
		buttonFocus.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Toast.makeText(MainActivity.this, "對焦中!!！", Toast.LENGTH_SHORT).show();
				mCamPreview.camera.autoFocus(mCamPreview.onCamAutoFocus);
			}
		});
		//初始化Alpha Beta
		buttonInit = (ImageButton) findViewById(R.id.imageButtonInit);
		buttonInit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				getInitAB();
			}
		});
		//Set Width Height事件
		mSetWHButton = (Button) findViewById(R.id.setButton);
		mSetWHButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				W = Double.parseDouble(mWidthText.getText().toString());
				H = Double.parseDouble(mHeightText.getText().toString());
			}
		});
		//Set切換紅點綠點偵測事件
        mGreenCheckBox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener()
        {
				@Override
				public void onCheckedChanged(CompoundButton buttonView,	boolean isChecked) {
					// TODO Auto-generated method stub
					if (isChecked)
					{
						bGreenLaser = true;
						RTextView.setText("G > ");
						RRGBTextView.setText("G / (R + G + B) > ");
						GRGBTextView.setText("R / (R + G + B) < ");
					}
					else
					{
						bGreenLaser = false;
						RTextView.setText("R > ");
						RRGBTextView.setText("R / (R + G + B) > 	");
						GRGBTextView.setText("G / (R + G + B) < ");
					}
				}
        });
        //Set EV事件
		mEVAddImageButton =  (ImageButton) findViewById(R.id.imageButtonEVAdd);
		mEVAddImageButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Parameters para = mCamPreview.camera.getParameters();
				if (para.getExposureCompensation()+1 > para.getMaxExposureCompensation() )
				   return;
				para.setExposureCompensation(para.getExposureCompensation()+1);
				Log.i("AR","EV:" + para.getMaxExposureCompensation()); 
				mCamPreview.camera.setParameters(para);
				mLogText.setText( "EV:" + Integer.toString(para.getExposureCompensation()) + "EV" );   
			}
		});
		mEVSubstractImageButton =  (ImageButton) findViewById(R.id.imageButtonEVSubstract);
		mEVSubstractImageButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Parameters para = mCamPreview.camera.getParameters();
				if (para.getExposureCompensation() - 1 < para.getMinExposureCompensation())
					return;
				para.setExposureCompensation(para.getExposureCompensation() - 1);
				Log.i("AR", "EV:" + para.getMaxExposureCompensation());
				mCamPreview.camera.setParameters(para);
				mLogText.setText("EV:" + Integer.toString(para.getExposureCompensation()) + "EV");
			}
		});
	}

	/**
	 * Rotation轉角初始化
	 */
	private void initMath()
	{
		currentRotationMatrix[0] = 1.0f;
		currentRotationMatrix[4] = 1.0f;
		currentRotationMatrix[8] = 1.0f;
	}

	/**
	 *  Open Sensor 加速度 地磁 陀螺儀
	 */
	public void openSensor() {
		//是否為三星GC200
		String product  = Build.MODEL  ;// 設備名稱
		if (product.equals("EK-GC200"))
			bSamsungGC200 = true;
		else
			bSamsungGC200 = false
					;
		// Open ACCELEROMETER 加速度
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); // wyc1,20140622
		// 開啟
		List<Sensor> sensors_acc = sensorManager
				.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (sensors_acc.size() > 0)
			sensorManager.registerListener(this, sensors_acc.get(0),
					SensorManager.SENSOR_DELAY_NORMAL);
		else
			mLogText.setText("No ACCELEROMETER");// 呼叫SensorManager
													// //wyc1,20140622

		//Open MAGNETIC 地磁
		List<Sensor> sensors_mag = sensorManager
				.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
		if (sensors_mag.size() > 0)
			sensorManager.registerListener(this, sensors_mag.get(0),
					SensorManager.SENSOR_DELAY_NORMAL);
		else
			mLogText.setText("No MAGNETIC");// 呼叫SensorManager //wyc1,20140622

		//Open ORIENTATION 方向角(新版的API方向角就是加速度+地磁,不用開)
//		List<Sensor> sensors_ori = sensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
//		if (sensors_ori.size() > 0)
//			sensorManager.registerListener(this, sensors_ori.get(0),
//					SensorManager.SENSOR_DELAY_NORMAL);
//		else
//			mLogText.setText("No Orientation");// 呼叫SensorManager //wyc1,20140622

		//Open GYROSCOPE 陀螺儀
		List<Sensor> sensors_gyr =
		sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE);
		if (sensors_gyr.size() > 0)
			sensorManager.registerListener(this,sensors_gyr.get(0), SensorManager.SENSOR_DELAY_UI);
		else
		   mLogText.setText("No ORIENTATION");//呼叫SensorManager //wyc1,20140622

	}
	//上方抽屜拉開的R 分量拖曳bar的事件
	SeekBar.OnSeekBarChangeListener rseekBarOnChangeLis = new SeekBar.OnSeekBarChangeListener(){

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// TODO Auto-generated method stub
			mRText.setText(Integer.toString(progress));
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub		
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub		
		}
		
	};

	//上方抽屜拉開的R/r+g+b 拖曳bar的事件
	SeekBar.OnSeekBarChangeListener rrgbseekBarOnChangeLis = new SeekBar.OnSeekBarChangeListener(){

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// TODO Auto-generated method stub
			double pos = (double)progress / 100;
			DecimalFormat df = new DecimalFormat("#.##");
			mRRGBText.setText(df.format(pos));
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub		
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub		
		}
		
	};
	//上方抽屜拉開的G/r+g+b 拖曳bar的事件
	SeekBar.OnSeekBarChangeListener grgbseekBarOnChangeLis = new SeekBar.OnSeekBarChangeListener(){

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// TODO Auto-generated method stub
			double pos = (double)progress / 100;
			DecimalFormat df = new DecimalFormat("#.##");
			mGRGBText.setText(df.format(pos));
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub		
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub	
		}
		
	};
	//上方抽屜拉開的B/r+g+b 拖曳bar的事件
	SeekBar.OnSeekBarChangeListener brgbseekBarOnChangeLis = new SeekBar.OnSeekBarChangeListener(){

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// TODO Auto-generated method stub
			double pos = (double)progress / 100;
			DecimalFormat df = new DecimalFormat("#.##");
			mBRGBText.setText(df.format(pos));
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub		
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub	
		}
		
	};
	//上方抽屜拉開的Area 拖曳bar的事件
	SeekBar.OnSeekBarChangeListener areaseekBarOnChangeLis = new SeekBar.OnSeekBarChangeListener(){

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// TODO Auto-generated method stub
			//int pos = progress
			mAreaText.setText(Integer.toString(progress));
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
		}
		
	};

	/**
	 *  繼續Activity的行為
	 */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		mCamPreview.camera = openFrontFacingCamera();
		maxZoomLevel = mCamPreview.camera.getParameters().getMaxZoom();
		currentZoomLevel = mCamPreview.camera.getParameters().getZoom();

		super.onResume();
	}

	/**
	 * 只要Sensor有Output就會進入事件
	 * @param arg0
     */
	@Override
	public void onSensorChanged(SensorEvent arg0) {
		// TODO Auto-generated method stub
		//記錄各種的角度 產生alpha角 Beta角
		//magneticFieldValues :地磁
		//accelerometerValues :加速度

		float[] R = new float[9];
		float[] tmp_values = new float[3];
		final float[]  values = new float[3];
		switch ( arg0.sensor.getType() ) {
			case Sensor.TYPE_MAGNETIC_FIELD:
				magneticFieldValues = arg0.values.clone();
				break;
			case Sensor.TYPE_ACCELEROMETER:
                //三星手機GC200 bug修正
				if (bSamsungGC200){
					float tmp = arg0.values[0];
					arg0.values[0] = arg0.values[1];
					arg0.values[1] = tmp;
				}
				accelerometerValues = arg0.values.clone();

				SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
				SensorManager.getOrientation(R, tmp_values);

				values[0] = (float) Math.toDegrees(tmp_values[0]);
				values[1] = (float) Math.toDegrees(tmp_values[1]);
				values[2] = (float) Math.toDegrees(tmp_values[2]);
				if(!bGyroSwitch) {
					//原始方法
					aAngle = -values[0];
				}
				bAngle = -values[2];
				break;
			case Sensor.TYPE_ORIENTATION:
				//	orientationValues = arg0.values;
				break;
			case Sensor.TYPE_GYROSCOPE:
				final float[] deltaRotationVector = new float[4];
				float[] gyroscopeOrientation = new float[3];
				if (arg0.sensor.getType() == Sensor.TYPE_GYROSCOPE && bGyroSwitch) {
					if (bSamsungGC200){
						float tmp = arg0.values[1];
						arg0.values[1] = arg0.values[2];
						arg0.values[2] = tmp;
					}
					if (timestamp != 0) {
						//final float EPSILON = 0.000000001f;
						final float EPSILON = 0.02f;
						final float dT = (arg0.timestamp - timestamp) * NS2S;
						// Axis of the rotation sample, not normalized yet.
						float axisX = arg0.values[0]-0.02f;//+0.02f;
						float axisY = arg0.values[1];//+0.035f;
						float axisZ = arg0.values[2]+0.05f;//-0.03f;
						// Calculate the angular speed of the sample
						float omegaMagnitude = (float)Math.sqrt( axisX * axisX + axisY * axisY + axisZ * axisZ);
						// Normalize the rotation vector if it's big enough to get the axis
						// (that is, EPSILON should represent your maximum allowable margin of error)
						if (omegaMagnitude > EPSILON) {
							axisX /= omegaMagnitude;
							axisY /= omegaMagnitude;
							axisZ /= omegaMagnitude;
						}
						float thetaOverTwo = omegaMagnitude * dT / 2.0f;
						float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
						float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
						deltaRotationVector[0] = sinThetaOverTwo * axisX;
						deltaRotationVector[1] = sinThetaOverTwo * axisY;
						deltaRotationVector[2] = sinThetaOverTwo * axisZ;
						deltaRotationVector[3] = cosThetaOverTwo;
					}

					float[] deltaRotationMatrix = new float[9];
					SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
					currentRotationMatrix = matrixMultiplication(currentRotationMatrix,deltaRotationMatrix);
					SensorManager.getOrientation(currentRotationMatrix,gyroscopeOrientation);

					if(bGyroSwitch) {
						if (bSamsungGC200) {
							aAngle = -(float) Math.toDegrees(gyroscopeOrientation[0]);
							//bAngle = -(float) Math.toDegrees(gyroscopeOrientation[1]);
						}else {
							aAngle = -(float) Math.toDegrees(gyroscopeOrientation[1]);
							//bAngle = -(float) Math.toDegrees(gyroscopeOrientation[2]);
						}
					}
					timestamp = arg0.timestamp;
				}
				break;
			default: return;
		}
		// 張博的方法
		/*
		 * //aAngle =
		 * -(Math.toDegrees(Math.acos(arg0.values[0]/Math.pow(Math.pow
		 * (arg0.values
		 * [0],2)+Math.pow(arg0.values[1],2)+Math.pow(arg0.values[2],2), 0.5)))
		 * - 90); //bAngle =
		 * -(Math.toDegrees(Math.acos(arg0.values[1]/Math.pow(Math
		 * .pow(arg0.values
		 * [0],2)+Math.pow(arg0.values[1],2)+Math.pow(arg0.values[2],2), 0.5)))
		 * - 90); bAngle =
		 * (Math.toDegrees(Math.asin(arg0.values[1]/Math.pow(Math
		 * .pow(arg0.values
		 * [0],2)+Math.pow(arg0.values[1],2)+Math.pow(arg0.values[2],2),
		 * 0.5)))); aAngle =
		 * -(Math.toDegrees(Math.asin(arg0.values[0]/Math.cos(bAngle
		 * /180*3.1415926
		 * )/Math.pow(Math.pow(arg0.values[0],2)+Math.pow(arg0.values
		 * [1],2)+Math.pow(arg0.values[2],2), 0.5))));
		 * 
		 * //水平放手機與雷射投射器時(寬矩形)，角度需修正為 //double tmp=0.0; //tmp=bAngle;
		 * //bAngle=aAngle; //aAngle=-tmp; //****修正流速角度至裂縫的角度(直放)***** //bAngle
		 * = 90 - bAngle; //aAngle = -aAngle; //****修正流速角度至裂縫的角度(橫放)***** double
		 * tmp=0.0; tmp=bAngle; bAngle=aAngle; aAngle=-tmp; bAngle = 90 +
		 * bAngle;
		 * 
		 * aDisplayAngle = (Math.round(aAngle * 10000)) / 10000.0; bDisplayAngle
		 * = (Math.round(bAngle * 10000)) / 10000.0;
		 */

		// 網路找到的方法
		/*
		 * float x = arg0.values[0]; float y = arg0.values[1]; float z =
		 * arg0.values[2];
		 * 
		 * float dz = (float) (Math.atan(Math.sqrt(x*x+y*y)/z) *180 /3.1415926);
		 * float dy = (float) (Math.atan(x/Math.sqrt(y*y+z*z)) *180 /3.1415926);
		 * float dx = (float) (Math.atan(y/Math.sqrt(x*x+z*z)) *180 /3.1415926);
		 * 
		 * bAngle = dy; aAngle = dz;
		 */


		//values[0] = orientationValues[0];
		//values[1] = orientationValues[1];
		//values[2] = orientationValues[2];

		// values[0] = gyroscopeAngles[0];
		// values[1] = gyroscopeAngles[1];
		// values[2] = gyroscopeAngles[2];

/*
		//三星手機GC200 bug修正
		if (accelerometerValues[2] < 0) {
			//aAngle = (float)(Math.toDegrees(tmp_values[1])+360)%360;
			//aAngle = (float)(orientationValues[0]+360)%360;
			//aAngle = -magneticFieldValues[0];
			aAngle = -orientationValues[2];
			bAngle = 180-(-values[1]);
		}else
		{
			//aAngle = (float)(orientationValues[0]+360)%360;
			//aAngle = -magneticFieldValues[0];
			aAngle = -orientationValues[2];
			bAngle = -values[1];
		}
*/

		/*
		 * if (accelerometerValues[1] < 0) //手機面朝下 bAngle = 180 -
		 * Math.abs(values[2]); //手機面朝下無法偵測,須靠加速度器的z軸判斷 else bAngle =
		 * Math.abs(values[2]);
		 */
	}
	//矩陣相乘
	public float[] matrixMultiplication(float[] a, float[] b)
	{
		float[] result = new float[9];

		result[0] = a[0] * b[0] + a[1] * b[3] + a[2] * b[6];
		result[1] = a[0] * b[1] + a[1] * b[4] + a[2] * b[7];
		result[2] = a[0] * b[2] + a[1] * b[5] + a[2] * b[8];

		result[3] = a[3] * b[0] + a[4] * b[3] + a[5] * b[6];
		result[4] = a[3] * b[1] + a[4] * b[4] + a[5] * b[7];
		result[5] = a[3] * b[2] + a[4] * b[5] + a[5] * b[8];

		result[6] = a[6] * b[0] + a[7] * b[3] + a[8] * b[6];
		result[7] = a[6] * b[1] + a[7] * b[4] + a[8] * b[7];
		result[8] = a[6] * b[2] + a[7] * b[5] + a[8] * b[8];

		return result;
	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	/**
	 * Activity的暫停事件
	 */
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		if (mCamPreview.hasStartPreview) {
			mCamPreview.camera.stopPreview();
		}
		mCamPreview.camera.release();
		mCamPreview.camera = null;
		mCamPreview.hasStartPreview = false;

		super.onPause();
	}
	/**
	 * Activity的重起事件(退出主畫面,重新進入的事件)
	 */
	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub

		//***********尚未完成*********
		super.onRestart();
	}

	/**
	 *  Main Activity 's Menu setting
	 * @param menu  Activity 's Menu
	 * @return
     */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		menu.add(0, MENU_OPENCLOSE_SENSOR, 0, "打開/關閉感應器");
		menu.add(0, MENU_GYRO_ORI_SWITCH, 0, "陀螺儀/方向感應器");
		menu.add(0, MENU_INIT_AB, 0, "初始AlphaBeta");
		menu.add(0, MENU_GET_AB, 0, "得到AlphaBeta");
		menu.add(0, MENU_SAVE_REMOTEANGLE, 0, "儲存遠端角度");
		menu.add(0, MENU_TAKE_PICTURE, 0, "照相");
		menu.add(0, MENU_SHOW_PICTURE, 0, "觀看");
		menu.add(0, MENU_LASER_MEASURE, 0, "雷射測距");

		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Above the Activity button 's event
	 * @param item
         * @return
     */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case MENU_OPENCLOSE_SENSOR:
			//打開sensor
			openSensor();
			mLogText.setText("感應陀螺儀！");

			//顯示sensor
			// Rotation資訊
			if (bShowSensor == false) {
				mHandler = new Handler();
				updateTimer = new Timer();
				mClock = new TimerTask() {
					public void run() {
						mHandler.post(new Runnable() {
							@Override
							public void run() {
								// 顯示MagneticField & Accelerometer
								DecimalFormat df4 = new DecimalFormat("#.##");
								String str4 = "MagneticField: "
										+ df4.format(magneticFieldValues[0])
										+ " , "
										+ df4.format(magneticFieldValues[1])
										+ " , "
										+ df4.format(magneticFieldValues[2]);
								String str5 = "Accelerometer:"
										+ df4.format(accelerometerValues[0])
										+ " , "
										+ df4.format(accelerometerValues[1])
										+ " , "
										+ df4.format(accelerometerValues[2]);
								mRotText.setText(str4);
								mAccText.setText(str5);
								// 顯示angle 並且換算
								double aAngle2 = 0.0;
								double bAngle2 = 0.0;
								aAngle2 = aInitAngle - aAngle;
								if (aAngle2 < -180)
									aAngle2 = 360 + (aInitAngle - aAngle);
								if (aAngle2 > 180)
									aAngle2 = -(360 - (aInitAngle - aAngle));

								bAngle2 = bAngle - bInitAngle;
								String str6 = "Angle: " + df4.format(aAngle2)
										+ " , " + df4.format(bAngle2);
								mAngText.setText(str6);

							}
						});
					}
				};
				updateTimer.schedule(mClock, 0, 100);
				bShowSensor = true;
			} else {
				updateTimer.cancel();
				mRotText.setText("");
				mAccText.setText("");
				bShowSensor = false;
			}
			break;
		case MENU_GYRO_ORI_SWITCH:
			// 陀螺儀與方向角轉換
			if (bGyroSwitch){
				bGyroSwitch = false;
				mLogText.setText("感應方位感應器！");
			}else{
				bGyroSwitch = true;
				mLogText.setText("感應陀螺儀！");
			}
			break;
		case MENU_INIT_AB:
			//初始Alpha Beta
			getInitAB();
			break;
		case MENU_GET_AB:
			//Get Alpha Beta
			getAB();
			break;

		case MENU_TAKE_PICTURE:
			if (mCamPreview.hasStartPreview == false) {
				Toast.makeText(MainActivity.this, "還不能拍照！", Toast.LENGTH_SHORT);
				break;
			}
			mCamPreview.hasStartPreview = false;
			mCamPreview.camera.takePicture(camShutterCallback,
					camRawDataCallback, camJpegCallback);
			break;
		case MENU_SHOW_PICTURE:
			//Intent 相片模式
			// if (mDIRPath==null||mFileName==null)
			// break;
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			intent.setType("image/*");
			startActivity(intent);
			/*
			 * Intent it = new Intent(Intent.ACTION_VIEW); File file = new
			 * File(mDIRPath.getPath() + "/" + mFileName); if(file.exists()) {
			 * it.setDataAndType(Uri.fromFile(file), "image/*");
			 * startActivity(it);} else { Toast.makeText(MainActivity.this,
			 * "檔案不存在", Toast.LENGTH_SHORT); }
			 */

			break;
		case MENU_LASER_MEASURE:
			//雷射測距
			if (bLaserMeasure)
				bLaserMeasure = false;
			else
				bLaserMeasure = true;
		    break;
		case MENU_SAVE_REMOTEANGLE:
			//儲存角度到SD卡
			mHandler = new Handler();
			updateTimer = new Timer();
			mClock = new TimerTask() {
				public void run() {
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							saveABDat2SD(aAngle,bAngle,"RemoteAngle");
						}
					});
				}
			};
			updateTimer.schedule(mClock, 0, 1000);

			break;
		}

		return super.onOptionsItemSelected(item);
	}

	//快門事件
	ShutterCallback camShutterCallback = new ShutterCallback() {
		public void onShutter() {
			// 通知使用者已完成拍照,例如發出一個聲音
		}
	};

	//拍照事件(可以砍掉)
	PictureCallback camRawDataCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			// 用來接收原始的影像資料
		}
	};

	/**
	 * 拍找事件,拍完照寫入SD卡與EXIF檔案
	 */
	PictureCallback camJpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			// 用來接收壓縮成jpeg格式的影像資料
			FileOutputStream outStream = null;
			try {
				//File Name
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MMdd_HHmmss");
				Date dt = new Date();
				mFileName = sdf.format(dt) + ".jpg";
				// String path =
				// Environment.getExternalStorageDirectory().toString
				// ()+"/test/";
				//File Path
				mDIRPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CrackImage");
				mDIRPath.mkdirs();

				// File mDIRPath =
				// Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
				// mFileName =
				// String.format("still%d.jpg",System.currentTimeMillis());
				// 方法1
				outStream = new FileOutputStream(mDIRPath.getPath() + "/"
						+ mFileName);
				outStream.write(data);
				outStream.close();
				
				/*
				 * //方法2 outStream = new FileOutputStream(mDIRPath.getPath() +
				 * "/" + mFileName); Bitmap bmp =
				 * BitmapFactory.decodeByteArray(data, 0, data.length);
				 * bmp.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
				 * outStream.flush(); outStream.close();
				 */

				// saveEXIF
				try {
					saveExif(mDIRPath.getPath() + "/" + mFileName);
				} catch (ImageReadException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ImageWriteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// scan "Picture"file 以免開啟圖片庫看不到
				sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
						Uri.parse("file://" + mDIRPath.getPath() + "/"	+ mFileName)));

			} catch (IOException e) {
				Toast.makeText(MainActivity.this, "影像檔儲存錯誤！",
						Toast.LENGTH_SHORT).show();
			}
			//After Capture,restart the Preview Mode
			mCamPreview.camera.startPreview();
			mCamPreview.hasStartPreview = true;
		}
	};

	/**
	 * Preview 顯示攝影機影像 , 有寫入雷射測距模式
	 */
	PreviewCallback camPreviewCallback = new Camera.PreviewCallback() {
		public void onPreviewFrame(final byte[] data, Camera arg1) {
			if (bLaserMeasure) {
				new Thread() { 
					@Override  
					public void run() { 
						bLaserMeasure = false;

						//紅點偵測
						Camera.Parameters parameters = mCamPreview.camera.getParameters();
						int width = parameters.getPreviewSize().width;
						int height = parameters.getPreviewSize().height;
						mImageView.setCamImgSize(width, height);
						YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);
						Mat mYuv = new Mat(height+height/2 , width, CvType.CV_8UC1);
						Mat mImg = new Mat(height, width, CvType.CV_8UC3);

						mYuv.put(0, 0, data);    
						Imgproc.cvtColor(mYuv, mImg, Imgproc.COLOR_YUV420sp2RGBA, 4);

						final ArrayList<PointF> arrayList = getLaserDetectionPoint(mImg);
						if (arrayList.size()== 4)
						{
							double dist = getLaserDistance(arrayList);
							DecimalFormat df = new DecimalFormat("#.####");
							final String str = "Distance: " + df.format(dist);
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									mImageView.mArrayList = arrayList;
									mLog2Text.setText(str);
									
									mImageView.invalidate();
								}
							});

						}
						else
						{
							final String str = "偵測出 " + arrayList.size() + " 個點";
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									mImageView.mArrayList = arrayList;
									mLog2Text.setText(str);
									mImageView.invalidate();
								}
							});
						}

						//release
						mYuv.release();
						mImg.release();

						bLaserMeasure = true;
					}
				}.start();
			}
		}
	};

	/**
	 *  物理的按鈕事件
	 * @param keyCode
	 * @param event
     * @return
     */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		// 點選camera按鈕
		if (keyCode == KeyEvent.KEYCODE_CAMERA) {
			if (mCamPreview.hasStartPreview == false)
				return true;
			
			//寫入AB
			getAB();
			//拍照
			mCamPreview.hasStartPreview = false;
			mCamPreview.camera.takePicture(camShutterCallback,
					camRawDataCallback, camJpegCallback);
			return true;
		}
		/*
		 * //點選音量鍵按鈕 if(keyCode == KeyEvent.KEYCODE_VOLUME_UP){
		 * if(currentZoomLevel < maxZoomLevel){ currentZoomLevel++; Parameters
		 * para = mCamPreview.camera.getParameters();
		 * para.setZoom(currentZoomLevel);
		 * mCamPreview.camera.setParameters(para);
		 * //mCamPreview.camera.startSmoothZoom(currentZoomLevel); } return
		 * true; } if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
		 * if(currentZoomLevel > 0){ currentZoomLevel--; Parameters para =
		 * mCamPreview.camera.getParameters(); para.setZoom(currentZoomLevel);
		 * mCamPreview.camera.setParameters(para);
		 * //mCamPreview.camera.startSmoothZoom(currentZoomLevel); } return
		 * true; }
		 */
		// 點選攝影機zoom按鈕 IN
		if (keyCode == KeyEvent.KEYCODE_ZOOM_IN) {
			if (currentZoomLevel < maxZoomLevel) {
				currentZoomLevel++;
				Parameters para = mCamPreview.camera.getParameters();
				para.setZoom(currentZoomLevel);
				mCamPreview.camera.setParameters(para);
				mLogText.setText( "Zoom:" + Integer.toString(currentZoomLevel)); 
				// mCamPreview.camera.startSmoothZoom(currentZoomLevel);
			}
			return true;
		}
		// 點選攝影機zoom按鈕 OUT
		if (keyCode == KeyEvent.KEYCODE_ZOOM_OUT) {
			if (currentZoomLevel > 0) {
				currentZoomLevel--;
				Parameters para = mCamPreview.camera.getParameters();
				para.setZoom(currentZoomLevel);
				mCamPreview.camera.setParameters(para);
				// mCamPreview.camera.startSmoothZoom(currentZoomLevel);
				mLogText.setText( "Zoom:" + Integer.toString(currentZoomLevel)); 
			}
			return true;
		}
		//按上一頁的事件
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this); //創建訊息方塊	      
			builder.setMessage("要關閉CrackCamera？");	 
			builder.setTitle("離開");	 
			builder.setPositiveButton("確認", new DialogInterface.OnClickListener()  {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss(); //dismiss為關閉dialog,Activity還會保留dialog的狀態
					MainActivity.this.finish();
					System.exit(0);
					//System.exit(0);
					//this.finish();//關閉activity
				}
			});

			builder.setNegativeButton("取消", new DialogInterface.OnClickListener()  {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			builder.create().show();
		}
		
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * 得到角度
	 */
	private void getAngle() {
		// mRgba = inputFrame.rgba();

		// Log.i("!!!!!!!!!!!!!!!!", Double.toString(aDisplayAngle));

		// 取20筆資料的平均
		if (count1 > 10) { // 累加
			tmp_aAngle = tmp_aAngle + aAngle;
			tmp_bAngle = tmp_bAngle + bAngle;
		}

		if (count1 == 30) {
			tmp_aAngle = tmp_aAngle / 20.; // 取30-10筆之平均
			tmp_bAngle = tmp_bAngle / 20.;
			aAngle = tmp_aAngle;
			bAngle = tmp_bAngle;
			posA = "A(0,0)" + "\r\n"; // 換行;
			posB = "B("
					+ Double.toString(Math.round(W
							/ Math.cos(Math.toRadians(aAngle)) * 10000) / 10000.0)
					+ ","
					+ Double.toString(Math.round(W
							* Math.tan(Math.toRadians(bAngle))
							* Math.tan(Math.toRadians(aAngle)) * 10000) / 10000.0)
					+ ")" + "\r\n"; // 換行;
			posC = "C("
					+ Double.toString(Math.round(W
							/ Math.cos(Math.toRadians(aAngle)) * 10000) / 10000.0)
					+ ","
					+ Double.toString(Math.round((H
							/ Math.cos(Math.toRadians(bAngle)) + W
							* Math.tan(Math.toRadians(bAngle))
							* Math.tan(Math.toRadians(aAngle))) * 10000) / 10000.0)
					+ ")" + "\r\n"; // 換行;
			posD = "D(0,"
					+ Double.toString(Math.round(H
							/ Math.cos(Math.toRadians(bAngle)) * 10000) / 10000.0)
					+ ")" + "\r\n"; // 換行;

			key1 = false;
		} // if ( count1 == 30)

		count1 = count1 + 1;
	}

	/**
	 *  寫入Alpha Beta到SD卡
	 * @param alpha
	 * @param beta
	 * @param  fname
     */
	public void saveABDat2SD(double alpha, double beta, String fname){
		try {
			String path = Environment.getExternalStorageDirectory().toString();
			File file = new File(path + "/Crackdata");
			if( !file.exists() ) file.mkdirs();

			File fileout = new File(path + "/Crackdata", fname + ".dat");

			// 開啟檔案串流
			FileOutputStream out = new FileOutputStream(fileout);

			tmpString=Double.toString(alpha)+"\r\n";
			out.write(tmpString.getBytes());
			tmpString=Double.toString(beta)+"\r\n";
			out.write(tmpString.getBytes());

			// 刷新並關閉檔案串流
			out.flush();
			out.close();
		}

		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace ();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace ();
		}
	}

	//儲存EXIF
	private void saveExif(String filename) throws IOException,
			ImageReadException, ImageWriteException {
		/*
		 * try { ExifInterface exif = new ExifInterface(filename);
		 * exif.setAttribute("UserComment", "儲存exit:alpha = 60, beta = 60");
		 * exif.saveAttributes(); String str =
		 * exif.getAttribute(ExifInterface.TAG_MAKE);
		 * 
		 * } catch (IOException e1) { // TODO Auto-generated catch block
		 * Toast.makeText(this, "exif寫入失敗", 1000).show(); e1.printStackTrace();
		 * }
		 */

		// -------------Loadind Apache
		// sanselan-0.97-incubator.jar----------------
		OutputStream os = null;
		try {
			TiffOutputSet outputSet = null;

			// note that metadata might be null if no metadata is found.
			IImageMetadata metadata = Sanselan.getMetadata(new File(filename));
			JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
			if (null != jpegMetadata) {
				// note that exif might be null if no Exif metadata is found.
				TiffImageMetadata exif = jpegMetadata.getExif();

				if (null != exif) {
					// TiffImageMetadata class is immutable (read-only).
					// TiffOutputSet class represents the Exif data to write.
					//
					// Usually, we want to update existing Exif metadata by
					// changing
					// the values of a few fields, or adding a field.
					// In these cases, it is easiest to use getOutputSet() to
					// start with a "copy" of the fields read from the image.
					outputSet = exif.getOutputSet();
				}
			}

			// if file does not contain any exif metadata, we create an empty
			// set of exif metadata. Otherwise, we keep all of the other
			// existing tags.
			if (null == outputSet)
				outputSet = new TiffOutputSet();

			{
				// Example of how to add a field/tag to the output set.
				//
				// Note that you should first remove the field/tag if it already
				// exists in this directory, or you may end up with duplicate
				// tags. See above.
				//
				// Certain fields/tags are expected in certain Exif directories;
				// Others can occur in more than one directory (and often have a
				// different meaning in different directories).
				//
				// TagInfo constants often contain a description of what
				// directories are associated with a given tag.
				//
				// see
				// org.apache.sanselan.formats.tiff.constants.AllTagConstants
				//
				DecimalFormat df3 = new DecimalFormat("#.##");
				String comment = df3.format(aDiffAngle) + ";"
						+ df3.format(bDiffAngle);
				byte[] bytesUserComment = ExifTagConstants.EXIF_TAG_USER_COMMENT
						.encodeValue(TiffFieldTypeConstants.FIELD_TYPE_ASCII,
								comment, outputSet.byteOrder);

				TiffOutputField aperture = new TiffOutputField(
						ExifTagConstants.EXIF_TAG_USER_COMMENT,
						ExifTagConstants.EXIF_TAG_USER_COMMENT.dataTypes[0],
						bytesUserComment.length, bytesUserComment);

				TiffOutputDirectory exifDirectory = outputSet
						.getOrCreateExifDirectory();
				// make sure to remove old value if present (this method will
				// not fail if the tag does not exist).
				exifDirectory.removeField(TiffConstants.EXIF_TAG_USER_COMMENT);
				exifDirectory.add(aperture);
			}

			// printTagValue(jpegMetadata, TiffConstants.TIFF_TAG_DATE_TIME);
			File tempFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/tmp_exif.jpg");
			os = new FileOutputStream(tempFile);
			os = new BufferedOutputStream(os);

			try {
				new ExifRewriter().updateExifMetadataLossless(new File(filename), os, outputSet);// 無法寫在同一個檔案上面!
			} finally {
				os.close();
				// 將暫存檔名Rename成原檔名!
				tempFile.renameTo(new File(filename));
			}
			Toast.makeText(this, "照片寫入Picture/CrackImage", 2000).show();
			Toast.makeText(this, "Exif寫入USER COMMENT中", 2000).show();
			os.close();
			os = null;
		} finally {
			if (os != null)
				try {
					os.close();
				} catch (IOException e) {

				}
		}
	}

	//不重要 可以刪除
	public static String getFilePathFromContentUri(Uri selectedVideoUri,
			ContentResolver contentResolver) {
		String filePath;
		String[] filePathColumn = { MediaColumns.DATA };

		Cursor cursor = contentResolver.query(selectedVideoUri, filePathColumn,
				null, null, null);
		// 也可用下面的方法拿到cursor
		// Cursor cursor = this.context.managedQuery(selectedVideoUri,
		// filePathColumn, null, null, null);

		cursor.moveToFirst();

		int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
		filePath = cursor.getString(columnIndex);
		cursor.close();
		return filePath;
	}

	//打開FrontFacing鏡頭
	public Camera openFrontFacingCamera() {
		int cameraCount = 0;
		Camera cam = null;
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		cameraCount = Camera.getNumberOfCameras();
		for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
			Camera.getCameraInfo(camIdx, cameraInfo);
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
				try {
					cam = Camera.open(camIdx);
				} catch (RuntimeException e) {
					Log.e("PreView",
							"Camera failed to open: " + e.getLocalizedMessage());
				}
			}
		}

		return cam;
	}

	/**
	 * 偵測雷射點
	 * @param img Source Image
	 * @return  4points Position
     */
	private ArrayList getLaserDetectionPoint(Mat img) {
		//讀照片
		ArrayList mLaserPointList = new ArrayList ();
		//宣告影像變數
		int w,h;
		w = img.width();
		h = img.height();
		Mat imgR8u = null;
		Mat imgG8u = null;
		Mat imgB8u = null;	
		Mat temp8u =  new Mat(h, w, CvType.CV_8UC1);
		
		Mat imgR =  new Mat(h, w, CvType.CV_32FC1);
		Mat imgG =  new Mat(h, w, CvType.CV_32FC1);
		Mat imgB =  new Mat(h, w, CvType.CV_32FC1);
		Mat temp =  new Mat(h, w, CvType.CV_32FC1);
		Mat temp1 =  new Mat(h, w, CvType.CV_32FC1);	
		Mat temp2 =  new Mat(h, w, CvType.CV_32FC1);
		Mat temp3 =  new Mat(h, w, CvType.CV_32FC1);
		Mat temp4 =  new Mat(h, w, CvType.CV_32FC1);

		//通道分離
		List<Mat> listImg = new ArrayList<Mat>(3);
		Core.split(img, listImg); 
		if (bGreenLaser)//R->0 G->1 G ->2,R跟G對調為綠點偵測
		{
			imgR8u = listImg.get(1);  
			imgG8u = listImg.get(0); 
			imgB8u = listImg.get(2);
		}else{
			imgR8u = listImg.get(0);  
			imgG8u = listImg.get(1); 
			imgB8u = listImg.get(2);
		}
    	//8U->32F
    	//Core.convertScaleAbs(imgR8u, imgR);
    	//Core.convertScaleAbs(imgG8u, imgG);
    	//Core.convertScaleAbs(imgB8u, imgB);
		imgR8u.convertTo(imgR, CvType.CV_32FC1);
		imgG8u.convertTo(imgG, CvType.CV_32FC1);
		imgB8u.convertTo(imgB, CvType.CV_32FC1);
		
    	//取介面上的參數
        float fR, fRRGB, fGRGB, fBRGB, fMinArea;
        fR = Float.parseFloat(mRText.getText().toString()); 
        fRRGB = Float.parseFloat(mRRGBText.getText().toString());
        fGRGB = Float.parseFloat(mGRGBText.getText().toString());
        fBRGB = Float.parseFloat(mBRGBText.getText().toString());
        fMinArea = Float.parseFloat(mAreaText.getText().toString());
        /*
        fR = Float.parseFloat("200"); 
        fRRGB = Float.parseFloat("0.1");
        fGRGB = Float.parseFloat("0.31");
        fBRGB = Float.parseFloat("0.6");
        fMinArea = Float.parseFloat("50");*/
                
        //取門檻1
        //Imgproc.threshold(imgR,temp1,fR,255,Imgproc.THRESH_BINARY_INV); //方法1
        int size = (int) (imgR.total() * imgR.channels());                //方法2
        float[] buff = new float[size];
        imgR.get(0, 0, buff);
        for (int i = 0; i < size; i++)
        	if (buff[i] > fR)
        		buff[i] = (float) 255;
        	else
        		buff[i] = (float) 0;
        	//buff[i] = (byte) (255 - buff[i]);
        temp1.put(0, 0, buff);
        //取門檻2
        Core.add(imgR,imgG,temp);
        Core.add(temp,imgB,temp);
        Core.add(temp, new Scalar(1,1,1), temp);
        Core.divide(imgR,temp,temp2);
        Imgproc.threshold(temp2, temp2,fRRGB,255,Imgproc.THRESH_BINARY);
        //取門檻3
        Core.divide(imgG,temp,temp3);
        Imgproc.threshold(temp3,temp3,fGRGB,255,Imgproc.THRESH_BINARY_INV);
        //取門檻4
        Core.divide(imgB,temp,temp4);
        Imgproc.threshold(temp4,temp4,fBRGB,255,Imgproc.THRESH_BINARY_INV);
        //四個門檻做and交集
        Core.bitwise_and(temp1,temp2,temp2);
        Core.bitwise_and(temp2,temp3,temp3);
        Core.bitwise_and(temp3,temp4,temp);
        //轉換影像至8u
        temp.convertTo(temp8u, CvType.CV_8UC1);
        
        /*
        //取灰階門檻
        Mat gray = new Mat();
		Imgproc.cvtColor(img,gray,Imgproc.COLOR_BGRA2GRAY, 1);
        Imgproc.threshold(gray,temp8u,230,255,Imgproc.THRESH_BINARY); //方法1,針對綠點
        */
        
        /*
        //   debug用
        Bitmap bitmap = Bitmap.createBitmap(temp8u.cols(), temp8u.rows(), Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(temp8u, bitmap);
        iv.setImageBitmap(bitmap);
        */ 
        
        /*
        //Image inverse 超慢
        for (int i=0;i<temp.rows();i++) { 
        	for (int j=0;j<temp.cols();j++) { 
        		double[] data = temp.get(i, j); 
        		data[0] = (255 - data[0]);
        		temp.put(i, j, data); 
        	} 
        }*/
        /*
        //Image inverse 比較快的方法
        int size = (int) (temp.total() * temp.channels());
        byte[] buff = new byte[size];
        temp.get(0, 0, buff);
        for (int i = 0; i < size; i++)
        	buff[i] = (byte) (255- buff[i]);
        temp.put(0, 0, buff); */    	
            
        //找出所有輪廓中心
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();    
        Imgproc.findContours(temp8u, contours, new Mat(), Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_NONE);
        for (int i=0; i< contours.size();i++)
        	if (Imgproc.contourArea(contours.get(i)) > fMinArea )
        	{
        		Rect rect = Imgproc.boundingRect(contours.get(i));
        		PointF point = new PointF(rect.x + rect.width/2, rect.y + rect.height/2);
        		mLaserPointList.add(point);    
        	}
        //release記憶體 
        contours.clear();
        imgR8u.release();
        imgG8u.release();
        imgB8u.release();
        temp8u.release();
        imgR.release();
        imgG.release();
        imgB.release();
        temp.release();
        temp1.release();
        temp2.release();
        temp3.release();
        temp4.release();
               
        //排列ABCD四點位置
        //getSortLaserPostion(mLaserPointList);
        getSortLaserPostionForSort(mLaserPointList);
 
        return mLaserPointList;
	}

	/**
	 * 雷射測距
	 * @param arrayList 4Points Position
	 * @return Distant
     */
	private double getLaserDistance(ArrayList<PointF> arrayList){
		//分配點
		if (arrayList.size()!=4)
			return 0.0;
		
		double pointA_u= arrayList.get(0).x;
		double pointA_v= arrayList.get(0).y;
		double pointB_u= arrayList.get(1).x;
		double pointB_v= arrayList.get(1).y;
		double pointC_u= arrayList.get(2).x;
		double pointC_v= arrayList.get(2).y;
		double pointD_u= arrayList.get(3).x;
		double pointD_v= arrayList.get(3).y;	
		
		//得到焦距
		Camera.Parameters parameters = mCamPreview.camera.getParameters();
		//double width = 6.16;
		//double height = 4.62;
		double width = parameters.getPreviewSize().width;
		double height = parameters.getPreviewSize().height;
		int nZoomIndex = parameters.getZoom();
		List<Integer> nRatios =  parameters.getZoomRatios();
		float nZoom = nRatios.get(nZoomIndex) / 100;
		double ff = parameters.getFocalLength()*nZoom;	
        Log.d("Focal Length", Double.toString(ff));
		
		//計算距牆面的平均距離
		double ss_A=50; //雷射出口距鏡頭位置
		double ss_B=130;
		double ss_C=130;
		double ss_D=50;
		//double ff=0.028;  //鏡頭等效焦距，f=28mm
		double Xd_A=0.0;  //雷射距離牆面距離
		double Xd_B=0.0;
		double Xd_C=0.0;
		double Xd_D=0.0;
		double W_A=0.0; //雷射光點影像座標距中心點的距離
		double W_B=0.0;
		double W_C=0.0;
		double W_D=0.0;

		double u00=(double)(width/2);
		double v00=(double)(height/2);
		W_A=Math.sqrt((double)((pointA_u-u00)*(pointA_u-u00)+(pointA_v-v00)*(pointA_v-v00)));
		W_B=Math.sqrt((double)((pointB_u-u00)*(pointB_u-u00)+(pointB_v-v00)*(pointB_v-v00)));
		W_C=Math.sqrt((double)((pointC_u-u00)*(pointC_u-u00)+(pointC_v-v00)*(pointC_v-v00)));
		W_D=Math.sqrt((double)((pointD_u-u00)*(pointD_u-u00)+(pointD_v-v00)*(pointD_v-v00)));

		//pixel換為公尺，等效底片35mm(等效焦距28mm)
		/*
		W_A=W_A/(2*u00)*35+0.0001; //避免除到0, 所以加0.0001
		W_B=W_B/(2*u00)*35+0.0001;
		W_C=W_C/(2*u00)*35+0.0001;
		W_D=W_D/(2*u00)*35+0.0001;*/
		W_A=W_A*6.16/width; 
		W_B=W_B*6.16/width;
		W_C=W_C*6.16/width;
		W_D=W_D*6.16/width;

		Xd_A=ss_A*ff/W_A;
		Xd_B=ss_B*ff/W_B;
		Xd_C=ss_C*ff/W_C;
		Xd_D=ss_D*ff/W_D;

		//double distance1=(Xd_A+Xd_B+Xd_C+Xd_D)/4.0;//平均距離
		double distance1=Xd_A/1000;//A點距離 (公尺)
		distance1 = (Math.round(distance1 * 10000)) / 10000.0;
		
		//return focusDistances[parameters.FOCUS_DISTANCE_OPTIMAL_INDEX];
		return distance1;
	}

	/**
	 * sort 4Points
	 * @param list 4Points of non sorting
     */
	private void getSortLaserPostionForSort(ArrayList<PointF> list){
        if (list.size() != 4)
            return;
        
        PointF pointA = new PointF();
        PointF pointB = new PointF();
        PointF pointC = new PointF();
        PointF pointD = new PointF();
        //最左下角的是A點
        //先sort x的到AD兩點,在判斷Y取得AD,另外的兩點BC透過Y取得
        Collections.sort(list, new SortByX());
        if (list.get(0).y > list.get(1).y)
        {
        	pointA = list.get(0);
        	pointD = list.get(1);
        }
        else
        {
        	pointA = list.get(1);
        	pointD = list.get(0);
        }
        if (list.get(2).y > list.get(3).y)
        {
        	pointB = list.get(2);
        	pointC = list.get(3);
        }
        else
        {
        	pointB = list.get(3);
        	pointC = list.get(2);
        }
		list.clear();
		list.add(pointA);
		list.add(pointB);
		list.add(pointC);
		list.add(pointD);

	}
	//Sorting Method
	class SortByX  implements Comparator<PointF> {
		public int compare(final PointF a, final PointF b) {
			if (a.x < b.x) {
				return -1;
			}
			else if (a.x > b.x) {
				return 1;
			}
			else {
				return 0;
			}
		}
	}

	//Init Alpha Beta
	private void getInitAB(){
		// 取得 initial Alpah Beta
		// 取得Alpah Beta
		tmp_aAngle = 0;
		tmp_bAngle = 0;
		for (int i = 0; i < 31; i++) {
			getAngle();
		}
		count1 = 0;

		// 取得Initial Alpha Beta
		aInitAngle = aAngle;
		bInitAngle = bAngle;
		gyroscopeValues[0] = 0;
		gyroscopeValues[1] = 0;
		gyroscopeValues[2] = 0;

		// 顯示
		DecimalFormat df2 = new DecimalFormat("#.##");
		// String str2 = "Initial Alpha = " + df2.format(aInitAngle) +
		// " ,Beta =" + df2.format(bInitAngle);
		String str2 = "初始化 Alpha Beta !!!";
		mLogText.setText(str2);
	}
	//Get Alpha Beta
	private void getAB(){
		// 取得Alpah Beta
		tmp_aAngle = 0;
		tmp_bAngle = 0;
		for (int i = 0; i < 31; i++) {
			getAngle();
		}
		count1 = 0;

		// 取得Alpha與Beta的差
		// aAngle = aAngle - aInitAngle;
		// bAngle = bAngle - bInitAngle;
		aDiffAngle = aInitAngle - aAngle;
		if (aDiffAngle < -180)
			aDiffAngle = 360 + (aInitAngle - aAngle);
		if (aDiffAngle > 180)
			aDiffAngle = -(360 - (aInitAngle - aAngle));

		bDiffAngle = bAngle - bInitAngle;
		// 顯示
		DecimalFormat df3 = new DecimalFormat("#.##");
		String str3 = "Alpha = " + df3.format(aDiffAngle) + " ,Beta ="
				+ df3.format(bDiffAngle);
		mLogText.setText(str3);

		// 寫入角度標至檔案
		// saveLaserAngle(aAngle,bAngle);
	}
}
