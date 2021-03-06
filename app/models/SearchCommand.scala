package models

import play.api.libs.json._

case class SearchCommand(
    query: Option[String],
    contexts: Seq[String],
    repositories: Seq[String],
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
                "field" -> "context.untouched"
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
                "field" -> "repository.untouched",
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

    val autocompleteFields = Map(
        "contexts" -> "context",
        "keywords" -> "keyword",
        "repositories" -> "repository"
    )

    def toAutocompleteQuery(query: String) = Json.obj(
        "size" -> 0,
        "facets" -> autocompleteFields.mapValues { field =>
            Json.obj(
                "terms" -> Json.obj("field" -> s"$field.untouched"),
                "facet_filter" -> Json.obj(
                    "term" -> Json.obj(s"$field.autocomplete" -> query)
                )
            )
        }
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

        private def contexts = command.contexts match {
            case Nil => None
            case contexts => Some(Json.obj("in" -> Json.obj("context" -> contexts)))
        }

        private def repositories = command.repositories match {
          case Nil => None
          case repositories => Some(Json.obj("in" -> Json.obj("repositories" -> command.repositories)))
        }

        private def queryWithFilters =  Json.obj(
            "filtered" -> Json.obj(
                "query" -> query,
                "filter" -> Json.obj(
                    "and" -> Json.obj(
                        "filters" -> (Seq(contexts, repositories).flatten ++ filterEmptyFields)
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