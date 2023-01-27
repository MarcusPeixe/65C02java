; Simple screen test

screen	=	$0400
code	=	$0200

	.org	code

res:	ldx	#0

.lp:	txa
	sta	screen, x
	inx
	bne	.lp

	stp

	.org	$fffc
	.word	res