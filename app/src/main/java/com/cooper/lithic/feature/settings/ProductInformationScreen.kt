package com.cooper.lithic.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun CalculationExplanationScreen(onBack: () -> Unit) {
    InformationPage("成本计算说明", "calculation-explanation-screen", onBack) {
        InformationSections(calculationExplanation)
    }
}

@Composable
internal fun AboutLithicEntry(onBack: () -> Unit) {
    val context = LocalContext.current
    val info = remember(context) { readInstalledAppInformation(context) }
    AboutLithicScreen(info, onBack)
}

@Composable
internal fun AboutLithicScreen(info: InstalledAppInformation, onBack: () -> Unit) {
    InformationPage("关于砺石", "about-lithic-screen", onBack) {
        Text(info.applicationName ?: "应用名称暂无法读取", style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag("installed-app-name"))
        Text(info.versionLabel, modifier = Modifier.testTag("installed-app-version"))
        Text(info.buildLabel, modifier = Modifier.testTag("installed-app-build"))
        HorizontalDivider()
        InformationSections(productSafetyExplanation)
    }
}

@Composable
private fun InformationPage(title: String, tag: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag(tag),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .testTag("information-back")) { Text("返回设置") }
        Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        content()
    }
}

@Composable
private fun InformationSections(sections: List<InformationSection>) {
    sections.forEach { section ->
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(section.title, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() })
            section.paragraphs.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
        }
    }
}
