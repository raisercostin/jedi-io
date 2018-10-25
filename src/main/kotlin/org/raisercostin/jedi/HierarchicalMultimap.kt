package org.raisercostin.jedi

import scala.util.Properties
import scala.util.Try
import scala.collection.mutable.Buffer

trait HierarchicalMultimap {
  def subset(key: String): HierarchicalMultimap
  def get(key: String): Option[String]
  def list(key: String): Option[String]
  def asMap: Map[String, Seq[String]]
}

object HierarchicalMultimap {
  def apply(): HierarchicalMultimap = EmptyHMap()
  def apply(data: String): HierarchicalMultimap = load(Locations.memory("a").writeContent(data))
  def apply(data: InputLocation): HierarchicalMultimap = load(data)
  def load(data: InputLocation): HierarchicalMultimap = {
    val prop = new java.util.Properties()
    data.usingInputStream { s => prop.load(s) }
    import scala.collection.JavaConverters._
    MapHMap2(prop.asScala.toMap.mapValues(Seq(_)))
  }
  def save(map: HierarchicalMultimap, data: OutputLocation): Try[Unit] = Try {
    println(map)
    val prop = new java.util.Properties()
    import scala.collection.JavaConverters._
    prop.putAll(map.asMap.map {
      case (x, y) if x == null => ("", toLine(y))
      case (x, y)              => (x, toLine(y))
    }.asJava)
    data.usingWriter(s => prop.store(s, "saved HMap"))
  }
  def toLine(line: Seq[String]): String = line.mkString(",")
}

case class EmptyHMap() extends HierarchicalMultimap {
  def subset(key: String): HierarchicalMultimap = this
  def get(key: String): Option[String] = None
  def list(key: String): Option[String] = None
  def asMap: Map[String, Seq[String]] = Map()
}
case class MapHMap2(map: Map[String, Seq[String]]) extends HierarchicalMultimap {
  def subset(key: String): HierarchicalMultimap = subsetString(key + ".")
  private def subsetString(key: String): HierarchicalMultimap = MapHMap2(map.filterKeys(_.startsWith(key)).map { case (k, v) => k.stripPrefix(key) -> v })
  def get(key: String): Option[String] = map.getOrElse(key, Seq()).headOption
  def list(key: String): Option[String] = None
  def asMap: Map[String, Seq[String]] = map
}
/**HMap with good performance on subset,key,list.*/
object FastHMap {
  def apply(map: Map[String, Seq[String]]):HierarchicalMultimap = FastHMap("", map)
}
case class FastHMap(prefix: String, map: Map[String, Seq[String]]) extends HierarchicalMultimap {
  def subset(key: String): HierarchicalMultimap = FastHMap(prefix + "." + key, map)
  def get(key: String): Option[String] = map.getOrElse(prefix + "." + key, Seq()).headOption
  def list(key: String): Option[String] = None
  def asMap: Map[String, Seq[String]] =
    if (prefix.isEmpty)
      map
    else
      map.filterKeys(_.startsWith(prefix + ".")).map { case (k, v) => k.stripPrefix(prefix + ".") -> v }
}

case class HttpHMap(request: Map[String, Seq[String]], response: Map[String, Seq[String]]) extends HierarchicalMultimap {
  val all = request.map { case (key, value) => "request." + key -> value } ++ response.map { case (key, value) => "response." + key -> value }
  def subset(key: String): HierarchicalMultimap = key match {
    case "request" =>
      FastHMap(request)
    case "response" =>
      FastHMap(response)
    case _ =>
      EmptyHMap()
  }
  def get(key: String): Option[String] = all.getOrElse(key, Seq()).headOption
  def list(key: String): Option[String] = None
  def asMap: Map[String, Seq[String]] = all
}
