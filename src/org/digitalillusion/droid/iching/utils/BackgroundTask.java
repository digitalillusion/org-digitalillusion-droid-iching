package org.digitalillusion.droid.iching.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Replacement of the deprecated AsyncTask: runs work off the main thread, one task
 * at a time, and delivers the result on the main thread unless the task was cancelled
 *
 * @param <R> The type of the result
 * @author digitalillusion
 */
public abstract class BackgroundTask<R> {

  private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
  private static final Handler MAIN_THREAD = new Handler(Looper.getMainLooper());

  private volatile boolean cancelled;
  private Future<?> future;

  /**
   * Runs on the main thread before the background work starts
   */
  protected void onPreExecute() {
  }

  /**
   * Runs on a background thread
   *
   * @return The result passed to {@link #onPostExecute(Object)}
   */
  protected abstract R doInBackground();

  /**
   * Runs on the main thread after the background work, if the task wasn't cancelled
   *
   * @param result The result of {@link #doInBackground()}
   */
  protected void onPostExecute(R result) {
  }

  /**
   * Start the task. Must be called on the main thread
   *
   * @return This task
   */
  public final BackgroundTask<R> execute() {
    onPreExecute();
    future = EXECUTOR.submit(() -> {
      try {
        final R result = doInBackground();
        MAIN_THREAD.post(() -> {
          if (!cancelled) {
            onPostExecute(result);
          }
        });
      } catch (RuntimeException | Error e) {
        // Fail as loudly as AsyncTask did instead of losing the error in the Future
        MAIN_THREAD.post(() -> {
          throw new RuntimeException("An error occurred while executing doInBackground()", e);
        });
      }
    });
    return this;
  }

  /**
   * Cancel the task: {@link #onPostExecute(Object)} won't be called
   *
   * @param mayInterruptIfRunning True to interrupt the background thread
   * @return False if the task had already completed, true otherwise
   */
  public final boolean cancel(boolean mayInterruptIfRunning) {
    cancelled = true;
    return future == null || future.cancel(mayInterruptIfRunning);
  }

  /**
   * @return True if the task was cancelled
   */
  public final boolean isCancelled() {
    return cancelled;
  }
}
