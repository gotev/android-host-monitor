#!/bin/bash -e
./gradlew clean javadoc
rm -rf ../javadoc
mv hostmonitor/build/docs/javadoc/ ../
git checkout gh-pages
rm -rf javadoc
mv ../javadoc .
cd javadoc
git add . --force
git commit -m "updated javadocs"
git push
cd ..
git checkout master
