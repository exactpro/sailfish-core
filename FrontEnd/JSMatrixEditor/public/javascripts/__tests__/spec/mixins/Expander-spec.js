/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

//jest.dontMock('../../view/mixins/Expander.js');

describe ('collapse/expand node by path', function() {

    it ('should save node path in config', function() {
        var TheExpander = require('view/mixins/Expander.js');

        var cfg = TheExpander.getEmptyExpanded();

        var path = '12345/342354/sdvev/123154';

        expect(TheExpander.isRowExpanded(path, cfg)).toEqual(false);

        cfg = TheExpander.expandRow(path);

        expect(TheExpander.isRowExpanded(path, cfg)).toEqual(true);

        cfg = TheExpander.collapseRow(path, cfg);

        expect(TheExpander.isRowExpanded(path, cfg)).toEqual(false);

        expect(cfg === TheExpander.getEmptyExpanded()).toEqual(true);

        cfg = TheExpander.collapseRow(path, cfg);

        expect(TheExpander.isRowExpanded(path, cfg)).toEqual(false);

        expect(cfg === TheExpander.getEmptyExpanded()).toEqual(true);

        cfg = TheExpander.expandRow(path);

        expect(TheExpander.isRowExpanded(path, cfg)).toEqual(true);

        cfg = TheExpander.collapseRow(path, cfg);

        expect(TheExpander.isRowExpanded(path, cfg)).toEqual(false);

        expect(cfg === TheExpander.getEmptyExpanded()).toEqual(true);
    });

    it ('should save few nodes path in config', function() {
        var TheExpander = require('../../../view/mixins/Expander.js');

        var cfg = TheExpander.getEmptyExpanded();

        var path1 = '12345/342354/sdvev/123154';
        var path2 = '12345/342354/sdvev/2342';
        var path3 = '12345/342354/11232';

        expect(TheExpander.isRowExpanded(path1, cfg)).toEqual(false);
        expect(TheExpander.isRowExpanded(path2, cfg)).toEqual(false);
        expect(TheExpander.isRowExpanded(path3, cfg)).toEqual(false);

        cfg = TheExpander.expandRow(path1);

        expect(TheExpander.isRowExpanded(path1, cfg)).toEqual(true);
        expect(TheExpander.isRowExpanded(path2, cfg)).toEqual(false);
        expect(TheExpander.isRowExpanded(path3, cfg)).toEqual(false);

        cfg = TheExpander.collapseRow(path1, cfg);

        expect(TheExpander.isRowExpanded(path1, cfg)).toEqual(false);
        expect(TheExpander.isRowExpanded(path2, cfg)).toEqual(false);
        expect(TheExpander.isRowExpanded(path3, cfg)).toEqual(false);
        expect(cfg === TheExpander.getEmptyExpanded()).toEqual(true);

        cfg = TheExpander.collapseRow(path1, cfg);
        cfg = TheExpander.collapseRow(path2, cfg);
        cfg = TheExpander.collapseRow(path3, cfg);

        expect(TheExpander.isRowExpanded(path1, cfg)).toEqual(false);
        expect(TheExpander.isRowExpanded(path2, cfg)).toEqual(false);
        expect(TheExpander.isRowExpanded(path3, cfg)).toEqual(false);
        expect(cfg === TheExpander.getEmptyExpanded()).toEqual(true);

        cfg = TheExpander.expandRow(path1, cfg);
        cfg = TheExpander.expandRow(path2, cfg);
        cfg = TheExpander.expandRow(path3, cfg);

        expect(TheExpander.isRowExpanded(path1, cfg)).toEqual(true);
        expect(TheExpander.isRowExpanded(path2, cfg)).toEqual(true);
        expect(TheExpander.isRowExpanded(path3, cfg)).toEqual(true);

        cfg = TheExpander.collapseRow(path2, cfg);

        expect(TheExpander.isRowExpanded(path1, cfg)).toEqual(true);
        expect(TheExpander.isRowExpanded(path2, cfg)).toEqual(false);
        expect(TheExpander.isRowExpanded(path3, cfg)).toEqual(true);

        cfg = TheExpander.collapseRow(path3, cfg);

        expect(TheExpander.isRowExpanded(path1, cfg)).toEqual(true);
        expect(TheExpander.isRowExpanded(path2, cfg)).toEqual(false);
        expect(TheExpander.isRowExpanded(path3, cfg)).toEqual(false);

        cfg = TheExpander.expandRow(path1, cfg);
        cfg = TheExpander.expandRow(path2, cfg);
        cfg = TheExpander.expandRow(path3, cfg);

        expect(TheExpander.isRowExpanded(path1, cfg)).toEqual(true);
        expect(TheExpander.isRowExpanded(path2, cfg)).toEqual(true);
        expect(TheExpander.isRowExpanded(path3, cfg)).toEqual(true);

        cfg = TheExpander.collapseRow(path1, cfg);

        expect(TheExpander.isRowExpanded(path1, cfg)).toEqual(false);
        expect(TheExpander.isRowExpanded(path2, cfg)).toEqual(true);
        expect(TheExpander.isRowExpanded(path3, cfg)).toEqual(true);

        cfg = TheExpander.collapseRow(path2, cfg);

        expect(TheExpander.isRowExpanded(path1, cfg)).toEqual(false);
        expect(TheExpander.isRowExpanded(path2, cfg)).toEqual(false);
        expect(TheExpander.isRowExpanded(path3, cfg)).toEqual(true);

        cfg = TheExpander.collapseRow(path1, cfg);
        cfg = TheExpander.collapseRow(path2, cfg);
        cfg = TheExpander.collapseRow(path3, cfg);

        expect(TheExpander.isRowExpanded(path1, cfg)).toEqual(false);
        expect(TheExpander.isRowExpanded(path2, cfg)).toEqual(false);
        expect(TheExpander.isRowExpanded(path3, cfg)).toEqual(false);
        expect(cfg === TheExpander.getEmptyExpanded()).toEqual(true);
    });
});
