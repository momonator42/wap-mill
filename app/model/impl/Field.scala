package model

import scala.xml.Node
import play.api.libs.json.JsValue
import play.api.libs.json.Json

case class Field(val x: Int, val y: Int, val ring: Int, val color: String = "⚫")
    extends FieldInterface {
  override def toString: String = color
  override def equals(field: Any): Boolean = field match {
    case f: FieldInterface =>
      f.x.equals(x) && f.y.equals(y) && f.ring.equals(ring)
    case _ => false
  }
  def copyColor(color: String): FieldInterface = copy(color = color)
  override def toXml: Node =
    <field>
      <x>{x.toString}</x>
      <y>{y.toString}</y>
      <ring>{ring.toString}</ring>
      <color>{color}</color>
    </field>
  override def toJson: JsValue = Json.obj(
    "x" -> Json.toJson(x),
    "y" -> Json.toJson(y),
    "ring" -> Json.toJson(ring),
    "color" -> Json.toJson(color)
  )
}

object Field {
  def fromXml(node: Node): FieldInterface = Field(
    x = (node \\ "x").text.trim.toInt,
    y = (node \\ "y").text.trim.toInt,
    ring = (node \\ "ring").text.trim.toInt,
    color = (node \\ "color").text.trim
  )
  def fromJson(json: JsValue): FieldInterface = Field(
    x = (json \ "x").as[Int],
    y = (json \ "y").as[Int],
    ring = (json \ "ring").as[Int],
    color = (json \ "color").as[String]
  )
}
