import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.graphql
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val exposedVersion: String by project
val ktorVersion: String by project

plugins {
    kotlin("jvm") version "1.7.10"
    application
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.10"
    id("com.expediagroup.graphql") version "6.2.5"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "creat.xinkle"
version = "1.09"

repositories {
    mavenCentral()

}

dependencies {
    implementation("dev.kord:kord-core:0.8.0-M16")
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.xerial:sqlite-jdbc:3.39.3.0")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("com.expediagroup:graphql-kotlin-ktor-client:6.3.0")
    implementation("org.seleniumhq.selenium:selenium-java:4.6.0")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.6.0")
    implementation("mysql:mysql-connector-java:8.0.30")
    implementation("org.slf4j:slf4j-simple:2.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
}

graphql {
    client {
        endpoint = "https://www.fflogs.com/api/v2/client"
        packageName = "creat.xinkle.Romangway"
        headers = mapOf(
            "authorization" to "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiI5NzY5OWVhNy0xMGNlLTRmNjYtYTdjYy1mNjUwZjFhOTgyOWEiLCJqdGkiOiJmOWUyYWVhZDc0NWFjMmZlZGMwYjBmNjc5MWY4Y2UxODJhZjQyOTIyZjkxYTEyNGQ0N2Y2NDUyOWIzZWRlM2Q1Nzg5YjcyYjlkMWVhYTNlYiIsImlhdCI6MTY2NDc5OTg5NS43NTYwNTcsIm5iZiI6MTY2NDc5OTg5NS43NTYwNTksImV4cCI6MTY5NTkwMzg5NS43NDg3NjQsInN1YiI6IiIsInNjb3BlcyI6WyJ2aWV3LXVzZXItcHJvZmlsZSIsInZpZXctcHJpdmF0ZS1yZXBvcnRzIl19.kVUnLb_R_sw1bC7iLwRMGpoyU9vaJGjpUhsQEcSj3MopZSHyaXiQPHGvlKDZHRAOyiIVVqb1fPk2gDkqVtKezX_r48tv-FTJfeLRUrA4fCtl9noHPVci26IuAOWQ3AnQ2QMve3Brh1ZacbqdKURTZKOWQp4uUSPfVhXetMGu9Ex0YfV-wTH2UBl7Q-w-hsiVADoCBptudAlBU0K9AWeNJmh9JAAgVpIuR8skB2eo2NFFRk5zlfpTfiVdLab-JLuKjxW_Yc6lRLfeYqMgWedbTSiywjQ2MifKcvERxL4c8vLwL_WJxDY1XOCelV_buY4RsmBRxNY-59teCAgDLC7ExKYRe_KVVfwqRd41JrY8rx6CkHo9ZkRj9kvVbAs4UtBYrCRj27CbqrHPS73VrCz3-K5GkERzVr3zEOP6nfxrdp0gzmlcz_x2JjbDqdQNCcAMLozksg5sgSi27oLwb7ob92pr1c8XZQVcVPDrgzzxVu7F6SxXsVwewXeObR-7WQQhUC0-GKCkz93uRmzq8KY_hgwd4C1PDhJr1JmSih-NmhhbMW-YNfDefzuzWVwO1h6rkEylvbnRlBcbmukh8J-BOdtfp6nrEsl7nt3c-q-CtJYP3p9-dnlgkMGVe-pUgjyPqAPZ6qEPeVCv_OZXQEe0v1zyn_x0Ur7Cu8DPK_HxvO8"
        )
        serializer = GraphQLSerializer.KOTLINX
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}