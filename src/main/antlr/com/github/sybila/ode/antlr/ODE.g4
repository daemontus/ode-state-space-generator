
grammar ODE;

@header {
package com.github.sybila.ode.antlr;
}

root : fullStop? (line fullStop)*;

line : 'EQ:' NAME '=' expr                           # equation
     | 'PARAMS:' paramInterval (';' paramInterval)*  # parameters
     | 'VARS:' varName (',' varName)*                # variables
     | 'THRES:' NAME ':' NUMBER (',' NUMBER)*        # thresholds
     | 'CONSTS:' (constant ';')* constant            # constants
     | 'INIT:' initInterval (';' initInterval)*      # initial
     | BA_LINE                                       # buchi
     | 'VAR_POINTS:' varInterval (';' varInterval)*  # variablePoints
     ;

fullStop : NEWLINE+ | EOF;

constant : NAME ',' NUMBER;

varName : NAME;

initInterval : NAME ':' NUMBER ',' NUMBER;
varInterval : NAME ':' NUMBER ',' NUMBER;
paramInterval : NAME ',' NUMBER ',' NUMBER;

expr : eval                 # evaluable
     | '-' eval             # negativeEvaluable
     | '(' expr ')'         # parenthesis
     | expr '*' expr        # multiplication
     | expr '-' expr        # subtraction
     | expr '+' expr        # addition
     ;

eval : NUMBER       # numberEval
     | NAME         # nameEval
     | ramp         # rampEval
     | sigm         # sigmoidEval
     | step         # stepEval
     | hill         # hillEval
     | approx       # approxEval
	 | sin			# sineEval
	 | pow			# powEval
	 | monod   # monodEval
	 | moser    # moserEval
	 | tessier  # tessierEval
	 | haldane  # haldaneEval
	 | aiba     # aibaEval
	 | tessiert # tessierTypeEval
	 | andrews  # andrewsEval
     ;

/** Lexer rules **/

arg : NAME | NUMBER;

ramp : RAMP'('NAME','arg','arg','arg','arg')';

step : STEP'('NAME','arg','arg','arg')';

hill : HILL'('NAME','arg','arg','arg','arg')';

sigm : SIGM'('NAME','arg','arg','arg','arg')';

approx : APPROX'('NAME')''('(pair',')* pair')';

sin : SIN'('NAME')';

pow : POW'('NAME','arg')';

monod : MONOD'('NAME','arg','arg')';

moser : MOSER'('NAME','arg','arg')';

tessier : TESSIER'('NAME','arg')';

haldane : HALDANE'('NAME','arg','arg')';

aiba : AIBA'('NAME','arg','arg')';

tessiert : TESSIERT'('NAME','arg','arg')';

andrews : ANDREWS'('NAME','arg','arg')';


pair : '['NUMBER','NUMBER']';

RAMP : [Rr][mp]('coor')?;
STEP : [Hh][mp];
HILL : [Hh]'ill'[mp];
SIGM : [Ss][mp]('inv')?;
APPROX : [Aa]'pprox';
SIN : [Ss]'in';
POW : [Pp]'ow';
MONOD : [Mm]'onod';
MOSER  : [Mm]'oser';
TESSIER : [Tt]'essier';
HALDANE : [Hh]'aldane';
AIBA : [Aa]'iba';
TESSIERT : [Tt]'essier_type';
ANDREWS : [Aa]'ndrews';

BA_LINE : 'BA:'~[\n]+;

NUMBER : [-]?[0-9]+('.'[0-9]+)?;
NAME : [a-zA-Z]+[_0-9a-zA-Z~{}]*;

WS : [ \t]+ -> channel(HIDDEN) ;

NEWLINE : '\r'?'\n';

Block_comment : '/*' (Block_comment|.)*? '*/' -> channel(HIDDEN) ; // nesting allow
C_Line_comment : '//' ~('\n'|'\r')* -> channel(HIDDEN) ;
Python_Line_comment : '#' ~('\n'|'\r')* -> channel(HIDDEN) ;

//legacy rules - must be first, because they override char_string
SYSTEM : 'system'(~[;])*';' -> channel(HIDDEN);
//This does not work, and I am not sure why wee need it any more,
//so unless you find a use for this, ignore.
//PROCESS: 'process'.*?'{'((~[{}])|[\r\n]|('{'(~[{}])*'}')*)*'}' -> channel(HIDDEN);