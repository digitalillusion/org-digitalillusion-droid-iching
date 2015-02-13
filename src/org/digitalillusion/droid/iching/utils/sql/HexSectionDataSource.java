package org.digitalillusion.droid.iching.utils.sql;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import org.digitalillusion.droid.iching.utils.Utils;

/**
 * DAO to access an hexagram section definition
 *
 * @author digitalillusion
 */
public class HexSectionDataSource {

  // Database fields
  private SQLiteDatabase database;
  private MySQLiteHelper dbHelper;
  private String[] allColumns = {
      MySQLiteHelper.COLUMN_HEX,
      MySQLiteHelper.COLUMN_DICTIONARY,
      MySQLiteHelper.COLUMN_LANG,
      MySQLiteHelper.COLUMN_SECTION,
      MySQLiteHelper.COLUMN_DEF
  };

  public HexSectionDataSource(Context context) {
    dbHelper = new MySQLiteHelper(context);
  }

  public void close() {
    dbHelper.close();
  }

  /**
   * Allows to delete a section definition for an hexagram
   *
   * @param hex        The hexagram parameter
   * @param dictionary The dictionary parameter
   * @param section    The section parameter
   * @param lang       The language parameter
   */
  public synchronized void deleteHexSection(String hex, String dictionary, String section, String lang) {
    if (database.isOpen()) {
      database.delete(
          MySQLiteHelper.TABLE_DEFINITIONS,
          MySQLiteHelper.COLUMN_HEX + "=? and " +
              MySQLiteHelper.COLUMN_DICTIONARY + "=? and " +
              MySQLiteHelper.COLUMN_SECTION + "=? and " +
              MySQLiteHelper.COLUMN_LANG + "=?",
          new String[]{hex, dictionary, section, lang}
      );
    }
  }

  /**
   * Allows to delete all the definitions of an hexagram
   *
   * @param hex        The hexagram parameter
   * @param dictionary The dictionary parameter
   * @param lang       The language parameter
   */
  public synchronized void deleteHexSections(String hex, String dictionary, String lang) {
    if (database.isOpen()) {
      database.delete(
          MySQLiteHelper.TABLE_DEFINITIONS,
          MySQLiteHelper.COLUMN_HEX + "=? and " +
              MySQLiteHelper.COLUMN_DICTIONARY + "=? and " +
              MySQLiteHelper.COLUMN_LANG + "=?",
          new String[]{hex, dictionary, lang}
      );
    }
  }

  /**
   * @param hex        The hexagram parameter
   * @param dictionary The dictionary parameter
   * @param lang       The language parameter
   * @param section    The section parameter
   * @return The result of the search corresponding to the given parameters, or null
   * if no match is found
   */
  public HexSection getHexSection(String hex, String dictionary, String lang, String section) throws NotFoundException {
    String querySelect = MySQLiteHelper.COLUMN_HEX + "=? and " +
        MySQLiteHelper.COLUMN_DICTIONARY + "=? and " +
        MySQLiteHelper.COLUMN_LANG + "=? and " +
        MySQLiteHelper.COLUMN_SECTION + "=?";
    String[] queryParams = new String[]{hex, dictionary, lang, section};

    HexSection hs = new HexSection();
    if (database.isOpen()) {
      Cursor cursor = database.query(
          MySQLiteHelper.TABLE_DEFINITIONS,
          allColumns,
          querySelect,
          queryParams,
          null, null, null
      );

      cursor.moveToFirst();
      try {
        if (!cursor.isAfterLast()) {
          hs = new HexSection(
              cursor.getString(0),
              cursor.getString(1),
              cursor.getString(2),
              cursor.getString(3),
              cursor.getString(4)
          );
          cursor.moveToNext();
        } else {
          throw new NotFoundException(
              "HexSectionDataSource.getHexSection() returned no results for query: " +
                  querySelect + " using parameters (" +
                  Utils.implode(queryParams, ",") + ")"
          );
        }
      } finally {
        // Make sure to close the cursor
        cursor.close();
      }
    }
    return hs;
  }

  public void open() throws SQLException {
    database = dbHelper.getWritableDatabase();
  }

  /**
   * Allows to insert or update the definition of an hexagram section
   *
   * @param hex        The hexagram parameter
   * @param dictionary The dictionary parameter
   * @param lang       The language parameter
   * @param section    The section parameter
   * @param def        The new definition
   */
  public synchronized void updateHexSection(String hex, String dictionary, String lang, String section, String def) {

    if (database.isOpen()) {
      ContentValues values = new ContentValues();
      values.put(MySQLiteHelper.COLUMN_HEX, hex);
      values.put(MySQLiteHelper.COLUMN_DICTIONARY, dictionary);
      values.put(MySQLiteHelper.COLUMN_LANG, lang);
      values.put(MySQLiteHelper.COLUMN_SECTION, section);
      values.put(MySQLiteHelper.COLUMN_DEF, def);

      try {
        String whereClause = MySQLiteHelper.COLUMN_HEX + "=? and " +
            MySQLiteHelper.COLUMN_DICTIONARY + "=? and " +
            MySQLiteHelper.COLUMN_LANG + "=? and " +
            MySQLiteHelper.COLUMN_SECTION + "=?";
        String[] whereArgs = new String[]{hex, dictionary, lang, section};

        getHexSection(hex, dictionary, lang, section);
        database.update(
            MySQLiteHelper.TABLE_DEFINITIONS,
            values,
            whereClause,
            whereArgs
        );
      } catch (NotFoundException e) {
        database.insert(
            MySQLiteHelper.TABLE_DEFINITIONS,
            null,
            values
        );
      }
    }
  }
} 