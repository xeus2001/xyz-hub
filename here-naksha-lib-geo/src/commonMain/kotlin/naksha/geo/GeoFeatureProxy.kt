@file:Suppress("OPT_IN_USAGE")

package naksha.geo

import naksha.base.NotNullProperty
import naksha.base.NullableProperty
import naksha.base.ObjectProxy
import naksha.base.PlatformUtil
import kotlin.js.JsExport

/**
 * The data mode for a GeoJSON feature.
 */
@JsExport
open class GeoFeatureProxy : ObjectProxy() {

    companion object {
        private val ID = NotNullProperty<Any, GeoFeatureProxy, String>(String::class) { _, _ -> PlatformUtil.randomString(12) }
        private val TYPE = NotNullProperty<Any, GeoFeatureProxy, String>(String::class) { self, _ -> self.typeDefaultValue() }
        private val BBOX = NullableProperty<Any, GeoFeatureProxy, BoundingBoxProxy>(BoundingBoxProxy::class)
        private val GEOMETRY = NotNullProperty<Any, GeoFeatureProxy, GeometryProxy>(GeometryProxy::class) { _, _ ->
            throw IllegalStateException("geometry is null")
        }
    }

    /**
     * The default type to set, when the type is _null_.
     */
    protected open fun typeDefaultValue(): String = "Feature"

    /**
     * The unique identifier of the feature.
     */
    var id by ID

    /**
     * The bounding box.
     */
    var bbox by BBOX

    /**
     * The geometry of the feature.
     */
    var geometry by GEOMETRY

    /**
     * The type of the feature.
     */
    var type by TYPE

    /**
     * Calculate the bounding box from the geometry and updated the [bbox] property.
     */
    fun updateBoundingBox(): BoundingBoxProxy {
        TODO("GeoFeature::updateBoundingBox is not yet implemented")
    }
}