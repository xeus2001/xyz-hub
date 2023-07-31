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
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A task that executes in an own dedicated thread to fulfill some job. The currently executed task can be queried using
 * {@link #currentTask()}.
 *
 * @param <RESPONSE> The response to be generated by the task.
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public abstract class AbstractTask<RESPONSE> extends NakshaContext implements INakshaBound, UncaughtExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(AbstractTask.class);

  /**
   * The soft-limit of tasks to run concurrently.
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
   * @param naksha The reference to the Naksha host.
   */
  public AbstractTask(@NotNull INaksha naksha) {
    this.naksha = naksha;
    this.attachments = new ConcurrentHashMap<>();
    this.streamId = RandomStringUtils.randomAlphanumeric(12);
  }

  private final @NotNull INaksha naksha;

  @Override
  public @NotNull INaksha naksha() {
    return naksha;
  }

  /**
   * Returns the start time of the task in nanoseconds.
   *
   * @return The start time of the task in nanoseconds.
   */
  public long getStartNanos() {
    return startNanos;
  }

  /**
   * The uncaught exception handler for the thread that executes.
   *
   * @param thread the thread.
   * @param t      the exception.
   */
  public void uncaughtException(Thread thread, Throwable t) {
    log.atError()
        .setMessage("Uncaught exception in task {}")
        .addArgument(getClass().getName())
        .setCause(t)
        .log();
  }

  /**
   * Returns all task attachments.
   *
   * @return The task attachments.
   */
  public @NotNull ConcurrentHashMap<@NotNull Object, Object> attachments() {
    return attachments;
  }

  /**
   * Returns the value for the give type; if it exists.
   *
   * @param valueClass the class of the value type.
   * @param <T>        the value type.
   * @return the value or {@code null}.
   */
  public <T> @Nullable T getAttachment(@NotNull Class<T> valueClass) {
    final @NotNull ConcurrentHashMap<@NotNull Object, Object> attachments = attachments();
    final Object value = attachments.get(valueClass);
    return valueClass.isInstance(value) ? valueClass.cast(value) : null;
  }

  /**
   * Returns the value for the give type. This method simply uses the given class as key in the {@link #attachments()} and expects that the
   * value is of the same type. If the value is {@code null} or of a wrong type, the method will create a new instance of the given value
   * class and store it in the attachments, returning the new instance.
   *
   * @param valueClass the class of the value type.
   * @param <T>        the value type.
   * @return the value.
   * @throws NullPointerException if creating a new value instance failed.
   */
  public <T> @NotNull T getOrCreateAttachment(@NotNull Class<T> valueClass) {
    final @NotNull ConcurrentHashMap<@NotNull Object, Object> attachments = attachments();
    while (true) {
      final Object o = attachments.get(valueClass);
      if (valueClass.isInstance(o)) {
        return valueClass.cast(o);
      }
      final T newValue;
      try {
        newValue = valueClass.getDeclaredConstructor().newInstance();
      } catch (Exception e) {
        throw new NullPointerException();
      }
      final Object existingValue = attachments.putIfAbsent(valueClass, newValue);
      if (existingValue == null) {
        return newValue;
      }
      if (valueClass.isInstance(existingValue)) {
        return valueClass.cast(existingValue);
      }
      // Overwrite the existing value, because it is of the wrong type.
      if (attachments.replace(valueClass, existingValue, newValue)) {
        return newValue;
      }
      // Conflict, two threads seem to want to update the same key the same time!
    }
  }

  /**
   * Sets the given value in the {@link #attachments()} using the class of the value as key.
   *
   * @param value the value to set.
   * @return the key.
   * @throws NullPointerException if the given value is null.
   */
  @SuppressWarnings("unchecked")
  public <T> @NotNull Class<T> setAttachment(@NotNull T value) {
    //noinspection ConstantConditions
    if (value == null) {
      throw new NullPointerException();
    }
    attachments().put(value.getClass(), value);
    return (Class<T>) value.getClass();
  }

  /**
   * The attachments of this context.
   */
  private final @NotNull ConcurrentHashMap<@NotNull Object, Object> attachments;

  /**
   * The steam-id of this task.
   */
  private @NotNull String streamId;

  @Override
  public @NotNull String getStreamId() {
    return streamId;
  }

  /**
   * Sets the stream-identifier of this task.
   *
   * @param streamId The stream-identifier to set.
   * @return The old stream-identifier.
   */
  public @NotNull String setStreamId(@NotNull String streamId) {
    final String old = this.streamId;
    this.streamId = streamId;
    if (thread != null) {
      thread.setName(streamId);
    }
    return old;
  }

  /**
   * A flag to signal that this task is internal.
   */
  protected boolean internal;

  /**
   * Flag this task as internal, so when starting the task, the maximum amount of parallel tasks {@link #limit} should be ignored.
   *
   * @param internal {@code true} if this task is internal and therefore bypassing the maximum parallel tasks limit.
   * @throws IllegalStateException If the task is not in the state {@link State#NEW}.
   */
  public void setInternal(boolean internal) {
    lockAndRequireNew();
    try {
      this.internal = internal;
    } finally {
      unlock();
    }
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
   * The thread to which this context is currently bound; if any.
   */
  @Nullable
  Thread thread;

  /**
   * The previously set uncaught exception handler.
   */
  @Nullable
  Thread.UncaughtExceptionHandler oldUncaughtExceptionHandler;

  @Nullable
  String oldName;

  /**
   * Returns the task attached to the current thread; if any.
   *
   * @return The task attached to the current thread or {@code null}, if the current thread has no task attached.
   * @throws ClassCastException if the task is not of the expected type.
   */
  @SuppressWarnings("unchecked")
  public static <T extends AbstractTask<?>> @Nullable T currentTask() {
    final Thread thread = Thread.currentThread();
    final UncaughtExceptionHandler uncaughtExceptionHandler = thread.getUncaughtExceptionHandler();
    if (uncaughtExceptionHandler instanceof AbstractTask<?> task) {
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
   * @throws IllegalStateException If this task is bound to another thread, or the current thread is bound to another task.
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
    thread.setName(streamId);
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
  public @NotNull Future<@NotNull RESPONSE> start() {
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
            final Future<RESPONSE> future = threadPool.submit(this::init_and_execute);
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

  private @NotNull RESPONSE init_and_execute() throws Exception {
    assert state.get() == State.START;
    state.set(State.EXECUTE);
    attachToCurrentThread();
    try {
      @NotNull RESPONSE response;
      try {
        init();
        response = execute();
      } catch (final Throwable t) {
        try {
          response = errorResponse(t);
        } catch (final Throwable ignore) {
          throw t;
        }
      }
      state.set(State.CALLING_LISTENER);
      for (final @NotNull Consumer<@NotNull RESPONSE> listener : listeners) {
        try {
          listener.accept(response);
        } catch (Throwable t) {
          log.atError()
              .setMessage("Uncaught exception in response listener")
              .setCause(t)
              .log();
        }
      }
      return response;
    } finally {
      state.set(State.DONE);
      final long newValue = AbstractTask.threadCount.decrementAndGet();
      assert newValue >= 0L;
      detachFromCurrentThread();
    }
  }

  /**
   * A method that creates an error-response from the given exception, being thrown by either {@link #init()} or {@link #execute()}. The
   * default implementation will simply throw the exception again.
   *
   * @param throwable The exception caught.
   * @return The error-response.
   */
  protected @NotNull RESPONSE errorResponse(@NotNull Throwable throwable) throws Exception {
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
  protected abstract @NotNull RESPONSE execute();

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

  private final @NotNull List<@NotNull Consumer<@NotNull RESPONSE>> listeners = new ArrayList<>();

  /**
   * Adds the given response listener.
   *
   * @param listener The listener to add.
   * @return {@code true} if added the listener; {@code false} if the listener already added.
   * @throws IllegalStateException If called after {@link #start()}.
   */
  public final boolean addListener(@NotNull Consumer<@NotNull RESPONSE> listener) {
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
  public final boolean removeListener(@NotNull Consumer<@NotNull RESPONSE> listener) {
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
