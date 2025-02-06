package universalis


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
{
"results": [
{
"itemId": 44178,
"nq": {
"minListing": {
"world": { "price": 3000 },
"dc": { "price": 800, "worldId": 2076 },
"region": { "price": 800, "worldId": 2076 }
},
"recentPurchase": {
"world": { "price": 2000, "timestamp": 1737725956000 },
"dc": { "price": 999, "timestamp": 1737728959000, "worldId": 2076 },
"region": { "price": 999, "timestamp": 1737728959000, "worldId": 2076 }
},
"averageSalePrice": {
"world": { "price": 1812.9032258064517 },
"dc": { "price": 2701.846875 },
"region": { "price": 2701.846875 }
},
"dailySaleVelocity": {
"world": { "quantity": 9.928652492895385 },
"dc": { "quantity": 204.97863137092227 },
"region": { "quantity": 204.97863048692176 }
}
},
"hq": {
"minListing": {
"world": { "price": 4000 },
"dc": { "price": 3800, "worldId": 2076 },
"region": { "price": 3800, "worldId": 2076 }
},
"recentPurchase": {
"world": { "price": 4100, "timestamp": 1737767479000 },
"dc": { "price": 4100, "timestamp": 1737770111000, "worldId": 2075 },
"region": { "price": 4100, "timestamp": 1737770111000, "worldId": 2075 }
},
"averageSalePrice": {
"world": { "price": 4128.087768522578 },
"dc": { "price": 4155.725804619892 },
"region": { "price": 4155.725804619892 }
},
"dailySaleVelocity": {
"world": { "quantity": 3652.7832800474794 },
"dc": { "quantity": 28201.856885523735 },
"region": { "quantity": 28201.856763899075 }
}
},
"worldUploadTimes": [
{ "worldId": 2078, "timestamp": 1737773294601 },
{ "worldId": 2076, "timestamp": 1737768468360 }
]
}
],
"failedItems": []
}
 */
@Serializable
data class UniversalisItemPrice(
    @SerialName("results")
    val results: List<Result?>? = null
) {
    @Serializable
    data class Result(
        @SerialName("hq")
        val hq: Hq? = null,
        @SerialName("itemId")
        val itemId: Int? = null,
        @SerialName("nq")
        val nq: Nq? = null,
        @SerialName("worldUploadTimes")
        val worldUploadTimes: List<WorldUploadTime?>? = null
    ) {
        @Serializable
        data class Hq(
            @SerialName("averageSalePrice")
            val averageSalePrice: AverageSalePrice? = null,
            @SerialName("dailySaleVelocity")
            val dailySaleVelocity: DailySaleVelocity? = null,
            @SerialName("minListing")
            val minListing: MinListing? = null,
            @SerialName("recentPurchase")
            val recentPurchase: RecentPurchase? = null
        ) {
            @Serializable
            data class AverageSalePrice(
                @SerialName("dc")
                val dc: Dc? = null,
                @SerialName("region")
                val region: Region? = null,
                @SerialName("world")
                val world: World? = null
            ) {
                @Serializable
                data class Dc(
                    @SerialName("price")
                    val price: Double? = null
                )

                @Serializable
                data class Region(
                    @SerialName("price")
                    val price: Double? = null
                )

                @Serializable
                data class World(
                    @SerialName("price")
                    val price: Double? = null
                )
            }

            @Serializable
            data class DailySaleVelocity(
                @SerialName("dc")
                val dc: Dc? = null,
                @SerialName("region")
                val region: Region? = null,
                @SerialName("world")
                val world: World? = null
            ) {
                @Serializable
                data class Dc(
                    @SerialName("quantity")
                    val quantity: Double? = null
                )

                @Serializable
                data class Region(
                    @SerialName("quantity")
                    val quantity: Double? = null
                )

                @Serializable
                data class World(
                    @SerialName("quantity")
                    val quantity: Double? = null
                )
            }

            @Serializable
            data class MinListing(
                @SerialName("dc")
                val dc: Dc? = null,
                @SerialName("region")
                val region: Region? = null,
                @SerialName("world")
                val world: World? = null
            ) {
                @Serializable
                data class Dc(
                    @SerialName("price")
                    val price: Int? = null,
                    @SerialName("worldId")
                    val worldId: Int? = null
                )

                @Serializable
                data class Region(
                    @SerialName("price")
                    val price: Int? = null,
                    @SerialName("worldId")
                    val worldId: Int? = null
                )

                @Serializable
                data class World(
                    @SerialName("price")
                    val price: Int? = null
                )
            }

            @Serializable
            data class RecentPurchase(
                @SerialName("dc")
                val dc: Dc? = null,
                @SerialName("region")
                val region: Region? = null,
                @SerialName("world")
                val world: World? = null
            ) {
                @Serializable
                data class Dc(
                    @SerialName("price")
                    val price: Int? = null,
                    @SerialName("timestamp")
                    val timestamp: Long? = null,
                    @SerialName("worldId")
                    val worldId: Int? = null
                )

                @Serializable
                data class Region(
                    @SerialName("price")
                    val price: Int? = null,
                    @SerialName("timestamp")
                    val timestamp: Long? = null,
                    @SerialName("worldId")
                    val worldId: Int? = null
                )

                @Serializable
                data class World(
                    @SerialName("price")
                    val price: Int? = null,
                    @SerialName("timestamp")
                    val timestamp: Long? = null
                )
            }
        }

        @Serializable
        data class Nq(
            @SerialName("averageSalePrice")
            val averageSalePrice: AverageSalePrice? = null,
            @SerialName("dailySaleVelocity")
            val dailySaleVelocity: DailySaleVelocity? = null,
            @SerialName("minListing")
            val minListing: MinListing? = null,
            @SerialName("recentPurchase")
            val recentPurchase: RecentPurchase? = null
        ) {
            @Serializable
            data class AverageSalePrice(
                @SerialName("dc")
                val dc: Dc? = null,
                @SerialName("region")
                val region: Region? = null,
                @SerialName("world")
                val world: World? = null
            ) {
                @Serializable
                data class Dc(
                    @SerialName("price")
                    val price: Double? = null
                )

                @Serializable
                data class Region(
                    @SerialName("price")
                    val price: Double? = null
                )

                @Serializable
                data class World(
                    @SerialName("price")
                    val price: Double? = null
                )
            }

            @Serializable
            data class DailySaleVelocity(
                @SerialName("dc")
                val dc: Dc? = null,
                @SerialName("region")
                val region: Region? = null,
                @SerialName("world")
                val world: World? = null
            ) {
                @Serializable
                data class Dc(
                    @SerialName("quantity")
                    val quantity: Double? = null
                )

                @Serializable
                data class Region(
                    @SerialName("quantity")
                    val quantity: Double? = null
                )

                @Serializable
                data class World(
                    @SerialName("quantity")
                    val quantity: Double? = null
                )
            }

            @Serializable
            data class MinListing(
                @SerialName("dc")
                val dc: Dc? = null,
                @SerialName("region")
                val region: Region? = null,
                @SerialName("world")
                val world: World? = null
            ) {
                @Serializable
                data class Dc(
                    @SerialName("price")
                    val price: Int? = null,
                    @SerialName("worldId")
                    val worldId: Int? = null
                )

                @Serializable
                data class Region(
                    @SerialName("price")
                    val price: Int? = null,
                    @SerialName("worldId")
                    val worldId: Int? = null
                )

                @Serializable
                data class World(
                    @SerialName("price")
                    val price: Int? = null
                )
            }

            @Serializable
            data class RecentPurchase(
                @SerialName("dc")
                val dc: Dc? = null,
                @SerialName("region")
                val region: Region? = null,
                @SerialName("world")
                val world: World? = null
            ) {
                @Serializable
                data class Dc(
                    @SerialName("price")
                    val price: Int? = null,
                    @SerialName("timestamp")
                    val timestamp: Long? = null,
                    @SerialName("worldId")
                    val worldId: Int? = null
                )

                @Serializable
                data class Region(
                    @SerialName("price")
                    val price: Int? = null,
                    @SerialName("timestamp")
                    val timestamp: Long? = null,
                    @SerialName("worldId")
                    val worldId: Int? = null
                )

                @Serializable
                data class World(
                    @SerialName("price")
                    val price: Int? = null,
                    @SerialName("timestamp")
                    val timestamp: Long? = null
                )
            }
        }

        @Serializable
        data class WorldUploadTime(
            @SerialName("timestamp")
            val timestamp: Long? = null,
            @SerialName("worldId")
            val worldId: Int? = null
        )
    }
}

fun UniversalisItemPrice.toReadableString(): String {
    if (results.isNullOrEmpty()) {
        return "결과가 없습니다."
    }

    val result = results.firstOrNull() ?: return "결과가 없습니다."
    val hqMinListing = result.hq?.minListing
    val nqMinListing = result.nq?.minListing

    val hqPrice = hqMinListing?.region?.price
    val hqWorldId = hqMinListing?.region?.worldId
    val hqTimestamp = result.worldUploadTimes.findTimestampByWorldId(hqWorldId)

    val nqPrice = nqMinListing?.region?.price
    val nqWorldId = nqMinListing?.region?.worldId
    val nqTimestamp = result.worldUploadTimes.findTimestampByWorldId(nqWorldId)

    val hqInfo = if (hqPrice != null && hqWorldId != null && hqTimestamp != null) {
        "HQ 최저가: $hqPrice / ${hqWorldId.toKoreanWorld()} / ${hqTimestamp.toRelativeTime()}"
    } else {
        "HQ 최저가 정보를 찾을 수 없습니다."
    }

    val nqInfo = if (nqPrice != null && nqWorldId != null && nqTimestamp != null) {
        "NQ 최저가: $nqPrice / ${nqWorldId.toKoreanWorld()} / ${nqTimestamp.toRelativeTime()}"
    } else {
        "NQ 최저가 정보를 찾을 수 없습니다."
    }

    return "$hqInfo\n$nqInfo"
}


fun Long.toRelativeTime(): String {
    // this 값은 초 단위라고 가정합니다.
    val currentTimeMillis = System.currentTimeMillis()
    val pastTimeMillis = this
    val duration = (currentTimeMillis - pastTimeMillis).milliseconds

    return when {
        duration < 1.minutes -> "방금 전"
        duration < 60.minutes -> "${duration.inWholeMinutes}분 전"
        duration < 24.hours -> "${duration.inWholeHours}시간 전"
        else -> "${duration.inWholeDays}일 전"
    }
}

fun List<UniversalisItemPrice.Result.WorldUploadTime?>?.findTimestampByWorldId(worldId: Int?): Long? {
    if (worldId == null) return null
    return this?.firstOrNull { it?.worldId == worldId }?.timestamp
}

fun Int?.toKoreanWorld(): String {
    return UniversalisWorlds.entries.firstOrNull { it.worldId == this }?.toKorean() ?: "알 수 없음"
}