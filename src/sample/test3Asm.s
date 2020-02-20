 .org $0000
screen = $0200

; .org $0300
; .text "If you are viewing this, you are very very very gay kk"

 .org $0400
 .text "Also, áàãâäéèêëíìîïóòôöõúùûüçý¡²³¤€¼½¾‘’¥×äåé®þüúíóöáßðø¶´«»¬æ©ñç¿"

 .org $0600

res:
 ldx #0

loop:
 lda message,x
 beq loop_end
 sta screen,x
 inx
 bra loop

loop_end:
 stp

message:
 .string "Hello, world!"

 .org $FFFA
 .word res, res, res