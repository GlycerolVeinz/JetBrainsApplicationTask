/* TODO Task
Write a kotlin program, which will print all public declarations from a Kotlin program/library sources.

The application should be written in kotlin and allowed to use kotlin-compiler-embeddable (https://central.sonatype.com/artifact/org.jetbrains.kotlin/kotlin-compiler-embeddable)
Sample Usage

$ git clone https://github.com/JetBrains/Exposed
$ ./your-solution ./Exposed
fun declaration1()
class A {
   fun declaration2()
}
....
* */

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.isPublic

import java.io.File


const val wrongInputErrMsg: String = "No input found, please specify a working directory"
const val invalidDirectoryErrMsg: String = "The specified file either doesn't exist, or isn't a directory. Please try something else"
const val kotlinFileExtension: String = "kt"
const val emptyString: String = ""
const val tabString: String = "  "

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

}

class KotlinFileFormatter {
    private fun formatVariable(ktProperty: KtProperty, depth: Int): String {
        return tabString.repeat(depth) + ktProperty.text
    }

    private fun findLocalFunctionDeclarations(declaration: KtNamedFunction): List<KtDeclaration> {
        val functionBody = declaration.bodyExpression ?: return emptyList()
        return functionBody.collectDescendantsOfType<KtDeclaration>()
    }

    private fun formatFunctionParameters(declaration: KtNamedFunction): String {
        var retString: String = emptyString
        declaration.valueParameters.forEachIndexed(){ index, param ->
            if (index != 0) retString += ", "
            retString += param.text
        }
        return retString
    }

    private fun formatFunctionBodyDeclarations(declaration: KtNamedFunction, depth: Int): String {
        var retString: String = emptyString
        findLocalFunctionDeclarations(declaration).forEach { dc ->
            retString += formatDeclarationSignature(dc, depth)
        }
        return retString
    }

    private fun formatFunctionSignature(declaration: KtNamedFunction, depth: Int): String {
        return (tabString.repeat(depth) + "${declaration.funKeyword?.text} ${declaration.name}(${formatFunctionParameters(declaration)}){\n")
    }

    private fun formatFunction(declaration: KtNamedFunction, depth: Int): String {
        return formatFunctionSignature(declaration, depth) +
                formatFunctionBodyDeclarations(declaration, depth + 1) +
                tabString.repeat(depth) + "}\n"
    }

    fun formatDeclarationSignature(declaration: KtDeclaration, depth: Int): String {
        if (!declaration.isPublic) return emptyString

        val signature: String = when (declaration){
            is KtProperty -> formatVariable(declaration, depth)
            is KtNamedFunction -> formatFunction(declaration, depth)
            is KtClassOrObject -> formatClassOrObject(declaration, depth)
            else -> return emptyString
        }

        return signature + "\n"
    }

    private fun formatClassOrObject(declaration: KtClassOrObject, depth: Int): String{
        return formatClassSignature(declaration, depth) +
                formatClassBody(declaration, depth + 1) +
                tabString.repeat(depth) + "}\n"
    }

    private fun findLocalClassDeclarations(declaration: KtClassOrObject): List<KtDeclaration> {
        return declaration.declarations ?: emptyList()
    }

    private fun formatClassBody(declaration: KtClassOrObject, depth: Int): String {
        var retString: String = emptyString
        findLocalClassDeclarations(declaration).forEach{ dec ->
            retString += formatDeclarationSignature(dec, depth)
        }
        return retString
    }

    private fun formatClassSignature(declaration: KtClassOrObject, depth: Int): String {
        return tabString.repeat(depth) + "${declaration.getDeclarationKeyword()?.text} ${declaration.name} {\n"
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
    val formater = KotlinFileFormatter()

    var final: String = emptyString

    kotlinFiles.asSequence().map { kotlinFileAnalyzer.toKtFile(it) }
        .flatMap { it.declarations }
        .map { formater.formatDeclarationSignature(it, 0) }
        .forEach { final += it }

    println(final)
}