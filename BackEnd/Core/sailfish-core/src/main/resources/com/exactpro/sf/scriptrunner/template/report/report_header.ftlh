<table id="header" style="position: fixed; top: 0;">
    <tr>
        <td>
            <strong class="shead" style="width:50%; display: inline-block; word-wrap: break-word; position: relative">Test script: ${name}</strong>
        </td>
        <td>
            <div class="menu report-menu">
                <input type="checkbox" id="menu-fake-checkbox">
                <label for="menu-fake-checkbox" id="menu-label">View</label>
                <ul class="submenu">
                    <li>
                        <input type="checkbox" class="menu-item" id="tcs-menu" onclick='showHideAll(this);' checked>
                        <label for="tcs-menu" class="submenu-label">Test Cases</label>
                        <ul class="submenu-items">
                            <li class="submenu-item">
                                <input type="checkbox" class="menu-item" id="show-passed-tcs" data-rule=".passed-tc{display:none}" onclick='showHideElements(this);' checked>
                                <label for="show-passed-tcs" class="submenu-label">Passed</label>
                            </li>
                            <li class="submenu-item">
                                <input type="checkbox" class="menu-item" id="show-conditionally-passed-tcs" data-rule=".conditionally_passed-tc{display:none}" onclick='showHideElements(this);' checked>
                                <label for="show-conditionally-passed-tcs" class="submenu-label">Conditionally Passed</label>
                            </li>
                            <li class="submenu-item">
                                <input type="checkbox" class="menu-item" id="show-failed-tcs" data-rule=".failed-tc{display:none}"onclick='showHideElements(this);' checked>
                                <label for="show-failed-tcs" class="submenu-label">Failed</label>
                            </li>
                        </ul>
                    </li>

                </ul>
            </div>
        </td>
    </tr>
</table>
