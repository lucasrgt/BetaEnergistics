plugins { id("dev.worldline.test") version "0.2.0" }

val verifyMod by tasks.registering(Exec::class) {
    workingDir("../..")
    commandLine("java", "tools/harness/Verify.java")
}

worldline {
    runtime.set("b1.7.3")
    oracleProfile.set("b173-local")
    noRuntime.set(true)
    productClasspath.from("../../.betaenergistics/build/product")
}

tasks.named("compileWorldlineTestJava") { dependsOn(verifyMod) }
