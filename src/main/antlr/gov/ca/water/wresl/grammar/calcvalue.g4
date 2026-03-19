grammar calcvalue;

options { caseInsensitive = true; }

@header {
  package gov.ca.water.wrims.wresl.gramar;
    
  import org.antlr.runtime.ANTLRFileStream;
  import org.antlr.runtime.CharStream;
  import org.antlr.runtime.CommonTokenStream;
  import org.antlr.runtime.RecognitionException;
  import org.antlr.runtime.TokenStream;
  
  import java.util.HashMap;
  import gov.ca.water.wrims.engine.core.components.Error;
  import gov.ca.water.wrims.engine.core.components.IntDouble;
  import gov.ca.water.wrims.engine.core.parallel.ParallelVars;
}

@members {
  public IntDouble evalValue;
  public boolean evalCondition;
  public ParallelVars prvs;
  public Stack<LoopIndex> sumIndex= new Stack <LoopIndex>();
  
  @Override
  public void reportError(RecognitionException e) {
       Error.addEvaluationError(getErrorMessage(e, tokenNames));
  }
  
  public void setParallelVars (ParallelVars prvs1) {
       prvs=prvs1;
  }
  
  public void setSumIndex(Stack<LoopIndex> sumIndex){
      this.sumIndex=sumIndex;
  }
}

// ------------------------
// Parser Rules
// ------------------------
evaluator
    : expressionInput
    | conditionInput
    ;

expressionInput
    : 'v:' expressionCollection { evalValue = $expressionCollection.id; }
    ;

conditionInput
    : 'c:' conditionStatement { evalCondition = $conditionStatement.result; }
    ;

expressionCollection returns [IntDouble id]
    : expression { $id = $expression.id; }
    | tableSQL { $id = $tableSQL.id; }
    | timeseriesWithUnits
    | timeseries { $id = $timeseries.id; }
    | sumExpression { $id = $sumExpression.id; }
    | UPPERUNBOUNDED { $id = new IntDouble(1e38, true); }
    | LOWERUNBOUNDED { $id = new IntDouble(-1e38, true); }
    ;

sumExpression returns [IntDouble id]
@init { String s = ""; }
    : SUM '(' ident=IDENT { ValueEvaluation.sumExpression_IDENT($ident.text, sumIndex); }
          '=' e1=expression ';' e2=expression
          (';' (minus='-'? i=INTEGER { if ($minus != null) s += "-"; s += $i.text; }))?
      ')' e3=expression
      { $id = ValueEvaluation.sumExpression($e3.id, $e3.text, sumIndex); }
    ;

unary returns [IntDouble id]
    : (s=('+'|'-'))? term { $id = ValueEvaluation.unary($s != null ? $s.text : "+", $term.id); }
    ;

mult returns [IntDouble id]
    : u1=unary { $id = $u1.id; }
      (op=('*'|'/') u2=unary
       {
           if ($op.text.equals("*")) $id = ValueEvaluation.mult($id, $u2.id);
           else $id = ValueEvaluation.divide($id, $u2.id);
       }
      )*
    ;

add returns [IntDouble id]
    : m1=mult { $id = $m1.id; }
      (op=('+'|'-') m2=mult
       {
           if ($op.text.equals("+")) $id = ValueEvaluation.add($id, $m2.id);
           else $id = ValueEvaluation.substract($id, $m2.id);
       }
      )*
    ;

expression returns [IntDouble id]
    : i=add { $id = $i.id; }
    ;

// ------------------------
// Term Rules
// ------------------------
term returns [IntDouble id]
    : IDENT { $id = ValueEvaluation.term_IDENT($IDENT.text, sumIndex); }
    | INTEGER { $id = ValueEvaluation.term_INTEGER($INTEGER.text); }
    | FLOAT { $id = ValueEvaluation.term_FLOAT($FLOAT.text); }
    | '(' expression ')' { $id = $expression.id; }
    | func { $id = $func.id; }
    | tafcfs_term { $id = $tafcfs_term.id; }
    | YEAR { $id = ValueEvaluation.term_YEAR(); }
    | MONTH { $id = ValueEvaluation.term_MONTH(); }
    | DAY { $id = ValueEvaluation.term_DAY(); }
    | MONTH_CONST { $id = ValueEvaluation.term_MONTH_CONST($MONTH_CONST.text); }
    | PASTMONTH { $id = ValueEvaluation.term_PASTMONTH($PASTMONTH.text); }
    | DAYSIN { $id = ValueEvaluation.daysIn(); }
    | DAYSINTIMESTEP { $id = ValueEvaluation.daysInTimeStep(); }
    | SVAR { $id = ValueEvaluation.term_SVAR($SVAR.text.replace("{","").replace("}","")); }
    | ARRAY_ITERATOR { $id = ValueEvaluation.term_ARRAY_ITERATOR(prvs); }
    | '(' sumExpression ')' { $id = $sumExpression.id; }
    ;

// ------------------------
// Dummy Rules to fix undefined references
// ------------------------
tableSQL returns [IntDouble id] @init { $id = new IntDouble(0, true); } : /* dummy */ ;
timeseriesWithUnits : /* dummy */ ;
timeseries returns [IntDouble id] @init { $id = new IntDouble(0, true); } : /* dummy */ ;
tafcfs_term returns [IntDouble id] @init { $id = new IntDouble(0, true); } : TAFCFS '(' expression? ')' ;
max_func returns [IntDouble id] @init { $id = new IntDouble(0, true); } : MAX '(' expression ')' ;
min_func returns [IntDouble id] @init { $id = new IntDouble(0, true); } : MIN '(' expression ')' ;
relationRangeStatement returns [boolean result] @init { $result = true; } : '(' conditionStatement ')' ;

// ------------------------
// Function Rules (minimal for compilation)
// ------------------------
func returns [IntDouble id] : max_func | min_func | round_func ;
round_func returns [IntDouble id] : ROUND '(' e=expression ')' { $id = ValueEvaluation.round($e.id); } ;

// ------------------------
// Condition Rules
// ------------------------
conditionStatement returns [boolean result]
    : r=relationUnary { $result = $r.result; }
    | ALWAYS { $result = true; }
    ;

relationUnary returns [boolean result]
    : n=NOT? r=relationOr { $result = ($n==null) ? $r.result : !$r.result; }
    ;

relationOr returns [boolean result]
    : r1=relationAnd { $result = $r1.result; } 
      (OR r2=relationAnd { $result = ValueEvaluation.relationStatementSeries($result, $r2.result, ".or."); })*
    ;

relationAnd returns [boolean result]
    : r1=relationRangeStatement { $result = $r1.result; } 
      (AND r2=relationRangeStatement { $result = ValueEvaluation.relationStatementSeries($result, $r2.result, ".and."); })*
    ;

// ------------------------
// Lexer Rules
// ------------------------
INTEGER : [0-9]+;
FLOAT   : [0-9]* '.' [0-9]+ | [0-9]+ '.';
IDENT   : [a-zA-Z] [a-zA-Z0-9_]*;
SVAR    : '{' IDENT '}';
ARRAY_ITERATOR : '$m';

UPPERUNBOUNDED: 'upper_unbounded';
LOWERUNBOUNDED: 'lower_unbounded';
ALWAYS: 'always';

SUM: 'sum';
ROUND: 'round';
MAX: 'max';
MIN: 'min';
TAFCFS: 'taf_cfs' | 'cfs_taf' | 'cfs_af' | 'af_cfs';

// These were missing
YEAR : 'wateryear';
MONTH : 'month';
DAY : 'day';
MONTH_CONST : 'jan'|'feb'|'mar'|'apr'|'may'|'jun'|'jul'|'aug'|'sep'|'oct'|'nov'|'dec';
PASTMONTH : 'prevjan'|'prevfeb'|'prevmar'|'prevapr'|'prevmay'|'prevjun'|'prevjul'|'prevaug'|'prevsep'|'prevoct'|'prevnov'|'prevdec';
DAYSIN : 'daysin'|'daysinmonth';
DAYSINTIMESTEP : 'daysintimestep';

AND: '.and.';
OR: '.or.';
NOT: '.not.';

WS: [ \t\r\n]+ -> skip;
COMMENT: '!' ~[\r\n]* -> skip;