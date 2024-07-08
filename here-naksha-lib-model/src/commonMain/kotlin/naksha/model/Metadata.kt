@file:Suppress("OPT_IN_USAGE")

package naksha.model

import naksha.base.Int64
import kotlin.js.JsExport

/**
 * The metadata about a [Row] generated by the storage and read-only for the client.
 */
@JsExport
data class Metadata(
    val updatedAt: Int64,
    val createdAt: Int64 = updatedAt,
    val authorTs: Int64 = createdAt,
    val txnNext: Int64? = null,
    val txn: Int64,
    val ptxn: Int64? = null,
    val uid: Int,
    val puid: Int = 0,
    val fnva1: Int,
    val version: Int = 1,
    val geoGrid: Int,
    val flags: Int,
    val origin: String? = null,
    val appId: String,
    val author: String?,
    val type: String? = null,
    val id: String
)