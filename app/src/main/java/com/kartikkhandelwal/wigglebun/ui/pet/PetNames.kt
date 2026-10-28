package com.kartikkhandelwal.wigglebun.ui.pet

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet

/** How the active pet is drawn. */
val ActivePet.spec: PetSpec get() = PetSpecs.of(id)

/** The user's name for Bun (M9), otherwise the pet's own name. */
@Composable
fun ActivePet.displayName(): String = customName ?: stringResource(spec.defaultName)

/** [displayName] outside Compose (notification, widget). */
fun ActivePet.displayName(context: Context): String = customName ?: context.getString(spec.defaultName)
