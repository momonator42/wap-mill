package model

import scala.util.Random

object WinStrategy {
  var strategy =
    if (Random.nextInt() % 2 == 0) classicStrategy
    else firstMillStrategy
  def classicStrategy(game: GameInterface) =
    game.everyPlayerHasSetItsStones && game.board.fields.count(field =>
      field.color == game.players
        .find(p => !p.equals(game.currentPlayer))
        .get
        .color
    ) < game.board.size
  def firstMillStrategy(game: GameInterface) = 0
    .until(game.board.size)
    .to(LazyList)
    .flatMap(j =>
      0.until(game.board.size)
        .to(LazyList)
        .map(k => {
          val possibleMillOnRow = game.board.fields
            .count(field =>
              field.y == j && field.ring == k && field.color == game.currentPlayer.color
            ) == game.board.fields.size
          val possibleMillOnColumn = game.board.fields
            .count(field =>
              field.x == j && field.ring == k && field.color == game.currentPlayer.color
            ) == game.board.fields.size
          val possibleMillOnRing = game.board.fields
            .count(field =>
              field.y == j && field.x == k && field.color == game.currentPlayer.color
            ) == game.board.fields.size
          possibleMillOnRow || possibleMillOnColumn || possibleMillOnRing
        })
    )
    .size >= 1
}
