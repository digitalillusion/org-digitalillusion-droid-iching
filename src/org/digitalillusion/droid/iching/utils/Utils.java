package org.digitalillusion.droid.iching.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.util.Log;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.digitalillusion.droid.iching.changinglines.ChangingLinesEvaluator;
import org.digitalillusion.droid.iching.utils.lists.HistoryEntry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Commonly used functions
 *
 * @author digitalillusion
 */
public class Utils {

  /**
   * System newline character *
   */
  public static final String NEWLINE = System.getProperty("line.separator");
  /**
   * Empty String *
   */
  public static final String EMPTY_STRING = "";
  /**
   * Columns *
   */
  public static final String COLUMNS = ":";
  /**
   * Underscore *
   */
  public static final String UNDERSCORE = "_";
  /**
   * Delimiter of the quote in hexagram sections definition *
   */
  public static final String HEX_SECTION_QUOTE_DELIMITER = "\\e";
  private static Context context;

  public static HistoryEntry buildDummyHistoryEntry() {
    HistoryEntry entry = new HistoryEntry();
    entry.setChanging(-1);
    return entry;
  }

  public static void copy(File src, File dst) throws IOException {
    InputStream in = new FileInputStream(src);
    OutputStream out = new FileOutputStream(dst);

    // Transfer bytes from in to out
    byte[] buf = new byte[1024];
    int len;
    while ((len = in.read(buf)) > 0) {
      out.write(buf, 0, len);
    }
    in.close();
    out.close();
  }

  public static InputStream downloadUrl(String url, String[]... params) throws IOException {
    HttpURLConnection con = null;
    InputStream is = null;

    List<BasicNameValuePair> pairs = new ArrayList<BasicNameValuePair>();
    for (String[] p : params) {
      pairs.add(new BasicNameValuePair(p[0], p[1]));
    }

    String queryString = URLEncodedUtils.format(pairs, "utf-8");
    url += (!url.endsWith("?") ? "?" : EMPTY_STRING) + queryString;

    con = (HttpURLConnection) new URL(url).openConnection();
    con.setReadTimeout(3000);
    con.setConnectTimeout(10000);
    con.setRequestMethod("GET");
    con.setDoInput(true);

    con.connect();
    is = con.getInputStream();
    return is;
  }

  public static byte[] getBytes(InputStream is) throws IOException {

    int len;
    int size = 1024;
    byte[] buf;

    if (is instanceof ByteArrayInputStream) {
      size = is.available();
      buf = new byte[size];
      len = is.read(buf, 0, size);
    } else {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      buf = new byte[size];
      while ((len = is.read(buf, 0, size)) != -1) {
        bos.write(buf, 0, len);
      }
      buf = bos.toByteArray();
    }
    return buf;
  }

  /**
   * Finds the resource ID for the current application's resources.
   *
   * @param Rclass Resource class to find resource in.
   *               Example: R.string.class, R.layout.class, R.drawable.class
   * @param name   Name of the resource to search for.
   * @return The id of the resource or -1 if not found.
   */
  public static int getResourceByName(Class<?> Rclass, String name) {
    int id = -1;
    try {
      if (Rclass != null) {
        final Field field = Rclass.getField(name);
        if (field != null)
          id = field.getInt(null);
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
    return id;
  }

  /**
   * @param hex an hexagram
   * @return The map of the hex value to a cardinal index
   */
  public static String hexMap(int[] hex) {
    int value = 0;
    for (int i = 0; i < Consts.HEX_LINES_COUNT; i++) {
      value += (Math.pow(2, i)) * (hex[i] % 2);
    }
    switch (value) {
      case 0:
        return "02";
      case 1:
        return "24";
      case 2:
        return "07";
      case 3:
        return "19";
      case 4:
        return "15";
      case 5:
        return "36";
      case 6:
        return "46";
      case 7:
        return "11";
      case 8:
        return "16";
      case 9:
        return "51";
      case 10:
        return "40";
      case 11:
        return "54";
      case 12:
        return "62";
      case 13:
        return "55";
      case 14:
        return "32";
      case 15:
        return "34";
      case 16:
        return "08";
      case 17:
        return "03";
      case 18:
        return "29";
      case 19:
        return "60";
      case 20:
        return "39";
      case 21:
        return "63";
      case 22:
        return "48";
      case 23:
        return "05";
      case 24:
        return "45";
      case 25:
        return "17";
      case 26:
        return "47";
      case 27:
        return "58";
      case 28:
        return "31";
      case 29:
        return "49";
      case 30:
        return "28";
      case 31:
        return "43";
      case 32:
        return "23";
      case 33:
        return "27";
      case 34:
        return "04";
      case 35:
        return "41";
      case 36:
        return "52";
      case 37:
        return "22";
      case 38:
        return "18";
      case 39:
        return "26";
      case 40:
        return "35";
      case 41:
        return "21";
      case 42:
        return "64";
      case 43:
        return "38";
      case 44:
        return "56";
      case 45:
        return "30";
      case 46:
        return "50";
      case 47:
        return "14";
      case 48:
        return "20";
      case 49:
        return "42";
      case 50:
        return "59";
      case 51:
        return "61";
      case 52:
        return "53";
      case 53:
        return "37";
      case 54:
        return "57";
      case 55:
        return "09";
      case 56:
        return "12";
      case 57:
        return "25";
      case 58:
        return "06";
      case 59:
        return "10";
      case 60:
        return "33";
      case 61:
        return "13";
      case 62:
        return "44";
      case 63:
        return "01";
    }
    return "-1";
  }

  public static String implode(String[] array, String sep) {
    String out = null;
    for (String str : array) {
      if (out != null) {
        out += sep;
      }
      if (out == null) {
        out = EMPTY_STRING;
      }
      out += str;
    }
    return out;
  }

  /**
   * @param index a cardinal index
   * @return The map of the cardinal index to the hex value;
   */
  public static int[] invHexMap(int index) {
    int[] hex = new int[6];
    for (int i = 0; i < hex.length; i++) {
      hex[i] = Consts.ICHING_YOUNG_YIN;
    }
    switch (index) {
      case 24:
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 7:
        hex[1] = Consts.ICHING_YOUNG_YANG;
        break;
      case 19:
        hex[1] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 15:
        hex[2] = Consts.ICHING_YOUNG_YANG;
        break;
      case 36:
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 46:
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        break;
      case 11:
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 16:
        hex[3] = Consts.ICHING_YOUNG_YANG;
        break;
      case 51:
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 40:
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        break;
      case 54:
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 62:
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        break;
      case 55:
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 32:
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        break;
      case 34:
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 8:
        hex[4] = Consts.ICHING_YOUNG_YANG;
        break;
      case 3:
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 29:
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        break;
      case 60:
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 39:
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        break;
      case 63:
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 48:
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        break;
      case 5:
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 45:
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        break;
      case 17:
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 47:
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        break;
      case 58:
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 31:
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        break;
      case 49:
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 28:
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        break;
      case 43:
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 23:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        break;
      case 27:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 4:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        break;
      case 41:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 52:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        break;
      case 22:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 18:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        break;
      case 26:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 35:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        break;
      case 21:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 64:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        break;
      case 38:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 56:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        break;
      case 30:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 50:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        break;
      case 14:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 20:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[4] = Consts.ICHING_YOUNG_YANG;
        break;
      case 42:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 59:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        break;
      case 61:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 53:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        break;
      case 37:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 57:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        break;
      case 9:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 12:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        break;
      case 25:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 06:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        break;
      case 10:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 33:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        break;
      case 13:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
      case 44:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        break;
      case 01:
        hex[5] = Consts.ICHING_YOUNG_YANG;
        hex[4] = Consts.ICHING_YOUNG_YANG;
        hex[3] = Consts.ICHING_YOUNG_YANG;
        hex[2] = Consts.ICHING_YOUNG_YANG;
        hex[1] = Consts.ICHING_YOUNG_YANG;
        hex[0] = Consts.ICHING_YOUNG_YANG;
        break;
    }
    return hex;
  }


  public static boolean isDummyHistoryEntry(HistoryEntry entry) {
    if (entry.getChanging() == -1 &&
        entry.getHex() == null && entry.getTHex() == null &&
        entry.getDate() == null) {
      return true;
    }
    return false;
  }

  public static boolean isNumeric(Serializable str) {
    return str.toString().matches("-?\\d+(\\.\\d+)?");
  }

  public static boolean mask(Integer mask, Integer value) {
    return (mask & value) > 0;
  }

  public static String s(int id) {
    try {
      return context.getResources().getString(id);
    } catch (NotFoundException e) {
      Log.w("Utils.s()", e.getMessage());
      return EMPTY_STRING;
    }
  }

  public static String s(int id, Object... subst) {
    return context.getResources().getString(id, subst);
  }

  public static void setContext(Context context) {
    Utils.context = context;
  }

  public static void sortHistoryList(List<HistoryEntry> historyList) {
    Collections.sort(historyList, new Comparator<HistoryEntry>() {
      public int compare(HistoryEntry e1, HistoryEntry e2) {
        if (e1.getDate().getTime() > e2.getDate().getTime()) {
          return -1;
        }
        return 1;
      }
    });
  }

  public static String streamToString(InputStream stream) throws IOException {
    return new String(getBytes(stream), "UTF-8").replace("\\n", NEWLINE);
  }

  public static boolean isGoverning(String hex, int line) {
    return Arrays.binarySearch(ChangingLinesEvaluator.ICHING_GOVERNING_LINE[line], Integer.parseInt(hex)) >= 0;
  }

  public static boolean isConstituent(String hex, int line) {
    return Arrays.binarySearch(ChangingLinesEvaluator.ICHING_CONSTITUENT_LINE[line], Integer.parseInt(hex)) >= 0;
  }
}
