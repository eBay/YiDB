// global UUID generator
function guid() {
    function _p8(s) {
        var p = (Math.random().toString(16)+"000000000").substr(2,8);
        return s ? "-" + p.substr(0,4) + "-" + p.substr(4,4) : p ;
    }
    return _p8() + _p8(true) + _p8(true) + _p8();
}
$(function () {
    // Helper function for vertically aligning DOM elements
    // http://www.seodenver.com/simple-vertical-align-plugin-for-jquery/
    $.fn.vAlign = function () {
        return this.each(function (i) {
            var ah = $(this).height();
            var ph = $(this).parent().height();
            var mh = (ph - ah) / 2;
            $(this).css('margin-top', mh);
        });
    };
    $.fn.stretchFormtasticInputWidthToParent = function () {
        return this.each(function (i) {
            var p_width = $(this).closest("form").innerWidth();
            var p_padding = parseInt($(this).closest("form").css('padding-left'), 10) + parseInt($(this).closest("form").css('padding-right'), 10);
            var this_padding = parseInt($(this).css('padding-left'), 10) + parseInt($(this).css('padding-right'), 10);
            $(this).css('width', p_width - p_padding - this_padding);
        });
    };
    $('form.formtastic li.string input, form.formtastic textarea').stretchFormtasticInputWidthToParent();
    // Vertically center these paragraphs
    // Parent may need a min-height for this to work..
    $('ul.downplayed li div.content p').vAlign();
    // When a sandbox form is submitted..
    $("form.sandbox").submit(function () {
        var error_free = true;
        // Cycle through the forms required inputs
        $(this).find("input.required").each(function () {
            // Remove any existing error styles from the input
            $(this).removeClass('error');
            // Tack the error style on if the input is empty..
            if ($(this).val() == '') {
                $(this).addClass('error');
                $(this).wiggle();
                error_free = false;
            }
        });
        return error_free;
    });
});

// Logging function that accounts for browsers that don't have window.console
function log() {
    if (window.console) console.log.apply(console, arguments);
}
var Docs = {
    shebang: function () {
        // If shebang has an operation nickname in it..
        // e.g. /docs/#!/words/get_search
        var fragments = $.param.fragment().split('/');
        fragments.shift(); // get rid of the bang
        switch (fragments.length) {
        case 1:
            // Expand all operations for the resource and scroll to it
            // log('shebang resource:' + fragments[0]);
            var dom_id = 'resource_' + fragments[0];
            Docs.expandEndpointListForResource(fragments[0]);
            $("#" + dom_id).slideto({
                highlight: false
            });
            break;
        case 2:
            // Refer to the endpoint DOM element, e.g. #words_get_search
            // log('shebang endpoint: ' + fragments.join('_'));
            // Expand Resource
            Docs.expandEndpointListForResource(fragments[0]);
            $("#" + dom_id).slideto({
                highlight: false
            });
            // Expand operation
            var li_dom_id = fragments.join('_');
            var li_content_dom_id = li_dom_id + "_content";
            // log("li_dom_id " + li_dom_id);
            // log("li_content_dom_id " + li_content_dom_id);
            Docs.expandOperation($('#' + li_content_dom_id));
            $('#' + li_dom_id).slideto({
                highlight: false
            });
            break;
        }
    },
    toggleEndpointListForResource: function (resource) {
        var elem = $('li#resource_' + resource + ' ul.endpoints');
        if (elem.is(':visible')) {
            Docs.collapseEndpointListForResource(resource);
        } else {
            Docs.expandEndpointListForResource(resource);
        }
    },
    // Expand resource
    expandEndpointListForResource: function (resource) {
        $('#resource_' + resource).addClass('active');
        var elem = $('li#resource_' + resource + ' ul.endpoints');
        elem.slideDown();
    },
    // Collapse resource and mark as explicitly closed
    collapseEndpointListForResource: function (resource) {
        $('#resource_' + resource).removeClass('active');
        var elem = $('li#resource_' + resource + ' ul.endpoints');
        elem.slideUp();
    },
    expandOperationsForResource: function (resource) {
        // Make sure the resource container is open..
        Docs.expandEndpointListForResource(resource);
        $('li#resource_' + resource + ' li.operation div.content').each(function () {
            Docs.expandOperation($(this));
        });
    },
    collapseOperationsForResource: function (resource) {
        // Make sure the resource container is open..
        Docs.expandEndpointListForResource(resource);
        $('li#resource_' + resource + ' li.operation div.content').each(function () {
            Docs.collapseOperation($(this));
        });
    },
    expandOperation: function (elem) {
        elem.slideDown();
    },
    collapseOperation: function (elem) {
        elem.slideUp();
    }
};

//JSONFormatter convert JSON to Html nested list
function JSONFormatter() { }
JSONFormatter.prototype = {
    objectRef: "ref",
    hasRef:false,
    htmlEncode: function (t) {
        return t != null ? t.toString().replace(/&/g, "&amp;").replace(/"/g, "&quot;").replace(/</g, "&lt;").replace(/>/g, "&gt;") : '';
    },
    jsString: function (s) {
        var has = {
            '\b': 'b',
            '\f': 'f',
            '\r': 'r',
            '\n': 'n',
            '\t': 't'
        }, ws;
        for (ws in has) {
            if (-1 === s.indexOf(ws)) {
                delete has[ws];
            }
        }
        s = JSON.stringify({
            a: s
        });
        s = s.slice(6, - 2);
        for (ws in has) {
            s = s.replace(new RegExp('\\\\u000' + (ws.charCodeAt().toString(16)), 'ig'), '\\' + has[ws]);
        }
        return this.htmlEncode(s);
    },
    decorateWithSpan: function (value, className) {
        return '<span class="' + className + '">' + this.htmlEncode(value) + '</span>';
    },
    valueToHTML: function (value) {
        var valueType = typeof value;
        var output = "";
        if (value == null) {
            output += this.decorateWithSpan('null', 'null');
        } else if (value && value.constructor == Array) {
            output += this.arrayToHTML(value);
        } else if (valueType == 'object') {
            output += this.objectToHTML(value);
        } else if (valueType == 'number') {
            output += this.decorateWithSpan(value, 'num');
        } else if (valueType == 'string') {
            if (/^(http|https):\/\/[^\s]+$/i.test(value)) {
            	var refId = guid();
                output += '<a href="javascript:;" onClick="loadChild(\'ref_'+ refId +'\');" id="ref_'+ refId +'"><span class="string">"' + this.jsString(value) + '"</span></a>';
            } else if (this.hasRef) {
            	var refId = guid();
                output += '<a href="javascript:;" onClick="loadChild(\'ref_'+ refId +'\');" id="ref_'+ refId +'">' + this.jsString(value) + '</a>';
            } else {
                output += '<span class="string">"' + this.jsString(value) + '"</span>';
            }
        } else if (valueType == 'boolean') {
            output += this.decorateWithSpan(value, 'bool');
        }
        return output;
    },
    arrayToHTML: function (json) {
        var hasContents = false;
        var output = '';
        var numProps = 0;
        //for ref case
        if (this.hasRef){
            output += '<li>[<ul class="array collapsible">'
            for (var prop in json) {
                if (typeof json[prop] == 'object'){
                	var refId = guid();
                    output += '<li><a href="javascript:;" onClick="loadChild(\'ref_'+ refId +'\');" id="ref_'+ refId +'" alt="' + json[prop]["url"] + '">' + json[prop]["id"] + '</a></li>';
                }
            }
            output += '</ul>]</li>';
            this.hasRef = false;
            return output;
        }

        //ref case end
        for (var prop in json) {
            numProps++;
        }
        for (var prop in json) {
            hasContents = true;
            output += '<li>' + this.valueToHTML(json[prop]);
            if (numProps > 1) {
                output += ',';
            }
            output += '</li>';
            numProps--;
        }
        if (hasContents) {
            output = '[<ul class="array collapsible">' + output + '</ul>]';
        } else {
            output = '[ ]';
        }
        return output;
    },
    objectToHTML: function (json) {
        var hasContents = false;
        var output = '';
        var numProps = 0;
        //for ref case
        if (this.hasRef){
            output += '<li>[<ul class="array collapsible">';
            var refId = guid();
            output += '<li><a href="javascript:;" onClick="loadChild(\'ref_'+ refId +'\');" id="ref_'+ refId +'" alt="' + json["url"] + '" title="'+ json["url"] + '">' + json["id"] + '</a></li>';
            output += '</ul>]</li>';
            this.hasRef = false;
            return output;
        }

        //ref case end

        for (var prop in json) {
            numProps++;
        }
        for (var prop in json) {
            if (prop == this.objectRef)
                this.hasRef = true;
            else
               this.hasRef = false;
            hasContents = true;
            output += '<li>';
            output = (this.hasRef == true)?'':output+'<span class="prop"><span class="q">"</span>' + this.jsString(prop) + '<span class="q">"</span></span>: '
            output += this.valueToHTML(json[prop]);
            if (numProps > 1) {
                output += ',';
            }
            output += '</li>';
            numProps--;
        }
        if (hasContents) {
            output = (this.hasRef == true)? output:'{<ul class="obj collapsible">' + output + '</ul>}';
        } else {
            output = '{ }';
        }
        return output;
    },
    jsonToHTML: function (json, callback, uri) {
        var output = '<div id="json">' + this.valueToHTML(json) + '</div>';
        if (callback) {
            output = '<div class="callback">' + callback + '(</div>' + output + '<div class="callback">)</div>';
        }
        return output;
    }
};

function URLParamGetter() {}
URLParamGetter.prototype = {
    getUrlVars: function(){
        var vars = [], hash;
        var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
        for(var i = 0; i < hashes.length; i++)
        {
            hash = hashes[i].split('=');
            vars.push(hash[0]);
            vars[hash[0]] = hash[1];
        }
        return vars;
    },

    updateInputs: function(objget, objurl, objpost){
        var urlvars = this.getUrlVars();
        if(objget && urlvars['requesturl']){
            objget.val(decodeURIComponent(urlvars['requesturl']));
       }
        if(objurl && urlvars['requesturl']){
            objurl.val(decodeURIComponent(urlvars['requesturl']));
        }
        if(objpost && urlvars['requestbody']){
            objpost.html(decodeURIComponent(urlvars['requestbody']));
        }
    }
};


//build tree view from nested list
(function ($) {
    $.extend($.fn, {
        swapClass: function (c1, c2) {
            var c1Elements = this.filter('.' + c1);
            this.filter('.' + c2).removeClass(c2).addClass(c1);
            c1Elements.removeClass(c1).addClass(c2);
            return this;
        },
        replaceClass: function (c1, c2) {
            return this.filter('.' + c1).removeClass(c1).addClass(c2).end();
        },
        hoverClass: function (className) {
            className = className || "hover";
            return this.hover(function () {
                $(this).addClass(className);
            }, function () {
                $(this).removeClass(className);
            });
        },
        heightToggle: function (animated, callback) {
            animated ? this.animate({
                height: "toggle"
            }, animated, callback) : this.each(function () {
                jQuery(this)[jQuery(this).is(":hidden") ? "show" : "hide"]();
                if (callback) callback.apply(this, arguments);
            });
        },
        heightHide: function (animated, callback) {
            if (animated) {
                this.animate({
                    height: "hide"
                }, animated, callback);
            } else {
                this.hide();
                if (callback) this.each(callback);
            }
        },
        prepareBranches: function (settings) {
            if (!settings.prerendered) {
                this.filter(":last-child:not(ul)").addClass(CLASSES.last);
                this.filter((settings.collapsed ? "" : "." + CLASSES.closed) + ":not(." + CLASSES.open + ")").find(">ul").hide();
            }
            return this.filter(":has(>ul)");
        },
        applyClasses: function (settings, toggler) {
            this.filter(":has(>ul):not(:has(>a))").find(">span").unbind("click.treeview").bind("click.treeview", function (event) {
                if (this == event.target) toggler.apply($(this).next());
            }).add($("a", this)).hoverClass();
            if (!settings.prerendered) {
                this.filter(":has(>ul:hidden)").addClass(CLASSES.collapsable).replaceClass(CLASSES.last, CLASSES.lastCollapsable);
                this.not(":has(>ul:hidden)").addClass(CLASSES.collapsable).replaceClass(CLASSES.last, CLASSES.lastCollapsable);
                var hitarea = this.find("div." + CLASSES.hitarea);
                if (!hitarea.length) hitarea = this.prepend("<div class=\"" + CLASSES.hitarea + "\"/>").find("div." + CLASSES.hitarea);
                hitarea.removeClass().addClass(CLASSES.hitarea).each(function () {
                    var classes = "";
                    $.each($(this).parent().attr("class").split(" "), function () {
                        classes += this + "-hitarea ";
                    });
                    $(this).addClass(classes);
                })
            }
            this.find("div." + CLASSES.hitarea).click(toggler);
        },
        treeview: function (settings) {
            settings = $.extend({
                cookieId: "treeview"
            }, settings);
            if (settings.toggle) {
                var callback = settings.toggle;
                settings.toggle = function () {
                    return callback.apply($(this).parent()[0], arguments);
                };
            }

            function treeController(tree, control) {
                function handler(filter) {
                    return function () {
                        toggler.apply($("div." + CLASSES.hitarea, tree).filter(function () {
                            return filter ? $(this).parent("." + filter).length : true;
                        }));
                        return false;
                    };
                }
                $("a:eq(0)", control).click(handler(CLASSES.collapsable));
                $("a:eq(1)", control).click(handler(CLASSES.expandable));
                $("a:eq(2)", control).click(handler());
            }

            function toggler() {
                $(this).parent().find(">.hitarea").swapClass(CLASSES.collapsableHitarea, CLASSES.expandableHitarea).swapClass(CLASSES.lastCollapsableHitarea, CLASSES.lastExpandableHitarea).end().swapClass(CLASSES.collapsable, CLASSES.expandable).swapClass(CLASSES.lastCollapsable, CLASSES.lastExpandable).find(">ul").heightToggle(settings.animated, settings.toggle);
                if (settings.unique) {
                   $(this).parent().siblings().find(">.hitarea").replaceClass(CLASSES.collapsableHitarea, CLASSES.expandableHitarea).replaceClass(CLASSES.lastCollapsableHitarea, CLASSES.lastExpandableHitarea).end().replaceClass(CLASSES.collapsable, CLASSES.expandable).replaceClass(CLASSES.lastCollapsable, CLASSES.lastExpandable).find(">ul").heightHide(settings.animated, settings.toggle);
                }
            }
            this.data("toggler", toggler);

            function serialize() {
                function binary(arg) {
                    return arg ? 1 : 0;
                }
                var data = [];
                branches.each(function (i, e) {
                    data[i] = $(e).is(":has(>ul:visible)") ? 1 : 0;
                });
                $.cookie(settings.cookieId, data.join(""), settings.cookieOptions);
            }

            function deserialize() {
                var stored = $.cookie(settings.cookieId);
                if (stored) {
                    var data = stored.split("");
                    branches.each(function (i, e) {
                        $(e).find(">ul")[parseInt(data[i]) ? "show" : "hide"]();
                    });
                }
            }
            this.addClass("treeview");
            var branches = this.find("li").prepareBranches(settings);
            switch (settings.persist) {
            case "cookie":
                var toggleCallback = settings.toggle;
                settings.toggle = function () {
                    serialize();
                    if (toggleCallback) {
                        toggleCallback.apply(this, arguments);
                    }
                };
                deserialize();
                break;
            case "location":
                var current = this.find("a").filter(function () {
                    return this.href.toLowerCase() == location.href.toLowerCase();
                });
                if (current.length) {
                    var items = current.addClass("selected").parents("ul, li").add(current.next()).show();
                    if (settings.prerendered) {
                        items.filter("li").swapClass(CLASSES.collapsable, CLASSES.expandable).swapClass(CLASSES.lastCollapsable, CLASSES.lastExpandable).find(">.hitarea").swapClass(CLASSES.collapsableHitarea, CLASSES.expandableHitarea).swapClass(CLASSES.lastCollapsableHitarea, CLASSES.lastExpandableHitarea);
                    }
                }
                break;
            }
            branches.applyClasses(settings, toggler);
            if (settings.control) {
                treeController(this, settings.control);
                $(settings.control).show();
            }
            return this;
        }
    });
    $.treeview = {};
    var CLASSES = ($.treeview.classes = {
        open: "open",
        closed: "closed",
        expandable: "expandable",
        expandableHitarea: "expandable-hitarea",
        lastExpandableHitarea: "lastExpandable-hitarea",
        collapsable: "collapsable",
        collapsableHitarea: "collapsable-hitarea",
        lastCollapsableHitarea: "lastCollapsable-hitarea",
        lastCollapsable: "lastCollapsable",
        lastExpandable: "lastExpandable",
        last: "last",
        hitarea: "hitarea"
    });
})(jQuery);

function Subtree(){
}
Subtree.prototype.count = 0;

Date.prototype.Format = function (fmt) { //author: meizz 
    var o = {
        "M+": this.getMonth() + 1, 
        "d+": this.getDate(), 
        "h+": this.getHours(), 
        "m+": this.getMinutes(), 
        "s+": this.getSeconds(), 
        "q+": Math.floor((this.getMonth() + 3) / 3), 
        "S": this.getMilliseconds()
    };
    if (/(Y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
    for (var k in o)
    if (new RegExp("(" + k + ")").test(fmt)) fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
    return fmt;
}

function getDateThatSeveralDaysBeforeNow(num) {
    var newdate = new Date();
    var newtimems = newdate.getTime() - (num * 24 * 60 * 60 * 1000);
    newdate.setTime(newtimems);
    return newdate;
}

$(document).ready(function(){
	var now = new Date();
	var endTime = now.Format("YYYYMMdd");
	var startDate = getDateThatSeveralDaysBeforeNow(7);
	var startTime = startDate.Format("YYYYMMdd");
    $("input:text[name='startTime']").val(startTime);
    $("input:text[name='endTime']").val(endTime);
    
    var editor = CodeMirror.fromTextArea(document.getElementById("code"), {
        //lineNumbers: true,
        matchBrackets: true,
        smartIndent: false,
        lineWrapping:true
    });
    
    $('.toggleOperation').click(function(){
        var opobj = $(this).parents('.operation').find('.content');
        if (opobj.is(':visible')){
            Docs.collapseOperation(opobj);
        } else {
            Docs.expandOperation(opobj);
        }
    });

    $('.toggleOption').click(function(){
        var opobj = $(this).parents('.option').find('.contents');
        if (opobj.is(':visible')){
            Docs.collapseOperation(opobj);
        } else {
            Docs.expandOperation(opobj);
        }
    });
    
    $.fn.selectRange = function(start, end) {
	    return this.each(function() {
	        if (this.setSelectionRange) {
	            this.focus();
	            this.setSelectionRange(start, end);
	        } else if (this.createTextRange) {
	            var range = this.createTextRange();
	            range.collapse(true);
	            range.moveEnd('character', end);
	            range.moveStart('character', start);
	            range.select();
	        }
	    });
	};

    $('.number').keyup(function(){
        var tmptxt=$(this).val();
        var start = $(this).caret().start;
        var replacedtxt = tmptxt.replace(/\D/g,'');
        $(this).val(replacedtxt);
        if (replacedtxt != tmptxt) {
        	start = start -1;
        }
        $(this).selectRange(start, start);
    }).bind("paste",function(){
        var tmptxt=$(this).val();
        $(this).val(tmptxt.replace(/\D/g,''));
    }).css("ime-mode", "disabled");
    
    var getAuditUrl = function() {
    	var auditUrl;
        var loc = window.location;
        var hostname = loc.hostname;
        auditUrl="http://localhost:8080";
        return auditUrl;
    }
    
    $('.submit').click(function(event){
    	var globalAuthorizationString = $($("li#resource_security form div#respbody").html()).find("li.last>span.string").text();
		var globalAuthorization = "" != globalAuthorizationString ? globalAuthorizationString.substring(1, globalAuthorizationString.length-1) : "";
        var url = "";
        var formob = $(this).parents('form');
        url = (formob.find("input:hidden[name='repopath']").length > 0)? url + formob.find("input:hidden[name='repopath']").attr('value'):url;
        url = (formob.find("input:text[name='reponame']").length > 0)? url + "/" + encodeURIComponent(formob.find("input:text[name='reponame']").attr('value')):url;
        url = (formob.find("input:hidden[name='branchpath']").length > 0)? url + formob.find("input:hidden[name='branchpath']").attr('value'):url;
        url = (formob.find("input:text[name='branchname']").length > 0)? url + "/" + encodeURIComponent(formob.find("input:text[name='branchname']").attr('value')):url;
        url = (formob.find("input:hidden[name='metapath']").length > 0)? url + formob.find("input:hidden[name='metapath']").attr('value'):url;
        url = (formob.find("input:text[name='metatype']").length > 0)? url + "/" + encodeURIComponent(formob.find("input:text[name='metatype']").attr('value')):url;
        url = (formob.find("input:hidden[name='indexes']").length > 0)? url + formob.find("input:hidden[name='indexes']").attr('value'):url;
        url = (formob.find("input:text[name='indexname']").length > 0)? url + "/" + encodeURIComponent(formob.find("input:text[name='indexname']").attr('value')):url;
        url = (formob.find("input:text[name='oid']").length > 0)? url + "/" + encodeURIComponent(formob.find("input:text[name='oid']").attr('value')):url;
        url = (formob.find("input:text[name='fieldname']").length > 0)? url + "/" + encodeURIComponent(formob.find("input:text[name='fieldname']").attr('value')):url;
        url = (formob.find("input:hidden[name='querypath']").length > 0)? url + formob.find("input:hidden[name='querypath']").attr('value'):url;
        url = (formob.find("textarea[name='query']").length > 0)? url + "/" + encodeURIComponent(editor.getValue()) :url;
        url = (formob.find("input:hidden[name='monitors']").length > 0)? url + formob.find("input:hidden[name='monitors']").attr('value'):url;
        url = (formob.find("input:text[name='monitorname']").length > 0)? url + "/" + encodeURIComponent(formob.find("input:text[name='monitorname']").attr('value')):url;
        url = (formob.find("input:hidden[name='management']").length > 0)? url + formob.find("input:hidden[name='management']").attr('value'):url;
        url = (formob.find("input:hidden[name='configuration']").length > 0)? url + formob.find("input:hidden[name='configuration']").attr('value'):url;
        url = (formob.find("input:hidden[name='service']").length > 0)? url + formob.find("input:hidden[name='service']").attr('value'):url;
        url = (formob.find("input:text[name='servicename']").length > 0)? url + "/" + encodeURIComponent(formob.find("input:text[name='servicename']").attr('value')):url;
        if(formob.find("select[name='actions']").length > 0){
        	url = (formob.find("select[name='actions']").val().length > 0)? url + "/actions/" + formob.find("select[name='actions']").attr('value'):url;
        }
        url = (formob.find("select[name='mode']").length > 0)? url + "?mode=" + formob.find("select[name='mode']").attr('value'):url;
        url = (formob.find("input:text[name='path']").length > 0 && formob.find("input:text[name='path']").attr('value').length > 0)? url + "&path=" + formob.find("input:text[name='path']").attr('value'):url;
        url = (formob.find("select[name='fetchHistory']").length > 0)? url + "&fetchHistory=" + formob.find("select[name='fetchHistory']").attr('value'):url;

        url = (formob.find("input:hidden[name='actions']").length > 0)? url + formob.find("input:hidden[name='actions']").attr('value'):url;
        url = (formob.find("input:hidden[name='validate']").length > 0)? url + formob.find("input:hidden[name='validate']").attr('value'):url;

        url = (formob.find("input:hidden[name='user']").length > 0)? url + formob.find("input:hidden[name='user']").attr('value'):url;
        url = (formob.find("input:text[name='user']").length > 0)? url + "/" + formob.find("input:text[name='user']").attr('value'):url;

        if(formob.find("input:text[name='filter']").length > 0){
            url = (formob.find("input:text[name='filter']").val().length > 0)? url + "&filter=" + formob.find("input:text[name='filter']").attr('value'):url;
        }
        if(formob.find("input:text[name='fields']").length > 0){
            url = (formob.find("input:text[name='fields']").val().length > 0)? url + "&fields=" + formob.find("input:text[name='fields']").attr('value'):url;
        }
        if(formob.find("select[name='allowFullTableScan']").length > 0){
            url = (formob.find("select[name='allowFullTableScan']").val().length > 0)? url + "&allowFullTableScan=" + formob.find("select[name='allowFullTableScan']").attr('value'):url;
        }
        if(formob.find("select[name='explain']").length > 0){
            url = (formob.find("select[name='explain']").val().length > 0)? url + "&explain=" + formob.find("select[name='explain']").attr('value'):url;
        }
        if(formob.find("input:text[name='sortOn']").length > 0){
            url = (formob.find("input:text[name='sortOn']").val().length > 0)? url + "&sortOn=" + formob.find("input:text[name='sortOn']").attr('value'):url;
        }
        if(formob.find("input:text[name='sortOrder']").length > 0){
            url = (formob.find("input:text[name='sortOrder']").val().length > 0)? url + "&sortOrder=" + formob.find("input:text[name='sortOrder']").attr('value'):url;
        }
        if(formob.find("input:text[name='limit']").length > 0){
            url = (formob.find("input:text[name='limit']").val().length > 0)? url + "&limit=" + formob.find("input:text[name='limit']").attr('value') :url;
        }
        if(formob.find("input:text[name='maxFetch']").length > 0){
            url = (formob.find("input:text[name='maxFetch']").val().length > 0)? url + "&maxFetch=" + formob.find("input:text[name='maxFetch']").attr('value'):url;
        }
        if(formob.find("input:text[name='skip']").length > 0){
            url = (formob.find("input:text[name='skip']").val().length > 0)? url + "&skip=" + formob.find("input:text[name='skip']").attr('value'):url;
        }
        if(formob.find("select[name='paginationMode']").length > 0){
            url = (formob.find("select[name='paginationMode']").val().length > 0)? url + "&paginationMode=" + formob.find("select[name='paginationMode']").attr('value'):url;
        }
        if(formob.find("input:text[name='cursor']").length > 0){
            url = (formob.find("input:text[name='cursor']").val().length > 0)? url + "&cursor=" + formob.find("input:text[name='cursor']").attr('value'):url;
        }
        if(formob.find("input:text[name='hint']").length > 0){
            url = (formob.find("input:text[name='hint']").val().length > 0)? url + "&hint=" + formob.find("input:text[name='hint']").attr('value'):url;
        }

        var srcElement=event.srcElement||event.target;
        
        if(srcElement.name=="commit"){
            url = "/cms" + url;
            var xmr=formob.find("select[name='priority']").val();
            if (typeof xmr != "undefined") {
              xmr = (xmr.length > 0)? formob.find("select[name='priority']").attr('value') :'';
            }
            var xpr=formob.find("select[name='policy']").val();
            if (typeof xpr != "undefined") {
              xpr = (xpr.length > 0)? formob.find("select[name='policy']").attr('value') :'';
            }
            var auther = formob.find("input:text[name='authorization']").val();
            if (typeof auther != "undefined") {
              auther = (auther.length > 0)? formob.find("input:text[name='authorization']").attr('value') :globalAuthorization;
            }
            var metaVersion =formob.find("input:text[name='metaVersion']").val();
            if (typeof metaVersion != "undefined") {
                metaVersion = (metaVersion.length > 0)? formob.find("input:text[name='metaVersion']").attr('value') :'';
            }
            var opType = formob.find("input:hidden[name='opType']").attr('value');
            var reqBody = (formob.find("textarea[name='reqbody']").length > 0)?formob.find("textarea[name='reqbody']").val():{};
            var xpassword = formob.find("input:password[name='x-password']").attr('value');
            sendcall(url, opType, reqBody, xmr, xpr, auther, xpassword, metaVersion, formob);
        } else if(srcElement.name=="copy"){
            url = "/cms" + url;
            var uri=window.prompt('Copy the URI below',window.location.protocol+'//'+window.location.host+url);
            if(uri!=null){
                window.open(url);
            }
        } else if(srcElement.name=="goto_audit"){
        if("true" == formob.find("select[name='delta']").val() && formob.find("input:text[name='fieldName']").val().length == 0) {
             alert("Audit service only support query delta changes on a specified field, please input 'fieldName' parameter value.");
             formob.find("input:text[name='fieldName']").focus();
             return;
        }
            url = (formob.find("select[name='sortOrder']").val().length > 0)? url + "?order=" + formob.find("select[name='sortOrder']").attr('value'):url;
            url = (formob.find("input:text[name='startTime']").val().length > 0)? url + "&startTime=" + formob.find("input:text[name='startTime']").attr('value'):url;
            url = (formob.find("input:text[name='endTime']").val().length > 0)? url + "&endTime=" + formob.find("input:text[name='endTime']").attr('value'):url;
            url = (formob.find("input:text[name='uid']").val().length > 0)? url + "&uid=" + formob.find("input:text[name='uid']").attr('value'):url;
            url = (formob.find("input:text[name='cid']").val().length > 0)? url + "&cid=" + formob.find("input:text[name='cid']").attr('value'):url;
            url = (formob.find("input:text[name='fieldName']").val().length > 0)? url + "&fieldName=" + formob.find("input:text[name='fieldName']").attr('value'):url;
            url = (formob.find("select[name='delta']").val().length > 0)? url + "&delta=" + formob.find("select[name='delta']").attr('value'):url;
            url = (formob.find("input:text[name='limit']").val().length > 0)? url + "&limit=" + formob.find("input:text[name='limit']").attr('value') :url;
            
            var uri=window.prompt('Copy the URI below', getAuditUrl() + url);
            if(uri!=null){
                window.open(uri);
            }
        } else if(srcElement.name=="goto_audit_deleted"){
            url = "/audit/getDeletedObjectOids";
            url = (formob.find("input:text[name='reponame']").length > 0)? url + "?repo=" + encodeURIComponent(formob.find("input:text[name='reponame']").attr('value')):url;
            url = (formob.find("input:text[name='branchname']").length > 0)? url + "&branch=" + encodeURIComponent(formob.find("input:text[name='branchname']").attr('value')):url;
            url = (formob.find("input:text[name='metatype']").length > 0)? url + "&class=" + encodeURIComponent(formob.find("input:text[name='metatype']").attr('value')):url;
            url = (formob.find("input:text[name='resourceid']").length > 0)? url + "&resourceId=" + encodeURIComponent(formob.find("input:text[name='resourceid']").attr('value')):url;
            url = (formob.find("input:text[name='startTime']").val().length > 0)? url + "&startTime=" + formob.find("input:text[name='startTime']").attr('value'):url;
            url = (formob.find("input:text[name='endTime']").val().length > 0)? url + "&endTime=" + formob.find("input:text[name='endTime']").attr('value'):url;
            url = (formob.find("select[name='sortOrder']").val().length > 0)? url + "&order=" + formob.find("select[name='sortOrder']").attr('value'):url;
            url = (formob.find("input:text[name='limit']").val().length > 0)? url + "&limit=" + formob.find("input:text[name='limit']").attr('value') :url;
            
            var uri=window.prompt('Copy the URI below', getAuditUrl() + url);
            if(uri!=null){
                window.open(uri);
            }
        }
    });

    // register event to simply hide all reponse when hide is clicked
    $('.response_hider').click(function(event){
        if (event != null) {
            event.preventDefault();
        }
        var formob = $(this).parents('form');
        formob.find("#response").slideUp();
        formob.find(".response_hider").fadeOut();
        $('#global_respcode').hide();
        $('#global_respbody').hide();
    });
});

$('li#resource_query ul.endpoints').show().hide(50);
$('li#resource_query li.operation div.content').show().hide(50);

function sendcall(url, type, reqbody,xmr, xpr, auther, xpassword, metaVersion, formbob){
    var count = 0;
    var formatter = new JSONFormatter();
    formbob.find('.response_throbber').show();
    var url = url || "/cms/repository";
    formbob.find('#respbody').empty();
    
    formbob.find('#progressBar').empty().hide();
    formbob.find('#cancel').unbind('click').hide();
    
    var clippyer = formbob.find('#response #respbodyText').next();
    var clippy_swf = "js/clippy/clippy.swf";
    
    $.ajax({
        type: type,
        url: url,
        accept: 'application/json',
        contentType: 'application/json',
        processData: true,
        data: reqbody,
        dataType: "json",
        beforeSend:function(XHR){
        if (typeof xmr != "undefined" && xmr.length > 0) {
              XHR.setRequestHeader('X-CMS-PRIORITY',xmr);
            } 
         if (typeof xpr != "undefined" && xpr.length > 0) {
                XHR.setRequestHeader('X-CMS-CONSISTENCY', xpr);
            }
        if( typeof auther != "undefined" && auther.length > 0) {
              XHR.setRequestHeader('Authorization',auther);
            }
         if (typeof xpassword != "undefined" && xpassword.length > 0) {
              XHR.setRequestHeader('X-Password', xpassword);
            }
            if (typeof metaVersion != "undefined" && metaVersion.length > 0) {
                XHR.setRequestHeader('X-CMS-METAVERSION', metaVersion);
            }
        },
        success: function (data, textStatus, xhr) {
            formbob.find('#respcode').text(xhr.status);
            var copyData;
            if (typeof data == 'object'){
            	copyData = JSON.stringify(data, null, 4);
                var result = data.result;
                if(typeof result != 'undefined') {
	                var length = result.length;
	                var supportWebWorker = false;
	                if (typeof(Worker) !== "undefined") {
	                	supportWebWorker = true;
	                }
	                if(length > 10 && supportWebWorker) {
	                	var worker = new Worker('worker.js');
	                	var createJob = function(jsonData) {
	                		worker.postMessage(jsonData); // start the worker.
	                	}
	                	formbob.find('#progressBar').empty().show();
	                	formbob.find('#cancel').click(function(event) {
	                		worker.terminate();
	                		formbob.find('.response_throbber').hide();
	                		var currentLoaded = formbob.find('#progressBar').text();
	                		formbob.find('#progressBar').text("" + currentLoaded + " but canceled by user");
	                		clippyer.clippy({'text': copyData, clippy_path: clippy_swf });
	                		formbob.find('#cancel').hide();
	                	}).show();
	                	var handleMessage = function(e) {
	                		 count = count + 1;
	                         var objHtml = e.data;
	                         if(count < length) {
	                            var newContent = $('<li/>').append($(objHtml).html()).append(",");
	                            ($(hieraDiv).clone(true)).insertBefore($(newContent).contents().first());
	                            $(newContent).appendTo(appender);
	                            $(newContent).treeview({});
	                            $(newContent).removeClass('treeview').addClass('collapsable');
	                            var loadedPercentage = Math.floor( (count * 100) / length);
		                        formbob.find('#progressBar').text("Loaded " + loadedPercentage + "%");
	                            createJob(result[count]);
	                         } else {
	                            worker.terminate();
	                            clippyer.clippy({'text': copyData, clippy_path: clippy_swf });
	                            formbob.find('#cancel').hide();
	                            var newContent = $('<li/>').append($(objHtml).html());
	                            ($(hieraDiv).clone(true).addClass('lastCollapsable-hitarea')).insertBefore($(newContent).contents().first());
	                            $(newContent).appendTo(appender);  
	                            $(newContent).treeview({});
	                            $(newContent).removeClass('treeview').addClass('collapsable').addClass('lastCollapsable');
	                            formbob.find('.response_throbber').hide();
	                            formbob.find('#progressBar').hide();
	                         }
	                	}
	                 	worker.onmessage = handleMessage;
	                 	var initArray = new Array(1);
	                 	initArray[0] = result[0];
	                 	data.result = initArray;
	                 	var htmlTemplate = formatter.jsonToHTML(data);
	                 	formbob.find('#respbody').append(htmlTemplate);
	                    formbob.find('#respbody').treeview({});
	                    var appender = formbob.find('#respbody').find("div#json > ul > li:contains('result') > ul.array");
	                    var hieraDiv = $(appender).children('li').children('div').removeClass('lastCollapsable-hitarea').clone(true);
	                 	appender.empty();
	                    createJob(result[count]);
	                } else {
	                  var totalHtml = formatter.jsonToHTML(data);
	                  formbob.find('#respbody').html(totalHtml);
	                  formbob.find('#respbody').treeview({});
	                  formbob.find('.response_throbber').hide();
	                }
                } else {
                	var totalHtml = formatter.jsonToHTML(data);
	                formbob.find('#respbody').html(totalHtml);
	                formbob.find('#respbody').treeview({});
	                formbob.find('.response_throbber').hide();
                }
            } else {
            	copyData = data;
                formbob.find('#respbody').html(data);
            }
            
            clippyer.clippy({'text': copyData, clippy_path: clippy_swf });
            
            formbob.find('.response_hider').show();
            formbob.find('#response').show();
            // hide global
            $('#global_respcode').hide();
            $('#global_respbody').hide();
        },
        error: function(err){
            formbob.find('.response_throbber').hide();
            formbob.find('#respcode').text(err.status);
            formbob.find('#respbody').html(err.statusText+":");
            formbob.find('#respbody').append(err.responseText);
            
            copyData = formbob.find('#respbody').html();
            clippyer.clippy({'text': copyData, clippy_path: clippy_swf });
            
            formbob.find('#response').show();
            formbob.find('.response_hider').show();
            // hide global
            $('#global_respcode').hide();
            $('#global_respbody').hide();
        }
    });
}

function loadChild(id){
    var formatter = new JSONFormatter();
    var refOjb = $("#"+id);
    var url = refOjb.attr('alt');
    $('.pt div.hitarea').remove();
    $.ajax({
        type: 'get',
        url: '/cms' + url,
        accept: 'application/json',
        contentType: 'application/json',
        processData: true,
        data: {},
        dataType: "json",
        success: function (data) {
            if (typeof data == 'object'){
                $('<div id="result'+id+'"></div>').remove();
                $('<div id="result'+id+'" style="display:none"></div>').appendTo('body');
                liobj = refOjb.parent('li');
                liobj.addClass('collapsable pt');
                liobj.prepend('<div class="hitarea collapsable-hitarea"></div>');
                liobj.find('div.hitarea').click(function(){
                    var parent = $(this).parent('li');
                    parent.find('#result'+id).hide();
                    parent.removeClass('collapsable pt');
                    $(this).remove();
                });
                var childnode=$('#result'+id);
                childnode.html(formatter.jsonToHTML(data.result[0]));
                childnode.treeview(json);
                childnode.appendTo(refOjb.parent('li:first'));
                childnode.toggle(0,function(){
                    if ($(this).is(':hidden')){
                        liobj.find('div.hitarea').parent('li').removeClass('collapsable pt');
                        liobj.find('div.hitarea').remove();
                    }
                });
            } else {
                $(this).parent('#respbody').html(data);
                $(this).parent('#response').show();
            }
        },
        error: function(err){
            $(this).parent('#respcode').text(err.status);
            $(this).parent('#respbody').html(err.statusText);
        }
    });
}


var args =  new Object();
var para=location.search.substring(1);
var pairs=para.split("&");
for(var i=0;i<pairs.length;i++){
    var pos=pairs[i].indexOf("=");
    var argname=pairs[i].substring(0,pos);
    var value=pairs[i].substring(pos+1);
    args[argname]=unescape(value);
}
if(args['opid']!=undefined){
    var dataType='';
    switch(args['opid']){
        case '1':
            dataType='metadata';
            break;
        case '2':
            $('#'+args['opid']).parents('.operation').find("textarea[name='reqbody']").val(args['reqBody']);
            dataType='metadata';
            break;
        case '3':
        $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
            dataType='metadata';
            break;
        case '4':
        $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
            dataType='metadata';
            break;
        case '5':
        $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
            $('#'+args['opid']).parents('.operation').find("textarea[name='reqbody']").val(args['reqBody']);
            dataType='metadata';
            break;
        case '6':
        $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='metatype']").attr('value',args['metatype']);
            dataType='metadata';
            break;
        case '7':
        $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='metatype']").attr('value',args['metatype']);
            $('#'+args['opid']).parents('.operation').find("textarea[name='reqbody']").val(args['reqBody']);
            dataType='metadata';
            break;
        case '8':
        $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
            dataType='entity';
            break;
        case '9':
        $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='branchname']").attr('value',args['branch']);
            dataType='entity';
            break;
        case '10':
        $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
            $('#'+args['opid']).parents('.operation').find("textarea[name='reqbody']").val(args['reqBody']);
            dataType='entity';
            break;
        case '11':
        $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='branchname']").attr('value',args['branch']);
            $('#'+args['opid']).parents('.operation').find("textarea[name='reqbody']").val(args['reqBody']);
            dataType='entity';
            break;
        case '12':
        $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='branchname']").attr('value',args['branch']);
            dataType='entity';
            break;
        case '13':
        $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='branchname']").attr('value',args['branch']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='metatype']").attr('value',args['metatype']);
            $('#'+args['opid']).parents('.operation').find("input:text[name='oid']").attr('value',args['oid']);
            dataType='entity';
            break;      
        case '14':
        $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='branchname']").attr('value',args['branch']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='metatype']").attr('value',args['metatype']);
            $('#'+args['opid']).parents('.operation').find("input:text[name='path']").attr('value',args['path']);
            $('#'+args['opid']).parents('.operation').find("textarea[name='reqbody']").val(args['reqBody']);
            dataType='entity';
            break;      
        case '15':
        $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='branchname']").attr('value',args['branch']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='metatype']").attr('value',args['metatype']);
            $('#'+args['opid']).parents('.operation').find("textarea[name='reqbody']").val(args['reqBody']);
            dataType='entity';
            break;      
        case '16':
        $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='branchname']").attr('value',args['branch']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='metatype']").attr('value',args['metatype']);
            //$('#'+args['opid']).parents('.operation').find("textarea[name='reqbody']").val(args['reqBody']);
            dataType='entity';
            break;      
        case '17':
        $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='branchname']").attr('value',args['branch']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='metatype']").attr('value',args['metatype']);
            dataType='entity';
            break;              
        case '18':
        $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='branchname']").attr('value',args['branch']);
            $('#'+args['opid']).parents('.operation').find("input:text[name='limit']").attr('value',args['limit']);
            $('#'+args['opid']).parents('.operation').find("textarea[name='query']").val(args['query']);
            dataType='query';
            break;  
        case '50':
        $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='branchname']").attr('value',args['branch']);
            $('#'+args['opid']).parents('.operation').find("input:text[name='limit']").attr('value',args['limit']);
            $('#'+args['opid']).parents('.operation').find("textarea[name='query']").val(args['query']);
            dataType='query';
            break;  
        case '19':
            dataType='management';
            break;  
        case '20':
        $('#'+args['opid']).parents('.operation').find("input:text[name='monitorname']").attr('value',args['monitorname']);
            dataType='management';
            break;
        case '21':
            dataType='management';
            break;
        case '22':
            $('#'+args['opid']).parents('.operation').find("textarea[name='reqbody']").val(args['reqBody']);
            dataType='management';
            break;
        case '23':
            dataType='management';
            break;
        case '24':
        $('#'+args['opid']).parents('.operation').find("input:text[name='servicename']").attr('value',args['servicename']);
            dataType='management';
            break;
        case '25':
            $('#'+args['opid']).parents('.operation').find("textarea[name='reqbody']").val(args['reqBody']);
            dataType='management';
            break;
        case '26':
        $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='branchname']").attr('value',args['branch']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='metatype']").attr('value',args['metatype']);
            dataType='entity';
            break;
        case '27':
        $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='metatype']").attr('value',args['metatype']);
            dataType='metadata';
            break;
        case '28':
        $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='metatype']").attr('value',args['metatype']);
            $('#'+args['opid']).parents('.operation').find("textarea[name='reqbody']").val(args['reqBody']);
            dataType='metadata';
            break;
        case '29':
        $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='metatype']").attr('value',args['metatype']);
        $('#'+args['opid']).parents('.operation').find("input:text[name='indexname']").attr('value',args['indexname']);
            dataType='metadata';
            break;
        case '30':
            dataType='management';
            break;
        case '31':
            $('#'+args['opid']).parents('.operation').find("textarea[name='reqbody']").val(args['reqBody']);
            dataType='management';
            break;
	    case '33':
	    $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
	    $('#'+args['opid']).parents('.operation').find("input:text[name='branchname']").attr('value',args['branch']);
	    $('#'+args['opid']).parents('.operation').find("input:text[name='metatype']").attr('value',args['metatype']);
	        dataType='entity';
	    case '34':
	    $('#'+args['opid']).parents('.operation').find("input:text[name='reponame']").attr('value',args['reponame']);
	    $('#'+args['opid']).parents('.operation').find("input:text[name='branchname']").attr('value',args['branch']);
	    $('#'+args['opid']).parents('.operation').find("input:text[name='metatype']").attr('value',args['metatype']);
	    $('#'+args['opid']).parents('.operation').find("input:text[name='actions']").attr('value',args['actions']);
	        dataType='entity';
	        break;        
    }

    var opobject=$('#'+args['opid']).parents('.operation').find('.content');
    Docs.collapseOperationsForResource(dataType);
    Docs.expandOperation(opobject);
}
