/*
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exactpro.sf.aml.scriptutil;

import static com.exactpro.sf.aml.scriptutil.ExpressionResult.EXPRESSION_RESULT_FALSE;
import static com.exactpro.sf.aml.scriptutil.ExpressionResult.create;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.exactpro.sf.comparison.ComparisonExistenceFilter;
import com.exactpro.sf.comparison.ComparisonNotNullFilter;
import com.exactpro.sf.comparison.ComparisonNullFilter;
import com.exactpro.sf.comparison.IComparisonFilter;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.mvel2.MVEL;
import org.mvel2.PropertyAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.AMLLangConst;
import com.exactpro.sf.aml.generator.MVELInitializer;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.comparison.Convention;
import com.exactpro.sf.util.LRUMap;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;

public class StaticUtil {
	private static final Logger logger = LoggerFactory.getLogger(StaticUtil.class);

	/**
	 * Pattern for string um.call(SailfishURI.parse("plugin:class.function")[, ]
	 */
	private static final Pattern UTILITY_FUNCTION = Pattern.compile("um\\.call\\(SailfishURI\\.parse\\(\"(?<SURI>.+?\\:.+?\\..+?)\"\\)(, )?+");
	private static final int EXPRESSIONS_CACHE_SIZE = 10_000;

	// Do not clear this map as soon as users
	// will execute the same matrix for a long time
	// and most of expressions will reused several times.
	private static final Map<String, Serializable> EXPRESSION_CACHE = new LRUMap<>(EXPRESSIONS_CACHE_SIZE);

	private static Map<String, Object> toMap(Object... args) {
	    Objects.requireNonNull(args, "Arguments cannot be null");

	    if(args.length % 2 > 0) {
	        throw new IllegalArgumentException("Amount of arguments must be even");
		}

		Map<String, Object> argsMap = new HashMap<>();

		for (int i=0; i<args.length; i+=2) {
		    argsMap.put(String.valueOf(args[i]), args[i + 1]);
	    }

		return argsMap;
	}

    public static Object eval(long line, String column, String expression, Object... args) {
        try {
            return eval(line, column, expression, toMap(args));
        } catch(MvelException e) {
            throw e;
        } catch(Exception e) {
            throw new MvelException(line, column, e);
        }
	}

    public static boolean evalCondition(long line, String column, String expression, Object... args) {
        try {
            return Boolean.TRUE.equals(eval(line, column, expression, args));
        } catch (MvelException e) {
            throw new ConditionException(e.getLine(), e.getColumn(), expression, e);
        }
    }

	private static Object eval(long line, String column, String expression, Map<String, Object> args) {
		try {
    	    Serializable compiledExpression = compileExpression(expression);
    		Object result = MVEL.executeExpression(compiledExpression, args);

		    /* TODO: check for field presence while parsing expression
		     * if (result == null) {
			    throw new EPSCommonException("Can not evaluate expression: "+expression);
		    }*/

		    return result;
        } catch(PropertyAccessException e) {
            throw processPropertyAccessException(line, column, e);
        } catch(Exception e) {
		    throw new MvelException(line, column, e);
	    }
	}

	public static IFilter nullFilter(long line, String column) {
		return new NullFilter(line, column);
	}

	public static IFilter notNullFilter(long line, String column) {
		return new NotNullFilter(line, column);
	}

	public static IFilter existenceFilter(long line, String column) {
		return new ExistenceFilter(line, column);
	}

    /**
     * This method is called from matrix generated code.
     */
	public static IFilter simpleFilter(long line, String column, String condition, Object... args) {
	    Map<String, Object> variables = toMap(args);
	    Object value = eval(line, column, condition, variables);
	    if (value instanceof IKnownBug) {
            return new KnownBugFilter(line, column, (IKnownBug)value);
        } else if (value instanceof IFilter) {
            return (IFilter)value;
	    } else if(value == Convention.CONV_PRESENT_OBJECT) {
	        return new NotNullFilter(line, column);
	    } else if(value == Convention.CONV_MISSED_OBJECT) {
	        return new NullFilter(line, column);
        } else if(value == Convention.CONV_EXISTENCE_OBJECT) {
            return new ExistenceFilter(line, column);
	    } else {
	        return new SimpleMvelFilter(line, column, value, variables);
	    }
	}

    public static IFilter regexFilter(long line, String column, String condition, Object... args) {
		return new RegexMvelFilter(line, column, StringEscapeUtils.escapeJava(condition), toMap(args));
	}

	public static IFilter filter(long line, String column, String condition, Object... args) {
		return new MvelFilter(line, column, condition, toMap(args));
	}

    public static IFilter countFilter(long line, String column, String condition, Object... args) {
        Map<String, Object> variables = toMap(args);
        Object value = eval(line, column, condition, variables);

        if((value instanceof String || value instanceof Number) && MessageCount.isValidExpression(value.toString())) {
            return new MessageCountFilter(line, MessageCount.fromString(value.toString()));
        } else if(value instanceof IKnownBug) {
            return new KnownBugFilter(line, column, (IKnownBug)value);
        } else if (value instanceof IFilter) {
            return (IFilter)value;
        }

        throw new MvelException(line, column, "Unsupported value type: " + ClassUtils.getSimpleName(value, null));
	}

    public static Object stripFilter(Object o) {
        return stripFilter(o, false);
    }

	public static Object stripFilter(Object o, boolean dontFailOnFilter) {
		if(o instanceof IFilter) {
		    IFilter filter = (IFilter)o;
		    try {
                return filter.getValue();
            } catch(MvelException e) {
                if(dontFailOnFilter) {
                    return filter.getCondition();
                }

                throw new EPSCommonException("Failed to get value: probably wrong filter type", e);
            }
		} else if(o instanceof IMessage) {
			IMessage msg = ((IMessage)o).cloneMessage();

			for(String fldName : msg.getFieldNames()) {
				msg.addField(fldName, stripFilter(msg.getField(fldName), dontFailOnFilter));
			}

			return msg;
		} else if(o instanceof List<?>) {
			List<Object> list = new ArrayList<>();

			for(Object value : (List<?>)o) {
				list.add(stripFilter(value, dontFailOnFilter));
			}

			return list;
		}

		return o;
	}

	/**
     * Remove redundant utility method calls from human readable condition
     */
    public static String removeUtilityCall(CharSequence condition) {
        Matcher matcher = UTILITY_FUNCTION.matcher(condition);
        return matcher.replaceAll("${SURI}(");
    }

    private static Serializable compileExpression(String expression) {
	    Serializable compiledExpression = EXPRESSION_CACHE.get(expression);

	    if(compiledExpression == null) {
	        compiledExpression = MVEL.compileExpression(expression, MVELInitializer.getInstance().getCtx());
	        EXPRESSION_CACHE.put(expression, compiledExpression);
	    }

	    return compiledExpression;
	}

    private static MvelException processPropertyAccessException(long line, String column, PropertyAccessException e) {
        char c = e.getExpr()[e.getCursor()];

        switch(c) {
        case '\u201c':
        case '\u201d':
            return new MvelException(line, column, "Expression contains invalid double quotation mark: " + c, e);
        default:
            return new MvelException(line, column, e);
        }
    }

    public interface IKnownBug {
        ExpressionResult validate(Object actualValue);
        String getCondition();
        IKnownBug Bug(String subject, Object alternativeValue, String... categories);
        IKnownBug BugEmpty(String subject, String... categories);
        IKnownBug BugAny(String subject, String... categories);
        IKnownBug BugExistence(String subject, String... categories);
        IKnownBug Actual(Object obj);
        IFilter toFilter();
    }

    public interface IFilter extends IComparisonFilter { }

    private static class MvelFilter implements IFilter {
		private static final Pattern NOT_VARIABLE_REGEX = Pattern.compile(AMLLangConst.REGEX_MVEL_NOT_VARIABLE);

		private final String condition;
		private final Map<String, Object> variables;
		private final Serializable compiledCondition;
		private final long line;
		private final String column;

		public MvelFilter(long line, String column, String condition, Map<String, Object> variables) {
			this.line = line;
			this.column = column;
		    this.condition = evaluateCondition(line, column, condition, variables);
			this.variables = Collections.unmodifiableMap(variables);

			try {
			    this.compiledCondition = compileExpression(condition);
			} catch(Exception e) {
			    throw new MvelException(line, column, e);
		    }
		}

		@Override
        public ExpressionResult validate(Object value) {
            Map<String, Object> vars = new HashMap<>(variables);
            vars.put("x", value);
            try {
                Object obj = MVEL.executeExpression(compiledCondition, vars);
                if (obj instanceof IKnownBug) {
                    IKnownBug knownBug = (IKnownBug) obj;
                    return knownBug.validate(value);
                } else if (obj instanceof Boolean) {
                    return create((boolean)obj);
                } else if (obj instanceof ExpressionResult) {
                	return (ExpressionResult) obj;
				}
                throw new EPSCommonException("Incorrect type of expression result: expected 'Boolean', actual '" + (obj != null ? obj.getClass().getSimpleName() : obj) + "'");
            } catch(PropertyAccessException e) {
                throw processPropertyAccessException(line, column, e);
            } catch (Exception e) {
                throw new MvelException(line, column, e);
            }
		}

		@Override
		public String getCondition() {
			return condition;
		}

        @Override
        public String getCondition(Object value) {
            return getCondition();
        }

        private String evaluateCondition(long line, String column, String condition, Map<String, Object> variables) {
            try {
			    Matcher m = NOT_VARIABLE_REGEX.matcher(condition);
			    StringBuilder result = new StringBuilder(condition.length());
			    int lastMatch = 0;

			    while (m.find()) {
				    String var = condition.substring(lastMatch, m.start());

				    if (var.isEmpty()) {
					    // skip
				    } else if ("x".equals(var)) {
					    result.append("x"); // don't replace 'x'
				    } else {
					    Object evaluated;
					    try {
                            evaluated = eval(line, column, var, variables);
						    // escape strings and characters
						    if (evaluated instanceof String) {
							    evaluated = "\"" + evaluated + "\"";
						    } else if (evaluated instanceof Character) {
							    evaluated = "'" + evaluated + "'";
						    } else if (evaluated instanceof Method) {
							    Method method =  (Method) evaluated;
							    evaluated = method.getDeclaringClass().getSimpleName() + "." + method.getName();
						    }
                        } catch (RuntimeException e) {
                            logger.debug("Failed to evaluate variable: {}", var, e);
				        	// don't break
						    evaluated = var;
					    }

					    result.append(evaluated);
				    }

				    // copy delimeter to result
				    result.append(m.group());
				    lastMatch = m.end();
			    }

			    return removeUtilityCall(result);
            } catch(RuntimeException ex) {
                logger.debug("Failed to get condition: {}", condition, ex);
                // don't break test. Print as is:
                return condition;
            } catch(Exception e) {
                throw new MvelException(line, column, e);
            }
		}

		@Override
        public Object getValue() {
            throw new MvelException(line, column, "Cannot get value from " + getClass().getSimpleName());
        }

        @Override
        public boolean hasValue() {
            return false;
        }
	}

	private static class NullFilter extends ComparisonNullFilter implements IFilter {
	    private final long line;
	    private final String column;

	    public NullFilter(long line, String column) {
	        this.line = line;
	        this.column = column;
	    }

        @Override
        public Object getValue() {
            throw new MvelException(line, column, "Cannot get value from " + getClass().getSimpleName());
        }
    }

	private static class NotNullFilter extends ComparisonNotNullFilter implements IFilter {
	    private final long line;
        private final String column;

        public NotNullFilter(long line, String column) {
            this.line = line;
            this.column = column;
        }

        @Override
        public Object getValue() {
            throw new MvelException(line, column, "Cannot get value from " + getClass().getSimpleName());
        }
    }

    private static class ExistenceFilter extends ComparisonExistenceFilter implements IFilter {
        private final long line;
        private final String column;

        public ExistenceFilter(long line, String column) {
            this.line = line;
            this.column = column;
        }

        @Override
        public Object getValue() {
            throw new MvelException(line, column, "Cannot get value from " + getClass().getSimpleName());
        }
    }

    public static class KnownBugFilter implements IFilter {

	    private final IKnownBug knownBug;
	    private final long line;
	    private final String column;

	    public KnownBugFilter(long line, String column, IKnownBug checkable) {
	        this.knownBug = checkable;
	        this.line = line;
	        this.column = column;
        }

        @Override
        public ExpressionResult validate(Object value) {
            try {
                return knownBug.validate(value);
            } catch (RuntimeException e) {
                throw new MvelException(line, column, e);
            }
        }

        @Override
        public String getCondition() {
            return knownBug.getCondition();
        }

        @Override
        public String getCondition(Object value) {
            return getCondition();
        }

        @Override
        public Object getValue() throws MvelException {
            throw new MvelException(line, column, "Cannot get value from " + getClass().getSimpleName());
        }

        @Override
        public boolean hasValue() {
            return false;
        }
	}

	public static class SimpleMvelFilter implements IFilter {
        protected static final String SIMPLE_VALUE_ARG = "SIMPLE_VALUE";
        private static final String SIMPLE_VALUE_EXPRESSION = "x == (" + SIMPLE_VALUE_ARG + ")";
        private static final String SIMPLE_FLOAT_VALUE_EXPRESSION = "Objects.equals(x, " + SIMPLE_VALUE_ARG + ")";
		private final String condition;
		private final Map<String, Object> variables;
		private final Serializable compiledCondition;
		private final Object value;
		private final long line;
		private final String column;

		public SimpleMvelFilter(long line, String column, String simpleCondition, Map<String, Object> variables) {
			this(line, column, simpleCondition, variables, SIMPLE_VALUE_EXPRESSION);
		}

        public SimpleMvelFilter(long line, String column, Object simpleValue, Map<String, Object> variables) {
            this(line, column, simpleValue, variables, SIMPLE_VALUE_EXPRESSION);
        }

        public SimpleMvelFilter(long line, String column, String simpleCondition, Map<String, Object> variables, String expression) {
            this(line, column, eval(line, column, simpleCondition, variables), variables, expression);
        }

        public SimpleMvelFilter(long line, String column, Object simpleValue, Map<String, Object> variables, String expression) {
			this.line = line;
			this.column = column;
			this.value = simpleValue;
            this.condition = String.valueOf(value);
			this.variables = new HashMap<>(variables);
            this.variables.put(SIMPLE_VALUE_ARG, value);

            if((value instanceof Double && !Doubles.isFinite((Double)value))
                    || (value instanceof Float && !Floats.isFinite((Float)value))) {
                expression = SIMPLE_FLOAT_VALUE_EXPRESSION;
            }

            try {
                this.compiledCondition = compileExpression(expression);
            } catch(Exception e) {
                throw new MvelException(line, column, e);
	    	}
		}

		@Override
		public ExpressionResult validate(Object value) {
            Map<String, Object> vars = new HashMap<>(variables);

			vars.put("x", value);

			try {
			    if (logger.isDebugEnabled()) {
                    logger.debug("comparing ... condition: {}", condition);

				    for (Entry<String, Object> var : vars.entrySet()) {
					    String cls = null;
					    Object val = var.getValue();

					    if (val != null) {
						    cls = val.getClass().getCanonicalName();
					    }

	                    logger.debug("variable: {}, value: {}, class: {}", var.getKey(), val, cls);
	                }
				}
                return create(MVEL.executeExpression(compiledCondition, vars, Boolean.class));
            } catch(PropertyAccessException e) {
                throw processPropertyAccessException(line, column, e);
            } catch (Exception e) {
                throw new MvelException(line, column, e);
            }
		}

		@Override
		public String getCondition(){
			return condition;
		}

        @Override
        public String getCondition(Object value) {
            return getCondition();
        }

        @Override
        public Object getValue() {
	        return value;
        }

        @Override
        public String toString() {
            return getCondition();
        }

        @Override
        public boolean hasValue() {
            return true;
        }
    }

    public static class RegexMvelFilter extends SimpleMvelFilter {
        private static final String REGEX_VALUE_EXPRESSION = "x ~= ( " + SIMPLE_VALUE_ARG + " )";

        public RegexMvelFilter(long line, String column, String regex, Map<String, Object> variables) {
            super(line, column, "'" + regex + "'", variables, REGEX_VALUE_EXPRESSION);
        }
    }

    public static class MessageCountFilter implements IFilter {
        private final long line;
        private final MessageCount count;

        public MessageCountFilter(long line, MessageCount count) {
            this.line = line;
            this.count = count;
        }

        @Override
        public ExpressionResult validate(Object value) {
            return value instanceof Integer ? create(count.checkInt((int)value)) : EXPRESSION_RESULT_FALSE;
        }

        @Override
        public String getCondition() {
            return count.toString();
        }

        @Override
        public String getCondition(Object value) {
            return getCondition();
        }

        @Override
        public Object getValue() throws MvelException {
            throw new MvelException(line, Column.MessageCount.getName(), "Cannot get value from " + getClass().getSimpleName());
        }

        @Override
        public boolean hasValue() {
            return false;
        }
    }
}
