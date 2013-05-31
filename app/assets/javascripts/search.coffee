app = angular.module 'searchApp', ['chart.directives', 'infinite-scroll', '$strap.directives']
app.controller 'SearchCtrl', ($scope, $http) ->

    toHits = (data) ->
        total: data.total
        hits: data.hits.map (hit) ->
            id: hit._id
            title: hit.highlight?.title or hit.fields.title
            description: hit.highlight?.description or hit.fields.description
            location: hit.fields.location

    termsChart = (data) ->
        chart:
            borderWidth: 2
        credits:
            enabled: false
        plotOptions:
            series:
                animation: false
        series: [
            data: data.terms.map (term) ->
                name: term.term
                y: term.count
        ]

    busy = false

    $scope.command =
        query: ''
        order: 'relevance'
        page: 1,
        facets: [ "contexts", "contributions", "repositories" ]

    $scope.resultForms =
        0: 'Geen resultaten'
        one: '{} resultaat'
        other: '{{hits.total|number}} resultaten'

    $scope.suggest = (suggestion) ->
        $scope.command.query = suggestion
        $scope.search()

    $scope.typeahead = (query, callback) -> $http.get("/autocomplete?q=#{query}").success (data) ->
        callback data.hits.hits.map (hit) ->
            hit.fields.title

    $scope.nextPage = ->
      if (!$scope.busy)
          busy = true
          $scope.command.page += 1
          $http.post('/', $scope.command).success (data) ->
            $scope.hits.hits.push(hit) for hit in toHits(data.hits).hits
            busy = false

    $scope.search = ->
        $scope.command.page = 1
        $http.post('/', $scope.command).success (data) ->
            $scope.hits = toHits(data.hits)
            $scope.suggestions = data.suggestions
            if (data.facets)
                $scope.contexts = termsChart(data.facets.contexts)
                $scope.repositories = termsChart(data.facets.repositories)
                $scope.contributions = termsChart(data.facets.contributions)

    $scope.search()