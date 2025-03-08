import org.jetbrains.kotlin.asJava.classes.runReadAction
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isPublic

import java.io.File


const val wrongInputErrMsg: String = "No input found, please specify a working directory"
const val invalidDirectoryErrMsg: String = "The specified file either doesn't exist, or isn't a directory. Please try something else"
const val kotlinFileExtension: String = "kt"
const val emptyString: String = ""

class DirectoryBrowser {
    fun isValidDirectory(dir: String): Boolean {
        val file = File(dir)
        return file.exists() && file.isDirectory
    }

    fun getFilesByExtension(dir: File, extension: String): List<File> {
        return dir.walk()
            .filter { it.extension == extension }
            .toList()
    }
}

class KotlinFileAnalyzer {
    private val environment: KotlinCoreEnvironment
    private val psiManager: PsiManager
    private val psiFactory: KtPsiFactory

    init {
        val disposable = Disposer.newDisposable()
        val configuration = CompilerConfiguration()
        configuration.put(
            CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
            PrintingMessageCollector(System.err, MessageRenderer.PLAIN_RELATIVE_PATHS, false)
        )

        environment = KotlinCoreEnvironment.createForProduction(
            disposable,
            configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )

        psiManager = PsiManager.getInstance(environment.project)
        psiFactory = KtPsiFactory(environment.project)

    }

    fun toKtFile(file: File): KtFile{
        return this.psiFactory.createFile(file.name, file.readText())
    }

    fun getInsideClass(declaration: KtDeclaration): List<KtDeclaration> {
        return when(declaration){
            is KtClassOrObject -> declaration.declarations + listOf(declaration)
            else -> listOf(declaration)
        }
    }
}

fun main(args: Array<String>) {
    if (args.isEmpty()){
        throw RuntimeException(wrongInputErrMsg)
    }

    val directoryBrowser = DirectoryBrowser()
    if (!directoryBrowser.isValidDirectory(args[0])) {
        throw RuntimeException(invalidDirectoryErrMsg)
    }
    val workingDirectory = File(args[0])
    val kotlinFiles = directoryBrowser.getFilesByExtension(workingDirectory, kotlinFileExtension)

    val kotlinFileAnalyzer = KotlinFileAnalyzer()
    kotlinFiles.asSequence().map { kotlinFileAnalyzer.toKtFile(it) }
        .flatMap { it.declarations }
        .flatMap { kotlinFileAnalyzer.getInsideClass(it) }
        .filter { it.isPublic }
        .map { it.name }
        .forEach(::println)

}