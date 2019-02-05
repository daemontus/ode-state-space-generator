[![Release](https://jitpack.io/v/sybila/ode-generator.svg)](https://jitpack.io/#sybila/ode-generator)
[![Build Status](https://travis-ci.org/sybila/ode-generator.svg?branch=master)](https://travis-ci.org/sybila/ode-generator)
[![codecov.io](https://codecov.io/github/sybila/ode-generator/coverage.svg?branch=master)](https://codecov.io/github/sybila/ode-generator?branch=master)
[![License](https://img.shields.io/badge/License-GPL%20v3-blue.svg?style=flat)](https://github.com/sybila/ode-generator/blob/master/LICENSE.txt)
[![Kotlin](https://img.shields.io/badge/kotlin-1.0.0-blue.svg)](http://kotlinlang.org)

State space generator for models based on ordinary differential equations. 
Part of BioDivine tool for model checking and parameter synthesis of biological models.

Repository can be added as a dependency to your project using jitpack (See badge above).

## Model Syntax

So far, only supported format of input model file is **.bio** (or **BIO**) format. 
Every model file has to contain at least the following parts:
- declaration of model variables,
- corresponding thresholds (at least two numeric values have to be defined as the lower and upper bound thresholds for each variable), 
- declaration of parameters (each with a lower and upper bound),
- differential equations (one for each variable).  

The corresponding lines start with predefined keywords and have the following syntax (the order of lines is strongly recommended):

- **VARS: _variables_** (mandatory, only one occurrence) where **_variables_** is a list of variable names delimited by comma (**,**).
- **THRES: _variable\_name: threshold\_values_** (mandatory, one for each model variable) where **_threshold\_values_** is a list of at least two **_numeric\_values_** delimited by comma (**,**) and **_variable_name_** is the reference to **_variables_** list.
- **PARAMS: _parameters_** (mandatory, only one occurrence) where **_parameters_** is a list of expressions delimited by semicolon (**;**) in the form **_param\_name, lower\_bound, upper\_bound_**.
- **CONSTS: _constants_** (optional, only one occurrence) where **_constants_** is a list of named constants delimited by semicolon (**;**) in the form **_constant\_name, constant\_value_**.
- **EQ: _variable\_name = equation\_expression_** (mandatory, one for each model variable) where **_variable\_name_** is the reference to **_variables_** list (syntax of the **_equation\_expression_** is defined later in this section).
- **VAR\_POINTS: _var\_points_** (optional, only one occurrence) where **_var\_points_** is a list delimited by semicolons (**;**) of the form **_variable\_name: valuation\_points, ramps\_count_**. Here, **_valuation\_points_** defines the accuracy of the approximation and **_ramps\_count_** defines the number of additional created thresholds, effectively determining the accuracy of the parameter synthesis process.

In the syntax above, the terms **_lower\_bound_**, **_upper\_bound_**, **_constant\_value_** and **_numeric\_values_** are either integers or floating-point numbers (the scientific notation is not supported), while the terms **_valuation\_points_** and **_ramps\_count_** are integers only. _Note that lines do not end with a semicolon (;) or any other special
ending character._

The syntax of **_equation\_expression_** is defined as an arithmetic expression that uses only the operations **+** (addition), **-** (subtraction or unary negation), and __*__ (multiplication). The operands can be either memebrs of **_numeric\_values_**, **_variables_**, **_constant\_name_**, **_param\_name_**, or the application of one of the following functions:

- [ **_Hp_** | **_Hm_** ]**_(var, thr, a, b)_** is the so-called [Heaviside step function](https://en.wikipedia.org/wiki/Heaviside_step_function) in increasing/positive form  (**_Hp_**) or in decreasing/negative form (**_Hm_**); they are defined in the following way:
  - <a href="https://www.codecogs.com/eqnedit.php?latex=\texttt{Hp}(\texttt{var},\texttt{thr},\texttt{a},\texttt{b})&space;=&space;\begin{cases}&space;\texttt{a},&space;&(\texttt{var}&space;<&space;\texttt{thr})&space;\land&space;(a&space;<&space;b)&space;\\&space;\texttt{b},&space;&(\texttt{var}&space;<&space;\texttt{thr})&space;\land&space;(b&space;<&space;a)&space;\\&space;\texttt{b},&space;&(\texttt{var}&space;\geq&space;\texttt{thr})&space;\land&space;(a&space;<&space;b)&space;\\&space;\texttt{a},&space;&(\texttt{var}&space;\geq&space;\texttt{thr})&space;\land&space;(b&space;<&space;a)&space;\end{cases}" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\texttt{Hp}(\texttt{var},\texttt{thr},\texttt{a},\texttt{b})&space;=&space;\begin{cases}&space;\texttt{a},&space;&(\texttt{var}&space;<&space;\texttt{thr})&space;\land&space;(a&space;<&space;b)&space;\\&space;\texttt{b},&space;&(\texttt{var}&space;<&space;\texttt{thr})&space;\land&space;(b&space;<&space;a)&space;\\&space;\texttt{b},&space;&(\texttt{var}&space;\geq&space;\texttt{thr})&space;\land&space;(a&space;<&space;b)&space;\\&space;\texttt{a},&space;&(\texttt{var}&space;\geq&space;\texttt{thr})&space;\land&space;(b&space;<&space;a)&space;\end{cases}" title="\texttt{Hp}(\texttt{var},\texttt{thr},\texttt{a},\texttt{b}) = \begin{cases} \texttt{a}, &(\texttt{var} < \texttt{thr}) \land (a < b) \\ \texttt{b}, &(\texttt{var} < \texttt{thr}) \land (b < a) \\ \texttt{b}, &(\texttt{var} \geq \texttt{thr}) \land (a < b) \\ \texttt{a}, &(\texttt{var} \geq \texttt{thr}) \land (b < a) \end{cases}" /></a>
  - <a href="https://www.codecogs.com/eqnedit.php?latex=\texttt{Hm}(\texttt{var},\texttt{thr},\texttt{a},\texttt{b})&space;=&space;\begin{cases}&space;\texttt{b},&space;&(\texttt{var}&space;<&space;\texttt{thr})&space;\land&space;(a&space;<&space;b)&space;\\&space;\texttt{a},&space;&(\texttt{var}&space;<&space;\texttt{thr})&space;\land&space;(b&space;<&space;a)&space;\\&space;\texttt{a},&space;&(\texttt{var}&space;\geq&space;\texttt{thr})&space;\land&space;(a&space;<&space;b)&space;\\&space;\texttt{b},&space;&(\texttt{var}&space;\geq&space;\texttt{thr})&space;\land&space;(b&space;<&space;a)&space;\end{cases}" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\texttt{Hm}(\texttt{var},\texttt{thr},\texttt{a},\texttt{b})&space;=&space;\begin{cases}&space;\texttt{b},&space;&(\texttt{var}&space;<&space;\texttt{thr})&space;\land&space;(a&space;<&space;b)&space;\\&space;\texttt{a},&space;&(\texttt{var}&space;<&space;\texttt{thr})&space;\land&space;(b&space;<&space;a)&space;\\&space;\texttt{a},&space;&(\texttt{var}&space;\geq&space;\texttt{thr})&space;\land&space;(a&space;<&space;b)&space;\\&space;\texttt{b},&space;&(\texttt{var}&space;\geq&space;\texttt{thr})&space;\land&space;(b&space;<&space;a)&space;\end{cases}" title="\texttt{Hm}(\texttt{var},\texttt{thr},\texttt{a},\texttt{b}) = \begin{cases} \texttt{b}, &(\texttt{var} < \texttt{thr}) \land (a < b) \\ \texttt{a}, &(\texttt{var} < \texttt{thr}) \land (b < a) \\ \texttt{a}, &(\texttt{var} \geq \texttt{thr}) \land (a < b) \\ \texttt{b}, &(\texttt{var} \geq \texttt{thr}) \land (b < a) \end{cases}" /></a>
 
- [ **_Rp_** | **_Rm_** ]**_(var, thr1, thr2, a, b)_** is the so-called [ramp function](https://en.wikipedia.org/wiki/Ramp_function) in increasing/positive form  (**_Rp_**) or in decreasing/negative form (**_Rm_**) defined as follows:
  - <a href="https://www.codecogs.com/eqnedit.php?latex=\texttt{Rp}(\texttt{var},\texttt{thr1},\texttt{thr2},\texttt{a},\texttt{b})&space;=&space;\begin{cases}&space;\texttt{a},&space;&(\texttt{var}&space;<&space;\texttt{thr1})&space;\land&space;(a&space;<&space;b)&space;\\&space;\texttt{b},&space;&(\texttt{var}&space;<&space;\texttt{thr1})&space;\land&space;(b&space;<&space;a)&space;\\&space;\texttt{a}&plus;(\texttt{b}-\texttt{a})\cdot\frac{\texttt{var}-\texttt{thr1}}{\texttt{thr2}-\texttt{thr1}},&space;&(\texttt{thr1}&space;\leq&space;\texttt{var}&space;\leq&space;\texttt{thr2})&space;\land&space;(a&space;<&space;b)&space;\\&space;\texttt{b}&plus;(\texttt{a}-\texttt{b})\cdot\frac{\texttt{var}-\texttt{thr1}}{\texttt{thr2}-\texttt{thr1}},&space;&(\texttt{thr1}&space;\leq&space;\texttt{var}&space;\leq&space;\texttt{thr2})&space;\land&space;(b&space;<&space;a)&space;\\&space;\texttt{b},&space;&(\texttt{var}&space;>&space;\texttt{thr2})&space;\land&space;(a&space;<&space;b)&space;\\&space;\texttt{a},&space;&(\texttt{var}&space;>&space;\texttt{thr2})&space;\land&space;(b&space;<&space;a)&space;\end{cases}" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\texttt{Rp}(\texttt{var},\texttt{thr1},\texttt{thr2},\texttt{a},\texttt{b})&space;=&space;\begin{cases}&space;\texttt{a},&space;&(\texttt{var}&space;<&space;\texttt{thr1})&space;\land&space;(a&space;<&space;b)&space;\\&space;\texttt{b},&space;&(\texttt{var}&space;<&space;\texttt{thr1})&space;\land&space;(b&space;<&space;a)&space;\\&space;\texttt{a}&plus;(\texttt{b}-\texttt{a})\cdot\frac{\texttt{var}-\texttt{thr1}}{\texttt{thr2}-\texttt{thr1}},&space;&(\texttt{thr1}&space;\leq&space;\texttt{var}&space;\leq&space;\texttt{thr2})&space;\land&space;(a&space;<&space;b)&space;\\&space;\texttt{b}&plus;(\texttt{a}-\texttt{b})\cdot\frac{\texttt{var}-\texttt{thr1}}{\texttt{thr2}-\texttt{thr1}},&space;&(\texttt{thr1}&space;\leq&space;\texttt{var}&space;\leq&space;\texttt{thr2})&space;\land&space;(b&space;<&space;a)&space;\\&space;\texttt{b},&space;&(\texttt{var}&space;>&space;\texttt{thr2})&space;\land&space;(a&space;<&space;b)&space;\\&space;\texttt{a},&space;&(\texttt{var}&space;>&space;\texttt{thr2})&space;\land&space;(b&space;<&space;a)&space;\end{cases}" title="\texttt{Rp}(\texttt{var},\texttt{thr1},\texttt{thr2},\texttt{a},\texttt{b}) = \begin{cases} \texttt{a}, &(\texttt{var} < \texttt{thr1}) \land (a < b) \\ \texttt{b}, &(\texttt{var} < \texttt{thr1}) \land (b < a) \\ \texttt{a}+(\texttt{b}-\texttt{a})\cdot\frac{\texttt{var}-\texttt{thr1}}{\texttt{thr2}-\texttt{thr1}}, &(\texttt{thr1} \leq \texttt{var} \leq \texttt{thr2}) \land (a < b) \\ \texttt{b}+(\texttt{a}-\texttt{b})\cdot\frac{\texttt{var}-\texttt{thr1}}{\texttt{thr2}-\texttt{thr1}}, &(\texttt{thr1} \leq \texttt{var} \leq \texttt{thr2}) \land (b < a) \\ \texttt{b}, &(\texttt{var} > \texttt{thr2}) \land (a < b) \\ \texttt{a}, &(\texttt{var} > \texttt{thr2}) \land (b < a) \end{cases}" /></a>
  - <a href="https://www.codecogs.com/eqnedit.php?latex=\texttt{Rm}(\texttt{var},\texttt{thr1},\texttt{thr2},\texttt{a},\texttt{b})&space;=&space;\begin{cases}&space;\texttt{b},&space;&(\texttt{var}&space;<&space;\texttt{thr1})&space;\land&space;(a&space;<&space;b)&space;\\&space;\texttt{a},&space;&(\texttt{var}&space;<&space;\texttt{thr1})&space;\land&space;(b&space;<&space;a)&space;\\&space;\texttt{a}-(\texttt{a}-\texttt{b})\cdot\frac{\texttt{var}-\texttt{thr1}}{\texttt{thr2}-\texttt{thr1}},&space;&(\texttt{thr1}&space;\leq&space;\texttt{var}&space;\leq&space;\texttt{thr2})&space;\land&space;(b&space;<&space;a)&space;\\&space;\texttt{b}-(\texttt{b}-\texttt{a})\cdot\frac{\texttt{var}-\texttt{thr1}}{\texttt{thr2}-\texttt{thr1}},&space;&(\texttt{thr1}&space;\leq&space;\texttt{var}&space;\leq&space;\texttt{thr2})&space;\land&space;(a&space;<&space;b)&space;\\&space;\texttt{a},&space;&(\texttt{var}&space;>&space;\texttt{thr2})&space;\land&space;(a&space;<&space;b)&space;\\&space;\texttt{b},&space;&(\texttt{var}&space;>&space;\texttt{thr2})&space;\land&space;(b&space;<&space;a)&space;\end{cases}" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\texttt{Rm}(\texttt{var},\texttt{thr1},\texttt{thr2},\texttt{a},\texttt{b})&space;=&space;\begin{cases}&space;\texttt{b},&space;&(\texttt{var}&space;<&space;\texttt{thr1})&space;\land&space;(a&space;<&space;b)&space;\\&space;\texttt{a},&space;&(\texttt{var}&space;<&space;\texttt{thr1})&space;\land&space;(b&space;<&space;a)&space;\\&space;\texttt{a}-(\texttt{a}-\texttt{b})\cdot\frac{\texttt{var}-\texttt{thr1}}{\texttt{thr2}-\texttt{thr1}},&space;&(\texttt{thr1}&space;\leq&space;\texttt{var}&space;\leq&space;\texttt{thr2})&space;\land&space;(b&space;<&space;a)&space;\\&space;\texttt{b}-(\texttt{b}-\texttt{a})\cdot\frac{\texttt{var}-\texttt{thr1}}{\texttt{thr2}-\texttt{thr1}},&space;&(\texttt{thr1}&space;\leq&space;\texttt{var}&space;\leq&space;\texttt{thr2})&space;\land&space;(a&space;<&space;b)&space;\\&space;\texttt{a},&space;&(\texttt{var}&space;>&space;\texttt{thr2})&space;\land&space;(a&space;<&space;b)&space;\\&space;\texttt{b},&space;&(\texttt{var}&space;>&space;\texttt{thr2})&space;\land&space;(b&space;<&space;a)&space;\end{cases}" title="\texttt{Rm}(\texttt{var},\texttt{thr1},\texttt{thr2},\texttt{a},\texttt{b}) = \begin{cases} \texttt{b}, &(\texttt{var} < \texttt{thr1}) \land (a < b) \\ \texttt{a}, &(\texttt{var} < \texttt{thr1}) \land (b < a) \\ \texttt{a}-(\texttt{a}-\texttt{b})\cdot\frac{\texttt{var}-\texttt{thr1}}{\texttt{thr2}-\texttt{thr1}}, &(\texttt{thr1} \leq \texttt{var} \leq \texttt{thr2}) \land (b < a) \\ \texttt{b}-(\texttt{b}-\texttt{a})\cdot\frac{\texttt{var}-\texttt{thr1}}{\texttt{thr2}-\texttt{thr1}}, &(\texttt{thr1} \leq \texttt{var} \leq \texttt{thr2}) \land (a < b) \\ \texttt{a}, &(\texttt{var} > \texttt{thr2}) \land (a < b) \\ \texttt{b}, &(\texttt{var} > \texttt{thr2}) \land (b < a) \end{cases}" /></a>
  
- [ **_Sm_** | **_Sp_** ]**_(var, k, thr, a, b)_** is a sigmoidal function in increasing/positive form (**_Sp_**) or in decreasing/negative form (**_Sm_**); defined in the following way:
  - <a href="https://www.codecogs.com/eqnedit.php?latex=\texttt{Sp}(\texttt{var},\texttt{k},\texttt{thr},\texttt{a},\texttt{b})&space;=&space;\begin{cases}&space;\texttt{a}&space;&plus;&space;(\texttt{b}&space;-&space;\texttt{a})&space;\cdot&space;\frac{1&plus;\tanh(\texttt{k}\cdot&space;(\texttt{var}&space;-&space;\texttt{thr}))}{2},&space;&&space;a&space;<&space;b&space;\\&space;\texttt{b}&space;&plus;&space;(\texttt{a}&space;-&space;\texttt{b})&space;\cdot&space;\frac{1&plus;\tanh(\texttt{k}\cdot&space;(\texttt{var}&space;-&space;\texttt{thr}))}{2},&space;&&space;b&space;<&space;a&space;\end{cases}" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\texttt{Sp}(\texttt{var},\texttt{k},\texttt{thr},\texttt{a},\texttt{b})&space;=&space;\begin{cases}&space;\texttt{a}&space;&plus;&space;(\texttt{b}&space;-&space;\texttt{a})&space;\cdot&space;\frac{1&plus;\tanh(\texttt{k}\cdot&space;(\texttt{var}&space;-&space;\texttt{thr}))}{2},&space;&&space;a&space;<&space;b&space;\\&space;\texttt{b}&space;&plus;&space;(\texttt{a}&space;-&space;\texttt{b})&space;\cdot&space;\frac{1&plus;\tanh(\texttt{k}\cdot&space;(\texttt{var}&space;-&space;\texttt{thr}))}{2},&space;&&space;b&space;<&space;a&space;\end{cases}" title="\texttt{Sp}(\texttt{var},\texttt{k},\texttt{thr},\texttt{a},\texttt{b}) = \begin{cases} \texttt{a} + (\texttt{b} - \texttt{a}) \cdot \frac{1+\tanh(\texttt{k}\cdot (\texttt{var} - \texttt{thr}))}{2}, & a < b \\ \texttt{b} + (\texttt{a} - \texttt{b}) \cdot \frac{1+\tanh(\texttt{k}\cdot (\texttt{var} - \texttt{thr}))}{2}, & b < a \end{cases}" /></a>
  - <a href="https://www.codecogs.com/eqnedit.php?latex=\texttt{Sm}(\texttt{var},\texttt{k},\texttt{thr},\texttt{a},\texttt{b})&space;=&space;\begin{cases}&space;\texttt{a}&space;-&space;(\texttt{a}&space;-&space;\texttt{b})&space;\cdot&space;\frac{1&plus;\tanh(\texttt{k}&space;(\texttt{var}&space;-&space;\texttt{thr}))}{2},&space;&&space;b&space;<&space;a&space;\\&space;\texttt{b}&space;-&space;(\texttt{b}&space;-&space;\texttt{a})&space;\cdot&space;\frac{1&plus;\tanh(\texttt{k}&space;(\texttt{var}&space;-&space;\texttt{thr}))}{2},&space;&&space;a&space;<&space;b&space;\end{cases}" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\texttt{Sm}(\texttt{var},\texttt{k},\texttt{thr},\texttt{a},\texttt{b})&space;=&space;\begin{cases}&space;\texttt{a}&space;-&space;(\texttt{a}&space;-&space;\texttt{b})&space;\cdot&space;\frac{1&plus;\tanh(\texttt{k}&space;(\texttt{var}&space;-&space;\texttt{thr}))}{2},&space;&&space;b&space;<&space;a&space;\\&space;\texttt{b}&space;-&space;(\texttt{b}&space;-&space;\texttt{a})&space;\cdot&space;\frac{1&plus;\tanh(\texttt{k}&space;(\texttt{var}&space;-&space;\texttt{thr}))}{2},&space;&&space;a&space;<&space;b&space;\end{cases}" title="\texttt{Sm}(\texttt{var},\texttt{k},\texttt{thr},\texttt{a},\texttt{b}) = \begin{cases} \texttt{a} - (\texttt{a} - \texttt{b}) \cdot \frac{1+\tanh(\texttt{k} (\texttt{var} - \texttt{thr}))}{2}, & b < a \\ \texttt{b} - (\texttt{b} - \texttt{a}) \cdot \frac{1+\tanh(\texttt{k} (\texttt{var} - \texttt{thr}))}{2}, & a < b \end{cases}" /></a>

- [ **_Hillp_** | **_Hillm_** ]**_(var, thr, n, a, b)_** is the so-called [Hill function](https://en.wikipedia.org/wiki/Hill_equation_(biochemistry)) in positive form (**_Hillp_**) or in negative form (**_Hillm_**); they are defined in the following way:
  - <a href="https://www.codecogs.com/eqnedit.php?latex=\texttt{Hillp}(\texttt{var},\texttt{thr},\texttt{n},\texttt{a},\texttt{b})&space;=&space;\begin{cases}&space;\texttt{a}&space;&plus;&space;\frac{(\texttt{b}&space;-&space;\texttt{a})&space;\cdot&space;\texttt{var}^\texttt{n}}{\texttt{var}^\texttt{n}&space;&plus;&space;\texttt{thr}^\texttt{n}},&space;&&space;a&space;<&space;b&space;\\&space;\texttt{b}&space;&plus;&space;\frac{(\texttt{a}&space;-&space;\texttt{b})&space;\cdot&space;\texttt{var}^\texttt{n}}{\texttt{var}^\texttt{n}&space;&plus;&space;\texttt{thr}^\texttt{n}},&space;&&space;b&space;<&space;a&space;\end{cases}" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\texttt{Hillp}(\texttt{var},\texttt{thr},\texttt{n},\texttt{a},\texttt{b})&space;=&space;\begin{cases}&space;\texttt{a}&space;&plus;&space;\frac{(\texttt{b}&space;-&space;\texttt{a})&space;\cdot&space;\texttt{var}^\texttt{n}}{\texttt{var}^\texttt{n}&space;&plus;&space;\texttt{thr}^\texttt{n}},&space;&&space;a&space;<&space;b&space;\\&space;\texttt{b}&space;&plus;&space;\frac{(\texttt{a}&space;-&space;\texttt{b})&space;\cdot&space;\texttt{var}^\texttt{n}}{\texttt{var}^\texttt{n}&space;&plus;&space;\texttt{thr}^\texttt{n}},&space;&&space;b&space;<&space;a&space;\end{cases}" title="\texttt{Hillp}(\texttt{var},\texttt{thr},\texttt{n},\texttt{a},\texttt{b}) = \begin{cases} \texttt{a} + \frac{(\texttt{b} - \texttt{a}) \cdot \texttt{var}^\texttt{n}}{\texttt{var}^\texttt{n} + \texttt{thr}^\texttt{n}}, & a < b \\ \texttt{b} + \frac{(\texttt{a} - \texttt{b}) \cdot \texttt{var}^\texttt{n}}{\texttt{var}^\texttt{n} + \texttt{thr}^\texttt{n}}, & b < a \end{cases}" /></a>
  - <a href="https://www.codecogs.com/eqnedit.php?latex=\texttt{Hillm}(\texttt{var},\texttt{thr},\texttt{n},\texttt{a},\texttt{b})&space;=&space;\begin{cases}&space;\texttt{a}&space;-&space;\frac{(\texttt{a}&space;-&space;\texttt{b})&space;\cdot&space;\texttt{thr}^\texttt{n}}{\texttt{var}^\texttt{n}&space;&plus;&space;\texttt{thr}^\texttt{n}},&space;&&space;b&space;<&space;a&space;\\&space;\texttt{b}&space;-&space;\frac{(\texttt{b}&space;-&space;\texttt{a})&space;\cdot&space;\texttt{thr}^\texttt{n}}{\texttt{var}^\texttt{n}&space;&plus;&space;\texttt{thr}^\texttt{n}},&space;&&space;a&space;<&space;b&space;\end{cases}" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\texttt{Hillm}(\texttt{var},\texttt{thr},\texttt{n},\texttt{a},\texttt{b})&space;=&space;\begin{cases}&space;\texttt{a}&space;-&space;\frac{(\texttt{a}&space;-&space;\texttt{b})&space;\cdot&space;\texttt{thr}^\texttt{n}}{\texttt{var}^\texttt{n}&space;&plus;&space;\texttt{thr}^\texttt{n}},&space;&&space;b&space;<&space;a&space;\\&space;\texttt{b}&space;-&space;\frac{(\texttt{b}&space;-&space;\texttt{a})&space;\cdot&space;\texttt{thr}^\texttt{n}}{\texttt{var}^\texttt{n}&space;&plus;&space;\texttt{thr}^\texttt{n}},&space;&&space;a&space;<&space;b&space;\end{cases}" title="\texttt{Hillm}(\texttt{var},\texttt{thr},\texttt{n},\texttt{a},\texttt{b}) = \begin{cases} \texttt{a} - \frac{(\texttt{a} - \texttt{b}) \cdot \texttt{thr}^\texttt{n}}{\texttt{var}^\texttt{n} + \texttt{thr}^\texttt{n}}, & b < a \\ \texttt{b} - \frac{(\texttt{b} - \texttt{a}) \cdot \texttt{thr}^\texttt{n}}{\texttt{var}^\texttt{n} + \texttt{thr}^\texttt{n}}, & a < b \end{cases}" /></a>

  When **_n_** equals 1, the function serves as the [Michaelis-Menten kinetics](https://en.wikipedia.org/wiki/Michaelis%E2%80%93Menten_kinetics). In that case, the parameters **_var_** and **_thr_** have the meaning of <a href="https://www.codecogs.com/eqnedit.php?latex=S,&space;K_\text{m},&space;n" target="_blank"><img src="https://latex.codecogs.com/gif.latex?[S],&space;K_\text{M}" title="S, K_\text{M}" /></a>, respectively. Moreover, if **_a_** equals 0 then **_b_** acts as the maximum rate constant <a href="https://www.codecogs.com/eqnedit.php?latex=V_\text{max}" target="_blank"><img src="https://latex.codecogs.com/gif.latex?V_\text{max}" title="V_\text{max}" /></a> and vice-versa.

- **_Pow(var, n)_** is the well-known power function defined in the following way:
  - <a href="https://www.codecogs.com/eqnedit.php?latex=\texttt{Pow}(\texttt{var},\texttt{n})&space;=&space;\texttt{var}^\texttt{n}" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\texttt{Pow}(\texttt{var},\texttt{n})&space;=&space;\texttt{var}^\texttt{n}" title="\texttt{Pow}(\texttt{var},\texttt{n}) = \texttt{var}^\texttt{n}" /></a>

- **_Monod(var,thr,y)_** is the most common function for description of the microbial growth kinetics called [Monod equation](https://en.wikipedia.org/wiki/Monod_equation) where **_var_** usually stands for the substrate concentration; it is defined in the following way:
  - <a href="https://www.codecogs.com/eqnedit.php?latex=\texttt{Monod}(\texttt{var},\texttt{thr},\texttt{y})&space;=&space;\frac{\texttt{var}}{\texttt{y}\cdot(\texttt{var}&plus;\texttt{thr})}" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\texttt{Monod}(\texttt{var},\texttt{thr},\texttt{y})&space;=&space;\frac{\texttt{var}}{\texttt{y}\cdot(\texttt{var}&plus;\texttt{thr})}" title="\texttt{Monod}(\texttt{var},\texttt{thr},\texttt{y}) = \frac{\texttt{var}}{\texttt{y}\cdot(\texttt{var}+\texttt{thr})}" /></a>
  
  When the function is used to model an increase of microbial population based on the substrate concentration the **_y_** (i.e., the yield coefficient) equals 1. Otherwise, it is used to model a decrease of the substrate proportional to the population growth.
  
- **_Moser(var,thr,n)_** is another function for modelling of microbial growth defined as follows:
  - <a href="https://www.codecogs.com/eqnedit.php?latex=\texttt{Moser}(\texttt{var},\texttt{thr},\texttt{n})&space;=&space;\frac{\texttt{var}^\texttt{n}}{\texttt{var}^\texttt{n}&plus;\texttt{thr}}" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\texttt{Moser}(\texttt{var},\texttt{thr},\texttt{n})&space;=&space;\frac{\texttt{var}^\texttt{n}}{\texttt{var}^\texttt{n}&plus;\texttt{thr}}" title="\texttt{Moser}(\texttt{var},\texttt{thr},\texttt{n}) = \frac{\texttt{var}^\texttt{n}}{\texttt{var}^\texttt{n}+\texttt{thr}}" /></a>
  
- **_Tessier(var,thr)_** is another function for modelling of microbial growth defined in the following way:
  - <a href="https://www.codecogs.com/eqnedit.php?latex=\texttt{Tessier}(\texttt{var},\texttt{thr})&space;=&space;1-\exp\left(-\frac{\texttt{var}}{\texttt{thr}}\right)" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\texttt{Tessier}(\texttt{var},\texttt{thr})&space;=&space;1-\exp\left(-\frac{\texttt{var}}{\texttt{thr}}\right)" title="\texttt{Tessier}(\texttt{var},\texttt{thr}) = 1-\exp\left(-\frac{\texttt{var}}{\texttt{thr}}\right)" /></a>
  
- **_Haldane(var,thr,k)_** is microbial growth function defined as follows:
  - <a href="https://www.codecogs.com/eqnedit.php?latex=\texttt{Haldane}(\texttt{var},\texttt{thr},\texttt{k})&space;=&space;\frac{\texttt{var}}{\texttt{var}&plus;\texttt{thr}&plus;\frac{\texttt{var}^2}{k}}" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\texttt{Haldane}(\texttt{var},\texttt{thr},\texttt{k})&space;=&space;\frac{\texttt{var}}{(\texttt{var}&plus;\texttt{thr}&plus;\frac{\texttt{var}^2}{k})))}" title="\texttt{Haldane}(\texttt{var},\texttt{thr},\texttt{k}) = \frac{\texttt{var}}{\texttt{var}+\texttt{thr}+\frac{\texttt{var}^2}{k}}" /></a>

- **_Aiba(var,thr,k)_** is another microbial growth function defined in the following way:
  - <a href="https://www.codecogs.com/eqnedit.php?latex=\texttt{Aiba}(\texttt{var},\texttt{thr},\texttt{k})&space;=&space;\frac{\texttt{var}\cdot\exp{(-\frac{\texttt{var}}{\texttt{thr}})}}{\texttt{var}&plus;\texttt{thr}}" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\texttt{Aiba}(\texttt{var},\texttt{thr},\texttt{k})&space;=&space;\frac{\texttt{var}\cdot\exp{(-\frac{\texttt{var}}{\texttt{thr}})}}{\texttt{var}&plus;\texttt{thr}}" title="\texttt{Aiba}(\texttt{var},\texttt{thr},\texttt{k}) = \frac{\texttt{var}\cdot\exp{(-\frac{\texttt{var}}{\texttt{thr}})}}{\texttt{var}+\texttt{thr}}" /></a>
  
- **_Tessier_type(var,thr,k)_** is a microbial growth function defined as follows:
  - <a href="https://www.codecogs.com/eqnedit.php?latex=\texttt{Tessier\_type}(\texttt{var},\texttt{thr},\texttt{k})&space;=&space;\exp\left(-\frac{\texttt{var}}{\texttt{k}}\right)-\exp\left(-\frac{\texttt{var}}{\texttt{thr}}\right&space;)" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\texttt{Tessier\_type}(\texttt{var},\texttt{thr},\texttt{k})&space;=&space;\exp\left(-\frac{\texttt{var}}{\texttt{k}}\right)-\exp\left(-\frac{\texttt{var}}{\texttt{thr}}\right&space;)" title="\texttt{Tessier\_type}(\texttt{var},\texttt{thr},\texttt{k}) = \exp\left(-\frac{\texttt{var}}{\texttt{k}}\right)-\exp\left(-\frac{\texttt{var}}{\texttt{thr}}\right )" /></a>
  
- **_Andrews(var,thr,k)_** is another microbial growth function defind in the following way:
  - <a href="https://www.codecogs.com/eqnedit.php?latex=\texttt{Andrews}(\texttt{var},\texttt{thr},\texttt{k})&space;=&space;\frac{1}{(1&plus;\frac{\texttt{thr}}{\texttt{var}})\cdot(1&plus;\frac{\texttt{var}}{\texttt{k}})}" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\texttt{Andrews}(\texttt{var},\texttt{thr},\texttt{k})&space;=&space;\frac{1}{(1&plus;\frac{\texttt{thr}}{\texttt{var}})\cdot(1&plus;\frac{\texttt{var}}{\texttt{k}})}" title="\texttt{Andrews}(\texttt{var},\texttt{thr},\texttt{k}) = \frac{1}{(1+\frac{\texttt{thr}}{\texttt{var}})\cdot(1+\frac{\texttt{var}}{\texttt{k}})}" /></a>
  
where **_var_** is a member of **_variables_** list; **_thr_** is meant to be an important value of interest for the particular variable **_var_** and should be represented as numeric value; all other coefficients (e.g., **_k_**, **_a_**, etc.) are either numeric values or one of the names defined in the **_constants_** list.

### Example:

VARS: x, y

CONSTS: k2, 1; deg\_y, 0.1; a, 1; b, 0; n, 5

PARAMS: k1, 0, 2; deg\_x, 0, 1

EQ: x = k1\*Hillm(y, 5, n, a, b) - deg\_x\*x

EQ: y = k2\*Hillm(x, 5, 5, 1, 0) - deg\_y\*y

VAR\_POINTS: x: 1500, 10; y: 1500, 10

THRES: x: 0, 15

THRES: y: 0, 15
