package models

import play.api.libs.json._

case class SearchCommand(
    query: Option[String],
    context: Option[String],
    repository: Option[String],
    order: String,
    page: Int,
    pageSize: Int,
    facets: Seq[String])

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
        )
    )

    val facets = Json.obj(
        "contexts" -> Json.obj(
            "terms" -> Json.obj(
                "field" -> "context"
            )
        ),
        "contributions" -> Json.obj(
            "nested" -> "contribution",
            "terms" -> Json.obj(
                "field" -> "contribution.name"
            )
        ),
        "repositories" -> Json.obj(
            "terms" -> Json.obj(
                "field" -> "repository",
                "size" -> 5
            )
        )
    )

    val sortOrders = Map(
        "relevance" -> Json.obj("_score" -> Json.obj()),
        "title" -> Json.obj("title.untouched" -> Json.obj("order" -> "asc"))
    )

    val filterEmptyFields = fields.map { field =>
        Json.obj("exists" -> Json.obj("field" -> field))
    }

    def toAutocompleteQuery(query: String) = Json.obj(
        "query" -> Json.obj(
            "filtered" -> Json.obj(
                "query" -> Json.obj(
                    "match" -> Json.obj(
                        "title.autocomplete" -> query
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

    implicit class RichCommand(val command: SearchCommand) extends AnyVal {
        private def query = command.query.map { query =>
             Json.obj(
                "multi_match" -> Json.obj(
                    "query" -> query,
                    "fields" -> Seq("title", "description"),
                    "type" -> "phrase_prefix"
                )
            )
        }

        private def context = command.context.map { context =>
            Json.obj("term" -> Json.obj("context" -> context))
        }

        private def repository = command.repository.map { repository =>
            Json.obj("term" -> Json.obj("repository" -> repository))
        }

        private def queryWithFilters =  Json.obj(
            "filtered" -> Json.obj(
                "query" -> query,
                "filter" -> Json.obj(
                    "and" -> Json.obj(
                        "filters" -> (Seq(context, repository).flatten ++ filterEmptyFields)
                    )
                )
            )
        )

        def toSearchQuery = command.page match {
            case 1 => completeQuery
            case n => Json.obj(
                "query" -> queryWithFilters,
                "from" -> (n-1) * command.pageSize,
                "size" -> command.pageSize,
                "sort" -> sortOrders(command.order)
            ) ++ skeleton
        }

        def completeQuery = {

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
                "size" -> command.pageSize,
                "sort" -> sortOrders(command.order),
                "facets" -> (facets.keys -- command.facets).foldLeft(facets) { (facets, name) => facets - name }
            ) ++ Seq(suggest).flatten.foldLeft(skeleton) { _ ++ _ }
        }
    }
}