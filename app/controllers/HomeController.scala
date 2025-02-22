package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import model.{
  GameState, 
  Player, 
  Field, 
  GameEvent, 
  GameInterface, 
  SettingState, 
  MovingState, 
  FlyingState, 
  RemovingState,
  FieldInterface
}
import model.WinStrategy

import scala.util.{Try, Success, Failure}

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}



/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */

  def index(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok("Welcome to the WAP-Mill Game! " +
      "This is the REST API microservice for the webserver")
  }

  def action(): Action[JsValue] = Action(parse.json) { implicit request: Request[JsValue] =>
    val maybeToken = request.headers.get("Authorization").flatMap { header =>
      header.split(" ").lastOption
    }

    maybeToken match {
      case Some(token) if validateJWT(token) => {
          val body: JsValue = request.body
          (body \ "State").asOpt[JsValue] match 
            case Some(state) => {
              try {
                (body \ "Move").asOpt[JsValue] match
                  case Some(move) => {
                      val gameState: GameState = GameState.fromJson(state)
                      val currentPlayer: JsValue = (state \ "game" \ "currentPlayer").as[JsValue]
                      val twoPlayers = (state \ "game" \ "players").as[JsArray]
                      (body \ "Shift").asOpt[JsValue] match
                        case Some(shift) => {
                          val newState: Try[GameState] = doMove(gameState, move, Some(shift), currentPlayer, twoPlayers)
                            newState match
                              case Success(value) => 
                                doTurn(value, twoPlayers) match {
                                  case Some(updatedState) => Ok(updatedState.toJson)
                                  case None => Ok(Json.obj("message" -> "Das Spiel ist beendet."))
                                }
                              case Failure(ex) => BadRequest("Zug konnte nicht geführt werden")
                        }
                        case None => {
                          val newState: Try[GameState] = doMove(gameState, move, None, currentPlayer, twoPlayers)
                            newState match
                              case Success(value) => 
                                doTurn(value, twoPlayers) match {
                                  case Some(updatedState) => Ok(updatedState.toJson)
                                  case None => Ok(Json.obj("message" -> "Das Spiel ist beendet."))
                                }
                              case Failure(ex) => BadRequest("Zug konnte nicht geführt werden")
                        }
                  }
                  case None => BadRequest("Move not available")
              } catch {
                case ex: Exception => {
                  println(ex)
                  InternalServerError("Konnte nicht verarbeitet werden")
                }
              }
            }
            case None => BadRequest("State not available")
      }
      case _ => Unauthorized("Ungültiges oder fehlendes Authorization-Token")
    }
  }

  def validateJWT(token: String): Boolean = {
    val secret = sys.env.getOrElse("JWT_SECRET", "password")
    try {
      Jwt.decode(token, secret, Seq(JwtAlgorithm.HS256)).isSuccess
    } catch {
      case _: Exception => false
    }
  }

  def doMove(gameState: GameState, move: JsValue, shift: Option[JsValue], currentPlayer: JsValue, twoPlayers: JsArray): Try[GameState] = {
    val firstPlayer: JsValue = twoPlayers(0)
    val secondPlayer: JsValue = twoPlayers(1)
    val firstPlayerObj: Player = Player((firstPlayer \ "name").as[String], (firstPlayer \ "color").as[String])
    val secondPlayerObj: Player = Player((secondPlayer \ "name").as[String], (secondPlayer \ "color").as[String])

    val twoPlayersList: Array[Player] = Array(firstPlayerObj, secondPlayerObj)

    val currentPlayerObj = Player((currentPlayer \ "name").as[String], (currentPlayer \ "color").as[String])
    val enemy = twoPlayersList.find(p => p != currentPlayerObj).getOrElse(currentPlayerObj)

    
    val field: Field = Field((move \ "x").as[Int], (move \ "y").as[Int], (move \ "ring").as[Int], currentPlayerObj.color)

    val moving: Option[Field] = shift match
      case Some(value) => 
        Some(Field((value \ "x").as[Int], (value \ "y").as[Int], (value \ "ring").as[Int], color = "⚫"))
      case None => None
    
    val enemyField: Field = Field((move \ "x").as[Int], (move \ "y").as[Int], (move \ "ring").as[Int], enemy.color)

    if (gameState.isInstanceOf[SettingState]) {
      gameState.handle(GameEvent.OnSetting, (field, None))
    } else if (gameState.isInstanceOf[RemovingState]) {
      if (findFieldColor(enemyField.x, enemyField.y, enemyField.ring, gameState) != "⚫") {
        gameState.handle(GameEvent.OnRemoving, (enemyField, None))
      } else {
        return Failure(
          IllegalArgumentException(
            "InvalidUnsetField"
          )
        )
      }
    } else if(gameState.isInstanceOf[MovingState]) {
      if (findFieldColor(enemyField.x, enemyField.y, enemyField.ring, gameState) != "⚫"
          && findFieldColor(enemyField.x, enemyField.y, enemyField.ring, gameState) != enemy.color) {
        gameState.handle(GameEvent.OnMoving, (field, moving))
      } else {
        return Failure(
          IllegalArgumentException(
            "InvalidMoveField"
          )
        )
      }
    } else {
      if (findFieldColor(enemyField.x, enemyField.y, enemyField.ring, gameState) != "⚫"
          && findFieldColor(enemyField.x, enemyField.y, enemyField.ring, gameState) != enemy.color) {
        gameState.handle(GameEvent.OnFlying, (field, moving))
      } else {
        return Failure(
          IllegalArgumentException(
            "InvalidFlyingField"
          )
        )
      }
    }
  }

  def findFieldColor(x: Int, y: Int, ring: Int, gameState: GameState): String = {
    val fields = gameState.game.board.fields
    fields.find(f => f.x == x && f.y == y && f.ring == ring).map(_.color).getOrElse("⚫")
  }

  def doTurn(turn: GameState, twoPlayersJsValues: JsArray): Option[GameState] = {
    val winStrategy = WinStrategy.classicStrategy
    var currentGame: Option[GameInterface] = None
    var gameState: Option[GameState] = None
    val firstPlayer: JsValue = twoPlayersJsValues(0)
    val secondPlayer: JsValue = twoPlayersJsValues(1)
    val firstPlayerObj: Player = Player((firstPlayer \ "name").as[String], (firstPlayer \ "color").as[String])
    val secondPlayerObj: Player = Player((secondPlayer \ "name").as[String], (secondPlayer \ "color").as[String])

    val twoPlayers: Array[Player] = Array(firstPlayerObj, secondPlayerObj)

    turn match {
      case RemovingState(game: GameInterface) => {
        gameState = Some(turn)
        currentGame = Some(game)
      }

      case SettingState(game: GameInterface) => {
        gameState = Some(
          SettingState(
            game.copyCurrentPlayer(
                currentPlayer = twoPlayers.find(p => !p.equals(game.currentPlayer))
                  .get
              )
            )
        )
        currentGame = Some(game)
      }

      case MovingState(game: GameInterface) => {
        gameState = Some(
          MovingState(
            game.copyCurrentPlayer(
                currentPlayer = twoPlayers.find(p => !p.equals(game.currentPlayer))
                  .get
              )
            )
        )
        currentGame = Some(game)
      }

      case FlyingState(game: GameInterface) => {
        gameState = Some(
          FlyingState(
            game.copyCurrentPlayer(
                currentPlayer = twoPlayers.find(p => !p.equals(game.currentPlayer))
                  .get
              )
            )
        )
        currentGame = Some(game)
      }
    }
    if (winStrategy(currentGame.get)) {
      None
    } else {
      gameState
    }
  }
  
}
