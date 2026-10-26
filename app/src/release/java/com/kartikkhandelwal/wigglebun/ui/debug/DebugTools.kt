package com.kartikkhandelwal.wigglebun.ui.debug

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder

// Release build: no debug tools. The debug source set has the real versions (Pet lab).

@Composable
fun DebugHomeEntry(@Suppress("UNUSED_PARAMETER") navController: NavController) = Unit

fun NavGraphBuilder.debugDestinations(@Suppress("UNUSED_PARAMETER") navController: NavController) = Unit
