var parentEl;
var expand;
var boldNode;

function load() {
    var jsonData = JSON.parse(data);

    createNodes(jsonData, document.getElementsByTagName("ul")[0]);

    data = null;

    document.getElementsByTagName("head")[0].removeChild(document.getElementsByTagName("script")[0]);
}

function createNodes(jsonData, parent) {

    var newNode = document.createElement("li");

    var iconSpan = document.createElement("span");
    iconSpan.setAttribute('class', "ui-treenode-icon ui-icon " + jsonData.icon);

    var nameSpan = document.createElement("span");
    nameSpan.setAttribute('class', "ui-treenode-label ui-corner-all");

    nameSpan.setAttribute('onclick', "loadContent(this)");
    nameSpan.innerHTML = jsonData.name;
    if(jsonData.type === "FIELD"){
        var type = document.createElement("span");
        type.innerHTML = "::" + jsonData.javaType;
        type.setAttribute("class", "eps-help-data-type");
        nameSpan.appendChild(type);
    }
    nameSpan.setAttribute('data-href', jsonData.filePath);


    var childNodes = jsonData.childNodes;

    if ((childNodes !== undefined && childNodes !== null)) {
        var triangleSpan = document.createElement("span");
        triangleSpan.setAttribute('class', "ui-tree-toggler ui-icon ui-icon-triangle-1-e");
        triangleSpan.setAttribute('onclick', "expandNode(this)");

        if (jsonData.filePath.indexOf(".json") != -1) {
            triangleSpan.setAttribute('data-json', jsonData.filePath);
        }

        newNode.appendChild(triangleSpan);
        newNode.appendChild(iconSpan);
        newNode.appendChild(nameSpan);
        parent.appendChild(newNode);


        var parentUl = document.createElement("ul");
        newNode.appendChild(parentUl);

        for (var i = 0; i < childNodes.length; i++) {
            createNodes(childNodes[i], parentUl);
        }

    } else if (jsonData.filePath.indexOf(".json") != -1) {
        var triangleSpan = document.createElement("span");
        triangleSpan.setAttribute('data-json', jsonData.filePath);
        triangleSpan.setAttribute('class', "ui-tree-toggler ui-icon ui-icon-triangle-1-e");
        triangleSpan.setAttribute('onclick', "expandNode(this)");

        nameSpan.setAttribute("data-href", jsonData.filePath.substr(0, jsonData.filePath.length - 5) + ".html");

        newNode.appendChild(triangleSpan);
        newNode.appendChild(iconSpan);
        newNode.appendChild(nameSpan);

        var parentUl = document.createElement("ul");
        newNode.appendChild(parentUl);

        parent.appendChild(newNode);


    }
    else {
        nameSpan.setAttribute("data-href", jsonData.filePath);


        newNode.appendChild(iconSpan);
        newNode.appendChild(nameSpan);
        parent.appendChild(newNode);
    }

    jsonData = null;
}

function expandNode(element) {

    element.setAttribute("onclick", "collapseNode(this)");
    element.setAttribute("class", "ui-tree-toggler ui-icon ui-icon-triangle-1-s")

    var parentNode = element.parentNode;
    var ul = parentNode.childNodes[3];
    var ulNodes = ul.childNodes;
    parentUl = element;

    if (ulNodes === null || ulNodes === undefined || ulNodes.length == 0) {
        loadNodes(element);
    } else {
        ul.style.display = "block";
    }
}

function collapseNode(element) {
    element.setAttribute("onclick", "expandNode(this)");
    element.setAttribute("class", "ui-tree-toggler ui-icon ui-icon-triangle-1-e")

    var parentNode = element.parentNode;
    var ul = parentNode.childNodes[3];
    ul.style.display = "none";
}

function loadNodes(element) {
    addScript(element.getAttribute("data-json"));
    expand = true;
    parentEl = element;

}


function loadContent(element) {
    if(boldNode !== null && boldNode !== undefined){
        boldNode.style.fontWeight = "normal";
    }
    boldNode = element;
    boldNode.style.fontWeight = "bold";
    var iFrame = document.getElementsByClassName("eps-content-frame")[0];
    var href = element.getAttribute("data-href");
    iFrame.setAttribute("src", href);

}


function addScript(script) {
    var newScript = document.createElement("script");
    newScript.src = script;
    newScript.type = "text/javascript";
    newScript.setAttribute("onload", "scriptLoad()")

    document.getElementsByTagName("head")[0].insertBefore(newScript, document.getElementsByTagName("script")[0]);
}


function scriptLoad() {

    try {
        var jsonData = JSON.parse(data);
        data = null;

        var parentNode = parentEl.parentNode;

        var ul = parentNode.childNodes[3];

        var nodeName = parentNode.childNodes[2];
        nodeName.setAttribute("data-href", jsonData.filePath);

        if (expand) {
            var childNodes = jsonData.childNodes;

            for (var i = 0; i < childNodes.length; i++) {
                createNodes(childNodes[i], ul);
            }

            expandNode(parentNode.childNodes[0]);
        } else {
            loadContent(nodeName);
        }

    } catch (error) {
        console.log(error);
        data = null;
    }

    document.getElementsByTagName("head")[0].removeChild(document.getElementsByTagName("script")[0]);
}