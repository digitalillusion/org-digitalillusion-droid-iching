package org.digitalillusion.droid.iching;

import java.util.ArrayList;
import java.util.Arrays;

import org.digitalillusion.droid.changinglines.ChangingLinesEvaluator;
import org.digitalillusion.droid.utils.Consts;
import org.digitalillusion.droid.utils.RemoteResolver;
import org.digitalillusion.droid.utils.Utils;


import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ActivityInfo;
import android.text.Html;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

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
	
	/** Changing lines section identifier **/
	private static final String ICHING_REMOTE_SECTION_LINE 	= "line";

	/** SORTED Subset of the hexagrams set when all lines changing have a particular meaning **/
	private static final Integer[] ICHING_ALL_LINES_DESC = new Integer[] { 1, 2, 12, 47 };
	
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
	 * @param mode The mode used to display changing lines
	 */
	protected void renderReadDescChanging(final String question, final int[] hexToRender, final int changing, READ_DESC_MODE mode) {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

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
		final String hexMap = Utils.hexMap(hexToRender);
		
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
						int intMap = Integer.parseInt(hexMap);
						for (int allLines : ICHING_ALL_LINES_DESC) {
							if (intMap == allLines) {
								RemoteResolver.renderRemoteString(etOutput, hexMap, ICHING_REMOTE_SECTION_LINE + changing, retryAction, this);
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
						RemoteResolver.renderRemoteString(etOutput, hexMap, ICHING_REMOTE_SECTION_LINE + (changing + 1), retryAction, this);
				}
				break;
			case VIEW_HEX :
				desc = Utils.s(R.string.read_changing_select) + "<br/>";
				
				ArrayList<String> lines = new ArrayList<String>();
				lines.add(Utils.s(R.string.read_changing_select_1));
				lines.add(Utils.s(R.string.read_changing_select_2));
				lines.add(Utils.s(R.string.read_changing_select_3));
				lines.add(Utils.s(R.string.read_changing_select_4));
				lines.add(Utils.s(R.string.read_changing_select_5));
				lines.add(Utils.s(R.string.read_changing_select_6));
				
				int hexId = Integer.parseInt(Utils.hexMap(hexToRender));
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
						RemoteResolver.renderRemoteString(
							etOutput, hexMap, ICHING_REMOTE_SECTION_LINE + position, 
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
	}

	/**
	 * Renders a tab of the readDesc layout, given the associated hexagram
	 * 
	 * @param hexToRender The hexagram to render and describe
	 */
	protected void renderReadDesc(final String question, final int[] hexToRender, final int changing) {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		LinearLayout layButtonsAndChanging = (LinearLayout) findViewById(R.id.layButtonsAndChanging);
		for (int i = 0; i < layButtonsAndChanging.getChildCount(); i++) {
			layButtonsAndChanging.getChildAt(i).setVisibility(View.GONE);
		}
		
		for (int i = 0; i < 6; i++) {
			renderRow(i, hexToRender[i], false);
		} 
		
		final String hexMap = Utils.hexMap(hexToRender);
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
				RemoteResolver.renderRemoteString(
					etOutput, hexMap, RemoteResolver.ICHING_REMOTE_SECTION_DESC, 
					new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							renderReadDesc(question, hexToRender, changing);
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
				RemoteResolver.renderRemoteString(
					etOutput, hexMap, RemoteResolver.ICHING_REMOTE_SECTION_JUDGE,
					new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							renderReadDesc(question, hexToRender, changing);
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
				RemoteResolver.renderRemoteString(
					etOutput, hexMap, RemoteResolver.ICHING_REMOTE_SECTION_IMAGE, 
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							renderReadDesc(question, hexToRender, changing);
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
	}	

}
