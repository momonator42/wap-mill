package model

import scala.xml.Node
import play.api.libs.json.JsValue

trait FieldInterface {
  val x: Int
  val y: Int
  val ring: Int
  val color: String
  def unsetFieldColor = "⚫"
  override def toString: String
  override def equals(field: Any): Boolean
  def copyColor(color: String): FieldInterface
  def toXml: Node
  def toJson: JsValue
}
