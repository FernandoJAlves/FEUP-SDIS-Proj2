# Chord specification

## Definitions

---

The **Chord** keeps keys of length $m$ bits, and can hold at most $2^m$ keys.
The number $m$ must be set at **Chord** construction time and cannot be changed.

    Chord:
        m: int = length of keys, in bits
        2^m: BigInteger = number of keys. all key arithmetic is done modulo 2^m

Each *node* $n$ is represented in a **NodeInfo** struct, which needs to
get passed to other peers so they know how to communicate with it and identify it.

    NodeInfo:
        chordid: BigInteger
        ip address of SSLServerSocket: InetAddress
        port number of SSLServerSocket: int

The **Finger Table**, $F$ for short, of node $n$ is $n.finger$.
The element at position $i$ in the finger table of node $n$ is $n.finger[$i$]$.
Each entry of the finger table is a NodeInfo, so F is mostly a **NodeInfo[]** of size m.

    FingerTable:
        node: NodeInfo = whose node is this finger table
        predecessor: NodeInfo = the predecessor of node
        finger: NodeInfo[m] = the array of successors
            0: successor(n + 2^0)  --> immediate successor
            1: successor(n + 2^1)
            2: successor(n + 2^2)
            3: successor(n + 2^3)
            ...
          m-1: successor(n + 2^(m-1))

As per the article, $n.finger[i]=successor(n + 2^i), i = 0,1,...,m-1$.

## Observations

---

A **chain** of communications occurs when a node **n_0** makes a request to node **n1**,
and then **n1** in return makes a request to node **n2**, and then **n2** makes a request
to a node **n3**, etc, until a final node **nR**, the *responsible node*, is found. The
`i` in **ni** is the index of the *node* in the **chain**, not its `chordid`.

Whenever a *node* **nA** needs to communicate with another *node* **nB**, it needs to
know the other node's metadata **NodeInfo**, so the metadata needs to be passed around
in all communication chains. We'll represent each node's metadata in a Java class
called **NodeInfo**. It is merely a serializable data structure, with no behaviours and
no references to any other class in the codebase.

## Protocol Messages

    LOOKUP(k, n0)

## Lookup algorithm

---

### Paper lookup algorithms

> **find_successor**(`id`): finds the immediate predecessor node of the desired
> identifier: the successor of that node must be the successor of the identifier.
>
> **find_predecessor**(`id`): contacts a series of nodes moving forward around the Chord
> circle towards `id`. If the node `n` contacts `n'` such that `id` falls between `n'` and
> the successor of `n'`, then `n'` is the searched for node.
>
> It is guaranteed that **successor**($k$) is responsible for $k$.

    n.find_successor(id):
        n' = n.find_predecessor(id)           Node n executes find_predecessor on id.
        get n'.successor                      Node n' returns its successor to node n.

    Must return: Chord id, IP address, port

    n.find_predecessor(id):
        n' = n
        while (id not in [n', n'.successor[)
            n'' = n'.closest_preceding_finger(id)   Ask n' to find the highest node n''
            n' = n''                                which is before id in its finger
                                                    table, and we continue on n''.
        return n'

    Must return: Chord id, IP address, port

    n.closest_preceding_finger(id):
        for i = m down to 1
            if (finger[i].chordid in [n, id[)       Will be the highest
                return finger[i].chordid
        return n

    Must return: Chord id, IP address, port

### Implementation: $lookup(k, n_0)$ algorithm, the core communication function

---

A node $n_0$ wants to know the node responsible for key $k$, which we'll call $n_R$,
for whatever purpose, and perform some action on said $k$ or on $n_R$. The design is:

The method

    Promise<NodeInfo> lookup(k, n0)

is the primary interface method of **Chord**, and returns a **Promise<NodeInfo>** which
eventually resolves to a **NodeInfo** containing the identification of the node
responsible for the requested key $k$. The resolution process may repeat itself if there
are serious communication problems present in the network.

Since the node $n_R$ had to create a socket stream to talk to $n_0$, then in the set of
open socket streams there should be one from $n_R$ already open and ready to receive
commands from $n_0$.

### Request algorithm: sending a $lookup(k, n_0)$ request

First $n_0$ checks whether he is the actual responsible for key $k$,
**and in that case returns himself, $n_0$, with no hassles**.

Otherwise, $n_0$ runs through its own finger table *F0* in reverse until he finds the
highest `n1` whose chordid is not after $k$ (modulo `2^m`).

Then $n_0$ asks `n1` through its opened *SSLSocket* stream the question and `n1`
will be responsible for finding the responsible node $n_R$ of $k$, as detailed below.
The node $n_0$ can expect a response in either one of its already open socket streams,
or in a new one that will be opened by $n_R$.

### Response algorithm: answering a received $lookup(k, n_0)$

Our node $n_A$ receives a request $lookup(k, n_0)$.

First $n_A$ checks whether it is the actual responsible for key $k$. In that case,
it sends its NodeInfo struct to the peer $n_0$, through an existing socket if possible,
or a newly created one.

Otherwise, $n_A$ runs through its own finger table $n_A.finger$ **in reverse** until it
finds the highest $n_B$ whose *chordid* is not larger than $k$ (modulo `2^m`).

Then $n_A$ asks $n_B$ through its opened *SSLSocket* stream the question and $n_B$
will be responsible for finding the responsible node $n_R$ of $k$, and having it answer
the originating node $n_0$.

## Node Joins

---

> **Chord** needs to preserve two invariants:
>
> 1. Each node's successor is correcly maintained.
> 2. For every key $k$, node $successor(k)$ is responsible for $k$.
>
> Theorem. Any node joining or leaving an N-node Chord network will use
> $O(log^2N)$ to re-establish the Chord routing invariants and finger tables.
>
> Chord must perfom three tasks when a node $n$ joins the network:
>
> 1. Initialize the predecessor and fingers of node $n$.
> 2. Update the fingers and predecessors of existing nodes to reflect the addition of $n$.
> 3. Notify the higher layer software so that it can transfer state associated with keys
>    that node $n$ is now responsible for. This means, in practice, it must receive the
>    files from its successor.
>
> Assume a new node, $n_0$, joins the network by communicating with an existing node $E$.
> The very first thing to do is ask $E$ to locate $successor(n_0)$, so that $n_0$ knows
> who its successor will be. Call this $successor(n_0)$ $n_1$.
>
> 1. **Initializing fingers and predecessor**
>   Node $n_0$ inherits its predecessor from $n_1$ and learns its fingers by asking $n_1$
> to compute them for him. This is faster than making a $lookup()$ for each $i=0,...,m-1$
> because $n_0$ will inherit many of the fingers from $n_1$ as well.
>   Practical optimization: $n_0$ asks $n_1$ for its complete finger table, and then does
> the calculations himself.
>
> 2. **Update fingers of existing nodes**
>   Node $n_0$ will need to be entered into the finger tables of some existing nodes.
>
> 3. **Transferring keys**
>   This is straightforward: node $n_0$ asks $n_1$ to send it all content associated with
> the keys it has taken responsibility for.

### Paper join algorithms

---

    n0.join():                      n0 is the first node in the network
        for i = 0 to m - 1
            n0.finger[i] = n0
        n0.finger.predecessor = n0

    n0.join(n'):                    n0 joins the network, n' is arbitrary
        n1 = n'.lookup(n0, n0)      ask n' to find n0's successor, implicitly asking n1
                                        if n0 can become its predecessor. assume yes
        n0.init_finger_table(n1)
        n0.update_others()
        n0.move_keys(n1)

    n0.init_finger_table(n1):       n1 is n0's successor, who has accepted n0 as its new
        n0.finger[0] = n1                                                    predecessor
        n0.finger.predecessor = n1.finger.predecessor
        n1.finger.predecessor = n0
        for i = 1 to m - 1
            if n0 + 2^i in (n0, n0.finger[i-1].node)
                n0.finger[i] = n0.finger[i-1]               good optimization
            else
                n0.finger[i] = n1.lookup(n0 + 2^i, n0)      possibly inherit finger[i]

    n0.update_others():
        for i = 0 to m - 1               find last node p whose ith finger might be n
            p = n0.predecessor(n0 - 2^i, n0)
            p.update_finger_table(n0, i)

    n.update_finger_table(s, i):
        if s in (n, finger[i])
            finger[i] = s
            p = predecessor              get first node preceding n
            p.update_finger_table(s, i)

### Implementation

---

Joining comprises of several steps. First, our node $n_0$ finds its successor $n_1$
by asking $n'$, its initial node. The socket is opened by $n_1$ and $n_0$ asks to be
$n_1$'s predecessor with a **JOIN** message. Assume a positive answer is given.
The answer contains n1's entire Finger Table $F_1$ for $n_0$ to use to insert itself
into the Chord.

So it goes: $n_0.finger[0] = n_1$. Afterwards, each $n_0.finger[i]$ is set to
$n_0.finger[i-1]$ if that is sufficiently large, and otherwise we have

$$n_0 < n_1 \leq n_0.finger[i-1] < n_0 + 2^i \leq successor(n_0 + 2^i) = n_0.finger[i]$$

so we set $n_0.finger[i] = n_0.finger[i-1].lookup(n_0 + 2^i, n_0)$ which opens the stream
to $n_0.finger[i]$. Nice. This is step **1**.

Then we copy $n_1$'s files over to $n_0$, for this we employ the **COPYKEYS** message.
This is step **2**.

Once all files have been copied, $n_1$ could dispose of them immediately, or wait for
some **FREEKEYS** message to arrive just a moment later.

Then $n_0$ can update
