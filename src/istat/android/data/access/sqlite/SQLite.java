package istat.android.data.access.sqlite;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import istat.android.data.access.sqlite.utils.SQLiteParser;

public final class SQLite {
    static SQLiteDatabase
            lastOpenedDb;
    final static ConcurrentHashMap<String, SQLiteConnection> dbNameConnectionPair = new ConcurrentHashMap<String, SQLiteConnection>();

    private SQLite() {

    }

    public static SQLiteDatabase getLastOpenedDb() {
        return lastOpenedDb;
    }

    public static SQL from(SQLiteDatabase db) {
        lastOpenedDb = db;
        return new SQL(db);
    }

    public static SQL fromConnection(String dbName) throws Exception {
        return fromConnection(dbName, false);
    }

    public static SQL fromOneShotConnection(String dbName) throws Exception {
        return fromConnection(dbName, true);
    }

    public static SQL fromConnection(String dbName, boolean closeDataBaseOnExecute) throws Exception {
        SQLiteDataAccess access = findOrCreateConnectionAccess(dbName);
        SQL sql = SQLite.from(access.open());
        sql.setAutoClose(closeDataBaseOnExecute);
        return sql;
    }

    public static void addConnection(SQLiteConnection... connections) {
        for (SQLiteConnection launcher : connections) {
            addConnection(launcher, false);
        }
    }

    public static SQLiteDataAccess getAccess(String dbName) {
        try {
            return findOrCreateConnectionAccess(dbName);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static SQLiteDatabase getDataBase(String dbName) {
        try {
            SQLiteDataAccess access = findOrCreateConnectionAccess(dbName);
            if (access != null) {
                return access.open();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void addConnection(SQLiteConnection connection, boolean connectInstantly) {
        if (connectInstantly) {
            connect(connection);
        } else {
            dbNameConnectionPair.put(connection.dbName, connection);
        }
    }

    public static boolean removeConnection(String dbName) {
//        boolean contain = dbNameAccessPair.containsKey(dbName);
//        if (contain) {
//            dbNameAccessPair.remove(dbName);
//        }
//        return contain;
        return true;
    }

    public static boolean removeConnection(SQLiteConnection connection) {
        return removeConnection(connection.dbName);
    }

    public static void prepareSQL(String dbName, PrepareHandler handler) {
        prepareSQL(dbName, handler, false);
    }

    public static void prepareTransactionalSQL(String dbName, SQLReadyHandler handler) {
        prepareSQL(dbName, handler, true);
    }

    public static void prepareTransactionalSQL(String dbName, PrepareHandler handler) {
        prepareSQL(dbName, handler, true);
    }

    private static SQLiteDataAccess findOrCreateConnectionAccess(String dbName) throws IllegalAccessException {
        SQLiteConnection connection = dbNameConnectionPair.get(dbName);
        if (connection == null) {
            throw new IllegalAccessException("Oups, no launcher is currently added to Data base with name: " + dbName);
        }
        return connect(connection);
//        SQLiteDataAccess access = dbNameAccessPair.get(dbName);
//        if (access != null && access.isOpened()) {
//            try {
//                access = access.cloneAccess();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        boolean hasLauncher = dbNameConnectionPair.containsKey(dbName);
//        if (access != null) {
//            return access;
//        } else if (access == null && hasLauncher) {
//            access = connect(dbNameConnectionPair.get(dbName));
//            dbNameConnectionPair.remove(dbName);
//        } else {
//            throw new IllegalAccessException("Oups, no launcher is currently added to Data base with name: " + dbName);
//        }
//        return access;
    }

    public static void prepareSQL(String dbName, PrepareHandler handler, boolean transactional) {
        SQLiteDatabase db = null;
        try {
            SQLiteDataAccess access = findOrCreateConnectionAccess(dbName);

            db = access.open();
            if (transactional) {
                db.beginTransaction();
            }
            SQL sql = SQLite.from(db);
            handler.onSQLReady(sql);
            if (transactional) {
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            handler.onSQLPrepareFail(e);

        } finally {
            if (transactional && db != null) {
                db.endTransaction();
                if (db.isOpen()) {
                    db.close();
                }
            }

        }
    }

    public static void prepareSQL(SQLiteConnection connection, PrepareHandler handler) {
        prepareSQL(connection, false, handler);
    }

    public static void prepareTransactionalSQL(SQLiteConnection connection, PrepareHandler handler) {
        prepareSQL(connection, true, handler);
    }

    public static void prepareSQL(SQLiteConnection connection, boolean transactional, PrepareHandler handler) {
        SQLiteDatabase db = null;
        try {
            SQLiteDataAccess access = connect(connection);
            db = access.open();
            if (transactional) {
                db.beginTransaction();
            }
            SQL sql = SQLite.from(db);
            handler.onSQLReady(sql);
            if (db.isOpen()) {
                db.close();
            }
            if (transactional) {
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            handler.onSQLPrepareFail(e);
        } finally {
            if (transactional && db != null) {
                db.endTransaction();
                if (db.isOpen()) {
                    db.close();
                }
            }
        }
    }

    public static void prepareSQL(SQLiteDatabase db, PrepareHandler handler) {
        prepareSQL(db, false, handler);
    }

    public static void prepareSQL(SQLiteDatabase db, boolean transactional, PrepareHandler handler) {
        try {
            if (transactional) {
                db.beginTransaction();
            }
            SQL sql = SQLite.from(db);
            handler.onSQLReady(sql);

            if (transactional) {
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (transactional && db != null) {
                db.endTransaction();
                if (db.isOpen()) {
                    db.close();
                }
            }
        }
    }

    public static void addConnection(Context context, File file, boolean connectInstantly) {
        SQLiteConnection connection = SQLiteConnection.create(context, file, -1, null);
        if (connectInstantly) {
            connect(connection);
        } else {
            dbNameConnectionPair.put(connection.dbName, connection);
        }
    }

    public static SQL fromFile(File dbFile) throws Exception {
        return fromFile(dbFile, false);
    }

    public static SQL fromFile(File dbFile, boolean autoCloseAble) throws Exception {
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
        SQL sql = new SQL(db);
        sql.setAutoClose(autoCloseAble);
        return sql;
    }

    public static SQL fromUri(Uri dbUri) throws Exception {
        return fromUri(dbUri, false);
    }

    public static SQL fromUri(Uri dbUri, boolean autoCloseAble) throws Exception {
        File file = new File(dbUri.getPath());
        return fromFile(file, autoCloseAble);
    }

    public static void addConnection(Context context, File... file) {
        for (File f : file) {
            addConnection(context, f, false);
        }
    }

    public static SQLiteDataAccess connect(Context context, File file) {
        return connect(context, file, -1, null);
    }

    public static SQLiteDataAccess connect(Context context, File file, int version, BootDescription bootDescription) {
        SQLiteConnection connection = SQLiteConnection.create(context, file, version, bootDescription);
        return connect(connection);
    }

    public static SQLiteDataAccess connect(SQLiteConnection connection) {
        return connect(connection.context, connection.dbName, connection.dbVersion, connection);
    }
//
//    public static void close(String connectionName) {
////        SQLiteDataAccess access = dbNameAccessPair.get(connectionName);
////        if (access != null) {
////            access.close();
////        }
//    }
//
//    public static void desconnect(String dbName) {
////        SQLiteDataAccess access = dbNameAccessPair.get(dbName);
////        if (access != null) {
////            access.close();
////            dbNameAccessPair.remove(dbName);
////            dbNameConnectionPair.remove(dbName);
////        }
//    }
//
//    public static void release() {
////        Iterator<String> iterator = dbNameAccessPair.keySet().iterator();
////        while (iterator.hasNext()) {
////            String accessName = iterator.next();
////            SQLiteDataAccess access = dbNameAccessPair.get(accessName);
////            access.close();
////        }
////        dbNameAccessPair.clear();
//        dbNameConnectionPair.clear();
//    }

    public static SQLiteDataAccess connect(Context context, String dbName, int dbVersion, final BootDescription description) {
        SQLiteDataAccess access = new SQLiteDataAccess(context, dbName, dbVersion) {
            @Override
            public void onUpgradeDb(SQLiteDatabase db, int oldVersion, int newVersion) {
                if (description != null) {
                    description.onUpgradeDb(db, oldVersion, newVersion);
                }
            }

            @Override
            public void onCreateDb(SQLiteDatabase db) {
                if (description != null) {
                    description.onCreateDb(db);
                }
            }
        };
//        dbNameAccessPair.put(dbName, access);
        return access;
    }


    public static class SQL {
        SQLiteDatabase db;
        boolean autoClose = false;
        HashMap<Class<?>, SQLiteModel.Serializer> serializers = new HashMap<Class<?>, SQLiteModel.Serializer>() {
            {
                put(Object.class, SQLiteModel.DEFAULT_SERIALIZER);
            }
        };
        HashMap<Class<?>, SQLiteModel.CursorReader> cursorReaders = new HashMap<Class<?>, SQLiteModel.CursorReader>() {
            {
                put(Object.class, SQLiteModel.DEFAULT_CURSOR_READER);
            }
        };

        HashMap<Class<?>, SQLiteModel.ContentValueHandler> contentValueHandlers = new HashMap<Class<?>, SQLiteModel.ContentValueHandler>() {
            {
                put(Object.class, SQLiteModel.DEFAULT_CONTAIN_VALUE_HANDLER);
            }
        };
        ArrayList<Class<?>> serializerAssignableTo = new ArrayList<Class<?>>();
        ArrayList<Class<?>> cursorReaderAssignableTo = new ArrayList<Class<?>>();
        ArrayList<Class<?>> contentValueHandlerAssignableTo = new ArrayList<Class<?>>();

        //TODO manage isAssignableTo
        SQLiteModel.Serializer getSerializer(Class cLass) {
            SQLiteModel.Serializer out = serializers.get(cLass);
            if (out != null) {
                return out;
            }
            for (Class<?> c : serializerAssignableTo) {
                if (c.isAssignableFrom(cLass)) {
                    return getSerializer(c);
                }
            }
            return serializers.get(Object.class);

        }

        SQLiteModel.CursorReader getCursorReader(Class cLass) {
            SQLiteModel.CursorReader out = cursorReaders.get(cLass);
            if (out != null) {
                return out;
            }
            for (Class<?> c : cursorReaderAssignableTo) {
                if (c.isAssignableFrom(cLass)) {
                    return getCursorReader(c);
                }
            }
            return cursorReaders.get(Object.class);

        }

        SQLiteModel.ContentValueHandler getContentValueHandler(Class cLass) {
            SQLiteModel.ContentValueHandler out = contentValueHandlers.get(cLass);
            if (out != null) {
                return out;
            }
            for (Class<?> c : contentValueHandlerAssignableTo) {
                if (c.isAssignableFrom(cLass)) {
                    return getContentValueHandler(c);
                }
            }
            return contentValueHandlers.get(Object.class);

        }

        public SQL useSerializer(Class<?> cLass, SQLiteModel.Serializer serializer) {
            return useSerializer(cLass, serializer, false);
        }

        public SQL useCursorReader(Class<?> cLass, SQLiteModel.CursorReader reader) {
            return useCursorReader(cLass, reader, false);
        }

        public SQL useContentValueHandler(Class<?> cLass, SQLiteModel.ContentValueHandler contentValueHandler) {
            return useContentValueHandler(cLass, contentValueHandler, false);
        }

        //-----------------------------------------------
        public SQL useSerializer(Class<?> cLass, SQLiteModel.Serializer serializer, boolean assignableTo) {
            this.serializers.put(cLass, serializer);
            if (assignableTo) {
                this.serializerAssignableTo.add(cLass);
            }
            return this;
        }

        public SQL useCursorReader(Class<?> cLass, SQLiteModel.CursorReader reader, boolean assignableTo) {
            this.cursorReaders.put(cLass, reader);
            if (assignableTo) {
                this.cursorReaderAssignableTo.add(cLass);
            }
            return this;
        }

        public SQL useContentValueHandler(Class<?> cLass, SQLiteModel.ContentValueHandler contentValueHandler, boolean assignableTo) {
            this.contentValueHandlers.put(cLass, contentValueHandler);
            if (assignableTo) {
                this.contentValueHandlerAssignableTo.add(cLass);
            }
            return this;
        }

        public SQL useSerializer(SQLiteModel.Serializer serializer) {
            return useSerializer(Object.class, serializer);
        }

        public SQL useCursorReader(SQLiteModel.CursorReader reader) {
            return useCursorReader(Object.class, reader);
        }

        public SQL useContentValueHandler(SQLiteModel.ContentValueHandler contentValueHandler) {
            return useContentValueHandler(Object.class, contentValueHandler);
        }

        public void setAutoClose(boolean autoClose) {
            this.autoClose = autoClose;
        }

        SQL(SQLiteDatabase db) {
            this.db = db;
        }

        public SQLiteSelect select(Class<?> clazz) {
            return new SQLiteSelect(this, clazz);
        }

        public SQLiteSelect select(boolean distinct, Class<?> clazz) {
            SQLiteSelect select = new SQLiteSelect(this, clazz);
            return select.distinct(distinct);
        }

        public SQLiteSelect select(boolean distinct, Class<?>... clazz) {
            SQLiteSelect select = new SQLiteSelect(this, clazz);
            return select.distinct(distinct);
        }

        public SQLiteSelect select(Class<?>... clazz) {
            return new SQLiteSelect(this, clazz);
        }

        //------------------------------------------
        public SQLiteSelect select(String uniqueColumn, Class<?> clazz) {
            SQLiteSelect select = new SQLiteSelect(this, clazz);
            select.columns = new String[]{uniqueColumn};
            return select;
        }

        public SQLiteSelect select(boolean distinct, String uniqueColumn, Class<?> clazz) {
            SQLiteSelect select = new SQLiteSelect(this, clazz);
            select.distinct(distinct);
            select.columns = new String[]{uniqueColumn};
            return select;
        }

        public SQLiteSelect select(boolean distinct, String uniqueColumn, Class<?>... clazz) {
            SQLiteSelect select = new SQLiteSelect(this, clazz);
            select.distinct(distinct);
            select.columns = new String[]{uniqueColumn};
            return select;
        }

        public SQLiteSelect select(String uniqueColumn, Class<?>... clazz) {
            SQLiteSelect select = new SQLiteSelect(this, clazz);
            select.columns = new String[]{uniqueColumn};
            return select;
        }

        //------------------------------------------
        public SQLiteSelect select(String[] columns, Class<?> clazz) {
            SQLiteSelect select = new SQLiteSelect(this, clazz);
            select.columns = columns;
            return select;
        }

        public SQLiteSelect select(boolean distinct, String[] columns, Class<?> clazz) {
            SQLiteSelect select = new SQLiteSelect(this, clazz);
            select.distinct(distinct);
            select.columns = columns;
            return select;
        }

        public SQLiteSelect select(boolean distinct, String[] columns, Class<?>... clazz) {
            SQLiteSelect select = new SQLiteSelect(this, clazz);
            select.distinct(distinct);
            select.columns = columns;
            return select;
        }

        public SQLiteSelect select(String[] columns, Class<?>... clazz) {
            SQLiteSelect select = new SQLiteSelect(this, clazz);
            select.columns = columns;
            return select;
        }

        //---------------------------------------
        public SQLiteUpdate update(Class<?> clazz) {
            return new SQLiteUpdate(clazz, this);
        }

        public SQLiteDelete delete(Class<?> clazz) {
            return new SQLiteDelete(clazz, this);
        }


        public int delete(Object... object) {
            int count = 0;
            for (Object obj : object) {
                if (delete(obj)) {
                    count++;
                }
            }
            return count;
        }

        public boolean delete(Object object) {
            try {
                Class<?> cLass = object.getClass();
                SQLiteModel model = SQLiteModel.fromClass(cLass);
                return delete(cLass).where(model.getPrimaryFieldName()).equalTo(model.getPrimaryKey()).execute() > 0;
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        }

        public boolean deleteById(Class<?> cLass, Object id) {
            try {
                SQLiteModel model = SQLiteModel.fromClass(cLass);
                return delete(cLass)
                        .where(model.getPrimaryFieldName())
                        .equalTo(id)
                        .execute() > 0;
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        }

        public <T> T findById(Class<T> cLass, Object id) {
            try {
                SQLiteModel model = SQLiteModel.fromClass(cLass);
                if (!model.isPrimaryFieldDefined()) {
                    throw new RuntimeException("Oups, class:" + cLass + " mapped by table:" + model.getName() + " doesn't has primary field defined.");
                }
                return select(cLass)
                        .where(model.getPrimaryFieldName())
                        .equalTo(id)
                        .executeLimit1();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return null;
        }

        public <T> List<T> findAll(Class<T> classTable) {
            return findAll(classTable, false);
        }

        public <T> List<T> findAll(Class<T> classTable, boolean distinct) {
            return findAll(classTable, distinct, null, null, null, null, null, null);
        }

        public <T> List<T> findAll(Class<T> classTable, boolean distinct, String whereClause, String[] whereParams, String groupBy, String having, String orderBy, String limit) {
            SQLiteSelect select = select(distinct, classTable);
            select.distinct = distinct;
            select.whereClause = new StringBuilder(whereClause);
            select.whereParams = whereParams != null ? Arrays.asList(whereParams) : null;
            select.limit = limit;
            select.orderBy = orderBy;
            select.groupBy = groupBy;
            select.having = new StringBuilder(having);
            return select.execute();
        }

        public <T> List<T> findAll(Class<T> classTable, final String rawQuery, final String[] selectionArgs) {
            return findAll(classTable, rawQuery, selectionArgs, null);
        }

        public <T> List<T> findAll(Class<T> classTable, final String rawQuery, final String[] selectionArgs, final CancellationSignal signal) {
            SQLiteSelect select = new SQLiteSelect(this, classTable) {
                @Override
                protected Cursor onExecute(SQLiteDatabase db) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        return db.rawQuery(rawQuery, selectionArgs, signal);
                    } else {
                        return db.rawQuery(rawQuery, selectionArgs);
                    }
                }
            };
            return select.execute();
        }

        public <T> int update(Class<T> classTable, ContentValues contentValues, String whereClause, String[] whereParams, String having, String limit) {
            SQLiteUpdate.Updater update = update(classTable).updater;
            update.model.fillFromContentValues(contentValues);
            update.whereClause = new StringBuilder(whereClause);
            update.whereParams = whereParams != null ? Arrays.asList(whereParams) : null;
            update.limit = limit;
            update.having = new StringBuilder(having);
            return update.execute();
        }

        //------------------------------------------
        public SQLiteInsert insert(Object entity) {
            SQLiteInsert insert = new SQLiteInsert(this);
            return insert.insert(entity);
        }

        public SQLiteInsert insert(Object... entity) {
            SQLiteInsert insert = new SQLiteInsert(this);
            return insert.insert(entity);
        }

        public <T> SQLiteInsert insert(List<T> entity) {
            SQLiteInsert insert = new SQLiteInsert(this);
            return insert.insert(entity);
        }

        public SQLitePersist persist(Object entity) {
            SQLitePersist persist = new SQLitePersist(this);
            return persist.persist(entity);
        }

        public SQLitePersist persist(Object... entity) {
            SQLitePersist persist = new SQLitePersist(this);
            return persist.persist(entity);
        }

        public <T> SQLitePersist persist(List<T> entity) {
            SQLitePersist persist = new SQLitePersist(this);
            return persist.persist(entity);
        }

        public <T> void replaces(List<T> entity) {
            try {
                if (entity != null && !entity.isEmpty()) {
                    delete(entity.get(0).getClass()).execute();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            SQLitePersist insert = new SQLitePersist(this);
            insert.persist(entity).execute();
        }

        //---------------------------------------------
        public SQLiteMerge merge(Object entity) {
            SQLiteMerge merge = new SQLiteMerge(this);
            return merge.merge(entity);
        }

        public SQLiteMerge merge(Object... entity) {
            SQLiteMerge merge = new SQLiteMerge(this);
            return merge.merge(entity);
        }

        public <T> SQLiteMerge merge(List<T> entity) {
            SQLiteMerge merge = new SQLiteMerge(this);
            return merge.merge(entity);
        }
        //---------------------------------------------

        public void executeStatements(List<String> statements) {
            for (String ask : statements) {
                db.execSQL(ask);
            }
            if (autoClose) {
                db.close();
            }
        }

        public void executeStatements(String... statements) {
            for (String ask : statements) {
                db.execSQL(ask);
            }
            if (autoClose) {
                db.close();
            }
        }

        public void executeSQLScript(InputStream sqlFileInputStream) throws IOException {
            List<String> statements = SQLiteParser.parseSqlStream(sqlFileInputStream);
            executeStatements(statements);
        }

        public boolean isTableExist(Class<?> cLass) {
            try {
                select(cLass).count();
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        public final void close() {
            db.close();
        }

        public final void beginTransaction() {
            db.beginTransaction();
        }

        public final void setTransactionSuccessful() {
            db.setTransactionSuccessful();
        }

        public final void endTransaction(boolean success) {
            if (success) {
                setTransactionSuccessful();
            }
            endTransaction();
        }

        public final void endTransaction() {
            db.endTransaction();
        }


        public boolean isReady() {
            return db != null && db.isOpen();
        }

        public Cursor query(String table, String[] column, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
            return this.db.query(table, column, selection, selectionArgs, groupBy, having, orderBy, limit);
        }

        public Cursor query(boolean distinct, String table, String[] column, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
            return this.db.query(distinct, table, column, selection, selectionArgs, groupBy, having, orderBy, limit);
        }

        @SuppressLint("NewApi")
        public Cursor query(boolean distinct, String table, String[] column, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit, CancellationSignal cancellationSignal) {
            return this.db.query(distinct, table, column, selection, selectionArgs, groupBy, having, orderBy, limit, cancellationSignal);
        }

        public Cursor queryWithFactory(SQLiteDatabase.CursorFactory factory, boolean distinct, String table, String[] column, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
            return this.db.queryWithFactory(factory, distinct, table, column, selection, selectionArgs, groupBy, having, orderBy, limit);
        }

        @SuppressLint("NewApi")
        public Cursor queryWithFactory(SQLiteDatabase.CursorFactory factory, boolean distinct, String table, String[] column, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit, CancellationSignal cancellationSignal) {
            return this.db.queryWithFactory(factory, distinct, table, column, selection, selectionArgs, groupBy, having, orderBy, limit, cancellationSignal);
        }

        public Cursor rawQuery(String sql, String[] selectionArgs) {
            return this.db.rawQuery(sql, selectionArgs);
        }

        @SuppressLint("NewApi")
        public Cursor rawQuery(String sql, String[] selectionArgs, CancellationSignal cancellationSignal) {
            return this.db.rawQuery(sql, selectionArgs, cancellationSignal);
        }

        public Cursor rawQueryWithFactory(SQLiteDatabase.CursorFactory cursorFactory, String sql, String[] selectionArgs, String ediTable) {
            return this.db.rawQueryWithFactory(cursorFactory, sql, selectionArgs, ediTable);
        }

        @SuppressLint("NewApi")
        public Cursor rawQueryWithFactory(SQLiteDatabase.CursorFactory cursorFactory, String sql, String[] selectionArgs, String ediTable, CancellationSignal cancellationSignal) {
            return this.db.rawQueryWithFactory(cursorFactory, sql, selectionArgs, ediTable, cancellationSignal);
        }

        public SQLiteDatabase getDb() {
            return db;
        }
    }

    public static abstract class SQLiteConnection implements BootDescription {
        String dbName;
        int dbVersion = 1;
        Context context;

        public static SQLiteConnection create(Context context, File file) {
            return create(context, file, -1, null);
        }

        public static SQLiteConnection create(Context context, File file, int version, final BootDescription description) {
            if (version < 0) {
                SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, null);
                db.getVersion();
                db.close();
            }
            return create(context, file.getAbsolutePath(), version, description);
        }

        public static SQLiteConnection create(Context context, String dbName, int dbVersion, final BootDescription description) {
            SQLiteConnection connection = new SQLiteConnection(context, dbName, dbVersion) {
                @Override
                public void onCreateDb(SQLiteDatabase db) {
                    if (description != null) {
                        description.onCreateDb(db);
                    }
                }

                @Override
                public void onUpgradeDb(SQLiteDatabase db, int oldVersion, int newVersion) {
                    if (description != null) {
                        description.onUpgradeDb(db, oldVersion, newVersion);
                    }
                }
            };
            return connection;
        }

        public SQLiteConnection(Context context, String dbName, int dbVersion) {
            this.dbName = dbName;
            this.dbVersion = dbVersion;
            this.context = context;
        }

        public final static void executeScripts(SQLiteDatabase db, List<String> scripts) {
            for (String script : scripts) {
                db.execSQL(script);
            }
        }
    }

    public interface BootDescription {
        void onCreateDb(SQLiteDatabase db);

        void onUpgradeDb(SQLiteDatabase db, int oldVersion,
                         int newVersion);
    }

    public static void executeSQLScript(SQLiteDatabase db,
                                        InputStream sqlFileInputStream) throws IOException {
        List<String> statements = SQLiteParser.parseSqlStream(sqlFileInputStream);
        for (String statement : statements)
            db.execSQL(statement);
    }

    public static void executeStatements(SQLiteDatabase db, List<String> statements) {
        for (String ask : statements) {
            db.execSQL(ask);
        }
    }

    public static void executeStatements(SQLiteDatabase db, String... statements) {
        for (String ask : statements) {
            db.execSQL(ask);
        }
    }

    public interface PrepareHandler {
        void onSQLReady(SQL sql) throws Exception;

        void onSQLPrepareFail(Exception e);
    }

    public static abstract class SQLReadyHandler implements PrepareHandler {

        public void onSQLPrepareFail(Exception e) {
            e.printStackTrace();
        }
    }
}
