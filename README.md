# FEUP-SDIS-Proj2

## General notes

> The Chord protocol we implemented assumes that no two nodes in the network have the same
chord id. No checking is ever performed on this end to ensure that new nodes have
free chord ids. If at any point in time, two nodes in the network have the same chord id,
the behaviour is undefined.

## Chord

All chord scripts should run in the project's main directory.

For testing Chord in the local network we have `config.sh`, `launch.sh` and `extract.sh`.

The first script, `config.sh`, sets the network's configuration variables (`m`, local address `addr`, etc.).
All tests run in the same networks, and nodes take different ports for their server sockets.

The second script, `launch.sh`, will first establish a stable start node on port `29500` (node 0).
This node creates the network using the `create` command.

Then the script will launch 30 more nodes: 1 (on port `29501`), 2, ... up to 30.
Many nodes will be launched concurrently, in *groups*, resulting in a lot of concurrent joins.
Nodes will only join nodes which have been launched in a previous *group*.

Due to the nature of the recursive implementation of `lookup`, the new node's initial connection
is usually not the sought successor of the new node. Because the Chord network is highly
unstable after a large batch of joins, it might be impossible for the initial
connection to resolve the lookup, in which case the request is **dropped**. This means
the new node failed to join the network. After a short, variable timeout, the node will try
joining the network again, in a time where it will be presumably more stable.

After the script is done waiting, the directory `log/` will contain the logfiles for each
of the peers, organized by server port (`29500+`). These will usually contain finger table
dumps if they have been enabled in `ChordLogger.java`. To collect the final finger tables
of the nodes, run `extract.sh` which will populate a file called `tables` with the last
finger table dump in each logfile. Then you can compare with the expected finger tables.

## Distributed Backup System
