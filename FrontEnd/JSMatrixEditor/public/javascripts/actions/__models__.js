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
import {polymorphCfg} from 'utils';

export class FieldDefinition {
    static ctrParams = ['include', 'name', 'type', 'ref', 'req', 'coll', 'owner', 'taboo', 'idx', 'values']
    constructor() {
        const cfg = polymorphCfg(this, arguments);

        this.includeBlock = cfg.include;
        this.name = cfg.name;
        this.type = cfg.type;
        this.targetRefType = cfg.ref;
        this.isRequired = cfg.req;
        this.isCollection = cfg.coll;
        this.owner = cfg.owner;
        this.isTaboo = cfg.taboo;
        // index in message
        this.index = cfg.idx;
        // enum's values
        this.values = cfg.values;
    }
}

export class DataItem {
    static ctrParams = ['path', 'data', 'def', 'hasErrors']
    constructor() {
        const cfg = polymorphCfg(this, arguments);

        this.path = cfg.path;
        this.data = cfg.data;
        this.definition = cfg.def;
        this.hasErrors = cfg.hasErrors;
    }
}

export class MessageDefinition {
    static ctrParams = ['name', 'namespace', 'fields', 'dict', 'isAML2', 'isAML3', 'utilClasses']
    constructor() {
        const cfg = polymorphCfg(this, arguments);

        this.name = cfg.name;
        this.namespace = cfg.namespace;
        this.fields = cfg.fields;
        this.isAML2 = cfg.isAML2 === undefined ? false : cfg.isAML2;
        this.isAML3 = cfg.isAML3 === undefined ? true : cfg.isAML3;
        this.dictionary = cfg.dict;
        this.utilClasses = cfg.utilClasses;
    }
}

export class PanelCfg {
    static ctrParams = ['panelId', 'storeId', 'showBreadcrumbs', 'showBookmarks', 'canClose']
    constructor() {
        const cfg = polymorphCfg(this, arguments);

        this.panelId = '' + cfg.panelId;
        this.storeId = '' + cfg.storeId;
        this.showBreadcrumbs = cfg.showBreadcrumbs;
        this.showBookmarks = cfg.showBookmarks;
        this.canClose = cfg.canClose;
        this.searchText =  '';
    }
}

export class StoreCfg {
    static ctrParams = ['storeId', 'filename', 'title', 'readonly', 'date', 'amlVersion', 'bookmarks', 'copyFields']
    constructor() {
        const cfg = polymorphCfg(this, arguments);

        this.storeId = '' + cfg.storeId;
        this.filename = cfg.filename;
        this.title = cfg.title;
        this.readonly = cfg.readonly;
        this.date = cfg.date;
        this.amlVersion = cfg.amlVersion;
        this.loaded = false;
        this.bookmarks = cfg.bookmarks || [];
        this.copyFields = cfg.copyFields || {
            sourcePath: undefined,
            destinationPath: undefined
        }
    }
}

export class AutoCompleteOption {
    static ctrParams = ['value', 'help', 'isTemplate', 'brace']
    constructor() {
        const cfg = polymorphCfg(this, arguments);
        this.value = '' + cfg.value;
        this.help = cfg.help; // HTML-formated help string
        this.isTemplate = cfg.isTemplate || false;
        this.brace = cfg.brace || null;
    }
}
