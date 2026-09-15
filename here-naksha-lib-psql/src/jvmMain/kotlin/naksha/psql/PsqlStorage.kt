package naksha.psql

import naksha.base.Platform.PlatformCompanion.logger
import naksha.base.fn.Fx2
import naksha.jbon.JbDictionary
import naksha.model.*
import naksha.base.NakshaError.NakshaErrorCompanion.ILLEGAL_STATE
import naksha.base.NakshaError.NakshaErrorCompanion.UNINITIALIZED
import naksha.base.unsupportedOp
import kotlin.reflect.KClass

/**
 * The Java implementation of the [PgStorage], classname `naksha.psql.PsqlStorage`.
 */
open class PsqlStorage : PgStorage(), IStorage {

    override val configKlass: KClass<PgConfig> = PgConfig::class

    override val adminCatalog: PsqlAdminCatalog
        get() = super.adminCatalog as PsqlAdminCatalog

    private var _cluster: PgCluster? = null

    /**
     * The cluster, set by [initStorage].
     * @since 3.0.0
     */
    val cluster: PgCluster
        get() = _cluster ?: throwUninitialized()

    private var _channel: String? = null

    /**
     * The name of the notification channel used.
     *
     * - Will throw [UNINITIALIZED] if read before [initStorage].
     * - Will throw [ILLEGAL_STATE] if change after [initStorage]
     */
    val channel: String
        get() = _channel ?: throwUninitialized()

    override fun initStorage(config: PgConfig, create: Boolean?, upgrade: Boolean?) {
        // Note: We need to initialize cluster first, so that newConnection and adminConnection calls work.
        //       The PsqlAdminMap will use connections!
        var c = _cluster
        if (c == null) {
            logger.info("Create cluster for storage '${id}'")
            val master = PsqlInstance.get(config.masterUri)
            val replicas = mutableListOf<PgInstance>()
            for (replicaUri in config.replicaUris) {
                if (replicaUri == null) continue
                val replica = PsqlInstance.get(replicaUri)
                if (!replicas.contains(replica)) replicas.add(replica)
            }
            c = PsqlCluster(master, replicas)
            _cluster = c
        }
        setAdminMap(newAdminMap(config, create, upgrade))
        adminCatalog.seedAdminHistoryPartitions()
        adminCatalog.start()
    }

    protected open fun newAdminMap(config: PgConfig, create: Boolean?, upgrade: Boolean?): PsqlAdminCatalog
        = PsqlAdminCatalog(this, config, create, upgrade)

    override fun newSession(options: SessionOptions, readOnly: Boolean): PgSession {
        useInitialized()
        return PgSession(this, options, readOnly)
    }

    override fun newStreamSession(options: SessionOptions?): IStreamSession {
        throw unsupportedOp("newStreamSession")
    }

    override fun getDictionary(id: String): JbDictionary? = adminCatalog.getDictionary(id)

    override fun getEncodingDictionary(feature: Any?, context: Any?): JbDictionary? = adminCatalog.getEncodingDictionary(feature, context)

    override fun newConnection(options: SessionOptions, readOnly: Boolean, init: Fx2<PgConnection, String>?): PgConnection
        = cluster.newConnection(options, readOnly, init)

    override fun adminConnection(): PgConnection = newConnection(Naksha.adminOptions, false)

    override fun afterInit() {
        // TODO: Do we need anything?
    }

    override fun shutdownStorage(dropCache: Boolean) {
        // TODO: Do we need anything?
    }
}