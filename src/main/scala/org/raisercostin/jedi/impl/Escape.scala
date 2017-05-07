package org.raisercostin.util

import scala.util.Try

object Escape {
  private val nonUrlChars = """[()\[\]{}_'\"`%^+_*!×&ƒ\:? -]+""".r.unanchored
  private val nonUrlPathChars = """[\/]+""".r

  def toSlug(text: String) = {
    var result = text
    //decode if url
    result = Try { java.net.URLDecoder.decode(result, "UTF-8") }.getOrElse("")
    //convert to latin
    import java.text.Normalizer
    result = Normalizer.normalize(result, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
    result = result.toLowerCase

    result = nonUrlChars.replaceAllIn(result, "-")
    result = nonUrlPathChars.replaceAllIn(result, "--")

    /*
    //remove dot(.) except last
    result = result.replaceFirst("\\.([^./]{1,5}$)", "--punct--final--$1")
    result = result.replaceAllLiterally(".", "-")
    result = result.replaceFirst("--punct--final--", ".")
    //convert ()
    //convert underlines
    //convert apostrofe
    result = result.replaceAll("[()\\[\\]{}]", "-")
    result = result.replaceAllLiterally("_", "-")
    result = result.replaceAll("['\"`]", "-")
    //convert final non words
    result = result.replaceAll("""[%^+_^*!×&ƒ\\:?]+""", "-")
    //convert spaces
    result = result.replaceAllLiterally(" ", "-")
    //convert final non words
    result = result.replaceAll("[-]+$", "")
    //convert final non words
    result = result.replaceAll("""[\/]+""", "--")
    //val exceptLast = """\.(?=[^.]*\.)"""
    //result = result.replaceAll(exceptLast,"-")
    //println(s"toSlug($text)=[$result]")
     */
    result
  }
}