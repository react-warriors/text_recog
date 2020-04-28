// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.innov.chequescan.text_detection;

import androidx.annotation.NonNull;

import android.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;

import com.innov.chequescan.ISuccess;
import com.innov.chequescan.MainActivity;
import com.innov.chequescan.camera.CameraReticleAnimator;
import com.innov.chequescan.camera.TextReticleGraphic;
import com.innov.chequescan.others.FrameMetadata;
import com.innov.chequescan.camera.GraphicOverlay;
//import com.innov.chequescan.others.VisionProcessorBase;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * Processor for the text recognition demo.
 */
public class TextRecognitionProcessor {

	private static final String TAG = "TextRecProc";

	private final FirebaseVisionTextRecognizer detector;

	private CameraReticleAnimator cameraReticleAnimator;
	ISuccess iSuccess;

	// Whether we should ignore process(). This is usually caused by feeding input data faster than
	// the model can handle.
	private final AtomicBoolean shouldThrottle = new AtomicBoolean(false);

	public TextRecognitionProcessor(GraphicOverlay graphicOverlayNew) {
		detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
	}

	public TextRecognitionProcessor(GraphicOverlay graphicOverlay, ISuccess success) {
		detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
		cameraReticleAnimator = new CameraReticleAnimator(graphicOverlay);
		iSuccess = success;
	}



	//region ----- Exposed Methods -----


	public void stop() {
		try {
			detector.close();
		} catch (IOException e) {
			Log.e(TAG, "Exception thrown while trying to close Text Detector: " + e);
		}
	}


	public void process(ByteBuffer data, FrameMetadata frameMetadata, GraphicOverlay graphicOverlay) throws FirebaseMLException {

		if (shouldThrottle.get()) {
			return;
		}
		FirebaseVisionImageMetadata metadata =
				new FirebaseVisionImageMetadata.Builder()
						.setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_YV12)
						.setWidth(frameMetadata.getWidth())
						.setHeight(frameMetadata.getHeight())
						.setRotation(frameMetadata.getRotation())
						.build();

		detectInVisionImage(FirebaseVisionImage.fromByteBuffer(data, metadata), frameMetadata, graphicOverlay);
	}

	//endregion

	//region ----- Helper Methods -----

	protected Task<FirebaseVisionText> detectInImage(FirebaseVisionImage image) {
		return detector.processImage(image);
	}


	protected void onSuccess( @NonNull FirebaseVisionText results, @NonNull FrameMetadata frameMetadata, @NonNull GraphicOverlay graphicOverlay) {

		graphicOverlay.clear();
		String matchedText = "";

		cameraReticleAnimator.start();
		graphicOverlay.add(new TextReticleGraphic(graphicOverlay, cameraReticleAnimator));
		graphicOverlay.invalidate();

		List<FirebaseVisionText.TextBlock> blocks = results.getTextBlocks();

		for (int i = 0; i < blocks.size(); i++) {
			List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
			for (int j = 0; j < lines.size(); j++) {
				List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
				String text = lines.get(j).getText().replace(" ","");
				Log.e("TEXT12",text);
				boolean chk = Pattern.compile("^(?=(?:.*?\\d){6})([a-zA-Z]*)").matcher(text).find();
				text = text.replaceAll("L","1");
				text = text.replaceAll("i","1");
				text = text.replaceAll("G","6");
				text = text.replaceAll("O","0");
				text = text.replaceAll("B","8");
				text = text.replaceAll("\\?","7");
				//boolean chk1 = Pattern.compile(("(^(\\d){9})")).matcher(text).find();
				if(chk && text.split(":").length>1){
					Log.e("MATCHED",text);

                   String[] test = text.split(":");
					//Log.e("STR",test[0]+" :: "+test[1]);
					/*for (String chk1:test
						 ) {
						Log.e("STR",chk1);
					}*/
					GraphicOverlay.Graphic textGraphic = new TextGraphic(graphicOverlay, lines.get(j));
					graphicOverlay.add(textGraphic);
					matchedText+= " :: "+text;


				}
				//GraphicOverlay.Graphic textGraphic = new TextGraphic(graphicOverlay, lines.get(j));
				//graphicOverlay.add(textGraphic);

				/*for (int k = 0; k < elements.size(); k++) {
					String text = elements.get(k).getText();
					//boolean isAccountNo = text.matches("^[0-9]{7,14}$");
					//if(isAccountNo) Log.e("ACCOUNT NO",text);
					Log.e("TEXT",text);
					GraphicOverlay.Graphic textGraphic = new TextGraphic(graphicOverlay, elements.get(k));
					graphicOverlay.add(textGraphic);
					*//*if(text.length() >=4) {
						boolean isValidText = new IbanValidator(elements.get(k).getText()).isValid();
						if (isValidText) {
							GraphicOverlay.Graphic textGraphic = new TextGraphic(graphicOverlay, elements.get(k));
							graphicOverlay.add(textGraphic);
						}
					}*//*

				}*/
			}
		}
		if(!TextUtils.isEmpty(matchedText)){
			iSuccess.onSuccess(matchedText);
		}
	}

	protected void onFailure(@NonNull Exception e) {
		Log.w(TAG, "Text detection failed." + e);
	}

	private void detectInVisionImage( FirebaseVisionImage image, final FrameMetadata metadata, final GraphicOverlay graphicOverlay) {

		detectInImage(image)
				.addOnSuccessListener(
						new OnSuccessListener<FirebaseVisionText>() {
							@Override
							public void onSuccess(FirebaseVisionText results) {
								shouldThrottle.set(false);
								TextRecognitionProcessor.this.onSuccess(results, metadata, graphicOverlay);
							}
						})
				.addOnFailureListener(
						new OnFailureListener() {
							@Override
							public void onFailure(@NonNull Exception e) {
								shouldThrottle.set(false);
								TextRecognitionProcessor.this.onFailure(e);
							}
						});
		// Begin throttling until this frame of input has been processed, either in onSuccess or
		// onFailure.
		shouldThrottle.set(true);
	}

	//endregion


}
