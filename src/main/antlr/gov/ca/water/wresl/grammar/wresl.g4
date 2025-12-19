grammar wresl;

options { caseInsensitive = true; }

start
    : ( initial
      | ifStatement
      | model
      | sequence
      | include
      | goal
      | objective
      | group
      | svar
      | dvar
      )* EOF
    ;

// SEQUENCE
sequence
    : SEQUENCE OBJECT_NAME OPEN_BRACE sequenceBody? CLOSE_BRACE
    ;
sequenceBody
    : MODEL OBJECT_NAME sequenceCondition? ORDER INT timestepSpecification?
    ;
sequenceCondition: CONDITION expression opComp expression ;
timestepSpecification: TIMESTEP STEP ;

// MODEL
model: MODEL OBJECT_NAME OPEN_BRACE modelBody+ CLOSE_BRACE ;
modelBody: include | svar | dvar | goal | objective | ifStatement ;

// INCLUDE
include: INCLUDE scope? includeBody ;
includeBody
    : groupReference       # IncludeGroup
    | SPECIFICATION_STRING # IncludeFile
    ;
groupReference: GROUP OBJECT_NAME ;

// GOAL
goal: GOAL scope? arraySizeDefinition? OBJECT_NAME OPEN_BRACE goalBody CLOSE_BRACE ;
goalBody: goalShortForm | goalViaPenalty | goalViaCase ;
goalShortForm: expression opComp expression ;
goalViaPenalty: SIDE expression SIDE expression penalty* ;
goalViaCase: SIDE expression caseStatement+ ;

// OBJECTIVE
objective: OBJECTIVE OBJECT_NAME EQUALS_SIGN OPEN_BRACE objectiveBody CLOSE_BRACE ;
objectiveBody: weightsByPair | commonWeights ;
weightsByPair: varWeightPair (COMMA? varWeightPair)* COMMA? ;
varWeightPair: OPEN_BRACKET expression COMMA expression CLOSE_BRACKET ;
commonWeights: weight variables ;
weight: WEIGHT expression ;
variables: VARIABLE expression+ ;

// GROUP
group: GROUP OBJECT_NAME OPEN_BRACE groupBody+ CLOSE_BRACE ;
groupBody: include | dvar | svar | goal | objective | ifStatement ;

// DEFINE
dvar: (DVAR | DEFINE) scope? arraySizeDefinition? OBJECT_NAME OPEN_BRACE dvarBody CLOSE_BRACE ;
svar: (SVAR | DEFINE) scope? arraySizeDefinition? OBJECT_NAME OPEN_BRACE svarBody CLOSE_BRACE ;
dvarBody: defineViaBounds | defineViaAlias ;
svarBody: delayedSvarBody | immediateSvarBody ;
delayedSvarBody: defineViaCase | defineViaLookup | defineViaExternal | defineViaSum ;
immediateSvarBody: defineViaValue | defineViaTimeseries ;
defineViaValue: VALUE expression ;
defineViaCase: caseStatement+ ;
defineViaLookup: select ;
defineViaExternal: EXTERNAL SPECIFICATION_STRING ;
defineViaBounds: defineBoundLimits definitionSpecifics+ ;
defineViaTimeseries: TIMESERIES (SPECIFICATION_STRING)? definitionSpecifics+ ;
defineViaAlias: ALIAS expression definitionSpecifics* ;
defineViaSum: sumExpression ;

defineBoundLimits: INTEGER? (STD | defineBoundUl) ;
defineBoundUl: BOUND_SIDE expression (BOUND_SIDE expression)? ;

definitionSpecifics: kind | units | convert ;
kind: KIND SPECIFICATION_STRING ;
units: UNITS SPECIFICATION_STRING ;
convert: CONVERT SPECIFICATION_STRING ;

// INITIAL
initial: INITIAL OPEN_BRACE svar+ CLOSE_BRACE ;

// IF STATEMENT
ifStatement: ifClause elseIfClause* elseClause? ;
ifClause: IF expression ifBlock ;
elseIfClause: ELSEIF expression ifBlock ;
elseClause: ELSE ifBlock ;
ifBlock: OPEN_BRACE (include | svar | dvar)+ CLOSE_BRACE ;

// caseStatement
caseStatement: CASE OBJECT_NAME OPEN_BRACE caseCondition? caseBody CLOSE_BRACE ;
caseCondition: CONDITION (expression | ALWAYS) ;
caseBody: caseViaValue | goalCase | caseViaSelect | caseViaExpression ;
caseViaValue: VALUE expression ;
caseViaExpression: expression ;
goalCase: SIDE expression penalty* ;
caseViaSelect: select ;

// PENALTY
penalty: SIDE OP_COMP_LIMITED SIDE penaltyValue? ;
penaltyValue: CONSTANT | (PENALTY expression) ;

// SELECT
select: SELECT OBJECT_NAME FROM OBJECT_NAME given? use? where? ;
given: GIVEN OBJECT_NAME EQUALS_SIGN expression ;
use: USE interpolation ;
where: WHERE OBJECT_NAME EQUALS_SIGN expression (COMMA OBJECT_NAME EQUALS_SIGN expression)* ;

// -----------------------------
// Expressions (unified)
// -----------------------------

expression
    : expression opComp expression
    | expression OP_MULT expression
    | expression OP_ADD expression
    | OP_UNARY expression
    | primaryExpression
    ;

primaryExpression
    : paren
    | sumExpression
    | call
    | variableReference
    | RUNTIME
    | MONTH
    | prev_month
    | CONSTANT
    | FUTURE_ARRAY_MAXMUM
    | SIGNED_FLOAT
    | SIGNED_INT
    | INT
    ;

// Parentheses
paren: OPEN_PAREN expression CLOSE_PAREN ;

// Function calls
call
    : (F_UNARY | F_BINARY | F_ITER) OPEN_PAREN arguments? CLOSE_PAREN
    ;

arguments
    : expression (COMMA expression)*
    ;

// Sum expression
sumExpression
    : SUM OPEN_PAREN OBJECT_NAME EQUALS_SIGN expression COMMA sumEnd (COMMA sumStep)? CLOSE_PAREN accumulatingExpression
    ;

accumulatingExpression: expression ;
sumEnd: expression ;
sumStep: expression ;

// Variable references
variableReference
    : OBJECT_NAME scope? tsRef?
    ;
tsRef
    : OPEN_PAREN (SIGNED_INT | OBJECT_NAME) CLOSE_PAREN
    ;
scope: SCOPE_SPEC ;
arraySizeDefinition: OPEN_PAREN objectReference CLOSE_PAREN ;

// Object references
objectReference
    : RUNTIME
    | MONTH
    | prev_month
    | CONSTANT
    | FUTURE_ARRAY_MAXMUM
    | SIGNED_FLOAT
    | variableReference
    ;

// simple parser rules
prev_month: 'prev' MONTH;
interpolation: 'linear' | MIN_MAX | 'maximum' | 'minimum' ;
opComp: EQUALS_SIGN | '<=' | '>=' | '==' | OP_COMP_LIMITED | '.and.' | '.or.' | '.ne.' ;

// -----------------------------
// Lexer rules
// -----------------------------

// scope
SCOPE_SPEC: OPEN_BRACKET ('global' | 'local' | OBJECT_NAME | SIGNED_INT) CLOSE_BRACKET ;

// Keywords
MODEL: 'model';
SEQUENCE: 'sequence';
ORDER: 'order';
INCLUDE: 'include';
GROUP: 'group';
INITIAL: 'initial';
GOAL: 'goal';
OBJECTIVE: 'objective';
WEIGHT: 'weight';
DEFINE: 'define' ;
DVAR: 'dvar';
SVAR: 'svar';
VALUE: 'value';
EXTERNAL: 'external';
TIMESERIES: 'timeseries';
TIMESTEP: 'timestep';
ALIAS: 'alias';
INTEGER: 'integer';
STD: 'std';
KIND: 'kind';
UNITS: 'units';
CONVERT: 'convert';
CASE: 'case';
CONDITION: 'condition';
IF: 'if';
ELSEIF: 'elseif';
ELSE: 'else';
SELECT: 'select';
GIVEN: 'given';
USE: 'use';
WHERE: 'where';
FROM: 'from';
SUM: 'sum';
PENALTY: 'penalty';
VARIABLE: 'variable';
ALWAYS: 'always';

// Braces, parentheses, operators, punctuation
OPEN_BRACE: '{';
CLOSE_BRACE: '}';
OPEN_PAREN: '(';
CLOSE_PAREN: ')';
OPEN_BRACKET: '[';
CLOSE_BRACKET: ']';
EQUALS_SIGN: '=';
COMMA: ',';

// Intrinsic functions and constants
RUNTIME: 'daysin' | 'month' | 'wateryear' ;
MONTH: 'jan' | 'feb' | 'mar' | 'apr' | 'may' | 'jun' | 'jul' | 'aug' | 'sep' | 'oct' | 'nov' | 'dec' ;
F_UNARY: 'abs' | 'int' | 'real' | 'exp' | 'log' | 'log10' | 'sqrt' | 'round' ;
F_BINARY: 'pow' | 'mod' ;
F_ITER: MIN_MAX | 'range' ;
CONSTANT: 'never' | 'unbounded' | 'constrain' ;
STEP: '1mon' | '1day' ;
SIDE: [lr] 'hs' ;
BOUND_SIDE: 'upper' | 'lower' ;
FUTURE_ARRAY_MAXMUM: '$' 'm' ;
MIN_MAX: 'max' | 'min';

// Operators
OP_ADD: '+' | '-';
OP_MULT: '*' | '/';
OP_COMP_LIMITED: '<' | '>';
OP_UNARY: '.not.' | '-' | '+' ;

// Quoted strings
SPECIFICATION_STRING
    : '\'' STRING_BODY '\''
    | '"'  STRING_BODY '"'
    ;
fragment STRING_BODY: ~['"\r\n]+ ;

// Numbers
INT: DIGITS ;  // references fragment only
SIGNED_INT: ('+' | '-')? INT ;
SIGNED_FLOAT
    : ('+'|'-')? ( DIGITS ('.' DIGITS)?) ( [e] [+-]? DIGITS )?
    ;
fragment DIGITS: [0-9]+ ;

// Identifiers
OBJECT_NAME: [A-Z] [A-Z0-9_]* ;

// Comments and whitespace
WRESL_COMMENT: '!' ~[\r\n]* -> skip ;
CPP_COMMENT: '//' ~[\r\n]* -> skip ;
C_COMMENT: '/*' .*? '*/' -> skip ;
WS: [ \t\r\n]+ -> skip ;
