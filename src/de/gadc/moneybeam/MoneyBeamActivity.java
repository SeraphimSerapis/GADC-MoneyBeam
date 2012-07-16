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

import java.nio.charset.Charset;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * This is MoneyBeam's main Activity. It is used to request a certain amount of
 * money from another user.
 * 
 * @author tmesserschmidt@paypal.com
 * @author weiss@neofonie.de
 * 
 */
public class MoneyBeamActivity extends Activity implements
		CreateNdefMessageCallback {
	private NfcAdapter					nfcAdapter;
	private EditText					dollars, cents;
	private Spinner						currency;
	private ArrayAdapter<CharSequence>	adapter;
	private SharedPreferences			prefs;
	boolean								ready			= true;
	boolean								paypalIsInit	= false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		if (!prefs.contains(Configuration.PREF_MAIL)) {
			showEmailDialog();
		}

		setContentView(R.layout.activity_main);
		initUi();
		registerNFC();
	}

	/**
	 * Used as onClick handler for the "ready"-{@link Button}
	 * 
	 * @param the
	 *            clicked {@link View} (handled by the system itself}
	 */
	public void onReadyClick(View view) {
		setReadyState();
	}

	/**
	 * This method creates a {@link JSONObject} from the current data which may
	 * be passed via NFC.
	 * 
	 * @return the final {@link JSONObject}
	 * @throws JSONException
	 */
	public JSONObject generateJSON() throws JSONException {
		final JSONObject object = new JSONObject();
		String dollarString = dollars.getText().toString().trim();
		if (dollarString.length() == 0) {
			dollarString = "0000";
		}
		String centsString = cents.getText().toString().trim();
		if (centsString.length() == 0) {
			centsString = "00";
		}
		object.put(Configuration.PREF_MAIL,
				prefs.getString(Configuration.PREF_MAIL, ""));
		object.put(Configuration.DOLLAR, dollarString);
		object.put(Configuration.CENTS, centsString);
		object.put(Configuration.CURRENCY, currency.getSelectedItem()
				.toString().trim());
		return object;
	}

	/**
	 * This method creates a new {@link NdefMessage} by using a
	 * {@link JSONObject}.
	 * 
	 * @return the {@link NdefMessage} for the message callback
	 */
	public NdefMessage createNdefMessage(NfcEvent event) {
		NdefMessage msg = null;
		String text = null;
		try {
			text = generateJSON().toString();
			msg = new NdefMessage(new NdefRecord[] {
					createMimeRecord("application/paypal-moneybeam",
							text.getBytes()),
					NdefRecord.createApplicationRecord(getPackageName())
			});

		} catch (JSONException e) {
			e.printStackTrace();
		}
		return msg;
	}

	/**
	 * Creates a custom MIME type encapsulated in an NDEF record.
	 * 
	 * @return the {@link NdefRecord} for usage in the {@link NdefMessage}
	 */
	public NdefRecord createMimeRecord(String mimeType, byte[] payload) {
		byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
		final NdefRecord mimeRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
				mimeBytes, new byte[0], payload);
		return mimeRecord;
	}

	/**
	 * This method presents the user the dialog that enables him to enter his
	 * email address.
	 */
	private void showEmailDialog() {
		final EditText editText = new EditText(this);
		AlertDialog.Builder emailDialogBuilder = new Builder(this);
		emailDialogBuilder.setTitle(R.string.hint_email);
		editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
		editText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		emailDialogBuilder.setView(editText);
		emailDialogBuilder.setPositiveButton(R.string.button_ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						String mail = editText.getText().toString();
						prefs.edit().putString(Configuration.PREF_MAIL, mail)
								.commit();
					}
				});
		emailDialogBuilder.setNegativeButton(R.string.button_cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		emailDialogBuilder.show();
	}

	/**
	 * This method handles the NFC registration and sets the callback.
	 */
	private void registerNFC() {
		nfcAdapter = NfcAdapter.getDefaultAdapter(this);

		if (nfcAdapter == null) {
			Toast.makeText(this, R.string.nfc_not_available, Toast.LENGTH_LONG)
					.show();
			finish();
			return;
		}

		nfcAdapter.setNdefPushMessageCallback(this, this);
	}

	/**
	 * This method handles the initialization of the whole UI.
	 */
	private void initUi() {
		dollars = (EditText) findViewById(R.id.money_dollars);
		cents = (EditText) findViewById(R.id.money_cents);
		currency = (Spinner) findViewById(R.id.money_currency);
		adapter = ArrayAdapter.createFromResource(this, R.array.currencies,
				R.layout.spinner_text);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		currency.setAdapter(adapter);
	}

	/**
	 * Called when the "ready"-{@link Button} got clicked.
	 */
	private void handleReady() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.beam_title)
				.setMessage(
						getString(R.string.beam_message, getCurrentMoney()
								+ " " + currency.getSelectedItem().toString()))
				.setCancelable(true)
				.setNeutralButton(R.string.button_cancel_please,
						new OnClickListener() {

							public void onClick(DialogInterface dialog,
									int which) {
								setReadyState();
							}
						}).show();
	}

	private void setReadyState() {
		if (ready) {
			handleReady();
		}
		ready = !ready;
		dollars.setEnabled(ready);
		cents.setEnabled(ready);
		currency.setEnabled(ready);
	}

	/**
	 * This method handles the two {@link EditText}s and adds some protective
	 * coding.
	 * 
	 * @return the current amount
	 */
	private String getCurrentMoney() {
		String dollarsText = dollars.getText().toString();
		String centsText = cents.getText().toString();

		int tmpDollars = Integer.parseInt(dollarsText.isEmpty() ? "0"
				: dollarsText);
		int tmpCents = Integer.parseInt(centsText.isEmpty() ? "0" : centsText);

		if (tmpCents < 10) {
			centsText = "0" + tmpCents;
		}

		return tmpDollars + "." + centsText;
	}
}
