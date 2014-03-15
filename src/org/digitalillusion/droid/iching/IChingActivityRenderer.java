package org.digitalillusion.droid.iching;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.digitalillusion.droid.iching.changinglines.ChangingLinesEvaluator;
import org.digitalillusion.droid.iching.utils.Consts;
import org.digitalillusion.droid.iching.utils.DataPersister;
import org.digitalillusion.droid.iching.utils.RemoteResolver;
import org.digitalillusion.droid.iching.utils.SettingsManager;
import org.digitalillusion.droid.iching.utils.SettingsManager.SETTINGS_MAP;
import org.digitalillusion.droid.iching.utils.Utils;
import org.digitalillusion.droid.iching.utils.lists.ExpandableDropDownListItem2Adapter;
import org.digitalillusion.droid.iching.utils.lists.HistoryEntry;
import org.digitalillusion.droid.iching.utils.lists.ListItem2Adapter;
import org.digitalillusion.droid.iching.utils.sql.HexSection;
import org.digitalillusion.droid.iching.utils.sql.HexSectionDataSource;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Decorator that performs the rendering duties onto the views
 *
 * @author digitalillusion
 */
public class IChingActivityRenderer extends Activity {
	
	/** Data object representing the current state **/
	public static class CurrentState {
		/** The user question **/
		public String question;
		/** The changing line index **/
		public int changing;
		/** The currently selected section or changing line **/
		public String section;
		/** The currently selected hexagram **/
		public String hex;
		/** The currently selected consultation mode **/
		public READ_DESC_MODE mode;
		/** The currently selected tab index**/
		public int tabIndex;
		/** The changing line count **/
		public int changingCount;
		/** The changing line index in manual mode **/
		public int changingManualIndex;
		/** The current View **/
		public Integer viewId;	
		
		public CurrentState() {
			tabIndex = 0;
		}
	}

	/** The consultation mode, being using the oracle or reading the book **/
	protected enum READ_DESC_MODE {
		ORACLE,
		VIEW_HEX
	}
	
	/** SORTED Subset of the hexagrams set when all lines changing have a particular meaning **/
	private static final Integer[] ICHING_ALL_LINES_DESC = new Integer[] { 1, 2, 12, 47 };
	
	/** Settings manager**/
	protected SettingsManager settings;
	
	/** The local data source for the hexagrams sections strings **/
	protected HexSectionDataSource dsHexSection;

	/** The current state **/
	protected CurrentState current;

	/** The edit hexagram description dialog **/
	protected AlertDialog editDescDialog;
	
	/** The history creation dialog **/
	protected AlertDialog newHistoryDialog;
	
	/** The history password insertion dialog */
	protected AlertDialog passwordDialog;
	/** The history password insertion dialog on cancel listener*/
	protected OnCancelListener passwordDialogOnCancel;
	
	/** A dialog that allows to select an item from a list*/
	protected Dialog itemSelectDialog;
	
	/** The options menu **/
	protected Menu optionsMenu;
	
	/** Memory cache of the local history **/
	protected ArrayList<HistoryEntry> historyList = new ArrayList<HistoryEntry>();
	
	/** The cleanup operation after history password dialog has been cancelled */
	protected final Runnable DEFAULT_HISTORY_REVERT_TASK = new Runnable() {
		public void run() {
			if (DataPersister.revertSelectedHistory()) {
				renderLoadHistory(null, null);
			}
		}
	};
	
	/** The operation for the button that cancel history password dialog */ 
	protected final OnClickListener DEFAULT_HISTORY_REVERT_DIALOG_BUTTON = new OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			DEFAULT_HISTORY_REVERT_TASK.run();
		}
	};
	
	/**
	 * @return The currently selected hexagram
	 */
	public String getCurrentHex() {
		return current.hex;
	}
	
	/**
	 * @return The currently selected section or changing line
	 */
	public String getCurrentSection() {
		return current.section;
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
	 * onClick handler to show the popup of creation of a new history
	 * 
	 * @param view Not used
	 */
	public void onClickShowCreateHistory(View view) {
		LayoutInflater li = LayoutInflater.from(this);
		View editDescView = li.inflate(R.layout.newhistory, null);

		AlertDialog.Builder newHistoryDialogBuilder = new AlertDialog.Builder(this);
		newHistoryDialogBuilder.setView(editDescView);
		newHistoryDialogBuilder.setPositiveButton(R.string.create, new OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				final CheckBox cbHistoryPassword = (CheckBox) newHistoryDialog.findViewById(R.id.cbHistoryPassword);
				final EditText etHistoryName = (EditText) newHistoryDialog.findViewById(R.id.etHistoryName);
				final EditText etHistoryPassword = (EditText) newHistoryDialog.findViewById(R.id.etHistoryPassword);
				
				String historyName = etHistoryName.getText().toString();
				String historyPassword = Utils.EMPTY_STRING;
				
				if (cbHistoryPassword.isChecked()) {
					historyPassword = etHistoryPassword.getText().toString();
				}
				
				DataPersister.setSelectedHistory(historyName, historyPassword, true);
				// Avoid saving an empty file, otherwise it cannot be encrypted
				List<HistoryEntry> dummyList = new ArrayList<HistoryEntry>();
				dummyList.add(Utils.buildDummyHistoryEntry());
				newHistoryDialog.dismiss();
				
				if (DataPersister.saveHistory(dummyList, IChingActivityRenderer.this)) {
					CharSequence text = Utils.s(
						R.string.history_create_done, 
						new String[] { historyName }
					);
					
					showToast(text);
					
					// Request re-render
					renderLoadHistory(null, null);
				}
			}
		});
		
		newHistoryDialog = newHistoryDialogBuilder.show();
		
		final Button btHistoryCreate = newHistoryDialog.getButton(DialogInterface.BUTTON_POSITIVE);
		btHistoryCreate.setEnabled(false);
		final CheckBox cbHistoryPassword = (CheckBox) newHistoryDialog.findViewById(R.id.cbHistoryPassword);
		final EditText etHistoryName = (EditText) newHistoryDialog.findViewById(R.id.etHistoryName);
		final EditText etHistoryPassword = (EditText) newHistoryDialog.findViewById(R.id.etHistoryPassword);
		final EditText etHistoryPasswordVerify = (EditText) newHistoryDialog.findViewById(R.id.etHistoryPasswordVerify);
		
		etHistoryName.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				if (s.toString().isEmpty()) {
					etHistoryName.setError(Utils.s(R.string.validator_error_empty));
				} else {
					etHistoryName.setError(null);
				}

				showCreateHistoryValidation(btHistoryCreate, cbHistoryPassword,
					etHistoryName, etHistoryPassword,
					etHistoryPasswordVerify);
			}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
			public void onTextChanged(CharSequence s, int start, int before, int count) { }
		});
		etHistoryName.setError(Utils.s(R.string.validator_error_empty));
		
		etHistoryPassword.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				if (s.toString().isEmpty()) {
					etHistoryPassword.setError(Utils.s(R.string.validator_error_empty));
					btHistoryCreate.setEnabled(false);
				} else if (!s.toString().matches("[A-Za-z0-9]+")) {
					etHistoryPassword.setError(Utils.s(R.string.validator_error_non_alphanumeric));
						btHistoryCreate.setEnabled(false);
				}
				
				if (!s.toString().equals(etHistoryPasswordVerify.getText().toString())) {
					etHistoryPasswordVerify.setError(Utils.s(R.string.validator_error_password_verify));
					btHistoryCreate.setEnabled(false);
				} else if (!etHistoryPasswordVerify.getText().toString().isEmpty()) {
					etHistoryPasswordVerify.setError(null);
				}
				
				showCreateHistoryValidation(btHistoryCreate, cbHistoryPassword,
					etHistoryName, etHistoryPassword,
					etHistoryPasswordVerify);
			}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
			public void onTextChanged(CharSequence s, int start, int before, int count) { }
		});
		
		etHistoryPasswordVerify.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				if (s.toString().isEmpty()) {
					etHistoryPasswordVerify.setError(Utils.s(R.string.validator_error_empty));
					btHistoryCreate.setEnabled(false);
				} else if (!s.toString().matches("[A-Za-z0-9]+")) {
					etHistoryPasswordVerify.setError(Utils.s(R.string.validator_error_non_alphanumeric));
						btHistoryCreate.setEnabled(false);
				}
				
				if (!s.toString().equals(etHistoryPassword.getText().toString())) {
					etHistoryPasswordVerify.setError(Utils.s(R.string.validator_error_password_verify));
					btHistoryCreate.setEnabled(false);
				} else if (!etHistoryPasswordVerify.getText().toString().isEmpty()) {
					etHistoryPasswordVerify.setError(null);
				}
				
				showCreateHistoryValidation(btHistoryCreate, cbHistoryPassword,
					etHistoryName, etHistoryPassword,
					etHistoryPasswordVerify);
			}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
			public void onTextChanged(CharSequence s, int start, int before, int count) { }
		});
		
		
		cbHistoryPassword.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				final LinearLayout lHistoryPassword = (LinearLayout) newHistoryDialog.findViewById(R.id.layHistoryPasswordSection);
				if (cbHistoryPassword.isChecked()) {
					lHistoryPassword.setVisibility(View.VISIBLE);
					etHistoryPassword.setText(Utils.EMPTY_STRING);
					etHistoryPasswordVerify.setText(Utils.EMPTY_STRING);
					etHistoryPassword.setError(Utils.s(R.string.validator_error_empty));
					etHistoryPasswordVerify.setError(Utils.s(R.string.validator_error_empty));
					btHistoryCreate.setEnabled(false);
				} else {
					lHistoryPassword.setVisibility(View.GONE);
					etHistoryPassword.setText(Utils.EMPTY_STRING);
					etHistoryPasswordVerify.setText(Utils.EMPTY_STRING);
					etHistoryPasswordVerify.setError(null);
					etHistoryPassword.setError(null);
					// Trigger text change
					etHistoryName.setText(etHistoryName.getText().toString());
				}
			}
		});
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
		current = new CurrentState();
	}
	
	/** 
	 * Called when the activity is suspended.
	 * 
	 * @param savedInstanceState The saved state 
	 */
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		if (passwordDialog != null && passwordDialog.isShowing()) { 
			// Immediate call to revert potentially undergoing operations in popups and prevent data loss
			passwordDialogOnCancel.onCancel(passwordDialog);
		}
		
		super.onSaveInstanceState(savedInstanceState);
	}
	
	/**
	 * Render the page where to edit the text content of a section of an Hexagram
	 */
	protected void renderEditHexSection() {
		LayoutInflater li = LayoutInflater.from(this);
		View editDescView = li.inflate(R.layout.editdesc, null);

		AlertDialog.Builder editDescDialogBuilder = new AlertDialog.Builder(this);
		editDescDialogBuilder.setView(editDescView);
		editDescDialogBuilder.setPositiveButton(R.string.update, new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				final TextView tvEditSecHex = (TextView) editDescDialog.findViewById(R.id.tvEditSecHex);
				final EditText etQuote = (EditText) editDescDialog.findViewById(R.id.etQuote);
				final EditText etReading = (EditText) editDescDialog.findViewById(R.id.etReading);
				CharSequence text = Utils.s(
					R.string.edit_section_update, 
					new String[] { tvEditSecHex.getText().toString() }
				);
				
				String def;
				if (!etQuote.getText().toString().isEmpty()) {
					def = etQuote.getText() + Utils.HEX_SECTION_QUOTE_DELIMITER + Utils.NEWLINE + etReading.getText();
				} else {
					def = etReading.getText().toString();
				}
					
				String dictionary = (String) settings.get(SETTINGS_MAP.DICTIONARY);
				String lang = (String) settings.get(SETTINGS_MAP.LANGUAGE);
				
				RemoteResolver.resetCache(current.hex, current.section, IChingActivityRenderer.this);
				dsHexSection.updateHexSection(current.hex, dictionary, lang, current.section, def);
				
				EditText etOutput = (EditText) findViewById(R.id.etOutput);
				etOutput.setText(RemoteResolver.getSpannedFromRemoteString(def));
				
				showToast(text);
				
				editDescDialog.dismiss();

			}
		});
		
		editDescDialog = editDescDialogBuilder.show();
		
		final TextView tvEditSecHex = (TextView) editDescView.findViewById(R.id.tvEditSecHex);
		String title = Utils.s(Utils.getResourceByName(R.string.class, "hex" + current.hex));
		if (current.section.startsWith(RemoteResolver.ICHING_REMOTE_SECTION_LINE)) {
			title += " - " + Utils.s(Utils.getResourceByName(R.string.class, "read_changing_select_" + current.section));
		} else {
			title += " - " + Utils.s(Utils.getResourceByName(R.string.class, "read_" + current.section));
		}
		tvEditSecHex.setText(title);
		
		String dictionary = (String) settings.get(SETTINGS_MAP.DICTIONARY);
		String lang = (String) settings.get(SETTINGS_MAP.LANGUAGE);
		HexSection section = new HexSection(Utils.EMPTY_STRING, Utils.EMPTY_STRING,  Utils.EMPTY_STRING, lang, Utils.EMPTY_STRING);
		try {
			section = dsHexSection.getHexSection(current.hex, dictionary, lang, current.section);
		} catch (NotFoundException e) {}
		
		final EditText etQuote = (EditText) editDescView.findViewById(R.id.etQuote);
		etQuote.setText(section.getDefQuote());
		final EditText etReading = (EditText) editDescView.findViewById(R.id.etReading);
		etReading.setText(section.getDefReading());
	}
	
	private String getChangingLinesDescription(READ_DESC_MODE mode) {
		String desc = Utils.EMPTY_STRING;
		
		switch (mode) {
			case VIEW_HEX :
				desc = Utils.s(R.string.read_changing_select) + "<br/>";
				break;
			case ORACLE :
				if (current.changingCount == 0) {
					desc = Utils.s(R.string.read_changing_none) + "<br/>";
				} else {
					int resId = current.changingCount == 1 ? R.string.read_changing_one : R.string.read_changing_count;
					desc = Utils.s(resId, new Integer[] { current.changingCount }) + Utils.COLUMNS + "<br/>";
				}
				
				
				desc += getChangingLinesDescriptionApply();
				break;
		}
		return desc;
	}
	
	private String getChangingLinesDescriptionApply() {
		String desc = Utils.EMPTY_STRING;
		switch (current.changing) {
			case ChangingLinesEvaluator.ICHING_APPLY_BOTH :
				desc += "<em>" + Utils.s(R.string.read_changing_apply_ht) + "</em>";
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
				desc += "<em>" + Utils.s(R.string.read_changing_apply, new Integer[] { current.changing + 1 }) + "</em>";
		}
		return desc;
	}
	
	private void prepareReadingDescription(final EditText etOutput,
			OnClickListener retryAction) {
		if (current.changing == ChangingLinesEvaluator.ICHING_APPLY_BOTH) {
			int intMap = Integer.parseInt(current.hex);
			for (int allLines : ICHING_ALL_LINES_DESC) {
				if (intMap == allLines) {
					RemoteResolver.renderRemoteString(etOutput, retryAction, this);
					break;
				}
			}
		} else if (current.changing != ChangingLinesEvaluator.ICHING_APPLY_CAST &&
				   current.changing != ChangingLinesEvaluator.ICHING_APPLY_TRANSFORMED &&
				   current.changing != ChangingLinesEvaluator.ICHING_APPLY_NONE) {
			RemoteResolver.renderRemoteString(etOutput, retryAction, this);
		}
	}

	private void promptForHistoryPassword(final ArrayList<HistoryEntry> historyList, final Runnable successTask, final Runnable failureTask) {
		if (passwordDialog == null || !passwordDialog.isShowing()) {
			passwordDialog = new AlertDialog.Builder(IChingActivityRenderer.this).create();
			final EditText input = new EditText(IChingActivityRenderer.this);
			input.setInputType(InputType.TYPE_CLASS_TEXT| InputType.TYPE_TEXT_VARIATION_PASSWORD);
			input.setHint(R.string.password_required);
			passwordDialog.setView(input);
			passwordDialog.setTitle(DataPersister.getSelectedHistoryName());
			passwordDialog.setButton(DialogInterface.BUTTON_POSITIVE, Utils.s(android.R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					DataPersister.setSelectedHistory(DataPersister.getSelectedHistoryName(), input.getText().toString(), true);
					renderLoadHistory(successTask, new Runnable() {
						public void run() {
							CharSequence text = Utils.s(R.string.history_password_invalid);
							showToast(text);
							
							// Run failure task if any
							if (failureTask != null) {
								failureTask.run();
							}
						}
					});
				} 
			});
			passwordDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				} 
			});
			passwordDialogOnCancel = new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					// Run failure task if any
					if (failureTask != null) {
						failureTask.run();
					}
				}
			};
			passwordDialog.setOnCancelListener(passwordDialogOnCancel);
			passwordDialog.show();
		} else {
			// Run failure task if any
			if (failureTask != null) {
				failureTask.run();
			}
		}
	}

	/**
	 * Render the action buttons
	 * 
	 * If editing an hexagram using the custom language set of definitions, enable the edit
	 * and reset hexagram sections action buttons
	 * Else, while viewing a reading, show action button to share content
	 */
	protected void renderOptionsMenu() {
		if (optionsMenu != null) {
			final MenuItem omEdit = optionsMenu.findItem(R.id.omReadDescEdit);
			final MenuItem omUndo = optionsMenu.findItem(R.id.omReadDescUndo);
			final MenuItem omShare = optionsMenu.findItem(R.id.omReadDescShare);
			
			final String dictionary = (String) getSettingsManager().get(SETTINGS_MAP.DICTIONARY);
			if (current.viewId == R.layout.readdesc || current.viewId == R.layout.editdesc) {
				if (current.mode == READ_DESC_MODE.VIEW_HEX && dictionary.equals(Consts.DICTIONARY_CUSTOM)) {
					omEdit.setVisible(true);
					omUndo.setVisible(true);
					omShare.setVisible(true);
				} else {
					omEdit.setVisible(false);
					omUndo.setVisible(false);
					omShare.setVisible(true);
				}
			} else {
				omEdit.setVisible(false);
				omUndo.setVisible(false);
				omShare.setVisible(false);
			}
		}
	}

	private void renderQuestion() {
		final TextView tvQuestion = (TextView) findViewById(R.id.tvQuestionReadDesc);
		if (current.question != null && !current.question.isEmpty()) {
			tvQuestion.setText(current.question);
		} else {
			tvQuestion.setVisibility(View.GONE);
		}
	}
	
	private void showCreateHistoryValidation(final Button btHistoryCreate,
			final CheckBox cbHistoryPassword, final EditText etHistoryName,
			final EditText etHistoryPassword,
			final EditText etHistoryPasswordVerify) {
		// Emptiness check cannot rely on hasError() because backspace removes error from fields
		if (!etHistoryName.getText().toString().isEmpty() && etHistoryName.getError() == null &&
			(!cbHistoryPassword.isChecked() ||
			 !etHistoryPassword.getText().toString().isEmpty() && etHistoryPassword.getError() == null &&
			 !etHistoryPasswordVerify.getText().toString().isEmpty() && etHistoryPasswordVerify.getError() == null)
			) {
			btHistoryCreate.setEnabled(true);
		} else {
			btHistoryCreate.setEnabled(false);
		}
	}

	protected Dialog buildItemSelectionDialog(CharSequence[] items, String title, OnClickListener onClick) {
		if (itemSelectDialog == null || !itemSelectDialog.isShowing()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(title);
			builder.setItems(items, onClick);
			itemSelectDialog = builder.create();
		}
		return itemSelectDialog;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		dsHexSection.close();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		dsHexSection.open();
	}
	
	protected void performShare() {
		Intent sharingIntent = new Intent(Intent.ACTION_SEND);
		sharingIntent.setType("text/html");
		
		final EditText fakeEditText = new EditText(this.getApplicationContext());
		final OnClickListener retryAction = new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				performShare();
			} 
		};
				
		String reading = null;
		switch (current.tabIndex) {
		case 0 :
			reading = Utils.s(R.string.read_cast);
			RemoteResolver.renderRemoteString(
				fakeEditText,
				retryAction, 
				IChingActivityRenderer.this
			);
			break;
		case 1 :
			reading = Utils.s(R.string.read_changing);
			prepareReadingDescription(fakeEditText, retryAction);
			break;
		case 2 :
			reading = Utils.s(R.string.read_transformed);
			RemoteResolver.renderRemoteString(
					fakeEditText,
					retryAction, 
					IChingActivityRenderer.this
				);
			break;
		}
		if (current.mode == READ_DESC_MODE.ORACLE) {
			reading += Utils.COLUMNS + "<br/>";
		} else {
			reading = Utils.EMPTY_STRING;
		}
		
		// Question
		final String title ="<h1>" + current.question + "</h1>";
		
		// Hexagram
		final String hexagram = "<h3>" + reading +
			Utils.s(Utils.getResourceByName(R.string.class, "hex" + current.hex)) + 
			"</h3>";
		
		// Section
		String changingText = Utils.EMPTY_STRING;
		if (current.section.startsWith(RemoteResolver.ICHING_REMOTE_SECTION_LINE)) {
			if (current.mode == READ_DESC_MODE.ORACLE) {
				changingText = getChangingLinesDescription(current.mode);
			} else {
				changingText = getChangingLinesDescriptionApply();
			}
		} else if (current.section.equals(RemoteResolver.ICHING_REMOTE_SECTION_DESC)) { 
			final Button button = (Button) findViewById(R.id.btReadDesc);
			changingText = button.getText().toString();
		} else if (current.section.equals(RemoteResolver.ICHING_REMOTE_SECTION_JUDGE)) {
			final Button button = (Button) findViewById(R.id.btReadJudge);
			changingText = button.getText().toString();
		} else if (current.section.equals(RemoteResolver.ICHING_REMOTE_SECTION_IMAGE)) {
			final Button button = (Button) findViewById(R.id.btReadImage);
			changingText = button.getText().toString();
		}
		final String section = "<strong>" + changingText + "</strong>";
		
		// Content
		final String content = "<p>" + Html.toHtml(fakeEditText.getText()) + "</p>";
		
		final String shareContent = title + hexagram + section + content;
		sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, Html.fromHtml(shareContent));
		startActivity(Intent.createChooser(sharingIntent,Utils.s(R.string.read_share_using)));
	}
	
	protected void renderLoadHistory(final Runnable successTask, final Runnable failureTask) {
		final ListView lvHistory = (ListView) findViewById(R.id.lvHistory);
		final TextView tvHistory = (TextView) findViewById(R.id.tvHistory);
		final EditText etQuestion = (EditText) findViewById(R.id.etQuestion);
		final ExpandableListView elSelectHistory = (ExpandableListView) findViewById(R.id.elSelectHistory);
		try {
			List<String> historyNames = DataPersister.getHistoryNames();

			// Render list of histories
			elSelectHistory.setAdapter(new ExpandableDropDownListItem2Adapter<String>(this, elSelectHistory, historyNames) {
    			@Override
    			public void childViewInit(TextView childView) {
    				childView.setId(R.id.elSelectHistory);
    				registerForContextMenu(childView);
    				
    				childView.setOnClickListener(new View.OnClickListener() {
						@SuppressWarnings("unchecked")
						ExpandableDropDownListItem2Adapter<String> expandibleAdapter = ((ExpandableDropDownListItem2Adapter<String>)elSelectHistory.getExpandableListAdapter());
    					final List<String> historyNamesList = expandibleAdapter.getList();
    						
						public void onClick(View v) {
							final int childPosition = historyNamesList.indexOf(((TextView)v).getText().toString());
							if (childPosition == -1) {
								onClickShowCreateHistory(v);
							} else {
								DataPersister.setSelectedHistory(historyNamesList.get(childPosition), Utils.EMPTY_STRING, true);
								
								renderLoadHistory(
									new Runnable() {
										public void run() {
											String selected = historyNamesList.remove(childPosition);
											historyNamesList.add(0, selected);
											elSelectHistory.collapseGroup(0);
											etQuestion.requestFocus();
											
											renderLoadHistory(successTask, null);
										}
									},
									DEFAULT_HISTORY_REVERT_TASK
								);
							}
						}
					});
    			}
    			@Override
    			public String getText1(int groupPosition, int childPosition,
    					String entry) {
    				if (childPosition == 0) {
    					return Utils.s(R.string.history_create);
    				}
    				return entry;
    			}
    			@Override
    			public String getText2(int groupPosition, int childPosition,
    					String entry) {
    				return Utils.s(R.string.history_change);
    			}
    		});
			BaseAdapter listAdapter = (BaseAdapter) elSelectHistory.getAdapter();
			listAdapter.notifyDataSetChanged();
			
			elSelectHistory.setOnGroupExpandListener(new OnGroupExpandListener() {
			    public void onGroupExpand(int groupPosition) {
			        lvHistory.setVisibility(View.GONE);
			        tvHistory.setVisibility(View.GONE);
			        elSelectHistory.requestFocus();
			    }
			});
			elSelectHistory.setOnGroupCollapseListener(new OnGroupCollapseListener() {
			    public void onGroupCollapse(int groupPosition) {
			    	if (historyList.size() > 0) {
				    	lvHistory.setVisibility(View.VISIBLE);
				    	tvHistory.setVisibility(View.VISIBLE);
			    	}
			    	elSelectHistory.requestFocus();
			    }
			});
    		
			// Manage the load of history and all it's exceptions
			DataPersister.loadHistory(historyList);
			
			// Render list of readings of the selected history
    		lvHistory.setAdapter(new ListItem2Adapter<HistoryEntry>(this, historyList) {
				@Override
				public String getText1(HistoryEntry entry) {
					return entry.getQuestion();
				}
				@Override
				public String getText2(HistoryEntry entry) {
					String template = "yyyy/MM/dd HH:mm:ss";
					SimpleDateFormat dateFormat = new SimpleDateFormat(template, settings.getLocale());
					return dateFormat.format(entry.getDate());
				}
    		});
    		if (historyList.size() > 0) {
	    		tvHistory.setVisibility(View.VISIBLE);
	    		lvHistory.setVisibility(View.VISIBLE);
		    	lvHistory.requestFocus();
		    	registerForContextMenu(lvHistory);
    		} else {
    			tvHistory.setVisibility(View.GONE);
    		}
			
			// Run success task if any
			if (successTask != null) {
				successTask.run();
			}
			return;
    	} catch (FileNotFoundException e) {
    		// Run failure task if any
    		if (failureTask != null) {
    			failureTask.run();
    		}
    		
    		tvHistory.setVisibility(View.GONE);
			etQuestion.requestFocus();
    	} catch (IOException e) {
    		// Run failure task if any
    		if (failureTask != null) {
    			failureTask.run();
    		}
    		
    		tvHistory.setVisibility(View.GONE);
    		AlertDialog alertDialog = new AlertDialog.Builder(IChingActivityRenderer.this).create();
			alertDialog.setMessage(Utils.s(R.string.history_unavailable));
			alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, Utils.s(android.R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {} 
			});
			alertDialog.show();
    	} catch (GeneralSecurityException e) {
    		promptForHistoryPassword(historyList, successTask, failureTask);
    		return;
		}
	}
	
	/**
	 * Renders a tab of the readDesc layout, given the associated hexagram
	 * 
	 * @param hexToRender The hexagram to evaluate for changing lines
	 */
	protected void renderReadDesc(final int[] hexToRender) {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		final TextView tvDescTitle = (TextView) findViewById(R.id.tvHexName);
		setCurrentHex(hexToRender);
		tvDescTitle.setText(Utils.getResourceByName(R.string.class, "hex" + current.hex));

		LinearLayout layButtonsAndChanging = (LinearLayout) findViewById(R.id.layButtonsAndChanging);
		for (int i = 0; i < layButtonsAndChanging.getChildCount(); i++) {
			layButtonsAndChanging.getChildAt(i).setVisibility(View.GONE);
		}
		
		for (int i = 0; i < 6; i++) {
			renderRow(i, hexToRender[i], false);
		} 
		
		renderQuestion();
		
		final EditText etOutput = (EditText) findViewById(R.id.etOutput);
		final Button btReadDesc = (Button) findViewById(R.id.btReadDesc);
		btReadDesc.setVisibility(View.VISIBLE);
		final Button btReadJudge = (Button) findViewById(R.id.btReadJudge);
		btReadJudge.setVisibility(View.VISIBLE);
		final Button btReadImage = (Button) findViewById(R.id.btReadImage);
		btReadImage.setVisibility(View.VISIBLE);
		
		final OnTouchListener lisReadDesc = new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				IChingActivityRenderer.this.setCurrentSection(RemoteResolver.ICHING_REMOTE_SECTION_DESC);
				RemoteResolver.renderRemoteString(
					etOutput,
					new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							renderReadDesc(hexToRender);
						} 
					}, 
					IChingActivityRenderer.this
				);
				btReadDesc.setPressed(true);
				btReadJudge.setPressed(false);
				btReadImage.setPressed(false);
				return true;
			}
		};
		btReadDesc.setOnTouchListener(lisReadDesc);
		
		final OnTouchListener lisReadJudge = new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				IChingActivityRenderer.this.setCurrentSection(RemoteResolver.ICHING_REMOTE_SECTION_JUDGE);
				RemoteResolver.renderRemoteString(
					etOutput,
					new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							renderReadDesc(hexToRender);
						} 
					},
					IChingActivityRenderer.this
				);
				btReadDesc.setPressed(false);
				btReadJudge.setPressed(true);
				btReadImage.setPressed(false);
				return true;
			}
			
		};
		btReadJudge.setOnTouchListener(lisReadJudge);

		OnTouchListener lisReadImage = new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				IChingActivityRenderer.this.setCurrentSection(RemoteResolver.ICHING_REMOTE_SECTION_IMAGE);
				RemoteResolver.renderRemoteString(
					etOutput,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							renderReadDesc(hexToRender);
						} 
					}, 
					IChingActivityRenderer.this
				);
				btReadDesc.setPressed(false);
				btReadJudge.setPressed(false);
				btReadImage.setPressed(true);
				return true;
			}
		};
		btReadImage.setOnTouchListener(lisReadImage);
		
		// Actionate the selected section button
		if (RemoteResolver.ICHING_REMOTE_SECTION_JUDGE.equals(current.section)) {
			lisReadJudge.onTouch(btReadJudge, null);
		} else if (RemoteResolver.ICHING_REMOTE_SECTION_IMAGE.equals(current.section)) {
			lisReadImage.onTouch(btReadImage, null);
		} else {
			lisReadDesc.onTouch(btReadDesc, null);
		}
		
		renderOptionsMenu();
	}
	
	/**
	 * Renders the changing lines tab of the readDesc layout, given the associated hexagram
	 * 
	 * @param hexToRender The hexagram to evaluate for changing lines
	 */
	protected void renderReadDescChanging(final int[] hexToRender) {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		final TextView tvDescTitle = (TextView) findViewById(R.id.tvHexName);
		setCurrentHex(hexToRender);	
		tvDescTitle.setText(Utils.s(R.string.read_changing));
		
		LinearLayout layButtonsAndChanging = (LinearLayout) findViewById(R.id.layButtonsAndChanging);
		for (int i = 0; i < layButtonsAndChanging.getChildCount(); i++) {
			layButtonsAndChanging.getChildAt(i).setVisibility(View.GONE);
		}
		
		renderQuestion();
		
		// Update changing count and consultation mode before getting changing lines
		// description
		current.changingCount = 0;
		for (int i = 0; i < 6; i++) {
			if (ChangingLinesEvaluator.isChangingLine(hexToRender[i])) {
				current.changingCount++;
			}
			renderRow(i, hexToRender[i], true);
		}
		READ_DESC_MODE mode = current.mode;
		if (current.changing == ChangingLinesEvaluator.ICHING_APPLY_MANUAL) {
			// Force manual selection of changing lines
			mode = READ_DESC_MODE.VIEW_HEX;
		}
		

		final EditText etOutput = (EditText) findViewById(R.id.etOutput);		
		etOutput.setText(Utils.EMPTY_STRING);
		switch (mode) {
			case ORACLE :
				setCurrentSection(current.changing);
				OnClickListener retryAction = new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						renderReadDescChanging(hexToRender);
					}
				};
				prepareReadingDescription(etOutput, retryAction);
				break;
			case VIEW_HEX :
				
				ArrayList<String> lines = new ArrayList<String>();
				lines.add(Utils.s(R.string.read_changing_select_line1));
				lines.add(Utils.s(R.string.read_changing_select_line2));
				lines.add(Utils.s(R.string.read_changing_select_line3));
				lines.add(Utils.s(R.string.read_changing_select_line4));
				lines.add(Utils.s(R.string.read_changing_select_line5));
				lines.add(Utils.s(R.string.read_changing_select_line6));
				
				int hexId = Integer.parseInt(current.hex);
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
						current.changingManualIndex = (position + 1 > 6) ? ChangingLinesEvaluator.ICHING_APPLY_BOTH : position;
						setCurrentSection(current.changingManualIndex);
						RemoteResolver.renderRemoteString(
							etOutput, 
							new OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									renderReadDescChanging(hexToRender);
								} 
							}, 
							IChingActivityRenderer.this
						);
					}
	
					public void onNothingSelected(AdapterView<?> arg0) {
						etOutput.setText(Utils.EMPTY_STRING);
					}
				});
				if (current.changingManualIndex >= 0 && current.changingManualIndex < 6) {
					spinner.setSelection(current.changingManualIndex);
				}

				break;
		}
		
		final TextView tvChanging = (TextView) findViewById(R.id.tvChanging);
		tvChanging.setVisibility(View.VISIBLE);		
		tvChanging.setText(Html.fromHtml("<small>" + getChangingLinesDescription(mode) + "</small>"));
		
		renderOptionsMenu();
	}


	/**
	 * Render page to reset the text content of a section of an Hexagram
	 */
	protected void renderResetHexSection() {
		AlertDialog resetConfirmDialog = new AlertDialog.Builder(this).create();
		resetConfirmDialog.setMessage(Utils.s(R.string.hex_reset_section));
		resetConfirmDialog.setButton(DialogInterface.BUTTON_POSITIVE, Utils.s(R.string.yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				RemoteResolver.resetCache(current.hex, IChingActivityRenderer.this);
				EditText etOutput = (EditText) findViewById(R.id.etOutput);
				etOutput.setText(Utils.EMPTY_STRING);

				CharSequence text = Utils.s(
					R.string.edit_section_reset, 
					new String[] { 
						Utils.s(Utils.getResourceByName(R.string.class, "hex" + current.hex)) 
					}
				);
				
				showToast(text);
			} 
		});
		resetConfirmDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.no), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {} 
		});
		resetConfirmDialog.show();
	}
	
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
	
	protected void renderTabs(final TabHost tabHost) {
		// Restyle tabs
		TabWidget tabWidget = tabHost.getTabWidget();
		for(int i = 0; i < tabWidget.getChildCount();i++)
		{
			View child = tabWidget.getChildAt(i);
			TextView title = (TextView) child.findViewById(android.R.id.title);
			title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
			title.setText(title.getText().toString().toUpperCase(settings.getLocale()));

			child.getLayoutParams().height = 39;
			
			child.setPadding(3, 3, 3, 10);
		}
	}
	
	/**
	 * Setter for the currently selected hex
	 * 
	 * @param hex The currently selected hex
	 */
	protected void setCurrentHex(int[] hex) {
		current.hex = Utils.hexMap(hex);
	}

	/**
	 * Setter for the selected section or changing line
	 * 
	 * @param current.changing The section or changing line
	 */
	protected void setCurrentSection(Serializable section) {
		if (section.equals(ChangingLinesEvaluator.ICHING_APPLY_BOTH) ||
			section.equals(ChangingLinesEvaluator.ICHING_APPLY_CAST) ||
			section.equals(ChangingLinesEvaluator.ICHING_APPLY_MANUAL) ||
			section.equals(ChangingLinesEvaluator.ICHING_APPLY_NONE) ||
			section.equals(ChangingLinesEvaluator.ICHING_APPLY_TRANSFORMED)) {
			current.section = RemoteResolver.ICHING_REMOTE_SECTION_LINE + ((Integer) section);
		} else if (Utils.isNumeric(section)) {
			current.changing = (Integer) section;
			current.section = RemoteResolver.ICHING_REMOTE_SECTION_LINE + ((Integer) section + 1);
		} else {
			current.section = section.toString(); 
		}
	}
	
	protected void showToast(CharSequence text) {
		Toast toast = Toast.makeText(
				getApplicationContext(), 
				text, 
				Toast.LENGTH_SHORT
			);
		toast.show();
	}
}