#!/bin/bash -e
# Copyright (C) 2014 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

V=$(git describe | sed 's/^v//')
sed -i "0,/<version>/s/<version>[^<]*<\/version>/<version>$V<\/version>/" pom.xml
mvn clean && mvn package -DskipTests -Dmaven.javadoc.skip=true

cat <<EOF


Set version to $V

To run tests:
  mvn package -Dmaven.javadoc.skip=true
To deploy a snapshot:'
  mvn clean && mvn deploy -DskipTests -Dmaven.javadoc.skip=true
EOF
