package pl.epodreczniki.activity;

import java.lang.ref.WeakReference;

import com.google.zxing.PlanarYUVLuminanceSource;

import pl.epodreczniki.R;
import pl.epodreczniki.fragment.ScanWorkerFragment;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class DevScanActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback{
	
	private SurfaceView preview;
	
	private SurfaceHolder previewHolder;
	
	private Camera camera;
	
	private boolean cameraConfigured = false;
	
	private boolean inPreview = false;
	
	private int orientation;
	
	private ScanWorkerFragment workerFragment;
	
	private FrameHandler handler = new FrameHandler(this);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_dev_scan);
		final FragmentManager fm = getFragmentManager();
		workerFragment = (ScanWorkerFragment) fm.findFragmentByTag(ScanWorkerFragment.TAG);
		if(workerFragment == null){
			workerFragment = new ScanWorkerFragment();
			fm.beginTransaction().add(workerFragment, ScanWorkerFragment.TAG).commit();
		}
		preview = (SurfaceView) findViewById(R.id.ads_scan_preview);
		previewHolder = preview.getHolder();		
	}
	
	@Override
	protected void onResume() {	
		super.onResume();
		camera = Camera.open();
		startPreview();
		if(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE.equals(camera.getParameters().getFocusMode())){
			camera.autoFocus(null);	
		}		
		previewHolder.addCallback(this);
	}
	
	@Override
	protected void onPause() {	
		super.onPause();
		if(inPreview){
			camera.stopPreview();
		}
		previewHolder.removeCallback(this);
		camera.release();
		camera = null;
		inPreview = false;
	}
	
	@Override
	public void onBackPressed() {	
		setResult(RESULT_CANCELED);
		finish();
	}
	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		try{
			Size s = camera.getParameters().getPreviewSize();
			workerFragment.decode(data, s.width, s.height);	
		}catch(Exception e){
			Log.e("ERROR","camera already released?");
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {		
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		setCameraDisplayOrientation(this, 0, camera);		
		initPreview(width, height);
		startPreview();
		if(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE.equals(camera.getParameters().getFocusMode())){
			camera.autoFocus(null);			
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}

	private void initPreview(int width, int height) {
		if (camera != null && previewHolder.getSurface() != null) {
			try {
				camera.setPreviewDisplay(previewHolder);
			} catch (Throwable t) {				
				Toast.makeText(DevScanActivity.this, t.getMessage(),
						Toast.LENGTH_LONG).show();
			}
			if (!cameraConfigured) {
				Camera.Parameters parameters = camera.getParameters();
				for(String fm : parameters.getSupportedFocusModes()){
					Log.e("SUPPORTED","_"+fm);
				}
				if(parameters.getSupportedFocusModes().contains(Camera.Parameters.FLASH_MODE_AUTO)){
					parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
				}else if(parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
					parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);	
				}								
				Camera.Size size = getBestPreviewSize(width, height, parameters);
				if (size != null) {
					parameters.setPreviewSize(size.width, size.height);
					camera.setParameters(parameters);
					cameraConfigured = true;
				}
			}
		}
	}
	
	private Camera.Size getBestPreviewSize(int width, int height,
			Camera.Parameters parameters) {
		Camera.Size result = null;
		for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
			if (size.width <= width && size.height <= height) {
				if (result == null) {
					result = size;
				} else {
					int resultArea = result.width * result.height;
					int newArea = size.width * size.height;
					if (newArea > resultArea) {
						result = size;
					}
				}
			}
		}		
		return (result);
	}
	
	private void startPreview() {
		if (camera != null && cameraConfigured) {
			camera.startPreview();
			inPreview = true;
			camera.setOneShotPreviewCallback(this);
		}
	}
	
	public final Rect getBoundingRect(){	
		if(camera!=null){
			Camera.Size previewSize = camera.getParameters().getPreviewSize();
			float side = Math.min(previewSize.width, previewSize.height)-48;
		    float left = (previewSize.width-side)/2;
		    float top = (previewSize.height-side)/2;
		    return new Rect((int)left,(int)top,(int)(left+side),(int)(top+side));
		}
		return null;
	}
	
	public static void setCameraDisplayOrientation(DevScanActivity activity,
			int cameraId, Camera camera) {
		Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}
		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;
		} else {
			result = (info.orientation - degrees + 360) % 360;
		}
		activity.orientation = result;
		camera.setDisplayOrientation(result);
	}
	
	public synchronized PlanarYUVLuminanceSource buildLuminanceSource(
			byte[] data, int width, int height, Rect boundingRect) {		
		byte[] rotated = data;
		switch (orientation) {
		case 0:
			break;
		case 90:
			rotated = rotate(data, width, height);
			return new PlanarYUVLuminanceSource(rotated, height, width,
					boundingRect.top, boundingRect.left, boundingRect.height(),
					boundingRect.width(), false);

		case 180:
			break;
		case 270:
			rotated = rotate(data, width, height);
			break;
		}

		return new PlanarYUVLuminanceSource(rotated, width, height,
				boundingRect.left, boundingRect.top, boundingRect.width(),
				boundingRect.height(), false);
	}
	
	private byte[] rotate(byte[] data, int width, int height) {
		byte[] rotatedData = new byte[data.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++)
				rotatedData[x * height + height - y - 1] = data[x + y * width];
		}
		int tmp = width;
		width = height;
		height = tmp;
		return rotatedData;
	}
	
	public void sendSuccessMessage(String url) {
		final Message msg = handler.obtainMessage(R.id.qr_success);
		final Bundle b = new Bundle();
		b.putString(FrameHandler.DATA_URL, url);
		msg.setData(b);
		handler.sendMessage(msg);
	}

	public void sendFailMessage() {
		handler.sendMessage(handler.obtainMessage(R.id.qr_fail));
	}
	
	static class FrameHandler extends Handler {

		public static final String DATA_URL = "qr_url";

		private final WeakReference<DevScanActivity> actRef;

		FrameHandler(DevScanActivity act) {
			this.actRef = new WeakReference<DevScanActivity>(act);
		}

		@Override
		public void handleMessage(Message msg) {
			final DevScanActivity act = actRef.get();
			if (act != null) {				
				switch (msg.what) {
				case R.id.qr_success:					
					String data = msg.getData().getString(DATA_URL);
					Intent i = new Intent();
					i.putExtra(DevMainActivity.EXTRA_RESULT_URL, data);
					act.setResult(RESULT_OK, i);
					act.finish();
					break;
				case R.id.qr_fail:
					if (act.camera != null) {
						act.camera.setOneShotPreviewCallback(act);
					}
					break;
				}
			}
		}
	}
}
