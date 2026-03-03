import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.graphql
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.Properties

val exposedVersion: String by project
val ktorVersion: String by project

fun loadSecretProfileProperty(key: String): String? {
    val secretFile = File(rootDir, "secret_profile.properties")
    if (!secretFile.exists()) return null

    val properties = Properties()
    secretFile.inputStream().use(properties::load)
    return properties.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
}

fun urlEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

fun requestFFLogsBearerToken(clientId: String, clientSecret: String): String {
    val requestBody = buildString {
        append("grant_type=").append(urlEncode("client_credentials"))
        append("&client_id=").append(urlEncode(clientId))
        append("&client_secret=").append(urlEncode(clientSecret))
    }

    val response = HttpClient.newHttpClient().send(
        HttpRequest.newBuilder()
            .uri(URI("https://www.fflogs.com/oauth/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build(),
        HttpResponse.BodyHandlers.ofString()
    )

    if (response.statusCode() !in 200..299) {
        throw GradleException("Failed to issue FFLogs token (HTTP ${response.statusCode()})")
    }

    return Regex("\"access_token\"\\s*:\\s*\"([^\"]+)\"")
        .find(response.body())
        ?.groupValues
        ?.get(1)
        ?: throw GradleException("Failed to parse FFLogs access token from OAuth response.")
}

val fflogsClientId = providers.environmentVariable("FFLOGS_CLIENT_ID").orNull
    ?: providers.environmentVariable("FFLOG_CLIENT_ID").orNull
    ?: loadSecretProfileProperty("fflog_client_id")
val fflogsClientSecret = providers.environmentVariable("FFLOGS_CLIENT_SECRET").orNull
    ?: providers.environmentVariable("FFLOG_CLIENT_SECRET").orNull
    ?: loadSecretProfileProperty("fflog_client_secret")
val fflogsBearerToken: String by lazy {
    val clientId = requireNotNull(fflogsClientId) {
        "FFLogs client id is required. Set FFLOGS_CLIENT_ID/FFLOG_CLIENT_ID or secret_profile.properties:fflog_client_id"
    }
    val clientSecret = requireNotNull(fflogsClientSecret) {
        "FFLogs client secret is required. Set FFLOGS_CLIENT_SECRET/FFLOG_CLIENT_SECRET or secret_profile.properties:fflog_client_secret"
    }

    requestFFLogsBearerToken(clientId, clientSecret)
}

plugins {
    kotlin("jvm") version "2.2.10"
    application
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10"
    id("com.expediagroup.graphql") version "6.2.5"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "creat.xinkle"
version = "1.16"

repositories {
    mavenCentral()

}

dependencies {
    implementation("dev.kord:kord-core:0.17.0")
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("com.expediagroup:graphql-kotlin-ktor-client:6.3.0")
    implementation("com.microsoft.playwright:playwright:1.44.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.4.1")
    implementation("org.slf4j:slf4j-simple:2.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}

graphql {
    client {
        endpoint = "https://www.fflogs.com/api/v2/client"
        packageName = "creat.xinkle.Romangway"
        headers = mapOf(
            "authorization" to "Bearer $fflogsBearerToken"
        )
        serializer = GraphQLSerializer.KOTLINX
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}
