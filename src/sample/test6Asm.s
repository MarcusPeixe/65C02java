 .org 0
 .org $0600

res:
 lda #$69
 ; test3 = loop.test2

loop:
test1 = .test + $0fff
.test = $fa
;.test2 = <test3
 sta .test,x
 inx
 bne loop

 bra .end

.end:
 stp

 .org $fffa
 .word 0, res, 0