package org.digitalillusion.droid.iching;

import static org.digitalillusion.droid.iching.utils.SettingsManager.NO_ACTION;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.InputType;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
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
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.TwoLineListItem;

import org.digitalillusion.droid.iching.anim.AnimCoin;
import org.digitalillusion.droid.iching.changinglines.ChangingLinesEvaluator;
import org.digitalillusion.droid.iching.connection.RemoteResolver;
import org.digitalillusion.droid.iching.utils.Consts;
import org.digitalillusion.droid.iching.utils.DataPersister;
import org.digitalillusion.droid.iching.utils.IncrementalUpgrades;
import org.digitalillusion.droid.iching.utils.SettingsManager;
import org.digitalillusion.droid.iching.utils.SettingsManager.SETTINGS_MAP;
import org.digitalillusion.droid.iching.utils.Utils;
import org.digitalillusion.droid.iching.utils.lists.HistoryEntry;
import org.digitalillusion.droid.iching.utils.lists.ListItem2Adapter;
import org.digitalillusion.droid.iching.utils.lists.SettingsEntry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * A complete I Ching oracle for Android OS
 *
 * @author digitalillusion
 */
public class IChingActivity extends IChingActivityRenderer {

  /**
   * The currently generated hexagram row *
   */
  protected int hexRow;
  /**
   * The currently generated hexagram *
   */
  int[] hex;
  /**
   * The hexagram transformed from the currently generated one *
   */
  int[] tHex;

  /**
   * Random number generator
   */
  private Random rndGen = new Random();

  /**
   * Flag indicating that onResume was called
   */
  private boolean onResumeCalledAlready;
  /**
   * Key to access the onResume called flag in the instance state
   */
  private String ON_RESUME_CALLED_PREFERENCE_KEY = "onResumeCalled";


  /**
   * Move to the consult view
   */
  public void gotoConsult() {
    setScreenOrientation();
    setContentView(R.layout.consult);
    TextView tvQuestionShow = (TextView) findViewById(R.id.tvQuestionConsult);
    tvQuestionShow.setText(current.question);
    rndGen.setSeed(System.currentTimeMillis());

    for (int i = 0; i < hexRow; i++) {
      renderRow(i, hex[i], true, null, null);
    }

    if (hexRow < 6) {
      sensorManager.registerListener(shakeDetector, accelerometer,	SensorManager.SENSOR_DELAY_UI);
      prepareDivinationMethod();
    } else {
      sensorManager.unregisterListener(shakeDetector);
      ((ImageView) findViewById(R.id.picCoin01)).setVisibility(View.GONE);
      ((ImageView) findViewById(R.id.picCoin02)).setVisibility(View.GONE);
      ((ImageView) findViewById(R.id.picCoin03)).setVisibility(View.GONE);

      TextView tvInstructions = (TextView) findViewById(R.id.tvInstructions);
      String hexMap = Utils.hexMap(hex);
      tvInstructions.setText(Utils.getResourceByName(R.string.class, "hex" + hexMap));
      tvInstructions.setText(hexMap + " " + tvInstructions.getText());

      final Button btnReadDesc = new Button(getApplicationContext());
      final LinearLayout layout = (LinearLayout) findViewById(R.id.layCoins);
      btnReadDesc.setText(R.string.consult_read_desc);
      btnReadDesc.setOnTouchListener(new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
          if (event.getAction() == MotionEvent.ACTION_DOWN) {
            btnReadDesc.setVisibility(View.GONE);
            current.mode = READ_DESC_MODE.ORACLE;
            gotoReadDesc();

            // Save history entry
            HistoryEntry historyEntry = new HistoryEntry();
            historyEntry.setHex(hex);
            historyEntry.setChanging(current.changing);
            historyEntry.setTHex(tHex);
            historyEntry.setQuestion(current.question);
            historyEntry.setDate(new Date());

            historyList.add(0, historyEntry);
            DataPersister.saveHistory(historyList, IChingActivity.this);

            // Save all pending settings changes now that a new entry
            // has been added to the history
            settings.save(IChingActivity.this);

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
    setScreenOrientation();
    RemoteResolver.prepareRetryPopup();

    final EditText etQuestion = (EditText) findViewById(R.id.etQuestion);
    etQuestion.setOnEditorActionListener(new OnEditorActionListener() {
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        current.question = ((EditText) v).getText().toString();
        return false;
      }
    });
    etQuestion.setOnFocusChangeListener(new OnFocusChangeListener() {
      public void onFocusChange(View v, boolean hasFocus) {
        // Close keyboard on lose focus
        if (v.getId() == R.id.etQuestion && !hasFocus) {
          InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
          imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
      }
    });
    if (current.question != null) {
      etQuestion.setText(current.question);
    }

    final ListView lvHistory = (ListView) findViewById(R.id.lvHistory);
    lvHistory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
        final ListView lvHistory = (ListView) findViewById(R.id.lvHistory);
        HistoryEntry entry = (HistoryEntry) lvHistory.getItemAtPosition(position);
        IChingActivity thiz = IChingActivity.this;
        thiz.current.changing = entry.getChanging();
        thiz.current.changingManualIndex = 0;
        thiz.hex = entry.getHex();
        thiz.tHex = entry.getTHex();
        thiz.current.question = entry.getQuestion();
        thiz.current.mode = READ_DESC_MODE.ORACLE;
        thiz.current.tabIndex = 0;
        thiz.current.section = RemoteResolver.ICHING_REMOTE_SECTION_DESC;
        thiz.gotoReadDesc();
      }
    });

    renderOptionsMenu();

    // Trigger load history
    renderLoadHistory(null, () -> {
      if (historyList.size() == 0) {
        historyList.add(Utils.buildDummyHistoryEntry());
        historyList.clear();
      }
    });

    hexRow = 0;
    hex = new int[6];
    tHex = new int[6];
    current = new CurrentState();
  }



  /**
   * Move to the read description view
   */
  public void gotoReadDesc() {
    setScreenOrientation();

    ChangingLinesEvaluator changingLinesEvaluator = getChangingLinesEvaluator();
    current.changing = changingLinesEvaluator.evaluate(hex, tHex);
    current.changingCount = changingLinesEvaluator.countChanging(hex);

    current.screen = READ_DESC_SCREEN.DEFAULT;

    setContentView(R.layout.readdesc);

    final TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
    tabHost.setup();


    switch (current.mode) {
      case ORACLE:
        if (current.changing == ChangingLinesEvaluator.ICHING_APPLY_CAST) {
          setupTab(tabHost, "tab_consult", R.string.read_cast, R.id.layReadDesc);
          setupTab(tabHost, "tab_changing", R.string.read_changing, R.id.layReadDesc);
        } else {
          setupTab(tabHost, "tab_consult", R.string.read_cast, R.id.layReadDesc);
          setupTab(tabHost, "tab_changing", R.string.read_changing, R.id.layReadDesc);
          setupTab(tabHost, "tab_future", R.string.read_transformed, R.id.layReadDesc);
        }
        break;
      case VIEW_HEX:
        setupTab(tabHost, "tab_consult", R.string.read_cast, R.id.layReadDesc);
        setupTab(tabHost, "tab_changing", R.string.read_changing, R.id.layReadDesc);
        break;
    }

    // Display current tab
    tabHost.getCurrentView().setVisibility(View.VISIBLE);
    final TextView tvDescTitle = (TextView) findViewById(R.id.tvHexName);
    String hexMap = Utils.hexMap(hex);
    tvDescTitle.setText(Utils.getResourceByName(R.string.class, "hex" + hexMap));
    tvDescTitle.setText(hexMap + " " + tvDescTitle.getText());

    renderTabs(tabHost);

    final List<String> listTabId = Arrays.asList(new String[]{
        "tab_consult", "tab_changing", "tab_future"
    });
    final OnTabChangeListener onTabChange = new OnTabChangeListener() {
      public void onTabChanged(String tabId) {
        if (current.tabIndex != listTabId.indexOf(tabId)) {
          current.section = RemoteResolver.ICHING_REMOTE_SECTION_DESC;
          current.tabIndex = listTabId.indexOf(tabId);
        }
        switch (current.tabIndex) {
          case TAB_READ_DESC_CAST_HEXAGRAM:
            renderReadDesc(hex);
            break;
          case TAB_READ_DESC_CHANGING_LINES:
            renderReadDescChanging(hex);
            break;
          case TAB_READ_DESC_TRANSFORMED_HEXAGRAM:
            renderReadDesc(tHex);
            break;
        }
      }
    };
    tabHost.setOnTabChangedListener(onTabChange);
    onTabChange.onTabChanged(listTabId.get(current.tabIndex));
    tabHost.getTabWidget().getChildAt(current.tabIndex).performClick();
  }

  /**
   * Move to the settings view
   */
  public void gotoSettings() {
    setScreenOrientation();
    setContentView(R.layout.settings);

    final ListView lvSettings = (ListView) findViewById(R.id.lvSettings);

    List<SettingsEntry<?>> settingsList = new ArrayList<SettingsEntry<?>>();

    renderOptionsMenu();

    // Vibration
    settings.createOption(
        settingsList,
        SettingsEntry.VIBRATION,
        SettingsManager.SETTINGS_VALUES_MAP.get(SETTINGS_MAP.HAPTIC_FEEDBACK),
        SETTINGS_MAP.HAPTIC_FEEDBACK
    );
    // Divination method
    settings.createOption(
        settingsList,
        SettingsEntry.DIVINATION_METHOD,
        SettingsManager.SETTINGS_VALUES_MAP.get(SETTINGS_MAP.DIVINATION_METHOD),
        SETTINGS_MAP.DIVINATION_METHOD
    );
    // Changing lines
    settings.createOption(
        settingsList,
        SettingsEntry.CHLINES_EVALUATOR,
        SettingsManager.SETTINGS_VALUES_MAP.get(SETTINGS_MAP.CHANGING_LINES_EVALUATOR),
        SETTINGS_MAP.CHANGING_LINES_EVALUATOR
    );
    // Language
    settings.createOption(
        settingsList,
        SettingsEntry.LANGUAGE,
        SettingsManager.SETTINGS_VALUES_MAP.get(SETTINGS_MAP.LANGUAGE),
        SETTINGS_MAP.LANGUAGE
    );
    // Dictionary
    settings.createOption(
        settingsList,
        SettingsEntry.DICTIONARY,
        SettingsManager.SETTINGS_VALUES_MAP.get(SETTINGS_MAP.DICTIONARY),
        SETTINGS_MAP.DICTIONARY
    );
    // Storage
    settings.createOption(
        settingsList,
        SettingsEntry.STORAGE,
        SettingsManager.SETTINGS_VALUES_MAP.get(SETTINGS_MAP.STORAGE),
        SETTINGS_MAP.STORAGE
    );
    // Connection mode
    settings.createOption(
        settingsList,
        SettingsEntry.CONNECTION_MODE,
        SettingsManager.SETTINGS_VALUES_MAP.get(SETTINGS_MAP.CONNECTION_MODE),
        SETTINGS_MAP.CONNECTION_MODE
    );
    // Share
    settings.createOption(
        settingsList,
        SettingsEntry.SHARE,
        SettingsManager.SETTINGS_VALUES_MAP.get(SETTINGS_MAP.SHARE),
        SETTINGS_MAP.SHARE
    );
    // Screen Orientation
    settings.createOption(
        settingsList,
        SettingsEntry.SCREEN_ORIENTATION,
        SettingsManager.SETTINGS_VALUES_MAP.get(SETTINGS_MAP.SCREEN_ORIENTATION),
        SETTINGS_MAP.SCREEN_ORIENTATION
    );
    // Theme
    settings.createOption(
        settingsList,
        SettingsEntry.THEME,
        SettingsManager.SETTINGS_VALUES_MAP.get(SETTINGS_MAP.THEME),
        SETTINGS_MAP.THEME
    );
    // Theme
    settings.createOption(
        settingsList,
        SettingsEntry.BACKUP_AND_RESTORE,
        SettingsManager.SETTINGS_VALUES_MAP.get(SETTINGS_MAP.BACKUP_AND_RESTORE),
        SETTINGS_MAP.BACKUP_AND_RESTORE
    );

    lvSettings.setAdapter(new ListItem2Adapter<SettingsEntry<?>>(this, settingsList) {
      @Override
      public String getText1(SettingsEntry<?> entry) {
        return Utils.s(Utils.getResourceByName(R.string.class, entry.getOptionName()));
      }

      @Override
      public String getText2(SettingsEntry<?> entry) {
        return Utils.s(Utils.getResourceByName(R.string.class, entry.getOptionName() + Utils.UNDERSCORE + entry.getOptionValue()));
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        TwoLineListItem line = (TwoLineListItem) super.getView(position, convertView, parent);
        if (Utils.isDarkMode(settings)) {
          line.getText1().setTextColor(getResources().getColor(android.R.color.primary_text_dark));
          line.getText2().setTextColor(getResources().getColor(android.R.color.primary_text_dark));
        }
        return line;
      }
    });

    lvSettings.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      public void onItemClick(AdapterView<?> adapter, View view, final int settingIndex, long id) {
        @SuppressWarnings("unchecked")
        final SettingsEntry<Serializable> entry = (SettingsEntry<Serializable>) lvSettings.getItemAtPosition(settingIndex);
        Spinner spinner = (Spinner) findViewById(R.id.spBacking);
        ArrayList<String> optionsText = new ArrayList();
        for (Serializable value : entry.getOptionValues()) {
          if (!value.equals(NO_ACTION)) {
            optionsText.add(Utils.s(Utils.getResourceByName(R.string.class, entry.getOptionName() + Utils.UNDERSCORE + value.toString())));
          }
        }
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
            IChingActivity.this,
            android.R.layout.simple_spinner_item,
            optionsText.toArray(new String [0])
        ) {
          @Override
          public View getView(int position, View convertView, ViewGroup parent) {
            if (position < optionsText.size()) {
              TextView view = (TextView) super.getView(position, convertView, parent);
              if (Utils.isDarkMode(settings)) {
                view.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
              }
              return view;
            }
            return new View(parent.getContext());
          }
        };
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinner.setAdapter(arrayAdapter);
        spinner.setSelection(entry.getOptionIndex());
        spinner.setPromptId(Utils.getResourceByName(R.string.class, entry.getOptionName()));
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final SETTINGS_MAP mapKey = SETTINGS_MAP.values()[settingIndex];
            final Serializable newValue = entry.getOptionValues().get(position);
            final Runnable renderSettingChange = new Runnable() {
              public void run() {
                renderSettingChanged(lvSettings, entry, mapKey,
                    newValue);
              }
            };

            performOnItemSelected(mapKey, newValue, renderSettingChange);
          }

          public void onNothingSelected(AdapterView<?> arg0) {
          }

          private void performOnItemSelected(final SETTINGS_MAP mapKey,
                                             final Serializable newValue,
                                             final Runnable renderSettingChange) {
            boolean changed = true;
            switch (mapKey) {
              case LANGUAGE:
                Locale locale = new Locale(newValue.toString());
                settings.setLocale(locale);
                // Not breaking here
              case DICTIONARY:
                // Clear remote strings cache in case language or dictionary change
                RemoteResolver.clearCache();
                break;
              case STORAGE:
                // Switch the storage
                if (newValue.equals(Consts.STORAGE_SDCARD)) {
                  changed = DataPersister.useStorageSDCard(IChingActivity.this, settings);
                } else if (newValue.equals(Consts.STORAGE_INTERNAL)) {
                  changed = DataPersister.useStorageInternal(IChingActivity.this, settings);
                }
                break;
              case CONNECTION_MODE:
                changed = false;
                if (newValue.equals(Consts.CONNECTION_MODE_OFFLINE)) {
                  connectionManager.fromOnlineToOffline(IChingActivity.this, renderSettingChange);
                } else if (newValue.equals(Consts.CONNECTION_MODE_ONLINE)) {
                  connectionManager.fromOfflineToOnline(IChingActivity.this, renderSettingChange);
                }
                break;
              case THEME:
                changeTheme(newValue);
                break;
              case BACKUP_AND_RESTORE:
                changed = false;
                if (newValue.equals(Consts.BACKUP_AND_RESTORE_CREATE_BACKUP)) {
                  DataPersister.createBackup(IChingActivity.this);
                } else if (newValue.equals(Consts.BACKUP_AND_RESTORE_RESTORE_BACKUP)) {
                  Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                  intent.addCategory(Intent.CATEGORY_OPENABLE);
                  intent.setType("application/zip");

                  startActivityForResult(intent, ACTIVITY_RESULT_REQUEST_CODE.BACKUP_RESTORE_OPEN_FILE.ordinal());
                }
                break;
            }

            if (changed) {
              renderSettingChange.run();
            }
          }

          private void renderSettingChanged(
              final ListView lvSettings,
              final SettingsEntry<Serializable> entry,
              SETTINGS_MAP mapKey, Serializable newValue) {
            entry.setOptionValue(newValue);
            settings.put(mapKey, newValue);

            ((BaseAdapter) lvSettings.getAdapter()).notifyDataSetChanged();
            lvSettings.invalidateViews();

            settings.save(IChingActivity.this);
          }
        });
        spinner.performClick();
      }
    });
  }

  private void changeTheme(Serializable newValue) {
    boolean isSystemLight = newValue.equals(Consts.THEME_SYSTEM) && !Utils.isDarkMode(null);
    boolean isSystemDark = newValue.equals(Consts.THEME_SYSTEM) && Utils.isDarkMode(null);
    if (isSystemDark || newValue.equals(Consts.THEME_DARK)) {
      setTheme(android.R.style.Theme_Material);
    } else if (isSystemLight || newValue.equals(Consts.THEME_LIGHT)) {
      setTheme(android.R.style.Theme_DeviceDefault_Light);
    } else if (newValue.equals(Consts.THEME_HOLO)) {
      setTheme(android.R.style.Theme_Holo);
    }
    if (!settings.get(SETTINGS_MAP.THEME).equals(newValue)) {
      finish();
      startActivity(getIntent());
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
        current.question = etQuestion.getText().toString();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etQuestion.getWindowToken(), 0);

        if (current.question.isEmpty()) {
          AlertDialog alertDialog = new AlertDialog.Builder(IChingActivity.this).create();
          alertDialog.setMessage(Utils.s(R.string.intro_noquestion_alert));
          alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, Utils.s(R.string.yes), new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              gotoConsult();
            }
          });
          alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.no), new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
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
    for (int i = 0; i < 3; i++) {
      coinsValue += rndGen.nextInt(2) + 2;
    }

    generateRow(coinsValue);
  }

  /**
   * Callback for a context menu voice selection
   *
   * @param item The selected menu voice
   * @return true
   */
  @Override
  public boolean onContextItemSelected(final MenuItem item) {
    final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
        .getMenuInfo();
    ListView lvHistory = (ListView) findViewById(R.id.lvHistory);
    if (lvHistory == null) {
      return true;
    }
    final BaseAdapter listAdapter = (BaseAdapter) lvHistory.getAdapter();
    final View tvHistory = (View) findViewById(R.id.tvHistory);
    contextSelectDialog = new AlertDialog.Builder(IChingActivity.this).create();
    switch (item.getItemId()) {
      case ContextMenuItem.HISTORY_MOVE_ENTRY:
        final List<String> historyNames = DataPersister.getHistoryNames();
        historyNames.remove(DataPersister.getSelectedHistoryName());
        if (historyNames.size() > 0) {
          Dialog dialog = buildItemSelectionDialog(
              historyNames.toArray(new String[historyNames.size()]),
              Utils.s(R.string.item_select_dest_history),
              new OnClickListener() {
                public void onClick(final DialogInterface dialog, int index) {
                  final HistoryEntry entry = historyList.remove(info.position);
                  final String targetHistory = historyNames.get(index);
                  DataPersister.saveHistory(historyList, IChingActivity.this);
                  DataPersister.setSelectedHistory(targetHistory, Utils.EMPTY_STRING, true);
                  IChingActivity.this.renderLoadHistory(new Runnable() {
                    public void run() {
                      historyList.add(entry);
                      Utils.sortHistoryList(historyList);
                      DataPersister.saveHistory(historyList, IChingActivity.this);
                      renderLoadHistory(null, null);
                      dialog.dismiss();
                    }
                  }, new Runnable() {
                    public void run() {
                      DataPersister.revertSelectedHistory();
                      // Reload history and put back the removed entry
                      renderLoadHistory(null, null);
                      historyList.add(entry);
                      Utils.sortHistoryList(historyList);
                      DataPersister.saveHistory(historyList, IChingActivity.this);
                      renderLoadHistory(null, null);
                      dialog.dismiss();
                    }
                  });
                }
              });

          dialog.show();
        } else {
          showToast(Utils.s(R.string.history_no_destination));
        }
        break;
      case ContextMenuItem.HISTORY_DELETE_ENTRY:
        String question = historyList.get(info.position).getQuestion();
        if (question.isEmpty()) {
          question = Utils.s(R.string.contextmenu_noquestion);
        }
        contextSelectDialog.setTitle(question);
        contextSelectDialog.setMessage(Utils.s(R.string.contextmenu_history_erase_entry));
        contextSelectDialog.setButton(DialogInterface.BUTTON_POSITIVE,
            Utils.s(R.string.yes),
            new OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                historyList.remove(info.position);
                if (historyList.size() == 0) {
                  tvHistory.setVisibility(View.GONE);
                }
                DataPersister.saveHistory(historyList, IChingActivity.this);
                listAdapter.notifyDataSetChanged();
              }
            });
        contextSelectDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
            Utils.s(R.string.no),
            new OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
              }
            });
        contextSelectDialog.show();
        break;
      case ContextMenuItem.HISTORY_DELETE_ALL:
        contextSelectDialog.setTitle(DataPersister.getSelectedHistoryName());
        contextSelectDialog.setMessage(Utils.s(R.string.contextmenu_history_erase));
        contextSelectDialog.setButton(DialogInterface.BUTTON_POSITIVE,
            Utils.s(R.string.yes),
            new OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                historyList.clear();
                tvHistory.setVisibility(View.GONE);
                DataPersister.saveHistory(historyList, IChingActivity.this);
                listAdapter.notifyDataSetChanged();
              }
            });
        contextSelectDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
            Utils.s(R.string.no),
            DEFAULT_HISTORY_REVERT_DIALOG_BUTTON);
        contextSelectDialog.show();
        break;
      case ContextMenuItem.HISTORY_REMOVE:
        renderLoadHistory(new Runnable() {
          public void run() {
            contextSelectDialog.setTitle(DataPersister.getSelectedHistoryName());
            contextSelectDialog.setMessage(Utils.s(R.string.contextmenu_history_remove));
            contextSelectDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                Utils.s(R.string.yes),
                new OnClickListener() {
                  public void onClick(DialogInterface dialog,
                                      int which) {
                    DataPersister.removeHistory(IChingActivity.this);
                    DEFAULT_HISTORY_REVERT_TASK.run();
                  }
                });
            contextSelectDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                Utils.s(R.string.no),
                new OnClickListener() {
                  public void onClick(DialogInterface dialog, int which) {
                    DEFAULT_HISTORY_REVERT_TASK.run();
                  }
                });
            contextSelectDialog.show();
          }
        }, DEFAULT_HISTORY_REVERT_TASK);
        break;
      case ContextMenuItem.HISTORY_RENAME:
        renderLoadHistory(new Runnable() {
            public void run() {
              contextSelectDialog.setMessage(Utils.s(R.string.contextmenu_history_rename));
              final EditText input = new EditText(IChingActivity.this);
              input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
              input.setHint(DataPersister.getSelectedHistoryName());
              contextSelectDialog.setView(input);
              contextSelectDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                  Utils.s(android.R.string.ok),
                  new OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                      String historyNewName = input.getText().toString();
                      if (!historyNewName.isEmpty()) {
                        DataPersister.renameHistory(historyList, IChingActivity.this, historyNewName);
                      } else {
                        DataPersister.revertSelectedHistory();
                      }
                      renderLoadHistory(null, null);
                    }
                  });
              contextSelectDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                  Utils.s(R.string.cancel),
                  DEFAULT_HISTORY_REVERT_DIALOG_BUTTON);
              contextSelectDialog.show();
            }
          },
          DEFAULT_HISTORY_REVERT_TASK);
        break;
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
    Utils.setContext(this);

    loadSettings();
    changeTheme(settings.get(SETTINGS_MAP.THEME));

    sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    accelerometer = sensorManager
        .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    shakeDetector = new ShakeDetector();

    if (savedInstanceState != null) {
      onResumeCalledAlready = savedInstanceState.getBoolean(ON_RESUME_CALLED_PREFERENCE_KEY);
    } else {
      onResumeCalledAlready = false;
    }
  }

  /**
   * Create a context menu
   */
  @Override
  public void onCreateContextMenu(final ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

    switch (v.getId()) {
      case R.id.lvHistory:
        HistoryEntry entry = historyList.get(info.position);
        String question = entry.getQuestion();
        if (question.isEmpty()) {
          question = Utils.s(R.string.contextmenu_noquestion);
        }
        menu.setHeaderTitle(question);
        menu.add(0, ContextMenuItem.HISTORY_MOVE_ENTRY, 0, R.string.contextmenu_move_entry);
        menu.add(0, ContextMenuItem.HISTORY_DELETE_ENTRY, 1, R.string.contextmenu_delete_entry);
        menu.add(0, ContextMenuItem.HISTORY_DELETE_ALL, 2, R.string.contextmenu_delete_all);
        break;
      case R.id.elSelectHistory:
        final TextView tvChild = (TextView) v;
        final String historyName = tvChild.getText().toString();
        if (!historyName.equals(DataPersister.ICHING_HISTORY_PATH_FILENAME_DEFAULT)) {
          menu.setHeaderTitle(historyName);
          menu.add(0, ContextMenuItem.HISTORY_RENAME, 0, R.string.contextmenu_rename_entry);
          menu.add(0, ContextMenuItem.HISTORY_REMOVE, 1, R.string.contextmenu_remove_entry);
          DataPersister.setSelectedHistory(historyName, Utils.EMPTY_STRING, true);
        }
        break;
    }
  }

  /**
   * Create an option menu for the "About" section and other stuff
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu, menu);
    optionsMenu = menu;

    renderOptionsMenu();

    return true;
  }

  /**
   * Implementation of the onKeyDown method to deal with back button
   *
   * @param keyCode The pressed key code
   * @param event   The key down event
   * @return true if the event has been consumed, false otherwise
   */
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      if (current.viewId == R.layout.main) {
        onBackPressed();
        System.exit(0);
        return true;
      } else {
        current.question = Utils.EMPTY_STRING;
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

    TextView tvMessage = new TextView(this);
    tvMessage.setId(android.R.id.message);
    if (Utils.isDarkMode(settings)) {
      tvMessage.setBackgroundColor(getResources().getColor(android.R.color.background_dark));
      tvMessage.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
    } else {
      tvMessage.setBackgroundColor(getResources().getColor(android.R.color.background_light));
      tvMessage.setTextColor(getResources().getColor(android.R.color.primary_text_light));
    }
    tvMessage.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.text_size_small));
    tvMessage.setPadding(5, 5, 5, 5);

    alertDialog.setView(tvMessage);
    alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, Utils.s(android.R.string.ok), new OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        alertDialog.dismiss();
      }
    });

    switch (item.getItemId()) {
      case R.id.omSettings:
        gotoSettings();
        break;
      case R.id.omViewHex:
        String[] hexArray = new String[Consts.HEX_COUNT];
        for (int i = 0; i < hexArray.length; i++) {
          String index = (i + 1 < 10 ? "0" : Utils.EMPTY_STRING) + (i + 1);
          int entry = Utils.getResourceByName(R.string.class, "hex" + index);
          hexArray[i] = (i + 1) + " " + Utils.s(entry);
        }

        Dialog dialog = buildTrigramSelectionDialog(
            hexArray,
            Utils.s(R.string.item_select_hex),
            new OnClickListener() {
              public void onClick(DialogInterface dialog, int index) {
                hex = Utils.invHexMap(index + 1);
                current = new CurrentState();

                gotoReadDesc();
                dialog.dismiss();
              }
            }
        );

        dialog.show();
        break;
      case R.id.omAlgo:
        alertDialog.setMessage(Utils.s(R.string.options_algo));
        tvMessage.setText(Html.fromHtml(Utils.s(R.string.options_algo_message)));
        alertDialog.show();
        break;
      case R.id.omReferences:
        alertDialog.setMessage(Utils.s(R.string.options_references));
        tvMessage.setText(Html.fromHtml(Utils.s(R.string.options_references_message)));
        alertDialog.show();
        break;
      case R.id.omAbout:
        alertDialog.setMessage(Utils.s(R.string.options_about));
        tvMessage.setText(Html.fromHtml(Utils.s(R.string.options_about_message)));
        alertDialog.show();
        break;
      case R.id.omGoVegan:
        alertDialog.setTitle(Utils.s(R.string.options_govegan));
        tvMessage.setText(Html.fromHtml(Utils.s(R.string.options_govegan_message)));
        alertDialog.show();
        break;
      case R.id.omReadDescEdit:
        renderEditHexSection();
        break;
      case R.id.omReadDescUndo:
        renderResetHexSection();
        break;
      case R.id.omReadDescShare:
        new ShareTool(this, current).performShare();
        break;
    }
    return true;
  }

  /**
   * Update the option menu for the "About" section and other stuff
   */
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    final String dictionary = (String) getSettingsManager().get(SETTINGS_MAP.DICTIONARY);
    final MenuItem omSettings = menu.findItem(R.id.omSettings);
    omSettings.setTitle(R.string.options_settings);
    final MenuItem omViewHex = menu.findItem(R.id.omViewHex);
    if (dictionary.equals(Consts.DICTIONARY_CUSTOM)) {
      omViewHex.setTitle(R.string.options_view_edit_hex);
    } else {
      omViewHex.setTitle(R.string.options_view_hex);
    }
    final MenuItem omReferences = menu.findItem(R.id.omReferences);
    omReferences.setTitle(R.string.options_references);
    final MenuItem omAlgo = menu.findItem(R.id.omAlgo);
    omAlgo.setTitle(R.string.options_algo);
    final MenuItem omAbout = menu.findItem(R.id.omAbout);
    omAbout.setTitle(R.string.options_about);
    final MenuItem omGoVegan = menu.findItem(R.id.omGoVegan);
    omGoVegan.setTitle(R.string.options_govegan);
    return true;
  }

  /**
   * Called when the activity is restored.
   *
   * @param savedInstanceState The saved state
   */
  @Override
  public void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);

    current.viewId = savedInstanceState.getInt("currentView");
    current.question = savedInstanceState.getString("question");

    hexRow = savedInstanceState.getInt("hexRow");
    current.changing = savedInstanceState.getInt("changing");
    hex = savedInstanceState.getIntArray("hex");
    tHex = savedInstanceState.getIntArray("tHex");

    current.mode = READ_DESC_MODE.valueOf(savedInstanceState.getString("mode"));

    setCurrentSection(current, current.changing);
    setCurrentHex(hex);

    renderLoadHistory(null, null);
  }

  /**
   * Called when the activity is suspended.
   *
   * @param savedInstanceState The saved state
   */
  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    savedInstanceState.putInt("currentView", current.viewId);
    savedInstanceState.putString("question", current.question);

    savedInstanceState.putInt("hexRow", hexRow);
    savedInstanceState.putInt("changing", current.changing);
    savedInstanceState.putIntArray("hex", hex);
    savedInstanceState.putIntArray("tHex", tHex);

    READ_DESC_MODE mode = current.mode == null ? READ_DESC_MODE.ORACLE : current.mode;
    savedInstanceState.putString("mode", String.valueOf(mode));

    savedInstanceState.putBoolean(ON_RESUME_CALLED_PREFERENCE_KEY, onResumeCalledAlready);

    super.onSaveInstanceState(savedInstanceState);
  }

  /**
   * Handle permission requests
   * @param requestCode
   * @param permissions
   * @param grantResults
   */
  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    switch (requestCode) {
      default:
        break;
    }
  }

  /**
   * Allows handling external activities
   *
   * @param requestCode The ACTIVITY_RESULT_REQUEST_CODE to reply to
   * @param resultCode The external activity result code
   * @param resultData The external activity result data
   */
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
    if (requestCode == ACTIVITY_RESULT_REQUEST_CODE.BACKUP_RESTORE_OPEN_FILE.ordinal() &&
        resultCode == Activity.RESULT_OK && resultData != null) {
        DataPersister.restoreBackup(IChingActivity.this, resultData.getData());
    }
  }


  /**
   * Wrapper method to set the content view after storing it
   *
   * @param resId A layout view resource identifier
   */
  @Override
  public void setContentView(int resId) {
    current.viewId = resId;
    changeTheme(settings.get(SETTINGS_MAP.THEME));
    super.setContentView(current.viewId);
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (!onResumeCalledAlready) {
      onResumeCalledAlready = true;

      loadSettings();
      switch (current.viewId) {
        case R.layout.consult:
          gotoConsult();
          break;
        case R.layout.readdesc:
          gotoReadDesc();
          break;
        case R.layout.settings:
          gotoSettings();
          break;
        default:
          gotoMain();
      }

      onAppUpgrade();
    }
  }

  private void prepareDivinationMethod() {
    final Resources res = getResources();
    int divinationMethod = (Integer) getSettingsManager().get(SETTINGS_MAP.DIVINATION_METHOD);
    Random rnd = new Random();
    final AnimCoin coin1, coin2, coin3;
    TextView tvInstructions = (TextView) findViewById(R.id.tvInstructions);

    switch (divinationMethod) {
      case Consts.DIVINATION_METHOD_COINS_SHAKE:
        coin1 = new AnimCoin((ImageView) findViewById(R.id.picCoin01), res, rnd.nextInt(2) + 2);
        coin2 = new AnimCoin((ImageView) findViewById(R.id.picCoin02), res, rnd.nextInt(2) + 2);
        coin3 = new AnimCoin((ImageView) findViewById(R.id.picCoin03), res, rnd.nextInt(2) + 2);

        shakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {
          @Override
          public void onStartShake() {
          }

          @Override
          public void onEndShake() {
            int coinSum = coin1.getCoinValue() + coin2.getCoinValue() + coin3.getCoinValue();
            generateRow(coinSum);
          }

          @Override
          public void onShake(float xForce) {
            AnimCoin[] coins = new AnimCoin[] { coin1, coin2, coin3};
            int flips = 0;
            if (Math.abs(xForce) < 1.2f) {
              flips = 3;
            } else if (Math.abs(xForce) < 1.15f) {
              flips = 2;
            } else if (Math.abs(xForce) < 1.12f) {
              flips = 1;
            }
            for (int i = 0; i < flips; i++) {
              int index = rndGen.nextInt(3);
              coins[index].flip(res);
              coins[index].runFlipAnimation();
            }
            renderRowShake();
          }

          private void renderRowShake() {
            hex[hexRow] = coin1.getCoinValue() + coin2.getCoinValue() + coin3.getCoinValue();
            renderRow(hexRow, hex[hexRow], true, null, null);
          }

        });
        tvInstructions.setText(Utils.s(R.string.consult_shakecoins_manual));
        break;
      case Consts.DIVINATION_METHOD_COINS_MANUAL:
        coin1 = new AnimCoin((ImageView) findViewById(R.id.picCoin01), res, rnd.nextInt(2) + 2);
        coin2 = new AnimCoin((ImageView) findViewById(R.id.picCoin02), res, rnd.nextInt(2) + 2);
        coin3 = new AnimCoin((ImageView) findViewById(R.id.picCoin03), res, rnd.nextInt(2) + 2);

        OnTouchListener coinTouchListener = new OnTouchListener() {
          public boolean onTouch(View v, MotionEvent event) {
            hex[hexRow] = coin1.getCoinValue() + coin2.getCoinValue() + coin3.getCoinValue();
            renderRow(hexRow, hex[hexRow], true, null, null);
            return true;
          }
        };
        coin1.setOnTouchListener(coinTouchListener);
        coin2.setOnTouchListener(coinTouchListener);
        coin3.setOnTouchListener(coinTouchListener);
        coinTouchListener.onTouch(null, null);
        final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {

          private static final int SWIPE_MIN_DISTANCE = 120;
          private static final int SWIPE_THRESHOLD_VELOCITY = 200;

          @Override
          public boolean onDown(MotionEvent event) {
            return true;
          }

          @Override
          public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            int coinSum = coin1.getCoinValue() + coin2.getCoinValue() + coin3.getCoinValue();
            if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY &&
                hexRow < Consts.HEX_LINES_COUNT) {
              generateRow(coinSum);
              if (hexRow < Consts.HEX_LINES_COUNT) {
                hex[hexRow] = coinSum;
                renderRow(hexRow, hex[hexRow], true, null, null);
              }
            } else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
              eraseRow(coinSum);
            }
            return super.onFling(e1, e2, velocityX, velocityY);
          }
        });
        View layConsult = findViewById(R.id.layConsult);
        layConsult.setOnTouchListener(new OnTouchListener() {
          public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
          }
        });
        tvInstructions = (TextView) findViewById(R.id.tvInstructions);
        tvInstructions.setText(Utils.s(R.string.consult_tapcoins_manual));
        break;
      default:
        new AnimCoin((ImageView) findViewById(R.id.picCoin01), res);
        new AnimCoin((ImageView) findViewById(R.id.picCoin02), res);
        new AnimCoin((ImageView) findViewById(R.id.picCoin03), res);
    }
  }

  private void eraseRow(int coinSum) {
    if (hexRow > 0) {
      hex[hexRow--] = -1;
    }
    if (hexRow < Consts.HEX_LINES_COUNT) {
      hex[hexRow] = coinSum;
    }
    for (int i = 0; i < Consts.HEX_LINES_COUNT; i++) {
      renderRow(i, hex[i], true, null, null);
    }
    if (Utils.mask((Integer) settings.get(SETTINGS_MAP.HAPTIC_FEEDBACK), Consts.HAPTIC_FEEDBACK_ON_THROW_COINS)) {
      Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
      v.vibrate(300);
    }
  }

  private void generateRow(int coinsValue) {
    hex[hexRow] = coinsValue;
    renderRow(hexRow++, coinsValue, true, null, null);

    if (hexRow >= Consts.HEX_LINES_COUNT) {
      gotoConsult();
    }

    if (Utils.mask((Integer) settings.get(SETTINGS_MAP.HAPTIC_FEEDBACK), Consts.HAPTIC_FEEDBACK_ON_THROW_COINS)) {
      Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
      v.vibrate(300);
    }
  }

  /**
   * Load settings from sd card. If none is found, default apply
   */
  private void loadSettings() {
    try {
      if (settings == null) {
        settings = new SettingsManager(this);
      }
      settings.load();
      if (current.viewId != null) {
        setContentView(current.viewId);
      }
      invalidateOptionsMenu();
    } catch (FileNotFoundException e) {
      settings.resetDefaults(false);
    } catch (IOException e) {
      settings.resetDefaults(true);
      AlertDialog alertDialog = new AlertDialog.Builder(IChingActivity.this).create();
      alertDialog.setMessage(Utils.s(R.string.options_unavailable));
      alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, Utils.s(android.R.string.ok), new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
        }
      });
      alertDialog.show();
    }
  }

  /**
   *  Check App is Upgraded
   */
  private void onAppUpgrade(){
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    int versionCode = sharedPreferences.getInt(Consts.SHARED_PREF_VERSION_CODE, BuildConfig.VERSION_CODE);

    if (BuildConfig.VERSION_CODE != versionCode) {
      IncrementalUpgrades incrementalUpgrades = new IncrementalUpgrades(this);
      incrementalUpgrades.onAppUpdated(versionCode);

      sharedPreferences.edit().putInt(Consts.SHARED_PREF_VERSION_CODE, BuildConfig.VERSION_CODE).commit();
    }

  }

  /**
   * Internal method to add a tab to an host
   *
   * @param tabHost The host
   * @param tag     A tag for the tab
   * @param indId   The tab title resource identifier
   * @param resId   The content layout resource identifier
   */
  private void setupTab(TabHost tabHost, String tag, int indId, int resId) {
    tabHost.addTab(
        tabHost.newTabSpec(tag)
            .setIndicator(Utils.s(indId))
            .setContent(resId)
    );

  }

  /**
   * Unique identifiers for the context menu voices *
   */
  private class ContextMenuItem {
    private static final int HISTORY_MOVE_ENTRY = 1;
    private static final int HISTORY_DELETE_ENTRY = 2;
    private static final int HISTORY_DELETE_ALL = 3;
    private static final int HISTORY_REMOVE = 4;
    private static final int HISTORY_RENAME = 5;
  }
}