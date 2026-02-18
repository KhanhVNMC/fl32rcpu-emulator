.define HELLO       16
.define BALL        #HELLO * 2
.define ZERO        0
.define STR         "Stringareno"

.define SIZE 4
.undef SIZE 
.define SIZE 8

@data
value    .word #SIZE
ballsack .asciz #STR

@text
LD RBX, $value
LD RAX, $ballsack[#ZERO + 1] 
KILL
