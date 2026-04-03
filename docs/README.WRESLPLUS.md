

# WRESL+ User Documentation

WRESL+ (Water Resources Engineering Scripting Language Plus) is a domain-specific language for water resources modeling, used within the WRIMS (Water Resources Integrated Modeling System). It enables users to define state variables, decision variables, goals, and optimization objectives for complex water resource simulations. WRESL+ is designed to support reproducible, modular, and transparent modeling for hydrologic and operations studies.


---


## Contents

- [Explanation](#explanation) – Background, concepts, and rationale
- [Tutorials](#tutorials) – Step-by-step lessons for beginners
- [Practical Guides](#practical-guides) – Recipes for common tasks and goals
- [Reference](#reference) – Technical details and valid syntax
- [Debugging Tips](#debugging-tips) – Advice for solving common issues
- [Contributing](#contributing-to-the-documentation) – How to help improve this documentation

---


## Explanation
*Understanding-oriented: background, concepts, and design rationale.*

This section introduces the core ideas behind WRESL+ to the readers to have a solid conceptual foundation about the LP model structure and utilized syntaxes.

### How WRESL+ based built models are passed to solvers?
At each simulation time step, WRESL+ defines a **linear programming (LP) problem**, a set of known inputs, unknown decision variables, and constraints, that a solver optimizes. The language cleanly separates three concerns:

| Concern | WRESL+ construct | Role | CalSim example |
|---|---|---|---|
| Known inputs | State Variables (SVAR) | Provide data to the solver | `I_SHSTA` (Shasta inflow) |
| Unknowns to solve | Decision Variables (DVAR) | Values determined by the solver | `C_SHSTA` (channel flow at Shasta) |
| Rules & objectives | Goals / Objectives | Constraints the solver must satisfy or optimize | Mass balance, minimum instream flow |

### CalSim Naming Conventions
Before diving into WRESL+ concepts, it helps to understand the standard variable naming conventions used in CalSim models:

| Prefix | Meaning | Example | Description |
|---|---|---|---|
| `I_` | Inflow | `I_SHSTA` | Sacramento River inflow to Shasta |
| `S_` | Storage | `S_SHSTA` | Shasta Reservoir storage |
| `C_` | Channel flow | `C_SHSTA` | Sacramento River flow at Shasta |
| `D_` | Diversion | `D_REDBLF` | Diversion at Red Bluff |
| `E_` | Evaporation | `E_SHSTA` | Evaporation from Shasta |
| `AD_` | Accretion/Depletion | `AD_RedBlf` | Accretion/Depletion at Red Bluff |

These naming conventions make models self-documenting and users can immediately tell what a variable represents.

### Key Concepts

#### State Variables (SVAR)
State variables represent known or precomputed values at each time step. They can be:
- **Fixed values** (`value`): constants or expressions computed from other variables
- **Time series** (`timeseries`): data read from HEC-DSS files (e.g., historical inflows like `I_SHSTA`)
- **Table lookups** (`select...from...where`): values interpolated from external data tables
- **Conditional** (`case`/`condition`): values that vary based on runtime conditions (e.g., wet vs. dry year operations)
- **Summations** (`sum`): aggregated values over an index range

SVARs are fully determined *before* the solver runs — they provide inputs and parameters to the optimization.

#### Decision Variables (DVAR)
Decision variables are the unknowns that the linear programming solver determines. They have:
- **Bounds** (`lower`/`upper`): feasible range for the variable
- **Standard form** (`std`): non-negative by default (lower=0, upper=unbounded)
- **Integer option** (`integer`): restricts the variable to integer values
- **Metadata** (`kind`/`units`): descriptive labels for output (e.g., `kind 'FLOW-CHANNEL'`, `units 'CFS'`)

The solver adjusts DVARs (e.g., `C_SHSTA`, `D_REDBLF`) to satisfy goals while optimizing the objective function.

#### Goals (Constraints)
Goals define linear constraints that the solver must satisfy or approximate. They can be:
- **Simple constraints**: equality (`=`) or inequality (`<`, `>`) between expressions (e.g., mass balance at a reservoir)
- **Penalty-based**: when a constraint cannot be strictly enforced, a penalty cost is applied for violation (e.g., relaxing minimum instream flow requirements)
- **Conditional**: different constraints apply under different conditions using `case`/`condition` (e.g., summer vs. winter operating rules)

#### Objectives
Objectives define the optimization direction by assigning weights to decision variables. The solver minimizes the total weighted cost, so higher-weighted violations are more strongly discouraged.

#### Constants and Aliases
- **Constants** (`const`): fixed numeric values that never change during a simulation (e.g., reservoir dead pool level).
- **Aliases** (`alias`): alternative names for computed expressions, primarily used to tag output for post-processing (e.g., defining a combined storage output `TRINITY_IMPORTDV`).

### Model Structure
A WRESL+ model is organized hierarchically. Here is an example based on CalSim conventions:

```
Main File (.wresl)
├── initial { ... }                   ! Pre-simulation setup
├── sequence ShastaOps { ... }        ! Defines solve order
├── sequence DeltaOps { ... }
├── model upstream {                  ! Grouped variables & goals
│   ├── include 'Shasta_storage.wresl'
│   ├── include 'Trinity_import.wresl'
│   └── include 'NOD_goals.wresl'
├── model downstream { ... }
└── group shared { ... }              ! Reusable definitions
```

1. **Sequences** define the order in which models are solved.
2. **Models** group related variables and goals into a solvable unit for one solver pass.
3. **Groups** organize shared definitions that can be included by multiple models.
4. **Initial blocks** set up variables and conditions evaluated once before the main simulation loop.
5. **Include files** modularize definitions across multiple `.wresl` files.

This structure supports **multi-cycle solving**, where results from an upstream model feed into downstream calculations (e.g., upstream reservoir releases feeding into downstream Delta operations).

### How the Solver Works (Conceptual)
At each time step, WRIMS:
1. Evaluates all SVARs (inputs, time series, table lookups, conditional values).
2. Constructs the LP problem from DVARs (unknowns) and goals (constraints).
3. Applies the objective function weights.
4. Solves the LP to determine optimal DVAR values.
5. Advances to the next time step and repeats.

Understanding this cycle is essential: SVARs must be resolvable *before* the solver runs, while DVARs are only determined *by* the solver.

### Design Philosophy
- **Declarative:** Users declare *what* to solve, not *how* to solve it — the optimizer handles the mechanics.
- **Modularity:** Models are composed from reusable `.wresl` files via `include`.
- **Reproducibility:** All logic is explicit, text-based, and version-controllable.
- **Extensibility:** External functions (DLLs), table lookups, and time series data can be integrated.
- **Case-insensitivity:** All keywords can be written in any case (`define`, `DEFINE`, `Define`).

### Common Pitfalls Explained
- **Missing `kind` and `units`:** DVAR and timeseries definitions require `kind` and `units` metadata. Omitting them causes parser errors.
- **Incorrect string quotes:** WRESL+ uses **single quotes** (`'...'`), not double quotes. Using double quotes will cause syntax errors.
- **Wrong comment syntax:** Comments use `!` or `/* */` not `//`.
- **Wrong logical operators:** Use `.and.`, `.or.`, `.not.` — not `&&`, `||`, `!`.
- **Case order matters:** In `case`/`condition` blocks, conditions are evaluated top-to-bottom. The first matching condition is used, so place the `condition always` case last as a default.
- **Circular dependencies:** Avoid SVARs that reference each other in a cycle, as this creates unresolvable definitions.
- **Include path errors:** File paths in `include` statements use single quotes and are relative to the model root.

---


## Tutorials
*Learning-oriented: step-by-step lessons for beginners.*

Before starting these tutorials, review the [Explanation](#explanation) section above to understand the core concepts of SVARs, DVARs, goals, and model structure.

### Getting Started with WRESL+

#### Prerequisites
- Download and install the WRIMS GUI from the [official source](https://data.cnra.ca.gov/dataset/wrims-2-gui/resource/93b6243c-57eb-47b5-ad62-7209c0acdfb6).
- Ensure Java is installed and available in your PATH.
- Familiarize yourself with the WRIMS IDE (Eclipse-based environment).

#### Your First Model
This tutorial creates a simple model that determines outflow from a reservoir given a known inflow.

1. Create a new file named `shasta_basic.wresl`:
   ```wresl
   ! Declare a state variable for known inflow to Shasta (I_ prefix = inflow)
   define I_SHSTA {value 10000}

   ! Declare a decision variable for channel flow below Shasta (C_ prefix = channel)
   dvar C_SHSTA {std kind 'FLOW-CHANNEL' units 'CFS'}

   ! Set a constraint: release must equal 90% of inflow
   goal setRelease {C_SHSTA = I_SHSTA * 0.9}
   ```
2. Set up the model structure by creating a main file `main.wresl`:
   ```wresl
   sequence main_seq {
     model shastaModel
     order 1
   }

   model shastaModel {
     include 'shasta_basic.wresl'
   }
   ```
3. Run the model using the WRIMS GUI or command-line interface.
4. Examine the output: the solver should determine `C_SHSTA = 9000`.

#### What Just Happened?
- `define I_SHSTA {value 10000}` created a **state variable** as a known inflow input.
- `dvar C_SHSTA {std kind 'FLOW-CHANNEL' units 'CFS'}` created a **decision variable** — an unknown for the solver.
- `goal setRelease {C_SHSTA = I_SHSTA * 0.9}` created a **constraint** as a rule the solver must satisfy.
- The `sequence` / `model` / `include` structure told WRIMS which files to load and in what order.

### Tutorial: Shasta Reservoir Mass Balance
This example demonstrates a realistic reservoir mass balance. The goal is to find the release (`C_SHSTA`) that maintains a target storage level:

```wresl
! State variables: known inputs
define I_SHSTA {timeseries kind 'FLOW' units 'CFS'}       ! Sacramento River inflow
define E_SHSTA {value 50}                                   ! evaporation loss (CFS)

! Previous storage (from previous time step)
define S_SHSTA_prev {value S_SHSTA(-1)}

! Decision variables
dvar C_SHSTA {std kind 'FLOW-CHANNEL' units 'CFS'}         ! channel flow below dam
dvar S_SHSTA {                                              ! reservoir storage
  lower 550                                                 ! dead pool (TAF)
  upper 4552                                                ! full pool (TAF)
  kind 'STORAGE'
  units 'TAF'
}

! Mass balance: storage_new = storage_old + inflow - release - evap (all in TAF)
goal massBalance {S_SHSTA = S_SHSTA_prev + I_SHSTA * cfs_taf - C_SHSTA * cfs_taf - E_SHSTA * cfs_taf}
```

**Key points:**
- `cfs_taf` is a built-in conversion factor from CFS to TAF (thousand acre-feet).
- `S_SHSTA(-1)` references the storage from the previous time step.
- The DVAR `S_SHSTA` has explicit bounds for dead pool and full pool levels.

### Tutorial: Conditional Operations with Case/Condition
Real-world operations often depend on conditions such as hydrology, time of year, or regulatory requirements. This example sets minimum instream flow requirements on the Sacramento River at Keswick that vary by month:

```wresl
! Minimum instream flow below Keswick varies by season
define C_KSWCK_MIF {
  case summer {
    condition month >= jun .and. month <= sep
    value 3250                                   ! summer base flow (CFS)
  }
  case winter {
    condition month >= oct .and. month <= feb
    value 3000                                   ! winter base flow (CFS)
  }
  case spring {
    condition always                             ! default / spring
    value 4500                                   ! spring pulse flow (CFS)
  }
}

! Enforce minimum instream flow as a goal
dvar C_KSWCK {std kind 'FLOW-CHANNEL' units 'CFS'}
goal minInstream {C_KSWCK > C_KSWCK_MIF}
```

**Key points:**
- Conditions are evaluated top-to-bottom; the first match is used.
- Always end with `condition always` as a default/fallback.
- You can have as many `case` blocks as needed.

### Tutorial: Multi-File Model Organization
As models grow, splitting definitions across files keeps things manageable. Here is how a CalSim-style model might be organized:

**main.wresl** (entry point):
```wresl
sequence cycle1 {
  model NOD_ops
  order 1
}

sequence cycle2 {
  model Delta_ops
  order 2
}

model NOD_ops {
  include 'Shasta/Shasta_storage.wresl'
  include 'Shasta/Shasta_goals.wresl'
  include 'Trinity/Trinity_import.wresl'
}

model Delta_ops {
  include 'Delta/Delta_inflow.wresl'
  include 'Delta/Delta_outflow.wresl'
}
```

**Shasta/Shasta_storage.wresl**:
```wresl
define I_SHSTA {timeseries kind 'FLOW' units 'CFS'}
dvar S_SHSTA {lower 550 upper 4552 kind 'STORAGE' units 'TAF'}
dvar C_SHSTA {std kind 'FLOW-CHANNEL' units 'CFS'}
```

**Shasta/Shasta_goals.wresl**:
```wresl
goal ShastaBalance {S_SHSTA = S_SHSTA(-1) + I_SHSTA * cfs_taf - C_SHSTA * cfs_taf}
goal ShastaMinRelease {C_SHSTA > 3250}
```

This pattern — separating variables from goals, organizing by geographic location, and using `include` — is the standard way to organize WRESL+ models.

---


## Practical Guides
*Task-oriented: recipes for accomplishing specific goals.*

Each guide below addresses a specific task. Skim the headings to find what you need.

### How to Define State Variables (SVARs)
State variables hold known or computed values. They can be defined using `define` or `svar`:

```wresl
! Simple fixed value
define I_SHSTA {value 10000}

! Using the svar keyword (equivalent to define for state variables)
svar E_SHSTA {value 50}

! Time series from HEC-DSS
define I_SHSTA {
  timeseries 'I_SHSTA' kind 'FLOW' units 'CFS'
}

! Table lookup from external data
define evapRate_SHSTA {
  select evap from evap_table where month = month
}

! Case-based (conditional) definition
define C_KSWCK_MIF {
  case wet {
    condition I_SHSTA > 20000
    value 4500
  }
  case dry {
    condition always
    value 3250
  }
}

! Summation over an index range
define annualInflow {
  sum(i=1,12) I_SHSTA(i)
}
```

### How to Define Decision Variables (DVARs)
Decision variables are determined by the optimization solver:

```wresl
! Standard decision variable (non-negative by default)
dvar C_SHSTA {std kind 'FLOW-CHANNEL' units 'CFS'}

! With explicit bounds (e.g., reservoir storage between dead pool and full pool)
dvar S_SHSTA {
  lower 550
  upper 4552
  kind 'STORAGE'
  units 'TAF'
}

! Integer decision variable
dvar numGates {
  integer std kind 'GATE-OPERATION' units 'NONE'
}
```

### How to Define Constants
```wresl
const deadPool_SHSTA {550}
const fullPool_SHSTA {4552}
```

### How to Define Goals (Constraints)
Goals define constraints for the optimizer:

```wresl
! Simple equality constraint: mass balance
goal ShastaBalance {S_SHSTA = S_SHSTA(-1) + I_SHSTA * cfs_taf - C_SHSTA * cfs_taf}

! Inequality constraint: enforce minimum instream flow
goal KeswickMinFlow {C_KSWCK > C_KSWCK_MIF}

! Goal with lhs/rhs and penalty structure
goal targetRelease {
  lhs C_SHSTA
  rhs C_SHSTA_target
  lhs > rhs constrain                 ! hard: do not exceed target
  lhs < rhs penalty 100               ! soft: penalize shortfall
}

! Goal with case-based conditions (seasonal release targets)
goal seasonalOps {
  lhs C_KSWCK
  case summerOps {
    condition month >= jun .and. month <= sep
    rhs 3250
  }
  case winterOps {
    condition always
    rhs 3000
  }
}
```

### How to Define Aliases
Aliases provide alternative names for computed expressions and are useful for output:

```wresl
define TRINITY_IMPORTDV {
  alias D_CLEARTU kind 'FLOW-TUNNEL' units 'CFS'
}
```

### How to Define Objectives
Objectives specify what the model should optimize, with weighted decision variables:

```wresl
objective minimizeShortage {
  [C_SHSTA, -1],
  [shortage_SHSTA, 9999]
}
```

### How to Include Files
Use `include` to modularize your model across multiple files:

```wresl
include 'Shasta/Shasta_storage.wresl'
include 'Shasta/Shasta_goals.wresl'

! Local include: definitions are scoped to the current model only
include [local] 'local_adjustments.wresl'
```

### How to Structure a Model
A complete WRESL+ model uses `sequence`, `model`, and `group` blocks:

```wresl
! Initial block for pre-simulation setup
initial {
  include 'init/initial_conditions.wresl'
}

sequence cycle1 {
  model NOD
  order 1
}

sequence cycle2 {
  model SOD
  order 2
}

model NOD {
  include 'NOD/Trinity_ops.wresl'
  include 'NOD/Shasta_ops.wresl'
  include 'NOD/Folsom_ops.wresl'
}

model SOD {
  include 'SOD/SanLuis_ops.wresl'
  include 'SOD/Delta_ops.wresl'
}

group commonDefs {
  include 'common/unit_conversions.wresl'
  include 'common/lookup_tables.wresl'
}
```

### How to Use Table Lookups
Look up values from external data tables:

```wresl
define S_SHSTAlevel5 {
  select target from res_level
    given storage = S_SHSTA
    use linear
    where month = month, reservoir = 'SHSTA'
}
```

### How to Use Conditional Includes
Include files conditionally based on runtime logic:

```wresl
if month >= oct .and. month <= mar {
  include 'COA/wet_season_rules.wresl'
}
elseif month >= apr .and. month <= sep {
  include 'COA/dry_season_rules.wresl'
}
```

### How to Use Unit Conversions
WRESL+ provides built-in conversion identifiers for flows and volumes:

```wresl
! Convert CFS to TAF (thousand acre-feet)
define I_SHSTA_TAF {value I_SHSTA * cfs_taf}

! Convert TAF to CFS
define C_SHSTA_CFS {value C_SHSTA_TAF * taf_cfs}

! Convert AF to CFS
define D_REDBLF_CFS {value D_REDBLF_AF * af_cfs}
```

### How to Reference Variables Across Model Cycles
When a downstream model needs a value computed in an upstream cycle:

```wresl
! Reference C_SHSTA as solved in the NOD (North of Delta) model
define C_SHSTA_upstream {value C_SHSTA[NOD]}

! Reference storage from the previous time step
define S_SHSTA_prev {value S_SHSTA(-1)}
```

### How to Use the Range Function
Check if the current month falls within a range:

```wresl
! Define a variable based on whether we are in the flood season
define floodSeason {
  case inRange {
    condition range(month, oct, mar) == 1
    value 1
  }
  case outOfRange {
    condition always
    value 0
  }
}
```

---


## Reference
*Information-oriented: technical details and valid syntax.*

This section is a comprehensive reference for the WRESL+ language. Use it to look up exact syntax, available keywords, and supported functions.

### WRESL+ Syntax Reference

#### Keywords
The following keywords are recognized by the WRESL+ parser (case-insensitive):

| Category | Keywords |
|---|---|
| Variable definition | `define`, `svar`, `dvar`, `const` |
| Variable content | `value`, `timeseries`, `alias`, `external`, `std`, `integer` |
| Bounds | `lower`, `upper`, `unbounded` |
| Metadata | `kind`, `units`, `convert` |
| Goals/Objectives | `goal`, `objective`, `lhs`, `rhs`, `penalty`, `constrain` |
| Table lookups | `select`, `from`, `given`, `use`, `where` |
| Summation | `sum` |
| Conditions | `condition`, `always`, `case` |
| Logical operators | `.and.`, `.or.`, `.not.` |
| Control flow | `if`, `elseif`, `else` |
| Model structure | `model`, `group`, `sequence`, `initial`, `include`, `order`, `timestep` |
| Special identifiers | `month`, `wateryear`, `day`, `daysin`, `daysinmonth`, `daysintimestep` |
| Unit conversions | `taf_cfs`, `cfs_taf`, `af_cfs`, `cfs_af` |
| Calendar months | `jan`, `feb`, `mar`, `apr`, `may`, `jun`, `jul`, `aug`, `sep`, `oct`, `nov`, `dec` |
| Previous months | `prevjan`, `prevfeb`, ..., `prevdec` |

#### Variable Definition Syntax

**State Variable (SVAR):**
```
define|svar [TimeArray] [[local]] Name { SvarContent }
```
Where `SvarContent` is one of:
- `value Expression`
- `timeseries ['BPart'] kind 'K' units 'U' [convert 'C']`
- `select Column from Table [given Assign use ID] [where Assignments]`
- `sum(i=Expr1,Expr2) Expression`
- One or more `case Name { condition LogicalExpr|always  value|select|sum ... }`

Examples:
```wresl
define I_SHSTA {timeseries 'I_SHSTA' kind 'FLOW' units 'CFS'}
define E_SHSTA {value 50}
define S_SHSTA_target {
  select target from ShastaLevelTable where month = month
}
define annualInflow {sum(i=1,12) I_SHSTA(i)}
define C_KSWCK_MIF {
  case summer {
    condition month >= jun .and. month <= sep
    value 3250
  }
  case default {
    condition always
    value 3000
  }
}
```

**Decision Variable (DVAR):**
```
define|dvar [TimeArray] [[local]] Name { DvarContent }
```
Where `DvarContent` is one of:
- `std kind 'K' units 'U'`
- `[lower Expr] [upper Expr] kind 'K' units 'U'`
- `integer std kind 'K' units 'U'`
- `integer [lower Expr] [upper Expr] kind 'K' units 'U'`

Examples:
```wresl
dvar C_SHSTA {std kind 'FLOW-CHANNEL' units 'CFS'}
dvar S_SHSTA {lower 550 upper 4552 kind 'STORAGE' units 'TAF'}
dvar numGates {integer std kind 'GATE-OPERATION' units 'NONE'}
```

**Constant:**
```
const [[local]] Name { Number }
```
Example:
```wresl
const deadPool_SHSTA {550}
```

**Alias:**
```
define [TimeArray] [[local]] Name { alias Expression [kind 'K'] [units 'U'] }
```
Example:
```wresl
define TRINITY_IMPORTDV {alias D_CLEARTU kind 'FLOW-TUNNEL' units 'CFS'}
```

**External:**
```
define [[local]] Name { external LibraryName[.dll] }
```
Example:
```wresl
define customCalc {external myLibrary}
```

#### Goal Syntax

**Simple goal:**
```
goal [TimeArray] [[local]] Name { Expression < | > | = Expression }
```
Examples:
```wresl
goal ShastaBalance {S_SHSTA = S_SHSTA(-1) + I_SHSTA * cfs_taf - C_SHSTA * cfs_taf}
goal KeswickMinFlow {C_KSWCK > C_KSWCK_MIF}
```

**Complex goal with LHS/RHS:**
```
goal [TimeArray] [[local]] Name {
  lhs Expression
  rhs Expression
  [lhs > rhs  constrain | penalty Expression]
  [lhs < rhs  constrain | penalty Expression]
}
```
Example:
```wresl
goal targetRelease {
  lhs C_SHSTA
  rhs C_SHSTA_target
  lhs > rhs constrain
  lhs < rhs penalty 100
}
```

**Complex goal with cases:**
```
goal [TimeArray] [[local]] Name {
  lhs Expression
  case CaseName {
    condition LogicalExpression | always
    rhs Expression
    [lhs > rhs  constrain | penalty Expression]
    [lhs < rhs  constrain | penalty Expression]
  }
  ...
}
```
Example:
```wresl
goal seasonalOps {
  lhs C_KSWCK
  case summerOps {
    condition month >= jun .and. month <= sep
    rhs 3250
    lhs > rhs constrain
    lhs < rhs penalty 9999
  }
  case winterOps {
    condition always
    rhs 3000
    lhs > rhs penalty 100
    lhs < rhs constrain
  }
}
```

#### Objective Syntax
```
objective [[local]] Name [=] {
  [VarRef [TimeArray], WeightExpression],
  ...
}
```
Example:
```wresl
objective minimizeShortage {
  [shortage_SHSTA, 9999],
  [C_SHSTA, -1]
}
```

#### Model Structure Syntax

**Sequence:**
```
sequence Name { model ModelRef [condition LogicalExpr] [order INT] [timestep 1MON|1DAY] }
```
Example:
```wresl
sequence cycle1 {model NOD order 1}
sequence cycle2 {model SOD order 2 timestep 1MON}
```

**Model:**
```
model Name { (Pattern | IfIncItems)+ }
```
Example:
```wresl
model NOD {
  include 'Shasta/Shasta_ops.wresl'
  include 'Trinity/Trinity_ops.wresl'
  if month >= oct .and. month <= mar {
    include 'COA/flood_rules.wresl'
  }
}
```

**Group:**
```
group Name { (Pattern | IfIncItems)+ }
```
Example:
```wresl
group commonDefs {
  include 'common/lookup_tables.wresl'
}
```

**Initial:**
```
initial { Pattern+ }
```
Example:
```wresl
initial {
  include 'init/initial_storage.wresl'
  include 'init/initial_flows.wresl'
}
```

**Include:**
```
include [[local]] 'filepath.wresl'
include model ModelName
include group GroupName
```

#### Data Types
- Integer (e.g., `42`, `550`)
- Float (e.g., `3.14`, `.5`, `1.98347`)

#### Operators

| Type | Operators |
|---|---|
| Arithmetic | `+`, `-`, `*`, `/` |
| Comparison | `==`, `/=` (not equal), `<`, `>`, `<=`, `>=` |
| Logical | `.and.`, `.or.`, `.not.` |

#### Comments
Single-line comments begin with `!`:
```wresl
! Sacramento River inflow to Shasta
define I_SHSTA {value 10000}   ! CFS
```

#### Strings
Strings use **single quotes**:
```wresl
kind 'FLOW-CHANNEL'
units 'CFS'
include 'Shasta/Shasta_ops.wresl'
```

### Supported Functions
| Function | Description | Example |
|---|---|---|
| `min(a, b, ...)` | Minimum of values | `min(I_SHSTA, maxCapacity)` |
| `max(a, b, ...)` | Maximum of values | `max(C_SHSTA, 0)` |
| `abs(x)` | Absolute value | `abs(S_SHSTA - target)` |
| `round(x)` | Round to nearest integer | `round(ratio)` |
| `int(x)` | Truncate to integer | `int(2.9)` → 2 |
| `mod(a, b)` | Modulo (remainder) | `mod(month, 12)` |
| `pow(a, b)` | Power (a raised to b) | `pow(base, exp)` |
| `log(x)` | Logarithm (also `log10`) | `log(value)` |
| `sin(x)` | Sine | `sin(angle)` |
| `cos(x)` | Cosine | `cos(angle)` |
| `tan(x)` | Tangent | `tan(angle)` |
| `cot(x)` | Cotangent | `cot(angle)` |
| `asin(x)` | Arc sine | `asin(ratio)` |
| `acos(x)` | Arc cosine | `acos(ratio)` |
| `atan(x)` | Arc tangent | `atan(slope)` |
| `acot(x)` | Arc cotangent | `acot(value)` |
| `range(var, start, end)` | Check if month/year is in range | `range(month, oct, mar)` |

### Special Built-in Identifiers
| Identifier | Description |
|---|---|
| `month` | Current simulation month (numeric) |
| `wateryear` | Current water year |
| `day` | Current simulation day |
| `daysin` / `daysinmonth` / `daysintimestep` | Number of days in current month/timestep |
| `jan` ... `dec` | Calendar month constants |
| `prevjan` ... `prevdec` | Previous year's month references |
| `taf_cfs` | TAF to CFS conversion factor for the current timestep |
| `cfs_taf` | CFS to TAF conversion factor for the current timestep |
| `af_cfs` | AF to CFS conversion factor for the current timestep |
| `cfs_af` | CFS to AF conversion factor for the current timestep |
| `$m` | Multi-step reference |
| `i` | Loop variable (used inside `sum` expressions) |

### Variable Cross-References
| Syntax | Meaning | Example |
|---|---|---|
| `varName[modelName]` | Reference variable as solved in another model cycle | `C_SHSTA[NOD]` |
| `varName(-1)` | Reference variable from the previous time step | `S_SHSTA(-1)` |
| `varName[modelName](offset)` | Cross-model reference with time offset | `C_SHSTA[NOD](-1)` |
| `varName[-1]` | Index-based variable reference | `S_SHSTA[-1]` |

---


## Debugging Tips
*Advice for solving common issues.*

### Common Error Messages

| Error Message | Likely Cause | Solution |
|---|---|---|
| Undefined variable | Variable not declared or typo in name | Check spelling; ensure variable is defined with `define`, `svar`, `dvar`, or `const` (e.g., `I_SHSTA` vs. `I_Shsta`) |
| Syntax error | Missing brace `{}`, wrong quotes, or invalid keyword | Review line for correct syntax; use single quotes `'...'` for strings |
| Circular reference detected | SVARs reference each other in a loop | Refactor definitions to break the cycle |
| File not found | Incorrect path in `include` statement | Check the file path (single-quoted, relative to model root, e.g., `'Shasta/Shasta_ops.wresl'`) |
| Missing kind/units | DVAR or timeseries missing required metadata | Add `kind '...'` and `units '...'` to the definition |
| Infeasible solution | Constraints cannot all be satisfied simultaneously | Review goals for conflicting constraints; consider using `penalty` instead of `constrain` |

### Troubleshooting Workflow
1. Read the error message carefully and note the file name and line number.
2. Check the referenced line for syntax issues (braces, quotes, keywords).
3. Verify all referenced variables are declared before use.
4. Simplify your model to isolate the issue and comment out sections with `!`.
5. Check that `case` blocks end with a `condition always` default.
6. Verify `include` file paths exist and are correctly quoted.
7. Consult the [Reference](#reference) and [Explanation](#explanation) sections.

### Frequently Asked Questions (FAQ)

**Q: How do I write comments in WRESL+?**
A: Use `!` at any point in a line. Everything after `!` on that line is treated as a comment.

**Q: How do I include external data?**
A: Use `timeseries` for HEC-DSS data (e.g., `define I_SHSTA {timeseries kind 'FLOW' units 'CFS'}`) or `select...from...where` for lookup tables. Use `include` to bring in other `.wresl` files.

**Q: What kind of quotes does WRESL+ use?**
A: Single quotes only: `'FLOW-CHANNEL'`. Double quotes are not valid.

**Q: What are the logical operators?**
A: `.and.`, `.or.`, `.not.` — not `&&`, `||`, `!`.

**Q: How do I make a variable local to a model cycle?**
A: Add the `[local]` modifier: `define [local] myVar {value 42}` or `include [local] 'file.wresl'`.

**Q: What do the CalSim variable prefixes mean?**
A: `I_` = inflow, `S_` = storage, `C_` = channel flow, `D_` = diversion, `E_` = evaporation, `AD_` = accretion/depletion. See [CalSim Naming Conventions](#calsim-naming-conventions).

**Q: Where can I find more examples?**
A: See the Tutorials and How-to Guides sections above, or examine existing CalSim3 study files for real-world patterns.

---


## Contributing to the Documentation
If you find errors or have suggestions for improving this documentation:
1. Open an issue or pull request on the [wrims-gui repository](https://github.com/CentralValleyModeling/wrims-gui).
2. Reference the [WRESL+ Xtext grammar](xtext-editor/src/main/resources/gov/ca/dwr/wresl/xtext/editor/WreslEditor.xtext) as the authoritative syntax source.
3. Follow the [Diátaxis](https://diataxis.fr/) framework when adding content — place material in the appropriate section (Explanation, Tutorials, How-to, or Reference).
4. Use CalSim naming conventions (`I_`, `S_`, `C_`, `D_`) in examples for consistency.


