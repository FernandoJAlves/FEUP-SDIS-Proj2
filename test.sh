#!/bin/bash

timeout=3000; # Time before the tabs close

# Peers
gnome-terminal -x sh -c '((sleep \'$timeout' && kill -9 $$)&); 
                        java -cp bin dbs.Dbs create localhost 29500;
                        exec bash'
sleep 1s;

gnome-terminal -x sh -c '((sleep \'$timeout' && kill -9 $$)&); 
                        java -cp bin dbs.Dbs join localhost 29501 3840547707 localhost 29500;
                        exec bash'
sleep 1s;

gnome-terminal -x sh -c '((sleep \'$timeout' && kill -9 $$)&); 
                        java -cp bin dbs.Dbs join localhost 29502 3840547707 localhost 29500;
                        exec bash'                        
sleep 1s;


gnome-terminal -x sh -c '((sleep \'$timeout' && kill -9 $$)&);
                        sleep 1s;
                        echo java -cp bin dbs.TestApp 29501 BACKUP /Desktop/testD.txt 3
                        exec bash;'
