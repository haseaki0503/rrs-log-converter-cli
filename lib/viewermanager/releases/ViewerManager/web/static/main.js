
(function() {

    let viewers = {};
    let providers = {};


	function format(fmt, a) {
		var rep_fn = undefined;
		if (typeof a == "object") {
			rep_fn = function(m, k) { return a[ k ]; }
		}
		else {
			var args = arguments;
			rep_fn = function(m, k) { return args[ parseInt(k)+1 ]; }
		}
		return fmt.replace( /\{\{(\w+)\}\}/g, rep_fn);
	}


    // send request
    function request(method, uri, param, callback) {
        $.ajax({
            type: method,
            url: uri,
            async: true,
            data: param,
            dataType: 'json',
            error: function(request, textStatus, error) {
                console.log("Ajax Failed " + error);
                $('#alert-error').text("[ERROR]: " + error);
                $('#alert-box').alert();
                $('#alert-box').addClass('show');
                $('#alert-holder').removeClass('hidden');
            },
            success: function (data, dataType) {
                callback(data);
            },
        });
    };


    // Get Error List and update elements
    let list_context = {
        method: "GET",
        uri: null,
        data: null,
        listId: null,
        format: function (data) {
            let items = [];
            $.each(data, function (k, v){
                let level = (v.level in error_class) ? error_class[v.level] : error_class.DEFAULT;
                let label = "<span class='" + level + "'>" + v.level + "</span>";
                let message = v.message;
                let inner = format(error_format.template, {level: level, label: label, message: message});

                let entity = $(inner);
                items.unshift(entity);
            });
            return items;
        },
    };
    function updateList (context) {
        let con = Object.assign (list_context, context);
        request (context.method, context.uri, context.data, function (data) {
            let list = $(context.listId);
            let entities = context.format(data);
            list.empty();
            let tb = $("<tbody></tbody>");
            tb.append(entities);
            list.append(tb);
        });
    }

    let error_class = {
        DEBUG: 'bg-faded',
        WARNING: 'table-warning',
        ERROR: 'table-error',
        DEFAULT: 'table-secondary'
    };
    function updateErrors () {
        updateList({
            uri: '/server/errors',
            listId: '#errors',
            format: function (data) {
                let template ="<tr class='{{level}}'><td>{{label}}</td>" +
                    "<td align='left'>{{message}}</td></tr>";
                let items = [];

                console.log(data);
                if ('data' in data && 'list' in data.data) {
                    $.each(data.data.list, function (k, v){
                        let info = JSON.parse(v);
                        let level = (info.level in error_class)
                            ? error_class[info.level] : error_class.DEFAULT;
                        let label = "<span class='" + level + "'>"
                            + info.level + "</span>"
                            let message = info.message;
                        let inner = format(template,
                                {level: level, label: label, message: message});

                        let entity = $(inner);
                        entity.on('click', function (e) { console.log(e);});
                        items.unshift(entity);
                    });
                }
                return items;
            }
        });
    };

    // Update Log List
    function updateLogs () {
        updateList({
            uri: '/provider/log/list',
            listId: '#logs',
            format: function(data) {
                let items = [];

                if ('data' in data && 'list' in data.data) {
                    $.each (data.data.list, function (k, v) {
                        let entity = $(format("<tr item-group='log'><td align='left'>{{file}}</td></tr>", {file: v}));
                        entity.on("click", function(elm) {
                            listControl($(this));
                        });
                        items.unshift(entity);
                    });
                }
                else {
                    console.log("list not found");
                }
                return items;
            }
        });
    }

    // Update Lists
    let update = {
        formatElementWith: function (value, fmt) {
            let title = format(fmt.title, value);
            let elm = $(format(fmt.element, {title: title, content: JSON.stringify(value)}));
            elm.popover(fmt.popover);
            elm.on('click', fmt.onClick);

            value['element'] = elm;
            fmt.list[value.id] = value;
            return elm;
        },

        // Viewer Format and function
        viewer_format: {
            title: "Viewer-{{viewerId}}",
            element: "<a href='#' class='list-group-item list-group-item-action justify-content-between' item-group='viewer'>{{title}}" + 
//                 " <button class='close'>&#x1f51a;</button>" +
                "<span class='info' hidden='true'>{{content}}</span></a>",
            list: {},
            popover: {
                html: true,
                trigger: 'hover',
                title: "INFO",
                template: '<div class="popover" role="tooltip" style="max-width: 50%;"><div class="popover-arrow"></div>' +
                    '<h3 class="popover-title"></h3><table class="list-group list">' +
                    '<div class="popover-content"></div></ul></div>',
                placement: "bottom",
                container: 'body',
                content: function() {
                    return content_from_list($(this));
                }
            },
            onClick: function(elm) {
                listControl($(this));
            }
        },
        viewer: function () {
            request("GET", "/viewer/list", "", function(data) {
                let list = $('#viewer-list');
                list.empty();
                viewers = {};

                if ('data' in data && 'list' in data.data) {
                    // For each Viewer
                    $.each (data.data.list, function (k, v) {
                        let element = update.formatElementWith(v, update.viewer_format);
                        list.append(element);
                    });
                }
                else {
                    console.log("list not found");
                }
                viewers = update.viewer_format.list;
            });
        },

        // Update Providers List
        provider_format: {
            title: "Provider-{{providerId}}",
            element: "<a href='#' class='list-group-item list-group-item-action' item-group='provider'>{{title}}" + 
                "<span class='info' hidden='true'>{{content}}</span></a>",
            list: {},
            popover: {
                html: true,
                trigger: 'hover',
                title: "INFO",
                template: '<div class="popover" role="tooltip" style="max-width: 50%;"><div class="popover-arrow"></div>' +
                    '<h3 class="popover-title"></h3><ul class="list-group list">' +
                    '<div class="popover-content"></div></ul></div>',
                placement: "bottom",
                content: function() {
                    return content_from_list($(this));
                }
            },
            onClick: function() {
                listControl($(this));
            }
        },
        provider: function (data) {
            request("GET", "/provider/list", "", function(data) {
                let list = $('#provider-list');
                list.empty();
                providers = {};

                if (('data' in data) && ('list' in data.data)) {
                    // For each Provider
                    $.each (data.data.list, function (k, v) {
                        let element = update.formatElementWith(v, update.provider_format);
                        list.append(element);
                    });
                }
                providers = update.provider_format.list;
            });
        }
    };

    function updateInformation() {
        update.viewer();
        update.provider();
        updateErrors();
    }


    let active = {
        provider: null,
        viewer: null,
        log: null,
        mode: null
    };

    let procedure_mode = {
        CONNECT: "CONNECT",
        DISCONNECT: "DISCONNECT",
        OPEN: "OPEN",
        OPENLOG: "OPENLOG",
        CLOSE: "CLOSE",
    };
    let procedure = {
        mode: null,
        callback: function (){},
    };


    // Open Viewer Control
    function listControl (element) {
        // Remove Current Selection
        if (!element) {
            return;
        }
        
        let type = element.attr('item-group');
        with (procedure) {
            if (!(mode in procedure_mode)) {
                return;
            }
            
            let item = element;
            // value check
            if (type == 'viewer') {
                $('#viewer-item').html(item.html());
                $('#viewer-item').removeClass(controlHighlight['#viewer-item']);
                $('#viewer-header').removeClass(controlHighlight['#viewer-header']);
            }
            else if (type == 'provider') {
                $('#provider-item').html(item.html());
                $('#provider-item').removeClass(controlHighlight['#provider-item']);
                $('#provider-header').removeClass(controlHighlight['#provider-header']);
            }
            else if (type == 'log') {
                let uri = item.contents().eq(0).text();
                $('#log-item').html(uri);
                $('#log-item').removeClass(controlHighlight['#log-item']);
                $('#tab-log').removeClass(controlHighlight['#tab-log']);
            }

            let viewer = !$('#viewer-item').text();
            let provider = !$('#provider-item').text();
            let log = !$('#log-item').text();

            console.log(mode, viewer, provider, log);
            // Check fo result
            if (mode == procedure_mode.CONNECT && (!viewer && !provider))
            {
                $('#btn-request').removeClass('disabled');
            }
            else if (mode == procedure_mode.DISCONNECT && !viewer)
            {
                $('#btn-request').removeClass('disabled');
            }
            else if (mode == procedure_mode.OPENLOG && !log)
            {
                $('#btn-request').removeClass('disabled');
            }
            else if (mode == procedure_mode.CLOSE && !provider) {
                $('#btn-request').removeClass('disabled');
            }
        }
    }

    function controlOpenInput() {
        let request = $('#btn-request');
        let hostName = $('#open-host').val();
        let portNum = $('#open-port').val();
        if (!hostName) {;
            // HostName empty, deactivate
            request.addClass('disabled');
        }
        else {
            // HostName given
            if ($('#open-port-holder').hasClass('show')) {
                // Port given,  is the port empty?
                if (!portNum) {
                    // Empty, deactivate
                    request.addClass('disabled');
                }
                else {
                    // Activate
                    request.removeClass('disabled');
                }
            }
            else {
                // Port not given, activate
                request.removeClass('disabled');
            }
        }
    }

    function controlRequest() {
        $('#btn-request').addClass('disabled');

        let vtex = $('#viewer-item').children('.info').text();
        let ptex = $('#provider-item').children('.info').text();
        vtex = (vtex) ? vtex : '{}';
        ptex = (ptex) ? ptex : '{}';
        let vi = JSON.parse(vtex);
        let pi = JSON.parse(ptex);
        console.log(vtex, ptex, vi, pi);

        if (procedure.mode == procedure_mode.CONNECT) {
            let params = {viewerId: vi.viewerId, providerId: pi.providerId};
            console.log(params);
            request("POST", "/viewer/connect", params, function() {
                // TODO SUCCESS
                updateInformation();
            });
        }
        else if (procedure.mode == procedure_mode.DISCONNECT) {
            let params = {viewerId: vi.viewerId};
            request("POST", "/viewer/disconnect", params, function(data) {
                // TODO: SUCCESS
                console.log(data);
                if (data.providerId) {
                    console.log("Viewer-" + data.viewerId
                            + " disconnected from Provider-" + data.providerId);
                }
                updateInformation();
            });
        }
        else if (procedure.mode == procedure_mode.OPEN) {
            let host = $('#open-host').val();
            let port = ($('#open-port-holder').hasClass('show')) ? $('#open-port').val() : "7000";
            let params = {host: host, port: port};
            request("POST", "/provider/open", params, function(data) {
                // TODO: SUCCESS
            });
        }
        else if (procedure.mode == procedure_mode.OPENLOG) {
            let logName = $('#log-item').text();
            console.log(logName);
            let params = {path: logName};
            request("POST", "/provider/log/open", params, function(data) {
                // TODO: SUCCESS
            });
        }
        else if (procedure.mode == procedure_mode.CLOSE) {
            let params = {providerId: pi.providerId};
            request("POST", "/provider/close", params, function(data) {
                // TODO: SUCCESS
            });
        }
    }

    let controlHighlight = {
        '#viewer-item': 'bg-success',
        '#provider-item': 'bg-info',
        '#log-item': 'bg-warning',

        '#viewer-header': 'bg-success',
        '#provider-header': 'bg-info',
        '#tab-log': 'bg-warning'
    };

    function clearHighlight () {
        for (let k in controlHighlight) {
            $(k).removeClass(controlHighlight[k]);
        }

        $('#viewer-item').empty();
        $('#provider-item').empty();
        $('#log-item').empty();

        $('#btn-request').addClass('disabled');
        procedure.mode = "";
    }

    // Initial Time Functions
    $(document).ready(function() {
        // Update Button
        $('#btn-update').on('click', function() {
            // Update Viewer and Provider Lists
            updateInformation();
        });

        // Error Popover Wake up
        $('#tgl-error').popover(error_pop);
        $('#tgl-error').on('hide.bs.popover', function() {
            updateErrors();
        });

        // Provider Addition Button
        $('#btn-provider-add').popover(prov_pop);

       
        // Error Tab
        $('#tab-error').on('click', function() {
            updateErrors();
        });
        updateErrors();

        // Controller
        $('#control-label').on(window.vcol.event.shown, function (evt, data) {
            let mode = $(data.source).attr('ctrl')
            $('#btn-label').html(mode);
            procedure.mode = mode;
        });

        $('#control-label').on(window.vcol.event.hidden, function () {
            $('[vcol-group="ctrl-input"]').removeClass('show');
        });

        $('#controller').on(window.vcol.event.shown, function () {
            clearHighlight();
        });

        $('#ctrl-viewer').on(window.vcol.event.shown, function() {
            $('#viewer-item').addClass(controlHighlight['#viewer-item']);
            $('#viewer-header').addClass(controlHighlight['#viewer-header']);
        });

        $('#ctrl-provider').on(window.vcol.event.shown, function() {
            $('#provider-item').addClass(controlHighlight['#provider-item']);
            $('#provider-header').addClass(controlHighlight['#provider-header']);
        });

        // Log Control
        $('#ctrl-openlog').on(window.vcol.event.shown, function() {
            $('#log-item').addClass(controlHighlight['#log-item']);
            $('#tab-log').addClass(controlHighlight['#tab-log']);
        });

        // Open (Host/Port) Control
        $('#open-host').on('input', controlOpenInput);
        $('#open-port').on('input', controlOpenInput);

        $('#open-port-holder').on(window.vcol.event.shown, function(evt, data) {
            console.log($('#open-port-holder').hasClass('show'));
            console.log($(data.source).text('PORT'));
            controlOpenInput();
        });
        $('#open-port-holder').on(window.vcol.event.hide, function(evt, data) {
            console.log($('#open-port-holder').hasClass('show'));
            console.log($(data.source).text(':7000'));
            controlOpenInput();
        });

        $('#btn-request').on('click', function () {
            controlRequest();
        });

        // Log List
        $('#tab-log').on('click', function() {
            updateLogs();
        });

        // Get Vieweres and Providers
        update.viewer();
        update.provider();
    });


    // Error Popover Settings
    let error_pop = {
        html: true,
        trigger: 'click',
        title: "ERRORS",
        template: '<div class="popover" role="tooltip" style="max-width: 35%;"><div class="popover-arrow"></div>' +
            '<h3 class="popover-title"></h3><ul class="list-group list">' +
            '<div class="popover-content"></div></ul></div>',
        placement: "top",
        content: function() {
            return $('#errors').html();
        }
    };

    // Provider Popover Settings
    let prov_pop = {
        html: true,
        trigger: 'click',
        title: "ADD PROVIDER",
        template: '<div class="popover" role="tooltip" style="max-width: auto;"><div class="popover-arrow"></div>' +
            '<h3 class="popover-title"></h3><div class="popover-content"></div></div>',
        placement: "bottom",
        content: '<table><tr><td>Host:</td><td><input type="text" class="form-control" placeholder="127.0.0.1"></input></tr>' +
            "<tr><td>PORT:</td><td><input type='text' class='form-control' placeholder='7000'></input></tr></table>" +
            "<div class='justify-content-between'><p class='d-inline '>Or Open Log... </p><button class='btn btn-sm btn-outline-primary'>OPEN LOG</button></div>" +
            "<div class='d-flex flex-row-reverse'><button class='btn btn-error'>CANCEL</button><button class='btn btn-primary'>OPEN</button></div>"
    };


    let content_from_list = function(element) {
        let info = element.children('.info');
        let res = '';
        if (info) {
            let tr = "<tr><td>{{key}}</td><td>{{value}}</td></tr>";
            let data = JSON.parse(info.text());
            let trs = [];
            for (let k in data) {
                trs.push(format(tr, {key: k, value: data[k]}));
            }
            res = "<table>" + trs.join("") + "</table>";
        }
        return res;
    }

}).call(this);








