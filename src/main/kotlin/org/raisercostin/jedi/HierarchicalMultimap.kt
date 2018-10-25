package org.raisercostin.jedi

import scala.util.Properties
import scala.util.Try
import scala.collection.mutable.Buffer

interface HierarchicalMultimap {
  fun subset(key: String): HierarchicalMultimap
  fun get(key: String): Option<String>
  fun list(key: String): Option<String>
  fun asMap: Map<String, List<String>>
}

object HierarchicalMultimap {
  fun apply(): HierarchicalMultimap = EmptyHMap()
  fun apply(data: String): HierarchicalMultimap = load(Locations.memory("a").writeContent(data))
  fun apply(data: InputLocation): HierarchicalMultimap = load(data)
  fun load(data: InputLocation): HierarchicalMultimap {
    val prop = java.util.Properties()
    data.usingInputStream { s -> prop.load(s) }
    import scala.collection.JavaConverters._
    MapHMap2(prop.asScala.toMap.mapValues(Seq(_)))
  }
  fun save(map: HierarchicalMultimap, data: OutputLocation): Try<Unit> = Try {
    println(map)
    val prop = java.util.Properties()
    import scala.collection.JavaConverters._
    prop.putAll(map.asMap.map {
      (x, y) if x == null -> ("", toLine(y))
      (x, y)              -> (x, toLine(y))
    }.asJava)
    data.usingWriter(s -> prop.store(s, "saved HMap"))
  }
  fun toLine(line: List<String>): String = line.mkString(",")
}

data class EmptyHMap() : HierarchicalMultimap {
  fun subset(key: String): HierarchicalMultimap = this
  fun get(key: String): Option<String> = None
  fun list(key: String): Option<String> = None
  fun asMap: Map<String, List<String>> = Map()
}
data class MapHMap2(map: Map<String, List<String>>) : HierarchicalMultimap {
  fun subset(key: String): HierarchicalMultimap = subsetString(key + ".")
  private fun subsetString(key: String): HierarchicalMultimap = MapHMap2(map.filterKeys(_.startsWith(key)).map { (k, v) -> k.stripPrefix(key) -> v })
  fun get(key: String): Option<String> = map.getOrElse(key, Seq()).headOption
  fun list(key: String): Option<String> = None
  fun asMap: Map<String, List<String>> = map
}
/**HMap , good performance on subset,key,list.*/
object FastHMap {
  fun apply(map: Map<String, List<String>>):HierarchicalMultimap = FastHMap("", map)
}
data class FastHMap(prefix: String, map: Map<String, List<String>>) : HierarchicalMultimap {
  fun subset(key: String): HierarchicalMultimap = FastHMap(prefix + "." + key, map)
  fun get(key: String): Option<String> = map.getOrElse(prefix + "." + key, Seq()).headOption
  fun list(key: String): Option<String> = None
  fun asMap: Map<String, List<String>> =
    if (prefix.isEmpty)
      map
    else
      map.filterKeys(_.startsWith(prefix + ".")).map { (k, v) -> k.stripPrefix(prefix + ".") -> v }
}

data class HttpHMap(request: Map<String, List<String>>, response: Map<String, List<String>>) : HierarchicalMultimap {
  val all = request.map { (key, value) -> "request." + key -> value } ++ response.map { (key, value) -> "response." + key -> value }
  fun subset(key: String): HierarchicalMultimap = key when {
    "request" ->
      FastHMap(request)
    "response" ->
      FastHMap(response)
    else ->
      EmptyHMap()
  }
  fun get(key: String): Option<String> = all.getOrElse(key, Seq()).headOption
  fun list(key: String): Option<String> = None
  fun asMap: Map<String, List<String>> = all
}