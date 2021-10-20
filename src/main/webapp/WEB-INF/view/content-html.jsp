<!DOCTYPE html>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix = "c" uri = "http://java.sun.com/jsp/jstl/core" %>
<div>
    <div class="jk-content-thumbnail" style="float:left; margin-right:10px;">
        <img alt="content thumbnail picture" src="/content-thumbnail/${mediafile.uuid}" height="66px"/>
    </div>
    <div>
        <span class="jk-content-title" style="font-weight: bold;"><c:out value="${mediafile.title}"/></span>
        <div class="jk-content-body">
            <c:out value="${mediafile.content}" escapeXml="false"/>
        </div>
    </div>
</div>