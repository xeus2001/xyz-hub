@file:OptIn(ExperimentalJsExport::class)

package com.here.naksha.lib.jbon

import naksha.base.P_DataView
import naksha.base.P_JsMap
import naksha.base.Platform
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/**
 * Represents feature held in naksha~transaction.feature.
 */
@JsExport
class NakshaTransaction(dictManager: IDictManager) : JbFeature(dictManager) {
    var modifiedFeatureCount: Int = 0
    var collectionCounters: P_JsMap = P_JsMap() // TODO: If we stick with this class, make the map a Map<String, Int>!
    var seqNumber: Int? = null

    fun addModifiedCount(count: Int) {
        modifiedFeatureCount += count
    }

    fun addCollectionCounts(collection: String, count: Int) {
        if (!collectionCounters.containsKey(collection)) {
            collectionCounters.put(collection, count)
        } else {
            val oldCount: Int = collectionCounters.getAs(collection, Int::class)!!
            collectionCounters.put(collection, oldCount + count)
        }
    }

    fun toBytes(): ByteArray {
        // FIXME maybe we should keep all in header?
        val map = P_JsMap()
        map["modifiedFeatureCount"] = modifiedFeatureCount
        map["collectionCounters"] = collectionCounters
        map["seqNumber"] = seqNumber
        return JbBuilder().buildFeatureFromMap(map)
    }
}