package com.whatap.product.aop;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class CustomSpringELParser {
  private CustomSpringELParser() {}

  public static Object getDynamicValue(String[] parameterNames, Object[] args, String key) {
    ExpressionParser parser = new SpelExpressionParser();
    StandardEvaluationContext context = new StandardEvaluationContext();

    // 반복문을 돌며 method 파라미터의 이름과 값 매핑
    for (int i = 0; i < parameterNames.length; i++) {
      context.setVariable(parameterNames[i], args[i]);
    }

    // key에 해당하는 파라미터 값 가져오기
    return parser.parseExpression(key).getValue(context, Object.class);
  }
}
