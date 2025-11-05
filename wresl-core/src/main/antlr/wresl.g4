grammar wresl;

options { caseInsensitive = true; }

// -----------------------------
// Parser rules
// -----------------------------
start
    : ( initial
      | if_statement
      | model
      | sequence
      | include
      | goal
      | objective
      | group
      | define
      )* EOF
    ;

// SEQUENCE
sequence
    : SEQUENCE OBJECT_NAME OPEN_BRACE sequence_body? CLOSE_BRACE
    ;
sequence_body
    : MODEL OBJECT_NAME condition? ORDER INT timestep_specification?
    ;
condition: CONDITION expression OP_COMP expression ;
timestep_specification: TIMESTEP STEP ;

// MODEL
model: MODEL OBJECT_NAME OPEN_BRACE model_body+ CLOSE_BRACE ;
model_body: include | define | goal | objective | if_statement ;

// INCLUDE
include: INCLUDE scope? include_body ;
include_body: group_reference | PATH ;
group_reference: GROUP OBJECT_NAME ;

// GOAL
goal: GOAL scope? array_size_definition? OBJECT_NAME OPEN_BRACE goal_body CLOSE_BRACE ;
goal_body: goal_short_form | goal_via_penalty | goal_via_case ;
goal_short_form: expression OP_COMP expression ;
goal_via_penalty: SIDE expression SIDE expression penalty* ;
goal_via_case: SIDE expression case+ ;

// OBJECTIVE
objective: OBJECTIVE OBJECT_NAME EQUALS_SIGN OPEN_BRACE objective_body CLOSE_BRACE ;
objective_body: weights_by_pair | common_weights ;
weights_by_pair: var_weight_pair (COMMA? var_weight_pair)* COMMA? ;
var_weight_pair: OPEN_BRACKET expression COMMA expression CLOSE_BRACKET ;
common_weights: weight variables ;
weight: WEIGHT expression ;
variables: VARIABLE expression+ ;

// GROUP
group: GROUP OBJECT_NAME OPEN_BRACE group_body+ CLOSE_BRACE ;
group_body: include | define | goal | objective | if_statement ;

// DEFINE
define: dvar | svar ;
dvar: (DVAR | DEFINE) scope? array_size_definition? OBJECT_NAME OPEN_BRACE dvar_body CLOSE_BRACE ;
svar: (SVAR | DEFINE) scope? array_size_definition? OBJECT_NAME OPEN_BRACE svar_body CLOSE_BRACE ;
dvar_body: define_via_bounds | define_via_alias ;
svar_body: define_via_value | define_via_case | define_via_lookup | define_via_external | define_via_timeseries | define_via_sum ;
define_via_value: VALUE expression ;
define_via_case: case+ ;
define_via_lookup: select ;
define_via_external: EXTERNAL SPECIFICATION_STRING ;
define_via_bounds: define_bound_limits definition_specifics+ ;
define_via_timeseries: TIMESERIES ('\'' OBJECT_NAME '\'')? definition_specifics+ ;
define_via_alias: ALIAS expression definition_specifics* ;
define_via_sum: sum_expression ;

define_bound_limits: INT? (STD | define_bound_ul) ;
define_bound_ul: BOUND_SIDE expression (BOUND_SIDE expression)? ;

definition_specifics: kind | units | convert ;
kind: KIND SPECIFICATION_STRING ;
units: UNITS SPECIFICATION_STRING ;
convert: CONVERT SPECIFICATION_STRING ;

// INITIAL
initial: INITIAL OPEN_BRACE svar+ CLOSE_BRACE ;

// IF STATEMENT
if_statement: if_clause else_if_clause* else_clause? ;
if_clause: IF expression if_block ;
else_if_clause: ELSEIF expression if_block ;
else_clause: ELSE if_block ;
if_block: OPEN_BRACE (include | define)+ CLOSE_BRACE ;

// CASE
case: CASE OBJECT_NAME OPEN_BRACE case_condition? case_body CLOSE_BRACE ;
case_condition: CONDITION expression ;
case_body: case_via_value | goal_case | case_via_select | case_via_expression ;
case_via_value: VALUE expression ;
case_via_expression: expression ;
goal_case: SIDE expression penalty* ;
case_via_select: select ;

// PENALTY
penalty: SIDE OP_COMP_LIMITED SIDE penalty_value? ;
penalty_value: CONSTANT | (PENALTY expression) ;

// SELECT
select: SELECT OBJECT_NAME FROM OBJECT_NAME given? use? where? ;
given: GIVEN OBJECT_NAME EQUALS_SIGN expression ;
use: USE INTERPOLATION ;
where: WHERE OBJECT_NAME EQUALS_SIGN expression (COMMA OBJECT_NAME EQUALS_SIGN expression)* ;

// EXPRESSION
assignment: expression EQUALS_SIGN expression ;

expression
    : expression OP_COMP expression
    | paren
    | sum_expression
    | object_reference
    | call
    | OP_UNARY expression
    | expression OP_MULT expression
    | expression OP_ADD expression
    ;

call: called_obj OPEN_PAREN arguments? CLOSE_PAREN ;
called_obj: F_UNARY | F_BINARY | F_ITER | variable_reference ;
arguments: expression (COMMA expression)* ;
paren: OPEN_PAREN expression CLOSE_PAREN ;

sum_expression
    : SUM OPEN_PAREN OBJECT_NAME EQUALS_SIGN expression COMMA sum_end (COMMA sum_step)? CLOSE_PAREN accumulating_expression
    ;
accumulating_expression: expression ;
sum_end: expression ;
sum_step: expression ;

// objects
object_reference
    : RUNTIME
    | MONTH
    | PREV_MONTH
    | CONSTANT
    | NULL
    | FUTURE_ARRAY_MAXMUM
    | SIGNED_NUMBER
    | variable_reference
    ;

variable_reference: OBJECT_NAME scope? ;
scope: SCOPE_SPEC ;
array_size_definition: OPEN_PAREN object_reference CLOSE_PAREN ;

// -----------------------------
// Lexer rules
// -----------------------------
// Keywords and intrinsic words (placed before OBJECT_NAME so they match first)
MODEL: 'model';
SEQUENCE: 'sequence';
ORDER: 'order';
INCLUDE: 'include';
GROUP: 'group';
INITIAL: 'initial';
GOAL: 'goal';
OBJECTIVE: 'objective';
WEIGHT: 'weight';
VARIABLE: 'variable';
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
OPEN_BRACE: '{';
CLOSE_BRACE: '}';
OPEN_PAREN: '(';
CLOSE_PAREN: ')';
OPEN_BRACKET: '[';
CLOSE_BRACKET: ']';
EQUALS_SIGN: '=';
COMMA: ',';

// intrinsics
RUNTIME: 'daysin' | 'month' | 'wateryear' ;
MONTH: 'jan' | 'feb' | 'mar' | 'apr' | 'may' | 'jun' | 'jul' | 'aug' | 'sep' | 'oct' | 'nov' | 'dec' ;
PREV_MONTH: 'prev' MONTH;

F_UNARY: 'cfs_taf' | 'taf_cfs' | 'abs' | 'int' | 'real' | 'exp' | 'log' | 'log10' | 'sqrt' | 'round' ;
F_BINARY: 'pow' | 'mod' ;
F_ITER: 'max' | 'min' | 'range' ;

CONSTANT: 'never' | 'always' | 'unbounded' | 'constrain' ;
STEP: '1mon' | '1day' ;
SIDE: [lr] 'hs' ;            // lhs or rhs
INTERPOLATION: 'linear' | 'max' | 'min' | 'maximum' | 'minimum' ;
BOUND_SIDE: 'upper' | 'lower' ;
NULL: 'null' ;
FUTURE_ARRAY_MAXMUM: '$' 'm' ;

OP_BINARY: OP_ADD | OP_MULT ;
OP_ADD: '+' | '-';
OP_MULT: '*' | '/';
OP_COMP: '<=' | '>=' | '==' | OP_COMP_LIMITED | '.and.' | '.or.' | '.ne.' ;
OP_COMP_LIMITED: '<' | '>';
OP_UNARY: '.not.' | '-' | '+' ;

// Scope tokens
SCOPE_SPEC: OPEN_BRACKET ('global' | 'local' | OBJECT_NAME | SIGNED_INT) CLOSE_BRACKET ;

// Name, path, and strings
PATH: '\'' (~['\r\n])+ '\'' ;
SPECIFICATION_STRING
    : '\'' (~['\r\n])* '\''
    | '"'  (~["\r\n])* '"'
    ;

// Numbers
INT: DIGITS ;
SIGNED_INT: ('+' | '-')? INT ;
SIGNED_NUMBER
    : ('+'|'-')? ( DIGITS ('.' DIGITS)? | '.' DIGITS ) ( [e] [+-]? DIGITS )?
    ;

// Simple INT and digits
fragment DIGITS: [0-9]+ ;

// OBJECT_NAME (identifier)
OBJECT_NAME: [A-Z] [A-Z0-9_]* ;

// COMMENTS and WHITESPACE
WRESL_COMMENT: '!' ~[\r\n]* -> skip ;
CPP_COMMENT: '//' ~[\r\n]* -> skip ;
C_COMMENT: '/*' .*? '*/' -> skip ;
WS: [ \t\r\n]+ -> skip ;