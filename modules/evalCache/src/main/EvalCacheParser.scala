package lila.evalCache

import org.joda.time.DateTime
import play.api.libs.json.JsObject

import chess.format.Uci
import EvalCacheEntry._
import lila.common.PimpedJson._
import lila.tree.Eval._
import lila.user.User

private object EvalCacheParser {

  def parsePut(user: User, o: JsObject): Option[Input.Candidate] = for {
    d <- o obj "d"
    variant = chess.variant.Variant orDefault ~d.str("variant")
    if variant.standard
    fen <- d str "fen"
    nodes <- d int "nodes"
    depth <- d int "depth"
    pvObjs <- d objs "pvs"
    pvs <- pvObjs.map(parsePv).sequence.flatMap(_.toNel)
  } yield Input.Candidate(fen, Eval(
    pvs = pvs,
    nodes = nodes,
    depth = depth,
    by = user.id,
    date = DateTime.now))

  private def parsePv(d: JsObject): Option[Pv] = for {
    movesStr <- d str "moves"
    moves <- movesStr.split(' ').take(EvalCacheEntry.MAX_PV_SIZE).toList.foldLeft(List.empty[Uci].some) {
      case (Some(ucis), str) => Uci(str) map (_ :: ucis)
      case _                 => None
    }.flatMap(_.reverse.toNel) map Moves.apply
    cp = d int "cp" map Cp.apply
    mate = d int "mate" map Mate.apply
    score <- cp.map(Score.cp) orElse mate.map(Score.mate)
  } yield Pv(score, moves)
}
