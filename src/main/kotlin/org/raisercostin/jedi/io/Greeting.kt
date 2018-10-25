package org.raisercostin.jedi.io
import java.io.File

// usage - one argument a .kt file (Scala file that was only renamed)
// or a directory
//try {
//    main(args)
//} catch (e: Exception) {
//    e.printStackTrace()
//}

fun convert(lines: List<String>): List<String> {
    val methodNoBracsRegex = ".*fun\\s+\\w+\\s+[:=].*".toRegex()
    val linesWithoutLicense = lines
//  The below lines just removed license comment
//       if (lines[0].startsWith("package "))
//         lines
//       else
//         lines.drop(15)
    val result = mutableListOf<String>()
    linesWithoutLicense.forEach { lineBeforeConv ->
        val convertedLine = lineBeforeConv
                .replace("extends", ":")
                .replace(" def ", " fun ")
                .replace("BigInt(", "BigInteger(")
                .replace("trait", "interface")
                .replace("[", "<")
                .replace("]", ">")
                .replace(" = {", " {")
                .replace(" new ", " ")
                .replace(" Future<", " CompletableFuture<")
                .replace(" Promise<", " CompletableFuture<")
                .replace(" Array<Byte>(", " ByteArray(")
                .replace(" Array<Char>(", " CharArray(")
                .replace("with", ",")
                .replace("match", "when")
                .replace("case class", "data class")
                .replace("case _", "else")
                .replace("case ", "")
                .replace("=>", "->")
                .replace(".asInstanceOf<", " as ") //manually fix >
                .replace("final ", "")
                .replace("fun this(", "constructor(")
                .replace(" Seq<", " List<")
                .replace(" IndexedSeq<", " List<")
                .replace("<:", ":")
        when {
            convertedLine.startsWith("import ") -> {
                val importsLines = if (convertedLine.contains("{")) {
                    val before = convertedLine.substringBefore("{")
                    convertedLine.substringAfter("{").substringBefore("}").split(",")
                            .map { "$before${it.trim()}" }
                } else listOf(convertedLine)
                importsLines.map { it.replace("_", "*") }.forEach {
                    result.add(it)
                }
            }
            convertedLine.matches(methodNoBracsRegex) -> {
                if (convertedLine.contains(":"))
                    result.add(convertedLine.replace(":", "():"))
                else
                    result.add(convertedLine.replace("=", "()="))
            }
            else -> result.add(convertedLine)
        }
    }
    return result
}

fun main(args: Array<String>) {
    val fileName = "d:\\home\\raiser\\work\\jedi-io-scala\\src\\main\\scala"//args[0]
    if (fileName.endsWith(".kt")) {
        workOnFile(fileName)
    } else {
        File(fileName).walk().forEach {
            if (it.name.endsWith(".scala")) {
                workOnFile(it.path)
            }
        }
    }
}

fun readFileAsLinesUsingReadLines(fileName: String): List<String> = File(fileName).readLines()

fun String.fileExists() : Boolean = File(this).exists()

fun workOnFile(fileName: String) {
    if (!fileName.fileExists()) {
        println("WARN: file not exists $fileName")
        return
    }
    println("working on $fileName")
    val lines = readFileAsLinesUsingReadLines(fileName)
    val fileContent = convert(lines).joinToString("\n")
    File(fileName).writeText(fileContent)
}