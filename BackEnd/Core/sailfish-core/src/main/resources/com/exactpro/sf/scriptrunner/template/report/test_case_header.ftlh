<table id="header" style="position: fixed; top: 0px;">
    <tr>
        <td>
            <a href="#top">
                <button title="Go to page top" id="totop" name="totop" class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-icon-left" style="position:fixed; top:8px; left:135px; z-index:3; height: 24px;" type="submit" role="button" aria-disabled="false">
                    <span class="ui-button-text ui-c">Go top</span>
                </button>
            </a>
            <a href="report.html?action=view">
                <button title="Go to test cases list" id="toTCList" name="toTCList" class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-icon-left" style="position:fixed; top:8px; left:25px; z-index:3; height: 24px;" type="submit" role="button" aria-disabled="false">
                    <span class="ui-button-text ui-c">Back to list</span>
                </button>
            </a>
            <strong class="shead <#if passed>passed<#else><#if conditionallyPassed>conditionally_passed<#else>failed</#if></#if>" style="width:50%; display: inline-block; word-wrap: break-word; position: relative; left: -12px;">
                ${name} (<#if passed>PASSED<#else><#if conditionallyPassed>CONDITIONALLY PASSED<#else>FAILED</#if></#if>) [${(duration / 1000.0)?c}s]<#if description??> : ${description}</#if>
                <a href="#" id="prevTCLink">
                    <button title="Go to previous test case"<#if order == 1> disabled</#if> id="prevBtn" name="prevBtn" class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-icon-left<#if order == 1> ui-state-disabled</#if>" style="position:absolute; left:-21px; top: 50%; margin-top: -12px; z-index:3; height: 25px;" type="submit" role="button" aria-disabled="false">
                        <span class="ui-button-text ui-c"><</span>
                    </button>
                </a>
                <a id="nextTCLink" href="#" data-order=${order}>
                    <button title="Go to next test case" id="nextBtn" name="nextBtn" class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-icon-left" style="position:absolute; right:-21px; top: 50%; margin-top: -12px; z-index:3; height: 25px;" type="submit" role="button" aria-disabled="false">
                        <span class="ui-button-text ui-c">></span>
                    </button>
                </a>
            </strong>
            <div class="menu">
                <input type="checkbox" id="menu-fake-checkbox">
                <label for="menu-fake-checkbox" id="menu-label">View</label>
                <ul class="submenu">
                    <li>
                        <input type="checkbox" class="menu-item" id="actions-menu" onclick='showHideAll(this);' checked>
                        <label for="actions-menu" class="submenu-label">Actions</label>
                        <ul class="submenu-items">
                            <li class="submenu-item">
                                <input type="checkbox" class="menu-item" id="show-passed-actions" data-rule=".passed-action{display:none}" onclick='showHideElements(this);' checked>
                                <label for="show-passed-actions" class="submenu-label">Passed</label>
                            </li>
                            <li class="submenu-item">
                                <input type="checkbox" class="menu-item" id="show-conditionally-passed-actions" data-rule=".conditionally_passed-action{display:none}" onclick='showHideElements(this);' checked>
                                <label for="show-conditionally-passed-actions" class="submenu-label">Conditionally Passed</label>
                            </li>
                            <li class="submenu-item">
                                <input type="checkbox" class="menu-item" id="show-failed-actions" data-rule=".failed-action{display:none}"onclick='showHideElements(this);' checked>
                                <label for="show-failed-actions" class="submenu-label">Failed</label>
                            </li>
                        </ul>
                    </li>

                    <li>
                        <input type="checkbox" class="menu-item" id="fields-menu" onclick='showHideAll(this);' checked>
                        <label for="fields-menu" class="submenu-label">Fields</label>
                        <ul class="submenu-items fields-submenu">
                            <li class="submenu-item">
                                <input type="checkbox" class="menu-item" id='show-passed-fields' data-rule=".passed-field{display:none}" onclick='showHideElements(this);' checked>
                                <label for="show-passed-fields" class="submenu-label">Passed</label>
                            </li>
                            <li class="submenu-item">
                                <input type="checkbox" class="menu-item" id="show-na-fields" data-rule=".na-field{display:none}" onclick='showHideElements(this);' checked>
                                <label for="show-na-fields" class="submenu-label">NA</label>
                            </li>
                            <li class="submenu-item">
                                <input type="checkbox" class="menu-item" id="show-conditionally-passed-fields" data-rule=".conditionally_passed-fields{display:none}" onclick='showHideElements(this);' checked>
                                <label for="show-conditionally-passed-fields" class="submenu-label">Conditionally Passed</label>
                            </li>
                            <li class="submenu-item">
                                <input type="checkbox" class="menu-item" id="show-failed-fields" data-rule=".failed-field{display:none}" onclick='showHideElements(this);' checked>
                                <label for="show-failed-fields" class="submenu-label">Failed</label>
                            </li>
                        </ul>
                    </li>

                    <li>
                        <input type="checkbox" class="menu-item" id="messages-menu" onclick='showHideAll(this);' checked>
                        <label for="messages-menu" class="submenu-label">Messages</label>
                        <ul class="submenu-items messages-submenu">
                            <li class="submenu-item">
                                <input type="checkbox" class="menu-item" id='show-checkpoints' onclick='showHideCheckPoints(this);' checked>
                                <label for="show-checkpoints" class="submenu-label">Checkpoints</label>
                            </li>
                        </ul>
                    </li>

                </ul>
            </div>
        </td>
    </tr>
</table>