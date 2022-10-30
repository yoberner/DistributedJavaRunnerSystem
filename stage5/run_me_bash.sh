#!/usr/bin/env bash

# Yonatan Berner

echo "Yonatan Berner stage5/run_me_bash.sh"

mvn clean package

javac src/main/java/edu/yu/cs/com3800/*.java
# javac src/main/java/edu/yu/cs/com3800/stage5/*.java


# javac $dirStr/*.java

# java $startServer 1 & # this is the gateway
# server1=$!
# java $startServer 2 &
# server2=$!
# java $startServer 3 &
# server3=$!
# java $startServer 4 &
# server4=$!
# java $startServer 5 &
# server5=$!
# java $startServer 6 &
# server6=$!
# java $startServer 7 &
# server7=$!
# java $startServer 8 &
# server8=$!

# sleep 5s

# gatewayUrl="http://localhost:9000"

# foundLeader=$(curl -s "$gatewayUrl/getLeader")