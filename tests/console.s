ptr	=	$00		; HI-LO zp addresses
char	=	$02		; cursor char
key 	=	$ff
screen	=	$0400
size	=	$0400
end	=	screen + size - 1
	.org	$0800

res:	sei
	lda	#<screen
	stz	ptr
	lda	#>screen
	sta	ptr + 1
	lda	#'|'
	sta	char
	ldx	#0

cls:	stz	screen + $000,x
	stz	screen + $100,x
	stz	screen + $200,x
	stz	screen + $300,x
	inx
	bne	cls

new_key:
	lda	char
	sta	(ptr)

	wai
	lda	key
	stz	key

	beq	blink

	cmp	#'\n'		; newline
	beq	newline

	cmp	#'\b'		; backspace
	beq	backspace

letter:
	sta	(ptr)

	lda	ptr + 1 	; boundary check
	cmp	#>end
	bne	.inc_cur
	lda	ptr
	cmp	#<end
	beq	new_key

.inc_cur:
	inc	ptr
	bne	2
	inc	ptr + 1

	bra	new_key

newline:
	lda	ptr + 1 	; boundary check
	cmp	#>end
	bne	.nl
	lda	ptr
	cmp	#%1110_0000
	bcs	new_key

.nl:	lda	#' '
	sta	(ptr)

.loop:	inc	ptr
	bne	2
	inc	ptr + 1

	lda	ptr
	bit	#%00011111
	bne	.loop

	bra	new_key

backspace:
	lda	#'\0'
	sta	(ptr)

	lda	ptr + 1 	; boundary check
	cmp	#>screen
	bne	.loop
	lda	ptr
	cmp	#$00
	beq	new_key

.loop:	lda	ptr
	bne	2
	dec	ptr + 1
	dec	ptr

	lda	(ptr)
	beq	.loop

	bra	new_key

blink:	lda	char
	eor	# '|' ^ ' '
	sta	char
	sta	(ptr)
	bra	new_key

irq:	
nmi:	rti		; trap

	.org	$fffa
	.word	nmi, res, irq