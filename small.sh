#!/bin/bash

source config.sh

echo "Waiting for 29500..."
read
(java -cp bin dbs.TestApp 29500 BACKUP ex/file01.txt 07 && echo "01 R07 @ 29500") &

echo "Waiting for 29501..."
read
(java -cp bin dbs.TestApp 29501 BACKUP ex/file02.txt 16 && echo "02 R16 @ 29501") &

echo "Waiting for 29502..."
read
(java -cp bin dbs.TestApp 29503 BACKUP ex/file03.txt 24 && echo "03 R24 @ 29503") &

echo "Waiting for 29505..."
read
(java -cp bin dbs.TestApp 29505 BACKUP ex/file04.txt 09 && echo "03 R16 @ 29505") &

echo "Waiting for 29506..."
read
(java -cp bin dbs.TestApp 29507 BACKUP ex/file05.txt 02 && echo "05 R02 @ 29507") &
(java -cp bin dbs.TestApp 29510 BACKUP ex/file06.txt 99 && echo "06 R99 @ 29510") &

echo "Waiting for 29512..."
read
(java -cp bin dbs.TestApp 29512 BACKUP ex/file07.txt 24 && echo "07 R24 @ 29512") &
(java -cp bin dbs.TestApp 29516 BACKUP ex/file08.txt 11 && echo "08 R11 @ 29516") &

echo "Waiting for 29517..."
read
(java -cp bin dbs.TestApp 29518 BACKUP ex/file09.txt 01 && echo "09 R01 @ 29518") &

echo "Waiting for 29520..."
read
(java -cp bin dbs.TestApp 29521 BACKUP ex/file10.txt 19 && echo "10 R19 @ 29521") &
(java -cp bin dbs.TestApp 29523 BACKUP ex/file11.txt 14 && echo "11 R14 @ 29523") &
(java -cp bin dbs.TestApp 29526 BACKUP ex/file12.txt 44 && echo "12 R44 @ 29526") &

echo "Waiting for 29530..."
read
(java -cp bin dbs.TestApp 29530 BACKUP ex/file13.txt 32 && echo "13 R32 @ 29530") &
