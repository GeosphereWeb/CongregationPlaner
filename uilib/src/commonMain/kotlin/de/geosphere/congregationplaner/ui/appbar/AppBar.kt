package de.geosphere.congregationplaner.ui.appbar

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.customColors

/**
 * App TopBar Komponente
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
            )
        },
        modifier = modifier,
        navigationIcon =
        if (onNavigationClick != null) {
            {
                IconButton(onClick = onNavigationClick) {
                    Text("<") // Simple back button
                }
            }
        } else {
            {}
        },
        actions = {
            actions()
        },
        colors =
        TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.customColors.btnContainerColor,
            scrolledContainerColor = MaterialTheme.customColors.btnContentColorDisabled,
            navigationIconContentColor = MaterialTheme.customColors.btnContentColor,
            titleContentColor = MaterialTheme.customColors.btnContentColor,
            actionIconContentColor = Color.Yellow
        ),
    )
}

/**
 * Simple App TopBar ohne Navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleAppTopBar(title: String, modifier: Modifier = Modifier) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
            )
        },
        modifier = modifier,
//        colors =
//        TopAppBarDefaults.topAppBarColors(
//            containerColor = MaterialTheme.customColors.werner1,
////            scrolledContainerColor = Color.Unspecified,
////            navigationIconContentColor = Color.Unspecified,
//            titleContentColor = MaterialTheme.customColors.onBrandCustom,
////            actionIconContentColor = Color.Unspecified
//        ),
    )
}

@ThemePreviews
@Composable
private fun WernerPreview() = PreviewThemeWrapper {
    AppTopBar(title = "AppBar", onNavigationClick = {})
}

@ThemePreviews
@Composable
private fun Werner2Preview() = PreviewThemeWrapper {
    SimpleAppTopBar(title = "AppBar")
}
