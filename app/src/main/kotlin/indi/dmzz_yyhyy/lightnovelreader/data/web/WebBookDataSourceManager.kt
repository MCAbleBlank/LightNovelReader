package indi.dmzz_yyhyy.lightnovelreader.data.web

import dalvik.system.PathClassLoader
import indi.dmzz_yyhyy.lightnovelreader.data.plugin.injector.PluginInjector
import indi.dmzz_yyhyy.lightnovelreader.data.userdata.UserDataRepository
import indi.dmzz_yyhyy.lightnovelreader.utils.convertOldId
import indi.dmzz_yyhyy.lightnovelreader.utils.ofId
import io.nightfish.lightnovelreader.api.identifier.Identifier
import io.nightfish.lightnovelreader.api.userdata.UserDataPath
import io.nightfish.lightnovelreader.api.web.WebBookDataSource
import io.nightfish.lightnovelreader.api.web.WebBookDataSourceManagerApi
import io.nightfish.lightnovelreader.api.web.WebDataSource
import io.nightfish.lightnovelreader.api.web.WebDataSourceItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class WebBookDataSourceManager @Inject constructor(
    val userDataRepository: UserDataRepository
) : WebBookDataSourceManagerApi {
    private val _webDataSourceItems = mutableListOf<WebDataSourceItem>()
    private val webDataSourceItemListMap = mutableMapOf<String, List<WebDataSourceItem>>()
    val webDataSourceItems: List<WebDataSourceItem> get() = _webDataSourceItems

    private val mutableWebDataSourceProvider = MutableWebDataSourceProvider()
    private val webBookDataSources = mutableListOf<WebBookDataSource>()
    private val loadedWebDataSourceIds = mutableSetOf<Identifier>()
    private var _enabledDataSourceIds: Set<Identifier> = emptySet()
    private var _aggregationMode: Boolean = false
    private val _sourceConfigVersion = MutableStateFlow(0L)

    /** 数据源启用/主数据源变化时递增, 供界面重新过滤数据 */
    val sourceConfigFlow = _sourceConfigVersion.asStateFlow()

    /** 当前实际参与搜索/探索的数据源 */
    val enabledDataSourceIds: Set<Identifier> get() = _enabledDataSourceIds

    /** 多数据源聚合模式; 关闭时只使用主数据源 */
    val aggregationMode: Boolean get() = _aggregationMode

    override fun registerWebDataSource(
        webBookDataSource: WebBookDataSource,
        webDataSourceItem: WebDataSourceItem
    ) {
        if (_webDataSourceItems.any { it.id == webDataSourceItem.id }) return
        _webDataSourceItems.add(webDataSourceItem)
        webBookDataSources.add(webBookDataSource)
        onWebDataSourceListChange()
    }

    override fun unregisterWebDataSource(webDataSourceId: Identifier) {
        _webDataSourceItems.removeAll { it.id == webDataSourceId }
        webBookDataSources.removeAll { it.id == webDataSourceId }
        loadedWebDataSourceIds.remove(webDataSourceId)
        onWebDataSourceListChange()
    }

    override fun getWebDataSource(): WebBookDataSource = mutableWebDataSourceProvider.value.origin

    fun loadWebDataSourcesFromClassLoader(
        classLoader: PathClassLoader,
        injector: PluginInjector,
        packageName: String,
        webDataSourceClassNames: List<String>
    ) {
        val items = mutableListOf<WebDataSourceItem>()
        webDataSourceClassNames.forEach { className ->
            val clazz =
                runCatching { classLoader.loadClass(className) }.getOrNull() ?: return@forEach
            if (!WebBookDataSource::class.java.isAssignableFrom(clazz)) return@forEach
            val instance = injector.provide<WebBookDataSource>(clazz)
            if (instance is WebBookDataSource) items.add(loadWebDataSourceClass(instance))
        }
        webDataSourceItemListMap[packageName] = items
    }

    fun <T : WebBookDataSource> loadWebDataSourceFromClass(
        clazz: Class<T>,
        injector: PluginInjector
    ) {
        if (!WebBookDataSource::class.java.isAssignableFrom(clazz)) return
        val instance = injector.provide<WebBookDataSource>(clazz)
        if (instance is WebBookDataSource) {
            val item = loadWebDataSourceClass(instance)
            val packageName = clazz.`package`?.name ?: return
            if (webDataSourceItemListMap.contains(packageName)) {
                webDataSourceItemListMap[packageName] =
                    webDataSourceItemListMap[packageName]!! + listOf(item)
            } else {
                webDataSourceItemListMap[packageName] = listOf(item)
            }
        }
    }

    fun loadWebDataSourceClass(instance: WebBookDataSource): WebDataSourceItem {
        val info = instance.javaClass.getAnnotationsByType(WebDataSource::class.java)
        val item = WebDataSourceItem(
            instance.id,
            info.first().name,
            info.first().provider,
        )
        registerWebDataSource(instance, item)
        return item
    }

    fun unloadWebDataSourcesFromClassLoader(packageName: String) {
        webDataSourceItemListMap[packageName]?.let { _webDataSourceItems.removeAll(it) }
    }

    fun getWebDataSourceProvider(): WebBookDataSourceProvider {
        return mutableWebDataSourceProvider
    }

    val primaryDataSourceId: Identifier get() = mutableWebDataSourceProvider.value.id

    fun setPrimaryWebDataSource(sourceId: Identifier) = runBlocking {
        userDataRepository
            .stringUserData(UserDataPath.Settings.Data.WebDataSourceId.path)
            .set(sourceId.toString())
        onWebDataSourceListChange()
    }

    fun onWebDataSourceListChange() = runBlocking {
        val registeredIds = webBookDataSources.map { it.id }.toSet()
        val configuredPrimaryId = userDataRepository
            .stringUserData(UserDataPath.Settings.Data.WebDataSourceId.path)
            .get()
            ?.convertOldId()
        var primaryId = configuredPrimaryId
            ?.takeIf { registeredIds.contains(it) }
            ?: registeredIds.firstOrNull()
            ?: "Wenku8".ofId()

        val aggregation = userDataRepository
            .booleanUserData(UserDataPath.Settings.Data.AggregationMode.path)
            .getOrDefault(false)
        _aggregationMode = aggregation

        // 未设置过启用列表时默认启用全部数据源
        val storedEnabled = userDataRepository
            .stringListUserData(UserDataPath.Settings.Data.EnabledWebDataSourceIds.path)
            .get()
        val userEnabledIds = storedEnabled
            ?.mapNotNull { runCatching { it.convertOldId() }.getOrNull() }
            ?.filter { registeredIds.contains(it) }
            ?.toSet()
            ?: registeredIds

        val enabledIds = if (!aggregation) {
            setOf(primaryId)
        } else {
            if (userEnabledIds.isEmpty()) {
                setOf(primaryId)
            } else if (userEnabledIds.contains(primaryId)) {
                userEnabledIds
            } else {
                primaryId = userEnabledIds.first()
                userDataRepository
                    .stringUserData(UserDataPath.Settings.Data.WebDataSourceId.path)
                    .set(primaryId.toString())
                userEnabledIds
            }
        }
        _enabledDataSourceIds = enabledIds
        _sourceConfigVersion.value += 1

        // 每个已注册的数据源都要初始化一次, 且只初始化一次
        webBookDataSources.forEach { dataSource ->
            if (enabledIds.contains(dataSource.id) &&
                loadedWebDataSourceIds.add(dataSource.id)
            ) {
                dataSource.onLoad()
            }
        }
        mutableWebDataSourceProvider.updateAll(
            webBookDataSources.associateBy { it.id },
            primaryId,
            enabledIds
        )
    }

    /** 切换聚合模式; 关闭时只保留主数据源。 */
    fun setAggregationMode(enabled: Boolean) {
        runBlocking {
            userDataRepository
                .booleanUserData(UserDataPath.Settings.Data.AggregationMode.path)
                .set(enabled)
            onWebDataSourceListChange()
        }
    }

    /** 书架等界面用: 判断某本书是否属于当前启用的数据源。 */
    fun isBookIdEnabled(bookId: String): Boolean {
        val enabledIds = _enabledDataSourceIds
        matchSourceId(
            bookId,
            mutableWebDataSourceProvider.getAll().map { it.id }
        )?.let { (sourceId, _) ->
            return enabledIds.contains(sourceId)
        }
        parseScopedId(bookId)?.let { parsed ->
            val source = mutableWebDataSourceProvider.getAll().firstOrNull { source ->
                sourceIdPrefix(source.id) == "${parsed.sourcePrefix}:" ||
                        "${source.id.namespace}_${source.id.id}:" == "${parsed.sourcePrefix}:"
            }
            if (source != null) return enabledIds.contains(source.id)
        }
        parseLegacyScopedId(bookId)?.let { parsed ->
            return enabledIds.contains(parsed.sourceId.convertOldId())
        }
        // 无前缀的旧书 id 归当前主数据源
        return enabledIds.contains(primaryDataSourceId)
    }

    /**
     * 启用/停用某个数据源。停用当前主数据源时会自动切换到另一个已启用数据源。
     */
    fun setWebDataSourceEnabled(sourceId: Identifier, enabled: Boolean) {
        val current = _enabledDataSourceIds.toMutableSet()
        if (enabled) current.add(sourceId) else current.remove(sourceId)
        runBlocking {
            userDataRepository
                .stringListUserData(UserDataPath.Settings.Data.EnabledWebDataSourceIds.path)
                .set(current.map { it.toString() })
            onWebDataSourceListChange()
        }
    }
}