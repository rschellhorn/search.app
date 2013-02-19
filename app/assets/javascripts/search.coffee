app = angular.module 'searchApp', ['chart.directives', '$strap.directives']
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

    $scope.command =
        query: ''
        order: 'relevance'
        page: 1
        pageSize: 10

    $scope.resultForms =
        0: 'Geen resultaten'
        one: '{} resultaat'
        other: '{{hits.total|number}} resultaten'

    $scope.suggest = (suggestion) ->
        $scope.command.query = suggestion
        $scope.search()

    $scope.gotoPage = (page) ->
        $scope.command.page = page
        $scope.search()

    $scope.typeahead = (query, callback) -> $http.get("/autocomplete?q=#{query}").success callback

    $scope.search = ->
        $http.post('/', $scope.command).success (data) ->
            maxPages = data.hits.total / $scope.command.pageSize
            pages = [($scope.command.page - 5) ... ($scope.command.page + 5)].filter (p) -> p > 0 and p < maxPages

            $scope.hits = toHits(data.hits)
            $scope.pages = pages
            $scope.suggestions = data.suggestions
            $scope.contexts = termsChart(data.facets.contexts)
            $scope.repositories = termsChart(data.facets.repositories)

    $scope.search()