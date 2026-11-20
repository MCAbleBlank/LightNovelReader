package indi.dmzz_yyhyy.lightnovelreader.ui.home.settings.pluginmanager

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import indi.dmzz_yyhyy.lightnovelreader.R
import indi.dmzz_yyhyy.lightnovelreader.data.plugin.PluginMetadata
import indi.dmzz_yyhyy.lightnovelreader.ui.components.PluginCard
import indi.dmzz_yyhyy.lightnovelreader.ui.components.SectionDescription
import indi.dmzz_yyhyy.lightnovelreader.ui.components.SectionHeader
import indi.dmzz_yyhyy.lightnovelreader.utils.LocalClaimSnackbarHost
import indi.dmzz_yyhyy.lightnovelreader.utils.LocalSnackbarHost
import io.nightfish.lightnovelreader.api.identifier.Identifier
import io.nightfish.lightnovelreader.api.web.WebDataSourceItem
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginManagerScreen(
    enabledPluginList: List<String>,
    errorMessageMap: Map<String, String>,
    updateVersionNames: Map<String, String>,
    getPluginFile: (String) -> File,
    onClickInstall: () -> Unit,
    onClickBack: () -> Unit,
    onClickPluginApps: () -> Unit,
    onClickDetail: (String) -> Unit,
    onClickDelete: (String, Boolean) -> Unit,
    onClickSwitch: (PluginMetadata) -> Unit,
    onClickKeyAlert: () -> Unit,
    onClickErrorAlert: () -> Unit,
    onClickIncompatibleAlert: () -> Unit,
    onClickCheckUpdate: (String) -> Unit,
    pluginInfoList: List<PluginMetadata>,
    onClickShowSignatures: (String) -> Unit,
    dataSourceItems: List<WebDataSourceItem>,
    enabledDataSourceIds: Set<Identifier>,
    aggregationMode: Boolean,
    primaryDataSourceId: Identifier,
    onAggregationModeChange: (Boolean) -> Unit,
    onDataSourceEnabledChange: (Identifier, Boolean) -> Unit,
    onPrimaryDataSourceChange: (Identifier) -> Unit
) {
    val enterAlwaysScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val claim = LocalClaimSnackbarHost.current

    DisposableEffect(Unit) {
        claim(true)
        onDispose { claim(false) }
    }

    Scaffold(
        topBar = {
            TopBar(
                onClickBack = onClickBack,
                scrollBehavior = enterAlwaysScrollBehavior,
                onClickPluginApps = onClickPluginApps,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(end = 12.dp, bottom = 24.dp),
                onClick = onClickInstall,
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.archive_24px),
                        contentDescription = "install"
                    )
                },
                text = {
                    Text(text = stringResource(R.string.plugin_install_plugin))
                }
            )
        },
        snackbarHost = {
            SnackbarHost(LocalSnackbarHost.current)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .nestedScroll(enterAlwaysScrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                SectionHeader(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp, bottom = 2.dp),
                    text = stringResource(R.string.settings_select_data_source)
                )
            }
            item {
                SectionDescription(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 8.dp),
                    text = stringResource(
                        if (aggregationMode) R.string.settings_data_source_section_desc_aggregate
                        else R.string.settings_data_source_section_desc_single
                    )
                )
            }
            item {
                DataSourceCard(
                    items = dataSourceItems,
                    enabledIds = enabledDataSourceIds,
                    aggregationMode = aggregationMode,
                    primaryId = primaryDataSourceId,
                    onAggregationModeChange = onAggregationModeChange,
                    onEnabledChange = onDataSourceEnabledChange,
                    onPrimaryChange = onPrimaryDataSourceChange
                )
            }
            item {
                SectionHeader(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 8.dp),
                    text = stringResource(R.string.settings_plugins)
                )
            }
            item {
                GetPluginsFromStoreTips()
            }
            item {
                ThirdPartyPluginTips()
            }
            if (pluginInfoList.isEmpty()) {
                item {
                    EmptyPlugins()
                }
            } else {
                items(pluginInfoList) { plugin ->
                    PluginCard(
                        modifier = Modifier.animateItem(),
                        pluginInfo = plugin,
                        pluginFile = getPluginFile(plugin.packageName),
                        updateVersionName = updateVersionNames[plugin.packageName],
                        onClickDetail = onClickDetail,
                        enabledPluginList = enabledPluginList,
                        isErrorDisabled = errorMessageMap.containsKey(plugin.packageName),
                        onClickSwitch = onClickSwitch,
                        onClickDelete = onClickDelete,
                        onClickCheckUpdate = onClickCheckUpdate,
                        onClickKeyAlert = onClickKeyAlert,
                        onClickErrorAlert = onClickErrorAlert,
                        onClickIncompatibleAlert = onClickIncompatibleAlert,
                        onClickShowSignatures = onClickShowSignatures
                    )
                }
            }
        }
    }
}

val horizontalPadding = 16.dp

@Composable
private fun DataSourceCard(
    items: List<WebDataSourceItem>,
    enabledIds: Set<Identifier>,
    aggregationMode: Boolean,
    primaryId: Identifier,
    onAggregationModeChange: (Boolean) -> Unit,
    onEnabledChange: (Identifier, Boolean) -> Unit,
    onPrimaryChange: (Identifier) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        shape = RoundedCornerShape(18.dp),
        color = colorScheme.surfaceContainer
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        onAggregationModeChange(!aggregationMode)
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_aggregation_mode),
                        style = typography.titleMedium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_aggregation_mode_desc),
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = aggregationMode,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        onAggregationModeChange(it)
                    }
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            if (items.isEmpty()) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    text = stringResource(R.string.nothing_here),
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
            items.forEachIndexed { index, item ->
                SourceRow(
                    item = item,
                    enabled = enabledIds.contains(item.id),
                    selected = item.id == primaryId,
                    aggregationMode = aggregationMode,
                    onEnabledChange = onEnabledChange,
                    onPrimaryChange = onPrimaryChange
                )
                if (index != items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                        color = colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceRow(
    item: WebDataSourceItem,
    enabled: Boolean,
    selected: Boolean,
    aggregationMode: Boolean,
    onEnabledChange: (Identifier, Boolean) -> Unit,
    onPrimaryChange: (Identifier) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                if (aggregationMode) onEnabledChange(item.id, !enabled)
                else onPrimaryChange(item.id)
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(12.dp),
            color = colorScheme.surfaceContainerHighest
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.public_24px),
                    contentDescription = null,
                    tint = colorScheme.primary
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = item.name,
                style = typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.data_source_provider, item.provider),
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (aggregationMode) {
            Switch(
                checked = enabled,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    onEnabledChange(item.id, it)
                }
            )
        } else {
            RadioButton(
                selected = selected,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    onPrimaryChange(item.id)
                }
            )
        }
    }
}

@Composable
private fun EmptyPlugins() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            painter = painterResource(R.drawable.deployed_code_update_24px),
            contentDescription = null,
            tint = colorScheme.secondary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.plugin_empty_title),
            style = typography.titleMedium,
            color = colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.plugin_empty_desc),
            style = typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ThirdPartyPluginTips() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = horizontalPadding),
        shape = RoundedCornerShape(18.dp),
        color = colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                painter = painterResource(id = R.drawable.info_24px),
                contentDescription = "warning",
                tint = colorScheme.error
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.plugin_third_party_tips),
                style = typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GetPluginsFromStoreTips() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = horizontalPadding)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                val intent = Intent(Intent.ACTION_VIEW, "https://plugins.nariko.org".toUri())
                context.startActivity(intent, null)
            },
        shape = RoundedCornerShape(18.dp),
        color = colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.archive_24px),
                contentDescription = "archive",
                tint = colorScheme.primary
            )
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.plugin_store_get_plugins),
                style = typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(id = R.drawable.open_in_new_24px),
                contentDescription = "open",
                tint = colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    onClickBack: () -> Unit,
    onClickPluginApps: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.settings_sources_and_plugins),
                style = typography.displayLarge,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onClickBack) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back_24px),
                    contentDescription = "back"
                )
            }
        },
        actions = {
            IconButton(
                onClick = onClickPluginApps
            ) {
                Icon(
                    painter = painterResource(R.drawable.deployed_code_24px),
                    contentDescription = "plugin apps",
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}
