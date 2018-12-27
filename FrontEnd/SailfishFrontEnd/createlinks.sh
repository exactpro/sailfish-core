#*******************************************************************************
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
#*******************************************************************************
cd ./WebContent/cfg
rm -rf data* dictionaries* actions.xml data.xml descriptor.xml dictionaries.xml environment.xml fixService.properties hibernate.cfg.xml script.xml validators.xml
mkdir data
mkdir dictionaries
ln -s ../../../../BackEnd/TestTools/src/main/resources/cfg/* ./
ln -s ../../../../BackEnd/TestTools/environment.xml ./
ln -s ../../../../BackEnd/TestTools/script.xml ./
ln -s ../../../../BackEnd/TestTools/src/main/etc/hibernate.cfg.xml ./
cd dictionaries/
ln -s ../../../../../BackEnd/TestTools/src/main/resources/com/exactpro/sf/configuration/dictionaries/* ./
ln -s ../../../../../BackEnd/TestTools/src/gen/resources/com/exactpro/sf/configuration/dictionaries/* ./
cd ../data
ln -s ../../../../../BackEnd/TestTools/src/main/data/* ./
