grammar wresl;

options { caseInsensitive = true; }

// Entry point for parsing Main File
mainStart
    : ( initial
      | group
      | sequence
      | model
      | include
      )+ EOF
    ;
    
// Entry point for parsing Include File
includeStart
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
      | external
      | alias
      | timeSeries
      )* EOF
    ;


// SEQUENCE
sequence
    : SEQUENCE OBJECT_NAME OPEN_BRACE sequenceBody CLOSE_BRACE
    ;
sequenceBody
    : MODEL OBJECT_NAME sequenceCondition? ORDER INT timestepSpecification?
    ;
sequenceCondition: CONDITION expression ;
timestepSpecification: TIMESTEP (STEP_1MON | STEP_1DAY) ;


// MODEL
model: MODEL OBJECT_NAME OPEN_BRACE modelBody+ CLOSE_BRACE ;
modelBody
    : include
    | svar
    | dvar
    | external
    | alias
    | timeSeries
    | goal
    | objective
    | ifStatement
    ;


// INCLUDE
include: INCLUDE scope? includeBody ;
includeBody
    : groupReference       # IncludeGroup
    | modelReference       # IncludeModel
    | specificationString  # IncludeFile
    ;
groupReference: GROUP OBJECT_NAME ;
modelReference: MODEL OBJECT_NAME ;


// GOAL
goal: GOAL scope? arraySizeDefinition? OBJECT_NAME OPEN_BRACE goalBody CLOSE_BRACE ;
goalBody
    : goalShortForm
    | goalViaPenalty
    | goalViaCase
    ;
goalShortForm: expression opCompare expression ;
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
    | svar
    | dvar
    | external
    | alias
    | timeSeries
    | goal
    | objective
    | ifStatement
    ;


// DEFINE (SVAR or DVAR)
dvar: (DVAR | DEFINE) scope? arraySizeDefinition? OBJECT_NAME OPEN_BRACE defineBoundLimits definitionSpecifics+ CLOSE_BRACE ;
svar: (SVAR | DEFINE) scope? arraySizeDefinition? OBJECT_NAME OPEN_BRACE svarBody CLOSE_BRACE ;
svarBody
    : caseStatement+                                         #svarCase
    | select                                                 #svarLookup
    | sumExpressionBody                                      #svarSum
    | VALUE expression                                       #svarValue
    ;

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
    | NOSOLVER
    ;
optionalBPart: specificationString ;
kind: KIND specificationString ;
units: UNITS specificationString ;
convert: CONVERT specificationString ;


// ALIAS
alias
    : DEFINE scope? arraySizeDefinition? OBJECT_NAME OPEN_BRACE ALIAS expression (kind | units)* CLOSE_BRACE   
    | ALIAS  scope? arraySizeDefinition? OBJECT_NAME OPEN_BRACE       expression (kind | units)* CLOSE_BRACE   
    ;


// EXTERNAL
external: DEFINE scope? OBJECT_NAME OPEN_BRACE EXTERNAL externalTarget CLOSE_BRACE ;
externalTarget
    : specificationString
    | unescapedTargetString
    ;
unescapedTargetString: (OBJECT_NAME ('.' OBJECT_NAME)?);


// TIMESERIES
// 1 kind and 1 unit must exist; 1 convert and 1 optionalBPart are optional; compliance will be checked in Visitors
timeSeries
    : TIMESERIES OBJECT_NAME OPEN_BRACE            (kind | units | convert)+                 CLOSE_BRACE  #timeSeriesTypeTS
    | DEFINE     OBJECT_NAME OPEN_BRACE TIMESERIES (optionalBPart | kind | units | convert)+ CLOSE_BRACE  #timeSeriesTypeDef
    ;


// INITIAL
initial: INITIAL OPEN_BRACE svar+ CLOSE_BRACE ;


// IF STATEMENT
ifStatement: ifClause elseIfClause* elseClause? ;
ifClause: IF expression ifBlock ;
elseIfClause: ELSEIF expression ifBlock ;
elseClause: ELSE ifBlock ;
ifBlock: OPEN_BRACE (include | svar | dvar | external | alias | timeSeries | external | goal | objective)+ CLOSE_BRACE ;


// CASE STATEMENT
caseStatement: CASE caseName OPEN_BRACE caseCondition? caseBody CLOSE_BRACE ;
caseName
    : OBJECT_NAME
    | nonReservedKeywords
    ;
caseCondition: CONDITION (expression | ALWAYS) ;
caseBody
    : VALUE expression          #caseViaValue       
    | SIDE expression penalty*  #caseViaGoal           
    | select                    #caseViaSelect      
    | expression                #caseViaExpression  
    ;


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
    : expression opCompare expression                                       #expressionComparison
    | expression opMultiplicationDivision expression                        #expressionMultDiv
    | expression opAdditionSubtraction expression                           #expressionAddSub
    | NOT expression                                                        #expressionNot
    | (PLUS | MINUS) expression                                             #expressionSigned // +1, or -1 without a left hand side
    | sumExpressionBody                                                     #expressionSum
    | (preDefinedFunction | OBJECT_NAME) OPEN_PAREN arguments? CLOSE_PAREN  #expressionCall
    | expression COLON expression                                           #expressionSlice
    | variableReference                                                     #expressionReference
    | OPEN_PAREN expression CLOSE_PAREN                                     #expressionParen
    ;

sumExpressionBody
    : SUM OPEN_PAREN OBJECT_NAME EQUALS_SIGN sumBegin COMMA sumEnd (COMMA sumStep)? CLOSE_PAREN accumulatingExpression;
accumulatingExpression: expression ;
sumBegin: expression;
sumEnd: expression ;
sumStep: expression ;

variableReference
    : OBJECT_NAME scope? timestepOffset? #objectReference
    | DAYSIN                             #daysInMonthReference
    | CURRENT_MONTH                      #currentMonthReference
    | WATER_YEAR                         #waterYearReference
    | MONTH                              #monthReference
    | FUTURE_ARRAY_MAXIMUM               #arrayMaximumReference
    | DOUBLE                             #doubleNumber
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

timestepOffset: OPEN_PAREN (expression) CLOSE_PAREN ;
scope: OPEN_BRACKET scopeBody CLOSE_BRACKET ;
scopeBody
    : GLOBAL
    | LOCAL
    | expression
    ;
arraySizeDefinition: OPEN_PAREN expression CLOSE_PAREN ;

// common parser groups
opCompare
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
opAdditionSubtraction
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
      'jan'    | 'prevjan'
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
FUTURE_ARRAY_MAXIMUM: '$m' ;
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
NOSOLVER: 'nosolver';
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
DOUBLE
    : DIGITS? '.' DIGITS? ([e] [+-]? DIGITS)?
    ;
INT: DIGITS ;  // references fragment only
fragment DIGITS: [0-9]+ ;

// Identifiers
OBJECT_NAME: [A-Z] [A-Z0-9_]* ;

// Comments and whitespace
WRESL_COMMENT: '!' ~[\r\n]* -> skip ;
CPP_COMMENT: '//' ~[\r\n]* -> skip ;
C_COMMENT: '/*' .*? '*/' -> skip ;
WS: [ \t\r\n]+ -> skip ;
