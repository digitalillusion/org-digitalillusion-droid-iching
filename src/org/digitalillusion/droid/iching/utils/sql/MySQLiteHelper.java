package org.digitalillusion.droid.iching.utils.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper {

  public static final String TABLE_DEFINITIONS = "definitions";
  public static final String COLUMN_HEX = "hex";
  public static final String COLUMN_DICTIONARY = "dictionary";
  public static final String COLUMN_LANG = "lang";
  public static final String COLUMN_SECTION = "section";
  public static final String COLUMN_DEF = "def";
  // Database creation sql statement
  private static final String DATABASE_CREATE = "create table "
      + TABLE_DEFINITIONS + "(" +
      COLUMN_HEX + " string, " +
      COLUMN_DICTIONARY + " string, " +
      COLUMN_LANG + " string, " +
      COLUMN_SECTION + " string, " +
      COLUMN_DEF + " text not null," +
      "primary key (" +
      COLUMN_HEX + ", " +
      COLUMN_DICTIONARY + ", " +
      COLUMN_LANG + ", " +
      COLUMN_SECTION +
      ")" +
      ");";
  private static final String DATABASE_NAME = "definitions.db";
  private static final int DATABASE_VERSION_1 = 1;
  private static final int DATABASE_VERSION_CURRENT = DATABASE_VERSION_1;

  public MySQLiteHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION_CURRENT);
  }

  @Override
  public void onCreate(SQLiteDatabase database) {
    database.execSQL(DATABASE_CREATE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.w(MySQLiteHelper.class.getName(),
        "No database upgrade from version " + oldVersion + " to "
            + newVersion + ". Dropped old data.");
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_DEFINITIONS);
    onCreate(db);
  }

} 