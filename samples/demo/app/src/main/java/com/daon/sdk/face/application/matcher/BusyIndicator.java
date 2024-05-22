// COPYRIGHT (c) 2014 by Daon Holdings Limited. All rights reserved.
//
// This software is the confidential and proprietary property and information
// of Daon Holdings Limited.
//
// You shall not disclose such Confidential Information and shall use it
// only in accordance with the terms of the license agreement entered into
// with Daon.
//
// Products sold or licensed by Daon are covered by the terms of its
// contractual agreements, license agreements and the warranty and
// guarantees therein. Daon reserves the right to discontinue production
// and change specifications and processes at any time without notice.
//
// No part of this software may be reproduced in any form without the
// prior written consent of Daon.

package com.daon.sdk.face.application.matcher;


import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.daon.sdk.face.application.R;


public class BusyIndicator {

	private static BusyIndicator instance = null;

	public static BusyIndicator getInstance() {
		if (instance == null)
			instance = new BusyIndicator();
		return instance;
	}

	private static class CustomProgressDialog extends android.app.Dialog {
		TextView message;

		final ProgressBar progressBar;

		public CustomProgressDialog(Context context) {
			super(context);

			requestWindowFeature(Window.FEATURE_NO_TITLE);

			setContentView(R.layout.busy);

			if (getWindow() != null)
				getWindow().getDecorView().setBackgroundResource(android.R.color.transparent);

			message = findViewById(R.id.message);
			progressBar = findViewById(R.id.progress);
		}

		public void hideProgress() {
			progressBar.setVisibility(View.GONE);
		}

		public void setMessage(int msg) {
			message.setText(msg);
		}
	}

	private  CustomProgressDialog dlg = null;


	public void setBusy(Activity activity) {
		setBusy(activity, true, R.string.please_wait, true, false);
	}

	public void setNotBusy(Activity activity) {
		setBusy(activity, false, 0, true, false);
	}


	public void setBusy(Activity activity, boolean busy, int message, boolean progress, boolean cancelable) {
		try {
			if (busy) {
				
				if (dlg != null)
					dlg.dismiss();
			
				dlg = new BusyIndicator.CustomProgressDialog(activity);
				if (!progress)
					dlg.hideProgress();

				if (message > 0)
					dlg.setMessage(message);

				dlg.setCancelable(cancelable);
				dlg.setOnCancelListener(dialog -> {
					if (dlg != null) {
						dlg.dismiss();
						dlg = null;
					}
				});
				
				dlg.show();	
			} else {
				if (dlg != null) {
					dlg.dismiss();
					dlg = null;
				}
			}
		} catch (Exception e) {
			dlg = null;
		}
	}
}
