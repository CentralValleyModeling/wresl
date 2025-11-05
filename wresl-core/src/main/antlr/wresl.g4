grammar wresl;

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
    : 'sequence' OBJECT_NAME '{' sequence_body? '}'
    ;
sequence_body
    : 'model' OBJECT_NAME condition? 'order' INT timestep_specification?
    ;
condition: 'condition' expression ;
timestep_specification: 'timestep' STEP ;

// MODEL
model: 'model' OBJECT_NAME '{' model_body+ '}' ;
model_body: include | define | goal | objective | if_statement ;

// INCLUDE
include: 'include' scope? include_body ;
include_body: group_reference | PATH ;
group_reference: 'group' OBJECT_NAME ;

// GOAL
goal: 'goal' scope? array_size_definition? OBJECT_NAME '{' goal_body '}' ;
goal_body: goal_short_form | goal_via_penalty | goal_via_case ;
goal_short_form: expression OP_COMP expression ;
goal_via_penalty: SIDE expression SIDE expression penalty* ;
goal_via_case: SIDE expression case+ ;

// OBJECTIVE
objective: 'objective' OBJECT_NAME '=' '{' objective_body '}' ;
objective_body: weights_by_pair | common_weights ;
weights_by_pair: var_weight_pair (','? var_weight_pair)* ','? ;
var_weight_pair: '[' expression ',' expression ']' ;
common_weights: weight variables ;
weight: 'weight' expression ;
variables: 'variable' expression+ ;

// GROUP
group: 'group' OBJECT_NAME '{' group_body+ '}' ;
group_body: include | define | goal | objective | if_statement ;

// DEFINE
define: dvar | svar ;
dvar: ('dvar' | 'define') scope? array_size_definition? OBJECT_NAME '{' dvar_body '}' ;
svar: ('svar' | 'define') scope? array_size_definition? OBJECT_NAME '{' svar_body '}' ;
dvar_body: define_via_bounds | define_via_alias ;
svar_body: define_via_value | define_via_case | define_via_lookup | define_via_external | define_via_timeseries | define_via_sum ;
define_via_value: 'value' expression ;
define_via_case: case+ ;
define_via_lookup: select ;
define_via_external: 'external' SPECIFICATION_STRING ;
define_via_bounds: define_bound_limits definition_specifics+ ;
define_via_timeseries: 'timeseries' ('\'' OBJECT_NAME '\'')? definition_specifics+ ;
define_via_alias: 'alias' expression definition_specifics* ;
define_via_sum: sum_expression ;

define_bound_limits: define_type_int? (define_bound_std | define_bound_ul) ;
define_type_int: 'integer' ;
define_bound_std: 'std' ;
define_bound_ul: BOUND_SIDE expression (BOUND_SIDE expression)? ;

definition_specifics: kind | units | convert ;
kind: 'kind' SPECIFICATION_STRING ;
units: 'units' SPECIFICATION_STRING ;
convert: 'convert' SPECIFICATION_STRING ;

// INITIAL
initial: 'initial' '{' svar+ '}' ;

// IF STATEMENT
if_statement: if_clause else_if_clause* else_clause? ;
if_clause: 'if' expression if_block ;
else_if_clause: 'elseif' expression if_block ;
else_clause: 'else' if_block ;
if_block: '{' (include | define)+ '}' ;

// CASE
case: 'case' OBJECT_NAME '{' case_condition? case_body '}' ;
case_condition: 'condition' expression ;
case_body: case_via_value | goal_case | case_via_select | case_via_expression ;
case_via_value: 'value' expression ;
case_via_expression: expression ;
goal_case: SIDE expression penalty* ;
case_via_select: select ;

// PENALTY
penalty: SIDE ('<' | '>') SIDE penalty_value? ;
penalty_value: CONSTANT | ('penalty' expression) ;

// SELECT
select: 'select' OBJECT_NAME 'from' OBJECT_NAME given? use? where? ;
given: 'given' OBJECT_NAME '=' expression ;
use: 'use' INTERPOLATION ;
where: 'where' OBJECT_NAME '=' expression (',' OBJECT_NAME '=' expression)* ;

// EXPRESSION
assignment: expression OP_ASSIGNMENT expression ;

expression
    : additiveExpression
    ;

additiveExpression
    : multiplicativeExpression (('+'|'-') multiplicativeExpression)*
    ;

multiplicativeExpression
    : unaryExpression (('*'|'/') unaryExpression)*
    ;

unaryExpression
    : OP_UNARY unaryExpression
    | primary
    ;

primary
    : call
    | object_reference
    | sum_expression
    | paren
    ;

call: called_obj '(' arguments? ')' ;
called_obj: F_UNARY | F_BINARY | F_ITER | variable_reference ;
arguments: expression (',' expression)* ;
paren: '(' expression ')' ;

sum_expression
    : 'sum' '(' OBJECT_NAME '=' expression ',' sum_end (',' sum_step)? ')' accumulating_expression
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
array_size_definition: '(' object_reference ')' ;

// -----------------------------
// Lexer rules
// -----------------------------

// Keywords and intrinsic words (placed before OBJECT_NAME so they match first)
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

OP_BINARY: '+' | '-' | '*' | '/' ;
OP_COMP: '<=' | '>=' | '==' | '<' | '>' | '.and.' | '.or.' | '.ne.' ;
OP_UNARY: '.not.' | '-' | '+' ;
OP_ASSIGNMENT: '=' ;

// Scope tokens
SCOPE_SPEC: '[' SCOPE ']' ;
SCOPE: 'global' | 'local' | OBJECT_NAME | SIGNED_INT ;

// Name, path, and strings
PATH: '\'' (~['\r\n])+ '\'' ;
SPECIFICATION_STRING
    : '\'' (~['\r\n])* '\''
    | '"'  (~["\r\n])* '"'
    ;

// Numbers
SIGNED_INT: ('+' | '-')? INT ;
SIGNED_NUMBER
    : ('+'|'-')? ( DIGITS ('.' DIGITS)? | '.' DIGITS ) ( [eE] [+-]? DIGITS )?
    ;

// Simple INT and digits
INT: DIGITS ;
fragment DIGITS: [0-9]+ ;

// OBJECT_NAME (identifier)
OBJECT_NAME: LETTER WORD* ;

// COMMENTS and WHITESPACE
WRESL_COMMENT: '!' ~[\r\n]* -> skip ;
CPP_COMMENT: '//' ~[\r\n]* -> skip ;
C_COMMENT: '/*' .*? '*/' -> skip ;
WS: [ \t\r\n]+ -> skip ;

// Misc tokens used in grammar
SLASH : [\\/] ;
VALID_WINDOWS_NAME: ~["*'<>\\|/:?]+ ; // used only if needed

// fragments for clarity
fragment LETTER: [A-Za-z] ;
fragment WORD: [A-Za-z0-9_] ;
