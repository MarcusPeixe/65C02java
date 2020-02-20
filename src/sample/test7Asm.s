; program start
.org $600

; constants
screen = $200

res:
 .message = message2
 lda #<.message
 sta puts.ptr
 lda #>.message
 sta puts.ptr + 1
 jsr puts
stp


puts:
 .ptr = $0000
 
 lda #0
 ldy #0
 .loop:
  lda (<.ptr),y
  beq .loop_end
  sta screen,y
  iny
 bra .loop
 .loop_end:
rts

message1:
.string "Hello, world!"

message2:
.string "2nd string"

.org $fffa .word 0, res, 0