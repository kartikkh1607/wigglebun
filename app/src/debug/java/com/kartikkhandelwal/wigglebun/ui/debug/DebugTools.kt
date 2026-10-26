package com.kartikkhandelwal.wigglebun.ui.debug

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButtonStyle
import kotlinx.serialization.Serializable

// Debug build only (src/debug). The release source set has no-op versions, so none of this
// ships in the Play build.

@Serializable
data object PetLabRoute

@Composable
fun DebugHomeEntry(navController: NavController) {
    ChunkyButton(
        text = stringResource(R.string.pet_lab_open),
        onClick = { navController.navigate(PetLabRoute) },
        style = ChunkyButtonStyle.Secondary,
    )
}

fun NavGraphBuilder.debugDestinations(@Suppress("UNUSED_PARAMETER") navController: NavController) {
    composable<PetLabRoute> { PetLabScreen() }
}
