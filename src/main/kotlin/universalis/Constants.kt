package universalis

enum class UniversalisWorlds(val worldId: Int) {
    Carbuncle(2075),
    Chocobo(2076),
    Moogle(2077),
    Tonberry(2078),
    Fenlir(2080);

    fun toKorean(): String = when (this) {
        Carbuncle -> "카벙클"
        Chocobo -> "초코보"
        Moogle -> "모그리"
        Tonberry -> "톤베리"
        Fenlir -> "펜리르"
    }
}
