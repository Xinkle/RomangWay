package feature.universalis


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UniversalisDetailItemPrice(
    @SerialName("averagePrice")
    val averagePrice: Double? = null,
    @SerialName("averagePriceHQ")
    val averagePriceHQ: Double? = null,
    @SerialName("averagePriceNQ")
    val averagePriceNQ: Double? = null,
    @SerialName("currentAveragePrice")
    val currentAveragePrice: Double? = null,
    @SerialName("currentAveragePriceHQ")
    val currentAveragePriceHQ: Double? = null,
    @SerialName("currentAveragePriceNQ")
    val currentAveragePriceNQ: Double? = null,
    @SerialName("hasData")
    val hasData: Boolean? = null,
    @SerialName("hqSaleVelocity")
    val hqSaleVelocity: Int? = null,
    @SerialName("itemID")
    val itemID: Int? = null,
    @SerialName("lastUploadTime")
    val lastUploadTime: Long? = null,
    @SerialName("listings")
    val listings: List<Listings?>? = null,
    @SerialName("listingsCount")
    val listingsCount: Int? = null,
    @SerialName("maxPrice")
    val maxPrice: Int? = null,
    @SerialName("maxPriceHQ")
    val maxPriceHQ: Int? = null,
    @SerialName("maxPriceNQ")
    val maxPriceNQ: Int? = null,
    @SerialName("minPrice")
    val minPrice: Int? = null,
    @SerialName("minPriceHQ")
    val minPriceHQ: Int? = null,
    @SerialName("minPriceNQ")
    val minPriceNQ: Int? = null,
    @SerialName("nqSaleVelocity")
    val nqSaleVelocity: Double? = null,
    @SerialName("recentHistory")
    val recentHistory: List<RecentHistory?>? = null,
    @SerialName("recentHistoryCount")
    val recentHistoryCount: Int? = null,
    @SerialName("regularSaleVelocity")
    val regularSaleVelocity: Double? = null,
    @SerialName("unitsForSale")
    val unitsForSale: Int? = null,
    @SerialName("unitsSold")
    val unitsSold: Int? = null,
    @SerialName("worldID")
    val worldID: Int? = null,
    @SerialName("worldName")
    val worldName: String? = null
) {
    @Serializable
    data class Listings(
        @SerialName("hq")
        val hq: Boolean? = null,
        @SerialName("isCrafted")
        val isCrafted: Boolean? = null,
        @SerialName("lastReviewTime")
        val lastReviewTime: Int? = null,
        @SerialName("listingID")
        val listingID: String? = null,
        @SerialName("pricePerUnit")
        val pricePerUnit: Int? = null,
        @SerialName("quantity")
        val quantity: Int? = null,
        @SerialName("retainerCity")
        val retainerCity: Int? = null,
        @SerialName("retainerID")
        val retainerID: String? = null,
        @SerialName("retainerName")
        val retainerName: String? = null,
        @SerialName("stainID")
        val stainID: Int? = null,
        @SerialName("tax")
        val tax: Int? = null,
        @SerialName("total")
        val total: Int? = null
    )

    @Serializable
    data class RecentHistory(
        @SerialName("buyerName")
        val buyerName: String? = null,
        @SerialName("hq")
        val hq: Boolean? = null,
        @SerialName("onMannequin")
        val onMannequin: Boolean? = null,
        @SerialName("pricePerUnit")
        val pricePerUnit: Int? = null,
        @SerialName("quantity")
        val quantity: Int? = null,
        @SerialName("timestamp")
        val timestamp: Int? = null,
        @SerialName("total")
        val total: Int? = null
    )

    fun getSummary(): String = "[${worldName}] NQ최저가: $minPriceNQ, HQ최저가: $minPriceHQ, 평균가(10): ${
        String.format(
            "%.1f",
            currentAveragePrice
        )
    }, 최근 업데이트 시간: ${lastUploadTime?.toRelativeTime()}"
}