package org.misho

import java.io.FileInputStream
import java.util.Properties

object Config {
  private val filename = "misho.conf"
  private lazy val properties = load()

  lazy val mbrolaExecutable: String = properties.getProperty("mbrola.executable")
  lazy val mbrolaVoiceDb: String = properties.getProperty("mbrola.voice_db")

  private def load(): Properties = {
    val props = new Properties()
    props.load(new FileInputStream(filename))
    props
  }
}
