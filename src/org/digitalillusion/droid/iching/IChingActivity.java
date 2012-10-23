package org.digitalillusion.droid.iching;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.digitalillusion.droid.iching.anim.AnimCoin;
import org.digitalillusion.droid.iching.changinglines.ChangingLinesEvaluator;
import org.digitalillusion.droid.iching.utils.Consts;
import org.digitalillusion.droid.iching.utils.DataPersister;
import org.digitalillusion.droid.iching.utils.ListItem2Adapter;
import org.digitalillusion.droid.iching.utils.RemoteResolver;
import org.digitalillusion.droid.iching.utils.SettingsManager.SETTINGS_MAP;
import org.digitalillusion.droid.iching.utils.Utils;
import org.digitalillusion.droid.iching.utils.lists.HistoryEntry;
import org.digitalillusion.droid.iching.utils.lists.SettingsEntry;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Html;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

/**
 * A complete I Ching oracle for Android OS
 * 
 * @author digitalillusion
 */
public class IChingActivity extends IChingActivityRenderer {
	
	/** Unique identifier for the "Delete entry" context menu voice **/
	private static final int CONTEXTMENU_HISTORY_DELETE_ENTRY = 1;
	
	/** Unique identifier for the "Delete history" context menu voice **/
	private static final int CONTEXTMENU_HISTORY_DELETE_ALL = 2;
		
	/** Memory cache of the local history **/
	private ArrayList<HistoryEntry> historyList = new ArrayList<HistoryEntry>();
	
	/** The user question **/
	protected String question;
	/** The currently generated hexagram row **/
	protected int hexRow;
	/** The currently generated hexagram **/
	protected int[] hex;
	/** The changing line index **/
	protected int changing;

	/** The hexagram transformed from the currently generated one **/
	protected int[] tHex;
	/** The current View **/
	private Integer currentViewId;	 
	/** Proxed changing lines evaluator **/
	private ChangingLinesEvaluator changingLinesEvaluator;
		
	/**
     * Move to the consult view
     */
	public void gotoConsult() {
		setContentView(R.layout.consult);
		TextView tvQuestionShow = (TextView) findViewById(R.id.tvQuestionConsult);
		tvQuestionShow.setText(question);
		
		for (int i = 0; i < hexRow; i++) {
			renderRow(i, hex[i], true);
		}
		
		if (hexRow < 6) {
			Resources res = getResources();
			new AnimCoin((ImageView) findViewById(R.id.picCoin01), res);
			new AnimCoin((ImageView) findViewById(R.id.picCoin02), res);
			new AnimCoin((ImageView) findViewById(R.id.picCoin03), res);
		} else {
			((ImageView) findViewById(R.id.picCoin01)).setVisibility(View.GONE);
			((ImageView) findViewById(R.id.picCoin02)).setVisibility(View.GONE);
			((ImageView) findViewById(R.id.picCoin03)).setVisibility(View.GONE);

			TextView instructions = (TextView) findViewById(R.id.tvInstructions);
			instructions.setText(Utils.getResourceByName(R.string.class, "hex" + Utils.hexMap(hex)));
			
			final Button btnReadDesc = new Button(getApplicationContext());
			final LinearLayout layout = (LinearLayout) findViewById(R.id.layCoins);
			btnReadDesc.setText(R.string.consult_read_desc);
			btnReadDesc.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						btnReadDesc.setVisibility(View.GONE);
						mode = READ_DESC_MODE.ORACLE;
						gotoReadDesc();
						
						// Save history entry
						HistoryEntry historyEntry = new HistoryEntry();
						historyEntry.setHex(hex);
						historyEntry.setChanging(changing);
						historyEntry.setTHex(tHex);
						historyEntry.setQuestion(question);
						historyEntry.setDate(new Date());
						
						historyList.add(0, historyEntry);
						DataPersister.saveHistory(historyList, IChingActivity.this);
						
						return true;
					}
					return false;
				}
			});
			layout.addView(btnReadDesc);	
		}
	}
	
	/**
     * Move to the main view
     */
	public void gotoMain() {
		setContentView(R.layout.main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		RemoteResolver.prepareRetryPopup();
		
		final EditText etQuestion = (EditText) findViewById(R.id.etQuestion);
        etQuestion.setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				question = ((EditText) v).getText().toString();
				return false;
			}
		});
        etQuestion.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				View btnQuestion = (View) findViewById(R.id.btnQuestion);
				if (!hasFocus && btnQuestion != null) {
					btnQuestion.performClick();
				}
			}
		});
		if (question != null) {
			etQuestion.setText(question);
		}
		
		TextView tvHistory = (TextView) findViewById(R.id.tvHistory);
		final ListView lvHistory = (ListView) findViewById(R.id.lvHistory);
    	try {
    		DataPersister.loadHistory(historyList);
    		lvHistory.setAdapter(new ListItem2Adapter<HistoryEntry>(this, historyList) {
				@Override
				public String getText1(HistoryEntry entry) {
					return entry.getQuestion();
				}
				@Override
				public String getText2(HistoryEntry entry) {
					SimpleDateFormat dateFormat = new SimpleDateFormat();
					dateFormat.applyPattern("yyyy/MM/dd HH:mm:ss");
					return dateFormat.format(entry.getDate());
				}
    		});
    		lvHistory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
    			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
    				HistoryEntry entry = (HistoryEntry) lvHistory.getItemAtPosition(position);
    				IChingActivity thiz = IChingActivity.this;
    				thiz.changing = entry.getChanging();
    				thiz.hex = entry.getHex();
    				thiz.tHex = entry.getTHex();
    				thiz.question = entry.getQuestion();
    				thiz.mode = READ_DESC_MODE.ORACLE;
    				gotoReadDesc();
    			}
    		});
    		if (historyList.size() > 0) {
	    		tvHistory.setVisibility(View.VISIBLE);
		    	lvHistory.requestFocus();
		    	registerForContextMenu(lvHistory);
    		} else {
    			tvHistory.setVisibility(View.GONE);
    		}
    	} catch (FileNotFoundException e) {
    		tvHistory.setVisibility(View.GONE);
			etQuestion.requestFocus();
    	} catch (IOException e) {
    		tvHistory.setVisibility(View.GONE);
    		AlertDialog alertDialog = new AlertDialog.Builder(IChingActivity.this).create();
			alertDialog.setMessage(Utils.s(R.string.history_unavailable));
			alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, Utils.s(android.R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {} 
			});
			alertDialog.show();
    	}
        
        hexRow = 0;
		hex = new int[6];
		tHex = new int[6];
	}
	
	/**
     * Move to the read description view
     * @param setup True to setup the hexagrams, false if they are already setup
     */
	public void gotoReadDesc() {
		
		if (changingLinesEvaluator == null) {
			Integer evalType = (Integer) settings.get(SETTINGS_MAP.CHANGING_LINES_EVALUATOR);
			changingLinesEvaluator = ChangingLinesEvaluator.produce(evalType);
		}
		changing = changingLinesEvaluator.evaluate(hex, tHex);
		
		setContentView(R.layout.readdesc);
		
		final TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
		tabHost.setup();

		tabHost.setOnTabChangedListener(new OnTabChangeListener() {
			public void onTabChanged(String tabId) {
				if (tabId.equals("tab_consult")) {
					renderReadDesc(question, hex, changing, mode);
				} else if (tabId.equals("tab_changing")) {
					renderReadDescChanging(question, hex, changing, mode);
				} else if (tabId.equals("tab_future")) {
					renderReadDesc(question, tHex, changing, mode);
				}
			}
        });

		switch (mode) {
			case ORACLE : 
				if (changing == ChangingLinesEvaluator.ICHING_APPLY_CAST) {
					setupTab(tabHost, "tab_consult", R.string.read_cast, R.id.layReadDesc);
					setupTab(tabHost, "tab_changing", R.string.read_changing, R.id.layReadDesc);
					renderReadDesc(question, hex, changing, mode);
				} else {
					setupTab(tabHost, "tab_consult", R.string.read_cast, R.id.layReadDesc);
					setupTab(tabHost, "tab_changing", R.string.read_changing, R.id.layReadDesc);
					setupTab(tabHost, "tab_future", R.string.read_transformed, R.id.layReadDesc);
					renderReadDesc(question, hex, changing, mode);
				}
				break;
			case VIEW_HEX :
				setupTab(tabHost, "tab_consult", R.string.read_cast, R.id.layReadDesc);
				setupTab(tabHost, "tab_changing", R.string.read_changing, R.id.layReadDesc);
				renderReadDesc(question, hex, changing, mode);
				break;
		}
		
		// Display current tab
		tabHost.getCurrentView().setVisibility(View.VISIBLE);
		final TextView tvDescTitle = (TextView) findViewById(R.id.tvHexName);
		String hexMap = Utils.hexMap(hex);
		tvDescTitle.setText(Utils.getResourceByName(R.string.class, "hex" + hexMap));
		
		TabWidget tabWidget = tabHost.getTabWidget();
		for(int i = 0; i < tabWidget.getChildCount();i++)
		{
			View child = tabWidget.getChildAt(i);
			child.getLayoutParams().height = 35;
			child.setPadding(0, 0, 0, 10);
		}
	}
	
	/**
     * Move to the settings view
     */
	public void gotoSettings() {
		setContentView(R.layout.settings);
		
		final ListView lvSettings = (ListView) findViewById(R.id.lvSettings);
		
		List<SettingsEntry<?>> settingsList = new ArrayList<SettingsEntry<?>>();
		
		// Vibration
		settings.createOption(
			settingsList,
			"settings_vibration", 
			new Integer[] {
				Consts.HAPTIC_FEEDBACK_OFF,
				Consts.HAPTIC_FEEDBACK_ON_THROW_COINS
			},
			SETTINGS_MAP.HAPTIC_FEEDBACK
		);
		// Changing lines
		settings.createOption(
			settingsList,
			"settings_chlines_evaluator", 
			new Integer[] {
				Consts.EVALUATOR_MANUAL,
				Consts.EVALUATOR_MASTERYIN,
				Consts.EVALUATOR_NAJING, 
			},
			SETTINGS_MAP.CHANGING_LINES_EVALUATOR
		);
		// Language
		settings.createOption(
			settingsList,
			"settings_lang", 
			new String[] {
				Consts.LANGUAGE_EN
			},
			SETTINGS_MAP.LANGUAGE
		);
		// Dictionary
		settings.createOption(
			settingsList,
			"settings_dictionary", 
			new String[] {
				Consts.DICTIONARY_ALTERVISTA,
				Consts.DICTIONARY_CUSTOM,
			},
			SETTINGS_MAP.DICTIONARY
		);
		
		
		lvSettings.setAdapter(new ListItem2Adapter<SettingsEntry<?>>(this, settingsList) {
			@Override
			public String getText1(SettingsEntry<?> entry) {
				return Utils.s(Utils.getResourceByName(R.string.class, entry.getOptionName()));
			}

			@Override
			public String getText2(SettingsEntry<?> entry) {
				return Utils.s(Utils.getResourceByName(R.string.class, entry.getOptionName() + "_" + entry.getOptionValue()));
			}
		});
		
		lvSettings.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapter, View view, final int settingIndex, long id) {
				@SuppressWarnings("unchecked")
				final SettingsEntry<Serializable> entry = (SettingsEntry<Serializable>) lvSettings.getItemAtPosition(settingIndex);
				Spinner spinner = (Spinner) findViewById(R.id.spBacking);		
				String[] optionsText = new String[entry.getOptionValues().size()];
				int count = 0;
				for (Serializable value : entry.getOptionValues()) {
					optionsText[count++] = Utils.s(Utils.getResourceByName(R.string.class, entry.getOptionName() + "_" + value.toString()));
				}
				final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
					getApplicationContext(),  
					android.R.layout.simple_spinner_item,
					optionsText
				);
				arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				spinner.setAdapter(arrayAdapter);
				spinner.setSelection(entry.getOptionIndex());
				spinner.setPromptId(Utils.getResourceByName(R.string.class, entry.getOptionName()));
				spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
					public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
						SETTINGS_MAP mapKey = null;
						switch (settingIndex) {
							case 0:
								mapKey = SETTINGS_MAP.HAPTIC_FEEDBACK;
								break;
							case 1:
								// Setting to null will reinit evaluator next time is needed
								changingLinesEvaluator = null;
								mapKey = SETTINGS_MAP.CHANGING_LINES_EVALUATOR;
								break;
							case 2:
								// Clear remote strings cache in case language changes
								RemoteResolver.clearCache();
								mapKey = SETTINGS_MAP.LANGUAGE;
								break;
							case 3:
								// Clear remote strings cache in case dictionary changes
								RemoteResolver.clearCache();
								mapKey = SETTINGS_MAP.DICTIONARY;
								break;
						}
						entry.setOptionValue(entry.getOptionValues().get(position));
						settings.put(mapKey, entry.getOptionValue());
						
						((BaseAdapter)lvSettings.getAdapter()).notifyDataSetChanged();
						lvSettings.invalidateViews();
						
						settings.save(IChingActivity.this);
					}

					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});
				spinner.performClick();
			}
		});
	}
	
	/**
     * onClick handler for the init (main) view
     * 
     * @param view The event target
     */
	public void onClickConsultBtn(View view) {
		switch (view.getId()) {
			case R.id.btnQuestion:
				EditText etQuestion = (EditText) findViewById(R.id.etQuestion);
				question = etQuestion.getText().toString();
				
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(etQuestion.getWindowToken(), 0);

				if (question.equals("")) {
					AlertDialog alertDialog = new AlertDialog.Builder(IChingActivity.this).create();
					alertDialog.setMessage(Utils.s(R.string.intro_noquestion_alert));
					alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, Utils.s(R.string.yes), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							gotoConsult();
						} 
					});
					alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.no), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {} 
					}); 
					alertDialog.show();
				} else {
					gotoConsult();
				}

				break;
		}
	}
	
	/**
	 * onClick handler for the coins in the consult view. Generates the hexagram
	 * 
	 * @param view Not used
	 */
	public void onClickGenerateRow(View view) {
		int coinsValue = 0;
		for (int i = 0; i <3; i++) {
			double rnd = Math.random();
			if (rnd < 0.5) {
				coinsValue += 2;
			} else {
				coinsValue += 3;
			}
		}
				
		hex[hexRow] = coinsValue;
		renderRow(hexRow++, coinsValue, true);
		
		if (hexRow >= 6) {
			gotoConsult();
		}
		
		if (Utils.mask((Integer) settings.get(SETTINGS_MAP.HAPTIC_FEEDBACK), Consts.HAPTIC_FEEDBACK_ON_THROW_COINS)) {
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			v.vibrate(300);
		}
	}
	

	
	/**
	 * Callback for a context menu voice selection
	 * @param item The selected menu voice
	 * @return true
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
	  AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
	  ListView lvHistory = (ListView) findViewById(R.id.lvHistory);
	  final BaseAdapter listAdapter = (BaseAdapter) lvHistory.getAdapter();
	  final View tvHistory = (View) findViewById(R.id.tvHistory);
	  if (item.getItemId() == CONTEXTMENU_HISTORY_DELETE_ENTRY) {
		  historyList.remove(info.position);
		  if(historyList.size() == 0) {
			  tvHistory.setVisibility(View.GONE);
		  }
		  DataPersister.saveHistory(historyList, this);
		  listAdapter.notifyDataSetChanged();
	  } else if (item.getItemId() == CONTEXTMENU_HISTORY_DELETE_ALL) {
		  AlertDialog alertDialog = new AlertDialog.Builder(IChingActivity.this).create();
		  alertDialog.setMessage(Utils.s(R.string.history_erase));
		  alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, Utils.s(R.string.yes), new DialogInterface.OnClickListener() {
			  public void onClick(DialogInterface dialog, int which) {
				  historyList.clear();
				  tvHistory.setVisibility(View.GONE);
				  DataPersister.saveHistory(historyList, IChingActivity.this);
				  listAdapter.notifyDataSetChanged(); 
			  } 
		  });
		  alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.no), new DialogInterface.OnClickListener() {
			  public void onClick(DialogInterface dialog, int which) {} 
		  }); 
		  alertDialog.show();
	  }
	  
	  return true;
	}
	
	/** 
	 * Called when the activity is first created.
	 * 
	 * @param savedInstanceState The saved state 
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);     
        Utils.setContext(getApplicationContext());
        
        loadSettings();
        
        if (currentViewId == null) {
        	gotoMain();
        }
    }
	
	/**
     * Create a context menu on the history list to delete entries
     */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {  
	    super.onCreateContextMenu(menu, v, menuInfo); 
	    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
	    HistoryEntry entry = historyList.get(info.position);
	    
        menu.setHeaderTitle(entry.getQuestion());  
        menu.add(0, CONTEXTMENU_HISTORY_DELETE_ENTRY, 0, R.string.intro_history_delete_entry);  
        menu.add(0, CONTEXTMENU_HISTORY_DELETE_ALL, 0, R.string.intro_history_delete_all);  
	}
	
	/**
	 * Create an option menu for the "About" section and other stuff
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);

	    return true;
	}
	
	/**
	 * Update the option menu for the "About" section and other stuff
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) { 
		final String dictionary = (String) getSettingsManager().get(SETTINGS_MAP.DICTIONARY);
	    final MenuItem omViewHex = menu.findItem(R.id.omViewHex);
	    if (dictionary.equals(Consts.DICTIONARY_CUSTOM)) {
	    	omViewHex.setTitle(R.string.options_view_edit_hex);
	    } else {
	    	omViewHex.setTitle(R.string.options_view_hex);
	    }
		return true;
	}
	
	/**
	 * Implementation of the onKeyDown method to deal with back button
	 * 
	 * @param keyCode The pressed key code
	 * @param event The key down event
	 * @return true if the event has been consumed, false otherwise
	 */
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
	    if(keyCode == KeyEvent.KEYCODE_BACK) {
            if (currentViewId == R.layout.main) {
            	onBackPressed();
            	return true;
            } else {
            	question = "";
            	gotoMain();
            	return true;
            }
	    }
	    return false;
	}
    
	/**
	 * Respond to the option menu voices selection
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final AlertDialog alertDialog = new AlertDialog.Builder(IChingActivity.this).create();
	  	
	  	TextView tvMessage = new TextView(getApplicationContext());
	  	tvMessage.setBackgroundColor(getResources().getColor(android.R.color.background_dark));
	  	tvMessage.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
	  	tvMessage.setTextSize(13);
	  	tvMessage.setPadding(5, 5, 5, 5);
	  	
	  	alertDialog.setView(tvMessage);
	  	alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, Utils.s(android.R.string.ok), new DialogInterface.OnClickListener() {
	  		public void onClick(DialogInterface dialog, int which) {
	  			alertDialog.dismiss(); 
	  		} 
	  	});
		  	
	    switch (item.getItemId()) {
	    	case R.id.omSettings :
	    		gotoSettings();
	    		break;
	    	case R.id.omViewHex :
	    		String[] hexArray = new String[64];
	    		for (int i = 0; i < hexArray.length; i++) {
	    			String index = (i + 1 < 10 ? "0" : "") + (i + 1);
	    			int entry = Utils.getResourceByName(R.string.class, "hex" + index);
	    			hexArray[i] = (i + 1) + " " + Utils.s(entry);
	    		}
	    		
	    		final Dialog dialog;
	    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    		builder.setTitle("Select an hexagram");
	    		builder.setItems(hexArray, new DialogInterface.OnClickListener() {
	    		    public void onClick(DialogInterface dialog, int index) {
	    		    	hex = Utils.invHexMap(index+1);
	    		    	question = "";
	    		    	mode = READ_DESC_MODE.VIEW_HEX;
	    		    	gotoReadDesc();
	    		    	dialog.dismiss();
	    		    }
	    		});
	    		dialog = builder.create();

	    		dialog.show();
	    		break;
	    	case R.id.omAlgo :
	    		alertDialog.setMessage(Utils.s(R.string.options_algo));
	    		tvMessage.setText(Html.fromHtml(Utils.s(R.string.options_algo_message)));
	    		alertDialog.show();
	    		break;
	    	case R.id.omReferences :
	    		alertDialog.setMessage(Utils.s(R.string.options_references));
	    		tvMessage.setText(Html.fromHtml(Utils.s(R.string.options_references_message)));
	    		alertDialog.show();
	    		break;
	    	case R.id.omAbout :
	    		alertDialog.setMessage(Utils.s(R.string.options_about));
	    		tvMessage.setText(Html.fromHtml(Utils.s(R.string.options_about_message)));
	    		alertDialog.show();
	    		break;
	    }
	    return true;
	}

    /** 
	 * Called when the activity is restored.
	 * 
	 * @param savedInstanceState The saved state 
	 */
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		currentViewId = savedInstanceState.getInt("currentView");
		question 	= savedInstanceState.getString("question");
		
		hexRow 		= savedInstanceState.getInt("hexRow");
		changing 	= savedInstanceState.getInt("changing");
		hex 		= savedInstanceState.getIntArray("hex");
		tHex 		= savedInstanceState.getIntArray("tHex");
		
		mode		= READ_DESC_MODE.valueOf(savedInstanceState.getString("mode"));
		
		setCurrentSection(changing);
		setCurrentHex(hex);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		switch (currentViewId) {
			case R.layout.main :
				gotoMain();
				break;
			case R.layout.consult :
				gotoConsult();
				break;
			case R.layout.readdesc :
				gotoReadDesc();
				break;
		}
		loadSettings();
	}
	
	/** 
	 * Called when the activity is suspended.
	 * 
	 * @param savedInstanceState The saved state 
	 */
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putInt("currentView", currentViewId);
		savedInstanceState.putString("question", question);
		
		savedInstanceState.putInt("hexRow", hexRow);
		savedInstanceState.putInt("changing", changing);
		savedInstanceState.putIntArray("hex", hex);
		savedInstanceState.putIntArray("tHex", tHex);

		savedInstanceState.putString("mode", String.valueOf(mode == null ? READ_DESC_MODE.ORACLE : mode));
		
		super.onSaveInstanceState(savedInstanceState);
	}
	
	/**
	 * Wrapper method to set the content view after storing it
	 * 
	 * @param resId A layout view resource identifier
	 */
	@Override
	public void setContentView(int resId) {
		currentViewId = resId;
		super.setContentView(currentViewId);
	}
		
	/**
	 * Internal method to add a tab to an host
	 * 
	 * @param tabHost The host
	 * @param tag A tag for the tab
	 * @param indicator The tab title resource identifier
	 * @param resId The content layout resource identifier
	 */
	private void setupTab(TabHost tabHost, String tag, int indId, int resId) {
		tabHost.addTab(
			tabHost.newTabSpec(tag)
				.setIndicator(Utils.s(indId))
				.setContent(resId)
			);
		
	}
	
	/**
	 * Load settings from sd card. If none is found, default apply
	 */
	private void loadSettings() {
		try {
        	settings.load();
        } catch(FileNotFoundException e) {
        	settings.resetDefaults();
        } catch(IOException e) {
        	settings.resetDefaults();
        	AlertDialog alertDialog = new AlertDialog.Builder(IChingActivity.this).create();
			alertDialog.setMessage(Utils.s(R.string.options_unavailable));
			alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, Utils.s(android.R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {} 
			});
			alertDialog.show();
        }
	}
}