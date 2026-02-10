ADD rax, RAX, RBX
@text
    main:
    LD RAX, $var2
    LD RBX, $var3[0]
    ADD rax, RAX, RBX
    TEST
@data
string .half 1, 2, 's'
var2 .space(1024)
var3 .word 1024