#!/usr/bin/env sh
# To render all svgs, you'll need the svg-to-android tool
# You can install this tool with the command `npm install -g svg-to-android`
# Obviously you'll need npm installed to install that, too.
svg-to-android -o ../app/src/main/res/ -D mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi *.svg
