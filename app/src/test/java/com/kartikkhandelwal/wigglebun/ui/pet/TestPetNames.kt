package com.kartikkhandelwal.wigglebun.ui.pet

import com.kartikkhandelwal.wigglebun.domain.model.MAX_PET_NAME_LENGTH

/** Pet names at the length limit, for the long-name tests. */
internal object TestPetNames {
    /** A real-looking name of the maximum length: fits the Home chip on a 390 dp phone. */
    val LONG = "Wigglebottom".also { check(it.length == MAX_PET_NAME_LENGTH) }

    /** The widest name allowed: always needs shortening somewhere. */
    val WIDEST = "W".repeat(MAX_PET_NAME_LENGTH)
}
