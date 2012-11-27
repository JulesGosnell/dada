DADA
====

I've tested building on various i386 and x86_64 Java/Linux platforms.

## build DADA
git clone https://github.com/JulesGosnell/dada.git
cd dada
mvn clean install

## (in one shell) start DADA Whales demo
cd dada/dada-demo
../bin/clj
(load-file "src/main/clojure/org/dada/demo/whales.clj")

## (in another shell) start DADA Client GUI
cd dada/dada-swt
../bin/clj

cd ..
./bin/client

You should now be looking at DADA's MetaModel - it's Model of Models.
Click on Whales
 - The Whales model should open
 - This demo imitates a live feed of whale sightings
 - A whale's attributes may vary from sighting to sighting
Click on Oceans
 - The Oceans model should open
 - This model imitates a live feed of ocean samplings
 - An ocean's attributes may vary from sample to sample
Click on WhalesAndOceans
 - This model demonstrates DADA's ability to join to live streams
 - Each Whales is shown joined to its corresponding Ocean
 - Whale and Ocean attribute changes are reflected in realtime in WhalesAndOceans
