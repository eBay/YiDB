//JSONFormatter convert JSON to Html nested list
function guid() {
    function _p8(s) {
        var p = (Math.random().toString(16)+"000000000").substr(2,8);
        return s ? "-" + p.substr(0,4) + "-" + p.substr(4,4) : p ;
    }
    return _p8() + _p8(true) + _p8(true) + _p8();
}
function JSONFormatter() {}
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

var parseJSONToHTML = function(json) {
    var formatter = new JSONFormatter();
    return formatter.jsonToHTML(json);
}

self.addEventListener('message', function (e) {
    var data = e.data;
    var html = parseJSONToHTML(data);
    self.postMessage(html);
});
