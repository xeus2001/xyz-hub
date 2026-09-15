@file:OptIn(ExperimentalJsExport::class)
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package naksha.base

@JsExport
actual open class NakshaException actual constructor(actual val error: NakshaError) : RuntimeException(error.msg, error.cause) {
    @JsName("ofException")
    actual constructor(code: String, msg: String, cause: Throwable?) : this(NakshaError(code, msg, cause))

    @JsName("of")
    actual constructor(code: String, msg: String) : this(NakshaError(code, msg, null))
}