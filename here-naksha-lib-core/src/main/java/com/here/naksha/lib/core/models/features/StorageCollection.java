package com.here.naksha.lib.core.models.features;

import static com.here.naksha.lib.core.models.features.Storage.NUMBER;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.here.naksha.lib.core.INaksha;
import com.here.naksha.lib.core.models.geojson.implementation.Feature;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.NotNull;

/**
 * A collection is a virtual container for features, managed by a {@link Storage}. All collections optionally have a history and transaction
 * log.
 */
@JsonTypeName(value = "StorageCollection")
@AvailableSince(INaksha.v2_0_0)
public class StorageCollection extends Feature {

  // Note: Meta information attached JSON serialized to collection tables in PostgresQL.
  //       COMMENT ON TABLE test IS 'Some table';
  //       SELECT pg_catalog.obj_description('test'::regclass, 'pg_class');
  //       Comments can also be added on other objects, like columns, data types, functions, etc.
  // See:
  // https://stackoverflow.com/questions/17947274/is-it-possible-to-add-table-metadata-in-postgresql

  @AvailableSince(INaksha.v2_0_0)
  public static final String MAX_AGE = "maxAge";

  @AvailableSince(INaksha.v2_0_0)
  public static final String HISTORY = "history";

  @AvailableSince(INaksha.v2_0_0)
  public static final String DELETED_AT = "deleted";

  /**
   * Create a new empty collection.
   *
   * @param id     the identifier of the collection.
   * @param number the storage number, a 40-but unsigned integer generated by the Naksha-Hub. The value zero is reserved for Hub internal
   *               collections.
   */
  @AvailableSince(INaksha.v2_0_0)
  @JsonCreator
  public StorageCollection(@JsonProperty(ID) @NotNull String id, @JsonProperty(NUMBER) long number) {
    super(id);
    this.number = number;
  }

  /**
   * The unique storage identifier, being a 40-bit unsigned integer.
   */
  @AvailableSince(INaksha.v2_0_0)
  @JsonProperty(NUMBER)
  private long number;

  /**
   * The maximum age of the history entries in days. Zero means no history, {@link Long#MAX_VALUE} means unlimited.
   */
  @AvailableSince(INaksha.v2_0_0)
  @JsonProperty(MAX_AGE)
  private long maxAge = Long.MAX_VALUE;

  /**
   * Toggle if the history is enabled.
   */
  @AvailableSince(INaksha.v2_0_0)
  @JsonProperty(HISTORY)
  private boolean history = Boolean.TRUE;

  /**
   * A value greater than zero implies that the collection shall be treated as deleted and represents the UTC Epoch timestamp in
   * milliseconds when the deletion has been done.
   */
  @AvailableSince(INaksha.v2_0_0)
  @JsonProperty(DELETED_AT)
  @JsonInclude(Include.NON_DEFAULT)
  private long deletedAt = 0L;

  /**
   * Returns the storage number, a 40-bit unsigned integer.
   *
   * @return the storage number, a 40-bit unsigned integer.
   */
  public long getNumber() {
    return number;
  }

  /**
   * Returns the maximum age of the storage collection history in days.
   *
   * @return the maximum age of the storage collection history in days.
   */
  public long getMaxAge() {
    return maxAge;
  }

  /**
   * Sets the maximum age of the storage collection history in days.
   *
   * @param maxAge the maximum age of the storage collection history in days.
   */
  public void setMaxAge(long maxAge) {
    this.maxAge = maxAge;
  }

  /**
   * Returns true if the history is currently enabled; false otherwise.
   *
   * @return true if the history is currently enabled; false otherwise.
   */
  public boolean isHistory() {
    return history;
  }

  /**
   * Enable or disable the history.
   *
   * @param history true to enable the history; false to disable it.
   */
  public void setHistory(boolean history) {
    this.history = history;
  }

  /**
   * Returns the UNIX epoch time in milliseconds when to delete the collection; zero or less when the collection has no deletion time.
   *
   * @return the UNIX epoch time in milliseconds when to delete the collection; zero or less when the collection has no deletion time.
   */
  public long getDeletedAt() {
    return deletedAt;
  }

  /**
   * Sets the UNIX epoch time in milliseconds when to delete the collection; zero or less when the collection should never be deleted.
   *
   * @param deletedAt the UNIX epoch time in milliseconds when to delete the collection; zero or less when the collection should never be
   *                  deleted.
   */
  public void setDeletedAt(long deletedAt) {
    this.deletedAt = deletedAt;
  }
}
