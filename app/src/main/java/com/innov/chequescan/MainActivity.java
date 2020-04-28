package com.innov.chequescan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

import com.innov.chequescan.camera.CameraSource;
import com.innov.chequescan.camera.CameraSourcePreview;
import com.innov.chequescan.others.GraphicOverlay;
import com.innov.chequescan.text_detection.TextRecognitionProcessor;
import com.google.firebase.FirebaseApp;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements ISuccess, View.OnClickListener {

	//region ----- Instance Variables -----

	private CameraSource cameraSource = null;
	private CameraSourcePreview preview;
	//private GraphicOverlay graphicOverlay;
	private com.innov.chequescan.camera.GraphicOverlay graphicOverlayNew;
	private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
	private static final int CAMERA_PERMISSION_REQUEST = 0;
	private View flashButton = null;
	private CheckBox checkBox = null;
	private static boolean checkboxStatus = false;

	private static String TAG = MainActivity.class.getSimpleName().toString().trim();

	//endregion

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		FirebaseApp.initializeApp(this);

		preview = (CameraSourcePreview) findViewById(R.id.camera_source_preview);
		if (preview == null) {
			Log.d(TAG, "Preview is null");
		}
		/*graphicOverlay = (GraphicOverlay) findViewById(R.id.graphics_overlay);
		if (graphicOverlay == null) {
			Log.d(TAG, "graphicOverlay is null");
		}
*/


		graphicOverlayNew = ( com.innov.chequescan.camera.GraphicOverlay) findViewById(R.id.graphics_overlay1);
		flashButton = findViewById(R.id.flash_button);
		flashButton.setOnClickListener(this);
		checkBox = findViewById(R.id.cb_scan);
		checkBox.setOnClickListener(this);


		requestCameraPermission();
	}
	protected boolean hasCameraPermission() {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
				|| ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION
		) == PackageManager.PERMISSION_GRANTED;
	}

	@TargetApi(Build.VERSION_CODES.M)
	protected void requestCameraPermission() {
		// For Android M and onwards we need to request the camera permission from the user.
		if (!hasCameraPermission()) {
			// The user already denied the permission once, we don't ask twice.
			requestPermissions(new String[] { CAMERA_PERMISSION }, CAMERA_PERMISSION_REQUEST);

		} else {
			// We already have the permission or don't need it.
			createCameraSource();
			startCameraSource();
		}
	}

	@Override
	public void onRequestPermissionsResult(
			int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == CAMERA_PERMISSION_REQUEST) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				createCameraSource();
				startCameraSource();
			}
		} else {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}


	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		startCameraSource();
		if(checkboxStatus){
			checkBox.setSelected(true);
		}
	}

	/** Stops the camera. */
	@Override
	protected void onPause() {
		super.onPause();
		flashButton.setSelected(false);
		//checkBox.setSelected(false);
		preview.stop();
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		if (cameraSource != null) {
			cameraSource.release();
		}
		checkBox.setSelected(false);
	}

	private void createCameraSource() {

		if (cameraSource == null) {
			cameraSource = new CameraSource(this, graphicOverlayNew);
			cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
		}

		cameraSource.setMachineLearningFrameProcessor(new TextRecognitionProcessor(graphicOverlayNew,this));
	}

	private void startCameraSource() {
		if (cameraSource != null) {
			try {
				if (preview == null) {
					Log.d(TAG, "resume: Preview is null");
				}
				if (graphicOverlayNew == null) {
					Log.d(TAG, "resume: graphOverlay is null");
				}
				preview.start(cameraSource, graphicOverlayNew);
			} catch (IOException e) {
				Log.e(TAG, "Unable to start camera source.", e);
				cameraSource.release();
				cameraSource = null;
			}
		}
	}

	@Override
	public void onSuccess(String text) {
		if(checkBox.isSelected()) {
			preview.stop();
			AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
			builder1.setTitle("Matched our pattern");
			builder1.setMessage(text);
			builder1.setCancelable(true);

			builder1.setPositiveButton(
					"Yes",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
							startCameraSource();
						}
					});

			builder1.setNegativeButton(
					"Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
							startCameraSource();
						}
					});

			AlertDialog alert11 = builder1.create();
			alert11.show();
		}
	}


	@Override
	public void onClick(View view) {
		if(view.getId() == R.id.flash_button){
			if (flashButton.isSelected()) {
				flashButton.setSelected(false);
				cameraSource.updateFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			} else {
				flashButton.setSelected(true);
				cameraSource.updateFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			}
		}else if(view.getId() == R.id.cb_scan){
			if (checkBox.isSelected()) {
				checkboxStatus = false;
				checkBox.setSelected(false);
			} else {
				checkboxStatus = true;
				checkBox.setSelected(true);
			}
		}

	}
}

