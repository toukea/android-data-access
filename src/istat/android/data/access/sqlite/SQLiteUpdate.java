package istat.android.data.access.sqlite;

import android.database.sqlite.SQLiteDatabase;

import java.util.List;

public final class SQLiteUpdate {
    Updater updater;

    SQLiteUpdate(Class<?> clazz, SQLite.SQL sql) {
        updater = new Updater(clazz, sql);
    }
//
//    SQLiteUpdate(String table, SQLiteDatabase db) {
//        updater = new Updater(table, db);
//    }

    public Updater setAs(Object entity) {
        String tbName = entity.getClass().getName();
        try {
            SQLiteModel model = SQLiteModel.fromObject(entity);
            updater.model.fieldNameValuePair.putAll(model.fieldNameValuePair);
            tbName = model.getName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Updater(tbName, this.updater.sql);
    }

    public Updater set(String name, Object value) {
        updater.model.set(name, value);
        return updater;
    }

    public SQLiteClause.ClauseBuilder where(String column) {
        return updater.where(column);
    }

    public class Updater extends SQLiteClause<Updater> {
        protected SQLiteModel model;

        private void setModel(SQLiteModel model) {
            this.model = model;
        }

        protected Updater(Class<?> clazz, SQLite.SQL sql) {
            super(clazz, sql);
            try {
                model = SQLiteModel.fromClass(clazz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        protected Updater(String clazz, SQLite.SQL sql) {
            super(clazz, null, sql);
        }

        public Updater set(String name, Object value) {
            model.set(name, value);
            return this;
        }

        @Override
        protected Object onExecute(SQLiteDatabase db) {
            String whereClause = getWhereClause();
            String[] whereParams = getWhereParams();
            return db.update(model.getName(), model.toContentValues(),
                    whereClause, whereParams);
        }

        public int execute() {
            int out = Integer.valueOf(onExecute(sql.db) + "");
            notifyExecuted();
            return out;
        }

        public int execute(int offset, int limit) {
            String limitS;
            if (limit < 0) {
                limitS = null;
            } else {
                if (offset < 0) {
                    offset = 0;
                }
                limitS = offset + ", " + limit;
            }
            return execute(limitS);
        }

        public int execute(String limit) {
            this.limit = limit;
            return execute();
        }

        public int execute(int limit) {
            return execute(limit > 0 ? "" + limit : null);
        }


        @Override
        public final String getStatement() {
            return super.getStatement();
        }
    }
}
