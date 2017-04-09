package istat.android.data.access.sqlite.utils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;

import istat.android.data.access.sqlite.SQLiteModel;

/**
 * Created by istat on 24/01/17.
 */

public class TableUtils {
    public final static boolean exist(SQLiteDatabase db, Class cLass) {
        try {
            SQLiteModel model = SQLiteModel.fromClass(cLass);
            db.query(model.getName(), null, null, null, null, null, null);
            return true;
        } catch (Exception e) {

        }
        return false;
    }

    public static void drop(SQLiteDatabase db, Class... tables) throws InstantiationException, IllegalAccessException {
        List<String> scripts = TableScriptFactory.drop(tables);
        for (String sql : scripts) {
            db.execSQL(sql);
        }
    }

    public static void truncate(SQLiteDatabase db, Class... tables) throws InstantiationException, IllegalAccessException {
        List<String> scripts = TableScriptFactory.truncate(tables);
        for (String sql : scripts) {
            db.execSQL(sql);
        }
    }

    public static void create(SQLiteDatabase db, Class... tables) throws InstantiationException, IllegalAccessException {
        List<String> scripts = TableScriptFactory.create(tables);
        for (String sql : scripts) {
            db.execSQL(sql);
        }
    }

    //TODO make a good implementation.
    public static boolean alter(SQLiteDatabase db, Class tables) throws InstantiationException, IllegalAccessException {
        List<String> scripts = TableScriptFactory.alter(tables);
        execute(db, scripts);
        return false;
    }

    public static boolean execute(SQLiteDatabase db, String... scripts) {
        try {
            for (String sql : scripts) {
                db.execSQL(sql);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public static boolean execute(SQLiteDatabase db, List<String> scripts) {
        for (String sql : scripts) {
            db.execSQL(sql);
        }
        return false;
    }
}
