/*
 * Copyright (C) 2017-2023 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */
package com.here.naksha.lib.core;

import static com.here.naksha.lib.core.exceptions.UncheckedException.unchecked;

import com.here.naksha.lib.core.exceptions.TooManyTasks;
import com.here.naksha.lib.core.util.NanoTime;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A task that executes in an own dedicated thread to fulfill some job. The currently executed task can be queried using
 * {@link #currentTask()}.
 *
 * @param <RESULT> the response-type to be generated by the task.
 * @param <SELF> the self-type.
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public abstract class AbstractTask<RESULT, SELF extends AbstractTask<RESULT, SELF>>
    implements INakshaBound, UncaughtExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(AbstractTask.class);

  /**
   * The soft-limit of tasks to run concurrently. This limit does normally not apply to child tasks.
   */
  public static final AtomicLong limit =
      new AtomicLong(Math.max(1000, Runtime.getRuntime().availableProcessors() * 50L));

  /**
   * A thread pool to execute tasks.
   */
  private static final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();

  /**
   * Creates a new task.
   *
   * @param naksha  the reference to the Naksha host.
   * @param context the reference to the context.
   */
  public AbstractTask(@NotNull INaksha naksha, @NotNull NakshaContext context) {
    this.naksha = naksha;
    this.context = context;
    this.startNanos = NanoTime.now();
  }

  /**
   * Reference to the Naksha-Hub to which this task is bound.
   */
  private final @NotNull INaksha naksha;

  /**
   * The context bound to this task.
   */
  private final @NotNull NakshaContext context;

  /**
   * The time when the task was created.
   */
  private final long startNanos;

  @Override
  public final @NotNull INaksha naksha() {
    return naksha;
  }

  /**
   * Returns the {@link NakshaContext} to which this task is bound.
   *
   * @return the {@link NakshaContext} to which this task is bound.
   */
  public final @NotNull NakshaContext context() {
    return context;
  }

  /**
   * Returns the start time of the task in nanoseconds. This will differ from the {@link NakshaContext#startNanos()} time, the difference
   * can even be big, if this is just a child task.
   *
   * @return The start time of the task in nanoseconds.
   */
  public final long startNanos() {
    return startNanos;
  }

  /**
   * The uncaught exception handler for the thread that executes this task.
   *
   * @param thread the thread.
   * @param t      the exception.
   */
  public void uncaughtException(@NotNull Thread thread, @NotNull Throwable t) {
    log.atError()
        .setMessage("Uncaught exception in task {}")
        .addArgument(getClass().getName())
        .setCause(t)
        .log();
  }

  @SuppressWarnings("unchecked")
  protected final @NotNull SELF self() {
    return (SELF) this;
  }

  /**
   * A flag to signal that this task is internal.
   */
  private boolean internal;

  /**
   * Flag this task as internal, so when starting the task, the maximum amount of parallel tasks {@link #limit} is ignored.
   *
   * @param internal {@code true} if this task is internal and therefore bypassing the maximum parallel tasks limit.
   * @throws IllegalStateException if the task is not in the state {@link State#NEW}.
   */
  public @NotNull SELF setInternal(boolean internal) {
    lockAndRequireNew();
    try {
      this.internal = internal;
    } finally {
      unlock();
    }
    return self();
  }

  /**
   * Tests whether this task flagged as internal.
   *
   * @return {@code true} if this task flagged as internal; {@code false} otherwise.
   */
  public boolean isInternal() {
    return internal;
  }

  /**
   * The thread to which this task is currently bound; if any.
   */
  @Nullable
  private Thread thread;

  /**
   * The previously set uncaught exception handler.
   */
  @Nullable
  private Thread.UncaughtExceptionHandler oldUncaughtExceptionHandler;

  @Nullable
  private String oldName;

  /**
   * Returns the task attached to the current thread; if any.
   *
   * @return The task attached to the current thread or {@code null}, if the current thread has no task attached.
   * @throws ClassCastException if the task is not of the expected type.
   */
  @SuppressWarnings("unchecked")
  public static <T extends AbstractTask<?, ?>> @Nullable T currentTask() {
    final Thread thread = Thread.currentThread();
    final UncaughtExceptionHandler uncaughtExceptionHandler = thread.getUncaughtExceptionHandler();
    if (uncaughtExceptionHandler instanceof AbstractTask<?, ?> task) {
      return (T) task;
    }
    return null;
  }

  /**
   * Returns the thread to which this task is currently bound; if any.
   *
   * @return The thread to which this task is currently bound; if any.
   */
  public @Nullable Thread getThread() {
    return thread;
  }

  /**
   * Binds this task to the current thread.
   *
   * @throws IllegalStateException if this task is bound to another thread, or the current thread is bound to another task.
   */
  public void attachToCurrentThread() {
    if (thread != null) {
      throw new IllegalStateException("Already bound to a thread");
    }
    final Thread thread = Thread.currentThread();
    final String threadName = thread.getName();
    final UncaughtExceptionHandler threadUncaughtExceptionHandler = thread.getUncaughtExceptionHandler();
    if (threadUncaughtExceptionHandler instanceof AbstractTask) {
      throw new IllegalStateException("The current thread is already bound to task " + threadName);
    }
    this.thread = thread;
    this.oldName = threadName;
    this.oldUncaughtExceptionHandler = threadUncaughtExceptionHandler;
    thread.setName(context.streamId());
    thread.setUncaughtExceptionHandler(this);
  }

  /**
   * Removes this task form the current thread. The call will be ignored, if the task is unbound.
   *
   * @throws IllegalStateException If called from a thread to which this task is not bound.
   */
  public void detachFromCurrentThread() {
    if (this.thread == null) {
      return;
    }
    final Thread thread = Thread.currentThread();
    if (this.thread != thread) {
      throw new IllegalStateException("Can't unbind from foreign thread");
    }
    assert oldName != null;
    thread.setName(oldName);
    thread.setUncaughtExceptionHandler(oldUncaughtExceptionHandler);
    this.thread = null;
    this.oldName = null;
    this.oldUncaughtExceptionHandler = null;
  }

  /**
   * A lock to be used to modify the task thread safe.
   */
  private final ReentrantLock mutex = new ReentrantLock();

  /**
   * Acquire a lock, but only if the {@link #state()} is {@link State#NEW}.
   *
   * @throws IllegalStateException If the current state is not the one expected.
   */
  protected final void lockAndRequireNew() {
    mutex.lock();
    final State currentState = state.get();
    if (currentState != State.NEW) {
      mutex.unlock();
      throw new IllegalStateException("Found illegal state " + currentState.name() + ", expected NEW");
    }
  }

  /**
   * Unlocks a lock acquired previously via {@link #lockAndRequireNew()}.
   */
  protected final void unlock() {
    mutex.unlock();
  }

  /**
   * Creates a new thread, attach this task to the new thread, then call {@link #init()} followed by an invocation of {@link #execute()} to
   * generate the response.
   *
   * @return The future to the result.
   * @throws IllegalStateException If the {@link #state()} is not {@link State#NEW}.
   * @throws TooManyTasks          If too many tasks are executing already; not thrown for internal tasks.
   * @throws RuntimeException      If adding the task to the thread pool failed for an unknown error.
   */
  public @NotNull Future<@NotNull RESULT> start() {
    final long LIMIT = AbstractTask.limit.get();
    lockAndRequireNew();
    try {
      do {
        final long threadCount = AbstractTask.threadCount.get();
        assert threadCount >= 0L;
        if (!internal && threadCount >= LIMIT) {
          throw new TooManyTasks();
        }
        if (AbstractTask.threadCount.compareAndSet(threadCount, threadCount + 1)) {
          try {
            final Future<RESULT> future = threadPool.submit(this::init_and_execute);
            // TODO HP_QUERY : Wouldn't setting this flag, after submitting task, have concurrency failure
            // risk?
            state.set(State.START);
            return future;
          } catch (RejectedExecutionException e) {
            throw new TooManyTasks();
          } catch (Throwable t) {
            AbstractTask.threadCount.decrementAndGet();
            log.atError()
                .setMessage("Unexpected exception while trying to fork a new thread")
                .setCause(t)
                .log();
            throw new RuntimeException("Internal error while forking new worker thread", t);
          }
        }
        // Conflict, two threads concurrently try to fork.
      } while (true);
    } finally {
      unlock();
    }
  }

  private static final AtomicLong threadCount = new AtomicLong();

  private @NotNull RESULT init_and_execute() {
    assert state.get() == State.START;
    state.set(State.EXECUTE);
    attachToCurrentThread();
    try {
      @NotNull RESULT RESULT;
      try {
        init();
        RESULT = execute();
      } catch (final Throwable t) {
        try {
          RESULT = errorResponse(t);
        } catch (final Throwable ignore) {
          throw t;
        }
      }
      state.set(State.CALLING_LISTENER);
      for (final @NotNull Consumer<@NotNull RESULT> listener : listeners) {
        try {
          listener.accept(RESULT);
        } catch (Throwable t) {
          log.atError()
              .setMessage("Uncaught exception in response listener")
              .setCause(t)
              .log();
        }
      }
      return RESULT;
    } finally {
      state.set(State.DONE);
      final long newValue = AbstractTask.threadCount.decrementAndGet();
      assert newValue >= 0L;
      detachFromCurrentThread();
    }
    /* TODO HP_QUERY : As this function doesn't return response in case of exception, it gets suppressed under
     * thread.submit() function, and API client endlessly waits for response.
     * How do we return errorResponse from here? (return type doesn't match)
     */
  }

  /**
   * A method that creates an error-response from the given exception, being thrown by either {@link #init()} or {@link #execute()}. The
   * default implementation will simply throw the exception again.
   *
   * @param throwable The exception caught.
   * @return The error-response.
   */
  protected @NotNull RESULT errorResponse(@NotNull Throwable throwable) throws Exception {
    log.atWarn()
        .setMessage("The task failed with an exception")
        .setCause(throwable)
        .log();
    if (throwable instanceof Exception e) {
      throw e;
    }
    throw unchecked(throwable);
  }

  /**
   * Initializes this task.
   */
  protected abstract void init();

  /**
   * Execute this task.
   *
   * @return the response.
   */
  protected abstract @NotNull RESULT execute();

  /**
   * Try to cancel the task.
   *
   * @return {@code true} if the task cancelled successfully; {@code false} otherwise.
   */
  public boolean cancel() {
    return false;
  }

  /**
   * The state of the task.
   */
  public enum State {
    /**
     * The task is new.
     */
    NEW,

    /**
     * The task is starting.
     */
    START,

    /**
     * The task is executing.
     */
    EXECUTE,

    /**
     * Done executing and notifying listener.
     */
    CALLING_LISTENER,

    /**
     * Fully done.
     */
    DONE
  }

  private final AtomicReference<@NotNull State> state = new AtomicReference<>(State.NEW);

  /**
   * Returns the current state of the task.
   *
   * @return The current state of the task.
   */
  public final @NotNull State state() {
    return state.get();
  }

  private final @NotNull List<@NotNull Consumer<@NotNull RESULT>> listeners = new ArrayList<>();

  /**
   * Adds the given response listener.
   *
   * @param listener The listener to add.
   * @return {@code true} if added the listener; {@code false} if the listener already added.
   * @throws IllegalStateException If called after {@link #start()}.
   */
  public final boolean addListener(@NotNull Consumer<@NotNull RESULT> listener) {
    lockAndRequireNew();
    try {
      if (!listeners.contains(listener)) {
        listeners.add(listener);
        return true;
      }
      return false;
    } finally {
      state.set(State.NEW);
    }
  }

  /**
   * Remove the given response listener.
   *
   * @param listener The listener to remove.
   * @return {@code true} if removed the listener; {@code false} otherwise.
   * @throws IllegalStateException After {@link #start()} called.
   */
  public final boolean removeListener(@NotNull Consumer<@NotNull RESULT> listener) {
    lockAndRequireNew();
    try {
      // TODO HP_QUERY : Purpose of checking absence before removing?
      if (!listeners.contains(listener)) {
        listeners.remove(listener);
        return true;
      }
      return false;
    } finally {
      state.set(State.NEW);
    }
  }
}
