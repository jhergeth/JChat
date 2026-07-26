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
    implementation("io.micronaut.langchain4j:micronaut-langchain4j-core")

    // Provider-Modul: austauschbar, z.B. auch -openai oder -ollama
    implementation("io.micronaut.langchain4j:micronaut-langchain4j-anthropic")

    implementation("jakarta.annotation:jakarta.annotation-api")
    runtimeOnly("ch.qos.logback:logback-classic")

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
