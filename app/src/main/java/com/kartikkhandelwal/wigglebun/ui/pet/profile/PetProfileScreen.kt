package com.kartikkhandelwal.wigglebun.ui.pet.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.ui.components.EmptyScreen
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

@Composable
fun PetProfileScreen(modifier: Modifier = Modifier) {
    EmptyScreen(title = stringResource(R.string.pet_default_name), modifier = modifier)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 760)
@Composable
private fun PetProfileScreenPreview() {
    WigglebunTheme {
        PetProfileScreen()
    }
}
