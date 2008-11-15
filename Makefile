# JSON-RPC for Google Web Toolkit
#
# Copyright 2008 Google Inc.
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
#
# Define GWT_SDK to the location of the Google Web Toolkit SDK.
#
# Define GWT_OS to your operating system, e.g. 'linux', 'mac'.
#

uname_S := $(shell sh -c 'uname -s 2>/dev/null || echo not')

JAVAC      = javac
GWT_OS     = unknown

ifeq ($(uname_S),Darwin)
	GWT_OS = mac
endif
ifeq ($(uname_S),Linux)
	GWT_OS = linux
endif
ifeq ($(uname_S),Cygwin)
	GWT_OS = win
endif

-include config.mak

GWT_CP     = $(GWT_SDK)/gwt-user.jar:$(GWT_SDK)/gwt-dev-$(GWT_OS).jar
MY_JAR     = lib/gwtjsonrpc.jar
MY_JAVA    = $(shell find src -name \*.java)
MY_GWT_XML = com/google/gwtjsonrpc/GWTJSONRPC.gwt.xml

all: $(MY_JAR)

clean:
	rm -f $(MY_JAR)
	rm -rf classes .bin

$(MY_JAR): $(MY_JAVA) src/$(MY_GWT_XML)
	rm -rf .bin
	mkdir .bin
	cd src && $(JAVAC) \
		-encoding utf-8 \
		-source 1.5 \
		-target 1.5 \
		-g \
		-cp $(GWT_CP):../lib/gson.jar:../lib/commons-codec.jar \
		-d ../.bin \
		$(patsubst src/%,%,$(MY_JAVA))
	cd .bin && jar cf ../$(MY_JAR) .
	cd src && jar uf ../$(MY_JAR) .
	rm -rf .bin

.PHONY: all
.PHONY: clean
