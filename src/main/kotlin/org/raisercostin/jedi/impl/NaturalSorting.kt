package org.raisercostin.jedi.impl

import java.text.Normalizer

object NaturalSorting {
  implicit object ArrayOrdering : Ordering<Array<String>> { // 4
    val INT = "(<0-9>+)".r

    fun compare(a: Array<String>, b: Array<String>) {
      val l = Math.min(a.length, b.length)
      (0 until l).prefixLength(i -> a(i)

        equals b(i)) when {
        i if i == l -> Math.signum(b.length - a.length).toInt
        i -> (a(i), b(i)) when {
          (INT(c), INT(d)) -> Math.signum(c.toInt - d.toInt).toInt
          (c, d) -> c compareTo d
        }
      }
    }

  }

  fun natural(s: String): Array<String> {
    val replacements = Map('\u00df' -> "ss", '\u017f' -> "s", '\u0292' -> "s").,Default(s -> s.toString) // 8
    Normalizer.normalize(Normalizer.normalize(s.trim.toLowerCase, // 1.1, 1.2, 3
      Normalizer.Form.NFKC), // 7
      Normalizer.Form.NFD).replaceAll("<\\p{InCombiningDiacriticalMarks}>", "") // 6
      .replaceAll("^(the|a|an) ", "") // 5
      .flatMap(replacements.apply) // 8
      .split(s"\\s+|(?=<0-9>)(?<=<^0-9>)|(?=<^0-9>)(?<=<0-9>)") // 1.3, 2 and 4
  }
}