package org.digitalillusion.droid.iching;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import org.digitalillusion.droid.iching.anim.AnimCoin;
import org.digitalillusion.droid.utils.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
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
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.TwoLineListItem;

/**
 * A complete I Ching oracle for Android OS
 * 
 * @author digitalillusion
 */
public class IChingActivity extends Activity {
	
	/** The consultation mode, being using the oracle or reading the book **/
	private enum READ_DESC_MODE {
		ORACLE,
		VIEW_HEX
	}
	
	/** Old Yin coin value **/
	private static final int ICHING_OLD_YIN = 6;
	/** Young Yang coin value **/
	private static final int ICHING_YOUNG_YANG = 7;
	/** Young Yin coin value **/
	private static final int ICHING_YOUNG_YIN = 8;
	
	/** Old Yang coin value **/
	private static final int ICHING_OLD_YANG = 9;
	/** Path of the remote string resources **/
	private static final String ICHING_REMOTE_URL = "http://androidiching.altervista.org/en/";
	
	/** Path of the local history resources **/
	private static final String ICHING_HISTORY_PATH = "/Android/data/org.digitalillusion.droid.iching/files/history.bin";
	/** Description section identifier **/
	private static final String ICHING_REMOTE_SECTION_DESC 	= "desc";
	/** Judgment section identifier **/
	private static final String ICHING_REMOTE_SECTION_JUDGE = "judge";
	/** Image section identifier **/
	private static final String ICHING_REMOTE_SECTION_IMAGE = "image";
	
	/** Changing lines section identifier **/
	private static final String ICHING_REMOTE_SECTION_LINE 	= "line";
	/** Flag to use when cast hexagram applies **/
	private static final int ICHING_APPLY_CAST = -1;
	/** Flag to use when transformed hexagram applies **/
	private static final int ICHING_APPLY_TRANSFORMED = -2;
	
	/** Flag to use when both cast and transformed hexagrams apply **/
	private static final int ICHING_APPLY_BOTH = -3;
	
	/** SORTED Subset of the hexagrams set when all lines changing have a particular meaning **/
	private static final Integer[] ICHING_ALL_LINES_DESC = new Integer[] { 1, 2, 12, 47 };
	/** Unique identifier for the "Delete entry" context menu voice **/
	private static final int CONTEXTMENU_HISTORY_DELETE_ENTRY = 1;
	
	/** Unique identifier for the "Delete history" context menu voice **/
	private static final int CONTEXTMENU_HISTORY_DELETE_ALL = 2;
	/** Memory cache of remote strings **/
	private HashMap<String, Spanned> remoteStringCache = new HashMap<String, Spanned>();
	
	/** Memory cache of the local history **/
	private ArrayList<HistoryEntry> historyList = new ArrayList<HistoryEntry>();
	
	/** Utilities class **/
	protected Utils utils;
	/** The user question **/
	protected String question;
	/** The currently generated hexagram row **/
	protected int hexRow;
	/** The currently generated hexagram **/
	protected int[] hex;
	/** The changing line index **/
	protected int changing;
	/** The consultation mode **/
	protected READ_DESC_MODE mode;

	/** The hexagram transformed from the currently generated one **/
	protected int[] tHex;
	/** The current View **/
	private Integer currentViewId;	 
	
	/** The network exception dialogue **/
	private AlertDialog networkExceptionDialog;
	
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
			instructions.setText(Utils.getResourceByName(R.string.class, "hex" + hexMap(hex)));
			
			final Button btnReadDesc = new Button(getApplicationContext());
			final LinearLayout layout = (LinearLayout) findViewById(R.id.layCoins);
			btnReadDesc.setText(R.string.consult_read_desc);
			btnReadDesc.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						btnReadDesc.setVisibility(View.GONE);
						mode = READ_DESC_MODE.ORACLE;
						gotoReadDesc(true);
						
						// Save history entry
						HistoryEntry historyEntry = new HistoryEntry();
						historyEntry.setHex(hex);
						historyEntry.setChanging(changing);
						historyEntry.setTHex(tHex);
						historyEntry.setQuestion(question);
						historyEntry.setDate(new Date());
						
						historyList.add(0, historyEntry);
						saveHistory();
						
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
		
		final EditText etQuestion = (EditText) findViewById(R.id.etQuestion);
        etQuestion.setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				question = ((EditText) v).getText().toString();
				return false;
			}
		});
        etQuestion.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					View btnQuestion = (View) findViewById(R.id.btnQuestion);
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
    		loadHistory();
    		lvHistory.setAdapter(new BaseAdapter() {

    			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				public int getCount() {
					if (historyList != null) {
						return historyList.size();
					} else {
						return 0;
					}
				}
				
				public Object getItem(int position) {
					if (historyList != null) {
						return historyList.get(position);
					} else {
						return null;
					}
				}
				
				public long getItemId(int position) {
					return position;
				}
				
				public View getView(int position, View convertView, ViewGroup parent) {
					TwoLineListItem row;       
					if (convertView == null) {
		                row = (TwoLineListItem) inflater.inflate(android.R.layout.simple_list_item_2, null);
		                row.getText1().setTextAppearance(getApplicationContext(), android.R.style.TextAppearance_Medium);
		            } else {
		                row = (TwoLineListItem) convertView;
		            }
					HistoryEntry entry = (HistoryEntry) getItem(position);
					
					row.getText1().setText(entry.getQuestion());
					SimpleDateFormat dateFormat = new SimpleDateFormat();
					dateFormat.applyPattern("yyyy/MM/dd HH:mm:ss");
					row.getText2().setText(
						dateFormat.format(entry.getDate())
					);
					
					return row;
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
    				gotoReadDesc(false);
    			}
    		});
    		
    		tvHistory.setVisibility(View.VISIBLE);
	    	lvHistory.requestFocus();
	    	registerForContextMenu(lvHistory);
	    	
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
	public void gotoReadDesc(boolean setup) {
		
		if (setup) {
			setupReadDesc();
		}
		
		setContentView(R.layout.readdesc);
		
		final TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
		tabHost.setup();
		

		switch (mode) {
			case ORACLE : 
				setupTab(tabHost, "tab_consult", R.string.read_cast, R.id.layReadDesc);
				setupTab(tabHost, "tab_changing", R.string.read_changing, R.id.layReadDesc);
				if (changing != ICHING_APPLY_CAST) {
					setupTab(tabHost, "tab_future", R.string.read_transformed, R.id.layReadDesc);
				}
				break;
			case VIEW_HEX :
				setupTab(tabHost, "tab_consult", R.string.read_cast, R.id.layReadDesc);
				setupTab(tabHost, "tab_changing", R.string.read_changing, R.id.layReadDesc);
				break;
		}
		
		final TextView tvDescTitle = (TextView) findViewById(R.id.tvHexName);
		tabHost.setOnTabChangedListener(new OnTabChangeListener() {
			public void onTabChanged(String tabId) {
				if (tabId.equals("tab_consult")) {
					String hexMap = hexMap(hex);
					tvDescTitle.setText(Utils.getResourceByName(R.string.class, "hex" + hexMap));
					renderReadDesc(hex);
				} else if (tabId.equals("tab_changing")) {
					tvDescTitle.setText(Utils.s(R.string.read_changing));
					renderReadDescChanging(hex, mode);
				} else if (tabId.equals("tab_future")) {
					String hexMap = hexMap(tHex);
					tvDescTitle.setText(Utils.getResourceByName(R.string.class, "hex" + hexMap));
					renderReadDesc(tHex);
				}
			}
        });
		
		// Display current tab
		tabHost.getCurrentView().setVisibility(View.VISIBLE);
		String hexMap = hexMap(hex);
		tvDescTitle.setText(Utils.getResourceByName(R.string.class, "hex" + hexMap));
		renderReadDesc(hex);
		
		TabWidget tabWidget = tabHost.getTabWidget();
		tabWidget.setStripEnabled(false);
		for(int i = 0; i < tabWidget.getChildCount();i++)
		{
			View child = tabWidget.getChildAt(i);
			child.getLayoutParams().height = 35;
			child.setPadding(0, 0, 0, 10);
		}
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
		  saveHistory();
		  listAdapter.notifyDataSetChanged();
	  } else if (item.getItemId() == CONTEXTMENU_HISTORY_DELETE_ALL) {
		  AlertDialog alertDialog = new AlertDialog.Builder(IChingActivity.this).create();
		  alertDialog.setMessage(Utils.s(R.string.history_erase));
		  alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, Utils.s(R.string.yes), new DialogInterface.OnClickListener() {
			  public void onClick(DialogInterface dialog, int which) {
				  historyList.clear();
				  tvHistory.setVisibility(View.GONE);
				  saveHistory();
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
	  	tvMessage.setBackgroundColor(android.R.color.darker_gray);
	  	tvMessage.setTextSize(13);
	  	tvMessage.setPadding(5, 5, 5, 5);
	  	
	  	alertDialog.setView(tvMessage);
	  	alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, Utils.s(android.R.string.ok), new DialogInterface.OnClickListener() {
	  		public void onClick(DialogInterface dialog, int which) {
	  			alertDialog.dismiss(); 
	  		} 
	  	});
		  	
	    switch (item.getItemId()) {
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
	    		    	hex = invHexMap(index+1);
	    		    	question = "";
	    		    	mode = READ_DESC_MODE.VIEW_HEX;
	    		    	gotoReadDesc(true);
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
		
		switch (currentViewId) {
			case R.layout.main :
				gotoMain();
				break;
			case R.layout.consult :
				gotoConsult();
				break;
			case R.layout.readdesc :
				gotoReadDesc(false);
				break;
		}
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
	 * @param lineVal The line to evaluate
	 * @return The changed line if the line is changing, the same line otherwise
	 */
	private int getChangedLine(int lineVal) {
		if (lineVal == ICHING_OLD_YANG) {
			return ICHING_YOUNG_YIN;
		} else if (lineVal == ICHING_OLD_YIN) {
			return ICHING_YOUNG_YANG;
		} else {
			return lineVal;
		}
	}
	
	/**
	 * @param hex The hexagram currently being evaluated
	 * @param section One of ICHING_REMOTE_SECTION_ flags
	 * @return A string from the server
	 * @throws IOException if the connection fails
	 */
	private Spanned getRemoteString(final String hex, final String section) throws IOException {
		String key = hex + section;
		Spanned spanned;
		
		final class RemoteWorker extends Thread {
			private String text;
			private IOException ioe;
			@Override
			public void run() {
				try {
					InputStream is = Utils.downloadUrl(
						ICHING_REMOTE_URL,
						new String[] { "h", hex },
						new String[] { "s", section }
					);
					
					text = Utils.streamToString(is);
				} catch (IOException ioe) {
					this.ioe = ioe;
				}
			}
			
			public String getText() {
				return text;
			}
			
			public IOException getIOException() {
				return ioe;
			}
		};

		
		if (!remoteStringCache.containsKey(key)) {
			// Avoid NetworkInMainThreadException
			RemoteWorker worker = new RemoteWorker();
			worker.start();
			
			while (worker.isAlive()) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {}
			}
			
			if (worker.getIOException() != null) {
				throw worker.getIOException();
			}
			String text = worker.getText();
	
			if (text.equals("")) {
				// Retry
				spanned = getRemoteString(hex, section);
			} else {
				if (text.indexOf("\\e") > 0) {
					text = text.replaceAll(Utils.NEWLINE, "<br/>");
					spanned = Html.fromHtml(
						"<small><em>" + text.substring(0, text.indexOf("\\e")) + "</em><br/>" + 
			            text.substring(text.indexOf("\\e") + 2) + "</small>"
			           );
				} else {
					spanned = Html.fromHtml("<small>" + Html.fromHtml(text) + "</small>");
				}
				remoteStringCache.put(key, spanned);
			}
		} else {
			spanned = remoteStringCache.get(key);
		}
		return spanned;
	}
	
	/**
	 * Display a dialogue to signal that the application is offline
	 * 
	 * @param retryAction The action to execute if the user wants to retry connection
	 */
	private void handleNetworkException(OnClickListener retryAction) {
		if (networkExceptionDialog == null) {
			networkExceptionDialog = new AlertDialog.Builder(IChingActivity.this).create();
			networkExceptionDialog.setMessage(Utils.s(R.string.remoteconn_unavailable));
			networkExceptionDialog.setButton(DialogInterface.BUTTON_POSITIVE, Utils.s(R.string.retry), retryAction);
			networkExceptionDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {} 
			});
			networkExceptionDialog.setOnDismissListener(new OnDismissListener() {
				public void onDismiss(DialogInterface dialog) {
					networkExceptionDialog = null;
				}
			});
			networkExceptionDialog.show();
		}
	}
	
	/**
	 * @param an hexagram
	 * @return The map of the hex value to a cardinal index
	 */
	private String hexMap(int[] hex) {
		int value = 0;
		for (int i = 0; i < 6; i++) {
			value += (Math.pow(2, i))*(hex[i]%2);
		}
		switch (value) {
			case 0 : 
				return "02";
			case 1 : 
				return "24";
			case 2 : 
				return "07";
			case 3 : 
				return "19";
			case 4 : 
				return "15";
			case 5 : 
				return "36";
			case 6 : 
				return "46";
			case 7 : 
				return "11";
			case 8 : 
				return "16";
			case 9 : 
				return "51";
			case 10 : 
				return "40";
			case 11 : 
				return "54";
			case 12 : 
				return "62";
			case 13 : 
				return "55";
			case 14 : 
				return "32";
			case 15 : 
				return "34";
			case 16 : 
				return "08";
			case 17 : 
				return "03";
			case 18 : 
				return "29";
			case 19 : 
				return "60";
			case 20 : 
				return "39";
			case 21 : 
				return "63";
			case 22 : 
				return "48";
			case 23 : 
				return "05";
			case 24 : 
				return "45";
			case 25 : 
				return "17";
			case 26 : 
				return "47";
			case 27 : 
				return "58";
			case 28 : 
				return "31";
			case 29 : 
				return "49";
			case 30 : 
				return "28";
			case 31 : 
				return "43";
			case 32 : 
				return "23";
			case 33 : 
				return "27";
			case 34 : 
				return "04";
			case 35 : 
				return "41";
			case 36 : 
				return "52";
			case 37 : 
				return "22";
			case 38 : 
				return "18";
			case 39 : 
				return "26";
			case 40 : 
				return "35";
			case 41 : 
				return "21";
			case 42 : 
				return "64";
			case 43 : 
				return "38";
			case 44 : 
				return "56";
			case 45 : 
				return "30";
			case 46 : 
				return "50";
			case 47 : 
				return "14";
			case 48 : 
				return "20";
			case 49 : 
				return "42";
			case 50 : 
				return "59";
			case 51 : 
				return "61";
			case 52 : 
				return "53";
			case 53 : 
				return "37";
			case 54 : 
				return "57";
			case 55 : 
				return "09";
			case 56 : 
				return "12";
			case 57 : 
				return "25";
			case 58 : 
				return "06";
			case 59 : 
				return "10";
			case 60 : 
				return "33";
			case 61 :
				return "13";
			case 62 :
				return "44";
			case 63 :
				return "01";
		}
		return "-1";
	}


	/**
	 * @param a cardinal index
	 * @return The map of the cardinal index to the hex value;
	 */
	private int[] invHexMap(int index) {
		int[] hex = new int[6];
		for (int i = 0; i < hex.length; i++) {
			hex[i] = ICHING_YOUNG_YIN;
		}
		switch (index) {
			case 24 :
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 7 : 
				hex[1] = ICHING_YOUNG_YANG;
				break;
			case 19 : 
				hex[1] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 15 : 
				hex[2] = ICHING_YOUNG_YANG;
				break;
			case 36 : 
				hex[2] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 46 : 
				hex[2] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				break;
			case 11 : 
				hex[2] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 16 : 
				hex[3] = ICHING_YOUNG_YANG;
				break;
			case 51 : 
				hex[3] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 40 : 
				hex[3] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				break;
			case 54 : 
				hex[3] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 62 : 
				hex[3] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				break;
			case 55 : 
				hex[3] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 32 : 
				hex[3] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				break;
			case 34 : 
				hex[3] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 8 : 
				hex[4] = ICHING_YOUNG_YANG;
				break;
			case 3 : 
				hex[4] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 29 : 
				hex[4] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				break;
			case 60 : 
				hex[4] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 39 : 
				hex[4] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				break;
			case 63 : 
				hex[4] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 48 : 
				hex[4] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				break;
			case 5 : 
				hex[4] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 45 : 
				hex[4] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				break;
			case 17 : 
				hex[4] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 47 : 
				hex[4] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				break;
			case 58 : 
				hex[4] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 31 : 
				hex[4] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				break;
			case 49 : 
				hex[4] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 28 : 
				hex[4] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				break;
			case 43 : 
				hex[4] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 23 : 
				hex[5] = ICHING_YOUNG_YANG;
				break;
			case 27 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 4 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				break;
			case 41 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 52 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				break;
			case 22 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 18 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				break;
			case 26 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 35 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				break;
			case 21 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 64 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				break;
			case 38 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 56 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				break;
			case 30 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 50 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				break;
			case 14 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 20 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[4] = ICHING_YOUNG_YANG;
				break;
			case 42 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[4] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 59 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[4] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				break;
			case 61 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[4] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 53 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[4] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				break;
			case 37 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[4] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 57 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[4] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				break;
			case 9 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[4] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 12 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[4] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				break;
			case 25 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[4] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 06 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[4] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				break;
			case 10 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[4] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 33 : 
				hex[5] = ICHING_YOUNG_YANG;
				hex[4] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				break;
			case 13 :
				hex[5] = ICHING_YOUNG_YANG;
				hex[4] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
			case 44 :
				hex[5] = ICHING_YOUNG_YANG;
				hex[4] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				break;
			case 01 :
				hex[5] = ICHING_YOUNG_YANG;
				hex[4] = ICHING_YOUNG_YANG;
				hex[3] = ICHING_YOUNG_YANG;
				hex[2] = ICHING_YOUNG_YANG;
				hex[1] = ICHING_YOUNG_YANG;
				hex[0] = ICHING_YOUNG_YANG;
				break;
		}
		return hex;
	}

	
	/**
	 * @param lineVal The line to evaluate
	 * @return True if the line is changing, false otherwise
	 */
	private boolean isChangingLine(int lineVal) {
		return lineVal == ICHING_OLD_YANG || lineVal == ICHING_OLD_YIN;
	}

    /**
	 * @return True if media is readable, false otherwise
	 */
	private boolean isSDReadable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    return true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    return true;
		}
		return false;
	}
	
	/**
	 * @return True if media is writable, false otherwise
	 */
	private boolean isSDWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	/**
	 * Load history from SD card
	 * 
	 * @throws IOException if SD is not readable
	 * @throws FileNotFoundException if no history was saved;
	 */
	@SuppressWarnings("unchecked")
	private void loadHistory() throws IOException, FileNotFoundException {
		if (historyList.size() == 0) {
			if (!isSDReadable()) {
				throw new IOException();
			}
			File path = Environment.getExternalStorageDirectory();
			File historyFile = new File(path.getAbsolutePath() + ICHING_HISTORY_PATH) ;
			if (historyFile.exists()) {
				FileInputStream fis = new FileInputStream(historyFile);
				ObjectInputStream stream = new ObjectInputStream(fis); 
				try {
					historyList = (ArrayList<HistoryEntry>) stream.readObject();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					Log.e("IChingActivity.loadHistory()", e.getMessage());
				}
	    		if(historyList.size() == 0) {
	    			throw new FileNotFoundException();
	    		}
			} else {
				throw new FileNotFoundException();
			}
		}
	}
	
	/**
	 * Renders a tab of the readDesc layout, given the associated hexagram
	 * 
	 * @param hexToRender The hexagram to render and describe
	 */
	private void renderReadDesc(final int[] hexToRender) {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		LinearLayout layButtonsAndChanging = (LinearLayout) findViewById(R.id.layButtonsAndChanging);
		for (int i = 0; i < layButtonsAndChanging.getChildCount(); i++) {
			layButtonsAndChanging.getChildAt(i).setVisibility(View.GONE);
		}
		
		for (int i = 0; i < 6; i++) {
			renderRow(i, hexToRender[i], false);
		} 
		
		final String hexMap = hexMap(hexToRender);
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
				try {
					etOutput.setText(getRemoteString(hexMap, ICHING_REMOTE_SECTION_DESC));
				} catch (IOException e) {
					handleNetworkException(new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							renderReadDesc(hexToRender);
						} 
					});
				}
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
				try {
					etOutput.setText(getRemoteString(hexMap, ICHING_REMOTE_SECTION_JUDGE));
				} catch (IOException e) {
					handleNetworkException(new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							renderReadDesc(hexToRender);
						} 
					});
				}
				btReadDesc.setPressed(false);
				btReadJudge.setPressed(true);
				btImage.setPressed(false);
				return true;
			}
			
		});

		btImage.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				try {
					etOutput.setText(getRemoteString(hexMap, ICHING_REMOTE_SECTION_IMAGE));
				} catch (IOException e) {
					handleNetworkException(new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							renderReadDesc(hexToRender);
						} 
					});
				}
				btReadDesc.setPressed(false);
				btReadJudge.setPressed(false);
				btImage.setPressed(true);
				return true;
			}
		});
	}
	
	/**
	 * Renders the changing lines tab of the readDesc layout, given the associated hexagram
	 * 
	 * @param hexToRender The hexagram to evaluate for changing lines
	 * @param mode The mode used to display changing lines
	 */
	private void renderReadDescChanging(final int[] hexToRender, final READ_DESC_MODE mode) {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		LinearLayout layButtonsAndChanging = (LinearLayout) findViewById(R.id.layButtonsAndChanging);
		for (int i = 0; i < layButtonsAndChanging.getChildCount(); i++) {
			layButtonsAndChanging.getChildAt(i).setVisibility(View.GONE);
		}
		
		int changingCount = 0;
		for (int i = 0; i < 6; i++) {
			if (isChangingLine(hexToRender[i])) {
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
		final String hexMap = hexMap(hexToRender);
		
		etChanging.setVisibility(View.VISIBLE);
		String desc = "";
		etOutput.setText(desc);
		
		switch (mode) {
			case ORACLE :
				if (changingCount == 0) {
					desc = Utils.s(R.string.read_changing_none) + "<br/>";
				} else {
					int resId = changingCount == 1 ? R.string.read_changing_one : R.string.read_changing_count;
					desc = Utils.s(resId, new Integer[] { changingCount }) + "<br/>";
				}
				
				try {
					switch (changing) {
						case ICHING_APPLY_BOTH :
							desc += "<em>" + Utils.s(R.string.read_changing_apply_ht) + "</em>";
							int intMap = Integer.parseInt(hexMap);
							for (int allLines : ICHING_ALL_LINES_DESC) {
								if (intMap == allLines) {
									etOutput.setText(getRemoteString(hexMap, ICHING_REMOTE_SECTION_LINE + changing));
									break;
								}
							}
							break;
						case ICHING_APPLY_CAST :
							desc += "<em>" + Utils.s(R.string.read_changing_apply_h) + "</em>";
							break;
						case ICHING_APPLY_TRANSFORMED :
							desc += "<em>" + Utils.s(R.string.read_changing_apply_t) + "</em>";
							break;
						default :
							desc += "<em>" + Utils.s(R.string.read_changing_apply, new Integer[] { changing + 1 }) + "</em>";
							etOutput.setText(getRemoteString(hexMap, ICHING_REMOTE_SECTION_LINE + (changing + 1)));
					}
				} catch (IOException e) {
					handleNetworkException( new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							renderReadDescChanging(hexToRender, mode);
						} 
					});
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
				
				int hexId = Integer.parseInt(hexMap(hexToRender));
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
						try {
							position = (position + 1 > 6) ? ICHING_APPLY_BOTH : position + 1;
							etOutput.setText(getRemoteString(hexMap, ICHING_REMOTE_SECTION_LINE + position));
						} catch (IOException e) {
							handleNetworkException( new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									renderReadDescChanging(hexToRender, mode);
								} 
							});
						}
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
	 * Render a row of the hex
	 * 
	 * @param index the row index from 0 (first) to 6 (last)
	 * @param coinsValue The value of the coins for this row
	 * @param renderMobileLines True to show mobile lines, false to render them fixed
	 */
	private void renderRow(int index, int coinsValue, boolean renderMobileLines) {
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
			case ICHING_OLD_YIN:
				lineRes = R.drawable.oldyin;
				break;
			case ICHING_YOUNG_YANG:
				lineRes = R.drawable.yang;
				break;
			case ICHING_YOUNG_YIN:
				lineRes = R.drawable.yin;
				break;
			case ICHING_OLD_YANG:
				lineRes = R.drawable.oldyang;
				break;
		}
		
		if (!renderMobileLines) {
			if (coinsValue == ICHING_OLD_YIN) {
				lineRes = R.drawable.yin;
			} else if (coinsValue == ICHING_OLD_YANG) {
				lineRes = R.drawable.yang;
			}
		}
		
		row.setBackgroundResource(lineRes);
	}
	
	
	/**
	 * Save history to SD card
	 * 
	 * @throws IOException if SD is not writable
	 */
	private void saveHistory() {
		try {
			if (!isSDWritable()) {
				throw new IOException();
			}
			File path = Environment.getExternalStorageDirectory();
			File historyFile = new File(path.getAbsolutePath() + ICHING_HISTORY_PATH);
			if(!historyFile.exists()) {
				String absPath = historyFile.getAbsolutePath();
				File historyDir = new File(absPath.substring(0, absPath.lastIndexOf(File.separator)));
				historyDir.mkdirs();
				
				historyFile.createNewFile();
			}
			
			FileOutputStream fos = new FileOutputStream(historyFile);
			ObjectOutputStream stream = new ObjectOutputStream(fos); 
			stream.writeObject(historyList);
		} catch (IOException e) {
			AlertDialog alertDialog = new AlertDialog.Builder(IChingActivity.this).create();
			alertDialog.setMessage(Utils.s(R.string.history_unsaveable));
			alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, Utils.s(R.string.retry), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					saveHistory();
				} 
			});
			alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {} 
			}); 
		}
	}
	
	
	/**
	 * Setup the readDesc layout by evaluating the cast hexagram, the applicable
	 * changing lines and obtaining the transformed hexagram
	 */
	private void setupReadDesc() {
		// Master Yin's rules
		int changingCount = 0;
		for (int i = 0; i < 6; i++) {
			if (isChangingLine(hex[i])) {
				changingCount++;
			}
			tHex[i] = getChangedLine(hex[i]);
		}
		
		switch (changingCount) {
			case 0 :
				// Read only the Cast Hexagram
				changing = ICHING_APPLY_CAST; 
				break;
			case 1 :
				// Consult this changing line
				for (int i = 0; i < 6; i++) {
					if (isChangingLine(hex[i])) {
						changing = i;
						break;
					}
				}
				break;
			case 2 :
				// If both lines are Six or Nine, the Upper Line applies.
				// If one is Six and the other Nine, the Six prevails.
				int sum = 0;
				for (int i = 0; i < 6; i++) {
					if (isChangingLine(hex[i])) {
						sum += hex[i];
					}
				}
				if ((sum == ICHING_OLD_YANG*2) || (sum == ICHING_OLD_YIN*2)) {
					boolean taken = false;
					for (int i = 5; i >= 0; i--) {
						if (!taken && isChangingLine(hex[i])) {
							taken = true;
							changing = i;
						}
					}	
				} else { 
					for (int i = 5; i >= 0; i--) {
						if (isChangingLine(hex[i]) && hex[i] == ICHING_OLD_YIN) {
							changing = i;
						}
					}
				}
				break;
			case 3 :
				// The middle line counts
				boolean dropped = false;
				for (int i = 0; i < 6; i++) {
					if (isChangingLine(hex[i])) {
						if (dropped) {
							changing = i;
						} else {
							dropped = true;
						}
					}
				}
				break;
			case 4 :
				// Read the upper NON-changing line
				boolean taken = false;
				for (int i = 5; i >= 0; i--) {
					if (taken || isChangingLine(hex[i])) {
					} else {
						taken = true;
						changing = i;
						tHex[i] = getChangedLine(hex[i]);
					}
				}
				break;
			case 5 :
				// Read the only NON-changing line
				for (int i = 5; i >= 0; i--) {
					if (!isChangingLine(hex[i])) {
						changing = i;
					}
				}
				break;
			case 6 :
				// Only the Transformed Hexagram applies.
				// With two exceptions: If all lines are Six or all lines are Nine, 
				// read both Cast Hexagram and Transformed Hexagram.
				changing = ICHING_APPLY_TRANSFORMED;
				sum = 0;
				for (int i = 0; i < 6; i++) {
					sum += hex[i];
				}
				if(sum == ICHING_OLD_YANG*6 || sum == ICHING_OLD_YIN*6) {
					changing = ICHING_APPLY_BOTH;
				}
				break;
		}
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

}