import org.jetbrains.kotlin.psi.*

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
    private fun analyzeDeclaration(declaration: KtDeclaration): String{
        return when (declaration) {
            is KtNamedFunction -> {
                declaration.name ?: emptyString
            }

            is KtClass -> {
                declaration.name ?: emptyString
            }

            is KtProperty -> {
                declaration.name ?: emptyString
            }

            else -> {
                ""
            }
        }
    }

    fun writePublicDeclarations(file: KtFile){
        file.declarations.map { analyzeDeclaration(it) }.forEach(::println)
    }

    fun toKtFile(codeString: String, fileName: String): KtFile {

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
}