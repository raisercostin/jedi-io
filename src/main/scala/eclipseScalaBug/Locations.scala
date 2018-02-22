package eclipseScalaBug
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.nio.charset.CodingErrorAction
import java.nio.file.FileStore
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDate

import scala.io.Codec.decoder2codec

import org.apache.commons.io.IOUtils

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.io.BufferedSource
import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.Failure
import scala.util.Properties
import scala.util.Success
import scala.util.Try

import org.apache.commons.io.{ FileUtils => CommonsFileUtils }
import org.apache.commons.io.FilenameUtils
import org.raisercostin.jedi.impl.Escape

import sun.net.www.protocol.file.FileURLConnection

import java.util.UUID

import org.apache.commons.codec.digest.DigestUtils

object Locations {
  val logger = org.slf4j.LoggerFactory.getLogger("locations")
  private val tmpdir = new File(System.getProperty("java.io.tmpdir"))
  //def temp: TempLocation = TempLocation(tmpdir)
}
trait IsFileOrFolder {
}

trait BaseLocation extends IsFileOrFolder {
}
trait LocationState
trait ResolvedLocationState extends LocationState with IsFileOrFolder {
  type MetaRepr <: InputLocation
}
trait HierarchicalMultimap {
}

trait AbsoluteBaseLocation extends BaseLocation with ResolvedLocationState { self =>
  type Repr = self.type
}
trait VersionedLocation extends ResolvedLocationState
trait InputLocation extends AbsoluteBaseLocation with ResolvedLocationState with VersionedLocation { self =>
  override type Repr = self.type
}
