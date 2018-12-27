<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">
        <title>${title}</title>
        <link rel="icon" href="resources/SFicon.png" type="image/x-icon"/>
        <link rel="shortcut icon" href="resources/SFicon.png" type="image/x-icon"/>
        <style></style> <!-- Empty style for dynamic hiding passed actions/fields (look logic.js). Should be first style in document. -->
        <link rel="stylesheet" type="text/css" href="resources/htmlreport.css">
        <link rel="stylesheet" type="text/css" href="resources/jquery-ui.min.css">
    </head>
    <body onkeypress="onKeyPress(event)">
        <script src="resources/logic.js"></script>
        <script src="resources/jquery.min.js"></script>
        <script src="resources/jquery-ui.min.js"></script>
<#if with_wrapper>
        <div id="wrapper">
            <a name="top">&nbsp;</a>
</#if>