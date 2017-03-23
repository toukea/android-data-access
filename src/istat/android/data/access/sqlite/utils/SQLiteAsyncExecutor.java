package istat.android.data.access.sqlite.utils;

import android.os.Handler;
import android.os.Looper;


import java.util.List;

import istat.android.data.access.sqlite.SQLiteDelete;
import istat.android.data.access.sqlite.SQLiteInsert;
import istat.android.data.access.sqlite.SQLiteMerge;
import istat.android.data.access.sqlite.SQLitePersist;
import istat.android.data.access.sqlite.SQLiteSelect;
import istat.android.data.access.sqlite.SQLiteUpdate;

/**
 * Created by istat on 08/02/17.
 */

public class SQLiteAsyncExecutor {
    private Handler handler = new Handler(Looper.getMainLooper());

    public SQLiteAsyncExecutor(Handler handler) {
        this.handler = handler;
    }

    public Handler getHandler() {
        return handler;
    }

    public SQLiteAsyncExecutor() {

    }

    public <T> SQLiteThread execute(final SQLiteSelect clause, int limit, final ExecutionCallback<List<T>> callback) {
        return execute(clause, -1, limit, callback);
    }

    public <T> SQLiteThread execute(final SQLiteSelect clause, final ExecutionCallback<List<T>> callback) {
        return execute(clause, -1, -1, callback);
    }

    public <T> SQLiteThread execute(final SQLiteSelect clause, final int offset, final int limit, final ExecutionCallback<List<T>> callback) {
        SQLiteThread<List<T>> thread = new SQLiteThread<List<T>>(callback) {

            @Override
            protected List<T> onExecute() {
                return clause.execute(offset, limit);
            }
        };
        thread.start();
        return thread;
    }

    public SQLiteThread execute(final SQLiteUpdate.Updater clause, ExecutionCallback<Integer> callback) {
        return execute(clause, -1, -1, callback);
    }

    public SQLiteThread execute(final SQLiteUpdate.Updater clause, final int limit, ExecutionCallback<Integer> callback) {
        return execute(clause, -1, limit, callback);
    }

    public SQLiteThread execute(final SQLiteUpdate.Updater clause, final int offset, final int limit, ExecutionCallback<Integer> callback) {
        SQLiteThread<Integer> thread = new SQLiteThread<Integer>(callback) {

            @Override
            protected Integer onExecute() {
                return clause.execute(offset, limit);
            }
        };
        thread.start();
        return thread;
    }

    public SQLiteThread execute(final SQLiteInsert clause, ExecutionCallback<long[]> callback) {
        SQLiteThread<long[]> thread = new SQLiteThread<long[]>(callback) {

            @Override
            protected long[] onExecute() {
                try {
                    return clause.execute();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        };
        thread.start();
        return thread;
    }

    public SQLiteThread execute(final SQLiteMerge clause, ExecutionCallback<long[]> callback) {
        SQLiteThread<long[]> thread = new SQLiteThread<long[]>(callback) {

            @Override
            protected long[] onExecute() {
                return clause.execute();
            }
        };
        thread.start();
        return thread;
    }

    public SQLiteThread execute(final SQLitePersist clause, ExecutionCallback<long[]> callback) {
        SQLiteThread<long[]> thread = new SQLiteThread<long[]>(callback) {

            @Override
            protected long[] onExecute() {
                return clause.execute();
            }
        };
        thread.start();
        return thread;
    }

    public SQLiteThread execute(final SQLiteDelete clause, ExecutionCallback<Integer> callback) {
        SQLiteThread<Integer> thread = new SQLiteThread<Integer>(callback) {

            @Override
            protected Integer onExecute() {
                return clause.execute();
            }
        };
        thread.start();
        return thread;
    }

    public abstract class SQLiteThread<T> extends Thread {
        boolean running = false;
        ExecutionCallback<T> callback;

        public SQLiteThread(ExecutionCallback<T> callback) {
            this.callback = callback;
        }

        @Override
        public final void run() {
            try {
                notifySuccess(onExecute());
            } catch (Exception e) {
                notifyError(e);
            }
        }

        protected abstract T onExecute();

        @Override
        public synchronized void start() {
            running = true;
            super.start();
            notifyStarted(this);
        }

        @Override
        public void interrupt() {
            running = false;
            if (callback != null) {
                callback.onAborted();
            }
            super.interrupt();
        }

        public void cancel() {
            interrupt();
        }

        protected void notifySuccess(T result) {
            if (callback != null) {
                notifyCompleted(true);
                callback.onSuccess(result);
            }

        }

        protected void notifyError(Throwable e) {
            if (callback != null) {
                notifyCompleted(false);
                callback.onError(e);
            }

        }

        private void notifyCompleted(boolean state) {
            callback.onComplete(state);
        }

        private void notifyStarted(SQLiteThread thread) {
            if (callback != null) {
                callback.onStart(thread);
            }
        }
    }

    public interface ExecutionCallback<T> {
        void onStart(SQLiteThread thread);

        void onComplete(boolean success);

        void onSuccess(T result);

        void onError(Throwable error);

        void onAborted();
    }
}