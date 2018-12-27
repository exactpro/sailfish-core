#!/usr/bin/env bash

# Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# this script will automatically build this project for you
# it downloads ruby+sass and all dependencies
#
# You can use webpack and compass directly in your front-end development work (it works faster)

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

export JRUBY_PATH=${DIR}/build/jruby
export GEM_HOME=${DIR}/build/gem
export GEM_PATH=${GEM_HOME}
export PATH=${DIR}/scripts:${GEM_HOME}/bin:$PATH

./gradlew $* -PuseJRuby -PuseNodejsPlugin

exit 0
