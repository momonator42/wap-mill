package model

import scala.util.{Try, Success, Failure}
import util.Messages
import play.api.libs.json.JsValue
import scala.xml.Node
import play.api.libs.json.Json

object GameState {
  def fromJson(json: JsValue): GameState = (json \ "type").as[String] match {
    case "SettingState"  => SettingState(Game.fromJson((json \ "game").get))
    case "MovingState"   => MovingState(Game.fromJson((json \ "game").get))
    case "FlyingState"   => FlyingState(Game.fromJson((json \ "game").get))
    case "RemovingState" => RemovingState(Game.fromJson((json \ "game").get))
  }
  def fromXml(node: Node): GameState =
    (node \\ "type").text.trim match {
      case "SettingState" => SettingState(Game.fromXml((node \\ "game").head))
      case "MovingState"  => MovingState(Game.fromXml((node \\ "game").head))
      case "FlyingState"  => FlyingState(Game.fromXml((node \\ "game").head))
      case "RemovingState" =>
        RemovingState(Game.fromXml((node \\ "game").head))
    }
}

sealed trait GameState(val game: GameInterface) {
  override def equals(state: Any): Boolean = state match {
    case s: GameState => s.game.equals(game)
    case _            => false
  }
  def nextState(
      game: GameInterface,
      fields: (FieldInterface, Option[FieldInterface])
  ): GameState
  def execute(fields: (FieldInterface, Option[FieldInterface])): Try[GameState]
  def handle(
      e: GameEvent,
      fields: (FieldInterface, Option[FieldInterface])
  ): Try[GameState] = {
    e match {
      case GameEvent.OnSetting =>
        if (!this.isInstanceOf[SettingState])
          return Failure(
            IllegalArgumentException(
              Messages.isNotSettingStateMessage
            )
          )

      case GameEvent.OnMoving =>
        if (!this.isInstanceOf[MovingState])
          return Failure(
            IllegalArgumentException(
              Messages.isNotMovingStateMessage
            )
          )

      case GameEvent.OnFlying =>
        if (!this.isInstanceOf[FlyingState])
          return Failure(
            IllegalArgumentException(
              Messages.isNotFlyingStateMessage
            )
          )
      case GameEvent.OnRemoving =>
        if (!this.isInstanceOf[RemovingState])
          return Failure(
            IllegalArgumentException(
              Messages.isNotRemovingStateMessage
            )
          )

    }
    execute(fields)
  }
  def toJson: JsValue = Json.obj(
    "type" -> this.getClass.getSimpleName,
    "game" -> Json.toJson(game.toJson)
  )
  def toXml: Node =
    <GameState>
      <type>{this.getClass.getSimpleName}</type>
      {game.toXml}
   </GameState>
}

private trait Moving(game: GameInterface) {
  def movePiece(
      fields: (FieldInterface, Option[FieldInterface])
  ): Try[GameInterface] = {
    if (fields(0).color != game.currentPlayer.color)
      return Failure(
        IllegalArgumentException(
          Messages.movedOtherPieceMessage
        )
      )

    Success(
      game.copyBoard(board =
        Board(
          game.board.fields
            .updated(
              game.board.fields.indexOf(fields(0)),
              fields(0).copyColor(color = fields(0).unsetFieldColor)
            )
            .updated(
              game.board.fields.indexOf(fields(1).get),
              fields(1).get.copyColor(color = game.currentPlayer.color)
            ),
          game.board.size
        )
      )
    )
  }
}

case class MovingState(override val game: GameInterface)
    extends GameState(game)
    with Moving(game) {
  override def nextState(
      game: GameInterface,
      fields: (FieldInterface, Option[FieldInterface])
  ): GameState =
    if (game.isMill(fields(1).get)) RemovingState(game)
    else copy(game)
  override def execute(
      fields: (FieldInterface, Option[FieldInterface])
  ): Try[GameState] = {
    if (!game.isValidMove(fields(0), fields(1).get))
      return Failure(
        IllegalArgumentException(
          Messages.invalidMoveFieldAlreadyInUseMessage
        )
      )

    movePiece(fields).map(game => nextState(game, fields))
  }
}
case class SettingState(override val game: GameInterface)
    extends GameState(game) {
  override def nextState(
      game: GameInterface,
      fields: (FieldInterface, Option[FieldInterface])
  ): GameState =
    if (game.isMill(fields(0)))
      RemovingState(game.copyStones(setStones = game.setStones + 1))
    else if (
      game.copyStones(setStones = game.setStones + 1).everyPlayerHasSetItsStones
    ) MovingState(game)
    else copy(game.copyStones(setStones = game.setStones + 1))

  override def execute(
      fields: (FieldInterface, Option[FieldInterface])
  ): Try[GameState] = {
    if (!game.isValidSet(fields(0)))
      return Failure(
        IllegalArgumentException(
          Messages.invalidSetFieldAlreadyInUseMessage
        )
      )
    Success(
      nextState(
        game.copyBoard(
          board = Board(
            game.board.fields.updated(
              game.board.fields.indexOf(fields(0)),
              fields(0).copyColor(color = game.currentPlayer.color)
            ),
            game.board.size
          )
        ),
        fields
      )
    )
  }
}
case class RemovingState(
    override val game: GameInterface
) extends GameState(game) {
  override def nextState(
      game: GameInterface,
      fields: (FieldInterface, Option[FieldInterface])
  ): GameState = {
    var nextState: GameState = SettingState(game)
    if (game.everyPlayerHasSetItsStones) {
      nextState = MovingState(game)
      val otherPlayersPieces = game.board.fields.count(field =>
        field.color == game.players
          .find(p => !p.equals(game.currentPlayer))
          .get
          .color
      )
      if (otherPlayersPieces == game.board.size) {
        nextState = FlyingState(game)
      }
    }
    nextState
  }
  override def execute(
      fields: (FieldInterface, Option[FieldInterface])
  ): Try[GameState] = {
    if (fields(0).color == game.currentPlayer.color)
      return Failure(
        IllegalArgumentException(
          Messages.invalidRemoveFieldOwnPieceMessage
        )
      )

    if (fields(0).color == fields(0).unsetFieldColor)
      return Failure(
        IllegalArgumentException(
          Messages.invalidRemoveFieldUnsetFieldMessage
        )
      )

    if (game.isMill(fields(0)))
      return Failure(
        IllegalArgumentException(
          Messages.invalidRemoveFieldOnAMillMessage
        )
      )

    Success(
      nextState(
        game.copyBoard(
          board = Board(
            game.board.fields
              .updated(
                game.board.fields.indexOf(fields(0)),
                fields(0).copyColor(color = fields(0).unsetFieldColor)
              ),
            game.board.size
          )
        ),
        fields
      )
    )
  }
}
case class FlyingState(
    override val game: GameInterface
) extends GameState(game)
    with Moving(game) {
  override def nextState(
      game: GameInterface,
      fields: (FieldInterface, Option[FieldInterface])
  ): GameState = if (game.isMill(fields(1).get)) RemovingState(game)
  else copy(game)
  override def execute(
      fields: (FieldInterface, Option[FieldInterface])
  ): Try[GameState] = {
    if (!game.isValidSet(fields(1).get))
      return Failure(
        IllegalArgumentException(
          Messages.invalidFlyingFieldAlreadyInUseMessage
        )
      )
    movePiece(fields).map(game => nextState(game, fields))
  }
}

enum GameEvent {
  case OnSetting extends GameEvent
  case OnMoving extends GameEvent
  case OnRemoving extends GameEvent
  case OnFlying extends GameEvent
}
