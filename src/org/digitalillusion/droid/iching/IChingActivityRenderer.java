package org.digitalillusion.droid.iching;

import java.util.ArrayList;
import java.util.Arrays;

import org.digitalillusion.droid.iching.changinglines.ChangingLinesEvaluator;
import org.digitalillusion.droid.iching.utils.Consts;
import org.digitalillusion.droid.iching.utils.RemoteResolver;
import org.digitalillusion.droid.iching.utils.SettingsManager;
import org.digitalillusion.droid.iching.utils.SettingsManager.SETTINGS_MAP;
import org.digitalillusion.droid.iching.utils.Utils;
import org.digitalillusion.droid.iching.utils.sql.HexSection;
import org.digitalillusion.droid.iching.utils.sql.HexSectionDataSource;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ActivityInfo;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Decorator that performs the rendering duties onto the views
 *
 * @author digitalillusion
 */
public class IChingActivityRenderer extends Activity {
	
	/** The consultation mode, being using the oracle or reading the book **/
	protected enum READ_DESC_MODE {
		ORACLE,
		VIEW_HEX
	}
	
	/** The consultation mode **/
	protected READ_DESC_MODE mode;
	
	/** SORTED Subset of the hexagrams set when all lines changing have a particular meaning **/
	private static final Integer[] ICHING_ALL_LINES_DESC = new Integer[] { 1, 2, 12, 47 };
	
	/** Settings manager**/
	protected SettingsManager settings;
	
	/** The local data source for the hexagrams sections strings **/
	protected HexSectionDataSource dsHexSection;
	
	/** The currently selected section or changing line **/
	protected String currentSection;
	
	/** The currently selected hexagram **/
	protected String currentHex;
	
	/** The edit hexagram description dialog **/
	protected AlertDialog editDescDialog;
	
	
	/** 
	 * Render a row of the hex
	 * 
	 * @param index the row index from 0 (first) to 6 (last)
	 * @param coinsValue The value of the coins for this row
	 * @param renderMobileLines True to show mobile lines, false to render them fixed
	 */
	protected void renderRow(int index, int coinsValue, boolean renderMobileLines) {
		TableRow row = null;
		switch (index) {
			case 0:
				row = (TableRow) findViewById(R.id.hexRow1);
				break;
			case 1:
				row = (TableRow) findViewById(R.id.hexRow2);
				break;
			case 2:
				row = (TableRow) findViewById(R.id.hexRow3);
				break;
			case 3:
				row = (TableRow) findViewById(R.id.hexRow4);
				break;
			case 4:
				row = (TableRow) findViewById(R.id.hexRow5);
				break;
			case 5:
				row = (TableRow) findViewById(R.id.hexRow6);
				break;
		}
		
		int lineRes = 0;
		switch (coinsValue) {
			case Consts.ICHING_OLD_YIN:
				lineRes = R.drawable.oldyin;
				break;
			case Consts.ICHING_YOUNG_YANG:
				lineRes = R.drawable.yang;
				break;
			case Consts.ICHING_YOUNG_YIN:
				lineRes = R.drawable.yin;
				break;
			case Consts.ICHING_OLD_YANG:
				lineRes = R.drawable.oldyang;
				break;
		}
		
		if (!renderMobileLines) {
			if (coinsValue == Consts.ICHING_OLD_YIN) {
				lineRes = R.drawable.yin;
			} else if (coinsValue == Consts.ICHING_OLD_YANG) {
				lineRes = R.drawable.yang;
			}
		}
		
		row.setBackgroundResource(lineRes);
	}
	
	/**
	 * Renders the changing lines tab of the readDesc layout, given the associated hexagram
	 * 
	 * @param question The posed question
	 * @param hexToRender The hexagram to evaluate for changing lines
	 * @param changing The changing line index
	 * @param mode The mode used to display the view
	 */
	protected void renderReadDescChanging(final String question, final int[] hexToRender, final int changing, READ_DESC_MODE mode) {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		final TextView tvDescTitle = (TextView) findViewById(R.id.tvHexName);
		currentHex = Utils.hexMap(hexToRender);	
		tvDescTitle.setText(Utils.s(R.string.read_changing));
		
		LinearLayout layButtonsAndChanging = (LinearLayout) findViewById(R.id.layButtonsAndChanging);
		for (int i = 0; i < layButtonsAndChanging.getChildCount(); i++) {
			layButtonsAndChanging.getChildAt(i).setVisibility(View.GONE);
		}
		
		int changingCount = 0;
		for (int i = 0; i < 6; i++) {
			if (ChangingLinesEvaluator.isChangingLine(hexToRender[i])) {
				changingCount++;
			}
			renderRow(i, hexToRender[i], true);
		}

		final TextView tvQuestion = (TextView) findViewById(R.id.tvQuestionReadDesc);
		if (question != null && !question.equals("")) {
			tvQuestion.setText(question);
		} else {
			tvQuestion.setVisibility(View.GONE);
		}
		
		final TextView etChanging = (TextView) findViewById(R.id.tvChanging);
		final EditText etOutput = (EditText) findViewById(R.id.etOutput);
		
		etChanging.setVisibility(View.VISIBLE);
		String desc = "";
		etOutput.setText(desc);
		
		// Force manual selection of changing lines
		final READ_DESC_MODE selectedMode;
		if (changing == ChangingLinesEvaluator.ICHING_APPLY_MANUAL) {
			selectedMode = READ_DESC_MODE.VIEW_HEX;
		} else {
			selectedMode = mode;
		}	
		switch (selectedMode) {
			case ORACLE :
				currentSection = RemoteResolver.ICHING_REMOTE_SECTION_LINE + (changing + 1);
				if (changingCount == 0) {
					desc = Utils.s(R.string.read_changing_none) + "<br/>";
				} else {
					int resId = changingCount == 1 ? R.string.read_changing_one : R.string.read_changing_count;
					desc = Utils.s(resId, new Integer[] { changingCount }) + "<br/>";
				}
				
				OnClickListener retryAction = new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						renderReadDescChanging(question, hexToRender, changing, selectedMode);
					}
				};
				switch (changing) {
					case ChangingLinesEvaluator.ICHING_APPLY_BOTH :
						desc += "<em>" + Utils.s(R.string.read_changing_apply_ht) + "</em>";
						int intMap = Integer.parseInt(currentHex);
						for (int allLines : ICHING_ALL_LINES_DESC) {
							if (intMap == allLines) {
								RemoteResolver.renderRemoteString(etOutput, retryAction, this);
								break;
							}
						}
						break;
					case ChangingLinesEvaluator.ICHING_APPLY_CAST :
						desc += "<em>" + Utils.s(R.string.read_changing_apply_h) + "</em>";
						break;
					case ChangingLinesEvaluator.ICHING_APPLY_TRANSFORMED :
						desc += "<em>" + Utils.s(R.string.read_changing_apply_t) + "</em>";
						break;
					case ChangingLinesEvaluator.ICHING_APPLY_NONE :
						desc += "<em>" + Utils.s(R.string.read_changing_apply_n) + "</em>";
						break;
					default :
						desc += "<em>" + Utils.s(R.string.read_changing_apply, new Integer[] { changing + 1 }) + "</em>";
						RemoteResolver.renderRemoteString(etOutput, retryAction, this);
				}
				break;
			case VIEW_HEX :
				desc = Utils.s(R.string.read_changing_select) + "<br/>";
				
				ArrayList<String> lines = new ArrayList<String>();
				lines.add(Utils.s(R.string.read_changing_select_line1));
				lines.add(Utils.s(R.string.read_changing_select_line2));
				lines.add(Utils.s(R.string.read_changing_select_line3));
				lines.add(Utils.s(R.string.read_changing_select_line4));
				lines.add(Utils.s(R.string.read_changing_select_line5));
				lines.add(Utils.s(R.string.read_changing_select_line6));
				
				int hexId = Integer.parseInt(currentHex);
				if (Arrays.binarySearch(ICHING_ALL_LINES_DESC, hexId) >= 0) {
					lines.add(Utils.s(R.string.read_changing_select_all));
				}
				
				Spinner spinner = (Spinner) findViewById(R.id.spChanging);
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(
					getApplicationContext(),  
					android.R.layout.simple_spinner_item,
					lines.toArray(new String[lines.size()])
				);
				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				spinner.setAdapter(adapter);
				spinner.setVisibility(View.VISIBLE);
				spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

					public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
						position = (position + 1 > 6) ? ChangingLinesEvaluator.ICHING_APPLY_BOTH : position + 1;
						currentSection = RemoteResolver.ICHING_REMOTE_SECTION_LINE + position;
						RemoteResolver.renderRemoteString(
							etOutput, 
							new OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									renderReadDescChanging(question, hexToRender, changing, selectedMode);
								} 
							}, 
							IChingActivityRenderer.this
						);
					}

					public void onNothingSelected(AdapterView<?> arg0) {
						etOutput.setText("");
					}
				});

				break;
		}
		
		etChanging.setText(Html.fromHtml("<small>" + desc + "</small>"));
		
		renderEditDesc(mode);
	}

	/**
	 * Renders a tab of the readDesc layout, given the associated hexagram
	 * 
	 * @param question The posed question
	 * @param hexToRender The hexagram to evaluate for changing lines
	 * @param changing The changing line index
	 * @param mode The mode used to display the view
	 */
	protected void renderReadDesc(final String question, final int[] hexToRender, final int changing, final READ_DESC_MODE mode) {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		final TextView tvDescTitle = (TextView) findViewById(R.id.tvHexName);
		currentHex = Utils.hexMap(hexToRender);
		tvDescTitle.setText(Utils.getResourceByName(R.string.class, "hex" + currentHex));

		LinearLayout layButtonsAndChanging = (LinearLayout) findViewById(R.id.layButtonsAndChanging);
		for (int i = 0; i < layButtonsAndChanging.getChildCount(); i++) {
			layButtonsAndChanging.getChildAt(i).setVisibility(View.GONE);
		}
		
		for (int i = 0; i < 6; i++) {
			renderRow(i, hexToRender[i], false);
		} 
		
		final TextView tvQuestion = (TextView) findViewById(R.id.tvQuestionReadDesc);
		if (question != null && !question.equals("")) {
			tvQuestion.setText(question);
		} else {
			tvQuestion.setVisibility(View.GONE);
		}
		
		final EditText etOutput = (EditText) findViewById(R.id.etOutput);
		final Button btReadDesc = (Button) findViewById(R.id.btReadDesc);
		btReadDesc.setVisibility(View.VISIBLE);
		final Button btReadJudge = (Button) findViewById(R.id.btReadJudge);
		btReadJudge.setVisibility(View.VISIBLE);
		final Button btImage = (Button) findViewById(R.id.btReadImage);
		btImage.setVisibility(View.VISIBLE);
		
		OnTouchListener onTouchListener = new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				currentSection = RemoteResolver.ICHING_REMOTE_SECTION_DESC;
				RemoteResolver.renderRemoteString(
					etOutput,
					new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							renderReadDesc(question, hexToRender, changing, mode);
						} 
					}, 
					IChingActivityRenderer.this
				);
				btReadDesc.setPressed(true);
				btReadJudge.setPressed(false);
				btImage.setPressed(false);
				return true;
			}
		};
		btReadDesc.setOnTouchListener(onTouchListener);
		onTouchListener.onTouch(btReadDesc, null);
		
		btReadJudge.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				currentSection = RemoteResolver.ICHING_REMOTE_SECTION_JUDGE;
				RemoteResolver.renderRemoteString(
					etOutput,
					new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							renderReadDesc(question, hexToRender, changing, mode);
						} 
					},
					IChingActivityRenderer.this
				);
				btReadDesc.setPressed(false);
				btReadJudge.setPressed(true);
				btImage.setPressed(false);
				return true;
			}
			
		});

		btImage.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				currentSection = RemoteResolver.ICHING_REMOTE_SECTION_IMAGE;
				RemoteResolver.renderRemoteString(
					etOutput,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							renderReadDesc(question, hexToRender, changing, mode);
						} 
					}, 
					IChingActivityRenderer.this
				);
				btReadDesc.setPressed(false);
				btReadJudge.setPressed(false);
				btImage.setPressed(true);
				return true;
			}
		});
		
		renderEditDesc(mode);
	}	
	
	/**
	 * If editing an exagram using the custom language set of definitions, enable the edit
	 * and reset hexagram sections buttons
	 * 
	 * @param mode The current reading mode
	 */
	private void renderEditDesc(READ_DESC_MODE mode) {
		final String dictionary = (String) getSettingsManager().get(SETTINGS_MAP.DICTIONARY);
		if (mode == READ_DESC_MODE.VIEW_HEX && dictionary.equals(Consts.DICTIONARY_CUSTOM)) {
			final Button btEdit = (Button) findViewById(R.id.btReadDescEdit);
			final Button btReset = (Button) findViewById(R.id.btReadDescReset);
			btEdit.setVisibility(View.VISIBLE);
			btReset.setVisibility(View.VISIBLE);
		}
	}

	/** 
	 * Called when the activity is first created.
	 * 
	 * @param savedInstanceState The saved state 
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState); 
		settings = new SettingsManager();
		dsHexSection = new HexSectionDataSource(getApplicationContext());
	}

	@Override
	protected void onResume() {
		dsHexSection.open();
		super.onResume();
	}

	@Override
	protected void onPause() {
		dsHexSection.close();
		super.onPause();
	}
	
	/**
	 * onClick handler to edit the text content of a section of an Hexagram
	 * 
	 * @param view Not used
	 */
	public void onClickEditHexSection(View view) {
		LayoutInflater li = LayoutInflater.from(this);
		View editDescView = li.inflate(R.layout.editdesc, null);

		AlertDialog.Builder editDescDialogBuilder = new AlertDialog.Builder(this);
		editDescDialogBuilder.setView(editDescView);
		
		editDescDialog = editDescDialogBuilder.show();
		
		final TextView tvEditSecHex = (TextView) editDescView.findViewById(R.id.tvEditSecHex);
		String title = Utils.s(Utils.getResourceByName(R.string.class, "hex" + currentHex));
		if (currentSection.startsWith(RemoteResolver.ICHING_REMOTE_SECTION_LINE)) {
			title += " - " + Utils.s(Utils.getResourceByName(R.string.class, "read_changing_select_" + currentSection));
		} else {
			title += " - " + Utils.s(Utils.getResourceByName(R.string.class, "read_" + currentSection));
		}
		tvEditSecHex.setText(title);
		
		String dictionary = (String) settings.get(SETTINGS_MAP.DICTIONARY);
		String lang = (String) settings.get(SETTINGS_MAP.LANGUAGE);
		HexSection section = new HexSection("", "",  "", lang, "");
		try {
			section = dsHexSection.getHexSection(currentHex, dictionary, lang, currentSection);
		} catch (NotFoundException e) {}
		
		final EditText etQuote = (EditText) editDescView.findViewById(R.id.etQuote);
		etQuote.setText(section.getDefQuote());
		final EditText etReading = (EditText) editDescView.findViewById(R.id.etReading);
		etReading.setText(section.getDefReading());
	}
	
	/**
	 * onClick handler to reset the text content of a section of an Hexagram
	 * 
	 * @param view Not used
	 */
	public void onClickResetHexSection(View view) {
		AlertDialog resetConfirmDialog = new AlertDialog.Builder(this).create();
		resetConfirmDialog.setMessage(Utils.s(R.string.hex_reset_section));
		resetConfirmDialog.setButton(DialogInterface.BUTTON_POSITIVE, Utils.s(R.string.yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				RemoteResolver.resetCache(currentHex, IChingActivityRenderer.this);
				EditText etOutput = (EditText) findViewById(R.id.etOutput);
				etOutput.setText("");

				CharSequence text = Utils.s(
					R.string.edit_section_reset, 
					new String[] { 
						Utils.s(Utils.getResourceByName(R.string.class, "hex" + currentHex)) 
					}
				);
				
				Toast toast = Toast.makeText(
					getApplicationContext(), 
					text, 
					Toast.LENGTH_SHORT
				);
				toast.show();
			} 
		});
		resetConfirmDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.no), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {} 
		});
		resetConfirmDialog.show();
	}
	
	/**
	 * onClick handler to update the text content of a section of an Hexagram
	 * 
	 * @param view Not used
	 */
	public void onClickUpdateHexSection(View view) {
		
		final TextView tvEditSecHex = (TextView) editDescDialog.findViewById(R.id.tvEditSecHex);
		final EditText etQuote = (EditText) editDescDialog.findViewById(R.id.etQuote);
		final EditText etReading = (EditText) editDescDialog.findViewById(R.id.etReading);
		CharSequence text = Utils.s(
			R.string.edit_section_update, 
			new String[] { tvEditSecHex.getText().toString() }
		);
		
		String def;
		if (!etQuote.getText().toString().equals("")) {
			def = etQuote.getText() + Utils.HEX_SECTION_QUOTE_DELIMITER + Utils.NEWLINE + etReading.getText();
		} else {
			def = etReading.getText().toString();
		}
			
		String dictionary = (String) settings.get(SETTINGS_MAP.DICTIONARY);
		String lang = (String) settings.get(SETTINGS_MAP.LANGUAGE);
		
		RemoteResolver.resetCache(currentHex, currentSection, this);
		dsHexSection.updateHexSection(currentHex, dictionary, lang, currentSection, def);
		
		EditText etOutput = (EditText) findViewById(R.id.etOutput);
		etOutput.setText(RemoteResolver.getSpannedFromRemoteString(def));
		
		Toast toast = Toast.makeText(
			getApplicationContext(), 
			text, 
			Toast.LENGTH_SHORT
		);
		toast.show();
		
		editDescDialog.dismiss();

	}

	/** 
	 * @return The hex section data source currently in use
	 */
	public HexSectionDataSource getHexSectionDataSource() {
		return dsHexSection;
	}
	
	/**
	 * @return The settings manager currently in use
	 */
	public SettingsManager getSettingsManager() {
		return settings;
	}
	
	/**
	 * @return The currently selected section or changing line
	 */
	public String getCurrentSection() {
		return currentSection;
	}
	
	/**
	 * @return The currently selected hexagram
	 */
	public String getCurrentHex() {
		return currentHex;
	}
}