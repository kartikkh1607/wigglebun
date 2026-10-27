package com.kartikkhandelwal.wigglebun.domain.model

import java.time.Instant

/** Why sparks moved (CLAUDE.md §5.4). Stored by name in spark_ledger. */
enum class SparkReason { BREAK, STREAK }

/** A completed break, as the streak calculator needs it. */
data class DoneBreak(val type: BreakType, val at: Instant)
