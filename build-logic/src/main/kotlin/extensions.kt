import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion

fun Project.latestCommitHash(): String {
    return runGitCommand(listOf("rev-parse", "--short", "HEAD"))
}

fun Project.latestCommitMessage(): String {
    return runGitCommand(listOf("log", "-1", "--pretty=%B"))
}

fun Project.branchName(): String {
    return runGitCommand(listOf("rev-parse", "--abbrev-ref", "HEAD"))
}

fun Project.runGitCommand(args: List<String>): String {
    return providers.exec {
        commandLine = listOf("git") + args
    }.standardOutput.asBytes.get().toString(Charsets.UTF_8).trim()
}

fun Project.parseMinecraftSnapshotVersion(version: String): String? {
    val separatorIndex = version.indexOf('-')
    val lastSeparatorIndex = version.lastIndexOf('-')
    if (separatorIndex == -1 || separatorIndex == lastSeparatorIndex) {
        return null
    }
    return version.substring(separatorIndex + 1, lastSeparatorIndex)
}

fun JavaPluginExtension.javaTarget(version: Int) {
    toolchain.languageVersion.set(JavaLanguageVersion.of(version))
}
