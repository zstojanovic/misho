package org.misho

import scala.collection.mutable

case class Song(
  notes: Seq[Note]
)

case class Note(
  pitch: Option[Int], // if not defined - then it's a rest
  duration: Duration,
  sounds: Seq[Sound]  // should be empty if this is a rest
)

case class Sound(
  phoneme: String,
  durationWeight: Int = 1
)

case class Duration(numerator: Int, denominator: Int)

object Sounds {

  def of(s: String): Seq[Sound] = {
    val keys = PhonemeConverter.map.keys.toSeq.sortBy(k => -k.length)
    var set = mutable.Set[(Int, String)]()
    var s2 = s.toLowerCase

    keys.foreach { key =>
      var found = s2.indexOf(key)
      while (found > -1) {
        set += found -> key
        s2 = s2.replaceFirst(key, Seq.fill(key.length)("*").mkString(""))
        found = s2.indexOf(key)
      }
    }
    set.toSeq.sortBy(_._1).map(p => Sound(p._2))
  }

}

object PhonemeConverter {
  val map = Map("_" -> "_",
    "i" -> "i", "e" -> "e", "a" -> "a", "o" -> "o", "u" -> "u", "r" -> "r",
    "p" -> "p", "b" -> "b", "t" -> "t", "d" -> "d", "k" -> "k", "g" -> "g",
    "c" -> "ts", "č" -> "tS", "dž" -> "dZ", "ć" -> "tS'", "đ" -> "dZ'",
    "f" -> "f", "s" -> "s", "z" -> "z", "š" -> "S", "ž" -> "Z",
    "h" -> "x", "m" -> "m", "n" -> "n", "nj" -> "J", "l" -> "l", "lj" -> "L", "r" -> "r", "v" -> "v", "j" -> "j")

  def convert(s: String): String = map(s)
}
