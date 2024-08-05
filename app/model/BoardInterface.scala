package model

import scala.xml.Node
import play.api.libs.json.JsValue

trait BoardInterface {
  val fields: List[FieldInterface]
  val size: Int
  def fieldsDump: String
  def getField(x: Int, y: Int, ring: Int): Option[FieldInterface]
  def toXml: Node
  def toJson: JsValue
}
