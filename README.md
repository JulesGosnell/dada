DADA
====

I've tested building on various i386 and x86_64 Java/Linux
platforms. You will need a recent JDK and Maven (and probably
JAVA_HOME and MAVEN_HOME set).

For a demo of DADA please follow the instructions below. The demo
domain is whale sightings (I am fascinated by whales), however DADA is
generic enough to slice, dice and aggregate pretty much anything in
realtime. At the bank at which I currently work, it is being used by
Treasury to project 6 month cash trade settlement ladders and
highlight late amendments.

The DADA maven website may be found here: http://ouroboros.dyndns-free.com/ci/job/dada/site/?

DADA is rebuilt on a matrix of platforms by Jenkins on each checkin: http://ouroboros.dyndns-free.com/ci/job/dada-matrix/

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


![Alt text](https://raw.github.com/JulesGosnell/dada/master/images/MetaModel.gif)


Click on Whales


![Alt text](https://raw.github.com/JulesGosnell/dada/master/images/Whales.gif)


 - The Whales model should open
 - This demo imitates a live feed of whale sightings
 - A whale's attributes (except type) may vary from sighting to sighting

Click on Oceans


![Alt text](https://raw.github.com/JulesGosnell/dada/master/images/Oceans.gif)


 - The Oceans model should open
 - This model imitates a live feed of ocean samplings
 - An ocean's attributes may vary from sample to sample

Click on WhalesAndOceans


![Alt text](https://raw.github.com/JulesGosnell/dada/master/images/WhalesAndOceans.gif)


 - The WhalesAndOceans model is derived from the Whales and Oceans models by a query.
 - This model demonstrates DADA's ability to join live streams in real time
 - Each Whale is shown joined to its current Ocean (which may change over time)
 - Whale and Ocean attribute changes are reflected in realtime in WhalesAndOceans

## In DADA Whales Demo Server shell
<pre>
;; select namespace
(ns org.dada.demo.whales)
;; run query
(? (dunion)(dsum :weight)(dsplit :ocean)(dfrom "Whales"))
</pre>

 - You should see a number of new Models leap into existence in the MetaModel
 - These models are derived from the inital Whales and Oceans models via the query above
 - Intermediate models are maintained so that they may also be viewed


![Alt text](https://raw.github.com/JulesGosnell/dada/master/images/MetaModel2.gif)


Click on Whales:ocean.sum(:weight).union()


![Alt text](https://raw.github.com/JulesGosnell/dada/master/images/WhaleWeightSummedByOcean.gif)


 - This Model carries the ultimate result of the query you have just entered
 - The Whales have been grouped by Ocean and the weight of Whales in each Ocean summed up.
 - As the weights and ocean attributes of Whales change over time, so does this model

Click on Whales.split(:ocean=arctic)


![Alt text](https://raw.github.com/JulesGosnell/dada/master/images/ArcticWhales.gif)


 - This is a intermediate result contributing to the ultimate result above.
 - It represents the real time set of all Whales currently in the Arctic.

Click on Whales.split(:ocean=arctic).sum(:weight)


![Alt text](https://raw.github.com/JulesGosnell/dada/master/images/ActicWhalesTotalWeight.gif)


 - This is a intermediate result drawing upon another intermediate result and contributing to the ultimate result above.
 - It represents the real time sum of the weight of all Whales currently in the Arctic.

If you like what you see, get in touch and I will write some more doc :-)

DADA is a POC, allowing me to learn about Event Stream Processing in
Clojure from the inside, not (yet) production software.

Clojure is an ideal platform on which to implement ESP - A functional
approach, sequence comprehension, simple concurrency, persistant
containers and immutable record types.

The only serious change that I had to make to Clojure was enabling the
recording of dynamically created classes on the server and their
on-demand loading into a client. This allows me to create models
containing new types and functions server-side, and then view them in
a running client.
