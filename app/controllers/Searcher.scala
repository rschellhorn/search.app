package controllers

import java.io.File
import models.SearchCommand
import play.api.{ Logger, Play }
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{ JsValue, Json }
import play.api.libs.ws.WS
import play.api.mvc.{ Action, Controller }

object Searcher extends Controller {

    val endpoint = Play.configuration.getString("elasticsearch.search.endpoint").get

    val commandForm = Form(
        mapping(
            "query" -> optional(text),
            "context" -> seq(text),
            "repository" -> seq(text),
            "order" -> default(text.verifying(SearchCommand.sortOrders.contains(_)), "title"),
            "page" -> default(number(min = 1), 1),
            "pageSize" -> default(number(min = 0, max = 100), 10),
            "facets" -> seq(text.verifying(SearchCommand.facets.keys.contains(_)))
        )(SearchCommand.apply)(SearchCommand.unapply)
    )

    def index = Action {
        Ok(views.html.main())
    }

    def search = Action { implicit request =>
        commandForm.bindFromRequest.fold(
            errors => Forbidden(errors.errorsAsJson),
            command => query(command.toSearchQuery)
        )
    }

    def autocomplete(q: String) = Action {
        query(SearchCommand.toAutocompleteQuery(q))
    }

    def show(id: String) = Action {
        Async {
            WS.url(s"$endpoint/lom/$id/_source").get.map {
                case response if response.status == OK => Ok.sendFile(new File((response.json\"file").as[String]), inline = true).as(XML)
                case response => NotFound
            }
        }
    }

    private def query(command: JsValue) = Async {
        WS.url(s"$endpoint/_search").post(command).map(r => Ok(r.json))
    }
}