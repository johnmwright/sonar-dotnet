/*
 * Sonar C# Plugin :: C# Squid :: Squid
 * Copyright (C) 2010 Jose Chillan, Alexandre Victoor and SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonar.csharp.squid.tree;

import com.google.common.collect.ImmutableSet;
import com.sonar.csharp.squid.api.CSharpMetric;
import com.sonar.csharp.squid.api.CSharpPunctuator;
import com.sonar.csharp.squid.api.source.SourceMember;
import com.sonar.csharp.squid.api.source.SourceMethod;
import com.sonar.csharp.squid.api.source.SourceType;
import com.sonar.csharp.squid.parser.CSharpGrammar;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.squid.SquidAstVisitor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.SonarException;

import java.util.Collections;
import java.util.Set;

/**
 * Visitor that creates member resources (= methods, property accessors, event accessors, indexer accessors, operators, constructors,
 * finalizers) and computes the number of members.
 */


public class CSharpMemberVisitor extends SquidAstVisitor<Grammar> {

    private static final Logger LOG = LoggerFactory.getLogger(CSharpMemberVisitor.class);


    /**
   * {@inheritDoc}
   */
  @Override
  public void init() {
    subscribeTo(
        CSharpGrammar.METHOD_DECLARATION,
        CSharpGrammar.CONSTRUCTOR_BODY,
        CSharpGrammar.STATIC_CONSTRUCTOR_BODY,
        CSharpGrammar.DESTRUCTOR_BODY,
        CSharpGrammar.ACCESSOR_BODY,
        CSharpGrammar.ADD_ACCESSOR_DECLARATION,
        CSharpGrammar.REMOVE_ACCESSOR_DECLARATION,
        CSharpGrammar.OPERATOR_BODY);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void visitNode(AstNode astNode) {
    if (astNode.getChild(0).is(CSharpPunctuator.SEMICOLON)) {
      // this is an empty declaration
      return;
    }

    SourceMember member = createSourceMember(astNode);
    member.setMeasure(CSharpMetric.METHODS, 1);
    getContext().addSourceCode(member);
  }

  private SourceMember createSourceMember(AstNode astNode) {
      String memberSignature = defineMemberSignature(astNode);

      if (astNode.is(CSharpGrammar.METHOD_DECLARATION)){
          String methodSignatureWithParams = extractMethodSignatureWithParams(astNode);
          return new SourceMethod((SourceType) getContext().peekSourceCode(), memberSignature, astNode.getTokenLine(), methodSignatureWithParams);
      } else {
          return new SourceMember((SourceType) getContext().peekSourceCode(), memberSignature, astNode.getTokenLine());
      }
  }

    private String defineMemberSignature(AstNode astNode) {
    String memberSignature = "";
    if (astNode.is(CSharpGrammar.METHOD_DECLARATION)) {
      memberSignature = extractMethodSignature(astNode.getFirstChild(CSharpGrammar.METHOD_BODY));
    } else if (astNode.is(CSharpGrammar.ACCESSOR_BODY)) {
      memberSignature = extractPropertySignature(astNode);
    } else if (astNode.is(CSharpGrammar.ADD_ACCESSOR_DECLARATION)) {
      memberSignature = extractEventSignature("add", astNode);
    } else if (astNode.is(CSharpGrammar.REMOVE_ACCESSOR_DECLARATION)) {
      memberSignature = extractEventSignature("remove", astNode);
    } else if (astNode.is(CSharpGrammar.CONSTRUCTOR_BODY)) {
      memberSignature = ".ctor:" + astNode.getTokenLine();
    } else if (astNode.is(CSharpGrammar.STATIC_CONSTRUCTOR_BODY)) {
      memberSignature = ".cctor():" + astNode.getTokenLine();
    } else if (astNode.is(CSharpGrammar.DESTRUCTOR_BODY)) {
      memberSignature = "Finalize:" + astNode.getTokenLine();
    } else if (astNode.is(CSharpGrammar.OPERATOR_BODY)) {
      // call it "op", but should be more precise: for instance, "+" => "op_Addition"
      memberSignature = "op:" + astNode.getTokenLine();
    } else {
      throw new IllegalStateException("The current AST node is not supported by this visitor.");
    }
    return memberSignature;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void leaveNode(AstNode astNode) {
    if (astNode.getChild(0).is(CSharpPunctuator.SEMICOLON)) {
      // this was an empty declaration
      return;
    }
    getContext().popSourceCode();
  }

  private String extractMethodSignature(AstNode astNode) {
    return astNode.getParent().getFirstChild(CSharpGrammar.MEMBER_NAME).getTokenValue() + ":" + astNode.getTokenLine();
  }


    private String extractMethodSignatureWithParams(AstNode astNode) {
        if (!astNode.is(CSharpGrammar.METHOD_DECLARATION)) {
            throw new SonarException("member is not a method");
        }
        String methodName = extractMethodName(astNode);
        String params = StringUtils.join( getFormalParameters(astNode), ", "); //TODO: is this the right node to pass in?
        return methodName + "(" + params + ")";
    }

    private String extractMethodName(AstNode methodDeclarationNode) {
        AstNode astNode = methodDeclarationNode.getFirstChild(CSharpGrammar.METHOD_BODY);

        AstNode parentNode = astNode.getParent();
        AstNode memberNameNode = parentNode.getFirstChild(CSharpGrammar.MEMBER_NAME);
        String methodName =  memberNameNode.getTokenValue();

        return methodName;
    }

    private static Set<String> getFormalParameters(AstNode methodNode) {
        //Logic is very similar to ParameterAssignedToCheck.getNonOutNorRefFormalParameters -- consider consolidating

        if (!methodNode.hasDirectChildren(CSharpGrammar.FORMAL_PARAMETER_LIST)){
           return Collections.EMPTY_SET;
        }

        AstNode paramsNode = methodNode.getFirstChild(CSharpGrammar.FORMAL_PARAMETER_LIST);

        ImmutableSet.Builder<String> builder = ImmutableSet.builder();

        Iterable<AstNode> fixedParameters = paramsNode
                .select()
                .children(CSharpGrammar.FIXED_PARAMETERS)
                .children(CSharpGrammar.FIXED_PARAMETER);

        for (AstNode fixedParameter : fixedParameters) {
           builder.add(fixedParameter.getFirstChild(CSharpGrammar.TYPE).getTokenOriginalValue());
        }

        return builder.build();
    }


  private String extractPropertySignature(AstNode astNode) {
    StringBuilder signature = new StringBuilder(astNode.getPreviousSibling().getLastToken().getValue());
    signature.append("_");
    AstNode delcarationNode = astNode.getParent().getParent().getParent();
    if (delcarationNode.is(CSharpGrammar.INDEXER_DECLARATION)) {
      signature.append("Item");
    } else {
      signature.append(delcarationNode.getFirstChild(CSharpGrammar.MEMBER_NAME).getTokenValue());
    }
    signature.append(":");
    signature.append(astNode.getTokenLine());
    return signature.toString();
  }

  private String extractEventSignature(String accessor, AstNode astNode) {
    StringBuilder signature = new StringBuilder(accessor);
    signature.append("_");
    AstNode delcarationNode = astNode.getParent().getParent();
    signature.append(delcarationNode.getFirstChild(CSharpGrammar.MEMBER_NAME).getTokenValue());
    signature.append(":");
    signature.append(astNode.getTokenLine());
    return signature.toString();
  }

}
