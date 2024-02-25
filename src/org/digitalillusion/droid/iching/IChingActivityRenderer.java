package org.digitalillusion.droid.iching;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ActivityInfo;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
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
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.digitalillusion.droid.iching.changinglines.ChangingLinesEvaluator;
import org.digitalillusion.droid.iching.connection.ConnectionManager;
import org.digitalillusion.droid.iching.connection.RemoteResolver;
import org.digitalillusion.droid.iching.utils.Consts;
import org.digitalillusion.droid.iching.utils.DataPersister;
import org.digitalillusion.droid.iching.utils.SettingsManager;
import org.digitalillusion.droid.iching.utils.SettingsManager.SETTINGS_MAP;
import org.digitalillusion.droid.iching.utils.Utils;
import org.digitalillusion.droid.iching.utils.lists.ExpandableDropDownListItem2Adapter;
import org.digitalillusion.droid.iching.utils.lists.HistoryEntry;
import org.digitalillusion.droid.iching.utils.lists.ListItem2Adapter;
import org.digitalillusion.droid.iching.utils.sql.HexSection;
import org.digitalillusion.droid.iching.utils.sql.HexSectionDataSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Decorator that performs the rendering duties onto the views
 *
 * @author digitalillusion
 */
public class IChingActivityRenderer extends Activity {

    /**
     * The id of the cast hexagram tab
     */
    public static final int TAB_READ_DESC_CAST_HEXAGRAM = 0;
    /**
     * The id of the cast hexagram tab
     */
    public static final int TAB_READ_DESC_CHANGING_LINES = 1;
    /**
     * The id of the cast hexagram tab
     */
    public static final int TAB_READ_DESC_TRANSFORMED_HEXAGRAM = 2;
    /**
     * Settings manager*
     */
    protected SettingsManager settings;
    /**
     * The local data source for the hexagrams sections strings *
     */
    protected HexSectionDataSource dsHexSection;
    /**
     * The current state *
     */
    protected CurrentState current;
    /**
     * The edit hexagram description dialog *
     */
    protected AlertDialog editDescDialog;
    /**
     * The history creation dialog *
     */
    protected AlertDialog newHistoryDialog;
    /**
     * The history password insertion dialog
     */
    protected AlertDialog passwordDialog;
    /**
     * The context menu selection dialog
     */
    protected AlertDialog contextSelectDialog;
    /**
     * The history password insertion dialog on cancel listener
     */
    protected OnCancelListener passwordDialogOnCancel;
    /**
     * A dialog that allows to select an item from a list
     */
    protected Dialog itemSelectDialog;
    /**
     * The options menu *
     */
    protected Menu optionsMenu;
    /**
     * Memory cache of the local history *
     */
    protected ArrayList<HistoryEntry> historyList = new ArrayList<HistoryEntry>();

    /*
     * The following are used for the shake detection
     */
    protected SensorManager sensorManager;
    protected Sensor accelerometer;
    protected ShakeDetector shakeDetector;

    /**
     * The cleanup operation after history password dialog has been cancelled
     */
    protected final Runnable DEFAULT_HISTORY_REVERT_TASK = new Runnable() {
        public void run() {
            if (DataPersister.revertSelectedHistory()) {
                renderLoadHistory(null, null);
            }
        }
    };
    /**
     * The operation for the button that cancel history password dialog
     */
    protected final OnClickListener DEFAULT_HISTORY_REVERT_DIALOG_BUTTON = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            DEFAULT_HISTORY_REVERT_TASK.run();
        }
    };
    /**
     * The connection manager *
     */
    protected ConnectionManager connectionManager = new ConnectionManager();

    /**
     * @return The currently selected hexagram
     */
    public String getCurrentHex() {
        return current.hex;
    }

    /**
     * Setter for the currently selected hex
     *
     * @param hex The currently selected hex
     */
    public void setCurrentHex(int[] hex) {
        current.hex = Utils.hexMap(hex);
    }

    /**
     * @return The currently selected section or changing line
     */
    public String getCurrentSection() {
        return current.section;
    }

    /**
     * Setter for the selected section or changing line
     *
     * @param current The current state
     * @param section The section or changing line
     */
    public void setCurrentSection(CurrentState current, Serializable section) {
        if (section.equals(ChangingLinesEvaluator.ICHING_APPLY_BOTH) ||
                section.equals(ChangingLinesEvaluator.ICHING_APPLY_CAST) ||
                section.equals(ChangingLinesEvaluator.ICHING_APPLY_MANUAL) ||
                section.equals(ChangingLinesEvaluator.ICHING_APPLY_NONE) ||
                section.equals(ChangingLinesEvaluator.ICHING_APPLY_TRANSFORMED)) {
            current.section = RemoteResolver.ICHING_REMOTE_SECTION_LINE + ((Integer) section);
        } else if (Utils.isNumeric(section)) {
            if (current.changing != ChangingLinesEvaluator.ICHING_APPLY_MANUAL &&
                    current.screen != READ_DESC_SCREEN.LINES) {
                current.changing = (Integer) section;
            }
            current.section = RemoteResolver.ICHING_REMOTE_SECTION_LINE + ((Integer) section + 1);
        } else {
            current.section = section.toString();
        }
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
                            historyName
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
                } else if (s.toString().matches(".*[:\\\\/*?|<>\\.]+.*")) {
                    etHistoryName.setError(Utils.s(R.string.validator_error_invalid_chars));
                } else {
                    etHistoryName.setError(null);
                }

                showCreateHistoryValidation(btHistoryCreate, cbHistoryPassword,
                        etHistoryName, etHistoryPassword,
                        etHistoryPasswordVerify);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
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

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
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

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
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
     * onClick handler to change read hexagram description screen
     *
     * @param view Not used
     */
    public void onClickSwitchReadDescScreen(View view) {
        if (current.tabIndex == TAB_READ_DESC_CHANGING_LINES) {
            return;
        }

        if (current.screen == READ_DESC_SCREEN.DEFAULT) {
            current.screen = READ_DESC_SCREEN.LINES;
        } else {
            current.screen = READ_DESC_SCREEN.DEFAULT;
        }
        renderReadDesc(Utils.invHexMap(Integer.parseInt(current.hex)));
    }

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState The saved state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dsHexSection = new HexSectionDataSource(this);
        current = new CurrentState();
    }

    /**
     * Called when the activity is suspended.
     *
     * @param savedInstanceState The saved state
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        dismissDialogs();
        super.onSaveInstanceState(savedInstanceState);
    }

    public void showToast(CharSequence text) {
        Toast toast = Toast.makeText(
                this,
                text,
                Toast.LENGTH_SHORT
        );
        toast.show();
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

    protected Dialog buildTrigramSelectionDialog(final CharSequence[] items, String title, final DialogInterface.OnClickListener onClick) {
        if (itemSelectDialog == null || !itemSelectDialog.isShowing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            LinearLayout lDialog = new LinearLayout(this);
            lDialog.setOrientation(LinearLayout.VERTICAL);

            float textSizeSmall = getResources().getDimensionPixelSize(R.dimen.text_size_small);
            LinearLayout lFilter = new LinearLayout(this);
            lFilter.setOrientation(LinearLayout.VERTICAL);

            LinearLayout lFilterList = new LinearLayout(this);
            lFilterList.setOrientation(LinearLayout.HORIZONTAL);
            lFilterList.setGravity(Gravity.CENTER);
            final TextView tvHexPreview = new TextView(this);
            tvHexPreview.setTypeface(Typeface.createFromAsset(getAssets(), "font/DejaVuSans.ttf"));
            tvHexPreview.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeSmall);
            tvHexPreview.setText(Utils.s(R.string.view_hex_filter_tri_all) + Utils.NEWLINE + Utils.s(R.string.view_hex_filter_tri_all));
            tvHexPreview.setLineSpacing(0, 0.85f);
            TextView tvFilterInstr = new TextView(this);
            tvFilterInstr.setText(Utils.s(R.string.view_hex_filter_instr));
            tvFilterInstr.setPadding(0, 0, 10, 0);
            tvFilterInstr.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeSmall);
            lFilterList.addView(tvFilterInstr);
            lFilterList.addView(tvHexPreview);
            lFilter.addView(lFilterList);


            LinearLayout lPickers = new LinearLayout(this);
            lPickers.setOrientation(LinearLayout.HORIZONTAL);
            lPickers.setGravity(Gravity.CENTER);
            lFilter.addView(lPickers);

            final NumberPicker npHiTri = buildTrigramFilter(true);
            final NumberPicker npLoTri = buildTrigramFilter(true);
            ArrayList<CharSequence> listItems = new ArrayList<CharSequence>();
            listItems.addAll(Arrays.asList(items));
            final ArrayAdapter<CharSequence> laItems = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_list_item_1, listItems) {
                private final ArrayAdapter<CharSequence> adapter = this;
                private final CharSequence[] arrayItems = items;
                private final Filter triFilter = new Filter() {

                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        int[] hiFilter = getTriFilter(npHiTri.getValue());
                        int[] loFilter = getTriFilter(npLoTri.getValue());
                        ArrayList<CharSequence> filtered = new ArrayList<CharSequence>();
                        for (int i = 0; i < arrayItems.length; i++) {
                            int[] hex = Utils.invHexMap(i + 1);
                            boolean match = true;
                            for (int j = 0; match && loFilter != null && j < loFilter.length; j++) {
                                match &= loFilter[j] == hex[j];
                            }
                            for (int j = 0; match && hiFilter != null && j < hiFilter.length; j++) {
                                match &= hiFilter[j] == hex[j + 3];
                            }
                            if (match) {
                                filtered.add(arrayItems[i]);
                            }
                        }
                        FilterResults results = new FilterResults();
                        results.values = filtered;
                        results.count = filtered.size();
                        return results;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        adapter.clear();
                        adapter.addAll((ArrayList<CharSequence>) results.values);
                        if (results.count > 0) {
                            notifyDataSetChanged();
                        } else {
                            notifyDataSetInvalidated();
                        }
                    }

                    private int[] getTriFilter(int index) {
                        switch (index) {
                            case 1:
                                return new int[]{Consts.ICHING_YOUNG_YANG, Consts.ICHING_YOUNG_YANG, Consts.ICHING_YOUNG_YANG};
                            case 2:
                                return new int[]{Consts.ICHING_YOUNG_YANG, Consts.ICHING_YOUNG_YANG, Consts.ICHING_YOUNG_YIN};
                            case 3:
                                return new int[]{Consts.ICHING_YOUNG_YANG, Consts.ICHING_YOUNG_YIN, Consts.ICHING_YOUNG_YANG};
                            case 4:
                                return new int[]{Consts.ICHING_YOUNG_YANG, Consts.ICHING_YOUNG_YIN, Consts.ICHING_YOUNG_YIN};
                            case 5:
                                return new int[]{Consts.ICHING_YOUNG_YIN, Consts.ICHING_YOUNG_YANG, Consts.ICHING_YOUNG_YANG};
                            case 6:
                                return new int[]{Consts.ICHING_YOUNG_YIN, Consts.ICHING_YOUNG_YANG, Consts.ICHING_YOUNG_YIN};
                            case 7:
                                return new int[]{Consts.ICHING_YOUNG_YIN, Consts.ICHING_YOUNG_YIN, Consts.ICHING_YOUNG_YANG};
                            case 8:
                                return new int[]{Consts.ICHING_YOUNG_YIN, Consts.ICHING_YOUNG_YIN, Consts.ICHING_YOUNG_YIN};
                            default:
                                return null;
                        }
                    }
                };

                @Override
                public Filter getFilter() {
                    return triFilter;
                }
            };
            NumberPicker.OnValueChangeListener lisValueChange = new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                    String upperTri = Utils.s(R.string.view_hex_filter_tri_all);
                    int hiTri = picker == npHiTri ? newVal : npHiTri.getValue();
                    int loTri = picker == npLoTri ? newVal : npLoTri.getValue();
                    if (hiTri > 0) {
                        upperTri = Utils.s(Utils.getResourceByName(R.string.class, "view_hex_filter_tri_" + (hiTri - 1)));
                    }
                    String lowerTri = Utils.s(R.string.view_hex_filter_tri_all);
                    if (loTri > 0) {
                        lowerTri = Utils.s(Utils.getResourceByName(R.string.class, "view_hex_filter_tri_" + (loTri - 1)));
                    }
                    tvHexPreview.setText(upperTri + Utils.NEWLINE + lowerTri);
                    laItems.getFilter().filter("");
                }
            };
            npHiTri.setOnValueChangedListener(lisValueChange);
            npLoTri.setOnValueChangedListener(lisValueChange);
            lPickers.addView(npHiTri);
            lPickers.addView(npLoTri);

            builder.setTitle(title);
            ListView lvHexList = new ListView(this);
            lvHexList.setAdapter(laItems);
            lvHexList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    CharSequence selected = laItems.getItem(position);
                    for (int i = 0; i < items.length; i++) {
                        if (items[i].equals(selected)) {
                            onClick.onClick(itemSelectDialog, i);
                            break;
                        }
                    }
                }
            });
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, (int) (metrics.heightPixels * 0.3));
            lvHexList.setLayoutParams(params);

            lvHexList.setLayoutParams(params);
            lDialog.addView(lvHexList);
            lDialog.addView(lFilter);
            builder.setView(lDialog);
            itemSelectDialog = builder.create();
        }
        return itemSelectDialog;
    }

    @Override
    protected void onPause() {
        super.onPause();
        dismissDialogs();
        connectionManager.cleanUp(this);
        dsHexSection.close();

        sensorManager.unregisterListener(shakeDetector);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (current == null) {
            current = new CurrentState();
        }
        dsHexSection.open();
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
                        tvEditSecHex.getText().toString()
                );

                String def;
                if (!etQuote.getText().toString().isEmpty()) {
                    def = etQuote.getText() + Utils.HEX_SECTION_QUOTE_DELIMITER + Utils.NEWLINE + etReading.getText();
                } else {
                    def = etReading.getText().toString();
                }

                String dictionary = (String) settings.get(SETTINGS_MAP.DICTIONARY);
                String lang = (String) settings.get(SETTINGS_MAP.LANGUAGE);

                RemoteResolver.resetCache(current.hex, dictionary, lang, current.section);
                dsHexSection.updateHexSection(current.hex, dictionary, lang, current.section, def);

                EditText etOutput = (EditText) findViewById(R.id.etOutput);
                etOutput.setText(RemoteResolver.getSpannedFromRemoteString(def));

                if (Utils.isDarkMode(settings)) {
                    etOutput.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
                }

                showToast(text);

                editDescDialog.dismiss();

            }
        });

        editDescDialog = editDescDialogBuilder.show();

        final TextView tvEditSecHex = (TextView) editDescView.findViewById(R.id.tvEditSecHex);
        String title = current.hex + " " + Utils.s(Utils.getResourceByName(R.string.class, "hex" + current.hex));
        if (current.section.startsWith(RemoteResolver.ICHING_REMOTE_SECTION_LINE)) {
            title += " - " + Utils.s(Utils.getResourceByName(R.string.class, "read_changing_select_" + current.section));
        } else {
            title += " - " + Utils.s(Utils.getResourceByName(R.string.class, "read_" + current.section));
        }
        tvEditSecHex.setText(title);

        String dictionary = (String) settings.get(SETTINGS_MAP.DICTIONARY);
        String lang = (String) settings.get(SETTINGS_MAP.LANGUAGE);
        HexSection section = new HexSection(Utils.EMPTY_STRING, Utils.EMPTY_STRING, Utils.EMPTY_STRING, lang, Utils.EMPTY_STRING);
        try {
            section = dsHexSection.getHexSection(current.hex, dictionary, lang, current.section);
        } catch (NotFoundException e) {
        }

        final EditText etQuote = (EditText) editDescView.findViewById(R.id.etQuote);
        etQuote.setText(section.getDefQuote());
        final EditText etReading = (EditText) editDescView.findViewById(R.id.etReading);
        etReading.setText(section.getDefReading());
    }

    protected void renderLoadHistory(final Runnable successTask, final Runnable failureTask) {
        final ListView lvHistory = (ListView) findViewById(R.id.lvHistory);
        final TextView tvHistory = (TextView) findViewById(R.id.tvHistory);
        final EditText etQuestion = (EditText) findViewById(R.id.etQuestion);
        final ExpandableListView elSelectHistory = (ExpandableListView) findViewById(R.id.elSelectHistory);
        try {
            final List<String> historyNames = DataPersister.getHistoryNames();

            if (elSelectHistory != null) {
                // Render list of histories
                elSelectHistory.setAdapter(new ExpandableDropDownListItem2Adapter<String>(this, elSelectHistory, historyNames) {
                    @Override
                    public void childViewInit(TextView childView) {
                        childView.setId(R.id.elSelectHistory);
                        registerForContextMenu(childView);

                        childView.setOnClickListener(new View.OnClickListener() {
                            @SuppressWarnings("unchecked")
                            ExpandableDropDownListItem2Adapter<String> expandibleAdapter = ((ExpandableDropDownListItem2Adapter<String>) elSelectHistory.getExpandableListAdapter());
                            final List<String> historyNamesList = expandibleAdapter.getList();

                            public void onClick(View v) {
                                String selected = ((TextView) v).getText().toString();
                                if (Utils.s(R.string.history_default).equals(selected)) {
                                    selected = DataPersister.getDefaultHistoryFilename();
                                }
                                final int childPosition = historyNamesList.indexOf(selected);
                                if (childPosition == -1) {
                                    onClickShowCreateHistory(v);
                                } else {
                                    DataPersister.setSelectedHistory(historyNamesList.get(childPosition), Utils.EMPTY_STRING, true);

                                    renderLoadHistory(
                                            () -> {
                                                String selected1 = historyNamesList.remove(childPosition);
                                                historyNamesList.add(0, selected1);
                                                elSelectHistory.collapseGroup(0);
                                                etQuestion.requestFocus();

                                                renderLoadHistory(successTask, null);
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
                        if (DataPersister.getDefaultHistoryFilename().equals(entry)) {
                            return Utils.s(R.string.history_default);
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
            }

            // Manage the load of history and all it's exceptions
            DataPersister.loadHistory(historyList);

            List<HistoryEntry> lvHistoryList = new ArrayList<>();
            for (HistoryEntry e : historyList) {
                lvHistoryList.add(HistoryEntry.from(e));
            }

            if (lvHistory != null) {
                // Render list of readings of the selected history
                lvHistory.setAdapter(new ListItem2Adapter<HistoryEntry>(this, lvHistoryList) {
                    @Override
                    public String getText1(HistoryEntry entry) {
                        return entry.getQuestion();
                    }

                    @Override
                    public String getText2(HistoryEntry entry) {
                        String template = "yyyy/MM/dd HH:mm:ss";
                        SimpleDateFormat dateFormat = new SimpleDateFormat(template, settings.getLocale());
                        if (entry.getDate() != null) {
                            return dateFormat.format(entry.getDate());
                        }
                        return null;
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

            if (tvHistory != null) {
                tvHistory.setVisibility(View.GONE);
            }
            if (etQuestion != null) {
                etQuestion.requestFocus();
            }
        } catch (IOException e) {
            // Run failure task if any
            if (failureTask != null) {
                failureTask.run();
            }

            if (tvHistory != null) {
                tvHistory.setVisibility(View.GONE);
            }
            AlertDialog alertDialog = new AlertDialog.Builder(IChingActivityRenderer.this).create();
            alertDialog.setMessage(Utils.s(R.string.history_unavailable));
            alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, Utils.s(android.R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            alertDialog.show();
        } catch (GeneralSecurityException e) {
            promptForHistoryPassword(successTask, failureTask);
            return;
        }
    }

    /**
     * Render the action buttons
     * <p/>
     * If editing an hexagram using the custom language set of definitions, enable the edit
     * and reset hexagram sections action buttons
     * Else, while viewing a reading, show action button to share content
     */
    protected void renderOptionsMenu() {
        if (optionsMenu == null) {
            return;
        }
        final MenuItem omEdit = optionsMenu.findItem(R.id.omReadDescEdit);
        final MenuItem omUndo = optionsMenu.findItem(R.id.omReadDescUndo);
        final MenuItem omShare = optionsMenu.findItem(R.id.omReadDescShare);
        if (!Utils.isDarkMode(settings)) {
            omEdit.setIcon(Utils.invertDrawable(getResources(), R.drawable.ic_action_edit));
            omUndo.setIcon(Utils.invertDrawable(getResources(), R.drawable.ic_action_undo));
            omShare.setIcon(Utils.invertDrawable(getResources(), R.drawable.ic_action_share));
        }

        final MenuItem omGoVegan = optionsMenu.findItem(R.id.omGoVegan);
        if (omEdit != null && omUndo != null && omShare != null) {
            final String dictionary = (String) getSettingsManager().get(SETTINGS_MAP.DICTIONARY);
            if (current.viewId == R.layout.readdesc || current.viewId == R.layout.editdesc) {
                if (current.mode == READ_DESC_MODE.VIEW_HEX && dictionary.equals(Consts.DICTIONARY_CUSTOM)) {
                    omEdit.setVisible(true);
                    omUndo.setVisible(true);
                    omShare.setVisible(true);
                    omGoVegan.setVisible(false);
                } else {
                    omEdit.setVisible(false);
                    omUndo.setVisible(false);
                    omShare.setVisible(true);
                    omGoVegan.setVisible(false);
                }
            } else if (current.viewId == R.layout.main) {
                omEdit.setVisible(false);
                omUndo.setVisible(false);
                omShare.setVisible(false);
                omGoVegan.setVisible(true);
            } else {
                omEdit.setVisible(false);
                omUndo.setVisible(false);
                omShare.setVisible(false);
                omGoVegan.setVisible(false);
            }
        }
    }

    /**
     * Renders a tab of the readDesc layout, given the associated hexagram
     *
     * @param hexToRender The hexagram to evaluate for changing lines
     */
    protected void renderReadDesc(final int[] hexToRender) {
        final TextView tvDescTitle = (TextView) findViewById(R.id.tvHexName);
        setCurrentHex(hexToRender);
        tvDescTitle.setText(Utils.getResourceByName(R.string.class, "hex" + current.hex));
        tvDescTitle.setText(current.hex + " " + tvDescTitle.getText());

        LinearLayout layButtonsAndChanging = (LinearLayout) findViewById(R.id.layButtonsAndChanging);
        for (int i = 0; i < layButtonsAndChanging.getChildCount(); i++) {
            layButtonsAndChanging.getChildAt(i).setVisibility(View.GONE);
        }

        renderQuestion();

        final EditText etOutput = (EditText) findViewById(R.id.etOutput);
        final Button btReadDesc = (Button) findViewById(R.id.btReadDesc);
        final Button btReadImage = (Button) findViewById(R.id.btReadImage);
        final Button btReadJudge = (Button) findViewById(R.id.btReadJudge);
        final Spinner spinner = (Spinner) findViewById(R.id.spChanging);
        final TextView tvFurtherReadDesc = (TextView) findViewById(R.id.tvFurtherReadDesc);
        if (!Utils.isDarkMode(settings)) {
            etOutput.setTextColor(getResources().getColor(android.R.color.primary_text_light));
        }
        switch (current.screen) {
            case LINES:
                final List<String> lines = new ArrayList<String>();
                for (int i = 0; i < Consts.HEX_LINES_COUNT; i++) {
                    boolean isGoverning = Utils.isGoverning(current.hex, i);
                    boolean isConstituent = Utils.isConstituent(current.hex, i);
                    if (isGoverning || isConstituent) {
                        lines.add(Utils.s(Utils.getResourceByName(R.string.class, ChangingLinesEvaluator.READ_CHANGING_SELECT_LINE + (i + 1))));
                    } else {
                        lines.add(Utils.EMPTY_STRING);
                    }
                    renderRow(i, hexToRender[i], false, isGoverning, isConstituent);
                }

                btReadDesc.setVisibility(View.GONE);
                btReadJudge.setVisibility(View.GONE);
                btReadImage.setVisibility(View.GONE);
                spinner.setVisibility(View.VISIBLE);
                tvFurtherReadDesc.setVisibility(View.VISIBLE);
                final OnItemSelectedListener onItemSelect = new OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        current.changingManualIndex = position;
                        setCurrentSection(current, current.changingManualIndex);
                        RemoteResolver.renderRemoteString(
                                etOutput,
                                new OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        renderReadDesc(hexToRender);
                                    }
                                },
                                IChingActivityRenderer.this
                        );
                    }

                    public void onNothingSelected(AdapterView<?> arg0) {
                        etOutput.setText(Utils.EMPTY_STRING);
                    }
                };
                buildChangingLineSelector(spinner, lines, onItemSelect);
                final TextView tvChanging = (TextView) findViewById(R.id.tvChanging);
                tvChanging.setVisibility(View.VISIBLE);
                tvChanging.setText(Html.fromHtml("<small>" + getChangingLinesDescription(current) + "</small>"));
                if (!Utils.isDarkMode(settings)) {
                    tvChanging.setTextColor(getResources().getColor(android.R.color.primary_text_light));
                }
                break;
            default:
                for (int i = 0; i < Consts.HEX_LINES_COUNT; i++) {
                    renderRow(i, hexToRender[i], false, null, null);
                }

                btReadDesc.setVisibility(View.VISIBLE);
                btReadJudge.setVisibility(View.VISIBLE);
                btReadImage.setVisibility(View.VISIBLE);
                tvFurtherReadDesc.setVisibility(View.VISIBLE);
                spinner.setVisibility(View.GONE);

                final OnTouchListener lisReadDesc = new OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        IChingActivityRenderer.this.setCurrentSection(current, RemoteResolver.ICHING_REMOTE_SECTION_DESC);
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
                        IChingActivityRenderer.this.setCurrentSection(current, RemoteResolver.ICHING_REMOTE_SECTION_JUDGE);
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
                        IChingActivityRenderer.this.setCurrentSection(current, RemoteResolver.ICHING_REMOTE_SECTION_IMAGE);
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
        }

        renderOptionsMenu();
    }

    /**
     * Renders the changing lines tab of the readDesc layout, given the associated hexagram
     *
     * @param hexToRender The hexagram to evaluate for changing lines
     */
    protected void renderReadDescChanging(final int[] hexToRender) {
        final TextView tvDescTitle = (TextView) findViewById(R.id.tvHexName);
        setCurrentHex(hexToRender);
        tvDescTitle.setText(Utils.s(R.string.read_changing));

        LinearLayout layButtonsAndChanging = (LinearLayout) findViewById(R.id.layButtonsAndChanging);
        for (int i = 0; i < layButtonsAndChanging.getChildCount(); i++) {
            layButtonsAndChanging.getChildAt(i).setVisibility(View.GONE);
        }
        final TextView tvFurtherReadDesc = (TextView) findViewById(R.id.tvFurtherReadDesc);
        tvFurtherReadDesc.setVisibility(View.GONE);

        renderQuestion();

        READ_DESC_MODE mode = current.mode;
        if (current.changing == ChangingLinesEvaluator.ICHING_APPLY_MANUAL) {
            // Force manual selection of changing lines
            mode = READ_DESC_MODE.VIEW_HEX;
        }
        renderReadDescChangingHex(hexToRender);


        final EditText etOutput = (EditText) findViewById(R.id.etOutput);
        final Spinner spinner = (Spinner) findViewById(R.id.spChanging);
        etOutput.setText(Utils.EMPTY_STRING);
        if (!Utils.isDarkMode(settings)) {
            etOutput.setTextColor(getResources().getColor(android.R.color.primary_text_light));
        }
        switch (mode) {
            case ORACLE:
                setCurrentSection(current, current.changing);
                OnClickListener retryAction = new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        renderReadDescChanging(hexToRender);
                    }
                };
                prepareReadingDescription(etOutput, retryAction, current);
                break;
            case VIEW_HEX:

                ArrayList<String> lines = new ArrayList();
                ArrayList<Integer> lines_index = new ArrayList();
                int evalType = (Integer) settings.get(SETTINGS_MAP.CHANGING_LINES_EVALUATOR);
                if (evalType == Consts.EVALUATOR_CHANGING) {
                    if (current.changing == ChangingLinesEvaluator.ICHING_APPLY_CAST || ChangingLinesEvaluator.isChangingLine(hexToRender[0])) {
                        lines.add(Utils.s(R.string.read_changing_select_line1));
                        lines_index.add(0);
                    }
                    if (current.changing == ChangingLinesEvaluator.ICHING_APPLY_CAST || ChangingLinesEvaluator.isChangingLine(hexToRender[1])) {
                        lines.add(Utils.s(R.string.read_changing_select_line2));
                        lines_index.add(1);
                    }
                    if (current.changing == ChangingLinesEvaluator.ICHING_APPLY_CAST || ChangingLinesEvaluator.isChangingLine(hexToRender[2])) {
                        lines.add(Utils.s(R.string.read_changing_select_line3));
                        lines_index.add(2);
                    }
                    if (current.changing == ChangingLinesEvaluator.ICHING_APPLY_CAST || ChangingLinesEvaluator.isChangingLine(hexToRender[3])) {
                        lines.add(Utils.s(R.string.read_changing_select_line4));
                        lines_index.add(3);
                    }
                    if (current.changing == ChangingLinesEvaluator.ICHING_APPLY_CAST || ChangingLinesEvaluator.isChangingLine(hexToRender[4])) {
                        lines.add(Utils.s(R.string.read_changing_select_line5));
                        lines_index.add(4);
                    }
                    if (current.changing == ChangingLinesEvaluator.ICHING_APPLY_CAST || ChangingLinesEvaluator.isChangingLine(hexToRender[5])) {
                        lines.add(Utils.s(R.string.read_changing_select_line6));
                        lines_index.add(5);
                    }
                } else {
                    lines.add(Utils.s(R.string.read_changing_select_line1));
                    lines.add(Utils.s(R.string.read_changing_select_line2));
                    lines.add(Utils.s(R.string.read_changing_select_line3));
                    lines.add(Utils.s(R.string.read_changing_select_line4));
                    lines.add(Utils.s(R.string.read_changing_select_line5));
                    lines.add(Utils.s(R.string.read_changing_select_line6));
                    lines_index.addAll(Arrays.asList(new Integer [] {0, 1, 2, 3, 4, 5}));
                }
                final OnItemSelectedListener onItemSelect = new OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        current.changingManualIndex = (position + 1 > Consts.HEX_LINES_COUNT) ? ChangingLinesEvaluator.ICHING_APPLY_BOTH : lines_index.get(position);
                        setCurrentSection(current, current.changingManualIndex);
                        renderReadDescChangingHex(hexToRender);
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
                };

                int hexId = Integer.parseInt(current.hex);
                if (Arrays.binarySearch(ChangingLinesEvaluator.ICHING_ALL_LINES_DESC, hexId) >= 0) {
                    lines.add(Utils.s(R.string.read_changing_select_all));
                }
                buildChangingLineSelector(spinner, lines, onItemSelect);

                break;
        }

        final TextView tvChanging = (TextView) findViewById(R.id.tvChanging);
        tvChanging.setVisibility(View.VISIBLE);
        if (!Utils.isDarkMode(settings)) {
            tvChanging.setTextColor(getResources().getColor(android.R.color.primary_text_light));
        }
        tvChanging.setText(Html.fromHtml("<small>" + getChangingLinesDescription(current) + "</small>"));

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
                SettingsManager settings = IChingActivityRenderer.this.getSettingsManager();
                final String dictionary = (String) settings.get(SETTINGS_MAP.DICTIONARY);
                final String lang = (String) settings.get(SETTINGS_MAP.LANGUAGE);
                RemoteResolver.resetCache(current.hex, dictionary, lang);
                getHexSectionDataSource().deleteHexSections(current.hex, dictionary, lang);
                EditText etOutput = (EditText) findViewById(R.id.etOutput);
                etOutput.setText(Utils.EMPTY_STRING);

                CharSequence text = Utils.s(
                        R.string.edit_section_reset,
                        Utils.s(Utils.getResourceByName(R.string.class, "hex" + current.hex))
                );

                showToast(text);
            }
        });
        resetConfirmDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        resetConfirmDialog.show();
    }

    void prepareReadingDescription(final EditText etOutput,
                                   DialogInterface.OnClickListener retryAction,
                                   IChingActivityRenderer.CurrentState current) {
        if (current.changing == ChangingLinesEvaluator.ICHING_APPLY_BOTH) {
            int intMap = Integer.parseInt(current.hex);
            if (Arrays.binarySearch(ChangingLinesEvaluator.ICHING_ALL_LINES_DESC, intMap) >= 0) {
                RemoteResolver.renderRemoteString(etOutput, retryAction, this, current.hex, current.section);
            }
        } else if (current.changing != ChangingLinesEvaluator.ICHING_APPLY_CAST &&
                current.changing != ChangingLinesEvaluator.ICHING_APPLY_TRANSFORMED &&
                current.changing != ChangingLinesEvaluator.ICHING_APPLY_NONE) {
            RemoteResolver.renderRemoteString(etOutput, retryAction, this, current.hex, current.section);
        }
    }

    /**
     * Render a row of the hex
     *
     * @param index             the row index from 0 (first) to 6 (last)
     * @param coinsValue        The value of the coins for this row
     * @param renderMobileLines True to show mobile lines, false to render them fixed
     * @param governingLine     True to render as governing line
     * @param constituentLine   True to render as constituent line
     */
    protected void renderRow(int index, int coinsValue, boolean renderMobileLines, Boolean governingLine, Boolean constituentLine) {
        View row = null;
        switch (index) {
            case 0:
                row = findViewById(R.id.hexRow1);
                break;
            case 1:
                row = findViewById(R.id.hexRow2);
                break;
            case 2:
                row = findViewById(R.id.hexRow3);
                break;
            case 3:
                row = findViewById(R.id.hexRow4);
                break;
            case 4:
                row = findViewById(R.id.hexRow5);
                break;
            case 5:
                row = findViewById(R.id.hexRow6);
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
            default:
                lineRes = R.drawable.empty;
        }

        if (!renderMobileLines) {
            if (coinsValue == Consts.ICHING_OLD_YIN) {
                lineRes = R.drawable.yin;
            } else if (coinsValue == Consts.ICHING_OLD_YANG) {
                lineRes = R.drawable.yang;
            }
        }

        if (row instanceof TableRow) {
            if (!Utils.isDarkMode(settings)) {
                ((TableRow) row).setBackgroundDrawable(Utils.invertDrawable(getResources(), lineRes));
            } else {
                ((TableRow) row).setBackgroundResource(lineRes);
            }
        } else if (row instanceof TextView) {
            TextView tvRow = (TextView) row;
            int padding = (int) getResources().getDimensionPixelSize(R.dimen.text_size_medium);
            int width = (int) getResources().getDimension(R.dimen.hex_small_width) - padding;
            int height = (int) getResources().getDimension(R.dimen.hex_small_row_height);
            Bitmap bMap = BitmapFactory.decodeResource(getResources(), lineRes);
            Bitmap bMapScaled = Bitmap.createScaledBitmap(bMap, width, height, true);
            if (!Utils.isDarkMode(settings)) {
                bMapScaled = Utils.invert(bMapScaled);
                tvRow.setTextColor(getResources().getColor(android.R.color.primary_text_light));
            }
            Drawable drawable = new BitmapDrawable(getResources(), bMapScaled);
            tvRow.setCompoundDrawablesWithIntrinsicBounds(
                    null, null, drawable, null
            );
            tvRow.setText(" ");
            if (governingLine != null || constituentLine != null) {
                if (governingLine) {
                    tvRow.setText(Utils.s(R.string.view_hex_line_governing));
                } else if (constituentLine) {
                    tvRow.setText(Utils.s(R.string.view_hex_line_constituent));
                }
            }
        }
    }

    protected void renderTabs(final TabHost tabHost) {
        // Restyle tabs
        TabWidget tabWidget = tabHost.getTabWidget();
        for (int i = 0; i < tabWidget.getChildCount(); i++) {
            View child = tabWidget.getChildAt(i);
            TextView title = (TextView) child.findViewById(android.R.id.title);
            float textSizeTabs = getResources().getDimensionPixelSize(R.dimen.text_size_tabs);
            title.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeTabs);
            title.setText(title.getText().toString().toUpperCase(settings.getLocale()));
            title.setSingleLine();

            child.getLayoutParams().height = (int) (textSizeTabs * 3);
            child.getLayoutParams().width = ((View) tabHost.getParent()).getWidth() / tabWidget.getChildCount();

            child.setPadding(3, 0, 3, 0);
        }
    }

    protected void setScreenOrientation() {
        Serializable screenOrientation = settings.get(SETTINGS_MAP.SCREEN_ORIENTATION);
        if (Consts.SCREEN_ORIENTATION_ROTATE.equals(screenOrientation)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        } else if (Consts.SCREEN_ORIENTATION_LANDSCAPE.equals(screenOrientation)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else if (Consts.SCREEN_ORIENTATION_PORTRAIT.equals(screenOrientation)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }
    }

    private void buildChangingLineSelector(Spinner spinner, final List<String> lines, final OnItemSelectedListener onItemSelect) {
        List<String> adapterLines = new ArrayList<String>(lines);
        adapterLines.removeAll(Collections.singleton(null));
        adapterLines.removeAll(Collections.singleton(Utils.EMPTY_STRING));
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                adapterLines.toArray(new String[adapterLines.size()])
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.text_size_small));
                if (Utils.isDarkMode(settings)) {
                    view.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
                }
                return view;
            }
        };
        spinner.setAdapter(adapter);
        spinner.setVisibility(View.VISIBLE);
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                position = lines.indexOf(adapter.getItem(position));
                onItemSelect.onItemSelected(parent, view, position, id);
            }

            public void onNothingSelected(AdapterView<?> parent) {
                onItemSelect.onNothingSelected(parent);
            }
        });
        if (current.changingManualIndex >= 0 && current.changingManualIndex < adapterLines.size()) {
            spinner.setSelection(current.changingManualIndex);
        }
    }

    ChangingLinesEvaluator getChangingLinesEvaluator() {
        Integer evalType = (Integer) settings.get(SETTINGS_MAP.CHANGING_LINES_EVALUATOR);
        ChangingLinesEvaluator changingLinesEvaluator = ChangingLinesEvaluator.produce(evalType);
        return changingLinesEvaluator;
    }

    String getChangingLinesDescription(IChingActivityRenderer.CurrentState state) {
        String desc = Utils.EMPTY_STRING;

        if (IChingActivityRenderer.TAB_READ_DESC_CHANGING_LINES != state.tabIndex &&
                READ_DESC_SCREEN.LINES == state.screen) {
            desc = Utils.s(R.string.view_hex_line_gov_legend) + "<br/>" +
                    Utils.s(R.string.view_hex_line_const_legend) + "<br/>" +
                    Utils.s(R.string.read_changing_select) + "<br/>";
        } else {
            switch (state.mode) {
                case VIEW_HEX:
                    desc += Utils.s(R.string.read_changing_select) + "<br/>";
                    break;
                case ORACLE:
                    if (state.changingCount == 0) {
                        desc = Utils.s(R.string.read_changing_none) + "<br/>";
                    } else {
                        int resId = state.changingCount == 1 ? R.string.read_changing_one : R.string.read_changing_count;
                        desc = Utils.s(resId, state.changingCount) + Utils.COLUMNS + "<br/>";
                    }
                    desc += getChangingLinesDescriptionApply(state.changing);
                    break;
            }
        }
        return desc;
    }

    String getChangingLinesDescriptionApply(int changing) {
        String desc = Utils.EMPTY_STRING;
        switch (changing) {
            case ChangingLinesEvaluator.ICHING_APPLY_BOTH:
                desc += "<em>" + Utils.s(R.string.read_changing_apply_ht) + "</em>";
                break;
            case ChangingLinesEvaluator.ICHING_APPLY_CAST:
                desc += "<em>" + Utils.s(R.string.read_changing_apply_h) + "</em>";
                break;
            case ChangingLinesEvaluator.ICHING_APPLY_TRANSFORMED:
                desc += "<em>" + Utils.s(R.string.read_changing_apply_t) + "</em>";
                break;
            case ChangingLinesEvaluator.ICHING_APPLY_NONE:
                desc += "<em>" + Utils.s(R.string.read_changing_apply_n) + "</em>";
                break;
            default:
                if (changing >= 0) {
                    desc += "<em>" + Utils.s(R.string.read_changing_apply, changing + 1) + "</em>";
                }
        }
        return desc;
    }

    private void promptForHistoryPassword(final Runnable successTask, final Runnable failureTask) {
        if (passwordDialog == null || !passwordDialog.isShowing()) {
            passwordDialog = new AlertDialog.Builder(IChingActivityRenderer.this).create();
            final EditText input = new EditText(IChingActivityRenderer.this);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
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

    private void renderQuestion() {
        final TextView tvQuestion = (TextView) findViewById(R.id.tvQuestionReadDesc);
        if (!Utils.isDarkMode(settings)) {
            tvQuestion.setTextColor(getResources().getColor(android.R.color.primary_text_light));
        }
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

    private NumberPicker buildTrigramFilter(boolean lowHiFlag) {
        String[] filters = new String[]{
                Utils.s(R.string.view_hex_filter_none),
                Utils.s(R.string.view_hex_filter_heaven),
                Utils.s(R.string.view_hex_filter_lake),
                Utils.s(R.string.view_hex_filter_fire),
                Utils.s(R.string.view_hex_filter_thunder),
                Utils.s(R.string.view_hex_filter_wind),
                Utils.s(R.string.view_hex_filter_water),
                Utils.s(R.string.view_hex_filter_mountain),
                Utils.s(R.string.view_hex_filter_earth),
        };

        NumberPicker triFilter = new NumberPicker(this);
        triFilter.setMinValue(0);
        triFilter.setMaxValue(8);
        triFilter.setOrientation(NumberPicker.HORIZONTAL);
        triFilter.setDisplayedValues(filters);
        triFilter.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        return triFilter;
    }

    private void dismissDialogs() {
        // Last chance to dismiss dialogs even if they are no longer showing
        if (passwordDialog != null && passwordDialog.isShowing()) {
            // Immediate call to revert potentially undergoing operations in popups and prevent data loss
            passwordDialogOnCancel.onCancel(passwordDialog);
            passwordDialog.dismiss();
        }

        if (newHistoryDialog != null && newHistoryDialog.isShowing()) {
            newHistoryDialog.dismiss();
        }
        if (editDescDialog != null && editDescDialog.isShowing()) {
            editDescDialog.dismiss();
        }
        if (itemSelectDialog != null && itemSelectDialog.isShowing()) {
            itemSelectDialog.dismiss();
        }
        if (contextSelectDialog != null && contextSelectDialog.isShowing()) {
            contextSelectDialog.dismiss();
        }
        RemoteResolver.dismissProgressDialog();
    }

    private void renderReadDescChangingHex(int[] hexToRender) {
        // Update changing count and consultation mode before getting changing lines
        // description
        current.changingCount = 0;
        for (int i = 0; i < Consts.HEX_LINES_COUNT; i++) {
            if (ChangingLinesEvaluator.isChangingLine(hexToRender[i])) {
                current.changingCount++;
            }
            if (current.mode == READ_DESC_MODE.VIEW_HEX &&
                    (current.changingManualIndex == i || current.changingManualIndex == ChangingLinesEvaluator.ICHING_APPLY_BOTH)) {
                // Draw the manually selected line as changing
                renderRow(i, ChangingLinesEvaluator.getChangingLineOf(hexToRender[i]), true, null, null);
            } else {
                renderRow(i, hexToRender[i], true, null, null);
            }
        }
    }

    /**
     * The consultation mode, being using the oracle or reading the book *
     */
    protected enum READ_DESC_MODE {
        ORACLE,
        VIEW_HEX
    }

    /**
     * The read hexagram description screens
     */
    protected enum READ_DESC_SCREEN {
        DEFAULT,
        LINES
    }

    public enum ACTIVITY_RESULT_REQUEST_CODE {
        BACKUP_RESTORE_OPEN_FILE
    }

    /**
     * Data object representing the current state *
     */
    public static class CurrentState {
        /**
         * The user question *
         */
        public String question;
        /**
         * The changing line index *
         */
        public int changing;
        /**
         * The currently selected section or changing line *
         */
        public String section;
        /**
         * The currently selected hexagram *
         */
        public String hex;
        /**
         * The currently selected consultation mode *
         */
        public READ_DESC_MODE mode;
        /**
         * The currently selected consultation screen
         */
        public READ_DESC_SCREEN screen;
        /**
         * The currently selected tab index*
         */
        public int tabIndex;
        /**
         * The changing line count *
         */
        public int changingCount;
        /**
         * The changing line index in manual mode *
         */
        public int changingManualIndex;
        /**
         * The current View *
         */
        public Integer viewId;


        public CurrentState() {
            tabIndex = 0;
            question = Utils.EMPTY_STRING;
            mode = READ_DESC_MODE.VIEW_HEX;
            screen = READ_DESC_SCREEN.DEFAULT;
            changingManualIndex = 0;
            viewId = R.layout.main;
        }
    }
}
