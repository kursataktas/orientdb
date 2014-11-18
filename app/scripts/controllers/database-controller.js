var dbModule = angular.module('database.controller', ['database.services', 'bookmarks.services']);
dbModule.controller("BrowseController", ['$scope', '$routeParams', '$location', 'Database', 'CommandApi', 'localStorageService', 'Spinner', '$modal', '$q', '$window', 'Bookmarks', 'Notification', 'Aside', function ($scope, $routeParams, $location, Database, CommandApi, localStorageService, Spinner, $modal, $q, $window, Bookmarks, Notification, Aside) {

    $scope.database = Database;
    $scope.limit = 20;

    $scope.bookmarksClass = "";
    $scope.items = [
        {name: "history", title: "History"},
        {name: "bookmarks", title: "Bookmarks"}
    ];


    Aside.show({scope: $scope, title: "Bookmarks", template: 'views/database/context/bookmarksAside.html', show: false, absolute: true});
    $scope.item = {};
    $scope.queries = [];
    $scope.$watch("queryText", function (val) {
        $scope.item.query = val;
    });
    $scope.context = $scope.items[0];
    $scope.nContext = $scope.items[1];
    $scope.nextPage = function () {
        var idx = $scope.items.indexOf($scope.context);
        var newIdx = (idx < $scope.items.length - 1) ? idx + 1 : 0;
        $scope.context = $scope.items[newIdx];
        var nextIdx = (newIdx < $scope.items.length - 1) ? newIdx + 1 : 0;
        $scope.nContext = $scope.items[nextIdx];
    }
    $scope.countPage = 5;

    $scope.toggleBookmarks = function () {
        Aside.toggle();
    }


    if (Database.hasClass(Bookmarks.CLAZZ)) {
        Bookmarks.getAll(Database.getName());
    } else {
        Bookmarks.init(Database.getName()).then(function () {
            Bookmarks.getAll(Database.getName());
        });
    }

    $scope.hideSettings = localStorageService.get("hideSettings");
    if ($scope.hideSettings == null) {
        $scope.hideSettings = true;
        localStorageService.add("hideSettings", $scope.hideSettings);
    } else {
        $scope.hideSettings = JSON.parse($scope.hideSettings);
    }
    $scope.keepLimit = localStorageService.get("keepLimit");
    if (!$scope.keepLimit) {
        $scope.keepLimit = 10;
        localStorageService.add("keepLimit", $scope.keepLimit);
    }
    $scope.shallow = localStorageService.get("shallowCollection");

    if ($scope.shallow == null) {
        $scope.shallow = true;
        localStorageService.add("shallowCollection", $scope.shallow);

    } else {
        $scope.shallow = JSON.parse($scope.shallow);
    }
    $scope.countPageOptions = [5, 10, 20, 50, 100, 500, 1000, 2000, 5000];
    var dbTime = localStorageService.get("Timeline");
    if (!dbTime || dbTime instanceof  Array) {
        dbTime = new Object;
        localStorageService.add("Timeline", dbTime);
    }

    if (!dbTime[Database.getName()]) {
        dbTime[Database.getName()] = new Object
        localStorageService.add("Timeline", dbTime);
    } else if (dbTime[Database.getName()] instanceof Array) {
        dbTime[Database.getName()] = new Object
        localStorageService.add("Timeline", dbTime);

    }
    $scope.timeline = dbTime[Database.getName()][Database.currentUser()];

    if (!$scope.timeline) {
        $scope.timeline = new Array;
        var localTime = localStorageService.get("Timeline");
        if (!localTime[Database.getName()][Database.currentUser()]) {
            localTime[Database.getName()][Database.currentUser()] = new Array;
        }
        localTime[Database.getName()][Database.currentUser()] = $scope.timeline;
        localStorageService.add("Timeline", localTime);

    }
    $scope.tm = $scope.timeline;
    Database.setWiki("Query.html");

    $scope.language = 'sql';

    $scope.table = true;
    $scope.contentType = ['JSON', 'CSV'];
    $scope.selectedContentType = $scope.contentType[0];
    $scope.editorOptions = {
        lineWrapping: true,
        lineNumbers: true,
        readOnly: false,
//        theme: 'ambiance',
        mode: 'text/x-sql',
        metadata: Database,
        extraKeys: {
            "Ctrl-Enter": function (instance) {
                $scope.$apply(function () {
                    if ($scope.queryText)
                        $scope.query();
                });

            },
            "Ctrl-Space": "autocomplete",
            'Cmd-/': 'toggleComment',
            'Ctrl-/': 'toggleComment'


        },
        onLoad: function (_cm) {
            $scope.cm = _cm;
            if ($routeParams.query) {
                $scope.queryText = $routeParams.query;
                //$scope.cm.setValue($scope.queryText);

                $scope.query();
            }
            $scope.cm.on("change", function () { /* script */
                var wrap = $scope.cm.getWrapperElement();
                var approp = $scope.cm.getScrollInfo().height > 300 ? "300px" : "auto";
                if (wrap.style.height != approp) {
                    wrap.style.height = approp;
                    $scope.cm.refresh();
                }
            });

        }
    };

    $scope.query = function (explain) {

        var queryBuffer = "" + $scope.queryText;
        var selection = $scope.cm.getSelection();
        if (selection && selection != "") {
            queryBuffer = "" + selection;
        }

        queryBuffer = queryBuffer.trim();
        queryBuffer = queryBuffer.replace(/\n/g, " ");
        queryBuffer = queryBuffer.replace(/\t/g, " ");
        queryBuffer = queryBuffer.replace(/(\/\*([\s\S]*?)\*\/)|(\/\/(.*)$)/gm, '');
        if (queryBuffer.length != 0) {
            Spinner.start(function () {
                CommandApi.interrupt(Database.getName(), queryBuffer).then(function () {
                    Spinner.stop();
                });
            });
        }


        if (queryBuffer.startsWith('g.')) {
            $scope.language = 'gremlin';
        } else {
            $scope.language = 'sql';
        }
        if (queryBuffer.startsWith('#')) {
            $location.path('/database/' + $routeParams.database + '/browse/edit/' + queryBuffer.replace('#', ''));
            return;
        }

        var conttype;
        if ($scope.selectedContentType == 'CSV')
            conttype = 'text/csv';

        if (explain) {
            queryBuffer = "explain " + queryBuffer;
        }
        if($scope.search)
          delete $scope.search.query ;
        CommandApi.queryText({database: $routeParams.database, contentType: conttype, language: $scope.language, text: queryBuffer, limit: $scope.limit, shallow: $scope.shallow, verbose: false}, function (data) {

            if (data.result) {

                var item = new Object;
                item.query = $scope.queryText;
                item.executedQuery = queryBuffer;
                if (selection && selection != "") {
                    item.query = selection;
                }

                item.language = $scope.language;
                $scope.headers = Database.getPropertyTableFromResults(data.result);
                if ($scope.headers.length == 0) {
                    $scope.alerts = new Array;
                    $scope.alerts.push({content: "No records found."});
                }

                $scope.rawData = JSON.stringify(data);
                $scope.resultTotal = data.result;
                $scope.results = data.result.slice(0, $scope.countPage);
                $scope.currentPage = 1;
                $scope.numberOfPage = new Array(Math.ceil(data.result.length / $scope.countPage));
                item.subHeaders = { "a": {"name": "METADATA", span: 0}, "b": {"name": "PROPERTIES", span: 0}, "c": {"name": "IN", span: 0}, "d": {"name": "OUT", span: 0}};
                $scope.headers.forEach(function (n) {
                    if (n.startsWith("in_")) {
                        item.subHeaders["c"].span++;
                    } else if (n.startsWith("out_")) {
                        item.subHeaders["d"].span++;
                    } else if (n.startsWith('@')) {
                        item.subHeaders["a"].span++;
                    } else {
                        item.subHeaders["b"].span++;
                    }
                });
                $scope.headers.sort(function (a, b) {
                    var aI = a.startsWith("in_") ? 2 : (a.startsWith("out_") ? 3 : ((a.startsWith("@") ? 0 : 1)));
                    var bI = b.startsWith("in_") ? 2 : (b.startsWith("out_") ? 3 : ((b.startsWith("@") ? 0 : 1)));
                    return aI - bI;
                });
                item.headers = $scope.headers;

                item.rawData = $scope.rawData;
                item.resultTotal = $scope.resultTotal;
                item.results = $scope.results;
                item.currentPage = $scope.currentPage;
                item.numberOfPage = $scope.numberOfPage;
                item.countPage = 5;
                item.countPageOptions = [5, 10, 20, 50, 100, 500, 1000, 2000, 5000];
                item.notification = data.notification;
                $scope.timeline.unshift(item);
                if ($scope.timeline.length > $scope.keepLimit) {
                    $scope.timeline.splice($scope.timeline.length - 1, 1);
                }
                var dbTime = localStorageService.get("Timeline");
                dbTime[Database.getName()][[Database.currentUser()]] = $scope.timeline;
                localStorageService.add("Timeline", dbTime);

                Spinner.stopSpinner();
                $scope.context = $scope.items[0];
                $scope.nContext = $scope.items[1];
                Notification.clear();
            } else {
                Notification.push({content: "The command has been executed", autoHide: true});
            }
            Spinner.stopSpinner();
        }, function (data) {
            Spinner.stopSpinner();
            $scope.headers = undefined;
            $scope.resultTotal = undefined;
            $scope.results = undefined;
            if (!data) {
                data = "The command has not been executed"
            }
            Notification.push({content: data, error: true, autoHide: true});
        });


    }

    $scope.clear = function () {

        Utilities.confirm($scope, $modal, $q, {
            title: 'Warning!',
            body: 'You are clearing history. Are you sure?',
            success: function () {
                $scope.timeline = new Array;
                var dbTime = localStorageService.get("Timeline");
                dbTime[Database.getName()][Database.currentUser()] = $scope.timeline;
                localStorageService.add("Timeline", dbTime);
            }
        });

    }

    $scope.removeItem = function (item) {
        var idx = $scope.timeline.indexOf(item);
        $scope.timeline.splice(idx, 1);
        var dbTime = localStorageService.get("Timeline");
        dbTime[Database.getName()][Database.currentUser()] = $scope.timeline;
        localStorageService.add("Timeline", dbTime);
    }


    $scope.$watch("limit", function (data) {
        $scope.limit = data;
    });
    $scope.$watch("selectedContentType", function (data) {
        $scope.selectedContentType = data;
    });
    $scope.$watch("shallow", function (data) {
        $scope.shallow = data;
        localStorageService.add("shallowCollection", data);
    });
    $scope.$watch("keepLimit", function (data) {
        $scope.keepLimit = data;
        localStorageService.add("keepLimit", data);
    });
    $scope.$watch("hideSettings", function (data) {
        $scope.hideSettings = data;
        localStorageService.add("hideSettings", data);
    });


}]);
dbModule.controller("QueryController", ['$scope', '$routeParams', '$filter', '$location', 'Database', 'CommandApi', 'localStorageService', 'Spinner', 'ngTableParams', 'scroller', '$ojson', 'Graph', function ($scope, $routeParams, $filter, $location, Database, CommandApi, localStorageService, Spinner, ngTableParams, scroller, $ojson, Graph) {


    var data = $scope.item.resultTotal;

    if ($scope.item.rawData instanceof Object) {
        $scope.item.rawData = JSON.stringify($scope.item.rawData);
    }

    $scope.indexes = []
    var total = 0;
    Object.keys($scope.item.subHeaders).forEach(function (e) {
        total += $scope.item.subHeaders[e].span;
        $scope.indexes.push(total);
    });

    $scope.current = 'table';
    $scope.bookIcon = 'fa fa-star';
    $scope.viewerOptions = {
        lineWrapping: true,
        lineNumbers: true,
        readOnly: true,
        mode: 'javascript',
        onLoad: function (_cm) {
            $scope.vcm = _cm;
            //$scope.vcm.setValue($ojson.format($scope.item.rawData));
        }

    };

    $scope.graphOptions = {
        data: data,
        config: {
            height: 300,
            width: 1200,
            classes: {
                "Dir": {
                    style: '',
                    css: 'vertex-dir',
                    display: "inode"
                }
            },
            node: {
                r: 50
            }

        }
    }
    $scope.sendToGraph = function () {
        Graph.add($scope.item.resultTotal);
        Graph.query = $scope.item.query;
        $location.path("/database/" + $scope.database.getName() + "/graph");
    }
    $scope.changeIcon = function () {
        $scope.bookIcon = 'icon-star';
    }
    $scope.sort = function (header) {
        var order = $scope.tableParams.isSortBy(header, 'asc') ? 'desc' : 'asc';
        var obj = {};
        obj[header] = order;
        $scope.tableParams.sorting(obj);
    }
    $scope.tableParams = new ngTableParams({
        page: 1,            // show first page
        count: 10          // count per page

    }, {
        total: data.length, // length of data
        getData: function ($defer, params) {
//            use build-in angular filter
            var emtpy = !params.orderBy() || params.orderBy().length == 0;
            var orderedData = (params.sorting() && !emtpy) ?
                $filter('orderBy')(data, params.orderBy()) :
                data;
            $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
        }
    });

    $scope.tableParams.settings().counts = [10, 25, 50, 100, 1000, 5000];
    $scope.switchPage = function (index) {
        if (index != $scope.item.currentPage) {
            $scope.item.currentPage = index;
            $scope.item.results = $scope.item.resultTotal.slice(
                (index - 1) * $scope.item.countPage,
                index * $scope.item.countPage
            );
        }
    }
    $scope.isDivider = function (index, header) {

        var sort = $scope.tableParams.isSortBy(header, 'asc') ? 'sort-asc' : ($scope.tableParams.isSortBy(header, 'desc') ? 'sort-desc' : '');
        return $scope.indexes.indexOf(index) != -1 ? 'header-divider ' + sort : sort;
    }
    $scope.previous = function () {
        if ($scope.item.currentPage > 1) {
            $scope.switchPage($scope.item.currentPage - 1);
        }
    }
    $scope.next = function () {

        if ($scope.item.currentPage < $scope.item.numberOfPage.length) {
            $scope.switchPage($scope.item.currentPage + 1);
        }
    }
    $scope.$watch("item.countPage", function (data) {
        if ($scope.item.resultTotal) {
            $scope.item.results = $scope.item.resultTotal.slice(0, $scope.item.countPage);
            $scope.item.currentPage = 1;
            $scope.item.numberOfPage = new Array(Math.ceil($scope.item.resultTotal.length / $scope.item.countPage));
        }
    });
    $scope.openRecord = function (doc) {
        $location.path("/database/" + $scope.database.getName() + "/browse/edit/" + doc["@rid"].replace('#', ''));
    }
    $scope.changeQuery = function () {
        $scope.queryText = $scope.item.query;
        scroller.scrollTo(0, 0, 2000);
        $scope.cm.focus();

        $scope.cm.setValue($scope.queryText);
        $scope.cm.setCursor($scope.cm.lineCount());

    }


}]);
dbModule.controller("QueryConfigController", ['$scope', '$routeParams', 'localStorageService', function ($scope, $routeParams, localStorageService) {


    $scope.$watch("limit", function (data) {
        $scope.$parent.$parent.$parent.limit = data;
    });
    $scope.$watch("selectedContentType", function (data) {
        $scope.$parent.$parent.$parent.selectedContentType = data;
    });
    $scope.$watch("shallow", function (data) {
        $scope.$parent.$parent.$parent.shallow = data;
        localStorageService.add("shallowCollection", data);
    });
    $scope.$watch("keepLimit", function (data) {
        $scope.$parent.$parent.$parent.keepLimit = data;
        localStorageService.add("keepLimit", data);
    });
    $scope.$watch("hideSettings", function (data) {
        $scope.$parent.$parent.$parent.hideSettings = data;
        localStorageService.add("hideSettings", data);

        if (!data && $scope.$parent.$hide) {
            $scope.$parent.$hide();
        }
    });
    $scope.$parent.$watch("limit", function (data) {
        $scope.limit = data;
    });
    $scope.$parent.$watch("selectedContentType", function (data) {
        $scope.selectedContentType = data;
    });
    $scope.$parent.$watch("shallow", function (data) {
        $scope.shallow = data;
    });
    $scope.$parent.$watch("keepLimit", function (data) {
        $scope.keepLimit = data;
    });
    $scope.$parent.$watch("hideSettings", function (data) {
        $scope.hideSettings = data;
    });


}]);
dbModule.controller("BookmarkNewController", ['$scope', '$rootScope', 'Bookmarks', 'DocumentApi', 'Database', function ($scope, $rootScope, Bookmarks, DocumentApi, Database) {


    $scope.bookmark = DocumentApi.createNewDoc(Bookmarks.CLAZZ);

    $scope.bookmark.name = $scope.item.query;
    $scope.bookmark.query = $scope.item.query;

    Bookmarks.getTags(Database.getName()).then(function (data) {
        $scope.tags = data;
        $scope.select2Options = {
            'multiple': true,
            'simple_tags': true,
            'tags': $scope.tags  // Can be empty list.
        };
        $scope.viewTags = true;
    });

    $scope.viewTags = false;
    $scope.addBookmark = function () {
        Bookmarks.addBookmark(Database.getName(), $scope.bookmark).then(function () {
            $rootScope.$broadcast('bookmarks:changed');
            $scope.$hide();
        });
    }
}]);
dbModule.controller("BookmarkEditController", ['$scope', '$rootScope', 'Bookmarks', 'DocumentApi', 'Database', function ($scope, $rootScope, Bookmarks, DocumentApi, Database) {

    $scope.bookmark = $scope.bk;


    Bookmarks.getTags(Database.getName()).then(function (data) {
        $scope.tags = data;
        $scope.select2Options = {
            'multiple': true,
            'simple_tags': true,
            'tags': $scope.tags  // Can be empty list.
        };
        $scope.viewTags = true;
    });

    $scope.viewTags = false;
    $scope.addBookmark = function () {
        Bookmarks.update(Database.getName(), $scope.bookmark).then(function () {
            $rootScope.$broadcast('bookmarks:changed');
            $scope.$hide();
        });
    }
}]);
dbModule.controller("BookmarkController", ['$scope', 'Bookmarks', 'DocumentApi', 'Database', 'scroller', 'Aside', function ($scope, Bookmarks, DocumentApi, Database, scroller, Aside) {


//    $(document).bind("keydown", function (e) {
//
//        if ($scope.$parent.bookmarksClass == "show") {
//            $scope.$apply(function () {
//                $scope.closeIfReturn(e);
//            });
//        }
//    });

    $scope.closeIfReturn = function (e) {
        if (e.keyCode == '27') {
            $scope.click();
        }
    }
    $scope.$on("bookmarks:changed", function (event) {
        Bookmarks.getAll(Database.getName()).then(function (data) {
            $scope.bks = data.result;

        });
    });

    Bookmarks.getAll(Database.getName()).then(function (data) {
        $scope.bks = data.result
    });

    $scope.isSelected = function (bk) {
        return $scope.selected == bk ? '' : 'hide';
    }
    $scope.hover = function (bk) {
        $scope.selected = bk;
    }
    $scope.run = function (r) {
        $scope.queryText = r.query;
        scroller.scrollTo(0, 0, 2000);
        $scope.cm.focus();

        $scope.cm.setValue($scope.queryText);
        $scope.cm.setCursor($scope.cm.lineCount());
        Aside.toggle();
    }

    $scope.stopProps = function ($event) {
        if ($event) {
            $event.stopPropagation();
        }
    }
    $scope.remove = function (r, $event) {
        if ($event) {
            $event.stopPropagation();
        }
        Bookmarks.remove(Database.getName(), r).then(function (data) {
            var idx = $scope.bks.indexOf(r);
            $scope.bks.splice(idx, 1);
        });

    }
    $scope.update = function (r) {
        Bookmarks.update(Database.getName(), r).then(function (data) {
            var idx = $scope.bks.indexOf(r);
            $scope.bks.splice(idx, 1, data);
        });
    }
}]);



