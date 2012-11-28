DADA
====

I've tested building on various i386 and x86_64 Java/Linux platforms.

## Build DADA
<pre>
git clone https://github.com/JulesGosnell/dada.git
cd dada
mvn clean install
</pre>

## In one shell: Start DADA Whales Demo Server
<pre>
cd dada/dada-demo
../bin/clj
(load-file "src/main/clojure/org/dada/demo/whales.clj")
</pre>

## In another shell: Start DADA Client GUI
<pre>
cd dada/dada-swt
../bin/clj
(System/exit 0)
cd ..
./bin/client
</pre>

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

 - This model demonstrates DADA's ability to join live streams
 - Each Whale is shown joined to its corresponding Ocean
 - Whale and Ocean attribute changes are reflected in realtime in WhalesAndOceans

If you like what you see, get in touch and I will write some more doc :-)

DADA is a POC, allowing me to learn about Event Stream Processing in
Clojure from the inside, not (yet) production software.

Clojure is an ideal platform on which to implement ESP - A functional
approach, sequence comprehension, simple concurrency, persistant
containers and immutable record types.

The only serious change that I had to make to Clojure was enabling the
recording of dynamically created classes on the server and their
on-demand loading into a client. This allows me to create models
containing new types server-side, and then view them in a running
client.
