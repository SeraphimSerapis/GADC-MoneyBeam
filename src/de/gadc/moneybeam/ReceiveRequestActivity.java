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

import java.math.BigDecimal;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.paypal.android.MEP.CheckoutButton;
import com.paypal.android.MEP.PayPal;
import com.paypal.android.MEP.PayPalPayment;

/**
 * This Activity is is opened when we receive a NDEF push. It handles the
 * payment by creating a new {@link Payment} object and handles the transaction.
 * 
 * @author tmesserschmidt@paypal.com
 * @author weiss@neofonie.de
 * 
 */
public class ReceiveRequestActivity extends Activity {
	private static Activity	activity;
	private String			currency, money, email;
	private LinearLayout	layout;
	private TextView		requestView;
	private CheckoutButton	payButton;
	private PayPal			pp;
	private boolean			paypalIsInit	= false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_receive);
		activity = this;
		initLibrary();
		initUi();
	}

	public void initUi() {
		layout = (LinearLayout) findViewById(R.id.receive_layout);
		requestView = (TextView) findViewById(R.id.receive_request_text);
	}

	public void initLibrary() {
		new Thread() {
			@Override
			public void run() {
				pp = PayPal.getInstance();

				if (pp == null) {
					pp = PayPal.initWithAppID(activity,
							"APP-80W284485P519543T", PayPal.ENV_SANDBOX);
					pp.setLanguage("en_US");
					pp.setFeesPayer(PayPal.FEEPAYER_EACHRECEIVER);
					pp.setShippingEnabled(true);
					pp.setDynamicAmountCalculationEnabled(false);

					paypalIsInit = true;

					payButton = pp.getCheckoutButton(activity,
							PayPal.BUTTON_194x37, CheckoutButton.TEXT_PAY);
					payButton.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							onPayClick(v);
						}
					});
					activity.runOnUiThread(new Runnable() {
						public void run() {
							layout.addView(payButton);
						}
					});
				}
			}
		}.start();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
			processIntent(getIntent());
		}
	}

	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent);
	}

	/**
	 * Creates and returns a new instance of a {@link PayPalPayment}. It used
	 * the provided currency, email and requested money to init the Object.
	 * 
	 * @return the ready to use {@link PayPalPayment}
	 */
	public PayPalPayment createPayment() {
		final PayPalPayment payment = new PayPalPayment();
		payment.setCurrencyType(currency);
		payment.setRecipient(email);
		payment.setSubtotal(new BigDecimal(money));
		payment.setPaymentType(PayPal.PAYMENT_TYPE_GOODS);
		return payment;
	}

	/**
	 * This static method is used for the {@link ResultDelegate}. It provides a
	 * quick way for toasting.
	 * 
	 * @param text
	 *            the id of the message to show
	 */
	public static void showToast(final int text) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
			}
		});
	}

	/**
	 * Handles the whole OnClick-Event for PayPal's {@link Button} by creating a
	 * new {@link PayPalPayment} and passing it to the Mobile Payments Library
	 * via an {@link Intent}.
	 * 
	 * @param view
	 */
	public void onPayClick(View view) {
		if (paypalIsInit) {
			final PayPalPayment payment = createPayment();
			final Intent checkoutIntent = PayPal.getInstance().checkout(
					payment, this, new ResultDelegate());
			startActivity(checkoutIntent);
		}
	}

	/**
	 * Parses the NDEF Message from the {@link Intent} and sets the
	 * {@link TextView}'s text.
	 */
	private void processIntent(Intent intent) {
		final Parcelable[] rawMsgs = intent
				.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		final NdefMessage msg = (NdefMessage) rawMsgs[0];

		try {
			final JSONObject object = new JSONObject(new String(
					msg.getRecords()[0].getPayload()));

			email = object.getString(Configuration.PREF_MAIL);
			currency = object.getString(Configuration.CURRENCY);
			money = object.getString(Configuration.DOLLAR) + "."
					+ object.getString(Configuration.CENTS);

			final String moneyString = object.getString(Configuration.DOLLAR)
					+ "," + object.getString(Configuration.CENTS) + " "
					+ object.getString(Configuration.CURRENCY);

			requestView.setText(getString(R.string.xy_wants_z_dollar_from_you,
					email, moneyString));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
