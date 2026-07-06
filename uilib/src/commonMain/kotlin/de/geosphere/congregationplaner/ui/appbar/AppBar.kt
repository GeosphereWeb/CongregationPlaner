package de.geosphere.congregationplaner.ui.appbar

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.geosphere.congregationplaner.theming.AppColors

/**
 * App TopBar Komponente
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                color = AppColors.OnPrimary
            )
        },
        modifier = modifier,
        navigationIcon = if (onNavigationClick != null) {
            {
                IconButton(onClick = onNavigationClick) {
                    Text("<")  // Simple back button
                }
            }
        } else {
            {}
        },
        actions = {
            actions()
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = AppColors.Primary,
            titleContentColor = AppColors.OnPrimary,
            navigationIconContentColor = AppColors.OnPrimary
        )
    )
}

/**
 * Simple App TopBar ohne Navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleAppTopBar(
    title: String,
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                color = AppColors.OnPrimary
            )
        },
        modifier = modifier,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = AppColors.Primary,
            titleContentColor = AppColors.OnPrimary
        )
    )
}
