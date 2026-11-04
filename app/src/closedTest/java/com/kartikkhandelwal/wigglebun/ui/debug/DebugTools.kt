package com.kartikkhandelwal.wigglebun.ui.debug

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder

// Closed-test build: no debug tools, same as release (src/release). The debug source set has the
// real versions (Pet lab).

@Composable
fun DebugHomeEntry(@Suppress("UNUSED_PARAMETER") navController: NavController) = Unit

fun NavGraphBuilder.debugDestinations(@Suppress("UNUSED_PARAMETER") navController: NavController) = Unit
