package models

import play.api.libs.json._

case class SearchCommand(
    query: Option[String],
    context: Option[String],
    repository: Option[String],
    order: Option[String],
    page: Int,
    pageSize: Int)

object SearchCommand {

    val fields = Seq("title", "description", "location")

    val skeleton = Json.obj(
        "fields" -> fields,
        "highlight" -> Json.obj(
            "number_of_fragments" -> 1,
            "fields" -> Json.obj(
                "title" -> Json.obj(),
                "description" -> Json.obj()
            )
        ),
        "facets" -> Json.obj(
            "contexts" -> Json.obj(
                "terms" -> Json.obj(
                    "field" -> "context"
                )
            ),
            "repositories" -> Json.obj(
                "terms" -> Json.obj(
                    "field" -> "repository",
                    "size" -> 5
                )
            )
        )
    )

    val matchAll = Json.obj("match_all" -> Json.obj())
    val sortOrders = Map(
        "title" -> Json.obj(
            "sort" -> Json.obj(
                "title.untouched" -> Json.obj("order" -> "asc")
            )
        )
    )

    val filterEmptyFields = fields.map { field =>
        Json.obj("exists" -> Json.obj("field" -> field))
    }

    implicit class RichCommand(val command: SearchCommand) extends AnyVal {

        def toAutocompleteQuery = Json.obj(
            "query" -> Json.obj(
                "filtered" -> Json.obj(
                    "query" -> Json.obj(
                        "match" -> Json.obj(
                            "title.autocomplete" -> command.query
                        )
                    ),
                    "filter" -> Json.obj(
                        "and" -> Json.obj(
                            "filters" -> filterEmptyFields
                        )
                    )
                )
            ),
            "fields" -> Seq("title")
        )

        def toSearchQuery = {
            val query = command.query.map { query =>
                 Json.obj(
                    "multi_match" -> Json.obj(
                        "query" -> query,
                        "fields" -> Seq("title", "description"),
                        "type" -> "phrase_prefix"
                    )
                )
            }.getOrElse(matchAll)

            val context = command.context.map { context =>
                Json.obj("term" -> Json.obj("context" -> context))
            }

            val repository = command.repository.map { repository =>
                Json.obj("term" -> Json.obj("repository" -> repository))
            }

            val queryWithFilters = Seq(context, repository).flatten ++ filterEmptyFields match {
                case Nil => query
                case filters => Json.obj(
                    "filtered" -> Json.obj(
                        "query" -> query,
                        "filter" -> Json.obj(
                            "and" -> Json.obj(
                                "filters" -> filters
                            )
                        )
                    )
                )
            }

            val sort = command.order.flatMap(sortOrders.get)

            val suggest = command.query.map { query =>
                Json.obj(
                    "suggest" -> Json.obj(
                        "text" -> query,
                        "typos" -> Json.obj(
                            "phrase" -> Json.obj(
                                "size" -> 1,
                                "real_word_error_likelihood" -> 0.95,
                                "max_errors" -> 0.5,
                                "field" -> "_all"
                            )
                        )
                    )
                )
            }

            Json.obj(
                "query" -> queryWithFilters,
                "from" -> (command.page-1) * command.pageSize,
                "size" -> command.pageSize
            ) ++ Seq(sort, suggest).flatten.foldLeft(skeleton) { _ ++ _ }
        }
    }
}