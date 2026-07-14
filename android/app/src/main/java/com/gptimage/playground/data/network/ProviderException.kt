package com.gptimage.playground.data.network

/**
 * Thrown when an image generation/edit request fails.
 */
class ProviderException(
    val kind: Kind,
    message: String,
    cause: Throwable? = null,
    /** Raw HTTP response body for debugging, masked for safety. */
    val rawBody: String? = null,
    val httpCode: Int? = null
) : RuntimeException(message, cause) {

    enum class Kind {
        NETWORK,
        AUTH,
        RATE_LIMIT,
        BAD_REQUEST,
        SERVER,
        PARSE,
        UNKNOWN
    }

    companion object {
        fun fromHttp(code: Int, body: String?): ProviderException {
            val kind = when (code) {
                401, 403 -> Kind.AUTH
                429 -> Kind.RATE_LIMIT
                in 400..499 -> Kind.BAD_REQUEST
                in 500..599 -> Kind.SERVER
                else -> Kind.UNKNOWN
            }
            val truncated = body?.take(500)
            return ProviderException(
                kind = kind,
                message = "HTTP $code: ${truncated.orEmpty()}",
                rawBody = truncated,
                httpCode = code
            )
        }
    }
}
