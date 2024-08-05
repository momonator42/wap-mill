package model
import scala.util.{Try, Success, Failure}
import java.lang.IllegalArgumentException
import scala.collection.immutable.ListMap
import scala.xml.Node
import play.api.libs.json.JsValue
import play.api.libs.json.Json

object Board {
  case class NewBoard(val fields: List[FieldInterface], val size: Int)
      extends BoardInterface {
    val endOfLine = sys.props("line.separator")
    val lineHeight = 1
    val barWidth = 4
    val spaceWidth = 3

    override def getField(x: Int, y: Int, ring: Int): Option[FieldInterface] = {
      for {
        field <- fields.find(f =>
          f.equals(
            Field(
              x,
              y,
              ring
            )
          )
        )
      } yield field

      getField(x, y, ring) match {
        case Some(f) => Some(f)
        case None    => None
      }
    }
    override def equals(board: Any): Boolean = board match {
      case b: BoardInterface =>
        b.size.equals(size) && b.fields.equals(fields)
      case _ => false
    }

    def lowerSection: Map[Int, List[FieldInterface]] = ListMap(
      fields
        .filter(field => field.y == (size - 1))
        .groupBy(_.ring)
        .toSeq
        .sortWith(_._1 > _._1): _*
    )
    def middleSection: (List[FieldInterface], List[FieldInterface]) = {
      val section = fields
        .filter(field => field.y > 0 && field.y < (size - 1))
        .sortBy(_.x)
      section.splitAt(section.length / 2)
    }
    def upperSection: Map[Int, List[FieldInterface]] = fields
      .filter(field => field.y == 0)
      .groupBy(_.ring)

    override def toString(): String = {
      def bar(width: Int = barWidth) = "―" * width
      def line(height: Int = lineHeight) = "│" * height
      def space(width: Int = spaceWidth) = " " * width
      def dividerRow =
        (line() + space()) * size + space() * (size - 1) + (space() + line()) * size
          + endOfLine
      def formattedFieldsByRing = (fieldsByRing: (Int, List[FieldInterface])) =>
        (line() + space()) * fieldsByRing(0) + fieldsByRing(1)
          .mkString(bar((((size - 1) - fieldsByRing(0)) + 1) * barWidth))
          + (space() + line()) * fieldsByRing(0)

      endOfLine
        + upperSection
          .map(formattedFieldsByRing)
          .mkString(endOfLine)
        + endOfLine
        + dividerRow
        + middleSection(0)
          .sortWith(_.ring < _.ring)
          .mkString(
            bar(barWidth / 2)
          ) + space() * size
        + middleSection(1)
          .sortWith(_.ring > _.ring)
          .mkString(
            bar(barWidth / 2)
          ) + endOfLine
        + dividerRow
        + lowerSection
          .map(formattedFieldsByRing)
          .mkString(endOfLine)
    }
    override def fieldsDump = fields
      .map(field => s"(${field.x}, ${field.y}, ${field.ring})")
      .mkString(",")

    override def toXml: Node =
      <board>
        {fields.map(_.toXml)}
        <size>{size.toString}</size>
      </board>
    override def toJson: JsValue = Json.obj(
      "fields" -> Json.toJson(fields.map(_.toJson)),
      "size" -> Json.toJson(size)
    )
  }
  def withSize(size: Int = 3): Try[BoardInterface] = {
    // check if board size is valid
    if (size < 3 || size % 2 == 0 || size > 9)
      return Failure(
        IllegalArgumentException(
          "Invalid board size. Should be uneven and greater than 2 and less than 10."
        )
      )

    val fields = (0 until size)
      .to(LazyList)
      .flatMap { ring =>
        (0 until size).to(LazyList).flatMap { row =>
          (0 until size)
            .to(LazyList)
            .filterNot { col =>
              row > 0 && row < size - 1 && col > 0 && col < size - 1
            }
            .map(col => Field(col, row, ring))
        }
      }
      .toList

    Success(
      NewBoard(fields, size)
    )
  }
  def apply(fields: List[FieldInterface], size: Int): BoardInterface = {
    NewBoard(fields, size)
  }
  def fromXml(node: Node): BoardInterface = NewBoard(
    fields = (node \\ "field").map(n => Field.fromXml(n)).toList,
    size = (node \\ "size").text.trim.toInt
  )
  def fromJson(json: JsValue): BoardInterface = NewBoard(
    fields = (json \ "fields")
      .validate[List[JsValue]]
      .get
      .map(j => Field.fromJson(j)),
    size = (json \ "size").as[Int]
  )
}
