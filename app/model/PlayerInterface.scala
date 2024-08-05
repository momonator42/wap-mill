package model

import scala.xml.Node
import play.api.libs.json.JsValue

trait PlayerInterface {
  val name: String
  val color: String
  def toXml: Node
  def toJson: JsValue
}
