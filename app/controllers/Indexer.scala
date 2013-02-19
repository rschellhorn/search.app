package controllers

import java.nio.file.{ Files, Paths }
import org.joda.time.Period
import play.api.{ Logger, Play }
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.mvc.{ Action, Controller }
import scala.collection.JavaConversions._
import scala.util.Try
import scala.xml.{ Node, NodeSeq }
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

/**
 * curl -XPOST --data-urlencode "store=/path/to/loms" http://localhost:9000/bulk-index
 */
object Indexer extends Controller {

    val endpoint = Play.configuration.getString("elasticsearch.index.endpoint").get
    val form = Form("store" -> text)

    def bulkIndex = Action { implicit request =>
        form.bindFromRequest.fold(
            errors => Forbidden(errors.errorsAsJson),
            store => Async {
                WS.url(endpoint).post(Play.current.getFile("conf/elasticsearch/index.json")).map { response =>
                    Logger.info(response.body)
                    for (
                        directory <- Files.newDirectoryStream(Paths.get(store)).filter(Files.isDirectory(_));
                        file <- Files.newDirectoryStream(directory)
                    ) {
                        val filename = file.getFileName().toString().replaceAllLiterally("%2F", ":")
                        val xml = scala.xml.XML.loadFile(file.toFile())
                        val json = Json.obj(
                            "title"             -> (xml\"general"\"title").bestValue,
                            "description"       -> (xml\"general"\"description").bestValue,
                            "keyword"           -> (xml\"general"\"keyword").bestValues,
                            "context"           -> (xml\"educational"\"context"\"value").map(_.text),
                            "costs"             -> ((xml\"rights"\"cost"\"value").text == "yes"),
                            "duration"          -> (xml\"educational"\"typicalLearningTime"\"duration").asDuration,
                            "contribution"      -> (xml\"lifecycle"\"contribute").asContributions,
                            "location"          -> (xml\"technical"\"location").headOption.map(_.text),
                            "repository"        -> filename.split(":").headOption,
                            "file"              -> file.toAbsolutePath().toString()
                        )
                        WS.url(s"$endpoint/lom/$filename").post(json).map { response =>
                            Logger.debug(response.body)
                        }
                    }
                    Ok
                }
            }
        )
    }

    private implicit class RichNodeSeq(val nodes: NodeSeq) extends AnyVal {

        private def contribution(node: Node) = {
            Json.obj(
                "timestamp" -> (node\"date"\"datetime").text
            )
        }

        private def langString(node: Node) = node.find { node => (node\"langstring"\"@language").text == "nl" }
                                         .orElse { node\"langstring" headOption }
                                         .map(_.text.trim)
                                         .filter(_.length > 0)

        def asDuration = Try(Period.parse(nodes.text)).map(_.getHours()).toOption
        def bestValue = nodes.headOption.flatMap(langString)
        def bestValues = nodes.flatMap(langString)
        def asContributions = nodes.map(contribution)
    }
}