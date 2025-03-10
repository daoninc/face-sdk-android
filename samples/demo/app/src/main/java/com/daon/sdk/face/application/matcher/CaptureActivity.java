package com.daon.sdk.face.application.matcher;


import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.MenuItem;

import com.daon.sdk.face.DaonFace;
import com.daon.sdk.face.application.EdgeToEdgeActivity;
import com.daon.sdk.face.application.R;


public class CaptureActivity extends EdgeToEdgeActivity implements CaptureFaceFragment.CaptureCallback {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_matching);

		try {
			DaonFace df = new DaonFace(this, DaonFace.OPTION_RECOGNITION);
			if (df.isEnrolled())
				replaceFragment(new VerificationFragment());
			else
				replaceFragment(new EnrollmentFragment());
		} catch (Exception e) {
			showMessage(R.string.error_initializing_daonface);
		}
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == 0) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}


	@Override
	public void onCaptureComplete() {
		finish();
	}

	@Override
	public void onCaptureFailed(int resid) {
		showMessage(resid);
	}


	public void replaceFragment(Fragment frgmnt) {

		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

		fragmentTransaction.replace(R.id.fragment, frgmnt);
		fragmentTransaction.commit();
	}

	private void showMessage(int resid) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(false);
		builder.setMessage(resid);

		builder.setPositiveButton("OK", (dialogInterface, i) -> {
			finish();
		});

		AlertDialog dialog = builder.create();
		dialog.show();
	}

}
