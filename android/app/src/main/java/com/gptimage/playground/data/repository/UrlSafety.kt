package com.gptimage.playground.data.repository

import java.net.URL

/**
 * URL 安全检查工具。移植自 Web 项目：
 * - `src/lib/provider-config.ts` 的 `normalizeOpenAICompatibleBaseUrl`（P0 归一化）
 * - `src/lib/server-url-safety.ts` 的 `validatePublicHttpBaseUrl`（P1 SSRF 防护）
 *
 * 与 Web 端对齐的规则：
 * - 拒绝非 http/https 协议
 * - 拒绝 URL 内嵌 username/password
 * - 拒绝 localhost / .localhost / localhost.localdomain / metadata.google.internal
 * - 拒绝 IPv4 私网/回环/链路本地/保留段（9 个 CIDR）
 * - 拒绝 IPv6 :: / ::1 / fe80: / fc / fd / ::ffff:映射私网
 *
 * 不实现：强制 https、端口限制、IDN/Unicode punycode（与 Web 端对齐，未实现）。
 */
object UrlSafety {

    /** 拒绝的主机名集合（与 Web 端 BLOCKED_HOSTNAMES 对齐）。 */
    private val BLOCKED_HOSTNAMES = setOf(
        "localhost",
        "localhost.localdomain",
        "metadata.google.internal"
    )

    sealed class Result {
        data class Ok(val normalizedUrl: String) : Result()
        data class Bad(val reason: String) : Result()
    }

    /**
     * 归一化 OpenAI 兼容 base url。**纯归一化，不做安全检查**。
     *
     * 行为：
     * - 输入为空 → 返回空
     * - 无协议自动补 https://
     * - 仅 http/https 协议会被处理；其他协议返回原 trimmed 字符串
     * - 清掉 username/password/search/hash
     * - 去掉尾部 /
     * - pathname 为空时补 /v1
     * - 解析失败返回原 trimmed 字符串
     */
    fun normalizeOpenAICompatibleBaseUrl(value: String?): String {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) return ""

        val withProtocol = if (Regex("^[a-z][a-z0-9+.-]*://", RegexOption.IGNORE_CASE).containsMatchIn(trimmed)) {
            trimmed
        } else {
            "https://$trimmed"
        }

        return try {
            val url = URL(withProtocol)
            if (url.protocol != "http" && url.protocol != "https") return trimmed

            var path = url.path.trimEnd('/')
            if (path.isEmpty()) path = "/v1"

            // 重新构造 URL 清掉 userInfo/query/ref
            val reconstructed = URL(url.protocol, url.host, url.port, path)
            reconstructed.toString()
        } catch (e: Exception) {
            trimmed
        }
    }

    /**
     * 校验 base url 是否为公网 http(s) 地址（防 SSRF）。
     * 在用户录入 provider base url 时调用，若返回 [Result.Bad] 应展示错误并阻止保存。
     */
    fun validatePublicHttpBaseUrl(value: String): Result {
        val parsed = try {
            URL(value)
        } catch (e: Exception) {
            try {
                URL("https://$value")
            } catch (e2: Exception) {
                return Result.Bad("Base URL 格式无效。")
            }
        }
        if (parsed.protocol != "http" && parsed.protocol != "https") {
            return Result.Bad("Base URL 只支持 http 或 https 协议。")
        }
        if (!parsed.userInfo.isNullOrEmpty()) {
            return Result.Bad("Base URL 不允许包含用户名或密码。")
        }
        val host = parsed.host.lowercase().trimStart('[').trimEnd(']')
        if (host.isEmpty()) return Result.Bad("Base URL 缺少主机名。")

        if (host in BLOCKED_HOSTNAMES || host.endsWith(".localhost")) {
            return Result.Bad("Base URL 不允许指向 localhost 或本机服务。")
        }

        // IPv4 字面量检查
        if (host.matches(Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+$"))) {
            if (isUnsafeIpv4(host)) {
                return Result.Bad("Base URL 不允许指向私网、链路本地、回环或保留 IPv4 地址。")
            }
        }

        // IPv6 字面量检查
        val v6 = host.lowercase()
        if (v6 == "::" || v6 == "::1" ||
            v6.startsWith("fe80:") || v6.startsWith("fc") || v6.startsWith("fd")
        ) {
            return Result.Bad("Base URL 不允许指向私网、链路本地、回环或保留 IPv6 地址。")
        }
        if (v6.startsWith("::ffff:")) {
            val mapped = v6.removePrefix("::ffff:")
            if (mapped.matches(Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) && isUnsafeIpv4(mapped)) {
                return Result.Bad("Base URL 不允许指向私网、链路本地、回环或保留 IPv6 地址。")
            }
        }

        return Result.Ok(parsed.toString().trimEnd('/'))
    }

    /**
     * 判断 IPv4 是否为私网/回环/链路本地/保留地址。
     * 拒绝的 CIDR：
     * - 0.0.0.0/8
     * - 10.0.0.0/8
     * - 100.64.0.0/10 (CGNAT)
     * - 127.0.0.0/8
     * - 169.254.0.0/16 (链路本地，含云元数据 169.254.169.254)
     * - 172.16.0.0/12
     * - 192.168.0.0/16
     * - 224.0.0.0/4 (组播)
     * - 240.0.0.0/4 (保留)
     */
    private fun isUnsafeIpv4(ip: String): Boolean {
        val parts = ip.split('.').mapNotNull { it.toIntOrNull() }
        if (parts.size != 4 || parts.any { it !in 0..255 }) return false
        val num = (parts[0] shl 24) or (parts[1] shl 16) or (parts[2] shl 8) or parts[3]
        fun inCidr(base: Int, prefix: Int): Boolean {
            val mask = if (prefix == 0) 0 else (-1 shl (32 - prefix))
            return (num and mask) == (base and mask)
        }
        return inCidr(0x00000000, 8) ||
            inCidr(0x0A000000, 8) ||
            inCidr(0x64400000, 10) ||
            inCidr(0x7F000000, 8) ||
            inCidr(0xA9FE0000, 16) ||
            inCidr(0xAC100000, 12) ||
            inCidr(0xC0A80000, 16) ||
            inCidr(0xE0000000, 4) ||
            inCidr(0xF0000000, 4)
    }
}
