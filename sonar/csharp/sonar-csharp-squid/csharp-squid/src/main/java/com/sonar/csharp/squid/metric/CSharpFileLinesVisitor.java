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
package com.sonar.csharp.squid.metric;

import com.google.common.collect.Sets;
import com.sonar.csharp.squid.api.CSharpMetric;
import com.sonar.sslr.api.*;
import com.sonar.sslr.squid.SquidAstVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Visitor that computes the CoreMetrics.NCLOC_DATA_KEY & CoreMetrics.COMMENT_LINES_DATA_KEY metrics used by the DevCockpit.
 */
public class CSharpFileLinesVisitor extends SquidAstVisitor<Grammar> implements AstAndTokenVisitor {

  private static final Logger LOG = LoggerFactory.getLogger(CSharpFileLinesVisitor.class);

  private final Project project;
  private final FileLinesContextFactory fileLinesContextFactory;
  private final boolean includeTestProjects;
  private FileLinesContext fileLinesContext;
  private final Set<Integer> linesOfCode = Sets.newHashSet();
  private final Set<Integer> linesOfComments = Sets.newHashSet();

  public CSharpFileLinesVisitor(Project project, FileLinesContextFactory fileLinesContextFactory, boolean includeTestProjects) {
    this.project = project;
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.includeTestProjects =  includeTestProjects;

  }

  @Override
  public void visitFile(AstNode astNode) {
    java.io.File contextFile = getContext().getFile();

    List<java.io.File> searchDirs = new ArrayList<java.io.File>();
    searchDirs.addAll(project.getFileSystem().getSourceDirs());
    if (includeTestProjects) {
      searchDirs.addAll(project.getFileSystem().getTestDirs());
    }
    File sonarFile = File.fromIOFile(contextFile, searchDirs);


    if (sonarFile == null) {
       LOG.error("Unable to find sonarFile for contextFile {}. If this is a test file, you'll need to enable test file parsing.", contextFile.getName());
    }

    fileLinesContext = fileLinesContextFactory.createFor(sonarFile);
  }

  @Override
  public void leaveFile(AstNode astNode) {
    int fileLength = getContext().peekSourceCode().getInt(CSharpMetric.LINES);

    for (int line = 1; line <= fileLength; line++) {
      fileLinesContext.setIntValue(CoreMetrics.NCLOC_DATA_KEY, line, getLineOfCode(line));
      fileLinesContext.setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, line, getLineOfComment(line));
    }
    fileLinesContext.save();

    linesOfCode.clear();
    linesOfComments.clear();
  }

  public void visitToken(Token token) {
    if (token.getType().equals(GenericTokenType.EOF)) {
      return;
    }

    linesOfCode.add(token.getLine());
    List<Trivia> trivias = token.getTrivia();
    for (Trivia trivia : trivias) {
      if (trivia.isComment()) {
        linesOfComments.add(trivia.getToken().getLine());
      }
    }
  }

  private int getLineOfCode(int line) {
    if (linesOfCode.contains(line)) {
      return 1;
    }
    return 0;
  }

  private int getLineOfComment(int line) {
    if (linesOfComments.contains(line)) {
      return 1;
    }
    return 0;
  }

}
