@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package naksha.base

actual open class NakshaException actual constructor(actual val error: NakshaError) : RuntimeException(error.msg, error.cause) {
    actual constructor(code: String, msg: String, cause: Throwable?) : this(NakshaError(code, msg, cause))
    actual constructor(code: String, msg: String) : this(NakshaError(code, msg, null))
}