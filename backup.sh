#!/bin/bash

source config.sh

(java -cp bin dbs.TestApp 29500 BACKUP ex/file01.txt 27 && echo "01 R27 @ 29500") &
(java -cp bin dbs.TestApp 29517 BACKUP ex/file02.txt 34 && echo "02 R34 @ 29517") &
(java -cp bin dbs.TestApp 29507 BACKUP ex/file03.txt 16 && echo "03 R16 @ 29507") &
(java -cp bin dbs.TestApp 29504 BACKUP ex/file04.txt 02 && echo "04 R02 @ 29504") &
(java -cp bin dbs.TestApp 29525 BACKUP ex/file05.txt 78 && echo "05 R78 @ 29525") &
(java -cp bin dbs.TestApp 29516 BACKUP ex/file06.txt 14 && echo "06 R14 @ 29516") &
(java -cp bin dbs.TestApp 29503 BACKUP ex/file07.txt 13 && echo "07 R13 @ 29503") &
(java -cp bin dbs.TestApp 29502 BACKUP ex/file08.txt 01 && echo "08 R01 @ 29502") &
(java -cp bin dbs.TestApp 29509 BACKUP ex/file09.txt 04 && echo "09 R04 @ 29509") &
(java -cp bin dbs.TestApp 29530 BACKUP ex/file10.txt 19 && echo "10 R19 @ 29530") &
