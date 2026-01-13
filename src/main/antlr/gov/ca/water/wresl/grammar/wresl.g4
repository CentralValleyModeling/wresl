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
timestepSpecification: TIMESTEP (STEP_1MON | STEP_1DAY) ;

// MODEL
model: MODEL OBJECT_NAME OPEN_BRACE modelBody+ CLOSE_BRACE ;
modelBody
    : include
    | svar
    | dvar
    | goal
    | objective
    | ifStatement
    ;

// INCLUDE
include: INCLUDE scope? includeBody ;
includeBody
    : groupReference       # IncludeGroup
    | specificationString  # IncludeFile
    | start                # IncludeAnotherTree  // this should never appear in a file, but keep this so ANTLR generates the class for us
    ;
groupReference: GROUP OBJECT_NAME  ;

// GOAL
goal: GOAL scope? arraySizeDefinition? OBJECT_NAME OPEN_BRACE goalBody CLOSE_BRACE ;
goalBody
    : goalShortForm
    | goalViaPenalty
    | goalViaCase
    ;
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
groupBody
    : include
    | dvar
    | svar
    | goal
    | objective
    | ifStatement
    ;

// DEFINE
dvar: (DVAR | DEFINE) scope? arraySizeDefinition? OBJECT_NAME OPEN_BRACE dvarBody CLOSE_BRACE ;
svar: (SVAR | DEFINE) scope? arraySizeDefinition? OBJECT_NAME OPEN_BRACE svarBody CLOSE_BRACE ;
dvarBody
    : defineBoundLimits definitionSpecifics+ #dvarBounds
    | ALIAS expression definitionSpecifics*  #dvarAlias
    ;
svarBody
    : caseStatement+                                         #svarCase
    | select                                                 #svarLookup
    | EXTERNAL externalTarget                                #svarExternal
    | sumExpressionBody                                      #svarSum
    | VALUE expression                                       #svarValue
    | TIMESERIES (specificationString)? definitionSpecifics+ #svarTimeseries
    ;

externalTarget
    : specificationString
    | unescapedTargetString
    ;
unescapedTargetString: (OBJECT_NAME ('.' OBJECT_NAME)?);

defineBoundLimits: INTEGER? (STD | defineBoundUl) ;
defineBoundUl: boundSide boundType (boundSide boundType)? ;
boundSide: UPPER | LOWER;
boundType
    : UNBOUNDED    #unbounded
    | expression   #expressionBounded
    ;

definitionSpecifics
    : kind
    | units
    | convert
    ;
kind: KIND specificationString ;
units: UNITS specificationString ;
convert: CONVERT specificationString ;

// INITIAL
initial: INITIAL OPEN_BRACE svar+ CLOSE_BRACE ;

// IF STATEMENT
ifStatement: ifClause elseIfClause* elseClause? ;
ifClause: IF expression ifBlock ;
elseIfClause: ELSEIF expression ifBlock ;
elseClause: ELSE ifBlock ;
ifBlock: OPEN_BRACE (include | svar | dvar)+ CLOSE_BRACE ;

// caseStatement
caseStatement: CASE caseName OPEN_BRACE caseCondition? caseBody CLOSE_BRACE ;
caseName
    : OBJECT_NAME
    | nonReservedKeywords
    ;
caseCondition: CONDITION (expression | ALWAYS) ;
caseBody
    : caseViaValue
    | goalCase
    | caseViaSelect
    | caseViaExpression
    ;
caseViaValue: VALUE expression ;
caseViaExpression: expression ;
goalCase: SIDE expression penalty* ;
caseViaSelect: select ;

// PENALTY
penalty: SIDE (LESS_THAN | GREATER_THAN) SIDE penaltyValue? ;
penaltyValue
    : NEVER
    | CONSTRAIN
    | (PENALTY expression)
    ;

// SELECT
select: SELECT columnName FROM OBJECT_NAME given? use? where? ;
given: GIVEN columnName EQUALS_SIGN expression ;
use: USE interpolation ;
where: WHERE columnName EQUALS_SIGN expression (COMMA columnName EQUALS_SIGN expression)* ;
interpolation
    : LINEAR
    | MIN
    | MAX
    | MAXIMUM
    | MINIMUM
    ;
columnName
    : OBJECT_NAME
    | nonReservedKeywords
    ;

// -----------------------------
// Expressions
// -----------------------------

expression
    : expression opComp expression                                          #comparsionExpression
    | expression opMultiplicationDivision expression                        #multDivExpression
    | expression opAdditionSubtration expression                            #addSubExpression
    | NOT expression                                                        #notExpression
    | opAdditionSubtration expression                                       #signedExpression // +1, or -1 without a left hand side
    | sumExpressionBody                                                     #sumExpression
    | (preDefinedFunction | OBJECT_NAME) OPEN_PAREN arguments? CLOSE_PAREN  #callExpression
    | expression COLON expression                                           #sliceExpression
    | variableReference                                                     #referenceExpression
    | OPEN_PAREN expression CLOSE_PAREN                                     #parenExpression
    ;

sumExpressionBody
    : SUM OPEN_PAREN OBJECT_NAME EQUALS_SIGN expression COMMA sumEnd (COMMA sumStep)? CLOSE_PAREN accumulatingExpression;

variableReference
    : OBJECT_NAME scope? timestepOffset? #objectReference
    | DAYSIN                             #daysInMonthReference
    | CURRENT_MONTH                      #currentMonthReference
    | WATER_YEAR                         #waterYearReference
    | MONTH                              #monthReference
    | FUTURE_ARRAY_MAXMUM                #arrayMaximumReference
    | FLOAT                              #floatNumber
    | INT                                #intNumber
    ;


// Function calls
preDefinedFunction
   : F_ABSOLUTE_VALUE
   | F_INTEGER
   | F_REAL
   | F_EXPONENTIAL
   | F_LOG_E
   | F_LOG_10
   | F_SQRT
   | F_ROUND
   | F_POWER
   | F_MODULUS
   | F_RANGE
   | MIN
   | MAX
   ;

arguments
    : expression (COMMA expression)*
    ;

accumulatingExpression: expression ;
sumEnd: expression ;
sumStep: expression ;

timestepOffset: OPEN_PAREN (expression) CLOSE_PAREN ;
scope: OPEN_BRACKET scopeBody CLOSE_BRACKET ;
scopeBody
    : GLOBAL
    | LOCAL
    | expression
    ;
arraySizeDefinition: OPEN_PAREN expression CLOSE_PAREN ;

// common parser groups
opComp
    : EQUALS_SIGN
    | GREATER_THAN
    | GREATER_THAN_OR_EQUAL
    | LESS_THAN
    | LESS_THAN_OR_EQUAL
    | DOUBLE_EQUAL
    | AND
    | OR
    | NOT_EQUAL
    ;
opMultiplicationDivision
    : MULT
    | DIVIDE
    ;
opAdditionSubtration
    : PLUS
    | MINUS
    ;
specificationString
    : SINGLE_QUOTE_STRING
    | DOUBLE_QUOTE_STRING
    ;
nonReservedKeywords
    : MONTH
    | CURRENT_MONTH
    | WATER_YEAR
    ;

// -----------------------------
// Lexer rules
// -----------------------------

// Keywords - Objects
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
// Keywords - constants
LOCAL: 'local';
GLOBAL: 'global';
ALWAYS: 'always';
DAYSIN: 'daysin';
CURRENT_MONTH: 'month';
WATER_YEAR: 'wateryear';
MONTH:
    'jan'      | 'prevjan'
    | 'feb'    | 'prevfeb'
    | 'mar'    | 'prevmar'
    | 'apr'    | 'prevapr'
    | 'may'    | 'prevmay'
    | 'jun'    | 'prevjun'
    | 'jul'    | 'prevjul'
    | 'aug'    | 'prevaug'
    | 'sep'    | 'prevsep'
    | 'oct'    | 'prevoct'
    | 'nov'    | 'prevnov'
    | 'dec'    | 'prevdec'
    ;
STEP_1MON: '1mon';
STEP_1DAY: '1day';
FUTURE_ARRAY_MAXMUM: '$m' ;
// Keywords - instructions
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
LINEAR: 'linear';
MAXIMUM: 'maximum';
MINIMUM: 'minimum';
NEVER: 'never';
UNBOUNDED: 'unbounded';
CONSTRAIN: 'constrain';
UPPER: 'upper';
LOWER: 'lower';
// Keywords - functions
F_RANGE: 'range';
F_ABSOLUTE_VALUE: 'abs';
F_INTEGER: 'int';
F_REAL: 'real';
F_EXPONENTIAL: 'exp';
F_LOG_E: 'log';
F_LOG_10: 'log10';
F_SQRT: 'sqrt';
F_ROUND: 'round';
F_POWER: 'pow';
F_MODULUS: 'mod';
MIN: 'min';
MAX: 'max';
// Keywords - operators (logic)
GREATER_THAN: '>';
LESS_THAN: '<';
GREATER_THAN_OR_EQUAL: '>=';
LESS_THAN_OR_EQUAL: '<=';
DOUBLE_EQUAL: '==';
NOT: '.not.';
AND: '.and.';
OR: '.or.';
NOT_EQUAL: '.ne.';
SIDE: [lr] 'hs' ;
// Keywords - operators (math)
PLUS: '+';
MINUS: '-';
MULT: '*';
DIVIDE: '/';
COLON: ':';

// Braces, parentheses, operators, punctuation
OPEN_BRACE: '{';
CLOSE_BRACE: '}';
OPEN_PAREN: '(';
CLOSE_PAREN: ')';
OPEN_BRACKET: '[';
CLOSE_BRACKET: ']';
EQUALS_SIGN: '=';
COMMA: ',';

// Quoted strings (we don't allow nesting, so this is not allowed: "they're")
SINGLE_QUOTE_STRING: '\'' STRING_BODY '\'';
DOUBLE_QUOTE_STRING:  '"'  STRING_BODY '"';
fragment STRING_BODY: ~['"\r\n]+ ;

// Numbers
INT: DIGITS ;  // references fragment only
FLOAT
    : DIGITS? '.' DIGITS? ([e] [+-]? DIGITS)?
    ;
fragment DIGITS: [0-9]+ ;

// Identifiers
OBJECT_NAME: [A-Z] [A-Z0-9_]* ;

// Comments and whitespace
WRESL_COMMENT: '!' ~[\r\n]* -> skip ;
CPP_COMMENT: '//' ~[\r\n]* -> skip ;
C_COMMENT: '/*' .*? '*/' -> skip ;
WS: [ \t\r\n]+ -> skip ;
