package controllers

import java.io.File
import models.SearchCommand
import play.api.{ Logger, Play }
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.mvc.{ Action, Controller }

object Searcher extends Controller {

    val endpoint = Play.configuration.getString("elasticsearch.search.endpoint").get
    val sortOrder = Seq("relevance", "title")

    val commandForm = Form(
        mapping(
            "query" -> optional(text),
            "context" -> optional(text),
            "repository" -> optional(text),
            "order" -> optional(text.verifying(sortOrder.contains(_))),
            "page" -> default(number(min = 1), 1),
            "pageSize" -> default(number(min = 0, max = 100), 10)
        )(SearchCommand.apply)(SearchCommand.unapply)
    )

    def index = Action {
        Ok(views.html.main())
    }

    def search = Action { implicit request =>
        commandForm.bindFromRequest.fold(
            errors => Forbidden(errors.errorsAsJson),
            command => Async {
                WS.url(s"$endpoint/_search").post(command.toSearchQuery).map(r => Ok(r.json))
            }
        )
    }

    def autocomplete(q: String) = Action {
        Async {
            val command = commandForm.bind(Map("query" -> q)).get
            WS.url(s"$endpoint/_search").post(command.toAutocompleteQuery).map(r => Ok(r.json))
        }
    }

    def show(id: String) = Action {
        Async {
            WS.url(s"$endpoint/lom/$id").get.map {
                case response if response.status == NotFound => NotFound
                case response => Ok.sendFile(new File((response.json\"_source"\"file").as[String]), inline = true).as(XML)
            }
        }
    }
}