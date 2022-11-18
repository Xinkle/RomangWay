import java.util.concurrent.TimeUnit

fun formatMillisecond(timestamp: Long): String = String.format(
    "%02d:%02d",
    TimeUnit.MILLISECONDS.toMinutes(timestamp) % TimeUnit.HOURS.toMinutes(1),
    TimeUnit.MILLISECONDS.toSeconds(timestamp) % TimeUnit.MINUTES.toSeconds(1)
)