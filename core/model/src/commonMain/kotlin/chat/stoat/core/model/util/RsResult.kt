package chat.stoat.core.model.util

// Result class similar to Rust std::result::Result
data class RsResult<V, E>(val value: V?, val error: E?) {
    val ok: Boolean
        get() = value != null

    val err: Boolean
        get() = error != null

    fun unwrap(): V {
        if (value == null) {
            throw IllegalStateException("Called unwrap on RsResult with error")
        }

        return value
    }

    fun unwrapOr(default: V): V {
        if (value == null) {
            return default
        }

        return value
    }

    fun unwrapOrElse(default: () -> V): V {
        if (value == null) {
            return default()
        }

        return value
    }

    fun unwrapError(): E {
        if (error == null) {
            throw IllegalStateException("Called unwrapError on RsResult with value")
        }

        return error
    }

    companion object {
        fun <V, E> ok(value: V): RsResult<V, E> {
            return RsResult(value, null)
        }

        fun <V, E> err(error: E): RsResult<V, E> {
            return RsResult(null, error)
        }
    }
}
