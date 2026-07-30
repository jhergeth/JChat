plugins {
    id("io.micronaut.application") version "4.4.4"
}

version = "0.1"
group = "name.hergeth.jchat"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("io.micronaut:micronaut-http-validation")
    annotationProcessor("io.micronaut.langchain4j:micronaut-langchain4j-processor")

    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut:micronaut-jackson-databind")
    implementation("io.micronaut.langchain4j:micronaut-langchain4j-core")

    // Micronaut-Provider-Module (Runtime-Integration)
    implementation("io.micronaut.langchain4j:micronaut-langchain4j-anthropic")
    implementation("io.micronaut.langchain4j:micronaut-langchain4j-openai")
    implementation("io.micronaut.langchain4j:micronaut-langchain4j-ollama")

    // Compile-Zugriff für manuelles Modell-Building in ChatModelRegistry
    implementation("dev.langchain4j:langchain4j-anthropic")
    implementation("dev.langchain4j:langchain4j-open-ai")
    implementation("dev.langchain4j:langchain4j-ollama")

    implementation("jakarta.annotation:jakarta.annotation-api")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("org.yaml:snakeyaml")

    testImplementation("io.micronaut:micronaut-http-client")
}

application {
    mainClass = "name.hergeth.jchat.Application"
}

java {
    sourceCompatibility = JavaVersion.toVersion("17")
    targetCompatibility = JavaVersion.toVersion("17")
}

micronaut {
    version("4.7.4")
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("name.hergeth.jchat.*")
    }
}
