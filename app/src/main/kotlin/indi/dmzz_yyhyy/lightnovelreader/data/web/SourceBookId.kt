package indi.dmzz_yyhyy.lightnovelreader.data.web

import io.nightfish.lightnovelreader.api.identifier.Identifier

/**
 * 数据源隔离后的本地 id。
 *
 * 所有会落库、会在页面之间传递的远端字符串 id（书、卷、章节、上一章/下一章等）
 * 都统一加上插件/数据源自己的 id 作为前缀：`插件id:原始id`。
 * 例如 `lightnovelreader:Wenku8:12345`。
 *
 * 旧数据有三种形式：没有前缀的原始 id、早期 `数据源id|原始id`、
 * 以及上一版的 `命名空间_数据源id:原始id`。解析时都会兼容，新写入统一用新格式。
 */
private const val ID_SEPARATOR = ':'
private const val LEGACY_SEPARATOR = '|'

/** 数据源前缀，包含结尾的分隔符，例如 `lightnovelreader:Wenku8:` */
fun sourceIdPrefix(sourceId: Identifier): String =
    "$sourceId$ID_SEPARATOR"

/**
 * 在一组已知数据源中匹配 id 前缀，返回所属数据源和去掉前缀后的原始 id。
 * 使用最长的前缀，避免命名空间/数据源 id 互为前缀时误判。
 */
fun matchSourceId(
    id: String,
    sourceIds: Iterable<Identifier>
): Pair<Identifier, String>? =
    sourceIds.mapNotNull { sourceId ->
        val prefix = sourceIdPrefix(sourceId)
        if (id.length > prefix.length && id.startsWith(prefix)) {
            Triple(sourceId, prefix, id.substring(prefix.length))
        } else null
    }.maxByOrNull { it.second.length }
        ?.let { it.first to it.third }

fun qualifyId(sourceId: Identifier, rawId: String): String {
    if (rawId.isEmpty()) return rawId
    val prefix = sourceIdPrefix(sourceId)
    return if (rawId.startsWith(prefix)) rawId else prefix + rawId
}

fun isIdQualifiedBy(sourceId: Identifier, id: String): Boolean =
    id.startsWith(sourceIdPrefix(sourceId))

/** 去掉指定数据源的前缀；没有前缀的旧 id 原样返回。 */
fun stripSourcePrefix(sourceId: Identifier, id: String): String =
    id.removePrefix(sourceIdPrefix(sourceId))

/** 上一版 `命名空间_数据源id:原始id` 的 id 拆分结果。 */
data class ScopedId(
    val sourcePrefix: String,
    val rawId: String
)

/** 仅按结构拆分旧格式，prefix 是否对应真实数据源由调用方校验。 */
fun parseScopedId(id: String): ScopedId? {
    val index = id.indexOf(ID_SEPARATOR)
    if (index <= 0 || index == id.lastIndex) return null
    val prefix = id.substring(0, index)
    if (!prefix.contains('_')) return null
    return ScopedId(prefix, id.substring(index + 1))
}

/** 兼容早期 `数据源id|原始id` 的书本 id。 */
data class LegacyScopedId(
    val sourceId: String,
    val rawId: String
)

fun parseLegacyScopedId(id: String): LegacyScopedId? {
    val index = id.indexOf(LEGACY_SEPARATOR)
    if (index <= 0 || index == id.lastIndex) return null
    return LegacyScopedId(
        sourceId = id.substring(0, index),
        rawId = id.substring(index + 1)
    )
}
