package org.digitalillusion.droid.iching;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.Html;
import android.widget.Button;
import android.widget.EditText;

import org.digitalillusion.droid.iching.changinglines.ChangingLinesEvaluator;
import org.digitalillusion.droid.iching.connection.RemoteResolver;
import org.digitalillusion.droid.iching.utils.Consts;
import org.digitalillusion.droid.iching.utils.SettingsManager;
import org.digitalillusion.droid.iching.utils.Utils;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Implementation of the "share" feature
 *
 * @author digitalillusion
 */
public class ShareTool {

  private final IChingActivity activity;
  private final IChingActivityRenderer.CurrentState current;

  ShareTool(IChingActivity activity, IChingActivityRenderer.CurrentState current) {
    this.activity = activity;
    this.current = current;
  }

  public synchronized void performShare() {
    final DialogInterface.OnClickListener retryAction = new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        performShare();
      }
    };

    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
    sharingIntent.setType("text/html");

    String shareContent = null;
    IChingActivityRenderer.CurrentState state = new IChingActivityRenderer.CurrentState();
    state.tabIndex = current.tabIndex;
    state.mode = current.mode;
    state.changing = activity.getChangingLinesEvaluator().evaluate(activity.hex, activity.tHex);
    state.changingCount = current.mode == IChingActivityRenderer.READ_DESC_MODE.VIEW_HEX ? 1 : current.changingCount;
    state.screen = IChingActivityRenderer.READ_DESC_SCREEN.DEFAULT;
    if (state.tabIndex == IChingActivityRenderer.TAB_READ_DESC_TRANSFORMED_HEXAGRAM) {
      state.hex = Utils.hexMap(activity.tHex);
    } else {
      state.hex = Utils.hexMap(activity.hex);
    }

    String hexHeader = "";
    if (state.tabIndex == IChingActivityRenderer.TAB_READ_DESC_CHANGING_LINES &&
        state.mode == IChingActivityRenderer.READ_DESC_MODE.VIEW_HEX) {
      hexHeader = produceHexagramHeader(state, "");
    }
    Serializable mode = activity.getSettingsManager().get(SettingsManager.SETTINGS_MAP.SHARE);
    if (Consts.SHARE_PAGE.equals(mode)) {
      state.screen = current.screen;
      state.changing = current.changing;
      state.changingManualIndex = current.changingManualIndex;
      if (state.changing == ChangingLinesEvaluator.ICHING_APPLY_MANUAL) {
        state.changing = state.changingManualIndex;
      }
      state.section = current.section;
      shareContent = performSharePage(state, retryAction);
    } else if (Consts.SHARE_HEXAGRAM.equals(mode)) {
      shareContent = performShareHex(state, retryAction);
    } else if (Consts.SHARE_READING.equals(mode)) {
      hexHeader = "";
      shareContent = performShareReading(state, retryAction);
    }

    // Question
    final String title = "<h1>" + current.question + "</h1>";
    if (shareContent != null) {
      sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, Html.fromHtml(title + hexHeader + shareContent));
      activity.startActivity(Intent.createChooser(sharingIntent, Utils.s(R.string.read_share_using)));
    } else {
      final AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
      alertDialog.setMessage(Utils.s(R.string.read_share_no_content));
      alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, Utils.s(android.R.string.ok), new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          alertDialog.dismiss();
        }
      });
      alertDialog.show();
    }
  }

  private String produceHeader(IChingActivityRenderer.CurrentState state) {
    String header = null;
    final boolean viewHexChangingLines = state.mode == IChingActivityRenderer.READ_DESC_MODE.VIEW_HEX &&
        state.tabIndex == IChingActivityRenderer.TAB_READ_DESC_CHANGING_LINES;
    if (state.mode == IChingActivityRenderer.READ_DESC_MODE.ORACLE ||
        viewHexChangingLines) {
      switch (state.tabIndex) {
        case IChingActivityRenderer.TAB_READ_DESC_CAST_HEXAGRAM:
          header = Utils.s(R.string.read_cast);
          break;
        case IChingActivityRenderer.TAB_READ_DESC_CHANGING_LINES:
          header = Utils.s(R.string.read_changing);
          break;
        case IChingActivityRenderer.TAB_READ_DESC_TRANSFORMED_HEXAGRAM:
          header = Utils.s(R.string.read_transformed);
          break;
      }
      header += Utils.COLUMNS + "<br/>";
    } else {
      header = Utils.EMPTY_STRING;
    }

    if (viewHexChangingLines) {
      return "<h3>" + header + "</h3>";
    } else {
      // Hexagram
      final String hexagram = produceHexagramHeader(state, header);
      return hexagram;
    }
  }

  private String producePage(IChingActivityRenderer.CurrentState state, DialogInterface.OnClickListener retryAction) {
    final EditText fakeEditText = new EditText(activity.getApplicationContext());

    switch (state.tabIndex) {
      case IChingActivityRenderer.TAB_READ_DESC_CAST_HEXAGRAM:
        RemoteResolver.renderRemoteString(
            fakeEditText,
            retryAction,
            activity,
            state.hex,
            state.section
        );
        break;
      case IChingActivityRenderer.TAB_READ_DESC_CHANGING_LINES:
        activity.prepareReadingDescription(fakeEditText, retryAction, state);
        break;
      case IChingActivityRenderer.TAB_READ_DESC_TRANSFORMED_HEXAGRAM:
        RemoteResolver.renderRemoteString(
            fakeEditText,
            retryAction,
            activity,
            state.hex,
            state.section
        );
        break;
    }

    // Section
    String changingText = Utils.EMPTY_STRING;
    if (state.section.startsWith(RemoteResolver.ICHING_REMOTE_SECTION_LINE)) {
      if (IChingActivityRenderer.TAB_READ_DESC_CHANGING_LINES != state.tabIndex &&
          IChingActivityRenderer.READ_DESC_SCREEN.LINES == state.screen) {
        if (Utils.isConstituent(state.hex, state.changingManualIndex)) {
          changingText = Utils.s(R.string.read_share_constituent_line);
        } else if (Utils.isGoverning(state.hex, state.changingManualIndex)) {
          changingText = Utils.s(R.string.read_share_governing_line);
        }
      } else {
        if (state.mode == IChingActivityRenderer.READ_DESC_MODE.ORACLE) {
          changingText = activity.getChangingLinesDescription(state);
        }
      }
    } else if (state.section.equals(RemoteResolver.ICHING_REMOTE_SECTION_DESC)) {
      final Button button = (Button) activity.findViewById(R.id.btReadDesc);
      changingText = button.getText().toString();
    } else if (state.section.equals(RemoteResolver.ICHING_REMOTE_SECTION_JUDGE)) {
      final Button button = (Button) activity.findViewById(R.id.btReadJudge);
      changingText = button.getText().toString();
    } else if (state.section.equals(RemoteResolver.ICHING_REMOTE_SECTION_IMAGE)) {
      final Button button = (Button) activity.findViewById(R.id.btReadImage);
      changingText = button.getText().toString();
    }
    final String em = "<strong>" + changingText + "</strong>";

    Editable remoteText = fakeEditText.getText();
    if (state.section.startsWith(RemoteResolver.ICHING_REMOTE_SECTION_LINE) &&
        state.tabIndex == IChingActivityRenderer.TAB_READ_DESC_CHANGING_LINES && state.changingCount == 0) {
      return em;
    }
    if (remoteText.length() == 0 ) {
      return null;
    }
    // Content
    final String content = "<p>" + Html.toHtml(fakeEditText.getText()) + "</p>";
    return em + content;
  }

  private String performSharePage(IChingActivityRenderer.CurrentState state, DialogInterface.OnClickListener retryAction) {
    // Header
    String header = produceHeader(state);
    // Page
    String page = producePage(state, retryAction);
    if (page == null) {
      return null;
    }
    return header + page;
  }

  private String produceHexagramHeader(IChingActivityRenderer.CurrentState state, String header) {
    return "<h3>" + header +
        state.hex + " " +
        Utils.s(Utils.getResourceByName(R.string.class, "hex" + state.hex)) +
        "</h3>";
  }

  private String performShareHex(IChingActivityRenderer.CurrentState state, DialogInterface.OnClickListener retryAction) {
    // Header
    String header = produceHeader(state);
    // Page
    if (state.tabIndex == IChingActivityRenderer.TAB_READ_DESC_CHANGING_LINES) {
      return produceChangingLinesPage(state, retryAction, header);
    } else {
      state.section = RemoteResolver.ICHING_REMOTE_SECTION_DESC;
      // Desc
      String desc = producePage(state, retryAction);
      if (desc == null) {
        return null;
      }
      // Judgement
      state.section = RemoteResolver.ICHING_REMOTE_SECTION_JUDGE;
      String judgement = producePage(state, retryAction);
      if (judgement == null) {
        return null;
      }
      // Image
      state.section = RemoteResolver.ICHING_REMOTE_SECTION_IMAGE;
      String image = producePage(state, retryAction);
      if (image == null) {
        return null;
      }
      return header + desc + judgement + image;
    }
  }

  private String produceChangingLinesPage(IChingActivityRenderer.CurrentState state, DialogInterface.OnClickListener retryAction, String header) {
    if (state.mode == IChingActivityRenderer.READ_DESC_MODE.ORACLE) {
      // Current changing line
      if (state.changingCount == 0) {
        state.section = RemoteResolver.ICHING_REMOTE_SECTION_LINE + state.changing;
      } else {
        String section = current.section.replaceAll("[a-z]", Utils.EMPTY_STRING);
        int currentLineIndex = state.changing < 0 ? current.changingManualIndex : state.changing;
        int lineIndex = Integer.parseInt(section.isEmpty() ? String.valueOf(currentLineIndex + 1) : section);
        state.changing = lineIndex - 1;
        state.section = RemoteResolver.ICHING_REMOTE_SECTION_LINE + lineIndex;
      }
      String page = producePage(state, retryAction);
      if (page == null) {
        return null;
      }
      return header + page;
    } else {
      // All changing lines
      String lines = "";
      for (int lineIndex = 1; lineIndex <= Consts.HEX_LINES_COUNT; lineIndex++) {
        state.changing = lineIndex - 1;
        state.section = RemoteResolver.ICHING_REMOTE_SECTION_LINE + lineIndex;
        String page = producePage(state, retryAction);
        if (page == null) {
          return null;
        } else {
          lines += page;
        }
      }
      if (Arrays.binarySearch(ChangingLinesEvaluator.ICHING_ALL_LINES_DESC, Integer.parseInt(state.hex)) >= 0) {
        state.changing = ChangingLinesEvaluator.ICHING_APPLY_BOTH;
        state.section = RemoteResolver.ICHING_REMOTE_SECTION_LINE + ChangingLinesEvaluator.ICHING_APPLY_BOTH;
        String page = producePage(state, retryAction);
        if (page == null) {
          return null;
        } else {
          lines += page;
        }
      }
      return header + lines;
    }
  }

  private String performShareReading(IChingActivityRenderer.CurrentState state, DialogInterface.OnClickListener retryAction) {
    // Cast page
    state.tabIndex = IChingActivityRenderer.TAB_READ_DESC_CAST_HEXAGRAM;
    state.hex = Utils.hexMap(activity.hex);
    String cast = performShareHex(state, retryAction);
    if (cast == null) {
      return null;
    }

    state.tabIndex = IChingActivityRenderer.TAB_READ_DESC_CHANGING_LINES;
    // Changing Lines page
    String changingLines = performShareHex(state, retryAction);
    if (changingLines == null) {
      return null;
    }

    if (current.mode != IChingActivityRenderer.READ_DESC_MODE.VIEW_HEX && state.changingCount > 0) {
      state.tabIndex = IChingActivityRenderer.TAB_READ_DESC_TRANSFORMED_HEXAGRAM;
      state.hex = Utils.hexMap(activity.tHex);
      // Transformed page
      String transformed = performShareHex(state, retryAction);
      if (transformed == null) {
        return null;
      }
      return cast + changingLines + transformed;
    }
    return cast + changingLines;
  }

}
