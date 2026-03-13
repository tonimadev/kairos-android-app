package digital.tonima.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DistanceMatrixResponse(
    val rows: List<DistanceMatrixRow>,
    val status: String,
)

@Serializable
data class DistanceMatrixRow(
    val elements: List<DistanceMatrixElement>,
)

@Serializable
data class DistanceMatrixElement(
    val duration: DurationInfo? = null,
    val status: String,
)

@Serializable
data class DurationInfo(
    val text: String,
    val value: Int,
)
