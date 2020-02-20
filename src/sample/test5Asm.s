 .org $0000
 .org $0600

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

res:
 lda #$3D
 sta $01

 lda #$01
 sta $02

 lda #(1 + 1)

 ldy #0

.hex_tostr:
 sta $00
 and #$F0

 ldx #4
 jsr shift_right

 tax
 lda digits,x
 sta $0221,y
 iny

 lda $00
 ldx #4
 jsr shift_left

 cpy #2
 bne .hex_tostr

 stp

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

digits:
 .text "0123456789ABCDEF"

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

shift_right:
 lsr
 dex
 bne shift_right

 cmp #0
 rts

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

shift_left:
 asl
 dex
 bne shift_left

 cmp #0
 rts

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

 .org $fffa
 .word 0, res, 0
