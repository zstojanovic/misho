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

object LyricsParser {
  private val map = Map("_" -> "_",
    "i" -> "i", "e" -> "e", "a" -> "a", "o" -> "o", "u" -> "u", "r" -> "r",
    "p" -> "p", "b" -> "b", "t" -> "t", "d" -> "d", "k" -> "k", "g" -> "g",
    "c" -> "ts", "č" -> "tS", "dž" -> "dZ", "ć" -> "tS'", "đ" -> "dZ'",
    "f" -> "f", "s" -> "s", "z" -> "z", "š" -> "S", "ž" -> "Z",
    "h" -> "x", "m" -> "m", "n" -> "n", "nj" -> "J", "l" -> "l", "lj" -> "L", "v" -> "v", "j" -> "j")
  private val parseRegex = "dž|lj|nj|a|b|c|č|ć|d|đ|e|f|g|h|i|j|k|l|m|n|o|p|r|s|š|t|u|v|z|ž|\\_|[0-9]*\\.?[0-9]+".r

  def parse(lyric: String): Seq[Sound] = {
    val tokens = parseRegex.findAllIn(lyric.toLowerCase).toArray
    var currentPhoneme: String = map(tokens.head) // should break if first token is not phoneme
    var currentWeight: Int = 1
    val sounds = tokens.drop(1).flatMap { token =>
      val phoneme = map.get(token)
      if (phoneme.isDefined) {
        if (currentPhoneme == phoneme.get) {
          currentWeight += 1
          None
        } else {
          val s = Sound(currentPhoneme, currentWeight)
          currentPhoneme = phoneme.get
          currentWeight = 1
          Some(s)
        }
      } else {
        currentWeight = token.toInt // should break if token is neither a phoneme or a weight
        None
      }
    }
    sounds.toSeq :+ Sound(currentPhoneme, currentWeight)
  }
}