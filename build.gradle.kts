import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.graphql

val exposedVersion: String by project
val ktorVersion: String by project

plugins {
    kotlin("jvm") version "2.0.0"
    application
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
    id("com.expediagroup.graphql") version "6.2.5"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "creat.xinkle"
version = "1.11"

repositories {
    mavenCentral()

}

dependencies {
    implementation("dev.kord:kord-core:0.15.0")
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("com.expediagroup:graphql-kotlin-ktor-client:6.3.0")
    implementation("org.seleniumhq.selenium:selenium-java:4.10.0")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.6.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.4.1")
    implementation("org.slf4j:slf4j-simple:2.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
}

graphql {
    client {
        endpoint = "https://www.fflogs.com/api/v2/client"
        packageName = "creat.xinkle.Romangway"
        headers = mapOf(
            "authorization" to "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiI5ZTBhYWNjNy00NDk2LTQyYzQtYTg1YS1lOWI1OGM1OWUzMzUiLCJqdGkiOiJjNjZhMTA2MTYzN2Q1MjBkMDY1NDYzNDQ2OWFjMTM2YzllODNiN2RlNWYwYjI1OGM3OWQ1M2JiYWUyMzg2Yjk4ZTUyNThmOTNhOThmNDU5MCIsImlhdCI6MTczNzY4NzE5NC42NzUxNTQsIm5iZiI6MTczNzY4NzE5NC42NzUxNTgsImV4cCI6MTc2ODc5MTE5NC42Njc2MDMsInN1YiI6IiIsInNjb3BlcyI6WyJ2aWV3LXVzZXItcHJvZmlsZSIsInZpZXctcHJpdmF0ZS1yZXBvcnRzIl19.kYsSlygVIRTfiil31BUze3WsUMlERXvhAq2RT_mA2E7P3Ar02rw_14rcuOP5Wk2RldtQl0USNeaHkg73PF23x2AYNQ_QRBydJL8jOESq7sdrFGTZ7hW84zTd5VRS0_EHWsP_LcB0L6o_hssyJrSIgN7k0mLGCMX_-xS1dOvxJlG2GRUzbOFyohmmWGLWMYge97GUdwQEVCMdz4f9516V6uutHrHNDOsAxUrCy1PUnvKay5ki0DSJMgfaYn61DFxQaTPPffLCjitDgkp-4RUYfa8BsG8yYH5nNPv_hRE8TnpcytUc6reazlm9z9uyBgx6-LEraBFPA6CgSgWkSvEj-WNfGvkGsiMm_1-iUM0XaV9qsV7OWVQ1iL-L0YWeHBRQ5lva9t96vgkDVb50nxldkRwi1gyOjgwcWYhRQBQPbfOepBfl-Sn-1YtqAldNN89Hu4buDEqP12wPAiLCzTf6251OYSg_YNkp2T3Vtcz61zON0Mz1nnxY8rDTYRylyvE77fw0qau1q-4aUw7R-wLsYqLDaRlzT8vlH_lSOAFdidp1duP0JZS2MbznJpLhqTDxMpaZK8GdeqfavpdmTiJO-QkmTUOcVx-BEHXRzSLUS_ovAfHjvDc49UwTwjTilwg_ETMEpPdZQMy4vx0B57kuxGfVqIA9TnSZS9WmSoKWSpY"
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