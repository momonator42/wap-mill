package model

import scala.util.{Try, Success, Failure}
import model.PlayerInterface
import play.api.libs.json.JsValue
import scala.xml.Node
import play.api.libs.json.Json
/*
  1             2             3
1 ⚫――――――――――――⚫――――――――――――⚫
  │   ⚫――――――――⚫――――――――⚫   │ 1
  │   │   ⚫――――⚫――――⚫   │ 2 │
  │   │   │            │ 3 │   │
2 ⚫――⚫――⚫          ⚫――⚫――⚫
  │   │   │            │   │   │
  │   │   ⚫――――⚫――――⚫   │   │
  │   ⚫――――――――⚫――――――――⚫   │
3 ⚫――――――――――――⚫――――――――――――⚫
 */
case class Game(
    val board: BoardInterface,
    val players: Array[PlayerInterface],
    val currentPlayer: PlayerInterface,
    val setStones: Int = 0
) extends GameInterface {
  override def equals(game: Any): Boolean = game match {
    case g: GameInterface =>
      g.board.equals(board) && g.players.sameElements(players)
    case _ => false
  }
  def isValidSet(field: FieldInterface): Boolean =
    val validField: Boolean =
    field.x < board.size && field.x >= 0 && field.y < board.size &&
    field.y >= 0 && field.ring < board.size && field.ring >= 0

    val unsetColor: Option[Boolean] = board.fields.find(_.equals(field)).map(f => f.color == field.unsetFieldColor)

    (validField, unsetColor) match {
    case (true, Some(bool)) => bool
    case _ => false
  }

  def isValidMove(from: FieldInterface, to: FieldInterface): Boolean =
    isValidSet(to) &&
      (Math.abs(from.x - to.x) == 1 ^ Math.abs(from.y - to.y) == 1
        ^ Math.abs(from.ring - to.ring) == 1)

  def isMill(to: FieldInterface): Boolean = {
    def countFieldsTo = countFields(to);
    val possibleMillOnRow = countFieldsTo(field => field.x == to.x)
    val possibleMillOnColumn = countFieldsTo(field => field.y == to.y)
    val isMiddlePoint =
      to.x == Math.floor(board.size / 2) || to.y == Math.floor(
        board.size / 2
      )
    if (isMiddlePoint) {
      val possibleMillOnRing = board.fields
        .count(field =>
          field.y == to.y && field.x == to.x && field.color == currentPlayer.color
        ) == board.size
      return possibleMillOnRow || possibleMillOnColumn || possibleMillOnRing
    }
    possibleMillOnColumn || possibleMillOnRow
  }

  def countFields(to: FieldInterface)(condition: FieldInterface => Boolean): Boolean = {
  board.fields
    .count(field =>
      condition(field) && field.ring == to.ring && field.color == currentPlayer.color
    ) == board.size
  }

  def everyPlayerHasSetItsStones =
    setStones == Math.pow(board.size, 2).toInt * players.length
  def copyStones(setStones: Int): GameInterface = copy(setStones = setStones)
  def copyBoard(board: BoardInterface): GameInterface = copy(board = board)
  def copyCurrentPlayer(currentPlayer: PlayerInterface): GameInterface =
    copy(currentPlayer = currentPlayer)
  override def toJson: JsValue = Json.obj(
    "board" -> Json.toJson(board.toJson),
    "players" -> Json.toJson(players.map(_.toJson)),
    "currentPlayer" -> Json.toJson(currentPlayer.toJson),
    "setStones" -> Json.toJson(setStones)
  )
  override def toXml: Node =
    <game>
      {board.toXml}
      {players.map(_.toXml)}
      <currentPlayer>{currentPlayer.toXml}</currentPlayer>
      <setStones>{setStones.toString}</setStones>
    </game>
}

object Game {
  def fromXml(node: Node): Game = Game(
    board = Board.fromXml((node \\ "board").head),
    players = (node \\ "player").map(n => Player.fromXml(n)).toArray,
    currentPlayer = Player.fromXml((node \\ "currentPlayer").head),
    setStones = (node \\ "setStones").text.trim.toInt
  )
  def fromJson(json: JsValue): Game = Game(
    board = Board.fromJson((json \ "board").get),
    players = (json \ "players")
      .validate[Array[JsValue]]
      .get
      .map(j => Player.fromJson(j))
      .toArray,
    currentPlayer = Player.fromJson((json \ "currentPlayer").get),
    setStones = (json \ "setStones").as[Int]
  )
}
