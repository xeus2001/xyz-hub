package com.here.naksha.lib.core.models.hub.plugins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.here.naksha.lib.core.INaksha;
import com.here.naksha.lib.core.models.geojson.implementation.Feature;
import com.here.naksha.lib.core.storage.IStorage;
import java.lang.reflect.InvocationTargetException;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.NotNull;

/**
 * The configuration of a storage. Storages are internally used to access and modify features and
 * collection.
 */
@AvailableSince(INaksha.v2_0_0)
@JsonTypeName(value = "Storage")
public class Storage extends Feature implements IPlugin<IStorage> {

    @AvailableSince(INaksha.v2_0_0)
    public static final String NUMBER = "number";

    @AvailableSince(INaksha.v2_0_0)
    public static final String CLASS_NAME = "className";

    /**
     * Create a new storage.
     *
     * @param id the unique identifier of the storage (selected by the user).
     * @param number the unique identifier (40-bit unsigned integer) that is auto-generated by the
     *     Naksha-Hub.
     * @param cla$$ the class, that implement the {@link IStorage} API.
     */
    @AvailableSince(INaksha.v2_0_0)
    public Storage(@NotNull String id, long number, @NotNull Class<? extends IStorage> cla$$) {
        super(id);
        this.number = number;
        this.className = cla$$.getName();
    }

    /**
     * Create a new empty storage.
     *
     * @param id the unique identifier of the storage (selected by the user).
     * @param number the unique identifier (40-bit unsigned integer) that is auto-generated by the
     *     Naksha-Hub.
     * @param className the full qualified name of the class to load for this storage. The class need
     *     to implement the {@link IStorage} API.
     */
    @AvailableSince(INaksha.v2_0_0)
    @JsonCreator
    public Storage(
            @JsonProperty(ID) @NotNull String id,
            @JsonProperty(NUMBER) long number,
            @JsonProperty(CLASS_NAME) @NotNull String className) {
        super(id);
        this.number = number;
        this.className = className;
    }

    /** The unique storage number, being a 40-bit unsigned integer. */
    @AvailableSince(INaksha.v2_0_0)
    @JsonProperty(NUMBER)
    public long number;

    /** The classname to load. */
    @AvailableSince(INaksha.v2_0_0)
    @JsonProperty(CLASS_NAME)
    public @NotNull String className;

    /**
     * Initialize the storage engine, invoked from the Naksha-Hub when creating a new instance of the
     * storage. This should ensure that the storage is accessible and in a good state. If the method
     * fails, it is invoked again after a couple of minutes. This method is invoked at least ones for
     * every service start and therefore must be concurrency safe, because it may be called in
     * parallel by multiple Naksha-Hub instances.
     */
    @Override
    public @NotNull IStorage newInstance() throws Exception {
        try {
            // TODO: Keep storage engines in memory and only instantiate the same storage ones!
            return PluginCache.newInstance(className, IStorage.class, this);
        } catch (InvocationTargetException ite) {
            if (ite.getCause() instanceof Exception e) {
                throw e;
            }
            throw ite;
        }
    }
}
