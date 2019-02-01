package org.misho

import java.io.{File, PrintWriter}

import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import collection.JavaConverters._
import scala.collection.mutable
import sys.process._

object Misho extends App {
  private val noteToMidiMap = Map("A" -> 0, "B" -> 2, "C" -> 3, "D" -> 5, "E" -> 7, "F" -> 8, "G" -> 10)
  private val minimalDenominator = 8

  private val song = load(args(0))
  private val mbrola = convertSongToMbrola(song, 4, 180)
  convertMbrolaToWav(mbrola, args(1))

  private def convertSongToMbrola(song: Song, meter: Int, tempo: Int) = {
    song.notes.map { note =>
      val f = note.pitch.map( p => frequency(p, -24))
      val totalDuration = duration(note.duration, meter, tempo)
      if (f.isDefined) {
        val soundCount = note.sounds.size
        note.sounds.map { sound =>
          val ph = PhonemeConverter.convert(sound.phoneme)
          val duration = totalDuration / soundCount
          s"$ph $duration 5 ${f.get} 50 ${f.get} 95 ${f.get}"
        }
      } else {
        Seq(s"_ $totalDuration")
      }
    }.flatten.mkString("\n")
  }

  private def load(file: String): Song = {
    val workbook = new XSSFWorkbook(file)
    val sheet = workbook.getSheetAt(0)
    val mergedRegions = sheet.getMergedRegions.asScala
    val transpose = convertNoteNameToMidi(sheet.getRow(0).getCell(0).getStringCellValue)

    val excelNotes = (for {
      rowNum <- 0 to sheet.getLastRowNum
      row = sheet.getRow(rowNum) if row != null
      cellNum <- 1 to row.getLastCellNum
      cell = row.getCell(cellNum) if cell != null && cell.getCellType == CellType.STRING
      s = cell.getStringCellValue if s != null && s != ""
    } yield {
      val mergedRegion = mergedRegions.find(r => r.getFirstRow == rowNum && r.getFirstColumn == cellNum)
      if (mergedRegion.isDefined) {
        assert(mergedRegion.get.getFirstRow == mergedRegion.get.getLastRow)
        ExcelNote(cellNum, Some(rowNum), Some(s), mergedRegion.get.getLastColumn - mergedRegion.get.getFirstColumn + 1)
      } else {
        ExcelNote(cellNum, Some(rowNum), Some(s), 1)
      }
    }).sortBy(_.column)

    // assert no overlaps
    val columns = mutable.Set[Int]()
    excelNotes.foreach{ note =>
      val currentCount = columns.size
      for ( c <- note.column until note.column + note.length ) {
        columns += c
      }
      assert(columns.size == currentCount + note.length, "Found note overlap: " + note)
    }

    // TODO handle pauses here

    val notes = excelNotes.map{ note =>
      if (note.row.isDefined) {
        Note(Some(transpose - note.row.get), Duration(note.length, minimalDenominator), Sounds.of(note.lyric.get))
      } else {
        Note(None, Duration(note.length, minimalDenominator), Nil)
      }
    }

    Song(notes)
  }

  private case class ExcelNote(column: Int, row: Option[Int], lyric: Option[String], length: Int)

  private def convertNoteNameToMidi(note: String): Int = {
    note.substring(1, 2).toInt * 12 + noteToMidiMap(note.substring(0, 1)) + 21
  }

  private def frequency(midiNoteNumber: Int, transpose: Int = 0): Float = {
    (440 * math.pow(2, (midiNoteNumber + transpose - 69) / 12f)).toFloat
  }

  private def duration(duration: Duration, meter: Int, tempo: Int): Float = {
    60f / tempo * meter / duration.denominator * duration.numerator * 1000f
  }

  private def convertMbrolaToWav(m: String, wavFile: String) = {
    val phoFile = File.createTempFile("misho-", ".pho")
    phoFile.deleteOnExit()
    val writer = new PrintWriter(phoFile)
    writer.print(m)
    writer.close()
    val mbrolaCommand = s"${Config.mbrolaExecutable} ${Config.mbrolaVoiceDb} - $wavFile"
    phoFile #> mbrolaCommand !
  }
}
