package model

import model.PlayerInterface
import scala.xml.Node
import play.api.libs.json.JsValue
import play.api.libs.json.Json

case class Player(val name: String, val color: String) extends PlayerInterface {
  override def toString: String = s"$name $color"
  override def equals(player: Any): Boolean = player match {
    case p: PlayerInterface =>
      p.color.equals(color) && p.name.equals(name)
    case _ => false
  }
  override def toXml: Node =
    <player>
      <name>{name}</name>
      <color>{color}</color>
    </player>
  override def toJson: JsValue = Json.obj(
    "name" -> Json.toJson(name),
    "color" -> Json.toJson(color)
  )
}

object Player {
  def fromXml(node: Node): PlayerInterface = Player(
    name = (node \\ "name").text.trim,
    color = (node \\ "color").text.trim
  )
  def fromJson(json: JsValue): PlayerInterface = Player(
    name = (json \ "name").as[String],
    color = (json \ "color").as[String]
  )
}
