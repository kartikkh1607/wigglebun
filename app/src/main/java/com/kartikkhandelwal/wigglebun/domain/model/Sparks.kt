package com.kartikkhandelwal.wigglebun.domain.model

import java.time.Instant

/**
 * Why sparks moved (CLAUDE.md §5.4). Stored by name in spark_ledger, so new values need no
 * migration. UNLOCK rows are negative (adopting a pet); DEBUG only comes from the Pet lab.
 */
enum class SparkReason { BREAK, STREAK, UNLOCK, DEBUG }

/** A completed break, as the streak calculator needs it. */
data class DoneBreak(val type: BreakType, val at: Instant)
