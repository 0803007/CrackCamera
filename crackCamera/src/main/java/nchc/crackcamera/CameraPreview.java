/**
 *
 */
package nchc.crackcamera;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.OnZoomChangeListener;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;


class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    SurfaceHolder mHolder;
    public Camera camera;
    int nPreviewRecord = 0;
    int stillCount = 0;

	Vector<byte[]> records = new Vector<byte[]>();
	Vector<Long> times = new Vector<Long>();
	
	int fcount = 0;
	Date start;
	public boolean hasStartPreview = false;  
	
	PreviewCallback mPreviewCallback;
    
	CameraPreview(Context context, PreviewCallback previewCallback) {
        super(context);
        
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mPreviewCallback = previewCallback;
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        //camera = openFrontFacingCamera();
        //camera.setDisplayOrientation(0);
    }

	
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        //camera = Camera.open();
        try {
			camera.setPreviewDisplay(holder);			
			camera.setPreviewCallback(mPreviewCallback);
			Camera.Parameters camParas = camera.getParameters();
			
			/*
			if (camParas.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_AUTO) || 
				camParas.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_MACRO))
				camera.autoFocus(onCamAutoFocus);
			else
				Toast.makeText(getContext(), "照相機不支援自動對焦！", Toast.LENGTH_SHORT).show();*/

		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
	Camera.AutoFocusCallback onCamAutoFocus = new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			// TODO Auto-generated method stub
			Camera.Parameters parameters = camera.getParameters();
			float[]distances = new float[3];
			parameters.getFocusDistances(distances);
            Log.d("Focus Mode: ", parameters.getFocusMode());
            Log.d("focus distance near", Float.toString(distances[0]));
            Log.d("focus distance optimum", Float.toString(distances[1]));
            Log.d("focus distance far", Float.toString(distances[2]));
            Log.d("focus length", Float.toString(parameters.getFocalLength()));

			if (success)
				Toast.makeText(getContext(), "對焦成功!!！", Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(getContext(), "對焦失敗!!！", Toast.LENGTH_SHORT).show();
		}
		
	};
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused.
        //camera.stopPreview();
        //camera.release();
        //camera = null;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
    	if (camera != null && hasStartPreview == false) {  
    		Camera.Parameters parameters = camera.getParameters();

    		//設置preview的尺寸
    		w = parameters.getSupportedPreviewSizes().get(0).width; //1024
    		h = parameters.getSupportedPreviewSizes().get(0).height;      //763
    		parameters.setPreviewSize(w,h);
    		/*List<Size> sizes = parameters.getSupportedPreviewSizes();
    		Size maxSize = parameters.getPictureSize();
    		for(Size size:sizes){
    			maxSize= maxSize.width * maxSize.height>size.width * size.height?maxSize:size;
    		}
    		parameters.setPreviewSize(maxSize.width, maxSize.height);*/

    		//設置照片的尺寸
    		List<Size> sizes = parameters.getSupportedPictureSizes();
    		Size maxSize = parameters.getPictureSize();
    		for(Size size:sizes){
    			maxSize= maxSize.width * maxSize.height>size.width * size.height?maxSize:size;
    		}
    		parameters.setPictureSize(maxSize.width, maxSize.height);

    		//設置Flash
    		parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
    		

    		//parameters.setPictureSize(4096, 3072);
    		//List<Integer> fps = parameters.getSupportedPreviewFrameRates();
    		//parameters.setPreviewFpsRange(10000, 10000);
    		
    		//設置Focus參數
    		Rect newRect = new Rect(-100,-100,100,100);
    		Camera.Area focusArea = new Camera.Area(newRect, 1000);
    		List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
    		focusAreas.add(focusArea);

			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
    		parameters.setFocusAreas(focusAreas);


    		//寫入參數
    		camera.setParameters(parameters);

    		camera.startPreview();
    		hasStartPreview = true;
    		/*
    		int a= 0;
    		camera.startSmoothZoom(a+1);
            camera.setZoomChangeListener(new OnZoomChangeListener() { 

                public void onZoomChange(int zoomValue, boolean stopped, Camera 
    camera) { 
                        // TODO Auto-generated method stub 
                	camera.startSmoothZoom(zoomValue);


                } 
        }); */
    	}
    }
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		//Log.d(TAG, "畫框");
        // 黃色的線
		Paint p = new Paint();
		p.setColor(Color.YELLOW);
        // 方框
		float w = canvas.getWidth();
		float h = canvas.getHeight();
        // 上下左右四邊
		int margin = 90;
		canvas.drawLine(margin, margin, w - margin, margin, p);
		canvas.drawLine(margin, h - margin, w - margin, h - margin, p);
		canvas.drawLine(margin, margin, margin, h - margin, p);
		canvas.drawLine(w - margin, margin, w - margin, h - margin, p);
	}


}
