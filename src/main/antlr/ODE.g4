
grammar ODE;

root : (line '\n')*;

line :  //empty
    | 'EQ:' NAME '=' expr
    | 'PARAMS:' p_interval (';' p_interval)*
    | 'VARS:' NAME (',' NAME)*
    | 'THRES:' NAME ':' NUMBER (',' NUMBER)*
    | 'CONSTS:' (constant ';')* constant
    | 'INIT:' interval (';' interval)*
    | BA_LINE
    | 'VAR_POINTS:' interval (';' interval)*;

constant : NAME ',' NUMBER;

interval : NAME ':' NUMBER ',' NUMBER;
p_interval : NAME ',' NUMBER ',' NUMBER;

expr : summant (('+'|'-') summant)*;

summant : eval ('*' eval)*;

eval : NUMBER | '-'? NAME
    | '-'? RAMP | '-'? SIGM | '-'? STEP | '-'? HILL;

/** Lexer rules **/

//make no mistake, we actually do not allow whitespace between arguemnts!

RAMP : [Rr][mp]('coor')?'('NAME','(NAME|NUMBER)','(NAME|NUMBER)','(NAME|NUMBER)','(NAME|NUMBER)')';

SIGM : [Ss][mp]('inv')*('['([0-9]+|NAME)']')*'('NAME','(NAME|NUMBER)','(NAME|NUMBER)','(NAME|NUMBER)','(NAME|NUMBER)')';

STEP : [Hh][mp]'('NAME','(NAME|NUMBER)','(NAME|NUMBER)','(NAME|NUMBER)')';

HILL : [Hh]'ill'[mp]'('NAME','(NAME|NUMBER)','(NAME|NUMBER)','(NAME|NUMBER)','(NAME|NUMBER)')';

BA_LINE : 'BA:'~[\n]+;

NUMBER : [-]?[0-9]+([\.][0-9]+)?;
NAME : [a-zA-Z]+[_0-9a-zA-Z~{}]*;

WS : [ \t\u]+ -> channel(HIDDEN) ;

Block_comment : '/*' (Block_comment|.)*? '*/' -> channel(HIDDEN) ; // nesting allow
Line_comment : ('//' | '#') .*? '\n' -> channel(HIDDEN) ;

//legacy rules - must be first, because they override char_string
SYSTEM : 'system'(~[;])*';' -> channel(HIDDEN);
//This does not work, and I am not sure why wee need it any more,
//so unless you find a use for this, ignore.
//PROCESS: 'process'.*?'{'((~[{}])|[\r\n]|('{'(~[{}])*'}')*)*'}' -> channel(HIDDEN);