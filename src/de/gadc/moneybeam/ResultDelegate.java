/*
 * Copyright 2012 MoneyBeam.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gadc.moneybeam;

import java.io.Serializable;

import com.paypal.android.MEP.PayPalResultDelegate;

/**
 * This delegate is being used to interact with the user.
 * 
 * @author tmesserschmidt@paypal.com
 * @author weiss@neofonie.de
 */
public class ResultDelegate implements PayPalResultDelegate, Serializable {
	private static final long	serialVersionUID	= 10001L;

	/**
	 * Notification that the payment has been completed successfully.
	 * 
	 * @param payKey
	 *            the pay key for the payment
	 * @param paymentStatus
	 *            the status of the transaction
	 */
	public void onPaymentSucceeded(String payKey, String paymentStatus) {
		ReceiveRequestActivity.showToast(R.string.paypal_success);
	}

	/**
	 * Notification that the payment has failed.
	 * 
	 * @param paymentStatus
	 *            the status of the transaction
	 * @param correlationID
	 *            the correlationID for the transaction failure
	 * @param payKey
	 *            the pay key for the payment
	 * @param errorID
	 *            the ID of the error that occurred
	 * @param errorMessage
	 *            the error message for the error that occurred
	 */
	public void onPaymentFailed(String paymentStatus, String correlationID,
			String payKey, String errorID, String errorMessage) {
		ReceiveRequestActivity.showToast(R.string.paypal_failed);
	}

	/**
	 * Notification that the payment was canceled.
	 * 
	 * @param paymentStatus
	 *            the status of the transaction
	 */
	public void onPaymentCanceled(String paymentStatus) {
		ReceiveRequestActivity.showToast(R.string.paypal_canceled);
	}
}
