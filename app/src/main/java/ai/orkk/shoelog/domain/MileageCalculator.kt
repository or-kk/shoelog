package ai.orkk.shoelog.domain

object MileageCalculator {
    fun calculate(
        initialMeters: Long,
        assignedMeters: Iterable<Long?>,
        targetMeters: Long,
    ): MileageSummary {
        val safeInitial = initialMeters.coerceAtLeast(0)
        val assignedTotal = assignedMeters.sumOf { distance -> distance?.coerceAtLeast(0) ?: 0 }
        val total = safeInitial + assignedTotal

        if (targetMeters <= 0) {
            return MileageSummary(
                totalMeters = total,
                remainingMeters = 0,
                progress = 0f,
                usageRatio = 0f,
                status = MileageStatus.NORMAL,
            )
        }

        val usageRatio = total.toFloat() / targetMeters.toFloat()
        val status = when {
            usageRatio >= 1f -> MileageStatus.TARGET_REACHED
            usageRatio >= 0.9f -> MileageStatus.NEAR_TARGET
            else -> MileageStatus.NORMAL
        }
        return MileageSummary(
            totalMeters = total,
            remainingMeters = (targetMeters - total).coerceAtLeast(0),
            progress = usageRatio.coerceIn(0f, 1f),
            usageRatio = usageRatio,
            status = status,
        )
    }
}
